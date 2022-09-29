package jenkins.plugins.jfrog.configuration;

import org.apache.commons.lang3.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.Serializable;
import java.util.List;

import static jenkins.plugins.jfrog.configuration.JfrogPlatformBuilder.getJFrogPlatformInstances;

/**
 * Represents an instance of jenkins JFrog instance configuration page.
 */
public class JFrogPlatformInstance implements Serializable {
    private String url;
    private String artifactoryUrl;
    private String distributionUrl;
    private String xrayUrl;
    private String id;
    private CredentialsConfig credentialsConfig;

    public CredentialsConfig getCredentialsConfig() {
        return credentialsConfig;
    }

    @DataBoundConstructor
    public JFrogPlatformInstance(String serverId, String platformUrl, CredentialsConfig credentialsConfig, String artifactoryUrl, String distributionUrl, String xrayUrl) {
        this.id = serverId;
        this.url = StringUtils.isNotEmpty(platformUrl) ? StringUtils.removeEnd(platformUrl, "/") : null;
        this.credentialsConfig = credentialsConfig;
        this.artifactoryUrl = addUrlSuffix(artifactoryUrl, this.url, "artifactory");
        this.distributionUrl = addUrlSuffix(distributionUrl, this.url, "distribution");
        this.xrayUrl = addUrlSuffix(xrayUrl, this.url, "xray");
    }

    /**
     * Returns the list of {@link JFrogPlatformInstance} configured.
     * Used by Jenkins Jelly for displaying values.
     *
     * @return can be empty but never null.
     */
    public List<JFrogPlatformInstance> getJfrogInstances() {
        return getJFrogPlatformInstances();
    }

    private String addUrlSuffix(String Url, String platformUrl, String suffix) {
        return StringUtils.isNotEmpty(Url) ? Url : platformUrl + "/" + suffix;
    }

    public String getId() {
        return id;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    // Required by external plugins (JCasC).
    @SuppressWarnings("unused")
    public String getServerId() {
        return getId();
    }

    // Required by external plugins (JCasC).
    @SuppressWarnings("unused")
    public void setServerId(String serverId) {
        this.id = serverId;
    }

    // Required by external plugins (JCasC).
    @SuppressWarnings("unused")
    public String getPlatformUrl() {
        return getUrl();
    }

    // Required by external plugins (JCasC).
    @SuppressWarnings("unused")
    public void setPlatformUrl(String url) {
        setUrl(url);
    }

    public String getArtifactoryUrl() {
        return artifactoryUrl;
    }

    public void setArtifactoryUrl(String artifactoryUrl) {
        this.artifactoryUrl = artifactoryUrl;
    }

    public String getDistributionUrl() {
        return distributionUrl;
    }

    public void setDistributionUrl(String distributionUrl) {
        this.distributionUrl = distributionUrl;
    }

    public String getXrayUrl() {
        return xrayUrl;
    }

    public void setXrayUrl(String xrayUrl) {
        this.xrayUrl = xrayUrl;
    }

}
