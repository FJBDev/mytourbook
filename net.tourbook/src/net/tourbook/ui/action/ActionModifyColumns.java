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
package net.tourbook.ui.action;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.ui.Messages;
import net.tourbook.util.ColumnManager;
import net.tourbook.util.ITourViewer;

import org.eclipse.jface.action.Action;

public class ActionModifyColumns extends Action {

	private ITourViewer	fTourViewer;

	public ActionModifyColumns(final ITourViewer tourViewer) {

		fTourViewer = tourViewer;

		setText(Messages.Action_configure_columns);
		setToolTipText(Messages.Action_configure_columns_tooltip);

		setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image_configure_columns));
	}

	@Override
	public void run() {

		final ColumnManager columnManager = fTourViewer.getColumnManager();
		
		if (columnManager != null) {
			columnManager.openColumnDialog();
		}
	}

}
