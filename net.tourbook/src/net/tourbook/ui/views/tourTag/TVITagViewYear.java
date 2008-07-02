package net.tourbook.ui.views.tourTag;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;

import net.tourbook.database.TourDatabase;
import net.tourbook.tour.TreeViewerItem;
import net.tourbook.ui.SQLFilter;
import net.tourbook.ui.UI;

public class TVITagViewYear extends TVITagViewItem {

	private final int		fYear;
	private TVITagViewTag	fTagItem;

	private static Calendar	fCalendar	= GregorianCalendar.getInstance();

	/**
	 * <code>true</code> when the children of this year item contains month items<br>
	 * <code>false</code> when the children of this year item contains tour items
	 */
	private boolean			fIsMonth;

	public TVITagViewYear(final TVITagViewTag parentItem, final int year, final boolean isMonth) {
		setParentItem(parentItem);
		fTagItem = parentItem;
		fYear = year;
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

		if (fYear < otherYearItem.fYear) {
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
		if (fYear != other.fYear) {
			return false;
		}

		if (fTagItem == null) {
			if (other.fTagItem != null) {
				return false;
			}
		} else if (!fTagItem.equals(other.fTagItem)) {
			return false;
		}

		return true;
	}

	@Override
	protected void fetchChildren() {

		if (fIsMonth) {
			getChildrenByYearMonth();
		} else {
			getChildrenByYear();
		}
	}

	private void getChildrenByYear() {

		/*
		 * set the children for the root item, these are year items
		 */
		final ArrayList<TreeViewerItem> children = new ArrayList<TreeViewerItem>();
		setChildren(children);

		try {

			/*
			 * get all tours for the tag Id of this tree item
			 */
			final SQLFilter sqlFilter = new SQLFilter();
			final StringBuilder sb = new StringBuilder();

			sb.append("SELECT");

			sb.append(" tourID,"); //							1	
			sb.append(" jTdataTtag2.TourTag_tagId,");//			2
			sb.append(TVITagViewTour.SQL_TOUR_COLUMNS); //		3

			sb.append(" FROM " + TourDatabase.JOINTABLE_TOURDATA__TOURTAG + " jTdataTtag");

			// get all tours for current tag and year/month
			sb.append(" LEFT OUTER JOIN " + TourDatabase.TABLE_TOUR_DATA + " TourData");
			sb.append(" ON jTdataTtag.TourData_tourId=TourData.tourId ");

			// get all tag id's for one tour 
			sb.append(" LEFT OUTER JOIN " + TourDatabase.JOINTABLE_TOURDATA__TOURTAG + " jTdataTtag2");
			sb.append(" ON TourData.tourID = jTdataTtag2.TourData_tourId");

			sb.append(" WHERE jTdataTtag.TourTag_TagId=?");
			sb.append(" AND startYear=?");
			sb.append(sqlFilter.getWhereClause());

			sb.append(" ORDER BY startMonth, startDay, startHour, startMinute");

			final Connection conn = TourDatabase.getInstance().getConnection();

			final PreparedStatement statement = conn.prepareStatement(sb.toString());
			statement.setLong(1, fTagItem.getTagId());
			statement.setInt(2, fYear);
			sqlFilter.setParameters(statement, 3);

			long lastTourId = -1;
			TVITagViewTour tourItem = null;

			final ResultSet result = statement.executeQuery();
			while (result.next()) {

				final long tourId = result.getLong(1);
				final Object resultTagId = result.getObject(2);

				if (tourId == lastTourId) {

					// get tags from outer join

					if (resultTagId instanceof Long) {
						tourItem.tagIds.add((Long) resultTagId);
					}

				} else {

					// new tour is in the resultset
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
	}

	private void getChildrenByYearMonth() {
		/*
		 * set the children for the root item, these are year items
		 */
		final ArrayList<TreeViewerItem> children = new ArrayList<TreeViewerItem>();
		setChildren(children);

		try {

			/*
			 * get all tours for the tag Id of this tree item
			 */
			final SQLFilter sqlFilter = new SQLFilter();
			final StringBuilder sb = new StringBuilder();

			sb.append("SELECT");
			sb.append(" startYear,"); //		// 1
			sb.append(" startMonth,"); //		// 2

			sb.append(SQL_SUM_COLUMNS);

			sb.append(" FROM " + TourDatabase.JOINTABLE_TOURDATA__TOURTAG + " jTdataTtag");

			// get all tours for current tag and year
			sb.append(" LEFT OUTER JOIN " + TourDatabase.TABLE_TOUR_DATA + " TourData");
			sb.append(" ON jTdataTtag.TourData_tourId=TourData.tourId ");

			sb.append(" WHERE jTdataTtag.TourTag_TagId=?");
			sb.append(" AND startYear=?");
			sb.append(sqlFilter.getWhereClause());

			sb.append(" GROUP BY startYear, startMonth");
			sb.append(" ORDER BY startYear");

			final Connection conn = TourDatabase.getInstance().getConnection();

			final PreparedStatement statement = conn.prepareStatement(sb.toString());
			statement.setLong(1, fTagItem.getTagId());
			statement.setInt(2, fYear);
			sqlFilter.setParameters(statement, 3);

			final SimpleDateFormat dfMonth = new SimpleDateFormat("MMM");
//			final DateTime dt = new DateTime(2000, 1, 1, 0, 0, 0, 0);
//			final Date monthDate = new Date();

			final ResultSet result = statement.executeQuery();
			while (result.next()) {

				final int dbYear = result.getInt(1);
				final int dbMonth = result.getInt(2);

				final TVITagViewMonth tourItem = new TVITagViewMonth(this, dbYear, dbMonth);
				children.add(tourItem);

//				dt.withMonthOfYear(dbMonth);
				fCalendar.set(2000, dbMonth - 1, 1);
				tourItem.treeColumn = dfMonth.format(fCalendar.getTime());
//				tourItem.treeColumn = tourItem.treeColumn + "   " + Integer.toString(dbMonth);

				tourItem.readSumColumnData(result, 3);
			}

			conn.close();

		} catch (final SQLException e) {
			UI.showSQLException(e);
		}
	}

	public long getTagId() {
		return fTagItem.tagId;
	}

	public int getYear() {
		return fYear;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (fIsMonth ? 1231 : 1237);
		result = prime * result + ((fTagItem == null) ? 0 : fTagItem.hashCode());
		result = prime * result + fYear;
		return result;
	}

	@Override
	protected void remove() {}

}
