/*******************************************************************************
 * Copyright (C) 2005, 2008  Wolfgang Schramm and Contributors
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
package net.tourbook.data;

import static javax.persistence.CascadeType.ALL;
import static javax.persistence.FetchType.EAGER;

import java.awt.Point;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.PostLoad;
import javax.persistence.PostUpdate;
import javax.persistence.Transient;

import net.tourbook.Messages;
import net.tourbook.chart.ChartMarker;
import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.ui.UI;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.graphics.Rectangle;
import org.hibernate.annotations.Cascade;

/**
 * Tour data contains all data for a tour (except markers), an entity will be saved in the database
 */
@Entity
public class TourData {

	/**
	 * 
	 */
	@Transient
	public static final int			MIN_TIMEINTERVAL_FOR_MAX_SPEED	= 20;

	@Transient
	public static final float		MAX_BIKE_SPEED					= 120f;

	/**
	 * persistence unique id which identifies the tour
	 */
	@Id
	private Long					tourId;

	/**
	 * HH (d) hour of tour
	 */
	private short					startHour;

	/**
	 * MM (d) minute of tour
	 */
	private short					startMinute;

	/**
	 * year of tour start
	 */
	private short					startYear;

	/**
	 * mm (d) month of tour
	 */
	private short					startMonth;

	/**
	 * dd (d) day of tour
	 */
	private short					startDay;

	/**
	 * week of the tour, 0 is the first week
	 */
	private short					startWeek;

	/**
	 * total distance of the device at tour start (km) tttt (h), the distance for the tour is stored
	 * the field tourDistance
	 */
	private int						startDistance;

	/**
	 * ssss distance msw
	 * <p>
	 * is not used any more since 6.12.2006 but it's necessary then it's a field in the database
	 */
	@SuppressWarnings("unused")//$NON-NLS-1$
	private int						distance;

	/**
	 * aaaa (h) initial altitude (m)
	 */
	private short					startAltitude;

	/**
	 * pppp (h) initial pulse (bpm)
	 */
	private short					startPulse;

	/**
	 * tolerance for the Douglas Peucker algorithm
	 */
	private short					dpTolerance						= 50;

	/**
	 * tt (h) type of tour <br>
	 * "2E" bike2 (CM414M) <br>
	 * "3E" bike1 (CM414M) <br>
	 * "81" jogging <br>
	 * "91" ski <br>
	 * "A1" bike<br>
	 * "B1" ski-bike
	 */
	@Column(length = 2)
	private String					deviceTourType;

	/*
	 * data from the device
	 */
	private long					deviceTravelTime;

	private int						deviceDistance;
	private int						deviceWheel;

	private int						deviceWeight;

	private int						deviceTotalUp;
	private int						deviceTotalDown;

	/**
	 * total distance of the tour (m), this value is computed from the distance data serie
	 */
	private int						tourDistance;

	/**
	 * total recording time (sec)
	 */
	private int						tourRecordingTime;

	/**
	 * total driving time (sec)
	 */
	private int						tourDrivingTime;

	/**
	 * altitude up (m)
	 */
	private int						tourAltUp;

	/**
	 * altitude down (m)
	 */
	private int						tourAltDown;

	/**
	 * plugin id for the device which was used for this tour
	 */
	private String					devicePluginId;

	/**
	 * Profile used by the device
	 */
	private short					deviceMode;														// db-version 3

	/**
	 * time difference between 2 time slices or <code>-1</code> for GPS devices when the time slices
	 * are unequally
	 */
	private short					deviceTimeInterval;												// db-version 3

	/**
	 * maximum altitude in metric system
	 */
	private int						maxAltitude;														// db-version 4

	private int						maxPulse;															// db-version 4

	/**
	 * maximum speed in metric system
	 */
	private float					maxSpeed;															// db-version 4

	private int						avgPulse;															// db-version 4

	private int						avgCadence;														// db-version 4

	private int						avgTemperature;													// db-version 4
	private String					tourTitle;															// db-version 4
	private String					tourDescription;													// db-version 4

	private String					tourStartPlace;													// db-version 4
	private String					tourEndPlace;														// db-version 4

	private String					calories;															// db-version 4
	private float					bikerWeight;														// db-version 4

	/**
	 * visible name for the used plugin to import the data
	 */
	private String					devicePluginName;													// db-version 4

	/**
	 * visible name for {@link #deviceMode}
	 */
	private String					deviceModeName;													// db-version 4
	/**
	 * data series for time, speed, altitude,...
	 */
	@Basic(optional = false)
	private SerieData				serieData;

	@OneToMany(mappedBy = "tourData", fetch = FetchType.EAGER, cascade = ALL)//$NON-NLS-1$
	@Cascade(org.hibernate.annotations.CascadeType.DELETE_ORPHAN)
	private Set<TourMarker>			tourMarkers						= new HashSet<TourMarker>();

	@OneToMany(fetch = FetchType.EAGER, cascade = ALL, mappedBy = "tourData")//$NON-NLS-1$
	@Cascade(org.hibernate.annotations.CascadeType.DELETE_ORPHAN)
	private Set<TourReference>		tourReferences					= new HashSet<TourReference>();

	@ManyToMany(fetch = EAGER)
	@JoinTable(inverseJoinColumns = @JoinColumn(name = "tourTag_tagId", referencedColumnName = "tagId"))
	private Set<TourTag>			tourTags						= new HashSet<TourTag>();

	/**
	 * Category of the tour, e.g. bike, mountainbike, jogging, inlinescating
	 */
	@ManyToOne
	private TourType				tourType;

	/**
	 * Person which created this tour or <code>null</code> when the tour is not saved in the
	 * database
	 */
	@ManyToOne
	private TourPerson				tourPerson;

	/**
	 * plugin id for the device which was used for this tour Bike used for this tour
	 */
	@ManyToOne
	private TourBike				tourBike;

	/*
	 * tourCategory is currently (version 1.6) not used but is defined in older databases, it is
	 * disabled because the field is not available in the database table
	 */
	//	@ManyToMany(fetch = FetchType.LAZY, mappedBy = "tourData")
	//	private Set<TourCategory>	tourCategory					= new HashSet<TourCategory>();
	//
	//
	/////////////////////////////////////////////////////////////////////
	/*
	 * TRANSIENT DATA
	 */
	/////////////////////////////////////////////////////////////////////
	/**
	 * contains the relative time in seconds, {@link #startHour} and {@link #startMinute} contains
	 * the absolute time when a tour is started
	 */
	@Transient
	public int[]					timeSerie;

	/**
	 * contains the absolute distance in m (metric system)
	 */
	@Transient
	public int[]					distanceSerie;

	/**
	 * contains the absolute distance in miles/1000 (imperial system)
	 */
	@Transient
	private int[]					distanceSerieImperial;

	/**
	 * contains the absolute altitude in m (metric system)
	 */
	@Transient
	public int[]					altitudeSerie;

	/**
	 * contains the absolute altitude in feet (imperial system)
	 */
	@Transient
	private int[]					altitudeSerieImperial;

	@Transient
	public int[]					cadenceSerie;

	@Transient
	public int[]					pulseSerie;

	@Transient
	public int[]					temperatureSerie;

	/**
	 * contains the temperature in the imperial measurement system
	 */
	@Transient
	private int[]					temperatureSerieImperial;

	/**
	 * the metric speed serie is required form computing the power even if the current measurement
	 * system is imperial
	 */
	@Transient
	private int[]					speedSerie;

	@Transient
	private int[]					speedSerieImperial;

	/**
	 * Is <code>true</code> when the data in {@link #speedSerie} are from the device and not
	 * computed. Speed data are normally available from an ergometer and not from a bike computer
	 */
	@Transient
	private boolean					isSpeedSerieFromDevice			= false;

	@Transient
	private int[]					paceSerie;

	/*
	 * computed data series
	 */

	@Transient
	private int[]					paceSerieImperial;
	@Transient
	private int[]					powerSerie;

	/**
	 * Is <code>true</code> when the data in {@link #powerSerie} are from the device and not
	 * computed. Power data are normally available from an ergometer and not from a bike computer
	 */
	@Transient
	private boolean					isPowerSerieFromDevice			= false;

	@Transient
	private int[]					altimeterSerie;
	@Transient
	private int[]					altimeterSerieImperial;

	@Transient
	public int[]					gradientSerie;

	@Transient
	public int[]					tourCompareSerie;

	/*
	 * GPS data
	 */
	@Transient
	public double[]					latitudeSerie;
	@Transient
	public double[]					longitudeSerie;

	/**
	 * contains the bounds of the tour in latitude/longitude
	 */
	@Transient
	public Rectangle				gpsBounds;

	/**
	 * Index of the segmented data in the original serie
	 */
	@Transient
	public int[]					segmentSerieIndex;

	/**
	 * oooo (o) DD-record // offset
	 */
	@Transient
	public int						offsetDDRecord;
	@Transient
	protected Object[]				fTourSegments;

	/*
	 * data for the tour segments
	 */
	@Transient
	public int[]					segmentSerieAltitude;

	@Transient
	public int[]					segmentSerieDistance;

	@Transient
	public int[]					segmentSerieTime;

	@Transient
	public int[]					segmentSerieDrivingTime;

	@Transient
	public float[]					segmentSerieAltimeter;

	@Transient
	public int[]					segmentSerieAltitudeDown;

	@Transient
	public float[]					segmentSerieSpeed;

	@Transient
	public float[]					segmentSeriePace;

	@Transient
	public float[]					segmentSeriePower;

	@Transient
	public float[]					segmentSerieGradient;

	@Transient
	public float[]					segmentSeriePulse;

	@Transient
	public float[]					segmentSerieCadence;

	/**
	 * contains the filename from which the data are imported, when set to <code>null</code> the
	 * data are not imported they are from the database
	 */
	@Transient
	public String					importRawDataFile;

	/**
	 * Latitude for the center position in the map or {@link Double#MIN_VALUE} when the position is
	 * not set
	 */
	@Transient
	public double					mapCenterPositionLatitude		= Double.MIN_VALUE;

	/**
	 * Longitude for the center position in the map or {@link Double#MIN_VALUE} when the position is
	 * not set
	 */
	@Transient
	public double					mapCenterPositionLongitude		= Double.MIN_VALUE;

	/**
	 * Zoomlevel in the map
	 */
	@Transient
	public int						mapZoomLevel;

	@Transient
	public double					mapMinLatitude;

	@Transient
	public double					mapMaxLatitude;

	@Transient
	public double					mapMinLongitude;

	@Transient
	public double					mapMaxLongitude;
	/**
	 * caches the world positions for lat/long values for each zoom level
	 */
	@Transient
	public Map<Integer, Point[]>	fWorldPosition					= new HashMap<Integer, Point[]>();
	/**
	 * when a tour was deleted and is still visible in the raw data view, resaving the tour or
	 * finding the tour in the entity manager causes lots of trouble with hibernate, therefor this
	 * tour cannot be saved again, it must be reloaded from the file system
	 */
	@Transient
	public boolean					isTourDeleted					= false;

	public TourData() {}

	/**
	 * clear imperial altitude series so the next time when it's needed it will be recomputed
	 */
	public void clearAltitudeSeries() {
		altitudeSerieImperial = null;
	}

	/**
	 * clear computed data series so the next time when they are needed they will be recomputed
	 */
	public void clearComputedSeries() {

		if (isSpeedSerieFromDevice == false) {
			speedSerie = null;
		}
		if (isPowerSerieFromDevice == false) {
			powerSerie = null;
		}

		paceSerie = null;
		altimeterSerie = null;

		speedSerieImperial = null;
		paceSerieImperial = null;
		altimeterSerieImperial = null;
		altitudeSerieImperial = null;
	}

	public void computeAltimeterGradientSerie() {

		// optimization: don't recreate the data series when they are available
		if (altimeterSerie != null && altimeterSerieImperial != null && gradientSerie != null) {
			return;
		}

		if (deviceTimeInterval == -1) {
			computeAltimeterGradientSerieWithVariableInterval();
		} else {
			computeAltimeterGradientSerieWithFixedInterval();
		}
	}

	/**
	 * Computes the data serie for altimeters with the internal algorithm for a fix time interval
	 */
	private void computeAltimeterGradientSerieWithFixedInterval() {

		if (distanceSerie == null || altitudeSerie == null) {
			return;
		}

		final int serieLength = timeSerie.length;

		final int dataSerieAltimeter[] = new int[serieLength];
		final int dataSerieGradient[] = new int[serieLength];

		int adjustIndexLow;
		int adjustmentIndexHigh;

		final IPreferenceStore prefStore = TourbookPlugin.getDefault().getPreferenceStore();

		if (prefStore.getBoolean(ITourbookPreferences.GRAPH_PROPERTY_IS_VALUE_COMPUTING)) {

			// use custom settings to compute altimeter and gradient

			final int computeTimeSlice = prefStore.getInt(ITourbookPreferences.GRAPH_PROPERTY_CUSTOM_VALUE_TIMESLICE);
			final int slices = computeTimeSlice / deviceTimeInterval;

			final int slice2 = slices / 2;
			adjustmentIndexHigh = (1 >= slice2) ? 1 : slice2;
			adjustIndexLow = slice2;

			// round up
			if (adjustIndexLow + adjustmentIndexHigh < slices) {
				adjustmentIndexHigh++;
			}

		} else {

			// use internal algorithm to compute altimeter and gradient

//			if (deviceTimeInterval <= 2) {
//				adjustIndexLow = 15;
//				adjustmentIndexHigh = 15;
//				
//			} else if (deviceTimeInterval <= 5) {
//				adjustIndexLow = 5;
//				adjustmentIndexHigh = 6;
//				
//			} else if (deviceTimeInterval <= 10) {
//				adjustIndexLow = 2;
//				adjustmentIndexHigh = 3;
//			} else {
//				adjustIndexLow = 1;
//				adjustmentIndexHigh = 2;
//			}

			if (deviceTimeInterval <= 2) {
				adjustIndexLow = 15;
				adjustmentIndexHigh = 15;

			} else if (deviceTimeInterval <= 5) {
				adjustIndexLow = 4;
				adjustmentIndexHigh = 4;

			} else if (deviceTimeInterval <= 10) {
				adjustIndexLow = 2;
				adjustmentIndexHigh = 3;
			} else {
				adjustIndexLow = 1;
				adjustmentIndexHigh = 2;
			}
		}

		/*
		 * compute values
		 */

		for (int serieIndex = 0; serieIndex < serieLength; serieIndex++) {

			/*
			 * adjust index to the array size, this is optimized to NOT use Math.min or Math.max
			 */
			final int serieLengthLow = serieLength - 1;

			final int indexLowTemp = serieIndex - adjustIndexLow;
			final int indexLowTempMax = ((0 >= indexLowTemp) ? 0 : indexLowTemp);
			final int indexLow = ((indexLowTempMax <= serieLengthLow) ? indexLowTempMax : serieLengthLow);

			final int indexHighTemp = serieIndex + adjustmentIndexHigh;
			final int indexHighTempMin = ((indexHighTemp <= serieLengthLow) ? indexHighTemp : serieLengthLow);
			final int indexHigh = ((0 >= indexHighTempMin) ? 0 : indexHighTempMin);

			final int distanceDiff = distanceSerie[indexHigh] - distanceSerie[indexLow];
			final int altitudeDiff = altitudeSerie[indexHigh] - altitudeSerie[indexLow];

			final float timeDiff = deviceTimeInterval * (indexHigh - indexLow);

			// keep altimeter data
			dataSerieAltimeter[serieIndex] = (int) (3600F * altitudeDiff / timeDiff / UI.UNIT_VALUE_ALTITUDE);

			// keep gradient data
			dataSerieGradient[serieIndex] = distanceDiff == 0 ? 0 : altitudeDiff * 1000 / distanceDiff;
		}

		if (UI.UNIT_VALUE_ALTITUDE != 1) {

			// set imperial system

			altimeterSerieImperial = dataSerieAltimeter;

		} else {

			// set metric system

			altimeterSerie = dataSerieAltimeter;
		}

		gradientSerie = dataSerieGradient;
	}

	/**
	 * Computes the data serie for gradient and altimeters for a variable time interval
	 */
	private void computeAltimeterGradientSerieWithVariableInterval() {

		if (distanceSerie == null || altitudeSerie == null) {
			return;
		}

		final int[] checkSpeedSerie = getSpeedSerie();

		final int serieLength = timeSerie.length;
		final int serieLengthLast = serieLength - 1;

		final int dataSerieAltimeter[] = new int[serieLength];
		final int dataSerieGradient[] = new int[serieLength];

		final IPreferenceStore prefStore = TourbookPlugin.getDefault().getPreferenceStore();
		final boolean isCustomProperty = prefStore.getBoolean(ITourbookPreferences.GRAPH_PROPERTY_IS_VALUE_COMPUTING);

		// get minimum difference
		int minTimeDiff;
		if (isCustomProperty) {
			// use custom settings to compute altimeter and gradient
			minTimeDiff = prefStore.getInt(ITourbookPreferences.GRAPH_PROPERTY_CUSTOM_VALUE_TIMESLICE);
		} else {
			// use internal algorithm to compute altimeter and gradient
			minTimeDiff = 16;
		}
		final int minDistanceDiff = minTimeDiff;

		final boolean checkPosition = latitudeSerie != null && longitudeSerie != null;

		for (int serieIndex = 0; serieIndex < serieLength; serieIndex++) {

			if (checkSpeedSerie[serieIndex] == 0) {
				// continue when no speed is available
//				dataSerieAltimeter[serieIndex] = 2000;
				continue;
			}

			final int sliceTimeDiff = timeSerie[serieIndex] - timeSerie[serieIndex - 1];

			// check if a lat and long diff is available
			if (checkPosition && serieIndex > 0 && serieIndex < serieLengthLast - 1) {

				if (sliceTimeDiff > 10) {

					if (latitudeSerie[serieIndex] == latitudeSerie[serieIndex - 1]
							&& longitudeSerie[serieIndex] == longitudeSerie[serieIndex - 1]) {
//						dataSerieAltimeter[serieIndex] = 100;
						continue;
					}

					if (distanceSerie[serieIndex] == distanceSerie[serieIndex - 1]) {
//						dataSerieAltimeter[serieIndex] = 120;
						continue;
					}

					if (altitudeSerie[serieIndex] == altitudeSerie[serieIndex - 1]) {
//						dataSerieAltimeter[serieIndex] = 130;
						continue;
					}
				}
			}
			final int serieIndexPrev = serieIndex - 1;

			// adjust index to the array size
			int lowIndex = ((0 >= serieIndexPrev) ? 0 : serieIndexPrev);
			int highIndex = ((serieIndex <= serieLengthLast) ? serieIndex : serieLengthLast);

			int timeDiff = timeSerie[highIndex] - timeSerie[lowIndex];
			int distanceDiff = distanceSerie[highIndex] - distanceSerie[lowIndex];
			int altitudeDiff = altitudeSerie[highIndex] - altitudeSerie[lowIndex];

			boolean toggleIndex = true;

			while (timeDiff < minTimeDiff || distanceDiff < minDistanceDiff) {

				// toggle between low and high index
				if (toggleIndex) {
					lowIndex--;
				} else {
					highIndex++;
				}
				toggleIndex = !toggleIndex;

				// check array scope
				if (lowIndex < 0 || highIndex >= serieLength) {
					break;
				}

				timeDiff = timeSerie[highIndex] - timeSerie[lowIndex];
				distanceDiff = distanceSerie[highIndex] - distanceSerie[lowIndex];
				altitudeDiff = altitudeSerie[highIndex] - altitudeSerie[lowIndex];

			}

			highIndex = (highIndex <= serieLengthLast) ? highIndex : serieLengthLast;
			lowIndex = (lowIndex >= 0) ? lowIndex : 0;

			/*
			 * check if a time difference is available between 2 time data, this can happen in gps
			 * data that lat+long is available but no time
			 */
			boolean isTimeValid = true;
			int prevTime = timeSerie[lowIndex];
			for (int timeIndex = lowIndex + 1; timeIndex <= highIndex; timeIndex++) {
				final int currentTime = timeSerie[timeIndex];
				if (prevTime == currentTime) {
					isTimeValid = false;
					break;
				}
				prevTime = currentTime;
			}

			if (isTimeValid) {

				if (timeDiff > 50 && isCustomProperty == false) {
//					dataSerieAltimeter[serieIndex] = 300;
					continue;
				}

				// check if lat and long diff is available
				if (checkPosition && lowIndex > 0 && highIndex < serieLengthLast - 1) {

					if (sliceTimeDiff > 10) {

						if (latitudeSerie[lowIndex] == latitudeSerie[lowIndex - 1]
								&& longitudeSerie[lowIndex] == longitudeSerie[lowIndex - 1]) {
//							dataSerieAltimeter[serieIndex] = 210;
							continue;
						}
						if (latitudeSerie[highIndex] == latitudeSerie[highIndex + 1]
								&& longitudeSerie[highIndex] == longitudeSerie[highIndex + 1]) {
//							dataSerieAltimeter[serieIndex] = 220;
							continue;
						}
					}
				}

				// compute altimeter 
				if (timeDiff > 0) {
					final int altimeter = (int) (3600f * altitudeDiff / timeDiff / UI.UNIT_VALUE_ALTITUDE);
					dataSerieAltimeter[serieIndex] = altimeter;
				} else {
//					dataSerieAltimeter[serieIndex] = -100;
				}

				// compute gradient 
				if (distanceDiff > 0) {
					final int gradient = altitudeDiff * 1000 / distanceDiff;
					dataSerieGradient[serieIndex] = gradient;
				} else {
//					dataSerieAltimeter[serieIndex] = -200;
				}

			} else {
//				dataSerieAltimeter[serieIndex] = -300;
			}
		}

		if (UI.UNIT_VALUE_ALTITUDE != 1) {

			// set imperial system

			altimeterSerieImperial = dataSerieAltimeter;

		} else {

			// set metric system

			altimeterSerie = dataSerieAltimeter;
		}

		gradientSerie = dataSerieGradient;
	}

	private void computeAvgCadence() {

		if (cadenceSerie == null) {
			return;
		}

		long cadenceSum = 0;
		int cadenceCount = 0;

		for (final int cadence : cadenceSerie) {
			if (cadence > 0) {
				cadenceCount++;
				cadenceSum += cadence;
			}
		}
		if (cadenceCount > 0) {
			avgCadence = (int) cadenceSum / cadenceCount;
		}
	}

	private void computeAvgPulse() {

		if (pulseSerie == null) {
			return;
		}

		long pulseSum = 0;
		int pulseCount = 0;

		for (final int pulse : pulseSerie) {
			if (pulse > 0) {
				pulseCount++;
				pulseSum += pulse;
			}
		}

		if (pulseCount > 0) {
			avgPulse = (int) pulseSum / pulseCount;
		}
	}

	private void computeAvgTemperature() {

		if (temperatureSerie == null) {
			return;
		}

		long temperatureSum = 0;

		for (final int temperature : temperatureSerie) {
			temperatureSum += temperature;
		}

		final int tempLength = temperatureSerie.length;
		if (tempLength > 0) {
			avgTemperature = (int) temperatureSum / tempLength;
		}
	}

	private int computeBreakTimeVariable(final int minStopTime, final int startIndex, int endIndex) {

		endIndex = Math.min(endIndex, timeSerie.length - 1);

		int lastMovingDistance = 0;
		int lastMovingTime = 0;

		int totalBreakTime = 0;
		int breakTime = 0;
		int currentBreakTime = 0;

//		cadenceSerie = new int[timeSerie.length];

		for (int serieIndex = startIndex; serieIndex <= endIndex; serieIndex++) {

			final int currentDistance = distanceSerie[serieIndex];
			final int currentTime = timeSerie[serieIndex];

			final int timeDiff = currentTime - lastMovingTime;
			final int distDiff = currentDistance - lastMovingDistance;

			if (distDiff == 0 || timeDiff > 20 && distDiff < 10) {

				// distance has not changed, check if a longer stop is done

				// speed must be greater than 1.8 km/h

				final int breakDiff = currentTime - currentBreakTime;

				breakTime += breakDiff;

//				int breakValue = 0;
				if (timeDiff > minStopTime) {

					// person has stopped for a break
					totalBreakTime += breakTime;

					breakTime = 0;
					currentBreakTime = currentTime;

//					breakValue = -500 - breakTime;

				} else {

//					breakValue = 20;
				}

//				if (distDiff == 0) {
//					cadenceSerie[serieIndex] = 100 + breakValue;
//				} else if (timeDiff > 20) {
//					cadenceSerie[serieIndex] = 200 + breakValue;
//				} else if (distDiff < 5) {
//					cadenceSerie[serieIndex] = 300 + breakValue;
//				}

			} else {

				// keep time and distance when the distance is changing
				lastMovingTime = currentTime;
				lastMovingDistance = currentDistance;

				breakTime = 0;
				currentBreakTime = currentTime;

//				cadenceSerie[serieIndex] = 0;
			}
		}

		return totalBreakTime;
	}

	private void computeMaxAltitude() {

		if (altitudeSerie == null) {
			return;
		}

		int maxAltitude = 0;
		for (final int altitude : altitudeSerie) {
			if (altitude > maxAltitude) {
				maxAltitude = altitude;
			}
		}
		this.maxAltitude = maxAltitude;
	}

	private void computeMaxPulse() {

		if (pulseSerie == null) {
			return;
		}

		int maxPulse = 0;

		for (final int pulse : pulseSerie) {
			if (pulse > maxPulse) {
				maxPulse = pulse;
			}
		}
		this.maxPulse = maxPulse;
	}

	private void computeMaxSpeed() {
		if (distanceSerie != null) {
			computeSpeedSerie();
		}
	}

	/**
	 * computes the speed data serie which can be retrieved with {@link TourData#getSpeedSerie()}
	 */
	public void computeSpeedSerie() {

		if (speedSerie != null && speedSerieImperial != null && paceSerie != null && paceSerieImperial != null) {
			return;
		}

		if (isSpeedSerieFromDevice) {

			// speed is from the device

			computeSpeedSerieFromDevice();

		} else {

			// speed is computed from distance and time

			final IPreferenceStore prefStore = TourbookPlugin.getDefault().getPreferenceStore();

			if (prefStore.getBoolean(ITourbookPreferences.GRAPH_PROPERTY_IS_VALUE_COMPUTING)) {

				// compute speed for custom settings

				if (deviceTimeInterval == -1) {
					computeSpeedSerieInternalWithVariableInterval();
				} else {
					computeSpeedSerieCustomWithFixedInterval();
				}
			} else {

				// compute speed with internal algorithm

				if (deviceTimeInterval == -1) {
					computeSpeedSerieInternalWithVariableInterval();
				} else {
					computeSpeedSerieInternalWithFixedInterval();
				}
			}
		}
	}

	private void computeSpeedSerieCustomWithFixedInterval() {

		if (distanceSerie == null) {
			return;
		}

		final int serieLength = timeSerie.length;

		speedSerie = new int[serieLength];
		speedSerieImperial = new int[serieLength];
		paceSerie = new int[serieLength];
		paceSerieImperial = new int[serieLength];

		int lowIndexAdjustment = 0;
		int highIndexAdjustment = 1;

		final int speedTimeSlice = TourbookPlugin.getDefault()
				.getPreferenceStore()
				.getInt(ITourbookPreferences.GRAPH_PROPERTY_CUSTOM_VALUE_TIMESLICE);

		final int slices = speedTimeSlice / deviceTimeInterval;

		final int slice2 = slices / 2;
		highIndexAdjustment = (1 >= slice2) ? 1 : slice2;
		lowIndexAdjustment = slice2;

		// round up
		if (lowIndexAdjustment + highIndexAdjustment < slices) {
			highIndexAdjustment++;
		}

		final int serieLengthLast = serieLength - 1;

		for (int serieIndex = 0; serieIndex < serieLength; serieIndex++) {

			// adjust index to the array size, this is optimized to NOT use Math functions
			final int serieIndexLow = serieIndex - lowIndexAdjustment;
			final int serieIndexLowMax = ((0 >= serieIndexLow) ? 0 : serieIndexLow);
			final int distIndexLow = ((serieIndexLowMax <= serieLengthLast) ? serieIndexLowMax : serieLengthLast);

			final int serieIndexHigh = serieIndex + highIndexAdjustment;
			final int serieIndexHighMin = ((serieIndexHigh <= serieLengthLast) ? serieIndexHigh : serieLengthLast);
			final int distIndexHigh = ((0 >= serieIndexHighMin) ? 0 : serieIndexHighMin);

			final int distance = distanceSerie[distIndexHigh] - distanceSerie[distIndexLow];
			final float time = deviceTimeInterval * (distIndexHigh - distIndexLow);

			/*
			 * speed
			 */
			int speedMetric = 0;
			int speedImperial = 0;
			if (time != 0) {
				final float speed = (distance * 36F) / time;
				speedMetric = (int) (speed);
				speedImperial = (int) (speed / UI.UNIT_MILE);
			}

			speedSerie[serieIndex] = speedMetric;
			speedSerieImperial[serieIndex] = speedImperial;

			maxSpeed = Math.max(maxSpeed, speedMetric);

			/*
			 * pace
			 */
			int paceMetric = 0;
			int paceImperial = 0;

			if (speedMetric != 0 && distance != 0) {
				final float pace = time * 166.66f / distance;
				paceMetric = (int) (pace);
				paceImperial = (int) (pace * UI.UNIT_MILE);
			}

			paceSerie[serieIndex] = paceMetric;
			paceSerieImperial[serieIndex] = paceImperial;
		}

		maxSpeed /= 10;
	}

	/**
	 * Computes the imperial speed data serie and max speed
	 * 
	 * @return
	 */
	private void computeSpeedSerieFromDevice() {

		if (speedSerie == null) {
			return;
		}

		final int serieLength = speedSerie.length;

		speedSerieImperial = new int[serieLength];

		for (int serieIndex = 0; serieIndex < serieLength; serieIndex++) {

			/*
			 * speed
			 */

			final int speedMetric = speedSerie[serieIndex];

			speedSerieImperial[serieIndex] = ((int) (speedMetric / UI.UNIT_MILE));
			maxSpeed = Math.max(maxSpeed, speedMetric);
		}

		maxSpeed /= 10;
	}

	/**
	 * Computes the speed data serie with the internal algorithm for a fix time interval
	 * 
	 * @return
	 */
	private void computeSpeedSerieInternalWithFixedInterval() {

		if (distanceSerie == null) {
			return;
		}

		final int serieLength = timeSerie.length;

		speedSerie = new int[serieLength];
		speedSerieImperial = new int[serieLength];
		paceSerie = new int[serieLength];
		paceSerieImperial = new int[serieLength];

		int lowIndexAdjustmentDefault = 0;
		int highIndexAdjustmentDefault = 0;

		if (deviceTimeInterval <= 2) {
			lowIndexAdjustmentDefault = 3;
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

		final int serieLengthLast = serieLength - 1;

		for (int serieIndex = 0; serieIndex < serieLength; serieIndex++) {

			// adjust index to the array size
			final int serieIndexLow = serieIndex - lowIndexAdjustmentDefault;
			final int serieIndexLowMax = ((0 >= serieIndexLow) ? 0 : serieIndexLow);
			int distIndexLow = ((serieIndexLowMax <= serieLengthLast) ? serieIndexLowMax : serieLengthLast);

			final int serieIndexHigh = serieIndex + highIndexAdjustmentDefault;
			final int serieIndexHighMax = ((serieIndexHigh <= serieLengthLast) ? serieIndexHigh : serieLengthLast);
			int distIndexHigh = ((0 >= serieIndexHighMax) ? 0 : serieIndexHighMax);

			final int distanceDefault = distanceSerie[distIndexHigh] - distanceSerie[distIndexLow];

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
			final int serieIndexLowAdjusted = serieIndex - lowIndexAdjustment;
			final int serieIndexLowAdjustedMax = ((0 >= serieIndexLowAdjusted) ? 0 : serieIndexLowAdjusted);

			distIndexLow = (serieIndexLowAdjustedMax <= serieLengthLast) ? serieIndexLowAdjustedMax : serieLengthLast;

			final int serieIndexHighAdjusted = serieIndex + highIndexAdjustment;
			final int serieIndexHighAdjustedMin = ((serieIndexHighAdjusted <= serieLengthLast)
					? serieIndexHighAdjusted
					: serieLengthLast);

			distIndexHigh = (0 >= serieIndexHighAdjustedMin) ? 0 : serieIndexHighAdjustedMin;

			final int distance = distanceSerie[distIndexHigh] - distanceSerie[distIndexLow];
			final float time = timeSerie[distIndexHigh] - timeSerie[distIndexLow];

			/*
			 * speed
			 */
			int speedMetric = 0;
			int speedImperial = 0;
			if (time != 0) {
				final float speed = (distance * 36F) / time;
				speedMetric = (int) (speed);
				speedImperial = (int) (speed / UI.UNIT_MILE);
			}

			speedSerie[serieIndex] = speedMetric;
			speedSerieImperial[serieIndex] = speedImperial;

			maxSpeed = Math.max(maxSpeed, speedMetric);

			/*
			 * pace
			 */
			int paceMetric = 0;
			int paceImperial = 0;

			if (speedMetric != 0 && distance != 0) {
				final float pace = time * 166.66f / distance;
				paceMetric = (int) (pace);
				paceImperial = (int) (pace * UI.UNIT_MILE);
			}
			paceSerie[serieIndex] = paceMetric;
			paceSerieImperial[serieIndex] = paceImperial;
		}

		maxSpeed /= 10;
	}

	/**
	 * compute the speed when the time serie has unequal time intervalls
	 */
	private void computeSpeedSerieInternalWithVariableInterval() {

		if (distanceSerie == null) {
			return;
		}

		int minTimeDiff;
		final IPreferenceStore prefStore = TourbookPlugin.getDefault().getPreferenceStore();

		if (prefStore.getBoolean(ITourbookPreferences.GRAPH_PROPERTY_IS_VALUE_COMPUTING)) {
			minTimeDiff = prefStore.getInt(ITourbookPreferences.GRAPH_PROPERTY_CUSTOM_VALUE_TIMESLICE);
			minTimeDiff = minTimeDiff < 1 ? 1 : minTimeDiff;
		} else {
			minTimeDiff = 10;
		}

		final int serieLength = timeSerie.length;
		final int serieLengthLast = serieLength - 1;

		speedSerie = new int[serieLength];
		speedSerieImperial = new int[serieLength];
		paceSerie = new int[serieLength];
		paceSerieImperial = new int[serieLength];

		final boolean checkPosition = latitudeSerie != null && longitudeSerie != null;

		for (int serieIndex = 0; serieIndex < serieLength; serieIndex++) {

			// check if a lat and long diff is available
			if (checkPosition && serieIndex > 0 && serieIndex < serieLengthLast - 1) {

				if (latitudeSerie[serieIndex] == latitudeSerie[serieIndex - 1]
						&& longitudeSerie[serieIndex] == longitudeSerie[serieIndex - 1]) {
					continue;
				}
			}

			final int serieIndexPrev = serieIndex - 1;

			// adjust index to the array size
			int lowIndex = ((0 >= serieIndexPrev) ? 0 : serieIndexPrev);
			int highIndex = ((serieIndex <= serieLengthLast) ? serieIndex : serieLengthLast);

			int timeDiff = timeSerie[highIndex] - timeSerie[lowIndex];
			int distDiff = distanceSerie[highIndex] - distanceSerie[lowIndex];

			boolean toggleIndex = true;

			while (timeDiff < minTimeDiff) {

				// toggle between low and high index
				if (toggleIndex) {
					highIndex++;
				} else {
					lowIndex--;
				}
				toggleIndex = !toggleIndex;

				// check array scope
				if (lowIndex < 0 || highIndex >= serieLength) {
					break;
				}

				timeDiff = timeSerie[highIndex] - timeSerie[lowIndex];
				distDiff = distanceSerie[highIndex] - distanceSerie[lowIndex];
			}

			/*
			 * speed
			 */
			int speedMetric = 0;
			int speedImperial = 0;

			/*
			 * check if a time difference is available between 2 time data, this can happen in gps
			 * data that lat+long is available but no time
			 */
			highIndex = (highIndex <= serieLengthLast) ? highIndex : serieLengthLast;
			lowIndex = (lowIndex >= 0) ? lowIndex : 0;

			boolean isTimeValid = true;
			int prevTime = timeSerie[lowIndex];

			for (int timeIndex = lowIndex + 1; timeIndex <= highIndex; timeIndex++) {
				final int currentTime = timeSerie[timeIndex];
				if (prevTime == currentTime) {
					isTimeValid = false;
					break;
				}
				prevTime = currentTime;
			}

			if (isTimeValid && serieIndex > 0 && timeDiff != 0) {

				// check if a lat and long diff is available
				if (checkPosition && lowIndex > 0 && highIndex < serieLengthLast - 1) {

					if (latitudeSerie[lowIndex] == latitudeSerie[lowIndex - 1]
							&& longitudeSerie[lowIndex] == longitudeSerie[lowIndex - 1]) {
						continue;
					}
					if (longitudeSerie[highIndex] == longitudeSerie[highIndex + 1]
							&& latitudeSerie[highIndex] == latitudeSerie[highIndex + 1]) {
						continue;
					}
				}

				if (timeDiff > 20 && distDiff < 10) {
					// speed must be greater than 1.8 km/h
					speedMetric = 0;
				} else {
					speedMetric = (int) ((distDiff * 36f) / timeDiff);
					speedMetric = speedMetric < 0 ? 0 : speedMetric;

					speedImperial = (int) ((distDiff * 36f) / (timeDiff * UI.UNIT_MILE));
					speedImperial = speedImperial < 0 ? 0 : speedImperial;
				}
			}
			speedSerie[serieIndex] = speedMetric;
			speedSerieImperial[serieIndex] = speedImperial;

			maxSpeed = Math.max(maxSpeed, speedMetric);

			/*
			 * pace
			 */
			int paceMetric = 0;
			int paceImperial = 0;

			if (speedMetric != 0 && distDiff != 0) {
				final float pace = timeDiff * 166.66f / distDiff;
				paceMetric = (int) (pace);
				paceImperial = (int) (pace * UI.UNIT_MILE);
			}

			paceSerie[serieIndex] = paceMetric;
			paceSerieImperial[serieIndex] = paceImperial;
		}

		maxSpeed /= 10;
	}

	public void computeTourDrivingTime() {
		tourDrivingTime = Math.max(0, timeSerie[timeSerie.length - 1] - getBreakTime(0, timeSerie.length));
	}

	/**
	 * compute maximum and average fields
	 */
	public void computeValues() {

		computeMaxAltitude();
		computeMaxPulse();
		computeMaxSpeed();

		computeAvgPulse();
		computeAvgCadence();
		computeAvgTemperature();
	}

	/**
	 * Create a device marker at the current position
	 * 
	 * @param timeData
	 * @param timeIndex
	 * @param timeAbsolute
	 * @param distanceAbsolute
	 */
	private void createMarker(	final TimeData timeData,
								final int timeIndex,
								final int timeAbsolute,
								final int distanceAbsolute) {

		// create a new marker
		final TourMarker tourMarker = new TourMarker(this, ChartMarker.MARKER_TYPE_DEVICE);

		tourMarker.setVisualPosition(ChartMarker.VISUAL_HORIZONTAL_ABOVE_GRAPH_CENTERED);
		tourMarker.setTime(timeAbsolute + timeData.marker);
		tourMarker.setDistance(distanceAbsolute);
		tourMarker.setSerieIndex(timeIndex);

		if (timeData.markerLabel == null) {
			tourMarker.setLabel(Messages.TourData_Label_device_marker);
		} else {
			tourMarker.setLabel(timeData.markerLabel);
		}

		getTourMarkers().add(tourMarker);
	}

	/**
	 * Convert {@link TimeData} into {@link TourData} this will be done after data are imported or
	 * transfered
	 * 
	 * @param isCreateMarker
	 *            creates markers when <code>true</code>
	 */
	public void createTimeSeries(final ArrayList<TimeData> timeDataList, final boolean isCreateMarker) {

		final int serieLength = timeDataList.size();

		if (serieLength == 0) {
			return;
		}

		final TimeData firstTimeDataItem = timeDataList.get(0);

		boolean isDistance = false;
		boolean isAltitude = false;
		boolean isPulse = false;
		boolean isCadence = false;
		boolean isTemperature = false;
		boolean isSpeed = false;
		boolean isPower = false;

		final boolean isAbsoluteData = firstTimeDataItem.absoluteTime != Long.MIN_VALUE;

		/*
		 * time and distance serie is always available
		 */
		timeSerie = new int[serieLength];

		/*
		 * create data series only when data are available
		 */
		if (firstTimeDataItem.distance != Integer.MIN_VALUE || isAbsoluteData) {
			distanceSerie = new int[serieLength];
			isDistance = true;
		}

		/*
		 * altitude serie
		 */
		if (isAbsoluteData) {

			if (firstTimeDataItem.absoluteAltitude == Integer.MIN_VALUE) {

				// search for first altitude value

				int firstAltitudeIndex = 0;
				for (final TimeData timeData : timeDataList) {
					if (timeData.absoluteAltitude != Integer.MIN_VALUE) {

						// altitude was found

						altitudeSerie = new int[serieLength];
						isAltitude = true;

						// set altitude to the first available altitude value

						final int firstAltitudeValue = (int) timeData.absoluteAltitude;

						for (int valueIndex = 0; valueIndex < firstAltitudeIndex; valueIndex++) {
							altitudeSerie[valueIndex] = firstAltitudeValue;
						}
						break;
					}

					firstAltitudeIndex++;
				}

			} else {

				// altitude is available

				altitudeSerie = new int[serieLength];
				isAltitude = true;
			}

		} else if (firstTimeDataItem.altitude != Integer.MIN_VALUE) {

			// altitude is available

			altitudeSerie = new int[serieLength];
			isAltitude = true;
		}

		/*
		 * pulse serie
		 */
		if (firstTimeDataItem.pulse == Integer.MIN_VALUE) {

			// search for first pulse value

			for (final TimeData timeData : timeDataList) {
				if (timeData.pulse != Integer.MIN_VALUE) {

					// pulse was found

					pulseSerie = new int[serieLength];
					isPulse = true;

					break;
				}
			}

		} else {

			// pulse is available

			pulseSerie = new int[serieLength];
			isPulse = true;
		}

		/*
		 * cadence serie
		 */
		if (firstTimeDataItem.cadence == Integer.MIN_VALUE) {

			// search for first cadence value

			for (final TimeData timeData : timeDataList) {
				if (timeData.cadence != Integer.MIN_VALUE) {

					// cadence was found

					cadenceSerie = new int[serieLength];
					isCadence = true;

					break;
				}
			}

		} else {

			// cadence is available

			cadenceSerie = new int[serieLength];
			isCadence = true;
		}

		if (firstTimeDataItem.temperature != Integer.MIN_VALUE) {
			temperatureSerie = new int[serieLength];
			isTemperature = true;
		}

		if (firstTimeDataItem.speed != Integer.MIN_VALUE) {
			speedSerie = new int[serieLength];
			isSpeed = true;

			isSpeedSerieFromDevice = true;
		}

		if (firstTimeDataItem.power != Integer.MIN_VALUE) {
			powerSerie = new int[serieLength];
			isPower = true;

			isPowerSerieFromDevice = true;
		}

		// check if GPS data are available
		boolean isGPS = false;
		if (firstTimeDataItem.latitude != Double.MIN_VALUE) {
			isGPS = true;
		} else {

			// check all data if lat/long is available

			for (final TimeData timeDataItem : timeDataList) {
				if (timeDataItem.latitude != Double.MIN_VALUE) {
					isGPS = true;
					break;
				}
			}
		}
		if (isGPS) {
			latitudeSerie = new double[serieLength];
			longitudeSerie = new double[serieLength];
		}

		int timeIndex = 0;

		int recordingTime = 0; // time in seconds

		int altitudeAbsolute = 0;
		int distanceAbsolute = 0;

		if (isAbsoluteData) {

			/*
			 * absolute data are available when data are from GPS devices
			 */

			long firstTime = 0;

			// index when altitude is available in the time data list
			int altitudeStartIndex = -1;

			int distanceDiff;
			int altitudeDiff;

			int lastValidTime = 0;

			/*
			 * get first valid altitude
			 */
			// set initial min/max latitude/longitude
			if (firstTimeDataItem.latitude == Double.MIN_VALUE || firstTimeDataItem.longitude == Double.MIN_VALUE) {

				// find first valid latitude/longitude
				for (final TimeData timeData : timeDataList) {
					if (timeData.latitude != Double.MIN_VALUE && timeData.longitude != Double.MIN_VALUE) {
						mapMinLatitude = timeData.latitude + 90;
						mapMaxLatitude = timeData.latitude + 90;
						mapMinLongitude = timeData.longitude + 180;
						mapMaxLongitude = timeData.longitude + 180;
						break;
					}
				}
			} else {
				mapMinLatitude = firstTimeDataItem.latitude + 90;
				mapMaxLatitude = firstTimeDataItem.latitude + 90;
				mapMinLongitude = firstTimeDataItem.longitude + 180;
				mapMaxLongitude = firstTimeDataItem.longitude + 180;
			}
			double lastValidLatitude = mapMinLatitude - 90;
			double lastValidLongitude = mapMinLongitude - 180;

			// convert data from the tour format into interger[] arrays
			for (final TimeData timeData : timeDataList) {

				if (altitudeStartIndex == -1 && isAltitude) {
					altitudeStartIndex = timeIndex;
					altitudeAbsolute = (int) timeData.absoluteAltitude;
				}

				final long absoluteTime = timeData.absoluteTime;

				if (timeIndex == 0) {

					// first trackpoint

					/*
					 * time
					 */
					timeSerie[timeIndex] = 0;
					if (absoluteTime == Long.MIN_VALUE) {
						firstTime = 0;
					} else {
						firstTime = absoluteTime;
					}

					recordingTime = 0;
					lastValidTime = (int) firstTime;

					/*
					 * distance
					 */
					final float tdDistance = timeData.absoluteDistance;
					if (tdDistance == Float.MIN_VALUE) {
						distanceDiff = 0;
					} else {
						distanceDiff = (int) tdDistance;
					}
					distanceSerie[timeIndex] = distanceAbsolute += distanceDiff;

					/*
					 * altitude
					 */
					if (isAltitude) {
						altitudeSerie[timeIndex] = altitudeAbsolute;
					}

				} else {

					// 1..n trackpoint

					/*
					 * time
					 */
					if (absoluteTime == Long.MIN_VALUE) {
						recordingTime = lastValidTime;
					} else {
						recordingTime = (int) ((absoluteTime - firstTime) / 1000);
					}
					timeSerie[timeIndex] = lastValidTime = recordingTime;

					/*
					 * distance
					 */
					final float tdDistance = timeData.absoluteDistance;
					if (tdDistance == Float.MIN_VALUE) {
						distanceDiff = 0;
					} else {
						distanceDiff = (Math.round(tdDistance) - distanceAbsolute);
					}
					distanceSerie[timeIndex] = distanceAbsolute += distanceDiff;

					/*
					 * altitude
					 */
					if (isAltitude) {

						if (altitudeStartIndex == -1) {
							altitudeDiff = 0;
						} else {
							final float tdAltitude = timeData.absoluteAltitude;
							if (tdAltitude == Float.MIN_VALUE) {
								altitudeDiff = 0;
							} else {
								altitudeDiff = (int) (tdAltitude - altitudeAbsolute);
							}
						}
						altitudeSerie[timeIndex] = altitudeAbsolute += altitudeDiff;
					}
				}

				/*
				 * latitude & longitude
				 */
				final double latitude = timeData.latitude;
				final double longitude = timeData.longitude;

				if (latitudeSerie != null && longitudeSerie != null) {

					if (latitude == Double.MIN_VALUE || longitude == Double.MIN_VALUE) {
						latitudeSerie[timeIndex] = lastValidLatitude;
						longitudeSerie[timeIndex] = lastValidLongitude;
					} else {

						latitudeSerie[timeIndex] = lastValidLatitude = latitude;
						longitudeSerie[timeIndex] = lastValidLongitude = longitude;
					}

					final double lastValidLatitude90 = lastValidLatitude + 90;
					mapMinLatitude = Math.min(mapMinLatitude, lastValidLatitude90);
					mapMaxLatitude = Math.max(mapMaxLatitude, lastValidLatitude90);

					final double lastValidLongitude180 = lastValidLongitude + 180;
					mapMinLongitude = Math.min(mapMinLongitude, lastValidLongitude180);
					mapMaxLongitude = Math.max(mapMaxLongitude, lastValidLongitude180);
				}

				/*
				 * pulse
				 */
				if (isPulse) {
					final int tdPulse = timeData.pulse;
					pulseSerie[timeIndex] = tdPulse == Integer.MIN_VALUE ? 0 : tdPulse;
				}

				/*
				 * cadence
				 */
				if (isCadence) {
					final int tdCadence = timeData.cadence;
					cadenceSerie[timeIndex] = tdCadence == Integer.MIN_VALUE ? 0 : tdCadence;
				}

				/*
				 * marker
				 */
				if (isCreateMarker && timeData.marker != 0) {
					createMarker(timeData, timeIndex, recordingTime, distanceAbsolute);
				}

				timeIndex++;
			}

			mapMinLatitude -= 90;
			mapMaxLatitude -= 90;
			mapMinLongitude -= 180;
			mapMaxLongitude -= 180;

		} else {

			/*
			 * relativ data are available, these data are from non GPS devices
			 */

			// convert data from the tour format into an interger[]
			for (final TimeData timeData : timeDataList) {

				timeSerie[timeIndex] = recordingTime += timeData.time;

				if (isDistance) {
					distanceSerie[timeIndex] = distanceAbsolute += timeData.distance;
				}

				if (isAltitude) {
					altitudeSerie[timeIndex] = altitudeAbsolute += timeData.altitude;
				}

				if (isPulse) {
					pulseSerie[timeIndex] = timeData.pulse;
				}

				if (isTemperature) {
					temperatureSerie[timeIndex] = timeData.temperature;
				}

				if (isCadence) {
					cadenceSerie[timeIndex] = timeData.cadence;
				}

				if (isPower) {
					powerSerie[timeIndex] = timeData.power;
				}

				if (isSpeed) {
					speedSerie[timeIndex] = timeData.speed;
				}

				if (isCreateMarker && timeData.marker != 0) {
					createMarker(timeData, timeIndex, recordingTime, distanceAbsolute);
				}

				timeIndex++;
			}
		}

		tourDistance = distanceAbsolute;
		tourRecordingTime = recordingTime;

	}

	/**
	 * Creates the unique tour id from the tour date/time and the unique key
	 * 
	 * @param uniqueKey
	 *            unique key to identify a tour
	 */
	public void createTourId(final String uniqueKey) {

//		final String uniqueKey = Integer.toString(Math.abs(getStartDistance()));

		String tourId;

		try {
			/*
			 * this is the default implementation to create a tour id, but on the 5.5.2007 a
			 * NumberFormatException occured so the calculation for the tour id was adjusted
			 */
			tourId = Short.toString(getStartYear())
					+ Short.toString(getStartMonth())
					+ Short.toString(getStartDay())
					+ Short.toString(getStartHour())
					+ Short.toString(getStartMinute())
					+ uniqueKey;

			setTourId(Long.parseLong(tourId));

		} catch (final NumberFormatException e) {

			/*
			 * the distance was shorted so that the maximum of a Long datatype is not exceeded
			 */

			tourId = Short.toString(getStartYear())
					+ Short.toString(getStartMonth())
					+ Short.toString(getStartDay())
					+ Short.toString(getStartHour())
					+ Short.toString(getStartMinute())
					+ uniqueKey.substring(0, Math.min(5, uniqueKey.length()));

			setTourId(Long.parseLong(tourId));
		}

	}

	/**
	 * Create the tour segment list from the segment index array
	 * 
	 * @return
	 */
	public Object[] createTourSegments() {

		if (segmentSerieIndex == null || segmentSerieIndex.length < 2) {
			// at least two points are required to build a segment
			return new Object[0];
		}

		final int segmentSerieLength = segmentSerieIndex.length;

		final ArrayList<TourSegment> tourSegments = new ArrayList<TourSegment>(segmentSerieLength);
		final int firstSerieIndex = segmentSerieIndex[0];

		// get start values
		int distanceStart = distanceSerie[firstSerieIndex];
		int altitudeStart = altitudeSerie[firstSerieIndex];
		int timeStart = timeSerie[firstSerieIndex];

		segmentSerieAltitude = new int[segmentSerieLength];
		segmentSerieDistance = new int[segmentSerieLength];
		segmentSerieTime = new int[segmentSerieLength];
		segmentSerieDrivingTime = new int[segmentSerieLength];
		segmentSerieAltitudeDown = new int[segmentSerieLength];

		segmentSerieAltimeter = new float[segmentSerieLength];
		segmentSerieSpeed = new float[segmentSerieLength];
		segmentSeriePace = new float[segmentSerieLength];
		segmentSeriePower = new float[segmentSerieLength];
		segmentSerieGradient = new float[segmentSerieLength];
		segmentSeriePulse = new float[segmentSerieLength];
		segmentSerieCadence = new float[segmentSerieLength];

		for (int iSegment = 1; iSegment < segmentSerieLength; iSegment++) {

			final int segmentIndex = iSegment;

			final int segmentStartIndex = segmentSerieIndex[iSegment - 1];
			final int segmentEndIndex = segmentSerieIndex[iSegment];

			final TourSegment segment = new TourSegment();
			tourSegments.add(segment);

			segment.serieIndexStart = segmentStartIndex;
			segment.serieIndexEnd = segmentEndIndex;

			// compute difference values between start and end
			final int altitudeEnd = altitudeSerie[segmentEndIndex];
			final int distanceEnd = distanceSerie[segmentEndIndex];
			final int timeEnd = timeSerie[segmentEndIndex];
			final int recordingTime = timeEnd - timeStart;
			final int drivingTime;

			segmentSerieAltitude[segmentIndex] = segment.altitude = altitudeEnd - altitudeStart;
			segmentSerieDistance[segmentIndex] = segment.distance = distanceEnd - distanceStart;

			segmentSerieTime[segmentIndex] = segment.recordingTime = recordingTime;

			final int drivingTimeTemp = recordingTime - getBreakTime(segmentStartIndex, segmentEndIndex);
			segmentSerieDrivingTime[segmentIndex] = //
			segment.drivingTime = //
			drivingTime = (0 >= drivingTimeTemp) ? 0 : drivingTimeTemp;

			final int[] localPowerSerie = getPowerSerie();
			int altitudeUp = 0;
			int altitudeDown = 0;
			int pulseSum = 0;
			int powerSum = 0;

			int altitude1 = altitudeSerie[segmentStartIndex];

			// compute altitude up/down, pulse and power for a segment
			for (int serieIndex = segmentStartIndex + 1; serieIndex <= segmentEndIndex; serieIndex++) {

				final int altitude2 = altitudeSerie[serieIndex];
				final int altitudeDiff = altitude2 - altitude1;
				altitude1 = altitude2;

				altitudeUp += altitudeDiff >= 0 ? altitudeDiff : 0;
				altitudeDown += altitudeDiff < 0 ? altitudeDiff : 0;

				powerSum += localPowerSerie[serieIndex];

				if (pulseSerie != null) {
					pulseSum += pulseSerie[serieIndex];
				}
			}

			segment.altitudeUp = altitudeUp;
			segmentSerieAltitudeDown[segmentIndex] = segment.altitudeDown = altitudeDown;

			segmentSerieSpeed[segmentIndex] = segment.speed //
			= drivingTime == 0 ? 0 : (float) ((float) segment.distance / drivingTime * 3.6 / UI.UNIT_VALUE_DISTANCE);

			segmentSeriePace[segmentIndex] = segment.pace //
			= drivingTime == 0 ? 0 : (float) (drivingTime * 16.666 / segment.distance * UI.UNIT_VALUE_DISTANCE);

			segmentSerieGradient[segmentIndex] = segment.gradient //
			= (float) segment.altitude * 100 / segment.distance;

			segmentSerieAltimeter[segmentIndex] = drivingTime == 0 ? 0 : (float) (altitudeUp + altitudeDown)
					/ recordingTime
					* 3600
					/ UI.UNIT_VALUE_ALTITUDE;

			segmentSeriePower[segmentIndex] = segment.power = powerSum / (segmentEndIndex - segmentStartIndex);

			if (segmentSeriePulse != null) {
				segmentSeriePulse[segmentIndex] = pulseSum / (segmentEndIndex - segmentStartIndex);
			}

			// end point of current segment is the start of the next segment
			altitudeStart = altitudeEnd;
			distanceStart = distanceEnd;
			timeStart = timeEnd;
		}

		fTourSegments = tourSegments.toArray();

		return fTourSegments;
	}

	public void dumpData() {

		final PrintStream out = System.out;

		out.println("----------------------------------------------------"); //$NON-NLS-1$
		out.println("TOUR DATA"); //$NON-NLS-1$
		out.println("----------------------------------------------------"); //$NON-NLS-1$
// out.println("Typ: " + getDeviceTourType()); //$NON-NLS-1$
		out.println("Date:			" + getStartDay() + "." + getStartMonth() + "." + getStartYear()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		out.println("Time:			" + getStartHour() + ":" + getStartMinute()); //$NON-NLS-1$ //$NON-NLS-2$
		out.println("Total distance:		" + getStartDistance()); //$NON-NLS-1$
		// out.println("Distance: " + getDistance());
		out.println("Altitude:		" + getStartAltitude()); //$NON-NLS-1$
		out.println("Pulse:			" + getStartPulse()); //$NON-NLS-1$
		out.println("Offset DD record:	" + offsetDDRecord); //$NON-NLS-1$
	}

	public void dumpTime() {
		final PrintStream out = System.out;

		out.print((getTourRecordingTime() / 3600) + ":" //$NON-NLS-1$
				+ ((getTourRecordingTime() % 3600) / 60)
				+ ":" //$NON-NLS-1$
				+ ((getTourRecordingTime() % 3600) % 60)
				+ "  "); //$NON-NLS-1$
		out.print(getTourDistance());
	}

	public void dumpTourTotal() {

		final PrintStream out = System.out;

		out.println("Tour distance (m):	" + getTourDistance()); //$NON-NLS-1$

		out.println("Tour time:		" //$NON-NLS-1$
				+ (getTourRecordingTime() / 3600)
				+ ":" //$NON-NLS-1$
				+ ((getTourRecordingTime() % 3600) / 60)
				+ ":" //$NON-NLS-1$
				+ (getTourRecordingTime() % 3600)
				% 60);

		out.println("Driving time:		" //$NON-NLS-1$
				+ (getTourDrivingTime() / 3600)
				+ ":" //$NON-NLS-1$
				+ ((getTourDrivingTime() % 3600) / 60)
				+ ":" //$NON-NLS-1$
				+ (getTourDrivingTime() % 3600)
				% 60);

		out.println("Altitude up (m):	" + getTourAltUp()); //$NON-NLS-1$
		out.println("Altitude down (m):	" + getTourAltDown()); //$NON-NLS-1$
	}

	@Override
	public boolean equals(final Object obj) {

		if (this == obj) {
			return true;
		}

		if (obj instanceof TourData) {
			return tourId.longValue() == ((TourData) obj).tourId.longValue();
		}

		return false;
	}

	/**
	 * @return Returns the metric or imperial altimeter serie depending on the active measurement
	 */
	public int[] getAltimeterSerie() {

		if (UI.UNIT_VALUE_ALTITUDE != 1) {

			// use imperial system

			if (altimeterSerieImperial == null) {
				computeAltimeterGradientSerie();
			}
			return altimeterSerieImperial;

		} else {

			// use metric system

			if (altimeterSerie == null) {
				computeAltimeterGradientSerie();
			}
			return altimeterSerie;
		}
	}

	/**
	 * @return Returns the metric or imperial altitude serie depending on the active measurement or
	 *         <code>null</code> when altitude data serie is not available
	 */
	public int[] getAltitudeSerie() {

		if (altitudeSerie == null) {
			return null;
		}

		if (UI.UNIT_VALUE_ALTITUDE != 1) {

			// imperial system is used

			if (altitudeSerieImperial == null) {

				// compute imperial altitude

				altitudeSerieImperial = new int[altitudeSerie.length];

				for (int valueIndex = 0; valueIndex < altitudeSerie.length; valueIndex++) {
					altitudeSerieImperial[valueIndex] = (int) (altitudeSerie[valueIndex] / UI.UNIT_VALUE_ALTITUDE);
				}
			}
			return altitudeSerieImperial;

		} else {

			return altitudeSerie;
		}
	}

	/**
	 * @return the avgCadence
	 */
	public int getAvgCadence() {
		return avgCadence;
	}

	/**
	 * @return the avgPulse
	 */
	public int getAvgPulse() {
		return avgPulse;
	}

	/**
	 * @return the avgTemperature
	 */
	public int getAvgTemperature() {
		return avgTemperature;
	}

	/**
	 * @return the bikerWeight
	 */
	public float getBikerWeight() {
		return bikerWeight;
	}

	/**
	 * Computes the time between start index and end index when the speed is <code>0</code>
	 * 
	 * @param startIndex
	 * @param endIndex
	 * @return Returns the break time in seconds
	 */
	public int getBreakTime(final int startIndex, final int endIndex) {

		if (distanceSerie == null) {
			return 0;
		}

		final int minBreakTime = 20;

		if (deviceTimeInterval == -1) {

			// variable time slices

			return computeBreakTimeVariable(minBreakTime, startIndex, endIndex);

		} else {

			// fixed time slices

			final int ignoreTimeSlices = deviceTimeInterval == 0 ? //
					0
					: getBreakTimeSlices(distanceSerie, startIndex, endIndex, minBreakTime / deviceTimeInterval);

			return ignoreTimeSlices * deviceTimeInterval;
		}
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
	private int getBreakTimeSlices(final int[] distanceValues, final int indexLeft, int indexRight, int sliceMin) {

		final int distanceLengthLast = distanceValues.length - 1;

		int ignoreTimeCounter = 0;
		int oldDistance = 0;

		sliceMin = (sliceMin >= 1) ? sliceMin : 1;
		indexRight = (indexRight <= distanceLengthLast) ? indexRight : distanceLengthLast;

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

	/**
	 * @return the calories
	 */
	public String getCalories() {
		return calories;
	}

	public int getDeviceDistance() {
		return deviceDistance;
	}

	public String getDeviceId() {
		return devicePluginId;
	}

	public short getDeviceMode() {
		return deviceMode;
	}

	public String getDeviceModeName() {
		return deviceModeName;
	}

	public String getDeviceName() {
		if (devicePluginName == null) {
			return UI.EMPTY_STRING;
		} else {
			return devicePluginName;
		}
	}

	/**
	 * @return Returns the time difference between 2 time slices or <code>-1</code> when the time
	 *         slices are unequally
	 */
	public short getDeviceTimeInterval() {
		return deviceTimeInterval;
	}

	public int getDeviceTotalDown() {
		return deviceTotalDown;
	}

	public int getDeviceTotalUp() {
		return deviceTotalUp;
	}

	public String getDeviceTourType() {
		return deviceTourType;
	}

	public long getDeviceTravelTime() {
		return deviceTravelTime;
	}

	public int getDeviceWeight() {
		return deviceWeight;
	}

	public int getDeviceWheel() {
		return deviceWheel;
	}

	/**
	 * @return Returns the distance data serie for the current measurement system which can be
	 *         metric or imperial
	 */
	public int[] getDistanceSerie() {

		if (distanceSerie == null) {
			return null;
		}

		int[] serie;

		final float unitValueDistance = UI.UNIT_VALUE_DISTANCE;

		if (unitValueDistance != 1) {

			// use imperial system

			if (distanceSerieImperial == null) {

				// compute imperial data

				distanceSerieImperial = new int[distanceSerie.length];

				for (int valueIndex = 0; valueIndex < distanceSerie.length; valueIndex++) {
					distanceSerieImperial[valueIndex] = (int) (distanceSerie[valueIndex] / unitValueDistance);
				}
			}
			serie = distanceSerieImperial;

		} else {

			// use metric system

			serie = distanceSerie;
		}

		return serie;
	}

	public short getDpTolerance() {
		return dpTolerance;
	}

	/**
	 * @return Returns the metric or imperial altimeter serie depending on the active measurement
	 */
	public int[] getGradientSerie() {

		if (gradientSerie == null) {
			computeAltimeterGradientSerie();
		}

		return gradientSerie;
	}

	/**
	 * @return the maxAltitude
	 */
	public int getMaxAltitude() {
		return maxAltitude;
	}

	/**
	 * @return the maxPulse
	 */
	public int getMaxPulse() {
		return maxPulse;
	}

	/**
	 * @return the maxSpeed
	 */
	public float getMaxSpeed() {
		return maxSpeed;
	}

	/**
	 * @return Returns the distance serie from the metric system, the distance serie is
	 *         <b>always</b> saved in the database in the metric system
	 */
	public int[] getMetricDistanceSerie() {
		return distanceSerie;
	}

	public int[] getPaceSerie() {

		if (UI.UNIT_VALUE_DISTANCE == 1) {

			// use metric system

			if (paceSerie == null) {
				computeSpeedSerie();
			}

			return paceSerie;

		} else {

			// use imperial system

			if (paceSerieImperial == null) {
				computeSpeedSerie();
			}

			return paceSerieImperial;
		}
	}

	public int[] getPowerSerie() {

		if (powerSerie != null || isPowerSerieFromDevice) {
			return powerSerie;
		}

		if (speedSerie == null) {
			computeSpeedSerie();
		}

		if (gradientSerie == null) {
			computeAltimeterGradientSerie();
		}

		// check if required data series are available 
		if (speedSerie == null || gradientSerie == null) {
			return null;
		}

		powerSerie = new int[timeSerie.length];

		final int weightBody = 75;
		final int weightBike = 10;
		final int bodyHeight = 188;

		final float cR = 0.008f; // Rollreibungskoeffizient Asphalt
		final float cD = 0.8f;// Str�mungskoeffizient
		final float p = 1.145f; // 20C / 400m
//		float p = 0.968f; // 10C / 2000m

		final float weightTotal = weightBody + weightBike;
		final float bsa = (float) (0.007184f * Math.pow(weightBody, 0.425) * Math.pow(bodyHeight, 0.725));
		final float aP = bsa * 0.185f;

		final float fRoll = weightTotal * 9.81f * cR;
		final float fSlope = weightTotal * 9.81f; // * gradient/100
		final float fAir = 0.5f * p * cD * aP;// * v2;

		for (int timeIndex = 0; timeIndex < timeSerie.length; timeIndex++) {

			final float speed = (float) speedSerie[timeIndex] / 36; // speed (m/s) /10
			float gradient = (float) gradientSerie[timeIndex] / 1000; // gradient (%) /10 /100

			// adjust computed errors
//			if (gradient < 0.04 && gradient > 0) {
//				gradient *= 0.5;
////				gradient = 0;
//			}

			if (gradient < 0) {
				if (gradient < -0.02) {
					gradient *= 3;
				} else {
					gradient *= 1.5;
				}
			}

			final float fSlopeTotal = fSlope * gradient;
			final float fAirTotal = fAir * speed * speed;

			final float fTotal = fRoll + fAirTotal + fSlopeTotal;

			final int pTotal = (int) (fTotal * speed);

//			if (pTotal > 600) {
//				pTotal = pTotal * 1;
//			}
			powerSerie[timeIndex] = pTotal < 0 ? 0 : pTotal;
		}

		return powerSerie;
	}

	public SerieData getSerieData() {
		return serieData;
	}

	public int[] getSpeedSerie() {

		if (isSpeedSerieFromDevice) {
			return getSpeedSerieInternal();
		}
		if (distanceSerie == null) {
			return null;
		}

		return getSpeedSerieInternal();
	}

	private int[] getSpeedSerieInternal() {

		computeSpeedSerie();

		/*
		 * when the speed series are not computed, the internal algorithm will be used to create the
		 * speed data serie
		 */
		if (UI.UNIT_VALUE_DISTANCE == 1) {

			// use metric system

			return speedSerie;

		} else {

			// use imperial system

			return speedSerieImperial;
		}
	}

	public short getStartAltitude() {
		return startAltitude;
	}

	public short getStartDay() {
		return startDay;
	}

	public int getStartDistance() {
		return startDistance;
	}

	public short getStartHour() {
		return startHour;
	}

	public short getStartMinute() {
		return startMinute;
	}

	/**
	 * @return Returns the month for the tour start in the range 1...12
	 */
	public short getStartMonth() {
		return startMonth;
	}

	public short getStartPulse() {
		return startPulse;
	}

	public short getStartWeek() {
		return startWeek;
	}

	public short getStartYear() {
		return startYear;
	}

	/**
	 * @return Returns the temperature serie for the current measurement system or <code>null</code>
	 *         when temperature is not available
	 */
	public int[] getTemperatureSerie() {

		if (temperatureSerie == null) {
			return null;
		}

		int[] serie;

		final float unitValueTempterature = UI.UNIT_VALUE_TEMPERATURE;
		final float fahrenheitMulti = UI.UNIT_FAHRENHEIT_MULTI;
		final float fahrenheitAdd = UI.UNIT_FAHRENHEIT_ADD;

		if (unitValueTempterature != 1) {

			// use imperial system

			if (temperatureSerieImperial == null) {

				// compute imperial data

				temperatureSerieImperial = new int[temperatureSerie.length];

				for (int valueIndex = 0; valueIndex < temperatureSerie.length; valueIndex++) {
					temperatureSerieImperial[valueIndex] = (int) (temperatureSerie[valueIndex] * fahrenheitMulti + fahrenheitAdd);
				}
			}
			serie = temperatureSerieImperial;

		} else {

			// use metric system

			serie = temperatureSerie;
		}

		return serie;
	}

	public int getTourAltDown() {
		return tourAltDown;
	}

	public int getTourAltUp() {
		return tourAltUp;
	}

	public TourBike getTourBike() {
		return tourBike;
	}

	/**
	 * @return the tourDescription
	 */
	public String getTourDescription() {
		return tourDescription == null ? "" : tourDescription; //$NON-NLS-1$
	}

	public int getTourDistance() {
		return tourDistance;
	}

	public int getTourDrivingTime() {
		return tourDrivingTime;
	}

	/**
	 * @return the tourEndPlace
	 */
	public String getTourEndPlace() {
		return tourEndPlace == null ? "" : tourEndPlace; //$NON-NLS-1$
	}

//	public Set<TourCategory> getTourCategory() {
//		return tourCategory;
//	}

	/**
	 * @return Returns the unique key in the database for this {@link TourData} entity
	 */
	public Long getTourId() {
		return tourId;
	}

	public Set<TourMarker> getTourMarkers() {
		return tourMarkers;
	}

	/**
	 * @return returns the person for whom the tour data is saved or <code>null</code> when the tour
	 *         is not saved in the database
	 */
	public TourPerson getTourPerson() {
		return tourPerson;
	}

	public int getTourRecordingTime() {
		return tourRecordingTime;
	}

	public Collection<TourReference> getTourReferences() {
		return tourReferences;
	}

	public Object[] getTourSegments() {
		return fTourSegments;
	}

	/**
	 * @return the tourStartPlace
	 */
	public String getTourStartPlace() {
		return tourStartPlace == null ? "" : tourStartPlace; //$NON-NLS-1$
	}

	/**
	 * @return Returns the tags {@link #tourTags} which are defined for this tour
	 */
	public Set<TourTag> getTourTags() {
		return tourTags;
	}

	/**
	 * @return the tourTitle
	 */
	public String getTourTitle() {
		return tourTitle == null ? "" : tourTitle; //$NON-NLS-1$
	}

	/**
	 * @return Returns the {@link TourType} for the tour or <code>null</code> when tour type is not
	 *         defined
	 */
	public TourType getTourType() {
		return tourType;
	}

	/**
	 * @param zoomLevel
	 * @return Returns the world position for the suplied zoom level and projection id
	 */
	public Point[] getWorldPosition(final String projectionId, final int zoomLevel) {
		return fWorldPosition.get(projectionId.hashCode() + zoomLevel);
	}

	/**
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		int result = 17;

		result = 37 * result + this.getStartYear();
		result = 37 * result + this.getStartMonth();
		result = 37 * result + this.getStartDay();
		result = 37 * result + this.getStartHour();
		result = 37 * result + this.getStartMinute();
		result = 37 * result + this.getTourDistance();
		result = 37 * result + this.getTourRecordingTime();

		return result;
	}

	/**
	 * Called after the object was loaded from the persistence store
	 */
	@PostLoad
	@PostUpdate
	public void onPostLoad() {

		timeSerie = serieData.timeSerie;

		altitudeSerie = serieData.altitudeSerie;
		cadenceSerie = serieData.cadenceSerie;
		distanceSerie = serieData.distanceSerie;
		pulseSerie = serieData.pulseSerie;
		temperatureSerie = serieData.temperatureSerie;
		powerSerie = serieData.powerSerie;
		speedSerie = serieData.speedSerie;

		latitudeSerie = serieData.latitude;
		longitudeSerie = serieData.longitude;

		/*
		 * cleanup dataseries because dataseries has been saved before version 1.3.0 even when no
		 * data are available
		 */
		int sumAltitude = 0;
		int sumCadence = 0;
		int sumDistance = 0;
		int sumPulse = 0;
		int sumTemperature = 0;
		int sumPower = 0;
		int sumSpeed = 0;

		// get first valid latitude/longitude
		if (latitudeSerie != null && longitudeSerie != null) {

			for (int timeIndex = 0; timeIndex < timeSerie.length; timeIndex++) {
				if (latitudeSerie[timeIndex] != Double.MIN_VALUE && longitudeSerie[timeIndex] != Double.MIN_VALUE) {
					mapMinLatitude = mapMaxLatitude = latitudeSerie[timeIndex] + 90;
					mapMinLongitude = mapMaxLongitude = longitudeSerie[timeIndex] + 180;
					break;
				}
			}
		}
		double lastValidLatitude = mapMinLatitude - 90;
		double lastValidLongitude = mapMinLongitude - 180;
		boolean isLatitudeValid = false;

		for (int serieIndex = 0; serieIndex < timeSerie.length; serieIndex++) {

			if (altitudeSerie != null) {
				sumAltitude += altitudeSerie[serieIndex];
			}

			if (cadenceSerie != null) {
				sumCadence += cadenceSerie[serieIndex];
			}
			if (distanceSerie != null) {
				sumDistance += distanceSerie[serieIndex];
			}
			if (pulseSerie != null) {
				sumPulse += pulseSerie[serieIndex];
			}
			if (temperatureSerie != null) {
				final int temp = temperatureSerie[serieIndex];
				sumTemperature += (temp < 0) ? -temp : temp;
			}
			if (powerSerie != null) {
				sumPower += powerSerie[serieIndex];
			}
			if (speedSerie != null) {
				sumSpeed += speedSerie[serieIndex];
			}

			if (latitudeSerie != null && longitudeSerie != null) {

				final double latitude = latitudeSerie[serieIndex];
				final double longitude = longitudeSerie[serieIndex];

				if (latitude == Double.MIN_VALUE || longitude == Double.MIN_VALUE) {
					latitudeSerie[serieIndex] = lastValidLatitude;
					longitudeSerie[serieIndex] = lastValidLongitude;
				} else {
					latitudeSerie[serieIndex] = lastValidLatitude = latitude;
					longitudeSerie[serieIndex] = lastValidLongitude = longitude;
				}

				mapMinLatitude = Math.min(mapMinLatitude, lastValidLatitude + 90);
				mapMaxLatitude = Math.max(mapMaxLatitude, lastValidLatitude + 90);
				mapMinLongitude = Math.min(mapMinLongitude, lastValidLongitude + 180);
				mapMaxLongitude = Math.max(mapMaxLongitude, lastValidLongitude + 180);

				/*
				 * check if latitude is not 0, there was a bug until version 1.3.0 where latitude
				 * and longitude has been saved with 0 values
				 */
				if (isLatitudeValid == false && lastValidLatitude != 0) {
					isLatitudeValid = true;
				}
			}
		}

		mapMinLatitude -= 90;
		mapMaxLatitude -= 90;
		mapMinLongitude -= 180;
		mapMaxLongitude -= 180;

		/*
		 * remove data series when the summary of the values is 0, for temperature this can be a
		 * problem but for a longer tour the temperature varies
		 */

		if (sumAltitude == 0) {
			altitudeSerie = null;
		}
		if (sumCadence == 0) {
			cadenceSerie = null;
		}
		if (sumDistance == 0) {
			distanceSerie = null;
		}
		if (sumPulse == 0) {
			pulseSerie = null;
		}
		if (sumTemperature == 0) {
			temperatureSerie = null;
		}
		if (sumPower == 0) {
			powerSerie = null;
		}
		if (sumSpeed == 0) {
			speedSerie = null;
		}

		if (powerSerie != null) {
			isPowerSerieFromDevice = true;
		}

		if (speedSerie != null) {
			isSpeedSerieFromDevice = true;
		}

		if (isLatitudeValid == false) {
			latitudeSerie = null;
			longitudeSerie = null;
		}

	}

	/**
	 * Called before this object gets persisted, copy data from the tourdata object into the object
	 * which gets serialized
	 */
	/*
	 * @PrePersist + @PreUpdate is currently disabled for EJB events because of bug
	 * http://opensource.atlassian.com/projects/hibernate/browse/HHH-1921 2006-08-11
	 */
	public void onPrePersist() {

		serieData = new SerieData();

		serieData.altitudeSerie = altitudeSerie;
		serieData.cadenceSerie = cadenceSerie;
		serieData.distanceSerie = distanceSerie;
		serieData.pulseSerie = pulseSerie;
		serieData.temperatureSerie = temperatureSerie;
		serieData.timeSerie = timeSerie;

		/*
		 * don't save computed data series
		 */
		if (isSpeedSerieFromDevice) {
			serieData.speedSerie = speedSerie;
		}

		if (isPowerSerieFromDevice) {
			serieData.powerSerie = powerSerie;
		}

		serieData.latitude = latitudeSerie;
		serieData.longitude = longitudeSerie;
	}

	/**
	 * @param avgCadence
	 *            the avgCadence to set
	 */
	public void setAvgCadence(final int avgCadence) {
		this.avgCadence = avgCadence;
	}

//	/**
//	 * Called before this object gets persisted, copy data from the tourdata object into the object
//	 * which gets serialized
//	 */
//	/*
//	 * @PrePersist + @PreUpdate is currently disabled for EJB events because of bug
//	 * http://opensource.atlassian.com/projects/hibernate/browse/HHH-1921 2006-08-11
//	 */
//	public void onPrePersistOLD() {
//
//		if (timeSerie == null) {
//			serieData = new SerieData();
//			return;
//		}
//
//		final int serieLength = timeSerie.length;
//
//		serieData = new SerieData(serieLength);
//
//		System.arraycopy(altitudeSerie, 0, serieData.altitudeSerie, 0, serieLength);
//		System.arraycopy(cadenceSerie, 0, serieData.cadenceSerie, 0, serieLength);
//		System.arraycopy(distanceSerie, 0, serieData.distanceSerie, 0, serieLength);
//		System.arraycopy(pulseSerie, 0, serieData.pulseSerie, 0, serieLength);
//		System.arraycopy(temperatureSerie, 0, serieData.temperatureSerie, 0, serieLength);
//		System.arraycopy(timeSerie, 0, serieData.timeSerie, 0, serieLength);
//
//		// System.arraycopy(speedSerie, 0, serieData.speedSerie, 0,
//		// serieLength);
//		// System.arraycopy(powerSerie, 0, serieData.powerSerie, 0,
//		// serieLength);
//
//		if (latitudeSerie != null) {
//
//			serieData.initializeGPSData(serieLength);
//
//			System.arraycopy(latitudeSerie, 0, serieData.latitude, 0, serieLength);
//			System.arraycopy(longitudeSerie, 0, serieData.longitude, 0, serieLength);
//		}
//	}

	/**
	 * @param avgPulse
	 *            the avgPulse to set
	 */
	public void setAvgPulse(final int avgPulse) {
		this.avgPulse = avgPulse;
	}

	/**
	 * @param avgTemperature
	 *            the avgTemperature to set
	 */
	public void setAvgTemperature(final int avgTemperature) {
		this.avgTemperature = avgTemperature;
	}

	/**
	 * @param bikerWeight
	 *            the bikerWeight to set
	 */
	public void setBikerWeight(final float bikerWeight) {
		this.bikerWeight = bikerWeight;
	}

	/**
	 * @param calories
	 *            the calories to set
	 */
	public void setCalories(final String calories) {
		this.calories = calories;
	}

	public void setDeviceDistance(final int deviceDistance) {
		this.deviceDistance = deviceDistance;
	}

	public void setDeviceId(final String deviceId) {
		this.devicePluginId = deviceId;
	}

	public void setDeviceMode(final short deviceMode) {
		this.deviceMode = deviceMode;
	}

	public void setDeviceModeName(final String deviceModeName) {
		this.deviceModeName = deviceModeName;
	}

	public void setDeviceName(final String deviceName) {
		devicePluginName = deviceName;
	}

	/**
	 * time difference between 2 time slices or <code>-1</code> for GPS devices or ergometer when
	 * the time slices are not equally
	 * 
	 * @param deviceTimeInterval
	 */
	public void setDeviceTimeInterval(final short deviceTimeInterval) {
		this.deviceTimeInterval = deviceTimeInterval;
	}

	public void setDeviceTotalDown(final int deviceTotalDown) {
		this.deviceTotalDown = deviceTotalDown;
	}

	public void setDeviceTotalUp(final int deviceTotalUp) {
		this.deviceTotalUp = deviceTotalUp;
	}

	public void setDeviceTourType(final String tourType) {
		this.deviceTourType = tourType;
	}

	public void setDeviceTravelTime(final long deviceTravelTime) {
		this.deviceTravelTime = deviceTravelTime;
	}

	public void setDeviceWeight(final int deviceWeight) {
		this.deviceWeight = deviceWeight;
	}

	public void setDeviceWheel(final int deviceWheel) {
		this.deviceWheel = deviceWheel;
	}

	public void setDpTolerance(final short dpTolerance) {
		this.dpTolerance = dpTolerance;
	}

	public void setStartAltitude(final short startAltitude) {
		this.startAltitude = startAltitude;
	}

	public void setStartDay(final short startDay) {
		this.startDay = startDay;
	}

	/**
	 * Set the distance at tour start, this is the distance which the device has accumulated
	 * 
	 * @param startDistance
	 */
	public void setStartDistance(final int startDistance) {
		this.startDistance = startDistance;
	}

	public void setStartHour(final short startHour) {
		this.startHour = startHour;
	}

	public void setStartMinute(final short startMinute) {
		this.startMinute = startMinute;
	}

	public void setStartMonth(final short startMonth) {
		this.startMonth = startMonth;
	}

	public void setStartPulse(final short startPulse) {
		this.startPulse = startPulse;
	}

	public void setStartWeek(final short startWeek) {
		this.startWeek = startWeek;
	}

	public void setStartYear(final short startYear) {
		this.startYear = startYear;
	}

	public void setTourAltDown(final int tourAltDown) {
		this.tourAltDown = tourAltDown;
	}

	public void setTourAltUp(final int tourAltUp) {
		this.tourAltUp = tourAltUp;
	}

	public void setTourBike(final TourBike tourBike) {
		this.tourBike = tourBike;
	}

	/**
	 * @param tourDescription
	 *            the tourDescription to set
	 */
	public void setTourDescription(final String tourDescription) {
		this.tourDescription = tourDescription;
	}

	public void setTourDistance(final int tourDistance) {
		this.tourDistance = tourDistance;
	}

	/**
	 * Set total driving time
	 * 
	 * @param tourDrivingTime
	 */
	public void setTourDrivingTime(final int tourDrivingTime) {
		this.tourDrivingTime = tourDrivingTime;
	}

	/**
	 * @param tourEndPlace
	 *            the tourEndPlace to set
	 */
	public void setTourEndPlace(final String tourEndPlace) {
		this.tourEndPlace = tourEndPlace;
	}

	public void setTourId(final Long tourId) {
		this.tourId = tourId;
	}

	public void setTourMarkers(final Set<TourMarker> tourMarkers) {
		this.tourMarkers = tourMarkers;
	}

	/**
	 * Sets the {@link TourPerson} for the tour or <code>null</code> when the tour is not saved in
	 * the database
	 * 
	 * @param tourPerson
	 */
	public void setTourPerson(final TourPerson tourPerson) {
		this.tourPerson = tourPerson;
	}

	public void setTourRecordingTime(final int tourRecordingTime) {
		this.tourRecordingTime = tourRecordingTime;
	}

	/**
	 * @param tourStartPlace
	 *            the tourStartPlace to set
	 */
	public void setTourStartPlace(final String tourStartPlace) {
		this.tourStartPlace = tourStartPlace;
	}

	public void setTourTags(final Set<TourTag> tourTags) {
		this.tourTags = tourTags;
	}

	/**
	 * @param tourTitle
	 *            the tourTitle to set
	 */
	public void setTourTitle(final String tourTitle) {
		this.tourTitle = tourTitle;
	}

	public void setTourType(final TourType tourType) {
		this.tourType = tourType;
	}

	public void setWorldPosition(final String projectionId, final Point[] worldPositions, final int zoomLevel) {
		fWorldPosition.put(projectionId.hashCode() + zoomLevel, worldPositions);
	}

	@Override
	public String toString() {

		final StringBuilder sb = new StringBuilder();

		sb.append("[TourData] ");
		sb.append("tourId:");
		sb.append(tourId);

		return sb.toString();
	}
}
