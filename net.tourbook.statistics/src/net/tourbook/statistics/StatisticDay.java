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

import java.util.Calendar;
import java.util.Formatter;
import java.util.GregorianCalendar;

import net.tourbook.chart.BarChartMinMaxKeeper;
import net.tourbook.chart.Chart;
import net.tourbook.chart.ChartDataModel;
import net.tourbook.chart.ChartSegments;
import net.tourbook.chart.ChartToolTipInfo;
import net.tourbook.chart.IBarSelectionListener;
import net.tourbook.chart.IChartInfoProvider;
import net.tourbook.chart.SelectionBarChart;
import net.tourbook.data.TourPerson;
import net.tourbook.database.TourDatabase;
import net.tourbook.tour.SelectionTourId;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.TourTypeFilter;
import net.tourbook.ui.UI;

import org.eclipse.jface.viewers.IPostSelectionProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPartSite;

public abstract class StatisticDay extends YearStatistic implements IBarSelectionProvider {

	static final String		DISTANCE_DATA	= "distance";						//$NON-NLS-1$
	static final String		ALTITUDE_DATA	= "altitude";						//$NON-NLS-1$
	static final String		DURATION_DATA	= "duration";						//$NON-NLS-1$

	TourTypeFilter			fActiveTourTypeFilter;
	private TourPerson		fActivePerson;

	Long					fSelectedTourId;

	int						fCurrentYear;
	int						fCurrentMonth;
	int						fNumberOfYears;

	Calendar				fCalendar		= GregorianCalendar.getInstance();

	IPostSelectionProvider	fPostSelectionProvider;

	Chart					fChart;
	BarChartMinMaxKeeper	fMinMaxKeeper	= new BarChartMinMaxKeeper();

	private TourDayData		fTourDayData;

	boolean					fIsSynchScaleEnabled;

	@Override
	public void activateActions(IWorkbenchPartSite partSite) {
		fChart.updateChartActionHandlers();
	}

	public boolean canTourBeVisible() {
		return true;
	}

	/**
	 * create segments for the chart
	 */
	ChartSegments createChartSegments(TourDayData tourTimeData) {

		int segmentStart[] = new int[fNumberOfYears];
		int segmentEnd[] = new int[fNumberOfYears];
		String[] segmentTitle = new String[fNumberOfYears];

		int[] allYearDays = tourTimeData.yearDays;
		int oldestYear = fCurrentYear - fNumberOfYears + 1;
		int yearDaysSum = 0;

		// create segments for each year
		for (int yearIndex = 0; yearIndex < allYearDays.length; yearIndex++) {

			int yearDays = allYearDays[yearIndex];

			segmentStart[yearIndex] = yearDaysSum;
			segmentEnd[yearIndex] = yearDaysSum + yearDays - 1;
			segmentTitle[yearIndex] = Integer.toString(oldestYear + yearIndex);

			yearDaysSum += yearDays;
		}

		ChartSegments chartSegments = new ChartSegments();
		chartSegments.valueStart = segmentStart;
		chartSegments.valueEnd = segmentEnd;
		chartSegments.segmentTitle = segmentTitle;

		chartSegments.years = tourTimeData.years;
		chartSegments.yearDays = tourTimeData.yearDays;
		chartSegments.allValues = tourTimeData.allDaysInAllYears;

		return chartSegments;
	}

	@Override
	public void createControl(	final Composite parent,
								IViewSite viewSite,
								final IPostSelectionProvider postSelectionProvider) {

		super.createControl(parent);

		fPostSelectionProvider = postSelectionProvider;

		// create statistic chart
		fChart = new Chart(parent, SWT.BORDER | SWT.FLAT);
		fChart.setShowPartNavigation(true);
		fChart.setShowZoomActions(true);
		fChart.setCanScrollZoomedChart(true);
		fChart.setToolBarManager(viewSite.getActionBars().getToolBarManager(), false);

		fChart.addBarSelectionListener(new IBarSelectionListener() {
			public void selectionChanged(final int serieIndex, final int valueIndex) {
				if (fTourDayData.fTypeIds.length > 0) {
					fSelectedTourId = fTourDayData.fTourIds[valueIndex];
					fPostSelectionProvider.setSelection(new SelectionTourId(fSelectedTourId));
				}
			}
		});

		/*
		 * open tour with double click on the tour bar
		 */
		fChart.addDoubleClickListener(new IBarSelectionListener() {
			public void selectionChanged(final int serieIndex, final int valueIndex) {
				fSelectedTourId = fTourDayData.fTourIds[valueIndex];
				TourManager.getInstance().openTourInEditor(fSelectedTourId);
			}
		});

		/*
		 * open tour with Enter key
		 */
		fChart.addTraverseListener(new TraverseListener() {
			public void keyTraversed(TraverseEvent event) {

				if (event.detail == SWT.TRAVERSE_RETURN) {
					ISelection selection = fChart.getSelection();
					if (selection instanceof SelectionBarChart) {
						SelectionBarChart barChartSelection = (SelectionBarChart) selection;

						if (barChartSelection.serieIndex != -1) {

							long selectedTourId = fTourDayData.fTourIds[barChartSelection.valueIndex];
							TourManager.getInstance().openTourInEditor(selectedTourId);
						}
					}
				}
			}
		});
	}

	private ChartToolTipInfo createToolTipInfo(int valueIndex) {

		final int[] tourDOYValues = fTourDayData.fDOYValues;

		if (valueIndex >= tourDOYValues.length) {
			valueIndex -= tourDOYValues.length;
		}

		if (tourDOYValues == null || valueIndex >= tourDOYValues.length) {
			return null;
		}

		/*
		 * set calendar day/month/year
		 */
		final int oldestYear = fCurrentYear - fNumberOfYears + 1;
		final int tourDOY = tourDOYValues[valueIndex];
		fCalendar.set(oldestYear, 0, 1);
		fCalendar.set(Calendar.DAY_OF_YEAR, tourDOY + 1);

		fCurrentMonth = fCalendar.get(Calendar.MONTH) + 1;
		fSelectedTourId = fTourDayData.fTourIds[valueIndex];

		String tourTypeName = TourDatabase.getTourTypeName(fTourDayData.fTypeIds[valueIndex]);

		final int[] startValue = fTourDayData.fTourStartValues;
		final int[] endValue = fTourDayData.fTourEndValues;

		final Integer recordingTime = fTourDayData.fTourRecordingTimeValues.get(valueIndex);
		final Integer drivingTime = fTourDayData.fTourDrivingTimeValues.get(valueIndex);
		int breakTime = recordingTime - drivingTime;

		StringBuilder infoText = new StringBuilder();
		infoText.append(Messages.tourtime_info_date_format);
		infoText.append(Messages.tourtime_info_distance);
		infoText.append(Messages.tourtime_info_altitude);
		infoText.append(Messages.tourtime_info_recording_time);
		infoText.append(Messages.tourtime_info_driving_time);
		infoText.append(Messages.tourtime_info_break_time);
		infoText.append(Messages.tourtime_info_tour_type);

		final String toolTipLabel = new Formatter().format(infoText.toString(),
		//
				fCalendar.get(Calendar.DAY_OF_MONTH),
				fCalendar.get(Calendar.MONTH) + 1,
				fCalendar.get(Calendar.YEAR),
				startValue[valueIndex] / 3600,
				(startValue[valueIndex] % 3600) / 60,
				endValue[valueIndex] / 3600,
				(endValue[valueIndex] % 3600) / 60,
				//
				fTourDayData.fTourDistanceValues[valueIndex],
				UI.UNIT_LABEL_DISTANCE,
				//
				fTourDayData.fTourAltitudeValues[valueIndex],
				UI.UNIT_LABEL_ALTITUDE,
				//
				recordingTime / 3600,
				(recordingTime % 3600) / 60,
				//
				drivingTime / 3600,
				(drivingTime % 3600) / 60,
				//
				breakTime / 3600,
				(breakTime % 3600) / 60,
				//
				tourTypeName).toString();

		/*
		 * create tool tip info
		 */
		String tourTitle = fTourDayData.fTourTitle.get(valueIndex);
		if (tourTitle == null || tourTitle.trim().length() == 0) {
			tourTitle = tourTypeName;
		}

		ChartToolTipInfo toolTipInfo = new ChartToolTipInfo();
		toolTipInfo.setTitle(tourTitle);
		toolTipInfo.setLabel(toolTipLabel);

		return toolTipInfo;
	}

	@Override
	public void deactivateActions(IWorkbenchPartSite partSite) {}

	public Integer getSelectedMonth() {
		return fCurrentMonth;
	}

	public Long getSelectedTourId() {
		return fSelectedTourId;
	}

	public void prefColorChanged() {
		refreshStatistic(fActivePerson, fActiveTourTypeFilter, fCurrentYear, 1, false);
	}

	public void refreshStatistic(	final TourPerson person,
									final TourTypeFilter tourTypeFilter,
									final int year,
									int numberOfYears,
									final boolean refreshData) {

		fActivePerson = person;
		fActiveTourTypeFilter = tourTypeFilter;
		fCurrentYear = year;
		fNumberOfYears = numberOfYears;

		/*
		 * get currently selected tour id
		 */
		long selectedTourId = -1;
		ISelection selection = fChart.getSelection();
		if (selection instanceof SelectionBarChart) {
			SelectionBarChart barChartSelection = (SelectionBarChart) selection;

			if (barChartSelection.serieIndex != -1) {

				int selectedValueIndex = barChartSelection.valueIndex;
				final long[] tourIds = fTourDayData.fTourIds;

				if (tourIds.length > 0) {

					if (selectedValueIndex >= tourIds.length) {
						selectedValueIndex = tourIds.length - 1;
					}

					selectedTourId = tourIds[selectedValueIndex];
				}
			}
		}

		fTourDayData = DataProviderTourDay.getInstance().getDayData(person,
				tourTypeFilter,
				year,
				numberOfYears,
				isDataDirtyWithReset() || refreshData);

		// reset min/max values
		if (fIsSynchScaleEnabled == false && refreshData) {
			fMinMaxKeeper.resetMinMax();
		}

		updateChart(fTourDayData, selectedTourId);
	}

	@Override
	public void resetSelection() {
		fChart.setSelectedBars(null);
	}

	@Override
	public boolean selectMonth(final Long date) {

		fCalendar.setTimeInMillis(date);
		final int selectedMonth = fCalendar.get(Calendar.MONTH);

		final int[] tourMonths = fTourDayData.fMonthValues;
		final boolean selectedItems[] = new boolean[tourMonths.length];

		boolean isSelected = false;
		// find the tours which have the same month as the selected month
		for (int tourIndex = 0; tourIndex < tourMonths.length; tourIndex++) {
			boolean isMonthSelected = tourMonths[tourIndex] == selectedMonth ? true : false;
			if (isMonthSelected) {
				isSelected = true;
			}
			selectedItems[tourIndex] = isMonthSelected;
		}

		if (isSelected) {
			fChart.setSelectedBars(selectedItems);
		}

		return isSelected;
	}

	@Override
	public boolean selectTour(Long tourId) {

		long[] tourIds = fTourDayData.fTourIds;
		boolean selectedItems[] = new boolean[tourIds.length];
		boolean isSelected = false;

		// find the tour which has the same tourId as the selected tour
		for (int tourIndex = 0; tourIndex < tourIds.length; tourIndex++) {
			boolean isTourSelected = tourIds[tourIndex] == tourId ? true : false;
			if (isTourSelected) {
				isSelected = true;
			}
			selectedItems[tourIndex] = isTourSelected;
		}

		if (isSelected == false) {
			// select first tour
//			selectedItems[0] = true;
		}

		fChart.setSelectedBars(selectedItems);

		return isSelected;
	}

	void setChartProviders(final Chart chartWidget, final ChartDataModel chartModel) {

		// set tool tip info
		chartModel.setCustomData(ChartDataModel.BAR_TOOLTIP_INFO_PROVIDER, new IChartInfoProvider() {
			public ChartToolTipInfo getToolTipInfo(final int serieIndex, int valueIndex) {
				return createToolTipInfo(valueIndex);
			}
		});

		// set the menu context provider
		chartModel.setCustomData(ChartDataModel.BAR_CONTEXT_PROVIDER, new TourContextProvider(fChart, this));
	}

	@Override
	public void setSynchScale(final boolean isSynchScaleEnabled) {
		fIsSynchScaleEnabled = isSynchScaleEnabled;
	}

	/**
	 * @param tourDataDay
	 * @param tourId
	 *        tour id which was selected before the update
	 */
	abstract void updateChart(TourDayData tourDataDay, long tourId);

	@Override
	public void updateToolBar(final boolean refreshToolbar) {
		fChart.fillToolbar(refreshToolbar);
	}
}
