package jenkins.plugins.jfrog.artifactoryclient;

import org.apache.http.client.methods.HttpRequestBase;

import java.util.HashMap;
import java.util.Map;

import static jenkins.plugins.jfrog.artifactoryclient.Utils.isBlank;

/**
 * This class represents needed headers of the Artifactory Client.
 *
 * @author yahavi
 **/
@SuppressWarnings("UnusedReturnValue")
public class RequestHeaders {
    public static final String CHECK_BINARY_EXISTENCE_IN_FILESTORE = "X-Check-Binary-Existence-In-Filestore";
    public static final String CHECKSUM_DEPLOY_HEADER_NAME = "X-Checksum-Deploy";
    public static final String LAST_MODIFIED = "X-Artifactory-Last-Modified";
    public static final String MODIFIED_BY = "X-Artifactory-Modified-By";
    public static final String SHA256_HEADER_NAME = "X-Checksum-Sha256";
    public static final String CREATED_BY = "X-Artifactory-Created-By";
    public static final String SHA1_HEADER_NAME = "X-Checksum-Sha1";
    public static final String CREATED = "X-Artifactory-Created";
    public static final String USER_AGENT = "User-Agent";

    private static final String DATA_TRANSFER_USER_AGENT = "data-transfer";

    private Map<String, String> headers = new HashMap<>();

    public RequestHeaders() {
        setUserAgent();
    }

    /**
     * Copy constructor
     *
     * @param other - The other request headers object
     */
    public RequestHeaders(RequestHeaders other) {
        this.headers = new HashMap<>(other.headers);
    }

    public RequestHeaders withCheckBinaryExistenceInFilestore() {
        return addHeader(CHECK_BINARY_EXISTENCE_IN_FILESTORE, "true");
    }

    public RequestHeaders withChecksumDeploy() {
        return addHeader(CHECKSUM_DEPLOY_HEADER_NAME, "true");
    }

    public RequestHeaders withLastModified(long value) {
        return addHeader(LAST_MODIFIED, String.valueOf(value));
    }

    public RequestHeaders withModifiedBy(String value) {
        return addHeader(MODIFIED_BY, value);
    }

    public RequestHeaders withSha256(String value) {
        return addHeader(SHA256_HEADER_NAME, value);
    }

    public RequestHeaders withCreatedBy(String value) {
        return addHeader(CREATED_BY, value);
    }

    public RequestHeaders withSha1(String value) {
        return addHeader(SHA1_HEADER_NAME, value);
    }

    public RequestHeaders withCreated(long value) {
        return addHeader(CREATED, String.valueOf(value));
    }

    public void addHeadersToRequest(HttpRequestBase request) {
        headers.forEach(request::addHeader);
    }

    public String getSha1() {
        return headers.get(SHA1_HEADER_NAME);
    }

    private RequestHeaders addHeader(String key, String value) {
        if (!isBlank(value)) {
            headers.put(key, value);
        }
        return this;
    }

    private void setUserAgent() {
        String userAgent = DATA_TRANSFER_USER_AGENT;
        String implementationVersion = getClass().getPackage().getImplementationVersion();
        if (implementationVersion != null) {
            userAgent += "/" + implementationVersion;
        }
        addHeader(USER_AGENT, userAgent);
    }
}
