/*******************************************************************************
 * Copyright (C) 2020 Frédéric Bard
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
package net.tourbook.cloud.strava;

import java.util.List;

import net.tourbook.cloud.Activator;
import net.tourbook.cloud.IPreferences;
import net.tourbook.common.util.StringUtils;
import net.tourbook.data.TourData;
import net.tourbook.extension.upload.TourbookCloudUploader;

import org.eclipse.jface.preference.IPreferenceStore;

public class StravaUploader extends TourbookCloudUploader {

   private IPreferenceStore _prefStore = Activator.getDefault().getPreferenceStore();

   public StravaUploader() {
      super("STRAVA", "Strava"); //$NON-NLS-1$ //$NON-NLS-2$
   }

   @Override
   protected boolean isReady() {
      return StringUtils.hasContent(_prefStore.getString(IPreferences.STRAVA_ACCESSTOKEN)) &&
            StringUtils.hasContent(_prefStore.getString(IPreferences.STRAVA_REFRESHTOKEN));
   }

   @Override
   public void uploadTours(final List<TourData> selectedTours, final int _tourStartIndex, final int _tourEndIndex) {
      // TODO Auto-generated method stub
      // Validate Token

      // Generate TCX.gz file

      // Send TCX.gz file
      System.out.println("TOTO!!!!!");

   }

}
