package jenkins.plugins.jfrog.artifactoryclient;

public interface PropertyFilter {
    /**
     * Return true if the property should be skipped from deploying to the target Artifactory.
     *
     * @param propertyKey - The key to check
     * @return true if the property should be skipped from deploying to the target Artifactory.
     */
    boolean shouldExcludeProperty(String propertyKey);
}
