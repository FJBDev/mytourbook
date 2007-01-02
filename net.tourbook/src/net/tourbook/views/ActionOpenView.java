/*******************************************************************************
 * Copyright (C) 2006, 2007  Wolfgang Schramm
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

package net.tourbook.views;

import net.tourbook.plugin.TourbookPlugin;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;


public class ActionOpenView extends Action {

	private final IWorkbenchWindow	window;
	private final String			viewId;


	/**
	 * @param window
	 * @param label
	 * @param toolTip
	 * @param viewId
	 * @param cmdId
	 * @param image
	 */
	public ActionOpenView(IWorkbenchWindow window, String label, String toolTip, String viewId,
			String cmdId, String image)
	{

		this.window = window;
		this.viewId = viewId;

		setText(label);
		setToolTipText(toolTip);
		setImageDescriptor(TourbookPlugin.getImageDescriptor(image));

		// The id is used to refer to the action in a menu or toolbar
		setId(cmdId);

		// Associate the action with a pre-defined command, to allow key
		// bindings.
//		setActionDefinitionId(cmdId);
		
	}


	public void run() {

		if (window != null) {
			try {
				window.getActivePage().showView(viewId, null, IWorkbenchPage.VIEW_ACTIVATE);
			}
			catch (PartInitException e) {
				MessageDialog.openError(window.getShell(), "Error", "Error opening view:"
						+ e.getMessage());
			}
		}
	}
}
