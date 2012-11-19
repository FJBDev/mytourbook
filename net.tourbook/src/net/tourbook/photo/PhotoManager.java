/*******************************************************************************
 * Copyright (C) 2005, 2012  Wolfgang Schramm and Contributors
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
package net.tourbook.photo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import net.tourbook.Messages;
import net.tourbook.application.PerspectiveFactoryPhoto;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.Util;
import net.tourbook.data.TourData;
import net.tourbook.data.TourPhoto;
import net.tourbook.database.TourDatabase;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.SQLFilter;

import org.eclipse.core.runtime.ListenerList;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.WorkbenchException;
import org.joda.time.DateTime;

public class PhotoManager {

	private static final String				STATE_CAMERA_ADJUSTMENT_NAME	= "STATE_CAMERA_ADJUSTMENT_NAME";			//$NON-NLS-1$
	private static final String				STATE_CAMERA_ADJUSTMENT_TIME	= "STATE_CAMERA_ADJUSTMENT_TIME";			//$NON-NLS-1$

	private static final String				CAMERA_UNKNOWN_KEY				= "CAMERA_UNKNOWN_KEY";					//$NON-NLS-1$

	private static final IDialogSettings	_state							= TourbookPlugin.getDefault() //
																					.getDialogSettingsSection(
																							"PhotoManager");
	private static PhotoManager				_instance;

	/**
	 * Contains all cameras which are every used, key is the camera name.
	 */
	private static HashMap<String, Camera>	_allAvailableCameras			= new HashMap<String, Camera>();

	private static final ListenerList		_photoEventListeners			= new ListenerList(ListenerList.IDENTITY);

	private Connection						_sqlConnection;
	private PreparedStatement				_sqlStatement;
	private long							_sqlTourStart					= Long.MAX_VALUE;
	private long							_sqlTourEnd						= Long.MIN_VALUE;

	private ArrayList<TourPhotoLink>		_allDbTourPhotoLinks			= new ArrayList<TourPhotoLink>();

	public static void addPhotoEventListener(final IPhotoEventListener listener) {
		_photoEventListeners.add(listener);
	}

	public static void fireEvent(final PhotoEventId photoEventId, final Object data) {

//		System.out.println(UI.timeStampNano() + " PhotoManager\tfireEvent\t" + data.getClass().getSimpleName());
//		// TODO remove SYSTEM.OUT.PRINTLN

		final Object[] allListeners = _photoEventListeners.getListeners();
		for (final Object listener : allListeners) {
			((IPhotoEventListener) listener).photoEvent(photoEventId, data);
		}
	}

	public static PhotoManager getInstance() {

		if (_instance == null) {
			_instance = new PhotoManager();
		}

		return _instance;
	}

	public static void openPhotoMergePerspective(final PhotosWithExifSelection photoWrapperSelection) {

		final IWorkbench workbench = PlatformUI.getWorkbench();
		final IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();

		if (window != null) {
			try {

				// show tour photo link perspective
				workbench.showPerspective(PerspectiveFactoryPhoto.PERSPECTIVE_ID, window);

				// show tour photo link view
				final TourPhotoLinkView view = (TourPhotoLinkView) Util.showView(TourPhotoLinkView.ID);

				if (view != null) {
					view.updatePhotosAndTours(photoWrapperSelection.photos, false);
				}

			} catch (final PartInitException e) {
				StatusUtil.showStatus(e);
			} catch (final WorkbenchException e) {
				StatusUtil.showStatus(e);
			}
		}
	}

	public static void removePhotoEventListener(final IPhotoEventListener listener) {
		if (listener != null) {
			_photoEventListeners.remove(listener);
		}
	}

	public static void restoreState() {

		/*
		 * cameras + time adjustment
		 */
		final String[] cameraNames = _state.getArray(STATE_CAMERA_ADJUSTMENT_NAME);
		final long[] adjustments = Util.getStateLongArray(_state, STATE_CAMERA_ADJUSTMENT_TIME, null);

		if (cameraNames != null && adjustments != null && cameraNames.length == adjustments.length) {

			// it seems that the values are OK, create cameras with time adjustmens

			for (int index = 0; index < cameraNames.length; index++) {

				final String cameraName = cameraNames[index];

				final Camera camera = new Camera(cameraName);
				camera.timeAdjustment = adjustments[index];

				_allAvailableCameras.put(cameraName, camera);
			}
		}
	}

	public static void saveState() {

		/*
		 * camera time adjustment
		 */
		final int size = _allAvailableCameras.size();

		final String[] cameras = new String[size];
		final long[] adjustment = new long[size];

		int index = 0;
		for (final Camera camera : _allAvailableCameras.values()) {
			cameras[index] = camera.cameraName;
			adjustment[index] = camera.timeAdjustment;
			index++;
		}
		_state.put(STATE_CAMERA_ADJUSTMENT_NAME, cameras);
		Util.setState(_state, STATE_CAMERA_ADJUSTMENT_TIME, adjustment);
	}

	private static void setTourCameras(final HashMap<String, String> cameras, final TourPhotoLink historyTour) {

		final Collection<String> allCameras = cameras.values();
		Collections.sort(new ArrayList<String>(allCameras));

		final StringBuilder sb = new StringBuilder();
		boolean isFirst = true;

		for (final String camera : allCameras) {
			if (isFirst) {
				isFirst = false;
				sb.append(camera);
			} else {
				sb.append(UI.COMMA_SPACE);
				sb.append(camera);
			}
		}
		historyTour.tourCameras = sb.toString();
	}

	/**
	 * create pseudo tours for photos which are not contained in a tour and remove all tours which
	 * do not contain any photos
	 * 
	 * @param allPhotos
	 * @param allTourPhotoLinks
	 * @param isShowToursOnlyWithPhotos
	 * @param _allTourCameras
	 */
	void createTourPhotoLinks(	final ArrayList<PhotoWrapper> allPhotos,
								final ArrayList<TourPhotoLink> allTourPhotoLinks,
								final boolean isShowToursOnlyWithPhotos,
								final HashMap<String, Camera> allTourCameras) {

		TourPhotoLink currentTourPhotoLink = createTourPhotoLinks_10_GetFirstTour(allPhotos);

		final HashMap<String, String> tourCameras = new HashMap<String, String>();

		final int numberOfRealTours = _allDbTourPhotoLinks.size();
		long nextDbTourStartTime = numberOfRealTours > 0 ? _allDbTourPhotoLinks.get(0).tourStartTime : Long.MIN_VALUE;

		int tourIndex = 0;
		long photoTime = 0;

		// loop: all photos
		for (final PhotoWrapper photoWrapper : allPhotos) {

			final Photo photo = photoWrapper.photo;
			photoTime = photoWrapper.adjustedTime;

			// check if current photo can be put into current tour photo link
			if (currentTourPhotoLink.isHistoryTour == false && photoTime <= currentTourPhotoLink.tourEndTime) {

				// current photo can be put into current real tour

			} else if (currentTourPhotoLink.isHistoryTour && photoTime < nextDbTourStartTime) {

				// current photo can be put into current history tour

			} else {

				// current photo do not fit into current photo link

				// finalize current tour photo link
				createTourPhotoLinks_30_FinalizeCurrentTourPhotoLink(
						currentTourPhotoLink,
						tourCameras,
						allTourPhotoLinks,
						isShowToursOnlyWithPhotos);

				currentTourPhotoLink = null;
				tourCameras.clear();

				/*
				 * create/get new merge tour
				 */
				if (tourIndex >= numberOfRealTours) {

					/*
					 * there are no further tours which can contain photos, put remaining photos
					 * into a history tour
					 */

					nextDbTourStartTime = Long.MAX_VALUE;

				} else {

					for (; tourIndex < numberOfRealTours; tourIndex++) {

						final TourPhotoLink dbTourPhotoLink = _allDbTourPhotoLinks.get(tourIndex);

						final long dbTourStart = dbTourPhotoLink.tourStartTime;
						final long dbTourEnd = dbTourPhotoLink.tourEndTime;

						if (photoTime < dbTourStart) {

							// image time is before the next tour start, create history tour

							nextDbTourStartTime = dbTourStart;

							break;
						}

						if (photoTime >= dbTourStart && photoTime <= dbTourEnd) {

							// current photo can be put into current tour

							currentTourPhotoLink = dbTourPhotoLink;

							break;
						}

						// current tour do not contain any images

						if (isShowToursOnlyWithPhotos == false) {

							// tours without photos are displayed

							createTourPhotoLinks_40_AddTour(dbTourPhotoLink, allTourPhotoLinks);
						}

						// get start time for the next tour
						if (tourIndex + 1 < numberOfRealTours) {
							nextDbTourStartTime = _allDbTourPhotoLinks.get(tourIndex + 1).tourStartTime;
						} else {
							nextDbTourStartTime = Long.MAX_VALUE;
						}
					}
				}

				if (currentTourPhotoLink == null) {

					// create history tour

					currentTourPhotoLink = new TourPhotoLink(photoTime);
				}
			}

			currentTourPhotoLink.tourPhotos.add(photoWrapper);

			// set camera into the photo
			final Camera camera = setCamera(photo, allTourCameras);

			tourCameras.put(camera.cameraName, camera.cameraName);

			// set number of GPS/No GPS photos
			final double latitude = photo.getLatitude();
			if (latitude == Double.MIN_VALUE) {
				currentTourPhotoLink.numberOfNoGPSPhotos++;
			} else {
				currentTourPhotoLink.numberOfGPSPhotos++;
			}
		}

		createTourPhotoLinks_30_FinalizeCurrentTourPhotoLink(
				currentTourPhotoLink,
				tourCameras,
				allTourPhotoLinks,
				isShowToursOnlyWithPhotos);

		createTourPhotoLinks_60_MergeHistoryTours(allTourPhotoLinks);

		/*
		 * put tour GPS into photo
		 */
		final ArrayList<PhotoWrapper> updatedPhotos = new ArrayList<PhotoWrapper>();
		final List<TourPhotoLink> tourPhotoLinksWithGps = new ArrayList<TourPhotoLink>();
		for (final TourPhotoLink tourPhotoLink : allTourPhotoLinks) {
			if (tourPhotoLink.tourId != Long.MIN_VALUE) {
				tourPhotoLinksWithGps.add(tourPhotoLink);
			}
		}

		if (tourPhotoLinksWithGps.size() > 0) {
			setTourGpsIntoPhotos(updatedPhotos, tourPhotoLinksWithGps);
		}
	}

	/**
	 * Get/Create first tour photo link
	 * 
	 * @param allPhotos
	 */
	private TourPhotoLink createTourPhotoLinks_10_GetFirstTour(final ArrayList<PhotoWrapper> allPhotos) {

		TourPhotoLink currentTourPhotoLink = null;

		if (_allDbTourPhotoLinks.size() > 0) {

			// real tours are available

			final TourPhotoLink firstTour = _allDbTourPhotoLinks.get(0);
			final PhotoWrapper firstPhotoWrapper = allPhotos.get(0);

			final DateTime firstPhotoTime = new DateTime(firstPhotoWrapper.adjustedTime);
			if (firstPhotoTime.isBefore(firstTour.tourStartTime)) {

				// first photo is before the first tour, create dummy tour

			} else {

				// first tour starts before the first photo

				currentTourPhotoLink = firstTour;
			}
		} else {

			// there are no real tours, create dummy tour
		}

		if (currentTourPhotoLink == null) {

			// 1st tour is a history tour

			final long tourStartUTC = allPhotos.get(0).adjustedTime;
//			final int tourStartUTCZoneOffset = DateTimeZone.getDefault().getOffset(tourStartUTC);

			currentTourPhotoLink = new TourPhotoLink(tourStartUTC);
		}

		return currentTourPhotoLink;
	}

	/**
	 * Keep current tour when it contains photos.
	 * 
	 * @param currentTourPhotoLink
	 * @param tourCameras
	 * @param allTourPhotoLinks
	 * @param isShowToursOnlyWithPhotos
	 */
	private void createTourPhotoLinks_30_FinalizeCurrentTourPhotoLink(	final TourPhotoLink currentTourPhotoLink,
																		final HashMap<String, String> tourCameras,
																		final ArrayList<TourPhotoLink> allTourPhotoLinks,
																		final boolean isShowToursOnlyWithPhotos) {

		// keep only tours which contain photos
		final boolean isNoPhotos = currentTourPhotoLink.tourPhotos.size() == 0;

		if (isNoPhotos && currentTourPhotoLink.isHistoryTour || isNoPhotos && isShowToursOnlyWithPhotos) {
			return;
		}

		// set tour end time
		if (currentTourPhotoLink.isHistoryTour) {
			currentTourPhotoLink.setTourEndTime(Long.MAX_VALUE);
		}

		setTourCameras(tourCameras, currentTourPhotoLink);

		createTourPhotoLinks_40_AddTour(currentTourPhotoLink, allTourPhotoLinks);
	}

	private void createTourPhotoLinks_40_AddTour(	final TourPhotoLink tourPhotoLink,
													final ArrayList<TourPhotoLink> allTourPhotoLinks) {

		boolean isAddLink = true;
		final int numberOfLinks = allTourPhotoLinks.size();

		if (numberOfLinks > 0) {

			// check if this tour is already added, this algorithm to add tours is a little bit complex

			final TourPhotoLink prevTour = allTourPhotoLinks.get(numberOfLinks - 1);
			if (prevTour.equals(tourPhotoLink)) {
				isAddLink = false;
			}
		}

		if (isAddLink) {
			allTourPhotoLinks.add(tourPhotoLink);
		}
	}

	/**
	 * History tours can occure multiple times in sequence, when tours between history tours do not
	 * contain photos. This will merge multiple history tours into one.
	 * 
	 * @param allTourPhotoLinks
	 */
	private void createTourPhotoLinks_60_MergeHistoryTours(final ArrayList<TourPhotoLink> allTourPhotoLinks) {

		if (allTourPhotoLinks.size() == 0) {
			return;
		}

		boolean isSubsequentHistory = false;
		boolean isHistory = false;

		for (final TourPhotoLink tourPhotoLink : allTourPhotoLinks) {

			if (isHistory && tourPhotoLink.isHistoryTour == isHistory) {

				// 2 subsequent tours contains the same tour type
				isSubsequentHistory = true;
				break;
			}

			isHistory = tourPhotoLink.isHistoryTour;
		}

		if (isSubsequentHistory == false) {
			// there is nothing to merge
			return;
		}

		final ArrayList<TourPhotoLink> mergedLinks = new ArrayList<TourPhotoLink>();
		TourPhotoLink prevHistoryTour = null;

		for (final TourPhotoLink tourPhotoLink : allTourPhotoLinks) {

			final boolean isHistoryTour = tourPhotoLink.isHistoryTour;

			if (isHistoryTour && prevHistoryTour == null) {

				// first history tour

				prevHistoryTour = tourPhotoLink;

				continue;
			}

			if (isHistoryTour && prevHistoryTour != null) {

				// this is a subsequent history tour, it is merged into previous history tour

				prevHistoryTour.tourPhotos.addAll(tourPhotoLink.tourPhotos);
				prevHistoryTour.numberOfGPSPhotos += tourPhotoLink.numberOfGPSPhotos;
				prevHistoryTour.numberOfNoGPSPhotos += tourPhotoLink.numberOfNoGPSPhotos;

				continue;
			}

			if (isHistoryTour == false && prevHistoryTour != null) {

				// this is a real tour, finalize previous history tour

				prevHistoryTour.setTourEndTime(Long.MAX_VALUE);
				mergedLinks.add(prevHistoryTour);
			}

			prevHistoryTour = null;

			// this is a real tour

			mergedLinks.add(tourPhotoLink);
		}

		if (prevHistoryTour != null) {

			// finalize previous history tour
			prevHistoryTour.setTourEndTime(Long.MAX_VALUE);
			mergedLinks.add(prevHistoryTour);
		}

		allTourPhotoLinks.clear();
		allTourPhotoLinks.addAll(mergedLinks);
	}

	void createTourPhotoLinks_90_FilterNoTours(	final ArrayList<PhotoWrapper> allPhotos,
												final ArrayList<TourPhotoLink> allTourPhotoLinks,
												final HashMap<String, Camera> allTourCameras) {

		final HashMap<String, String> tourCameras = new HashMap<String, String>();

		final TourPhotoLink historyTour = new TourPhotoLink(allPhotos.get(0).adjustedTime);
		historyTour.tourPhotos.addAll(allPhotos);

		for (final PhotoWrapper photoWrapper : allPhotos) {

			final Photo photo = photoWrapper.photo;

			// set camera into the photo
			final Camera camera = setCamera(photo, allTourCameras);

			tourCameras.put(camera.cameraName, camera.cameraName);

			// set number of GPS/No GPS photos
			final double latitude = photo.getLatitude();
			if (latitude == Double.MIN_VALUE) {
				historyTour.numberOfNoGPSPhotos++;
			} else {
				historyTour.numberOfGPSPhotos++;
			}
		}

		setTourCameras(tourCameras, historyTour);

		// finalize history tour
		historyTour.setTourEndTime(Long.MAX_VALUE);

		allTourPhotoLinks.add(historyTour);
	}

	ArrayList<TourPhotoLink> getAllDbTourPhotoLinks() {
		return _allDbTourPhotoLinks;
	}

	/**
	 * The loaded tours can be retrieved with {@link #getAllDbTourPhotoLinks()}
	 * 
	 * @param allPhotos
	 * @return Returns <code>true</code> when tours are loaded from the database, <code>false</code>
	 *         is returned when all photo time stamps are within the previously loaded tours.
	 */

	boolean loadToursFromDb(final ArrayList<PhotoWrapper> allPhotos) {

		/*
		 * get date for 1st and last photo
		 */
		long photoStartDate = allPhotos.get(0).adjustedTime;
		long photoEndDate = photoStartDate;

		for (final PhotoWrapper photoWrapper : allPhotos) {

			final long imageSortingTime = photoWrapper.imageExifTime;

			if (imageSortingTime < photoStartDate) {
				photoStartDate = imageSortingTime;
			} else if (imageSortingTime > photoEndDate) {
				photoEndDate = imageSortingTime;
			}
		}

		// check if tours are already loaded
		if (photoStartDate >= _sqlTourStart && photoEndDate <= _sqlTourEnd) {

			// photos are contained in the already loaded tours
			return false;
		}

		// adjust by 5 days that time adjustments are covered
		final long tourStartDate = photoStartDate - 5 * UI.DAY_IN_SECONDS * 1000;
		final long tourEndDate = photoEndDate + 5 * UI.DAY_IN_SECONDS * 1000;

		BusyIndicator.showWhile(Display.getCurrent(), new Runnable() {
			public void run() {
				loadToursFromDb_Runnable(tourStartDate, tourEndDate);
			}
		});

		return true;
	}

	private void loadToursFromDb_Runnable(final long dbStartDate, final long dbEndDate) {

//		final long start = System.currentTimeMillis();

		_allDbTourPhotoLinks.clear();

		try {

			if (_sqlConnection == null) {

				final SQLFilter sqlFilter = new SQLFilter();

				final String sql = UI.EMPTY_STRING //

						+ "SELECT " //$NON-NLS-1$

						+ " TourId," //						1 //$NON-NLS-1$
						+ " TourStartTime," //				2 //$NON-NLS-1$
						+ " TourEndTime," //				3 //$NON-NLS-1$
						+ " TourType_TypeId" //				4 //$NON-NLS-1$

						+ UI.NEW_LINE

						+ (" FROM " + TourDatabase.TABLE_TOUR_DATA + UI.NEW_LINE) //$NON-NLS-1$

						+ " WHERE" //$NON-NLS-1$
						+ (" TourStartTime >= ?") //$NON-NLS-1$
						+ (" AND TourEndTime <= ?") //$NON-NLS-1$

						+ sqlFilter.getWhereClause()

						+ UI.NEW_LINE

						+ (" ORDER BY TourStartTime"); //$NON-NLS-1$

				_sqlConnection = TourDatabase.getInstance().getConnection();
				_sqlStatement = _sqlConnection.prepareStatement(sql);

				sqlFilter.setParameters(_sqlStatement, 3);
			}

			_sqlStatement.setLong(1, dbStartDate);
			_sqlStatement.setLong(2, dbEndDate);

			_sqlTourStart = Long.MAX_VALUE;
			_sqlTourEnd = Long.MIN_VALUE;

			final ResultSet result = _sqlStatement.executeQuery();

			while (result.next()) {

				final long dbTourId = result.getLong(1);
				final long dbTourStart = result.getLong(2);
				final long dbTourEnd = result.getLong(3);
				final Object dbTourTypeId = result.getObject(4);

				final TourPhotoLink dbTourPhotoLink = new TourPhotoLink(dbTourId, dbTourStart, dbTourEnd);

				dbTourPhotoLink.tourTypeId = (dbTourTypeId == null ? //
						TourDatabase.ENTITY_IS_NOT_SAVED
						: (Long) dbTourTypeId);

				_allDbTourPhotoLinks.add(dbTourPhotoLink);

				// get range of all tour start/end
				if (dbTourStart < _sqlTourStart) {
					_sqlTourStart = dbTourStart;
				}
				if (dbTourEnd > _sqlTourEnd) {
					_sqlTourEnd = dbTourEnd;
				}
			}

		} catch (final SQLException e) {
			net.tourbook.ui.UI.showSQLException(e);
		}
//		System.out.println("loadToursFromDb_Runnable()\t"
//				+ (System.currentTimeMillis() - start)
//				+ " ms\t"
//				+ (new DateTime(_sqlTourStart))
//				+ "\t"
//				+ new DateTime(_sqlTourEnd));
//		// TODO remove SYSTEM.OUT.PRINTLN
	}

	void resetTourStartEnd() {

		if (_sqlConnection != null) {

			Util.sqlClose(_sqlStatement);
			TourDatabase.closeConnection(_sqlConnection);

			_sqlStatement = null;
			_sqlConnection = null;

			// force reloading cached start/end
			_sqlTourStart = Long.MAX_VALUE;
			_sqlTourEnd = Long.MIN_VALUE;
		}
	}

	/**
	 * Creates a camera when not yet created and sets it into the photo.
	 * 
	 * @param photo
	 * @param allTourCameras
	 * @return Returns camera which is set into the photo.
	 */
	Camera setCamera(final Photo photo, final HashMap<String, Camera> allTourCameras) {

		// get camera
		String photoCameraName = null;
		final PhotoImageMetadata metaData = photo.getImageMetaDataRaw();
		if (metaData != null) {
			photoCameraName = metaData.model;
		}

		Camera camera = null;

		if (photoCameraName == null || photoCameraName.length() == 0) {

			// camera is not set in the photo

			camera = _allAvailableCameras.get(CAMERA_UNKNOWN_KEY);

			if (camera == null) {
				camera = new Camera(Messages.Photos_AndTours_Label_NoCamera);
				_allAvailableCameras.put(CAMERA_UNKNOWN_KEY, camera);
			}

		} else {

			// camera is set in the photo

			camera = _allAvailableCameras.get(photoCameraName);

			if (camera == null) {
				camera = new Camera(photoCameraName);
				_allAvailableCameras.put(photoCameraName, camera);
			}
		}

		allTourCameras.put(camera.cameraName, camera);
		photo.getPhotoWrapper().camera = camera;

		return camera;
	}

	void setTourGpsIntoPhotos(	final ArrayList<PhotoWrapper> updatedPhotos,
								final List<TourPhotoLink> tourPhotoLinksWithGps) {

		for (final TourPhotoLink tourPhotoLink : tourPhotoLinksWithGps) {

			// set tour gps into photos
			setTourGPSIntoPhotos_10(tourPhotoLink, updatedPhotos);

			/*
			 * update number of photos
			 */
			tourPhotoLink.numberOfGPSPhotos = 0;
			tourPhotoLink.numberOfNoGPSPhotos = 0;

			for (final PhotoWrapper photoWrapper : tourPhotoLink.tourPhotos) {

				final Photo photo = photoWrapper.photo;

				// set number of GPS/No GPS photos
				final double latitude = photo.getLatitude();
				if (latitude == Double.MIN_VALUE) {
					tourPhotoLink.numberOfNoGPSPhotos++;
				} else {
					tourPhotoLink.numberOfGPSPhotos++;
				}
			}
		}

//		Photo.updateExifGeoPosition(updatedPhotos);
	}

	private void setTourGPSIntoPhotos_10(final TourPhotoLink tourPhotoLink, final ArrayList<PhotoWrapper> updatedPhotos) {

		final ArrayList<PhotoWrapper> allPhotoWrapper = tourPhotoLink.tourPhotos;

		final int numberOfPhotos = allPhotoWrapper.size();
		if (numberOfPhotos == 0) {
			// no photos are available for this tour
			return;
		}

		final TourData tourData = TourManager.getInstance().getTourData(tourPhotoLink.tourId);

		final double[] latitudeSerie = tourData.latitudeSerie;
		final double[] longitudeSerie = tourData.longitudeSerie;

		if (latitudeSerie == null) {
			// no geo positions
			return;
		}

		final int[] timeSerie = tourData.timeSerie;
		final int numberOfTimeSlices = timeSerie.length;

		final long tourStart = tourData.getTourStartTime().getMillis() / 1000;
		long timeSliceEnd = tourStart + (long) (timeSerie[1] / 2.0);

		/*
		 * get hashset for existing photos
		 */
		final Set<TourPhoto> tourPhotosSet = tourData.getTourPhotos();
		final HashMap<String, TourPhoto> tourPhotosMap = new HashMap<String, TourPhoto>();
		for (final TourPhoto tourPhoto : tourPhotosSet) {
			tourPhotosMap.put(tourPhoto.getImageFilePathName(), tourPhoto);
		}

//		if (_isAddPhotosToExistingTourPhotos == false) {
//
		// previous photos will be replaced
		tourPhotosSet.clear();
//		}

		int timeIndex = 0;
		int photoIndex = 0;

		// get first photo
		PhotoWrapper photoWrapper = allPhotoWrapper.get(photoIndex);

		// loop: time serie
		while (true) {

			// loop: photo serie, check if a photo is in the current time slice
			while (true) {

				final long imageAdjustedTime = photoWrapper.adjustedTime;
				long imageTime = 0;

				if (imageAdjustedTime != Long.MIN_VALUE) {
					imageTime = imageAdjustedTime;
				} else {
					imageTime = photoWrapper.imageExifTime;
				}

				final long photoTime = imageTime / 1000;

				if (photoTime <= timeSliceEnd) {

					// photo is contained within the current time slice

					final double tourLatitude = latitudeSerie[timeIndex];
					final double tourLongitude = longitudeSerie[timeIndex];

					setTourGPSIntoPhotos_20(
							tourData,
							tourPhotosSet,
							updatedPhotos,
							photoWrapper,
							tourLatitude,
							tourLongitude);

					photoIndex++;

				} else {

					// advance to the next time slice

					break;
				}

				if (photoIndex < numberOfPhotos) {
					photoWrapper = allPhotoWrapper.get(photoIndex);
				} else {
					break;
				}
			}

			if (photoIndex >= numberOfPhotos) {
				// no more photos
				break;
			}

			/*
			 * photos are still available
			 */

			// advance to the next time slice on the x-axis
			timeIndex++;

			if (timeIndex >= numberOfTimeSlices - 1) {

				/*
				 * end of tour is reached but there are still photos available, set remaining photos
				 * at the end of the tour
				 */

				while (true) {

					final double tourLatitude = latitudeSerie[timeIndex];
					final double tourLongitude = longitudeSerie[timeIndex];

					setTourGPSIntoPhotos_20(
							tourData,
							tourPhotosSet,
							updatedPhotos,
							photoWrapper,
							tourLatitude,
							tourLongitude);

					photoIndex++;

					if (photoIndex < numberOfPhotos) {
						photoWrapper = allPhotoWrapper.get(photoIndex);
					} else {
						break;
					}
				}

			} else {

				final long valuePointTime = timeSerie[timeIndex];
				final long sliceDuration = timeSerie[timeIndex + 1] - valuePointTime;

				timeSliceEnd = tourStart + valuePointTime + (sliceDuration / 2);
			}
		}
	}

	private void setTourGPSIntoPhotos_20(	final TourData tourData,
											final Set<TourPhoto> tourPhotosSet,
											final ArrayList<PhotoWrapper> updatedPhotos,
											final PhotoWrapper photoWrapper,
											final double tourLatitude,
											final double tourLongitude) {

		final TourPhoto tourPhoto = new TourPhoto(tourData, photoWrapper.imageFile, photoWrapper.imageExifTime);


		final Photo photo = photoWrapper.photo;

		if (photoWrapper.isGeoFromExif /* && _isOverwritePhotoGPS == false */) {

			// photo contains already EXIF GPS

			// don't overwrite geo from EXIF, use GPS geo from photo wrapper

			tourPhoto.setGeoLocation(photo.getLatitude(), photo.getLongitude());

		} else {

			// set gps from tour into the photo

			tourPhoto.setGeoLocation(tourLatitude, tourLongitude);

			// update photo+photowrapper
//			photoWrapper.isGpsSetFromTour = true;
			photo.setTourGeoPosition(tourLatitude, tourLongitude);

			updatedPhotos.add(photoWrapper);
		}

		tourPhotosSet.add(tourPhoto);
	}

}
