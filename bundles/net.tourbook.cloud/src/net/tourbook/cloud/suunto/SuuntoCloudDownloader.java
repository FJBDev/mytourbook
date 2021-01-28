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

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import net.tourbook.cloud.Activator;
import net.tourbook.cloud.Preferences;
import net.tourbook.cloud.oauth2.OAuth2Constants;
import net.tourbook.common.UI;
import net.tourbook.common.util.FilesUtils;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.StringUtils;
import net.tourbook.data.TourData;
import net.tourbook.data.TourType;
import net.tourbook.export.ExportTourGPX;
import net.tourbook.export.TourExporter;
import net.tourbook.ext.velocity.VelocityService;
import net.tourbook.extension.download.TourbookCloudDownloader;

import org.apache.http.HttpHeaders;
import org.eclipse.jface.preference.IPreferenceStore;

public class SuuntoCloudDownloader extends TourbookCloudDownloader {

   private static HttpClient       _httpClient   = HttpClient.newBuilder().connectTimeout(Duration.ofMinutes(5)).build();
   private static IPreferenceStore _prefStore    = Activator.getDefault().getPreferenceStore();
   private static TourExporter     _tourExporter = new TourExporter(ExportTourGPX.GPX_1_0_TEMPLATE);
   private static int[]            _numberOfUploadedTours;

   public SuuntoCloudDownloader() {
      super("SUUNTO", Messages.VendorName_Suunto_Routes, "DESCRIPTION", "URL"); //$NON-NLS-1$

      _tourExporter.setUseDescription(true);

      VelocityService.init();
   }

   //todo fb put the common method in a common cloud utils file so they can be resued and we can remove duplicated code

   private static void logUploadResult(final String activityupload1) {
      System.out.println(activityupload1);
   }

   private String ConvertResponseToUpload(final HttpResponse<String> name) {

      //todo fb in the server.js, return verbatim what suunto returns
      System.out.println(name.body());
      return "TOTO";
   }

   private String convertTourToGpx(final TourData tourData) {

      final String absoluteTourFilePath = createTemporaryTourFile(String.valueOf(tourData.getTourId()), "gpx"); //$NON-NLS-1$

      _tourExporter.useTourData(tourData);

      final TourType tourType = tourData.getTourType();

      boolean useActivityType = false;
      String activityName = UI.EMPTY_STRING;
      if (tourType != null) {
         useActivityType = true;
         activityName = tourType.getName();
      }
      _tourExporter.setUseActivityType(useActivityType);
      _tourExporter.setActivityType(activityName);

      _tourExporter.export(absoluteTourFilePath);

      final String tourGpx = FilesUtils.readFileContentString(absoluteTourFilePath);

      FilesUtils.deleteFile(Paths.get(absoluteTourFilePath));

      return tourGpx;
   }

   //TODO FB put in file utils
   private String createTemporaryTourFile(final String tourId, final String extension) {

      String absoluteFilePath = UI.EMPTY_STRING;

      try {
         FilesUtils.deleteFile(Paths.get(tourId + UI.SYMBOL_DOT + extension));

         absoluteFilePath = Files.createTempFile(tourId, UI.SYMBOL_DOT + extension).toString();

      } catch (final IOException e) {
         StatusUtil.log(e);
      }
      return absoluteFilePath;
   }

   @Override
   public void downloadTours() {
      uploadRoutes();
//      MessageDialog.openInformation(Display.getCurrent().getActiveShell(), "HIIHHAAA!", getAccessToken());

      //Get the list of workouts

      // get all the startTime

      //loop all the starttimes
      // if the starttime /1000 equals one in the db => ignore
      // else add to the list of tfile to donwload

      // async download of the files

      //

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

   private CompletableFuture<String> sendAsyncRequest(final HttpRequest request) {

      final CompletableFuture<String> activityUpload = _httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenApply(name -> ConvertResponseToUpload(name))
            .exceptionally(e -> {
               return e.getMessage();
            });
      return activityUpload;
   }

   private CompletableFuture<String> uploadRoute() {

      final HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(OAuth2Constants.HEROKU_APP_URL + "/suunto/workouts"))//$NON-NLS-1$
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + getAccessToken()) //$NON-NLS-1$     .timeout(Duration.ofMinutes(5))
            .GET()
               .build();


      return sendAsyncRequest(request);
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

   private void uploadRoutes() {

      final List<CompletableFuture<String>> activityUploads = new ArrayList<>();

         activityUploads.add(uploadRoute());

      activityUploads.stream().map(CompletableFuture::join).forEach(SuuntoCloudDownloader::logUploadResult);
   }
}
