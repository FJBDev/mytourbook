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
package net.tourbook.ui.views.calendar;

/**
 * These are the calendar profile default default id's except the {@link #USER_ID} value.
 */
public enum DefaultId {

	DEFAULT, //

	COMPACT, //
	COMPACT_II, //
	COMPACT_III, //

	YEAR, //
	YEAR_II, //
	YEAR_III, //

	CLASSIC, //

	/**
	 * This is a special id, this is a user parent id
	 */
	USER_ID,

	/**
	 * This is a special id which is used as default when loading xml config data to identify
	 * invalid id's
	 */
	XML_DEFAULT,
}
