/*******************************************************************************
 * Copyright (C) 2005, 2025 Wolfgang Schramm and Contributors
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
package net.tourbook.training;

import net.tourbook.Messages;
import net.tourbook.common.action.ActionResetToDefaults;
import net.tourbook.common.action.IActionResetToDefault;
import net.tourbook.common.font.MTFont;
import net.tourbook.common.tooltip.ToolbarSlideout;
import net.tourbook.ui.ChartOptions_Grid;
import net.tourbook.ui.ChartOptions_Layout;

import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ToolBar;

/**
 */
public class SlideoutTrainingOptions extends ToolbarSlideout implements IActionResetToDefault {

   private TrainingView          _trainingView;

   private ActionResetToDefaults _actionRestoreDefaults;
   private ActionEditHrZones     _actionEditHrZones;

   private ChartOptions_Grid     _gridUI;
   private ChartOptions_Layout   _layoutUI;

   /*
    * UI controls
    */

   public SlideoutTrainingOptions(final Control ownerControl,
                                  final ToolBar toolBar,
                                  final String prefStoreGridPrefix,
                                  final String layoutPrefPrefix,
                                  final TrainingView trainingView) {

      super(ownerControl, toolBar);

      _trainingView = trainingView;

      _gridUI = new ChartOptions_Grid(prefStoreGridPrefix);
      _layoutUI = new ChartOptions_Layout(layoutPrefPrefix);
   }

   private void createActions() {

      _actionRestoreDefaults = new ActionResetToDefaults(this);

      _actionEditHrZones = new ActionEditHrZones(_trainingView);
   }

   @Override
   protected Composite createToolTipContentArea(final Composite parent) {

      createActions();

      final Composite ui = createUI(parent);

      restoreState();

      return ui;
   }

   private Composite createUI(final Composite parent) {

      final Composite shellContainer = new Composite(parent, SWT.NONE);
      GridLayoutFactory.swtDefaults().applyTo(shellContainer);
      {
         final Composite container = new Composite(shellContainer, SWT.NONE);
         GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
         GridLayoutFactory.fillDefaults()//
               .numColumns(2)
               .applyTo(container);
//			container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
         {
            createUI_10_Title(container);
            createUI_12_Actions(container);

            _gridUI.createUI(container);
            _layoutUI.createUI(container);
         }
      }

      return shellContainer;
   }

   private void createUI_10_Title(final Composite parent) {

      /*
       * Label: Slideout title
       */
      final Label label = new Label(parent, SWT.NONE);
      GridDataFactory.fillDefaults().applyTo(label);
      label.setText(Messages.Slideout_TrainingOptions_Label_Title);
      MTFont.setBannerFont(label);
   }

   private void createUI_12_Actions(final Composite parent) {

      final ToolBar toolbar = new ToolBar(parent, SWT.FLAT);
      GridDataFactory.fillDefaults()//
            .grab(true, false)
            .align(SWT.END, SWT.BEGINNING)
            .applyTo(toolbar);

      final ToolBarManager tbm = new ToolBarManager(toolbar);

      tbm.add(_actionRestoreDefaults);
      tbm.add(_actionEditHrZones);

      tbm.update(true);
   }

   @Override
   public void resetToDefaults() {

      _gridUI.resetToDefaults();
      _layoutUI.resetToDefaults();
   }

   private void restoreState() {

      _gridUI.restoreState();
      _layoutUI.restoreState();
   }

}
