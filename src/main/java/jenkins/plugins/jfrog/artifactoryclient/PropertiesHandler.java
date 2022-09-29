package jenkins.plugins.jfrog.artifactoryclient;

import org.slf4j.Logger;

import java.io.UnsupportedEncodingException;
import java.util.*;

import static jenkins.plugins.jfrog.artifactoryclient.Utils.encodeUrl;


/**
 * This class gets properties and filters and creates the properties URL part.
 * We filter out the following properties:
 * 1. Properties with value > 2400 characters - In Postgress SQL, the maximum value allowed is 2400 characters long.
 * 2. Properties that will be natively generated in the target Artifactory during deployment or index recalculation.
 *
 * @author yahavi
 **/
public class PropertiesHandler {
    private static final int MAX_PROPERTY_SIZE = 2400;

    private Set<Map.Entry<String, String>> properties;
    private String matrixParamsString = "";
    private PropertyFilter propertyFilter;
    private boolean longProperties;
    private Logger logger;

    public PropertiesHandler() {
    }

    public PropertiesHandler(Set<Map.Entry<String, String>> properties, PropertyFilter propertyFilter, Logger logger) throws UnsupportedEncodingException {
        this.propertyFilter = propertyFilter;
        this.properties = properties;
        this.logger = logger;
        buildMatrixParamsString();
    }

    public String getMatrixParamsString() {
        return matrixParamsString;
    }

    public boolean isLargeProperties() {
        return longProperties;
    }

    /**
     * Build the parameters URL part
     *
     * @throws UnsupportedEncodingException in any unexpected encoding error during the creation of the properties' handler.
     */
    private void buildMatrixParamsString() throws UnsupportedEncodingException {
        if (properties == null || properties.isEmpty()) {
            matrixParamsString = "";
            return;
        }
        StringBuilder params = new StringBuilder();
        for (Map.Entry<String, String> entry : properties) {
            if (propertyFilter.shouldExcludeProperty(entry.getKey())) {
                logger.debug("Skipping unnecessary property " + entry.getKey());
                continue;
            }
            if (entry.getValue().length() <= MAX_PROPERTY_SIZE) {
                params.append(";").append(encodeUrl((entry.getKey()))).append("=").append(encodeUrl(entry.getValue()));
            } else {
                logger.warn("Property value of " + entry.getKey() + " is longer than " + MAX_PROPERTY_SIZE + ". Skipping property.");
                longProperties = true;
            }
        }
        matrixParamsString = params.toString();
    }
}
