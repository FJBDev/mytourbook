/*******************************************************************************
 * Copyright (C) 2005, 2009  Wolfgang Schramm and Contributors
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
import java.util.ArrayList;

import net.tourbook.common.util.TreeViewerItem;
import net.tourbook.database.TourDatabase;
import net.tourbook.ui.UI;

/**
 * root item for the tag view
 */
public class TVITagViewRoot extends TVITagViewItem {

	private TaggingView	fTagView;
	private int		fTagViewStructure;

	public TVITagViewRoot(final TaggingView tagView, final int tagViewStructure) {
		fTagView = tagView;
		fTagViewStructure = tagViewStructure;
	}

	@Override
	protected void fetchChildren() {

		/*
		 * set the children for the root item, these are year items
		 */
		final ArrayList<TreeViewerItem> children = new ArrayList<TreeViewerItem>();
		setChildren(children);

		final StringBuilder sb = new StringBuilder();

		try {

			final Connection conn = TourDatabase.getInstance().getConnection();
			PreparedStatement statement;
			ResultSet result;

			if (fTagViewStructure == TaggingView.TAG_VIEW_LAYOUT_HIERARCHICAL) {

				/*
				 * get tag categories
				 */
				sb.append("SELECT"); //$NON-NLS-1$
				sb.append(" tagCategoryId,"); // 	1 //$NON-NLS-1$
				sb.append(" name"); // 				2 //$NON-NLS-1$

				sb.append(" FROM " + TourDatabase.TABLE_TOUR_TAG_CATEGORY); //$NON-NLS-1$
				sb.append(" WHERE isRoot = 1"); //$NON-NLS-1$
				sb.append(" ORDER BY name"); //$NON-NLS-1$

				statement = conn.prepareStatement(sb.toString());
				result = statement.executeQuery();

				while (result.next()) {

					final TVITagViewTagCategory treeItem = new TVITagViewTagCategory(this);
					children.add(treeItem);

					treeItem.tagCategoryId = result.getLong(1);
					treeItem.treeColumn = treeItem.name = result.getString(2);
				}
			}

			/*
			 * get tags
			 */
			final String whereClause = fTagViewStructure == TaggingView.TAG_VIEW_LAYOUT_FLAT
					? UI.EMPTY_STRING
					: fTagViewStructure == TaggingView.TAG_VIEW_LAYOUT_HIERARCHICAL ? //
							" WHERE isRoot = 1" //$NON-NLS-1$
							: UI.EMPTY_STRING;

			sb.delete(0, sb.length());
			sb.append("SELECT"); //$NON-NLS-1$
			sb.append(" tagId,"); //		1 //$NON-NLS-1$
			sb.append(" name,"); //			2 //$NON-NLS-1$
			sb.append(" expandType,"); //	3 //$NON-NLS-1$
			sb.append(" isRoot"); //		44 //$NON-NLS-1$

			sb.append(" FROM " + TourDatabase.TABLE_TOUR_TAG); //$NON-NLS-1$
			sb.append(whereClause);
			sb.append(" ORDER BY name"); //$NON-NLS-1$

			statement = conn.prepareStatement(sb.toString());
			result = statement.executeQuery();

			while (result.next()) {

				final TVITagViewTag tagItem = new TVITagViewTag(this);
				children.add(tagItem);

				final long tagId = result.getLong(1);

				tagItem.tagId = tagId;
				tagItem.treeColumn = tagItem.name = result.getString(2);
				tagItem.setExpandType(result.getInt(3));
				tagItem.isRoot = result.getInt(4) == 1;

				readTagTotals(tagItem);
			}

			conn.close();

		} catch (final SQLException e) {
			UI.showSQLException(e);
		}
	}

	public TaggingView getTagView() {
		return fTagView;
	}

	@Override
	protected void remove() {}

}
