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
package net.tourbook.ui.views;

import java.text.NumberFormat;
import java.util.ArrayList;

import net.tourbook.Messages;
import net.tourbook.algorithm.DouglasPeuckerSimplifier;
import net.tourbook.algorithm.Point;
import net.tourbook.chart.SelectionChartXSliderPosition;
import net.tourbook.data.TourData;
import net.tourbook.data.TourSegment;
import net.tourbook.database.MyTourbookException;
import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tour.ITourEventListener;
import net.tourbook.tour.SelectionActiveEditor;
import net.tourbook.tour.SelectionTourData;
import net.tourbook.tour.SelectionTourId;
import net.tourbook.tour.TourEditor;
import net.tourbook.tour.TourEvent;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.ColumnDefinition;
import net.tourbook.ui.ColumnManager;
import net.tourbook.ui.ITourViewer;
import net.tourbook.ui.TableColumnFactory;
import net.tourbook.ui.UI;
import net.tourbook.ui.action.ActionModifyColumns;
import net.tourbook.ui.tourChart.TourChart;
import net.tourbook.ui.tourChart.TourChartView;
import net.tourbook.util.PixelConverter;
import net.tourbook.util.PostSelectionProvider;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.runtime.Preferences.IPropertyChangeListener;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Scale;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.PageBook;
import org.eclipse.ui.part.ViewPart;

/**
 *
 */
public class TourSegmenterView extends ViewPart implements ITourViewer {

	public static final String		ID					= "net.tourbook.views.TourSegmenter";						//$NON-NLS-1$

	final IDialogSettings			fViewState			= TourbookPlugin.getDefault().getDialogSettingsSection(ID);

	private static final int		COLUMN_DEFAULT		= 0;														// sort by time
	private static final int		COLUMN_SPEED		= 10;
	private static final int		COLUMN_PACE			= 20;
	private static final int		COLUMN_GRADIENT		= 30;

	private PageBook				fPageBook;
	private Composite				fPageSegmenter;
	private Label					fPageInvalidData;
	private Label					fPageNoData;

	private Composite				fViewerContainer;

	private Scale					fScaleTolerance;
	private Label					fLabelToleranceValue;
	private Label					fLblTitle;

	private TableViewer				fSegmentViewer;
	private ColumnManager			fColumnManager;

	/**
	 * {@link TourChart} contains the chart for the tour, this is necessary to move the slider in
	 * the chart to a selected segment
	 */
	private TourChart				fTourChart;
	private TourData				fTourData;

	private int						fDpTolerance;
	private int						fSavedDpTolerance	= -1;

	private ISelectionListener		fPostSelectionListener;
	private IPartListener2			fPartListener;
	private IPropertyChangeListener	fPrefChangeListener;
	private ITourEventListener		fTourEventListener;

	private PostSelectionProvider	fPostSelectionProvider;

	private final NumberFormat		fNf					= NumberFormat.getNumberInstance();

	private boolean					fShowSegmentsInChart;

	private ActionShowSegments		fActionShowSegments;

	private boolean					fIsTourDirty		= false;

	private boolean					fIsSaving;

	/**
	 * when <code>true</code>, the tour dirty flag is disabled to load data into the fields
	 */
	private boolean					fIsDirtyDisabled	= false;

	private class ActionShowSegments extends Action {

		public ActionShowSegments() {

			super(Messages.App_Action_open_tour_segmenter, SWT.TOGGLE);
			setToolTipText(Messages.App_Action_open_tour_segmenter_tooltip);

			setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__tour_segmenter));
		}

		@Override
		public void run() {
			fShowSegmentsInChart = !fShowSegmentsInChart;
			fireSegmentLayerChanged();
		}
	}

	/**
	 * The content provider class is responsible for providing objects to the view. It can wrap
	 * existing objects in adapters or simply return objects as-is. These objects may be sensitive
	 * to the current input of the view, or ignore it and always show the same content (like Task
	 * List, for example).
	 */
	class ViewContentProvider implements IStructuredContentProvider {

		public ViewContentProvider() {}

		public void dispose() {}

		public Object[] getElements(final Object parent) {
			if (fTourData == null) {
				return new Object[0];
			} else {
				return fTourData.createTourSegments();
			}
		}

		public void inputChanged(final Viewer v, final Object oldInput, final Object newInput) {}
	}

	private class ViewSorter extends ViewerSorter {

		// private static final int ASCENDING = 0;

		private static final int	DESCENDING	= 1;

		private int					column;

		private int					direction;

		/**
		 * Compares the object for sorting
		 */
		@Override
		public int compare(final Viewer viewer, final Object obj1, final Object obj2) {

			final TourSegment segment1 = ((TourSegment) obj1);
			final TourSegment segment2 = ((TourSegment) obj2);

			int rc = 0;

			// Determine which column and do the appropriate sort
			switch (column) {
			case COLUMN_DEFAULT:
				rc = segment1.serieIndexStart - segment2.serieIndexStart;
				if (direction == DESCENDING) {
					rc = -rc;
				}
				break;

			case COLUMN_SPEED:
				rc = (int) ((segment1.speed - segment2.speed) * 100);
				break;

			case COLUMN_PACE:
				rc = (int) ((segment1.pace - segment2.pace) * 100);
				break;

			case COLUMN_GRADIENT:
				rc = (int) ((segment1.gradient - segment2.gradient) * 100);
				break;
			}

			// If descending order, flip the direction
			if (direction == DESCENDING) {
				rc = -rc;
			}

			return rc;
		}

		/**
		 * Does the sort. If it's a different column from the previous sort, do an ascending sort.
		 * If it's the same column as the last sort, toggle the sort direction.
		 * 
		 * @param column
		 */
		public void setSortColumn(final int column) {

			if (column == this.column) {
				// Same column as last sort; toggle the direction
				direction = 1 - direction;
			} else {
				// New column; do an descending sort
				this.column = column;
				direction = DESCENDING;
			}
		}
	}

	/**
	 * Constructor
	 */
	public TourSegmenterView() {
		super();
	}

	private void addPartListener() {

		// set the part listener
		fPartListener = new IPartListener2() {
			public void partActivated(final IWorkbenchPartReference partRef) {}

			public void partBroughtToTop(final IWorkbenchPartReference partRef) {}

			public void partClosed(final IWorkbenchPartReference partRef) {
				if (partRef.getPart(false) == TourSegmenterView.this) {
					saveTour();
					saveState();
					hideTourSegmentsInChart();
				}
			}

			public void partDeactivated(final IWorkbenchPartReference partRef) {}

			public void partHidden(final IWorkbenchPartReference partRef) {}

			public void partInputChanged(final IWorkbenchPartReference partRef) {}

			public void partOpened(final IWorkbenchPartReference partRef) {}

			public void partVisible(final IWorkbenchPartReference partRef) {}
		};

		getSite().getPage().addPartListener(fPartListener);
	}

	private void addPrefListener() {

		fPrefChangeListener = new Preferences.IPropertyChangeListener() {
			public void propertyChange(final Preferences.PropertyChangeEvent event) {

				final String property = event.getProperty();

				if (property.equals(ITourbookPreferences.MEASUREMENT_SYSTEM)) {

					// measurement system has changed

					UI.updateUnits();

					fColumnManager.saveState(fViewState);
					fColumnManager.clearColumns();
					defineViewerColumns(fViewerContainer);

					recreateViewer(null);

					// refresh tour with the new measurement system
					fireSegmentLayerChanged();
				}
			}
		};

		TourbookPlugin.getDefault().getPluginPreferences().addPropertyChangeListener(fPrefChangeListener);
	}

	private void addSelectionListener() {

		fPostSelectionListener = new ISelectionListener() {
			public void selectionChanged(final IWorkbenchPart part, final ISelection selection) {

				if (part == TourSegmenterView.this) {
					return;
				}

				onSelectionChanged(selection);
			}
		};

		getSite().getPage().addPostSelectionListener(fPostSelectionListener);
	}

	private void addTourEventListener() {

		fTourEventListener = new ITourEventListener() {
			public void tourChanged(final IWorkbenchPart part, final TourEventId eventId, final Object eventData) {

				if (fTourData == null || part == TourSegmenterView.this) {
					return;
				}

				if (eventId == TourEventId.TOUR_CHANGED && eventData instanceof TourEvent) {

					final TourEvent tourEvent = (TourEvent) eventData;
					final ArrayList<TourData> modifiedTours = tourEvent.getModifiedTours();

					if (modifiedTours == null || modifiedTours.size() == 0) {
						return;
					}

					final TourData modifiedTourData = modifiedTours.get(0);
					final long viewTourId = fTourData.getTourId();

					if (modifiedTourData.getTourId() == viewTourId) {

						// update existing tour

						if (checkDataValidation(modifiedTourData)) {

							if (tourEvent.isReverted) {

								/*
								 * tour is reverted, saving existing tour is not necessary, just
								 * update the tour
								 */
								setTour(modifiedTourData);

							} else {

								createSegments();
								reloadViewer();
							}
						}

					} else {

						// display new tour

						onSelectionChanged(new SelectionTourData(null, modifiedTourData));
					}

					// removed old tour data from the selection provider
					fPostSelectionProvider.clearSelection();
				}
			}
		};

		TourManager.getInstance().addTourEventListener(fTourEventListener);
	}

	/**
	 * check if data for the segmenter is valid
	 */
	private boolean checkDataValidation(final TourData tourData) {

		if (tourData == null) {

			fPageBook.showPage(fPageNoData);

			fTourData = null;
			fTourChart = null;

			return false;
		}

		if (tourData.altitudeSerie == null
				|| tourData.altitudeSerie.length == 0
				|| tourData.getMetricDistanceSerie() == null
				|| tourData.getMetricDistanceSerie().length == 0) {

			fPageBook.showPage(fPageInvalidData);

			return false;
		}

		fPageBook.showPage(fPageSegmenter);

		return true;
	}

	private void createActions() {

		fActionShowSegments = new ActionShowSegments();
		final ActionModifyColumns actionModifyColumns = new ActionModifyColumns(this);

		/*
		 * fill view menu
		 */
		final IMenuManager menuMgr = getViewSite().getActionBars().getMenuManager();
		menuMgr.add(actionModifyColumns);

		/*
		 * fill view toolbar
		 */
		final IToolBarManager tbm = getViewSite().getActionBars().getToolBarManager();
		tbm.add(fActionShowSegments);
	}

	@Override
	public void createPartControl(final Composite parent) {

		// define all columns
		fColumnManager = new ColumnManager(this, fViewState);
		defineViewerColumns(parent);

		fPageBook = new PageBook(parent, SWT.NONE);

		fPageNoData = new Label(fPageBook, SWT.WRAP);
		fPageNoData.setText(Messages.Tour_Segmenter_Label_no_chart);

		fPageInvalidData = new Label(fPageBook, SWT.WRAP);
		fPageInvalidData.setText(Messages.Tour_Segmenter_label_invalid_data);

		fPageSegmenter = new Composite(fPageBook, SWT.NONE);
		GridLayoutFactory.fillDefaults().spacing(0, 0).applyTo(fPageSegmenter);

		fPageBook.showPage(fPageNoData);

		createUIHeader(fPageSegmenter);
		createUIViewer(fPageSegmenter);

		createActions();

		addSelectionListener();
		addPartListener();
		addPrefListener();
		addTourEventListener();

		// tell the site that this view is a selection provider
		getSite().setSelectionProvider(fPostSelectionProvider = new PostSelectionProvider());

		// set default value, show segments in opened charts
		fShowSegmentsInChart = true;
		fActionShowSegments.setChecked(fShowSegmentsInChart);

		// update viewer with current selection
		onSelectionChanged(getSite().getWorkbenchWindow().getSelectionService().getSelection());

		if (fTourData == null) {
			Display.getCurrent().asyncExec(new Runnable() {
				public void run() {

					final ArrayList<TourData> selectedTours = TourManager.getSelectedTours();

					if (selectedTours != null && selectedTours.size() > 0) {
						onSelectionChanged(new SelectionTourData(null, selectedTours.get(0)));
					}
				}
			});
		}

	}

	/**
	 * create points for the simplifier from distance and altitude
	 */
	private void createSegments() {

		final int[] distanceSerie = fTourData.getMetricDistanceSerie();
		final int[] altitudeSerie = fTourData.altitudeSerie;

		final Point graphPoints[] = new Point[distanceSerie.length];
		for (int iPoint = 0; iPoint < graphPoints.length; iPoint++) {
			graphPoints[iPoint] = new Point(distanceSerie[iPoint], altitudeSerie[iPoint], iPoint);
		}

		final DouglasPeuckerSimplifier dpSimplifier = new DouglasPeuckerSimplifier(fDpTolerance, graphPoints);
		final Object[] simplePoints = dpSimplifier.simplify();

		/*
		 * copie the data index for the simplified points into the tour data
		 */
		fTourData.segmentSerieIndex = new int[simplePoints.length];

		final int[] segmentSerieIndex = fTourData.segmentSerieIndex;

		for (int iPoint = 0; iPoint < simplePoints.length; iPoint++) {
			final Point point = (Point) simplePoints[iPoint];
			segmentSerieIndex[iPoint] = point.serieIndex;
		}
	}

	private void createSegmentViewer(final Composite parent) {

		final Table table = new Table(parent, //
				SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.MULTI /* | SWT.BORDER */);

		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(table);

		fSegmentViewer = new TableViewer(table);
		fColumnManager.createColumns(fSegmentViewer);

		fSegmentViewer.setContentProvider(new ViewContentProvider());
		fSegmentViewer.setSorter(new ViewSorter());

		fSegmentViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(final SelectionChangedEvent event) {

				final StructuredSelection selection = (StructuredSelection) event.getSelection();
				if (selection != null) {

					/*
					 * select the chart sliders according to the selected segment(s)
					 */

					final Object[] segments = selection.toArray();

					if (segments.length > 0) {

						if (fTourChart == null) {
							fTourChart = getActiveTourChart(fTourData);
						}

						final SelectionChartXSliderPosition selectionSliderPosition = //
						new SelectionChartXSliderPosition(fTourChart,
								((TourSegment) (segments[0])).serieIndexStart,
								((TourSegment) (segments[segments.length - 1])).serieIndexEnd);

						fPostSelectionProvider.setSelection(selectionSliderPosition);
					}
				}
			}
		});
	}

	private void createUIHeader(final Composite parent) {

		final PixelConverter pc = new PixelConverter(parent);
		GridData gd;
		Label label;

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(3).extendedMargins(3, 3, 3, 2).applyTo(container);

		/*
		 * tour title
		 */
		fLblTitle = new Label(container, SWT.NONE);
		gd = new GridData(SWT.FILL, SWT.NONE, true, false);
		gd.horizontalSpan = 3;
		fLblTitle.setLayoutData(gd);

		/*
		 * scale: tolerance
		 */
		label = new Label(container, SWT.NONE);
		label.setText(Messages.Tour_Segmenter_Label_tolerance);

		fScaleTolerance = new Scale(container, SWT.HORIZONTAL);
		gd = new GridData(SWT.FILL, SWT.NONE, true, false);
		fScaleTolerance.setMaximum(100);
		fScaleTolerance.setLayoutData(gd);
		fScaleTolerance.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onToleranceChanged(getTolerance(), false);
				setTourDirty();
			}
		});

		fScaleTolerance.addListener(SWT.MouseWheel, new Listener() {
			public void handleEvent(final Event event) {
//				onToleranceChanged(getTolerance(), false);
//				setTourDirty();
			}
		});

		fLabelToleranceValue = new Label(container, SWT.NONE);
		fLabelToleranceValue.setText(Messages.Tour_Segmenter_Label_default_tolerance);
		GridDataFactory.fillDefaults()
				.align(SWT.FILL, SWT.CENTER)
				.hint(pc.convertWidthInCharsToPixels(4), SWT.DEFAULT)
				.applyTo(fLabelToleranceValue);
	}

	private void createUIViewer(final Composite parent) {

		fViewerContainer = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(fViewerContainer);
		GridLayoutFactory.fillDefaults().applyTo(fViewerContainer);

		createSegmentViewer(fViewerContainer);
	}

	private void defineViewerColumns(final Composite parent) {

		final PixelConverter pixelConverter = new PixelConverter(parent);
		ColumnDefinition colDef;

		final SelectionAdapter defaultColumnSelectionListener = new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent event) {
				((ViewSorter) fSegmentViewer.getSorter()).setSortColumn(COLUMN_DEFAULT);
				fSegmentViewer.refresh();
			}
		};

		/*
		 * column: driving time
		 */
		colDef = TableColumnFactory.DRIVING_TIME.createColumn(fColumnManager, pixelConverter);
		colDef.setIsDefaultColumn();
		colDef.addSelectionListener(defaultColumnSelectionListener);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
				final TourSegment segment = (TourSegment) cell.getElement();
				cell.setText(UI.formatSeconds(segment.drivingTime));
			}
		});

		/*
		 * column: distance (km/mile)
		 */
		colDef = TableColumnFactory.DISTANCE.createColumn(fColumnManager, pixelConverter);
		colDef.setIsDefaultColumn();
		colDef.addSelectionListener(defaultColumnSelectionListener);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final TourSegment segment = (TourSegment) cell.getElement();

				fNf.setMinimumFractionDigits(2);
				fNf.setMaximumFractionDigits(2);

				cell.setText(fNf.format((segment.distance) / (1000 * UI.UNIT_VALUE_DISTANCE)));
			}
		});

		/*
		 * column: altitude (m/ft)
		 */
		colDef = TableColumnFactory.ALTITUDE.createColumn(fColumnManager, pixelConverter);
		colDef.setIsDefaultColumn();
		colDef.addSelectionListener(defaultColumnSelectionListener);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final TourSegment segment = (TourSegment) cell.getElement();

				fNf.setMinimumFractionDigits(0);
				fNf.setMaximumFractionDigits(0);

				cell.setText(fNf.format(segment.altitude / UI.UNIT_VALUE_ALTITUDE));
			}
		});

		/*
		 * column: speed
		 */
		colDef = TableColumnFactory.AVG_SPEED.createColumn(fColumnManager, pixelConverter);
		colDef.setIsDefaultColumn();
		colDef.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent event) {
				((ViewSorter) fSegmentViewer.getSorter()).setSortColumn(COLUMN_SPEED);
				fSegmentViewer.refresh();
			}
		});
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final TourSegment segment = (TourSegment) cell.getElement();
				if (segment.drivingTime == 0) {
					cell.setText(UI.EMPTY_STRING);
				} else {
					fNf.setMinimumFractionDigits(1);
					fNf.setMaximumFractionDigits(1);
					cell.setText(fNf.format(segment.speed));
				}
			}
		});

		/*
		 * column: pace
		 */
		colDef = TableColumnFactory.PACE.createColumn(fColumnManager, pixelConverter);
		colDef.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent event) {
				((ViewSorter) fSegmentViewer.getSorter()).setSortColumn(COLUMN_PACE);
				fSegmentViewer.refresh();
			}
		});
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final TourSegment segment = (TourSegment) cell.getElement();
				if (segment.drivingTime == 0) {
					cell.setText(UI.EMPTY_STRING);
				} else {
					fNf.setMinimumFractionDigits(1);
					fNf.setMaximumFractionDigits(1);
					cell.setText(fNf.format(segment.pace));
				}
			}
		});

		/*
		 * column: gradient
		 */
		colDef = TableColumnFactory.GRADIENT.createColumn(fColumnManager, pixelConverter);
		colDef.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent event) {
				((ViewSorter) fSegmentViewer.getSorter()).setSortColumn(COLUMN_GRADIENT);
				fSegmentViewer.refresh();
			}
		});
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final TourSegment segment = (TourSegment) cell.getElement();
				fNf.setMinimumFractionDigits(1);
				fNf.setMaximumFractionDigits(1);

				cell.setText(fNf.format(segment.gradient));
			}
		});

		/*
		 * column: altitude up m/h
		 */
		colDef = TableColumnFactory.ALTITUDE_UP_H.createColumn(fColumnManager, pixelConverter);
		colDef.addSelectionListener(defaultColumnSelectionListener);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final TourSegment segment = (TourSegment) cell.getElement();
				if (segment.drivingTime == 0) {
					cell.setText(UI.EMPTY_STRING);
				} else {
					final float result = (segment.altitudeUp / UI.UNIT_VALUE_ALTITUDE) / segment.drivingTime * 3600;
					if (result == 0) {
						cell.setText(UI.EMPTY_STRING);
					} else {
						fNf.setMinimumFractionDigits(1);
						fNf.setMaximumFractionDigits(0);
						cell.setText(fNf.format(result));
					}
				}
			}
		});

		/*
		 * column: altitude down m/h
		 */
		colDef = TableColumnFactory.ALTITUDE_DOWN_H.createColumn(fColumnManager, pixelConverter);
		colDef.addSelectionListener(defaultColumnSelectionListener);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final TourSegment segment = (TourSegment) cell.getElement();
				if (segment.drivingTime == 0) {
					cell.setText(UI.EMPTY_STRING);
				} else {
					final float result = (segment.altitudeDown / UI.UNIT_VALUE_ALTITUDE) / segment.drivingTime * 3600;
					if (result == 0) {
						cell.setText(UI.EMPTY_STRING);
					} else {
						fNf.setMinimumFractionDigits(1);
						fNf.setMaximumFractionDigits(0);
						cell.setText(fNf.format(result));
					}
				}
			}
		});

	}

	@Override
	public void dispose() {

		final IWorkbenchPage wbPage = getSite().getPage();
		wbPage.removePostSelectionListener(fPostSelectionListener);
		wbPage.removePartListener(fPartListener);

		TourbookPlugin.getDefault().getPluginPreferences().removePropertyChangeListener(fPrefChangeListener);
		TourManager.getInstance().removeTourEventListener(fTourEventListener);

		super.dispose();
	}

	/**
	 * notify listeners to show/hide the segments
	 */
	private void fireSegmentLayerChanged() {

		// show/hide the segments in the chart
		TourManager.fireEvent(TourEventId.SEGMENT_LAYER_CHANGED, fShowSegmentsInChart, TourSegmenterView.this);
	}

	/**
	 * try to get the tour chart and/or editor from the active part
	 * 
	 * @param tourData
	 * @return Returns the {@link TourChart} for the requested {@link TourData}
	 */
	private TourChart getActiveTourChart(final TourData tourData) {

		// get tour chart from the active editor part
		for (final IWorkbenchWindow wbWindow : PlatformUI.getWorkbench().getWorkbenchWindows()) {
			for (final IWorkbenchPage wbPage : wbWindow.getPages()) {

				final IEditorPart activeEditor = wbPage.getActiveEditor();
				if (activeEditor instanceof TourEditor) {

					/*
					 * check if the tour data in the editor is the same
					 */
					final TourChart tourChart = ((TourEditor) activeEditor).getTourChart();
					final TourData tourChartTourData = tourChart.getTourData();
					if (tourChartTourData == tourData) {

						try {
							UI.checkTourData(tourData, tourChartTourData);
						} catch (final MyTourbookException e) {
							e.printStackTrace();
						}
					}
				}
			}
		}

		// get tour chart from the tour chart view
		for (final IWorkbenchWindow wbWindow : PlatformUI.getWorkbench().getWorkbenchWindows()) {
			for (final IWorkbenchPage wbPage : wbWindow.getPages()) {

				final IViewReference viewRef = wbPage.findViewReference(TourChartView.ID);
				if (viewRef != null) {

					final IViewPart view = viewRef.getView(false);
					if (view instanceof TourChartView) {

						final TourChartView tourChartView = ((TourChartView) view);

						/*
						 * check if the tour data in the tour chart is the same
						 */
						final TourChart tourChart = tourChartView.getTourChart();
						final TourData tourChartTourData = tourChart.getTourData();
						if (tourChartTourData == tourData) {
							try {
								UI.checkTourData(tourData, tourChartTourData);
							} catch (final MyTourbookException e) {
								e.printStackTrace();
							}

							return tourChart;
						}
					}
				}
			}
		}

		return null;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Object getAdapter(final Class adapter) {

		if (adapter == ColumnViewer.class) {
			return fSegmentViewer;
		}

		return Platform.getAdapterManager().getAdapter(this, adapter);
	}

	public ColumnManager getColumnManager() {
		return fColumnManager;
	}

	private int getTolerance() {
		return (int) ((Math.pow(fScaleTolerance.getSelection(), 2.05)) / 50.0);
	}

	public ColumnViewer getViewer() {
		return fSegmentViewer;
	}

	/**
	 * hides the tour segments
	 */
	private void hideTourSegmentsInChart() {

		fShowSegmentsInChart = false;
		fActionShowSegments.setChecked(fShowSegmentsInChart);

		fireSegmentLayerChanged();
	}

	/**
	 * handle a tour selection event
	 * 
	 * @param selection
	 */
	private void onSelectionChanged(final ISelection selection) {

		if (fIsSaving) {
			return;
		}

		/*
		 * run selection async because a tour could be modified and needs to be saved, modifications
		 * are not reported to the tour data editor, saving needs also to be asynch with the tour
		 * data editor
		 */
		Display.getCurrent().asyncExec(new Runnable() {
			public void run() {

				if (fPageBook.isDisposed()) {
					return;
				}

				TourData nextTourData = null;
				TourChart nextTourChart = null;

				if (selection instanceof SelectionActiveEditor) {

					final IEditorPart editorPart = ((SelectionActiveEditor) selection).getEditor();

					if (editorPart instanceof TourEditor) {

						final TourEditor tourEditor = (TourEditor) editorPart;

						// check if editor changed
						if (fTourChart != null & fTourChart == tourEditor.getTourChart()) {
							return;
						}

						nextTourChart = tourEditor.getTourChart();
						nextTourData = nextTourChart.getTourData();

					} else {
						return;
					}

				} else if (selection instanceof SelectionTourData) {

					final SelectionTourData selectionTourData = (SelectionTourData) selection;

					nextTourData = selectionTourData.getTourData();
					nextTourChart = selectionTourData.getTourChart();

				} else if (selection instanceof SelectionTourId) {

					final SelectionTourId tourIdSelection = (SelectionTourId) selection;

					if (fTourData != null) {
						if (fTourData.getTourId().equals(tourIdSelection.getTourId())) {
							// don't reload the same tour
							return;
						}
					}

					nextTourData = TourManager.getInstance().getTourData(tourIdSelection.getTourId());

				} else {
					return;
				}

				if (checkDataValidation(nextTourData) == false) {
					return;
				}

				/*
				 * save previous tour when a new tour is selected
				 */
				if (fTourData != null && fTourData.getTourId() == nextTourData.getTourId()) {

					// nothing to do, it's the same tour

				} else {

					final TourData savedTour = saveTour();
					if (savedTour != null) {

						/*
						 * when a tour is saved, the change notification is not fired because
						 * another tour is already selected, but to update the tour in a TourViewer,
						 * a change nofification must be fired afterwords
						 */
//				Display.getCurrent().asyncExec(new Runnable() {
//					public void run() {
//						TourManager.fireEvent(TourEventId.TOUR_CHANGED,
//								new TourEvent(savedTour),
//								TourSegmenterView.this);
//					}
//				});
					}

					if (nextTourChart == null) {
						nextTourChart = getActiveTourChart(nextTourData);
					}

					fTourChart = nextTourChart;

					setTour(nextTourData);
				}
			}
		});
	}

	private void onToleranceChanged(final int dpTolerance, final boolean forceRecalc) {

		// update label in the ui
		fLabelToleranceValue.setText(Integer.toString(dpTolerance));

		if (fTourData == null || (fDpTolerance == dpTolerance && forceRecalc == false)) {
			return;
		}

		fDpTolerance = dpTolerance;

		// update tolerance into the tour data
		fTourData.setDpTolerance((short) dpTolerance);

		createSegments();

		// update table and create the tour segments in tour data
		reloadViewer();

		fireSegmentLayerChanged();
	}

	public ColumnViewer recreateViewer(final ColumnViewer columnViewer) {

		fViewerContainer.setRedraw(false);
		{
			fSegmentViewer.getTable().dispose();

			createSegmentViewer(fViewerContainer);
			fViewerContainer.layout();

			// update the viewer
			reloadViewer();
		}
		fViewerContainer.setRedraw(true);

		return fSegmentViewer;
	}

	public void reloadViewer() {
		fSegmentViewer.setInput(new Object[0]);
	}

	private void saveState() {
		fColumnManager.saveState(fViewState);
	}

	private TourData saveTour() {

		if (fIsTourDirty == false || fTourData == null || fSavedDpTolerance == -1) {
			// nothing to do
			return null;
		}

		TourData savedTour;
		fIsSaving = true;
		{
			savedTour = TourManager.saveModifiedTour(fTourData);
		}
		fIsSaving = false;

		fIsTourDirty = false;

		return savedTour;
	}

	@Override
	public void setFocus() {
		fScaleTolerance.setFocus();
	}

	/**
	 * Sets the tour for the segmenter
	 * 
	 * @param tourData
	 */
	private void setTour(final TourData tourData) {

		fIsDirtyDisabled = true;
		fTourData = tourData;

		fPageBook.showPage(fPageSegmenter);

		// update tour title
		fLblTitle.setText(TourManager.getTourTitleDetailed(fTourData));
		fLblTitle.pack(true);

		// keep original dp tolerance
		fSavedDpTolerance = fDpTolerance = fTourData.getDpTolerance();

		// update segmenter values, the factor is defined by experimentals
		final float factor = 1 / 2.05f;
		final double tolerance = Math.pow(fDpTolerance * 50, factor);

		fScaleTolerance.setSelection((int) tolerance);
		fLabelToleranceValue.setText(Integer.toString(fTourData.getDpTolerance()));

		fIsDirtyDisabled = false;

		// force the segements to be rebuild for the new tour
		onToleranceChanged(fDpTolerance, true);
	}

	/**
	 * when dp tolerance was changed set the tour dirty
	 */
	private void setTourDirty() {

		if (fIsDirtyDisabled) {
			return;
		}

		if (fTourData != null && fSavedDpTolerance != fTourData.getDpTolerance()) {
			fIsTourDirty = true;
		}
	}

}
