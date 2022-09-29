package jenkins.plugins.jfrog;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.model.Job;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;
import jenkins.MasterToSlaveFileCallable;
import jenkins.plugins.jfrog.artifactoryclient.ArtifactoryClient;
import jenkins.plugins.jfrog.configuration.Credentials;
import jenkins.plugins.jfrog.configuration.JFrogPlatformInstance;
import jenkins.plugins.jfrog.plugins.PluginsUtils;
import jenkins.security.MasterToSlaveCallable;
import org.apache.http.client.methods.CloseableHttpResponse;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

/**
 * @author gail
 */
public class Utils {
    static final String JFROG_CLI_HOME_DIR = "JFROG_CLI_HOME_DIR";
    public static final String BINARY_NAME = "jf";
    /**
     * The tool's directory name indicates its version.
     * To indicate the latest version, we will use constant name if no version was provided.
     */
    public static final String LATEST = "latest";

    /**
     * decoded "[RELEASE]" for thee download url
     */
    public static final String RELEASE = "latest";

    public static FilePath getWorkspace(Job<?, ?> project) {
        FilePath projectJob = new FilePath(project.getRootDir());
        return projectJob.getParent().sibling("workspace").child(project.getName());
    }

    public static EnvVars addJfrogCliHomeDirToEnv(EnvVars env, final FilePath workspace, String buildNumber) throws IOException, InterruptedException {
        if (env == null) {
            env = new EnvVars();
        }
        String jfrogHomeTempDir = createAndGetJfrogCliHomeTempDir(workspace, buildNumber).getRemote();
        env.put(JFROG_CLI_HOME_DIR, jfrogHomeTempDir);
        return env;
    }

    /**
     * Delete temp jfrog cli home directory associated with the build number.
     * @param ws           - The workspace
     * @param buildNumber  - The build number
     * @param taskListener - The logger
     */
    public static void deleteBuildJfrogHomeDir(FilePath ws, String buildNumber, TaskListener taskListener) {
        try {
            FilePath buildDataDir = createAndGetJfrogCliHomeTempDir(ws, buildNumber);
            buildDataDir.deleteRecursive();
            taskListener.getLogger().println(buildDataDir.getRemote() + " deleted");
        } catch (IOException | InterruptedException e) {
            taskListener.getLogger().println("Failed while attempting to delete build data dir for build number " + buildNumber + "\n"+ e.getMessage());
        }
    }

    /**
     * Create a temporary jfrog cli home directory under a given workspace
     */
    public static FilePath createAndGetTempDir(final FilePath ws) throws IOException, InterruptedException {
        // The token that combines the project name and unique number to create unique workspace directory.
        String workspaceList = System.getProperty("hudson.slaves.WorkspaceList");
        return ws.act(new MasterToSlaveCallable<FilePath, IOException>() {
            @Override
            public FilePath call() {
                final FilePath tempDir = ws.sibling(ws.getName() + Objects.toString(workspaceList, "@") + "tmp").child("jfrog");
                File tempDirFile = new File(tempDir.getRemote());
                if (tempDirFile.mkdirs()) {
                    tempDirFile.deleteOnExit();
                }
                return tempDir;
            }
        });
    }

    public static FilePath createAndGetJfrogCliHomeTempDir(final FilePath ws, String buildNumber) throws IOException, InterruptedException {
        return createAndGetTempDir(ws).child(buildNumber).child(".jfrog");
    }

    /**
     * We should skip the download if the tool's directory already contains the specific version, otherwise we should download it.
     * An empty directory naming the specific version in the tool directory indicates if a specific tool is already downloaded.
     */
    private static boolean shouldDownloadToll(FilePath toolLocation, String id) throws IOException, InterruptedException {
        // An empty id indicates the latest version - we would like to override and reinstall the latest tool in this case.
        if (!id.isEmpty()){
            if (toolLocation.child(id).child(BINARY_NAME).exists()) {
                return false;
            }
        }
        String version = id;
        if (id.isEmpty()){
            version = LATEST;
        }
        // Delete old versions if exists
        toolLocation.deleteContents();
        toolLocation.child(version).mkdirs();
        return true;
    }

    /**
     * Download and locate the JFrog CLI binary in the specific build home directory.
     * @param f
     * @param log
     * @param v version. empty string indicates the latest version.
     * @param instance JFrogPlatformInstance contains url and credentials needed for the downloading operation.
     * @param REPOSITORY identifies the repository in Artifactory where the CLIs binary is stored.
     * @throws IOException
     */
    private static void downloadJfrogCli(File f, TaskListener log, String v, JFrogPlatformInstance instance, String REPOSITORY) throws IOException {
        // Getting relevant operating system
        String osDetails;
        try {
            osDetails = OsUtils.getOsDetails();
        } catch (OsUtils.UnsupportedOperatingSystem e) {;
            throw new IOException(e);
        }
        final String RELEASES = URLEncoder.encode("[RELEASE]", "UTF-8");
        String version = v;
        // An empty string indicates the latest version.
        if (version.isEmpty()){
            version = RELEASES;
        }
        String suffix = "/"+REPOSITORY+"/v2-jf/"+version+"/jfrog-cli-"+osDetails+"/"+BINARY_NAME;
        if (version.equals(RELEASES)){
            log.getLogger().printf("Download \'%s\' latest version from: %s\n", BINARY_NAME, instance.getArtifactoryUrl()+suffix);
        } else {
            log.getLogger().printf("Download \'%s\' version %s from: %s\n", BINARY_NAME, version, instance.getArtifactoryUrl()+suffix);
        }
        // Getting credentials
        String username = "", password = "", accessToken = "";
        if (instance.getCredentialsConfig() != null) {
            Credentials credentials = PluginsUtils.credentialsLookup(instance.getCredentialsConfig().getCredentialsId(), null);
                username = credentials.getUsername();
                password = credentials.getPassword();
                accessToken = credentials.getAccessToken();
        }
        // Downloading binary from Artifactory
        try (ArtifactoryClient client = new ArtifactoryClient(instance.getArtifactoryUrl(), username, password, accessToken, null, log);
             CloseableHttpResponse response = client.download(suffix)) {
            InputStream input = response.getEntity().getContent();
            if (response.getStatusLine().getStatusCode() != HttpURLConnection.HTTP_OK) {
                throw new IOException("Failed downloading JFrog CLI binary: "+response.getStatusLine());
            }
            File file = new File(f, BINARY_NAME);
            Files.copy(input,file.toPath(),StandardCopyOption.REPLACE_EXISTING);
            if (!file.setExecutable(true)) {
                throw new IOException("No permission to add execution permission to binary");
            }
        }
    }

    public static FilePath performJfrogCliInstallation(FilePath toolLocation, TaskListener log, String version, JFrogPlatformInstance instance, String REPOSITORY) throws IOException, InterruptedException {
        // Download Jfrog CLI binary
        if (shouldDownloadToll(toolLocation, version)) {
            toolLocation.act(new MasterToSlaveFileCallable<Void>() {
                @Override
                public Void invoke(File f, VirtualChannel channel) throws IOException, InterruptedException {
                    downloadJfrogCli(f, log, version, instance, REPOSITORY);
                    return null;
                }
            });
        }
        return toolLocation;
    }
}
