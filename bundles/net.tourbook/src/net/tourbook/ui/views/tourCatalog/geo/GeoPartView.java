/*******************************************************************************
 * Copyright (C) 2005, 2018 Wolfgang Schramm and Contributors
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
package net.tourbook.ui.views.tourCatalog.geo;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.chart.Chart;
import net.tourbook.chart.ChartDataModel;
import net.tourbook.chart.SelectionChartInfo;
import net.tourbook.chart.SelectionChartXSliderPosition;
import net.tourbook.common.UI;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.util.ColumnDefinition;
import net.tourbook.common.util.ColumnManager;
import net.tourbook.common.util.ITourViewer;
import net.tourbook.common.util.TableColumnDefinition;
import net.tourbook.common.util.Util;
import net.tourbook.data.NormalizedGeoData;
import net.tourbook.data.TourData;
import net.tourbook.data.TourReference;
import net.tourbook.importdata.RawDataManager;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tour.ITourEventListener;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.TableColumnFactory;
import net.tourbook.ui.tourChart.TourChart;
import net.tourbook.ui.views.tourCatalog.ReferenceTourManager;
import net.tourbook.ui.views.tourCatalog.SelectionTourCatalogView;
import net.tourbook.ui.views.tourCatalog.TVICatalogComparedTour;
import net.tourbook.ui.views.tourCatalog.TVICompareResultComparedTour;
import net.tourbook.ui.views.tourCatalog.TourCompareConfig;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.part.PageBook;
import org.eclipse.ui.part.ViewPart;

public class GeoPartView extends ViewPart implements ITourViewer {

// SET_FORMATTING_OFF
	
	public static final String				ID									= "net.tourbook.ui.views.tourCatalog.geo.GeoPartView";	//$NON-NLS-1$
                                            
	private static final String				STATE_IS_LISTEN_TO_SLIDER_POSITION	= "STATE_IS_LISTEN_TO_SLIDER_POSITION";		//$NON-NLS-1$
	private static final String				STATE_IS_USE_APP_FILTER				= "STATE_IS_USE_APP_FILTER";				//$NON-NLS-1$
	
// SET_FORMATTING_ON

	private static final String				COLUMN_GEO_DIFF						= "geoDiff";							//$NON-NLS-1$
	private static final String				COLUMN_TOUR_START_DATE				= "tourStartDate";						//$NON-NLS-1$

	private static final IDialogSettings	_state								= TourbookPlugin.getState(ID);
	private final IPreferenceStore			_prefStore							= TourbookPlugin.getPrefStore();

	private IPartListener2					_partListener;
	private SelectionAdapter				_columnSortListener;
	private SelectionAdapter				_defaultSelectionListener;
	private IPropertyChangeListener			_prefChangeListener;
	private ISelectionListener				_postSelectionListener;
	private ITourEventListener				_tourEventListener;

	private int								_lastSelectionHash;

	private long							_prevTourId							= Long.MIN_VALUE;
	private int								_prevFirstIndex;
	private int								_prevLastIndex;
	private int[]							_geoParts;
	private int								_geoPartTours;

	private NormalizedGeoData				_normalizedTourPart;
	private AtomicInteger					_workedTours						= new AtomicInteger();
	private AtomicInteger					_runningId							= new AtomicInteger();

	private long							_workerExecutorId;

	private boolean							_isCompareEnabled;
	private boolean							_isUseAppFilter;
	private boolean							_isInUpdate;
	private long							_lastUIUpdate;

	/**
	 * Comparer items from the last comparison
	 */
	private ArrayList<GeoPartComparerItem>	_comparedTours						= new ArrayList<>();

	private GeoPartItem						_previousGeoPartItem;

	private ActionAppTourFilter				_actionAppTourFilter;
	private ActionOnOff						_actionOnOff;

	private TableViewer						_geoPartViewer;
	private ColumnManager					_columnManager;
	private CompareResultComparator			_geoPartComparator					= new CompareResultComparator();

	private PixelConverter					_pc;

	/*
	 * UI controls
	 */
	private Composite						_parent;
	private Composite						_viewerContainer;

	private PageBook						_pageBook;
	private Composite						_pageNoData;
	private Composite						_pageContent;

	private Label							_lblNumGeoParts;
	private Label							_lblNumSlices;
	private Label							_lblNumTours;
	private Label							_lblProgressBar;
	private Label							_lblSqlRuntime;

	private ProgressBar						_progressBar;

	private class ActionAppTourFilter extends Action {

		public ActionAppTourFilter() {

			super(null, AS_CHECK_BOX);

			setToolTipText(Messages.GeoPart_View_Action_AppFilter_Tooltip);
			setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__App_Filter));
			setDisabledImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__App_Filter_Disabled));
		}

		@Override
		public void run() {
			onAction_AppFilter(isChecked());
		}
	}

	private class ActionOnOff extends Action {

		public ActionOnOff() {

			super(null, AS_CHECK_BOX);

			setToolTipText(Messages.GeoPart_View_Action_OnOff_Tooltip);
			setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__App_Turn_On));
		}

		@Override
		public void run() {
			onAction_OnOff(isChecked());
		}

		private void setIcon(final boolean isSelected) {

			// switch icon
			if (isSelected) {
				setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__App_Turn_On));
			} else {
				setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__App_Turn_Off));
			}
		}
	}

	private class CompareResultComparator extends ViewerComparator {

		private static final int	ASCENDING		= 0;
		private static final int	DESCENDING		= 1;

		private String				__sortColumnId	= COLUMN_GEO_DIFF;
		private int					__sortDirection	= ASCENDING;

		@Override
		public int compare(final Viewer viewer, final Object e1, final Object e2) {

			final GeoPartComparerItem item1 = (GeoPartComparerItem) e1;
			final GeoPartComparerItem item2 = (GeoPartComparerItem) e2;

			boolean _isSortByTime = false;
			double rc = 0;

			// Determine which column and do the appropriate sort
			switch (__sortColumnId) {

			case COLUMN_GEO_DIFF:

				final long minDiffValue1 = item1.minDiffValue;
				final long minDiffValue2 = item2.minDiffValue;

				if (minDiffValue1 >= 0 && minDiffValue2 >= 0) {

					rc = minDiffValue1 - minDiffValue2;

				} else if (minDiffValue1 >= 0) {

					rc = -Integer.MAX_VALUE;

				} else if (minDiffValue2 >= 0) {

					rc = Integer.MAX_VALUE;

				} else {

					rc = minDiffValue1 - minDiffValue2;
				}

				_isSortByTime = true;

				break;

			case COLUMN_TOUR_START_DATE:

				_isSortByTime = true;
				break;

//			case COLUMN_NAME:
			default:
//				rc = item1.label.compareTo(item2.label);
				_isSortByTime = true;
			}

			if (rc == 0 && _isSortByTime) {
				rc = item1.tourStartTimeMS - item2.tourStartTimeMS;
			}

			// If descending order, flip the direction
			if (__sortDirection == DESCENDING) {
				rc = -rc;
			}

			/*
			 * MUST return 1 or -1 otherwise long values are not sorted correctly.
			 */
			return rc > 0 //
					? 1
					: rc < 0 //
							? -1
							: 0;
		}

		@Override
		public boolean isSorterProperty(final Object element, final String property) {

			// force resorting when a name is renamed
			return true;
		}

		public void setSortColumn(final Widget widget) {

			final ColumnDefinition columnDefinition = (ColumnDefinition) widget.getData();
			final String columnId = columnDefinition.getColumnId();

			if (columnId.equals(__sortColumnId)) {

				// Same column as last sort; toggle the direction

				__sortDirection = 1 - __sortDirection;

			} else {

				// New column; do an ascent sorting

				__sortColumnId = columnId;
				__sortDirection = ASCENDING;
			}

			updateUI_SetSortDirection(__sortColumnId, __sortDirection);
		}
	}

	private class CompareResultProvider implements IStructuredContentProvider {

		@Override
		public void dispose() {}

		@Override
		public Object[] getElements(final Object inputElement) {
			return _comparedTours.toArray();
		}

		@Override
		public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {}
	}

	private void addPartListener() {

		_partListener = new IPartListener2() {

			@Override
			public void partActivated(final IWorkbenchPartReference partRef) {

				if (partRef.getPart(false) == GeoPartView.this) {

				}
			}

			@Override
			public void partBroughtToTop(final IWorkbenchPartReference partRef) {}

			@Override
			public void partClosed(final IWorkbenchPartReference partRef) {

				if (partRef.getPart(false) == GeoPartView.this) {

					saveState();
					setState_CancelComparing();
				}
			}

			@Override
			public void partDeactivated(final IWorkbenchPartReference partRef) {}

			@Override
			public void partHidden(final IWorkbenchPartReference partRef) {
				if (partRef.getPart(false) == GeoPartView.this) {}
			}

			@Override
			public void partInputChanged(final IWorkbenchPartReference partRef) {}

			@Override
			public void partOpened(final IWorkbenchPartReference partRef) {}

			@Override
			public void partVisible(final IWorkbenchPartReference partRef) {
				if (partRef.getPart(false) == GeoPartView.this) {}
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

					recompareTours();
				}
			}
		};

		// register the listener
		_prefStore.addPropertyChangeListener(_prefChangeListener);
	}

	/**
	 * listen for events when a tour is selected
	 */
	private void addSelectionListener() {

		_postSelectionListener = new ISelectionListener() {
			@Override
			public void selectionChanged(final IWorkbenchPart part, final ISelection selection) {

				if (part == GeoPartView.this) {
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

				if (part == GeoPartView.this) {
					return;
				}

				if ((eventId == TourEventId.TOUR_SELECTION) && eventData instanceof ISelection) {

					onSelectionChanged((ISelection) eventData);

				} else if (eventId == TourEventId.SLIDER_POSITION_CHANGED && eventData instanceof ISelection) {

					onSelectionChanged((ISelection) eventData);
				}
			}
		};

		TourManager.getInstance().addTourEventListener(_tourEventListener);
	}

	private void compare_10_Compare(final TourData tourData,
									final int leftIndex,
									final int rightIndex) {

		if (_isCompareEnabled == false) {

			// ignore slider position
			return;
		}

		/*
		 * !!! This must be very early because it is running with a delay which could cause that an
		 * old is running !!!
		 */
		final int runnableRunningId = _runningId.incrementAndGet();

		final double[] latSerie = tourData.latitudeSerie;
		if (latSerie == null) {

			setState_StopComparing();

			_pageBook.showPage(_pageNoData);

			return;
		}

		/*
		 * Validate first/last indices
		 */
		int firstIndex = leftIndex < rightIndex ? leftIndex : rightIndex;
		int lastIndex = leftIndex > rightIndex ? leftIndex : rightIndex;
		if (firstIndex < 0) {
			firstIndex = 0;
		}
		if (lastIndex > latSerie.length) {
			lastIndex = latSerie.length;
		}

		final long tourId = tourData.getTourId();

		// skip same data or continue current comparison
		if (tourId == _prevTourId //
				&& leftIndex == _prevFirstIndex
				&& rightIndex == _prevLastIndex) {
			return;
		}

		GeoPartTourLoader.stopLoading(_previousGeoPartItem);

		final int runnableFirstIndex = firstIndex;
		final int runnableLastIndex = lastIndex;
		final TourData runnableTourData = tourData;

		_prevTourId = tourId;
		_prevFirstIndex = runnableFirstIndex;
		_prevLastIndex = runnableLastIndex;

		// delay tour comparator, moving the slider can occure very often
		_parent.getDisplay().timerExec(300, new Runnable() {

			private int __runningId = runnableRunningId;

			@Override
			public void run() {

				if (_parent.isDisposed()) {
					return;
				}

				if (__runningId != _runningId.get()) {

					// a newer runnable is created

					return;
				}

				compare_20_LoadGeoParts(runnableTourData, runnableFirstIndex, runnableLastIndex);
			}
		});
	}

	private void compare_20_LoadGeoParts(final TourData tourData, final int firstIndex, final int lastIndex) {

		// 1. get geo partitions from lat/lon first/last index
		_geoParts = tourData.computeGeo_Partitions(firstIndex, lastIndex);

		if (_geoParts == null) {

			_pageBook.showPage(_pageNoData);

			return;
		}

		_pageBook.showPage(_pageContent);

		_comparedTours.clear();
		updateUI_Viewer();

		// update UI
		final int numSlices = lastIndex - firstIndex;
		_lblNumSlices.setText(Integer.toString(numSlices));
		_lblNumGeoParts.setText(Integer.toString(_geoParts.length));
		updateUI_GeoItem(null);
		updateUI_Progress(0);

		/*
		 * Create geo data which should be compared
		 */
		_normalizedTourPart = tourData.computeGeo_NormalizedLatLon(firstIndex, lastIndex);

		// 2. load tour id's in the geo parts
		_previousGeoPartItem = GeoPartTourLoader.loadToursFromGeoParts(
				_geoParts,
				_normalizedTourPart,
				_isUseAppFilter,
				_previousGeoPartItem);
	}

	void compare_40_CompareTours(final GeoPartItem loaderItem) {

		_geoPartTours = loaderItem.tourIds.length;

		final long workerExecutorId[] = { 0 };

		if (_geoPartTours > 0) {

			_workedTours.set(0);

			_workerExecutorId = loaderItem.executorId;
			workerExecutorId[0] = _workerExecutorId;

			GeoPartTourComparer.compareGeoTours(loaderItem);
		}

		// update UI
		Display.getDefault().asyncExec(new Runnable() {
			@Override
			public void run() {

				if (_parent.isDisposed()) {
					return;
				}

				if (workerExecutorId[0] != _workerExecutorId) {
					// skip old tasks
					return;
				}

				_progressBar.setMaximum(_geoPartTours);

				updateUI_GeoItem(loaderItem);
			}
		});

	}

	void compare_50_TourIsCompared(final GeoPartComparerItem comparerItem) {

		final GeoPartItem geoPartItem = comparerItem.geoPartItem;

		if (geoPartItem.isCanceled || geoPartItem.executorId != _workerExecutorId) {
			return;
		}

		_comparedTours = geoPartItem.comparedTours;

		final int workedTours = _workedTours.incrementAndGet();

		final long now = System.currentTimeMillis();

		// update UI not too often until comparison is done
		if (now - _lastUIUpdate < 300 && workedTours != _geoPartTours) {
			return;
		}

		// reset paused time
		_lastUIUpdate = now;

		Display.getDefault().asyncExec(new Runnable() {
			@Override
			public void run() {

				if (_parent.isDisposed()) {
					return;
				}

				updateUI_Progress(workedTours);
				updateUI_Viewer();

				// fire geo part compare result
				TourManager.fireEventWithCustomData(
						TourEventId.GEO_PART_COMPARE,
						comparerItem.geoPartItem,
						GeoPartView.this);

				if (workedTours == _geoPartTours) {
					logCompareResult(geoPartItem);
				}
			}
		});
	}

	private void createActions() {

		_actionAppTourFilter = new ActionAppTourFilter();
		_actionOnOff = new ActionOnOff();
	}

	@Override
	public void createPartControl(final Composite parent) {

		initUI(parent);

		// define all columns for the viewer
		_columnManager = new ColumnManager(this, _state);
		defineAllColumns();

		createUI(parent);
		createActions();

		fillToolbar();

		addPartListener();
		addPrefListener();
		addTourEventListener();
		addSelectionListener();

		GeoPartTourLoader.geoPartView = this;
		GeoPartTourComparer.geoPartView = this;

		restoreState();

		_pageBook.showPage(_pageNoData);
	}

	private void createUI(final Composite parent) {

		_pageBook = new PageBook(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(_pageBook);

		_pageNoData = UI.createUI_PageNoData(_pageBook, Messages.GeoPart_View_Label_NoTourWithGeoData);

		_pageContent = new Composite(_pageBook, SWT.NONE);
		GridLayoutFactory.fillDefaults().applyTo(_pageContent);
		{
			final Composite container = new Composite(_pageContent, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
			GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
			{
				createUI_10_Comparator(container);

				_viewerContainer = new Composite(container, SWT.NONE);
				GridDataFactory.fillDefaults().grab(true, true).applyTo(_viewerContainer);
				GridLayoutFactory.fillDefaults().applyTo(_viewerContainer);
				{
					createUI_20_TableViewer(_viewerContainer);
				}
			}
		}
	}

	private void createUI_10_Comparator(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory
				.swtDefaults()//
				.numColumns(2)
				//				.spacing(10, 2)
				.applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
		{
			{
				/*
				 * Number of tours
				 */

				final Label label = new Label(container, SWT.NONE);
				label.setText("Tours"); //$NON-NLS-1$
				GridDataFactory.fillDefaults().applyTo(label);

				_lblNumTours = new Label(container, SWT.NONE);
				_lblNumTours.setText(UI.EMPTY_STRING);
				GridDataFactory.fillDefaults().grab(true, false).applyTo(_lblNumTours);
			}
			{
				/*
				 * Number of time slices
				 */

				final Label label = new Label(container, SWT.NONE);
				label.setText("Time Slices"); //$NON-NLS-1$
				GridDataFactory.fillDefaults().applyTo(label);

				_lblNumSlices = new Label(container, SWT.NONE);
				_lblNumSlices.setText(UI.EMPTY_STRING);
				GridDataFactory.fillDefaults().grab(true, false).applyTo(_lblNumSlices);
			}
			{
				/*
				 * Number of geo parts
				 */

				final Label label = new Label(container, SWT.NONE);
				label.setText("Geo Parts"); //$NON-NLS-1$
				GridDataFactory.fillDefaults().applyTo(label);

				_lblNumGeoParts = new Label(container, SWT.NONE);
				_lblNumGeoParts.setText(UI.EMPTY_STRING);
				GridDataFactory.fillDefaults().grab(true, false).applyTo(_lblNumGeoParts);
			}
			{
				/*
				 * SQL runtime
				 */

				final Label label = new Label(container, SWT.NONE);
				label.setText("SQL Runtime"); //$NON-NLS-1$
				GridDataFactory.fillDefaults().applyTo(label);

				_lblSqlRuntime = new Label(container, SWT.NONE);
				_lblSqlRuntime.setText(UI.EMPTY_STRING);
				GridDataFactory.fillDefaults().grab(true, false).applyTo(_lblSqlRuntime);
			}
			{
				/*
				 * Progress bar label
				 */
				_lblProgressBar = new Label(container, SWT.NONE);

				GridDataFactory
						.fillDefaults()
						.span(2, 1)
						.grab(true, false)
						.applyTo(_lblProgressBar);
			}
			{
				/*
				 * Progress bar
				 */
				_progressBar = new ProgressBar(container, SWT.HORIZONTAL);
				_progressBar.setMinimum(0);

				GridDataFactory
						.fillDefaults()
						.span(2, 1)
						.grab(true, false)
						.applyTo(_progressBar);
			}
		}
	}

	private void createUI_20_TableViewer(final Composite parent) {

		/*
		 * create table
		 */
		final Table table = new Table(parent, SWT.FULL_SELECTION /* | SWT.MULTI /* | SWT.BORDER */);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(table);

		table.setHeaderVisible(true);
		table.setLinesVisible(false);

		/*
		 * It took a while that the correct listener is set and also the checked item is fired and
		 * not the wrong selection.
		 */
		table.addListener(SWT.Selection, new Listener() {

			@Override
			public void handleEvent(final Event event) {
				onGeoPart_Select(event);
			}
		});
		/*
		 * create table viewer
		 */
		_geoPartViewer = new TableViewer(table);

		_columnManager.createColumns(_geoPartViewer);

		_geoPartViewer.setUseHashlookup(true);
		_geoPartViewer.setContentProvider(new CompareResultProvider());
		_geoPartViewer.setComparator(_geoPartComparator);

		_geoPartViewer.addSelectionChangedListener(new ISelectionChangedListener() {

			@Override
			public void selectionChanged(final SelectionChangedEvent event) {
				onGeoPart_Select();
			}
		});

		_geoPartViewer.addDoubleClickListener(new IDoubleClickListener() {

			@Override
			public void doubleClick(final DoubleClickEvent event) {
//				onBookmark_Rename(true);
			}
		});

		_geoPartViewer.getTable().addKeyListener(new KeyListener() {

			@Override
			public void keyPressed(final KeyEvent e) {

				switch (e.keyCode) {

				case SWT.DEL:
//					onBookmark_Delete();
					break;

				case SWT.F2:
//					onBookmark_Rename(false);
					break;

				default:
					break;
				}
			}

			@Override
			public void keyReleased(final KeyEvent e) {}
		});

		updateUI_SetSortDirection(//
				_geoPartComparator.__sortColumnId,
				_geoPartComparator.__sortDirection);

		createUI_22_ContextMenu();
	}

	/**
	 * Ceate the view context menus
	 */
	private void createUI_22_ContextMenu() {

		final MenuManager menuMgr = new MenuManager("#PopupMenu"); //$NON-NLS-1$

		menuMgr.setRemoveAllWhenShown(true);

		menuMgr.addMenuListener(new IMenuListener() {
			@Override
			public void menuAboutToShow(final IMenuManager manager) {
//				fillContextMenu(manager);
			}
		});

		final Table table = _geoPartViewer.getTable();
		final Menu tableHeaderContextMenu = menuMgr.createContextMenu(table);

		_columnManager.createHeaderContextMenu(table, tableHeaderContextMenu);
	}

	private void defineAllColumns() {

		defineColumn_10_GeoDiff();
		defineColumn_Time_TourStartDate();
	}

	/**
	 * Column: Name
	 */
	private void defineColumn_10_GeoDiff() {

		final TableColumnDefinition colDef = new TableColumnDefinition(_columnManager, COLUMN_GEO_DIFF, SWT.TRAIL);

		colDef.setColumnLabel(Messages.GeoPart_View_Column_GeoDiff_Label);
		colDef.setColumnHeaderText(Messages.GeoPart_View_Column_GeoDiff_Header);
		colDef.setColumnHeaderToolTipText(Messages.GeoPart_View_Column_GeoDiff_Label);

		colDef.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(30));

		colDef.setIsDefaultColumn();
		colDef.setCanModifyVisibility(false);
		colDef.setColumnSelectionListener(_columnSortListener);

		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final GeoPartComparerItem item = (GeoPartComparerItem) cell.getElement();
				final long minDiffValue = item.minDiffValue;

				final String minDiffText = minDiffValue == -2
						? "..."
						: minDiffValue == -1
								? "n/a"
								: Long.toString(minDiffValue);

				cell.setText(minDiffText);
			}
		});
	}

	/**
	 * column: Tour start date
	 */
	private void defineColumn_Time_TourStartDate() {

		final ColumnDefinition colDef = TableColumnFactory.TIME_TOUR_START_DATE.createColumn(_columnManager, _pc);

		// overwrite column id to identify the column when table is sorted
		colDef.setColumnId(COLUMN_TOUR_START_DATE);
		colDef.setColumnSelectionListener(_columnSortListener);

		colDef.setIsDefaultColumn();
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final GeoPartComparerItem item = (GeoPartComparerItem) cell.getElement();

				final ZonedDateTime tourStartTime = item.tourStartTime;
				cell.setText(
						tourStartTime == null
								? UI.EMPTY_STRING
								: tourStartTime.format(TimeTools.Formatter_Date_S));
			}
		});
	}

	@Override
	public void dispose() {

		getSite().getPage().removePostSelectionListener(_postSelectionListener);
		getSite().getPage().removePartListener(_partListener);

		TourManager.getInstance().removeTourEventListener(_tourEventListener);

		_prefStore.removePropertyChangeListener(_prefChangeListener);

		super.dispose();
	}

	private void enableControls() {

		_actionAppTourFilter.setEnabled(_isCompareEnabled);
	}

	private void fillToolbar() {

		final IToolBarManager tbm = getViewSite().getActionBars().getToolBarManager();

		tbm.add(_actionOnOff);
		tbm.add(_actionAppTourFilter);

		tbm.update(true);
	}

	@Override
	public ColumnManager getColumnManager() {
		return _columnManager;
	}

	/**
	 * @param sortColumnId
	 * @return Returns the column widget by it's column id, when column id is not found then the
	 *         first column is returned.
	 */
	private TableColumn getSortColumn(final String sortColumnId) {

		final TableColumn[] allColumns = _geoPartViewer.getTable().getColumns();

		for (final TableColumn column : allColumns) {

			final String columnId = ((ColumnDefinition) column.getData()).getColumnId();

			if (columnId.equals(sortColumnId)) {
				return column;
			}
		}

		return allColumns[0];
	}

	@Override
	public ColumnViewer getViewer() {
		return _geoPartViewer;
	}

	private StructuredSelection getViewerSelection() {

		return (StructuredSelection) _geoPartViewer.getSelection();
	}

	private void initUI(final Composite parent) {

		_parent = parent;

		_pc = new PixelConverter(parent);

		_defaultSelectionListener = new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				recompareTours();
			}
		};

		_columnSortListener = new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onSelect_SortColumn(e);
			}
		};
	}

	private void logCompareResult(final GeoPartItem geoPartItem) {

		final ArrayList<GeoPartComparerItem> comparedTours = geoPartItem.comparedTours;

		// sort by compare min diff value
		Collections.sort(comparedTours, new Comparator<GeoPartComparerItem>() {

			@Override
			public int compare(final GeoPartComparerItem compItem1, final GeoPartComparerItem compItem2) {

				if (compItem1 == null || compItem1.tourLatLonDiff == null) {
					return Integer.MAX_VALUE;
				}
				if (compItem2 == null || compItem2.tourLatLonDiff == null) {
					return Integer.MAX_VALUE;
				}

				final int minIndex1 = compItem1.tourMinDiffIndex;
				final int minIndex2 = compItem2.tourMinDiffIndex;

				final long diff1 = minIndex1 < 0 ? Integer.MAX_VALUE : compItem1.tourLatLonDiff[minIndex1];
				final long diff2 = minIndex2 < 0 ? Integer.MAX_VALUE : compItem2.tourLatLonDiff[minIndex2];

//				return (int) (diff1 - diff2); // smallest first
				return (int) (diff2 - diff1); // smallest last
			}
		});

//		System.out.println("updateUI_CompareResult()  execId " + geoPartItem.executorId);
// TODO remove SYSTEM.OUT.PRINTLN

		for (final GeoPartComparerItem comparerItem : comparedTours) {

			if (comparerItem == null) {

//				System.out.println("comparerItem == null"); //$NON-NLS-1$
// TODO remove SYSTEM.OUT.PRINTLN
				continue;
			}

			if (comparerItem.tourLatLonDiff == null) {

//				System.out.println("tourLatLonDiff == null"); //$NON-NLS-1$
// TODO remove SYSTEM.OUT.PRINTLN
				continue;
			}

//			final int tourMinDiffIndex = comparerItem.tourMinDiffIndex;
//			final long[] tourLatLonDiff = comparerItem.tourLatLonDiff;
//
//			System.out.println(
//					String.format(
//							""
//									+ "tourId %-20s" //$NON-NLS-1$
//									+ "   idx %5d" //$NON-NLS-1$
//									+ "   diff %12d" //$NON-NLS-1$
//
//									+ "   speed %5.1f" //$NON-NLS-1$
//									+ "   pulse %6.1f" //$NON-NLS-1$
//
//							,
//
//							comparerItem.tourId,
//
//							tourMinDiffIndex,
//							tourMinDiffIndex < 0 ? -1 : tourLatLonDiff[tourMinDiffIndex],
//
//							comparerItem.speed,
//							comparerItem.avgPulse
//
//					));
//// TODO remove SYSTEM.OUT.PRINTLN
		}
	}

	private void onAction_AppFilter(final boolean isSelected) {

		_isUseAppFilter = isSelected;

		recompareTours();
	}

	private void onAction_OnOff(final boolean isSelected) {

		_isCompareEnabled = isSelected;

		_actionOnOff.setIcon(_isCompareEnabled);

		if (isSelected) {

			// enable comparing

			recompareTours();

		} else {

			// cancel comparing

			setState_CancelComparing();
		}

		enableControls();
	}

	private void onGeoPart_Select() {
		// TODO Auto-generated method stub

	}

	private void onGeoPart_Select(final Event event) {
		// TODO Auto-generated method stub
		if (_isInUpdate) {
			return;
		}
	}

	private void onSelect_SortColumn(final SelectionEvent e) {

		_viewerContainer.setRedraw(false);
		{
			// keep selection
			final ISelection selectionBackup = getViewerSelection();
			{
				// update viewer with new sorting
				_geoPartComparator.setSortColumn(e.widget);
				_geoPartViewer.refresh();
			}
			updateUI_SelectTourMarker(selectionBackup, null);
		}
		_viewerContainer.setRedraw(true);
	}

	private void onSelectionChanged(final ISelection selection) {

		final int selectionHash = selection.hashCode();
		if (_lastSelectionHash == selectionHash) {

			/*
			 * Last selection has not changed, this can occure when the app lost the focus and got
			 * the focus again.
			 */
			return;
		}

		_lastSelectionHash = selectionHash;

		if (selection instanceof SelectionChartInfo) {

			TourData tourData = null;

			final SelectionChartInfo chartInfo = (SelectionChartInfo) selection;

			final Chart chart = chartInfo.getChart();
			if (chart instanceof TourChart) {

				final TourChart tourChart = (TourChart) chart;
				tourData = tourChart.getTourData();
			}

			if (tourData != null && tourData.isMultipleTours()) {

				// multiple tours are selected

			} else {

				// use old behaviour

				final ChartDataModel chartDataModel = chartInfo.chartDataModel;
				if (chartDataModel != null) {

					final Object tourId = chartDataModel.getCustomData(Chart.CUSTOM_DATA_TOUR_ID);
					if (tourId instanceof Long) {

						tourData = TourManager.getInstance().getTourData((Long) tourId);
						if (tourData == null) {

							// tour is not in the database, try to get it from the raw data manager

							final HashMap<Long, TourData> rawData = RawDataManager.getInstance().getImportedTours();
							tourData = rawData.get(tourId);
						}
					}
				}
			}

			if (tourData != null) {

				compare_10_Compare(
						tourData,
						chartInfo.leftSliderValuesIndex,
						chartInfo.rightSliderValuesIndex);
			}

		} else if (selection instanceof SelectionChartXSliderPosition) {

			final SelectionChartXSliderPosition xSliderPos = (SelectionChartXSliderPosition) selection;
			final Chart chart = xSliderPos.getChart();
			if (chart == null) {
				return;
			}

			final ChartDataModel chartDataModel = chart.getChartDataModel();

			final Object tourId = chartDataModel.getCustomData(Chart.CUSTOM_DATA_TOUR_ID);
			if (tourId instanceof Long) {

				final TourData tourData = TourManager.getInstance().getTourData((Long) tourId);
				if (tourData != null) {

					final int leftSliderValueIndex = xSliderPos.getLeftSliderValueIndex();
					int rightSliderValueIndex = xSliderPos.getRightSliderValueIndex();

					rightSliderValueIndex =
							rightSliderValueIndex == SelectionChartXSliderPosition.IGNORE_SLIDER_POSITION
									? leftSliderValueIndex
									: rightSliderValueIndex;

					compare_10_Compare(
							tourData,
							leftSliderValueIndex,
							rightSliderValueIndex);
				}
			}

		} else if (selection instanceof SelectionTourCatalogView) {

			showRefTour(((SelectionTourCatalogView) selection).getRefId());

		} else if (selection instanceof StructuredSelection) {

			final Object firstElement = ((StructuredSelection) selection).getFirstElement();

			if (firstElement instanceof TVICatalogComparedTour) {

				showRefTour(((TVICatalogComparedTour) firstElement).getRefId());

			} else if (firstElement instanceof TVICompareResultComparedTour) {

				showRefTour(((TVICompareResultComparedTour) firstElement).refTour.refId);
			}
		}
	}

	private void recompareTours() {

		if (_geoParts != null && _isCompareEnabled) {

			_previousGeoPartItem = GeoPartTourLoader.loadToursFromGeoParts(
					_geoParts,
					_normalizedTourPart,
					_isUseAppFilter,
					_previousGeoPartItem);
		}

	}

	@Override
	public ColumnViewer recreateViewer(final ColumnViewer columnViewer) {

		_viewerContainer.setRedraw(false);
		{
			_geoPartViewer.getTable().dispose();

			createUI_20_TableViewer(_viewerContainer);
			_viewerContainer.layout();

			// update the viewer
			reloadViewer();
		}
		_viewerContainer.setRedraw(true);

		return _geoPartViewer;
	}

	@Override
	public void reloadViewer() {

		updateUI_Viewer();
	}

	private void restoreState() {

		_isCompareEnabled = Util.getStateBoolean(_state, STATE_IS_LISTEN_TO_SLIDER_POSITION, true);
		_actionOnOff.setIcon(_isCompareEnabled);
		_actionOnOff.setChecked(_isCompareEnabled);

		_isUseAppFilter = Util.getStateBoolean(_state, STATE_IS_USE_APP_FILTER, true);
		_actionAppTourFilter.setChecked(_isUseAppFilter);

		enableControls();
	}

	private void saveState() {

		_state.put(STATE_IS_LISTEN_TO_SLIDER_POSITION, _isCompareEnabled);
		_state.put(STATE_IS_USE_APP_FILTER, _isUseAppFilter);

		_columnManager.saveState(_state);
	}

	@Override
	public void setFocus() {

		_geoPartViewer.getTable().setFocus();
	}

	private void setState_CancelComparing() {

		_lblProgressBar.setText("Comparing is canceled");

		setState_StopComparing();
	}

	private void setState_StopComparing() {

		GeoPartTourLoader.stopLoading(_previousGeoPartItem);

		// reset last id that the same compare can be restarted
		_prevTourId = Long.MIN_VALUE;
	}

	private void showRefTour(final long refId) {

		final TourCompareConfig tourCompareConfig = ReferenceTourManager.getInstance().getTourCompareConfig(refId);

		if (tourCompareConfig == null) {
			return;
		}

		final TourData tourData = tourCompareConfig.getRefTourData();
		if (tourData != null) {

			final TourReference refTour = tourCompareConfig.getRefTour();

			compare_10_Compare(
					tourData,
					refTour.getStartValueIndex(),
					refTour.getEndValueIndex());
		}
	}

	@Override
	public void updateColumnHeader(final ColumnDefinition colDef) {}

	private void updateUI_GeoItem(final GeoPartItem geoItem) {

		if (geoItem == null) {

			_lblNumTours.setText(UI.EMPTY_STRING);
			_lblSqlRuntime.setText(UI.EMPTY_STRING);

		} else {

			_lblNumTours.setText(Integer.toString(geoItem.tourIds.length));
			_lblSqlRuntime.setText(Long.toString(geoItem.sqlRunningTime) + " ms"); //$NON-NLS-1$
		}
	}

	private void updateUI_Progress(final int workedTours) {

		_progressBar.setSelection(workedTours);

		if (workedTours == _geoPartTours) {

			_lblProgressBar.setText("Comparing is done");

		} else {

			_lblProgressBar.setText(NLS.bind("Comparing tours: {0} / {1}", workedTours, _geoPartTours)); //$NON-NLS-1$
		}
	}

	/**
	 * Select and reveal tour marker item.
	 * 
	 * @param selection
	 * @param checkedElements
	 */
	private void updateUI_SelectTourMarker(final ISelection selection, final Object[] checkedElements) {

		_isInUpdate = true;
		{
			_geoPartViewer.setSelection(selection, true);

			final Table table = _geoPartViewer.getTable();
			table.showSelection();
		}
		_isInUpdate = false;
	}

	/**
	 * Set the sort column direction indicator for a column.
	 * 
	 * @param sortColumnId
	 * @param isAscendingSort
	 */
	private void updateUI_SetSortDirection(final String sortColumnId, final int sortDirection) {

		final Table table = _geoPartViewer.getTable();
		final TableColumn tc = getSortColumn(sortColumnId);

		table.setSortColumn(tc);
		table.setSortDirection(sortDirection == CompareResultComparator.ASCENDING ? SWT.UP : SWT.DOWN);
	}

	private void updateUI_Viewer() {

		_geoPartViewer.setInput(new Object[0]);

//		enableActions();
	}
}
