/*******************************************************************************
 * Copyright (C) 2005, 2024 Wolfgang Schramm and Contributors
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
package net.tourbook.chart;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Formatter;

import net.tourbook.common.UI;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;

public class Util {

   private static final String        SYMBOL_DASH     = "-";                              //$NON-NLS-1$
   public static final String         DASH_WITH_SPACE = " - ";                            //$NON-NLS-1$

   private static final String        FORMAT_MM_SS    = "%d:%02d";                        //$NON-NLS-1$
   private static final String        FORMAT_HH_MM    = "%d:%02d";                        //$NON-NLS-1$
   private static final String        FORMAT_HH_MM_SS = "%d:%02d:%02d";                   //$NON-NLS-1$

   private static final NumberFormat  _nf0            = NumberFormat.getNumberInstance();
   private static final NumberFormat  _nf1            = NumberFormat.getNumberInstance();
   private static final NumberFormat  _nf2            = NumberFormat.getNumberInstance();
   private static final NumberFormat  _nf3            = NumberFormat.getNumberInstance();
   private static final DecimalFormat _nfE            = new DecimalFormat("0.###E0");     //$NON-NLS-1$
   private static final NumberFormat  _df             = DecimalFormat.getNumberInstance();

   static {
      _nf0.setMinimumFractionDigits(0);
      _nf0.setMaximumFractionDigits(0);

      _nf1.setMinimumFractionDigits(1);
      _nf1.setMaximumFractionDigits(1);

      _nf2.setMinimumFractionDigits(2);
      _nf2.setMaximumFractionDigits(2);

      _nf3.setMinimumFractionDigits(3);
      _nf3.setMaximumFractionDigits(3);

      _df.setMinimumFractionDigits(0);
      _df.setMaximumFractionDigits(6);
   }

   private static StringBuilder _sbFormatter = new StringBuilder();
   private static Formatter     _formatter   = new Formatter(_sbFormatter);

   /**
    * Checks if an image can be reused, this is true if the image exists and has the same size
    *
    * @param newWidth
    * @param newHeight
    *
    * @return
    */
   public static boolean canReuseImage(final Image image, final Rectangle rect) {

      // check if we could reuse the existing image

      if ((image == null) || image.isDisposed()) {
         return false;
      } else {
         // image exist, check for the bounds
         final Rectangle oldBounds = image.getBounds();

         if (!((oldBounds.width == rect.width) && (oldBounds.height == rect.height))) {
            return false;
         }
      }
      return true;
   }

   /**
    * creates a new image
    *
    * @param display
    * @param image
    *           image which will be disposed if the image is not null
    * @param rect
    *
    * @return returns a new created image
    */
   public static Image createImage(final Display display, final Image image, final Rectangle rect) {

      if ((image != null) && !image.isDisposed()) {
         image.dispose();
      }

      return new Image(display, rect.width, rect.height);
   }

   public static String format_hh_mm(final long time) {

      _sbFormatter.setLength(0);

      if (time < 0) {
         _sbFormatter.append(SYMBOL_DASH);
      }

      final long timeAbsolute = time < 0 ? 0 - time : time;

      return _formatter.format(FORMAT_HH_MM,

            (timeAbsolute / 3600),
            (timeAbsolute % 3600) / 60)
            .toString();
   }

   public static String format_hh_mm_ss(final long time) {

      _sbFormatter.setLength(0);

      if (time < 0) {
         _sbFormatter.append(SYMBOL_DASH);
      }

      final long timeAbsolute = time < 0 ? 0 - time : time;

      return _formatter
            .format(
                  FORMAT_HH_MM_SS,
                  (timeAbsolute / 3600),
                  (timeAbsolute % 3600) / 60,
                  (timeAbsolute % 3600) % 60)
            .toString();
   }

   public static String format_hh_mm_ss_Optional(final long value) {

      boolean isShowSeconds = true;
      final int seconds = (int) ((value % 3600) % 60);

      if (isShowSeconds && seconds == 0) {
         isShowSeconds = false;
      }

      String valueText;
      if (isShowSeconds) {

         // show seconds only when they are available

         valueText = format_hh_mm_ss(value);
      } else {
         valueText = format_hh_mm(value);
      }

      return valueText;
   }

   public static String format_mm_ss(final long time) {

      _sbFormatter.setLength(0);

      if (time < 0) {
         _sbFormatter.append(SYMBOL_DASH);
      }

      final long timeAbsolute = time < 0 ? 0 - time : time;

      return _formatter
            .format(
                  FORMAT_MM_SS,
                  (timeAbsolute / 60),
                  (timeAbsolute % 60))
            .toString();
   }

   public static String formatNumber(final double rawValue,
                                     final int unitType,
                                     final int valueDivisor,
                                     final int valueDecimals) {

      String valueText;

      if (unitType == ChartDataSerie.AXIS_UNIT_NUMBER) {

         final double divValue = rawValue / valueDivisor;

         if (valueDecimals == 0 || divValue % 1 == 0) {

            valueText = _nf0.format(divValue);

         } else {

            switch (valueDecimals) {
            case 2:
               valueText = _nf2.format(divValue);
               break;

            case 3:
               valueText = _nf3.format(divValue);
               break;

            default:
               valueText = _nf1.format(divValue);
               break;
            }
         }

      } else {

         valueText = Util.formatValue(rawValue, unitType, valueDivisor, true, -1);
      }

      return valueText;
   }

   public static String formatValue(final double value,
                                    final int unitType,
                                    final float divisor,
                                    boolean isShowSeconds,
                                    final double graphUnit) {

      String valueText = UI.EMPTY_STRING;

      // format the unit label
      switch (unitType) {
      case ChartDataSerie.AXIS_UNIT_NUMBER:
      case ChartDataSerie.X_AXIS_UNIT_NUMBER_CENTER:

         final double divValue = value / divisor;

         if (divValue % 1 == 0) {

            // no decimal -> show whole value

            if (divValue <= -1_000_000 || divValue >= 1_000_000) {

               // format very large/small values

               valueText = _nfE.format(divValue);

            } else {

               valueText = _nf0.format(divValue);
            }

         } else {

            if (graphUnit == -1) {

               // use original formatting

               valueText = _nf1.format(divValue);

            } else {

               // show more decimals when necessary

               if (graphUnit < 0.1) {

                  // show 2 decimals

                  valueText = _nf2.format(divValue);

                  // truncate trailing 0

                  final int numCharacters = valueText.length();

                  if (valueText.charAt(numCharacters - 1) == '0') {
                     valueText = valueText.substring(0, numCharacters - 1);
                  }

               } else {

                  // show 1 decimal

                  valueText = _nf1.format(divValue);
               }

            }
         }
         break;

      case ChartDataSerie.AXIS_UNIT_HOUR_MINUTE:
      case ChartDataSerie.AXIS_UNIT_HOUR_MINUTE_24H:

         valueText = format_hh_mm((long) value);

         break;

      case ChartDataSerie.AXIS_UNIT_MINUTE_SECOND:
         valueText = format_mm_ss((long) value);
         break;

      case ChartDataSerie.AXIS_UNIT_HOUR_MINUTE_OPTIONAL_SECOND:

         // seconds are displayed when they are not 0

         final int seconds = (int) ((value % 3600) % 60);
         if (isShowSeconds && seconds == 0) {
            isShowSeconds = false;
         }

         // !!! the missing break; is intentional !!!

      case ChartDataSerie.AXIS_UNIT_HOUR_MINUTE_SECOND:

         if (isShowSeconds) {

            // show seconds only when they are available

            valueText = format_hh_mm_ss((long) value);
         } else {
            valueText = format_hh_mm((long) value);
         }
         break;

      case ChartDataSerie.X_AXIS_UNIT_DAY:
         valueText = _nf1.format(value);
         break;

      default:
         break;
      }

      return valueText;
   }

   /**
    * Formats a value according to the defined unit
    *
    * @param value
    * @param data
    *
    * @return
    */
   public static String formatValue(final int value, final int unitType) {

      return formatValue(value, unitType, 1, false, -1);
   }

}
