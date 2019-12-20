/*******************************************************************************
 *  Copyright (C) 2019 Frédéric Bard and Contributors
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

import java.time.ZonedDateTime;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.util.Util;
import net.tourbook.data.TourData;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;

public class DialogEditTimeSlicesValues extends TitleAreaDialog {

   private final boolean         _isOSX      = UI.IS_OSX;
   private final boolean         _isLinux    = UI.IS_LINUX;

   private final TourData        _tourData;

   private final IDialogSettings _state;
   private PixelConverter        _pc;

   private int                   _hintDefaultSpinnerWidth;
   private int                   _defaultCheckBoxIndent;

   private boolean               _isUpdateUI = false;

   /*
    * UI controls
    */
   private FormToolkit        _tk;
   private Form               _formContainer;

   private Button             _button_NewValues;
   private Button             _button_OffsetValues;
   private Button             _checkBox_Altitude_NewValue;
   private Button             _checkBox_Altitude_OffsetValue;
   private Button             _checkBox_Pulse_NewValue;
   private Button             _checkBox_Pulse_OffsetValue;
   private Button             _checkBox_Cadence_NewValue;
   private Button             _checkBox_Cadence_OffsetValue;
   private Button             _checkBox_Temperature_NewValue;
   private Button             _checkBox_Temperature_OffsetValue;

   private Combo              _comboNewValue_All;
   private Combo              _comboOffset_All;

   private Spinner            _spinAltitudeValue;
   private Spinner            _spinPulseValue;
   private Spinner            _spinCadenceValue;
   private Spinner            _spinTemperatureValue;

   private MouseWheelListener _mouseWheelListener;


   public DialogEditTimeSlicesValues(final Shell parentShell, final TourData tourData) {

      super(parentShell);

      // make dialog resizable
      setShellStyle(getShellStyle() | SWT.RESIZE);

      setDefaultImage(TourbookPlugin.getImageDescriptor(Messages.Image__quick_edit).createImage());

      _tourData = tourData;

      _state = TourbookPlugin.getDefault().getDialogSettingsSection(getClass().getName());
   }

   @Override
   protected void configureShell(final Shell shell) {

      super.configureShell(shell);

      shell.setText(Messages.Dialog_Edit_Timeslices_Values_Title);

      shell.addDisposeListener(new DisposeListener() {
         @Override
         public void widgetDisposed(final DisposeEvent e) {
            onDispose();
         }
      });
   }

   @Override
   public void create() {

      super.create();

      setTitle(Messages.Dialog_Edit_Timeslices_Values_Area_Title);

      final ZonedDateTime tourStart = _tourData.getTourStartTime();

      setMessage(
            tourStart.format(TimeTools.Formatter_Date_F)
                  + UI.SPACE2
                  + tourStart.format(TimeTools.Formatter_Time_S));
   }

   @Override
   protected final void createButtonsForButtonBar(final Composite parent) {

      super.createButtonsForButtonBar(parent);

      final String okText = net.tourbook.ui.UI.convertOKtoSaveUpdateButton(_tourData);

      getButton(IDialogConstants.OK_ID).setText(okText);
   }

   @Override
   protected Control createDialogArea(final Composite parent) {

      final Composite dlgAreaContainer = (Composite) super.createDialogArea(parent);

      // create ui
      createUI(dlgAreaContainer);

      updateUIFromModel();
      // enableControls();

      return dlgAreaContainer;
   }

   private void createUI(final Composite parent) {

      _pc = new PixelConverter(parent);
      _hintDefaultSpinnerWidth = _isLinux ? SWT.DEFAULT : _pc.convertWidthInCharsToPixels(_isOSX ? 14 : 7);
      _defaultCheckBoxIndent = 90;

      _tk = new FormToolkit(parent.getDisplay());

      _formContainer = _tk.createForm(parent);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(_formContainer);
      _tk.decorateFormHeading(_formContainer);
      _tk.setBorderStyle(SWT.BORDER);

      _mouseWheelListener = new MouseWheelListener() {
         @Override
         public void mouseScrolled(final MouseEvent event) {
            Util.adjustSpinnerValueOnMouseScroll(event);
         }
      };

      final Composite tourContainer = _formContainer.getBody();
      GridLayoutFactory.swtDefaults().applyTo(tourContainer);
      {
         createUI_100_Values(tourContainer);
      }

      final Label label = new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(label);

      tourContainer.layout(true, true);
   }

   private void createUI_100_Values(final Composite parent) {

      final Composite container = _tk.createComposite(parent);
      GridDataFactory.fillDefaults().span(2, 1).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(5).applyTo(container);
      {
         /*
          * Main checkboxes
          */
         {
            _button_NewValues = new Button(container, SWT.CHECK);
            _button_NewValues.setText("New values");
            GridDataFactory.fillDefaults().span(4, 1).indent(_defaultCheckBoxIndent, 0).align(SWT.END, SWT.FILL).applyTo(_button_NewValues);
            _button_NewValues.addSelectionListener(new SelectionListener() {

               @Override
               public void widgetDefaultSelected(final SelectionEvent arg0) {}

               @Override
               public void widgetSelected(final SelectionEvent arg0) {
                  ToggleCheckBoxes(!_button_NewValues.getSelection());

                  if (_button_NewValues.getSelection()) {
                     _button_OffsetValues.setSelection(false);
                  }
               }

            });

            _button_OffsetValues = new Button(container, SWT.CHECK);
            _button_OffsetValues.setText("Offset");
            GridDataFactory.fillDefaults().indent(_defaultCheckBoxIndent, 0).align(SWT.END, SWT.FILL).applyTo(_button_OffsetValues);
            _button_OffsetValues.addSelectionListener(new SelectionListener() {

               @Override
               public void widgetDefaultSelected(final SelectionEvent arg0) {}

               @Override
               public void widgetSelected(final SelectionEvent arg0) {
                  ToggleCheckBoxes(!_button_OffsetValues.getSelection());

                  if (_button_OffsetValues.getSelection()) {
                     _button_NewValues.setSelection(false);
                  }
               }

            });
         }

         /*
          * Altitude
          */
         {
            // label
            Label label = _tk.createLabel(container, Messages.tour_editor_label_wind_speed);
            label.setToolTipText(Messages.tour_editor_label_wind_speed_Tooltip);

            // spinner
            _spinAltitudeValue = new Spinner(container, SWT.BORDER);
            GridDataFactory.fillDefaults()
                  .hint(_hintDefaultSpinnerWidth, SWT.DEFAULT)
                  .align(SWT.BEGINNING, SWT.CENTER)
                  .applyTo(_spinAltitudeValue);
            _spinAltitudeValue.setMinimum(0);
            _spinAltitudeValue.setMaximum(120);
            _spinAltitudeValue.setToolTipText(Messages.tour_editor_label_wind_speed_Tooltip);
            _spinAltitudeValue.addMouseWheelListener(_mouseWheelListener);

            // label: m or ft
            label = _tk.createLabel(container, UI.UNIT_LABEL_ALTITUDE);

            /*
             * checkbox: new value
             */
            _checkBox_Altitude_NewValue = new Button(container, SWT.CHECK);
            GridDataFactory.fillDefaults().align(SWT.CENTER, SWT.FILL).applyTo(_checkBox_Altitude_NewValue);
            _checkBox_Altitude_NewValue.setToolTipText(Messages.Dialog_HRZone_Label_Trash_Tooltip);
            _checkBox_Altitude_NewValue.addSelectionListener(new SelectionListener() {

               @Override
               public void widgetDefaultSelected(final SelectionEvent arg0) {}

               @Override
               public void widgetSelected(final SelectionEvent arg0) {
                  if (_checkBox_Altitude_NewValue.getSelection()) {
                     _checkBox_Altitude_OffsetValue.setSelection(false);
                     _spinAltitudeValue.setSelection(0);
                  } else {
                     _checkBox_Altitude_NewValue.setSelection(true);
                  }
               }
            });

            /*
             * checkbox: offset
             */
            _checkBox_Altitude_OffsetValue = new Button(container, SWT.CHECK);
            GridDataFactory.fillDefaults().align(SWT.CENTER, SWT.FILL).applyTo(_checkBox_Altitude_OffsetValue);
            _checkBox_Altitude_OffsetValue.setToolTipText(Messages.Dialog_HRZone_Label_Trash_Tooltip);
            _checkBox_Altitude_OffsetValue.addSelectionListener(new SelectionListener() {

               @Override
               public void widgetDefaultSelected(final SelectionEvent arg0) {}

               @Override
               public void widgetSelected(final SelectionEvent arg0) {
                  if (_checkBox_Altitude_OffsetValue.getSelection()) {
                     _checkBox_Altitude_NewValue.setSelection(false);
                     _spinAltitudeValue.setSelection(0);
                  } else {
                     _checkBox_Altitude_OffsetValue.setSelection(true);
                  }

               }
            });

         }

         /*
          * Heart rate
          */
         {
            // label
            Label label = _tk.createLabel(container, "heart rate");
            label.setToolTipText(Messages.tour_editor_label_wind_speed_Tooltip);

            // spinner
            _spinPulseValue = new Spinner(container, SWT.BORDER);
            GridDataFactory.fillDefaults()
                  .hint(_hintDefaultSpinnerWidth, SWT.DEFAULT)
                  .align(SWT.BEGINNING, SWT.CENTER)
                  .applyTo(_spinPulseValue);
            _spinPulseValue.setMinimum(0);
            _spinPulseValue.setMaximum(120);
            _spinPulseValue.setToolTipText(Messages.tour_editor_label_wind_speed_Tooltip);
            _spinPulseValue.addMouseWheelListener(_mouseWheelListener);

            // label: bpm
            label = _tk.createLabel(container, "bpm");

            /*
             * checkbox: new value
             */
            _checkBox_Pulse_NewValue = new Button(container, SWT.CHECK);
            GridDataFactory.fillDefaults().align(SWT.CENTER, SWT.FILL).applyTo(_checkBox_Pulse_NewValue);
            _checkBox_Pulse_NewValue.setToolTipText(Messages.Dialog_HRZone_Label_Trash_Tooltip);
            _checkBox_Pulse_NewValue.addSelectionListener(new SelectionListener() {

               @Override
               public void widgetDefaultSelected(final SelectionEvent arg0) {}

               @Override
               public void widgetSelected(final SelectionEvent arg0) {
                  if (_checkBox_Pulse_NewValue.getSelection()) {
                     _checkBox_Pulse_OffsetValue.setSelection(false);
                     _spinPulseValue.setSelection(0);
                  } else {
                     _checkBox_Pulse_NewValue.setSelection(true);
                  }
               }
            });

            /*
             * checkbox: offset
             */
            _checkBox_Pulse_OffsetValue = new Button(container, SWT.CHECK);
            GridDataFactory.fillDefaults().align(SWT.CENTER, SWT.FILL).applyTo(_checkBox_Pulse_OffsetValue);
            _checkBox_Pulse_OffsetValue.setToolTipText(Messages.Dialog_HRZone_Label_Trash_Tooltip);
            _checkBox_Pulse_OffsetValue.addSelectionListener(new SelectionListener() {

               @Override
               public void widgetDefaultSelected(final SelectionEvent arg0) {}

               @Override
               public void widgetSelected(final SelectionEvent arg0) {
                  if (_checkBox_Pulse_OffsetValue.getSelection()) {
                     _checkBox_Pulse_NewValue.setSelection(false);
                     _spinPulseValue.setSelection(0);
                  } else {
                     _checkBox_Pulse_OffsetValue.setSelection(true);
                  }

               }
            });

         }

         /*
          * Cadence
          */
         {
            // label
            Label label = _tk.createLabel(container, "Cadecne");
            label.setToolTipText(Messages.tour_editor_label_wind_speed_Tooltip);

            // spinner
            _spinCadenceValue = new Spinner(container, SWT.BORDER);
            GridDataFactory.fillDefaults()
                  .hint(_hintDefaultSpinnerWidth, SWT.DEFAULT)
                  .align(SWT.BEGINNING, SWT.CENTER)
                  .applyTo(_spinCadenceValue);
            _spinCadenceValue.setMinimum(0);
            //TODO
            _spinCadenceValue.setMaximum(120);
            _spinCadenceValue.addMouseWheelListener(_mouseWheelListener);

            // label: m or ft
            label = _tk.createLabel(container, "spm");

            /*
             * checkbox: new value
             */
            _checkBox_Cadence_NewValue = new Button(container, SWT.CHECK);
            GridDataFactory.fillDefaults().align(SWT.CENTER, SWT.FILL).applyTo(_checkBox_Cadence_NewValue);
            _checkBox_Cadence_NewValue.setToolTipText(Messages.Dialog_HRZone_Label_Trash_Tooltip);
            _checkBox_Cadence_NewValue.addSelectionListener(new SelectionListener() {

               @Override
               public void widgetDefaultSelected(final SelectionEvent arg0) {}

               @Override
               public void widgetSelected(final SelectionEvent arg0) {
                  if (_checkBox_Cadence_NewValue.getSelection()) {
                     _checkBox_Cadence_OffsetValue.setSelection(false);
                     _spinCadenceValue.setSelection(0);
                  } else {
                     _checkBox_Cadence_NewValue.setSelection(true);
                  }
               }
            });

            /*
             * checkbox: offset
             */
            _checkBox_Cadence_OffsetValue = new Button(container, SWT.CHECK);
            GridDataFactory.fillDefaults().align(SWT.CENTER, SWT.FILL).applyTo(_checkBox_Cadence_OffsetValue);
            _checkBox_Cadence_OffsetValue.setToolTipText(Messages.Dialog_HRZone_Label_Trash_Tooltip);
            _checkBox_Cadence_OffsetValue.addSelectionListener(new SelectionListener() {

               @Override
               public void widgetDefaultSelected(final SelectionEvent arg0) {}

               @Override
               public void widgetSelected(final SelectionEvent arg0) {
                  if (_checkBox_Cadence_OffsetValue.getSelection()) {
                     _checkBox_Cadence_NewValue.setSelection(false);
                     _spinCadenceValue.setSelection(0);
                  } else {
                     _checkBox_Cadence_OffsetValue.setSelection(true);
                  }

               }
            });

         }

         /*
          * Temperature
          */
         {
            // label
            Label label = _tk.createLabel(container, "Temperature");
            label.setToolTipText(Messages.tour_editor_label_wind_speed_Tooltip);

            // spinner
            _spinTemperatureValue = new Spinner(container, SWT.BORDER);
            GridDataFactory.fillDefaults()
                  .hint(_hintDefaultSpinnerWidth, SWT.DEFAULT)
                  .align(SWT.BEGINNING, SWT.CENTER)
                  .applyTo(_spinTemperatureValue);
            _spinTemperatureValue.setMinimum(0);
            _spinTemperatureValue.setMaximum(120);
            _spinTemperatureValue.setToolTipText(Messages.tour_editor_label_wind_speed_Tooltip);
            _spinTemperatureValue.addMouseWheelListener(_mouseWheelListener);

            // label: Celsius or Fahrenheit
            label = _tk.createLabel(container, UI.UNIT_LABEL_TEMPERATURE);

            /*
             * checkbox: new value
             */
            _checkBox_Temperature_NewValue = new Button(container, SWT.CHECK);
            GridDataFactory.fillDefaults().align(SWT.CENTER, SWT.FILL).applyTo(_checkBox_Temperature_NewValue);
            _checkBox_Temperature_NewValue.setToolTipText(Messages.Dialog_HRZone_Label_Trash_Tooltip);
            _checkBox_Temperature_NewValue.addSelectionListener(new SelectionListener() {

               @Override
               public void widgetDefaultSelected(final SelectionEvent arg0) {}

               @Override
               public void widgetSelected(final SelectionEvent arg0) {
                  if (_checkBox_Temperature_NewValue.getSelection()) {
                     _checkBox_Temperature_OffsetValue.setSelection(false);
                     _spinTemperatureValue.setSelection(0);
                  } else {
                     _checkBox_Temperature_NewValue.setSelection(true);
                  }
               }
            });

            /*
             * checkbox: offset
             */
            _checkBox_Temperature_OffsetValue = new Button(container, SWT.CHECK);
            GridDataFactory.fillDefaults().align(SWT.CENTER, SWT.FILL).applyTo(_checkBox_Temperature_OffsetValue);
            _checkBox_Temperature_OffsetValue.setToolTipText(Messages.Dialog_HRZone_Label_Trash_Tooltip);
            _checkBox_Temperature_OffsetValue.addSelectionListener(new SelectionListener() {

               @Override
               public void widgetDefaultSelected(final SelectionEvent arg0) {}

               @Override
               public void widgetSelected(final SelectionEvent arg0) {
                  if (_checkBox_Temperature_OffsetValue.getSelection()) {
                     _checkBox_Temperature_NewValue.setSelection(false);
                     _spinTemperatureValue.setSelection(0);
                  } else {
                     _checkBox_Temperature_OffsetValue.setSelection(true);
                  }

               }
            });
         }
      }
   }

   private void enableControls(final SelectionEvent e) {
      //TODO If only one selected, prefill the boxes with the values
      //otherwise set the values to 0
      if (_checkBox_Altitude_NewValue.getSelection()) {
         _checkBox_Altitude_OffsetValue.setSelection(false);
      }
   }

   @Override
   protected IDialogSettings getDialogBoundsSettings() {

      // keep window size and position
      return _state;
//      return null;
   }

   @Override
   protected void okPressed() {

      if (_tourData.isValidForSave() == false) {
         // data are not valid to be saved which is done in the action which opened this dialog
         return;
      }

      TourManager.fireEvent(TourEventId.TOUR_CHANGED, new TourEvent(_tourData));

      super.okPressed();
   }

   private void onDispose() {

      if (_tk != null) {
         _tk.dispose();
      }

   }

   private void ToggleCheckBoxes(final boolean selection) {
      _checkBox_Altitude_NewValue.setEnabled(selection);
      _checkBox_Altitude_OffsetValue.setEnabled(selection);
      _checkBox_Pulse_NewValue.setEnabled(selection);
      _checkBox_Pulse_OffsetValue.setEnabled(selection);
      _checkBox_Cadence_NewValue.setEnabled(selection);
      _checkBox_Cadence_OffsetValue.setEnabled(selection);
      _checkBox_Temperature_NewValue.setEnabled(selection);
      _checkBox_Temperature_OffsetValue.setEnabled(selection);

      if (!selection) {
         _checkBox_Altitude_NewValue.setSelection(selection);
         _checkBox_Altitude_OffsetValue.setSelection(selection);
         _checkBox_Pulse_NewValue.setSelection(selection);
         _checkBox_Pulse_OffsetValue.setSelection(selection);
         _checkBox_Cadence_NewValue.setSelection(selection);
         _checkBox_Cadence_OffsetValue.setSelection(selection);
         _checkBox_Temperature_NewValue.setSelection(selection);
         _checkBox_Temperature_OffsetValue.setSelection(selection);
      }
   }

   private void updateUIFromModel() {

      _isUpdateUI = true;
      {
         //final Tour/event
         // set field content
         //  final Personal details
         final float bodyWeight = UI.convertBodyWeightFromMetric(_tourData.getBodyWeight());
         // wind direction
         final int weatherWindDirDegree = _tourData.getWeatherWindDir() * 10;
      }
      _isUpdateUI = false;

   }
}
