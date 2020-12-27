package net.tourbook.cloud.dropbox;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;

import net.tourbook.cloud.oauth2.IOAuth2Constants;
import net.tourbook.common.UI;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

public class MyHttpHandler implements HttpHandler {

   private String authorizationCode;

   @Override
   public void handle(final HttpExchange httpExchange) throws IOException {

      if ("GET".equals(httpExchange.getRequestMethod())) {

         authorizationCode = handleGetRequest(httpExchange);
      }

      handleResponse(httpExchange);
   }

   private String handleGetRequest(final HttpExchange httpExchange) {

      return retrieveTokenFromResponse(httpExchange.getRequestURI());

   }

   private void handleResponse(final HttpExchange httpExchange) throws IOException {

      httpExchange.sendResponseHeaders(200, UI.EMPTY_STRING.length());

   }

   private String retrieveTokenFromResponse(final URI uri) {

      final char[] separators = { '#', '&' };

      final String response = uri.toString();

      final List<NameValuePair> params = URLEncodedUtils.parse(response, StandardCharsets.UTF_8, separators);
      for (final NameValuePair param : params) {
         if (param.getName().equals(IOAuth2Constants.PARAM_AUTHORIZATION_CODE)) {
            return param.getValue();
         }
      }

      return UI.EMPTY_STRING;
   }

   public String getAuthorizationCode() {
      return authorizationCode;
   }

}
