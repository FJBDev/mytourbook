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
package net.tourbook.ui.tourChart.action;

import java.util.ArrayList;


import net.tourbook.ui.tourChart.TourChartConfiguration;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.commands.IElementUpdater;

/**
 * Handler for graphs in a tour chart
 */
class ActionHandlerGraph extends TCActionHandler implements IElementUpdater {

	private int	fGraphId;

	public ActionHandlerGraph(final int graphId, final String commandId) {

		fGraphId = graphId;
		fCommandId = commandId;
	}

	public Object execute(final ExecutionEvent execEvent) throws ExecutionException {

		final TourChartConfiguration chartConfig = fTourChart.getTourChartConfig();
		final ArrayList<Integer> visibleGraphs = chartConfig.getVisibleGraphs();

		final boolean isThisGraphVisible = visibleGraphs.contains(fGraphId);

		// check that at least one graph is visible
		if (isThisGraphVisible && visibleGraphs.size() == 1) {

			// this is a toggle button so the check status must be reset

			TCActionHandlerManager.getInstance().updateUICheckState(fCommandId);

			return null;
		}

		if (!isThisGraphVisible) {
			// add the graph to the visible list
			chartConfig.addVisibleGraph(fGraphId);
		} else {
			// remove the graph from the visible list
			chartConfig.removeVisibleGraph(fGraphId);
		}

		fTourChart.enableTourActions();
		fTourChart.updateTourChart(true);

		return null;
	}

}
