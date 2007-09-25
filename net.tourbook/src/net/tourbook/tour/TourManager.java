/*******************************************************************************
 * Copyright (C) 2005, 2007  Wolfgang Schramm
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

package net.tourbook.tour;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;

import net.tourbook.Messages;
import net.tourbook.chart.ChartDataModel;
import net.tourbook.chart.ChartDataSerie;
import net.tourbook.chart.ChartDataXSerie;
import net.tourbook.chart.ChartDataYSerie;
import net.tourbook.chart.ComputeChartValue;
import net.tourbook.colors.GraphColors;
import net.tourbook.data.TourData;
import net.tourbook.database.TourDatabase;
import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.ui.views.TourChartAnalyzerInfo;
import net.tourbook.ui.views.tourMap.TourDataNormalizer;
import net.tourbook.util.StringToArrayConverter;

import org.eclipse.core.runtime.ListenerList;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

public class TourManager {

	/**
	 * tour was changed and saved in the database
	 */
	public static final int					TOUR_PROPERTY_CHART_IS_MODIFIED			= 10;
	public static final int					TOUR_PROPERTY_SEGMENT_LAYER_CHANGED		= 20;
	public static final int					TOUR_PROPERTY_REFERENCE_TOUR_CHANGED	= 30;
	public static final int					TOUR_PROPERTY_COMPARE_TOUR_CHANGED		= 40;

	public static final String				CUSTOM_DATA_TIME						= "time";									//$NON-NLS-1$
	public static final String				CUSTOM_DATA_DISTANCE					= "distance";								//$NON-NLS-1$
	public static final String				CUSTOM_DATA_ALTITUDE					= "altitude";								//$NON-NLS-1$
	public static final String				CUSTOM_DATA_SPEED						= "speed";									//$NON-NLS-1$
	public static final String				CUSTOM_DATA_GRADIENT					= "gradient";								//$NON-NLS-1$
	public static final String				CUSTOM_DATA_ALTIMETER					= "altimeter";								//$NON-NLS-1$
	public static final String				CUSTOM_DATA_PULSE						= "pulse";									//$NON-NLS-1$

	public static final String				ANALYZER_INFO							= "AnalyzerInfo";							//$NON-NLS-1$
	public static final String				X_AXIS_TIME								= "time";									//$NON-NLS-1$
	public static final String				X_AXIS_DISTANCE							= "distance";								//$NON-NLS-1$

	public static final int					GRAPH_ALTITUDE							= 1000;
	public static final int					GRAPH_SPEED								= 1001;
	public static final int					GRAPH_ALTIMETER							= 1002;
	public static final int					GRAPH_PULSE								= 1003;
	public static final int					GRAPH_TEMPERATURE						= 1004;
	public static final int					GRAPH_CADENCE							= 1005;
	public static final int					GRAPH_GRADIENT							= 1006;
	public static final int					GRAPH_POWER								= 1007;

	public static final int					GRADIENT_DIVISOR						= 10;

	private static TourManager				instance;

	private ComputeChartValue				computeSpeedAvg;
	private ComputeChartValue				computeAltimeterAvg;
	private ComputeChartValue				computeGradientAvg;

	private final HashMap<Long, TourData>	fTourDataMap							= new HashMap<Long, TourData>();

	private final ListenerList				fPropertyListeners						= new ListenerList(ListenerList.IDENTITY);

	private TourManager() {}

	/**
	 * Compute the speed between start and end index
	 * 
	 * @param chartDataModel
	 * @param startIndex
	 * @param endIndex
	 * @return Returns the speed between start and end index
	 */
	public static float computeTourSpeed(ChartDataModel chartDataModel, int startIndex, int endIndex) {

		final int[] distanceValues = ((ChartDataXSerie) chartDataModel.getCustomData(TourManager.CUSTOM_DATA_DISTANCE)).getHighValues()[0];
		final int[] timeValues = ((ChartDataXSerie) chartDataModel.getCustomData(TourManager.CUSTOM_DATA_TIME)).getHighValues()[0];

		return computeTourSpeed(distanceValues, timeValues, startIndex, endIndex);
	}

	public static float computeTourSpeed(	int[] distanceSerie,
											int[] timeSerie,
											int startIndex,
											int endIndex) {

		final int distance = distanceSerie[endIndex] - distanceSerie[startIndex];
		int time = timeSerie[endIndex] - timeSerie[startIndex];

		final int timeInterval = timeSerie[1] - timeSerie[0];

		// remove breaks from the time
		int ignoreTimeSlices = getIgnoreTimeSlices(timeSerie,
				startIndex,
				endIndex,
				10 / timeInterval);

		time = time - (ignoreTimeSlices * timeInterval);

		return (float) ((float) distance / time * 3.6);
	}

	/**
	 * create the tour chart configuration by reading the settings from the preferences
	 * 
	 * @return
	 */
	public static TourChartConfiguration createTourChartConfiguration() {

		final TourChartConfiguration chartConfig = new TourChartConfiguration(true);

		final IPreferenceStore prefStore = TourbookPlugin.getDefault().getPreferenceStore();

		// convert the graph ids from the preferences into visible graphs in
		// the chart panel configuration
		final String[] prefGraphIds = StringToArrayConverter.convertStringToArray(prefStore.getString(ITourbookPreferences.GRAPH_VISIBLE));
		for (final String prefGraphId : prefGraphIds) {
			chartConfig.addVisibleGraph(Integer.valueOf(prefGraphId));
		}

		// set the unit which is shown on the x-axis
		if (prefStore.getString(ITourbookPreferences.GRAPH_X_AXIS).equals(X_AXIS_TIME)) {
			chartConfig.showTimeOnXAxis = true;
		} else {
			chartConfig.showTimeOnXAxis = false;
		}
		chartConfig.showTimeOnXAxisBackup = chartConfig.showTimeOnXAxis;

		// set the starttime from the prefs
		chartConfig.isStartTime = prefStore.getBoolean(ITourbookPreferences.GRAPH_X_AXIS_STARTTIME);

		updateZoomOptionsInChartConfig(chartConfig, prefStore);

		return chartConfig;
	}

	/**
	 * calculate the driving time, ignore the time when the distance is 0 within a time period which
	 * is defined by <code>sliceMin</code>
	 * 
	 * @param distanceValues
	 * @param indexLeft
	 * @param indexRight
	 * @param sliceMin
	 * @return Returns the number of slices which can be ignored
	 */
	public static int getIgnoreTimeSlices(	final int[] distanceValues,
											int indexLeft,
											int indexRight,
											int sliceMin) {
		int ignoreTimeCounter = 0;
		int oldDistance = 0;
		sliceMin = Math.max(sliceMin, 1);

		for (int valueIndex = indexLeft; valueIndex <= indexRight; valueIndex++) {

			if (distanceValues[valueIndex] == oldDistance) {
				ignoreTimeCounter++;
			}

			int oldIndex = valueIndex - sliceMin;
			if (oldIndex < 0) {
				oldIndex = 0;
			}
			oldDistance = distanceValues[oldIndex];
		}
		return ignoreTimeCounter;
	}

	public static TourManager getInstance() {

		if (instance == null) {
			instance = new TourManager();
		}

		return instance;
	}

	/**
	 * @return returns the date of this tour
	 */
	public static String getTourDate(final TourData tourData) {

		final Calendar calendar = GregorianCalendar.getInstance();
		calendar.set(tourData.getStartYear(), tourData.getStartMonth() - 1, tourData.getStartDay());

		return DateFormat.getDateInstance().format(calendar.getTime());
	}

	/**
	 * @return returns the detailed title of this tour (displayed as chart title)
	 */
	public static String getTourTitleDetailed(final TourData tourData) {

		final String tourTitle = tourData.getTourTitle();

		return getTourDate(tourData) + ((tourTitle.length() == 0) ? "" : " - " + tourTitle); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * set the graph colors from the pref store
	 * 
	 * @param prefStore
	 * @param yDataSerie
	 * @param graphName
	 */
	public static void setGraphColor(	final IPreferenceStore prefStore,
										final ChartDataYSerie yDataSerie,
										final String graphName) {

		final String prefGraphName = ITourbookPreferences.GRAPH_COLORS + graphName + "."; //$NON-NLS-1$

		yDataSerie.setRgbLine(new RGB[] { PreferenceConverter.getColor(prefStore, prefGraphName
				+ GraphColors.PREF_COLOR_LINE) });

		yDataSerie.setRgbDark(new RGB[] { PreferenceConverter.getColor(prefStore, prefGraphName
				+ GraphColors.PREF_COLOR_DARK) });

		yDataSerie.setRgbBright(new RGB[] { PreferenceConverter.getColor(prefStore, prefGraphName
				+ GraphColors.PREF_COLOR_BRIGHT) });
	}

	/**
	 * update the zoom options in the chart configuration from the pref store
	 * 
	 * @param chartConfig
	 * @param prefStore
	 */
	static void updateZoomOptionsInChartConfig(	final TourChartConfiguration chartConfig,
												final IPreferenceStore prefStore) {

		// get pref store settings
		boolean scrollZoomedGraph = prefStore.getBoolean(ITourbookPreferences.GRAPH_ZOOM_SCROLL_ZOOMED_GRAPH);
		boolean autoZoomToSlider = prefStore.getBoolean(ITourbookPreferences.GRAPH_ZOOM_AUTO_ZOOM_TO_SLIDER);

		// prevent setting both zoom options to true
		if (scrollZoomedGraph) {
			autoZoomToSlider = false;
		}
		if (autoZoomToSlider) {
			scrollZoomedGraph = false;
		}

		chartConfig.scrollZoomedGraph = scrollZoomedGraph;
		chartConfig.autoZoomToSlider = autoZoomToSlider;
	}

	public void addPropertyListener(ITourPropertyListener listener) {
		fPropertyListeners.add(listener);
	}

	/**
	 * adjust the min/max values to make them more visible and not at the same position as the
	 * x-axis or the top of the chart
	 */
	private void adjustMinMax(final ChartDataYSerie yData) {

		yData.setVisibleMaxValue(yData.getVisibleMaxValue() + 1);

		if (yData.getVisibleMinValue() > 0) {
			yData.setVisibleMinValue(yData.getVisibleMinValue() - 1);
		}
	}

	private int compareTour(final TourData compareTourData) {

		final TourDataNormalizer compareTourNormalizer = new TourDataNormalizer();
		final int[] compareTourDataDistance = compareTourData.distanceSerie;

		// normalize the tour which will be compared
		compareTourNormalizer.normalizeAltitude(compareTourData,
				0,
				compareTourDataDistance.length - 1);

		final int[] normCompDistances = compareTourNormalizer.getNormalizedDistance();
		final int[] normCompAltitudes = compareTourNormalizer.getNormalizedAltitude();
		final int[] normCompAltiDiff = new int[normCompAltitudes.length];
		final int[] normCompTour = new int[normCompAltitudes.length];

		/*
		 * reference tour
		 */

		Long refTourId;
		int refMeasureStartIndex;
		int refMeasureEndIndex;

		// Maur - Pfannenstiel
		// refTourId = 2005102416228826L;
		// refMeasureStartIndex = 50;
		// refMeasureEndIndex = 153;

		// D�bendorf - Greifensee
		// refTourId = 20059301618311L;
		// refMeasureStartIndex = 23;
		// refMeasureEndIndex = 132;

		// Egg - Pfannenstiel
		// refTourId = 2005102416228826L;
		// refMeasureStartIndex = 121;
		// refMeasureEndIndex = 167;

		// Maur - Forch
		refTourId = 200592616168216L;
		refMeasureStartIndex = 49;
		refMeasureEndIndex = 101;

		// get the reference tour
		final TourData refTourData = TourManager.getInstance().getTourData(refTourId);
		if (refTourData == null) {
			return -1;
		}

		// normalize the reference tour
		final TourDataNormalizer refTourNormalizer = new TourDataNormalizer();
		refTourNormalizer.normalizeAltitude(refTourData, refMeasureStartIndex, refMeasureEndIndex);

		final int[] refAltitudes = refTourNormalizer.getNormalizedAltitude();
		int minAltiDiff = Integer.MAX_VALUE;

		// start index of the reference tour in the compare tour
		int compareIndexStart = -1;

		final int compareLastIndex = normCompAltitudes.length;

		for (int compareIndex = 0; compareIndex < normCompAltitudes.length; compareIndex++) {

			int altitudeDiff = -1;

			// loop: all data in the reference tour
			for (int refIndex = 0; refIndex < refAltitudes.length; refIndex++) {

				final int compareRefIndex = compareIndex + refIndex;

				// make sure the ref index is not bigger than the compare index,
				// this can happen when the reference data exeed the compare
				// data
				if (compareRefIndex == compareLastIndex) {
					altitudeDiff = -1;
					break;
				}

				// get the altitude difference between the reference and the
				// measured value
				final int diffCompareRef = Math.abs(refAltitudes[refIndex]
						- normCompAltitudes[compareRefIndex]);

				altitudeDiff += diffCompareRef;
			}

			// save the altitude difference in the pulse data
			normCompAltiDiff[compareIndex] = altitudeDiff;

			// find the lowest altitude, this will be the start point for the
			// reference tour
			if (altitudeDiff < minAltiDiff && altitudeDiff != -1) {
				minAltiDiff = altitudeDiff;
				compareIndexStart = compareIndex;
			}
		}

		// show the reference tour in the temperature serie
		for (int refIndex = 0; refIndex < refAltitudes.length; refIndex++) {

			final int compareIndex = compareIndexStart + refIndex;

			// prevent out of bounds error
			if (compareIndex >= normCompTour.length) {
				break;
			}

			normCompTour[compareIndex] = normCompAltitudes[compareIndex];
		}

		// distance for the reference tour
//		final int refDistance = refTourData.distanceSerie[refMeasureEndIndex]
//				- refTourData.distanceSerie[refMeasureStartIndex];

		// get the start point in the compare tour
		final int distanceStart = normCompDistances[compareIndexStart];

		// find the start distance in the measure data
		int compareIndex = 0;
		for (compareIndex = 0; compareIndex < compareTourDataDistance.length; compareIndex++) {
			if (compareTourDataDistance[compareIndex] >= distanceStart) {
				break;
			}
		}
//		final int compareDistanceStart = compareTourDataDistance[compareIndex];

		// overwrite the changed data series
		compareTourData.distanceSerie = compareTourNormalizer.getNormalizedDistance();
		compareTourData.altitudeSerie = compareTourNormalizer.getNormalizedAltitude();
		compareTourData.pulseSerie = normCompAltiDiff;
		compareTourData.temperatureSerie = normCompTour;

		// overwrite all data series, otherwise the chart will not be drawn
		compareTourData.timeSerie = compareTourNormalizer.getNormalizedTime();
		compareTourData.speedSerie = compareTourNormalizer.getNormalizedTime();
		compareTourData.cadenceSerie = compareTourNormalizer.getNormalizedTime();

		return compareIndexStart;
	}

	/**
	 * the speed must be interpolated for low time intervals because the smallest distance is 10 m
	 * 
	 * @param tourData
	 * @param hasPropertyChanged
	 */
	private void computeAltiGradiSerie(TourData tourData, boolean hasPropertyChanged) {

		// check if altimeter was computed
		if (tourData.altimeterSerie != null && hasPropertyChanged == false) {
			return;
		}

		final int serieLength = tourData.timeSerie.length;

		final int altimeterSerie[] = tourData.altimeterSerie = new int[serieLength];
		final int gradientSerie[] = tourData.gradientSerie = new int[serieLength];

		int deviceTimeInterval = tourData.getDeviceTimeInterval();
		int indexLowAdjustment;
		int indexHighAdjustment;

		final IPreferenceStore prefStore = TourbookPlugin.getDefault().getPreferenceStore();

		if (prefStore.getBoolean(ITourbookPreferences.GRAPH_PROPERTY_IS_COMPUTE_VALUE)) {

			int computeTimeSlice = prefStore.getInt(ITourbookPreferences.GRAPH_PROPERTY_TIMESLICE_COMPUTE_VALUE);
			final int slices = computeTimeSlice / deviceTimeInterval;

			indexHighAdjustment = Math.max(1, slices / 2);
			indexLowAdjustment = slices / 2;

			// round up
			if (indexLowAdjustment + indexHighAdjustment < slices) {
				indexHighAdjustment++;
			}
		} else {
			if (deviceTimeInterval <= 2) {
				indexLowAdjustment = 15;
				indexHighAdjustment = 15;

			} else if (deviceTimeInterval <= 5) {
				indexLowAdjustment = 5;
				indexHighAdjustment = 6;

			} else if (deviceTimeInterval <= 10) {
				indexLowAdjustment = 2;
				indexHighAdjustment = 3;
			} else {
				indexLowAdjustment = 1;
				indexHighAdjustment = 2;
			}
		}

		/*
		 * compute values
		 */
		for (int serieIndex = 0; serieIndex < serieLength; serieIndex++) {

			// adjust index to the array size
			int indexLow = Math.min(Math.max(0, serieIndex - indexLowAdjustment), serieLength - 1);
			int indexHigh = Math.max(0, Math.min(serieIndex + indexHighAdjustment, serieLength - 1));

			final int distance = tourData.distanceSerie[indexHigh]
					- tourData.distanceSerie[indexLow];

			final int altitude = tourData.altitudeSerie[indexHigh]
					- tourData.altitudeSerie[indexLow];

			final float timeInterval = deviceTimeInterval * (indexHigh - indexLow);

			// keep altimeter data
			altimeterSerie[serieIndex] = (int) (3600F * altitude / timeInterval);

			// keep gradient data
			gradientSerie[serieIndex] = distance == 0 ? 0 : altitude * 1000 / distance;
		}
	}

	/**
	 * the speed must be interpolated for low time intervals because the smallest distance is 10 m
	 * 
	 * @param tourData
	 * @param hasPropertyChanged
	 * @param tourChartProperty
	 */
	private void computeSpeedSerie(TourData tourData, boolean hasPropertyChanged) {

		// check if speed was computed
		if (tourData.speedSerie != null && hasPropertyChanged == false) {
			return;
		}

		final int serieLength = tourData.timeSerie.length;

		final int speedSerie[] = tourData.speedSerie = new int[serieLength];

		int deviceTimeInterval = tourData.getDeviceTimeInterval();

		int lowIndexAdjustment = 0;
		int highIndexAdjustment = 1;

		IPreferenceStore store = TourbookPlugin.getDefault().getPreferenceStore();

		int speedTimeSlice = store.getInt(ITourbookPreferences.GRAPH_PROPERTY_TIMESLICE_COMPUTE_VALUE);
		final int slices = speedTimeSlice / deviceTimeInterval;

		highIndexAdjustment = Math.max(1, slices / 2);
		lowIndexAdjustment = slices / 2;

		// round up
		if (lowIndexAdjustment + highIndexAdjustment < slices) {
			highIndexAdjustment++;
		}

		for (int speedIndex = 0; speedIndex < serieLength; speedIndex++) {

			// adjust index to the array size
			int distIndexLow = Math.min(Math.max(0, speedIndex - lowIndexAdjustment),
					serieLength - 1);
			int distIndexHigh = Math.max(0, Math.min(speedIndex + highIndexAdjustment,
					serieLength - 1));

			final int distance = tourData.distanceSerie[distIndexHigh]
					- tourData.distanceSerie[distIndexLow];

			final float timeInterval = deviceTimeInterval * (distIndexHigh - distIndexLow);

			final int speed = (int) ((distance * 36F) / timeInterval);

			speedSerie[speedIndex] = speed;
//			speedSerie[speedIndex] = (int) (distance / timeInterval);
		}
	}

	/**
	 * the speed must be interpolated for low time intervals because the smallest distance is 10 m
	 * 
	 * @param tourData
	 * @param hasPropertyChanged
	 * @param tourChartProperty
	 */
	private void computeSpeedSerieInternal(TourData tourData, boolean hasPropertyChanged) {

		// check if speed was computed
		if (tourData.speedSerie != null && hasPropertyChanged == false) {
			return;
		}

		final int serieLength = tourData.timeSerie.length;

		final int speedSerie[] = tourData.speedSerie = new int[serieLength];

		int deviceTimeInterval = tourData.getDeviceTimeInterval();

		int lowIndexAdjustmentDefault = 0;
		int highIndexAdjustmentDefault = 0;
		if (deviceTimeInterval <= 2) {
			lowIndexAdjustmentDefault = 2;
			highIndexAdjustmentDefault = 3;

		} else if (deviceTimeInterval <= 5) {
			lowIndexAdjustmentDefault = 1;
			highIndexAdjustmentDefault = 1;

		} else if (deviceTimeInterval <= 10) {
			lowIndexAdjustmentDefault = 0;
			highIndexAdjustmentDefault = 1;
		} else {
			lowIndexAdjustmentDefault = 0;
			highIndexAdjustmentDefault = 1;
		}

		for (int speedIndex = 0; speedIndex < serieLength; speedIndex++) {

			// adjust index to the array size
			int distIndexLow = Math.min(Math.max(0, speedIndex - lowIndexAdjustmentDefault),
					serieLength - 1);

			int distIndexHigh = Math.max(0, Math.min(speedIndex + highIndexAdjustmentDefault,
					serieLength - 1));

			int distanceDefault = tourData.distanceSerie[distIndexHigh]
					- tourData.distanceSerie[distIndexLow];

			// adjust the accuracy for the distance
			int lowIndexAdjustment = lowIndexAdjustmentDefault;
			int highIndexAdjustment = highIndexAdjustmentDefault;

			if (distanceDefault < 30) {
				lowIndexAdjustment = lowIndexAdjustmentDefault + 3;
				highIndexAdjustment = highIndexAdjustmentDefault + 3;
			} else if (distanceDefault < 50) {
				lowIndexAdjustment = lowIndexAdjustmentDefault + 2;
				highIndexAdjustment = highIndexAdjustmentDefault + 2;
			} else if (distanceDefault < 100) {
				lowIndexAdjustment = lowIndexAdjustmentDefault + 1;
				highIndexAdjustment = highIndexAdjustmentDefault + 1;
			}

			// adjust index to the array size
			distIndexLow = Math.min(Math.max(0, speedIndex - lowIndexAdjustment), serieLength - 1);
			distIndexHigh = Math.max(0, Math.min(speedIndex + highIndexAdjustment, serieLength - 1));

			final int distance = tourData.distanceSerie[distIndexHigh]
					- tourData.distanceSerie[distIndexLow];

			final float timeInterval = deviceTimeInterval * (distIndexHigh - distIndexLow);

			final int speed = (int) ((distance * 36F) / timeInterval);

			speedSerie[speedIndex] = speed;
		}
	}

	/**
	 * Clip values when a minimum distance is fallen short of
	 * 
	 * @param tourData
	 */
	private void computeValueClipping(TourData tourData) {

		final IPreferenceStore prefStore = TourbookPlugin.getDefault().getPreferenceStore();

		final int[] speedSerie = tourData.speedSerie;
		final int[] altimeterSerie = tourData.altimeterSerie;
		final int[] gradientSerie = tourData.gradientSerie;
		final int[] distanceSerie = tourData.distanceSerie;

		final int serieLength = tourData.timeSerie.length;
		final int deviceTimeInterval = tourData.getDeviceTimeInterval();

		int clipTimeSlice;
		if (prefStore.getBoolean(ITourbookPreferences.GRAPH_PROPERTY_IS_CLIP_VALUE)) {
			// use custom clipping
			clipTimeSlice = prefStore.getInt(ITourbookPreferences.GRAPH_PROPERTY_TIMESLICE_CLIP_VALUE);
		} else {
			// use internal clipping, value is evaluated by experiments
			clipTimeSlice = 15;
		}

		final int slices = Math.max(1, clipTimeSlice / deviceTimeInterval);

		for (int serieIndex = 0; serieIndex < serieLength; serieIndex++) {

			// adjust index to the array size
			final int distIndex = Math.min(Math.max(0, serieIndex + slices), serieLength - 1);

			final int distance = distanceSerie[distIndex] - distanceSerie[serieIndex];

			if (distance == 0) {
				altimeterSerie[serieIndex] = 0;
				gradientSerie[serieIndex] = 0;
				speedSerie[serieIndex] = 0;

				altimeterSerie[distIndex] = 0;
				gradientSerie[distIndex] = 0;
				speedSerie[distIndex] = 0;
			}
		}
	}

	/**
	 * create the callbacks which compute the average
	 */
	private void createChartAvgCallbacks() {

		computeSpeedAvg = new ComputeChartValue() {

			/*
			 * Compute the average distance speed between the two sliders
			 */
			@Override
			public float compute() {

				final int[] distanceValues = ((ChartDataSerie) (chartModel.getCustomData(TourManager.CUSTOM_DATA_DISTANCE))).getHighValues()[0];
				final int[] timeValues = ((ChartDataSerie) (chartModel.getCustomData(TourManager.CUSTOM_DATA_TIME))).getHighValues()[0];

				final int leftDistance = distanceValues[valuesIndexLeft];
				final int rightDistance = distanceValues[valuesIndexRight];
				final int leftTime = timeValues[valuesIndexLeft];
				final int rightTime = timeValues[valuesIndexRight];

				if (leftTime == rightTime) {
					// left and right slider are at the same position
					return 0;

				} else {

					final int timeSlice = timeValues[1] - timeValues[0];

					if (timeSlice > 0) {

						final float time = rightTime
								- leftTime
								- (getIgnoreTimeSlices(distanceValues,
										valuesIndexLeft,
										valuesIndexRight,
										10 / timeSlice) * timeSlice);

						final float distance = rightDistance - leftDistance;
						final float speed = distance / time * 3.6f;

						return speed;
					}

					return 0;
				}

			}
		};

		computeAltimeterAvg = new ComputeChartValue() {

			/*
			 * Compute the average altimeter speed between the two sliders
			 */
			@Override
			public float compute() {

				final int[] altitudeValues = ((ChartDataSerie) (chartModel.getCustomData(TourManager.CUSTOM_DATA_ALTITUDE))).getHighValues()[0];

				final int[] timeValues = ((ChartDataSerie) (chartModel.getCustomData(TourManager.CUSTOM_DATA_TIME))).getHighValues()[0];

				final int leftAltitude = altitudeValues[valuesIndexLeft];
				final int rightAltitude = altitudeValues[valuesIndexRight];
				final int leftTime = timeValues[valuesIndexLeft];
				final int rightTime = timeValues[valuesIndexRight];

				if (leftTime == rightTime) {
					// left and right slider are at the same position
					return 0;
				} else {

					final int[] distanceValues = ((ChartDataSerie) (chartModel.getCustomData(TourManager.CUSTOM_DATA_DISTANCE))).getHighValues()[0];

					int timeSlice = timeValues[1] - timeValues[0];
					if (timeSlice > 0) {
						final float time = rightTime
								- leftTime
								- (getIgnoreTimeSlices(distanceValues,
										valuesIndexLeft,
										valuesIndexRight,
										10 / timeSlice) * timeSlice);

						return (((rightAltitude - leftAltitude) / time) * 3600);
					}

					return 0;
				}
			}
		};

		computeGradientAvg = new ComputeChartValue() {

			/*
			 * Compute the average altimeter speed between the two sliders
			 */
			@Override
			public float compute() {

				final int[] altitudeValues = ((ChartDataSerie) (chartModel.getCustomData(TourManager.CUSTOM_DATA_ALTITUDE))).getHighValues()[0];

				final int[] distanceValues = ((ChartDataSerie) (chartModel.getCustomData(TourManager.CUSTOM_DATA_DISTANCE))).getHighValues()[0];

				final int leftAltitude = altitudeValues[valuesIndexLeft];
				final int rightAltitude = altitudeValues[valuesIndexRight];
				final int leftDistance = distanceValues[valuesIndexLeft];
				final int rightDistance = distanceValues[valuesIndexRight];

				if (leftDistance == rightDistance) {
					// left and right slider are at the same position
					return 0;
				} else {
					return (float) ((rightAltitude - leftAltitude))
							/ (rightDistance - leftDistance)
							* 100;
				}
			}
		};
	}

	/**
	 * Creates a chart data fDataModel from the tour data
	 * 
	 * @param tourData
	 *        data which contains the tour data
	 * @param tourChartProperty
	 * @param fTourChartConfig
	 * @return
	 */
	public ChartDataModel createChartDataModel(	final TourData tourData,
												final TourChartConfiguration chartConfig) {

		return createChartDataModelInternal(tourData, chartConfig, false);
	}

	public ChartDataModel createChartDataModel(	TourData tourData,
												TourChartConfiguration chartConfig,
												boolean hasPropertyChanged) {

		return createChartDataModelInternal(tourData, chartConfig, hasPropertyChanged);
	}

//	/**
//	 * Creates a new tour context for a given tour data object
//	 * 
//	 * @param tourId
//	 * @return Returns the newly created tour context
//	 */
//	private TourEditorInput createTourEditorInput(final long tourId) {
//
//		final TourChartConfiguration chartConfiguration = createTourChartConfiguration();
//
////		chartConfiguration.setKeepMinMaxValues(true);
//
//		// create the tour editor input
//		final TourEditorInput editorInput = new TourEditorInput(tourId, chartConfiguration);
//
//		// keep the tour in a cache
////	 tourCache.put(tourData.getTourId(), editorInput);
//
//		return editorInput;
//	}

//	/**
//	 * Opens the tour editor for the the given editor input
//	 * 
//	 * @param editorInput
//	 */
//	private void openTourEditor(final TourEditorInput editorInput) {
//
//		try {
//			PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().openEditor(
//					editorInput,
//					"",//					TourEditorPart.ID,
//					true);
//
//		} catch (final PartInitException e) {
//			e.printStackTrace();
//		}
//	}

	private ChartDataModel createChartDataModelInternal(final TourData tourData,
														final TourChartConfiguration chartConfig,
														boolean hasPropertyChanged) {
//		long startTime = System.currentTimeMillis();

//		long endTime = System.currentTimeMillis();
//		System.out.println("Execution time : " + (endTime - startTime) + " ms");

		if (computeSpeedAvg == null) {
			createChartAvgCallbacks();
		}

		final ChartDataModel chartDataModel = new ChartDataModel(ChartDataModel.CHART_TYPE_LINE);

		if (tourData.timeSerie.length == 0) {
			return chartDataModel;
		}

		final IPreferenceStore prefStore = TourbookPlugin.getDefault().getPreferenceStore();

		computeAltiGradiSerie(tourData, hasPropertyChanged);

		if (prefStore.getBoolean(ITourbookPreferences.GRAPH_PROPERTY_IS_COMPUTE_VALUE)) {
			computeSpeedSerie(tourData, hasPropertyChanged);
		} else {
			computeSpeedSerieInternal(tourData, hasPropertyChanged);
		}

		computeValueClipping(tourData);

		/*
		 * distance
		 */
		final ChartDataXSerie xDataDistance = new ChartDataXSerie(tourData.distanceSerie);
		xDataDistance.setLabel(Messages.Tour_Label_distance);
		xDataDistance.setUnitLabel(Messages.Tour_Label_distance_unit);
		xDataDistance.setValueDivisor(1000);
		xDataDistance.setRgbLine(new RGB[] { new RGB(0, 0, 0) });

		/*
		 * time
		 */
		final ChartDataXSerie xDataTime = new ChartDataXSerie(tourData.timeSerie);
		xDataTime.setLabel(Messages.Tour_Label_time);
		xDataTime.setUnitLabel(Messages.Tour_Label_time_unit);
		xDataTime.setRgbLine(new RGB[] { new RGB(0, 0, 0) });
		xDataTime.setAxisUnit(ChartDataXSerie.AXIS_UNIT_HOUR_MINUTE_SECOND);

		/*
		 * show the distance on the x-axis when a distance is available, otherwise the time is
		 * displayed
		 */
		boolean showTimeOnXAxis;
		if (tourData.getTourDistance() == 0) {
			showTimeOnXAxis = true;

		} else {
			showTimeOnXAxis = chartConfig.showTimeOnXAxisBackup;
		}
		chartConfig.showTimeOnXAxis = showTimeOnXAxis;

		if (showTimeOnXAxis) {

			// time is displayed on the X axis

			chartDataModel.setXData(xDataTime);
			chartDataModel.setXData2nd(xDataDistance);

			chartDataModel.addXyData(xDataTime);
			chartDataModel.addXyData(xDataDistance);

			/*
			 * when time is displayed, the x-axis can show the start time starting from 0 or from
			 * the current time of the day
			 */
			final int startTime = chartConfig.isStartTime ? (tourData.getStartHour() * 3600)
					+ (tourData.getStartMinute() * 60) : 0;
			xDataTime.setStartValue(startTime);

		} else {

			// distance is displayed on the x axis

			chartDataModel.setXData(xDataDistance);
			chartDataModel.setXData2nd(xDataTime);

			chartDataModel.addXyData(xDataDistance);
			chartDataModel.addXyData(xDataTime);
		}

		int chartType = prefStore.getInt(ITourbookPreferences.GRAPH_PROPERTY_CHARTTYPE);

		/*
		 * altitude
		 */
		ChartDataYSerie yDataAltitude = getChartData(tourData.altitudeSerie, chartType);

		yDataAltitude.setYTitle(Messages.Graph_Label_Altitude);
		yDataAltitude.setUnitLabel(Messages.Graph_Label_Altitude_unit);
		yDataAltitude.setGraphFillMethod(ChartDataYSerie.FILL_METHOD_FILL_BOTTOM);
		yDataAltitude.setCustomData(ChartDataYSerie.YDATA_INFO, GRAPH_ALTITUDE);
		yDataAltitude.setCustomData(ANALYZER_INFO, new TourChartAnalyzerInfo(true));
		yDataAltitude.setShowYSlider(true);
		setGraphColor(prefStore, yDataAltitude, GraphColors.PREF_GRAPH_ALTITUDE);
		adjustMinMax(yDataAltitude);
		chartDataModel.addXyData(yDataAltitude);

		/*
		 * speed
		 */
		ChartDataYSerie yDataSpeed = getChartData(tourData.speedSerie, chartType);

		yDataSpeed.setYTitle(Messages.Graph_Label_Speed);
		yDataSpeed.setUnitLabel(Messages.Graph_Label_Speed_unit);
		yDataSpeed.setValueDivisor(10);
		yDataSpeed.setGraphFillMethod(ChartDataYSerie.FILL_METHOD_FILL_BOTTOM);
		yDataSpeed.setCustomData(ChartDataYSerie.YDATA_INFO, GRAPH_SPEED);
		yDataSpeed.setCustomData(ANALYZER_INFO, new TourChartAnalyzerInfo(true,
				true,
				computeSpeedAvg,
				2));
		yDataSpeed.setShowYSlider(true);
		setGraphColor(prefStore, yDataSpeed, GraphColors.PREF_GRAPH_SPEED);
		chartDataModel.addXyData(yDataSpeed);

		/*
		 * heartbeat
		 */
		ChartDataYSerie yDataPulse = getChartData(tourData.pulseSerie, chartType);
		yDataPulse.setYTitle(Messages.Graph_Label_Heartbeat);
		yDataPulse.setUnitLabel(Messages.Graph_Label_Heartbeat_unit);
		yDataPulse.setGraphFillMethod(ChartDataYSerie.FILL_METHOD_FILL_BOTTOM);
		yDataPulse.setCustomData(ChartDataYSerie.YDATA_INFO, GRAPH_PULSE);
		yDataPulse.setCustomData(ANALYZER_INFO, new TourChartAnalyzerInfo(true));
		yDataPulse.setShowYSlider(true);
		setGraphColor(prefStore, yDataPulse, GraphColors.PREF_GRAPH_HEARTBEAT);
		chartDataModel.addXyData(yDataPulse);

		/*
		 * altimeter
		 */
		ChartDataYSerie yDataAltimeter = getChartData(tourData.altimeterSerie, chartType);
		yDataAltimeter.setYTitle(Messages.Graph_Label_Altimeter);
		yDataAltimeter.setUnitLabel(Messages.Graph_Label_Altimeter_unit);
		yDataAltimeter.setGraphFillMethod(ChartDataYSerie.FILL_METHOD_FILL_ZERO);
		yDataAltimeter.setCustomData(ChartDataYSerie.YDATA_INFO, GRAPH_ALTIMETER);
		yDataAltimeter.setCustomData(ANALYZER_INFO, new TourChartAnalyzerInfo(true,
				computeAltimeterAvg));
		yDataAltimeter.setShowYSlider(true);
		setGraphColor(prefStore, yDataAltimeter, GraphColors.PREF_GRAPH_ALTIMETER);
		chartDataModel.addXyData(yDataAltimeter);

		// adjust min altitude when it's defined in the pref store
		if (prefStore.getBoolean(ITourbookPreferences.GRAPH_ALTIMETER_MIN_ENABLED)) {
			yDataAltimeter.setVisibleMinValue(prefStore.getInt(ITourbookPreferences.GRAPH_ALTIMETER_MIN_VALUE));
		}

		/*
		 * gradient
		 */
		ChartDataYSerie yDataGradient = getChartData(tourData.gradientSerie, chartType);
		yDataGradient.setYTitle(Messages.Graph_Label_Gradiend);
		yDataGradient.setUnitLabel(Messages.Graph_Label_Gradiend_unit);
		yDataGradient.setValueDivisor(GRADIENT_DIVISOR);
		yDataGradient.setGraphFillMethod(ChartDataYSerie.FILL_METHOD_FILL_ZERO);
		yDataGradient.setCustomData(ChartDataYSerie.YDATA_INFO, GRAPH_GRADIENT);
		yDataGradient.setCustomData(ANALYZER_INFO, new TourChartAnalyzerInfo(true,
				true,
				computeGradientAvg,
				1));
		yDataGradient.setShowYSlider(true);
		setGraphColor(prefStore, yDataGradient, GraphColors.PREF_GRAPH_GRADIEND);
		chartDataModel.addXyData(yDataGradient);

		// adjust min value when defined in the pref store
		if (prefStore.getBoolean(ITourbookPreferences.GRAPH_GRADIENT_MIN_ENABLED)) {
			yDataGradient.setVisibleMinValue(prefStore.getInt(ITourbookPreferences.GRAPH_GRADIENT_MIN_VALUE)
					* GRADIENT_DIVISOR);
		}

		/*
		 * cadence
		 */
		ChartDataYSerie yDataCadence = getChartData(tourData.cadenceSerie, chartType);
		yDataCadence.setYTitle(Messages.Graph_Label_Cadence);
		yDataCadence.setUnitLabel(Messages.Graph_Label_Cadence_unit);
		yDataCadence.setShowYSlider(true);
		yDataCadence.setGraphFillMethod(ChartDataYSerie.FILL_METHOD_FILL_BOTTOM);
		yDataCadence.setCustomData(ChartDataYSerie.YDATA_INFO, GRAPH_CADENCE);
		yDataCadence.setCustomData(ANALYZER_INFO, new TourChartAnalyzerInfo(true));
		setGraphColor(prefStore, yDataCadence, GraphColors.PREF_GRAPH_CADENCE);
		chartDataModel.addXyData(yDataCadence);

		/*
		 * temperature
		 */
		ChartDataYSerie yDataTemperature = getChartData(tourData.temperatureSerie, chartType);
		yDataTemperature.setYTitle(Messages.Graph_Label_Temperature);
		yDataTemperature.setUnitLabel(Messages.Graph_Label_Temperature_unit);
		yDataTemperature.setShowYSlider(true);
		yDataTemperature.setGraphFillMethod(ChartDataYSerie.FILL_METHOD_FILL_BOTTOM);
		yDataTemperature.setCustomData(ChartDataYSerie.YDATA_INFO, GRAPH_TEMPERATURE);
		yDataTemperature.setCustomData(ANALYZER_INFO, new TourChartAnalyzerInfo(true, true));
		setGraphColor(prefStore, yDataTemperature, GraphColors.PREF_GRAPH_TEMPTERATURE);
		adjustMinMax(yDataTemperature);
		chartDataModel.addXyData(yDataTemperature);

		/*
		 * all visible graphs are added as y-data to the chart data fDataModel in the sequence as
		 * they were activated
		 */
		for (final int actionId : chartConfig.getVisibleGraphs()) {

			switch (actionId) {
			case GRAPH_ALTITUDE:
				chartDataModel.addYData(yDataAltitude);
				break;

			case GRAPH_SPEED:
				chartDataModel.addYData(yDataSpeed);
				break;

			case GRAPH_ALTIMETER:
				chartDataModel.addYData(yDataAltimeter);
				break;

			case GRAPH_GRADIENT:
				chartDataModel.addYData(yDataGradient);
				break;

			case GRAPH_CADENCE:
				chartDataModel.addYData(yDataCadence);
				break;

			case GRAPH_PULSE:
				chartDataModel.addYData(yDataPulse);
				break;

			case GRAPH_TEMPERATURE:
				chartDataModel.addYData(yDataTemperature);
				break;

			default:
				break;
			}
		}

		chartDataModel.setCustomData(CUSTOM_DATA_DISTANCE, xDataDistance);
		chartDataModel.setCustomData(CUSTOM_DATA_TIME, xDataTime);
		chartDataModel.setCustomData(CUSTOM_DATA_ALTITUDE, yDataAltitude);
		chartDataModel.setCustomData(CUSTOM_DATA_SPEED, yDataSpeed);
		chartDataModel.setCustomData(CUSTOM_DATA_GRADIENT, yDataGradient);
		chartDataModel.setCustomData(CUSTOM_DATA_ALTIMETER, yDataAltimeter);
		chartDataModel.setCustomData(CUSTOM_DATA_PULSE, yDataPulse);

		return chartDataModel;
	}

	/**
	 * @param tourData
	 * @param useNormalizedData
	 */
	// public void createTour(final TourData tourData) {
	//
	// openTourEditor(createTourEditorInput(tourData));
	// }
	/**
	 * @param tourData
	 * @param useNormalizedData
	 */
	public void createTour(TourData tourData, final boolean useNormalizedData) {

		if (tourData.getTourPerson() != null) {
			// load tour from database
			tourData = TourManager.getInstance().getTourData(tourData.getTourId());
		}

		if (useNormalizedData) {
			compareTour(tourData);
		}

		// openTourEditor(createTourEditorInput(tourData));
	}

	public void firePropertyChange(int propertyId, Object propertyData) {
		Object[] allListeners = fPropertyListeners.getListeners();
		for (int i = 0; i < allListeners.length; i++) {
			final ITourPropertyListener listener = (ITourPropertyListener) allListeners[i];
			listener.propertyChanged(propertyId, propertyData);
		}
	}

	private ChartDataYSerie getChartData(final int[] dataSerie, int chartType) {

		ChartDataYSerie chartDataSerie;

		if (chartType == 0 || chartType == ChartDataModel.CHART_TYPE_LINE) {
			chartDataSerie = new ChartDataYSerie(ChartDataModel.CHART_TYPE_LINE, dataSerie);

		} else {
//			chartDataSerie = new ChartDataYSerie(ChartDataModel.CHART_TYPE_NEW,
//					new int[dataSerie.length],
//					dataSerie);
			chartDataSerie = new ChartDataYSerie(ChartDataModel.CHART_TYPE_LINE_WITH_BARS,
					dataSerie);
		}
		return chartDataSerie;
	}

	/**
	 * Get a tour from the cache, the cache is necessary because getting a tour from the database
	 * creates always a new instance
	 * 
	 * @param tourId
	 * @return Returns the tour data for the tour id or <code>null</code> when the tour is not in
	 *         the database
	 */
	public TourData getTourData(Long tourId) {

		if (fTourDataMap.containsKey(tourId)) {
			return fTourDataMap.get(tourId);
		}

		TourData tourData = TourDatabase.getTourData(tourId);

		// keep the tour data
		if (tourData != null) {
			fTourDataMap.put(tourId, tourData);
			return tourData;
		}

		return null;
	}

	/**
	 * Opens the tour for the given tour id
	 * 
	 * @param tourId
	 */
	public void openTourInEditor(final Long tourId) {

		if (tourId == null) {
			return;
		}

		try {
			PlatformUI.getWorkbench()
					.getActiveWorkbenchWindow()
					.getActivePage()
					.openEditor(new TourEditorInput(tourId), TourEditor.ID, true);

		} catch (final PartInitException e) {
			e.printStackTrace();
		}
	}

	public void removePropertyListener(ITourPropertyListener listener) {
		fPropertyListeners.remove(listener);
	}

	public void removeTourFromCache(Long tourId) {
		fTourDataMap.remove(tourId);
	}

}
