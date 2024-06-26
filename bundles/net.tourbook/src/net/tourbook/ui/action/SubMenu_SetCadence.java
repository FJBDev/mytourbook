/*******************************************************************************
 * Copyright (C) 2005, 2023 Wolfgang Schramm and Contributors
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

import java.util.ArrayList;

import net.tourbook.Messages;
import net.tourbook.common.ui.SubMenu;
import net.tourbook.data.TourData;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.ITourProvider2;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.swt.widgets.Menu;

/**
 */
public class SubMenu_SetCadence extends SubMenu {

   private ActionSetNone  _actionSetNone;
   private ActionSetRpm   _actionSetRpm;
   private ActionSetSpm   _actionSetSpm;

   private ITourProvider2 _tourProvider;

   private class ActionSetNone extends Action {

      public ActionSetNone() {
         super(Messages.Action_Cadence_Set_None, AS_CHECK_BOX);
      }

      @Override
      public void run() {
         setCadenceMultiplier(0);
      }
   }

   private class ActionSetRpm extends Action {

      public ActionSetRpm() {

         super(Messages.Action_Cadence_Set_Rpm, AS_CHECK_BOX);
      }

      @Override
      public void run() {
         setCadenceMultiplier(1.0f);
      }
   }

   private class ActionSetSpm extends Action {

      public ActionSetSpm() {
         super(Messages.Action_Cadence_Set_Spm, AS_CHECK_BOX);
      }

      @Override
      public void run() {
         setCadenceMultiplier(2.0f);
      }
   }

   public SubMenu_SetCadence(final ITourProvider2 tourProvider) {

      super(Messages.Action_Cadence_Set, AS_DROP_DOWN_MENU);

      _tourProvider = tourProvider;

      _actionSetNone = new ActionSetNone();
      _actionSetRpm = new ActionSetRpm();
      _actionSetSpm = new ActionSetSpm();
   }

   @Override
   public void enableActions() {

      final ArrayList<TourData> selectedTours = _tourProvider.getSelectedTours();

      int numNone = 0;
      int numSpm = 0;
      int numRpm = 0;

      for (final TourData tourData : selectedTours) {

         switch ((int) tourData.getCadenceMultiplier()) {

         case 0:
            numNone++;
            break;
         case 1:
            numRpm++;
            break;
         case 2:
            numSpm++;
            break;
         }
      }

      _actionSetRpm.setChecked(numRpm > 0);
      _actionSetSpm.setChecked(numSpm > 0);
      _actionSetNone.setChecked(numNone > 0);
   }

   @Override
   public void fillMenu(final Menu menu) {

      new ActionContributionItem(_actionSetNone).fill(menu, -1);
      new ActionContributionItem(_actionSetRpm).fill(menu, -1);
      new ActionContributionItem(_actionSetSpm).fill(menu, -1);
   }

   private void setCadenceMultiplier(final float cadenceMultiplier) {

//		 * 1.0f = Revolutions per minute (RPM)
//		 * 2.0f = Steps per minute (SPM)

      final ArrayList<TourData> selectedTours = _tourProvider.getSelectedTours();
      final ArrayList<TourData> modifiedTours = new ArrayList<>();

      for (final TourData tourData : selectedTours) {

         if (tourData.getCadenceMultiplier() != cadenceMultiplier) {

            // cadence multiplier is not the same

            tourData.setCadenceMultiplier(cadenceMultiplier);

            modifiedTours.add(tourData);
         }
      }

      if (modifiedTours.size() > 0) {
         TourManager.saveModifiedTours(modifiedTours);
      }
   }

}
