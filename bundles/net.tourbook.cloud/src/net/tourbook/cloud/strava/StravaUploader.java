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

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPOutputStream;

import net.tourbook.cloud.Activator;
import net.tourbook.cloud.IPreferences;
import net.tourbook.common.UI;
import net.tourbook.common.util.StringUtils;
import net.tourbook.data.TourData;
import net.tourbook.export.TourExporter;
import net.tourbook.extension.upload.TourbookCloudUploader;
import net.tourbook.tour.TourLogManager;
import net.tourbook.tour.TourLogState;

import org.apache.commons.io.FilenameUtils;
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

   private static HttpClient   httpClient     = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).build();
   private static final String _stravaBaseUrl = "https://www.strava.com/api/v3/";

   private IPreferenceStore    _prefStore     = Activator.getDefault().getPreferenceStore();

   public StravaUploader() {
      super("STRAVA", "Strava"); //$NON-NLS-1$ //$NON-NLS-2$
   }

   private static void compressGzipFile(final String file, final String gzipFile) {

      try (final FileInputStream fis = new FileInputStream(file);
            final FileOutputStream fos = new FileOutputStream(gzipFile);
            final GZIPOutputStream gzipOS = new GZIPOutputStream(fos)) {

         final byte[] buffer = new byte[1024];
         int len;
         while ((len = fis.read(buffer)) != -1) {
            gzipOS.write(buffer, 0, len);
         }
      } catch (final IOException e) {
         e.printStackTrace();
      }
   }

   private String getAccessToken() {
      return _prefStore.getString(IPreferences.STRAVA_ACCESSTOKEN);
   }

   private String getActivityId(final String id_str) {
      //TODO FB Maybe we don't want to do that as it is possible that activites are not fully processed
      final HttpRequest request = HttpRequest.newBuilder()
            .setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + getAccessToken()) //$NON-NLS-1$
            .GET()
            .uri(URI.create(_stravaBaseUrl + "uploads/" + id_str))//$NON-NLS-1$
            .build();

      try {
         final java.net.http.HttpResponse<String> response = httpClient.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
         final ObjectMapper mapper = new ObjectMapper();

         if (response.statusCode() == HttpURLConnection.HTTP_OK) {
            final ActivityUpload result2 = mapper.readValue(response.body(),
                  ActivityUpload.class);
            return result2.getActivity_id();
         }
         //else
         // if not ok, display the string in  "error": null,
      } catch (IOException | InterruptedException e) {
         // TODO Auto-generated catch block
         e.printStackTrace();
      }

      return null;
   }

   private String getRefreshToken() {
      return _prefStore.getString(IPreferences.STRAVA_REFRESHTOKEN);
   }

   @Override
   protected boolean isReady() {
      return StringUtils.hasContent(getAccessToken()) &&
            StringUtils.hasContent(getRefreshToken());
   }

   private void uploadFiles(final String tcxgz, final String tourTitle, final String tourDescription) {

      final List<String> uploadIds = new ArrayList<>();
      final HttpEntity entity = MultipartEntityBuilder
            .create()
            .addTextBody("data_type", "tcx.gz")
            .addTextBody("name", tourTitle) //$NON-NLS-1$
            .addTextBody("description", tourDescription) //$NON-NLS-1$
            .addBinaryBody("file", //$NON-NLS-1$
                  new File(tcxgz),
                  ContentType.create("application/octet-stream"), //$NON-NLS-1$
                  FilenameUtils.removeExtension(tcxgz))
            .build();

      try (final CloseableHttpClient apacheHttpClient = HttpClients.createDefault()) {
         final HttpPost httpPost = new HttpPost(_stravaBaseUrl + "/uploads");//$NON-NLS-1$
         httpPost.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + getAccessToken());
         httpPost.setEntity(entity);
         final HttpResponse response = apacheHttpClient.execute(httpPost);
         final HttpEntity result = response.getEntity();

         // Read the contents of an entity and return it as a String.
         final String content = EntityUtils.toString(result);
         System.out.println(content);

         if (response.getStatusLine().getStatusCode() == HttpURLConnection.HTTP_CREATED) {
            //Generate link
            System.out.println(response);
            TourLogManager.showLogView();
            String activityId = UI.EMPTY_STRING;

            final ObjectMapper mapper = new ObjectMapper();

            final ActivityUpload result2 = mapper.readValue(content,
                  ActivityUpload.class);
            uploadIds.add(result2.getId_str());

            activityId = getActivityId(result2.getId_str());
            //TODO See email to Wolfgang
            TourLogManager.addLog(//
                  TourLogState.DEFAULT,
                  "<a>https://www.strava.com/activities/" + activityId + "</a>");
         }
      } catch (final IOException e) {
         e.printStackTrace();
      }
   }

   @Override
   public void uploadTours(final List<TourData> selectedTours, final int _tourStartIndex, final int _tourEndIndex) {

      //check that a tour has a non empty time serie to avoid this strava error
      //"error": "Time information is missing from file.
      //Check that current token is not expired

      // If it is, refresh token with heroku /refreshToken

      // Generate TCX file
      //TODO FB Why the .vm file doens't get loaded but works if I have just exported a TCX file ?!
      final TourExporter tcxExporter = new TourExporter(selectedTours.get(0), "/format-templates/tcx-2.0.vm");
      final boolean toto = tcxExporter.export("C:\\Users\\frederic\\Downloads\\STMigration\\test.tcx");

      // Gzip the tour
      compressGzipFile("C:\\Users\\frederic\\Downloads\\STMigration\\test.tcx", "C:\\Users\\frederic\\Downloads\\STMigration\\test.tcx.gz");

      // Send TCX.gz file
      final TourData tourData = selectedTours.get(0);
      uploadFiles("C:\\Users\\frederic\\Downloads\\STMigration\\test.tcx.gz", tourData.getTourTitle(), tourData.getTourDescription());

      //Delete the temp tcx and tcx.gz file
   }
}
