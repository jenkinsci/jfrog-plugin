package io.jenkins.plugins.jfrog.configuration;

import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import hudson.Extension;
import hudson.model.Descriptor;
import hudson.model.Hudson;
import hudson.model.Item;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.model.GlobalConfiguration;
import jenkins.model.Jenkins;
import io.jenkins.plugins.jfrog.plugins.PluginsUtils;
import net.sf.json.JSONNull;
import net.sf.json.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.interceptor.RequirePOST;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Builder for JFrogPlatformInstance, used by the JFrog CLI config command.
 *
 * @author gail
 */
public class JFrogPlatformBuilder extends GlobalConfiguration {

    /**
     * Descriptor for {@link JFrogPlatformBuilder}. Used as a singleton.
     */
    @Extension
    // this marker indicates Hudson that this is an implementation of an extension point.
    public static final class DescriptorImpl extends Descriptor<GlobalConfiguration> {
        private List<JFrogPlatformInstance> jfrogInstances;

        public DescriptorImpl() {
            super(JFrogPlatformBuilder.class);
            load();
        }

        @SuppressWarnings("unused")
        @RequirePOST
        public ListBoxModel doFillCredentialsIdItems(@AncestorInPath Item project) {
            Jenkins jenkins = Jenkins.getInstanceOrNull();
            if (jenkins != null && jenkins.hasPermission(Jenkins.ADMINISTER)) {
                return PluginsUtils.fillPluginCredentials(project);
            }
            return new StandardListBoxModel();
        }

        /**
         * Performs on-the-fly validation of the form field 'ServerId'.
         *
         * @param value This parameter receives the value that the user has typed.
         * @return Indicates the outcome of the validation. This is sent to the browser.
         */
        @SuppressWarnings("unused")
        public FormValidation doCheckServerId(@QueryParameter String value) {
            Jenkins.get().checkPermission(Jenkins.ADMINISTER);
            if (StringUtils.isBlank(value)) {
                return FormValidation.error("Please set server ID");
            }
            List<JFrogPlatformInstance> JFrogPlatformInstances = getJFrogPlatformInstances();
            if (JFrogPlatformInstances == null) {
                return FormValidation.ok();
            }
            int countServersByValueAsName = 0;
            for (JFrogPlatformInstance JFrogPlatformInstance : JFrogPlatformInstances) {
                if (JFrogPlatformInstance.getId().equals(value)) {
                    countServersByValueAsName++;
                    if (countServersByValueAsName > 1) {
                        return FormValidation.error("Duplicated JFrog platform instances ID");
                    }
                }
            }
            return FormValidation.ok();
        }

        /**
         * Performs on-the-fly validation of the form field 'PlatformUrl'.
         *
         * @param value This parameter receives the value that the user has typed.
         * @return Indicates the outcome of the validation. This is sent to the browser.
         */
        @SuppressWarnings("unused")
        public FormValidation doCheckPlatformUrl(@QueryParameter String value) {
            Jenkins.get().checkPermission(Jenkins.ADMINISTER);
            if (StringUtils.isBlank(value)) {
                return FormValidation.error("Please set platform URL");
            }
            return FormValidation.ok();
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject o) throws FormException {
            Jenkins jenkins = Jenkins.getInstanceOrNull();
            if (jenkins != null && jenkins.hasPermission(Jenkins.ADMINISTER)) {
                configureJFrogInstances(req, o);
                save();
                return super.configure(req, o);
            }
            throw new FormException("User doesn't have permissions to save", "Server ID");
        }


        private void configureJFrogInstances(StaplerRequest req, JSONObject o) throws FormException {
            List<JFrogPlatformInstance> jfrogInstances = new ArrayList<>();
            Object jfrogInstancesObj = o.get("jfrogInstances"); // an array or single object
            if (!JSONNull.getInstance().equals(jfrogInstancesObj)) {
                jfrogInstances = req.bindJSONToList(JFrogPlatformInstance.class, jfrogInstancesObj);
            }

            if (!isJFrogInstancesIDConfigured(jfrogInstances)) {
                throw new FormException("Please set the Instance ID.", "ServerID");
            }

            if (isInstanceDuplicated(jfrogInstances)) {
                throw new FormException("The JFrog server ID you have entered is already configured", "Server ID");
            }

            if (isEmptyUrl(jfrogInstances)) {
                throw new FormException("Please set the The JFrog Platform URL", "URL");
            }
            autoFillPlatformServers(jfrogInstances);
            setJfrogInstances(jfrogInstances);
        }

        public void autoFillPlatformServers(List<JFrogPlatformInstance> newJFrogInstances) {
            if (newJFrogInstances == null) {
                return;
            }
            for (JFrogPlatformInstance newInstance : newJFrogInstances) {
                if (StringUtils.isBlank(newInstance.getUrl())) {
                    continue;
                }
            }
        }

        /**
         * verify instance ID was provided.
         */
        private boolean isJFrogInstancesIDConfigured(List<JFrogPlatformInstance> jfrogInstances) {
            if (jfrogInstances == null) {
                return true;
            }
            for (JFrogPlatformInstance server : jfrogInstances) {
                String platformId = server.getId();
                if (StringUtils.isBlank(platformId)) {
                    return false;
                }
            }
            return true;
        }

        private boolean isInstanceDuplicated(List<JFrogPlatformInstance> jfrogInstances) {
            Set<String> serversNames = new HashSet<>();
            if (jfrogInstances == null) {
                return false;
            }
            for (JFrogPlatformInstance instance : jfrogInstances) {
                String id = instance.getId();
                if (serversNames.contains(id)) {
                    return true;
                }
                serversNames.add(id);
            }
            return false;
        }

        /**
         * verify platform URL was provided.
         */
        private boolean isEmptyUrl(List<JFrogPlatformInstance> jfrogInstances) {
            if (jfrogInstances == null) {
                return false;
            }
            for (JFrogPlatformInstance instance : jfrogInstances) {
                if (StringUtils.isBlank(instance.getUrl())) {
                    return true;
                }
            }
            return false;
        }

        /**
         * Used by Jenkins Jelly for displaying values.
         */
        public List<JFrogPlatformInstance> getJfrogInstances() {
            return jfrogInstances;
        }

        /**
         * Used by Jenkins Jelly for setting values.
         */
        public void setJfrogInstances(List<JFrogPlatformInstance> jfrogInstances) {
            this.jfrogInstances = jfrogInstances;
        }

    }

    /**
     * Returns the list of {@link JFrogPlatformInstance} configured.
     *
     * @return can be empty but never null.
     */
    public static List<JFrogPlatformInstance> getJFrogPlatformInstances() {
        JFrogPlatformBuilder.DescriptorImpl descriptor = (JFrogPlatformBuilder.DescriptorImpl)
                Hudson.get().getDescriptor(JFrogPlatformBuilder.class);
        if (descriptor == null) {
            return new ArrayList<>();
        }
        return descriptor.getJfrogInstances();
    }
}
