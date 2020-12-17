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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;

import net.tourbook.cloud.Activator;
import net.tourbook.cloud.IPreferences;
import net.tourbook.common.util.StringUtils;
import net.tourbook.data.TourData;
import net.tourbook.extension.upload.TourbookCloudUploader;

import org.apache.http.HttpHeaders;
import org.eclipse.jface.preference.IPreferenceStore;

public class StravaUploader extends TourbookCloudUploader {

   private static HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

   private IPreferenceStore  _prefStore = Activator.getDefault().getPreferenceStore();

   public StravaUploader() {
      super("STRAVA", "Strava"); //$NON-NLS-1$ //$NON-NLS-2$
   }

   @Override
   protected boolean isReady() {
      return StringUtils.hasContent(_prefStore.getString(IPreferences.STRAVA_ACCESSTOKEN)) &&
            StringUtils.hasContent(_prefStore.getString(IPreferences.STRAVA_REFRESHTOKEN));
   }

   private void uploadFiles(final String tcxgz) throws FileNotFoundException, JsonProcessingException {
      // TODO Auto-generated method stub

      final var values = new HashMap<String, String>() {
         {
            put("data_type", "tcx.gz");
         }
      };

      final var objectMapper = new ObjectMapper();
      final String requestBody = objectMapper
            .writeValueAsString(values);

      final HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("https://www.strava.com/api/v3/uploads"))
            .setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + _prefStore.getString(IPreferences.STRAVA_ACCESSTOKEN))
//                  HttpRequest.BodyPublishers.ofInputStream( -> inputStream(new FileInputStream(tcxgz)))
//            .POST(HttpRequest.BodyPublishers.ofFile(Paths.get("/home/frederic/Downloads/export.gpx")))
            .POST(HttpRequest.BodyPublishers.ofFile(Paths.get("/home/frederic/Downloads/export.gpx")))
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build();

      try {
         final HttpResponse<String> response = httpClient.send(request, BodyHandlers.ofString());

         System.out.println(response);
      } catch (IOException | InterruptedException e) {
         // TODO Auto-generated catch block
         e.printStackTrace();
      }
   }

   @Override
   public void uploadTours(final List<TourData> selectedTours, final int _tourStartIndex, final int _tourEndIndex) {

      //Check that current token is not expired

      // If it is, refresh token with heroku /refreshToken

      // Generate TCX.gz file

      // Send TCX.gz file
      System.out.println("TOTO!!!!!");

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
