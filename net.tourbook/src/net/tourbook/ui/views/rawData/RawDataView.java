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
package net.tourbook.ui.views.rawData;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static net.tourbook.ui.UI.getIconUrl;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Calendar;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.action.ActionOpenPrefDialog;
import net.tourbook.common.util.ColumnDefinition;
import net.tourbook.common.util.ColumnManager;
import net.tourbook.common.util.ITourViewer3;
import net.tourbook.common.util.PostSelectionProvider;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.Util;
import net.tourbook.data.TourData;
import net.tourbook.data.TourMarker;
import net.tourbook.data.TourPerson;
import net.tourbook.data.TourTag;
import net.tourbook.data.TourType;
import net.tourbook.data.TourWayPoint;
import net.tourbook.database.TourDatabase;
import net.tourbook.extension.export.ActionExport;
import net.tourbook.importdata.DeviceImportState;
import net.tourbook.importdata.DialogEasyImportConfig;
import net.tourbook.importdata.EasyConfig;
import net.tourbook.importdata.EasyImportManager;
import net.tourbook.importdata.ImportConfig;
import net.tourbook.importdata.ImportDeviceState;
import net.tourbook.importdata.ImportLauncher;
import net.tourbook.importdata.OSFile;
import net.tourbook.importdata.RawDataManager;
import net.tourbook.importdata.SpeedTourType;
import net.tourbook.importdata.TourTypeConfig;
import net.tourbook.photo.ImageUtils;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.preferences.PrefPageImport;
import net.tourbook.tag.TagMenuManager;
import net.tourbook.tour.ActionOpenAdjustAltitudeDialog;
import net.tourbook.tour.ActionOpenMarkerDialog;
import net.tourbook.tour.ITourEventListener;
import net.tourbook.tour.ITourItem;
import net.tourbook.tour.SelectionDeletedTours;
import net.tourbook.tour.SelectionTourData;
import net.tourbook.tour.SelectionTourIds;
import net.tourbook.tour.TourDoubleClickState;
import net.tourbook.tour.TourEvent;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourManager;
import net.tourbook.tour.TourTypeMenuManager;
import net.tourbook.ui.ITourProviderAll;
import net.tourbook.ui.TableColumnFactory;
import net.tourbook.ui.action.ActionEditQuick;
import net.tourbook.ui.action.ActionEditTour;
import net.tourbook.ui.action.ActionJoinTours;
import net.tourbook.ui.action.ActionModifyColumns;
import net.tourbook.ui.action.ActionOpenTour;
import net.tourbook.ui.action.ActionSetTourTypeMenu;
import net.tourbook.ui.views.TableViewerTourInfoToolTip;
import net.tourbook.ui.views.TourInfoToolTipCellLabelProvider;
import net.tourbook.web.WEB;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.DeviceResourceException;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.BrowserFunction;
import org.eclipse.swt.browser.LocationAdapter;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.browser.ProgressAdapter;
import org.eclipse.swt.browser.ProgressEvent;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.MenuAdapter;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.part.PageBook;
import org.eclipse.ui.part.ViewPart;
import org.joda.time.DateTime;

/**
 *
 */
public class RawDataView extends ViewPart implements ITourProviderAll, ITourViewer3 {

	public static final String				ID											= "net.tourbook.views.rawData.RawDataView"; //$NON-NLS-1$
	//
	private static final String				HTML_TD										= "<td>";									//$NON-NLS-1$
	private static final String				HTML_TD_SPACE								= "<td ";									//$NON-NLS-1$
	private static final String				HTML_TD_END									= "</td>";									//$NON-NLS-1$
	private static final String				HTML_TR										= "<tr>";									//$NON-NLS-1$
	private static final String				HTML_TR_END									= "</tr>";									//$NON-NLS-1$
	//
	private static final String				JS_FUNCTION_ON_SELECT_IMPORT_CONFIG			= "onSelectImportConfig";					//$NON-NLS-1$
	//
	private static final String				WEB_RESOURCE_TITLE_FONT						= "Nunito-Bold.ttf";						//$NON-NLS-1$
//	private static final String				WEB_RESOURCE_TITLE_FONT						= "NothingYouCouldDo.ttf";					//$NON-NLS-1$
	private static final String				WEB_RESOURCE_TOUR_IMPORT_BG_IMAGE			= "mytourbook-icon.svg";					//$NON-NLS-1$
	private static final String				WEB_RESOURCE_TOUR_IMPORT_CSS				= "tour-import.css";						//$NON-NLS-1$
	private static final String				WEB_RESOURCE_TOUR_IMPORT_CSS3				= "tour-import-css3.css";					//$NON-NLS-1$
	//
	private static final String				CSS_IMPORT_BACKGROUND						= "div.import-background";					//$NON-NLS-1$
	private static final String				CSS_IMPORT_TILE								= "a.import-tile";							//$NON-NLS-1$
	//
	public static final int					COLUMN_DATE									= 0;
	public static final int					COLUMN_TITLE								= 1;
	public static final int					COLUMN_DATA_FORMAT							= 2;
	public static final int					COLUMN_FILE_NAME							= 3;
	//
	private static final String				STATE_IMPORTED_FILENAMES					= "importedFilenames";						//$NON-NLS-1$
	private static final String				STATE_SELECTED_TOUR_INDICES					= "SelectedTourIndices";					//$NON-NLS-1$
	//
	public static final String				STATE_IS_CHECKSUM_VALIDATION				= "isChecksumValidation";					//$NON-NLS-1$
	public static final boolean				STATE_IS_CHECKSUM_VALIDATION_DEFAULT		= true;
	public static final String				STATE_IS_CONVERT_WAYPOINTS					= "STATE_IS_CONVERT_WAYPOINTS";			//$NON-NLS-1$
	public static final boolean				STATE_IS_CONVERT_WAYPOINTS_DEFAULT			= true;
	public static final String				STATE_IS_CREATE_TOUR_ID_WITH_TIME			= "isCreateTourIdWithTime";				//$NON-NLS-1$
	public static final boolean				STATE_IS_CREATE_TOUR_ID_WITH_TIME_DEFAULT	= false;
	private static final String				STATE_IS_NEW_UI								= "STATE_IS_NEW_UI";						//$NON-NLS-1$
	private static final String				STATE_IS_REMOVE_TOURS_WHEN_VIEW_CLOSED		= "STATE_IS_REMOVE_TOURS_WHEN_VIEW_CLOSED"; //$NON-NLS-1$
	public static final String				STATE_IS_MERGE_TRACKS						= "isMergeTracks";							//$NON-NLS-1$
	public static final boolean				STATE_IS_MERGE_TRACKS_DEFAULT				= false;
	//
	private static final String				HREF_TOKEN									= "#";										//$NON-NLS-1$
	private static final String				PAGE_ABOUT_BLANK							= "about:blank";							//$NON-NLS-1$

	/**
	 * This is necessary otherwise XULrunner in Linux do not fire a location change event.
	 */
	private static final String				HTTP_DUMMY									= "http://dummy";							//$NON-NLS-1$

	private static final String				HTML_STYLE_TITLE_VERTICAL_PADDING			= "style='padding-top:10px;'";				//$NON-NLS-1$

	private static String					ACTION_DEVICE_IMPORT						= "DeviceImport";							//$NON-NLS-1$
	private static String					ACTION_DEVICE_WATCHING_ON_OFF				= "DeviceOnOff";							//$NON-NLS-1$
	private static final String				ACTION_IMPORT_FROM_FILES					= "ImportFromFiles";						//$NON-NLS-1$
	private static final String				ACTION_OLD_UI								= "OldUI";									//$NON-NLS-1$
	private static final String				ACTION_SERIAL_PORT_CONFIGURED				= "SerialPortConfigured";					//$NON-NLS-1$
	private static final String				ACTION_SERIAL_PORT_DIRECTLY					= "SerialPortDirectly";					//$NON-NLS-1$
	private static final String				ACTION_SETUP_EASY_IMPORT					= "SetupEasyImport";						//$NON-NLS-1$
	//
	private static final String				DOM_CLASS_DEVICE_ON							= "deviceOn";								//$NON-NLS-1$
	private static final String				DOM_CLASS_DEVICE_OFF						= "deviceOff";								//$NON-NLS-1$
	private static final String				DOM_CLASS_DEVICE_ON_ANIMATED				= "deviceOnAnimated";						//$NON-NLS-1$
	private static final String				DOM_CLASS_DEVICE_OFF_ANIMATED				= "deviceOffAnimated";						//$NON-NLS-1$
	//
	private static final String				DOM_ID_DEVICE_ON_OFF						= "deviceOnOff";							//$NON-NLS-1$
	private static final String				DOM_ID_DEVICE_STATE							= "deviceState";							//$NON-NLS-1$
	private static final String				DOM_ID_IMPORT_CONFIG						= "importConfig";							//$NON-NLS-1$
	private static final String				DOM_ID_IMPORT_TILES							= "importTiles";							//$NON-NLS-1$
	//
	private static String					HREF_ACTION_DEVICE_IMPORT;
	private static String					HREF_ACTION_DEVICE_WATCHING_ON_OFF;
	private static String					HREF_ACTION_IMPORT_FROM_FILES;
	private static String					HREF_ACTION_OLD_UI;
	private static String					HREF_ACTION_SERIAL_PORT_CONFIGURED;
	private static String					HREF_ACTION_SERIAL_PORT_DIRECTLY;
	private static String					HREF_ACTION_SETUP_EASY_IMPORT;

	static {
		HREF_ACTION_DEVICE_IMPORT = HREF_TOKEN + ACTION_DEVICE_IMPORT;
		HREF_ACTION_DEVICE_WATCHING_ON_OFF = HREF_TOKEN + ACTION_DEVICE_WATCHING_ON_OFF;
		HREF_ACTION_IMPORT_FROM_FILES = HREF_TOKEN + ACTION_IMPORT_FROM_FILES;
		HREF_ACTION_OLD_UI = HREF_TOKEN + ACTION_OLD_UI;
		HREF_ACTION_SERIAL_PORT_CONFIGURED = HREF_TOKEN + ACTION_SERIAL_PORT_CONFIGURED;
		HREF_ACTION_SERIAL_PORT_DIRECTLY = HREF_TOKEN + ACTION_SERIAL_PORT_DIRECTLY;
		HREF_ACTION_SETUP_EASY_IMPORT = HREF_TOKEN + ACTION_SETUP_EASY_IMPORT + HREF_TOKEN;
	}

	//
	private final IPreferenceStore			_prefStore									= TourbookPlugin.getPrefStore();
	private final IDialogSettings			_state										= TourbookPlugin.getState(ID);
	//
	private RawDataManager					_rawDataMgr									= RawDataManager.getInstance();
	private TableViewer						_tourViewer;
	private TableViewerTourInfoToolTip		_tourInfoToolTip;
	//
	private PostSelectionProvider			_postSelectionProvider;
	private IPartListener2					_partListener;
	private ISelectionListener				_postSelectionListener;
	private IPropertyChangeListener			_prefChangeListener;
	private ITourEventListener				_tourEventListener;
	//
	// context menu actions
	private ActionClearView					_actionClearView;
	private ActionDeleteTourFiles			_actionDeleteTourFile;
	private ActionExport					_actionExportTour;
	private ActionEditQuick					_actionEditQuick;
	private ActionEditTour					_actionEditTour;
	private ActionJoinTours					_actionJoinTours;
	private ActionMergeIntoMenu				_actionMergeIntoTour;
	private ActionMergeTour					_actionMergeTour;
	private ActionModifyColumns				_actionModifyColumns;
	private ActionOpenTour					_actionOpenTour;
	private ActionOpenMarkerDialog			_actionOpenMarkerDialog;
	private ActionOpenAdjustAltitudeDialog	_actionOpenAdjustAltitudeDialog;
	private ActionOpenPrefDialog			_actionEditImportPreferences;
	private ActionReimportSubMenu			_actionReimportSubMenu;
	private ActionRemoveTour				_actionRemoveTour;
	private ActionRemoveToursWhenClosed		_actionRemoveToursWhenClosed;
	private ActionSaveTourInDatabase		_actionSaveTour;
	private ActionSaveTourInDatabase		_actionSaveTourWithPerson;
	private ActionSetupImport				_actionSetupImport;
	private ActionSetTourTypeMenu			_actionSetTourType;
	//
	protected TourPerson					_activePerson;
	protected TourPerson					_newActivePerson;
	//
	protected boolean						_isPartVisible								= false;
	protected boolean						_isViewerPersonDataDirty					= false;
	//
	private ColumnManager					_columnManager;
	//
	private final Calendar					_calendar									= GregorianCalendar
																								.getInstance();
	private final DateFormat				_dateFormatter								= DateFormat
																								.getDateInstance(DateFormat.SHORT);
	private final DateFormat				_timeFormatter								= DateFormat
																								.getTimeInstance(DateFormat.SHORT);
	private final DateFormat				_durationFormatter							= DateFormat.getTimeInstance(
																								DateFormat.SHORT,
																								Locale.GERMAN);
	private final NumberFormat				_nf1										= NumberFormat
																								.getNumberInstance();
	private final NumberFormat				_nf3										= NumberFormat
																								.getNumberInstance();
	{
		_nf1.setMinimumFractionDigits(1);
		_nf1.setMaximumFractionDigits(1);
		_nf3.setMinimumFractionDigits(3);
		_nf3.setMaximumFractionDigits(3);
	}
	//
	private boolean							_isToolTipInDate;
	private boolean							_isToolTipInTime;
	private boolean							_isToolTipInTitle;
	private boolean							_isToolTipInTags;
	//
	private TagMenuManager					_tagMenuMgr;
	private TourDoubleClickState			_tourDoubleClickState						= new TourDoubleClickState();
	//
	private Thread							_watchingStoresThread;
	private Thread							_watchingFolderThread;
	private WatchService					_folderWatcher;
	private boolean							_isStopWatchingStoresThread;
	private AtomicBoolean					_isWatchingStores							= new AtomicBoolean();
	private AtomicBoolean					_isDeviceStateUpdateDelayed					= new AtomicBoolean();
	private ReentrantLock					WATCH_LOCK									= new ReentrantLock();
	//
	private HashMap<Long, Image>			_configImages								= new HashMap<>();
	private HashMap<Long, Integer>			_configImageHash							= new HashMap<>();
	//
	private boolean							_isBrowserCompleted;
	private boolean							_isInUIStartup;
	private boolean							_isNewUI;

	/**
	 * When <code>false</code> then the background WatchStores task must set it valid. Only when it
	 * is valid then the device state icon displays the state, otherwise it shows a waiting icon.
	 */
	private boolean							_isDeviceStateValid;
	private boolean							_isRunDashboardAnimation					= true;
	private boolean							_isShowWatcherAnimation;
	private boolean							_isUpdateDeviceState						= true;
	//
	private String							_cssFonts;
	private String							_cssFromFile;
	//
	private String							_imageUrl_Device_TurnOff;
	private String							_imageUrl_Device_TurnOn;
	private String							_imageUrl_DeviceFolder_OK;
	private String							_imageUrl_DeviceFolder_Disabled;
	private String							_imageUrl_DeviceFolder_NotAvailable;
	private String							_imageUrl_DeviceFolder_NotChecked;
	private String							_imageUrl_DeviceFolder_NotSetup;
	private String							_imageUrl_ImportFromFile;
	private String							_imageUrl_SerialPort_Configured;
	private String							_imageUrl_SerialPort_Directly;
	private String							_imageUrl_State_Error;
	private String							_imageUrl_State_OK;
	private String							_imageUrl_State_DeleteFiles;
	private String							_imageUrl_State_SaveTour;
	private String							_imageUrl_State_TourMarker;
	//
	private PixelConverter					_pc;

	/*
	 * resources
	 */
	private ImageDescriptor					_imageDescDatabase;
	private ImageDescriptor					_imageDescDatabaseOtherPerson;
	private ImageDescriptor					_imageDescDatabaseAssignMergedTour;
	private ImageDescriptor					_imageDescDatabasePlaceholder;
	private ImageDescriptor					_imageDescDelete;
	//
	private Image							_imageDatabase;
	private Image							_imageDatabaseOtherPerson;
	private Image							_imageDatabaseAssignMergedTour;
	private Image							_imageDatabasePlaceholder;
	private Image							_imageDelete;

	private DialogEasyImportConfig			_dialogImportConfig;

	/*
	 * UI controls
	 */
	private PageBook						_topPageBook;
	private Composite						_topPage_Dashboard;
	private Composite						_topPage_ImportViewer;
	private Composite						_topPage_OldUI;
	private Composite						_topPage_Startup;

	private PageBook						_dashboard_PageBook;
	private Composite						_dashboardPage_NoBrowser;
	private Composite						_dashboardPage_WithBrowser;

	private Composite						_parent;
	private Text							_txtNoBrowser;
	private Link							_linkImport;

	private Browser							_browser;

	/*
	 * OLD UI
	 */
	public static final String				IMAGE_DATA_TRANSFER							= "IMAGE_DATA_TRANSFER";					//$NON-NLS-1$
	public static final String				IMAGE_DATA_TRANSFER_DIRECT					= "IMAGE_DATA_TRANSFER_DIRECT";			//$NON-NLS-1$
	public static final String				IMAGE_IMPORT								= "IMAGE_IMPORT";							//$NON-NLS-1$

	private class JS_OnSelectImportConfig extends BrowserFunction {

		JS_OnSelectImportConfig(final Browser browser, final String name) {
			super(browser, name);
		}

		@Override
		public Object function(final Object[] arguments) {

			final int selectedIndex = ((Number) arguments[0]).intValue();

			Display.getCurrent().asyncExec(new Runnable() {
				@Override
				public void run() {

					onSelectImportConfig(selectedIndex);
				}
			});

//// this can be used to show created JS in the debugger
//			if (true) {
//				throw new RuntimeException();
//			}

			return null;
		}
	}

	private class TourDataContentProvider implements IStructuredContentProvider {

		public TourDataContentProvider() {}

		@Override
		public void dispose() {}

		@Override
		public Object[] getElements(final Object parent) {
			return (Object[]) (parent);
		}

		@Override
		public void inputChanged(final Viewer v, final Object oldInput, final Object newInput) {}
	}

	private void action_Easy_RunImport(final long tileId) {

		final EasyConfig easyConfig = getEasyConfig();
		final ImportConfig importConfig = easyConfig.getActiveImportConfig();

		/*
		 * Get import launcher
		 */
		ImportLauncher importLauncher = null;

		for (final ImportLauncher launcher : easyConfig.importLaunchers) {
			if (launcher.getId() == tileId) {
				importLauncher = launcher;
				break;
			}
		}

		if (importLauncher == null) {
			// this should not occure
			return;
		}

		/*
		 * Check person
		 */
		TourPerson person = null;
		if (importLauncher.isSaveTour) {

			person = TourbookPlugin.getActivePerson();

			if (person == null) {

				MessageDialog.openError(
						_parent.getShell(),
						Messages.Import_Data_Dialog_EasyImport_Title,
						Messages.Import_Data_Dialog_NoActivePersion_Message);
				return;
			}
		}

		/*
		 * Run the import
		 */
		ImportDeviceState importState = null;

		try {

			// disable state update during import, this causes lots of problems !!!
			_isUpdateDeviceState = false;

			importState = EasyImportManager.getInstance().runImport(importLauncher);

		} finally {

			_isUpdateDeviceState = true;
		}

		/*
		 * Update viewer with newly imported files
		 */
		final Collection<TourData> importedToursCollection = RawDataManager.getInstance().getImportedTours().values();
		final ArrayList<TourData> importedTours = new ArrayList<>(importedToursCollection);

		if (importState.isUpdateImportViewer) {
			_tourViewer.update(importedToursCollection.toArray(), null);
		}

		// stop all other actions when canceled
		if (importState.isImportCanceled) {
			return;
		}

		// open import config dialog to solve problems
		if (importState.isOpenSetup) {

			_parent.getDisplay().asyncExec(new Runnable() {
				@Override
				public void run() {
					action_Easy_SetupImport(0);
				}
			});

			return;
		}

		/*
		 * 4. Set last marker text
		 */
		if (importLauncher.isSetLastMarker) {
			doSetLastMarker(importLauncher, importedTours);
		}

		/*
		 * 99. Save imported tours
		 */
		if (importLauncher.isSaveTour) {
			doSaveTour(person, importedTours);
		}

		/*
		 * 100. Delete device files
		 */
		if (importConfig.isDeleteDeviceFiles) {
			doDeleteTourFiles(false);
		}

		/*
		 * 101. Turn watching off
		 */
		if (importConfig.isTurnOffWatching) {
			setWatcher_Off();
		}

		thread_FolderWatcher_Activate();
		updateUI_DeviceState();
	}

	private void action_Easy_SetDeviceWatching_OnOff() {

		_isShowWatcherAnimation = true;

		if (isWatchingOn()) {

			// stop watching

			setWatcher_Off();

		} else {

			// start watching

			setWatcher_On();
		}

		updateUI_DeviceState();
	}

	/**
	 * @param selectedTab
	 *            Tab which should be selected when config dialog is opened, -1 will select the
	 *            restored tab index.
	 */
	void action_Easy_SetupImport(final int selectedTab) {

		// prevent that the dialog is opened multiple times, this occured when testing
		if (_dialogImportConfig != null) {
			return;
		}

		final Shell shell = Display.getDefault().getActiveShell();

		final EasyConfig easyConfig = getEasyConfig();

		_dialogImportConfig = new DialogEasyImportConfig(shell, easyConfig, this, selectedTab);

		boolean isOK = false;

		if (_dialogImportConfig.open() == Window.OK) {
			isOK = true;
		}

		if (isOK) {

			// keep none live update values

			final EasyConfig modifiedEasyConfig = _dialogImportConfig.getModifiedConfig();

			easyConfig.setActiveImportConfig(modifiedEasyConfig.getActiveImportConfig());

			easyConfig.importConfigs.clear();
			easyConfig.importConfigs.addAll(modifiedEasyConfig.importConfigs);

			easyConfig.importLaunchers.clear();
			easyConfig.importLaunchers.addAll(modifiedEasyConfig.importLaunchers);

			updateModel_EasyConfig_Dashboard(_dialogImportConfig.getModifiedConfig());

			_isDeviceStateValid = false;
			updateUI_2_Dashboard();
		}

		_dialogImportConfig = null;
	}

	void actionClearView() {

		// force the state icon to be updated
		_isDeviceStateValid = false;
		thread_FolderWatcher_Activate();

		// remove all tours
		_rawDataMgr.removeAllTours();

		reloadViewer();

		_postSelectionProvider.setSelection(new SelectionDeletedTours());

		// don't throw the selection again
		_postSelectionProvider.clearSelection();
	}

	void actionDeleteTourFiles() {

		doDeleteTourFiles(true);
	}

	void actionMergeTours(final TourData mergeFromTour, final TourData mergeIntoTour) {

		// check if the tour editor contains a modified tour
		if (TourManager.isTourEditorModified()) {
			return;
		}

		// backup data
		final Long backupMergeSourceTourId = mergeIntoTour.getMergeSourceTourId();
		final Long backupMergeTargetTourId = mergeIntoTour.getMergeTargetTourId();

		// set tour data and tour id from which the tour is merged
		mergeIntoTour.setMergeSourceTourId(mergeFromTour.getTourId());
		mergeIntoTour.setMergeTargetTourId(null);

		// set temp data, this is required by the dialog because the merge from tour could not be saved
		mergeIntoTour.setMergeSourceTour(mergeFromTour);

		if (new DialogMergeTours(Display.getCurrent().getActiveShell(), mergeFromTour, mergeIntoTour).open() != Window.OK) {

			// dialog is canceled, restore modified values

			mergeIntoTour.setMergeSourceTourId(backupMergeSourceTourId);
			mergeIntoTour.setMergeTargetTourId(backupMergeTargetTourId);
		}

		// reset temp tour data
		mergeIntoTour.setMergeSourceTour(null);
	}

	/**
	 * Remove all tours from the raw data view which are selected
	 */
	void actionRemoveTour() {

		final IStructuredSelection selection = ((IStructuredSelection) _tourViewer.getSelection());
		if (selection.size() == 0) {
			return;
		}

		/*
		 * convert selection to array
		 */
		final Object[] selectedItems = selection.toArray();
		final TourData[] selectedTours = new TourData[selection.size()];
		for (int i = 0; i < selectedItems.length; i++) {
			selectedTours[i] = (TourData) selectedItems[i];
		}

		_rawDataMgr.removeTours(selectedTours);

		_postSelectionProvider.clearSelection();

		TourManager.fireEvent(TourEventId.CLEAR_DISPLAYED_TOUR, null, RawDataView.this);

		// update the table viewer
		reloadViewer();
	}

	void actionSaveTour(final TourPerson person) {

		doSaveTour(person);
	}

	private void addPartListener() {
		_partListener = new IPartListener2() {

			@Override
			public void partActivated(final IWorkbenchPartReference partRef) {}

			@Override
			public void partBroughtToTop(final IWorkbenchPartReference partRef) {}

			@Override
			public void partClosed(final IWorkbenchPartReference partRef) {
				if (partRef.getPart(false) == RawDataView.this) {

					saveState();

					// remove all tours
					_rawDataMgr.removeAllTours();

					TourManager.fireEvent(TourEventId.CLEAR_DISPLAYED_TOUR, null, RawDataView.this);
				}
			}

			@Override
			public void partDeactivated(final IWorkbenchPartReference partRef) {}

			@Override
			public void partHidden(final IWorkbenchPartReference partRef) {
				if (RawDataView.this == partRef.getPart(false)) {
					_isPartVisible = false;
				}
			}

			@Override
			public void partInputChanged(final IWorkbenchPartReference partRef) {}

			@Override
			public void partOpened(final IWorkbenchPartReference partRef) {}

			@Override
			public void partVisible(final IWorkbenchPartReference partRef) {
				if (RawDataView.this == partRef.getPart(false)) {

					_isPartVisible = true;

					if (_isViewerPersonDataDirty || (_newActivePerson != _activePerson)) {

						reloadViewer();
						updateViewerPersonData();

						_newActivePerson = _activePerson;
						_isViewerPersonDataDirty = false;
					}
				}
			}
		};
		getViewSite().getPage().addPartListener(_partListener);
	}

	private void addPrefListener() {

		_prefChangeListener = new IPropertyChangeListener() {
			@Override
			public void propertyChange(final PropertyChangeEvent event) {

				final String property = event.getProperty();

				if (property.equals(ITourbookPreferences.APP_DATA_FILTER_IS_MODIFIED)) {
					if (_isPartVisible) {
						updateViewerPersonData();
					} else {
						// keep new active person until the view is visible
						_newActivePerson = TourbookPlugin.getActivePerson();
					}

				} else if (property.equals(ITourbookPreferences.TOUR_PERSON_LIST_IS_MODIFIED)) {

					_actionSaveTour.resetPeopleList();

				} else if (property.equals(ITourbookPreferences.TOUR_TYPE_LIST_IS_MODIFIED)) {

					// tour type images can have been changed
					disposeConfigImages();

					// update tour type in the raw data
					_rawDataMgr.updateTourData_InImportView_FromDb(null);

					_tourViewer.refresh();

				} else if (property.equals(ITourbookPreferences.VIEW_TOOLTIP_IS_MODIFIED)) {

					updateToolTipState();

				} else if (property.equals(ITourbookPreferences.MEASUREMENT_SYSTEM)) {

					// measurement system has changed

					_columnManager.saveState(_state);
					_columnManager.clearColumns();
					defineAllColumns();

					_tourViewer = (TableViewer) recreateViewer(_tourViewer);

					_isDeviceStateValid = false;
					updateUI_2_Dashboard();

				} else if (property.equals(ITourbookPreferences.VIEW_LAYOUT_CHANGED)) {

					_tourViewer.getTable().setLinesVisible(
							_prefStore.getBoolean(ITourbookPreferences.VIEW_LAYOUT_DISPLAY_LINES));

					_tourViewer.refresh();

					/*
					 * the tree must be redrawn because the styled text does not show with the new
					 * color
					 */
					_tourViewer.getTable().redraw();
				}
			}
		};

		_prefStore.addPropertyChangeListener(_prefChangeListener);
	}

	private void addSelectionListener() {

		_postSelectionListener = new ISelectionListener() {
			@Override
			public void selectionChanged(final IWorkbenchPart part, final ISelection selection) {

				if (part == RawDataView.this) {
					return;
				}

				onSelectionChanged(selection);
			}
		};
		getSite().getPage().addPostSelectionListener(_postSelectionListener);
	}

	private void addTourEventListener() {

		_tourEventListener = new ITourEventListener() {
			@Override
			public void tourChanged(final IWorkbenchPart part, final TourEventId eventId, final Object eventData) {

				if (part == RawDataView.this) {
					return;
				}

				if ((eventId == TourEventId.TOUR_CHANGED) && (eventData instanceof TourEvent)) {

					// update modified tours
					final ArrayList<TourData> modifiedTours = ((TourEvent) eventData).getModifiedTours();
					if (modifiedTours != null) {

						// update model
						_rawDataMgr.updateTourDataModel(modifiedTours);

						// update viewer
						_tourViewer.update(modifiedTours.toArray(), null);

						// remove old selection, old selection can have the same tour but with old data
						_postSelectionProvider.clearSelection();
					}

				} else if (eventId == TourEventId.ALL_TOURS_ARE_MODIFIED) {

					// save imported file names
					final HashSet<String> importedFiles = _rawDataMgr.getImportedFiles();
					_state.put(STATE_IMPORTED_FILENAMES, importedFiles.toArray(new String[importedFiles.size()]));

					reimportAllImportFiles(false);

				} else if (eventId == TourEventId.TAG_STRUCTURE_CHANGED) {

					_rawDataMgr.updateTourData_InImportView_FromDb(null);

					reloadViewer();
				}
			}
		};
		TourManager.getInstance().addTourEventListener(_tourEventListener);
	}

	private void createActions() {

		_actionEditImportPreferences = new ActionOpenPrefDialog(
				Messages.Import_Data_Action_EditImportPreferences,
				PrefPageImport.ID);

		_actionClearView = new ActionClearView(this);
		_actionDeleteTourFile = new ActionDeleteTourFiles(this);
		_actionEditTour = new ActionEditTour(this);
		_actionEditQuick = new ActionEditQuick(this);
		_actionExportTour = new ActionExport(this);
		_actionJoinTours = new ActionJoinTours(this);
		_actionMergeIntoTour = new ActionMergeIntoMenu(this);
		_actionMergeTour = new ActionMergeTour(this);
		_actionModifyColumns = new ActionModifyColumns(this);
		_actionOpenTour = new ActionOpenTour(this);
		_actionOpenMarkerDialog = new ActionOpenMarkerDialog(this, true);
		_actionOpenAdjustAltitudeDialog = new ActionOpenAdjustAltitudeDialog(this);
		_actionReimportSubMenu = new ActionReimportSubMenu(this);
		_actionRemoveTour = new ActionRemoveTour(this);
		_actionRemoveToursWhenClosed = new ActionRemoveToursWhenClosed();
		_actionSaveTour = new ActionSaveTourInDatabase(this, false);
		_actionSaveTourWithPerson = new ActionSaveTourInDatabase(this, true);
		_actionSetupImport = new ActionSetupImport(this);
		_actionSetTourType = new ActionSetTourTypeMenu(this);

		_tagMenuMgr = new TagMenuManager(this, true);
	}

	/**
	 * Create css from the import configurations.
	 * 
	 * @return
	 */
	private String createCSS_Custom() {

		/*
		 * Import background image
		 */
		final EasyConfig easyConfig = getEasyConfig();

		final int itemSize = easyConfig.tileSize;
		final int opacity = easyConfig.backgroundOpacity;
		final int animationDuration = easyConfig.animationDuration;
		final int crazyFactor = easyConfig.animationCrazinessFactor;

		String bgImage = UI.EMPTY_STRING;
		if (opacity > 0) {

			/*
			 * Show image only when it is visible, opacity > 0
			 */

			File webFile;
			try {

				webFile = WEB.getResourceFile(WEB_RESOURCE_TOUR_IMPORT_BG_IMAGE);
				final String webContent = Util.readContentFromFile(webFile.getAbsolutePath());
				final String base64Encoded = Base64.getEncoder().encodeToString(webContent.getBytes());

				bgImage = (CSS_IMPORT_BACKGROUND + "\n")// //$NON-NLS-1$
						+ "{\n" //$NON-NLS-1$
						+ ("	background:				url('data:image/svg+xml;base64," + base64Encoded + "');\n") //$NON-NLS-1$ //$NON-NLS-2$
						+ ("	background-repeat:		no-repeat;\n") //$NON-NLS-1$
						+ ("	background-size: 		contain;\n") //$NON-NLS-1$
						+ ("	background-position: 	center center;\n") //$NON-NLS-1$
						+ ("	opacity:				" + (float) opacity / 100 + ";\n") //$NON-NLS-1$ //$NON-NLS-2$
						+ "}\n"; //$NON-NLS-1$

			} catch (IOException | URISyntaxException e) {
				StatusUtil.log(e);
			}
		}

		String animation = UI.EMPTY_STRING;
		if (_isRunDashboardAnimation && animationDuration > 0 && UI.IS_WIN) {

			// run animation only once
			_isRunDashboardAnimation = false;

			final double rotateX = Math.random() * 10 * crazyFactor;
			final double rotateY = Math.random() * 10 * crazyFactor;

			animation = ""// //$NON-NLS-1$

					+ ("body\n") //$NON-NLS-1$
					+ ("{\n") //$NON-NLS-1$
					+ ("	animation:					fadeinBody;\n") //$NON-NLS-1$
					+ ("	animation-duration:			" + _nf1.format(animationDuration / 10.0) + "s;\n") //$NON-NLS-1$ //$NON-NLS-2$
					+ ("	animation-timing-function:	ease;\n") //$NON-NLS-1$
					+ ("}\n") //$NON-NLS-1$

					+ ("@keyframes fadeinBody												\n") //$NON-NLS-1$
					+ ("{																	\n") //$NON-NLS-1$

					+ ("	from															\n") //$NON-NLS-1$
					+ ("	{																\n") //$NON-NLS-1$
					+ ("		opacity:				0;									\n") //$NON-NLS-1$
					+ ("		background-color:		ButtonFace;							\n") //$NON-NLS-1$
					+ ("		transform:				rotateX(" + (int) rotateX + "deg) rotateY(" + (int) rotateY + "deg);	\n") //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					+ ("	}																\n") //$NON-NLS-1$
//								transform:				rotateX(-80deg) rotateY(-80deg);
//								transform-origin: 		50% 200%;

					+ ("	to																\n") //$NON-NLS-1$
					+ ("	{																\n") //$NON-NLS-1$
					+ ("		opacity:				1;									\n") //$NON-NLS-1$
					+ ("	}																\n") //$NON-NLS-1$
					+ ("}																	\n"); //$NON-NLS-1$
		}

		/*
		 * Tile size
		 */
		final String tileSize = UI.EMPTY_STRING
		//
				+ (CSS_IMPORT_TILE + "\n") //$NON-NLS-1$
				+ ("{\n") //$NON-NLS-1$
				+ ("	min-height: " + itemSize + "px;\n") //$NON-NLS-1$ //$NON-NLS-2$
				+ ("	max-height: " + itemSize + "px;\n") //$NON-NLS-1$ //$NON-NLS-2$
				+ ("	min-width: " + itemSize + "px;\n") //$NON-NLS-1$ //$NON-NLS-2$
				+ ("	max-width: " + itemSize + "px;\n") //$NON-NLS-1$ //$NON-NLS-2$
				+ ("}\n"); //$NON-NLS-1$

		/*
		 * CSS
		 */
		final String customCSS = "" // //$NON-NLS-1$

				+ "<style>\n" // //$NON-NLS-1$
				+ animation
				+ bgImage
				+ tileSize
				+ "</style>\n"; //$NON-NLS-1$

		return customCSS;
	}

	private void createFilesLog(final StringBuilder sb, final ArrayList<String> fileNames, final String message) {

		sb.append(message + fileNames.size());

		for (final String fileName : fileNames) {
			sb.append(UI.NEW_LINE);
			sb.append(fileName);
		}
	}

	private String createHTML() {

//		Force Internet Explorer to not use compatibility mode. Internet Explorer believes that websites under
//		several domains (including "ibm.com") require compatibility mode. You may see your web application run
//		normally under "localhost", but then fail when hosted under another domain (e.g.: "ibm.com").
//		Setting "IE=Edge" will force the latest standards mode for the version of Internet Explorer being used.
//		This is supported for Internet Explorer 8 and later. You can also ease your testing efforts by forcing
//		specific versions of Internet Explorer to render using the standards mode of previous versions. This
//		prevents you from exploiting the latest features, but may offer you compatibility and stability. Lookup
//		the online documentation for the "X-UA-Compatible" META tag to find which value is right for you.

		final String html = "" // //$NON-NLS-1$
				+ "<!DOCTYPE html>\n" // ensure that IE is using the newest version and not the quirk mode //$NON-NLS-1$
				+ "<html style='height: 100%; width: 100%; margin: 0px; padding: 0px;'>\n" //$NON-NLS-1$
				+ ("<head>\n" + createHTML_10_Head() + "\n</head>\n") //$NON-NLS-1$ //$NON-NLS-2$
				+ ("<body>\n" + createHTML_20_Body() + "\n</body>\n") //$NON-NLS-1$ //$NON-NLS-2$
				+ "</html>"; //$NON-NLS-1$

		return html;
	}

	private String createHTML_10_Head() {

		final String html = ""// //$NON-NLS-1$
				+ "	<meta http-equiv='Content-Type' content='text/html; charset=UTF-8' />\n" //$NON-NLS-1$
				+ "	<meta http-equiv='X-UA-Compatible' content='IE=edge' />\n" //$NON-NLS-1$
				+ _cssFonts
				+ _cssFromFile
				+ createCSS_Custom()
				+ "\n"; //$NON-NLS-1$

		return html;
	}

	private String createHTML_20_Body() {

		final StringBuilder sb = new StringBuilder();

		sb.append("<div class='import-container'>\n"); //$NON-NLS-1$
		{
			/*
			 * Very tricky: When a parent has an opacity, a child cannot modify it. Therefore the
			 * different divs with position relative/absolute. It took me some time to
			 * find/implement this tricky but simple solution.
			 */
			sb.append("<div class='import-background'></div>\n"); //$NON-NLS-1$

			sb.append("<div class='import-content'>\n"); //$NON-NLS-1$
			{
				/*
				 * Device Import
				 */
				createHTML_50_Easy_Header(sb);
				createHTML_80_Easy_Tiles(sb);

				/*
				 * Get Tours
				 */
				sb.append("<div class='get-tours-title title'>\n"); //$NON-NLS-1$
				sb.append("	" + Messages.Import_Data_HTML_GetTours + "\n"); //$NON-NLS-1$ //$NON-NLS-2$
				sb.append("</div>\n"); //$NON-NLS-1$

				createHTML_90_SimpleImport(sb);
			}
			sb.append("</div>\n"); //$NON-NLS-1$
		}
		sb.append("</div>\n"); //$NON-NLS-1$

		return sb.toString();
	}

	private void createHTML_50_Easy_Header(final StringBuilder sb) {

		final String watchClass = isWatchingOn() ? DOM_CLASS_DEVICE_ON : DOM_CLASS_DEVICE_OFF;

		final String html = "" // //$NON-NLS-1$

				+ "<div class='auto-import-header'>\n" //$NON-NLS-1$
				+ ("	<table border=0><tbody><tr>\n") //$NON-NLS-1$

				// device state on/off
				+ ("		<td>\n") //$NON-NLS-1$
				+ ("			<div id='" + DOM_ID_DEVICE_ON_OFF + "'>" + createHTML_52_DeviceState_OnOff() + "</div>\n") //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				+ ("		</td>\n") //$NON-NLS-1$

				// title
				+ ("		<td><span class='title'>" + Messages.Import_Data_HTML_EasyImport + "</span></td>\n") //$NON-NLS-1$ //$NON-NLS-2$

				// state icon
				+ ("		<td>\n") //$NON-NLS-1$
				+ ("			<div id='" + DOM_ID_DEVICE_STATE + "' style='padding-left:25px;' class='" + watchClass + "'>\n") //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				+ (createHTML_54_DeviceState())
				+ ("			</div>\n") //$NON-NLS-1$
				+ ("		</td>\n") //$NON-NLS-1$

				// selected config
				+ ("		<td>") //$NON-NLS-1$
				+ ("			<div id='" + DOM_ID_IMPORT_CONFIG + "' class='" + watchClass + "'>") //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				+ (createHTML_60_SelectImportConfig())
				+ ("			</div>") //$NON-NLS-1$
				+ ("		</td>") //$NON-NLS-1$

				+ "	</tr></tbody></table>\n" // //$NON-NLS-1$
				+ "</div>\n"; //$NON-NLS-1$

		sb.append(html);
	}

	private String createHTML_52_DeviceState_OnOff() {

		final boolean isWatchingOn = isWatchingOn();

		String tooltip = isWatchingOn
				? Messages.Import_Data_HTML_DeviceOff_Tooltip
				: Messages.Import_Data_HTML_DeviceOn_Tooltip;

		tooltip = replaceNewLine(tooltip);

		// shwo red image when off
		final String imageUrl = isWatchingOn //
				? _imageUrl_Device_TurnOn
				: _imageUrl_Device_TurnOff;

		final String hrefAction = HTTP_DUMMY + HREF_ACTION_DEVICE_WATCHING_ON_OFF;
		final String onOffImage = createHTML_BgImageStyle(imageUrl);

		final String html = ""// //$NON-NLS-1$

				+ "<a class='onOffIcon dash-action'" // //$NON-NLS-1$
				+ ("title='" + tooltip + "'") //$NON-NLS-1$ //$NON-NLS-2$
				+ (" href='" + hrefAction + "'") //$NON-NLS-1$ //$NON-NLS-2$
				+ ">" //$NON-NLS-1$

				+ ("<div class='stateIcon' " + onOffImage + "></div>") //$NON-NLS-1$ //$NON-NLS-2$

				+ "</a>"; //$NON-NLS-1$

		return html;
	}

	private String createHTML_54_DeviceState() {

		final String hrefSetupAction = HTTP_DUMMY + HREF_ACTION_SETUP_EASY_IMPORT;

		final EasyConfig easyConfig = getEasyConfig();
		final ImportConfig importConfig = easyConfig.getActiveImportConfig();

		String html = null;

		final boolean isWatchAnything = importConfig.isWatchAnything();

		if (!isWatchingOn()) {

			// watching is off

			final String stateImage = createHTML_BgImageStyle(_imageUrl_DeviceFolder_Disabled);
			final String htmlTooltip = Messages.Import_Data_HTML_WatchingIsOff;

			html = "\n"// //$NON-NLS-1$

					+ "<a class='importState dash-action'" // //$NON-NLS-1$
					+ (" href='" + HTTP_DUMMY + "'") //$NON-NLS-1$ //$NON-NLS-2$
					+ ">" //$NON-NLS-1$

					+ ("<div class='stateIcon' " + stateImage + ">\n") //$NON-NLS-1$ //$NON-NLS-2$
					+ ("   <div class='stateIconValue'></div>\n") //$NON-NLS-1$
					+ ("</div>\n") //$NON-NLS-1$
					+ ("<div class='stateTooltip stateTooltipMessage'>" + htmlTooltip + "</div>\n") //$NON-NLS-1$ //$NON-NLS-2$

					+ "</a>\n"; //$NON-NLS-1$

		} else if (isWatchAnything && _isDeviceStateValid) {

			html = createHTML_55_DeviceState_IsValid(easyConfig, hrefSetupAction);

		} else {

			/*
			 * On startup, set the folder state without device info because this is retrieved in a
			 * background thread, if not, it is blocking the UI !!!
			 */

			final String stateImage = createHTML_BgImageStyle(isWatchAnything
					? _imageUrl_DeviceFolder_NotChecked
					: _imageUrl_DeviceFolder_NotSetup);

			final String htmlTooltip = isWatchAnything
					? Messages.Import_Data_HTML_AcquireDeviceInfo
					: Messages.Import_Data_HTML_NothingIsWatched;

			html = "\n"// //$NON-NLS-1$

					+ "<a class='importState dash-action'" // //$NON-NLS-1$
					+ (" href='" + hrefSetupAction + "'") //$NON-NLS-1$ //$NON-NLS-2$
					+ ">\n" //$NON-NLS-1$

					+ ("<div class='stateIcon' " + stateImage + ">\n") //$NON-NLS-1$ //$NON-NLS-2$
					+ ("   <div class='stateIconValue'></div>\n") //$NON-NLS-1$
					+ ("</div>\n") //$NON-NLS-1$
					+ ("<div class='stateTooltip stateTooltipMessage'>" + htmlTooltip + "</div>\n") //$NON-NLS-1$ //$NON-NLS-2$

					+ "</a>\n"; //$NON-NLS-1$
		}

		return html;
	}

	private String createHTML_55_DeviceState_IsValid(final EasyConfig easyConfig, final String hrefAction) {

		final ImportConfig importConfig = easyConfig.getActiveImportConfig();

		final int numDeviceFiles = easyConfig.numDeviceFiles;
		final String deviceOSFolder = importConfig.getDeviceOSFolder();
		final boolean isDeviceFolderOK = isOSFolderValid(deviceOSFolder);
		boolean isFolderOK = true;

		final StringBuilder sb = new StringBuilder();
		sb.append("<table border=0><tbody>"); //$NON-NLS-1$

		/*
		 * Title
		 */
		sb.append(HTML_TR);
		sb.append("<td colspan=2 class='importConfigTitle'>" + importConfig.name + HTML_TD_END); //$NON-NLS-1$
		sb.append(HTML_TR_END);

		/*
		 * Backup folder
		 */
		final boolean isCreateBackup = importConfig.isCreateBackup;
		if (isCreateBackup) {

			final String htmlBackupFolder = importConfig.getBackupFolder().replace(//
					UI.SYMBOL_BACKSLASH,
					UI.SYMBOL_HTML_BACKSLASH);

			// check OS folder
			final String backupOSFolder = importConfig.getBackupOSFolder();
			final boolean isBackupFolderOK = isOSFolderValid(backupOSFolder);
			isFolderOK &= isBackupFolderOK;

			final String folderTitle = Messages.Import_Data_HTML_Title_Backup;
			String folderInfo = null;

			/*
			 * Show back folder info only when device folder is OK because they are related
			 * together.
			 */
			if (isDeviceFolderOK) {

				final int numNotBackedUpFiles = easyConfig.notBackedUpFiles.size();

				folderInfo = numNotBackedUpFiles == 0 //
						? NLS.bind(Messages.Import_Data_HTML_AllFilesAreBackedUp, numDeviceFiles)
						: NLS.bind(Messages.Import_Data_HTML_NotBackedUpFiles, numNotBackedUpFiles, numDeviceFiles);

			}

			createHTML_56_FolderState(//
					sb,
					htmlBackupFolder,
					isBackupFolderOK,
					false,
					folderTitle,
					folderInfo);
		}

		/*
		 * Device folder
		 */
		final ArrayList<OSFile> notImportedFiles = easyConfig.notImportedFiles;
		final int numNotImportedFiles = notImportedFiles.size();
		{
			final String htmlDeviceFolder = importConfig.getDeviceFolder().replace(//
					UI.SYMBOL_BACKSLASH,
					UI.SYMBOL_HTML_BACKSLASH);

			final boolean isTopMargin = importConfig.isCreateBackup;

			final String folderTitle = Messages.Import_Data_HTML_Title_Device;

			final String folderInfo = numNotImportedFiles == 0 //
					? NLS.bind(Messages.Import_Data_HTML_AllFilesAreImported, numDeviceFiles)
					: NLS.bind(Messages.Import_Data_HTML_NotImportedFiles, numNotImportedFiles, numDeviceFiles);

			createHTML_56_FolderState(//
					sb,
					htmlDeviceFolder,
					isDeviceFolderOK,
					isTopMargin,
					folderTitle,
					folderInfo);

			isFolderOK &= isDeviceFolderOK;
		}

		/*
		 * Device files
		 */
		{
			final String deviceFiles = importConfig.deviceFiles.trim().length() == 0
					? ImportConfig.DEVICE_FILES_DEFAULT
					: importConfig.deviceFiles;

			sb.append(HTML_TR);

			sb.append(HTML_TD_SPACE + HTML_STYLE_TITLE_VERTICAL_PADDING + " class='folderTitle'>"); //$NON-NLS-1$
			sb.append(Messages.Import_Data_HTML_Title_Files);
			sb.append(HTML_TD_END);

			sb.append(HTML_TD_SPACE + HTML_STYLE_TITLE_VERTICAL_PADDING + " class='folderLocation'>"); //$NON-NLS-1$
			sb.append(deviceFiles);
			sb.append(HTML_TD_END);

			sb.append(HTML_TR_END);
		}

		/*
		 * 100. Delete device files
		 */
		{
			final boolean isDeleteDeviceFiles = importConfig.isDeleteDeviceFiles;

			final String deleteFiles = isDeleteDeviceFiles
					? Messages.Import_Data_HTML_DeleteFilesYES
					: Messages.Import_Data_HTML_DeleteFilesNO;

			sb.append(HTML_TR);

			sb.append(HTML_TD_SPACE + HTML_STYLE_TITLE_VERTICAL_PADDING + " class='folderTitle'>"); //$NON-NLS-1$
			sb.append(Messages.Import_Data_HTML_Title_Delete);
			sb.append(HTML_TD_END);

			sb.append(HTML_TD_SPACE + HTML_STYLE_TITLE_VERTICAL_PADDING + " class='folderLocation'>"); //$NON-NLS-1$
			sb.append(deleteFiles);
			sb.append(HTML_TD_END);

			sb.append(HTML_TR_END);
		}

		/*
		 * 101. Turn off device watching
		 */
		{
			final boolean isWatchingOff = importConfig.isTurnOffWatching;

			final String watchingText = isWatchingOff
					? Messages.Import_Data_HTML_WatchingOff
					: Messages.Import_Data_HTML_WatchingOn;

			// shwo red image when off
			final String imageUrl = isWatchingOff //
					? _imageUrl_Device_TurnOff
					: _imageUrl_Device_TurnOn;

			final String onOffImage = createHTML_BgImage(imageUrl);

			sb.append(HTML_TR);

			sb.append(HTML_TD_SPACE + HTML_STYLE_TITLE_VERTICAL_PADDING + ">"); //$NON-NLS-1$
			sb.append("	<div class='action-button-25' style='" + onOffImage + "'></div>"); //$NON-NLS-1$ //$NON-NLS-2$
			sb.append(HTML_TD_END);
			sb.append("<td class='folderInfo' " + HTML_STYLE_TITLE_VERTICAL_PADDING + ">" + watchingText + HTML_TD_END); //$NON-NLS-1$ //$NON-NLS-2$

			sb.append(HTML_TR_END);
		}

		sb.append("</tbody></table>"); //$NON-NLS-1$

		/*
		 * Import files
		 */
		if (numNotImportedFiles > 0) {
			createHTML_58_NotImportedFiles(sb, notImportedFiles);
		}

		final String htmlTooltip = sb.toString();

		/*
		 * All states
		 */
		final String imageUrl = isFolderOK ? _imageUrl_DeviceFolder_OK : _imageUrl_DeviceFolder_NotAvailable;
		final String stateImage = createHTML_BgImageStyle(imageUrl);
		final String stateIconValue = isDeviceFolderOK ? Integer.toString(numNotImportedFiles) : UI.EMPTY_STRING;

		/*
		 * Show overflow scrollbar ONLY when more than 10 entries are available because it looks
		 * ugly.
		 */
		final String cssOverflow = numNotImportedFiles > 10 //
				? "style='overflow-y: scroll;'" //$NON-NLS-1$
				: UI.EMPTY_STRING;

		final String html = "\n"// //$NON-NLS-1$

				+ "<a class='importState dash-action'" // //$NON-NLS-1$
				+ (" href='" + hrefAction + "'") //$NON-NLS-1$ //$NON-NLS-2$
				+ ">" //$NON-NLS-1$

				+ ("	<div class='stateIcon' " + stateImage + ">\n") //$NON-NLS-1$ //$NON-NLS-2$
				+ ("		<div class='stateIconValue'>" + stateIconValue + "</div>\n") //$NON-NLS-1$ //$NON-NLS-2$
				+ ("	</div>\n") //$NON-NLS-1$
				+ ("	<div class='stateTooltip' " + cssOverflow + ">" + htmlTooltip + "</div>\n") //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

				+ "</a>\n"; //$NON-NLS-1$

		return html;
	}

	/**
	 * @param sb
	 * @param folderLocation
	 * @param isOSFolderValid
	 * @param isTopMargin
	 * @param folderTitle
	 * @param folderInfo
	 */
	private void createHTML_56_FolderState(	final StringBuilder sb,
											final String folderLocation,
											final boolean isOSFolderValid,
											final boolean isTopMargin,
											final String folderTitle,
											final String folderInfo) {

		String htmlErrorState;
		String htmlFolderInfo;

		if (isOSFolderValid) {

			htmlErrorState = UI.EMPTY_STRING;
			htmlFolderInfo = folderInfo == null //
					? UI.EMPTY_STRING
					: "<span class='folderInfo'>" + folderInfo + "</span>"; //$NON-NLS-1$ //$NON-NLS-2$

		} else {

			htmlErrorState = "<div class='folderError'>" + Messages.Import_Data_HTML_FolderIsNotAvailable + "</div>"; //$NON-NLS-1$ //$NON-NLS-2$
			htmlFolderInfo = UI.EMPTY_STRING;
		}

		final String paddingTop = isTopMargin //
				? HTML_STYLE_TITLE_VERTICAL_PADDING
				: UI.EMPTY_STRING;

		final String imageUrl = isOSFolderValid ? _imageUrl_State_OK : _imageUrl_State_Error;
		final String folderStateIcon = "<img src='" //$NON-NLS-1$
				+ imageUrl
				+ "' style='padding-left:5px; vertical-align:text-bottom;'>"; //$NON-NLS-1$

		sb.append(HTML_TR);
		sb.append(HTML_TD_SPACE + paddingTop + " class='folderTitle'>" + folderTitle + HTML_TD_END); //$NON-NLS-1$
		sb.append(HTML_TD_SPACE + paddingTop + " class='folderLocation'>" + folderLocation + folderStateIcon); //$NON-NLS-1$
		sb.append(htmlErrorState);
		sb.append(HTML_TD_END);
		sb.append(HTML_TR_END);

		sb.append(HTML_TR);
		sb.append("<td></td>"); //$NON-NLS-1$
		sb.append(HTML_TD + htmlFolderInfo + HTML_TD_END);
		sb.append(HTML_TR_END);

	}

	private void createHTML_58_NotImportedFiles(final StringBuilder sb, final ArrayList<OSFile> notImportedFiles) {

		sb.append("<table class='deviceList'><tbody>"); //$NON-NLS-1$

		for (final OSFile deviceFile : notImportedFiles) {

			sb.append(HTML_TR);

			sb.append("<td class='column name'>"); //$NON-NLS-1$
			sb.append(deviceFile.fileName);
			sb.append(HTML_TD_END);

			sb.append("<td class='right column'>"); //$NON-NLS-1$
			sb.append(_dateFormatter.format(deviceFile.modifiedTime));
			sb.append(HTML_TD_END);

			sb.append("<td class='right'>"); //$NON-NLS-1$
			sb.append(_timeFormatter.format(deviceFile.modifiedTime));
			sb.append(HTML_TD_END);

			sb.append("<td class='right column'>"); //$NON-NLS-1$
			sb.append(deviceFile.size);
			sb.append(HTML_TD_END);

			sb.append(HTML_TR_END);
		}

		sb.append("</tbody></table>"); //$NON-NLS-1$
	}

	private String createHTML_60_SelectImportConfig() {

		final String onChange = "onchange='" + JS_FUNCTION_ON_SELECT_IMPORT_CONFIG + "(this.selectedIndex)'"; //$NON-NLS-1$ //$NON-NLS-2$

		final EasyConfig easyConfig = getEasyConfig();
		final ImportConfig selectedConfig = easyConfig.getActiveImportConfig();

		final StringBuilder sb = new StringBuilder();

		sb.append("<div class='selectConfigContainer'>"); //$NON-NLS-1$
		sb.append("	<select class='selectConfig dash-action' " + onChange + ">"); //$NON-NLS-1$ //$NON-NLS-2$

		for (final ImportConfig importConfig : easyConfig.importConfigs) {

			final String isSelected = importConfig.equals(selectedConfig)//
					? "selected" //$NON-NLS-1$
					: UI.EMPTY_STRING;

			sb.append("		<option class='selectOption' " + isSelected + ">" + importConfig.name + "</option>"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
		sb.append("	</select>"); //$NON-NLS-1$
		sb.append("</div>"); //$NON-NLS-1$

		return sb.toString();
	}

	private void createHTML_80_Easy_Tiles(final StringBuilder sb) {

		final EasyConfig easyConfig = getEasyConfig();

		final ArrayList<ImportLauncher> allImportLauncher = easyConfig.importLaunchers;
		final ArrayList<ImportConfig> allImportConfigs = easyConfig.importConfigs;

		if (allImportLauncher.size() == 0 || allImportConfigs.size() == 0) {

			// this case should not happen
			StatusUtil.showStatus(new Exception("Import config/launcher are not setup correctly."));//$NON-NLS-1$

			return;
		}

		// get available launcher
		int availableLauncher = 0;
		for (final ImportLauncher importLauncher : allImportLauncher) {
			if (importLauncher.isShowInDashboard) {
				availableLauncher++;
			}
		}

		int tileIndex = 0;
		int numHorizontalTiles = easyConfig.numHorizontalTiles;
		boolean isTrOpen = false;

		// reduce max tiles, otherwise there would be a BIG gap between tiles
		if (availableLauncher < numHorizontalTiles) {
			numHorizontalTiles = availableLauncher;
		}

		final String watchClass = isWatchingOn() //
				? DOM_CLASS_DEVICE_ON
				: DOM_CLASS_DEVICE_OFF;

		sb.append("<table border=0" //$NON-NLS-1$
				+ (" id='" + DOM_ID_IMPORT_TILES + "'") //$NON-NLS-1$ //$NON-NLS-2$
				+ (" style='margin-top:5px;'") //$NON-NLS-1$
				+ (" class='" + watchClass + "'") //$NON-NLS-1$ //$NON-NLS-2$
				+ "><tbody>\n"); //$NON-NLS-1$

		for (final ImportLauncher importLauncher : allImportLauncher) {

			if (importLauncher.isShowInDashboard) {

				if (tileIndex % numHorizontalTiles == 0) {
					sb.append("<tr>\n"); //$NON-NLS-1$
					isTrOpen = true;
				}

				// enforce equal column width
				sb.append("<td style='width:" + 100 / numHorizontalTiles + "%' class='import-tile'>\n"); //$NON-NLS-1$ //$NON-NLS-2$
				sb.append(createHTML_82_Easy_Tile(importLauncher));
				sb.append("</td>\n"); //$NON-NLS-1$

				if (tileIndex % numHorizontalTiles == numHorizontalTiles - 1) {
					sb.append("</tr>\n"); //$NON-NLS-1$
					isTrOpen = false;
				}

				tileIndex++;
			}
		}

		if (isTrOpen) {
			sb.append("</tr>\n"); //$NON-NLS-1$
		}

		sb.append("</tbody></table>\n"); //$NON-NLS-1$
	}

	private String createHTML_82_Easy_Tile(final ImportLauncher importTile) {

		/*
		 * Tooltip
		 */
		final String tooltip = createHTML_84_TileTooltip(importTile);

		/*
		 * Tile image
		 */
		final Image tileImage = getImportConfigImage(importTile);
		String htmlImage = UI.EMPTY_STRING;
		if (tileImage != null) {

			final byte[] pngImageData = ImageUtils.formatImage(tileImage, SWT.IMAGE_PNG);
			final String base64Encoded = Base64.getEncoder().encodeToString(pngImageData);

			htmlImage = "<img src='data:image/png;base64," + base64Encoded + "'>"; //$NON-NLS-1$ //$NON-NLS-2$
		}

		/*
		 * Tile HTML
		 */
		final String href = HTTP_DUMMY + HREF_ACTION_DEVICE_IMPORT + HREF_TOKEN + importTile.getId();

		final String htmlConfig = createHTML_86_Annotations(importTile);

		final String html = "" //$NON-NLS-1$

				+ ("<a href='" + href + "' title='" + tooltip + "' class='import-tile'>\n") //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				+ ("	<div class='import-tile-image'>" + htmlImage + "</div>\n") //$NON-NLS-1$ //$NON-NLS-2$
				+ ("	<div class='import-tile-config'>" + htmlConfig + "</div>\n") //$NON-NLS-1$ //$NON-NLS-2$
				+ ("</a>\n") //$NON-NLS-1$
		;

		return html;
	}

	private String createHTML_84_TileTooltip(final ImportLauncher importLauncher) {

		boolean isTextAdded = false;

		final StringBuilder sb = new StringBuilder();

		final String tileName = importLauncher.name.trim();
		final String tileDescription = importLauncher.description.trim();

		final StringBuilder ttText = new StringBuilder();
		final Enum<TourTypeConfig> ttConfig = importLauncher.tourTypeConfig;

		if (TourTypeConfig.TOUR_TYPE_CONFIG_BY_SPEED.equals(ttConfig)) {

			final ArrayList<SpeedTourType> speedTourTypes = importLauncher.speedTourTypes;
			boolean isSpeedAdded = false;

			for (final SpeedTourType speedTT : speedTourTypes) {

				if (isSpeedAdded) {
					ttText.append(UI.NEW_LINE);
				}

				final long tourTypeId = speedTT.tourTypeId;
				final double avgSpeed = (speedTT.avgSpeed / net.tourbook.ui.UI.UNIT_VALUE_DISTANCE) + 0.0001;

				ttText.append((int) avgSpeed);
				ttText.append(UI.SPACE);
				ttText.append(UI.UNIT_LABEL_SPEED);
				ttText.append(UI.DASH_WITH_DOUBLE_SPACE);
				ttText.append(net.tourbook.ui.UI.getTourTypeLabel(tourTypeId));

				isSpeedAdded = true;
			}

		} else if (TourTypeConfig.TOUR_TYPE_CONFIG_ONE_FOR_ALL.equals(ttConfig)) {

			final TourType oneTourType = importLauncher.oneTourType;
			if (oneTourType != null) {

				final String ttName = oneTourType.getName();

				// show this text only when the name is different
				if (!tileName.equals(ttName)) {
					ttText.append(ttName);
				}
			}
		}

		// tour type name
		if (tileName.length() > 0) {

			sb.append(tileName);
			isTextAdded = true;
		}

		// tour type description
		if (tileDescription.length() > 0) {

			if (isTextAdded) {
				sb.append(UI.NEW_LINE2);
			}

			sb.append(tileDescription);
			isTextAdded = true;
		}

		// tour type text
		if (ttText.length() > 0) {

			if (isTextAdded) {
				sb.append(UI.NEW_LINE2);
			}

			sb.append(ttText);
			isTextAdded = true;
		}

		sb.append(UI.NEW_LINE2);

		// last marker
		{
			final double distance = importLauncher.lastMarkerDistance / 1000.0 / net.tourbook.ui.UI.UNIT_VALUE_DISTANCE;

			final String distanceValue = _nf1.format(distance) + UI.SPACE1 + UI.UNIT_LABEL_DISTANCE;

			sb.append(importLauncher.isSetLastMarker //
					? NLS.bind(Messages.Import_Data_HTML_LastMarker_Yes, distanceValue, importLauncher.lastMarkerText)
					: Messages.Import_Data_HTML_LastMarker_No);
		}

		// save tour
		{
			sb.append(UI.NEW_LINE);

			sb.append(importLauncher.isSaveTour
					? Messages.Import_Data_HTML_SaveTour_Yes
					: Messages.Import_Data_HTML_SaveTour_No);
		}

		// delete device files
		{
			sb.append(UI.NEW_LINE);

			sb.append(getEasyConfig().getActiveImportConfig().isDeleteDeviceFiles
					? Messages.Import_Data_HTML_DeleteDeviceFiles_Yes
					: Messages.Import_Data_HTML_DeleteDeviceFiles_No);
		}

		return sb.toString();
	}

	private String createHTML_86_Annotations(final ImportLauncher importTile) {

		/*
		 * Save tour
		 */
		String htmlSaveTour = UI.EMPTY_STRING;
		if (importTile.isSaveTour) {

			final String stateImage = createHTML_BgImage(_imageUrl_State_SaveTour);

			htmlSaveTour = createHTML_TileAnnotation(stateImage);
		}

		/*
		 * Marker
		 */
		String htmlLastMarker = UI.EMPTY_STRING;
		if (importTile.isSetLastMarker) {

			final String stateImage = createHTML_BgImage(_imageUrl_State_TourMarker);

			htmlLastMarker = createHTML_TileAnnotation(stateImage);
		}

		/*
		 * Delete device files
		 */
		String htmlDeleteFiles = UI.EMPTY_STRING;
		if (getEasyConfig().getActiveImportConfig().isDeleteDeviceFiles) {

			final String stateImage = createHTML_BgImage(_imageUrl_State_DeleteFiles);

			htmlDeleteFiles = createHTML_TileAnnotation(stateImage);
		}

		final StringBuilder sb = new StringBuilder();

		// order is reverted that it looks in the correct order
		sb.append(htmlDeleteFiles);
		sb.append(htmlSaveTour);
		sb.append(htmlLastMarker);

		sb.append("<div style='float:left;'>" + importTile.name + "</div>"); //$NON-NLS-1$ //$NON-NLS-2$

		return sb.toString();
	}

	private void createHTML_90_SimpleImport(final StringBuilder sb) {

		sb.append("<div class='get-tours-items'>\n"); //$NON-NLS-1$
		sb.append("	<table><tbody><tr>\n"); //$NON-NLS-1$
		{
			createHTML_92_TileAction(
					sb,
					Messages.Import_Data_HTML_ImportFromFiles_Action,
					Messages.Import_Data_HTML_ImportFromFiles_ActionTooltip,
					(HTTP_DUMMY + HREF_ACTION_IMPORT_FROM_FILES),
					_imageUrl_ImportFromFile);

			createHTML_92_TileAction(
					sb,
					Messages.Import_Data_HTML_ReceiveFromSerialPort_ConfiguredAction,
					Messages.Import_Data_HTML_ReceiveFromSerialPort_ConfiguredLink,
					(HTTP_DUMMY + HREF_ACTION_SERIAL_PORT_CONFIGURED),
					_imageUrl_SerialPort_Configured);

			createHTML_92_TileAction(
					sb,
					Messages.Import_Data_HTML_ReceiveFromSerialPort_DirectlyAction,
					Messages.Import_Data_HTML_ReceiveFromSerialPort_DirectlyLink,
					(HTTP_DUMMY + HREF_ACTION_SERIAL_PORT_DIRECTLY),
					_imageUrl_SerialPort_Directly);

			createHTML_92_TileAction(
					sb,
					Messages.Import_Data_HTML_Action_OldUI,
					Messages.Import_Data_HTML_Action_OldUI_Tooltip,
					(HTTP_DUMMY + HREF_ACTION_OLD_UI),
					null);
		}
		sb.append("	</tr></tbody></table>\n"); // //$NON-NLS-1$
		sb.append("</div>\n"); //$NON-NLS-1$

	}

	private void createHTML_92_TileAction(	final StringBuilder sb,
											final String name,
											final String tooltip,
											final String href,
											final String imageUrl) {

		String htmlImage = UI.EMPTY_STRING;

		if (imageUrl != null) {

			htmlImage = "style='" // //$NON-NLS-1$

					+ ("background-image:	url(" + imageUrl + ");\n") //$NON-NLS-1$ //$NON-NLS-2$
					//
					+ "'"; //$NON-NLS-1$
		}

		final String validTooltip = tooltip.replace("'", ""); //$NON-NLS-1$ //$NON-NLS-2$

		final String html = "" //$NON-NLS-1$

				+ HTML_TD
				+ ("<a href='" + href + "' title='" + validTooltip + "' class='import-tile'>\n") //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				+ ("	<div class='import-tile-image action-button' " + htmlImage + "></div>\n") //$NON-NLS-1$ //$NON-NLS-2$
				+ ("	<div class='import-tile-config'>" + name + "</div>\n") //$NON-NLS-1$ //$NON-NLS-2$
				+ ("</a>\n") //$NON-NLS-1$
				+ HTML_TD_END;

		sb.append(html);
	}

	private String createHTML_BgImage(final String imageUrl) {
		return "background-image: url(" + imageUrl + ");"; //$NON-NLS-1$ //$NON-NLS-2$
	}

	private String createHTML_BgImageStyle(final String imageUrl) {

		final String bgImage = createHTML_BgImage(imageUrl);

		return "style='" + bgImage + "'"; //$NON-NLS-1$ //$NON-NLS-2$
	}

	private String createHTML_TileAnnotation(final String imageBgStyle) {

		return "<div style='float: right;" + imageBgStyle + "' class='action-button-16'></div>"; //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Override
	public void createPartControl(final Composite parent) {

		initUI(parent);

		// define all columns
		_columnManager = new ColumnManager(this, _state);
		defineAllColumns();

		createUI(parent);
		createActions();

		fillToolbar();

		addPartListener();
		addSelectionListener();
		addPrefListener();
		addTourEventListener();

		// set this view part as selection provider
		getSite().setSelectionProvider(_postSelectionProvider = new PostSelectionProvider(ID));

		_activePerson = TourbookPlugin.getActivePerson();

		// prevent that the UI start is run twice, 2nd would be in the part listener
		_newActivePerson = _activePerson;

		enableActions();
		restoreState();

		// the part visible listener shows the top page also
		updateUI_1_TopPage(true);
	}

	private void createResources_Image() {

		_imageDescDatabase = TourbookPlugin.getImageDescriptor(Messages.Image__database);
		_imageDescDatabaseOtherPerson = TourbookPlugin.getImageDescriptor(Messages.Image__database_other_person);
		_imageDescDatabaseAssignMergedTour = TourbookPlugin.getImageDescriptor(Messages.Image__assignMergedTour);
		_imageDescDatabasePlaceholder = TourbookPlugin.getImageDescriptor(Messages.Image__icon_placeholder);
		_imageDescDelete = TourbookPlugin.getImageDescriptor(Messages.Image__delete);

		try {

			final Display display = Display.getCurrent();

			_imageDatabase = (Image) _imageDescDatabase.createResource(display);
			_imageDatabaseOtherPerson = (Image) _imageDescDatabaseOtherPerson.createResource(display);
			_imageDatabaseAssignMergedTour = (Image) _imageDescDatabaseAssignMergedTour.createResource(display);
			_imageDatabasePlaceholder = (Image) _imageDescDatabasePlaceholder.createResource(display);
			_imageDelete = (Image) _imageDescDelete.createResource(display);

		} catch (final DeviceResourceException e) {
			StatusUtil.log(e);
		}

	}

	private void createResources_Web() {

		try {

			/*
			 * Font css must be on the top
			 */
			final File fontFile = WEB.getResourceFile(WEB_RESOURCE_TITLE_FONT);

			final Path path = Paths.get(fontFile.getAbsolutePath());
			final byte[] data = Files.readAllBytes(path);
			final String base64Encoded = Base64.getEncoder().encodeToString(data);

			_cssFonts = "<style>\n" // //$NON-NLS-1$

					+ ("@font-face\n") //$NON-NLS-1$
					+ ("{\n") //$NON-NLS-1$
//					+ ("	font-family:	'NothingYouCouldDo';\n") //$NON-NLS-1$
					+ ("	font-family:	'Nunito-Bold';\n") //$NON-NLS-1$
					+ ("	font-weight:	700;\n") //$NON-NLS-1$
					+ ("	font-style:		bold;\n") //$NON-NLS-1$
					+ ("	src:			url(data:font/truetype;charset=utf-8;base64," + base64Encoded + ") format('truetype');") //$NON-NLS-1$ //$NON-NLS-2$
					+ ("}\n") //$NON-NLS-1$

					+ "</style>\n"; //$NON-NLS-1$

			/*
			 * Webpage css
			 */
			File webFile = WEB.getResourceFile(WEB_RESOURCE_TOUR_IMPORT_CSS);
			final String css = Util.readContentFromFile(webFile.getAbsolutePath());

			String css3 = UI.EMPTY_STRING;

			/*
			 * Eclipse 3.8 with Linux has only a limited css3 support -> DISABLED
			 */
			if (UI.IS_WIN) {

				webFile = WEB.getResourceFile(WEB_RESOURCE_TOUR_IMPORT_CSS3);
				css3 = Util.readContentFromFile(webFile.getAbsolutePath());
			}

			_cssFromFile = ""// //$NON-NLS-1$
					+ "<style>\n" //$NON-NLS-1$
					+ css
					+ css3
					+ "</style>\n"; //$NON-NLS-1$

			/*
			 * Image urls
			 */
			_imageUrl_ImportFromFile = getIconUrl(Messages.Image__RawData_Import);
			_imageUrl_SerialPort_Configured = getIconUrl(Messages.Image__RawData_Transfer);
			_imageUrl_SerialPort_Directly = getIconUrl(Messages.Image__RawData_TransferDirect);

			_imageUrl_State_Error = getIconUrl(Messages.Image__State_Error);
			_imageUrl_State_OK = getIconUrl(Messages.Image__State_OK);
			_imageUrl_State_DeleteFiles = getIconUrl(Messages.Image__State_Delete);
			_imageUrl_State_SaveTour = getIconUrl(Messages.Image__State_SaveTour);
			_imageUrl_State_TourMarker = getIconUrl(Messages.Image__State_TourMarker);

			_imageUrl_Device_TurnOff = getIconUrl(Messages.Image__RawData_Device_TurnOff);
			_imageUrl_Device_TurnOn = getIconUrl(Messages.Image__RawData_Device_TurnOn);

			_imageUrl_DeviceFolder_OK = getIconUrl(Messages.Image__RawData_DeviceFolder);
			_imageUrl_DeviceFolder_Disabled = getIconUrl(Messages.Image__RawData_DeviceFolderDisabled);
			_imageUrl_DeviceFolder_NotAvailable = getIconUrl(Messages.Image__RawData_DeviceFolder_NotDefined);
			_imageUrl_DeviceFolder_NotChecked = getIconUrl(Messages.Image__RawData_DeviceFolder_NotChecked);
			_imageUrl_DeviceFolder_NotSetup = getIconUrl(Messages.Image__RawData_DeviceFolder_NotSetup);

		} catch (final IOException | URISyntaxException e) {
			StatusUtil.showStatus(e);
		}
	}

	private void createUI(final Composite parent) {

		_topPageBook = new PageBook(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(_topPageBook);

		_topPage_OldUI = createUI_05_Page_OldUI(_topPageBook);
		_topPage_Startup = createUI_10_Page_Startup(_topPageBook);
		_topPage_Dashboard = createUI_20_Page_Dashboard(_topPageBook);
		_topPage_ImportViewer = createUI_90_Page_TourViewer(_topPageBook);

		_topPageBook.showPage(_topPage_Startup);
	}

	private Composite createUI_05_Page_OldUI(final Composite parent) {

		final ImageRegistry imageRegistry = net.tourbook.ui.UI.IMAGE_REGISTRY;

		final int defaultWidth = 300;

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
		GridLayoutFactory.swtDefaults().numColumns(2).applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
		{
			{
				/*
				 * label: info
				 */
				final Label label = new Label(container, SWT.WRAP);
				GridDataFactory.fillDefaults()//
						.hint(defaultWidth, SWT.DEFAULT)
						.grab(true, false)
						.span(2, 1)
						.applyTo(label);
				label.setText(Messages.Import_Data_OldUI_Label_Info);
			}

			{
				/*
				 * link: import
				 */
				final CLabel iconImport = new CLabel(container, SWT.NONE);
				GridDataFactory.fillDefaults().indent(0, 10).applyTo(iconImport);
				iconImport.setImage(imageRegistry.get(IMAGE_IMPORT));

				_linkImport = new Link(container, SWT.NONE);
				GridDataFactory.fillDefaults()//
						.hint(defaultWidth, SWT.DEFAULT)
						.align(SWT.FILL, SWT.CENTER)
						.grab(true, false)
						.indent(0, 10)
						.applyTo(_linkImport);
				_linkImport.setText(Messages.Import_Data_OldUI_Link_Import);
				_linkImport.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(final SelectionEvent e) {
						_rawDataMgr.actionImportFromFile();
					}
				});
			}

			{
				/*
				 * link: data transfer
				 */
				final CLabel iconTransfer = new CLabel(container, SWT.NONE);
				GridDataFactory.fillDefaults().indent(0, 10).applyTo(iconTransfer);
				iconTransfer.setImage(imageRegistry.get(IMAGE_DATA_TRANSFER));

				final Link linkTransfer = new Link(container, SWT.NONE);
				GridDataFactory.fillDefaults()//
						.hint(defaultWidth, SWT.DEFAULT)
						.align(SWT.FILL, SWT.CENTER)
						.grab(true, false)
						.indent(0, 10)
						.applyTo(linkTransfer);
				//			<a>Transfer tours from a sportcomputer</a>
				//			<a>Touren von einem Sportcomputer �bertragen</a>
				linkTransfer.setText(Messages.Import_Data_OldUI_Link_ReceiveFromSerialPort_Configured);
				linkTransfer.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(final SelectionEvent e) {
						_rawDataMgr.actionImportFromDevice();
					}
				});
			}

			{
				/*
				 * link: direct data transfer
				 */
				final CLabel iconDirectTransfer = new CLabel(container, SWT.NONE);
				GridDataFactory.fillDefaults().indent(0, 10).applyTo(iconDirectTransfer);
				iconDirectTransfer.setImage(imageRegistry.get(IMAGE_DATA_TRANSFER_DIRECT));

				final Link linkTransferDirect = new Link(container, SWT.NONE);
				GridDataFactory.fillDefaults() //
						.hint(defaultWidth, SWT.DEFAULT)
						.align(SWT.FILL, SWT.CENTER)
						.grab(true, false)
						.indent(0, 10)
						.applyTo(linkTransferDirect);
				//			<a>Transfer tours from a sportcomputer with previous configuration</a>
				//			<a>Touren von einem Sportcomputer �bertragen, mit vorheriger Konfiguration</a>
				linkTransferDirect.setText(Messages.Import_Data_OldUI_Link_ReceiveFromSerialPort_Directly);
				linkTransferDirect.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(final SelectionEvent e) {
						_rawDataMgr.actionImportFromDeviceDirect();
					}
				});
			}

			{
				/*
				 * Link: New UI
				 */
				final Link link = new Link(container, SWT.NONE);
				GridDataFactory.fillDefaults()//
						.hint(defaultWidth, SWT.DEFAULT)
						.align(SWT.FILL, SWT.CENTER)
						.grab(true, false)
						.indent(28, 10)
						.span(2, 1)
						.applyTo(link);
				link.setText(Messages.Import_Data_OldUI_Link_ShowNewUI);
				link.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(final SelectionEvent e) {
						onSelectUI_New();
					}
				});
			}

			{
				/*
				 * label: hint
				 */
				final Label label = new Label(container, SWT.WRAP);
				GridDataFactory.fillDefaults()//
						.hint(defaultWidth, SWT.DEFAULT)
						.grab(true, false)
						.indent(0, 20)
						.span(2, 1)
						.applyTo(label);
				label.setText(Messages.Import_Data_OldUI_Label_Hint);
			}
		}

		return container;
	}

	/**
	 * This page is displayed until the first page of the browser is loaded.
	 * 
	 * @param parent
	 * @return
	 */
	private Composite createUI_10_Page_Startup(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
//		{
//			final Label label = new Label(container, SWT.NONE);
//			GridDataFactory.fillDefaults().grab(true, true).align(SWT.CENTER, SWT.CENTER).applyTo(label);
//			label.setText("TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST "); //$NON-NLS-1$
//		}

		return container;
	}

	private Composite createUI_20_Page_Dashboard(final Composite parent) {

		_dashboard_PageBook = new PageBook(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(_dashboard_PageBook);

		_dashboardPage_NoBrowser = new Composite(_dashboard_PageBook, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(_dashboardPage_NoBrowser);
		GridLayoutFactory.swtDefaults().numColumns(1).applyTo(_dashboardPage_NoBrowser);
		_dashboardPage_NoBrowser.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
		{
			_txtNoBrowser = new Text(_dashboardPage_NoBrowser, SWT.WRAP | SWT.READ_ONLY);
			GridDataFactory.fillDefaults()//
					.grab(true, true)
					.align(SWT.FILL, SWT.BEGINNING)
					.applyTo(_txtNoBrowser);
			_txtNoBrowser.setText(Messages.UI_Label_BrowserCannotBeCreated);
		}

		_dashboardPage_WithBrowser = new Composite(_dashboard_PageBook, SWT.NONE);
		GridLayoutFactory.fillDefaults().applyTo(_dashboardPage_WithBrowser);
		{
			createUI_22_Browser(_dashboardPage_WithBrowser);
		}

		return _dashboard_PageBook;
	}

	private void createUI_22_Browser(final Composite parent) {

		try {

			try {

				// use default browser
				_browser = new Browser(parent, SWT.NONE);

				// initial setup
				_browser.setRedraw(false);

			} catch (final Exception e) {

				/*
				 * Use mozilla browser, this is necessary for Linux when default browser fails
				 * however the XULrunner needs to be installed.
				 */
				_browser = new Browser(parent, SWT.MOZILLA);
			}

			GridDataFactory.fillDefaults().grab(true, true).applyTo(_browser);

//			final BrowserFunction function =
			new JS_OnSelectImportConfig(_browser, JS_FUNCTION_ON_SELECT_IMPORT_CONFIG);

			_browser.addLocationListener(new LocationAdapter() {

				@Override
				public void changed(final LocationEvent event) {

//					_browser.removeLocationListener(this);
//					function.dispose();
				}

				@Override
				public void changing(final LocationEvent event) {
					onBrowser_LocationChanging(event);
				}
			});

			_browser.addProgressListener(new ProgressAdapter() {
				@Override
				public void completed(final ProgressEvent event) {

					onBrowser_Completed(event);

				}
			});

		} catch (final SWTError e) {

			_txtNoBrowser.setText(NLS.bind(Messages.UI_Label_BrowserCannotBeCreated_Error, e.getMessage()));
		}
	}

	private Composite createUI_90_Page_TourViewer(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().applyTo(container);
		{
			createUI_92_TourViewer(container);
		}

		return container;
	}

	/**
	 * @param parent
	 */
	private void createUI_92_TourViewer(final Composite parent) {

		// table
		final Table table = new Table(parent, SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.MULTI);

		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		table.setHeaderVisible(true);
		table.setLinesVisible(_prefStore.getBoolean(ITourbookPreferences.VIEW_LAYOUT_DISPLAY_LINES));

		_tourViewer = new TableViewer(table);
		_columnManager.createColumns(_tourViewer);

		// table viewer
		_tourViewer.setContentProvider(new TourDataContentProvider());
		_tourViewer.setSorter(new DeviceImportSorter());

		_tourViewer.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(final DoubleClickEvent event) {

				final Object firstElement = ((IStructuredSelection) _tourViewer.getSelection()).getFirstElement();

				if ((firstElement != null) && (firstElement instanceof TourData)) {
					TourManager.getInstance().tourDoubleClickAction(RawDataView.this, _tourDoubleClickState);
				}
			}
		});

		_tourViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(final SelectionChangedEvent event) {
				fireSelectedTour();
			}
		});

		// set tour info tooltip provider
		_tourInfoToolTip = new TableViewerTourInfoToolTip(_tourViewer);

		createUI_94_ContextMenu();
	}

	/**
	 * create the views context menu
	 */
	private void createUI_94_ContextMenu() {

		final Table table = (Table) _tourViewer.getControl();

		final MenuManager menuMgr = new MenuManager("#PopupMenu"); //$NON-NLS-1$
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			@Override
			public void menuAboutToShow(final IMenuManager manager) {
				fillContextMenu(manager);
			}
		});

		final Menu tableContextMenu = menuMgr.createContextMenu(table);
		tableContextMenu.addMenuListener(new MenuAdapter() {
			@Override
			public void menuHidden(final MenuEvent e) {
				_tagMenuMgr.onHideMenu();
			}

			@Override
			public void menuShown(final MenuEvent menuEvent) {
				_tagMenuMgr.onShowMenu(menuEvent, table, Display.getCurrent().getCursorLocation(), _tourInfoToolTip);
			}
		});

		getSite().registerContextMenu(menuMgr, _tourViewer);

		_columnManager.createHeaderContextMenu(table, tableContextMenu);
	}

	/**
	 * Defines all columns for the table viewer in the column manager, the sequenze defines the
	 * default columns
	 * 
	 * @param parent
	 */
	private void defineAllColumns() {

		defineColumnDatabase();
		defineColumnDate();
		defineColumnTime();
		defineColumnTourType();
		defineColumnTourTypeText();
		defineColumnRecordingTime();
		defineColumnDrivingTime();
		defineColumnCalories();
		defineColumnDistance();
		defineColumnAvgSpeed();
		defineColumnAvgPace();
		defineColumnAltitudeUp();
		defineColumnAltitudeDown();
		defineColumnWeatherClouds();
		defineColumnTitle();
		defineColumnTags();
		defineColumnDeviceName();
		defineColumnDeviceProfile();
		defineColumnMarker();
		defineColumnTimeInterval();
		defineColumnImportFileName();
		defineColumnImportFilePath();
	}

	/**
	 * column: altitude down
	 */
	private void defineColumnAltitudeDown() {

		final ColumnDefinition colDef = TableColumnFactory.ALTITUDE_DOWN_SUMMARIZED_BORDER.createColumn(
				_columnManager,
				_pc);

		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
				final int tourAltDown = ((TourData) cell.getElement()).getTourAltDown();
				if (tourAltDown != 0) {
					cell.setText(Long.toString((long) (-tourAltDown / net.tourbook.ui.UI.UNIT_VALUE_ALTITUDE)));
				}
			}
		});
	}

	/**
	 * column: altitude up
	 */
	private void defineColumnAltitudeUp() {

		final ColumnDefinition colDef = TableColumnFactory.ALTITUDE_UP_SUMMARIZED_BORDER.createColumn(
				_columnManager,
				_pc);

		colDef.setIsDefaultColumn();
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
				final int tourAltUp = ((TourData) cell.getElement()).getTourAltUp();
				if (tourAltUp != 0) {
					cell.setText(Long.toString((long) (tourAltUp / net.tourbook.ui.UI.UNIT_VALUE_ALTITUDE)));
				}
			}
		});
	}

	/**
	 * column: average pace
	 */
	private void defineColumnAvgPace() {

		final ColumnDefinition colDef = TableColumnFactory.AVG_PACE.createColumn(_columnManager, _pc);

		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final TourData tourData = (TourData) cell.getElement();

				final float tourDistance = tourData.getTourDistance();
				final long drivingTime = tourData.getTourDrivingTime();

				final float pace = tourDistance == 0 ? //
						0
						: drivingTime * 1000 / tourDistance * net.tourbook.ui.UI.UNIT_VALUE_DISTANCE;

				cell.setText(net.tourbook.ui.UI.format_mm_ss((long) pace));
			}
		});
	}

	/**
	 * column: avg speed
	 */
	private void defineColumnAvgSpeed() {

		final ColumnDefinition colDef = TableColumnFactory.AVG_SPEED.createColumn(_columnManager, _pc);

		// show avg speed to verify the tour type by speed
		colDef.setIsDefaultColumn();

		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final TourData tourData = ((TourData) cell.getElement());
				final float tourDistance = tourData.getTourDistance();
				final long drivingTime = tourData.getTourDrivingTime();

				double speed = 0;

				if (drivingTime != 0) {
					speed = tourDistance / drivingTime * 3.6 / net.tourbook.ui.UI.UNIT_VALUE_DISTANCE;
				}

				cell.setText(speed == 0.0 ? UI.EMPTY_STRING : _nf1.format(speed));
			}
		});
	}

	/**
	 * column: calories (cal)
	 */
	private void defineColumnCalories() {

		final ColumnDefinition colDef = TableColumnFactory.CALORIES.createColumn(_columnManager, _pc);

		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final int tourCalories = ((TourData) cell.getElement()).getCalories();

				cell.setText(tourCalories == 0 ? UI.EMPTY_STRING : Integer.toString(tourCalories));
			}
		});
	}

	/**
	 * column: database indicator
	 */
	private void defineColumnDatabase() {

		final ColumnDefinition colDef = TableColumnFactory.DB_STATUS.createColumn(_columnManager, _pc);

		colDef.setIsDefaultColumn();
		colDef.setCanModifyVisibility(false);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
				// show the database indicator for the person who owns the tour
				cell.setImage(getDbImage((TourData) cell.getElement()));
			}
		});
	}

	/**
	 * column: date
	 */
	private void defineColumnDate() {

		final ColumnDefinition colDef = TableColumnFactory.TOUR_DATE.createColumn(_columnManager, _pc);

		colDef.setIsDefaultColumn();
		colDef.setCanModifyVisibility(false);
		colDef.setLabelProvider(new TourInfoToolTipCellLabelProvider() {

			@Override
			public Long getTourId(final ViewerCell cell) {

				if (_isToolTipInDate == false) {
					return null;
				}

				return ((TourData) cell.getElement()).getTourId();
			}

			@Override
			public void update(final ViewerCell cell) {

				final TourData tourData = (TourData) cell.getElement();

				cell.setText(_dateFormatter.format(tourData.getTourStartTimeMS()));
			}
		});

		// sort column
		colDef.setColumnSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent event) {
				((DeviceImportSorter) _tourViewer.getSorter()).doSort(COLUMN_DATE);
				_tourViewer.refresh();
			}
		});
	}

	/**
	 * column: device name
	 */
	private void defineColumnDeviceName() {

		final ColumnDefinition colDef = TableColumnFactory.DEVICE_NAME.createColumn(_columnManager, _pc);

		colDef.setColumnSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent event) {
				((DeviceImportSorter) _tourViewer.getSorter()).doSort(COLUMN_DATA_FORMAT);
				_tourViewer.refresh();
			}
		});
	}

	/**
	 * column: device profile
	 */
	private void defineColumnDeviceProfile() {

		TableColumnFactory.DEVICE_PROFILE.createColumn(_columnManager, _pc);
	}

	/**
	 * column: distance (km/mile)
	 */
	private void defineColumnDistance() {

		final ColumnDefinition colDef = TableColumnFactory.DISTANCE.createColumn(_columnManager, _pc);

		colDef.setIsDefaultColumn();
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
				final float tourDistance = ((TourData) cell.getElement()).getTourDistance();
				if (tourDistance == 0) {
					cell.setText(UI.EMPTY_STRING);
				} else {
					cell.setText(_nf3.format(tourDistance / 1000 / net.tourbook.ui.UI.UNIT_VALUE_DISTANCE));
				}
			}
		});
	}

	/**
	 * column: driving time
	 */
	private void defineColumnDrivingTime() {

		final ColumnDefinition colDef = TableColumnFactory.DRIVING_TIME.createColumn(_columnManager, _pc);

		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final int drivingTime = (int) ((TourData) cell.getElement()).getTourDrivingTime();

				if (drivingTime != 0) {
					_calendar
							.set(0, 0, 0, drivingTime / 3600, ((drivingTime % 3600) / 60), ((drivingTime % 3600) % 60));

					cell.setText(_durationFormatter.format(_calendar.getTime()));
				}
			}
		});
	}

	/**
	 * column: import file name
	 */
	private void defineColumnImportFileName() {

		final ColumnDefinition colDef = TableColumnFactory.IMPORT_FILE_NAME.createColumn(_columnManager, _pc);

		colDef.setColumnSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent event) {
				((DeviceImportSorter) _tourViewer.getSorter()).doSort(COLUMN_FILE_NAME);
				_tourViewer.refresh();
			}
		});
	}

	/**
	 * column: import file path
	 */
	private void defineColumnImportFilePath() {
		TableColumnFactory.IMPORT_FILE_PATH.createColumn(_columnManager, _pc);
	}

	/**
	 * column: markers
	 */
	private void defineColumnMarker() {

		final ColumnDefinition colDef = TableColumnFactory.TOUR_MARKERS.createColumn(_columnManager, _pc);

		colDef.setIsDefaultColumn();
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final TourData tourData = (TourData) cell.getElement();

				final Set<TourMarker> tourMarker = tourData.getTourMarkers();
				final Set<TourWayPoint> wayPoints = tourData.getTourWayPoints();

				if (tourMarker == null && wayPoints == null) {
					cell.setText(UI.EMPTY_STRING);
				} else {

					int size = 0;
					if (tourMarker != null) {
						size = tourMarker.size();
					}
					if (wayPoints != null) {
						size += wayPoints.size();
					}
					cell.setText(size == 0 ? UI.EMPTY_STRING : Integer.toString(size));
				}
			}
		});
	}

	/**
	 * column: recording time
	 */
	private void defineColumnRecordingTime() {

		final ColumnDefinition colDef = TableColumnFactory.RECORDING_TIME.createColumn(_columnManager, _pc);

		colDef.setIsDefaultColumn();
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final int recordingTime = (int) ((TourData) cell.getElement()).getTourRecordingTime();

				if (recordingTime != 0) {
					_calendar.set(
							0,
							0,
							0,
							recordingTime / 3600,
							((recordingTime % 3600) / 60),
							((recordingTime % 3600) % 60));

					cell.setText(_durationFormatter.format(_calendar.getTime()));
				}
			}
		});
	}

	/**
	 * column: tags
	 */
	private void defineColumnTags() {

		final ColumnDefinition colDef = TableColumnFactory.TOUR_TAGS.createColumn(_columnManager, _pc);

		colDef.setIsDefaultColumn();
		colDef.setLabelProvider(new TourInfoToolTipCellLabelProvider() {

			@Override
			public Long getTourId(final ViewerCell cell) {

				if (_isToolTipInTags == false) {
					return null;
				}

				return ((TourData) cell.getElement()).getTourId();
			}

			@Override
			public void update(final ViewerCell cell) {

				final Object element = cell.getElement();
				final TourData tourData = (TourData) element;

				final Set<TourTag> tourTags = tourData.getTourTags();

				if (tourTags.size() == 0) {

					// the tags could have been removed, set empty field

					cell.setText(UI.EMPTY_STRING);

				} else {

					// convert the tags into a list of tag ids

					cell.setText(TourDatabase.getTagNames(tourTags));
				}
			}
		});
	}

	/**
	 * column: time
	 */
	private void defineColumnTime() {

		final ColumnDefinition colDef = TableColumnFactory.TOUR_START_TIME.createColumn(_columnManager, _pc);

		colDef.setIsDefaultColumn();
		colDef.setCanModifyVisibility(false);
		colDef.setLabelProvider(new TourInfoToolTipCellLabelProvider() {

			@Override
			public Long getTourId(final ViewerCell cell) {

				if (_isToolTipInTime == false) {
					return null;
				}

				return ((TourData) cell.getElement()).getTourId();
			}

			@Override
			public void update(final ViewerCell cell) {

				final TourData tourData = (TourData) cell.getElement();

				cell.setText(_timeFormatter.format(tourData.getTourStartTimeMS()));
			}
		});

		// sort column
		colDef.setColumnSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent event) {
				((DeviceImportSorter) _tourViewer.getSorter()).doSort(COLUMN_DATE);
				_tourViewer.refresh();
			}
		});
	}

	/**
	 * column: time interval
	 */
	private void defineColumnTimeInterval() {

		TableColumnFactory.TIME_INTERVAL.createColumn(_columnManager, _pc);
	}

	/**
	 * column: tour title
	 */
	private void defineColumnTitle() {

		final ColumnDefinition colDef = TableColumnFactory.TOUR_TITLE.createColumn(_columnManager, _pc);

		colDef.setIsDefaultColumn();
		colDef.setLabelProvider(new TourInfoToolTipCellLabelProvider() {

			@Override
			public Long getTourId(final ViewerCell cell) {

				if (_isToolTipInTitle == false) {
					return null;
				}

				return ((TourData) cell.getElement()).getTourId();
			}

			@Override
			public void update(final ViewerCell cell) {
				final TourData tourData = (TourData) cell.getElement();
				cell.setText(tourData.getTourTitle());
			}
		});
		colDef.setColumnSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent event) {
				((DeviceImportSorter) _tourViewer.getSorter()).doSort(COLUMN_TITLE);
				_tourViewer.refresh();
			}
		});
	}

	/**
	 * column: tour type image
	 */
	private void defineColumnTourType() {

		final ColumnDefinition colDef = TableColumnFactory.TOUR_TYPE.createColumn(_columnManager, _pc);

		colDef.setIsDefaultColumn();
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final net.tourbook.ui.UI ui = net.tourbook.ui.UI.getInstance();

				final TourType tourType = ((TourData) cell.getElement()).getTourType();

				if (tourType == null) {
					cell.setImage(ui.getTourTypeImage(TourDatabase.ENTITY_IS_NOT_SAVED));
				} else {

					final long tourTypeId = tourType.getTypeId();
					final Image tourTypeImage = ui.getTourTypeImage(tourTypeId);

					/*
					 * when a tour type image is modified, it will keep the same image resource only
					 * the content is modified but in the rawDataView the modified image is not
					 * displayed compared with the tourBookView which displays the correct image
					 */
					cell.setImage(tourTypeImage);
				}
			}
		});
	}

	/**
	 * column: tour type text
	 */
	private void defineColumnTourTypeText() {

		final ColumnDefinition colDef = TableColumnFactory.TOUR_TYPE_TEXT.createColumn(_columnManager, _pc);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final TourType tourType = ((TourData) cell.getElement()).getTourType();
				if (tourType == null) {
					cell.setText(UI.EMPTY_STRING);
				} else {
					cell.setText(tourType.getName());
				}
			}
		});
	}

	/**
	 * column: clouds
	 */
	private void defineColumnWeatherClouds() {

		final ColumnDefinition colDef = TableColumnFactory.CLOUDS.createColumn(_columnManager, _pc);

		colDef.setIsDefaultColumn();
		colDef.setLabelProvider(new CellLabelProvider() {

			@Override
			public void update(final ViewerCell cell) {

				final String weatherCloudId = ((TourData) cell.getElement()).getWeatherClouds();
				if (weatherCloudId == null) {
					cell.setText(UI.EMPTY_STRING);
				} else {
					final Image img = UI.IMAGE_REGISTRY.get(weatherCloudId);
					if (img != null) {
						cell.setImage(img);
					} else {
						cell.setText(weatherCloudId);
					}
				}
			}
		});
	}

	private void deleteFile(final ArrayList<String> deletedFiles,
							final ArrayList<String> notDeletedFiles,
							final String fileFolder,
							final String fileName) {

		if (fileFolder == null || fileFolder.trim().length() == 0) {
			// there is no folder
			return;
		}

		Path filePath = null;

		try {

			filePath = Paths.get(fileFolder, fileName);

			Files.delete(filePath);

			deletedFiles.add(filePath.toString());

		} catch (final Exception e) {

			// file can be invalid

			notDeletedFiles.add('"' + fileFolder + '"' + UI.SPACE + '"' + fileName + '"');
		}
	}

	@Override
	public void dispose() {

		resetEasyImport();

		Util.disposeResource(_imageDatabase);
		Util.disposeResource(_imageDatabaseOtherPerson);
		Util.disposeResource(_imageDatabaseAssignMergedTour);
		Util.disposeResource(_imageDatabasePlaceholder);
		Util.disposeResource(_imageDelete);

		// don't throw the selection again
		_postSelectionProvider.clearSelection();

		getViewSite().getPage().removePartListener(_partListener);
		getSite().getPage().removeSelectionListener(_postSelectionListener);

		TourManager.getInstance().removeTourEventListener(_tourEventListener);

		_prefStore.removePropertyChangeListener(_prefChangeListener);

		disposeConfigImages();

		super.dispose();
	}

	private void disposeConfigImages() {

		for (final Image configImage : _configImages.values()) {

			if (configImage != null) {
				configImage.dispose();
			}
		}

		_configImages.clear();
		_configImageHash.clear();
	}

	/**
	 * @param isDeleteAllFiles
	 *            When <code>true</code> then all files (device and backup) will be deleted,
	 *            otherwise only device files will be deleted without any confirmation dialog and
	 *            the backup files are not touched.
	 */
	private void doDeleteTourFiles(final boolean isDeleteAllFiles) {

		if (isDeleteAllFiles) {

			if (MessageDialog.openConfirm(
					_parent.getShell(),
					Messages.Import_Data_Dialog_DeleteTourFiles_Title,
					Messages.Import_Data_Dialog_DeleteTourFiles_Message) == false) {
				return;
			}

			if (MessageDialog.openConfirm(
					_parent.getShell(),
					Messages.Import_Data_Dialog_DeleteTourFiles_Title,
					Messages.Import_Data_Dialog_DeleteTourFiles_LastChance_Message) == false) {
				return;
			}
		}

		final ArrayList<String> deletedFiles = new ArrayList<>();
		final ArrayList<String> notDeletedFiles = new ArrayList<>();
		final ArrayList<TourData> selectedTours = getAnySelectedTours();

		final IRunnableWithProgress saveRunnable = new IRunnableWithProgress() {
			@Override
			public void run(final IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {

				int saveCounter = 0;

				final int selectionSize = selectedTours.size();

				monitor.beginTask(Messages.Import_Data_Monitor_DeleteTourFiles, selectionSize);

				// loop: all selected tours, selected tours can already be saved
				for (final TourData tourData : selectedTours) {

					monitor.subTask(NLS.bind(
							Messages.Import_Data_Monitor_DeleteTourFiles_Subtask,
							++saveCounter,
							selectionSize));

					final String originalFilePath = tourData.importFilePathOriginal;

					// this is the backup folder when an backup is created
					final String importFilePath = tourData.getImportFilePath();
					final String importFileName = tourData.getImportFileName();

					// delete backup files
					if (isDeleteAllFiles) {
						deleteFile(deletedFiles, notDeletedFiles, importFilePath, importFileName);
					}

					// delete device files
					deleteFile(deletedFiles, notDeletedFiles, originalFilePath, importFileName);

					monitor.worked(1);
				}
			}
		};

		try {

			new ProgressMonitorDialog(_parent.getShell()).run(true, false, saveRunnable);

		} catch (InvocationTargetException | InterruptedException e) {
			StatusUtil.showStatus(e);
		}

		/*
		 * Log deleted files
		 */
		final StringBuilder sb = new StringBuilder();
		sb.append("DELETED TOUR FILES LOG");//$NON-NLS-1$

		sb.append(UI.NEW_LINE2);
		createFilesLog(sb, deletedFiles, "Deleted tour files: ");//$NON-NLS-1$

		sb.append(UI.NEW_LINE2);
		createFilesLog(sb, notDeletedFiles, "Not deleted tour files with errors: ");//$NON-NLS-1$

		StatusUtil.logInfo(sb.toString());

		if (isDeleteAllFiles) {

			MessageDialog.openInformation(
					_parent.getShell(),
					Messages.Import_Data_Dialog_DeleteTourFiles_Title,
					NLS.bind(Messages.Import_Data_Dialog_DeletedImportFiles_Message, deletedFiles.size()));
		}
	}

	public void doLiveUpdate(final DialogEasyImportConfig dialogImportConfig) {

		updateModel_EasyConfig_Dashboard(dialogImportConfig.getModifiedConfig());

		_isDeviceStateValid = false;
		updateUI_2_Dashboard();
	}

	private void doSaveTour(final TourPerson person) {

		final ArrayList<TourData> selectedTours = getAnySelectedTours();

		doSaveTour(person, selectedTours);
	}

	private void doSaveTour(final TourPerson person, final ArrayList<TourData> selectedTours) {

		final ArrayList<TourData> savedTours = new ArrayList<TourData>();

		final IRunnableWithProgress saveRunnable = new IRunnableWithProgress() {
			@Override
			public void run(final IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {

				int saveCounter = 0;
				final int selectionSize = selectedTours.size();

				monitor.beginTask(Messages.Tour_Data_SaveTour_Monitor, selectionSize);

				// loop: all selected tours, selected tours can already be saved
				for (final TourData tourData : selectedTours) {

					monitor.subTask(NLS.bind(Messages.Tour_Data_SaveTour_MonitorSubtask, ++saveCounter, selectionSize));

					doSaveTour_OneTour(tourData, person, savedTours, false);

					monitor.worked(1);
				}
			}
		};

		try {

			new ProgressMonitorDialog(Display.getCurrent().getActiveShell()).run(true, false, saveRunnable);

		} catch (InvocationTargetException | InterruptedException e) {
			StatusUtil.showStatus(e);
		}

		doSaveTour_PostActions(savedTours);
	}

	/**
	 * @param tourData
	 *            {@link TourData} which is not yet saved
	 * @param person
	 *            person for which the tour is being saved
	 * @param savedTours
	 *            the saved tour is added to this list
	 */
	private void doSaveTour_OneTour(final TourData tourData,
									final TourPerson person,
									final ArrayList<TourData> savedTours,
									final boolean isForceSave) {

		// workaround for hibernate problems
		if (tourData.isTourDeleted) {
			return;
		}

		if ((tourData.getTourPerson() != null) && (isForceSave == false)) {
			/*
			 * tour is already saved, resaving cannot be done in the import view it can be done in
			 * the tour editor
			 */
			return;
		}

		tourData.setTourPerson(person);
		tourData.setBikerWeight(person.getWeight());
		tourData.setTourBike(person.getTourBike());

		final TourData savedTour = TourDatabase.saveTour(tourData, true);
		if (savedTour != null) {
			savedTours.add(savedTour);
		}
	}

	/**
	 * After tours are saved, the internal structures and ui viewers must be updated
	 * 
	 * @param savedTours
	 *            contains the saved {@link TourData}
	 */
	private void doSaveTour_PostActions(final ArrayList<TourData> savedTours) {

		// update viewer, fire selection event
		if (savedTours.size() == 0) {
			return;
		}

		final ArrayList<Long> savedToursIds = new ArrayList<Long>();

		// update raw data map with the saved tour data
		final HashMap<Long, TourData> rawDataMap = _rawDataMgr.getImportedTours();
		for (final TourData tourData : savedTours) {

			final Long tourId = tourData.getTourId();

			rawDataMap.put(tourId, tourData);
			savedToursIds.add(tourId);
		}

		/*
		 * the selection provider can contain old tour data which conflicts with the tour data in
		 * the tour data editor
		 */
		_postSelectionProvider.clearSelection();

		// update import viewer
		reloadViewer();

		enableActions();

		/*
		 * notify all views, it is not checked if the tour data editor is dirty because newly saved
		 * tours can not be modified in the tour data editor
		 */
		TourManager.fireEventWithCustomData(TourEventId.UPDATE_UI, new SelectionTourIds(savedToursIds), this);
	}

	private void doSetLastMarker(final ImportLauncher importLauncher, final ArrayList<TourData> importedTours) {

		final String lastMarkerText = importLauncher.lastMarkerText;
		if (lastMarkerText == null || lastMarkerText.trim().length() == 0) {
			// there is nothing to do
			return;
		}

		final int ilLastMarkerDistance = importLauncher.lastMarkerDistance;

		for (final TourData tourData : importedTours) {

			// check if distance is available
			final float[] distancSerie = tourData.distanceSerie;
			if (distancSerie == null || distancSerie.length == 0) {
				continue;
			}

			final ArrayList<TourMarker> tourMarkers = tourData.getTourMarkersSorted();
			final int numMarkers = tourMarkers.size();

			// check if markers are available
			if (numMarkers == 0) {
				continue;
			}

			// get last marker
			final TourMarker lastMarker = tourMarkers.get(numMarkers - 1);

			final int markerIndex = lastMarker.getSerieIndex();

			final float lastMarkerDistance = distancSerie[markerIndex];
			final float tourDistance = tourData.getTourDistance();
			final float distanceDiff = tourDistance - lastMarkerDistance;

			if (distanceDiff <= ilLastMarkerDistance) {

				// this marker is in the range of the last marker distance -> set the tour marker text

				lastMarker.setLabel(lastMarkerText);
			}
		}
	}

	private void enableActions() {

		final Object[] rawData = _rawDataMgr.getImportedTours().values().toArray();
		final boolean isTourAvailable = rawData.length > 0;

		final StructuredSelection selection = (StructuredSelection) _tourViewer.getSelection();

		int savedTours = 0;
		int unsavedTours = 0;
		int selectedTours = 0;

		// contains all tours which are selected and not deleted
		int selectedNotDeleteTours = 0;

		TourData firstSavedTour = null;
		TourData firstValidTour = null;

		for (final Iterator<?> iter = selection.iterator(); iter.hasNext();) {
			final Object treeItem = iter.next();
			if (treeItem instanceof TourData) {

				selectedTours++;

				final TourData tourData = (TourData) treeItem;
				if (tourData.getTourPerson() == null) {

					// tour is not saved

					if (tourData.isTourDeleted == false) {

						// tour is not deleted, deleted tours are ignored

						unsavedTours++;
						selectedNotDeleteTours++;
					}

				} else {

					if (savedTours == 0) {
						firstSavedTour = tourData;
					}

					savedTours++;
					selectedNotDeleteTours++;
				}

				if (selectedNotDeleteTours == 1) {
					firstValidTour = tourData;
				}
			}
		}

		final boolean isSavedTourSelected = savedTours > 0;
		final boolean isOneSavedAndNotDeleteTour = (selectedNotDeleteTours == 1) && (savedTours == 1);

		final boolean isOneSelectedNotDeleteTour = selectedNotDeleteTours == 1;

		// action: save tour with person
		final TourPerson person = TourbookPlugin.getActivePerson();
		if (person != null) {
			_actionSaveTourWithPerson.setText(NLS.bind(
					Messages.import_data_action_save_tour_with_person,
					person.getName()));
			_actionSaveTourWithPerson.setPerson(person);
		}
		_actionSaveTourWithPerson.setEnabled((person != null) && (unsavedTours > 0));

		// action: save tour...
		if (selection.size() == 1) {
			_actionSaveTour.setText(Messages.import_data_action_save_tour_for_person);
		} else {
			_actionSaveTour.setText(Messages.import_data_action_save_tours_for_person);
		}
		_actionSaveTour.setEnabled(unsavedTours > 0);

		// action: merge tour ... into ...
		if (isOneSelectedNotDeleteTour) {

			final StringBuilder sb = new StringBuilder().append(UI.EMPTY_STRING)//
					.append(TourManager.getTourDateShort(firstValidTour))
					.append(UI.DASH_WITH_SPACE)
					.append(TourManager.getTourTimeShort(firstValidTour))
					.append(UI.DASH_WITH_SPACE)
					.append(firstValidTour.getDeviceName());

			_actionMergeIntoTour.setText(NLS.bind(Messages.import_data_action_assignMergedTour, sb.toString()));

		} else {
			// tour cannot be merged, display default text
			_actionMergeIntoTour.setText(Messages.import_data_action_assignMergedTour_default);
		}
		_actionMergeIntoTour.setEnabled(isOneSelectedNotDeleteTour);

		_actionMergeTour.setEnabled(isOneSavedAndNotDeleteTour && (firstSavedTour.getMergeSourceTourId() != null));
		_actionReimportSubMenu.setEnabled(selectedTours > 0);
		_actionRemoveTour.setEnabled(selectedTours > 0);
		_actionExportTour.setEnabled(selectedNotDeleteTours > 0);
		_actionJoinTours.setEnabled(selectedNotDeleteTours > 1);

		_actionEditTour.setEnabled(isOneSavedAndNotDeleteTour);
		_actionEditQuick.setEnabled(isOneSavedAndNotDeleteTour);
		_actionOpenTour.setEnabled(isOneSavedAndNotDeleteTour);
		_actionOpenMarkerDialog.setEnabled(isOneSavedAndNotDeleteTour);
		_actionOpenAdjustAltitudeDialog.setEnabled(isOneSavedAndNotDeleteTour);

		_actionDeleteTourFile.setEnabled(isTourAvailable);

		// set double click state
		_tourDoubleClickState.canEditTour = isOneSavedAndNotDeleteTour;
		_tourDoubleClickState.canQuickEditTour = isOneSavedAndNotDeleteTour;
		_tourDoubleClickState.canEditMarker = isOneSavedAndNotDeleteTour;
		_tourDoubleClickState.canAdjustAltitude = isOneSavedAndNotDeleteTour;
		_tourDoubleClickState.canOpenTour = isOneSelectedNotDeleteTour;

		final ArrayList<TourType> tourTypes = TourDatabase.getAllTourTypes();
		_actionSetTourType.setEnabled(isSavedTourSelected && (tourTypes.size() > 0));

		final ArrayList<Long> existingTagIds = new ArrayList<Long>();
		long existingTourTypeId = TourDatabase.ENTITY_IS_NOT_SAVED;
		boolean isOneTour;

		if ((firstSavedTour != null) && (savedTours == 1)) {

			// one tour is selected

			isOneTour = true;

			final TourType tourType = firstSavedTour.getTourType();
			existingTourTypeId = tourType == null ? TourDatabase.ENTITY_IS_NOT_SAVED : tourType.getTypeId();

			final Set<TourTag> existingTags = firstSavedTour.getTourTags();
			if ((existingTags != null) && (existingTags.size() > 0)) {

				// tour contains at least one tag
				for (final TourTag tourTag : existingTags) {
					existingTagIds.add(tourTag.getTagId());
				}
			}
		} else {

			// multiple tours are selected

			isOneTour = false;
		}

		// enable/disable actions for tags/tour types
		_tagMenuMgr.enableTagActions(isSavedTourSelected, isOneTour, existingTagIds);
		TourTypeMenuManager.enableRecentTourTypeActions(isSavedTourSelected, existingTourTypeId);

		/*
		 * Action: Setup import
		 */
		_actionSetupImport.setEnabled(!isTourAvailable);
		_actionClearView.setEnabled(isTourAvailable);
	}

	private void fillContextMenu(final IMenuManager menuMgr) {

		// hide tour info tooltip, this is displayed when the mouse context menu should be created
		_tourInfoToolTip.hide();

		if (TourbookPlugin.getActivePerson() != null) {
			menuMgr.add(_actionSaveTourWithPerson);
		}
		menuMgr.add(_actionSaveTour);
		menuMgr.add(_actionMergeIntoTour);
		menuMgr.add(_actionJoinTours);

		menuMgr.add(new Separator());
		menuMgr.add(_actionExportTour);
		menuMgr.add(_actionReimportSubMenu);
		menuMgr.add(_actionEditImportPreferences);
		menuMgr.add(_actionRemoveTour);
		menuMgr.add(_actionDeleteTourFile);

		menuMgr.add(new Separator());
		menuMgr.add(_actionEditQuick);
		menuMgr.add(_actionEditTour);
		menuMgr.add(_actionOpenMarkerDialog);
		menuMgr.add(_actionOpenAdjustAltitudeDialog);
		menuMgr.add(_actionMergeTour);
		menuMgr.add(_actionOpenTour);

		// tour type actions
		menuMgr.add(new Separator());
		menuMgr.add(_actionSetTourType);
		TourTypeMenuManager.fillMenuWithRecentTourTypes(menuMgr, this, true);

		// tour tag actions
		_tagMenuMgr.fillTagMenu(menuMgr);

		// add standard group which allows other plug-ins to contribute here
		menuMgr.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));

		enableActions();
	}

	private void fillToolbar() {

		/*
		 * fill view toolbar
		 */
		final IToolBarManager tbm = getViewSite().getActionBars().getToolBarManager();

		tbm.add(_actionSaveTourWithPerson);
		tbm.add(_actionSaveTour);
		tbm.add(new Separator());

		// place for import and transfer actions
		tbm.add(new GroupMarker("import")); //$NON-NLS-1$
		tbm.add(new Separator());

		tbm.add(_actionClearView);
		tbm.add(_actionSetupImport);

		/*
		 * fill view menu
		 */
		final IMenuManager menuMgr = getViewSite().getActionBars().getMenuManager();

		menuMgr.add(_actionRemoveToursWhenClosed);
		menuMgr.add(_actionEditImportPreferences);

		menuMgr.add(new Separator());
		menuMgr.add(_actionModifyColumns);
	}

	private void fireSelectedTour() {

		final IStructuredSelection selection = (IStructuredSelection) _tourViewer.getSelection();
		final TourData tourData = (TourData) selection.getFirstElement();

		enableActions();

		if (tourData != null) {
			_postSelectionProvider.setSelection(new SelectionTourData(null, tourData));
		}
	}

	@Override
	public ArrayList<TourData> getAllSelectedTours() {

		final TourManager tourManager = TourManager.getInstance();

		// get selected tours
		final IStructuredSelection selectedTours = ((IStructuredSelection) _tourViewer.getSelection());

		final ArrayList<TourData> selectedTourData = new ArrayList<TourData>();

		// loop: all selected tours
		for (final Iterator<?> iter = selectedTours.iterator(); iter.hasNext();) {

			final Object tourItem = iter.next();

			if (tourItem instanceof TourData) {

				final TourData tourData = (TourData) tourItem;

				if (tourData.isTourDeleted) {
					// skip deleted tour
					continue;
				}

				if (tourData.getTourPerson() == null) {

					// tour is not saved
					selectedTourData.add(tourData);

				} else {

					/*
					 * get the data from the database because the tag names could be changed and
					 * this is not reflected in the tours which are displayed in the raw data view
					 */

					final TourData tourDataInDb = tourManager.getTourData(tourData.getTourId());

					if (tourDataInDb != null) {
						selectedTourData.add(tourDataInDb);
					}
				}
			}
		}

		return selectedTourData;
	}

	private ArrayList<TourData> getAnySelectedTours() {

		final ArrayList<TourData> selectedTours = new ArrayList<TourData>();

		// get selected tours, this must be outside of the runnable !!!
		final IStructuredSelection selection = ((IStructuredSelection) _tourViewer.getSelection());

		for (final Iterator<?> iterator = selection.iterator(); iterator.hasNext();) {
			selectedTours.add((TourData) iterator.next());
		}

		return selectedTours;
	}

	@Override
	public ColumnManager getColumnManager() {
		return _columnManager;
	}

	Image getDbImage(final TourData tourData) {
		final TourPerson tourPerson = tourData.getTourPerson();
		final long activePersonId = _activePerson == null ? -1 : _activePerson.getPersonId();

		final Image dbImage = tourData.isTourDeleted ? //
				_imageDelete
				: tourData.getMergeTargetTourId() != null ? //
						_imageDatabaseAssignMergedTour
						: tourPerson == null ? _imageDatabasePlaceholder : tourPerson.getPersonId() == activePersonId
								? _imageDatabase
								: _imageDatabaseOtherPerson;
		return dbImage;
	}

	private EasyConfig getEasyConfig() {

		return EasyImportManager.getInstance().getEasyConfig();
	}

	public Image getImportConfigImage(final ImportLauncher importConfig) {

		final int imageWidth = importConfig.imageWidth;

		if (imageWidth == 0) {
			return null;
		}

		final long configId = importConfig.getId();
		Image image = _configImages.get(configId);

		if (isConfigImageValid(image, importConfig)) {
			return image;
		}

		final Display display = _parent.getDisplay();

		final Enum<TourTypeConfig> tourTypeConfig = importConfig.tourTypeConfig;
		final net.tourbook.ui.UI ui = net.tourbook.ui.UI.getInstance();

		if (TourTypeConfig.TOUR_TYPE_CONFIG_BY_SPEED.equals(tourTypeConfig)) {

			final ArrayList<SpeedTourType> speedVertices = importConfig.speedTourTypes;
			final int imageSize = TourType.TOUR_TYPE_IMAGE_SIZE;

			final Image tempImage = new Image(display, imageWidth, imageSize);
			{
				final GC gcImage = new GC(tempImage);
				final Color colorTransparent = new Color(display, TourType.TRANSPARENT_COLOR);
				{
					// fill with transparent color
					gcImage.setBackground(colorTransparent);
					gcImage.fillRectangle(0, 0, imageWidth, imageSize);

					for (int imageIndex = 0; imageIndex < speedVertices.size(); imageIndex++) {

						final SpeedTourType vertex = speedVertices.get(imageIndex);

						final Image ttImage = ui.getTourTypeImage(vertex.tourTypeId);

						gcImage.drawImage(ttImage, //
								0,
								0,
								imageSize,
								imageSize,

								imageSize * imageIndex,
								0,
								imageSize,
								imageSize);
					}
				}
				gcImage.dispose();
				colorTransparent.dispose();

				/*
				 * set transparency
				 */
				final ImageData imageData = tempImage.getImageData();
				imageData.transparentPixel = imageData.getPixel(0, 0);

				image = new Image(display, imageData);
			}
			tempImage.dispose();

		} else if (TourTypeConfig.TOUR_TYPE_CONFIG_ONE_FOR_ALL.equals(tourTypeConfig)) {

			final TourType tourType = importConfig.oneTourType;

			if (tourType != null) {

				// create a copy because the copied image can be disposed
				final Image tempImage = new Image(display, TourType.TOUR_TYPE_IMAGE_SIZE, TourType.TOUR_TYPE_IMAGE_SIZE);
				{

					final GC gcImage = new GC(tempImage);
					final Color colorTransparent = new Color(display, TourType.TRANSPARENT_COLOR);
					{
						// fill with transparent color
						gcImage.setBackground(colorTransparent);
						gcImage.fillRectangle(0, 0, TourType.TOUR_TYPE_IMAGE_SIZE, TourType.TOUR_TYPE_IMAGE_SIZE);

						final Image ttImage = ui.getTourTypeImage(tourType.getTypeId());
						gcImage.drawImage(ttImage, 0, 0);
					}
					gcImage.dispose();
					colorTransparent.dispose();

					/*
					 * set transparency
					 */
					final ImageData imageData = tempImage.getImageData();
					imageData.transparentPixel = imageData.getPixel(0, 0);

					image = new Image(display, imageData);

				}
				tempImage.dispose();
			}

		} else {

			// this is the default or TourTypeConfig.TOUR_TYPE_CONFIG_NOT_USED

		}

		// keep image in the cache
		final Image oldImage = _configImages.put(configId, image);

		Util.disposeResource(oldImage);

		_configImageHash.put(configId, importConfig.imageHash);

		return image;
	}

	@Override
	public PostSelectionProvider getPostSelectionProvider() {
		return _postSelectionProvider;
	}

	@Override
	public ArrayList<TourData> getSelectedTours() {

		final TourManager tourManager = TourManager.getInstance();

		// get selected tours
		final IStructuredSelection selectedTours = ((IStructuredSelection) _tourViewer.getSelection());

		final ArrayList<TourData> selectedTourData = new ArrayList<TourData>();

		// loop: all selected tours
		for (final Iterator<?> iter = selectedTours.iterator(); iter.hasNext();) {

			final Object tourItem = iter.next();

			if (tourItem instanceof TourData) {

				final TourData tourData = (TourData) tourItem;

				/*
				 * only tours are added which are saved in the database
				 */
				if (tourData.getTourPerson() != null) {

					/*
					 * get the data from the database because the tag names could be changed and
					 * this is not reflected in the tours which are displayed in the raw data view
					 */
					final TourData tourDataInDb = tourManager.getTourData(tourData.getTourId());
					if (tourDataInDb != null) {
						selectedTourData.add(tourDataInDb);
					}
				}
			}
		}

		return selectedTourData;
	}

	@Override
	public ColumnViewer getViewer() {
		return _tourViewer;
	}

	private void initUI(final Composite parent) {

		_parent = parent;
		_pc = new PixelConverter(parent);

		createResources_Image();
		createResources_Web();
	}

	/**
	 * @param image
	 * @param importConfig
	 * @return Returns <code>true</code> when the image is valid, returns <code>false</code> when
	 *         the profile image must be created,
	 */
	private boolean isConfigImageValid(final Image image, final ImportLauncher importConfig) {

		if (image == null || image.isDisposed()) {

			return false;
		}

		final Integer imageHash = _configImageHash.get(importConfig.getId());

		if (imageHash == null || imageHash != importConfig.imageHash) {

			image.dispose();

			return false;
		}

		return true;
	}

	private boolean isOSFolderValid(final String osFolder) {

		try {

			if (osFolder != null && osFolder.trim().length() > 0 && Files.exists(Paths.get(osFolder))) {

				// device folder exists
				return true;
			}

		} catch (final Exception e) {}

		return false;
	}

	/**
	 * @return Returns <code>true</code> when the devices are watched.
	 */
	private boolean isWatchingOn() {

		return _watchingStoresThread != null;
	}

	private void onBrowser_Completed(final ProgressEvent event) {

		_isBrowserCompleted = true;

		if (_isInUIStartup) {

			_isInUIStartup = false;

			// a redraw MUST be done otherwise nothing is displayed
			_browser.setRedraw(true);

			// set focus that clicking on an action works the 1st and not the 2nd time
			_browser.setFocus();

			// dashboard is visible, activate background task
			setWatcher_On();

			// make the import tiles visible otherwise they are 'hidden' after the startup
			_isShowWatcherAnimation = true;
			updateUI_WatcherAnimation(isWatchingOn() //
					? DOM_CLASS_DEVICE_ON_ANIMATED
					: DOM_CLASS_DEVICE_OFF_ANIMATED);
		}

		if (_isDeviceStateUpdateDelayed.getAndSet(false)) {
			updateUI_DeviceState_DOM();
		}
	}

	private void onBrowser_LocationChanging(final LocationEvent event) {

		final String location = event.location;

		final String[] locationParts = location.split(HREF_TOKEN);

		if (locationParts.length > 1) {

			// finalize loading of the browser page and then start the action
			_browser.getDisplay().asyncExec(new Runnable() {
				@Override
				public void run() {
					onBrowser_LocationChanging_Runnable(locationParts);
				}
			});
		}

		// prevent to load a new url
		if (location.equals(PAGE_ABOUT_BLANK) == false) {

			// about:blank is the initial page

			event.doit = false;
		}
	}

	private void onBrowser_LocationChanging_Runnable(final String[] locationParts) {

		final String hrefAction = locationParts[1];

		if (ACTION_DEVICE_IMPORT.equals(hrefAction)) {

			final long tileId = Long.parseLong(locationParts[2]);

			action_Easy_RunImport(tileId);

		} else if (ACTION_SETUP_EASY_IMPORT.equals(hrefAction)) {

			action_Easy_SetupImport(-1);

		} else if (ACTION_DEVICE_WATCHING_ON_OFF.equals(hrefAction)) {

			action_Easy_SetDeviceWatching_OnOff();

		} else if (ACTION_IMPORT_FROM_FILES.equals(hrefAction)) {

			_rawDataMgr.actionImportFromFile();

		} else if (ACTION_SERIAL_PORT_CONFIGURED.equals(hrefAction)) {

			_rawDataMgr.actionImportFromDevice();

		} else if (ACTION_SERIAL_PORT_DIRECTLY.equals(hrefAction)) {

			_rawDataMgr.actionImportFromDeviceDirect();

		} else if (ACTION_OLD_UI.equals(hrefAction)) {

			onSelectUI_Old();
		}
	}

	/**
	 * Import config is selected in the dashboard.
	 * 
	 * @param selectedIndex
	 */
	private void onSelectImportConfig(final int selectedIndex) {

		final EasyConfig easyConfig = getEasyConfig();

		final ImportConfig selectedConfig = easyConfig.importConfigs.get(selectedIndex);

		easyConfig.setActiveImportConfig(selectedConfig);

		_isDeviceStateValid = false;
		updateUI_2_Dashboard();
	}

	private void onSelectionChanged(final ISelection selection) {

		if (selection instanceof SelectionDeletedTours) {

			// tours are deleted

			_postSelectionProvider.clearSelection();

			final SelectionDeletedTours tourSelection = (SelectionDeletedTours) selection;
			final ArrayList<ITourItem> removedTours = tourSelection.removedTours;

			if (removedTours.size() == 0) {
				return;
			}

			removeTours(removedTours);

			if (_isPartVisible) {

				_rawDataMgr.updateTourData_InImportView_FromDb(null);

				// force the store watcher to update the device state
				_isDeviceStateValid = false;

				// update the table viewer
				reloadViewer();

			} else {
				_isViewerPersonDataDirty = true;
			}
		}
	}

	private void onSelectUI_New() {

		_isNewUI = true;
		updateUI_1_TopPage(true);
	}

	private void onSelectUI_Old() {

		resetEasyImport();

		_isNewUI = false;
		updateUI_1_TopPage(true);
	}

	@Override
	public ColumnViewer recreateViewer(final ColumnViewer columnViewer) {

		_topPage_ImportViewer.setRedraw(false);
		{
			_tourViewer.getTable().dispose();
			createUI_92_TourViewer(_topPage_ImportViewer);
			_topPage_ImportViewer.layout();

			// update the viewer
			reloadViewer();
		}
		_topPage_ImportViewer.setRedraw(true);

		return _tourViewer;
	}

	/**
	 * Update {@link TourData} from the database for all imported tours, displays a progress dialog.
	 * 
	 * @param canCancelable
	 */
	private void reimportAllImportFiles(final boolean canCancelable) {

		final String[] prevImportedFiles = _state.getArray(STATE_IMPORTED_FILENAMES);
		if ((prevImportedFiles == null) || (prevImportedFiles.length == 0)) {
			return;
		}

//		if (prevImportedFiles.length < 5) {
//			reimportAllImportFilesTask(null, prevImportedFiles);
//		} else {

		try {
			new ProgressMonitorDialog(Display.getDefault().getActiveShell()).run(
					true,
					canCancelable,
					new IRunnableWithProgress() {

						@Override
						public void run(final IProgressMonitor monitor) throws InvocationTargetException,
								InterruptedException {

							reimportAllImportFiles_Runnable(monitor, prevImportedFiles, canCancelable);
						}
					});

		} catch (final Exception e) {
			StatusUtil.log(e);
		}
	}

	/**
	 * reimport previous imported tours
	 * 
	 * @param monitor
	 * @param importedFiles
	 * @param canCancelable
	 */
	private void reimportAllImportFiles_Runnable(	final IProgressMonitor monitor,
													final String[] importedFiles,
													final boolean canCancelable) {

		int workedDone = 0;
		final int workedAll = importedFiles.length;

		if (monitor != null) {
			monitor.beginTask(Messages.import_data_importTours_task, workedAll);
		}

		final ArrayList<String> notImportedFiles = new ArrayList<String>();

		_rawDataMgr.getImportedTours().clear();
		_rawDataMgr.setImportId();

		int importedFileCounter = 0;

		// loop: import all files
		for (final String fileName : importedFiles) {

			if (monitor != null) {
				monitor.worked(1);
				monitor.subTask(NLS.bind(Messages.import_data_importTours_subTask, //
						new Object[] { workedDone++, workedAll, fileName }));
			}

			final File file = new File(fileName);
			if (file.exists()) {
				if (_rawDataMgr.importRawData(file, null, false, null, true)) {
					importedFileCounter++;
				} else {
					notImportedFiles.add(fileName);
				}
			}

			if (canCancelable && monitor.isCanceled()) {
				// stop importing but process imported tours
				break;
			}
		}

		if (importedFileCounter > 0) {

			_rawDataMgr.updateTourData_InImportView_FromDb(monitor);

			Display.getDefault().asyncExec(new Runnable() {
				@Override
				public void run() {

					reloadViewer();

					/*
					 * restore selected tour
					 */
					final String[] viewerIndices = _state.getArray(STATE_SELECTED_TOUR_INDICES);

					if (viewerIndices != null) {

						final ArrayList<Object> viewerTourData = new ArrayList<Object>();

						for (final String viewerIndex : viewerIndices) {

							Object tourData = null;

							try {
								final int index = Integer.parseInt(viewerIndex);
								tourData = _tourViewer.getElementAt(index);
							} catch (final NumberFormatException e) {
								// just ignore
							}

							if (tourData != null) {
								viewerTourData.add(tourData);
							}
						}

						if (viewerTourData.size() > 0) {
							_tourViewer.setSelection(new StructuredSelection(viewerTourData.toArray()), true);
						}
					}
				}
			});
		}

		if (notImportedFiles.size() > 0) {
			RawDataManager.showMsgBoxInvalidFormat(notImportedFiles);
		}
	}

	/**
	 * This will also aktivate/deactivate the folder/store watcher.
	 * 
	 * @see net.tourbook.common.util.ITourViewer#reloadViewer()
	 */
	@Override
	public void reloadViewer() {

		updateUI_1_TopPage(false);

		// update tour data viewer
		final Object[] rawData = _rawDataMgr.getImportedTours().values().toArray();
		_tourViewer.setInput(rawData);

		enableActions();
	}

	private void removeTours(final ArrayList<ITourItem> removedTours) {

		final HashMap<Long, TourData> tourMap = _rawDataMgr.getImportedTours();

		for (final ITourItem tourItem : removedTours) {

			final TourData tourData = tourMap.get(tourItem.getTourId());
			if (tourData != null) {

				// when a tour was deleted the person in the tour data must be removed
				tourData.setTourPerson(null);

				// remove tour properties
				tourData.setTourType(null);
				tourData.setTourTitle(UI.EMPTY_STRING);
				tourData.setTourTags(new HashSet<TourTag>());

				/**
				 * when a remove tour is saved again, this will cause the exception: <br>
				 * detached entity passed to persist: net.tourbook.data.TourMarker<br>
				 * I didn't find a workaround, so this tour cannot be saved again until it is
				 * reloaded from the file
				 */
				tourData.isTourDeleted = true;
			}
		}
	}

	private String replaceNewLine(final String text) {
		return text.replace("\n", "\\n"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	private void resetEasyImport() {

		setWatcher_Off();
		EasyImportManager.getInstance().reset();
	}

	private void restoreState() {

		_isNewUI = Util.getStateBoolean(_state, STATE_IS_NEW_UI, false);

		_actionRemoveToursWhenClosed.setChecked(Util.getStateBoolean(
				_state,
				STATE_IS_REMOVE_TOURS_WHEN_VIEW_CLOSED,
				true));

		// restore: set merge tracks status before the tours are imported
		final boolean isMergeTracks = _state.getBoolean(STATE_IS_MERGE_TRACKS);
		_rawDataMgr.setMergeTracks(isMergeTracks);

		// restore: set merge tracks status before the tours are imported
		final boolean isCreateTourIdWithTime = _state.getBoolean(STATE_IS_CREATE_TOUR_ID_WITH_TIME);
		_rawDataMgr.setCreateTourIdWithTime(isCreateTourIdWithTime);

		// restore: is checksum validation
		final boolean isValidation = _state.getBoolean(STATE_IS_CHECKSUM_VALIDATION);
		_rawDataMgr.setIsChecksumValidation(isValidation);

		updateToolTipState();

		Display.getCurrent().asyncExec(new Runnable() {
			@Override
			public void run() {
				reimportAllImportFiles(true);
			}
		});
	}

	private void saveState() {

		// check if UI is disposed
		final Table table = _tourViewer.getTable();
		if (table.isDisposed()) {
			return;
		}

		/*
		 * save imported file names
		 */
		final boolean isRemoveToursWhenClosed = _actionRemoveToursWhenClosed.isChecked();
		String[] stateImportedFiles;
		if (isRemoveToursWhenClosed) {
			stateImportedFiles = new String[] {};
		} else {
			final HashSet<String> importedFiles = _rawDataMgr.getImportedFiles();
			stateImportedFiles = importedFiles.toArray(new String[importedFiles.size()]);
		}
		_state.put(STATE_IMPORTED_FILENAMES, stateImportedFiles);
		_state.put(STATE_IS_REMOVE_TOURS_WHEN_VIEW_CLOSED, isRemoveToursWhenClosed);

		// keep selected tours
		Util.setState(_state, STATE_SELECTED_TOUR_INDICES, table.getSelectionIndices());

		_state.put(STATE_IS_NEW_UI, _isNewUI);

		_columnManager.saveState(_state);
	}

	/**
	 * select first tour in the viewer
	 */
	public void selectFirstTour() {

		final TourData firstTourData = (TourData) _tourViewer.getElementAt(0);
		if (firstTourData != null) {
			_tourViewer.setSelection(new StructuredSelection(firstTourData), true);
		}
	}

	void selectLastTour() {

		final Collection<TourData> tourDataCollection = _rawDataMgr.getImportedTours().values();

		final TourData[] tourList = tourDataCollection.toArray(new TourData[tourDataCollection.size()]);

		// select the last tour in the viewer
		if (tourList.length > 0) {
			final TourData tourData = tourList[0];
			_tourViewer.setSelection(new StructuredSelection(tourData), true);
		}
	}

	/**
	 * Passing the focus request to the viewer's control.
	 */
	@Override
	public void setFocus() {

		_tourViewer.getControl().setFocus();

		if (_postSelectionProvider.getSelection() == null) {

			// fire a selected tour when the selection provider was cleared sometime before
			Display.getCurrent().asyncExec(new Runnable() {
				@Override
				public void run() {
					fireSelectedTour();
				}
			});
		}
	}

	private void setWatcher_Off() {

		if (isWatchingOn()) {

			// !!! Store watching must be canceled before the watch folder thread because it could launch a new watch folder thread !!!
			thread_WatchStores_Cancel();
			thread_WatchFolders(false);

			updateUI_WatcherAnimation(DOM_CLASS_DEVICE_OFF_ANIMATED);
		}
	}

	private void setWatcher_On() {

		if (isWatchingOn()) {
			// do not start twice
			return;
		}

		updateUI_WatcherAnimation(DOM_CLASS_DEVICE_ON_ANIMATED);

		thread_WatchStores_Start();
		thread_FolderWatcher_Activate();
	}

	private void thread_FolderWatcher_Activate() {

		// activate store watching
		_isWatchingStores.set(true);

		// activate folder watching
		thread_WatchFolders(true);
	}

	private void thread_FolderWatcher_Deactivate() {

		// deactivate background tasks

		_isWatchingStores.set(false);

		thread_WatchFolders(false);
	}

	/**
	 * Retrieve files from the device folder and update the UI.
	 */
	private void thread_UpdateDeviceState() {

		final EasyConfig importConfig = getEasyConfig();

		if (importConfig.getActiveImportConfig().isWatchAnything()) {

			EasyImportManager.getInstance().checkImportedFiles(true);
			updateUI_DeviceState();
		}
	}

	/**
	 * @param isStartWatching
	 *            When <code>true</code> a new watcher ist restarted, otherwise this thread is
	 *            canceled.
	 */
	private void thread_WatchFolders(final boolean isStartWatching) {

		WATCH_LOCK.lock();
		try {

			// cancel previous watcher
			thread_WatchFolders_Cancel();

			if (_watchingFolderThread != null) {
				throw new RuntimeException();
			}

			if (isWatchingOn() == false) {
				// watching store is off -> do not watch folders
				return;
			}

			if (isStartWatching) {

				final Runnable runnable = thread_WatchFolders_Runnable();

				_watchingFolderThread = new Thread(runnable, "WatchDeviceFolder - " + new DateTime()); //$NON-NLS-1$
				_watchingFolderThread.setDaemon(true);
				_watchingFolderThread.start();
			}

		} finally {
			WATCH_LOCK.unlock();
		}
	}

	/**
	 * <b>!!! This thread is not interrupted it could cause SQL exceptions !!!</b>
	 */
	private void thread_WatchFolders_Cancel() {

		if (_watchingFolderThread != null) {

			try {

				if (_folderWatcher != null) {

					try {
						_folderWatcher.close();
					} catch (final IOException e) {
						StatusUtil.log(e);
					} finally {
						_folderWatcher = null;
					}
				}

			} catch (final Exception e) {
				StatusUtil.log(e);
			} finally {

				try {
					_watchingFolderThread.join();
				} catch (final InterruptedException e) {
					StatusUtil.log(e);
				} finally {
					_watchingFolderThread = null;
				}
			}
		}
	}

	private Runnable thread_WatchFolders_Runnable() {

		return new Runnable() {
			@Override
			public void run() {

				WatchService folderWatcher = null;
				WatchKey watchKey = null;

				try {

					// keep watcher local because it could be set to null !!!
					folderWatcher = _folderWatcher = FileSystems.getDefault().newWatchService();

					final EasyConfig easyConfig = getEasyConfig();
					final ImportConfig importConfig = easyConfig.getActiveImportConfig();

					/*
					 * Check device folder
					 */
					boolean isDeviceFolderValid = false;
					final String deviceFolder = importConfig.getDeviceOSFolder();

					if (deviceFolder != null) {

						try {

							final Path deviceFolderPath = Paths.get(deviceFolder);

							if (Files.exists(deviceFolderPath)) {

								isDeviceFolderValid = true;

								deviceFolderPath.register(folderWatcher, ENTRY_CREATE, ENTRY_DELETE);
							}

						} catch (final Exception e) {}
					}

					if (isDeviceFolderValid) {

						Thread.currentThread().setName(//
								"WatchingDeviceFolder: " + deviceFolder + " - " + new DateTime()); //$NON-NLS-1$ //$NON-NLS-2$

					} else {

						// the device folder is not available, update the folder state

						updateUI_DeviceState();

						return;
					}

					/*
					 * Check backup folder
					 */
					final String backupFolder = importConfig.getBackupOSFolder();
					if (backupFolder != null) {

						try {

							final Path watchBackupFolder = Paths.get(backupFolder);

							if (Files.exists(watchBackupFolder)) {
								watchBackupFolder.register(folderWatcher, ENTRY_CREATE, ENTRY_DELETE);
							}

						} catch (final Exception e) {}
					}

					// do not update the device state when the import is running otherwise the import file list can be wrong
					if (_isUpdateDeviceState) {
						thread_UpdateDeviceState();
					}

					do {

						// wait for the next event
						watchKey = folderWatcher.take();

						/*
						 * Events MUST be polled otherwise this will stay in an endless loop.
						 */
						@SuppressWarnings("unused")
						final List<WatchEvent<?>> polledEvents = watchKey.pollEvents();

//// log events, they are not used
//						for (final WatchEvent<?> event : polledEvents) {
//
//							final WatchEvent.Kind<?> kind = event.kind();
//
//							System.out.println((UI.timeStampNano() + " [" + getClass().getSimpleName() + "] ")
//									+ (String.format("Event: %s\tFile: %s", kind, event.context())));
//							// TODO remove SYSTEM.OUT.PRINTLN
//						}

						// do not update the device state when the import is running otherwise the import file list can be wrong
						if (_isUpdateDeviceState) {
							thread_UpdateDeviceState();
						}

					}
					while (watchKey.reset());

				} catch (final InterruptedException e) {
					//
				} catch (final ClosedWatchServiceException e) {
					//
				} catch (final Exception e) {
					StatusUtil.log(e);
				} finally {

					if (watchKey != null) {
						watchKey.cancel();
					}

					if (folderWatcher != null) {
						try {
							folderWatcher.close();
						} catch (final IOException e) {
							StatusUtil.log(e);
						}
					}
				}
			}
		};
	}

	/**
	 * Thread cannot be interrupted, it could cause SQL exceptions, so set flag and wait.
	 */
	private void thread_WatchStores_Cancel() {

		_isStopWatchingStoresThread = true;

		// run with progress, duration can be 0...5 seconds
		try {

			final IRunnableWithProgress runnable = new IRunnableWithProgress() {
				@Override
				public void run(final IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {

					try {

						monitor.beginTask(Messages.Import_Data_Task_CloseDeviceInfo, IProgressMonitor.UNKNOWN);

						final int waitingTime = 10000;

						_watchingStoresThread.join(waitingTime);

						if (_watchingStoresThread.isAlive()) {

							// thread is still alive

							_watchingStoresThread.interrupt();

							Display.getDefault().asyncExec(new Runnable() {
								@Override
								public void run() {

									StatusUtil.showInfo(NLS.bind(//
											Messages.Import_Data_Task_CloseDeviceInfo_CannotClose,
											waitingTime / 1000));
								}
							});
						}

					} catch (final InterruptedException e) {
						StatusUtil.log(e);
					} finally {

						_watchingStoresThread = null;
					}
				}
			};

			new ProgressMonitorDialog(Display.getDefault().getActiveShell()).run(true, false, runnable);

		} catch (InvocationTargetException | InterruptedException e) {
			StatusUtil.log(e);
		}

	}

	private void thread_WatchStores_Start() {

		_watchingStoresThread = new Thread("WatchingStores") { //$NON-NLS-1$
			@Override
			public void run() {

				while (!isInterrupted()) {

					try {

						Thread.sleep(1000);

						// check if this thread should be stopped
						if (_isStopWatchingStoresThread) {
							_isStopWatchingStoresThread = false;
							break;
						}

						// check if polling is currently enabled
						if (_isWatchingStores.get()) {

							final EasyConfig importConfig = getEasyConfig();

							// check if anything should be watched
							if (importConfig.getActiveImportConfig().isWatchAnything()) {

								final boolean isCheckFiles = _isDeviceStateValid == false;

								final DeviceImportState importState = EasyImportManager.getInstance()//
										.checkImportedFiles(isCheckFiles);

								if (importState.areTheSameStores == false || isCheckFiles) {

									// stores have changed, update the folder watcher

									thread_WatchFolders(true);
								}

								if (importState.areFilesRetrieved || isCheckFiles) {

									// import files have been retrieved, update the UI

									updateUI_DeviceState();
								}

								_isDeviceStateValid = true;
							}
						}

					} catch (final InterruptedException e) {
						interrupt();
					} catch (final Exception e) {
						StatusUtil.log(e);
					}
				}
			}
		};

		_watchingStoresThread.setDaemon(true);
		_watchingStoresThread.start();
	}

	/**
	 * Keep live update values, other values MUST already have been set.
	 */
	private void updateModel_EasyConfig_Dashboard(final EasyConfig modifiedConfig) {

		final EasyConfig easyConfig = getEasyConfig();

		if (easyConfig.animationDuration != modifiedConfig.animationDuration
				|| easyConfig.animationCrazinessFactor != modifiedConfig.animationCrazinessFactor) {

			// run animation only when it was modified
			_isRunDashboardAnimation = true;
		}
		easyConfig.animationCrazinessFactor = modifiedConfig.animationCrazinessFactor;
		easyConfig.animationDuration = modifiedConfig.animationDuration;

		easyConfig.backgroundOpacity = modifiedConfig.backgroundOpacity;
		easyConfig.isLiveUpdate = modifiedConfig.isLiveUpdate;
		easyConfig.numHorizontalTiles = modifiedConfig.numHorizontalTiles;
		easyConfig.tileSize = modifiedConfig.tileSize;

		EasyImportManager.getInstance().saveEasyConfig(easyConfig);
	}

	private void updateToolTipState() {

		_isToolTipInDate = _prefStore.getBoolean(ITourbookPreferences.VIEW_TOOLTIP_TOURIMPORT_DATE);
		_isToolTipInTime = _prefStore.getBoolean(ITourbookPreferences.VIEW_TOOLTIP_TOURIMPORT_TIME);
		_isToolTipInTitle = _prefStore.getBoolean(ITourbookPreferences.VIEW_TOOLTIP_TOURIMPORT_TITLE);
		_isToolTipInTags = _prefStore.getBoolean(ITourbookPreferences.VIEW_TOOLTIP_TOURIMPORT_TAGS);
	}

	/**
	 * Set top page.
	 */
	private void updateUI_1_TopPage(final boolean isInStartUp) {

		/*
		 * When imported tours are available then the import viewer page will ALLWAYS be displayed.
		 */
		final int numImportedTours = _rawDataMgr.getImportedTours().size();
		if (numImportedTours > 0) {

			thread_FolderWatcher_Deactivate();

			_topPageBook.showPage(_topPage_ImportViewer);

		} else {

			/*
			 * !!! Run async that the first page in the top pagebook is visible and to prevent
			 * flickering when the view toolbar is first drawn on the left side of the view !!!
			 */

			if (_isNewUI) {

				_parent.getDisplay().asyncExec(new Runnable() {
					@Override
					public void run() {

						_isInUIStartup = isInStartUp;

						_topPageBook.showPage(_topPage_Dashboard);

						// create dashboard UI
						updateUI_2_Dashboard();

						if (_browser == null) {

							// deactivate background task

							setWatcher_Off();
						}

						// the watcher is started in onBrowser_Completed
					}
				});

			} else {

				_topPageBook.showPage(_topPage_OldUI);

				_linkImport.setFocus();
			}
		}
	}

	/**
	 * Set/create dashboard page.
	 */
	private void updateUI_2_Dashboard() {

		final boolean isBrowserAvailable = _browser != null;

		// set dashboard page
		_dashboard_PageBook.showPage(isBrowserAvailable//
				? _dashboardPage_WithBrowser
				: _dashboardPage_NoBrowser);

		if (!isBrowserAvailable) {
			return;
		}

		final String html = createHTML();

		_isBrowserCompleted = false;

		_browser.setText(html);
	}

	private void updateUI_DeviceState() {

		Display.getDefault().asyncExec(new Runnable() {
			@Override
			public void run() {

				if (_browser.isDisposed()) {
					// this occured
					return;
				}

				if (_isBrowserCompleted) {
					updateUI_DeviceState_DOM();
				} else {
					_isDeviceStateUpdateDelayed.set(true);
				}
			}
		});
	}

	private void updateUI_DeviceState_DOM() {

		final String htmlDeviceOnOff = createHTML_52_DeviceState_OnOff();
		String jsDeviceOnOff = htmlDeviceOnOff.replace("\"", "\\\""); //$NON-NLS-1$ //$NON-NLS-2$
		jsDeviceOnOff = replaceNewLine(jsDeviceOnOff);

		final String htmlDeviceState = createHTML_54_DeviceState();
		String jsDeviceState = htmlDeviceState.replace("\"", "\\\""); //$NON-NLS-1$ //$NON-NLS-2$
		jsDeviceState = replaceNewLine(jsDeviceState);

		final String js = "\n" //$NON-NLS-1$

				+ ("var htmlDeviceOnOff=\"" + jsDeviceOnOff + "\";\n") //$NON-NLS-1$ //$NON-NLS-2$
				+ ("document.getElementById(\"" + DOM_ID_DEVICE_ON_OFF + "\").innerHTML = htmlDeviceOnOff;\n") //$NON-NLS-1$ //$NON-NLS-2$

				+ ("var htmlDeviceState =\"" + jsDeviceState + "\";\n") //$NON-NLS-1$ //$NON-NLS-2$
				+ ("document.getElementById(\"" + DOM_ID_DEVICE_STATE + "\").innerHTML = htmlDeviceState;\n") //$NON-NLS-1$ //$NON-NLS-2$
		;

		final boolean isSuccess = _browser.execute(js);

		if (!isSuccess) {
			System.out.println((UI.timeStampNano() + " [" + getClass().getSimpleName() + "] ") //$NON-NLS-1$ //$NON-NLS-2$
					+ ("\tupdateDOM_DeviceState: " + isSuccess + js)); //$NON-NLS-1$
			// TODO remove SYSTEM.OUT.PRINTLN
		}
	}

	private void updateUI_WatcherAnimation(final String domClassState) {

		if (_isShowWatcherAnimation && _browser != null && !_browser.isDisposed() && _isBrowserCompleted) {

			_isShowWatcherAnimation = false;

			final String js = UI.EMPTY_STRING//
					+ ("document.getElementById(\"" + DOM_ID_IMPORT_TILES + "\").className ='" + domClassState + "';\n") //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					+ ("document.getElementById(\"" + DOM_ID_DEVICE_STATE + "\").className ='" + domClassState + "';\n") //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					+ ("document.getElementById(\"" + DOM_ID_IMPORT_CONFIG + "\").className ='" + domClassState + "';\n") //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			;

			_browser.execute(js);
		}
	}

	/**
	 * when the active person was modified, the view must be updated
	 */
	private void updateViewerPersonData() {

		_activePerson = TourbookPlugin.getActivePerson();

		// update person in save action
		enableActions();

		// update person in the raw data
		_rawDataMgr.updateTourData_InImportView_FromDb(null);

		_tourViewer.refresh();
	}
}
