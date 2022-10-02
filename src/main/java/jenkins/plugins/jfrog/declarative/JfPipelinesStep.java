package jenkins.plugins.jfrog.declarative;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.*;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.ArgumentListBuilder;
import jenkins.plugins.jfrog.Utils;
import jenkins.plugins.jfrog.configuration.Credentials;
import jenkins.plugins.jfrog.configuration.JFrogPlatformInstance;
import jenkins.plugins.jfrog.plugins.PluginsUtils;
import jenkins.tasks.SimpleBuildStep;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.plaincredentials.StringCredentials;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;

import static jenkins.plugins.jfrog.JfrogInstallation.JFROG_BINARY_PATH;
import static jenkins.plugins.jfrog.Utils.getJfrogCliBinaryName;
import static jenkins.plugins.jfrog.configuration.JfrogPlatformBuilder.getJFrogPlatformInstances;

/**
 * @author gail
 */
@SuppressWarnings("unused")
public class JfPipelinesStep<T> extends Builder implements SimpleBuildStep {
    static final String STEP_NAME = "jf";
    static final String JFROG_CLI_HOME_DIR = "JFROG_CLI_HOME_DIR";
    protected String args;

    @DataBoundConstructor
    public JfPipelinesStep(String args) {
        this.args = args;
    }

    public String getArgs() {
        return args;
    }

    /**
     * Build and run a 'jf' command.
     *
     * @param run       a build this is running as a part of
     * @param workspace a workspace to use for any file operations
     * @param env       environment variables applicable to this step
     * @param launcher  a way to start processes
     * @param listener  a place to send output
     * @throws InterruptedException
     * @throws IOException
     */
    @Override
    public void perform(@NonNull Run<?, ?> run, @NonNull FilePath workspace, @NonNull EnvVars env, @NonNull Launcher launcher, @NonNull TaskListener listener) throws InterruptedException, IOException {
        workspace.mkdirs();
        // Build the 'jf' command
        ArgumentListBuilder argsBuilder = new ArgumentListBuilder();
        String jfrogBinaryPath = env.get(JFROG_BINARY_PATH);
        argsBuilder.add(jfrogBinaryPath);
        argsBuilder.add(StringUtils.split(args));
        if (!launcher.isUnix()) {
            argsBuilder = argsBuilder.toWindowsCommand();
        }

        try {
            // Set up a temporary Jfrog CLI home directory for a specific run.
            FilePath jfrogHomeTempDir = Utils.createAndGetJfrogCliHomeTempDir(workspace, String.valueOf(run.getNumber()));
            env.put(JFROG_CLI_HOME_DIR, jfrogHomeTempDir.getRemote());
            Utils.addJfrogCliHomeDirToEnv(env, workspace, String.valueOf(run.getNumber()));
            Launcher.ProcStarter jfLauncher = launcher.launch().envs(env).pwd(workspace);
            // Configure all servers, skip if all server ids have already been configured.
            if (shouldConfig(jfrogHomeTempDir)) {
                configAllServers(jfLauncher, listener, jfrogBinaryPath, !launcher.isUnix());
            }
            // Running the 'jf' command
            int exitValue = jfLauncher.cmds(argsBuilder).stdout(listener).join();
            if (exitValue != 0) {
                throw new RuntimeException("Running 'jf' command failed with exit code " + exitValue);
            }
        } catch (Exception e) {
            String errorMessage = "Couldn't execute 'jf' command. " + ExceptionUtils.getRootCauseMessage(e);
            throw new RuntimeException(errorMessage, e);
        }
    }

    /**
     * Before we run a 'jf' command for the first time, we want to configure all servers first.
     * We know that all servers have already been configured if there is a "jfrog-cli.conf" file in the ".jfrog" home directory.
     *
     * @jfrogHomeTempDir the temp ".jfrog" directory path.
     */
    private boolean shouldConfig(FilePath jfrogHomeTempDir) throws IOException, InterruptedException {
        List<FilePath> filesList = jfrogHomeTempDir.list();
        for (FilePath file : filesList) {
            if (file.getName().contains("jfrog-cli.conf")) {
                return false;
            }
        }
        return true;
    }

    /**
     * Locally configure all servers that was configured in the Jenkins UI.
     */
    private void configAllServers(Launcher.ProcStarter launcher, TaskListener listener, String jfrogBinaryPath, boolean isWindows) throws IOException, InterruptedException {
        // Config all servers using the 'jf c add' command.
        List<JFrogPlatformInstance> jfrogInstances = getJFrogPlatformInstances();
        if (jfrogInstances != null && jfrogInstances.size() > 0) {
            for (JFrogPlatformInstance jfrogPlatformInstance : jfrogInstances) {
                // Build 'jf' command
                ArgumentListBuilder argsBuilder = new ArgumentListBuilder();
                addConfigArguments(argsBuilder, jfrogPlatformInstance, jfrogBinaryPath);
                if (isWindows) {
                    argsBuilder = argsBuilder.toWindowsCommand();
                }
                // Running 'jf' command
                launcher.cmds(argsBuilder).stdout(listener).join();
            }
        }
    }

    private void addConfigArguments(ArgumentListBuilder argsBuilder, JFrogPlatformInstance jfrogPlatformInstance, String jfrogBinaryPath) {
        String credentialsId = jfrogPlatformInstance.getId();
        argsBuilder.add(jfrogBinaryPath);
        argsBuilder.add("c");
        argsBuilder.add("add");
        argsBuilder.add(credentialsId);
        // Add credentials
        StringCredentials accessTokenCredentials = PluginsUtils.accessTokenCredentialsLookup(credentialsId);
        if (accessTokenCredentials != null) {
            argsBuilder.addMasked("access-token=" + accessTokenCredentials.getSecret().getPlainText());
        } else {
            Credentials credentials = PluginsUtils.credentialsLookup(credentialsId, null);
            argsBuilder.add("--user=" + credentials.getUsername());
            argsBuilder.addMasked("--password=" + credentials.getPassword());
        }
        // Add URLs
        argsBuilder.add("--url=" + jfrogPlatformInstance.getUrl());
        argsBuilder.add("--artifactory-url=" + jfrogPlatformInstance.getArtifactoryUrl());
        argsBuilder.add("--distribution-url=" + jfrogPlatformInstance.getDistributionUrl());
        argsBuilder.add("--xray-url=" + jfrogPlatformInstance.getXrayUrl());

        argsBuilder.add("--interactive=false");
        // The installation process takes place more than once per build, so we will configure the same server ID several times.
        argsBuilder.add("--overwrite=true");

    }

    @Symbol("jf")
    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
        @Override
        public String getDisplayName() {
            return "jf command";
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }
    }
}