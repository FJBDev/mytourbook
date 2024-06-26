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

import net.tourbook.Messages;
import net.tourbook.common.ui.SubMenu;
import net.tourbook.ui.ITourProvider2;
import net.tourbook.ui.ITourProviderByID;

import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.swt.widgets.Menu;

public class SubMenu_Elevation extends SubMenu {

   private ActionComputeElevationGain       _action_ComputeElevationGain;
   private ActionSetElevationValuesFromSRTM _action_SetElevationFromSRTM;

   public SubMenu_Elevation(final ITourProvider2 tourProvider, final ITourProviderByID tourProviderById) {

      super(Messages.Tour_SubMenu_Elevation, AS_DROP_DOWN_MENU);

      _action_ComputeElevationGain = new ActionComputeElevationGain(tourProviderById);
      _action_SetElevationFromSRTM = new ActionSetElevationValuesFromSRTM(tourProvider);
   }

   @Override
   public void enableActions() {}

   @Override
   public void fillMenu(final Menu menu) {

      new ActionContributionItem(_action_ComputeElevationGain).fill(menu, -1);
      new ActionContributionItem(_action_SetElevationFromSRTM).fill(menu, -1);
   }
}
