/*******************************************************************************
 * Copyright (C) 2006, 2007  Wolfgang Schramm
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

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.PostLoad;
import javax.persistence.PostUpdate;
import javax.persistence.Transient;

import net.tourbook.Messages;
import net.tourbook.tour.TourManager;

import org.hibernate.annotations.Cascade;

/**
 * Tour data which will be saved in the database
 */
@Entity
public class TourData {

	/**
	 * Unique persistence id which identifies the tour
	 */
	@Id
	private Long				tourId;

	/**
	 * HH (d) hour of tour
	 */
	private short				startHour;

	/**
	 * MM (d) minute of tour
	 */
	private short				startMinute;

	/**
	 * year of tour start
	 */
	private short				startYear;

	/**
	 * mm (d) month of tour
	 */
	private short				startMonth;

	/**
	 * dd (d) day of tour
	 */
	private short				startDay;

	/**
	 * week of the tour, 0 is the first week
	 */
	private short				startWeek;

	/**
	 * tttt (h) total distance at tour start (km)
	 */
	private int					startDistance;

	/**
	 * ssss distance msw
	 */
	// is not used any more 6.12.2006
	private int					distance;

	/**
	 * aaaa (h) initial altitude (m)
	 */
	private short				startAltitude;

	/**
	 * pppp (h) initial pulse (bpm)
	 */
	private short				startPulse;

	/**
	 * Tolerance in the Douglas Peucker algorithm when segmenting the tour
	 */
	private short				dpTolerance		= 50;

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
	private String				deviceTourType;

	/*
	 * data from the device
	 */
	private long				deviceTravelTime;

	private int					deviceDistance;

	private int					deviceWheel;

	private int					deviceWeight;

	private int					deviceTotalUp;

	private int					deviceTotalDown;

	/*
	 * calculated tour data
	 */

	/**
	 * total distance (m)
	 */
	private int					tourDistance;

	/**
	 * total recording time (sec)
	 */
	private int					tourRecordingTime;

	/**
	 * total driving time (sec)
	 */
	private int					tourDrivingTime;

	/**
	 * altitude up (m)
	 */
	private int					tourAltUp;

	/**
	 * altitude down (m)
	 */
	private int					tourAltDown;

	// db-version 3 - start

	private short				deviceMode;

	private short				deviceTimeInterval;

	// db-version 3 - end

	/**
	 * data series for time, speed, altitude,...
	 */
	@Basic(optional = false)
	private SerieData			serieData;

	@OneToMany(cascade = CascadeType.ALL, mappedBy = "tourData", fetch = FetchType.EAGER) //$NON-NLS-1$
	@Cascade(org.hibernate.annotations.CascadeType.DELETE_ORPHAN)
	private Set<TourMarker>		tourMarkers		= new HashSet<TourMarker>();

	@OneToMany(cascade = CascadeType.ALL, mappedBy = "tourData", fetch = FetchType.EAGER) //$NON-NLS-1$
	@Cascade(org.hibernate.annotations.CascadeType.DELETE_ORPHAN)
	private Set<TourReference>	tourReferences	= new HashSet<TourReference>();

	@ManyToMany(mappedBy = "tourData", fetch = FetchType.EAGER) //$NON-NLS-1$
	private Set<TourCategory>	tourCategory	= new HashSet<TourCategory>();

	/**
	 * Category of the tour, e.g. bike, mountainbike, jogging, inlinescating
	 */
	@ManyToOne
	private TourType			tourType;

	/**
	 * Person which created this tour
	 */
	@ManyToOne
	private TourPerson			tourPerson;

	/**
	 * Device used for this tour
	 */
	private String				devicePluginId;

	/*
	 * data series from the device
	 */
	@Transient
	public int[]				timeSerie;

	@Transient
	public int[]				distanceSerie;

	@Transient
	public int[]				altitudeSerie;

	@Transient
	public int[]				cadenceSerie;

	@Transient
	public int[]				pulseSerie;

	@Transient
	public int[]				temperatureSerie;

	/*
	 * computed series
	 */
	@Transient
	public int[]				speedSerie;

	@Transient
	public int[]				powerSerie;

	@Transient
	public int[]				altimeterSerie;

	@Transient
	public int[]				gradientSerie;

	/**
	 * Index of the segmented data in the original serie
	 */
	@Transient
	public int					segmentSerieIndex[];

	/**
	 * oooo (o) DD-record // offset
	 */
	@Transient
	public int					offsetDDRecord;

	@Transient
	private Object[]			fTourSegments;

	/*
	 * data for the tour segments
	 */
	@Transient
	public int[]				segmentSerieAltitude;

	@Transient
	public int[]				segmentSerieDistance;

	@Transient
	public int[]				segmentSerieTime;

	@Transient
	public int[]				segmentSerieDrivingTime;

	@Transient
	public float[]				segmentSerieAltimeter;

	@Transient
	public int[]				segmentSerieAltitudeDown;

	@Transient
	public float[]				segmentSerieSpeed;

	@Transient
	public float[]				segmentSerieGradient;

	@Transient
	public float[]				segmentSeriePulse;

	@Transient
	public float[]				segmentSerieCadence;

	public TourData() {}

	/**
	 * Called before this object gets persisted, copy data from the tourdata
	 * object into the object which gets serialized
	 */
	/*
	 * @PrePersist + @PreUpdate is currently disabled for EJB events because of
	 * bug http://opensource.atlassian.com/projects/hibernate/browse/HHH-1921
	 * 2006-08-11
	 */
	public void onPrePersist() {

		if (timeSerie == null) {
			setSerieData(new SerieData(0));
			return;
		}

		final int serieLength = timeSerie.length;

		serieData = new SerieData(serieLength);

		System.arraycopy(altitudeSerie, 0, serieData.altitudeSerie, 0, serieLength);
		System.arraycopy(cadenceSerie, 0, serieData.cadenceSerie, 0, serieLength);
		System.arraycopy(distanceSerie, 0, serieData.distanceSerie, 0, serieLength);
		System.arraycopy(pulseSerie, 0, serieData.pulseSerie, 0, serieLength);
		System.arraycopy(temperatureSerie, 0, serieData.temperatureSerie, 0, serieLength);
		System.arraycopy(timeSerie, 0, serieData.timeSerie, 0, serieLength);

		// System.arraycopy(speedSerie, 0, serieData.speedSerie, 0,
		// serieLength);
		// System.arraycopy(powerSerie, 0, serieData.powerSerie, 0,
		// serieLength);
	}

	public void updateSerieData() {

		final int serieLength = timeSerie.length;

		System.arraycopy(altitudeSerie, 0, serieData.altitudeSerie, 0, serieLength);
		System.arraycopy(cadenceSerie, 0, serieData.cadenceSerie, 0, serieLength);
		System.arraycopy(distanceSerie, 0, serieData.distanceSerie, 0, serieLength);
		System.arraycopy(pulseSerie, 0, serieData.pulseSerie, 0, serieLength);
		System.arraycopy(temperatureSerie, 0, serieData.temperatureSerie, 0, serieLength);
		System.arraycopy(timeSerie, 0, serieData.timeSerie, 0, serieLength);

		// System.arraycopy(speedSerie, 0, serieData.speedSerie, 0,
		// serieLength);
		// System.arraycopy(powerSerie, 0, serieData.powerSerie, 0,
		// serieLength);
	}

	/**
	 * Called after the object was loaded from the persistence store
	 */
	@PostLoad
	@PostUpdate
	public void onPostLoad() {
		/*
		 * create tourdata from the serialized data
		 */

		int serieLength = serieData.timeSerie.length;

		altitudeSerie = new int[serieLength];
		cadenceSerie = new int[serieLength];
		distanceSerie = new int[serieLength];
		pulseSerie = new int[serieLength];
		temperatureSerie = new int[serieLength];
		timeSerie = new int[serieLength];

		// speedSerie = new int[serieLength];
		// powerSerie = new int[serieLength];

		System.arraycopy(serieData.altitudeSerie, 0, altitudeSerie, 0, serieLength);
		System.arraycopy(serieData.cadenceSerie, 0, cadenceSerie, 0, serieLength);
		System.arraycopy(serieData.distanceSerie, 0, distanceSerie, 0, serieLength);
		System.arraycopy(serieData.pulseSerie, 0, pulseSerie, 0, serieLength);
		System.arraycopy(serieData.temperatureSerie, 0, temperatureSerie, 0, serieLength);
		System.arraycopy(serieData.timeSerie, 0, timeSerie, 0, serieLength);

		// System.arraycopy(serieData.speedSerie, 0, speedSerie, 0,
		// serieLength);
		// System.arraycopy(serieData.powerSerie, 0, powerSerie, 0,
		// serieLength);
	}

	/**
	 * Convert TimeData from arraylist into the internal structure
	 */
	public void createTimeSeries(ArrayList<TimeData> timeDataList) {

		int serieLength = timeDataList.size();

		if (serieLength > 0) {

			timeSerie = new int[serieLength];
			distanceSerie = new int[serieLength];
			altitudeSerie = new int[serieLength];
			pulseSerie = new int[serieLength];
			cadenceSerie = new int[serieLength];
			temperatureSerie = new int[serieLength];

			// speedSerie = new int[serieLength];
			// powerSerie = new int[serieLength];

			int distanceDiff[] = new int[serieLength];
			int index = 0;

			int timeAbsolute = 0;
			int altitudeAbsolute = 0;
			int distanceAbsolute = 0;

			// convert data from the tour format into an interger[]
			for (TimeData timeItem : timeDataList) {

				timeSerie[index] = timeAbsolute += deviceTimeInterval;
				distanceSerie[index] = distanceAbsolute += timeItem.distance;
				altitudeSerie[index] = altitudeAbsolute += timeItem.altitude;
				pulseSerie[index] = timeItem.pulse;
				temperatureSerie[index] = timeItem.temperature;
				cadenceSerie[index] = timeItem.cadence;

				// powerSerie[index] = 0;

				distanceDiff[index] = timeItem.distance;

				if (timeItem.marker != 0) {

					// create a new marker
					TourMarker tourMarker = new TourMarker(this, TourMarker.MARKER_TYPE_DEVICE);
					tourMarker.setLabel(Messages.TourData_Label_device_marker);
					tourMarker.setVisualPosition(TourMarker.VISUAL_HORIZONTAL_ABOVE_GRAPH_CENTERED);
					tourMarker.setTime(timeAbsolute + timeItem.marker);
					tourMarker.setDistance(distanceAbsolute);
					tourMarker.setSerieIndex(index);

					getTourMarkers().add(tourMarker);
				}
				index++;
			}
		}
	}

	/**
	 * Creates the unique tour id
	 */
	public void createTourId() {

		String tourStart = Short.toString(getStartYear())
				+ Short.toString(getStartMonth())
				+ Short.toString(getStartDay())
				+ Short.toString(getStartHour())
				+ Short.toString(getStartMinute())
				+ Integer.toString(getStartDistance());

		setTourId(Long.parseLong(tourStart));
	}

	public Object[] getTourSegments() {
		return fTourSegments;
	}

	/**
	 * Create the tour segment list from the segment index array
	 * 
	 * @return
	 */
	public Object[] createTourSegments() {

		int segmentSerieLength = segmentSerieIndex.length;

		if (segmentSerieIndex == null || segmentSerieLength < 2) {
			// at least two points are required to build a segment
			return new Object[0];
		}

		ArrayList<TourSegment> tourSegments = new ArrayList<TourSegment>(segmentSerieLength);
		final int firstSerieIndex = segmentSerieIndex[0];

		// get start values
		int distanceStart = distanceSerie[firstSerieIndex];
		int altitudeStart = altitudeSerie[firstSerieIndex];
		int timeStart = timeSerie[firstSerieIndex];
		final int timeSlice = timeSerie[1] - timeSerie[0];

		segmentSerieAltitude = new int[segmentSerieLength];
		segmentSerieDistance = new int[segmentSerieLength];
		segmentSerieTime = new int[segmentSerieLength];
		segmentSerieDrivingTime = new int[segmentSerieLength];
		segmentSerieAltitudeDown = new int[segmentSerieLength];

		segmentSerieAltimeter = new float[segmentSerieLength];
		segmentSerieSpeed = new float[segmentSerieLength];
		segmentSerieGradient = new float[segmentSerieLength];
		segmentSeriePulse = new float[segmentSerieLength];
		segmentSerieCadence = new float[segmentSerieLength];

		for (int iSegment = 1; iSegment < segmentSerieLength; iSegment++) {

			final int segmentIndex = iSegment;

			final int segmentStartIndex = segmentSerieIndex[iSegment - 1];
			final int segmentEndIndex = segmentSerieIndex[iSegment];

			TourSegment segment = new TourSegment();
			tourSegments.add(segment);

			segment.serieIndexStart = segmentStartIndex;
			segment.serieIndexEnd = segmentEndIndex;

			// compute difference values between start and end
			int altitudeEnd = altitudeSerie[segmentEndIndex];
			int distanceEnd = distanceSerie[segmentEndIndex];
			int timeEnd = timeSerie[segmentEndIndex];

			segmentSerieAltitude[segmentIndex] = segment.altitude = altitudeEnd - altitudeStart;
			segmentSerieDistance[segmentIndex] = segment.distance = distanceEnd - distanceStart;
			segmentSerieTime[segmentIndex] = segment.time = timeEnd - timeStart;
			segmentSerieDrivingTime[segmentIndex] = segment.drivingTime = getDrivingTime(
					segment,
					segmentStartIndex,
					segmentEndIndex,
					timeSlice);

			int altitudeUp = 0;
			int altitudeDown = 0;
			int altitude1 = altitudeSerie[segmentStartIndex];

			// compute altitude up/down for the segment
			for (int serieIndex = segmentStartIndex + 1; serieIndex <= segmentEndIndex; serieIndex++) {

				int altitude2 = altitudeSerie[serieIndex];
				int altitudeDiff = altitude2 - altitude1;
				altitude1 = altitude2;

				altitudeUp += altitudeDiff >= 0 ? altitudeDiff : 0;
				altitudeDown += altitudeDiff < 0 ? altitudeDiff : 0;
			}

			// compute pulse average
			int pulseSum = 0;
			for (int serieIndex = segmentStartIndex + 1; serieIndex <= segmentEndIndex; serieIndex++) {
				pulseSum += pulseSerie[serieIndex];
			}

			segment.altitudeUp = altitudeUp;
			segmentSerieAltitudeDown[segmentIndex] = segment.altitudeDown = altitudeDown;

			segmentSerieSpeed[segmentIndex] = segment.speed = segment.drivingTime == 0
					? 0
					: (float) ((float) segment.distance / segment.drivingTime * 3.6);

			segmentSerieGradient[segmentIndex] = segment.gradient = (float) segment.altitude
					* 100
					/ segment.distance;

			segmentSerieAltimeter[segmentIndex] = segment.drivingTime == 0
					? 0
					: (float) (segment.altitudeUp) / segment.drivingTime * 3600;

			segmentSeriePulse[segmentIndex] = pulseSum / (segmentEndIndex - segmentStartIndex);

			// end point of current segment is the start of the next segment
			altitudeStart = altitudeEnd;
			distanceStart = distanceEnd;
			timeStart = timeEnd;
		}

		fTourSegments = tourSegments.toArray();

		return fTourSegments;
	}

	private int getDrivingTime(	TourSegment tourSegment,
								int valuesIndexLeft,
								int valuesIndexRight,
								int timeSlice) {

		/*
		 * calculate the driving time, ignore the time when the distance is 0
		 */
		int ignoreTimeCounter = 0;
		int oldDistance = 0;

		for (int valueIndex = valuesIndexLeft; valueIndex <= valuesIndexRight; valueIndex++) {
			if (distanceSerie[valueIndex] == oldDistance) {
				ignoreTimeCounter++;
			}
			oldDistance = distanceSerie[valueIndex];
		}

		final float time = tourSegment.time - (ignoreTimeCounter * timeSlice);
		// final float distance = tourSegment.distance;
		//
		// return (int) (distance / time * 3.6f);
		return (int) time;
	}

	public void dumpData() {

		PrintStream out = System.out;

		out.println("----------------------------------------------------"); //$NON-NLS-1$
		out.println("TOUR DATA"); //$NON-NLS-1$
		out.println("----------------------------------------------------"); //$NON-NLS-1$
		out.println("Typ:			" + getDeviceTourType()); //$NON-NLS-1$
		out.println("Date:			" + getStartDay() + "." + getStartMonth() + "." + getStartYear()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		out.println("Time:			" + getStartHour() + ":" + getStartMinute()); //$NON-NLS-1$ //$NON-NLS-2$
		out.println("Total distance:		" + getStartDistance()); //$NON-NLS-1$
		// out.println("Distance: " + getDistance());
		out.println("Altitude:		" + getStartAltitude()); //$NON-NLS-1$
		out.println("Pulse:			" + getStartPulse()); //$NON-NLS-1$
		out.println("Offset DD record:	" + offsetDDRecord); //$NON-NLS-1$
	}

	public void dumpTourTotal() {

		PrintStream out = System.out;

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

	public void dumpTime() {
		PrintStream out = System.out;

		out.print((getTourRecordingTime() / 3600)
				+ ":" //$NON-NLS-1$
				+ ((getTourRecordingTime() % 3600) / 60)
				+ ":" //$NON-NLS-1$
				+ ((getTourRecordingTime() % 3600) % 60)
				+ "  "); //$NON-NLS-1$
		out.print(getTourDistance());
	}

	/**
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}

		if (this == obj) {
			return true;
		}

		if (!this.getClass().equals(obj.getClass())) {
			return false;
		}

		TourData td = (TourData) obj;

		return this.getStartYear() == td.getStartYear()
				&& this.getStartMonth() == td.getStartMonth()
				&& this.getStartDay() == td.getStartDay()
				&& this.getStartHour() == td.getStartHour()
				&& this.getStartMinute() == td.getStartMinute()
				&& this.getTourDistance() == td.getTourDistance()
				&& this.getTourRecordingTime() == td.getTourRecordingTime();
	}

	/**
	 * @see java.lang.Object#hashCode()
	 */
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

	public void setTourId(Long tourId) {
		this.tourId = tourId;
	}

	public Long getTourId() {
		return tourId;
	}

	public void setDeviceTourType(String tourType) {
		this.deviceTourType = tourType;
	}

	public String getDeviceTourType() {
		return deviceTourType;
	}

	public void setStartHour(short startHour) {
		this.startHour = startHour;
	}

	public short getStartHour() {
		return startHour;
	}

	public void setStartMinute(short startMinute) {
		this.startMinute = startMinute;
	}

	public short getStartMinute() {
		return startMinute;
	}

	public void setStartMonth(short startMonth) {
		this.startMonth = startMonth;
	}

	public short getStartMonth() {
		return startMonth;
	}

	public void setStartDay(short startDay) {
		this.startDay = startDay;
	}

	public short getStartDay() {
		return startDay;
	}

	public void setStartYear(short startYear) {
		this.startYear = startYear;
	}

	public short getStartYear() {
		return startYear;
	}

	public void setStartDistance(int startDistance) {
		this.startDistance = startDistance;
	}

	public int getStartDistance() {
		return startDistance;
	}

	// public void setDistance(int distance) {
	// this.distance = distance;
	// }

	// public int getDistance() {
	// return distance;
	// }

	public void setStartAltitude(short startAltitude) {
		this.startAltitude = startAltitude;
	}

	public short getStartAltitude() {
		return startAltitude;
	}

	public void setStartPulse(short startPulse) {
		this.startPulse = startPulse;
	}

	public short getStartPulse() {
		return startPulse;
	}

	public void setDpTolerance(short dpTolerance) {
		this.dpTolerance = dpTolerance;
	}

	public short getDpTolerance() {
		return dpTolerance;
	}

	public void setDeviceTravelTime(long deviceTravelTime) {
		this.deviceTravelTime = deviceTravelTime;
	}

	public long getDeviceTravelTime() {
		return deviceTravelTime;
	}

	public void setDeviceDistance(int deviceDistance) {
		this.deviceDistance = deviceDistance;
	}

	public int getDeviceDistance() {
		return deviceDistance;
	}

	public void setDeviceWheel(int deviceWheel) {
		this.deviceWheel = deviceWheel;
	}

	public int getDeviceWheel() {
		return deviceWheel;
	}

	public void setDeviceWeight(int deviceWeight) {
		this.deviceWeight = deviceWeight;
	}

	public int getDeviceWeight() {
		return deviceWeight;
	}

	public void setDeviceTotalUp(int deviceTotalUp) {
		this.deviceTotalUp = deviceTotalUp;
	}

	public int getDeviceTotalUp() {
		return deviceTotalUp;
	}

	public void setDeviceTotalDown(int deviceTotalDown) {
		this.deviceTotalDown = deviceTotalDown;
	}

	public int getDeviceTotalDown() {
		return deviceTotalDown;
	}

	public void setTourDistance(int tourDistance) {
		this.tourDistance = tourDistance;
	}

	public int getTourDistance() {
		return tourDistance;
	}

	public void setTourRecordingTime(int tourRecordingTime) {
		this.tourRecordingTime = tourRecordingTime;
	}

	public int getTourRecordingTime() {
		return tourRecordingTime;
	}

	public void setTourDrivingTime(int tourDrivingTime) {
		this.tourDrivingTime = tourDrivingTime;
	}

	public int getTourDrivingTime() {
		return tourDrivingTime;
	}

	public void setTourAltUp(int tourAltUp) {
		this.tourAltUp = tourAltUp;
	}

	public int getTourAltUp() {
		return tourAltUp;
	}

	public void setTourAltDown(int tourAltDown) {
		this.tourAltDown = tourAltDown;
	}

	public int getTourAltDown() {
		return tourAltDown;
	}

	public void setSerieData(SerieData serieData) {
		this.serieData = serieData;
	}

	public SerieData getSerieData() {
		return serieData;
	}

	public Collection<TourMarker> getTourMarkers() {
		return tourMarkers;
	}

	public Collection<TourReference> getTourReferences() {
		return tourReferences;
	}

	public Collection<TourCategory> getTourCategory() {
		return tourCategory;
	}

	public TourType getTourType() {
		return tourType;
	}

	public void setTourType(TourType tourType) {
		this.tourType = tourType;
	}

	public short getStartWeek() {
		return startWeek;
	}

	public void setStartWeek(short startWeek) {
		this.startWeek = startWeek;
	}

	public TourPerson getTourPerson() {
		return tourPerson;
	}

	public void setTourPerson(TourPerson tourPerson) {
		this.tourPerson = tourPerson;
	}

	public String getDeviceId() {
		return devicePluginId;
	}

	public void setDeviceId(String deviceId) {
		this.devicePluginId = deviceId;
	}

	public short getDeviceMode() {
		return deviceMode;
	}

	public void setDeviceMode(short deviceMode) {
		this.deviceMode = deviceMode;
	}

	public short getDeviceTimeInterval() {
		return deviceTimeInterval;
	}

	public void setDeviceTimeInterval(short deviceTimeInterval) {
		this.deviceTimeInterval = deviceTimeInterval;
	}

	public void computeTourDrivingTime() {

		int maxIndex = Math.max(0, timeSerie.length - 1);

		tourDrivingTime = timeSerie[maxIndex]
				- (TourManager.getInstance().getIgnoreTimeSlices(
						distanceSerie,
						0,
						maxIndex,
						10 / deviceTimeInterval) * deviceTimeInterval);

	}

}
