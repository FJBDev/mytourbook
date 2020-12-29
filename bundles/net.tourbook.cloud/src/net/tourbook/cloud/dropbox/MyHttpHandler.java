package net.tourbook.cloud.dropbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
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
import net.tourbook.common.UI;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.StringUtils;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.eclipse.core.runtime.Preferences.IPropertyChangeListener;
import org.eclipse.core.runtime.Preferences.PropertyChangeEvent;
import org.eclipse.jface.preference.IPreferenceStore;

public class MyHttpHandler implements HttpHandler, IPropertyChangeListener {

   private static HttpClient   _httpClient        = HttpClient.newBuilder().connectTimeout(Duration.ofMinutes(1)).build();
   private static final String DropboxApiBaseUrl  = "https://api.dropboxapi.com";                                         //$NON-NLS-1$
   public static final String  DropboxCallbackUrl = "http://localhost:8001/dropboxAuthorizationCode";                     //$NON-NLS-1$

   private static String       _codeVerifier;

   private IPreferenceStore    _prefStore         = Activator.getDefault().getPreferenceStore();
   private String              _authorizationCode;

   public MyHttpHandler(final String codeVerifier) {
      _codeVerifier = codeVerifier;
   }

   private static DropboxTokens getTokens(final String authorizationCode,
                                   final boolean isRefreshToken,
                                   final String refreshToken,
                                   final String codeVerifier) {

      final Map<String, String> data = new HashMap<>();
      data.put(IOAuth2Constants.PARAM_REDIRECT_URI, DropboxCallbackUrl);
      data.put(IOAuth2Constants.PARAM_CLIENT_ID, "vye6ci8xzzsuiao");
      data.put("code_verifier", _codeVerifier);

      String grantType;
      if (isRefreshToken) {
         data.put(IOAuth2Constants.PARAM_REFRESH_TOKEN, refreshToken);
         grantType = IOAuth2Constants.PARAM_REFRESH_TOKEN;
      } else {
         data.put(IOAuth2Constants.PARAM_AUTHORIZATION_CODE, authorizationCode);
         grantType = "authorization_code"; //$NON-NLS-1$
      }

      data.put(IOAuth2Constants.PARAM_GRANT_TYPE, grantType);

      final HttpRequest request = HttpRequest.newBuilder()
            .header("Content-Type", "application/x-www-form-urlencoded") //$NON-NLS-1$ //$NON-NLS-2$
            .POST(ofFormData(data))
            .uri(URI.create(DropboxApiBaseUrl + "/oauth2/token"))//$NON-NLS-1$
            .build();

      DropboxTokens token = new DropboxTokens();
      try {
         final HttpResponse<String> response = _httpClient.send(request, HttpResponse.BodyHandlers.ofString());

         if (response.statusCode() == HttpURLConnection.HTTP_OK && StringUtils.hasContent(response.body())) {
            token = new ObjectMapper().readValue(response.body(), DropboxTokens.class);

            return token;
         }
      } catch (IOException | InterruptedException e) {
         StatusUtil.log(e);
      }

      return token;
   }

   private static BodyPublisher ofFormData(final Map<String, String> parameters) {

      final StringBuilder result = new StringBuilder();

      for (final Map.Entry<String, String> parameter : parameters.entrySet()) {

         if (StringUtils.hasContent(result.toString())) {
            result.append(UI.SYMBOL_MNEMONIC);
         }

         final String encodedName = URLEncoder.encode(parameter.getKey(), StandardCharsets.UTF_8);
         final String encodedValue = URLEncoder.encode(parameter.getValue(), StandardCharsets.UTF_8);
         result.append(encodedName);
         if (StringUtils.hasContent(encodedValue)) {
            result.append(UI.SYMBOL_EQUAL);
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

      final OutputStream outputStream = httpExchange.getResponseBody();

      final StringBuilder htmlBuilder = new StringBuilder();

      htmlBuilder.append("<html>").

            append("<body>").

            append("<h1>").

            append("Hello ")

            .append("</h1>")

            .append("</body>")

            .append("</html>");

      // encode HTML content

      final String htmlResponse = StringEscapeUtils.escapeHtml4(htmlBuilder.toString());

      // this line is a must

      httpExchange.sendResponseHeaders(200, htmlResponse.length());

      outputStream.write(htmlResponse.getBytes());

      outputStream.flush();

      outputStream.close();

      //you can close this browser page
//      httpExchange.sendResponseHeaders(200, UI.EMPTY_STRING.length());

   }

   @Override
   public void propertyChange(final PropertyChangeEvent arg0) {
      // TODO Auto-generated method stub

   }

   private void retrieveTokensFromResponse(final URI uri) {

      final char[] separators = { '#', '&', '?' };

      final String response = uri.toString();
      System.out.println(response);

      String authorizationCode = UI.EMPTY_STRING;
      final List<NameValuePair> params = URLEncodedUtils.parse(response, StandardCharsets.UTF_8, separators);
      for (final NameValuePair param : params) {
         if (param.getName().equals(IOAuth2Constants.PARAM_AUTHORIZATION_CODE)) {
            authorizationCode = param.getValue();
            break;
         }
      }

      if (StringUtils.isNullOrEmpty(authorizationCode)) {
         return;
      }

      //get tokens from auth code
      final DropboxTokens newTokens = getTokens(authorizationCode, false, UI.EMPTY_STRING, _codeVerifier);

      if (StringUtils.hasContent(newTokens.getAccess_token())) {
         _prefStore.setValue(IPreferences.DROPBOX_ACCESSTOKEN, newTokens.getAccess_token());
      }
   }
}
