package net.tourbook.cloud.dropbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.tourbook.cloud.Activator;
import net.tourbook.cloud.IPreferences;
import net.tourbook.cloud.oauth2.IOAuth2Constants;
import net.tourbook.cloud.strava.Tokens;
import net.tourbook.common.UI;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.StringUtils;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.eclipse.jface.preference.IPreferenceStore;

public class MyHttpHandler implements HttpHandler {

   private static HttpClient  _httpClient       = HttpClient.newBuilder().connectTimeout(Duration.ofMinutes(1)).build();
   private static final String DropboxApiBaseUrl  = "https://api.dropboxapi.com";                                         //$NON-NLS-1$
   public static final String DropboxCallbackUrl = "http://localhost:8001/dropboxAuthorizationCode";                     //$NON-NLS-1$

   private static String      _codeVerifier;

   private IPreferenceStore   _prefStore        = Activator.getDefault().getPreferenceStore();
   private String             _authorizationCode;

   public static Tokens getTokens(final String authorizationCode,
                                  final boolean isRefreshToken,
                                  final String refreshToken,
                                  final String codeVerifier) {

      _codeVerifier = codeVerifier;

      final Map<String, String> data = new HashMap<>();
      data.put("redirect_uri", DropboxCallbackUrl);
      data.put("client_id", "vye6ci8xzzsuiao");
      data.put("code_verifier", _codeVerifier);

      String grantType;
      if (isRefreshToken) {
         data.put("refresh_token", refreshToken);
         grantType = "refresh_token"; //$NON-NLS-1$
      } else {
         data.put("code", authorizationCode);
         grantType = "authorization_code"; //$NON-NLS-1$
      }

      data.put("grant_type", grantType);

      final HttpRequest request = HttpRequest.newBuilder()
            .header("Content-Type", "application/x-www-form-urlencoded") //$NON-NLS-1$ //$NON-NLS-2$
            .POST(ofFormData(data))
            .uri(URI.create(DropboxApiBaseUrl + "/oauth2/token"))//$NON-NLS-1$
            .build();

      try {
         final HttpResponse<String> response = _httpClient.send(request, HttpResponse.BodyHandlers.ofString());

         if (response.statusCode() == HttpURLConnection.HTTP_OK && StringUtils.hasContent(response.body())) {
            final Tokens token = new ObjectMapper().readValue(response.body(), Tokens.class);

            return token;
         }
      } catch (IOException | InterruptedException e) {
         StatusUtil.log(e);
      }

      return null;
   }

   private static BodyPublisher ofFormData(final Map<String, String> parameters) {

      final StringBuilder result = new StringBuilder();

      for (final Map.Entry<String, String> parameter : parameters.entrySet()) {

         if (StringUtils.hasContent(result.toString())) {
            result.append("&");
         }

         final String encodedName = URLEncoder.encode(parameter.getKey(), StandardCharsets.UTF_8);
         final String encodedValue = URLEncoder.encode(parameter.getValue(), StandardCharsets.UTF_8);
         result.append(encodedName);
         if (StringUtils.hasContent(encodedValue)) {
            result.append("=");
            result.append(encodedValue);
         }
      }
      return HttpRequest.BodyPublishers.ofString(result.toString());
   }

   public String getAuthorizationCode() {
      return _authorizationCode;
   }

   @Override
   public void handle(final HttpExchange httpExchange) throws IOException {

      if ("GET".equals(httpExchange.getRequestMethod())) {

         handleGetRequest(httpExchange);
         //TODO return boolean ?
      }

      handleResponse(httpExchange);
   }

   private void handleGetRequest(final HttpExchange httpExchange) {

      retrieveTokensFromResponse(httpExchange.getRequestURI());
   }

   private void handleResponse(final HttpExchange httpExchange) throws IOException {

      //you can close this browser page
      httpExchange.sendResponseHeaders(200, UI.EMPTY_STRING.length());

   }

   private void retrieveTokensFromResponse(final URI uri) {

      final char[] separators = { '#', '&', '?' };

      final String response = uri.toString();

      String authorizationCode = UI.EMPTY_STRING;
      final List<NameValuePair> params = URLEncodedUtils.parse(response, StandardCharsets.UTF_8, separators);
      for (final NameValuePair param : params) {
         if (param.getName().equals(IOAuth2Constants.PARAM_AUTHORIZATION_CODE)) {
            authorizationCode = param.getValue();
         }
      }

      if (StringUtils.isNullOrEmpty(authorizationCode)) {
         return;
      }

      //get tokens from auth code
      final Tokens newTokens = getTokens(authorizationCode, false, UI.EMPTY_STRING, _codeVerifier);

      _prefStore.setValue(IPreferences.DROPBOX_ACCESSTOKEN, newTokens.getAccess_token());
   }
}
