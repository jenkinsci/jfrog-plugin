package jenkins.plugins.jfrog.artifactoryclient;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.*;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.AuthState;
import org.apache.http.auth.Credentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.ServiceUnavailableRetryStrategy;
import org.apache.http.client.methods.*;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.DefaultRedirectStrategy;
import org.apache.http.protocol.HttpContext;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static jenkins.plugins.jfrog.artifactoryclient.ArtifactoryClient.ORIGINAL_HOST_CONTEXT_PARAM;


/**
 * @author yahavi
 **/
public class PreemptiveAuth implements HttpRequestInterceptor {

    @Override
    public void process(final HttpRequest request, final HttpContext context) throws HttpException, IOException {
        if (!shouldSetAuthScheme(request, context)) {
            return;
        }

        HttpClientContext finalContext = (HttpClientContext) context;
        AuthState authState = finalContext.getTargetAuthState();
        // If no auth scheme available yet, try to initialize it preemptively
        if (authState.getAuthScheme() == null) {
            String accessToken = finalContext.getUserToken(String.class);
            if (StringUtils.isNotEmpty(accessToken)) {
                request.addHeader("Authorization", "Bearer " + accessToken);
            } else {
                CredentialsProvider credsProvider = finalContext.getCredentialsProvider();
                HttpHost targetHost = finalContext.getTargetHost();
                Credentials creds = credsProvider.getCredentials(
                        new AuthScope(targetHost.getHostName(), targetHost.getPort()));
                if (creds == null) {
                    throw new HttpException("No credentials for preemptive authentication");
                }
                BasicScheme authScheme = new BasicScheme();
                authState.update(authScheme, creds);
            }
        }
    }

    /**
     * Used to determine whether preemptive authentication should be performed.
     * In the case of a redirect to a different host, preemptive authentication should not be performed.
     */
    private boolean shouldSetAuthScheme(final HttpRequest request, final HttpContext context) throws IOException {
        // Get the original host name (before the redirect).
        String originalHost = (String) context.getAttribute(ORIGINAL_HOST_CONTEXT_PARAM);
        if (originalHost == null) {

            // No redirect was performed.
            return true;
        }
        try {
            // In case of a redirect, get the new target host.
            String host = new URI(((HttpRequestWrapper) request).getOriginal().getRequestLine().getUri()).getHost();
            // Return true if the original host and the target host are identical.
            return host.equals(originalHost);
        } catch (URISyntaxException e) {
            throw new IOException(e);
        }
    }
}
//
///**
// * Class to handle retries when 5xx errors occurs.
// */
//
//private class PreemptiveRetryStrategy implements ServiceUnavailableRetryStrategy {
//
//    @Override
//    public boolean retryRequest(HttpResponse response, int executionCount, HttpContext context) {
//        // Code 500 means an unexpected behavior of Artifactory, thus we should not retry.
//        if (response.getStatusLine().getStatusCode() > 500) {
//            HttpClientContext clientContext = HttpClientContext.adapt(context);
////            log.warn("Error occurred for request " + clientContext.getRequest().getRequestLine().toString() +
////                    ". Received status code " + response.getStatusLine().getStatusCode() +
////                    " and message: " + response.getStatusLine().getReasonPhrase() + ".");
////            if (executionCount <= connectionRetries) {
////                log.warn("Attempting retry #" + executionCount);
////                return true;
////            }
//        }
//
//        return false;
//    }
//
//    @Override
//    public long getRetryInterval() {
//        return 0;
//    }
//}

///**
// * Class to handle retries when exception occurs.
// */
//
//private class PreemptiveRetryHandler extends DefaultHttpRequestRetryHandler {
//
//    PreemptiveRetryHandler(int connectionRetries) {
//        super(connectionRetries, REQUEST_SENT_RETRY_ENABLED, getNonRetriableClasses());
//    }
//
//    @Override
//    public boolean retryRequest(IOException exception, int executionCount, HttpContext context) {
//        HttpClientContext clientContext = HttpClientContext.adapt(context);
//        log.warn("Error occurred for request " + clientContext.getRequest().getRequestLine().toString() + ": " + exception.getMessage() + ".");
//        if (executionCount > connectionRetries) {
//            return false;
//        }
//        boolean shouldRetry = super.retryRequest(exception, executionCount, context);
//        if (shouldRetry) {
//            log.warn("Attempting retry #" + executionCount);
//            return true;
//        }
//
//        return false;
//    }
//}

/**
 * Class for performing redirection for the following status codes:
 * SC_MOVED_PERMANENTLY (301)
 * SC_MOVED_TEMPORARILY (302)
 * SC_SEE_OTHER (303)
 * SC_TEMPORARY_REDIRECT (307)
 */

class PreemptiveRedirectStrategy extends DefaultRedirectStrategy {

    private final Set<String> redirectableMethods = newHashSet(
            HttpGet.METHOD_NAME.toLowerCase(),
            HttpPost.METHOD_NAME.toLowerCase(),
            HttpHead.METHOD_NAME.toLowerCase(),
            HttpDelete.METHOD_NAME.toLowerCase(),
            HttpPut.METHOD_NAME.toLowerCase());

    @Override
    public HttpUriRequest getRedirect(HttpRequest request, HttpResponse response, HttpContext context) throws ProtocolException {
        // Get the original host name (before the redirect) and save it on the context.
        String originalHost = getHost(request);
        context.setAttribute(ORIGINAL_HOST_CONTEXT_PARAM, originalHost);
        URI uri = getLocationURI(request, response, context);
       // log.debug("Redirecting to " + uri);
        return RequestBuilder.copy(request).setUri(uri).build();
    }
    /**
     * Returns a new HashSet of the provided elements.
     */
    public static <E> HashSet<E> newHashSet(E... elements) {
        return new HashSet<>(Arrays.asList(elements));
    }
    private String getHost(HttpRequest request) {
        URI uri;
        try {
            uri = new URI(request.getRequestLine().getUri());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        return uri.getHost();
    }

    @Override
    protected boolean isRedirectable(String method) {
        String message = "The method " + method;
        if (redirectableMethods.contains(method.toLowerCase())) {
            //log.debug(message + " can be redirected.");
            return true;
        }
        //log.error(message + " cannot be redirected.");
        return false;
    }
}
