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
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;

public class DialogEditTimeSlicesValues extends TitleAreaDialog {

   private final boolean         _isOSX                         = UI.IS_OSX;
   private final boolean         _isLinux                       = UI.IS_LINUX;

   private final TourData        _tourData;

   private final IDialogSettings _state;
   private PixelConverter        _pc;

   private int                   _hintDefaultSpinnerWidth;
   private int                   _defaultCheckBoxIndent;

   private boolean               _isUpdateUI                    = false;
   private boolean               _isTemperatureManuallyModified = false;
   private boolean               _isWindSpeedManuallyModified   = false;
   private float                 _unitValueDistance;
   private float                 _unitValueTemperature;

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

   private Combo              _comboTitle;
   private Combo              _comboNewValue_All;
   private Combo              _comboOffset_All;

   private Spinner            _spinAltitudeValue;
   private Spinner            _spinPulseValue;
   private Spinner            _spinCadenceValue;
   private Spinner            _spinTemperatureValue;

   private Text               _txtLatitude;
   private Text               _txtLongitude;

   private MouseWheelListener _mouseWheelListener;
   {
      _mouseWheelListener = new MouseWheelListener() {
         @Override
         public void mouseScrolled(final MouseEvent event) {
            Util.adjustSpinnerValueOnMouseScroll(event);
         }
      };
   }

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
      _defaultCheckBoxIndent = 100;

      _unitValueDistance = net.tourbook.ui.UI.UNIT_VALUE_DISTANCE;
      _unitValueTemperature = net.tourbook.ui.UI.UNIT_VALUE_TEMPERATURE;

      _tk = new FormToolkit(parent.getDisplay());

      _formContainer = _tk.createForm(parent);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(_formContainer);
      _tk.decorateFormHeading(_formContainer);
      _tk.setBorderStyle(SWT.BORDER);

      final Composite tourContainer = _formContainer.getBody();
      GridLayoutFactory.swtDefaults().applyTo(tourContainer);
      {
         //createUI_100_Headers(tourContainer);
         createUI_200_Values(tourContainer);
      }

      final Label label = new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(label);

      tourContainer.layout(true, true);
   }

   private void createUI_200_Values(final Composite parent) {

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
            GridDataFactory.fillDefaults().span(4, 1).align(SWT.END, SWT.FILL).applyTo(_button_NewValues);
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
            GridDataFactory.fillDefaults().align(SWT.CENTER, SWT.FILL).applyTo(_button_OffsetValues);
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
            GridDataFactory.fillDefaults().indent(_defaultCheckBoxIndent, 0).align(SWT.CENTER, SWT.FILL).applyTo(_checkBox_Altitude_NewValue);
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
            GridDataFactory.fillDefaults().indent(_defaultCheckBoxIndent, 0).align(SWT.CENTER, SWT.FILL).applyTo(_checkBox_Altitude_OffsetValue);
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
            GridDataFactory.fillDefaults().indent(_defaultCheckBoxIndent, 0).align(SWT.CENTER, SWT.FILL).applyTo(_checkBox_Pulse_NewValue);
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
            GridDataFactory.fillDefaults().indent(_defaultCheckBoxIndent, 0).align(SWT.CENTER, SWT.FILL).applyTo(_checkBox_Pulse_OffsetValue);
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

         /*
          * Latitude
          */
         {

            // label
            final Label label = _tk.createLabel(container, "Latitude");
            label.setToolTipText(Messages.tour_editor_label_wind_direction_Tooltip);

            // spinner
            _txtLatitude = new Text(container, SWT.BORDER);
            GridDataFactory.fillDefaults()
                  .hint(_hintDefaultSpinnerWidth, SWT.DEFAULT)
                  .align(SWT.BEGINNING, SWT.CENTER)
                  .applyTo(_txtLatitude);
            _txtLatitude.setToolTipText(Messages.tour_editor_label_wind_speed_Tooltip);

            // label: direction unit = degree
            _tk.createLabel(container, Messages.Tour_Editor_Label_WindDirection_Unit);
         }
      }
   }

   private void enableControls(final SelectionEvent e) {
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

      updateModelFromUI();

      if (_tourData.isValidForSave() == false) {
         // data are not valid to be saved which is done in the action which opened this dialog
         return;
      }

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
   }

   /**
    * update tourdata from the fields
    */
   private void updateModelFromUI() {

      _tourData.setTourTitle(_comboTitle.getText().trim());
      //TODO_tourData.s(_txtLatitude.getText().trim());

      if (_isWindSpeedManuallyModified) {
         /*
          * update the speed only when it was modified because when the measurement is changed
          * when the tour is being modified then the computation of the speed value can cause
          * rounding errors
          */
         _tourData.setWeatherWindSpeed((int) (_spinAltitudeValue.getSelection() * _unitValueDistance));
      }

      //TODO_tourData.setWeather(_txtLatitude.getText().trim());

   }

   private void updateUIFromModel() {
      /*
       * _isUpdateUI = true;
       * {
       * Tour/event
       * // set field content
       * _comboTitle.setText(_tourData.getTourTitle());
       * _txtDescription.setText(_tourData.getTourDescription());
       * _comboLocation_Start.setText(_tourData.getTourStartPlace());
       * _comboLocation_End.setText(_tourData.getTourEndPlace());
       * Personal details
       * final float bodyWeight = UI.convertBodyWeightFromMetric(_tourData.getBodyWeight());
       * _spinBodyWeight.setSelection(Math.round(bodyWeight * 10));
       * _spinFTP.setSelection(_tourData.getPower_FTP());
       * _spinRestPuls.setSelection(_tourData.getRestPulse());
       * _spinCalories.setSelection(_tourData.getCalories());
       * Wind properties
       * _txtWeather.setText(_tourData.getWeather());
       * // wind direction
       * final int weatherWindDirDegree = _tourData.getWeatherWindDir() * 10;
       * _spinWeather_Wind_DirectionValue.setSelection(weatherWindDirDegree);
       * _comboWeather_Wind_DirectionText.select(UI.getCardinalDirectionTextIndex(
       * weatherWindDirDegree));
       * // wind speed
       * final int windSpeed = _tourData.getWeatherWindSpeed();
       * final int speed = (int) (windSpeed / _unitValueDistance);
       * _spinWeather_Wind_SpeedValue.setSelection(speed);
       * _comboWeather_Wind_SpeedText.select(getWindSpeedTextIndex(speed));
       * // weather clouds
       * _comboWeather_Clouds.select(_tourData.getWeatherIndex());
       * // icon must be displayed after the combobox entry is selected
       * displayCloudIcon();
       * Avg temperature
       * float avgTemperature = _tourData.getAvgTemperature();
       * if (_unitValueTemperature != 1) {
       * final float metricTemperature = avgTemperature;
       * avgTemperature = metricTemperature
       * net.tourbook.ui.UI.UNIT_FAHRENHEIT_MULTI
       * + net.tourbook.ui.UI.UNIT_FAHRENHEIT_ADD;
       * }
       * _spinWeather_Temperature_Avg.setDigits(1);
       * _spinWeather_Temperature_Avg.setSelection(Math.round(avgTemperature * 10));
       * }
       * _isUpdateUI = false;
       */
   }
}
