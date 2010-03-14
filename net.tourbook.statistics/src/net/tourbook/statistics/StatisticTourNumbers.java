/*******************************************************************************
 * Copyright (C) 2005, 2009  Wolfgang Schramm and Contributors
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

import java.util.ArrayList;
import java.util.Formatter;

import net.tourbook.chart.BarChartMinMaxKeeper;
import net.tourbook.chart.Chart;
import net.tourbook.chart.ChartDataModel;
import net.tourbook.chart.ChartDataSerie;
import net.tourbook.chart.ChartDataXSerie;
import net.tourbook.chart.ChartDataYSerie;
import net.tourbook.chart.ChartToolTipInfo;
import net.tourbook.chart.ChartUtil;
import net.tourbook.chart.IChartInfoProvider;
import net.tourbook.colors.GraphColorProvider;
import net.tourbook.data.TourPerson;
import net.tourbook.data.TourType;
import net.tourbook.database.TourDatabase;
import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.ui.TourTypeFilter;
import net.tourbook.ui.UI;

import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.runtime.Preferences.IPropertyChangeListener;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.IPostSelectionProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPartSite;

public class StatisticTourNumbers extends YearStatistic {

	private Composite					fStatisticPage;

	private Chart						fChartDistanceCounter;
	private Chart						fChartDistanceSum;
	private Chart						fChartDurationCounter;
	private Chart						fChartDurationSum;
	private Chart						fChartAltitudeCounter;
	private Chart						fChartAltitudeSum;

	private final BarChartMinMaxKeeper	fMinMaxKeeperStatAltitudeCounter	= new BarChartMinMaxKeeper();
	private final BarChartMinMaxKeeper	fMinMaxKeeperStatAltitudeSum		= new BarChartMinMaxKeeper();
	private final BarChartMinMaxKeeper	fMinMaxKeeperStatDistanceCounter	= new BarChartMinMaxKeeper();
	private final BarChartMinMaxKeeper	fMinMaxKeeperStatDistanceSum		= new BarChartMinMaxKeeper();
	private final BarChartMinMaxKeeper	fMinMaxKeeperStatDurationCounter	= new BarChartMinMaxKeeper();
	private final BarChartMinMaxKeeper	fMinMaxKeeperStatDurationSum		= new BarChartMinMaxKeeper();

	private int[]						fStatDistanceUnits;
	private int[]						fStatAltitudeUnits;
	private int[]						fStatTimeUnits;

	private int[][]						fStatDistanceCounterLow;
	private int[][]						fStatDistanceCounterHigh;
	private int[][]						fStatDistanceCounterColorIndex;

	private int[][]						fStatDistanceSumLow;
	private int[][]						fStatDistanceSumHigh;
	private int[][]						fStatDistanceSumColorIndex;

	private int[][]						fStatAltitudeCounterLow;
	private int[][]						fStatAltitudeCounterHigh;
	private int[][]						fStatAltitudeCounterColorIndex;

	private int[][]						fStatAltitudeSumLow;
	private int[][]						fStatAltitudeSumHigh;
	private int[][]						fStatAltitudeSumColorIndex;

	private int[][]						fStatTimeCounterLow;
	private int[][]						fStatTimeCounterHigh;
	private int[][]						fStatTimeCounterColorIndex;

	private int[][]						fStatTimeSumLow;
	private int[][]						fStatTimeSumHigh;
	private int[][]						fStatTimeSumColorIndex;

	private IPropertyChangeListener		fPrefChangeListener;

	private int							fCurrentYear;
	private TourPerson					fActivePerson;
	protected TourTypeFilter			fActiveTourTypeFilter;

	private boolean						fIsSynchScaleEnabled;

	private TourDayData					fTourDayData;

	private IViewSite					fViewSite;

	public StatisticTourNumbers() {}

	@Override
	public void activateActions(final IWorkbenchPartSite partSite) {}

	void addPrefListener(final Composite container) {

		// create pref listener
		fPrefChangeListener = new Preferences.IPropertyChangeListener() {
			public void propertyChange(final Preferences.PropertyChangeEvent event) {
				final String property = event.getProperty();

				// test if the color or statistic data have changed
				if (property.equals(ITourbookPreferences.GRAPH_COLORS_HAS_CHANGED)
						|| property.equals(ITourbookPreferences.STAT_DISTANCE_NUMBERS)
						|| property.equals(ITourbookPreferences.STAT_DISTANCE_LOW_VALUE)
						|| property.equals(ITourbookPreferences.STAT_DISTANCE_INTERVAL)
						|| property.equals(ITourbookPreferences.STAT_ALTITUDE_NUMBERS)
						|| property.equals(ITourbookPreferences.STAT_ALTITUDE_LOW_VALUE)
						|| property.equals(ITourbookPreferences.STAT_ALTITUDE_INTERVAL)
						|| property.equals(ITourbookPreferences.STAT_DURATION_NUMBERS)
						|| property.equals(ITourbookPreferences.STAT_DURATION_LOW_VALUE)
						|| property.equals(ITourbookPreferences.STAT_DURATION_INTERVAL)) {

					// get the changed preferences
					getPreferences();

					/*
					 * reset min/max keeper because they can be changed when the pref has changed
					 */
					resetMinMaxKeeper();

					// update chart
					refreshStatistic(fActivePerson, fActiveTourTypeFilter, fCurrentYear, 1, false);
				}
			}
		};

		// add pref listener
		TourbookPlugin.getDefault().getPluginPreferences().addPropertyChangeListener(fPrefChangeListener);

		// remove pref listener
		container.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(final DisposeEvent e) {
				TourbookPlugin.getDefault().getPluginPreferences().removePropertyChangeListener(fPrefChangeListener);
			}
		});
	}

	public boolean canTourBeVisible() {
		return false;
	}

	@Override
	public void createControl(	final Composite parent,
								final IViewSite viewSite,
								final IPostSelectionProvider postSelectionProvider) {

		super.createControl(parent);

		fViewSite = viewSite;

		// create statistic page
		fStatisticPage = new Composite(parent, SWT.BORDER | SWT.FLAT);
		fStatisticPage.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		// remove colored border
		fStatisticPage.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));

		final GridLayout gl = new GridLayout(2, true);
		gl.marginHeight = 0;
		gl.marginWidth = 0;
		gl.verticalSpacing = 0;
		gl.horizontalSpacing = 0;
		fStatisticPage.setLayout(gl);

		fChartDistanceCounter = new Chart(fStatisticPage, SWT.NONE);
		fChartDistanceSum = new Chart(fStatisticPage, SWT.NONE);

		fChartAltitudeCounter = new Chart(fStatisticPage, SWT.NONE);
		fChartAltitudeSum = new Chart(fStatisticPage, SWT.NONE);

		fChartDurationCounter = new Chart(fStatisticPage, SWT.NONE);
		fChartDurationSum = new Chart(fStatisticPage, SWT.NONE);

		fChartDistanceCounter.setToolBarManager(viewSite.getActionBars().getToolBarManager(), true);

		addPrefListener(parent);
		getPreferences();
	}

	/**
	 * calculate data for all statistics
	 * 
	 * @param tourDayData
	 */
	private void createStatisticData(final TourDayData tourDayData) {

		int colorOffset = 0;
		if (fActiveTourTypeFilter.showUndefinedTourTypes()) {
			colorOffset = StatisticServices.TOUR_TYPE_COLOR_INDEX_OFFSET;
		}

		final ArrayList<TourType> tourTypeList = TourDatabase.getActiveTourTypes();
		final int colorLength = colorOffset + tourTypeList.size();

		final int distanceLength = fStatDistanceUnits.length;
		final int altitudeLength = fStatAltitudeUnits.length;
		final int timeLength = fStatTimeUnits.length;

		fStatDistanceCounterLow = new int[colorLength][distanceLength];
		fStatDistanceCounterHigh = new int[colorLength][distanceLength];
		fStatDistanceCounterColorIndex = new int[colorLength][distanceLength];

		fStatDistanceSumLow = new int[colorLength][distanceLength];
		fStatDistanceSumHigh = new int[colorLength][distanceLength];
		fStatDistanceSumColorIndex = new int[colorLength][distanceLength];

		fStatAltitudeCounterLow = new int[colorLength][altitudeLength];
		fStatAltitudeCounterHigh = new int[colorLength][altitudeLength];
		fStatAltitudeCounterColorIndex = new int[colorLength][altitudeLength];

		fStatAltitudeSumLow = new int[colorLength][altitudeLength];
		fStatAltitudeSumHigh = new int[colorLength][altitudeLength];
		fStatAltitudeSumColorIndex = new int[colorLength][altitudeLength];

		fStatTimeCounterLow = new int[colorLength][timeLength];
		fStatTimeCounterHigh = new int[colorLength][timeLength];
		fStatTimeCounterColorIndex = new int[colorLength][timeLength];

		fStatTimeSumLow = new int[colorLength][timeLength];
		fStatTimeSumHigh = new int[colorLength][timeLength];
		fStatTimeSumColorIndex = new int[colorLength][timeLength];

		// loop: all tours
		for (int tourIndex = 0; tourIndex < tourDayData.distanceHigh.length; tourIndex++) {

			final int typeColorIndex = tourDayData.typeColorIndex[tourIndex];
			int unitIndex;

			final int diffDistance = (tourDayData.distanceHigh[tourIndex] - tourDayData.distanceLow[tourIndex] + 500) / 1000;
			final int diffAltitude = tourDayData.altitudeHigh[tourIndex] - tourDayData.altitudeLow[tourIndex];
			final int diffTime = tourDayData.timeHigh[tourIndex] - tourDayData.timeLow[tourIndex];

			unitIndex = createTourStatData(diffDistance,
					fStatDistanceUnits,
					fStatDistanceCounterHigh[typeColorIndex],
					fStatDistanceSumHigh[typeColorIndex]);

			fStatDistanceCounterColorIndex[typeColorIndex][unitIndex] = typeColorIndex;
			fStatDistanceSumColorIndex[typeColorIndex][unitIndex] = typeColorIndex;

			unitIndex = createTourStatData(diffAltitude,
					fStatAltitudeUnits,
					fStatAltitudeCounterHigh[typeColorIndex],
					fStatAltitudeSumHigh[typeColorIndex]);

			fStatAltitudeCounterColorIndex[typeColorIndex][unitIndex] = typeColorIndex;
			fStatAltitudeSumColorIndex[typeColorIndex][unitIndex] = typeColorIndex;

			unitIndex = createTourStatData(diffTime,
					fStatTimeUnits,
					fStatTimeCounterHigh[typeColorIndex],
					fStatTimeSumHigh[typeColorIndex]);

			fStatTimeCounterColorIndex[typeColorIndex][unitIndex] = typeColorIndex;
			fStatTimeSumColorIndex[typeColorIndex][unitIndex] = typeColorIndex;
		}

		updateLowHighValues(fStatDistanceCounterLow, fStatDistanceCounterHigh);
		updateLowHighValues(fStatDistanceSumLow, fStatDistanceSumHigh);
		updateLowHighValues(fStatAltitudeCounterLow, fStatAltitudeCounterHigh);
		updateLowHighValues(fStatAltitudeSumLow, fStatAltitudeSumHigh);
		updateLowHighValues(fStatTimeCounterLow, fStatTimeCounterHigh);
		updateLowHighValues(fStatTimeSumLow, fStatTimeSumHigh);
	}

	/**
	 * create tool tip info
	 */
	private ChartToolTipInfo createToolTipProvider(final int serieIndex, final String toolTipLabel) {

		final String tourTypeName = getTourTypeName(serieIndex, fActiveTourTypeFilter);

		final ChartToolTipInfo toolTipInfo = new ChartToolTipInfo();
		toolTipInfo.setTitle(tourTypeName);
		toolTipInfo.setLabel(toolTipLabel);

		return toolTipInfo;
	}

	private void createToolTipProviderAltitude(final ChartDataModel chartModel) {

		chartModel.setCustomData(ChartDataModel.BAR_TOOLTIP_INFO_PROVIDER, new IChartInfoProvider() {
			public ChartToolTipInfo getToolTipInfo(final int serieIndex, final int valueIndex) {

				String toolTipLabel;
				final StringBuilder infoText = new StringBuilder();

				if (valueIndex == 0) {

					infoText.append(Messages.numbers_info_altitude_down);
					infoText.append(NEW_LINE);
					infoText.append(Messages.numbers_info_altitude_total);

					toolTipLabel = new Formatter().format(infoText.toString(),
							fStatAltitudeUnits[valueIndex],
							UI.UNIT_LABEL_ALTITUDE,
							fStatAltitudeCounterHigh[serieIndex][valueIndex],
							//
							fStatAltitudeSumHigh[serieIndex][valueIndex],
							UI.UNIT_LABEL_ALTITUDE).toString();

				} else if (valueIndex == fStatAltitudeUnits.length - 1) {

					infoText.append(Messages.numbers_info_altitude_up);
					infoText.append(NEW_LINE);
					infoText.append(Messages.numbers_info_altitude_total);

					toolTipLabel = new Formatter().format(infoText.toString(),
							fStatAltitudeUnits[valueIndex - 1],
							UI.UNIT_LABEL_ALTITUDE,
							fStatAltitudeCounterHigh[serieIndex][valueIndex],
							//
							fStatAltitudeSumHigh[serieIndex][valueIndex],
							UI.UNIT_LABEL_ALTITUDE).toString();
				} else {

					infoText.append(Messages.numbers_info_altitude_between);
					infoText.append(NEW_LINE);
					infoText.append(Messages.numbers_info_altitude_total);

					toolTipLabel = new Formatter().format(infoText.toString(),
							fStatAltitudeUnits[valueIndex - 1],
							fStatAltitudeUnits[valueIndex],
							UI.UNIT_LABEL_ALTITUDE,
							fStatAltitudeCounterHigh[serieIndex][valueIndex],
							//
							fStatAltitudeSumHigh[serieIndex][valueIndex],
							UI.UNIT_LABEL_ALTITUDE).toString();
				}

				return createToolTipProvider(serieIndex, toolTipLabel);
			}
		});
	}

	private void createToolTipProviderDistance(final ChartDataModel chartModel) {

		chartModel.setCustomData(ChartDataModel.BAR_TOOLTIP_INFO_PROVIDER, new IChartInfoProvider() {

			public ChartToolTipInfo getToolTipInfo(final int serieIndex, final int valueIndex) {

				String toolTipLabel;
				final StringBuilder sb = new StringBuilder();

				final int distance = fStatDistanceSumHigh[serieIndex][valueIndex];
				final int counter = fStatDistanceCounterHigh[serieIndex][valueIndex];

				if (valueIndex == 0) {

					sb.append(Messages.numbers_info_distance_down);
					sb.append(NEW_LINE);
					sb.append(Messages.numbers_info_distance_total);

					toolTipLabel = new Formatter().format(sb.toString(),
							fStatDistanceUnits[valueIndex],
							UI.UNIT_LABEL_DISTANCE,
							counter,
							distance,
							UI.UNIT_LABEL_DISTANCE).toString();

				} else if (valueIndex == fStatDistanceUnits.length - 1) {

					sb.append(Messages.numbers_info_distance_up);
					sb.append(NEW_LINE);
					sb.append(Messages.numbers_info_distance_total);

					toolTipLabel = new Formatter().format(sb.toString(),
							fStatDistanceUnits[valueIndex - 1],
							UI.UNIT_LABEL_DISTANCE,
							counter,
							distance,
							UI.UNIT_LABEL_DISTANCE).toString();
				} else {

					sb.append(Messages.numbers_info_distance_between);
					sb.append(NEW_LINE);
					sb.append(Messages.numbers_info_distance_total);

					toolTipLabel = new Formatter().format(sb.toString(),
							fStatDistanceUnits[valueIndex - 1],
							fStatDistanceUnits[valueIndex],
							UI.UNIT_LABEL_DISTANCE,
							counter,
							distance,
							UI.UNIT_LABEL_DISTANCE).toString();
				}

				return createToolTipProvider(serieIndex, toolTipLabel);

			}
		});
	}

	private void createToolTipProviderDuration(final ChartDataModel chartModel) {

		chartModel.setCustomData(ChartDataModel.BAR_TOOLTIP_INFO_PROVIDER, new IChartInfoProvider() {
			public ChartToolTipInfo getToolTipInfo(final int serieIndex, final int valueIndex) {

				String toolTipLabel;
				final StringBuilder toolTipFormat = new StringBuilder();

				if (valueIndex == 0) {

					toolTipFormat.append(Messages.numbers_info_time_down);
					toolTipFormat.append(NEW_LINE);
					toolTipFormat.append(Messages.numbers_info_time_total);

					toolTipLabel = new Formatter().format(toolTipFormat.toString(),
							ChartUtil.formatValue(fStatTimeUnits[valueIndex], ChartDataSerie.AXIS_UNIT_HOUR_MINUTE),
							fStatTimeCounterHigh[serieIndex][valueIndex],
							ChartUtil.formatValue(fStatTimeSumHigh[serieIndex][valueIndex],
									ChartDataSerie.AXIS_UNIT_HOUR_MINUTE)).toString();

				} else if (valueIndex == fStatTimeUnits.length - 1) {

					toolTipFormat.append(Messages.numbers_info_time_up);
					toolTipFormat.append(NEW_LINE);
					toolTipFormat.append(Messages.numbers_info_time_total);

					toolTipLabel = new Formatter().format(toolTipFormat.toString(),
							ChartUtil.formatValue(fStatTimeUnits[valueIndex - 1], ChartDataSerie.AXIS_UNIT_HOUR_MINUTE),
							fStatTimeCounterHigh[serieIndex][valueIndex],
							ChartUtil.formatValue(fStatTimeSumHigh[serieIndex][valueIndex],
									ChartDataSerie.AXIS_UNIT_HOUR_MINUTE))
							.toString();
				} else {

					toolTipFormat.append(Messages.numbers_info_time_between);
					toolTipFormat.append(NEW_LINE);
					toolTipFormat.append(Messages.numbers_info_time_total);

					toolTipLabel = new Formatter().format(toolTipFormat.toString(),
							ChartUtil.formatValue(fStatTimeUnits[valueIndex - 1], ChartDataSerie.AXIS_UNIT_HOUR_MINUTE),
							ChartUtil.formatValue(fStatTimeUnits[valueIndex], ChartDataSerie.AXIS_UNIT_HOUR_MINUTE),
							fStatTimeCounterHigh[serieIndex][valueIndex],
							ChartUtil.formatValue(fStatTimeSumHigh[serieIndex][valueIndex],
									ChartDataSerie.AXIS_UNIT_HOUR_MINUTE))
							.toString();
				}

				return createToolTipProvider(serieIndex, toolTipLabel);
			}
		});
	}

	/**
	 * calculate the statistic for one tour
	 * 
	 * @param tourValue
	 * @param units
	 * @param counter
	 * @param sum
	 * @return
	 */
	private int createTourStatData(final int tourValue, final int[] units, final int[] counter, final int[] sum) {

		int lastUnit = -1;
		boolean isUnitFound = false;

		// loop: all units
		for (int unitIndex = 0; unitIndex < units.length; unitIndex++) {

			final int unit = units[unitIndex];

			if (lastUnit < 0) {
				// first unit
				if (tourValue < unit) {
					isUnitFound = true;
				}
			} else {
				// second and continuous units
				if (tourValue >= lastUnit && tourValue < unit) {
					isUnitFound = true;
				}
			}

			if (isUnitFound) {
				counter[unitIndex]++;
				sum[unitIndex] += tourValue;
				// colorIndex[unitIndex]=
				return unitIndex;
			} else {
				lastUnit = unit;
			}
		}

		// if the value was not found, add it to the last unit
		counter[units.length - 1]++;
		sum[units.length - 1] += tourValue;

		return units.length - 1;
	}

	@Override
	public void deactivateActions(final IWorkbenchPartSite partSite) {}

	private void getPreferences() {

		final IPreferenceStore store = TourbookPlugin.getDefault().getPreferenceStore();

		fStatDistanceUnits = getPrefUnits(store,
				ITourbookPreferences.STAT_DISTANCE_NUMBERS,
				ITourbookPreferences.STAT_DISTANCE_LOW_VALUE,
				ITourbookPreferences.STAT_DISTANCE_INTERVAL,
				ChartDataSerie.AXIS_UNIT_NUMBER);

		fStatAltitudeUnits = getPrefUnits(store,
				ITourbookPreferences.STAT_ALTITUDE_NUMBERS,
				ITourbookPreferences.STAT_ALTITUDE_LOW_VALUE,
				ITourbookPreferences.STAT_ALTITUDE_INTERVAL,
				ChartDataSerie.AXIS_UNIT_NUMBER);

		fStatTimeUnits = getPrefUnits(store,
				ITourbookPreferences.STAT_DURATION_NUMBERS,
				ITourbookPreferences.STAT_DURATION_LOW_VALUE,
				ITourbookPreferences.STAT_DURATION_INTERVAL,
				ChartDataSerie.AXIS_UNIT_HOUR_MINUTE);

	}

	/**
	 * create the units from the preference configuration
	 * 
	 * @param store
	 * @param prefInterval
	 * @param prefLowValue
	 * @param prefNumbers
	 * @param unitType
	 * @return
	 */
	private int[] getPrefUnits(	final IPreferenceStore store,
								final String prefNumbers,
								final String prefLowValue,
								final String prefInterval,
								final int unitType) {

		final int lowValue = store.getInt(prefLowValue);
		final int interval = store.getInt(prefInterval);
		final int numbers = store.getInt(prefNumbers);

		final int[] units = new int[numbers];

		for (int number = 0; number < numbers; number++) {
			if (unitType == ChartDataSerie.AXIS_UNIT_HOUR_MINUTE) {
				// adjust the values to minutes
				units[number] = (lowValue * 60) + (interval * number * 60);
			} else {
				units[number] = lowValue + (interval * number);
			}
		}

		return units;
	}

	public void prefColorChanged() {
		refreshStatistic(fActivePerson, fActiveTourTypeFilter, fCurrentYear, 1, false);
	}

	public void refreshStatistic(	final TourPerson person,
									final TourTypeFilter typeId,
									final int year,
									final int numberOfYears,
									final boolean refreshData) {

		fActivePerson = person;
		fActiveTourTypeFilter = typeId;
		fCurrentYear = year;

		fTourDayData = DataProviderTourDay.getInstance().getDayData(person,
				typeId,
				year,
				numberOfYears,
				isDataDirtyWithReset() || refreshData);

		// reset min/max values
		if (fIsSynchScaleEnabled == false && refreshData) {
			resetMinMaxKeeper();
		}

		// hide actions from other statistics
		final IToolBarManager tbm = fViewSite.getActionBars().getToolBarManager();
		tbm.removeAll();
		tbm.update(true);

		createStatisticData(fTourDayData);
		updateCharts();

	}

	private void resetMinMaxKeeper() {
		if (fIsSynchScaleEnabled == false) {
			fMinMaxKeeperStatAltitudeCounter.resetMinMax();
			fMinMaxKeeperStatAltitudeSum.resetMinMax();
			fMinMaxKeeperStatDistanceCounter.resetMinMax();
			fMinMaxKeeperStatDistanceSum.resetMinMax();
			fMinMaxKeeperStatDurationCounter.resetMinMax();
			fMinMaxKeeperStatDurationSum.resetMinMax();
		}
	}

	@Override
	public void resetSelection() {}

	@Override
	public void setSynchScale(final boolean isSynchScaleEnabled) {
		fIsSynchScaleEnabled = isSynchScaleEnabled;
	}

	private void updateChartAltitude(	final Chart statAltitudeChart,
										final BarChartMinMaxKeeper statAltitudeMinMaxKeeper,
										final int[][] lowValues,
										final int[][] highValues,
										final int[][] colorIndex,
										final String unit,
										final String title) {

		final ChartDataModel chartDataModel = new ChartDataModel(ChartDataModel.CHART_TYPE_BAR);

		// set the x-axis
		final ChartDataXSerie xData = new ChartDataXSerie(fStatAltitudeUnits);
		xData.setAxisUnit(ChartDataXSerie.AXIS_UNIT_NUMBER);
		xData.setUnitLabel(UI.UNIT_LABEL_ALTITUDE);
		chartDataModel.setXData(xData);

		// y-axis: altitude
		final ChartDataYSerie yData = new ChartDataYSerie(ChartDataModel.CHART_TYPE_BAR,
				ChartDataYSerie.BAR_LAYOUT_STACKED,
				lowValues,
				highValues);
		yData.setAxisUnit(ChartDataSerie.AXIS_UNIT_NUMBER);
		yData.setUnitLabel(unit);
		yData.setAllValueColors(0);
		yData.setYTitle(title);
		yData.setVisibleMinValue(0);
		chartDataModel.addYData(yData);

		StatisticServices.setDefaultColors(yData, GraphColorProvider.PREF_GRAPH_ALTITUDE);
		StatisticServices.setTourTypeColors(yData, GraphColorProvider.PREF_GRAPH_ALTITUDE, fActiveTourTypeFilter);
		yData.setColorIndex(colorIndex);

		createToolTipProviderAltitude(chartDataModel);

		if (fIsSynchScaleEnabled) {
			statAltitudeMinMaxKeeper.setMinMaxValues(chartDataModel);
		}

		// set grid size
		final IPreferenceStore prefStore = TourbookPlugin.getDefault().getPreferenceStore();
		statAltitudeChart.setGridDistance(prefStore.getInt(ITourbookPreferences.GRAPH_GRID_HORIZONTAL_DISTANCE),
				prefStore.getInt(ITourbookPreferences.GRAPH_GRID_VERTICAL_DISTANCE));

		// show the new data in the chart
		statAltitudeChart.updateChart(chartDataModel, true);
	}

	/**
	 * @param statDistanceChart
	 * @param statDistanceMinMaxKeeper
	 * @param highValues
	 * @param lowValues
	 * @param statDistanceColorIndex
	 * @param unit
	 * @param title
	 * @param valueDivisor
	 */
	private void updateChartDistance(	final Chart statDistanceChart,
										final BarChartMinMaxKeeper statDistanceMinMaxKeeper,
										final int[][] lowValues,
										final int[][] highValues,
										final int[][] colorIndex,
										final String unit,
										final String title,
										final int valueDivisor) {

		final ChartDataModel chartDataModel = new ChartDataModel(ChartDataModel.CHART_TYPE_BAR);

		// set the x-axis
		final ChartDataXSerie xData = new ChartDataXSerie(fStatDistanceUnits);
		xData.setAxisUnit(ChartDataXSerie.AXIS_UNIT_NUMBER);
		xData.setUnitLabel(UI.UNIT_LABEL_DISTANCE);
		chartDataModel.setXData(xData);

		// y-axis: distance
		final ChartDataYSerie yData = new ChartDataYSerie(ChartDataModel.CHART_TYPE_BAR,
				ChartDataYSerie.BAR_LAYOUT_STACKED,
				lowValues,
				highValues);
		yData.setAxisUnit(ChartDataSerie.AXIS_UNIT_NUMBER);
		yData.setUnitLabel(unit);
		yData.setAllValueColors(0);
		yData.setYTitle(title);
		yData.setVisibleMinValue(0);
		yData.setValueDivisor(valueDivisor);
		chartDataModel.addYData(yData);
		StatisticServices.setDefaultColors(yData, GraphColorProvider.PREF_GRAPH_DISTANCE);
		StatisticServices.setTourTypeColors(yData, GraphColorProvider.PREF_GRAPH_DISTANCE, fActiveTourTypeFilter);
		yData.setColorIndex(colorIndex);

		createToolTipProviderDistance(chartDataModel);

		if (fIsSynchScaleEnabled) {
			statDistanceMinMaxKeeper.setMinMaxValues(chartDataModel);
		}

		// set grid size
		final IPreferenceStore prefStore = TourbookPlugin.getDefault().getPreferenceStore();
		statDistanceChart.setGridDistance(prefStore.getInt(ITourbookPreferences.GRAPH_GRID_HORIZONTAL_DISTANCE),
				prefStore.getInt(ITourbookPreferences.GRAPH_GRID_VERTICAL_DISTANCE));

		// show the new data fDataModel in the chart
		statDistanceChart.updateChart(chartDataModel, true);
	}

	private void updateCharts() {

		updateChartDistance(fChartDistanceCounter,
				fMinMaxKeeperStatDistanceCounter,
				fStatDistanceCounterLow,
				fStatDistanceCounterHigh,
				fStatDistanceCounterColorIndex,
				Messages.NUMBERS_UNIT,
				Messages.LABEL_GRAPH_DISTANCE,
				1);

		updateChartDistance(fChartDistanceSum,
				fMinMaxKeeperStatDistanceSum,
				fStatDistanceSumLow,
				fStatDistanceSumHigh,
				fStatDistanceSumColorIndex,
				UI.UNIT_LABEL_DISTANCE,
				Messages.LABEL_GRAPH_DISTANCE,
				1);

		updateChartAltitude(fChartAltitudeCounter,
				fMinMaxKeeperStatAltitudeCounter,
				fStatAltitudeCounterLow,
				fStatAltitudeCounterHigh,
				fStatAltitudeCounterColorIndex,
				Messages.NUMBERS_UNIT,
				Messages.LABEL_GRAPH_ALTITUDE);

		updateChartAltitude(fChartAltitudeSum,
				fMinMaxKeeperStatAltitudeSum,
				fStatAltitudeSumLow,
				fStatAltitudeSumHigh,
				fStatAltitudeSumColorIndex,
				UI.UNIT_LABEL_ALTITUDE,
				Messages.LABEL_GRAPH_ALTITUDE);

		updateChartTime(fChartDurationCounter,
				fMinMaxKeeperStatDurationCounter,
				fStatTimeCounterLow,
				fStatTimeCounterHigh,
				fStatTimeCounterColorIndex,
				ChartDataXSerie.AXIS_UNIT_NUMBER,
				Messages.NUMBERS_UNIT,
				Messages.LABEL_GRAPH_TIME);

		updateChartTime(fChartDurationSum,
				fMinMaxKeeperStatDurationSum,
				fStatTimeSumLow,
				fStatTimeSumHigh,
				fStatTimeSumColorIndex,
				ChartDataXSerie.AXIS_UNIT_HOUR_MINUTE,
				Messages.LABEL_GRAPH_TIME_UNIT,
				Messages.LABEL_GRAPH_TIME);
	}

	private void updateChartTime(	final Chart statDurationChart,
									final BarChartMinMaxKeeper statDurationMinMaxKeeper,
									final int[][] lowValues,
									final int[][] highValues,
									final int[][] colorIndex,
									final int yUnit,
									final String unit,
									final String title) {

		final ChartDataModel chartDataModel = new ChartDataModel(ChartDataModel.CHART_TYPE_BAR);

		// set the x-axis
		final ChartDataXSerie xData = new ChartDataXSerie(fStatTimeUnits);
		xData.setAxisUnit(ChartDataSerie.AXIS_UNIT_HOUR_MINUTE);
		xData.setUnitLabel(UI.UNIT_LABEL_TIME);
		chartDataModel.setXData(xData);

		// y-axis: altitude
		final ChartDataYSerie yData = new ChartDataYSerie(ChartDataModel.CHART_TYPE_BAR,
				ChartDataYSerie.BAR_LAYOUT_STACKED,
				lowValues,
				highValues);
		yData.setAxisUnit(yUnit);
		yData.setUnitLabel(unit);
		yData.setAllValueColors(0);
		yData.setYTitle(title);
		yData.setVisibleMinValue(0);
		chartDataModel.addYData(yData);

		StatisticServices.setDefaultColors(yData, GraphColorProvider.PREF_GRAPH_TIME);
		StatisticServices.setTourTypeColors(yData, GraphColorProvider.PREF_GRAPH_TIME, fActiveTourTypeFilter);
		yData.setColorIndex(colorIndex);

		createToolTipProviderDuration(chartDataModel);

		if (fIsSynchScaleEnabled) {
			statDurationMinMaxKeeper.setMinMaxValues(chartDataModel);
		}

		// set grid size
		final IPreferenceStore prefStore = TourbookPlugin.getDefault().getPreferenceStore();
		statDurationChart.setGridDistance(prefStore.getInt(ITourbookPreferences.GRAPH_GRID_HORIZONTAL_DISTANCE),
				prefStore.getInt(ITourbookPreferences.GRAPH_GRID_VERTICAL_DISTANCE));

		// show the new data data model in the chart
		statDurationChart.updateChart(chartDataModel, true);
	}

	/**
	 * update the low and high values so they are stacked on each other
	 * 
	 * @param lowValues
	 * @param highValues
	 */
	private void updateLowHighValues(final int[][] lowValues, final int[][] highValues) {

		for (int colorIndex = 0; colorIndex < highValues.length; colorIndex++) {
			if (colorIndex > 0) {
				for (int valueIndex = 0; valueIndex < highValues[0].length; valueIndex++) {

					if (highValues[colorIndex][valueIndex] > 0) {

						final int previousHighValue = highValues[colorIndex - 1][valueIndex];

						highValues[colorIndex][valueIndex] += previousHighValue;
					}
				}
			}
		}
	}

	@Override
	public void updateToolBar(final boolean refreshToolbar) {}
}
