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

package net.tourbook.common.util;

import java.util.TimeZone;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.shredzone.commons.suncalc.SunTimes;

public final class TimeUtils {

   public static DateTime determineSunRiseTimes(final DateTime StartTime,
                                                final double latitude,
                                                final double longitude,
                                                final String timeZoneId) {
      // Because giving a date with a specific hour could result into getting the
      // sunrise of the previous day,
      // we adjust the date to the beginning of the day
      final DateTime beginningOfDay = new DateTime(StartTime.getYear(),
            StartTime.getMonthOfYear(),
            StartTime.getDayOfMonth(),
            0,
            0,
            DateTimeZone.forTimeZone(TimeZone.getTimeZone(timeZoneId)));

      final SunTimes times = SunTimes.compute().on(beginningOfDay.toDate()).at(latitude, longitude).execute();

      final DateTime sunriseTime = new DateTime(times.getRise())
            .withZone(DateTimeZone.forTimeZone(TimeZone.getTimeZone(timeZoneId)));

      return sunriseTime;
   }

   public static DateTime determineSunsetTimes(final DateTime StartTime,
                                               final double latitude,
                                               final double longitude,
                                               final String timeZoneId) {
      // Because giving a date with a specific hour could result into getting the
      // sunrise of the previous day,
      // we adjust the date to the beginning of the day
      final DateTime beginningOfDay = new DateTime(StartTime.getYear(),
            StartTime.getMonthOfYear(),
            StartTime.getDayOfMonth(),
            0,
            0,
            DateTimeZone.forTimeZone(TimeZone.getTimeZone(timeZoneId)));

      final SunTimes times = SunTimes.compute().on(beginningOfDay.toDate()).at(latitude, longitude)
            .execute();

      final DateTime sunsetTime = new DateTime(times.getSet())
            .withZone(DateTimeZone.forTimeZone(TimeZone.getTimeZone(timeZoneId)));
      return sunsetTime;

   }
}
