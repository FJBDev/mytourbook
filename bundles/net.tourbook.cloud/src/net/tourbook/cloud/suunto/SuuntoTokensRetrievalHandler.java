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

import net.tourbook.cloud.Activator;
import net.tourbook.cloud.Preferences;
import net.tourbook.cloud.oauth2.Tokens;
import net.tourbook.cloud.oauth2.TokensRetrievalHandler;
import net.tourbook.common.util.StringUtils;

import org.eclipse.jface.preference.IPreferenceStore;

public class SuuntoTokensRetrievalHandler extends TokensRetrievalHandler {

   private IPreferenceStore _prefStore = Activator.getDefault().getPreferenceStore();

   @Override
   public Tokens retrieveTokens(final String authorizationCode) {
      final Tokens newTokens = new SuuntoTokens();
      if (StringUtils.isNullOrEmpty(authorizationCode)) {
         return newTokens;
      }

      return newTokens;
//      return StravaUploader.getTokens(authorizationCode, false, UI.EMPTY_STRING);
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
