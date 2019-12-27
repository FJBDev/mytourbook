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
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;

public class DialogEditTimeSlicesValues extends TitleAreaDialog {

   private final boolean         _isOSX      = UI.IS_OSX;
   private final boolean         _isLinux    = UI.IS_LINUX;

   private final TourData        _tourData;
   private final int             _selectedIndex;

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

   private Button             _checkBox_NewValues;
   private Button             _checkBox_OffsetValues;

   private Button             _radioButton_Altitude_NewValue;
   private Button             _radioButton_Altitude_OffsetValue;
   private Button             _radioButton_Pulse_NewValue;
   private Button             _radioButton_Pulse_OffsetValue;
   private Button             _radioButton_Cadence_NewValue;
   private Button             _radioButton_Cadence_OffsetValue;
   private Button             _radioButton_Temperature_NewValue;
   private Button             _radioButton_Temperature_OffsetValue;

   private Text               _textAltitudeValue;
   private Text               _textPulseValue;
   private Text               _textCadenceValue;
   private Text               _textTemperatureValue;

   private int                _defaultMinimumSpinnerValue = -10000;
   private int                _defaultMaximumSpinnerValue;

   private MouseWheelListener _mouseWheelListener;

   private float              _newAltitudeValue;
   private boolean            _isAltitudeValueOffset;
   private float              _newPulseValue;
   private boolean            _isPulseValueOffset;
   private float              _newCadenceValue;
   private boolean            _isCadenceValueOffset;
   private float              _newTemperatureValue;
   private boolean            _isTemperatureValueOffset;

   public DialogEditTimeSlicesValues(final Shell parentShell, final TourData tourData, final int selectedIndex) {

      super(parentShell);

      // make dialog resizable
      setShellStyle(getShellStyle() | SWT.RESIZE);

      setDefaultImage(TourbookPlugin.getImageDescriptor(Messages.Image__quick_edit).createImage());

      _tourData = tourData;
      _selectedIndex = selectedIndex;

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
      //   enableControls();

      return dlgAreaContainer;
   }

   private void createUI(final Composite parent) {

      _pc = new PixelConverter(parent);
      _hintDefaultSpinnerWidth = _isLinux ? SWT.DEFAULT : _pc.convertWidthInCharsToPixels(_isOSX ? 14 : 7);
      _defaultCheckBoxIndent = 150;

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
            _checkBox_NewValues = new Button(container, SWT.CHECK);
            _checkBox_NewValues.setText("New values");
            GridDataFactory.fillDefaults().span(4, 1).indent(_defaultCheckBoxIndent, 0).align(SWT.END, SWT.FILL).applyTo(_checkBox_NewValues);
            _checkBox_NewValues.addSelectionListener(new SelectionListener() {

               @Override
               public void widgetDefaultSelected(final SelectionEvent arg0) {}

               @Override
               public void widgetSelected(final SelectionEvent arg0) {

                  //    ToggleCheckBoxes(!_checkBox_NewValues.getSelection());
                  if (_checkBox_NewValues.getSelection()) {
                     _checkBox_OffsetValues.setSelection(false);
                  }

                  enableControls();
               }

            });

            _checkBox_OffsetValues = new Button(container, SWT.CHECK);
            _checkBox_OffsetValues.setText("Offset");
            GridDataFactory.fillDefaults().indent(_defaultCheckBoxIndent, 0).align(SWT.END, SWT.FILL).applyTo(_checkBox_OffsetValues);
            _checkBox_OffsetValues.addSelectionListener(new SelectionListener() {

               @Override
               public void widgetDefaultSelected(final SelectionEvent arg0) {}

               @Override
               public void widgetSelected(final SelectionEvent arg0) {

                  // ToggleCheckBoxes(!_checkBox_OffsetValues.getSelection());
                  if (_checkBox_OffsetValues.getSelection()) {
                     _checkBox_NewValues.setSelection(false);
                  }

                  enableControls();
               }

            });
         }

         /*
          * Altitude
          */
         {
            // label
            Label label = _tk.createLabel(container, "altitude");
            label.setToolTipText(Messages.tour_editor_label_wind_speed_Tooltip);

            // spinner
            _textAltitudeValue = new Text(container, SWT.BORDER);
            GridDataFactory.fillDefaults()
                  .hint(_hintDefaultSpinnerWidth, SWT.DEFAULT)
                  .align(SWT.BEGINNING, SWT.CENTER)
                  .applyTo(_textAltitudeValue);
            _textAltitudeValue.setToolTipText(Messages.tour_editor_label_wind_speed_Tooltip);
            _textAltitudeValue.addMouseWheelListener(_mouseWheelListener);
            _textAltitudeValue.addModifyListener(new ModifyListener() {

               @Override
               public void modifyText(final ModifyEvent arg0) {
               }
            });

            // label: m or ft
            label = _tk.createLabel(container, UI.UNIT_LABEL_ALTITUDE);

            /*
             * checkbox: new value
             */
            _radioButton_Altitude_NewValue = new Button(container, SWT.RADIO);
            GridDataFactory.fillDefaults().align(SWT.CENTER, SWT.FILL).applyTo(_radioButton_Altitude_NewValue);
            _radioButton_Altitude_NewValue.setToolTipText(Messages.Dialog_HRZone_Label_Trash_Tooltip);
            _radioButton_Altitude_NewValue.addSelectionListener(new SelectionListener() {

               @Override
               public void widgetDefaultSelected(final SelectionEvent arg0) {}

               @Override
               public void widgetSelected(final SelectionEvent arg0) {
                  enableControls();
               }
            });

            /*
             * checkbox: offset
             */
            _radioButton_Altitude_OffsetValue = new Button(container, SWT.RADIO);
            GridDataFactory.fillDefaults().align(SWT.CENTER, SWT.FILL).applyTo(_radioButton_Altitude_OffsetValue);
            _radioButton_Altitude_OffsetValue.setToolTipText(Messages.Dialog_HRZone_Label_Trash_Tooltip);
            _radioButton_Altitude_OffsetValue.addSelectionListener(new SelectionListener() {

               @Override
               public void widgetDefaultSelected(final SelectionEvent arg0) {}

               @Override
               public void widgetSelected(final SelectionEvent arg0) {
                  enableControls();
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
            _textPulseValue = new Text(container, SWT.BORDER);
            GridDataFactory.fillDefaults()
                  .hint(_hintDefaultSpinnerWidth, SWT.DEFAULT)
                  .align(SWT.BEGINNING, SWT.CENTER)
                  .applyTo(_textPulseValue);
            _textPulseValue.setEnabled(false);
            _textPulseValue.setToolTipText(Messages.tour_editor_label_wind_speed_Tooltip);
            _textPulseValue.addMouseWheelListener(_mouseWheelListener);
            _textPulseValue.addModifyListener(new ModifyListener() {

               @Override
               public void modifyText(final ModifyEvent arg0) {

               }
            });

            // label: bpm
            label = _tk.createLabel(container, "bpm");

            /*
             * checkbox: new value
             */
            _radioButton_Pulse_NewValue = new Button(container, SWT.RADIO);
            GridDataFactory.fillDefaults().align(SWT.CENTER, SWT.FILL).applyTo(_radioButton_Pulse_NewValue);
            _radioButton_Pulse_NewValue.setToolTipText(Messages.Dialog_HRZone_Label_Trash_Tooltip);
            _radioButton_Pulse_NewValue.addSelectionListener(new SelectionListener() {

               @Override
               public void widgetDefaultSelected(final SelectionEvent arg0) {}

               @Override
               public void widgetSelected(final SelectionEvent arg0) {
                  enableControls();
               }
            });

            /*
             * checkbox: offset
             */
            _radioButton_Pulse_OffsetValue = new Button(container, SWT.RADIO);
            GridDataFactory.fillDefaults().align(SWT.CENTER, SWT.FILL).applyTo(_radioButton_Pulse_OffsetValue);
            _radioButton_Pulse_OffsetValue.setToolTipText(Messages.Dialog_HRZone_Label_Trash_Tooltip);
            _radioButton_Pulse_OffsetValue.addSelectionListener(new SelectionListener() {

               @Override
               public void widgetDefaultSelected(final SelectionEvent arg0) {}

               @Override
               public void widgetSelected(final SelectionEvent arg0) {
                  enableControls();
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
            _textCadenceValue = new Text(container, SWT.BORDER);
            GridDataFactory.fillDefaults()
                  .hint(_hintDefaultSpinnerWidth, SWT.DEFAULT)
                  .align(SWT.BEGINNING, SWT.CENTER)
                  .applyTo(_textCadenceValue);
            _textCadenceValue.addMouseWheelListener(_mouseWheelListener);
            _textCadenceValue.addModifyListener(new ModifyListener() {

               @Override
               public void modifyText(final ModifyEvent arg0) {

               }
            });

            // label: m or ft
            label = _tk.createLabel(container, "spm");

            /*
             * checkbox: new value
             */
            _radioButton_Cadence_NewValue = new Button(container, SWT.RADIO);
            GridDataFactory.fillDefaults().align(SWT.CENTER, SWT.FILL).applyTo(_radioButton_Cadence_NewValue);
            _radioButton_Cadence_NewValue.setToolTipText(Messages.Dialog_HRZone_Label_Trash_Tooltip);
            _radioButton_Cadence_NewValue.addSelectionListener(new SelectionListener() {

               @Override
               public void widgetDefaultSelected(final SelectionEvent arg0) {}

               @Override
               public void widgetSelected(final SelectionEvent arg0) {
                  enableControls();
               }
            });

            /*
             * checkbox: offset
             */
            _radioButton_Cadence_OffsetValue = new Button(container, SWT.RADIO);
            GridDataFactory.fillDefaults().align(SWT.CENTER, SWT.FILL).applyTo(_radioButton_Cadence_OffsetValue);
            _radioButton_Cadence_OffsetValue.setToolTipText(Messages.Dialog_HRZone_Label_Trash_Tooltip);
            _radioButton_Cadence_OffsetValue.addSelectionListener(new SelectionListener() {

               @Override
               public void widgetDefaultSelected(final SelectionEvent arg0) {}

               @Override
               public void widgetSelected(final SelectionEvent arg0) {
                  enableControls();
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
            _textTemperatureValue = new Text(container, SWT.BORDER);
            GridDataFactory.fillDefaults()
                  .hint(_hintDefaultSpinnerWidth, SWT.DEFAULT)
                  .align(SWT.BEGINNING, SWT.CENTER)
                  .applyTo(_textTemperatureValue);
            _textTemperatureValue.setToolTipText(Messages.tour_editor_label_wind_speed_Tooltip);
            _textTemperatureValue.addMouseWheelListener(_mouseWheelListener);
            _textTemperatureValue.addModifyListener(new ModifyListener() {

               @Override
               public void modifyText(final ModifyEvent arg0) {

               }
            });

            // label: Celsius or Fahrenheit
            label = _tk.createLabel(container, UI.UNIT_LABEL_TEMPERATURE);

            /*
             * checkbox: new value
             */
            _radioButton_Temperature_NewValue = new Button(container, SWT.RADIO);
            GridDataFactory.fillDefaults().align(SWT.CENTER, SWT.FILL).applyTo(_radioButton_Temperature_NewValue);
            _radioButton_Temperature_NewValue.setToolTipText(Messages.Dialog_HRZone_Label_Trash_Tooltip);
            _radioButton_Temperature_NewValue.addSelectionListener(new SelectionListener() {

               @Override
               public void widgetDefaultSelected(final SelectionEvent arg0) {}

               @Override
               public void widgetSelected(final SelectionEvent arg0) {
                  enableControls();
               }
            });

            /*
             * checkbox: offset
             */
            _radioButton_Temperature_OffsetValue = new Button(container, SWT.RADIO);
            GridDataFactory.fillDefaults().align(SWT.CENTER, SWT.FILL).applyTo(_radioButton_Temperature_OffsetValue);
            _radioButton_Temperature_OffsetValue.setToolTipText(Messages.Dialog_HRZone_Label_Trash_Tooltip);
            _radioButton_Temperature_OffsetValue.addSelectionListener(new SelectionListener() {

               @Override
               public void widgetDefaultSelected(final SelectionEvent arg0) {}

               @Override
               public void widgetSelected(final SelectionEvent arg0) {
                  enableControls();
               }
            });
         }
      }
   }

   private void enableAltitudeControls(final boolean enableText, final boolean enableRadioButtons) {
      _textAltitudeValue.setEnabled(enableText);
      _radioButton_Altitude_NewValue.setEnabled(enableRadioButtons);
      _radioButton_Altitude_OffsetValue.setEnabled(enableRadioButtons);

   }

   private void enableCadenceControls(final boolean enableText, final boolean enableRadioButtons) {
      _textCadenceValue.setEnabled(enableText);
      _radioButton_Cadence_NewValue.setEnabled(enableRadioButtons);
      _radioButton_Cadence_OffsetValue.setEnabled(enableRadioButtons);

   }

   private void enableControls() {
      final boolean isOverallButtonChecked = _checkBox_NewValues.getSelection() ||
            _checkBox_OffsetValues.getSelection();

      if (_tourData.altitudeSerie == null) {
         enableAltitudeControls(false, false);
      } else {
         final boolean enableRadioButtons = !isOverallButtonChecked;
         enableAltitudeControls(true, enableRadioButtons);
      }

      if (_tourData.pulseSerie == null ) {
         enablePulseControls(false, false);
      } else {
         final boolean enableRadioButtons = !isOverallButtonChecked;
         enablePulseControls(true, enableRadioButtons);
      }

      if (_tourData.getCadenceSerie() == null) {
         enableCadenceControls(false, false);
      } else {
         final boolean enableRadioButtons = !isOverallButtonChecked;
         enableCadenceControls(true, enableRadioButtons);
      }

      if (_tourData.temperatureSerie == null) {
         enableTemperatureControls(false, false);
      } else {
         final boolean enableRadioButtons = !isOverallButtonChecked;
         enableTemperatureControls(true, enableRadioButtons);
      }

   }

   private void enablePulseControls(final boolean enableText, final boolean enableRadioButtons) {
      _textPulseValue.setEnabled(enableText);
      _radioButton_Pulse_NewValue.setEnabled(enableRadioButtons);
      _radioButton_Pulse_OffsetValue.setEnabled(enableRadioButtons);
   }

   private void enableTemperatureControls(final boolean enableText, final boolean enableRadioButtons) {
      _textTemperatureValue.setEnabled(enableText);
      _radioButton_Temperature_NewValue.setEnabled(enableRadioButtons);
      _radioButton_Temperature_OffsetValue.setEnabled(enableRadioButtons);

   }

   @Override
   protected IDialogSettings getDialogBoundsSettings() {
      return _state;
   }

   public boolean getIsAltitudeValueOffset() {
      return _isAltitudeValueOffset;
   }

   public boolean getIsCadenceValueOffset() {
      return _isCadenceValueOffset;
   }

   public boolean getIsPulseValueOffset() {
      return _isPulseValueOffset;
   }

   public boolean getIsTemperatureValueOffset() {
      return _isTemperatureValueOffset;
   }

   public float getNewAltitudeValue() {
      return _newAltitudeValue;
   }

   public float getNewCadenceValue() {
      return _newCadenceValue;
   }

   public float getNewPulseValue() {
      return _newPulseValue;
   }

   public float getNewTemperatureValue() {
      return _newTemperatureValue;
   }

   @Override
   protected void okPressed() {

      //TODO convert the values to metric if needed
      final String altitudeValue = _textAltitudeValue.getText();
      _newAltitudeValue = !altitudeValue.equals("") ? Float.parseFloat(altitudeValue) : Float.MIN_VALUE;
      final String pulseValue = _textPulseValue.getText();
      _newPulseValue = !pulseValue.equals("") ? Float.parseFloat(pulseValue) : Float.MIN_VALUE;
      final String cadenceValue = _textCadenceValue.getText();
      _newCadenceValue = !cadenceValue.equals("") ? Float.parseFloat(cadenceValue) : Float.MIN_VALUE;
      final String temperatureValue = _textTemperatureValue.getText();
      _newTemperatureValue = !temperatureValue.equals("") ? Float.parseFloat(temperatureValue) : Float.MIN_VALUE;

      _isAltitudeValueOffset = _checkBox_OffsetValues.getSelection() || _radioButton_Altitude_OffsetValue.getSelection();
      _isPulseValueOffset = _checkBox_OffsetValues.getSelection() || _radioButton_Pulse_OffsetValue.getSelection();
      _isCadenceValueOffset = _checkBox_OffsetValues.getSelection() || _radioButton_Cadence_OffsetValue.getSelection();
      _isTemperatureValueOffset = _checkBox_OffsetValues.getSelection() || _radioButton_Temperature_OffsetValue.getSelection();

      super.okPressed();
   }

   private void onDispose() {

      if (_tk != null) {
         _tk.dispose();
      }

   }

   private void ToggleCheckBoxes(final boolean selection) {
      _radioButton_Altitude_NewValue.setEnabled(selection);
      _radioButton_Altitude_OffsetValue.setEnabled(selection);
      _radioButton_Pulse_NewValue.setEnabled(selection);
      _radioButton_Pulse_OffsetValue.setEnabled(selection);
      _radioButton_Cadence_NewValue.setEnabled(selection);
      _radioButton_Cadence_OffsetValue.setEnabled(selection);
      _radioButton_Temperature_NewValue.setEnabled(selection);
      _radioButton_Temperature_OffsetValue.setEnabled(selection);

      if (!selection) {
         _radioButton_Altitude_NewValue.setSelection(selection);
         _radioButton_Altitude_OffsetValue.setSelection(selection);
         _radioButton_Pulse_NewValue.setSelection(selection);
         _radioButton_Pulse_OffsetValue.setSelection(selection);
         _radioButton_Cadence_NewValue.setSelection(selection);
         _radioButton_Cadence_OffsetValue.setSelection(selection);
         _radioButton_Temperature_NewValue.setSelection(selection);
         _radioButton_Temperature_OffsetValue.setSelection(selection);
      }
   }

   private void updateUIFromModel() {

      if (_selectedIndex == -1) {
         return;
      }

      _isUpdateUI = true;
      {
         final int altitudeUnit = (int) (_tourData.altitudeSerie[_selectedIndex] * 10 / UI.UNIT_VALUE_TEMPERATURE);
         // _spinAltitudeValue.setSelection(altitudeUnit);

      }
      _isUpdateUI = false;

   }
}
