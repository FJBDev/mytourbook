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

package net.tourbook.ui.views.rawData;

import java.io.File;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;

import net.tourbook.Messages;
import net.tourbook.data.TourData;
import net.tourbook.data.TourPerson;
import net.tourbook.data.TourTag;
import net.tourbook.data.TourType;
import net.tourbook.database.TourDatabase;
import net.tourbook.importdata.RawDataManager;
import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tag.ActionRemoveAllTags;
import net.tourbook.tag.ActionSetTourTag;
import net.tourbook.tag.TagManager;
import net.tourbook.tour.ActionEditQuick;
import net.tourbook.tour.ITourItem;
import net.tourbook.tour.ITourPropertyListener;
import net.tourbook.tour.SelectionDeletedTours;
import net.tourbook.tour.SelectionTourData;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.ActionEditTour;
import net.tourbook.ui.ActionModifyColumns;
import net.tourbook.ui.ActionOpenPrefDialog;
import net.tourbook.ui.ActionSetTourType;
import net.tourbook.ui.ColumnManager;
import net.tourbook.ui.ISelectedTours;
import net.tourbook.ui.ITourViewer;
import net.tourbook.ui.TableColumnDefinition;
import net.tourbook.ui.TableColumnFactory;
import net.tourbook.ui.UI;
import net.tourbook.util.PixelConverter;
import net.tourbook.util.PostSelectionProvider;
import net.tourbook.util.StringToArrayConverter;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.runtime.Preferences.IPropertyChangeListener;
import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.DeviceResourceException;
import org.eclipse.jface.resource.ImageDescriptor;
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
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.XMLMemento;
import org.eclipse.ui.part.ViewPart;

/**
 * 
 */
public class RawDataView extends ViewPart implements ISelectedTours, ITourViewer {

	private static final String				FILESTRING_SEPARATOR			= "|";											//$NON-NLS-1$

	public static final String				ID								= "net.tourbook.views.rawData.RawDataView";	//$NON-NLS-1$

	public static final int					COLUMN_DATE						= 0;
	public static final int					COLUMN_TITLE					= 1;
	public static final int					COLUMN_DATA_FORMAT				= 2;
	public static final int					COLUMN_FILE_NAME				= 3;

	private static final String				MEMENTO_SASH_CONTAINER			= "importview.sash.container.";				//$NON-NLS-1$
	private static final String				MEMENTO_IMPORT_FILENAME			= "importview.raw-data.filename";				//$NON-NLS-1$
	private static final String				MEMENTO_SELECTED_TOUR_INDEX		= "importview.selected-tour-index";			//$NON-NLS-1$

	private static final String				MEMENTO_MERGE_TRACKS			= "importview.action.merge-tracks";			//$NON-NLS-1$
	private static final String				MEMENTO_IS_CHECKSUM_VALIDATION	= "importview.action.is-checksum-validation";	//$NON-NLS-1$

	private static IMemento					fSessionMemento;

	private TableViewer						fTourViewer;

	private ActionClearView					fActionClearView;
	private ActionModifyColumns				fActionModifyColumns;
	private ActionSaveTourInDatabase		fActionSaveTour;
	private ActionSaveTourInDatabase		fActionSaveTourWithPerson;
	private ActionAdjustYear				fActionAdjustImportedYear;
	private ActionMergeTours				fActionMergeTours;
	private ActionDisableChecksumValidation	fActionDisableChecksumValidation;
	private ActionSetTourType				fActionSetTourType;
	private ActionEditQuick					fActionEditQuick;
	private ActionEditTour					fActionEditTour;
	private ActionSetTourTag				fActionAddTag;
	private ActionSetTourTag				fActionRemoveTag;
	private ActionRemoveAllTags				fActionRemoveAllTags;
	private ActionOpenPrefDialog			fActionOpenTagPrefs;

	private ImageDescriptor					imageDatabaseDescriptor;
	private ImageDescriptor					imageDatabaseOtherPersonDescriptor;
	private ImageDescriptor					imageDatabasePlaceholderDescriptor;
	private Image							imageDatabase;
	private Image							imageDatabaseOtherPerson;
	private Image							imageDatabasePlaceholder;

	private IPartListener2					fPartListener;
	private ISelectionListener				fPostSelectionListener;
	private IPropertyChangeListener			fPrefChangeListener;
	private PostSelectionProvider			fPostSelectionProvider;
	private ITourPropertyListener			fTourPropertyListener;

	public Calendar							fCalendar						= GregorianCalendar.getInstance();
	private DateFormat						fDateFormatter					= DateFormat.getDateInstance(DateFormat.SHORT);
	private DateFormat						fTimeFormatter					= DateFormat.getTimeInstance(DateFormat.SHORT);
	private NumberFormat					fNumberFormatter				= NumberFormat.getNumberInstance();
	private DateFormat						fDurationFormatter				= DateFormat.getTimeInstance(DateFormat.SHORT,
																					Locale.GERMAN);

	protected TourPerson					fActivePerson;
	protected TourPerson					fNewActivePerson;

	protected boolean						fIsPartVisible					= false;
	protected boolean						fIsViewerPersonDataDirty		= false;

	private ColumnManager					fColumnManager;

	private Composite						fViewerContainer;

	private class TourDataContentProvider implements IStructuredContentProvider {

		public TourDataContentProvider() {}

		public void dispose() {}

		public Object[] getElements(final Object parent) {
			return (Object[]) (parent);
		}

		public void inputChanged(final Viewer v, final Object oldInput, final Object newInput) {}
	}

	private void addPartListener() {
		fPartListener = new IPartListener2() {
			public void partActivated(final IWorkbenchPartReference partRef) {
//				disableTourChartSelection();
			}

			public void partBroughtToTop(final IWorkbenchPartReference partRef) {}

			public void partClosed(final IWorkbenchPartReference partRef) {
				if (ID.equals(partRef.getId())) {
					saveSettings();
				}
			}

			public void partDeactivated(final IWorkbenchPartReference partRef) {
				if (ID.equals(partRef.getId())) {
					saveSettings();
				}
			}

			public void partHidden(final IWorkbenchPartReference partRef) {
				if (RawDataView.this == partRef.getPart(false)) {
					fIsPartVisible = false;
				}
			}

			public void partInputChanged(final IWorkbenchPartReference partRef) {}

			public void partOpened(final IWorkbenchPartReference partRef) {}

			public void partVisible(final IWorkbenchPartReference partRef) {
				if (RawDataView.this == partRef.getPart(false)) {
					fIsPartVisible = true;
					if (fIsViewerPersonDataDirty || (fNewActivePerson != fActivePerson)) {
						reloadViewer();
						updateViewerPersonData();
						fNewActivePerson = fActivePerson;
						fIsViewerPersonDataDirty = false;
					}
				}
			}
		};
		getViewSite().getPage().addPartListener(fPartListener);
	}

	private void addPrefListener() {

		final Preferences prefStore = TourbookPlugin.getDefault().getPluginPreferences();

		fPrefChangeListener = new Preferences.IPropertyChangeListener() {
			public void propertyChange(final Preferences.PropertyChangeEvent event) {

				final String property = event.getProperty();

				if (property.equals(ITourbookPreferences.APP_DATA_FILTER_IS_MODIFIED)) {
					if (fIsPartVisible) {
						updateViewerPersonData();
					} else {
						// keep new active person until the view is visible
						fNewActivePerson = TourbookPlugin.getDefault().getActivePerson();
					}

				} else if (property.equals(ITourbookPreferences.TOUR_PERSON_LIST_IS_MODIFIED)) {
					fActionSaveTour.resetPeopleList();

				} else if (property.equals(ITourbookPreferences.TOUR_TYPE_LIST_IS_MODIFIED)) {

					// update tour type in the raw data
					RawDataManager.getInstance().updateTourDataFromDb();

					fTourViewer.refresh();

				} else if (property.equals(ITourbookPreferences.MEASUREMENT_SYSTEM)) {

					// measurement system has changed

					UI.updateUnits();

					fColumnManager.saveState(fSessionMemento);
					fColumnManager.resetColumns();
					defineViewerColumns(fViewerContainer);

					recreateViewer();

				} else if (property.equals(ITourbookPreferences.TAG_COLOR_AND_LAYOUT_CHANGED)) {

					fTourViewer.getTable()
							.setLinesVisible(prefStore.getBoolean(ITourbookPreferences.TAG_VIEW_SHOW_LINES));

					fTourViewer.refresh();

					/*
					 * the tree must be redrawn because the styled text does not show with the new
					 * color
					 */
					fTourViewer.getTable().redraw();
				}
			}
		};

		prefStore.addPropertyChangeListener(fPrefChangeListener);
	}

	private void addSelectionListener() {

		fPostSelectionListener = new ISelectionListener() {
			public void selectionChanged(final IWorkbenchPart part, final ISelection selection) {

				if (!selection.isEmpty() && selection instanceof SelectionDeletedTours) {

					final SelectionDeletedTours tourSelection = (SelectionDeletedTours) selection;
					final ArrayList<ITourItem> removedTours = tourSelection.removedTours;

					if (removedTours.size() == 0) {
						return;
					}

					if (fIsPartVisible) {

						RawDataManager.getInstance().updateTourDataFromDb();

						// update the table viewer
						reloadViewer();
					} else {
						fIsViewerPersonDataDirty = true;
					}
				}
			}
		};
		getSite().getPage().addPostSelectionListener(fPostSelectionListener);
	}

	private void addTourPropertyListener() {

		fTourPropertyListener = new ITourPropertyListener() {
			@SuppressWarnings("unchecked")//$NON-NLS-1$
			public void propertyChanged(final int propertyId, final Object propertyData) {
				if (propertyId == TourManager.TOUR_PROPERTIES_CHANGED) {

					// update modified tours
					final ArrayList<TourData> modifiedTours = (ArrayList<TourData>) propertyData;

					fTourViewer.update(modifiedTours.toArray(), null);

				} else if (propertyId == TourManager.TAG_STRUCTURE_CHANGED) {

					RawDataManager.getInstance().updateTourDataFromDb();

					reloadViewer();
				}
			}
		};
		TourManager.getInstance().addPropertyListener(fTourPropertyListener);
	}

	private void createActions() {

		fActionEditTour = new ActionEditTour(this);

		fActionClearView = new ActionClearView(this);
		fActionModifyColumns = new ActionModifyColumns(this);
		fActionSaveTour = new ActionSaveTourInDatabase(this);
		fActionSaveTourWithPerson = new ActionSaveTourInDatabase(this);
		fActionSetTourType = new ActionSetTourType(this);
		fActionEditQuick = new ActionEditQuick(this);
		fActionAdjustImportedYear = new ActionAdjustYear(this);
		fActionMergeTours = new ActionMergeTours(this);
		fActionDisableChecksumValidation = new ActionDisableChecksumValidation(this);

		fActionAddTag = new ActionSetTourTag(this, true);
		fActionRemoveTag = new ActionSetTourTag(this, false);
		fActionRemoveAllTags = new ActionRemoveAllTags(this);

		fActionOpenTagPrefs = new ActionOpenPrefDialog(Messages.app_action_tag_open_tagging_structure,
				ITourbookPreferences.PREF_PAGE_TAGS);

	}

	private void createChart() {

		final Object firstElement = ((IStructuredSelection) fTourViewer.getSelection()).getFirstElement();

		if (firstElement != null && firstElement instanceof TourData) {
			TourManager.getInstance().createTour((TourData) firstElement);
		}
	}

	/**
	 * create the views context menu
	 */
	private void createContextMenu() {

		final MenuManager menuMgr = new MenuManager("#PopupMenu"); //$NON-NLS-1$
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(final IMenuManager manager) {
				fillContextMenu(manager);
			}
		});

		final Menu menu = menuMgr.createContextMenu(fTourViewer.getControl());
		fTourViewer.getControl().setMenu(menu);

		getSite().registerContextMenu(menuMgr, fTourViewer);
	}

	@Override
	public void createPartControl(final Composite parent) {

		createResources();

		// define all columns
		fColumnManager = new ColumnManager(this, fSessionMemento);
		defineViewerColumns(parent);

		fViewerContainer = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().applyTo(fViewerContainer);

		createTourViewer(fViewerContainer);

		createActions();
		fillToolbar();

		addPartListener();
		addSelectionListener();
		addPrefListener();
		addTourPropertyListener();

		// set this view part as selection provider
		getSite().setSelectionProvider(fPostSelectionProvider = new PostSelectionProvider());

		fActivePerson = TourbookPlugin.getDefault().getActivePerson();

		restoreState(fSessionMemento);
	}

	private void createResources() {

		imageDatabaseDescriptor = TourbookPlugin.getImageDescriptor(Messages.Image__database);
		imageDatabaseOtherPersonDescriptor = TourbookPlugin.getImageDescriptor(Messages.Image__database_other_person);
		imageDatabasePlaceholderDescriptor = TourbookPlugin.getImageDescriptor(Messages.Image__database_placeholder);

		try {
			final Display display = Display.getCurrent();
			imageDatabase = (Image) imageDatabaseDescriptor.createResource(display);
			imageDatabaseOtherPerson = (Image) imageDatabaseOtherPersonDescriptor.createResource(display);
			imageDatabasePlaceholder = (Image) imageDatabasePlaceholderDescriptor.createResource(display);
		} catch (final DeviceResourceException e) {
			e.printStackTrace();
		}

	}

	/**
	 * @param parent
	 */
	private void createTourViewer(final Composite parent) {

		// parent.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));

		// table
		final Table table = new Table(parent, SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.MULTI);

		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		table.setHeaderVisible(true);
		table.setLinesVisible(true);

		fTourViewer = new TableViewer(table);
		fColumnManager.createColumns();

		// table viewer
		fTourViewer.setContentProvider(new TourDataContentProvider());
		fTourViewer.setSorter(new DeviceImportSorter());

		fTourViewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(final DoubleClickEvent event) {
				createChart();
			}
		});

		fTourViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(final SelectionChangedEvent event) {
				final IStructuredSelection selection = (IStructuredSelection) fTourViewer.getSelection();

				final TourData tourData = (TourData) selection.getFirstElement();

				enableActions();

				if (tourData != null) {
					fPostSelectionProvider.setSelection(new SelectionTourData(null, tourData));
				}
			}
		});

		createContextMenu();
	}

	/**
	 * Defines all columns for the table viewer in the column manager
	 * 
	 * @param parent
	 */
	private void defineViewerColumns(final Composite parent) {

		final PixelConverter pixelConverter = new PixelConverter(parent);
		TableColumnDefinition colDef;

		/*
		 * column: database indicator
		 */
		colDef = TableColumnFactory.DB_STATUS.createColumn(fColumnManager, pixelConverter);
//		colDef.setColumnResizable(false);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				// show the database indicator for the person who owns the tour
				final TourPerson tourPerson = ((TourData) cell.getElement()).getTourPerson();
				final long activePersonId = fActivePerson == null ? -1 : fActivePerson.getPersonId();

				cell.setImage(tourPerson == null
						? imageDatabasePlaceholder
						: tourPerson.getPersonId() == activePersonId ? imageDatabase : imageDatabaseOtherPerson);
			}
		});

		/*
		 * column: date
		 */
		colDef = TableColumnFactory.TOUR_DATE.createColumn(fColumnManager, pixelConverter);
		colDef.setCanModifyVisibility(false);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final TourData tourData = (TourData) cell.getElement();

				fCalendar.set(tourData.getStartYear(), tourData.getStartMonth() - 1, tourData.getStartDay());
				cell.setText(fDateFormatter.format(fCalendar.getTime()));
			}
		});
		colDef.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent event) {
				((DeviceImportSorter) fTourViewer.getSorter()).doSort(COLUMN_DATE);
				fTourViewer.refresh();
			}
		});

		/*
		 * column: time
		 */
		colDef = TableColumnFactory.TOUR_START_TIME.createColumn(fColumnManager, pixelConverter);
		colDef.setCanModifyVisibility(false);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final TourData tourData = (TourData) cell.getElement();
				fCalendar.set(0, 0, 0, tourData.getStartHour(), tourData.getStartMinute(), 0);

				cell.setText(fTimeFormatter.format(fCalendar.getTime()));
			}
		});
		colDef.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent event) {
				((DeviceImportSorter) fTourViewer.getSorter()).doSort(COLUMN_DATE);
				fTourViewer.refresh();
			}
		});

		/*
		 * column: tour type
		 */
		colDef = TableColumnFactory.TOUR_TYPE.createColumn(fColumnManager, pixelConverter);
//		colDef.setColumnResizable(false);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
				final TourType tourType = ((TourData) cell.getElement()).getTourType();
				if (tourType != null) {
					cell.setImage(UI.getInstance().getTourTypeImage(tourType.getTypeId()));
				}
			}
		});

		/*
		 * column: tags
		 */
		colDef = TableColumnFactory.TOUR_TAGS.createColumn(fColumnManager, pixelConverter);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final Object element = cell.getElement();
				final Set<TourTag> tourTags = ((TourData) element).getTourTags();

				if (tourTags.size() == 0) {

					// the tags could have been removed, set empty field

					cell.setText(UI.EMPTY_STRING);

				} else {

					// convert the tags into a list of tag ids 
					final ArrayList<Long> tagIds = new ArrayList<Long>();
					for (final TourTag tourTag : tourTags) {
						tagIds.add(tourTag.getTagId());
					}

					cell.setText(TourDatabase.getTagNames(tagIds));
				}
			}
		});

		/*
		 * column: recording time
		 */
		colDef = TableColumnFactory.RECORDING_TIME.createColumn(fColumnManager, pixelConverter);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final int recordingTime = ((TourData) cell.getElement()).getTourRecordingTime();

				if (recordingTime != 0) {
					fCalendar.set(0,
							0,
							0,
							recordingTime / 3600,
							((recordingTime % 3600) / 60),
							((recordingTime % 3600) % 60));

					cell.setText(fDurationFormatter.format(fCalendar.getTime()));
				}
			}
		});

		/*
		 * column: driving time
		 */
		colDef = TableColumnFactory.DRIVING_TIME.createColumn(fColumnManager, pixelConverter);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final int drivingTime = ((TourData) cell.getElement()).getTourDrivingTime();

				if (drivingTime != 0) {
					fCalendar.set(0, 0, 0, drivingTime / 3600, ((drivingTime % 3600) / 60), ((drivingTime % 3600) % 60));

					cell.setText(fDurationFormatter.format(fCalendar.getTime()));
				}
			}
		});

		/*
		 * column: distance (km/mile)
		 */
		colDef = TableColumnFactory.DISTANCE.createColumn(fColumnManager, pixelConverter);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
				final int tourDistance = ((TourData) cell.getElement()).getTourDistance();
				if (tourDistance != 0) {
					fNumberFormatter.setMinimumFractionDigits(2);
					fNumberFormatter.setMaximumFractionDigits(2);
					cell.setText(fNumberFormatter.format(((float) tourDistance) / 1000 / UI.UNIT_VALUE_DISTANCE));
				}
			}
		});

		/*
		 * column: speed
		 */
		colDef = TableColumnFactory.SPEED.createColumn(fColumnManager, pixelConverter);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
				final TourData tourData = ((TourData) cell.getElement());
				final int tourDistance = tourData.getTourDistance();
				final int drivingTime = tourData.getTourDrivingTime();
				if (drivingTime != 0) {
					fNumberFormatter.setMinimumFractionDigits(1);
					fNumberFormatter.setMaximumFractionDigits(1);
					cell.setText(fNumberFormatter.format(((float) tourDistance)
							/ drivingTime
							* 3.6
							/ UI.UNIT_VALUE_DISTANCE));
				}
			}
		});

		/*
		 * column: altitude up
		 */
		colDef = TableColumnFactory.ALTITUDE_UP.createColumn(fColumnManager, pixelConverter);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
				final int tourAltUp = ((TourData) cell.getElement()).getTourAltUp();
				if (tourAltUp != 0) {
					cell.setText(Long.toString((long) (tourAltUp / UI.UNIT_VALUE_ALTITUDE)));
				}
			}
		});

		/*
		 * column: altitude down
		 */
		colDef = TableColumnFactory.ALTITUDE_DOWN.createColumn(fColumnManager, pixelConverter);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
				final int tourAltDown = ((TourData) cell.getElement()).getTourAltDown();
				if (tourAltDown != 0) {
					cell.setText(Long.toString((long) (-tourAltDown / UI.UNIT_VALUE_ALTITUDE)));
				}
			}
		});

		/*
		 * column: tour title
		 */
		colDef = TableColumnFactory.TOUR_TITLE.createColumn(fColumnManager, pixelConverter);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
				final TourData tourData = (TourData) cell.getElement();
				cell.setText(tourData.getTourTitle());
			}
		});
		colDef.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent event) {
				((DeviceImportSorter) fTourViewer.getSorter()).doSort(COLUMN_TITLE);
				fTourViewer.refresh();
			}
		});

		colDef = TableColumnFactory.DEVICE_NAME.createColumn(fColumnManager, pixelConverter);
		colDef.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent event) {
				((DeviceImportSorter) fTourViewer.getSorter()).doSort(COLUMN_DATA_FORMAT);
				fTourViewer.refresh();
			}
		});

		TableColumnFactory.DEVICE_PROFILE.createColumn(fColumnManager, pixelConverter);
		TableColumnFactory.TIME_INTERVAL.createColumn(fColumnManager, pixelConverter);

		colDef = TableColumnFactory.IMPORT_FILE_NAME.createColumn(fColumnManager, pixelConverter);
		colDef.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent event) {
				((DeviceImportSorter) fTourViewer.getSorter()).doSort(COLUMN_FILE_NAME);
				fTourViewer.refresh();
			}
		});

		TableColumnFactory.IMPORT_FILE_PATH.createColumn(fColumnManager, pixelConverter);
	}

	@Override
	public void dispose() {

		if (imageDatabase != null) {
			imageDatabaseDescriptor.destroyResource(imageDatabase);
		}
		if (imageDatabaseOtherPerson != null) {
			imageDatabaseOtherPersonDescriptor.destroyResource(imageDatabaseOtherPerson);
		}
		if (imageDatabasePlaceholder != null) {
			imageDatabasePlaceholderDescriptor.destroyResource(imageDatabasePlaceholder);
		}

		getViewSite().getPage().removePartListener(fPartListener);
		getSite().getPage().removeSelectionListener(fPostSelectionListener);
		TourManager.getInstance().removePropertyListener(fTourPropertyListener);

		TourbookPlugin.getDefault().getPluginPreferences().removePropertyChangeListener(fPrefChangeListener);

		super.dispose();
	}

	private void enableActions() {

		final StructuredSelection selection = (StructuredSelection) fTourViewer.getSelection();

		final int selectedItems = selection.size();
		int unsavedTours = 0;
		int savedTours = 0;
		TourData firstTour = null;

		for (final Iterator<?> iter = selection.iterator(); iter.hasNext();) {
			final Object treeItem = iter.next();
			if (treeItem instanceof TourData) {

				final TourData tourData = (TourData) treeItem;
				if (tourData.getTourPerson() == null) {
					unsavedTours++;
				} else {

					if (savedTours == 0) {
						firstTour = tourData;
					}

					savedTours++;
				}
			}
		}
		final boolean isTourSelected = savedTours > 0;

		final TourPerson person = TourbookPlugin.getDefault().getActivePerson();
		if (person != null) {
			fActionSaveTourWithPerson.setText(NLS.bind(Messages.import_data_action_save_tour_with_person,
					person.getName()));
			fActionSaveTourWithPerson.setPerson(person);
		}
		fActionSaveTourWithPerson.setEnabled(person != null && unsavedTours > 0);

		if (selection.size() == 1) {
			fActionSaveTour.setText(Messages.import_data_action_save_tour_for_person);
		} else {
			fActionSaveTour.setText(Messages.import_data_action_save_tours_for_person);
		}
		fActionSaveTour.setEnabled(unsavedTours > 0);

		fActionEditQuick.setEnabled(selectedItems == 1 && savedTours == 1);

		final ArrayList<TourType> tourTypes = TourDatabase.getTourTypes();
		fActionSetTourType.setEnabled(isTourSelected && tourTypes.size() > 0);

		fActionAddTag.setEnabled(isTourSelected);

		/*
		 * enable/disable remove actions
		 */
		if (firstTour != null && savedTours == 1) {

			// one tour is selected

			final Set<TourTag> tourTags = firstTour.getTourTags();
			if (tourTags != null && tourTags.size() > 0) {

				// at least one tag is within the tour

				fActionRemoveAllTags.setEnabled(true);
				fActionRemoveTag.setEnabled(true);
			} else {
				// tags are not available
				fActionRemoveAllTags.setEnabled(false);
				fActionRemoveTag.setEnabled(false);
			}
		} else {

			// multiple tours are selected

			fActionRemoveTag.setEnabled(isTourSelected);
			fActionRemoveAllTags.setEnabled(isTourSelected);
		}

		// enable/disable actions for the recent tags
		TagManager.enableRecentTagActions(isTourSelected);

	}

	private void fillContextMenu(final IMenuManager menuMgr) {

		menuMgr.add(fActionSaveTourWithPerson);
		menuMgr.add(fActionSaveTour);

		menuMgr.add(new Separator());
		menuMgr.add(fActionEditQuick);
		menuMgr.add(fActionSetTourType);
		menuMgr.add(fActionEditTour);

		menuMgr.add(new Separator());
		menuMgr.add(fActionAddTag);
		menuMgr.add(fActionRemoveTag);
		menuMgr.add(fActionRemoveAllTags);

		TagManager.fillRecentTagsIntoMenu(menuMgr, this, true);

		menuMgr.add(new Separator());
		menuMgr.add(fActionOpenTagPrefs);

		// add standard group which allows other plug-ins to contribute here
		menuMgr.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));

		enableActions();
	}

	private void fillToolbar() {
		/*
		 * fill view toolbar
		 */
		final IToolBarManager viewTbm = getViewSite().getActionBars().getToolBarManager();

		// place for import and transfer actions
		viewTbm.add(new GroupMarker("import")); //$NON-NLS-1$

		viewTbm.add(fActionClearView);

		/*
		 * fill view menu
		 */
		final IMenuManager menuMgr = getViewSite().getActionBars().getMenuManager();

		menuMgr.add(fActionMergeTours);
		menuMgr.add(fActionDisableChecksumValidation);
		menuMgr.add(fActionAdjustImportedYear);

		menuMgr.add(new Separator());
		menuMgr.add(fActionModifyColumns);
	}

	void fireSelectionEvent(final ISelection selection) {
		fPostSelectionProvider.setSelection(selection);
	}

	@SuppressWarnings("unchecked")//$NON-NLS-1$
	@Override
	public Object getAdapter(final Class adapter) {

		if (adapter == ColumnViewer.class) {
			return fTourViewer;
		}

		return Platform.getAdapterManager().getAdapter(this, adapter);
	}

	public ColumnManager getColumnManager() {
		return fColumnManager;
	}

	public ArrayList<TourData> getSelectedTours() {

		final TourManager tourManager = TourManager.getInstance();

		// get selected tours
		final IStructuredSelection selectedTours = ((IStructuredSelection) fTourViewer.getSelection());

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

	public ColumnViewer getViewer() {
		return fTourViewer;
	}

	@Override
	public void init(final IViewSite site, final IMemento memento) throws PartInitException {

		super.init(site, memento);

		// set the session memento
		if (fSessionMemento == null) {
			fSessionMemento = memento;
		}
	}

	public boolean isFromTourEditor() {
		return false;
	}

	public void recreateViewer() {

		fViewerContainer.setRedraw(false);
		{
			fTourViewer.getTable().dispose();
			createTourViewer(fViewerContainer);
			fViewerContainer.layout();

			// update the viewer
			reloadViewer();
		}
		fViewerContainer.setRedraw(true);
	}

	public void reloadViewer() {

		// update tour data viewer
		fTourViewer.setInput(RawDataManager.getInstance().getTourDataMap().values().toArray());
	}

	private void restoreState(final IMemento memento) {

		final RawDataManager rawDataManager = RawDataManager.getInstance();

		if (memento == null) {

			fActionMergeTours.setChecked(true);
			rawDataManager.setMergeTracks(true);

			// enable checksum validation
			fActionDisableChecksumValidation.setChecked(false);
			rawDataManager.setIsChecksumValidation(true);

		} else {

			// restore: set merge tracks status befor the tours are imported
			final Integer mergeTracks = memento.getInteger(MEMENTO_MERGE_TRACKS);
			if (mergeTracks == null) {
				fActionMergeTours.setChecked(false);
			} else {
				fActionMergeTours.setChecked(mergeTracks == 1 ? true : false);
			}
			rawDataManager.setMergeTracks(fActionMergeTours.isChecked());

			// restore: is checksum validation
			final Integer isChecksumValidation = memento.getInteger(MEMENTO_IS_CHECKSUM_VALIDATION);
			if (isChecksumValidation == null) {
				// enable checksum validation
				fActionDisableChecksumValidation.setChecked(false);
			} else {
				fActionDisableChecksumValidation.setChecked(isChecksumValidation == 1 ? false : true);
			}
			rawDataManager.setIsChecksumValidation(fActionDisableChecksumValidation.isChecked() == false);

			// restore imported tours
			final String mementoImportedFiles = memento.getString(MEMENTO_IMPORT_FILENAME);
			final ArrayList<String> notImportedFiles = new ArrayList<String>();

			if (mementoImportedFiles != null) {

				rawDataManager.getTourDataMap().clear();

				final String[] files = StringToArrayConverter.convertStringToArray(mementoImportedFiles,
						FILESTRING_SEPARATOR);
				int importCounter = 0;

				// loop: import all files
				for (final String fileName : files) {

					final File file = new File(fileName);
					if (file.exists()) {
						if (rawDataManager.importRawData(file, null, false, null)) {
							importCounter++;
						} else {
							notImportedFiles.add(fileName);
						}
					}
				}

				if (importCounter > 0) {

					rawDataManager.updateTourDataFromDb();
					reloadViewer();

					// restore selected tour
					final Integer selectedTourIndex = memento.getInteger(MEMENTO_SELECTED_TOUR_INDEX);

					final Object tourData = fTourViewer.getElementAt(selectedTourIndex);

					Display.getDefault().asyncExec(new Runnable() {
						public void run() {
							if (tourData != null) {
								fTourViewer.setSelection(new StructuredSelection(tourData), true);
							}
						}
					});
				}
			}

			if (notImportedFiles.size() > 0) {
				RawDataManager.showMsgBoxInvalidFormat(notImportedFiles);
			}

		}

	}

	private void saveSettings() {
		fSessionMemento = XMLMemento.createWriteRoot("DeviceImportView"); //$NON-NLS-1$
		saveState(fSessionMemento);
	}

	@Override
	public void saveState(final IMemento memento) {

		// save sash weights
		final Table table = fTourViewer.getTable();

		if (table.isDisposed()) {
			return;
		}

		memento.putInteger(MEMENTO_SASH_CONTAINER, table.getSize().x);

		final RawDataManager rawDataMgr = RawDataManager.getInstance();

		// save imported file names
		final HashSet<String> importedFiles = rawDataMgr.getImportedFiles();
		memento.putString(MEMENTO_IMPORT_FILENAME,
				StringToArrayConverter.convertArrayToString(importedFiles.toArray(new String[importedFiles.size()]),
						FILESTRING_SEPARATOR));

		// save selected tour in the viewer
		memento.putInteger(MEMENTO_SELECTED_TOUR_INDEX, table.getSelectionIndex());

		memento.putInteger(MEMENTO_MERGE_TRACKS, fActionMergeTours.isChecked() ? 1 : 0);
		memento.putInteger(MEMENTO_IS_CHECKSUM_VALIDATION, fActionDisableChecksumValidation.isChecked() ? 0 : 1);

		fColumnManager.saveState(memento);
	}

	/**
	 * select first tour in the viewer
	 */
	public void selectFirstTour() {

		final TourData firstTourData = (TourData) fTourViewer.getElementAt(0);
		if (firstTourData != null) {
			fTourViewer.setSelection(new StructuredSelection(firstTourData), true);
		}
	}

	void selectLastTour() {

		final Collection<TourData> tourDataCollection = RawDataManager.getInstance().getTourDataMap().values();

		final TourData[] tourList = tourDataCollection.toArray(new TourData[tourDataCollection.size()]);

		// select the last tour in the viewer
		if (tourList.length > 0) {
			final TourData tourData = tourList[0];
			fTourViewer.setSelection(new StructuredSelection(tourData), true);
		}
	}

	/**
	 * Passing the focus request to the viewer's control.
	 */
	@Override
	public void setFocus() {
		fTourViewer.getControl().setFocus();
	}

	/**
	 * when the active person was modified, the view must be updated
	 */
	private void updateViewerPersonData() {

		fActivePerson = TourbookPlugin.getDefault().getActivePerson();

		// update person in the raw data
		RawDataManager.getInstance().updateTourDataFromDb();

		fTourViewer.refresh();
	}
}
