/*******************************************************************************
 * Copyright (C) 2005, 2021 Wolfgang Schramm and Contributors
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
package net.tourbook.tour;

import java.util.ArrayList;

import net.tourbook.data.TourData;
import net.tourbook.data.TourMarker;

import org.eclipse.jface.viewers.ISelection;

/**
 * This selection is fired when tour markers are selected.
 */
public class SelectionTourMarker implements ISelection {

   private TourData         _tourData;
   private ArrayList<TourMarker> _selectedTourMarkers;

   /**
    * @param tourData
    * @param selectedTourMarker
    */
   public SelectionTourMarker(final TourData tourData, final ArrayList<TourMarker> selectedTourMarkers) {

      _tourData = tourData;
      _selectedTourMarkers = selectedTourMarkers;
   }

   public ArrayList<TourMarker> getSelectedTourMarkers() {
      return _selectedTourMarkers;
   }

   public TourData getTourData() {
      return _tourData;
   }

   @Override
   public boolean isEmpty() {
      return false;
   }

   @Override
   public String toString() {
      return "SelectionTourMarker [" //$NON-NLS-1$
//				+ ("_tourData=" + _tourData + ", ")
            + ("_selectedTourMarkers=" + _selectedTourMarkers) //$NON-NLS-1$
            +
            //
            "]"; //$NON-NLS-1$
   }

}
