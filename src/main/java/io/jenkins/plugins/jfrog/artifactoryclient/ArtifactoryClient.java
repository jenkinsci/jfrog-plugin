package io.jenkins.plugins.jfrog.artifactoryclient;

import hudson.model.TaskListener;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.TimeUnit;

import static io.jenkins.plugins.jfrog.artifactoryclient.Utils.isBlank;

/**
 * Internal Artifactory Java client for the data-transfer plugin.
 *
 * @author yahavi
 **/
public class ArtifactoryClient implements AutoCloseable {
    public static final String ORIGINAL_HOST_CONTEXT_PARAM = "original.host.context.param";
    static final String PING_ENDPOINT = "api/system/ping";

    private final PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
    public static final int TIMEOUT_MILLIS = (int) TimeUnit.MINUTES.toMillis(5);
    private final HttpClientContext clientContext = HttpClientContext.create();
    private final CloseableHttpClient httpClient;
    private final String artifactoryUrl;
    private final TaskListener logger;

    public ArtifactoryClient(String artifactoryUrl, String username, String password, String accessToken, HttpProxyDetails proxy, TaskListener logger) {
        // this.socketTimeoutMillis = socketTimeoutMillis;
        this.artifactoryUrl = StringUtils.removeEnd(artifactoryUrl, "/");
        this.logger = logger;
        this.httpClient = createHttpClient(proxy);
        CredentialsProvider basicCredentialsProvider = new BasicCredentialsProvider();
        if (isBlank(accessToken)) {
            logger.getLogger().println("Using basic auth");
            basicCredentialsProvider.setCredentials(new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT), new UsernamePasswordCredentials(username, password));
        } else {
            logger.getLogger().println("Using access token");
            this.clientContext.setUserToken(accessToken);
        }
        this.clientContext.setCredentialsProvider(basicCredentialsProvider);
    }

    /**
     * Create the HTTP client.
     *
     * @return HTTP client.
     */
    private CloseableHttpClient createHttpClient(HttpProxyDetails proxy) {
        RequestConfig.Builder defaultRequestConfig = RequestConfig.custom()
                //.setSocketTimeout(socketTimeoutMillis)
                .setConnectTimeout(TIMEOUT_MILLIS)
                .setConnectionRequestTimeout(TIMEOUT_MILLIS);
        if (proxy != null) {
            HttpHost httpHostProxy = new HttpHost(proxy.getHost(), proxy.getPort(), proxy.getPort() == 443 ? "https" : "http");
            defaultRequestConfig.setProxy(httpHostProxy);
        }
        return HttpClientBuilder.create()
                .setConnectionManager(connectionManager)
                .addInterceptorFirst(new PreemptiveAuth())
                .setRedirectStrategy(new PreemptiveRedirectStrategy())
                .setDefaultRequestConfig(defaultRequestConfig.build())
                .build();
    }

    /**
     * Send ping to Artifactory.
     *
     * @return HTTP response.
     * @throws IOException in case of any I/O error.
     */
    public CloseableHttpResponse ping() throws IOException {
        return execute(new HttpGet(PING_ENDPOINT));
    }

    public CloseableHttpResponse download(String s) throws IOException {
        return execute(new HttpGet(s));
    }

    private CloseableHttpResponse execute(HttpRequestBase request) throws IOException {
        String url = request.getURI().toString();
        if (!url.startsWith("/")) {
            url = "/" + url;
        }
        request.setURI(URI.create((artifactoryUrl + url)));
        return httpClient.execute(request, clientContext);
    }

    @Override
    public void close() throws IOException {
        connectionManager.close();
        httpClient.close();
    }
}
