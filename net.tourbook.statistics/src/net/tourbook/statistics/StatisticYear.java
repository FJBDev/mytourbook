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
package net.tourbook.statistics;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.chart.BarChartMinMaxKeeper;
import net.tourbook.chart.Chart;
import net.tourbook.chart.ChartDataModel;
import net.tourbook.chart.ChartDataSerie;
import net.tourbook.chart.ChartDataXSerie;
import net.tourbook.chart.ChartDataYSerie;
import net.tourbook.chart.ChartSegments;
import net.tourbook.chart.ChartToolTipInfo;
import net.tourbook.chart.IChartInfoProvider;
import net.tourbook.colors.GraphColorProvider;
import net.tourbook.data.TourPerson;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.statistic.StatisticContext;
import net.tourbook.ui.TourTypeFilter;
import net.tourbook.ui.UI;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.IPostSelectionProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPartSite;

public abstract class StatisticYear extends YearStatistic {

	private static final String			STRING_SEPARATOR	= " - ";						//$NON-NLS-1$

	private TourPerson					_activePerson;
	private TourTypeFilter				_activeTourTypeFilter;

	private int							_currentYear;
	private int							_numberOfYears;

	private Chart						_chart;

	private final BarChartMinMaxKeeper	_minMaxKeeper		= new BarChartMinMaxKeeper();
	private TourDataYear				_tourYearData;

	private boolean						_isSynchScaleEnabled;

	@Override
	public void activateActions(final IWorkbenchPartSite partSite) {
		_chart.updateChartActionHandlers();
	}

	public boolean canTourBeVisible() {
		return false;
	}

	ChartSegments createChartSegments(final TourDataYear tourDataYear) {

		final int yearCounter = tourDataYear.altitudeHigh[0].length;

		final long segmentStart[] = new long[_numberOfYears];
		final long segmentEnd[] = new long[_numberOfYears];
		final String[] segmentTitle = new String[_numberOfYears];

		final int oldestYear = _currentYear - _numberOfYears + 1;

		for (int yearIndex = 0; yearIndex < yearCounter; yearIndex++) {

			segmentTitle[yearIndex] = Integer.toString(oldestYear + yearIndex);

			segmentStart[yearIndex] = yearIndex;
			segmentEnd[yearIndex] = yearIndex;
		}

		final ChartSegments yearSegments = new ChartSegments();
		yearSegments.valueStart = segmentStart;
		yearSegments.valueEnd = segmentEnd;
		yearSegments.segmentTitle = segmentTitle;
		yearSegments.years = tourDataYear.years;

		return yearSegments;
	}

	@Override
	public void createControl(	final Composite parent,
								final IViewSite viewSite,
								final IPostSelectionProvider postSelectionProvider) {

		super.createControl(parent);

		// create chart
		_chart = new Chart(parent, SWT.BORDER | SWT.FLAT);
		_chart.setShowZoomActions(true);
		_chart.setToolBarManager(viewSite.getActionBars().getToolBarManager(), false);
	}

	private ChartToolTipInfo createToolTipInfo(final int serieIndex, final int valueIndex) {

		final int oldestYear = _currentYear - _numberOfYears + 1;

		final Integer recordingTime = _tourYearData.recordingTime[serieIndex][valueIndex];
		final Integer drivingTime = _tourYearData.drivingTime[serieIndex][valueIndex];
		final int breakTime = recordingTime - drivingTime;

		/*
		 * tool tip: title
		 */
		final StringBuilder titleString = new StringBuilder();

		final String tourTypeName = getTourTypeName(serieIndex, _activeTourTypeFilter);
		final boolean isTourType = tourTypeName != null && tourTypeName.length() > 0;

		if (isTourType) {
			titleString.append(tourTypeName);
			titleString.append(STRING_SEPARATOR);
		}
		titleString.append(Messages.tourtime_info_date_year);
		titleString.append(UI.NEW_LINE);

		final String toolTipTitle = String.format(titleString.toString(), oldestYear + valueIndex).toString();

		/*
		 * tool tip: label
		 */
		final StringBuilder toolTipFormat = new StringBuilder();
		toolTipFormat.append(Messages.tourtime_info_distance);
		toolTipFormat.append(UI.NEW_LINE);
		toolTipFormat.append(Messages.tourtime_info_altitude);
		toolTipFormat.append(UI.NEW_LINE);
		toolTipFormat.append(UI.NEW_LINE);
		toolTipFormat.append(Messages.tourtime_info_recording_time);
		toolTipFormat.append(UI.NEW_LINE);
		toolTipFormat.append(Messages.tourtime_info_driving_time);
		toolTipFormat.append(UI.NEW_LINE);
		toolTipFormat.append(Messages.tourtime_info_break_time);

		final String toolTipLabel = String.format(toolTipFormat.toString(), //
				//
				//
				(int) _tourYearData.distanceHigh[serieIndex][valueIndex],
				UI.UNIT_LABEL_DISTANCE,
				//
				(int) _tourYearData.altitudeHigh[serieIndex][valueIndex],
				UI.UNIT_LABEL_ALTITUDE,
				//
				recordingTime / 3600,
				(recordingTime % 3600) / 60,
				//
				drivingTime / 3600,
				(drivingTime % 3600) / 60,
				//
				breakTime / 3600,
				(breakTime % 3600) / 60
		//
				)
				.toString();

		/*
		 * create tool tip info
		 */

		final ChartToolTipInfo toolTipInfo = new ChartToolTipInfo();
		toolTipInfo.setTitle(toolTipTitle);
		toolTipInfo.setLabel(toolTipLabel);
//		toolTipInfo.setLabel(toolTipFormat.toString());

		return toolTipInfo;
	}

	void createXDataYear(final ChartDataModel chartDataModel) {
		// set the x-axis
		final ChartDataXSerie xData = new ChartDataXSerie(createYearData(_tourYearData));
		xData.setAxisUnit(ChartDataXSerie.X_AXIS_UNIT_YEAR);
		xData.setChartSegments(createChartSegments(_tourYearData));
		chartDataModel.setXData(xData);
	}

	/**
	 * altitude
	 * 
	 * @param chartDataModel
	 */
	void createYDataAltitude(final ChartDataModel chartDataModel) {

		final ChartDataYSerie yData = new ChartDataYSerie(
				ChartDataModel.CHART_TYPE_BAR,
				ChartDataYSerie.BAR_LAYOUT_BESIDE,
				_tourYearData.altitudeLow,
				_tourYearData.altitudeHigh);
		yData.setYTitle(Messages.LABEL_GRAPH_ALTITUDE);
		yData.setUnitLabel(UI.UNIT_LABEL_ALTITUDE);
		yData.setAxisUnit(ChartDataSerie.AXIS_UNIT_NUMBER);
		StatisticServices.setTourTypeColors(yData, GraphColorProvider.PREF_GRAPH_ALTITUDE, _activeTourTypeFilter);
		StatisticServices.setTourTypeColorIndex(yData, _tourYearData.typeIds, _activeTourTypeFilter);
		StatisticServices.setDefaultColors(yData, GraphColorProvider.PREF_GRAPH_ALTITUDE);

		chartDataModel.addYData(yData);
	}

	/**
	 * distance
	 * 
	 * @param chartDataModel
	 */
	void createYDataDistance(final ChartDataModel chartDataModel) {

		final ChartDataYSerie yData = new ChartDataYSerie(
				ChartDataModel.CHART_TYPE_BAR,
				ChartDataYSerie.BAR_LAYOUT_BESIDE,
				_tourYearData.distanceLow,
				_tourYearData.distanceHigh);
		yData.setYTitle(Messages.LABEL_GRAPH_DISTANCE);
		yData.setUnitLabel(UI.UNIT_LABEL_DISTANCE);
		yData.setAxisUnit(ChartDataSerie.AXIS_UNIT_NUMBER);
		StatisticServices.setTourTypeColors(yData, GraphColorProvider.PREF_GRAPH_DISTANCE, _activeTourTypeFilter);
		StatisticServices.setTourTypeColorIndex(yData, _tourYearData.typeIds, _activeTourTypeFilter);
		StatisticServices.setDefaultColors(yData, GraphColorProvider.PREF_GRAPH_DISTANCE);

		chartDataModel.addYData(yData);
	}

	/**
	 * duration
	 * 
	 * @param chartDataModel
	 */
	void createYDataDuration(final ChartDataModel chartDataModel) {

		final ChartDataYSerie yData = new ChartDataYSerie(
				ChartDataModel.CHART_TYPE_BAR,
				ChartDataYSerie.BAR_LAYOUT_BESIDE,
				_tourYearData.getTimeLowFloat(),
				_tourYearData.getTimeHighFloat());
		yData.setYTitle(Messages.LABEL_GRAPH_TIME);
		yData.setUnitLabel(Messages.LABEL_GRAPH_TIME_UNIT);
		yData.setAxisUnit(ChartDataSerie.AXIS_UNIT_HOUR_MINUTE);
		StatisticServices.setTourTypeColors(yData, GraphColorProvider.PREF_GRAPH_TIME, _activeTourTypeFilter);
		StatisticServices.setTourTypeColorIndex(yData, _tourYearData.typeIds, _activeTourTypeFilter);
		StatisticServices.setDefaultColors(yData, GraphColorProvider.PREF_GRAPH_TIME);

		chartDataModel.addYData(yData);
	}

	private double[] createYearData(final TourDataYear tourDataYear) {

		final int yearCounter = tourDataYear.altitudeHigh[0].length;
		final double allYears[] = new double[yearCounter];

		for (int yearIndex = 0; yearIndex < yearCounter; yearIndex++) {
			allYears[yearIndex] = yearIndex;
		}

		return allYears;
	}

	@Override
	public void deactivateActions(final IWorkbenchPartSite partSite) {}

	public void preferencesHasChanged() {
		updateStatistic(new StatisticContext(_activePerson, _activeTourTypeFilter, _currentYear, _numberOfYears, false));
	}

	@Override
	public void resetSelection() {
		_chart.setSelectedBars(null);
	}

	private void setChartProviders(final ChartDataModel chartModel) {

		// set tool tip info
		chartModel.setCustomData(ChartDataModel.BAR_TOOLTIP_INFO_PROVIDER, new IChartInfoProvider() {
			@Override
			public ChartToolTipInfo getToolTipInfo(final int serieIndex, final int valueIndex) {
				return createToolTipInfo(serieIndex, valueIndex);
			}
		});
	}

	@Override
	public void setSynchScale(final boolean isSynchScaleEnabled) {
		_isSynchScaleEnabled = isSynchScaleEnabled;
	}

	abstract ChartDataModel updateChart();

	public void updateStatistic(final StatisticContext statContext) {

		_activePerson = statContext.appPerson;
		_activeTourTypeFilter = statContext.appTourTypeFilter;
		_currentYear = statContext.statYoungestYear;
		_numberOfYears = statContext.statNumberOfYears;

		_tourYearData = DataProviderTourYear.getInstance().getYearData(
				statContext.appPerson,
				statContext.appTourTypeFilter,
				statContext.statYoungestYear,
				statContext.statNumberOfYears,
				isDataDirtyWithReset() || statContext.isRefreshData);

		// reset min/max values
		if (_isSynchScaleEnabled == false && statContext.isRefreshData) {
			_minMaxKeeper.resetMinMax();
		}

		final ChartDataModel chartDataModel = updateChart();

		setChartProviders(chartDataModel);

		if (_isSynchScaleEnabled) {
			_minMaxKeeper.setMinMaxValues(chartDataModel);
		}

		// set grid size
		final IPreferenceStore prefStore = TourbookPlugin.getDefault().getPreferenceStore();
		_chart.setGrid(
				prefStore.getInt(ITourbookPreferences.GRAPH_GRID_HORIZONTAL_DISTANCE),
				prefStore.getInt(ITourbookPreferences.GRAPH_GRID_VERTICAL_DISTANCE),
				prefStore.getBoolean(ITourbookPreferences.GRAPH_GRID_IS_SHOW_HORIZONTAL_GRIDLINES),
				prefStore.getBoolean(ITourbookPreferences.GRAPH_GRID_IS_SHOW_VERTICAL_GRIDLINES));

		// show the fDataModel in the chart
		_chart.updateChart(chartDataModel, true);

	}

	@Override
	public void updateToolBar(final boolean refreshToolbar) {
		_chart.fillToolbar(refreshToolbar);
	}
}
