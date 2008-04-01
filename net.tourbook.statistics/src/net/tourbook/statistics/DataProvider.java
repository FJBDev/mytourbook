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
import java.util.GregorianCalendar;

import net.tourbook.data.TourPerson;
import net.tourbook.ui.TourTypeFilter;

public abstract class DataProvider {

	TourPerson		fActivePerson;
	TourTypeFilter	fActiveTourTypeFilter;

	int				fLastYear;
	int				fNumberOfYears;

	/**
	 * number of days in a year
	 */

	int[]			fYearDays;

	/**
	 * years
	 */
	int[]			fYears;
	Calendar		fCalendar	= GregorianCalendar.getInstance();

	/**
	 * @param finalYear
	 * @param numberOfYears
	 * @return Returns a list with all years
	 */
	static String getYearList(final int finalYear, final int numberOfYears) {

		final StringBuffer buffer = new StringBuffer();

		for (int currentYear = finalYear; currentYear >= finalYear - numberOfYears + 1; currentYear--) {

			if (currentYear != finalYear) {
				buffer.append(',');
			}

			buffer.append(Integer.toString(currentYear));
		}

		return buffer.toString();
	}

	String getSQLFilter(TourPerson person, TourTypeFilter tourTypeFilter) {
		return getSQLFilterPerson(person) + tourTypeFilter.getSQLString();
	}

	String getSQLFilterPerson(TourPerson person) {
		return person == null ? "" : " AND tourPerson_personId = " //$NON-NLS-1$ //$NON-NLS-2$
				+ Long.toString(person.getPersonId());
	}

	/**
	 * @param selectedYear
	 * @param numberOfYears
	 * @return Returns the number of days between {@link #fLastYear} and selectedYear
	 */
	int getYearDOYs(final int selectedYear) {

		int yearDOYs = 0;
		int yearIndex = 0;

		for (int currentYear = fLastYear - fNumberOfYears + 1; currentYear < selectedYear; currentYear++) {

			if (currentYear == selectedYear) {
				return yearDOYs;
			}

			yearDOYs += fYearDays[yearIndex];

			yearIndex++;
		}

		return yearDOYs;
	}

	/**
	 * initialize the number of day's in a year for all years
	 */
	void initYearDOYs() {

		fYearDays = new int[fNumberOfYears];
		fYears = new int[fNumberOfYears];
		int yearIndex = 0;

		for (int currentYear = fLastYear - fNumberOfYears + 1; currentYear <= fLastYear; currentYear++) {

			fYears[yearIndex] = currentYear;

			fCalendar.set(currentYear, 11, 31);
			fYearDays[yearIndex] = fCalendar.get(Calendar.DAY_OF_YEAR);

			yearIndex++;
		}
	}
}
