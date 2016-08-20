/*******************************************************************************
 * Copyright (C) 2005, 2016 Wolfgang Schramm and Contributors
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
package net.tourbook.common.time;

import static java.time.temporal.ChronoField.DAY_OF_WEEK;
import gnu.trove.map.hash.TIntObjectHashMap;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Year;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.FormatStyle;
import java.time.format.TextStyle;
import java.time.temporal.ValueRange;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import net.tourbook.common.CommonActivator;
import net.tourbook.common.UI;
import net.tourbook.common.preferences.ICommonPreferences;

import org.eclipse.jface.preference.IPreferenceStore;

import com.skedgo.converter.TimezoneMapper;

public class TimeTools {

	private static final String						ZERO_0						= ":0";											//$NON-NLS-1$
	private static final String						ZERO_00_00					= "+00:00";										//$NON-NLS-1$
	private static final String						ZERO_00_00_DEFAULT			= ZERO_00_00 + '*';

	/**
	 * Cached time zone labels.
	 */
	private static final TIntObjectHashMap<String>	_timeZoneOffsetLabels		= new TIntObjectHashMap<>();

	/*
	 * Copied from java.time.LocalTime
	 */
//	/** Hours per day. */
//	private static final int						HOURS_PER_DAY			= 24;
	/** Minutes per hour. */
	private static final int						MINUTES_PER_HOUR			= 60;
//	/** Minutes per day. */
//	private static final int						MINUTES_PER_DAY			= MINUTES_PER_HOUR * HOURS_PER_DAY;
	/** Seconds per minute. */
	private static final int						SECONDS_PER_MINUTE			= 60;
	/** Seconds per hour. */
	private static final int						SECONDS_PER_HOUR			= SECONDS_PER_MINUTE * MINUTES_PER_HOUR;
//	/** Seconds per day. */
//	private static final int						SECONDS_PER_DAY			= SECONDS_PER_HOUR * HOURS_PER_DAY;

	public static final DateTimeFormatter			dateFormatter_Full			= DateTimeFormatter
																						.ofLocalizedDate(FormatStyle.FULL);
	public static final DateTimeFormatter			dateTimeFormatter_Medium	= DateTimeFormatter
																						.ofLocalizedDateTime(FormatStyle.MEDIUM);
	public static final DateTimeFormatter			timeFormatter_Medium		= DateTimeFormatter
																						.ofLocalizedTime(FormatStyle.MEDIUM);

	final static IPreferenceStore					_prefStoreCommon			= CommonActivator.getPrefStore();

	private static ArrayList<TimeZoneData>			_allSortedTimeZones;

	private static boolean							_isUseTimeZone;
	private static ZoneId							_defaultTimeZoneId;

	/**
	 * Calendar week which is defined in the preferences.
	 */
	public static WeekFields						calendarWeek;

	/**
	 * Contains the short weekday strings. For example: "Sun", "Mon", etc.
	 */
	public static String[]							weekDays_Short;

	/**
	 * Contails the full text, typically the full description. For example, day-of-week Monday might
	 * output "Monday".
	 */
	public static String[]							weekDays_Full;

	private static LocalDate						_dateToGetNumOfWeeks		= LocalDate.of(2000, 5, 5);

	static {

		_isUseTimeZone = _prefStoreCommon.getBoolean(ICommonPreferences.TIME_ZONE_IS_USE_TIME_ZONE);
		_defaultTimeZoneId = ZoneId.of(_prefStoreCommon.getString(ICommonPreferences.TIME_ZONE_LOCAL_ID));

		/*
		 * Set calendar week
		 */
		final int firstDayOfWeek = _prefStoreCommon.getInt(//
				ICommonPreferences.CALENDAR_WEEK_FIRST_DAY_OF_WEEK);

		final int minimalDaysInFirstWeek = _prefStoreCommon.getInt(//
				ICommonPreferences.CALENDAR_WEEK_MIN_DAYS_IN_FIRST_WEEK);

		setCalendarWeek(firstDayOfWeek, minimalDaysInFirstWeek);

		/*
		 * Create week day names. Found no better solution, the old API contained
		 * "DateFormatSymbols.getInstance().getShortWeekdays()"
		 */
		final DateTimeFormatter weekDayFormatter_Short = new DateTimeFormatterBuilder()//
				.appendText(DAY_OF_WEEK, TextStyle.SHORT)
				.toFormatter();

		final DateTimeFormatter weekDayFormatter_Full = new DateTimeFormatterBuilder()//
				.appendText(DAY_OF_WEEK, TextStyle.FULL)
				.toFormatter();

		weekDays_Short = new String[] {

			weekDayFormatter_Short.format(DayOfWeek.MONDAY),
			weekDayFormatter_Short.format(DayOfWeek.TUESDAY),
			weekDayFormatter_Short.format(DayOfWeek.WEDNESDAY),
			weekDayFormatter_Short.format(DayOfWeek.THURSDAY),
			weekDayFormatter_Short.format(DayOfWeek.FRIDAY),
			weekDayFormatter_Short.format(DayOfWeek.SATURDAY),
			weekDayFormatter_Short.format(DayOfWeek.SUNDAY) //
		};

		weekDays_Full = new String[] {

			weekDayFormatter_Full.format(DayOfWeek.MONDAY),
			weekDayFormatter_Full.format(DayOfWeek.TUESDAY),
			weekDayFormatter_Full.format(DayOfWeek.WEDNESDAY),
			weekDayFormatter_Full.format(DayOfWeek.THURSDAY),
			weekDayFormatter_Full.format(DayOfWeek.FRIDAY),
			weekDayFormatter_Full.format(DayOfWeek.SATURDAY),
			weekDayFormatter_Full.format(DayOfWeek.SUNDAY) //
		};
	}

	/**
	 * @return Returns a list with all available time zones which are sorted by zone offset, zone id
	 *         and zone name key.
	 */
	public static ArrayList<TimeZoneData> getAllTimeZones() {

		if (_allSortedTimeZones == null) {

			final ArrayList<TimeZoneData> sortedTimeZones = new ArrayList<>();

			for (final String rawZoneId : ZoneId.getAvailableZoneIds()) {

				final ZoneId zoneId = ZoneId.of(rawZoneId);
				final ZonedDateTime zonedDateTime = ZonedDateTime.now(zoneId);

				final ZoneOffset zoneOffset = zonedDateTime.getOffset();
				final int utcTimeZoneSeconds = zoneOffset.getTotalSeconds();

				final String label = printOffset(utcTimeZoneSeconds, false) + UI.SPACE4 + zoneId.getId();

				final TimeZoneData timeZone = new TimeZoneData();

				timeZone.label = label;
				timeZone.zoneId = zoneId.getId();
				timeZone.zoneOffsetSeconds = utcTimeZoneSeconds;

				sortedTimeZones.add(timeZone);
			}

			Collections.sort(sortedTimeZones, new Comparator<TimeZoneData>() {

				@Override
				public int compare(final TimeZoneData tz1, final TimeZoneData tz2) {

					int result;

					result = tz1.zoneId.compareTo(tz2.zoneId);

					if (result == 0) {
						result = tz1.zoneOffsetSeconds - tz2.zoneOffsetSeconds;
					}

					return result;
				}
			});

			_allSortedTimeZones = sortedTimeZones;
		}

		return _allSortedTimeZones;
	}

	/**
	 * @return Returns the time zone offset for the default time zone.
	 */
	public static String getDefaultTimeZoneOffset() {

		final ZonedDateTime zdt = ZonedDateTime.now();
		final int tzOffset = zdt.getOffset().getTotalSeconds();

		return printOffset(tzOffset, false);
	}

	/**
	 * @param year
	 * @return Returns the number of days in a year
	 */
	public static int getNumberOfDaysWithYear(final int year) {

		return Year.of(year).length();
	}

	/**
	 * @param year
	 * @return Returns the number of weeks in a year.
	 */
	public static int getNumberOfWeeksWithYear(final int year) {

		/*
		 * The date MUST not be in the first or last week of the year, this is very tricky to get
		 * the number of weeks in a year, found in the www.
		 */
		final LocalDate date = _dateToGetNumOfWeeks.withYear(year);
		final ValueRange range = date.range(calendarWeek.weekOfWeekBasedYear());

		final long numOfWeeks = range.getMaximum();

		return (int) numOfWeeks;
	}

	/**
	 * @param timeZoneId
	 * @return Returns the timezone for the ID or <code>null</code> when not available.
	 */
	public static TimeZoneData getTimeZone(final String timeZoneId) {

		final ArrayList<TimeZoneData> allTimeZones = getAllTimeZones();

		for (final TimeZoneData timeZone : allTimeZones) {

			if (timeZone.zoneId.equals(timeZoneId)) {
				return timeZone;
			}
		}

		return null;
	}

	/**
	 * @param latitude
	 * @param longitude
	 * @return Returns the time zone index in {@link #getAllTimeZones()}, when latitude is
	 *         {@link Double#MIN_VALUE} then the time zone index for the default time zone is
	 *         returned.
	 */
	public static int getTimeZoneIndex(final double latitude, final double longitude) {

		TimeZoneData timeZoneData = null;

		if (latitude != Double.MIN_VALUE) {

			final String timeZoneIdFromLatLon = TimezoneMapper.latLngToTimezoneString(latitude, longitude);
			final TimeZoneData timeZoneFromLatLon = TimeTools.getTimeZone(timeZoneIdFromLatLon);

			timeZoneData = timeZoneFromLatLon;
		}

		if (timeZoneData == null) {

			// use default
			timeZoneData = TimeTools.getTimeZone(_defaultTimeZoneId.getId());
		}

		return getTimeZoneIndex(timeZoneData.zoneId);
	}

	/**
	 * @param timeZoneId
	 * @return Returns the timezone index for the timezone ID or -1 when not available.
	 */
	public static int getTimeZoneIndex(final String timeZoneId) {

		final ArrayList<TimeZoneData> allTimeZones = getAllTimeZones();

		for (int timeZoneIndex = 0; timeZoneIndex < allTimeZones.size(); timeZoneIndex++) {

			final TimeZoneData timeZone = allTimeZones.get(timeZoneIndex);

			if (timeZone.zoneId.equals(timeZoneId)) {
				return timeZoneIndex;
			}
		}

		return -1;
	}

	/**
	 * Creates a tour date time with the tour time zone.
	 * 
	 * @param epochMilli
	 *            The number of milliseconds from 1970-01-01T00:00:00Z
	 * @param dbTimeZoneId
	 *            Time zone ID or <code>null</code> when the time zone ID is not defined, then the
	 *            local time zone is used.
	 * @return
	 */
	public static TourDateTime getTourDateTime(final long epochMilli, final String dbTimeZoneId) {

		final Instant tourStartInstant = Instant.ofEpochMilli(epochMilli);

		final boolean isDefaultZone = dbTimeZoneId == null;

		final ZoneId zoneId = isDefaultZone //
				? _defaultTimeZoneId
				: ZoneId.of(dbTimeZoneId);

		ZonedDateTime tourZonedDateTime;
		String timeZoneOffsetLabel = null;

		if (_isUseTimeZone) {

			tourZonedDateTime = ZonedDateTime.ofInstant(tourStartInstant, zoneId);

			final ZonedDateTime tourDateTimeWithDefaultZoneId = tourZonedDateTime
					.withZoneSameInstant(_defaultTimeZoneId);

			final int tourOffset = tourZonedDateTime.getOffset().getTotalSeconds();
			final int defaultOffset = tourDateTimeWithDefaultZoneId.getOffset().getTotalSeconds();

			final int offsetDiff = tourOffset - defaultOffset;
			timeZoneOffsetLabel = printOffset(offsetDiff, isDefaultZone);

		} else {

			tourZonedDateTime = ZonedDateTime.ofInstant(tourStartInstant, ZoneId.systemDefault());

			timeZoneOffsetLabel = printOffset(0, true);
		}

		// set an offset to have the index in the week array
		final int weekDayIndex = tourZonedDateTime.getDayOfWeek().getValue() - 1;

		return new TourDateTime(tourZonedDateTime, timeZoneOffsetLabel, weekDays_Short[weekDayIndex]);
	}

	/*
	 * Copied (and modified) from java.time.ZoneOffset.buildId(int)
	 */
	/**
	 * @param timeZoneOffset
	 *            Time zone offset in seconds
	 * @param isDefaultZone
	 *            When <code>true</code>, then a star is added to the offset value to indicate the
	 *            default zone
	 * @return Returns a time offset string
	 */
	private static String printOffset(final int timeZoneOffset, final boolean isDefaultZone) {

		if (timeZoneOffset == 0) {

			if (isDefaultZone) {

				return ZERO_00_00_DEFAULT;

			} else {

				return ZERO_00_00;
			}

		} else {

			String tzText = _timeZoneOffsetLabels.get(timeZoneOffset);

			if (tzText == null) {

				// create text

				final int absTotalSeconds = Math.abs(timeZoneOffset);
				final int absHours = absTotalSeconds / SECONDS_PER_HOUR;
				final int absMinutes = (absTotalSeconds / SECONDS_PER_MINUTE) % MINUTES_PER_HOUR;

				final StringBuilder sb = new StringBuilder()
						.append(timeZoneOffset < 0 ? '-' : '+')
						.append(absHours < 10 ? '0' : UI.EMPTY_STRING)
						.append(absHours)
						.append(absMinutes < 10 ? ZERO_0 : ':')
						.append(absMinutes);

				final int absSeconds = absTotalSeconds % SECONDS_PER_MINUTE;

				if (absSeconds != 0) {
					sb.append(absSeconds < 10 ? ZERO_0 : ':').append(absSeconds);
				}

				tzText = sb.toString();

				if (isDefaultZone) {
					// mark the default zone with a star
					tzText += UI.SYMBOL_STAR;
				}

				_timeZoneOffsetLabels.put(timeZoneOffset, tzText);
			}

			return tzText;
		}
	}

	public static void setCalendarWeek(final int firstDayOfWeek, final int minimalDaysInFirstWeek) {

		final DayOfWeek dow = DayOfWeek.SUNDAY.plus(firstDayOfWeek - 1);

		calendarWeek = WeekFields.of(dow, minimalDaysInFirstWeek);
	}

	public static void setDefaultTimeZoneOffset(final boolean isUseTimeZone, final String timeZoneId) {

		_isUseTimeZone = isUseTimeZone;
		_defaultTimeZoneId = ZoneId.of(timeZoneId);
	}

}
