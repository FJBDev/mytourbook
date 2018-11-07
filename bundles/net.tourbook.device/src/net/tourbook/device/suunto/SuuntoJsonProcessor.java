package net.tourbook.device.suunto;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.jface.preference.IPreferenceStore;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.Util;
import net.tourbook.data.TimeData;
import net.tourbook.data.TourData;
import net.tourbook.data.TourMarker;

public class SuuntoJsonProcessor {

	private final float				Kelvin		= 273.1499938964845f;
	private ArrayList<TimeData>	_sampleList;
	private int							_lapCounter;
	final IPreferenceStore			_prefStore	= TourbookPlugin.getDefault().getPreferenceStore();

	/**
	 * Processes and imports a Suunto activity (from a Suunto 9 or Spartan watch).
	 * 
	 * @param jsonFileContent
	 *           The Suunto's file content in JSON format.
	 * @param activityToReUse
	 *           If provided, the activity to concatenate the provided file to.
	 * @param sampleListToReUse
	 *           If provided, the activity's data from the activity to reuse.
	 * @return The created tour.
	 */
	public TourData ImportActivity(	String jsonFileContent,
												TourData activityToReUse,
												ArrayList<TimeData> sampleListToReUse) {
		_sampleList = new ArrayList<TimeData>();

		JSONArray samples = null;
		try {
			JSONObject jsonContent = new JSONObject(jsonFileContent);
			samples = (JSONArray) jsonContent.get("Samples");
		} catch (JSONException ex) {
			StatusUtil.log(ex);
			return null;
		}

		JSONObject firstSample = (JSONObject) samples.get(0);

		TourData tourData = InitializeActivity(firstSample, activityToReUse, sampleListToReUse);

		if (tourData == null)
			return null;

		boolean isPaused = false;

		boolean reusePreviousTimeEntry;
		for (int i = 0; i < samples.length(); ++i) {
			String currentSampleData;
			String sampleTime;
			try {
				JSONObject sample = samples.getJSONObject(i);
				String attributesContent = sample.get("Attributes").toString();
				if (attributesContent == null || attributesContent == "")
					continue;

				JSONObject currentSampleAttributes = new JSONObject(sample.get("Attributes").toString());
				String currentSampleSml = currentSampleAttributes.get("suunto/sml").toString();
				if (!currentSampleSml.contains("Sample"))
					continue;

				currentSampleData = new JSONObject(currentSampleSml).get("Sample").toString();

				sampleTime = sample.get("TimeISO8601").toString();
			} catch (Exception e) {
				StatusUtil.log(e);
				continue;
			}

			boolean wasDataPopulated = false;
			reusePreviousTimeEntry = false;
			TimeData timeData = null;

			ZonedDateTime currentZonedDateTime = ZonedDateTime.parse(sampleTime);
			currentZonedDateTime = currentZonedDateTime.truncatedTo(ChronoUnit.SECONDS);
			//Rounding to the nearest second
			if (Character.getNumericValue(sampleTime.charAt(20)) >= 5)
				currentZonedDateTime = currentZonedDateTime.plusSeconds(1);

			long currentTime = currentZonedDateTime.toInstant().toEpochMilli();

			if (_sampleList.size() > 0) {
				//Looking in the last 10 entries to see if their time is identical to the current sample's time
				for (int index = _sampleList.size() - 1; index > _sampleList.size() - 11 && index >= 0; --index) {
					if (_sampleList.get(index).absoluteTime == currentTime) {
						timeData = _sampleList.get(index);
						reusePreviousTimeEntry = true;
						break;
					}
				}
			}

			if (!reusePreviousTimeEntry) {
				timeData = new TimeData();
				timeData.absoluteTime = currentTime;
			}

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
				timeData.marker = 1;
				timeData.markerLabel = Integer.toString(++_lapCounter);
				if (!reusePreviousTimeEntry)
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
			wasDataPopulated |= TryAddSpeedData(new JSONObject(currentSampleData), timeData);

			// Cadence
			wasDataPopulated |= TryAddCadenceData(new JSONObject(currentSampleData), timeData);

			// Barometric Altitude
			if (_prefStore.getInt(IPreferences.ALTITUDE_DATA_SOURCE) == 1) {
				wasDataPopulated |= TryAddAltitudeData(new JSONObject(currentSampleData), timeData);
			}

			// Power
			wasDataPopulated |= TryAddPowerData(new JSONObject(currentSampleData), timeData);

			// Distance
			if (_prefStore.getInt(IPreferences.DISTANCE_DATA_SOURCE) == 1) {
				wasDataPopulated |= TryAddDistanceData(new JSONObject(currentSampleData), timeData);
			}

			// Temperature
			wasDataPopulated |= TryAddTemperatureData(new JSONObject(currentSampleData), timeData);

			if (wasDataPopulated && !reusePreviousTimeEntry)
				_sampleList.add(timeData);
		}

		//removing the entries that don't have GPS data
		Iterator<TimeData> sampleListIterator = _sampleList.iterator();
		while (sampleListIterator.hasNext()) {
			TimeData currentTimeData = sampleListIterator.next();
			if (currentTimeData.longitude == Double.MIN_VALUE &&
					currentTimeData.latitude == Double.MIN_VALUE)
				sampleListIterator.remove();
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
	 *           If provided, the activity to concatenate the current activity with.
	 * @param sampleListToReUse
	 *           If provided, the activity's data from the activity to reuse.
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
			tourData.clearComputedSeries();
			tourData.timeSerie = null;

		} else
			return null;

		return tourData;

	}

	/**
	 * Retrieves the current activity's data.
	 * 
	 * @return The list of data.
	 */
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
	 * @return True if successful, false otherwise.
	 */
	private boolean TryAddGpsData(JSONObject currentSample, TimeData timeData) {
		try {
			float latitude = Util.parseFloat(currentSample.get("Latitude").toString());
			float longitude = Util.parseFloat(currentSample.get("Longitude").toString());
			float altitude = Util.parseFloat(currentSample.get("GPSAltitude").toString());

			timeData.latitude = (latitude * 180) / Math.PI;
			timeData.longitude = (longitude * 180) / Math.PI;

			// GPS altitude
			if (_prefStore.getInt(IPreferences.ALTITUDE_DATA_SOURCE) == 0) {
				timeData.absoluteAltitude = altitude;
			}

			return true;
		} catch (Exception e) {
			StatusUtil.log(e);
		}
		return false;
	}

	/**
	 * Attempts to retrieve and add HR data to the current tour.
	 * 
	 * @param currentSample
	 *           The current sample data in JSON format.
	 * @param sampleList
	 *           The tour's time serie.
	 * @return True if successful, false otherwise.
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
	 * @return True if successful, false otherwise.
	 */
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
	 * @return True if successful, false otherwise.
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
	 * @return True if successful, false otherwise.
	 */
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
	 * @return True if successful, false otherwise.
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
	 * @return True if successful, false otherwise.
	 */
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
	 * @return True if successful, false otherwise.
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
		} catch (Exception e) {}
		if (result == "null")
			return null;

		return result;
	}
}
