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
package net.tourbook.cloud.oauth2;

<<<<<<< HEAD
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
=======
import net.tourbook.common.UI;
import net.tourbook.common.time.TimeTools;
>>>>>>> refs/remotes/origin/main

<<<<<<< HEAD
import net.tourbook.common.UI;
import net.tourbook.common.time.TimeTools;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;

/**
 * OAuth2 utilities.
 */
=======
>>>>>>> refs/remotes/origin/main
public class OAuth2Utils {

<<<<<<< HEAD
   public static String constructLocalExpireAtDateTime(final long expireAt) {
      if (expireAt == 0) {
         return UI.EMPTY_STRING;
      }

      return Instant.ofEpochMilli(expireAt).atZone(TimeTools.UTC).format(DateTimeFormatter.ISO_DATE_TIME);
=======
   public static String computeAccessTokenExpirationDate(final long accessTokenIssueDateTime,
                                                         final long accessTokenExpiresIn) {

      final long expireAt = accessTokenIssueDateTime + accessTokenExpiresIn;

      return (expireAt == 0) ? UI.EMPTY_STRING : TimeTools.getUTCISODateTime(expireAt);
>>>>>>> refs/remotes/origin/main
   }

   /**
    * We consider that an access token is expired if there are less
    * than 5 mins remaining until the actual expiration
    *
    * @return
    */
   public static boolean isAccessTokenExpired(final long tokenExpirationDate) {

      return tokenExpirationDate - System.currentTimeMillis() - 300000 < 0;
   }

   /**
    * We consider that an access token is expired if there are less
    * than 5 mins remaining until the actual expiration
    *
    * @return
    */
   public static boolean isAccessTokenExpired(final long tokenExpirationDate) {

      return tokenExpirationDate - System.currentTimeMillis() - 300000 < 0;
   }
}
