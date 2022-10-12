package io.jenkins.plugins.jfrog.artifactoryclient;

import com.fasterxml.jackson.annotation.JsonProperty;


/**
 * @author yahavi
 **/
@SuppressWarnings("unused")
public class AuthenticatedRequest {
    public String getTargetArtifactoryUrl() {
        return targetArtifactoryUrl;
    }

    public String getTargetProxyKey() {
        return targetProxyKey;
    }

    public String getTargetUsername() {
        return targetUsername;
    }

    public String getTargetPassword() {
        return targetPassword;
    }

    public String getTargetToken() {
        return targetToken;
    }

    public void setTargetArtifactoryUrl(String targetArtifactoryUrl) {
        this.targetArtifactoryUrl = targetArtifactoryUrl;
    }

    public void setTargetProxyKey(String targetProxyKey) {
        this.targetProxyKey = targetProxyKey;
    }

    public void setTargetUsername(String targetUsername) {
        this.targetUsername = targetUsername;
    }

    public void setTargetPassword(String targetPassword) {
        this.targetPassword = targetPassword;
    }

    public void setTargetToken(String targetToken) {
        this.targetToken = targetToken;
    }

    @JsonProperty("target_artifactory_url")
    private String targetArtifactoryUrl;
    @JsonProperty("target_proxy_key")
    private String targetProxyKey;
    @JsonProperty("target_username")
    private String targetUsername;
    @JsonProperty("target_password")
    private String targetPassword;
    @JsonProperty("target_token")
    private String targetToken;
}
