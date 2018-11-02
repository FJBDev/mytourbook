package net.tourbook.device.suunto;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

import org.apache.commons.io.FilenameUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.builder.Input;
import org.xmlunit.diff.Diff;

import net.tourbook.common.UI;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.Util;
import net.tourbook.data.TimeData;
import net.tourbook.data.TourData;
import net.tourbook.importdata.DeviceData;
import net.tourbook.importdata.SerialParameters;
import net.tourbook.importdata.TourbookDevice;

public class Suunto9DeviceDataReader extends TourbookDevice {

	private HashMap<TourData, ArrayList<TimeData>>	_processedActivities				= new HashMap<TourData, ArrayList<TimeData>>();
	private HashMap<String, String>						_childrenActivitiesToProcess	= new HashMap<String, String>();
	private HashMap<Long, TourData>						_newlyImportedTours				= new HashMap<Long, TourData>();
	private HashMap<Long, TourData>						_alreadyImportedTours			= new HashMap<Long, TourData>();

	// For Unit testing
	private static final boolean			UNITTESTS			= false;
	public static final String				IMPORT_FILE_PATH	= "/net/tourbook/device/suunto/testFiles/";
	private static Map<String, String>	testFiles			= new HashMap<>();									// Java 7

	@Override
	public String buildFileNameFromRawData(final String rawDataFileName) {
		// NEXT Auto-generated method stub
		return null;
	}

	@Override
	public boolean checkStartSequence(final int byteIndex, final int newByte) {
		return true;
	}

	public String getDeviceModeName(final int profileId) {
		return UI.EMPTY_STRING;
	}

	@Override
	public SerialParameters getPortParameters(final String portName) {
		return null;
	}

	@Override
	public int getStartSequenceSize() {
		return 0;
	}

	public int getTransferDataSize() {
		return -1;
	}

	@Override
	public boolean processDeviceData(final String importFilePath,
												final DeviceData deviceData,
												final HashMap<Long, TourData> alreadyImportedTours,
												final HashMap<Long, TourData> newlyImportedTours) {
		if (UNITTESTS) {
			return testSuuntoFiles(importFilePath, deviceData, alreadyImportedTours, newlyImportedTours);
		}

		_newlyImportedTours = newlyImportedTours;
		_alreadyImportedTours = alreadyImportedTours;

		// When a new import is started, we need to clean the previous saved activities
		if (newlyImportedTours.size() == 0 && alreadyImportedTours.size() == 0) {
			cleanUpActivities();
		}

		String jsonFileContent =
				GetJsonContentFromGZipFile(importFilePath);

		if (isValidJSONFile(jsonFileContent) == false) {
			return false;
		}
		return ProcessFile(importFilePath, jsonFileContent);
	}

	@Override
	public boolean validateRawData(final String fileName) {
		String jsonFileContent = GetJsonContentFromGZipFile(fileName);
		return isValidJSONFile(jsonFileContent);
	}

	/**
	 * Check if the file is a valid device JSON file.
	 * 
	 * @param importFilePath
	 * @return Returns <code>true</code> when the file contains content with the requested tag.
	 */
	protected boolean isValidJSONFile(String jsonFileContent) {
		BufferedReader fileReader = null;
		try {

			if (jsonFileContent == null ||
					jsonFileContent == "") {
				return false;
			}

			try {
				JSONObject jsonContent = new JSONObject(jsonFileContent);
				JSONArray samples = (JSONArray) jsonContent.get("Samples");

				String firstSample = samples.get(0).toString();
				if (firstSample.contains("Attributes") && firstSample.contains("Source") && firstSample.contains("TimeISO8601"))
					return true;

			} catch (JSONException ex) {
				return false;
			}

		} catch (final Exception e) {
			StatusUtil.log(e);
			return false;
		} finally {
			Util.closeReader(fileReader);
		}

		return false;
	}

	private String GetJsonContentFromGZipFile(String gzipFilePath) {
		String jsonFileContent = null;
		try {
			GZIPInputStream gzip = new GZIPInputStream(new FileInputStream(gzipFilePath));
			BufferedReader br = new BufferedReader(new InputStreamReader(gzip));

			jsonFileContent = br.readLine();

			// close resources
			br.close();
			gzip.close();

		} catch (IOException e) {
			return "";
		}

		return jsonFileContent;
	}

	private String GetContentFromResource(String resourceFilePath, boolean isZipFile) {
		String fileContent = null;
		try {
			InputStream inputStream =
					Suunto9DeviceDataReader.class.getResourceAsStream(resourceFilePath);

			BufferedReader br = null;
			GZIPInputStream gzip = null;
			if (isZipFile) {
				gzip = new GZIPInputStream(inputStream);
				br = new BufferedReader(new InputStreamReader(gzip));
			} else
				br = new BufferedReader(new InputStreamReader(inputStream));

			fileContent = br.lines().collect(Collectors.joining());

			// close resources
			br.close();
			if (isZipFile)
				gzip.close();
		} catch (IOException e) {
			return "";
		}

		return fileContent;
	}

	private boolean ProcessFile(String filePath, String jsonFileContent) {
		SuuntoJsonProcessor suuntoJsonProcessor = new SuuntoJsonProcessor();

		String fileName =
				FilenameUtils.removeExtension(filePath);

		if (fileName.substring(fileName.length() - 5, fileName.length()) == ".json") {
			fileName = FilenameUtils.removeExtension(fileName);
		}

		String fileNumberString =
				fileName.substring(fileName.lastIndexOf('-') + 1, fileName.lastIndexOf('-') + 2);

		int fileNumber;
		try {
			fileNumber = Integer.parseInt(fileNumberString);
		} catch (NumberFormatException e) {
			return false;
		}

		TourData activity = null;
		if (fileNumber == 1) {
			activity = suuntoJsonProcessor.ImportActivity(
					jsonFileContent,
					null,
					null);

			final String uniqueId = this.createUniqueId(activity, Util.UNIQUE_ID_SUFFIX_SUUNTO9);
			activity.createTourId(uniqueId);

			if (!processedActivityExists(activity.getTourId()))
				_processedActivities.put(activity, suuntoJsonProcessor.getSampleList());

		} else if (fileNumber > 1) {
			// if we find the parent (e.g: The activity just before the
			// current one. Example : If the current is xxx-3, we find xxx-2)
			// then we import it reusing the parent activity AND we check that there is no children waiting to be imported
			// If nothing is found, we store it for (hopefully) future use.
			Map.Entry<TourData, ArrayList<TimeData>> parentEntry = null;
			for (Map.Entry<TourData, ArrayList<TimeData>> entry : _processedActivities.entrySet()) {
				TourData key = entry.getKey();

				String parentFileName = GetFileNameWithoutNumber(
						FilenameUtils.getBaseName(filePath)) +
						"-" +
						String.valueOf(fileNumber - 1) +
						".json.gz";

				if (key.getImportFileName().contains(parentFileName)) {
					parentEntry = entry;
					break;
				}
			}

			if (parentEntry == null) {
				if (!_childrenActivitiesToProcess.containsKey(filePath))
					_childrenActivitiesToProcess.put(filePath, jsonFileContent);
			} else {
				activity = suuntoJsonProcessor.ImportActivity(
						jsonFileContent,
						parentEntry.getKey(),
						parentEntry.getValue());

				//We remove the parent activity to replace it with the
				//updated one (parent activity concatenated with the current
				//one).
				Iterator<Entry<TourData, ArrayList<TimeData>>> it = _processedActivities.entrySet().iterator();
				while (it.hasNext()) {
					Map.Entry<TourData, ArrayList<TimeData>> entry = (Entry<TourData, ArrayList<TimeData>>) it.next();
					if (entry.getKey().getTourId() == parentEntry.getKey().getTourId())
						it.remove(); // avoids a ConcurrentModificationException
				}
				//processedActivities.remove(parentEntry.getKey());

				if (!processedActivityExists(activity.getTourId()))
					_processedActivities.put(activity, suuntoJsonProcessor.getSampleList());
			}
		}

		//We check if the child(ren) has(ve) been provided earlier.
		//In this case, we concatenate it(them) with the parent
		//activity
		if (activity != null) {

			activity.setImportFilePath(filePath);

			ConcatenateChildrenActivities(
					filePath,
					fileNumber,
					activity,
					suuntoJsonProcessor.getSampleList());

			TryFinalizeTour(activity);
		}

		return true;
	}

	/**
	 * Concatenates children activities with a given activity.
	 * 
	 * @param filePath
	 *           The absolute full path of a given activity.
	 * @param currentFileNumber
	 *           The file number of the given activity. Example : If the current activity file is
	 *           1536723722706_{DeviceSerialNumber}_-2.json.gz its file number will be 2
	 * @param currentActivity
	 *           The current activity processed and created.
	 */
	private void ConcatenateChildrenActivities(	String filePath,
																int currentFileNumber,
																TourData currentActivity,
																ArrayList<TimeData> sampleListToReUse) {
		SuuntoJsonProcessor suuntoJsonProcessor = new SuuntoJsonProcessor();

		ArrayList<String> keysToRemove = new ArrayList<String>();
		for (@SuppressWarnings("unused")
		Map.Entry<String, String> unused : _childrenActivitiesToProcess.entrySet()) {

			String parentFileName = GetFileNameWithoutNumber(
					FilenameUtils.getBaseName(filePath)) +
					"-" +
					String.valueOf(++currentFileNumber) +
					".json.gz";

			Map.Entry<String, String> childEntry = getChildActivity(parentFileName);

			if (childEntry == null) {
				continue;
			}

			suuntoJsonProcessor.ImportActivity(
					childEntry.getValue(),
					currentActivity,
					sampleListToReUse);

			// We just concatenated a child activity so we can remove it
			// from the list of activities to process
			keysToRemove.add(childEntry.getKey());

			// We need to update the activity we just concatenated by
			// updating the file path and the activity object.
			removeProcessedActivity(currentActivity.getImportFilePath());
			currentActivity.setImportFilePath(childEntry.getKey());
			_processedActivities.put(currentActivity, suuntoJsonProcessor.getSampleList());
		}

		for (int index = 0; index < keysToRemove.size(); ++index) {
			_childrenActivitiesToProcess.remove(keysToRemove.get(index));
		}
	}

	/**
	 * Returns a file name without its number. Example : Input :
	 * C:\Users\fbard\Downloads\S9\IMTUF100\1537365863086_{DeviceSerialNumber}_post_timeline-1.json.gz
	 * Output : 1537365863086_183010004848_post_timeline-
	 * 
	 * @param fileName
	 *           The file name to process.
	 * @return The processed file name.
	 */

	private String GetFileNameWithoutNumber(String fileName) {
		return fileName.substring(0, fileName.lastIndexOf('-'));
	}

	/**
	 * Attempting to finalize an activity. If it doesn't contain a tourId, it is not a final
	 * activity.
	 * 
	 * @param tourData
	 *           The tour to finalize
	 */
	private void TryFinalizeTour(TourData tourData) {

		tourData.setDeviceId(deviceId);

		long tourId;
		try {

			tourId = tourData.getTourId();

		} catch (NullPointerException e) {
			tourId = -1;
		}

		if (tourId != -1) {
			// check if the tour is already imported
			if (_alreadyImportedTours.containsKey(tourId)) {
				_alreadyImportedTours.remove(tourId);
			}

			// add new tour to other tours
			if (_newlyImportedTours.containsKey(tourId)) {
				_newlyImportedTours.remove(tourId);
			}
			_newlyImportedTours.put(tourId, tourData);

			tourData.computeAltitudeUpDown();
			tourData.computeTourDrivingTime();
			tourData.computeComputedValues();

		}
	}

	private boolean processedActivityExists(long tourId) {
		for (Map.Entry<TourData, ArrayList<TimeData>> entry : _processedActivities.entrySet()) {
			TourData key = entry.getKey();
			if (key.getTourId() == tourId)
				return true;
		}

		return false;
	}

	private Entry<String, String> getChildActivity(String filePath) {
		for (Entry<String, String> childEntry : _childrenActivitiesToProcess.entrySet()) {
			if (childEntry.getKey().contains(filePath))
				return childEntry;
		}

		return null;
	}

	private void removeProcessedActivity(String filePath) {
		Iterator<Entry<TourData, ArrayList<TimeData>>> it = _processedActivities.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<TourData, ArrayList<TimeData>> entry = (Entry<TourData, ArrayList<TimeData>>) it.next();
			if (entry.getKey().getImportFilePath() == filePath) {
				it.remove(); // avoids a ConcurrentModificationException
				return;
			}
		}
	}

	/*
	 * At the end of the import process, we clean the accumulated data so that the next round will
	 * begin clean.
	 */

	private void cleanUpActivities() {
		_childrenActivitiesToProcess.clear();
		_processedActivities.clear();
	}

	/**
	 * Unit tests for the Suunto Spartan/9 import
	 */
	public boolean testSuuntoFiles(	final String importFilePath,
												final DeviceData deviceData,
												final HashMap<Long, TourData> alreadyImportedTours,
												final HashMap<Long, TourData> newlyImportedTours) {

		boolean testResults = true;

		// City of Rocks, ID
		String filePath =
				IMPORT_FILE_PATH + "1537365846902_183010004848_post_timeline-1.json.gz";
		String controlFilePath = IMPORT_FILE_PATH + "1537365846902_183010004848_post_timeline-1.xml";
		testFiles.put(controlFilePath, filePath);

		//Maxwell, CO

		filePath = IMPORT_FILE_PATH +
				"Original-1536723722706_183010004848_post_timeline-1.json.gz";
		controlFilePath =
				IMPORT_FILE_PATH + "1536723722706_183010004848_post_timeline-1.xml";
		testFiles.put(controlFilePath, filePath); // Single file tests SuuntoJsonProcessor

		TourData entry;
		String xml;
		String controlFileContent;

		for (Map.Entry<String, String> testEntry : testFiles.entrySet()) {
			String jsonFileContent =
					GetContentFromResource(testEntry.getValue(), true);
			ProcessFile(testEntry.getValue(), jsonFileContent);

			entry = GetLastTourDataImported();
			xml = entry.toXml();
			controlFileContent = GetContentFromResource(testEntry.getKey(), false);

			//testResults &= CompareAgainstControl(controlFileContent, xml);
		}

		// ------------------------------------------
		// Split files test
		// ------------------------------------------

		// Maxwell, CO (Split manually)

		// ORDER 2 - 1 - 3

		// File #2
		String maxWell2FilePath = IMPORT_FILE_PATH +
				"1536723722706_183010004848_post_timeline-2.json.gz";
		String jsonContent = GetContentFromResource(maxWell2FilePath, true);
		ProcessFile(maxWell2FilePath, jsonContent);

		// File #1
		String maxWell1FilePath = IMPORT_FILE_PATH +
				"1536723722706_183010004848_post_timeline-1.json.gz";
		jsonContent = GetContentFromResource(maxWell1FilePath, true);
		ProcessFile(maxWell1FilePath, jsonContent);

		// File #3
		String maxWell3FilePath = IMPORT_FILE_PATH +
				"1536723722706_183010004848_post_timeline-3.json.gz";
		jsonContent = GetContentFromResource(maxWell3FilePath, true);
		ProcessFile(maxWell3FilePath, jsonContent);

		String controlDocumentPath = IMPORT_FILE_PATH +
				"1536723722706_183010004848_post_timeline-1.xml";

		entry = GetLastTourDataImported();
		xml = entry.toXml();
		controlFileContent = GetContentFromResource(controlDocumentPath, false);
		testResults &= CompareAgainstControl(controlFileContent, xml);

		// ORDER 2 - 3 - 1

		// File #2
		jsonContent = GetContentFromResource(maxWell2FilePath, true);
		ProcessFile(maxWell2FilePath, jsonContent);

		// File #3
		jsonContent = GetContentFromResource(maxWell3FilePath, true);
		ProcessFile(maxWell3FilePath, jsonContent);

		// File #1
		jsonContent = GetContentFromResource(maxWell1FilePath, true);
		ProcessFile(maxWell1FilePath, jsonContent);

		entry = GetLastTourDataImported();
		xml = entry.toXml();
		testResults &= CompareAgainstControl(controlFileContent, xml);

		// ORDER 1 - 2 - 3

		// File #1
		jsonContent = GetContentFromResource(maxWell1FilePath, true);
		ProcessFile(maxWell1FilePath, jsonContent);

		// File #2
		jsonContent = GetContentFromResource(maxWell2FilePath, true);
		ProcessFile(maxWell2FilePath, jsonContent);

		// File #3
		jsonContent = GetContentFromResource(maxWell3FilePath, true);
		ProcessFile(maxWell3FilePath, jsonContent);

		entry = GetLastTourDataImported();
		xml = entry.toXml();
		testResults &= CompareAgainstControl(controlFileContent, xml);

		// ORDER 1 - 3 - 2

		// File #1
		jsonContent = GetContentFromResource(maxWell1FilePath, true);
		ProcessFile(maxWell1FilePath, jsonContent);

		// File #3
		jsonContent = GetContentFromResource(maxWell3FilePath, true);
		ProcessFile(maxWell3FilePath, jsonContent);

		// File #2
		jsonContent = GetContentFromResource(maxWell2FilePath, true);
		ProcessFile(maxWell2FilePath, jsonContent);

		entry = GetLastTourDataImported();
		xml = entry.toXml();
		testResults &= CompareAgainstControl(controlFileContent, xml);

		return testResults;
	}

	private TourData GetLastTourDataImported() {
		Iterator<Entry<TourData, ArrayList<TimeData>>> it = _processedActivities.entrySet().iterator();
		TourData lastTourData = null;
		while (it.hasNext()) {
			lastTourData = it.next().getKey();
		}

		return lastTourData;
	}

	private static boolean CompareAgainstControl(String controlDocument,
																String xmlTestDocument) {

		Diff myDiff = DiffBuilder.compare(Input.fromString(controlDocument))
				.withTest(Input.fromString(xmlTestDocument))
				.ignoreWhitespace()
				.build();

		return !myDiff.hasDifferences();
	}

}
