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
import java.lang.reflect.InvocationTargetException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPOutputStream;

import net.tourbook.cloud.Activator;
import net.tourbook.cloud.IPreferences;
import net.tourbook.common.UI;
import net.tourbook.common.util.StringUtils;
import net.tourbook.data.TourData;
import net.tourbook.export.ExportTourTCX;
import net.tourbook.export.TourExporter;
import net.tourbook.extension.upload.TourbookCloudUploader;
import net.tourbook.tour.TourLogManager;

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
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.widgets.Display;

public class StravaUploader extends TourbookCloudUploader {

   private static HttpClient   httpClient     = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).build();
   private static final String _stravaBaseUrl = "https://www.strava.com/api/v3/";

   private IPreferenceStore    _prefStore     = Activator.getDefault().getPreferenceStore();

   public StravaUploader() {
      super("STRAVA", "Strava"); //$NON-NLS-1$ //$NON-NLS-2$
   }

   private static String compressGzipFile(final String file) {

      final String compressedFilePath = file + ".gz";

      try (final FileInputStream fis = new FileInputStream(file);
            final FileOutputStream fos = new FileOutputStream(compressedFilePath);
            final GZIPOutputStream gzipOS = new GZIPOutputStream(fos)) {

         final byte[] buffer = new byte[1024];
         int len;
         while ((len = fis.read(buffer)) != -1) {
            gzipOS.write(buffer, 0, len);
         }
      } catch (final IOException e) {
         e.printStackTrace();
         return UI.EMPTY_STRING;
      }

      return compressedFilePath;
   }

   private String createTemporaryTourFile(final String tourId, final String extension) {
      String absoluteFilePath = UI.EMPTY_STRING;

      try {
         final Path temp = Files.createTempFile(tourId, "." + extension);

         absoluteFilePath = temp.toString();

      } catch (final IOException e) {
         e.printStackTrace();
      }
      return absoluteFilePath;
   }

   private void deleteTemporaryFiles(final String absoluteTourFilePath, final String absoluteCompressedTourFilePath) {
      try {
         Files.delete(Paths.get(absoluteTourFilePath));
         Files.delete(Paths.get(absoluteCompressedTourFilePath));
      } catch (final IOException e) {
         // TODO Auto-generated catch block
         e.printStackTrace();
      }
   }

   private String getAccessToken() {
      return _prefStore.getString(IPreferences.STRAVA_ACCESSTOKEN);
   }

   private long getAcessTokenExpirationDate() {
      return _prefStore.getLong(IPreferences.STRAVA_ACCESSTOKEN_EXPIRES_AT);
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

         if (response.statusCode() == HttpURLConnection.HTTP_OK) {
            final ObjectMapper mapper = new ObjectMapper();
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

   /**
    * We consider that an access token is expired if there are less
    * than 5 mins remaining until the actual expiration
    *
    * @return
    */
   private boolean isAccessTokenExpired() {

      final long toto = getAcessTokenExpirationDate() - System.currentTimeMillis();
      return toto - 300000 < 0;
   }

   @Override
   protected boolean isReady() {
      return StringUtils.hasContent(getAccessToken()) &&
            StringUtils.hasContent(getRefreshToken());
   }

   private String processTour(final TourData tourData, final String absoluteTourFilePath) {

      final TourExporter tcxExporter = new TourExporter(
            ExportTourTCX.TCX_2_0_TEMPLATE,
            true,
            true).useTourData(tourData);

      final boolean toto = tcxExporter.export(absoluteTourFilePath);

      // Gzip the tour
      return compressGzipFile(absoluteTourFilePath);
   }

   private void setAccessToken(final String accessToken) {
      _prefStore.setValue(IPreferences.STRAVA_ACCESSTOKEN, accessToken);
   }

   private void setAccessTokenExpirationDate(final long expireAt) {
      _prefStore.setValue(IPreferences.STRAVA_ACCESSTOKEN_EXPIRES_AT, expireAt);
   }

   private void setRefreshToken(final String refreshToken) {
      _prefStore.setValue(IPreferences.STRAVA_REFRESHTOKEN, refreshToken);
   }

   private void tryRenewTokens() {

      if (!isAccessTokenExpired()) {
         return;
      }

      final String body = "{\"refresh_token\" : \"" + getRefreshToken() + "\"}";
      final HttpRequest request = HttpRequest.newBuilder()
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .uri(URI.create("https://mytourbook-oauth-passeur.herokuapp.com/refreshToken"))//$NON-NLS-1$
            .build();

      try {
         final java.net.http.HttpResponse<String> response = httpClient.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());

         if (response.statusCode() == HttpURLConnection.HTTP_CREATED && StringUtils.hasContent(response.body())) {
            final ObjectMapper mapper = new ObjectMapper();
            final Token result2 = mapper.readValue(response.body(),
                  Token.class);

            setAccessTokenExpirationDate(result2.getExpires_at());
            setRefreshToken(result2.getRefresh_token());
            setAccessToken(result2.getAccess_token());
         }
         //else
         // if not ok, display the string in  "error": null,
      } catch (IOException | InterruptedException e) {
         // TODO Auto-generated catch block
         e.printStackTrace();
      }

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
            final String link = "https://www.strava.com/activities/" + activityId;
            TourLogManager.logInfo(
                  "<br><a>https://www.strava.com/activities/" + activityId + "</a>");
            TourLogManager.logInfo(
                  "<br><a href=\"" + link + "\">https://www.strava.com/activities/</a>");
         }
      } catch (final IOException e) {
         e.printStackTrace();
      }
   }

   @Override
   public void uploadTours(final List<TourData> selectedTours, final int _tourStartIndex, final int _tourEndIndex) {

      tryRenewTokens();

      final boolean[] isCanceled = new boolean[] { false };

      final IRunnableWithProgress runnable = new IRunnableWithProgress() {

         @Override
         public void run(final IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {

            monitor.beginTask("TOTO", selectedTours.size());

            // loop over all tours and compute values
            for (final TourData tourData : selectedTours) {
               //check that a tour has a non empty time serie to avoid this strava error
               //"error": "Time information is missing from file.

               if (tourData.timeSerie == null || tourData.timeSerie.length == 0) {
                  //TODO add log
                  monitor.worked(1);
                  continue;
               }

               // If it is, refresh token with heroku /refreshToken

               // Generate TCX file
               //TODO FB Why the .vm file doens't get loaded but works if I have just exported a TCX file ?!

               final String absoluteTourFilePath = createTemporaryTourFile(String.valueOf(tourData.getTourId()), "tcx");
               final String absoluteCompressedTourFilePath = processTour(tourData, absoluteTourFilePath);
               // Send TCX.gz file
               uploadFiles(absoluteCompressedTourFilePath, tourData.getTourTitle(), tourData.getTourDescription());

               deleteTemporaryFiles(absoluteTourFilePath, absoluteCompressedTourFilePath);

               monitor.subTask("DSNFKJ");
               monitor.worked(1);

               if (monitor.isCanceled()) {
                  isCanceled[0] = true;
                  break;
               }
            }
         }
      };
      try {

         new ProgressMonitorDialog(Display.getCurrent().getActiveShell()).run(true, true, runnable);

      } catch (final InvocationTargetException | InterruptedException e) {
         e.printStackTrace();
      }
   }
}
