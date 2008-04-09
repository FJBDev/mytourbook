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

package net.tourbook.statistics;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Formatter;
import java.util.GregorianCalendar;

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
import net.tourbook.ui.TourTypeFilter;
import net.tourbook.ui.UI;

import org.eclipse.jface.viewers.IPostSelectionProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPartSite;

public abstract class StatisticWeek extends YearStatistic {

	Chart					fChart;
	BarChartMinMaxKeeper	fMinMaxKeeper	= new BarChartMinMaxKeeper();

	private TourPerson		fActivePerson;
	TourTypeFilter			fActiveTourTypeFilter;

	int						fCurrentYear;
	int						fNumberOfYears;

	boolean					fIsSynchScaleEnabled;

	private final Calendar	fCalendar		= GregorianCalendar.getInstance();
	private DateFormat		fDateFormatter	= DateFormat.getDateInstance(DateFormat.SHORT);

	TourDataWeek			fTourWeekData;

	@Override
	public void activateActions(IWorkbenchPartSite partSite) {
		fChart.updateChartActionHandlers();
	}

	public boolean canTourBeVisible() {
		return false;
	}

	abstract ChartDataModel createChartDataModel();

	/**
	 * create segments for each week
	 */
	ChartSegments createChartSegments() {

		int segmentStart[] = new int[fNumberOfYears];
		int segmentEnd[] = new int[fNumberOfYears];
		String[] segmentTitle = new String[fNumberOfYears];

		int oldestYear = fCurrentYear - fNumberOfYears + 1;
		int[] yearWeeks = fTourWeekData.fYearWeeks;

		int weekCounter = 0;
		int yearIndex = 0;

		// get start/end and title for each segment
		for (int weeks : yearWeeks) {

			segmentStart[yearIndex] = weekCounter;
			segmentEnd[yearIndex] = weekCounter + weeks - 1;

			segmentTitle[yearIndex] = Integer.toString(oldestYear + yearIndex);

			weekCounter += weeks;
			yearIndex++;
		}

		ChartSegments weekSegments = new ChartSegments();
		weekSegments.valueStart = segmentStart;
		weekSegments.valueEnd = segmentEnd;
		weekSegments.segmentTitle = segmentTitle;

		weekSegments.years = fTourWeekData.fYears;
		weekSegments.yearWeeks = yearWeeks;
		weekSegments.yearDays = fTourWeekData.fYearDays;

		return weekSegments;
	}

	@Override
	public void createControl(Composite parent, IViewSite viewSite, final IPostSelectionProvider postSelectionProvider) {

		super.createControl(parent);

		// create statistic chart
		fChart = new Chart(parent, SWT.BORDER | SWT.FLAT);
		fChart.setShowZoomActions(true);
		fChart.setCanScrollZoomedChart(true);
		fChart.setToolBarManager(viewSite.getActionBars().getToolBarManager(), false);
	}

	private ChartToolTipInfo createToolTipInfo(int serieIndex, int valueIndex) {

		int oldestYear = fCurrentYear - fNumberOfYears + 1;

		Calendar calendar = GregorianCalendar.getInstance();

		calendar.set(Calendar.YEAR, oldestYear);
		calendar.set(Calendar.WEEK_OF_YEAR, valueIndex + 1);

		final int weekYear = calendar.get(Calendar.YEAR);
		final int weekWeekOfYear = calendar.get(Calendar.WEEK_OF_YEAR);

		String beginDate = fDateFormatter.format(calendar.getTime());

		calendar.add(Calendar.DAY_OF_MONTH, 6);
		String endDate = fDateFormatter.format(calendar.getTime());

		final Integer recordingTime = fTourWeekData.fRecordingTime[serieIndex][valueIndex];
		final Integer drivingTime = fTourWeekData.fDrivingTime[serieIndex][valueIndex];
		int breakTime = recordingTime - drivingTime;

		/*
		 * tool tip: title
		 */
		StringBuilder titleString = new StringBuilder();

		String tourTypeName = getTourTypeName(serieIndex, fActiveTourTypeFilter);
		if (tourTypeName != null && tourTypeName.length() > 0) {
			titleString.append(tourTypeName);
			titleString.append(NEW_LINE);
		}

		final String toolTipTitle = new Formatter().format(titleString.toString()).toString();

		/*
		 * tool tip: label
		 */
		StringBuilder toolTipFormat = new StringBuilder();
		toolTipFormat.append("Week:\t\t%d / %d");
		toolTipFormat.append(NEW_LINE);
		toolTipFormat.append("Date:\t\t%s - %s");
		toolTipFormat.append(NEW_LINE);
		toolTipFormat.append(NEW_LINE);
		toolTipFormat.append(Messages.tourtime_info_distance);
		toolTipFormat.append(NEW_LINE);
		toolTipFormat.append(Messages.tourtime_info_altitude);
		toolTipFormat.append(NEW_LINE);
		toolTipFormat.append(NEW_LINE);
		toolTipFormat.append(Messages.tourtime_info_recording_time);
		toolTipFormat.append(NEW_LINE);
		toolTipFormat.append(Messages.tourtime_info_driving_time);
		toolTipFormat.append(NEW_LINE);
		toolTipFormat.append(Messages.tourtime_info_break_time);

		final String toolTipLabel = new Formatter().format(toolTipFormat.toString(), //
				//
				weekWeekOfYear,
				weekYear,
				//
				beginDate,
				endDate,
				//
				fTourWeekData.fDistanceHigh[serieIndex][valueIndex],
				UI.UNIT_LABEL_DISTANCE,
				//
				fTourWeekData.fAltitudeHigh[serieIndex][valueIndex],
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

		ChartToolTipInfo toolTipInfo = new ChartToolTipInfo();
		toolTipInfo.setTitle(toolTipTitle);
		toolTipInfo.setLabel(toolTipLabel);
//		toolTipInfo.setLabel(toolTipFormat.toString());

		return toolTipInfo;
	}

//	private void debugWeekNumber() {
//
//		Calendar calendar1 = GregorianCalendar.getInstance();
//		Calendar calendar2 = GregorianCalendar.getInstance();
//		Calendar calendar3 = GregorianCalendar.getInstance();
//
//		final int firstYear = 2000;
//
//		calendar2.set(firstYear, 0, 1);
//		calendar3.set(firstYear, 11, 26);
//
//		for (int currentYear = firstYear; currentYear <= 2010; currentYear++) {
//
//			calendar1.set(Calendar.YEAR, currentYear);
//			calendar1.set(Calendar.WEEK_OF_YEAR, 1);
//			printDayAndWeek(calendar1);
//			System.out.print("\t");
//
//			printDayAndWeek(calendar2);
//			System.out.print("\t");
//			System.out.print("\t");
//
//			printDayAndWeek(calendar3);
//
//			System.out.println();
//
//			calendar2.add(Calendar.MONTH, 12);
//			calendar3.add(Calendar.MONTH, 12);
//
//		}
//
//		System.out.println();
//	}
//
//	private void printDayAndWeek(Calendar calendar2) {
//		System.out.print(calendar2.get(Calendar.DAY_OF_MONTH)
//				+ "."
//				+ (calendar2.get(Calendar.MONTH) + 1)
//				+ "."
//				+ calendar2.get(Calendar.YEAR)
//				+ " - "
//				+ calendar2.get(Calendar.WEEK_OF_YEAR)
//		//
//		);
//	}

	private int[] createWeekData() {

		int weekCounter = fTourWeekData.fAltitudeHigh[0].length;
		int allWeeks[] = new int[weekCounter];

		for (int weekIndex = 0; weekIndex < weekCounter; weekIndex++) {
			allWeeks[weekIndex] = weekIndex;
		}

//		debugWeekNumber();

		return allWeeks;
	}

	void createXDataWeek(ChartDataModel chartDataModel) {

		// set the x-axis
		ChartDataXSerie xData = new ChartDataXSerie(createWeekData());
		xData.setAxisUnit(ChartDataSerie.X_AXIS_UNIT_WEEK);
		xData.setChartSegments(createChartSegments());

		chartDataModel.setXData(xData);
	}

	void createYDataAltitude(ChartDataModel chartDataModel) {

		// altitude
		ChartDataYSerie yData = new ChartDataYSerie(ChartDataModel.CHART_TYPE_BAR,
				ChartDataYSerie.BAR_LAYOUT_STACKED,
				fTourWeekData.fAltitudeLow,
				fTourWeekData.fAltitudeHigh);
		yData.setYTitle(Messages.LABEL_GRAPH_ALTITUDE);
		yData.setUnitLabel(UI.UNIT_LABEL_ALTITUDE);
		yData.setAxisUnit(ChartDataSerie.AXIS_UNIT_NUMBER);
		yData.setAllValueColors(0);
		yData.setVisibleMinValue(0);
		StatisticServices.setTourTypeColors(yData, GraphColorProvider.PREF_GRAPH_ALTITUDE, fActiveTourTypeFilter);
		StatisticServices.setTourTypeColorIndex(yData, fTourWeekData.fTypeIds, fActiveTourTypeFilter);
		StatisticServices.setDefaultColors(yData, GraphColorProvider.PREF_GRAPH_ALTITUDE);

		chartDataModel.addYData(yData);
	}

	void createYDataDistance(ChartDataModel chartDataModel) {

		// distance
		ChartDataYSerie yData = new ChartDataYSerie(ChartDataModel.CHART_TYPE_BAR,
				ChartDataYSerie.BAR_LAYOUT_STACKED,
				fTourWeekData.fDistanceLow,
				fTourWeekData.fDistanceHigh);
		yData.setYTitle(Messages.LABEL_GRAPH_DISTANCE);
		yData.setUnitLabel(UI.UNIT_LABEL_DISTANCE);
		yData.setAxisUnit(ChartDataSerie.AXIS_UNIT_NUMBER);
		yData.setAllValueColors(0);
		yData.setVisibleMinValue(0);
		StatisticServices.setTourTypeColors(yData, GraphColorProvider.PREF_GRAPH_DISTANCE, fActiveTourTypeFilter);
		StatisticServices.setTourTypeColorIndex(yData, fTourWeekData.fTypeIds, fActiveTourTypeFilter);
		StatisticServices.setDefaultColors(yData, GraphColorProvider.PREF_GRAPH_DISTANCE);

		chartDataModel.addYData(yData);
	}

	void createYDataDuration(ChartDataModel chartDataModel) {

		// duration
		ChartDataYSerie yData = new ChartDataYSerie(ChartDataModel.CHART_TYPE_BAR,
				ChartDataYSerie.BAR_LAYOUT_STACKED,
				fTourWeekData.fTimeLow,
				fTourWeekData.fTimeHigh);
		yData.setYTitle(Messages.LABEL_GRAPH_TIME);
		yData.setUnitLabel(Messages.LABEL_GRAPH_TIME_UNIT);
		yData.setAxisUnit(ChartDataSerie.AXIS_UNIT_HOUR_MINUTE);
		yData.setAllValueColors(0);
		yData.setVisibleMinValue(0);
		StatisticServices.setTourTypeColors(yData, GraphColorProvider.PREF_GRAPH_TIME, fActiveTourTypeFilter);
		StatisticServices.setTourTypeColorIndex(yData, fTourWeekData.fTypeIds, fActiveTourTypeFilter);
		StatisticServices.setDefaultColors(yData, GraphColorProvider.PREF_GRAPH_TIME);

		chartDataModel.addYData(yData);
	}

	@Override
	public void deactivateActions(IWorkbenchPartSite partSite) {}

	public void prefColorChanged() {
		refreshStatistic(fActivePerson, fActiveTourTypeFilter, fCurrentYear, fNumberOfYears, false);
	}

	public void refreshStatistic(	TourPerson person,
									TourTypeFilter typeId,
									int currentYear,
									int numberOfYears,
									boolean refreshData) {

		fActivePerson = person;
		fActiveTourTypeFilter = typeId;
		fCurrentYear = currentYear;
		fNumberOfYears = numberOfYears;

		fTourWeekData = DataProviderTourWeek.getInstance().getWeekData(person,
				typeId,
				currentYear,
				numberOfYears,
				isDataDirtyWithReset() || refreshData);

		// reset min/max values
		if (fIsSynchScaleEnabled == false && refreshData) {
			fMinMaxKeeper.resetMinMax();
		}

		ChartDataModel chartDataModel = createChartDataModel();

		setChartProviders(fChart, chartDataModel);

		if (fIsSynchScaleEnabled) {
			fMinMaxKeeper.setMinMaxValues(chartDataModel);
		}

		fChart.updateChart(chartDataModel);
	}

	@Override
	public void resetSelection() {
		fChart.setSelectedBars(null);
	}

	@Override
	public boolean selectDay(Long date) {
		fCalendar.setTimeInMillis(date);
		int selectedWeek = fCalendar.get(Calendar.WEEK_OF_YEAR) - 0;

		boolean selectedItems[] = new boolean[53];
		selectedItems[selectedWeek] = true;

		fChart.setSelectedBars(selectedItems);

		return true;
	}

	@Override
	public boolean selectMonth(Long date) {

		fCalendar.setTimeInMillis(date);
		int selectedMonth = fCalendar.get(Calendar.MONTH);

		boolean selectedItems[] = new boolean[53];
		boolean isSelected = false;

		// select all weeks in the selected month
		for (int weekIndex = 0; weekIndex < selectedItems.length; weekIndex++) {
			fCalendar.set(Calendar.WEEK_OF_YEAR, weekIndex + 0);

			boolean isMonthSelected = fCalendar.get(Calendar.MONTH) == selectedMonth ? true : false;
			if (isMonthSelected) {
				isSelected = true;
			}
			selectedItems[weekIndex] = isMonthSelected;
		}

		if (isSelected) {
			fChart.setSelectedBars(selectedItems);
		}

		return isSelected;
	}

	void setChartProviders(final Chart chartWidget, final ChartDataModel chartModel) {

		// set tool tip info
		chartModel.setCustomData(ChartDataModel.BAR_TOOLTIP_INFO_PROVIDER, new IChartInfoProvider() {
			public ChartToolTipInfo getToolTipInfo(final int serieIndex, int valueIndex) {
				return createToolTipInfo(serieIndex, valueIndex);
			}
		});
	}

	@Override
	public void setSynchScale(boolean isSynchScaleEnabled) {
		fIsSynchScaleEnabled = isSynchScaleEnabled;
	}

	@Override
	public void updateToolBar(final boolean refreshToolbar) {
		fChart.fillToolbar(refreshToolbar);
	}
}
