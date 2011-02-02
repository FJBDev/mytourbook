/*******************************************************************************
 * Copyright (C) 2005, 2011  Wolfgang Schramm and Contributors
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
package net.tourbook.ui.tourChart;

import gnu.trove.list.array.TDoubleArrayList;

import java.util.ArrayList;
import java.util.Collections;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.chart.Chart;
import net.tourbook.chart.ChartDataModel;
import net.tourbook.chart.ChartDataXSerie;
import net.tourbook.chart.ChartDataYSerie;
import net.tourbook.chart.IChartLayer;
import net.tourbook.colors.GraphColorProvider;
import net.tourbook.data.TourData;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tour.ITourEventListener;
import net.tourbook.tour.SelectionDeletedTours;
import net.tourbook.tour.SelectionTourData;
import net.tourbook.tour.SelectionTourId;
import net.tourbook.tour.SelectionTourIds;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.UI;
import net.tourbook.util.PixelConverter;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Scale;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.part.PageBook;
import org.eclipse.ui.part.ViewPart;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import de.byteholder.geoclipse.ui.ViewerDetailForm;

/**
 * Show selected tours in a conconi test chart
 */
public class ConconiChartView extends ViewPart {

	public static final String		ID									= "net.tourbook.views.ConconiChartView";	//$NON-NLS-1$

	private static final String		STATE_CONCONIT_TOURS_VIEWER_WIDTH	= "STATE_CONCONIT_TOURS_VIEWER_WIDTH";		//$NON-NLS-1$

	private static final RGB		DEFAULT_RGB							= new RGB(0xd0, 0xd0, 0xd0);

	private final IPreferenceStore	_prefStore							= TourbookPlugin.getDefault() //
																				.getPreferenceStore();

	private final IDialogSettings	_state								= TourbookPlugin.getDefault().//
																				getDialogSettingsSection(ID);

	private final DateTimeFormatter	_tourDTFormatter					= DateTimeFormat.shortDateTime();

	private ISelectionListener		_postSelectionListener;
	private IPartListener2			_partListener;
	private ITourEventListener		_tourEventListener;

	private ChartDataYSerie			_yDataPulse;
	private ConconiData				_conconiData;

	private PixelConverter			_pc;

	/*
	 * UI controls
	 */
	private PageBook				_pageBook;
	private Label					_pageNoChart;

	private Composite				_pageConconiTest;

	private Composite				_containerConconiTours;
	private ViewerDetailForm		_detailFormConconi;

	private ArrayList<TourData>		_conconiTours;

	private Chart					_chartConconiTest;
	private ChartLayerConconiTest	_conconiLayer;

	private Combo					_comboTours;
	private Scale					_scaleDeflection;
	private Label					_lblDeflactionPulse;
	private Label					_lblDeflactionPower;

	private boolean					_isSelectionDisabled				= true;

	private void addPartListener() {
		_partListener = new IPartListener2() {

			public void partActivated(final IWorkbenchPartReference partRef) {}

			public void partBroughtToTop(final IWorkbenchPartReference partRef) {}

			public void partClosed(final IWorkbenchPartReference partRef) {
				if (partRef.getPart(false) == ConconiChartView.this) {
//					saveTour();
					saveState();
				}
			}

			public void partDeactivated(final IWorkbenchPartReference partRef) {}

			public void partHidden(final IWorkbenchPartReference partRef) {}

			public void partInputChanged(final IWorkbenchPartReference partRef) {}

			public void partOpened(final IWorkbenchPartReference partRef) {}

			public void partVisible(final IWorkbenchPartReference partRef) {}
		};
		getViewSite().getPage().addPartListener(_partListener);
	}

	/**
	 * listen for events when a tour is selected
	 */
	private void addSelectionListener() {

		_postSelectionListener = new ISelectionListener() {
			public void selectionChanged(final IWorkbenchPart part, final ISelection selection) {

				if (part == ConconiChartView.this) {
					return;
				}

				onSelectionChanged(selection);
			}
		};
		getSite().getPage().addPostSelectionListener(_postSelectionListener);
	}

	private void clearView() {

		_conconiTours = null;

		if (_chartConconiTest != null) {
			_chartConconiTest.updateChart(null, false);
		}

		_pageBook.showPage(_pageNoChart);
	}

	/**
	 * @param conconiTours
	 *            contains all tours which are displayed in the conconi chart, they can be valid or
	 *            invalid
	 * @param markedTour
	 *            contains tour which should be marked in the chart, when <code>null</code> the
	 *            first tour will be marked
	 * @return
	 */
	private ChartDataModel createChartDataModelConconiTest(final ArrayList<TourData> conconiTours, TourData markedTour) {

		final ChartDataModel chartDataModel = new ChartDataModel(ChartDataModel.CHART_TYPE_XY_SCATTER);

		final int serieLengthRaw = conconiTours.size();

		final TourData[] toursArray = conconiTours.toArray(new TourData[serieLengthRaw]);
		final ArrayList<TourData> validTourList = new ArrayList<TourData>();

		/*
		 * get all tours which has valid data
		 */
		for (int serieIndex = 0; serieIndex < serieLengthRaw; serieIndex++) {

			final TourData tourData = toursArray[serieIndex];

			final int[] tdPowerSerie = tourData.getPowerSerie();
			final int[] tdPulseSerie = tourData.pulseSerie;

			// check if required data series are available
			if (tdPowerSerie != null && tdPowerSerie.length != 0 && tdPulseSerie != null && tdPulseSerie.length != 0) {
				validTourList.add(tourData);
			}
		}

		final int validDataLength = validTourList.size();
		final int lastTourIndex = validDataLength - 1;

		// display error when required data are not available
		if (validDataLength == 0) {

			chartDataModel.setErrorMessage(Messages.Conconi_Chart_InvalidData);

			return chartDataModel;
		}

		// ensure a tour is marked
		if (markedTour == null) {
			markedTour = validTourList.get(0);
		}

		final String prefGraphName = ITourbookPreferences.GRAPH_COLORS + GraphColorProvider.PREF_GRAPH_HEARTBEAT + "."; //$NON-NLS-1$

		final int[][] powerSerie = new int[validDataLength][];
		final int[][] pulseSerie = new int[validDataLength][];
		final RGB[] rgbLine = new RGB[validDataLength];
		final RGB[] rgbDark = new RGB[validDataLength];
		final RGB[] rgbBright = new RGB[validDataLength];

		final TourData[] validTours = validTourList.toArray(new TourData[validTourList.size()]);
		int markedIndex = 0;

		/*
		 * create data series which contain valid data
		 */
		for (int tourIndex = 0; tourIndex < validDataLength; tourIndex++) {

			final TourData tourData = validTours[tourIndex];

			final int[] tourPowerSerie = tourData.getPowerSerie();
			final int[] tourPulseSerie = tourData.pulseSerie;

			// check if required data series are available
			if (tourPowerSerie != null
					&& tourPowerSerie.length != 0
					&& tourPulseSerie != null
					&& tourPulseSerie.length != 0) {

				powerSerie[tourIndex] = tourPowerSerie;
				pulseSerie[tourIndex] = tourPulseSerie;

				// set color, marked tour is displayed with pulse color
				if (tourData.equals(markedTour)) {

					// get index of marked tour
					markedIndex = tourIndex;

					final RGB prefLineColor = PreferenceConverter.getColor(//
							_prefStore,
							prefGraphName + GraphColorProvider.PREF_COLOR_LINE);

					final RGB prefDarkColor = PreferenceConverter.getColor(//
							_prefStore,
							prefGraphName + GraphColorProvider.PREF_COLOR_DARK);

					final RGB prefBrightColor = PreferenceConverter.getColor(//
							_prefStore,
							prefGraphName + GraphColorProvider.PREF_COLOR_BRIGHT);

					rgbLine[tourIndex] = prefLineColor;
					rgbDark[tourIndex] = prefDarkColor;
					rgbBright[tourIndex] = prefBrightColor;

				} else {

					rgbLine[tourIndex] = DEFAULT_RGB;
					rgbDark[tourIndex] = DEFAULT_RGB;
					rgbBright[tourIndex] = DEFAULT_RGB;
				}
			}
		}

		/*
		 * swap last tour with marked tour that the marked tour is painted at last to be not covered
		 * by other tours
		 */
		final int[] markedPowerSerie = powerSerie[markedIndex];
		final int[] markedPulseSerie = pulseSerie[markedIndex];
		final RGB markedRgbLine = rgbLine[markedIndex];
		final RGB markedRgbDark = rgbDark[markedIndex];
		final RGB markedRgbBright = rgbBright[markedIndex];

		powerSerie[markedIndex] = powerSerie[lastTourIndex];
		pulseSerie[markedIndex] = pulseSerie[lastTourIndex];
		rgbLine[markedIndex] = rgbLine[lastTourIndex];
		rgbDark[markedIndex] = rgbDark[lastTourIndex];
		rgbBright[markedIndex] = rgbBright[lastTourIndex];

		powerSerie[lastTourIndex] = markedPowerSerie;
		pulseSerie[lastTourIndex] = markedPulseSerie;
		rgbLine[lastTourIndex] = markedRgbLine;
		rgbDark[lastTourIndex] = markedRgbDark;
		rgbBright[lastTourIndex] = markedRgbBright;

		/*
		 * power
		 */
		final ChartDataXSerie xDataPower = new ChartDataXSerie(powerSerie);
		xDataPower.setLabel(Messages.Graph_Label_Power);
		xDataPower.setUnitLabel(Messages.Graph_Label_Power_unit);

		/*
		 * pulse
		 */
		_yDataPulse = new ChartDataYSerie(ChartDataModel.CHART_TYPE_XY_SCATTER, pulseSerie);
		_yDataPulse.setYTitle(Messages.Graph_Label_Heartbeat);
		_yDataPulse.setUnitLabel(Messages.Graph_Label_Heartbeat_unit);
		_yDataPulse.setRgbLine(rgbLine);
		_yDataPulse.setRgbDark(rgbDark);
		_yDataPulse.setRgbBright(rgbBright);

		//adjust min/max values that the chart do not stick to a border
		xDataPower.setVisibleMinValue(0, true);
		xDataPower.setVisibleMaxValue(xDataPower.getVisibleMaxValue() + 20, true);
		_yDataPulse.setVisibleMinValue(_yDataPulse.getVisibleMinValue() - 10, true);
		_yDataPulse.setVisibleMaxValue(_yDataPulse.getVisibleMaxValue() + 10, true);

		// setup chart data model
		chartDataModel.setXData(xDataPower);
		chartDataModel.addYData(_yDataPulse);

		_conconiData = createConconiData(powerSerie[lastTourIndex], pulseSerie[lastTourIndex]);

		/*
		 * updata layer for regression lines
		 */
		final ArrayList<IChartLayer> chartCustomLayers = new ArrayList<IChartLayer>();
		chartCustomLayers.add(_conconiLayer);

		_yDataPulse.setCustomLayers(chartCustomLayers);
		_yDataPulse.setCustomData(TourManager.CUSTOM_DATA_CONCONI_TEST, _conconiData);

		return chartDataModel;
	}

	private ConconiData createConconiData(final int[] xValues, final int[] yValues) {

		final TDoubleArrayList maxXValues = new TDoubleArrayList();
		final TDoubleArrayList maxYValues = new TDoubleArrayList();

		int lastMaxY = Integer.MIN_VALUE;
		int currentXValue = xValues[0];

		// loop: all values in the current serie
		for (int valueIndex = 0; valueIndex < xValues.length; valueIndex++) {

			// check array bounds
			if (valueIndex >= yValues.length) {
				break;
			}

			final int xValue = xValues[valueIndex];
			final int yValue = yValues[valueIndex];

			// ignore 0 values
//			if (xValue == 0) {
//				continue;
//			}

			if (xValue == currentXValue) {

				// get maximum y value for the same x value

				if (yValue > lastMaxY) {
					lastMaxY = yValue;
				}

			} else {

				// next x value is displayed, keep last max y

				maxXValues.add(currentXValue);
				maxYValues.add(lastMaxY);

				currentXValue = xValue;
				lastMaxY = yValue;
			}
		}

		// get last value
		maxXValues.add(currentXValue);
		maxYValues.add(lastMaxY);

		final ConconiData conconiData = new ConconiData();
		conconiData.maxXValues = maxXValues;
		conconiData.maxYValues = maxYValues;
		conconiData.selectedDeflection = _scaleDeflection.getSelection();

		return conconiData;
	}

	@Override
	public void createPartControl(final Composite parent) {

		createUI(parent);

		addSelectionListener();
		addPartListener();

		// show conconi chart from selection service
		onSelectionChanged(getSite().getWorkbenchWindow().getSelectionService().getSelection());

		// check if tour chart is displayed
		if (_conconiTours == null) {
			showTourFromTourProvider();
		}
	}

	private void createUI(final Composite parent) {

		_pc = new PixelConverter(parent);

		_pageBook = new PageBook(parent, SWT.NONE);

		_pageNoChart = new Label(_pageBook, SWT.NONE);
		_pageNoChart.setText(Messages.UI_Label_TourIsNotSelected);

		createUI10ConconiTest(_pageBook);
	}

	private void createUI10ConconiTest(final Composite parent) {

		_pageConconiTest = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(_pageConconiTest);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(_pageConconiTest);
		{
			createUI20ConconiChart(_pageConconiTest);
			createUI30PanelOptions(_pageConconiTest);
		}
	}

	/**
	 * chart: conconi test
	 */
	private void createUI20ConconiChart(final Composite parent) {

		_chartConconiTest = new Chart(parent, SWT.FLAT);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(_chartConconiTest);

		_conconiLayer = new ChartLayerConconiTest();
	}

	private void createUI30PanelOptions(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(2).extendedMargins(5, 5, 0, 0).applyTo(container);
		{
			/*
			 * label: tour
			 */
			Label label = new Label(container, SWT.NONE);
			label.setText(Messages.Conconi_Chart_Label_Tour);
			label.setToolTipText(Messages.Conconi_Chart_Label_Tour_Tooltip);

			/*
			 * combo: tour date/time
			 */
			_comboTours = new Combo(container, SWT.DROP_DOWN | SWT.READ_ONLY);
			_comboTours.setVisibleItemCount(20);
			_comboTours.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					onSelectTour();
				}
			});

			/*
			 * label: deflaction point
			 */
			label = new Label(container, SWT.NONE);
			label.setText(Messages.Conconi_Chart_DeflactionPoint);

			final Composite deflContainer = new Composite(container, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(deflContainer);
			GridLayoutFactory.fillDefaults().numColumns(2).applyTo(deflContainer);
			{
				/*
				 * scale: deflection point
				 */
				_scaleDeflection = new Scale(deflContainer, SWT.HORIZONTAL);
				GridDataFactory.fillDefaults().grab(true, false).applyTo(_scaleDeflection);
				_scaleDeflection.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(final SelectionEvent e) {
						onSelectDeflection();
//					setTourDirty();
					}
				});

				createUI32DeflPointValues(deflContainer);
			}
		}
	}

	private void createUI32DeflPointValues(final Composite parent) {

		Label label;

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(false, false).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
		{
			// label: heartbeat value
			_lblDeflactionPulse = new Label(container, SWT.TRAIL);
			GridDataFactory
					.fillDefaults()
					.align(SWT.FILL, SWT.CENTER)
					.hint(_pc.convertWidthInCharsToPixels(4), SWT.DEFAULT)
					.applyTo(_lblDeflactionPulse);

			// label: heartbeat unit
			label = new Label(container, SWT.NONE);
			GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(label);
			label.setText(Messages.Graph_Label_Heartbeat_unit);

			// label: power value
			_lblDeflactionPower = new Label(container, SWT.TRAIL);
			GridDataFactory
					.fillDefaults()
					.align(SWT.FILL, SWT.CENTER)
					.hint(_pc.convertWidthInCharsToPixels(4), SWT.DEFAULT)
					.applyTo(_lblDeflactionPower);

			// label: power unit
			label = new Label(container, SWT.NONE);
			GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(label);
			label.setText(UI.UNIT_LABEL_POWER);
		}
	}

	@Override
	public void dispose() {

		getViewSite().getPage().removePartListener(_partListener);
		getSite().getPage().removeSelectionListener(_postSelectionListener);
		TourManager.getInstance().removeTourEventListener(_tourEventListener);

		super.dispose();
	}

	private void onSelectDeflection() {

		// update conconi data
		_conconiData.selectedDeflection = _scaleDeflection.getSelection();
		_yDataPulse.setCustomData(TourManager.CUSTOM_DATA_CONCONI_TEST, _conconiData);

		updateUI20ConconiValues();

		// update tolerance into the tour data
//		_tourData.setConconiDeflection(_scaleDeflection.getSelection());
	}

	private void onSelectionChanged(final ISelection selection) {

		if (_pageBook != null && _pageBook.isDisposed()) {
			return;
		}

		if (selection instanceof SelectionTourData) {

			final TourData tourData = ((SelectionTourData) selection).getTourData();
			if (tourData != null) {

//				savePreviousTour(selectionTourData);

				updateChart20(tourData);
			}

		} else if (selection instanceof SelectionTourIds) {

			final SelectionTourIds selectionTourId = (SelectionTourIds) selection;
			final ArrayList<Long> tourIds = selectionTourId.getTourIds();
			if (tourIds != null && tourIds.size() > 0) {

//				savePreviousTour(tourId);

				updateChart12(tourIds);
			}

		} else if (selection instanceof SelectionTourId) {

			final SelectionTourId selectionTourId = (SelectionTourId) selection;
			final Long tourId = selectionTourId.getTourId();

			updateChart10(tourId);

		} else if (selection instanceof SelectionDeletedTours) {

			clearView();
		}
	}

	private void onSelectTour() {

		if (_isSelectionDisabled) {
			return;
		}

		int selectedIndex = _comboTours.getSelectionIndex();
		if (selectedIndex == -1) {
			selectedIndex = 0;
		}

		updateChart24(_conconiTours.get(selectedIndex));
	}

	private void restoreStateConconiUI() {

		// restore width for the marker list when the width is available
		try {
			_detailFormConconi.setViewerWidth(_state.getInt(STATE_CONCONIT_TOURS_VIEWER_WIDTH));
		} catch (final NumberFormatException e) {
			// ignore
		}
	}

	private void saveState() {

		// check if UI is disposed
		if (_pageBook.isDisposed()) {
			return;
		}

		if (_containerConconiTours != null) {

			final int viewerWidth = _containerConconiTours.getSize().x;
			if (viewerWidth > 0) {
				_state.put(STATE_CONCONIT_TOURS_VIEWER_WIDTH, viewerWidth);
			}
		}
	}

	@Override
	public void setFocus() {

		if (_pageConconiTest != null && _pageConconiTest.isVisible()) {
			_chartConconiTest.setFocus();
		}
	}

	private void showTourFromTourProvider() {

		_pageBook.showPage(_pageNoChart);

		// a tour is not displayed, find a tour provider which provides a tour
		Display.getCurrent().asyncExec(new Runnable() {
			public void run() {

				// validate widget
				if (_pageBook.isDisposed()) {
					return;
				}

				/*
				 * check if tour was set from a selection provider
				 */
				if (_conconiTours != null) {
					return;
				}

				final ArrayList<TourData> selectedTours = TourManager.getSelectedTours();
				if (selectedTours != null && selectedTours.size() > 0) {
//					updateChart10(selectedTours.get(0));
				}
			}
		});
	}

	private void updateChart10(final Long tourId) {

		final ArrayList<Long> tourIds = new ArrayList<Long>();
		tourIds.add(tourId);

		updateChart12(tourIds);
	}

	private void updateChart12(final ArrayList<Long> tourIds) {
		updateChart22(TourManager.getInstance().getTourData(tourIds));
	}

	private void updateChart20(final TourData tourData) {

		if (tourData == null) {
			return;
		}

		final ArrayList<TourData> tourDataList = new ArrayList<TourData>();
		tourDataList.add(tourData);

		updateChart22(tourDataList);
	}

	private void updateChart22(final ArrayList<TourData> tourDataList) {

		if (tourDataList == null || tourDataList.size() == 0) {
			// nothing to do
			clearView();
			return;
		}

		// sort tours by date/time
		Collections.sort(tourDataList);

		_conconiTours = tourDataList;

		updateUI10SetupConconi();
		updateChart24(null);

		_pageBook.showPage(_pageConconiTest);

		return;
	}

	/**
	 * @param markedTour
	 *            contains a tour which is marked in the conconi chart
	 */
	private void updateChart24(final TourData markedTour) {

		final ChartDataModel conconiChartDataModel = createChartDataModelConconiTest(_conconiTours, markedTour);

		updateUI12SetupNewTour();

		_chartConconiTest.updateChart(conconiChartDataModel, true, true);

		/*
		 * force the chart to be repainted because updating the conconi layer requires that the
		 * chart is already painted (it requires drawing data)
		 */
		_chartConconiTest.resizeChart();
		updateUI20ConconiValues();
	}

	private void updateUI10SetupConconi() {

		_isSelectionDisabled = true;
		{
			/*
			 * tour combo box
			 */
			_comboTours.removeAll();

			for (final TourData tourData : _conconiTours) {
				_comboTours.add(_tourDTFormatter.print(tourData.getStartDateTime()));
			}

			_comboTours.select(0);
		}
		_isSelectionDisabled = false;
	}

	private void updateUI12SetupNewTour() {

		/*
		 * update deflection scale
		 */
		final int maxDeflection = _conconiData.maxXValues.size();
		_scaleDeflection.setMaximum(maxDeflection > 0 ? maxDeflection - 1 : 0);

		// ensure that too much scale ticks are displayed
		final int pageIncrement = maxDeflection < 20 ? 1 : maxDeflection < 100 ? 5 : maxDeflection < 1000 ? 50 : 100;

		_scaleDeflection.setPageIncrement(pageIncrement);
	}

	private void updateUI20ConconiValues() {

		// deflation values
		final int scaleIndex = _scaleDeflection.getSelection();
		final int pulseValue = (int) _conconiData.maxYValues.get(scaleIndex);
		final int powerValue = (int) _conconiData.maxXValues.get(scaleIndex);

		_lblDeflactionPulse.setText(Integer.toString(pulseValue));
		_lblDeflactionPower.setText(Integer.toString(powerValue));

		// update conconi layer
		_chartConconiTest.updateCustomLayers();
	}

}
