package jenkins.plugins.jfrog;

import hudson.Extension;
import hudson.FilePath;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.tools.ToolInstallation;
import jenkins.plugins.jfrog.configuration.JFrogPlatformInstance;
import org.apache.commons.lang3.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;

/**
 * Download and install Jfrog CLI from 'releases.jfrog.io'.
 *
 * @author gail
 */
public class ReleasesInstaller extends BinaryInstaller {
    public final String id;
    public final String ARTIFACTORY_URL = "https://releases.jfrog.io/artifactory";
    public final String REPOSITORY = "jfrog-cli";

    @DataBoundConstructor
    public ReleasesInstaller(String id) {
        super(null);
        this.id = id;
    }

    @Override
    public FilePath performInstallation(ToolInstallation tool, Node node, TaskListener log) throws IOException, InterruptedException {
        JFrogPlatformInstance server = new JFrogPlatformInstance(StringUtils.EMPTY, StringUtils.EMPTY, null, ARTIFACTORY_URL, StringUtils.EMPTY, StringUtils.EMPTY, StringUtils.EMPTY, StringUtils.EMPTY);
        return Utils.performJfrogCliInstallation(getToolLocation(tool, node), log, id, server, REPOSITORY);
    }

    @Extension
    public static final class DescriptorImpl extends BinaryInstaller.DescriptorImpl<ReleasesInstaller> {
        public String getDisplayName() {
            return "Install from releases.jfrog.io";
        }

        @Override
        public boolean isApplicable(Class<? extends ToolInstallation> toolType) {
            return toolType == JfrogInstallation.class;
        }
    }
}