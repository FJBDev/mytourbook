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
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import net.tourbook.cloud.Activator;
import net.tourbook.cloud.Preferences;
import net.tourbook.cloud.oauth2.OAuth2Constants;
import net.tourbook.cloud.oauth2.Tokens;
import net.tourbook.cloud.oauth2.TokensRetrievalHandler;
import net.tourbook.common.UI;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.StringUtils;

import org.eclipse.jface.preference.IPreferenceStore;
import org.json.JSONObject;

public class GarminTokensRetrievalHandler extends TokensRetrievalHandler {

   private static HttpClient       _httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofMinutes(5)).build();

   private static IPreferenceStore _prefStore  = Activator.getDefault().getPreferenceStore();

   protected GarminTokensRetrievalHandler() {}

   public static GarminTokens getTokens(final String authorizationCode, final boolean isRefreshToken, final String refreshToken) {

//      {
//         "oauth_token":"-53---",
//         "oauth_token_secret":"",
//         "oauth_verifier":""
//     }
      final JSONObject body = new JSONObject();
      String grantType;
      if (isRefreshToken) {
         body.put(OAuth2Constants.PARAM_REFRESH_TOKEN, refreshToken);
         grantType = OAuth2Constants.PARAM_REFRESH_TOKEN;
      } else {
         body.put(OAuth2Constants.PARAM_CODE, authorizationCode);
         grantType = OAuth2Constants.PARAM_AUTHORIZATION_CODE;
      }

      body.put(OAuth2Constants.PARAM_GRANT_TYPE, grantType);
      final HttpRequest request = HttpRequest.newBuilder()
            .header(OAuth2Constants.CONTENT_TYPE, "application/json") //$NON-NLS-1$
            .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
            .uri(URI.create(OAuth2Constants.HEROKU_APP_URL + "/garmin/access_token"))//$NON-NLS-1$
            .build();

      try {
         final HttpResponse<String> response = _httpClient.send(request, HttpResponse.BodyHandlers.ofString());

         if (response.statusCode() == HttpURLConnection.HTTP_CREATED && StringUtils.hasContent(response.body())) {
            final GarminTokens token = new ObjectMapper().readValue(response.body(), GarminTokens.class);

            return token;
         }
      } catch (IOException | InterruptedException e) {
         StatusUtil.log(e);
         Thread.currentThread().interrupt();
      }

      return null;
   }

   public static boolean getValidTokens() {

      final GarminTokens newTokens = getTokens(UI.EMPTY_STRING, true, _prefStore.getString(Preferences.SUUNTO_REFRESHTOKEN));

      boolean isTokenValid = false;
      if (newTokens != null) {
         _prefStore.setValue(Preferences.GARMIN_ACCESSTOKEN_SECRET, newTokens.oauthAccessToken);
         _prefStore.setValue(Preferences.GARMIN_ACCESSTOKEN, newTokens.oauthAccessToken);
         isTokenValid = true;
      }

      return isTokenValid;
   }

   @Override
   public Tokens retrieveTokens(final String authorizationCode) {

      if (StringUtils.isNullOrEmpty(authorizationCode)) {
         return new GarminTokens();
      }

      return getTokens(authorizationCode, false, UI.EMPTY_STRING);
   }

   @Override
   public void saveTokensInPreferences(final Tokens tokens) {

      if (!(tokens instanceof GarminTokens) || StringUtils.isNullOrEmpty(tokens.getAccess_token())) {

         final String currentAccessToken = _prefStore.getString(Preferences.GARMIN_ACCESSTOKEN);
         _prefStore.firePropertyChangeEvent(Preferences.GARMIN_ACCESSTOKEN,
               currentAccessToken,
               currentAccessToken);
         return;
      }

      final GarminTokens garminTokens = (GarminTokens) tokens;

      _prefStore.setValue(Preferences.GARMIN_ACCESSTOKEN_SECRET, garminTokens.getRefresh_token());

      //Setting it last so that we trigger the preference change when everything is ready
      _prefStore.setValue(Preferences.GARMIN_ACCESSTOKEN, garminTokens.getAccess_token());
   }
}
