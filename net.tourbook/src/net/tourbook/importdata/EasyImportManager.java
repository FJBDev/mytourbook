/*******************************************************************************
 * Copyright (C) 2005, 2015 Wolfgang Schramm and Contributors
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
package net.tourbook.importdata;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.locks.ReentrantLock;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.NIO;
import net.tourbook.common.UI;
import net.tourbook.common.util.SQL;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.Util;
import net.tourbook.data.TourData;
import net.tourbook.data.TourType;
import net.tourbook.database.TourDatabase;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.XMLMemento;

public class EasyImportManager {

	private static final String			ID									= "net.tourbook.importdata.EasyImportManager";	//$NON-NLS-1$
	//
	private static final String			XML_STATE_EASY_IMPORT_CONFIG		= "XML_STATE_EASY_IMPORT_CONFIG";				//$NON-NLS-1$
	//
	private static final String			TAG_IMPORT_CONFIG					= "Config";									//$NON-NLS-1$
	private static final String			TAG_IMPORT_CONFIG_ROOT				= "DeviceImportConfig";						//$NON-NLS-1$
	private static final String			TAG_SPEED_VERTEX					= "Speed";										//$NON-NLS-1$
	//
	private static final String			ATTR_AVG_SPEED						= "avgSpeed";									//$NON-NLS-1$
	private static final String			ATTR_BACKUP_FOLDER					= "backupFolder";								//$NON-NLS-1$
	private static final String			ATTR_DEVICE_FOLDER					= "deviceFolder";								//$NON-NLS-1$
	private static final String			ATTR_IS_CREATE_BACKUP				= "isCreateBackup";							//$NON-NLS-1$
	private static final String			ATTR_IS_LAST_LAUNCHER_REMOVED		= "isLastLauncherRemoved";						//$NON-NLS-1$
	private static final String			ATTR_TOUR_TYPE_CONFIG				= "tourTypeConfig";							//$NON-NLS-1$
	private static final String			ATTR_TOUR_TYPE_ID					= "tourTypeId";								//$NON-NLS-1$
	//
	private static final String			ATTR_DASH_BACKGROUND_OPACITY		= "backgroundOpacity";							//$NON-NLS-1$
	private static final String			ATTR_DASH_ANIMATION_CRAZY_FACTOR	= "animationCrazyFactor";						//$NON-NLS-1$
	private static final String			ATTR_DASH_ANIMATION_DURATION		= "animationDuration";							//$NON-NLS-1$
	private static final String			ATTR_DASH_IS_LIVE_UPDATE			= "isLiveUpdate";								//$NON-NLS-1$
	private static final String			ATTR_DASH_NUM_UI_COLUMNS			= "uiColumns";									//$NON-NLS-1$
	private static final String			ATTR_DASH_TILE_SIZE					= "tileSize";									//$NON-NLS-1$
	//
	private static final String			ATTR_IL_DESCRIPTION					= "description";								//$NON-NLS-1$
	private static final String			ATTR_IL_NAME						= "name";										//$NON-NLS-1$
	private static final String			ATTR_IL_IS_SAVE_TOUR				= "isSaveTour";								//$NON-NLS-1$
	private static final String			ATTR_IL_IS_SHOW_IN_DASHBOARD		= "isShowInDashBoard";							//$NON-NLS-1$
	private static final String			ATTR_IL_IS_SET_LAST_MARKER			= "isSetLastMarker";							//$NON-NLS-1$
	private static final String			ATTR_IL_LAST_MARKER_TEXT			= "lastMarkerText";							//$NON-NLS-1$
	private static final String			ATTR_IL_LAST_MARKER_DISTANCE		= "lastMarkerDistance";						//$NON-NLS-1$
	//
	private static EasyImportManager	_instance;

	private final IDialogSettings		_state								= TourbookPlugin.getState(ID);

	private ImportConfig				_importConfig;

	private String						_fileStoresHash;

	private ReentrantLock				STORE_LOCK							= new ReentrantLock();

	public static EasyImportManager getInstance() {

		if (_instance == null) {
			_instance = new EasyImportManager();
		}

		return _instance;
	}

	/**
	 * @param isForceRetrieveFiles
	 *            When <code>true</code> files will be retrieved even when the stores have not
	 *            changed.
	 * @return Returns <code>true</code> when import files have been retrieved, otherwise
	 *         <code>false</code>.
	 *         <p>
	 *         {@link ImportConfig#notImportedFiles} contains the files which are available in the
	 *         device folder but not available in the tour database.
	 */
	public DeviceImportState checkImportedFiles(final boolean isForceRetrieveFiles) {

		final DeviceImportState returnState = new DeviceImportState();

		// this is called from multiple threads and propably cause problems
		STORE_LOCK.lock();
		{
			try {

				/*
				 * Create hashcode for all file stores
				 */
				final Iterable<FileStore> fileStores = NIO.getFileStores();
				final StringBuilder sb = new StringBuilder();

				for (final FileStore store : fileStores) {
					sb.append(store);
					sb.append(' ');
				}
				final String fileStoresHash = sb.toString();

				/*
				 * Check if stores has changed
				 */
				final boolean areTheSameStores = fileStoresHash.equals(_fileStoresHash);
				returnState.areTheSameStores = areTheSameStores;

				if (areTheSameStores && isForceRetrieveFiles == false) {

					returnState.areFilesRetrieved = false;

					return returnState;
				}

				/*
				 * Filestore has changed, a device was added/removed.
				 */
				_fileStoresHash = fileStoresHash;

				getImportFiles();

			} finally {
				STORE_LOCK.unlock();
			}
		}

		returnState.areFilesRetrieved = true;

		return returnState;
	}

	private HashSet<String> getBackupFiles(final String folder) {

		final HashSet<String> backupFiles = new HashSet<>();

		final Path validPath = getValidPath(folder);
		if (validPath == null) {
			return backupFiles;
		}

		try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(validPath)) {

			for (final Path path : directoryStream) {

				try {

					final BasicFileAttributeView fileAttributesView = Files.getFileAttributeView(
							path,
							BasicFileAttributeView.class);

					final BasicFileAttributes fileAttributes = fileAttributesView.readAttributes();

					// ignore not regular files
					if (fileAttributes.isRegularFile()) {

						backupFiles.add(path.getFileName().toString());
					}

				} catch (final Exception e) {
// this can occure too often
//					StatusUtil.log(e);
				}

			}

		} catch (final IOException ex) {
			StatusUtil.log(ex);
		}

		return backupFiles;
	}

	private HashSet<String> getDbFileNames(final List<OSFile> deviceFileNames) {

		final HashSet<String> dbFileNames = new HashSet<>();

		/*
		 * Create a IN list with all device file names which are searched in the db.
		 */
		final StringBuilder sb = new StringBuilder();

		for (int fileIndex = 0; fileIndex < deviceFileNames.size(); fileIndex++) {

			final OSFile deviceFile = deviceFileNames.get(fileIndex);
			final String fileName = deviceFile.fileName;

			if (fileIndex > 0) {
				sb.append(',');
			}

			sb.append('\'');

			// escape single quotes
			sb.append(fileName.replace("\'", "\\\'")); //$NON-NLS-1$ //$NON-NLS-2$

			sb.append('\'');
		}

		final String deviceFileNameINList = sb.toString();

		try (Connection conn = TourDatabase.getInstance().getConnection(); //
				Statement stmt = conn.createStatement()) {

			final String sqlQuery = ""// 													//$NON-NLS-1$
					+ "SELECT" //															//$NON-NLS-1$
					+ " TourImportFileName" //												//$NON-NLS-1$
					+ " FROM " + TourDatabase.TABLE_TOUR_DATA //							//$NON-NLS-1$
					+ (" WHERE TourImportFileName IN (" + deviceFileNameINList + ")") //	//$NON-NLS-1$ //$NON-NLS-2$
					+ " ORDER BY TourImportFileName"; //									//$NON-NLS-1$

			final ResultSet result = stmt.executeQuery(sqlQuery);

			while (result.next()) {

				final String dbFileName = result.getString(1);

				dbFileNames.add(dbFileName);
			}

		} catch (final SQLException e) {
			SQL.showException(e);
		}

		return dbFileNames;
	}

	public ImportConfig getDeviceImportConfig() {

		if (_importConfig == null) {
			_importConfig = loadImportConfig();
		}

		return _importConfig;
	}

	/**
	 */
	private void getImportFiles() {

		final ArrayList<OSFile> notImportedFiles = new ArrayList<>();
		final ArrayList<String> notBackedUpFiles = new ArrayList<>();

		final ImportConfig importConfig = getDeviceImportConfig();
		importConfig.notImportedFiles = notImportedFiles;
		importConfig.notBackedUpFiles = notBackedUpFiles;

		/*
		 * Get backup files
		 */
		HashSet<String> existingBackupFiles = null;
		if (importConfig.isCreateBackup) {

			existingBackupFiles = getBackupFiles(importConfig.getBackupOSFolder());
		}

		/*
		 * Get device files
		 */
		final List<OSFile> existingDeviceFiles = getOSFiles(importConfig.getDeviceOSFolder());
		importConfig.numDeviceFiles = existingDeviceFiles.size();

		/*
		 * Get files which are not yet backed up
		 */
		if (existingBackupFiles != null) {

			for (final OSFile deviceFile : existingDeviceFiles) {

				final String deviceFileName = deviceFile.fileName;

				if (existingBackupFiles.contains(deviceFileName) == false) {
					notBackedUpFiles.add(deviceFileName);
				}
			}
		}

		if (existingDeviceFiles.size() == 0) {
			// there is nothing to be imported
			return;
		}

		/*
		 * Get files which are not yet imported
		 */
		final HashSet<String> dbFileNames = getDbFileNames(existingDeviceFiles);

		for (final OSFile deviceFile : existingDeviceFiles) {

			if (dbFileNames.contains(deviceFile.fileName) == false) {
				notImportedFiles.add(deviceFile);
			}
		}

		// sort by filename
		Collections.sort(notImportedFiles, new Comparator<OSFile>() {
			@Override
			public int compare(final OSFile file1, final OSFile file2) {
				return file1.fileName.compareTo(file2.fileName);
			}
		});
	}

	private List<OSFile> getOSFiles(final String folder) {

		final List<OSFile> osFiles = new ArrayList<>();

		final Path validPath = getValidPath(folder);
		if (validPath == null) {
			return osFiles;
		}

		try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(validPath)) {

			for (final Path path : directoryStream) {

				try {

					final BasicFileAttributeView fileAttributesView = Files.getFileAttributeView(
							path,
							BasicFileAttributeView.class);

					final BasicFileAttributes fileAttributes = fileAttributesView.readAttributes();

					// ignore not regular files
					if (fileAttributes.isRegularFile()) {

						final OSFile deviceFile = new OSFile();

						deviceFile.path = path;
						deviceFile.fileName = path.getFileName().toString();
						deviceFile.size = fileAttributes.size();
						deviceFile.modifiedTime = fileAttributes.lastModifiedTime().toMillis();

						osFiles.add(deviceFile);
					}

				} catch (final Exception e) {
// this can occure too often
//					StatusUtil.log(e);
				}

			}

		} catch (final IOException ex) {
			StatusUtil.log(ex);
		}

		return osFiles;
	}

	/**
	 * @param osFolder
	 * @return Returns the device OS path or <code>null</code> when this folder is not valid.
	 */
	private Path getValidPath(final String osFolder) {

		if (osFolder != null && osFolder.trim().length() > 0) {

			try {

				final Path devicePath = Paths.get(osFolder);

				if (Files.exists(devicePath)) {
					return devicePath;
				}

			} catch (final Exception e) {}
		}

		return null;
	}

	private boolean isFolderValid(final String osFolder, final String invalidMessage, final String originalFolder) {

		boolean isFolderValid = false;

		String displayedFolder = null;

		if (osFolder != null && osFolder.trim().length() > 0) {

			displayedFolder = osFolder;

			// check file
			try {

				final Path deviceFolderPath = Paths.get(osFolder);
				if (Files.exists(deviceFolderPath)) {
					isFolderValid = true;
				}

			} catch (final Exception e) {
				// path can be invalid
			}

		} else {

			displayedFolder = originalFolder;
		}

		if (!isFolderValid) {

			MessageDialog.openError(
					Display.getDefault().getActiveShell(),
					Messages.Import_Data_Dialog_EazyImport_Title,
					NLS.bind(invalidMessage, displayedFolder));
		}

		return isFolderValid;
	}

	private ImportConfig loadImportConfig() {

		final ImportConfig importConfig = new ImportConfig();

		final String stateValue = Util.getStateString(_state, XML_STATE_EASY_IMPORT_CONFIG, null);

		if ((stateValue != null) && (stateValue.length() > 0)) {

			try {

				final Reader reader = new StringReader(stateValue);

				loadImportConfig_Data(XMLMemento.createReadRoot(reader), importConfig);

			} catch (final WorkbenchException e) {
				// ignore
			}
		}

		return importConfig;
	}

	private void loadImportConfig_Data(final XMLMemento xmlMemento, final ImportConfig importConfig) {

		importConfig.animationCrazinessFactor = Util.getXmlInteger(xmlMemento,//
				ATTR_DASH_ANIMATION_CRAZY_FACTOR,
				ImportConfig.ANIMATION_CRAZINESS_FACTOR_DEFAULT,
				ImportConfig.ANIMATION_CRAZINESS_FACTOR_MIN,
				ImportConfig.ANIMATION_CRAZINESS_FACTOR_MAX);

		importConfig.animationDuration = Util.getXmlInteger(xmlMemento,//
				ATTR_DASH_ANIMATION_DURATION,
				ImportConfig.ANIMATION_DURATION_DEFAULT,
				ImportConfig.ANIMATION_DURATION_MIN,
				ImportConfig.ANIMATION_DURATION_MAX);

		importConfig.backgroundOpacity = Util.getXmlInteger(xmlMemento,//
				ATTR_DASH_BACKGROUND_OPACITY,
				ImportConfig.BACKGROUND_OPACITY_DEFAULT,
				ImportConfig.BACKGROUND_OPACITY_MIN,
				ImportConfig.BACKGROUND_OPACITY_MAX);

		importConfig.numHorizontalTiles = Util.getXmlInteger(xmlMemento,//
				ATTR_DASH_NUM_UI_COLUMNS,
				ImportConfig.HORIZONTAL_TILES_DEFAULT,
				ImportConfig.HORIZONTAL_TILES_MIN,
				ImportConfig.HORIZONTAL_TILES_MAX);

		importConfig.tileSize = Util.getXmlInteger(xmlMemento,//
				ATTR_DASH_TILE_SIZE,
				ImportConfig.TILE_SIZE_DEFAULT,
				ImportConfig.TILE_SIZE_MIN,
				ImportConfig.TILE_SIZE_MAX);

		importConfig.isCreateBackup = Util.getXmlBoolean(xmlMemento, ATTR_IS_CREATE_BACKUP, true);
		importConfig.isLastLauncherRemoved = Util.getXmlBoolean(xmlMemento, ATTR_IS_LAST_LAUNCHER_REMOVED, false);
		importConfig.isLiveUpdate = Util.getXmlBoolean(xmlMemento, ATTR_DASH_IS_LIVE_UPDATE, true);
		importConfig.setBackupFolder(Util.getXmlString(xmlMemento, ATTR_BACKUP_FOLDER, UI.EMPTY_STRING));
		importConfig.setDeviceFolder(Util.getXmlString(xmlMemento, ATTR_DEVICE_FOLDER, UI.EMPTY_STRING));

		for (final IMemento xmlConfig : xmlMemento.getChildren()) {

			final ImportLauncher importLauncher = new ImportLauncher();

			importLauncher.name = Util.getXmlString(xmlConfig, ATTR_IL_NAME, UI.EMPTY_STRING);
			importLauncher.description = Util.getXmlString(xmlConfig, ATTR_IL_DESCRIPTION, UI.EMPTY_STRING);
			importLauncher.isSaveTour = Util.getXmlBoolean(xmlConfig, ATTR_IL_IS_SAVE_TOUR, false);
			importLauncher.isShowInDashboard = Util.getXmlBoolean(xmlConfig, ATTR_IL_IS_SHOW_IN_DASHBOARD, true);

			// last marker
			importLauncher.isSetLastMarker = Util.getXmlBoolean(xmlConfig, ATTR_IL_IS_SET_LAST_MARKER, false);
			importLauncher.lastMarkerText = Util.getXmlString(xmlConfig, ATTR_IL_LAST_MARKER_TEXT, UI.EMPTY_STRING);
			importLauncher.lastMarkerDistance = Util.getXmlInteger(
					xmlConfig,
					ATTR_IL_LAST_MARKER_DISTANCE,
					ImportConfig.LAST_MARKER_DISTANCE_DEFAULT,
					ImportConfig.LAST_MARKER_DISTANCE_MIN,
					ImportConfig.LAST_MARKER_DISTANCE_MAX);

			final Enum<TourTypeConfig> ttConfig = Util.getXmlEnum(
					xmlConfig,
					ATTR_TOUR_TYPE_CONFIG,
					TourTypeConfig.TOUR_TYPE_CONFIG_ONE_FOR_ALL);

			importLauncher.tourTypeConfig = ttConfig;

			if (TourTypeConfig.TOUR_TYPE_CONFIG_BY_SPEED.equals(ttConfig)) {

				final ArrayList<SpeedTourType> speedVertices = importLauncher.speedTourTypes;

				for (final IMemento memento : xmlConfig.getChildren()) {

					if (memento instanceof XMLMemento) {

						final XMLMemento xmlSpeed = (XMLMemento) memento;

						final Long xmlTourTypeId = Util.getXmlLong(xmlSpeed, ATTR_TOUR_TYPE_ID, null);

						/*
						 * Check if the loaded tour type id is valid
						 */
						final TourType tourType = TourDatabase.getTourType(xmlTourTypeId);

						if (tourType != null) {

							final SpeedTourType speedVertex = new SpeedTourType();

							speedVertex.tourTypeId = xmlTourTypeId;

							speedVertex.avgSpeed = Util.getXmlFloatFloat(
									xmlSpeed,
									ATTR_AVG_SPEED,
									ImportConfig.TOUR_TYPE_AVG_SPEED_DEFAULT,
									ImportConfig.TOUR_TYPE_AVG_SPEED_MIN,
									ImportConfig.TOUR_TYPE_AVG_SPEED_MAX);

							speedVertices.add(speedVertex);
						}
					}
				}

			} else if (TourTypeConfig.TOUR_TYPE_CONFIG_ONE_FOR_ALL.equals(ttConfig)) {

				final Long xmlTourTypeId = Util.getXmlLong(xmlConfig, ATTR_TOUR_TYPE_ID, null);

				importLauncher.oneTourType = TourDatabase.getTourType(xmlTourTypeId);

			} else {

				// this is the default, tour type is not set

			}

			importLauncher.setupItemImage();

			importConfig.importLaunchers.add(importLauncher);
		}
	}

	/**
	 * Reset stored values.
	 */
	public void reset() {

		// force that it will be reloaded
		_fileStoresHash = null;
	}

	public ImportDeviceState runImport(final ImportLauncher importLauncher) {

		final ImportDeviceState importState = new ImportDeviceState();

		final ImportConfig importConfig = getDeviceImportConfig();

		/*
		 * Check device folder
		 */
		final String deviceOSFolder = importConfig.getDeviceOSFolder();

		if (!isFolderValid(
				deviceOSFolder,
				Messages.Import_Data_Dialog_EazyImport_InvalidDeviceFolder_Message,
				importConfig.getDeviceFolder())) {

			importState.isOpenSetup = true;

			return importState;
		}

		/*
		 * Check backup folder
		 */
		if (importConfig.isCreateBackup) {

			final String backupOSFolder = importConfig.getBackupOSFolder();

			if (!isFolderValid(

					backupOSFolder,
					Messages.Import_Data_Dialog_EazyImport_InvalidBackupFolder_Message,
					importConfig.getBackupFolder())) {

				importState.isOpenSetup = true;

				return importState;
			}

			// folder is valid, run the backup
			final boolean isCanceled = runImport_Backup();
			if (isCanceled) {
				return importState;
			}
		}

		/*
		 * Check import files
		 */
		final ArrayList<OSFile> notImportedPaths = importConfig.notImportedFiles;
		if (notImportedPaths.size() == 0) {

			MessageDialog.openInformation(
					Display.getDefault().getActiveShell(),
					Messages.Import_Data_Dialog_EazyImport_Title,
					NLS.bind(Messages.Import_Data_Dialog_EazyImport_NoImportFiles_Message, deviceOSFolder));

			// there is nothing more to do
			importState.isImportCanceled = true;

			return importState;
		}

		/*
		 * Get not imported files
		 */
		final ArrayList<String> notImportedFileNames = new ArrayList<>();

		for (final OSFile deviceFile : notImportedPaths) {
			notImportedFileNames.add(deviceFile.fileName);
		}

		/*
		 * Import files
		 */
		final String firstFilePathName = notImportedPaths.get(0).path.toString();
		final String[] fileNames = notImportedFileNames.toArray(new String[notImportedFileNames.size()]);

		final ImportRunState importRunState = RawDataManager.getInstance().doTheImport(firstFilePathName, fileNames);

		importState.isImportCanceled = importRunState.isImportCanceled;

		/*
		 * Update tour data.
		 */
		runImport_SetPathAndTourType(importLauncher, importState);

		return importState;
	}

	/**
	 * @return Returns <code>true</code> when the backup is canceled.
	 */
	private boolean runImport_Backup() {

		final ImportConfig importConfig = getDeviceImportConfig();

		final String deviceOSFolder = importConfig.getDeviceOSFolder();
		final String backupOSFolder = importConfig.getBackupOSFolder();

		final Path backupPath = Paths.get(backupOSFolder);

		final ArrayList<String> notBackedUpFiles = importConfig.notBackedUpFiles;
		final int numBackupFiles = notBackedUpFiles.size();

		if (numBackupFiles == 0) {
			return false;
		}

		final boolean isCanceled[] = { false };

		final IRunnableWithProgress importRunnable = new IRunnableWithProgress() {

			@Override
			public void run(final IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {

				int copied = 0;

				monitor.beginTask(Messages.Import_Data_Monitor_Backup, numBackupFiles);

				for (final String backupFileName : notBackedUpFiles) {

					if (monitor.isCanceled()) {
						// stop this task
						isCanceled[0] = true;
						break;
					}

					// for debugging
//					Thread.sleep(800);

					monitor.worked(1);
					monitor.subTask(NLS.bind(Messages.Import_Data_Monitor_Backup_SubTask, //
							new Object[] { ++copied, numBackupFiles, backupFileName }));

					try {

						final Path devicePath = Paths.get(deviceOSFolder, backupFileName);

						Files.copy(devicePath, backupPath.resolve(backupFileName));

					} catch (final IOException e) {
						StatusUtil.log(e);
					}
				}
			}
		};

		try {
			new ProgressMonitorDialog(Display.getDefault().getActiveShell()).run(true, true, importRunnable);
		} catch (final Exception e) {
			StatusUtil.log(e);
		}

		return isCanceled[0];
	}

	private void runImport_SetPathAndTourType(final ImportLauncher importLauncher, final ImportDeviceState importState) {

		final HashMap<Long, TourData> importedTours = RawDataManager.getInstance().getImportedTours();

		if (importedTours.size() == 0) {
			// nothing is imported
			return;
		}

		final ImportConfig importConfig = getDeviceImportConfig();
		final String backupOSFolder = importConfig.getBackupOSFolder();

		for (final Entry<Long, TourData> entry : importedTours.entrySet()) {

			final TourData tourData = entry.getValue();

			setTourType(tourData, importLauncher);

			if (importConfig.isCreateBackup) {

				// use backup folder as import folder and not the device folder

				// set backup file path
				tourData.setImportBackupFileFolder(backupOSFolder);
			}

			importState.isUpdateImportViewer = true;
		}
	}

	public void saveImportConfig(final ImportConfig importConfig) {

		// Build the XML block for writing the bindings and active scheme.
		final XMLMemento xmlMemento = XMLMemento.createWriteRoot(TAG_IMPORT_CONFIG_ROOT);

		saveImportConfig_Data(xmlMemento, importConfig);

		// Write the XML block to the state store.
		final Writer writer = new StringWriter();
		try {

			xmlMemento.save(writer);
			_state.put(XML_STATE_EASY_IMPORT_CONFIG, writer.toString());

		} catch (final IOException e) {

			StatusUtil.log(e);

		} finally {

			try {
				writer.close();
			} catch (final IOException e) {
				StatusUtil.log(e);
			}
		}
	}

	private void saveImportConfig_Data(final XMLMemento xmlMemento, final ImportConfig importConfig) {

		xmlMemento.putInteger(ATTR_DASH_ANIMATION_CRAZY_FACTOR, importConfig.animationCrazinessFactor);
		xmlMemento.putInteger(ATTR_DASH_ANIMATION_DURATION, importConfig.animationDuration);
		xmlMemento.putInteger(ATTR_DASH_BACKGROUND_OPACITY, importConfig.backgroundOpacity);
		xmlMemento.putInteger(ATTR_DASH_NUM_UI_COLUMNS, importConfig.numHorizontalTiles);
		xmlMemento.putInteger(ATTR_DASH_TILE_SIZE, importConfig.tileSize);

		xmlMemento.putBoolean(ATTR_IS_CREATE_BACKUP, importConfig.isCreateBackup);
		xmlMemento.putBoolean(ATTR_IS_LAST_LAUNCHER_REMOVED, importConfig.isLastLauncherRemoved);
		xmlMemento.putBoolean(ATTR_DASH_IS_LIVE_UPDATE, importConfig.isLiveUpdate);
		xmlMemento.putString(ATTR_BACKUP_FOLDER, importConfig.getBackupFolder());
		xmlMemento.putString(ATTR_DEVICE_FOLDER, importConfig.getDeviceFolder());

		for (final ImportLauncher importLauncher : importConfig.importLaunchers) {

			final IMemento xmlConfig = xmlMemento.createChild(TAG_IMPORT_CONFIG);

			xmlConfig.putString(ATTR_IL_NAME, importLauncher.name);
			xmlConfig.putString(ATTR_IL_DESCRIPTION, importLauncher.description);
			xmlConfig.putBoolean(ATTR_IL_IS_SAVE_TOUR, importLauncher.isSaveTour);
			xmlConfig.putBoolean(ATTR_IL_IS_SHOW_IN_DASHBOARD, importLauncher.isShowInDashboard);

			// last marker
			xmlConfig.putBoolean(ATTR_IL_IS_SET_LAST_MARKER, importLauncher.isSetLastMarker);
			xmlConfig.putString(ATTR_IL_LAST_MARKER_TEXT, importLauncher.lastMarkerText);
			xmlConfig.putInteger(ATTR_IL_LAST_MARKER_DISTANCE, importLauncher.lastMarkerDistance);

			final Enum<TourTypeConfig> ttConfig = importLauncher.tourTypeConfig;
			Util.setXmlEnum(xmlConfig, ATTR_TOUR_TYPE_CONFIG, ttConfig);

			if (TourTypeConfig.TOUR_TYPE_CONFIG_BY_SPEED.equals(ttConfig)) {

				for (final SpeedTourType speedVertex : importLauncher.speedTourTypes) {

					final IMemento memento = xmlConfig.createChild(TAG_SPEED_VERTEX);

					if (memento instanceof XMLMemento) {

						final XMLMemento xmlSpeedVertex = (XMLMemento) memento;

						Util.setXmlLong(xmlSpeedVertex, ATTR_TOUR_TYPE_ID, speedVertex.tourTypeId);
						xmlSpeedVertex.putFloat(ATTR_AVG_SPEED, speedVertex.avgSpeed);
					}
				}

			} else if (TourTypeConfig.TOUR_TYPE_CONFIG_ONE_FOR_ALL.equals(ttConfig)) {

				final TourType oneTourType = importLauncher.oneTourType;

				if (oneTourType != null) {
					Util.setXmlLong(xmlConfig, ATTR_TOUR_TYPE_ID, oneTourType.getTypeId());
				}

			} else {

				// this is the default, a tour type is not set
			}
		}
	}

	/**
	 * Set tour type by speed
	 * 
	 * @param tourData
	 * @param importLauncher
	 */
	private void setTourType(final TourData tourData, final ImportLauncher importLauncher) {

		final Enum<TourTypeConfig> ttConfig = importLauncher.tourTypeConfig;

		if (TourTypeConfig.TOUR_TYPE_CONFIG_BY_SPEED.equals(ttConfig)) {

			// set tour type by speed

			final float tourDistanceKm = tourData.getTourDistance();
			final long drivingTime = tourData.getTourDrivingTime();

			double tourAvgSpeed = 0;

			if (drivingTime != 0) {
				tourAvgSpeed = tourDistanceKm / drivingTime * 3.6;
			}

			final ArrayList<SpeedTourType> speedTourTypes = importLauncher.speedTourTypes;
			long tourTypeId = -1;

			// find tour type for the tour avg speed
			for (final SpeedTourType speedTourType : speedTourTypes) {

				if (tourAvgSpeed <= speedTourType.avgSpeed) {

					tourTypeId = speedTourType.tourTypeId;
					break;
				}
			}

			if (tourTypeId == -1) {

				// tour avg speed is above the last speed tour type -> use the last

				final int numTourTypes = speedTourTypes.size();

				if (numTourTypes > 0) {
					tourTypeId = speedTourTypes.get(numTourTypes - 1).tourTypeId;
				}
			}

			if (tourTypeId != -1) {

				final TourType tourType = net.tourbook.ui.UI.getTourType(tourTypeId);

				tourData.setTourType(tourType);
			}

		} else if (TourTypeConfig.TOUR_TYPE_CONFIG_ONE_FOR_ALL.equals(ttConfig)) {

			// set one tour type

			tourData.setTourType(importLauncher.oneTourType);

		} else {

			// tour type is not set

		}
	}
}
