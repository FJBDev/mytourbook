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
import java.util.List;
import java.util.zip.GZIPOutputStream;

import net.tourbook.cloud.Activator;
import net.tourbook.cloud.IPreferences;
import net.tourbook.common.UI;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.util.StringUtils;
import net.tourbook.data.TourData;
import net.tourbook.export.ExportTourTCX;
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
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;

public class StravaUploader extends TourbookCloudUploader {

   private static HttpClient   httpClient     = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).build();
   private static final String _stravaBaseUrl = "https://www.strava.com/api/v3/";                                      //$NON-NLS-1$

   private IPreferenceStore    _prefStore     = Activator.getDefault().getPreferenceStore();

   public StravaUploader() {
      super("STRAVA", Messages.VendorName_Strava); //$NON-NLS-1$
   }

   private static String gzipFile(final String file) {

      final String compressedFilePath = file + ".gz"; //$NON-NLS-1$

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
         final Path temp = Files.createTempFile(tourId, "." + extension);//$NON-NLS-1$

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
         e.printStackTrace();
      }
   }

   private String getAccessToken() {
      return _prefStore.getString(IPreferences.STRAVA_ACCESSTOKEN);
   }

   private long getAcessTokenExpirationDate() {
      return _prefStore.getLong(IPreferences.STRAVA_ACCESSTOKEN_EXPIRES_AT);
   }

//   /**
//    * Retrieving the activity Id after the uploaded activity was created.
//    * Note: Maybe we don't want to do that as it is possible that activities are not fully processed
//    * and that it can take quite some times, and therefore a lot of API calls, until the activity is
//    * available
//    *
//    * @param uploadId
//    * @return The activity Id
//    */
//   private String getActivityId(final String uploadId) {
//
//      final HttpRequest request = HttpRequest.newBuilder()
//            .setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + getAccessToken()) //$NON-NLS-1$
//            .GET()
//            .uri(URI.create(_stravaBaseUrl + "uploads/" + uploadId))//$NON-NLS-1$
//            .build();
//
//      try {
//         final java.net.http.HttpResponse<String> response = httpClient.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
//
//         if (response.statusCode() == HttpURLConnection.HTTP_OK) {
//            final ObjectMapper mapper = new ObjectMapper();
//            final ActivityUpload activityUpload = mapper.readValue(response.body(),
//                  ActivityUpload.class);
//            return activityUpload.getActivity_id();
//         }
//      } catch (IOException | InterruptedException e) {
//         e.printStackTrace();
//      }
//
//      return UI.EMPTY_STRING;
//   }

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

      return getAcessTokenExpirationDate() - System.currentTimeMillis() - 300000 < 0;
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

      tcxExporter.export(absoluteTourFilePath);

      // Gzip the tour
      return gzipFile(absoluteTourFilePath);
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

      final String body = "{\"refresh_token\" : \"" + getRefreshToken() + "\"}"; //$NON-NLS-1$  //$NON-NLS-2$
      final HttpRequest request = HttpRequest.newBuilder()
            .header("Content-Type", "application/json") //$NON-NLS-1$ //$NON-NLS-2$
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
      } catch (IOException | InterruptedException e) {
         e.printStackTrace();
      }
   }

   private ActivityUpload uploadFile(final String absoluteCompressedTourFilePath, final TourData tourData) {

      final HttpEntity entity = MultipartEntityBuilder
            .create()
            .addTextBody("data_type", "tcx.gz")
            .addTextBody("name", tourData.getTourTitle()) //$NON-NLS-1$
            .addTextBody("description", tourData.getTourDescription()) //$NON-NLS-1$
            .addBinaryBody("file", //$NON-NLS-1$
                  new File(absoluteCompressedTourFilePath),
                  ContentType.create("application/octet-stream"), //$NON-NLS-1$
                  FilenameUtils.removeExtension(absoluteCompressedTourFilePath))
            .build();

      ActivityUpload activityUpload = new ActivityUpload();

      try (final CloseableHttpClient apacheHttpClient = HttpClients.createDefault()) {
         final HttpPost httpPost = new HttpPost(_stravaBaseUrl + "/uploads");//$NON-NLS-1$
         httpPost.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + getAccessToken());
         httpPost.setEntity(entity);
         final HttpResponse response = apacheHttpClient.execute(httpPost);
         final HttpEntity result = response.getEntity();

         // Read the contents of an entity and return it as a String.
         final String content = EntityUtils.toString(result);

         if (response.getStatusLine().getStatusCode() == HttpURLConnection.HTTP_CREATED) {

            final ObjectMapper mapper = new ObjectMapper();
            activityUpload = mapper.readValue(content, ActivityUpload.class);
         } else {
            activityUpload.setError(content);
         }
      } catch (final IOException e) {
         e.printStackTrace();
      }

      return activityUpload;
   }

   @Override
   public void uploadTours(final List<TourData> selectedTours) {

      tryRenewTokens();

      final IRunnableWithProgress runnable = new IRunnableWithProgress() {

         @Override
         public void run(final IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {

            monitor.beginTask("TOTO", selectedTours.size() * 2);

            monitor.subTask("DSNFKJ....\u231B\nfeff.....\u2713\neffe...");

            for (int index = 0; index < selectedTours.size() && !monitor.isCanceled(); ++index) {

               final TourData tourData = selectedTours.get(index);
               final String tourDate = tourData.getTourStartTime().format(TimeTools.Formatter_DateTime_S);

               //Check that a tour has a non empty time serie to avoid this Strava error
               //"error": "Time information is missing from file.
               //TODO? V2: Create activities without timeseries using this API endpoint :
               //https://developers.strava.com/playground/#/Activities/createActivity
               if (tourData.timeSerie == null || tourData.timeSerie.length == 0) {

                  TourLogManager.logError(NLS.bind(Messages.Log_UploadToursToStrava_002_NoTimeDataSeries, tourDate));
                  monitor.worked(1);
                  continue;
               }

               //TODO FB Why the .vm file doens't get loaded but works if I have just exported a TCX file ?!

               final String absoluteTourFilePath = createTemporaryTourFile(String.valueOf(tourData.getTourId()), "tcx");
               final String absoluteCompressedTourFilePath = processTour(tourData, absoluteTourFilePath);

               monitor.worked(1);

               // Send TCX.gz file
               final ActivityUpload activityUpload = uploadFile(absoluteCompressedTourFilePath, tourData);

               deleteTemporaryFiles(absoluteTourFilePath, absoluteCompressedTourFilePath);

               monitor.worked(1);

               if (StringUtils.hasContent(activityUpload.getError())) {
                  TourLogManager.logError(NLS.bind(Messages.Log_UploadToursToStrava_003_UploadError, tourDate, activityUpload.getError()));
               } else {
                  TourLogManager.addLog(TourLogState.IMPORT_OK,
                        NLS.bind(Messages.Log_UploadToursToStrava_002_UploadStatus,
                              new Object[] {
                                    tourDate,
                                    activityUpload.getId(),
                                    activityUpload.getStatus() }));
               }
            }
         }
      };

      try {
         final long start = System.currentTimeMillis();

         TourLogManager.showLogView();
         TourLogManager.logTitle(NLS.bind(Messages.Log_UploadToursToStrava_001_Start, selectedTours.size()));

         new ProgressMonitorDialog(Display.getCurrent().getActiveShell()).run(true, true, runnable);

         TourLogManager.logTitle(String.format(Messages.Log_UploadToursToStrava_004_End, (System.currentTimeMillis() - start) / 1000.0));

      } catch (final InvocationTargetException | InterruptedException e) {
         e.printStackTrace();
      }
   }
}
