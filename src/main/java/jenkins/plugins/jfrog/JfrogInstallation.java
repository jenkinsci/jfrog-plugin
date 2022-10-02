package jenkins.plugins.jfrog;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.EnvVars;
import hudson.Extension;
import hudson.model.EnvironmentSpecific;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.slaves.NodeSpecific;
import hudson.tools.ToolDescriptor;
import hudson.tools.ToolInstallation;
import hudson.tools.ToolInstaller;
import hudson.tools.ToolProperty;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import net.sf.json.JSONObject;

import javax.annotation.Nonnull;

import static jenkins.plugins.jfrog.Utils.getJfrogCliBinaryName;

/**
 * @author gail
 */
public class JfrogInstallation extends ToolInstallation
        implements NodeSpecific<JfrogInstallation>, EnvironmentSpecific<JfrogInstallation> {

    public static final String JFROG_BINARY_PATH = "JFROG_BINARY_PATH";

    @DataBoundConstructor
    public JfrogInstallation(String name, String home, List<? extends ToolProperty<?>> properties) {
        super(name, home, properties);
    }

    public JfrogInstallation forEnvironment(EnvVars environment) {
        return new JfrogInstallation(getName(), environment.expand(getHome()), getProperties().toList());
    }

    public JfrogInstallation forNode(@NonNull Node node, TaskListener log) throws IOException, InterruptedException {
        return new JfrogInstallation(getName(), translateFor(node, log), getProperties().toList());
    }

    @Override
    public void buildEnvVars(EnvVars env) {
        String home = getHome();
        if (home == null) {
            return;
        }
        env.put(JFROG_BINARY_PATH, home);
    }

    @Symbol("jfrog")
    @Extension
    public static final class Descriptor extends ToolDescriptor<JfrogInstallation> {

        public Descriptor() {
            setInstallations();
            load();
        }

        @Nonnull
        @Override
        public String getDisplayName() {
            return "JFrog";
        }

        @Override
        public JfrogInstallation newInstance(StaplerRequest req, JSONObject formData) throws FormException {
            return (JfrogInstallation) super.newInstance(req, formData.getJSONObject("jfrog"));
        }

        @Override
        public List<? extends ToolInstaller> getDefaultInstallers() {
            List<ToolInstaller> installersList = new ArrayList<>();
            // The default installation will be from 'releases.jfrog.io'
            installersList.add(new ReleasesInstaller(null));
            return installersList;
        }
    }
}
