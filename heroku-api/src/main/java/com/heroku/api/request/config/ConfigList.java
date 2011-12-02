package com.heroku.api.request.config;

import com.heroku.api.Heroku;
import com.heroku.api.exception.RequestFailedException;
import com.heroku.api.http.Http;
import com.heroku.api.http.HttpUtil;
import com.heroku.api.request.Request;
import com.heroku.api.request.RequestConfig;

import java.util.HashMap;
import java.util.Map;

import static com.heroku.api.parser.Json.parse;

/**
 * TODO: Javadoc
 *
 * @author Naaman Newbold
 */
public class ConfigList implements Request<Map<String, String>> {

    private final RequestConfig config;
    
    public ConfigList(String appName) {
        config = new RequestConfig().app(appName);
    }
    
    @Override
    public Http.Method getHttpMethod() {
        return Http.Method.GET;
    }

    @Override
    public String getEndpoint() {
        return Heroku.Resource.ConfigVars.format(config.get(Heroku.RequestKey.AppName));
    }

    @Override
    public boolean hasBody() {
        return false;
    }

    @Override
    public String getBody() {
        throw HttpUtil.noBody();
    }

    @Override
    public Http.Accept getResponseType() {
        return Http.Accept.JSON;
    }

    @Override
    public Map<String, String> getHeaders() {
        return new HashMap<String, String>();
    }

    @Override
    public Map<String, String> getResponse(byte[] bytes, int status) {
        if (status == Http.Status.OK.statusCode) {
            return parse(bytes, getClass());
        } else if (status == Http.Status.NOT_FOUND.statusCode) {
            throw new RequestFailedException("Application not found.", status, bytes);
        } else if (status == Http.Status.FORBIDDEN.statusCode) {
            throw new RequestFailedException(
                    "Insufficient privileges to \"" + config.get(Heroku.RequestKey.AppName) + "\"",
                    status,
                    bytes
            );
        } else {
            throw new RequestFailedException("Unable to list config failed.", status, bytes);
        }
    }
}
