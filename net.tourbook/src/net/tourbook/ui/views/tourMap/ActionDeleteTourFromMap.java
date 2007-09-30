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
package net.tourbook.ui.views.tourMap;

import java.util.ArrayList;
import java.util.Iterator;

import javax.persistence.EntityManager;

import net.tourbook.Messages;
import net.tourbook.data.TourData;
import net.tourbook.data.TourReference;
import net.tourbook.database.TourDatabase;
import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.tour.TreeViewerItem;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.window.Window;

public class ActionDeleteTourFromMap extends Action {

	private TourMapView	fTourView;

	public ActionDeleteTourFromMap(TourMapView view) {

		fTourView = view;

		setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__delete));
		setDisabledImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__delete_disabled));

		setText(Messages.Tour_Map_Action_delete_tours);

		setEnabled(false);

	}

	@Override
	public void run() {

		TreeViewer tourViewer = fTourView.getTourViewer();

		SelectionRemovedComparedTours selectionRemovedTours = new SelectionRemovedComparedTours();

		// get selected reference tours
		IStructuredSelection selection = (IStructuredSelection) tourViewer.getSelection();

		Object item = selection.getFirstElement();

		boolean isDeleted = false;
		if (item instanceof TourMapItemReferenceTour) {

			// delete the reference tours and it's children
			isDeleted = deleteRefTours(selection.iterator(), selectionRemovedTours);

		} else if (item instanceof TourMapItemComparedTour) {

			// delete compared tours
			isDeleted = deleteComparedTours(selection.iterator(), selectionRemovedTours);
		}

		if (isDeleted) {
			// update the compare result view
			fTourView.fPostSelectionProvider.setSelection(selectionRemovedTours);
		}
	}

	/**
	 * @param selection
	 * @param refCompTourSelection
	 * @return Returns <code>true</code> when the tours are deleted
	 */
	private boolean deleteRefTours(	Iterator<TourMapItemReferenceTour> selection,
									SelectionRemovedComparedTours refCompTourSelection) {

		// confirm deletion
		String[] buttons = new String[] { IDialogConstants.OK_LABEL, IDialogConstants.CANCEL_LABEL };

		MessageDialog dialog = new MessageDialog(fTourView.getSite().getShell(),
				Messages.Tour_Map_dlg_delete_refTour_title,
				null,
				Messages.Tour_Map_dlg_delete_refTour_msg,
				MessageDialog.QUESTION,
				buttons,
				1);

		if (dialog.open() != Window.OK) {
			return false;
		}

		TreeViewer tourViewer = fTourView.getTourViewer();

		for (Iterator<TourMapItemReferenceTour> selTour = selection; selTour.hasNext();) {

			TourMapItemReferenceTour refTourItem = selTour.next();

			ArrayList<TreeViewerItem> unfetchedChildren = refTourItem.getUnfetchedChildren();

			// remove all compared tours from the current ref tour
			if (unfetchedChildren != null) {

				// remove all compared tours for the current ref tour from
				// the database
				for (Iterator<TreeViewerItem> compTour = unfetchedChildren.iterator(); selTour.hasNext();) {

					long compId = ((TourMapItemComparedTour) compTour.next()).getCompId();

					TourCompareManager.removeComparedTourFromDb(compId);

					// change selection
					refCompTourSelection.removedComparedTours.add(compId);
				}
			}

			TourData tourData;
			TourReference refTour;

			// get the ref tour from the database
			EntityManager em = TourDatabase.getInstance().getEntityManager();
			refTour = em.find(TourReference.class, refTourItem.refId);

			if (refTour != null) {

				// remove the reference tour from the tour data and persist the
				// tour data
				tourData = refTour.getTourData();

				if (tourData.getTourReferences().remove(refTour)) {
					TourDatabase.saveTour(tourData);
				}

				// remove the ref tour from the fDataModel
				refTourItem.remove();

				// remove the ref tour from the tree
				tourViewer.remove(refTourItem);
			}

			em.close();
		}

		return true;
	}

	/**
	 * @param selection
	 * @param selectionRemovedComparedTours
	 * @return Returns <code>true</code> when the tours are deleted
	 */
	private boolean deleteComparedTours(Iterator<TourMapItemReferenceTour> selection,
										SelectionRemovedComparedTours selectionRemovedComparedTours) {

		// confirm deletion
		String[] buttons = new String[] { IDialogConstants.OK_LABEL, IDialogConstants.CANCEL_LABEL };

		MessageDialog dialog = new MessageDialog(fTourView.getSite().getShell(),
				Messages.Tour_Map_dlg_delete_comparedTour_title,
				null,
				Messages.Tour_Map_dlg_delete_comparedTour_msg,
				MessageDialog.QUESTION,
				buttons,
				1);

		if (dialog.open() != Window.OK) {
			return false;
		}

		TreeViewer tourViewer = fTourView.getTourViewer();

		// loop: selected tours
		for (Iterator<TourMapItemReferenceTour> selTour = selection; selTour.hasNext();) {

			Object tourItem = selTour.next();

			if (tourItem instanceof TourMapItemComparedTour) {

				TourMapItemComparedTour compTourItem = (TourMapItemComparedTour) tourItem;
				long compId = compTourItem.getCompId();

				if (TourCompareManager.removeComparedTourFromDb(compId)) {

					// remove compared tour from the fDataModel
					compTourItem.remove();

					// remove compared tour from the tree viewer
					tourViewer.remove(compTourItem);

					// update selection
					selectionRemovedComparedTours.removedComparedTours.add(compId);
				}
			}
		}

		return true;
	}

}
