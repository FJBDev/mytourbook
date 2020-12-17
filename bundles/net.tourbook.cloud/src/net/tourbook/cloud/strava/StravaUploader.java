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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import net.tourbook.cloud.Activator;
import net.tourbook.cloud.IPreferences;
import net.tourbook.common.util.StringUtils;
import net.tourbook.data.TourData;
import net.tourbook.export.DialogExportTour;
import net.tourbook.extension.upload.TourbookCloudUploader;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.eclipse.jface.preference.IPreferenceStore;

public class StravaUploader extends TourbookCloudUploader {

   private IPreferenceStore _prefStore = Activator.getDefault().getPreferenceStore();

   public StravaUploader() {
      super("STRAVA", "Strava"); //$NON-NLS-1$ //$NON-NLS-2$
   }

   private String getAccessToken() {
      return _prefStore.getString(IPreferences.STRAVA_ACCESSTOKEN);
   }

   private String getRefreshToken() {
      final String toto = _prefStore.getString(IPreferences.STRAVA_REFRESHTOKEN);
      return toto;
   }

   @Override
   protected boolean isReady() {
      return StringUtils.hasContent(getAccessToken()) &&
            StringUtils.hasContent(getRefreshToken());
   }

   private void uploadFiles(final String tcxgz) throws FileNotFoundException, JsonProcessingException {
      // TODO Auto-generated method stub

      final HashMap<String, String> values = new HashMap<>() {
         {
            put("data_type", "tcx.gz");
         }
      };

      final ObjectMapper objectMapper = new ObjectMapper();
      final String requestBody = objectMapper
            .writeValueAsString(values);

      final HttpEntity entity = MultipartEntityBuilder
            .create()
            .addTextBody("data_type", "tcx.gz")
            .addTextBody("trainer", "false")
            .addTextBody("commute", "false")
            .addTextBody("name", "Yosemite Run")
            .addTextBody("description", "Un truc que jaimerais faire a Yosemite")
            .addBinaryBody("file",
                  new File("C:\\Users\\frederic\\Downloads\\2016-05-11_05-37-42.tcx.gz"),
                  ContentType.create("application/octet-stream"),
                  "export")
            .build();

      try (final CloseableHttpClient httpClient = HttpClients.createDefault()) {
         final HttpPost httpPost = new HttpPost("https://www.strava.com/api/v3/uploads");
         httpPost.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + getAccessToken());
         httpPost.setEntity(entity);
         final HttpResponse response = httpClient.execute(httpPost);
         final HttpEntity result = response.getEntity();

         // Read the contents of an entity and return it as a String.
         final String content = EntityUtils.toString(result);
         System.out.println(content);

//         if (response.getStatusLine() == HttpURLConnection.HTTP_OK) {
//            //Generate link
//            System.out.println(response);
//            TourLogManager.showLogView();
//            TourLogManager.addLog(//
//                  TourLogState.DEFAULT,
//                  "LINK");
//         }
      } catch (final IOException e) {
         // TODO Auto-generated catch block
         e.printStackTrace();
      }
   }

   @Override
   public void uploadTours(final List<TourData> selectedTours, final int _tourStartIndex, final int _tourEndIndex) {

      //check that a tour has a non empty time serie to avoid this strava error
      //"error": "Time information is missing from file.
      //Check that current token is not expired

      // If it is, refresh token with heroku /refreshToken

      // Generate TCX.gz file
      final String toto = DialogExportTour.convertTourToTCX();
      System.out.println(toto);
      // Send TCX.gz file

      final String tcxgz = "";
      try {
         uploadFiles(tcxgz);
      } catch (final FileNotFoundException e) {
         // TODO Auto-generated catch block
         e.printStackTrace();
      } catch (final JsonProcessingException e) {
         // TODO Auto-generated catch block
         e.printStackTrace();
      }
   }

}
