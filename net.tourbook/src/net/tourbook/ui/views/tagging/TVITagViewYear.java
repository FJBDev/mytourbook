/*******************************************************************************
 * Copyright (C) 2005, 2013  Wolfgang Schramm and Contributors
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
package net.tourbook.ui.views.tagging;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;

import net.tourbook.common.util.TreeViewerItem;
import net.tourbook.database.TourDatabase;
import net.tourbook.ui.SQLFilter;
import net.tourbook.ui.UI;

public class TVITagViewYear extends TVITagViewItem {

	private final int				_year;
	private TVITagViewTag			_tagItem;

	private static Calendar			_calendar		= GregorianCalendar.getInstance();
	private static SimpleDateFormat	_monthFormatter	= new SimpleDateFormat("MMM");		//$NON-NLS-1$

	/**
	 * <code>true</code> when the children of this year item contains month items<br>
	 * <code>false</code> when the children of this year item contains tour items
	 */
	private boolean					fIsMonth;

	public TVITagViewYear(final TVITagViewTag parentItem, final int year, final boolean isMonth) {
		
		setParentItem(parentItem);
		
		_tagItem = parentItem;
		_year = year;
		fIsMonth = isMonth;
	}

	/**
	 * Compare two instances of {@link TVITagViewYear}
	 * 
	 * @param otherYearItem
	 * @return
	 */
	public int compareTo(final TVITagViewYear otherYearItem) {

		if (this == otherYearItem) {
			return 0;
		}

		if (_year == otherYearItem._year) {
			return 0;
		} else if (_year < otherYearItem._year) {
			return -1;
		} else {
			return 1;
		}
	}

	@Override
	public boolean equals(final Object obj) {

		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}

		final TVITagViewYear other = (TVITagViewYear) obj;
		if (fIsMonth != other.fIsMonth) {
			return false;
		}
		if (_year != other._year) {
			return false;
		}

		if (_tagItem == null) {
			if (other._tagItem != null) {
				return false;
			}
		} else if (!_tagItem.equals(other._tagItem)) {
			return false;
		}

		return true;
	}

	@Override
	protected void fetchChildren() {

		if (fIsMonth) {
			setChildren(readYearChildrenMonths());
		} else {
			setChildren(readYearChildrenTours());
		}
	}

	public long getTagId() {
		return _tagItem.tagId;
	}

	public TVITagViewTag getTagItem() {
		return _tagItem;
	}

	public int getYear() {
		return _year;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (fIsMonth ? 1231 : 1237);
		result = prime * result + ((_tagItem == null) ? 0 : _tagItem.hashCode());
		result = prime * result + _year;
		return result;
	}

	private ArrayList<TreeViewerItem> readYearChildrenMonths() {

		final ArrayList<TreeViewerItem> children = new ArrayList<TreeViewerItem>();

		try {

			/*
			 * get all tours for the tag Id of this tree item
			 */
			
			final SQLFilter sqlFilter = new SQLFilter();
			final StringBuilder sb = new StringBuilder();

			sb.append("SELECT"); //$NON-NLS-1$
			sb.append(" startYear,"); //		// 1 //$NON-NLS-1$
			sb.append(" startMonth,"); //		// 2 //$NON-NLS-1$

			sb.append(SQL_SUM_COLUMNS);

			sb.append(" FROM " + TourDatabase.JOINTABLE_TOURDATA__TOURTAG + " jTdataTtag"); //$NON-NLS-1$ //$NON-NLS-2$

			// get all tours for current tag and year
			sb.append(" LEFT OUTER JOIN " + TourDatabase.TABLE_TOUR_DATA + " TourData"); //$NON-NLS-1$ //$NON-NLS-2$
			sb.append(" ON jTdataTtag.TourData_tourId=TourData.tourId "); //$NON-NLS-1$

			sb.append(" WHERE jTdataTtag.TourTag_TagId=?"); //$NON-NLS-1$
			sb.append(" AND startYear=?"); //$NON-NLS-1$
			sb.append(sqlFilter.getWhereClause());

			sb.append(" GROUP BY startYear, startMonth"); //$NON-NLS-1$
			sb.append(" ORDER BY startYear"); //$NON-NLS-1$

			final Connection conn = TourDatabase.getInstance().getConnection();

			final PreparedStatement statement = conn.prepareStatement(sb.toString());
			statement.setLong(1, _tagItem.getTagId());
			statement.setInt(2, _year);
			sqlFilter.setParameters(statement, 3);

			final ResultSet result = statement.executeQuery();
			while (result.next()) {

				final int dbYear = result.getInt(1);
				final int dbMonth = result.getInt(2);

				final TVITagViewMonth tourItem = new TVITagViewMonth(this, dbYear, dbMonth);
				children.add(tourItem);

				_calendar.set(Calendar.MONTH, dbMonth - 1);
				tourItem.treeColumn = _monthFormatter.format(_calendar.getTime());

				tourItem.readSumColumnData(result, 3);
			}

			conn.close();

		} catch (final SQLException e) {
			UI.showSQLException(e);
		}

		return children;
	}

	private ArrayList<TreeViewerItem> readYearChildrenTours() {

		final ArrayList<TreeViewerItem> children = new ArrayList<TreeViewerItem>();

		try {

			/*
			 * get all tours for the tag Id of this tree item
			 */
			
			final SQLFilter sqlFilter = new SQLFilter();
			final StringBuilder sb = new StringBuilder();

			sb.append("SELECT"); //$NON-NLS-1$

			sb.append(" tourID,"); //							1	 //$NON-NLS-1$
			sb.append(" jTdataTtag2.TourTag_tagId,");//			2 //$NON-NLS-1$
			sb.append(TVITagViewTour.SQL_TOUR_COLUMNS); //		3

			sb.append(" FROM " + TourDatabase.JOINTABLE_TOURDATA__TOURTAG + " jTdataTtag"); //$NON-NLS-1$ //$NON-NLS-2$

			// get all tours for current tag and year/month
			sb.append(" LEFT OUTER JOIN " + TourDatabase.TABLE_TOUR_DATA + " TourData"); //$NON-NLS-1$ //$NON-NLS-2$
			sb.append(" ON jTdataTtag.TourData_tourId=TourData.tourId "); //$NON-NLS-1$

			// get all tag id's for one tour
			sb.append(" LEFT OUTER JOIN " + TourDatabase.JOINTABLE_TOURDATA__TOURTAG + " jTdataTtag2"); //$NON-NLS-1$ //$NON-NLS-2$
			sb.append(" ON TourData.tourID = jTdataTtag2.TourData_tourId"); //$NON-NLS-1$

			sb.append(" WHERE jTdataTtag.TourTag_TagId=?"); //$NON-NLS-1$
			sb.append(" AND startYear=?"); //$NON-NLS-1$
			sb.append(sqlFilter.getWhereClause());

			sb.append(" ORDER BY startMonth, startDay, startHour, startMinute"); //$NON-NLS-1$

			final Connection conn = TourDatabase.getInstance().getConnection();

			final PreparedStatement statement = conn.prepareStatement(sb.toString());
			statement.setLong(1, _tagItem.getTagId());
			statement.setInt(2, _year);
			sqlFilter.setParameters(statement, 3);

			long lastTourId = -1;
			TVITagViewTour tourItem = null;

			final ResultSet result = statement.executeQuery();
			while (result.next()) {

				final long tourId = result.getLong(1);
				final Object resultTagId = result.getObject(2);

				if (tourId == lastTourId) {

					// get tags from outer join for the current tour id

					if (resultTagId instanceof Long) {
						tourItem.tagIds.add((Long) resultTagId);
					}

				} else {

					// resultset contains a new tour
					
					tourItem = new TVITagViewTour(this);

					children.add(tourItem);

					tourItem.tourId = tourId;
					tourItem.getTourColumnData(result, resultTagId, 3);

					tourItem.treeColumn = UI.DateFormatterShort.format(tourItem.tourDate.toDate());
				}

				lastTourId = tourId;
			}
			conn.close();

		} catch (final SQLException e) {
			UI.showSQLException(e);
		}

		return children;
	}
}
