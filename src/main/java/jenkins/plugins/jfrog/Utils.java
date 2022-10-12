package jenkins.plugins.jfrog;

import hudson.FilePath;
import hudson.model.Job;
import hudson.model.TaskListener;
import jenkins.security.MasterToSlaveCallable;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

/**
 * @author gail
 */
public class Utils {
    static final String JFROG_CLI_HOME_DIR = "JFROG_CLI_HOME_DIR";
    private static final String BINARY_NAME = "jf";

    public static FilePath getWorkspace(Job<?, ?> project) {
        FilePath projectJob = new FilePath(project.getRootDir());
        FilePath workspace = projectJob.getParent();
        if (workspace == null) {
            throw new RuntimeException("Failed to get job workspace.");
        }
        workspace = workspace.sibling("workspace");
        if (workspace == null) {
            throw new RuntimeException("Failed to get job workspace.");
        }
        return workspace.child(project.getName());
    }

    public static String getJfrogCliBinaryName(boolean isWindows) {
        if (isWindows) {
            return BINARY_NAME + ".exe";
        }
        return BINARY_NAME;
    }

    /**
     * Delete temp jfrog cli home directory associated with the build number.
     *
     * @param ws           - The workspace
     * @param buildNumber  - The build number
     * @param taskListener - The logger
     */
    public static void deleteBuildJfrogHomeDir(FilePath ws, String buildNumber, TaskListener taskListener) {
        try {
            FilePath jfrogCliHomeDir = createAndGetJfrogCliHomeTempDir(ws, buildNumber);
            jfrogCliHomeDir.deleteRecursive();
            taskListener.getLogger().println(jfrogCliHomeDir.getRemote() + " deleted");
        } catch (IOException | InterruptedException e) {
            taskListener.getLogger().println("Failed while attempting to delete the JFrog CLI home dir \n" + ExceptionUtils.getRootCauseMessage(e));
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
                FilePath tempDir = ws.sibling(ws.getName() + Objects.toString(workspaceList, "@") + "tmp");
                if (tempDir == null) {
                    throw new RuntimeException("Failed to create JFrog CLI temporary directory");
                }
                tempDir = tempDir.child("jfrog");
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
}
