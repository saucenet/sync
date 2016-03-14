package com.sauce.sync;

import com.google.api.client.extensions.appengine.http.UrlFetchTransport;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpBackOffUnsuccessfulResponseHandler;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.json.JsonHttpContent;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.client.util.GenericData;
import com.google.appengine.api.urlfetch.URLFetchService;
import com.google.appengine.api.urlfetch.URLFetchServiceFactory;
import com.google.appengine.api.users.User;
import com.google.gson.Gson;
import com.sauce.sync.models.GCMMessage;
import com.sauce.sync.json.OAuthTokenInfoResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.logging.Logger;

/**
 * Created by sauce on 5/10/15.
 */
public class Networker {
    private static final String GOOGLE_APIS_HOST = "https://www.googleapis.com";
    private static final String GCM_HOST = "https://gcm-http.googleapis.com/gcm/send";
    private static final Logger log = Logger.getLogger(Networker.class.getSimpleName());
    private static final Gson gson = new Gson();
    private static final GsonFactory gsonFactory = GsonFactory.getDefaultInstance();
    private static final URLFetchService urlService = URLFetchServiceFactory.getURLFetchService();
    private static final UrlFetchTransport transport = UrlFetchTransport.getDefaultInstance();
    private static final HttpRequestFactory requestFactory = transport.createRequestFactory();

    //HTTP_TRANSPORT.createRequestFactory(credential);
    public static OAuthTokenInfoResponse getTokenInfo(User user, String authorizedHeader) throws IOException {
        String token = authorizedHeader.substring(7);
        String tokenType;


        if(user.getUserId() == null) {
            //token is an id token
            tokenType = "id_token";
        } else {
            //token is an access token
            tokenType = "access_token";
        }

        URL url = new URL(GOOGLE_APIS_HOST + "/oauth2/v1/tokeninfo?" + tokenType + "=" + URLEncoder.encode(token, "UTF-8"));
        log.info("auth url:" + url);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setDoOutput(true);
        BufferedReader in = new BufferedReader(
                new InputStreamReader(connection.getInputStream()));
        String inputLine;
        StringBuffer response = new StringBuffer();
        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();
        log.info("response:" + response);
        if(connection.getResponseCode() != 200) {
            throw new IOException("token info response from google returned" + connection.getResponseCode() + " with body: " + response);
        }

        OAuthTokenInfoResponse tokenInfo = gson.fromJson(response.toString(), OAuthTokenInfoResponse.class);
        return tokenInfo;
    }

    public static boolean sendGCMSync(String userId, String senderId) {
        if(Constants.GCM_API_KEY == null) {
            log.info("GCM not set up, see readme for how to configure");
            return false;
        }
        try {
            GCMMessage message = new GCMMessage(userId);
            log.info("message:" + message);
            log.info("gson:" + gson.toJson(message));
            GenericData body = new GenericData();
            GenericData data = new GenericData();
            body.put("to", "/topics/" + userId);
            if(senderId != null) {
                data.put("senderId",senderId);
                body.put("data", data);
            }

            //why does this only take a generic map?
            HttpRequest request = requestFactory.buildPostRequest(new GenericUrl(GCM_HOST),
                    new JsonHttpContent(gsonFactory, body))
                    .setUnsuccessfulResponseHandler(new HttpBackOffUnsuccessfulResponseHandler(new ExponentialBackOff()))
                    .setHeaders(new HttpHeaders().setAuthorization("key=" + Constants.GCM_API_KEY));
            request.getContent().writeTo(System.out);
            HttpResponse response = request.execute();
            log.info("response" + response);
            return true;
        } catch(IOException e) {
            log.info("could not send http message:");
            e.printStackTrace();
            return false;
        }
    }
}
