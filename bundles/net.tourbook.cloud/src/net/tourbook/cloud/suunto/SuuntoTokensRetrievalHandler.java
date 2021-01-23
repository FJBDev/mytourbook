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
package net.tourbook.cloud.suunto;

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

public class SuuntoTokensRetrievalHandler extends TokensRetrievalHandler {

   private static HttpClient _httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofMinutes(5)).build();
   private IPreferenceStore  _prefStore  = Activator.getDefault().getPreferenceStore();

   public static SuuntoTokens getTokens(final String authorizationCode, final boolean isRefreshToken, final String refreshToken) {

      final StringBuilder body = new StringBuilder();
      String grantType;
      if (isRefreshToken) {
         body.append("{\"" + OAuth2Constants.PARAM_REFRESH_TOKEN + "\" : \"" + refreshToken); //$NON-NLS-1$ //$NON-NLS-2$
         grantType = OAuth2Constants.PARAM_REFRESH_TOKEN;
      } else {
         body.append("{\"" + OAuth2Constants.PARAM_CODE + "\" : \"" + authorizationCode);//$NON-NLS-1$ //$NON-NLS-2$
         grantType = OAuth2Constants.PARAM_AUTHORIZATION_CODE;
      }

      body.append("\", \"" + OAuth2Constants.PARAM_GRANT_TYPE + "\" : \"" + grantType + "\"}"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

      final HttpRequest request = HttpRequest.newBuilder()
            .header(OAuth2Constants.CONTENT_TYPE, "application/json") //$NON-NLS-1$
            .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
            .uri(URI.create(OAuth2Constants.HEROKU_APP_URL + "/suunto/token"))//$NON-NLS-1$
            .build();

      try {
         final HttpResponse<String> response = _httpClient.send(request, HttpResponse.BodyHandlers.ofString());

         if (response.statusCode() == HttpURLConnection.HTTP_CREATED && StringUtils.hasContent(response.body())) {
            final SuuntoTokens token = new ObjectMapper().readValue(response.body(), SuuntoTokens.class);

            return token;
         }
      } catch (IOException | InterruptedException e) {
         StatusUtil.log(e);
         Thread.currentThread().interrupt();
      }

      return null;
   }

   @Override
   public Tokens retrieveTokens(final String authorizationCode) {
      final Tokens newTokens = new SuuntoTokens();
      if (StringUtils.isNullOrEmpty(authorizationCode)) {
         return newTokens;
      }

      return getTokens(authorizationCode, false, UI.EMPTY_STRING);
   }

   @Override
   public void saveTokensInPreferences(final Tokens tokens) {

      if (!(tokens instanceof SuuntoTokens) || !StringUtils.hasContent(tokens.getAccess_token())) {

         final String currentAccessToken = _prefStore.getString(Preferences.STRAVA_ACCESSTOKEN);
         _prefStore.firePropertyChangeEvent(Preferences.STRAVA_ACCESSTOKEN,
               currentAccessToken,
               currentAccessToken);
         return;
      }

      final SuuntoTokens stravaTokens = (SuuntoTokens) tokens;

      _prefStore.setValue(Preferences.STRAVA_REFRESHTOKEN, stravaTokens.getRefresh_token());
      _prefStore.setValue(Preferences.STRAVA_ACCESSTOKEN_EXPIRES_AT, stravaTokens.getExpires_at());

      //Setting it last so that we trigger the preference change when everything is ready
      _prefStore.setValue(Preferences.STRAVA_ACCESSTOKEN, stravaTokens.getAccess_token());
   }
}
