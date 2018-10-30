package net.tourbook.device.suunto;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import net.tourbook.common.util.Util;
import net.tourbook.data.TimeData;
import net.tourbook.data.TourData;
import net.tourbook.data.TourMarker;

public class SuuntoJsonProcessor {

	private final float				Kelvin	= 273.1499938964845f;
	private ArrayList<TimeData>	_sampleList;
	private int							_lapCounter;

	public TourData ImportActivity(	String jsonFileContent,
												TourData activityToReUse,
												ArrayList<TimeData> sampleListToReUse) {
		_sampleList = new ArrayList<TimeData>();

		JSONArray samples = null;
		try {
			JSONObject jsonContent = new JSONObject(jsonFileContent);
			samples = (JSONArray) jsonContent.get("Samples");
		} catch (JSONException ex) {
			return null;
		}

		JSONObject firstSample = (JSONObject) samples.get(0);

		TourData tourData = InitializeActivity(firstSample, activityToReUse, sampleListToReUse);

		if (tourData == null)
			return null;

		boolean isPaused = false;

		for (int i = 0; i < samples.length(); i++) {
			JSONObject sample = samples.getJSONObject(i);
			JSONObject currentSampleAttributes = new JSONObject(sample.get("Attributes").toString());
			String currentSampleSml = currentSampleAttributes.get("suunto/sml").toString();
			if (!currentSampleSml.contains("Sample"))
				continue;

			String currentSampleData = new JSONObject(currentSampleSml).get("Sample").toString();

			long currentSampleDate = ZonedDateTime.parse(sample.get("TimeISO8601").toString())
					.toInstant()
					.toEpochMilli();

			boolean wasDataPopulated = false;
			TimeData timeData = new TimeData();

			timeData.absoluteTime = currentSampleDate;

			/*
			 * TimeData minTDobject = null; long mintdvalue = Long.MAX_VALUE; for (TimeData object :
			 * _sampleList) { long difference = currentSampleDate - object.absoluteTime; if (difference
			 * < mintdvalue) { mintdvalue = difference; minTDobject = object; } } if (mintdvalue > -200
			 * && mintdvalue < 200) { timeData = minTDobject; }
			 */

			if (currentSampleData.contains("Pause")) {
				if (!isPaused) {
					if (currentSampleData.contains("true")) {
						isPaused = true;
					}
				} else {
					if (currentSampleData.contains("false"))
						isPaused = false;
				}
			}

			if (isPaused)
				continue;

			if (currentSampleData.contains("Lap") &&
					(currentSampleData.contains("Manual") ||
							currentSampleData.contains("Distance"))) {
				//TODO Support the case where we are in a children activity and the lap counter may need to be adjusted
				timeData.marker = 1;
				timeData.markerLabel = Integer.toString(++_lapCounter);
				_sampleList.add(timeData);
			}

			// GPS point
			if (currentSampleData.contains("GPSAltitude") && currentSampleData.contains("Latitude")
					&& currentSampleData.contains("Longitude")) {
				wasDataPopulated |= TryAddGpsData(new JSONObject(currentSampleData), timeData);
			}

			// Heart Rate
			wasDataPopulated |= TryAddHeartRateData(new JSONObject(currentSampleData), timeData);

			// Speed
			//wasDataPopulated |= TryAddSpeedData(new JSONObject(currentSampleData), timeData);

			// Cadence
			wasDataPopulated |= TryAddCadenceData(new JSONObject(currentSampleData), timeData);

			// Barometric Altitude
			// TryAddAltitudeData(new JSONObject(currentSampleData), currentSampleDate,
			// timeData);

			// Power
			wasDataPopulated |= TryAddPowerData(new JSONObject(currentSampleData), timeData);

			// Distance
			//wasDataPopulated |= TryAddDistanceData(new JSONObject(currentSampleData), timeData);

			// Temperature
			wasDataPopulated |= TryAddTemperatureData(new JSONObject(currentSampleData), timeData);

			if (wasDataPopulated)
				_sampleList.add(timeData);
		}
		tourData.createTimeSeries(_sampleList, true);

		return tourData;
	}

	/**
	 * Creates a new activity and initializes all the needed fields.
	 * 
	 * @param firstSample
	 *           The activity start time as a string.
	 * @param activityToReuse
	 *           The activity to concatenate the current activity with.
	 * @return If valid, the initialized tour
	 */
	private TourData InitializeActivity(JSONObject firstSample,
													TourData activityToReUse,
													ArrayList<TimeData> sampleListToReUse) {
		TourData tourData = new TourData();
		String firstSampleAttributes = firstSample.get("Attributes").toString();

		if (firstSampleAttributes.contains("Lap") &&
				firstSampleAttributes.contains("Type") &&
				firstSampleAttributes.contains("Start")) {

			ZonedDateTime startTime = ZonedDateTime.parse(firstSample.get("TimeISO8601").toString());
			tourData.setTourStartTime(startTime);

		} else if (activityToReUse != null) {

			Set<TourMarker> tourMarkers = activityToReUse.getTourMarkers();
			for (Iterator<TourMarker> it = tourMarkers.iterator(); it.hasNext();) {
				TourMarker tourMarker = it.next();
				_lapCounter = Integer.valueOf(tourMarker.getLabel());
			}
			activityToReUse.setTourMarkers(new HashSet<>());

			tourData = activityToReUse;
			_sampleList = sampleListToReUse;
			tourData.timeSerie = null;

		} else
			return null;

		return tourData;

	}

	public ArrayList<TimeData> getSampleList() {
		return _sampleList;
	}

	/**
	 * Attempts to retrieve and add GPS data to the current tour.
	 * 
	 * @param currentSample
	 *           The current sample data in JSON format.
	 * @param sampleList
	 *           The tour's time serie.
	 */
	private boolean TryAddGpsData(JSONObject currentSample, TimeData timeData) {
		try {
			String dateString = currentSample.get("UTC").toString();
			long unixTime = ZonedDateTime.parse(dateString).toInstant().toEpochMilli();
			float latitude = Util.parseFloat(currentSample.get("Latitude").toString());
			float longitude = Util.parseFloat(currentSample.get("Longitude").toString());
			float altitude = Util.parseFloat(currentSample.get("GPSAltitude").toString());

			timeData.latitude = (latitude * 180) / Math.PI;
			timeData.longitude = (longitude * 180) / Math.PI;
			timeData.absoluteTime = unixTime;
			timeData.absoluteAltitude = altitude;

			return true;
		} catch (Exception e) {}
		return false;
	}

	/**
	 * Attempts to retrieve and add HR data to the current tour.
	 * 
	 * @param currentSample
	 *           The current sample data in JSON format.
	 * @param sampleList
	 *           The tour's time serie.
	 */
	private boolean TryAddHeartRateData(JSONObject currentSample, TimeData timeData) {
		String value = null;
		if ((value = TryRetrieveStringElementValue(currentSample, "HR")) != null) {
			timeData.pulse = Util.parseFloat(value) * 60.0f;
			return true;
		}

		return false;
	}

	/**
	 * Attempts to retrieve and add speed data to the current tour.
	 * 
	 * @param currentSample
	 *           The current sample data in JSON format.
	 * @param sampleList
	 *           The tour's time serie.
	 */
	@SuppressWarnings("unused")
	private boolean TryAddSpeedData(JSONObject currentSample, TimeData timeData) {
		String value = null;
		if ((value = TryRetrieveStringElementValue(currentSample, "Speed")) != null) {
			timeData.speed = Util.parseFloat(value);
			return true;
		}
		return false;
	}

	/**
	 * Attempts to retrieve and add cadence data to the current tour.
	 * 
	 * @param currentSample
	 *           The current sample data in JSON format.
	 * @param sampleList
	 *           The tour's time serie.
	 */
	private boolean TryAddCadenceData(JSONObject currentSample, TimeData timeData) {
		String value = null;
		if ((value = TryRetrieveStringElementValue(currentSample, "Cadence")) != null) {
			timeData.cadence = Util.parseFloat(value) * 60.0f;
			return true;
		}
		return false;
	}

	/**
	 * Attempts to retrieve and add barometric altitude data to the current tour.
	 * 
	 * @param currentSample
	 *           The current sample data in JSON format.
	 * @param sampleList
	 *           The tour's time serie.
	 */
	@SuppressWarnings("unused")
	private boolean TryAddAltitudeData(JSONObject currentSample, TimeData timeData) {
		String value = null;
		if ((value = TryRetrieveStringElementValue(currentSample, "Altitude")) != null) {
			timeData.absoluteAltitude = Util.parseFloat(value);
			return true;
		}
		return false;
	}

	/**
	 * Attempts to retrieve and add power data to the current tour.
	 * 
	 * @param currentSample
	 *           The current sample data in JSON format.
	 * @param sampleList
	 *           The tour's time serie.
	 */
	private boolean TryAddPowerData(JSONObject currentSample, TimeData timeData) {
		String value = null;
		if ((value = TryRetrieveStringElementValue(currentSample, "Power")) != null) {
			timeData.power = Util.parseFloat(value);
			return true;
		}
		return false;
	}

	/**
	 * Attempts to retrieve and add power data to the current tour.
	 * 
	 * @param currentSample
	 *           The current sample data in JSON format.
	 * @param sampleList
	 *           The tour's time serie.
	 */
	@SuppressWarnings("unused")
	private boolean TryAddDistanceData(JSONObject currentSample, TimeData timeData) {
		String value = null;
		if ((value = TryRetrieveStringElementValue(currentSample, "Distance")) != null) {
			timeData.absoluteDistance = Util.parseFloat(value);
			return true;
		}
		return false;
	}

	/**
	 * Attempts to retrieve and add power data to the current tour.
	 * 
	 * @param currentSample
	 *           The current sample data in JSON format.
	 * @param sampleList
	 *           The tour's time serie.
	 */
	private boolean TryAddTemperatureData(JSONObject currentSample, TimeData timeData) {
		String value = null;
		if ((value = TryRetrieveStringElementValue(currentSample, "Temperature")) != null) {
			timeData.temperature = Util.parseFloat(value) - Kelvin;
			return true;
		}
		return false;
	}

	/**
	 * Searches for an element and returns its value as a string.
	 * 
	 * @param token
	 *           The JSON token in which to look for a given element.
	 * @param elementName
	 *           The element name to look for in a JSON content.
	 * @return The element value, if found.
	 */
	private String TryRetrieveStringElementValue(JSONObject token, String elementName) {
		if (!token.toString().contains(elementName))
			return null;

		String result = null;
		try {
			result = token.get(elementName).toString();
		} catch (Exception e) {

		}
		if (result == "null")
			return null;

		return result;
	}
}
