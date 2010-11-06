/*******************************************************************************
 * Copyright (C) 2005, 2010  Wolfgang Schramm and Contributors
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
package net.tourbook.device.sporttracks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import net.tourbook.chart.ChartLabel;
import net.tourbook.data.TimeData;
import net.tourbook.data.TourData;
import net.tourbook.data.TourMarker;
import net.tourbook.device.InvalidDeviceSAXException;
import net.tourbook.device.Messages;
import net.tourbook.importdata.TourbookDevice;
import net.tourbook.util.Util;

import org.eclipse.osgi.util.NLS;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class FitLogSAXHandler extends DefaultHandler {

	private static final String				TAG_ACTIVITY				= "Activity";							//$NON-NLS-1$
	//
	private static final String				TAG_TRACK					= "Track";								//$NON-NLS-1$
	private static final String				ATTRIB_TRACK_START_TIME		= "StartTime";							//$NON-NLS-1$
	//
	private static final String				TAG_TRACK_PT				= "pt";								//$NON-NLS-1$
	private static final String				ATTRIB_PT_TM				= "tm";								//$NON-NLS-1$
	private static final String				ATTRIB_PT_LAT				= "lat";								//$NON-NLS-1$
	private static final String				ATTRIB_PT_LON				= "lon";								//$NON-NLS-1$
	private static final String				ATTRIB_PT_DIST				= "dist";								//$NON-NLS-1$
	private static final String				ATTRIB_PT_ELE				= "ele";								//$NON-NLS-1$
	private static final String				ATTRIB_PT_HR				= "hr";								//$NON-NLS-1$
	private static final String				ATTRIB_PT_CADENCE			= "cadence";							//$NON-NLS-1$
	private static final String				ATTRIB_PT_POWER				= "power";								//$NON-NLS-1$
	//
	private static final String				TAG_ACTIVITY_NAME			= "Name";
	private static final String				TAG_ACTIVITY_NOTES			= "Notes";
	//
	private static final String				TAG_ACTIVITY_CALORIES		= "Calories";
	private static final String				ATTRIB_CALORIES_TOTALCAL	= "TotalCal";
	//
	private static final String				TAG_ACTIVITY_LOCATION		= "Location";
	private static final String				ATTRIB_LOCATION_NAME		= "Name";
	//
	private static final String				TAG_ACTIVITY_WEATHER		= "Weather";
	private static final String				ATTRIB_WEATHER_TEMP			= "Temp";
	private static final String				ATTRIB_WEATHER_CONDITIONS	= "Conditions";
	//
	private static final String				TAG_LAPS					= "Laps";
	private static final String				TAG_LAP						= "Lap";
	private static final String				ATTRIB_LAP_START_TIME		= "StartTime";
	//
	private String							_importFilePath;
	private HashMap<Long, TourData>			_tourDataMap;
	private TourbookDevice					_device;
	private Activity						_currentActivity;

	private double							_prevLatitude;
	private double							_prevLongitude;
	private double							_distanceAbsolute;
	private boolean							_isImported					= false;

	private boolean							_isInActivity;
	private boolean							_isInTrack;
	private boolean							_isInName;

	private boolean							_isInNotes;
	private boolean							_isInWeather;
	private StringBuilder					_characters					= new StringBuilder(100);

	private boolean							_isInLaps;
	private static final DateTimeFormatter	_dtParser					= ISODateTimeFormat.dateTimeParser();

	private class Activity {

		private ArrayList<TimeData>	timeSlices			= new ArrayList<TimeData>();
		private ArrayList<Lap>		laps				= new ArrayList<Lap>();

		private DateTime			tourDateTime;
		private long				tourStartTime		= Long.MIN_VALUE;

		private String				location;
		private String				name;
		private String				notes;
		private int					calories			= Integer.MIN_VALUE;

		private String				weatherText;
		private String				weatherConditions;
		private float				weatherTemperature	= Float.MIN_VALUE;
	}

	private class Lap {

		private long	lapStartTime;
	}

	public FitLogSAXHandler(final FitLogDeviceDataReader device,
							final String importFilePath,
							final HashMap<Long, TourData> tourDataMap) {

		_importFilePath = importFilePath;
		_tourDataMap = tourDataMap;
		_device = device;
	}

	@Override
	public void characters(final char[] chars, final int startIndex, final int length) throws SAXException {

		if (_isInName || _isInNotes || _isInWeather) {

			_characters.append(chars, startIndex, length);
		}
	}

	@Override
	public void endElement(final String uri, final String localName, final String name) throws SAXException {

		/*
		 * get values
		 */
		if (_isInName || _isInNotes || _isInWeather) {
			parseActivity02End(name);
		}

		/*
		 * set state
		 */
		if (name.equals(TAG_TRACK)) {

			_isInTrack = false;

		} else if (name.equals(TAG_LAPS)) {

			_isInLaps = false;

		} else if (name.equals(TAG_ACTIVITY)) {

			// activity/tour ends

			_isInActivity = false;

			finalizeTour();
		}
	}

	private void finalizeTour() {

		// create data object for each tour
		final TourData tourData = new TourData();

		/*
		 * set tour start date/time
		 */
		final DateTime tourStart = _currentActivity.tourDateTime;

		tourData.setStartHour((short) tourStart.getHourOfDay());
		tourData.setStartMinute((short) tourStart.getMinuteOfHour());
		tourData.setStartSecond((short) tourStart.getSecondOfMinute());

		tourData.setStartYear((short) tourStart.getYear());
		tourData.setStartMonth((short) tourStart.getMonthOfYear());
		tourData.setStartDay((short) tourStart.getDayOfMonth());

		tourData.setWeek(tourStart);

		tourData.setDeviceTimeInterval((short) -1);

		/*
		 * weather
		 */
		tourData.setWeather(_currentActivity.weatherText);

		final float weatherTemperature = _currentActivity.weatherTemperature;
		if (weatherTemperature != Float.MIN_VALUE) {
			tourData.setTemperatureScale(TourbookDevice.TEMPERATURE_SCALE);
			tourData.setAvgTemperature((int) (weatherTemperature * TourbookDevice.TEMPERATURE_SCALE));
		}

//		if (_currentActivity.weatherConditions != null) {
//
//			final int cloudIndex = _comboClouds.getSelectionIndex();
//			String cloudValue = IWeather.cloudIcon[cloudIndex];
//			if (cloudValue.equals(UI.IMAGE_EMPTY_16)) {
//				// replace invalid cloud key
//				cloudValue = UI.EMPTY_STRING;
//			}
//
////		<xs:enumeration value="Clear"/>
////		<xs:enumeration value="ScatterClouds"/>
////		<xs:enumeration value="PartClouds"/>
////		<xs:enumeration value="Overcast"/>
////		<xs:enumeration value="MostClouds"/>
////		<xs:enumeration value="Clouds"/>
////		<xs:enumeration value="ChanceRain"/>
////		<xs:enumeration value="LightDrizzle"/>
////		<xs:enumeration value="LightRain"/>
////		<xs:enumeration value="Rain"/>
////		<xs:enumeration value="HeavyRain"/>
////		<xs:enumeration value="ChanceThunder"/>
////		<xs:enumeration value="Thunder"/>
////		<xs:enumeration value="Snow"/>
////		<xs:enumeration value="Haze"/>
//		}
//		tourData.setWeatherClouds(cloudValue);

		tourData.importRawDataFile = _importFilePath;
		tourData.setTourImportFilePath(_importFilePath);

		tourData.setCalories(_currentActivity.calories);
//		tourData.setRestPulse(_currentExercise.restPulse);

		tourData.createTimeSeries(_currentActivity.timeSlices, false);

		finalizeTour10CreateMarkers(tourData);
		tourData.computeAltitudeUpDown();

		// after all data are added, the tour id can be created
		final Long tourId = tourData.createTourId(_device.createUniqueId(tourData, "241683"));

		// check if the tour is already imported
		if (_tourDataMap.containsKey(tourId) == false) {

			tourData.computeTourDrivingTime();
			tourData.computeComputedValues();

			tourData.setDeviceId(_device.deviceId);
			tourData.setDeviceName(_device.visibleName);

			// add new tour to other tours
			_tourDataMap.put(tourId, tourData);
		}

		// cleanup
		_currentActivity.timeSlices.clear();
		_currentActivity.laps.clear();

		_isImported = true;
	}

	private void finalizeTour10CreateMarkers(final TourData tourData) {

		final ArrayList<Lap> _laps = _currentActivity.laps;
		if (_laps.size() == 0) {
			return;
		}

		final int[] timeSerie = tourData.timeSerie;
		if (timeSerie.length == 0) {
			return;
		}

		final Set<TourMarker> tourMarkers = tourData.getTourMarkers();
		final int[] distanceSerie = tourData.distanceSerie;

		final long tourStartTime = _currentActivity.tourStartTime;
		int lapCounter = 1;

		for (final Lap lap : _laps) {

			final long lapRelativeTime = (lap.lapStartTime - tourStartTime) / 1000;
			int serieIndex = 0;

			// get serie index
			for (final int tourRelativeTime : timeSerie) {
				if (tourRelativeTime >= lapRelativeTime) {
					break;
				}
				serieIndex++;
			}

			// check array bounds
			if (serieIndex >= timeSerie.length) {
				serieIndex = timeSerie.length - 1;
			}

			final TourMarker tourMarker = new TourMarker(tourData, ChartLabel.MARKER_TYPE_DEVICE);

			tourMarker.setLabel(Integer.toString(lapCounter));
			tourMarker.setSerieIndex(serieIndex);
			tourMarker.setTime((int) lapRelativeTime);
			tourMarker.setVisualPosition(ChartLabel.VISUAL_HORIZONTAL_ABOVE_GRAPH_CENTERED);

			if (distanceSerie != null) {
				tourMarker.setDistance(distanceSerie[serieIndex]);
			}

			tourMarkers.add(tourMarker);

			lapCounter++;
		}
	}

	private void initTour() {

		_currentActivity = new Activity();

		_distanceAbsolute = 0;

		_prevLatitude = Double.MIN_VALUE;
		_prevLongitude = Double.MIN_VALUE;
	}

	public boolean isImported() {
		return _isImported;
	}

	private void parseActivity01Start(final String name, final Attributes attributes) {

		if (name.equals(TAG_ACTIVITY_CALORIES)) {

			_currentActivity.calories = Util.parseInt(attributes, ATTRIB_CALORIES_TOTALCAL);

		} else if (name.equals(TAG_ACTIVITY_LOCATION)) {

			_currentActivity.location = attributes.getValue(ATTRIB_LOCATION_NAME);

		} else if (name.equals(TAG_ACTIVITY_NAME)) {

			_isInName = true;

		} else if (name.equals(TAG_ACTIVITY_NOTES)) {

			_isInNotes = true;

		} else if (name.equals(TAG_ACTIVITY_WEATHER)) {

			_isInWeather = true;
			_currentActivity.weatherTemperature = Util.parseFloat(attributes, ATTRIB_WEATHER_TEMP);
			_currentActivity.weatherConditions = attributes.getValue(ATTRIB_WEATHER_CONDITIONS);

		} else {
			return;
		}

		_characters.delete(0, _characters.length());
	}

	private void parseActivity02End(final String name) {

		if (_isInName) {

			_isInName = false;
			_currentActivity.name = _characters.toString();

		} else if (_isInNotes) {

			_isInNotes = false;
			_currentActivity.notes = _characters.toString();

		} else if (_isInWeather) {

			_isInWeather = false;
			_currentActivity.weatherText = _characters.toString();
		}
	}

	private void parseLaps(final String name, final Attributes attributes) {

		if (name.equals(TAG_LAP)) {

			final String startTime = attributes.getValue(ATTRIB_LAP_START_TIME);

			if (startTime != null) {

				final Lap lap = new Lap();

				lap.lapStartTime = _dtParser.parseDateTime(startTime).getMillis();

				_currentActivity.laps.add(lap);
			}
		}
	}

	private void parseTrack(final Attributes attributes) {

		final String startTime = attributes.getValue(ATTRIB_TRACK_START_TIME);

		if (startTime != null) {
			_currentActivity.tourDateTime = _dtParser.parseDateTime(startTime);
			_currentActivity.tourStartTime = _currentActivity.tourDateTime.getMillis();
		}
	}

	private void parseTrackPoints(final String name, final Attributes attributes)
			throws InvalidDeviceSAXException {

		if (name.equals(TAG_TRACK_PT)) {

			if (_currentActivity.tourStartTime == Long.MIN_VALUE) {
				throw new InvalidDeviceSAXException(NLS.bind(Messages.FitLog_Error_InvalidStartTime, _importFilePath));
			}

			final TimeData timeSlice = new TimeData();

			// relative time in seconds
			final long longValue = Util.parseLong(attributes, ATTRIB_PT_TM);
			if (longValue != Long.MIN_VALUE) {
				timeSlice.absoluteTime = _currentActivity.tourStartTime + (longValue * 1000);
			}

			final double tpDistance = Util.parseDouble(attributes, ATTRIB_PT_DIST);
			final double latitude = Util.parseDouble(attributes, ATTRIB_PT_LAT);
			final double longitude = Util.parseDouble(attributes, ATTRIB_PT_LON);

			if (tpDistance != Double.MIN_VALUE) {
				_distanceAbsolute = tpDistance;
			} else if (tpDistance == Double.MIN_VALUE
					&& latitude != Double.MIN_VALUE
					&& longitude != Double.MIN_VALUE
					&& _prevLatitude != Double.MIN_VALUE
					&& _prevLongitude != Double.MIN_VALUE) {

				// get distance from lat/lon when it's not set
				_distanceAbsolute += Util.distanceVincenty(_prevLatitude, _prevLongitude, latitude, longitude);
			}

			if (latitude != Double.MIN_VALUE && longitude != Double.MIN_VALUE) {
				_prevLatitude = latitude;
				_prevLongitude = longitude;
			}

			timeSlice.absoluteDistance = (float) _distanceAbsolute;
			timeSlice.absoluteAltitude = Util.parseFloat(attributes, ATTRIB_PT_ELE);
			timeSlice.cadence = Util.parseInt(attributes, ATTRIB_PT_CADENCE);
			timeSlice.pulse = Util.parseInt(attributes, ATTRIB_PT_HR);
			timeSlice.power = Util.parseInt(attributes, ATTRIB_PT_POWER);
			timeSlice.latitude = latitude;
			timeSlice.longitude = longitude;

			_currentActivity.timeSlices.add(timeSlice);
		}
	}

	@Override
	public void startElement(final String uri, final String localName, final String name, final Attributes attributes)
			throws SAXException {

		if (_isInActivity) {

			if (_isInTrack) {
				parseTrackPoints(name, attributes);
			} else if (_isInLaps) {
				parseLaps(name, attributes);
			} else {
				parseActivity01Start(name, attributes);
			}
		}

		if (name.equals(TAG_TRACK)) {

			_isInTrack = true;

			parseTrack(attributes);

		} else if (name.equals(TAG_LAPS)) {

			_isInLaps = true;

		} else if (name.equals(TAG_ACTIVITY)) {

			/*
			 * a new exercise/tour starts
			 */

			_isInActivity = true;

			initTour();
		}
	}

}
