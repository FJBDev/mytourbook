/*******************************************************************************
 * Copyright (C) 2005, 2017 Wolfgang Schramm and Contributors
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
package net.tourbook.tour.filter;

public enum TourFilterFieldOperator {

	STARTS_WITH, //
	ENDS_WITH, //

	LIKE, //
	NOT_LIKE, //

	EQUALS, //
	NOT_EQUALS, //

	LESS_THAN, //
	LESS_THAN_OR_EQUAL, //

	GREATER_THAN, //
	GREATER_THAN_OR_EQUAL, //

	BETWEEN, //
	NOT_BETWEEN, //

	INCLUDE_ANY, //
	EXCLUDE_ALL, //

	IS_EMPTY, //
	IS_NOT_EMPTY,

	SEASON_CURRENT_MONTH, //
	SEASON_CURRENT_DAY, //
	SEASON_MONTH, //
	SEASON_TODAY_UNTIL_YEAR_END, //
	SEASON_TODAY_UNTIL_DATE, //
	SEASON_UNTIL_TODAY_FROM_YEAR_START, //
	SEASON_UNTIL_TODAY_FROM_DATE, //

}
