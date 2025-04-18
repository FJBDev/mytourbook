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
package net.tourbook.common.weather;

import net.tourbook.common.Messages;
import net.tourbook.common.UI;
import net.tourbook.common.color.ThemeUtil;

import org.eclipse.swt.graphics.Color;

public interface IWeather {

   public static final String   WEATHER_ID_CLEAR                = "weather-sunny";             //$NON-NLS-1$
   public static final String   WEATHER_ID_FOG                  = "weather-fog";               //$NON-NLS-1$
   public static final String   WEATHER_ID_PART_CLOUDS          = "weather-cloudy";            //$NON-NLS-1$
   public static final String   WEATHER_ID_OVERCAST             = "weather-clouds";            //$NON-NLS-1$
   public static final String   WEATHER_ID_LIGHTNING            = "weather-lightning";         //$NON-NLS-1$
   public static final String   WEATHER_ID_RAIN                 = "weather-rain";              //$NON-NLS-1$
   public static final String   WEATHER_ID_DRIZZLE              = "weather-drizzle";           //$NON-NLS-1$
   public static final String   WEATHER_ID_SNOW                 = "weather-snow";              //$NON-NLS-1$
   public static final String   WEATHER_ID_SEVERE_WEATHER_ALERT = "weather-severe";            //$NON-NLS-1$
   public static final String   WEATHER_ID_SCATTERED_SHOWERS    = "weather-showers-scattered"; //$NON-NLS-1$

   public static final String   AIRQUALITY_ID_GOOD              = "airquality-good";           //$NON-NLS-1$
   public static final String   AIRQUALITY_ID_FAIR              = "airquality-fair";           //$NON-NLS-1$
   public static final String   AIRQUALITY_ID_MODERATE          = "airquality-moderate";       //$NON-NLS-1$
   public static final String   AIRQUALITY_ID_POOR              = "airquality-poor";           //$NON-NLS-1$
   public static final String   AIRQUALITY_ID_VERYPOOR          = "airquality-verypoor";       //$NON-NLS-1$

   public static final String   windDirectionIsNotDefined       = UI.EMPTY_STRING;

   public static final String[] windDirectionText               = new String[] {

         windDirectionIsNotDefined,

         Messages.Weather_WindDirection_N,
         Messages.Weather_WindDirection_NNE,
         Messages.Weather_WindDirection_NE,
         Messages.Weather_WindDirection_ENE,
         Messages.Weather_WindDirection_E,
         Messages.Weather_WindDirection_ESE,
         Messages.Weather_WindDirection_SE,
         Messages.Weather_WindDirection_SSE,
         Messages.Weather_WindDirection_S,
         Messages.Weather_WindDirection_SSW,
         Messages.Weather_WindDirection_SW,
         Messages.Weather_WindDirection_WSW,
         Messages.Weather_WindDirection_W,
         Messages.Weather_WindDirection_WNW,
         Messages.Weather_WindDirection_NW,
         Messages.Weather_WindDirection_NNW

   };

   /**
    * <pre>
    *
    * Source: Wikipedia
    *
    * Bft Description            km/h        mph         knot
    * 0   Calm                   < 1         < 1         < 1
    * 1   Light air              1.1 - 5.5   1 - 3       1 - 3
    * 2   Light breeze           5.6 - 11    4 - 7       4 - 6
    * 3   Gentle breeze          12 - 19     8 - 12      7 - 10
    * 4   Moderate breeze        20 - 28     13 - 17     11 - 16
    * 5   Fresh breeze           29 - 38     18 - 24     17 - 21
    * 6   Strong breeze          39 - 49     25 - 30     22 - 27
    * 7   High wind, ...         50 - 61     31 - 38     28 - 33
    * 8   Gale, Fresh gale       62 - 74     39 - 46     34 - 40
    * 9   Strong gale            75 - 88     47 - 54     41 - 47
    * 10  Storm[6], Whole gale   89 - 102    55 - 63     48 - 55
    * 11  Violent storm          103 - 117   64 - 72     56 - 63
    * 12  Hurricane-force        >= 118      >= 73       >= 64
    * </pre>
    */
   public static final String[] windSpeedText                   = new String[] {

         Messages.Weather_WindSpeed_Bft00,
         Messages.Weather_WindSpeed_Bft01,
         Messages.Weather_WindSpeed_Bft02,
         Messages.Weather_WindSpeed_Bft03,
         Messages.Weather_WindSpeed_Bft04,
         Messages.Weather_WindSpeed_Bft05,
         Messages.Weather_WindSpeed_Bft06,
         Messages.Weather_WindSpeed_Bft07,
         Messages.Weather_WindSpeed_Bft08,
         Messages.Weather_WindSpeed_Bft09,
         Messages.Weather_WindSpeed_Bft10,
         Messages.Weather_WindSpeed_Bft11,
         Messages.Weather_WindSpeed_Bft12

   };

   public static final String[] windSpeedTextShort              = new String[] {

         Messages.Weather_WindSpeed_Bft00_Short,
         Messages.Weather_WindSpeed_Bft01_Short,
         Messages.Weather_WindSpeed_Bft02_Short,
         Messages.Weather_WindSpeed_Bft03_Short,
         Messages.Weather_WindSpeed_Bft04_Short,
         Messages.Weather_WindSpeed_Bft05_Short,
         Messages.Weather_WindSpeed_Bft06_Short,
         Messages.Weather_WindSpeed_Bft07_Short,
         Messages.Weather_WindSpeed_Bft08_Short,
         Messages.Weather_WindSpeed_Bft09_Short,
         Messages.Weather_WindSpeed_Bft10_Short,
         Messages.Weather_WindSpeed_Bft11_Short,
         Messages.Weather_WindSpeed_Bft12_Short

   };

// SET_FORMATTING_OFF

   /**
    * Wind speed in km/h
    */
   public static final int[]    WIND_SPEED_KMH        = new int[] {

         0,       // 0 bft
         5,       // 1 bft
         11,      // 2
         19,      // 3
         28,      // 4
         38,      // 5
         49,      // 6
         61,      // 7
         74,      // 8
         88,      // 9
         102,     // 10
         117,     // 11
         118,     // 12
   };

   public static final int[]    WIND_SPEED_MPH        = new int[] {

         0,       // 0 bft
         3,       // 1 bft
         7,       // 2
         12,      // 3
         17,      // 4
         24,      // 5
         30,      // 6
         38,      // 7
         46,      // 8
         54,      // 9
         63,      // 10
         72,      // 11
         73,      // 12
   };

   public static final int[]    WIND_SPEED_KNOT       = new int[] {

         0,       // 0 bft
         3,       // 1 bft
         6,       // 2
         10,      // 3
         16,      // 4
         21,      // 5
         27,      // 6
         33,      // 7
         40,      // 8
         47,      // 9
         55,      // 10
         63,      // 11
         64       // 12
   };

// SET_FORMATTING_ON

   public static final String cloudIsNotDefined = Messages.Weather_Clouds_IsNotDefined;

   /*
    * cloudText and cloudDbValue must be in synch
    */

   /**
    * Text for the weather, must be in sync with {@link #CLOUD_ICON}
    */
   public static final String[] CLOUD_TEXT                      = new String[] {

         cloudIsNotDefined,

         Messages.Weather_Clouds_Sunny,
         Messages.Weather_Clouds_Cloudy,
         Messages.Weather_Clouds_Clouds,
         Messages.Weather_Clouds_Fog,
         Messages.Weather_Clouds_Drizzle,
         Messages.Weather_Clouds_ScatteredShowers,
         Messages.Weather_Clouds_Rain,
         Messages.Weather_Clouds_Lightning,
         Messages.Weather_Clouds_Snow,
         Messages.Weather_Clouds_SevereWeatherAlert
   };

   /**
    * Icons for the weather, must be in sync with {@link #CLOUD_TEXT}
    */
   public static final String[] CLOUD_ICON                      = new String[] {

         UI.IMAGE_EMPTY_16,

         WEATHER_ID_CLEAR,
         WEATHER_ID_PART_CLOUDS,
         WEATHER_ID_OVERCAST,
         WEATHER_ID_FOG,
         WEATHER_ID_DRIZZLE,
         WEATHER_ID_SCATTERED_SHOWERS,
         WEATHER_ID_RAIN,
         WEATHER_ID_LIGHTNING,
         WEATHER_ID_SNOW,
         WEATHER_ID_SEVERE_WEATHER_ALERT,

   };
   /**
    *
    * Texts for the weather's air quality, must be in sync with
    * {@link #AIR_QUALITY_COLORS_BRIGHT_THEME} and {@link #AIR_QUALITY_COLORS_DARK_THEME}
    *
    * Those texts are displayed in the UI.
    */
   public static final String[] AIR_QUALITY_TEXT                = new String[] {

         Messages.Weather_AirQuality_0_IsNotDefined,

         Messages.Weather_AirQuality_1_Good,
         Messages.Weather_AirQuality_2_Fair,
         Messages.Weather_AirQuality_3_Moderate,
         Messages.Weather_AirQuality_4_Poor,
         Messages.Weather_AirQuality_5_VeryPoor

   };

   /**
    * Ids for the weather's air quality. Those Ids are saved in the db.
    */
   public static final String[] AIR_QUALITY_IDS                 = new String[] {

         UI.EMPTY_STRING,

         AIRQUALITY_ID_GOOD,
         AIRQUALITY_ID_FAIR,
         AIRQUALITY_ID_MODERATE,
         AIRQUALITY_ID_POOR,
         AIRQUALITY_ID_VERYPOOR

   };

   /**
    * Foreground and background colors for the air quality, must be in sync with
    * {@link #AIR_QUALITY_TEXT}
    */
   public static final Color[]  AIR_QUALITY_COLORS_BRIGHT_THEME = new Color[] {

         // not defined
         ThemeUtil.getDefaultForegroundColor_Combo(),
         ThemeUtil.getDefaultBackgroundColor_Combo(),

         // 1 Good - green
         UI.SYS_COLOR_WHITE, new Color(0, 175, 0),

         // 2 Fair - yellow
         UI.SYS_COLOR_BLACK, new Color(255, 255, 0),

         // 3 Moderate - orange
         UI.SYS_COLOR_WHITE, new Color(255, 128, 0),

         // 4 Poor - red
         UI.SYS_COLOR_WHITE, new Color(230, 0, 0),

         // 5 Very poor - pink
         UI.SYS_COLOR_WHITE, new Color(227, 0, 227),
   };

   /**
    * Foreground and background colors for the air quality, must be in sync with
    * {@link #AIR_QUALITY_TEXT}
    */
   public static final Color[]  AIR_QUALITY_COLORS_DARK_THEME   = new Color[] {

         // not defined
//         ThemeUtil.getDefaultForegroundColor_Combo(),
//         ThemeUtil.getDefaultBackgroundColor_Combo(),
         null,
         null,

         // 1 Good - green
         UI.SYS_COLOR_WHITE, new Color(0, 175, 0),

         // 2 Fair - yellow
         UI.SYS_COLOR_BLACK, new Color(227, 227, 0),

         // 3 Moderate - orange
         UI.SYS_COLOR_WHITE, new Color(255, 128, 0),

         // 4 Poor - red
         UI.SYS_COLOR_WHITE, new Color(230, 0, 0),

         // 5 Very poor - pink
         UI.SYS_COLOR_WHITE, new Color(227, 0, 227),
   };

   /**
    * @return Returns the wind speeds for the current measurements
    */
   public static int[] getAllWindSpeeds() {

      if (UI.UNIT_IS_DISTANCE_MILE) {

         return WIND_SPEED_MPH;

      } else if (UI.UNIT_IS_DISTANCE_NAUTICAL_MILE) {

         return WIND_SPEED_KNOT;
      }

      return WIND_SPEED_KMH;
   }
}
