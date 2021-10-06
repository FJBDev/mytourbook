/*******************************************************************************
 * Copyright (C) 2020, 2021 Frédéric Bard
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
package net.tourbook.cloud;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.util.StringUtils;
import net.tourbook.data.TourPerson;

public final class Preferences {

   /*
    * Dropbox preferences
    */
   public static final String DROPBOX_ACCESSTOKEN                = "DROPBOX_ACCESSTOKEN";                //$NON-NLS-1$
   public static final String DROPBOX_REFRESHTOKEN               = "DROPBOX_REFRESHTOKEN";               //$NON-NLS-1$
   public static final String DROPBOX_ACCESSTOKEN_EXPIRES_IN     = "DROPBOX_ACCESSTOKEN_EXPIRES_IN";     //$NON-NLS-1$
   public static final String DROPBOX_ACCESSTOKEN_ISSUE_DATETIME = "DROPBOX_ACCESSTOKEN_ISSUE_DATETIME"; //$NON-NLS-1$

   /*
    * Strava preferences
    */
   public static final String STRAVA_ACCESSTOKEN            = "STRAVA_ACCESSTOKEN";            //$NON-NLS-1$
   public static final String STRAVA_REFRESHTOKEN           = "STRAVA_REFRESHTOKEN";           //$NON-NLS-1$
   public static final String STRAVA_ACCESSTOKEN_EXPIRES_AT = "STRAVA_ACCESSTOKEN_EXPIRES_AT"; //$NON-NLS-1$
   public static final String STRAVA_ATHLETEID              = "STRAVA_ATHLETEID";              //$NON-NLS-1$
   public static final String STRAVA_ATHLETEFULLNAME        = "STRAVA_ATHLETEFULLNAME";        //$NON-NLS-1$

   /*
    * Suunto preferences
    */
   public static final String  SUUNTO_USE_SINGLE_ACCOUNT_FOR_ALL_PEOPLE = "SUUNTO_USE_SINGLE_ACCOUNT_FOR_ALL_PEOPLE";                   //$NON-NLS-1$
   public static final String  SUUNTO_SELECTED_PERSON_INDEX             = "SUUNTO_SELECTED_PERSON_INDEX";                               //$NON-NLS-1$
   public static final String  SUUNTO_SELECTED_PERSON_ID                = "SUUNTO_SELECTED_PERSON_ID";                                  //$NON-NLS-1$
   private static final String SUUNTO_ACCESSTOKEN                       = "SUUNTO_ACCESSTOKEN";                                         //$NON-NLS-1$
   public static final String  SUUNTO_REFRESHTOKEN                      = getActivePersonId() + "SUUNTO_REFRESHTOKEN";                  //$NON-NLS-1$
   public static final String  SUUNTO_ACCESSTOKEN_EXPIRES_IN            = getActivePersonId() + "SUUNTO_ACCESSTOKEN_EXPIRES_IN";        //$NON-NLS-1$
   public static final String  SUUNTO_ACCESSTOKEN_ISSUE_DATETIME        = getActivePersonId() + "SUUNTO_ACCESSTOKEN_ISSUE_DATETIME";    //$NON-NLS-1$
   public static final String  SUUNTO_WORKOUT_DOWNLOAD_FOLDER           = getActivePersonId() + "SUUNTO_DOWNLOAD_FOLDER";               //$NON-NLS-1$
   public static final String  SUUNTO_USE_WORKOUT_FILTER_SINCE_DATE     = getActivePersonId() + "SUUNTO_USE_WORKOUT_FILTER_SINCE_DATE"; //$NON-NLS-1$
   public static final String  SUUNTO_WORKOUT_FILTER_SINCE_DATE         = getActivePersonId() + "SUUNTO_WORKOUT_FILTER_SINCE_DATE";     //$NON-NLS-1$

   private static String getActivePersonId() {

      final TourPerson activePerson = TourbookPlugin.getActivePerson();

      String activePersonId = null;
      if (activePerson != null) {
         activePersonId = String.valueOf(activePerson.getPersonId());
      }

      return activePersonId;
   }

   public static String getPersonSuuntoAccessTokenString(final String personId) {

      final StringBuilder personSuuntoAccessToken = new StringBuilder();
      if (StringUtils.hasContent(personId)) {

         personSuuntoAccessToken.append(personId + UI.DASH);
      }

      return personSuuntoAccessToken.append(SUUNTO_ACCESSTOKEN).toString();
   }

   public static String getSuuntoAccessToken_Active_Person_String() {

      final String personId = getActivePersonId();

      return getPersonSuuntoAccessTokenString(personId);
   }
}
