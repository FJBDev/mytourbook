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

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import net.tourbook.cloud.Activator;
import net.tourbook.cloud.Preferences;
import net.tourbook.cloud.oauth2.OAuth2Constants;
import net.tourbook.cloud.suunto.workouts.Payload;
import net.tourbook.cloud.suunto.workouts.Workouts;
import net.tourbook.common.UI;
import net.tourbook.common.util.SQL;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.StringUtils;
import net.tourbook.database.TourDatabase;
import net.tourbook.extension.download.TourbookCloudDownloader;

import org.apache.http.HttpHeaders;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.dialogs.PreferencesUtil;

public class SuuntoCloudDownloader extends TourbookCloudDownloader {

   private static HttpClient       _httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofMinutes(5)).build();
   private static IPreferenceStore _prefStore  = Activator.getDefault().getPreferenceStore();

   public SuuntoCloudDownloader() {
      super("SUUNTO", Messages.VendorName_Suunto_Routes, "DESCRIPTION", "URL"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

   }

   private static void logDownloadResult(final WorkoutDownload workoutdownload) {}

   private CompletableFuture<WorkoutDownload> downloadFile(final String workoutKey) {

      final HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(OAuth2Constants.HEROKU_APP_URL + "/suunto/workout/exportFit?workoutKey=" + workoutKey))//$NON-NLS-1$
            .header(HttpHeaders.AUTHORIZATION, OAuth2Constants.BEARER + getAccessToken())
            .GET()
            .build();

      return sendAsyncRequest(workoutKey, request);
   }

   private void downloadFiles(final List<Payload> newWorkouts) {


      final List<CompletableFuture<WorkoutDownload>> workoutDownloads = new ArrayList<>();

      for (final Payload payload : newWorkouts) {

         workoutDownloads.add(downloadFile(payload.workoutKey));
      }

      workoutDownloads.stream().map(CompletableFuture::join).forEach(SuuntoCloudDownloader::logDownloadResult);

   }

   @Override
   public void downloadTours() {

      //todo fb if token not valid, do not continue and do the same for strava
      //if the preferences have not been set (tokens, folder)
      if (!SuuntoTokensRetrievalHandler.getValidTokens()) // The OK button was not clicked
      {

         final int returnResult = PreferencesUtil.createPreferenceDialogOn(
               Display.getCurrent().getActiveShell(),
               PrefPageSuunto.ID,
               null,
               null).open();

         if (returnResult != 0) {
            return;
         }
      }
      BusyIndicator.showWhile(Display.getCurrent(), () -> {
         //if the tokens are not valid
         //display a message for the user
         if (!SuuntoTokensRetrievalHandler.getValidTokens()) {
            return;
         }

         //Get the list of workouts
         final Workouts workouts = retrieveWorkoutsList();

         if (workouts.payload.size() == 0) {
            return;
         }

         final List<Long> tourStartTimes = retrieveAllTourStartTimes();

         //Identifying the workouts that have not yet been imported in the tour database
         final List<Payload> newWorkouts = new ArrayList<>();
         for (final Payload suuntoWorkout : workouts.payload) {

            if (tourStartTimes.contains(suuntoWorkout.startTime / 1000L * 1000L)) {
               continue;
            }

            newWorkouts.add(suuntoWorkout);
         }

         // async download of the files
         downloadFiles(newWorkouts);

      });

   }

   private String getAccessToken() {
      return _prefStore.getString(Preferences.SUUNTO_ACCESSTOKEN);
   }

   private String getRefreshToken() {
      return _prefStore.getString(Preferences.SUUNTO_REFRESHTOKEN);
   }

   @Override
   protected boolean isReady() {
      return StringUtils.hasContent(getAccessToken() + getRefreshToken());
   }

   private List<Long> retrieveAllTourStartTimes() {

      final List<Long> tourStartTimes = new ArrayList<>();
      try (Connection conn = TourDatabase.getInstance().getConnection();
            Statement stmt = conn.createStatement()) {

         final String sqlQuery = "SELECT tourStartTime FROM " + TourDatabase.TABLE_TOUR_DATA; //$NON-NLS-1$

         final ResultSet result = stmt.executeQuery(sqlQuery);

         while (result.next()) {

            tourStartTimes.add(result.getLong(1));
         }

      } catch (final SQLException e) {
         SQL.showException(e);
      }
      return tourStartTimes;
   }

   private Workouts retrieveWorkoutsList() {

      final var toto = _prefStore.getLong(Preferences.SUUNTO_FILE_DOWNLOAD_SINCE_DATE);
      final HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(OAuth2Constants.HEROKU_APP_URL + "/suunto/workouts?since=" + toto))//$NON-NLS-1$
            .header(HttpHeaders.AUTHORIZATION, OAuth2Constants.BEARER + getAccessToken())
            .GET()
            .build();

      try {
         final HttpResponse<String> response = _httpClient.send(request, HttpResponse.BodyHandlers.ofString());

         if (response.statusCode() == HttpURLConnection.HTTP_OK && StringUtils.hasContent(response.body())) {
            return new ObjectMapper().readValue(response.body(), Workouts.class);
         }
      } catch (IOException | InterruptedException e) {
         StatusUtil.log(e);
         Thread.currentThread().interrupt();
      }

      return new Workouts();
   }

   private CompletableFuture<WorkoutDownload> sendAsyncRequest(final String workoutKey, final HttpRequest request) {

      final CompletableFuture<WorkoutDownload> workoutDownload = _httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofInputStream())
            .thenApply(response -> writeFileToFolder(workoutKey, response))
            .exceptionally(e -> new WorkoutDownload(workoutKey));

      return workoutDownload;
   }

   private WorkoutDownload writeFileToFolder(final String workoutKey, final HttpResponse<InputStream> response) {

      final WorkoutDownload workoutDownload = new WorkoutDownload(workoutKey);

      final Optional<String> contentDisposition = response.headers().firstValue("Content-Disposition"); //$NON-NLS-1$
      String fileName = UI.EMPTY_STRING;
      if (contentDisposition.isPresent()) {
         fileName = contentDisposition.get().replaceFirst("(?i)^.*filename=\"([^\"]+)\".*$", "$1"); //$NON-NLS-1$ //$NON-NLS-2$
      }

      final Path filePath = Paths.get(_prefStore.getString(Preferences.SUUNTO_FILE_DOWNLOAD_FOLDER), StringUtils.sanitizeFileName(fileName));

      try (InputStream inputStream = response.body();
            FileOutputStream fileOutputStream = new FileOutputStream(filePath.toFile())) {

         int inByte;
         while ((inByte = inputStream.read()) != -1) {
            fileOutputStream.write(inByte);
         }

      } catch (final IOException e) {
         StatusUtil.log(e);
         return workoutDownload;
      }
      workoutDownload.setAbsoluteFilePath(filePath.toAbsolutePath().toString());
      workoutDownload.setDownloaded(true);

      return workoutDownload;
   }

//   @Override
//   public void uploadTours(final List<TourData> selectedTours) {
//
//      final int numberOfTours = selectedTours.size();
//      _numberOfUploadedTours = new int[1];
//
//      final IRunnableWithProgress runnable = new IRunnableWithProgress() {
//
//         @Override
//         public void run(final IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
//
//            monitor.beginTask(NLS.bind("Messages.UploadToursToStrava_Task", numberOfTours, _prefStore.getString(Preferences.STRAVA_ATHLETEFULLNAME)),
//                  numberOfTours * 2);
//
//            monitor.subTask(NLS.bind("Messages.UploadToursToStrava_SubTask",
//                  "Messages.UploadToursToStrava_Icon_Hourglass",
//                  UI.EMPTY_STRING));
//
//            final Map<Long, String> toursWithGpsSeries = new HashMap<>();
//            for (int index = 0; index < numberOfTours; ++index) {
//
//               final TourData tourData = selectedTours.get(index);
//
//               if (tourData.latitudeSerie == null || tourData.latitudeSerie.length == 0) {
//
//                  final String tourDate = tourData.getTourStartTime().format(TimeTools.Formatter_DateTime_S);
//
//                  TourLogManager.logError(NLS.bind("Messages.Log_UploadToursToStrava_002_NoTourTitle", tourDate));
//                  monitor.worked(2);
//
//               } else {
//
//                  toursWithGpsSeries.put(tourData.getTourStartTimeMS(), convertTourToGpx(tourData));
//
//                  monitor.worked(1);
//               }
//            }
//
//            monitor.subTask(NLS.bind("Messages.UploadToursToStrava_SubTask",
//                  "Messages.UploadToursToStrava_Icon_Check",
//                  "Messages.UploadToursToStrava_Icon_Hourglass"));
//
////                     tryRenewTokens();
//
//            uploadRoutes(toursWithGpsSeries);
//
//            monitor.worked(toursWithGpsSeries.size());
//
//            monitor.subTask(NLS.bind("Messages.UploadToursToStrava_SubTask",
//                  "Messages.UploadToursToStrava_Icon_Check",
//                  "Messages.UploadToursToStrava_Icon_Check"));
//         }
//      };
//
//      try {
//         final long start = System.currentTimeMillis();
//
//         TourLogManager.showLogView();
//         TourLogManager.logTitle(NLS.bind("Messages.Log_UploadToursToStrava_001_Start", numberOfTours));
//
//         new ProgressMonitorDialog(Display.getCurrent().getActiveShell()).run(true, false, runnable);
//
//         TourLogManager.logTitle(String.format("Messages.Log_UploadToursToStrava_005_End", (System.currentTimeMillis() - start) / 1000.0));
//
//         MessageDialog.openInformation(
//               Display.getDefault().getActiveShell(),
//               "Messages.Dialog_StravaUpload_Summary",
//               NLS.bind("Messages.Dialog_StravaUpload_Message", _numberOfUploadedTours[0], numberOfTours - _numberOfUploadedTours[0]));
//
//      } catch (final InvocationTargetException | InterruptedException e) {
//         StatusUtil.log(e);
//         Thread.currentThread().interrupt();
//      }
//   }

}
