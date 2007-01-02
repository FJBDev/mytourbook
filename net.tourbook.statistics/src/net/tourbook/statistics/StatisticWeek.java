/*******************************************************************************
 * Copyright (C) 2006, 2007  Wolfgang Schramm
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
import java.util.GregorianCalendar;

import net.tourbook.chart.BarChartMinMaxKeeper;
import net.tourbook.chart.Chart;
import net.tourbook.data.TourPerson;
import net.tourbook.ui.ITourChartViewer;

import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

public abstract class StatisticWeek extends YearStatistic {

	Chart					fChart;
	BarChartMinMaxKeeper	fMinMaxKeeper	= new BarChartMinMaxKeeper();

	private TourPerson		fActivePerson;
	private int				fCurrentYear;
	private long			fActiveTypeId;

	private Calendar		fCalendar		= GregorianCalendar.getInstance();
	boolean					fIsSynchScaleEnabled;

	public boolean canTourBeVisible() {
		return false;
	}

	public void createControl(	Composite parent,
								ITourChartViewer tourChartViewer,
								ToolBarManager tbm) {

		super.createControl(parent);

		// create chart
		fChart = new Chart(parent, SWT.BORDER | SWT.FLAT);
		fChart.setToolBarManager(tbm);
		fChart.setShowZoomActions(true);
		fChart.setCanScrollZoomedChart(true);
	}

	public void prefColorChanged() {
		refreshStatistic(fActivePerson, fActiveTypeId, fCurrentYear, false);
	}

	public void refreshStatistic(	TourPerson person,
									long typeId,
									int year,
									boolean refreshData) {

		fActivePerson = person;
		fActiveTypeId = typeId;
		fCurrentYear = year;

		TourDataWeek tourWeekData = ProviderTourWeek.getInstance().getWeekData(
				person,
				typeId,
				year,
				isRefreshDataWithReset() || refreshData);

		// reset min/max values
		if (fIsSynchScaleEnabled == false && refreshData) {
			fMinMaxKeeper.resetMinMax();
		}

		updateChart(tourWeekData);
	}

	public void resetSelection() {
		fChart.setSelectedBars(null);
	}

	public boolean selectDay(Long date) {
		fCalendar.setTimeInMillis(date);
		int selectedWeek = fCalendar.get(Calendar.WEEK_OF_YEAR) - 0;

		boolean selectedItems[] = new boolean[ProviderTourWeek.YEAR_WEEKS];
		selectedItems[selectedWeek] = true;

		fChart.setSelectedBars(selectedItems);

		return true;
	}

	public boolean selectMonth(Long date) {

		fCalendar.setTimeInMillis(date);
		int selectedMonth = fCalendar.get(Calendar.MONTH);

		boolean selectedItems[] = new boolean[ProviderTourWeek.YEAR_WEEKS];
		boolean isSelected = false;
		
		// select all weeks in the selected month
		for (int weekIndex = 0; weekIndex < selectedItems.length; weekIndex++) {
			fCalendar.set(Calendar.WEEK_OF_YEAR, weekIndex + 0);

			boolean isMonthSelected = fCalendar.get(Calendar.MONTH) == selectedMonth
					? true
					: false;
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

	public void setSynchScale(boolean isSynchScaleEnabled) {
		fIsSynchScaleEnabled = isSynchScaleEnabled;
	}

	abstract void updateChart(TourDataWeek tourWeekData);

	public void updateToolBar(boolean refreshToolbar) {
		fChart.showActions(refreshToolbar);
	}
}
