package jenkins.plugins.jfrog;

import hudson.FilePath;
import hudson.model.*;
import hudson.slaves.NodeSpecific;
import hudson.tools.ToolInstallation;
import hudson.tools.ToolInstaller;
import hudson.tools.ToolInstallerDescriptor;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;
import java.util.*;

/**
 * Installer for JFrog CLI binary.
 *
 * @author gail
 */
public abstract class BinaryInstaller extends ToolInstaller {
    protected BinaryInstaller(String label) {
        super(label);
    }

    /**
     * @param tool the tool being installed.
     * @param node the computer on which to install the tool.
     * @return Node's filesystem location where a tool should be installed.
     */
    protected FilePath getToolLocation(ToolInstallation tool, Node node) throws IOException, InterruptedException {
        FilePath location = preferredLocation(tool, node);
        if (!location.exists()) {
            location.mkdirs();
        }
        return location;
    }

    public abstract static class DescriptorImpl<T extends BinaryInstaller> extends ToolInstallerDescriptor<T> {

        @SuppressWarnings("deprecation") // intentionally adding dynamic item here
        protected DescriptorImpl() {
            DownloadService.Downloadable.all().add(createDownloadable());
        }

        /**
         * function that creates a {@link DownloadService.Downloadable}.
         * @return a downloadable object
         */
        public DownloadService.Downloadable createDownloadable() {
            final BinaryInstaller.DescriptorImpl delegate = this;
            return new DownloadService.Downloadable(getId()) {
                @Override
                public JSONObject reduce(List<JSONObject> jsonList) {
                    return super.reduce(jsonList);

                }
            };
        }

        /**
         * This ID needs to be unique, and needs to match the ID token in the JSON update file.
         * <p>
         * By default we use the fully-qualified class name of the {@link BinaryInstaller} subtype.
         */
        @Override
        public String getId() {
            return clazz.getName().replace('$', '.');
        }

    }
}

