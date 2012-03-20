package com.heroku.api.connection;

import com.heroku.api.Heroku;
import com.heroku.api.http.Http;
import com.heroku.api.http.HttpUtil;
import com.heroku.api.request.Request;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.*;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.params.CookiePolicy;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;

import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.*;

import static com.heroku.api.Heroku.Config.ENDPOINT;

public class HttpClientConnection implements AsyncConnection<Future<?>> {


    private URL endpoint = HttpUtil.toURL(ENDPOINT.value);
    private DefaultHttpClient httpClient = getHttpClient();
    private volatile ExecutorService executorService;
    private Object lock = new Object();

    public HttpClientConnection() {
    }


    @Override
    public <T> Future<T> executeAsync(final Request<T> request, final String apiKey) {

        Callable<T> callable = new Callable<T>() {
            @Override
            public T call() throws Exception {
                return execute(request, apiKey);
            }
        };
        return getExecutorService().submit(callable);

    }

    @Override
    public <T> T execute(Request<T> request, String key) {
        try {
            HttpRequestBase message = getHttpRequestBase(request.getHttpMethod(), ENDPOINT.value + request.getEndpoint());
            message.setHeader(Heroku.ApiVersion.HEADER, String.valueOf(Heroku.ApiVersion.v2.version));
            message.setHeader(request.getResponseType().getHeaderName(), request.getResponseType().getHeaderValue());
            message.setHeader(Http.UserAgent.LATEST.getHeaderName(), Http.UserAgent.LATEST.getHeaderValue("httpclient"));

            for (Map.Entry<String, String> header : request.getHeaders().entrySet()) {
                message.setHeader(header.getKey(), header.getValue());
            }

            if (request.hasBody()) {
                ((HttpEntityEnclosingRequestBase) message).setEntity(new StringEntity(request.getBody()));
            }

            HttpContext ctx = new BasicHttpContext();
            if (key != null) {
                CredentialsProvider p = new BasicCredentialsProvider();
                p.setCredentials(new AuthScope(endpoint.getHost(), endpoint.getPort()), new UsernamePasswordCredentials("", key));
                ctx.setAttribute(ClientContext.CREDS_PROVIDER, p);
            }
            HttpResponse httpResponse = httpClient.execute(message, ctx);

            return request.getResponse(HttpUtil.getBytes(httpResponse.getEntity().getContent()), httpResponse.getStatusLine().getStatusCode());
        } catch (
                IOException e
                )

        {
            throw new RuntimeException("exception while executing request", e);
        }
    }

    private HttpRequestBase getHttpRequestBase(Http.Method httpMethod, String endpoint) {
        switch (httpMethod) {
            case GET:
                return new HttpGet(endpoint);
            case PUT:
                return new HttpPut(endpoint);
            case POST:
                return new HttpPost(endpoint);
            case DELETE:
                return new HttpDelete(endpoint);
            default:
                throw new UnsupportedOperationException(httpMethod + " is not a supported request type.");
        }
    }

    private ExecutorService getExecutorService() {
        if (executorService == null) {
            synchronized (lock) {
                if (executorService == null) {
                    executorService = createExecutorService();
                }
            }
        }
        return executorService;
    }

    protected ExecutorService createExecutorService() {
        return Executors.newCachedThreadPool(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable runnable) {
                Thread t = new Thread(runnable);
                t.setDaemon(true);
                return t;
            }
        });
    }

    protected DefaultHttpClient getHttpClient() {
        SSLSocketFactory ssf = new SSLSocketFactory(Heroku.herokuSSLContext());
        ThreadSafeClientConnManager ccm = new ThreadSafeClientConnManager();
        if (!Heroku.Config.ENDPOINT.isDefault()) {
            ssf.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
            SchemeRegistry sr = ccm.getSchemeRegistry();
            sr.register(new Scheme("https", ssf, 443));
        }
        DefaultHttpClient defaultHttpClient = new DefaultHttpClient(ccm);
        defaultHttpClient.getParams().setParameter(ClientPNames.COOKIE_POLICY, CookiePolicy.IGNORE_COOKIES);
        return defaultHttpClient;
    }


    @Override
    public void close() {
        getExecutorService().shutdownNow();
    }


    public static class Provider implements ConnectionProvider {

        @Override
        public Connection getConnection() {
            return new HttpClientConnection();
        }
    }

}
