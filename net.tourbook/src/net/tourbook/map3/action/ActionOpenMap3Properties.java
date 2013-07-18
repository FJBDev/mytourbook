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
package net.tourbook.map3.action;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.util.Util;
import net.tourbook.map3.Messages;
import net.tourbook.map3.view.Map3LayerView;

import org.eclipse.jface.action.Action;

public class ActionOpenMap3Properties extends Action {

	public ActionOpenMap3Properties() {

		setText(Messages.Map3_Action_OpenMap3PropertiesView);
		setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image_Map3_Map3PropertiesView));
	}

	@Override
	public void run() {
		Util.showView(Map3LayerView.ID, true);
	}

}
