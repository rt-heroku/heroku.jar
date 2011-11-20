package com.heroku.api.http;

import com.heroku.api.Heroku;
import com.heroku.api.command.CommandConfig;
import com.heroku.api.exception.HerokuAPIException;
import com.heroku.api.exception.RequestFailedException;

import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.security.cert.X509Certificate;

/**
 * TODO: Javadoc
 *
 * @author Naaman Newbold
 */
public class HttpUtil {

    private static String ENCODE_FAIL = "Unsupported encoding exception while encoding parameters";

    public static String encodeParameters(CommandConfig config, Heroku.RequestKey... keys) {

        StringBuilder encodedParameters = new StringBuilder();
        String separator = "";
        for (Heroku.RequestKey key : keys) {
            if (config.get(key) != null) {
                encodedParameters.append(separator);
                encodedParameters.append(urlencode(key.queryParameter, ENCODE_FAIL));
                encodedParameters.append("=");
                encodedParameters.append(urlencode(config.get(key), ENCODE_FAIL));
                separator = "&";
            }
        }
        return new String(encodedParameters);

    }


    public static String urlencode(String toEncode, String messageIfFails) {
        try {
            return URLEncoder.encode(toEncode, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(messageIfFails, e);
        }
    }


    public static UnsupportedOperationException noBody() {
        return new UnsupportedOperationException("This command does not have a body. Use hasBody() to check for a body.");
    }

    public static URL toURL(String url) {
        try {
            return new URL(url);
        } catch (MalformedURLException e) {
            throw new RuntimeException("The URL was malformed");
        }
    }

    public static HerokuAPIException invalidLogin() {
        return new HerokuAPIException("Unable to login");
    }

    public static HerokuAPIException invalidKeys() {
        return new HerokuAPIException("Unable to add keys.");
    }

    public static RequestFailedException insufficientPrivileges(int code, byte[] bytes) {
        return new RequestFailedException("Insufficient privileges.", code, bytes);
    }

    public static TrustManager[] trustAllTrustManager() {
        return new TrustManager[]{new X509TrustManager() {
            @Override
            public void checkClientTrusted(final X509Certificate[] chain, final String authType) {
            }

            @Override
            public void checkServerTrusted(final X509Certificate[] chain, final String authType) {
            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }
        }};
    }




}
