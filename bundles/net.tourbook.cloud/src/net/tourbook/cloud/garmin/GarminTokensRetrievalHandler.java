/*******************************************************************************
 * Copyright (C) 2021 Frédéric Bard
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110, USA
 *******************************************************************************/
package net.tourbook.cloud.garmin;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import net.tourbook.cloud.Activator;
import net.tourbook.cloud.Preferences;
import net.tourbook.cloud.oauth2.OAuth2Constants;
import net.tourbook.cloud.oauth2.OAuth2Utils;
import net.tourbook.cloud.oauth2.Tokens;
import net.tourbook.cloud.oauth2.TokensRetrievalHandler;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.StringUtils;

import org.eclipse.jface.preference.IPreferenceStore;
import org.json.JSONObject;

public class GarminTokensRetrievalHandler extends TokensRetrievalHandler {

   private static HttpClient       _httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofMinutes(5)).build();

   private static IPreferenceStore _prefStore  = Activator.getDefault().getPreferenceStore();

   private String                  _oauthTokenSecret;

   protected GarminTokensRetrievalHandler(final String oauthTokenSecret) {
      _oauthTokenSecret = oauthTokenSecret;
   }

   public GarminTokens getTokens(final String oauthToken,
                                 final String oauthVerifier) {

      final JSONObject body = new JSONObject();
      body.put("oauth_token", oauthToken);
      body.put("oauth_verifier", oauthVerifier);
      body.put("oauth_token_secret", _oauthTokenSecret);

      final HttpRequest request = HttpRequest.newBuilder()
            .header(OAuth2Constants.CONTENT_TYPE, "application/json") //$NON-NLS-1$
            .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
            .uri(OAuth2Utils.createOAuthPasseurUri("/garmin/access_token"))//$NON-NLS-1$
            .build();

      try {
         final HttpResponse<String> response = _httpClient.send(request, HttpResponse.BodyHandlers.ofString());

         if (response.statusCode() == HttpURLConnection.HTTP_OK && StringUtils.hasContent(response.body())) {
            final GarminTokens token = new ObjectMapper().readValue(response.body(), GarminTokens.class);

            return token;
         }
      } catch (IOException | InterruptedException e) {
         StatusUtil.log(e);
         Thread.currentThread().interrupt();
      }

      return null;
   }

//   @Override
//   public Tokens handleGetRequest(final HttpExchange httpExchange) {
//
//      final char[] separators = { '#', '&', '?' };
//
//      final String response = httpExchange.getRequestURI().toString();
//
//      final List<NameValuePair> params = URLEncodedUtils.parse(response, StandardCharsets.UTF_8, separators);
//      final Optional<NameValuePair> optionalOauthToken = params
//            .stream()
//            .filter(param -> param.getName().equals("oauth_token")).findAny();
//
//      final Optional<NameValuePair> optionalOauthVerifier = params
//            .stream()
//            .filter(param -> param.getName().equals("oauth_verifier")).findAny();
//
//      String oauthToken = UI.EMPTY_STRING;
//      String oauthVerifier = UI.EMPTY_STRING;
//      if (optionalOauthToken.isPresent()) {
//         oauthToken = optionalOauthToken.get().getValue();
//      }
//      if (optionalOauthVerifier.isPresent()) {
//         oauthVerifier = optionalOauthVerifier.get().getValue();
//      }
//
//      return getTokens(oauthToken, oauthVerifier);
//   }

   public Tokens retrieveTokens() {
      return null;
   }

   @Override
   public Tokens retrieveTokens(final String authorizationCode) {
      return null;
   }

   @Override
   public void saveTokensInPreferences(final Tokens tokens) {

      if (!(tokens instanceof GarminTokens) || StringUtils.isNullOrEmpty(((GarminTokens) tokens).oauthAccessToken)) {

         final String currentAccessToken = _prefStore.getString(Preferences.GARMIN_ACCESSTOKEN);
         _prefStore.firePropertyChangeEvent(Preferences.GARMIN_ACCESSTOKEN,
               currentAccessToken,
               currentAccessToken);
         return;
      }

      final GarminTokens garminTokens = (GarminTokens) tokens;

      _prefStore.setValue(Preferences.GARMIN_ACCESSTOKEN_SECRET, garminTokens.oauthAccessTokenSecret);

      //Setting it last so that we trigger the preference change when everything is ready
      _prefStore.setValue(Preferences.GARMIN_ACCESSTOKEN, garminTokens.oauthAccessToken);
   }
}
