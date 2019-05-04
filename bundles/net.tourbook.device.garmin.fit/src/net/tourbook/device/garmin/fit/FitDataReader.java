/*******************************************************************************
 * Copyright (C) 2005, 2019 Wolfgang Schramm and Contributors
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
package net.tourbook.device.garmin.fit;

import com.garmin.fit.Decode;
import com.garmin.fit.Field;
import com.garmin.fit.Fit;
import com.garmin.fit.FitRuntimeException;
import com.garmin.fit.Mesg;
import com.garmin.fit.MesgBroadcaster;
import com.garmin.fit.MesgListener;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;

import net.tourbook.common.time.TimeTools;
import net.tourbook.data.TourData;
import net.tourbook.device.garmin.fit.listeners.MesgListener_Activity;
import net.tourbook.device.garmin.fit.listeners.MesgListener_BikeProfile;
import net.tourbook.device.garmin.fit.listeners.MesgListener_DeviceInfo;
import net.tourbook.device.garmin.fit.listeners.MesgListener_Event;
import net.tourbook.device.garmin.fit.listeners.MesgListener_FileCreator;
import net.tourbook.device.garmin.fit.listeners.MesgListener_FileId;
import net.tourbook.device.garmin.fit.listeners.MesgListener_Hr;
import net.tourbook.device.garmin.fit.listeners.MesgListener_Lap;
import net.tourbook.device.garmin.fit.listeners.MesgListener_Length;
import net.tourbook.device.garmin.fit.listeners.MesgListener_Record;
import net.tourbook.device.garmin.fit.listeners.MesgListener_Session;
import net.tourbook.importdata.DeviceData;
import net.tourbook.importdata.SerialParameters;
import net.tourbook.importdata.TourbookDevice;
import net.tourbook.tour.TourLogManager;

import org.apache.commons.io.IOUtils;

/**
 * Garmin FIT activity reader based on the official Garmin SDK.
 *
 * @author Wolfgang Schramm
 */
public class FitDataReader extends TourbookDevice {

   private static boolean _isLogging_FitData = System.getProperty("logFitData") != null; //$NON-NLS-1$

   private boolean        _isVersionLogged;

   private void addDebugLogListener(final MesgBroadcaster broadcaster) {

      broadcaster.addListener(new MesgListener() {
         @Override
         public void onMesg(final Mesg mesg) {

            long garminTimestamp = 0;

            for (final Field field : mesg.getFields()) {

               final String fieldName = field.getName();

//               if (fieldName.equals("temperature")) { //$NON-NLS-1$
//                  int a = 0;
//                  a++;
//               }

               if (fieldName.equals("timestamp")) { //$NON-NLS-1$
                  garminTimestamp = (Long) field.getValue();
               }

               /*
                * Set fields which should NOT be displayed in the log
                */
               if (fieldName.equals("") // //$NON-NLS-1$

                     || fieldName.equals("name") //                                                         //$NON-NLS-1$
                     || fieldName.equals("time") //                                                         //$NON-NLS-1$
                     || fieldName.equals("timestamp") //                                                    //$NON-NLS-1$
                                                                                   
                     || fieldName.equals("event_timestamp_12") //                                           //$NON-NLS-1$
                     || fieldName.equals("event_timestamp") //                                              //$NON-NLS-1$
                     || fieldName.equals("fractional_timestamp") //                             0.730 s     //$NON-NLS-1$
                                                                                   
                     || fieldName.equals("pool_length") //                                       25.0 m     //$NON-NLS-1$
                     || fieldName.equals("pool_length_unit") //                                     0       //$NON-NLS-1$

                     || fieldName.equals("") //                                                             //$NON-NLS-1$

               // Record data

                     || fieldName.equals("activity_type") //                                                //$NON-NLS-1$
                     || fieldName.equals("event") //                                                        //$NON-NLS-1$
                     || fieldName.equals("event_type") //                                                   //$NON-NLS-1$
                     || fieldName.equals("message_index") //                                                //$NON-NLS-1$

                     || fieldName.equals("altitude") //                                                     //$NON-NLS-1$
                     || fieldName.equals("cadence") //                                                      //$NON-NLS-1$
                     || fieldName.equals("fractional_cadence") //                                           //$NON-NLS-1$
                     || fieldName.equals("distance") //                                                     //$NON-NLS-1$
                     || fieldName.equals("grade") //                                                        //$NON-NLS-1$
                     || fieldName.equals("heart_rate") //                                                   //$NON-NLS-1$
                     || fieldName.equals("position_lat") //                                                 //$NON-NLS-1$
                     || fieldName.equals("position_long") //                                                //$NON-NLS-1$
                     || fieldName.equals("speed") //                                                        //$NON-NLS-1$
                     || fieldName.equals("compressed_speed_distance") //                                    //$NON-NLS-1$
                     || fieldName.equals("temperature") //                                                  //$NON-NLS-1$

                     || fieldName.equals("front_gear") //                                                   //$NON-NLS-1$
                     || fieldName.equals("front_gear_num") //                                               //$NON-NLS-1$
                     || fieldName.equals("rear_gear") //                                                    //$NON-NLS-1$
                     || fieldName.equals("rear_gear_num") //                                                //$NON-NLS-1$

                     || fieldName.equals("enhanced_altitude") //                                            //$NON-NLS-1$
                     || fieldName.equals("enhanced_min_altitude") //                                        //$NON-NLS-1$
                     || fieldName.equals("enhanced_max_altitude") //                                        //$NON-NLS-1$
                     || fieldName.equals("enhanced_speed") //                                               //$NON-NLS-1$
                     || fieldName.equals("enhanced_avg_speed") //                                           //$NON-NLS-1$
                     || fieldName.equals("enhanced_max_speed") //                                           //$NON-NLS-1$

//                     || fieldName.equals("total_training_effect") //            3.8                       //$NON-NLS-1$

               // Swimming

                     || fieldName.equals("filtered_bpm") //                     118 bpm                     //$NON-NLS-1$

                     || fieldName.equals("avg_swimming_cadence") //                      20 strokes/min     //$NON-NLS-1$
                     || fieldName.equals("length_type") //                                1                 //$NON-NLS-1$
                     || fieldName.equals("swim_stroke") //                                2 swim_stroke     //$NON-NLS-1$
                     || fieldName.equals("total_strokes") //                             12 strokes         //$NON-NLS-1$
                                                                                 
               // Swimming lap data                                              
                                                                                 
                     || fieldName.equals("first_length_index") //                        54                 //$NON-NLS-1$
                     || fieldName.equals("avg_stroke_distance") //                     2.25 m               //$NON-NLS-1$
                     || fieldName.equals("num_lengths") //                               16 lengths         //$NON-NLS-1$
                     || fieldName.equals("num_active_lengths") //                        16 lengths         //$NON-NLS-1$
                                                                                 
               // Running dynamics                                               
                                                                                 
                     || fieldName.equals("stance_time") //                            253.0 ms              //$NON-NLS-1$
                     || fieldName.equals("stance_time_percent") //                    34.75 percent         //$NON-NLS-1$
                     || fieldName.equals("stance_time_balance") //                    51.31 percent         //$NON-NLS-1$
                     || fieldName.equals("step_length") //                           1526.0 mm              //$NON-NLS-1$
                     || fieldName.equals("vertical_oscillation") //                    7.03 percent         //$NON-NLS-1$
                     || fieldName.equals("vertical_ratio") //                         114.2 mm              //$NON-NLS-1$

               // Lap data

                     || fieldName.equals("avg_cadence") //                                                  //$NON-NLS-1$
                     || fieldName.equals("avg_fractional_cadence") //                                       //$NON-NLS-1$
                     || fieldName.equals("avg_heart_rate") //                                               //$NON-NLS-1$
                     || fieldName.equals("avg_speed") //                                                    //$NON-NLS-1$
                     || fieldName.equals("data") //                                                         //$NON-NLS-1$
                     || fieldName.equals("event_group") //                                                  //$NON-NLS-1$
                     || fieldName.equals("end_position_lat") //                                             //$NON-NLS-1$
                     || fieldName.equals("end_position_long") //                                            //$NON-NLS-1$
                     || fieldName.equals("intensity") //                                                    //$NON-NLS-1$
                     || fieldName.equals("lap_trigger") //                                                  //$NON-NLS-1$
                     || fieldName.equals("max_cadence") //                                                  //$NON-NLS-1$
                     || fieldName.equals("max_fractional_cadence") //                                       //$NON-NLS-1$
                     || fieldName.equals("max_heart_rate") //                                               //$NON-NLS-1$
                     || fieldName.equals("max_speed") //                                                    //$NON-NLS-1$
                     || fieldName.equals("total_calories") //                                               //$NON-NLS-1$
                     || fieldName.equals("total_fat_calories") //                                           //$NON-NLS-1$
                     || fieldName.equals("sport") //                                                        //$NON-NLS-1$
                     || fieldName.equals("start_position_lat") //                                           //$NON-NLS-1$
                     || fieldName.equals("start_position_long") //                                          //$NON-NLS-1$
                     || fieldName.equals("start_time") //                                                   //$NON-NLS-1$
                     || fieldName.equals("total_ascent") //                                                 //$NON-NLS-1$
                     || fieldName.equals("total_descent") //                                                //$NON-NLS-1$
                     || fieldName.equals("total_cycles") //                                                 //$NON-NLS-1$
                     || fieldName.equals("total_distance") //                                               //$NON-NLS-1$
                     || fieldName.equals("total_elapsed_time") //                                           //$NON-NLS-1$
                     || fieldName.equals("total_timer_time") //                                             //$NON-NLS-1$

                     || fieldName.equals("avg_stance_time") //                                              //$NON-NLS-1$
                     || fieldName.equals("avg_stance_time_balance") //                                      //$NON-NLS-1$
                     || fieldName.equals("avg_stance_time_percent") //                                      //$NON-NLS-1$
                     || fieldName.equals("avg_step_length") //                                              //$NON-NLS-1$
                     || fieldName.equals("avg_vertical_oscillation") //                                     //$NON-NLS-1$
                     || fieldName.equals("avg_vertical_ratio") //                                           //$NON-NLS-1$
                     || fieldName.equals("avg_vam") //                                       0.348 m/s      //$NON-NLS-1$


                     || fieldName.equals("auto_activity_detect") //                              1          //$NON-NLS-1$
                     || fieldName.equals("autosync_min_steps") //                             2000 steps    //$NON-NLS-1$
                     || fieldName.equals("autosync_min_time") //                               240 minutes  //$NON-NLS-1$
                     || fieldName.equals("time_mode") //                                         1          //$NON-NLS-1$
                     || fieldName.equals("backlight_mode") //                                    3          //$NON-NLS-1$
                     || fieldName.equals("activity_tracker_enabled") //                          1          //$NON-NLS-1$
                     || fieldName.equals("move_alert_enabled") //                                0          //$NON-NLS-1$
                     || fieldName.equals("mounting_side") //                                     1          //$NON-NLS-1$
                     || fieldName.equals("lactate_threshold_autodetect_enabled") //              1          //$NON-NLS-1$
                     || fieldName.equals("wake_time") //                                     32400          //$NON-NLS-1$
                     || fieldName.equals("sleep_time") //                                    79200          //$NON-NLS-1$
                     || fieldName.equals("user_running_step_length") //                        0.0 m        //$NON-NLS-1$
                     || fieldName.equals("user_walking_step_length") //                        0.0 m        //$NON-NLS-1$
                     || fieldName.equals("resting_heart_rate") //                                0 bpm      //$NON-NLS-1$
                     || fieldName.equals("height_setting") //                                    0          //$NON-NLS-1$
                     || fieldName.equals("avg_temperature") //                                  28 C        //$NON-NLS-1$
                     || fieldName.equals("max_temperature") //                                  29 C        //$NON-NLS-1$
                     || fieldName.equals("avg_temperature") //                                  26 C        //$NON-NLS-1$
                     || fieldName.equals("max_temperature") //                                  27 C        //$NON-NLS-1$
                     || fieldName.equals("avg_temperature") //                                  26 C        //$NON-NLS-1$
                     || fieldName.equals("max_temperature") //                                  29 C        //$NON-NLS-1$
                     || fieldName.equals("total_anaerobic_training_effect") //                 0.0          //$NON-NLS-1$

               // Power

                     || fieldName.equals("power") //                                                        //$NON-NLS-1$
                     || fieldName.equals("accumulated_power") //                                            //$NON-NLS-1$
                     || fieldName.equals("left_right_balance") //                                           //$NON-NLS-1$
                     || fieldName.equals("left_torque_effectiveness") //                                    //$NON-NLS-1$
                     || fieldName.equals("right_torque_effectiveness") //                                   //$NON-NLS-1$
                     || fieldName.equals("left_pedal_smoothness") //                                        //$NON-NLS-1$
                     || fieldName.equals("right_pedal_smoothness") //                                       //$NON-NLS-1$

                     || fieldName.equals("functional_threshold_power") //                                   //$NON-NLS-1$
                     || fieldName.equals("power_setting") //                                                //$NON-NLS-1$
                     || fieldName.equals("pwr_calc_type") //                                                //$NON-NLS-1$

               // Device

                     || fieldName.equals("ant_network") //                                                  //$NON-NLS-1$
                     || fieldName.equals("ant_device_number") //                                            //$NON-NLS-1$
                     || fieldName.equals("ant_transmission_type") //                                        //$NON-NLS-1$
                     || fieldName.equals("battery_status") //                                               //$NON-NLS-1$
                     || fieldName.equals("battery_voltage") //                                              //$NON-NLS-1$
                     || fieldName.equals("cum_operating_time") //                                           //$NON-NLS-1$
                     || fieldName.equals("device_index") //                                                 //$NON-NLS-1$
                     || fieldName.equals("device_type") //                                                  //$NON-NLS-1$
                     || fieldName.equals("display_orientation") //                                          //$NON-NLS-1$
                     || fieldName.equals("friendly_name") //                                                //$NON-NLS-1$
                     || fieldName.equals("hardware_version") //                                             //$NON-NLS-1$
                     || fieldName.equals("manufacturer") //                                                 //$NON-NLS-1$
                     || fieldName.equals("product") //                                                      //$NON-NLS-1$
                     || fieldName.equals("serial_number") //                                                //$NON-NLS-1$
                     || fieldName.equals("software_version") //                                             //$NON-NLS-1$
                     || fieldName.equals("source_type") //                                                  //$NON-NLS-1$

                     || fieldName.equals("trigger") //                                                      //$NON-NLS-1$
                     || fieldName.equals("type") //                                                         //$NON-NLS-1$
                     || fieldName.equals("num_laps") //                                                     //$NON-NLS-1$
                     || fieldName.equals("num_sessions") //                                                 //$NON-NLS-1$
                     || fieldName.equals("sport_index") //                                                  //$NON-NLS-1$
                     || fieldName.equals("sub_sport") //                                                    //$NON-NLS-1$

               // HR

                     || fieldName.equals("activity_class") //                                               //$NON-NLS-1$
                     || fieldName.equals("default_max_biking_heart_rate") //                                //$NON-NLS-1$
                     || fieldName.equals("default_max_heart_rate") //                                       //$NON-NLS-1$
                     || fieldName.equals("first_lap_index") //                                              //$NON-NLS-1$
                     || fieldName.equals("hr_calc_type") //                                                 //$NON-NLS-1$
                     || fieldName.equals("hr_setting") //                                                   //$NON-NLS-1$
                     || fieldName.equals("time_in_hr_zone") //                                     9.0 s    //$NON-NLS-1$
                     || fieldName.equals("threshold_heart_rate") //                                  0      //$NON-NLS-1$
                     || fieldName.equals("total_training_effect") //                               3.0      //$NON-NLS-1$
                     
               // Other                                                                               

                     || fieldName.equals("nec_lat") //                                                      //$NON-NLS-1$
                     || fieldName.equals("nec_long") //                                                     //$NON-NLS-1$
                     || fieldName.equals("swc_lat") //                                                      //$NON-NLS-1$
                     || fieldName.equals("swc_long") //                                                     //$NON-NLS-1$

                     || fieldName.equals("active_time_zone") //                                             //$NON-NLS-1$
                     || fieldName.equals("local_timestamp") //                                              //$NON-NLS-1$
                     || fieldName.equals("time_created") //                                                 //$NON-NLS-1$
                     || fieldName.equals("time_offset") //                                                  //$NON-NLS-1$
                     || fieldName.equals("time_zone_offset") //                                             //$NON-NLS-1$
                     || fieldName.equals("utc_offset") //                                                   //$NON-NLS-1$

                     || fieldName.equals("age") //                                                          //$NON-NLS-1$
                     || fieldName.equals("gender") //                                                       //$NON-NLS-1$
                     || fieldName.equals("height") //                                                       //$NON-NLS-1$
                     || fieldName.equals("weight") //                                                       //$NON-NLS-1$
                     || fieldName.equals("language") //                                                     //$NON-NLS-1$

                     || fieldName.equals("dist_setting") //                                                 //$NON-NLS-1$
                     || fieldName.equals("elev_setting") //                                                 //$NON-NLS-1$
                     || fieldName.equals("position_setting") //                                             //$NON-NLS-1$
                     || fieldName.equals("speed_setting") //                                                //$NON-NLS-1$
                     || fieldName.equals("temperature_setting") //                                          //$NON-NLS-1$
                     || fieldName.equals("weight_setting") //                                               //$NON-NLS-1$

                     || fieldName.equals("unknown") //                                                      //$NON-NLS-1$
               //
               ) {
                  continue;
               }

               final long javaTime = (garminTimestamp * 1000) + com.garmin.fit.DateTime.OFFSET;

               System.out.println(

                     String.format("" //$NON-NLS-1$

                           + "[%s]" //$NON-NLS-1$

               // time
                           + " %-42s %d  %s  " //$NON-NLS-1$

               // field
                           + " %-5d %-30s %20s %s", //$NON-NLS-1$

                           FitDataReader.class.getSimpleName(),

                           TimeTools.getZonedDateTime(javaTime), //       show readable date/time
                           javaTime / 1000, //                           java time in s
                           Long.toString(garminTimestamp), //            garmin timestamp

                           field.getNum(),
                           fieldName,
                           field.getValue(),
                           field.getUnits()));
            }
         }
      });
   }

   @Override
   public String buildFileNameFromRawData(final String rawDataFileName) {
      return null;
   }

   @Override
   public boolean checkStartSequence(final int byteIndex, final int newByte) {
      return false;
   }

   @Override
   public String getDeviceModeName(final int modeId) {
      return null;
   }

   @Override
   public SerialParameters getPortParameters(final String portName) {
      return null;
   }

   @Override
   public int getStartSequenceSize() {
      return 0;
   }

   @Override
   public int getTransferDataSize() {
      return 0;
   }

   @Override
   public boolean processDeviceData(final String importFilePath,
                                    final DeviceData deviceData,
                                    final HashMap<Long, TourData> alreadyImportedTours,
                                    final HashMap<Long, TourData> newlyImportedTours) {

      boolean returnValue = false;

      try (FileInputStream fileInputStream = new FileInputStream(importFilePath)) {

         final MesgBroadcaster fitBroadcaster = new MesgBroadcaster(new Decode());

         final FitData fitData = new FitData(
               this,
               importFilePath,
               alreadyImportedTours,
               newlyImportedTours);

         // setup all fit listeners

         fitBroadcaster.addListener(new MesgListener_Activity(fitData));
         fitBroadcaster.addListener(new MesgListener_BikeProfile(fitData));
         fitBroadcaster.addListener(new MesgListener_DeviceInfo(fitData));
         fitBroadcaster.addListener(new MesgListener_Event(fitData));
         fitBroadcaster.addListener(new MesgListener_FileCreator(fitData));
         fitBroadcaster.addListener(new MesgListener_FileId(fitData));
         fitBroadcaster.addListener(new MesgListener_Hr(fitData));
         fitBroadcaster.addListener(new MesgListener_Lap(fitData));
         fitBroadcaster.addListener(new MesgListener_Length(fitData));
         fitBroadcaster.addListener(new MesgListener_Record(fitData));
         fitBroadcaster.addListener(new MesgListener_Session(fitData));

         if (_isLogging_FitData) {

            // show debug info

            System.out.println();
            System.out.println();
            System.out.println(
                  (System.currentTimeMillis() + " [" + getClass().getSimpleName() + "]") //$NON-NLS-1$ //$NON-NLS-2$
                        + (" \t" + importFilePath)); //$NON-NLS-1$
            System.out.println();
            System.out.println(String.format(//

                  "%-15s %-66s %-5s %-30s %20s %s", //$NON-NLS-1$

                  "Java", //$NON-NLS-1$
                  "Timestamp", //$NON-NLS-1$
                  "Num", //$NON-NLS-1$
                  "Name", //$NON-NLS-1$
                  "Value", //$NON-NLS-1$
                  "Units" //$NON-NLS-1$

            ));

            System.out.println();

            addDebugLogListener(fitBroadcaster);
         }

         fitBroadcaster.run(fileInputStream);

         fitData.finalizeTour();

         returnValue = true;

      } catch (final IOException e) {
         TourLogManager.logError_CannotReadDataFile(importFilePath, e);
      }

      return returnValue;
   }

   @Override
   public boolean validateRawData(final String fileName) {

      boolean returnValue = false;
      FileInputStream fis = null;

      try {

         fis = new FileInputStream(fileName);
         returnValue = new Decode().checkFileIntegrity(fis);

         if (returnValue) {

            // log version if not yet done

            if (_isVersionLogged == false) {

               TourLogManager.logInfo(
                     String.format(
                           "FIT SDK %d.%d", //$NON-NLS-1$
                           Fit.PROFILE_VERSION_MAJOR,
                           Fit.PROFILE_VERSION_MINOR));

               _isVersionLogged = true;
            }

         } else {

            TourLogManager.logError(
                  String.format(
                        "FIT checkFileIntegrity failed '%s' - FIT SDK %d.%d", //$NON-NLS-1$
                        fileName,
                        Fit.PROFILE_VERSION_MAJOR,
                        Fit.PROFILE_VERSION_MINOR));
         }

      } catch (final FileNotFoundException e) {
         TourLogManager.logError_CannotReadDataFile(fileName, e);
      } catch (final FitRuntimeException e) {
         TourLogManager.logEx(String.format("Invalid data file '%s'", fileName), e); //$NON-NLS-1$
      } finally {
         IOUtils.closeQuietly(fis);
      }

      return returnValue;
   }

}
