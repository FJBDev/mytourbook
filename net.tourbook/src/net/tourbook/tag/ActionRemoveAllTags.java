/*******************************************************************************
 * Copyright (C) 2005, 2008  Wolfgang Schramm and Contributors
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
package net.tourbook.tag;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import net.tourbook.Messages;
import net.tourbook.data.TourData;
import net.tourbook.data.TourTag;
import net.tourbook.tour.TourManager;
import net.tourbook.tour.TourEvent;
import net.tourbook.tour.TourEventId;
import net.tourbook.ui.ITourProvider;

import org.eclipse.jface.action.Action;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Display;

public class ActionRemoveAllTags extends Action {

	private ITourProvider	fTourProvider;
	private boolean			fIsSaveTour;

	/**
	 * Removes all tags and saves the tours
	 * 
	 * @param tourProvider
	 */
	public ActionRemoveAllTags(final ITourProvider tourProvider) {
		this(tourProvider, true);
	}

	/**
	 * Removes all tags
	 * 
	 * @param tourProvider
	 * @param isSaveTour
	 *            when <code>true</code> the tour will be saved and a
	 *            {@link TourManager#TOUR_CHANGED} event is fired, otherwise the
	 *            {@link TourData} from the tour provider is only modified
	 */
	public ActionRemoveAllTags(final ITourProvider tourProvider, final boolean isSaveTour) {

		super(Messages.action_tag_remove_all, AS_PUSH_BUTTON);

		fTourProvider = tourProvider;
		fIsSaveTour = isSaveTour;
	}

	@Override
	public void run() {

		final Runnable runnable = new Runnable() {

			public void run() {

				// get tours which tour type should be changed
				ArrayList<TourData> selectedTours = fTourProvider.getSelectedTours();

				if (selectedTours == null || selectedTours.size() == 0) {
					return;
				}

				final HashMap<Long, TourTag> removedTags = new HashMap<Long, TourTag>();

				// remove tag in all tours (without tours from an editor)
				for (final TourData tourData : selectedTours) {

					// get all tag's which will be removed
					final Set<TourTag> tourTags = tourData.getTourTags();

					for (final TourTag tourTag : tourTags) {
						removedTags.put(tourTag.getTagId(), tourTag);
					}

					// remove all tour tags
					tourTags.clear();
				}

				if (fIsSaveTour) {

					// save all tours with the removed tags
					final ArrayList<TourData> savedTours = TourManager.saveModifiedTours(selectedTours);
					selectedTours = savedTours;

				} else {

					// tours are not saved but the tour provider must be notified

					TourManager.fireEvent(TourEventId.TOUR_CHANGED,
							new TourEvent(selectedTours));
				}

				TourManager.fireEvent(TourEventId.NOTIFY_TAG_VIEW, //
						new ChangedTags(removedTags, selectedTours, false));
			}
		};

		BusyIndicator.showWhile(Display.getCurrent(), runnable);
	}
}
