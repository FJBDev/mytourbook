/*******************************************************************************
 * Copyright (C) 2005, 2016 Wolfgang Schramm and Contributors
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
package net.tourbook.common.util;

import java.util.ArrayList;

import net.tourbook.common.UI;

public class ColumnProfile implements Cloneable {

	String						name						= UI.EMPTY_STRING;

	/**
	 * Contains column definitions which are visible in the table/tree in the sort order of the
	 * table/tree.
	 */
	ArrayList<ColumnDefinition>	visibleColumnDefinitions	= new ArrayList<ColumnDefinition>();

	/**
	 * Contains the column ids which are visible in the viewer.
	 */
	String[]					visibleColumnIds;

	/**
	 * Contains a pair with column id/column width for visible columns.
	 */
	String[]					visibleColumnIdsAndWidth;

	private long				_id;
	private static long			_idCreator;

	public ColumnProfile() {
		_id = ++_idCreator;
	}

	@Override
	protected ColumnProfile clone() {

		ColumnProfile clonedObject = null;

		try {

			clonedObject = (ColumnProfile) super.clone();

			clonedObject._id = ++_idCreator;

		} catch (final CloneNotSupportedException e) {
			StatusUtil.log(e);
		}

		return clonedObject;
	}

	@Override
	public String toString() {
		return "ColumnProfile [" //

				+ ("name=" + name + ", ")
				+ ("_id=" + _id)

				+ "]";
	}
}
