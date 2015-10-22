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

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
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
import java.util.Locale;
import java.util.Set;

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
import net.tourbook.importdata.DialogAutomatedImportConfig;
import net.tourbook.importdata.ImportConfig;
import net.tourbook.importdata.ImportConfigItem;
import net.tourbook.importdata.RawDataManager;
import net.tourbook.importdata.SpeedVertex;
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
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.DeviceResourceException;
import org.eclipse.jface.resource.ImageDescriptor;
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
import org.eclipse.swt.browser.LocationAdapter;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.browser.ProgressAdapter;
import org.eclipse.swt.browser.ProgressEvent;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
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
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.part.PageBook;
import org.eclipse.ui.part.ViewPart;

/**
 *
 */
public class RawDataView extends ViewPart implements ITourProviderAll, ITourViewer3 {

	public static final String				ID											= "net.tourbook.views.rawData.RawDataView"; //$NON-NLS-1$
	//
	private static final String				TOUR_IMPORT_CSS								= "/tourbook/resources/tour-import.css";	//$NON-NLS-1$
	//
	public static final int					COLUMN_DATE									= 0;
	public static final int					COLUMN_TITLE								= 1;
	public static final int					COLUMN_DATA_FORMAT							= 2;
	public static final int					COLUMN_FILE_NAME							= 3;
	//
	private static final String				STATE_IMPORTED_FILENAMES					= "importedFilenames";						//$NON-NLS-1$
	private static final String				STATE_SELECTED_TOUR_INDICES					= "SelectedTourIndices";					//$NON-NLS-1$
	private static final String				STATE_IS_REMOVE_TOURS_WHEN_VIEW_CLOSED		= "STATE_IS_REMOVE_TOURS_WHEN_VIEW_CLOSED"; //$NON-NLS-1$
	public static final String				STATE_IS_MERGE_TRACKS						= "isMergeTracks";							//$NON-NLS-1$
	public static final String				STATE_IS_CHECKSUM_VALIDATION				= "isChecksumValidation";					//$NON-NLS-1$
	public static final String				STATE_IS_CONVERT_WAYPOINTS					= "STATE_IS_CONVERT_WAYPOINTS";			//$NON-NLS-1$
	public static final String				STATE_IS_CREATE_TOUR_ID_WITH_TIME			= "isCreateTourIdWithTime";				//$NON-NLS-1$
	public static final boolean				STATE_IS_MERGE_TRACKS_DEFAULT				= false;
	public static final boolean				STATE_IS_CHECKSUM_VALIDATION_DEFAULT		= true;
	public static final boolean				STATE_IS_CONVERT_WAYPOINTS_DEFAULT			= true;
	public static final boolean				STATE_IS_CREATE_TOUR_ID_WITH_TIME_DEFAULT	= false;
	//
	public static final String				IMAGE_DATA_TRANSFER							= "IMAGE_DATA_TRANSFER";					//$NON-NLS-1$
	public static final String				IMAGE_DATA_TRANSFER_DIRECT					= "IMAGE_DATA_TRANSFER_DIRECT";			//$NON-NLS-1$
	public static final String				IMAGE_IMPORT								= "IMAGE_IMPORT";							//$NON-NLS-1$
	public static final String				IMAGE_AUTOMATED_IMPORT						= "IMAGE_AUTOMATED_IMPORT";				//$NON-NLS-1$

//	private static final RGB				BACKGROUND_COLOR							= new RGB(0x20, 0x20, 0x50);
	//
	private final IPreferenceStore			_prefStore									= TourbookPlugin.getPrefStore();
	private final IDialogSettings			_state										= TourbookPlugin.getState(ID);
	//
	private RawDataManager					_rawDataMgr									= RawDataManager.getInstance();
	//
	private PostSelectionProvider			_postSelectionProvider;
	private IPartListener2					_partListener;
	private ISelectionListener				_postSelectionListener;
	private IPropertyChangeListener			_prefChangeListener;
	private ITourEventListener				_tourEventListener;
	// context menu actions
	private ActionClearView					_actionClearView;
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
	private ActionSaveTourInDatabase		_actionSaveTour;
	private ActionSaveTourInDatabase		_actionSaveTourWithPerson;
	private ActionSetTourTypeMenu			_actionSetTourType;
	private ActionRemoveToursWhenClosed		_actionRemoveToursWhenClosed;
//	private ActionAdjustYear				_actionAdjustImportedYear;
//	private ActionCreateTourIdWithTime		_actionCreateTourIdWithTime;
//	private ActionDisableChecksumValidation	_actionDisableChecksumValidation;
//	private ActionMergeGPXTours				_actionMergeGPXTours;
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
	private HashMap<Long, Image>			_configImages								= new HashMap<>();
	private HashMap<Long, Integer>			_configImageHash							= new HashMap<>();

	/*
	 * resources
	 */
	private ImageDescriptor					_imageDescDatabase;
	private ImageDescriptor					_imageDescDatabaseOtherPerson;

	private ImageDescriptor					_imageDescDatabaseAssignMergedTour;
	private ImageDescriptor					_imageDescDatabasePlaceholder;
	private ImageDescriptor					_imageDescDelete;
	private Image							_imageDatabase;
	private Image							_imageDatabaseOtherPerson;

	private Image							_imageDatabaseAssignMergedTour;
	private Image							_imageDatabasePlaceholder;
	private Image							_imageDelete;
	/*
	 * UI controls
	 */
	private PageBook						_pageBook_Import;
	private Composite						_pageImport_Actions;
	private Composite						_pageImport_Viewer;

	private PageBook						_pageBook_Actions;
	private Composite						_pageActions_NoBrowser;
	private Composite						_pageActions_Content;

	private Composite						_innerAutoImportContainer;
	private Composite						_outerAutoImportContainer;
	private TableViewer						_tourViewer;
	private TableViewerTourInfoToolTip		_tourInfoToolTip;

	private PixelConverter					_pc;

	private Browser							_browser;
	private Text							_txtNoBrowser;
	private String							_htmlCss;

	private class ActionAutoImport extends Action {

		private int	__configIndex;

		public ActionAutoImport(final int configIndex) {

			__configIndex = configIndex;
		}

		@Override
		public void run() {
			actionRunAutoImport(__configIndex);
		}
	}

	private class ActionImport extends Action {

		private ImportAction	__importAction;

		public ActionImport(final ImportAction importAction) {

			__importAction = importAction;
		}

		@Override
		public void run() {
			actionImport(__importAction);
		}
	}

	private enum ImportAction {

		FROM_FILES, //
		AUTOMATED_IMPORT_CONFIG, //
		DATA_TRANSFER, //
		DATA_TRANSFER_DIRECTLY, //
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

	void actionClearView() {

		// remove all tours
		_rawDataMgr.removeAllTours();

		reloadViewer();

		_postSelectionProvider.setSelection(new SelectionDeletedTours());

		// don't throw the selection again
		_postSelectionProvider.clearSelection();
	}

	private void actionImport(final ImportAction importAction) {

		switch (importAction) {

		case FROM_FILES:
			_rawDataMgr.actionImportFromFile();
			break;

		case AUTOMATED_IMPORT_CONFIG:
			actionSetupAutomatedImport();
			break;

		case DATA_TRANSFER:
			_rawDataMgr.actionImportFromDevice();
			break;

		case DATA_TRANSFER_DIRECTLY:
			_rawDataMgr.actionImportFromDeviceDirect();
			break;

		default:
			break;
		}
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

	private void actionRunAutoImport(final int configIndex) {
		// TODO Auto-generated method stub

	}

	void actionSaveTour(final TourPerson person) {

		final ArrayList<TourData> savedTours = new ArrayList<TourData>();

		// get selected tours, this must be outside of the runnable !!!
		final IStructuredSelection selection = ((IStructuredSelection) _tourViewer.getSelection());

		final IRunnableWithProgress saveRunnable = new IRunnableWithProgress() {
			@Override
			public void run(final IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {

				int saveCounter = 0;
				final int selectionSize = selection.size();

				monitor.beginTask(Messages.Tour_Data_SaveTour_Monitor, selectionSize);

				// loop: all selected tours, selected tours can already be saved
				for (final Iterator<?> iter = selection.iterator(); iter.hasNext();) {

					monitor.subTask(NLS.bind(Messages.Tour_Data_SaveTour_MonitorSubtask, ++saveCounter, selectionSize));

					final Object selObject = iter.next();
					if (selObject instanceof TourData) {
						saveTour((TourData) selObject, person, savedTours, false);
					}

					monitor.worked(1);
				}
			}
		};

		try {
			new ProgressMonitorDialog(Display.getCurrent().getActiveShell()).run(true, false, saveRunnable);
		} catch (final InvocationTargetException e) {
			StatusUtil.showStatus(e);
		} catch (final InterruptedException e) {
			StatusUtil.showStatus(e);
		}

		doSaveTourPostActions(savedTours);
	}

	public void actionSetupAutomatedImport() {

		final Shell shell = Display.getDefault().getActiveShell();

		final ImportConfig importConfig = RawDataManager.getInstance().getAutoImportConfigs();

		final DialogAutomatedImportConfig dialog = new DialogAutomatedImportConfig(shell, importConfig, this);

		if (dialog.open() == Window.OK) {

			final ImportConfig modifiedConfig = dialog.getModifiedConfig();

			importConfig.configItems.clear();
			importConfig.configItems.addAll(modifiedConfig.configItems);

			importConfig.numUIColumns = modifiedConfig.numUIColumns;

			RawDataManager.getInstance().saveImportConfig(importConfig);

//			createUI_74_AutoImport_InnerContainer();
//
//			// the size could be larger
//			_pageImport_Actions.layout(true, true);

			updateUI_Browser();
		}
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

					// update tour type in the raw data
					_rawDataMgr.updateTourDataFromDb(null);

					_tourViewer.refresh();

				} else if (property.equals(ITourbookPreferences.VIEW_TOOLTIP_IS_MODIFIED)) {

					updateToolTipState();

				} else if (property.equals(ITourbookPreferences.MEASUREMENT_SYSTEM)) {

					// measurement system has changed

					net.tourbook.ui.UI.updateUnits();

					_columnManager.saveState(_state);
					_columnManager.clearColumns();
					defineAllColumns();

					_tourViewer = (TableViewer) recreateViewer(_tourViewer);

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

					_rawDataMgr.updateTourDataFromDb(null);

					reloadViewer();
				}
			}
		};
		TourManager.getInstance().addTourEventListener(_tourEventListener);
	}

	private void createActions() {

		_actionClearView = new ActionClearView(this);
		_actionEditImportPreferences = new ActionOpenPrefDialog(
				Messages.Import_Data_Action_EditImportPreferences,
				PrefPageImport.ID);
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
		_actionSetTourType = new ActionSetTourTypeMenu(this);

		_tagMenuMgr = new TagMenuManager(this, true);
	}

	private String createHTML_10_Head() {

		final String html = ""// //$NON-NLS-1$
				+ "	<meta http-equiv='Content-Type' content='text/html; charset=UTF-8' />\n" //$NON-NLS-1$
				+ "	<meta http-equiv='X-UA-Compatible' content='IE=edge' />\n" //$NON-NLS-1$
				+ _htmlCss
				+ "\n"; //$NON-NLS-1$

		return html;
	}

	private String createHTML_20_Body() {

		final String sb = createHTML_50_AutoImportItems();

		return sb;
	}

	private String createHTML_50_AutoImportItems() {

		final ImportConfig importConfig = RawDataManager.getInstance().getAutoImportConfigs();
		final ArrayList<ImportConfigItem> configItems = importConfig.configItems;
		final int numColumns = importConfig.numUIColumns;

		if (configItems.size() == 0) {
			return UI.EMPTY_STRING;
		}

		boolean isTrOpen = false;

		final StringBuilder sb = new StringBuilder();

		// enforce equal column width
		sb.append("<table xstyle='table-layout: fixed;'><tbody>\n");

		for (int configIndex = 0; configIndex < configItems.size(); configIndex++) {

			if (configIndex % numColumns == 0) {
				sb.append("<tr>\n");
				isTrOpen = true;
			}

			final ImportConfigItem configItem = configItems.get(configIndex);

			// enforce equal column width
			sb.append("<td style='width:" + 100 / numColumns + "%' class='auto-import'>");
			sb.append(createHTML_52_ConfigItem(configItem));
			sb.append("</td>\n");

			if (configIndex % numColumns == numColumns - 1) {
				sb.append("</tr>\n");
				isTrOpen = false;
			}
		}

		if (isTrOpen) {
			sb.append("</tr>\n");
		}

		sb.append("</tbody></table>\n");

		return sb.toString();
	}

	private String createHTML_52_ConfigItem(final ImportConfigItem configItem) {

		final Image configImage = getImportConfigImage(configItem);

		String htmlImage = UI.EMPTY_STRING;

		if (configImage != null) {

			final byte[] pngImageData = ImageUtils.saveImage(configImage, SWT.IMAGE_PNG);
			final String base64Encoded = Base64.getEncoder().encodeToString(pngImageData);

			htmlImage = "<img src='data:image/png;base64," + base64Encoded + "'>";
		}

		final String html = ""
		//
				+ ("<div class='auto-import'>")
				+ ("<a href='#' class='auto-import'>\n")
				+ ("	<div class='auto-import-image'>" + htmlImage + "</div>")
				+ ("	<div class='auto-import-name'>" + configItem.name + "</div>")
				+ ("</a>")
				+ ("</div>")
		//
		;

		return html;
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

		// set default page
		_pageBook_Import.showPage(_pageImport_Actions);
		updateUI_Browser();

		restoreState();
	}

	private void createResources() {

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

	private void createUI(final Composite parent) {

		_pageBook_Import = new PageBook(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(_pageBook_Import);

		_pageImport_Actions = createUI_20_Page_ImportActions_HTML(_pageBook_Import);

		_pageImport_Viewer = new Composite(_pageBook_Import, SWT.NONE);
		GridLayoutFactory.fillDefaults().applyTo(_pageImport_Viewer);
		{
			createUI_90_Page_TourViewer(_pageImport_Viewer);
		}
	}

	private Composite createUI_20_Page_ImportActions_HTML(final Composite parent) {

		_pageBook_Actions = new PageBook(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(_pageBook_Actions);

		_pageActions_NoBrowser = new Composite(_pageBook_Actions, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(_pageActions_NoBrowser);
		GridLayoutFactory.swtDefaults().numColumns(1).applyTo(_pageActions_NoBrowser);
		_pageActions_NoBrowser.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
		{
			_txtNoBrowser = new Text(_pageActions_NoBrowser, SWT.WRAP | SWT.READ_ONLY);
			GridDataFactory.fillDefaults()//
					.grab(true, true)
					.align(SWT.FILL, SWT.BEGINNING)
					.applyTo(_txtNoBrowser);
			_txtNoBrowser.setText(Messages.UI_Label_BrowserCannotBeCreated);
		}

		_pageActions_Content = new Composite(_pageBook_Actions, SWT.NONE);
		GridLayoutFactory.fillDefaults().applyTo(_pageActions_Content);
		{
			createUI_22_Browser(_pageActions_Content);
		}

		return _pageBook_Actions;
	}

	private void createUI_22_Browser(final Composite parent) {

		try {

			try {

				// use default browser
				_browser = new Browser(parent, SWT.NONE);

			} catch (final Exception e) {

				/*
				 * Use mozilla browser, this is necessary for Linux when default browser fails
				 * however the XULrunner needs to be installed.
				 */
				_browser = new Browser(parent, SWT.MOZILLA);
			}

			GridDataFactory.fillDefaults().grab(true, true).applyTo(_browser);

			_browser.addLocationListener(new LocationAdapter() {
				@Override
				public void changing(final LocationEvent event) {
//					onBrowserLocationChanging(event);
				}
			});

			_browser.addProgressListener(new ProgressAdapter() {
				@Override
				public void completed(final ProgressEvent event) {
//					onBrowserCompleted(event);
				}
			});

		} catch (final SWTError e) {

			_txtNoBrowser.setText(NLS.bind(Messages.UI_Label_BrowserCannotBeCreated_Error, e.getMessage()));
		}
	}

	private Composite createUI_70_Page_ImportActions_SWT(final Composite parent) {

		// scrolled container
		final ScrolledComposite importContainer = new ScrolledComposite(parent, SWT.V_SCROLL);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(importContainer);
		importContainer.setExpandVertical(true);
		importContainer.setExpandHorizontal(true);

		// vertex container
		final Composite container = new Composite(importContainer, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
		GridLayoutFactory.swtDefaults()//
				.numColumns(1)
//				.spacing(10, 10)
				.margins(10, 10)
				.applyTo(container);
		importContainer.setContent(container);
		importContainer.addControlListener(new ControlAdapter() {
			@Override
			public void controlResized(final ControlEvent e) {
				importContainer.setMinSize(container.computeSize(SWT.DEFAULT, SWT.DEFAULT));
			}
		});
//		container.setBackground(_bgActionColor);

		{
			// action: Automated import from files
			createUI_72_AutoImport(container);

			// action: Import from files
			ToolBar tb = createUI_ImportAction(container, //
					Messages.Import_Data_Link_Import,
					IMAGE_IMPORT,
					ImportAction.FROM_FILES,
					false);
			GridDataFactory.fillDefaults().indent(0, 30).applyTo(tb);

			// action: data transfer
			tb = createUI_ImportAction(container, //
					Messages.Import_Data_Link_ReceiveFromSerialPort_Configured,
					IMAGE_DATA_TRANSFER,
					ImportAction.DATA_TRANSFER,
					false);
			GridDataFactory.fillDefaults().indent(0, 20).applyTo(tb);

			// action: direct data transfer
			createUI_ImportAction(container, //
					Messages.Import_Data_Link_ReceiveFromSerialPort_Directly,
					IMAGE_DATA_TRANSFER_DIRECT,
					ImportAction.DATA_TRANSFER_DIRECTLY,
					false);
		}

		return importContainer;
	}

	private void createUI_72_AutoImport(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults()//
				.grab(true, false)
				.indent(0, 0)
				.applyTo(container);
//		container.setBackground(_bgActionColor);

		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
		{
			/*
			 * label
			 */
			final Label label = new Label(container, SWT.NONE);
			GridDataFactory.fillDefaults()//
					.align(SWT.FILL, SWT.CENTER)
					.applyTo(label);
			label.setText(Messages.Import_Data_Label_AutomatedImport);
//			label.setForeground(_fgActionColor);
//			label.setBackground(_bgActionColor);

			/*
			 * action: configure automated import from files
			 */
			final ActionImport action = new ActionImport(ImportAction.AUTOMATED_IMPORT_CONFIG);

			action.setToolTipText(Messages.Import_Data_Action_AutomatedImportConfig_Tooltip);
			action.setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__tour_options));

			createUI_ImportAction(container, action, false);
		}

		_outerAutoImportContainer = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults()//
				.grab(true, false)
				.applyTo(_outerAutoImportContainer);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(_outerAutoImportContainer);
		{
			createUI_74_AutoImport_InnerContainer();
		}
	}

	private void createUI_74_AutoImport_InnerContainer() {

		if (_innerAutoImportContainer != null) {
			_innerAutoImportContainer.dispose();
		}

		final ImportConfig importConfig = RawDataManager.getInstance().getAutoImportConfigs();

		_innerAutoImportContainer = new Composite(_outerAutoImportContainer, SWT.NONE);
//		_innerAutoImportContainer.setBackground(_bgActionColor);

		GridDataFactory.fillDefaults()//
				.grab(true, false)
//				.align(SWT.CENTER, SWT.FILL)
				.applyTo(_innerAutoImportContainer);
		GridLayoutFactory.fillDefaults()//
				.numColumns(importConfig.numUIColumns)
				.equalWidth(true)
				.applyTo(_innerAutoImportContainer);
//		_innerAutoImportContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_BLUE));
		{
			final ArrayList<ImportConfigItem> configItems = importConfig.configItems;

			// create an action for each import configuration
			for (int configIndex = 0; configIndex < configItems.size(); configIndex++) {

				final ImportConfigItem configItem = configItems.get(configIndex);

				final String configName = createHTML_52_ConfigItem(configItem);
				final String configTooltip = createUI_ConfigTooltip(configItem);

				final String actionText = configName.trim().length() == 0 //
						? Messages.Import_Data_Link_ConfigName
						: configName;

				final Image importConfigImage = getImportConfigImage(configItem);

				final ActionAutoImport action = new ActionAutoImport(configIndex);
				action.setText(actionText);
				action.setToolTipText(configTooltip);

				if (importConfigImage != null) {
					action.setImageDescriptor(ImageDescriptor.createFromImage(importConfigImage));
				}

				createUI_ImportAction(_innerAutoImportContainer, action, true);
			}
		}
	}

	/**
	 * @param parent
	 */
	private void createUI_90_Page_TourViewer(final Composite parent) {

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

		createUI_92_ContextMenu();
	}

	/**
	 * create the views context menu
	 */
	private void createUI_92_ContextMenu() {

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

	private String createUI_ConfigTooltip(final ImportConfigItem importConfig) {

		final Enum<TourTypeConfig> tourTypeConfig = importConfig.tourTypeConfig;

		final StringBuilder sb = new StringBuilder();

		/*
		 * Config name
		 */
		final String configName = createHTML_52_ConfigItem(importConfig);
		if (configName.length() > 0) {

			sb.append(configName);

			sb.append(UI.NEW_LINE);
			sb.append(UI.NEW_LINE);
		}

		/*
		 * Tour types
		 */
		if (TourTypeConfig.TOUR_TYPE_CONFIG_BY_SPEED.equals(tourTypeConfig)) {

			for (final SpeedVertex vertex : importConfig.speedVertices) {

				final float speed = vertex.avgSpeed / net.tourbook.ui.UI.UNIT_VALUE_DISTANCE;

				sb.append('<');
				sb.append((int) speed);
				sb.append(UI.SPACE2);
				sb.append(UI.UNIT_LABEL_SPEED);
				sb.append(UI.SPACE2);
				sb.append(TourDatabase.getTourTypeName(vertex.tourTypeId));
				sb.append(UI.NEW_LINE);
			}

		} else if (TourTypeConfig.TOUR_TYPE_CONFIG_ONE_FOR_ALL.equals(tourTypeConfig)) {

			if (importConfig.oneTourType != null) {

				sb.append(TourDatabase.getTourTypeName(importConfig.oneTourType.getTypeId()));
				sb.append(UI.NEW_LINE);
			}

		} else {

			// this is the default or TourTypeConfig.TOUR_TYPE_CONFIG_NOT_USED

			sb.append(Messages.Import_Data_TourTypeConfig_NotUsed);
		}

		return sb.toString();
	}

	private ToolBar createUI_ImportAction(final Composite parent, final Action action, final boolean isCenter) {

		final ActionContributionItem item = new ActionContributionItem(action);

		item.setMode(ActionContributionItem.MODE_FORCE_TEXT);

		ToolBar tb;

		if (isCenter) {

			tb = new ToolBar(parent, SWT.FLAT);

			GridDataFactory.fillDefaults()//
					.align(SWT.CENTER, SWT.CENTER)
					.applyTo(tb);

		} else {

			tb = new ToolBar(parent, SWT.FLAT | SWT.RIGHT);
		}

//		tb.setBackground(_bgActionColor);

		final ToolBarManager tbm = new ToolBarManager(tb);

		tbm.add(item);
		tbm.update(true);

		return tb;
	}

	private ToolBar createUI_ImportAction(	final Composite parent,
											final String text,
											final String imageId,
											final ImportAction importAction,
											final boolean isCenter) {

		final Image actionImage = net.tourbook.ui.UI.IMAGE_REGISTRY.get(imageId);

		final ActionImport action = new ActionImport(importAction);

		action.setText(text);
		action.setImageDescriptor(ImageDescriptor.createFromImage(actionImage));

		return createUI_ImportAction(parent, action, isCenter);
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

	@Override
	public void dispose() {

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
	 * After tours are saved, the internal structures and ui viewers must be updated
	 * 
	 * @param savedTours
	 *            contains the saved {@link TourData}
	 */
	private void doSaveTourPostActions(final ArrayList<TourData> savedTours) {

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

	void enableActions() {

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

		// set double click state
		_tourDoubleClickState.canEditTour = isOneSavedAndNotDeleteTour;
		_tourDoubleClickState.canQuickEditTour = isOneSavedAndNotDeleteTour;
		_tourDoubleClickState.canEditMarker = isOneSavedAndNotDeleteTour;
		_tourDoubleClickState.canAdjustAltitude = isOneSavedAndNotDeleteTour;
		_tourDoubleClickState.canOpenTour = isOneSelectedNotDeleteTour;

		final ArrayList<TourType> tourTypes = TourDatabase.getAllTourTypes();
		_actionSetTourType.setEnabled(isSavedTourSelected && (tourTypes.size() > 0));

//		_actionAddTag.setEnabled(isTourSelected);

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

	public Image getImportConfigImage(final ImportConfigItem importConfig) {

		final int imageWidth = importConfig.imageWidth;

		if (imageWidth == 0) {
			return null;
		}

		final long configId = importConfig.getCreateId();
		Image image = _configImages.get(configId);

		if (isConfigImageValid(image, importConfig)) {
			return image;
		}

		final Display display = _pageBook_Import.getDisplay();

		final Enum<TourTypeConfig> tourTypeConfig = importConfig.tourTypeConfig;
		final net.tourbook.ui.UI ui = net.tourbook.ui.UI.getInstance();

		if (TourTypeConfig.TOUR_TYPE_CONFIG_BY_SPEED.equals(tourTypeConfig)) {

			final ArrayList<SpeedVertex> speedVertices = importConfig.speedVertices;
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

						final SpeedVertex vertex = speedVertices.get(imageIndex);

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
				imageData.transparentPixel = TourType.TRANSPARENT_COLOR_VALUE;


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
					imageData.transparentPixel = TourType.TRANSPARENT_COLOR_VALUE;

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

		createResources();

		_pc = new PixelConverter(parent);

		try {

			/*
			 * load css from file
			 */
			final File cssFile = WEB.getFile(TOUR_IMPORT_CSS);
			final String cssContent = Util.readContentFromFile(cssFile.getAbsolutePath());

			_htmlCss = "<style>" + cssContent + "</style>\n"; //$NON-NLS-1$ //$NON-NLS-2$

			/*
			 * set image urls
			 */
//			_actionEditImageUrl = net.tourbook.ui.UI.getIconUrl(Messages.Image__quick_edit);
//			_actionHideMarkerUrl = net.tourbook.ui.UI.getIconUrl(Messages.Image__Remove);
//			_actionShowMarkerUrl = net.tourbook.ui.UI.getIconUrl(Messages.Image__Eye);

		} catch (final IOException | URISyntaxException e) {
			StatusUtil.showStatus(e);
		}
	}

	/**
	 * @param image
	 * @param importConfig
	 * @return Returns <code>true</code> when the image is valid, returns <code>false</code> when
	 *         the profile image must be created,
	 */
	private boolean isConfigImageValid(final Image image, final ImportConfigItem importConfig) {

		if (image == null || image.isDisposed()) {

			return false;
		}

		final Integer imageHash = _configImageHash.get(importConfig.getCreateId());

		if (imageHash == null || imageHash != importConfig.imageHash) {

			image.dispose();

			return false;
		}

		return true;
	}

	private void onSelectImportConfig(final SelectionEvent e) {

		final int configIndex = (int) e.widget.getData();
	}

	private void onSelectionChanged(final ISelection selection) {

		if (!selection.isEmpty() && (selection instanceof SelectionDeletedTours)) {

			_postSelectionProvider.clearSelection();

			final SelectionDeletedTours tourSelection = (SelectionDeletedTours) selection;
			final ArrayList<ITourItem> removedTours = tourSelection.removedTours;

			if (removedTours.size() == 0) {
				return;
			}

			removeTours(removedTours);

			if (_isPartVisible) {

				_rawDataMgr.updateTourDataFromDb(null);

				// update the table viewer
				reloadViewer();
			} else {
				_isViewerPersonDataDirty = true;
			}
		}
	}

	@Override
	public ColumnViewer recreateViewer(final ColumnViewer columnViewer) {

		_pageImport_Viewer.setRedraw(false);
		{
			_tourViewer.getTable().dispose();
			createUI_90_Page_TourViewer(_pageImport_Viewer);
			_pageImport_Viewer.layout();

			// update the viewer
			reloadViewer();
		}
		_pageImport_Viewer.setRedraw(true);

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

			_rawDataMgr.updateTourDataFromDb(monitor);

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

	@Override
	public void reloadViewer() {

		final Object[] rawData = _rawDataMgr.getImportedTours().values().toArray();

		_pageBook_Import.showPage(rawData.length > 0 ? _pageImport_Viewer : _pageImport_Actions);

		// update tour data viewer
		_tourViewer.setInput(rawData);
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

	private void restoreState() {

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

		_columnManager.saveState(_state);
	}

	/**
	 * @param tourData
	 *            {@link TourData} which is not yet saved
	 * @param person
	 *            person for which the tour is being saved
	 * @param savedTours
	 *            the saved tour is added to this list
	 */
	private void saveTour(	final TourData tourData,
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

	private void showBrowserPage() {

		_pageBook_Actions.showPage(_browser == null ? _pageActions_NoBrowser : _pageActions_Content);
	}

	private void updateToolTipState() {

		_isToolTipInDate = _prefStore.getBoolean(ITourbookPreferences.VIEW_TOOLTIP_TOURIMPORT_DATE);
		_isToolTipInTime = _prefStore.getBoolean(ITourbookPreferences.VIEW_TOOLTIP_TOURIMPORT_TIME);
		_isToolTipInTitle = _prefStore.getBoolean(ITourbookPreferences.VIEW_TOOLTIP_TOURIMPORT_TITLE);
		_isToolTipInTags = _prefStore.getBoolean(ITourbookPreferences.VIEW_TOOLTIP_TOURIMPORT_TAGS);
	}

	/**
	 * Update the UI from {@link #_tourData}.
	 */
	private void updateUI_Browser() {

		showBrowserPage();

		if (_browser == null) {
			return;
		}

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

		_browser.setRedraw(true);
		_browser.setText(html);
	}

	/**
	 * when the active person was modified, the view must be updated
	 */
	private void updateViewerPersonData() {

		_activePerson = TourbookPlugin.getActivePerson();

		// update person in save action
		enableActions();

		// update person in the raw data
		_rawDataMgr.updateTourDataFromDb(null);

		_tourViewer.refresh();
	}
}
