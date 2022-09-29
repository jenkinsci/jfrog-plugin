package jenkins.plugins.jfrog;

import hudson.Extension;
import hudson.FilePath;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;
import hudson.tools.ToolInstallation;
import jenkins.MasterToSlaveFileCallable;
import jenkins.plugins.jfrog.configuration.JFrogPlatformInstance;
import org.apache.commons.io.FileUtils;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.File;
import java.io.IOException;
import java.net.URL;

// TODO change to ReleasesInstaller
public class JfrogInstaller extends BinaryInstaller {
    public final String id;

    public final String ARTIFACTORY_URL = "https://releases.jfrog.io/artifactory";
    public final String REPOSITORY = "jfrog-cli";


    @DataBoundConstructor
    public JfrogInstaller(String id) {
        super(null);
        this.id = id;
    }


    @Override
    public FilePath performInstallation(ToolInstallation tool, Node node, TaskListener log) throws IOException, InterruptedException {
        JFrogPlatformInstance server =  new JFrogPlatformInstance("", "", null, ARTIFACTORY_URL, "", "", "", "");
        return Utils.performJfrogCliInstallation(getToolLocation(tool, node), log, id, server, REPOSITORY);
    }


    @Extension
    public static final class DescriptorImpl extends BinaryInstaller.DescriptorImpl<JfrogInstaller> {
        public String getDisplayName() {
            return "Install from releases.jfrog.io";
        }

        @Override
        public boolean isApplicable(Class<? extends ToolInstallation> toolType) {
            return toolType== JfrogInstallation.class;
        }


    }
}