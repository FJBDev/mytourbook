/*******************************************************************************
 * Copyright (C) 2005, 2007  Wolfgang Schramm
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
/**
 * 
 */
package net.tourbook.ui.views.tourMap;

import java.util.List;

import javax.persistence.EntityManager;

import net.tourbook.Messages;
import net.tourbook.chart.SelectionChartInfo;
import net.tourbook.data.TourData;
import net.tourbook.data.TourReference;
import net.tourbook.database.TourDatabase;
import net.tourbook.tour.TourChart;
import net.tourbook.tour.TourEditor;

import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;

/**
 *
 */
public class ReferenceTourManager {

	private static ReferenceTourManager	instance	= null;

	private ReferenceTourManager() {}

	public static ReferenceTourManager getInstance() {
		if (instance == null) {
			instance = new ReferenceTourManager();
		}
		return instance;
	}

	/**
	 * persists a new reference tour
	 * 
	 * @param tourEditor
	 */
	public TourReference addReferenceTour(TourEditor tourEditor) {

		// ask for the reference tour name
		InputDialog dialog = new InputDialog(Display.getCurrent().getActiveShell(),
				Messages.TourMap_Dlg_add_reference_tour_title,
				Messages.TourMap_Dlg_add_reference_tour_msg,
				"", //$NON-NLS-1$
				null);

		if (dialog.open() != Window.OK) {
			return null;
		}
		TourChart tourChart = tourEditor.getTourChart();
		SelectionChartInfo chartInfo = tourChart.getChartInfo();
		TourData tourData = tourChart.getTourData();

		// create new tour reference
		TourReference newTourReference = new TourReference(dialog.getValue(),
				tourData,
				chartInfo.leftSliderValuesIndex,
				chartInfo.rightSliderValuesIndex);

		// add the tour reference into the tour data collection
		tourData.getTourReferences().add(newTourReference);

		tourEditor.setTourDirty();

		return newTourReference;
	}

	/**
	 * @return Returns an array with all reference tours
	 */
	public Object[] getReferenceTours() {

		List<?> referenceTours = null;

		EntityManager em = TourDatabase.getInstance().getEntityManager();

		if (em != null) {

			referenceTours = em.createQuery("SELECT refTour \n" //$NON-NLS-1$
					+ ("FROM " + TourDatabase.TABLE_TOUR_REFERENCE + " refTour")).getResultList();

			em.close();
		}

		return referenceTours.toArray();
	}
}
