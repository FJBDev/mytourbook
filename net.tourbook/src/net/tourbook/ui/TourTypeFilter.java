package net.tourbook.ui;

import net.tourbook.data.TourType;

/**
 * Contains a filter for tour types
 */
public class TourTypeFilter {

	/**
	 * this is a system filter like all tour types or no tour types
	 */
	public static final int		FILTER_TYPE_SYSTEM				= 1;

	/**
	 * filter contains one tour type which is stored in the database, the tour type can be fetched
	 * with {@link #getTourType()}
	 */
	public static final int		FILTER_TYPE_DB					= 2;

	/**
	 * filter contains several tour types
	 */
	public static final int		FILTER_TYPE_TOURTYPE_SET		= 3;

	public static final int		SYSTEM_FILTER_ID_ALL			= 1;
	public static final int		SYSTEM_FILTER_ID_NOT_DEFINED	= 2;

	private int					fFilterType;

	private String				fSystemFilterName;
	private int					fSystemFilterId;

	/**
	 * contains the tour type from the database when {@link TourTypeFilter#getFilterType()} is
	 * {@link TourTypeFilter#FILTER_TYPE_DB}
	 */
	private TourType			fTourType;

	private TourTypeFilterSet	fTourTypeSet;

	/**
	 * Create a filter with type {@link #FILTER_TYPE_SYSTEM}
	 * 
	 * @param tourType
	 */
	public TourTypeFilter(int systemFilterId, String filterName) {

		fFilterType = FILTER_TYPE_SYSTEM;

		fSystemFilterName = filterName;
		fSystemFilterId = systemFilterId;
	}

	/**
	 * Create a filter with type {@link #FILTER_TYPE_DB}
	 * 
	 * @param tourType
	 */
	public TourTypeFilter(TourType tourType) {
		fFilterType = FILTER_TYPE_DB;
		fTourType = tourType;
	}

	/**
	 * Create a filter with type {@link #FILTER_TYPE_TOURTYPE_SET}
	 * 
	 * @param filterSet
	 */
	public TourTypeFilter(TourTypeFilterSet filterSet) {
		fFilterType = FILTER_TYPE_TOURTYPE_SET;
		fTourTypeSet = filterSet;
	}

	public String getFilterName() {
		switch (fFilterType) {
		case FILTER_TYPE_SYSTEM:
			return fSystemFilterName;
		case FILTER_TYPE_DB:
			return fTourType.getName();
		case FILTER_TYPE_TOURTYPE_SET:
			return fTourTypeSet.getName();
		default:
			break;
		}
		return "?";
	}

	/**
	 * @return Returns the filter type of this filter which is one of
	 *         {@link TourTypeFilter#FILTER_TYPE_*}
	 */
	public int getFilterType() {
		return fFilterType;
	}

	public int getSystemFilterId() {
		return fSystemFilterId;
	}

	public String getSystemFilterName() {
		return fSystemFilterName;
	}

	/**
	 * @return Returns the tour type from the database when {@link TourTypeFilter#getFilterType()}
	 *         is {@link TourTypeFilter#FILTER_TYPE_DB}
	 */
	public TourType getTourType() {
		return fTourType;
	}

	/**
	 * @return Returns the filterset when the filter type {@link TourTypeFilter#getFilterType()}
	 *         returns {@link TourTypeFilter#FILTER_TYPE_TOURTYPE_SET}
	 */
	public TourTypeFilterSet getTourTypeSet() {
		return fTourTypeSet;
	}

	public void setName(String filterName) {
		switch (fFilterType) {
		case FILTER_TYPE_SYSTEM:
			// not supported
			break;

		case FILTER_TYPE_DB:
			// not supported
			break;

		case FILTER_TYPE_TOURTYPE_SET:
			fTourTypeSet.setName(filterName);
			break;

		default:
			break;
		}
	}

	/**
	 * @return Returns a sql string to select the tour types in the database
	 */
	public String getSQLString() {

		String sqlString = null;

		switch (fFilterType) {
		case FILTER_TYPE_SYSTEM:
			if (fSystemFilterId == SYSTEM_FILTER_ID_ALL) {
				// select all tour types
				sqlString = "";
			} else {
				// select tour types which are not defined
				sqlString = " AND tourType_typeId is null";
			}
			break;

		case FILTER_TYPE_DB:

			sqlString = " AND tourType_typeId =" + Long.toString(fTourType.getTypeId());
			break;

		case FILTER_TYPE_TOURTYPE_SET:

			final Object[] tourTypes = fTourTypeSet.getTourTypes();

			if (tourTypes.length == 0) {
				// select all tour types
				sqlString = "";
			} else {

				int itemIndex = 0;
				String filter = "";

				for (Object item : tourTypes) {

					if (itemIndex > 0) {
						filter += " OR ";
					}

					filter += " tourType_typeId =" + Long.toString(((TourType) item).getTypeId());

					itemIndex++;
				}
				sqlString = " AND (" + filter + ") \n";
			}

			break;

		default:
			break;
		}
		return sqlString;
	}
}
