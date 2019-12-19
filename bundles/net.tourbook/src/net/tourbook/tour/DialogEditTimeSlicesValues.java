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
import java.util.ArrayList;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.util.Util;
import net.tourbook.common.weather.IWeather;
import net.tourbook.data.TourData;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
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

   private static final String      GRAPH_LABEL_HEARTBEAT_UNIT     = net.tourbook.common.Messages.Graph_Label_Heartbeat_Unit;
   private static final String      VALUE_UNIT_K_CALORIES          = net.tourbook.ui.Messages.Value_Unit_KCalories;

   private final boolean            _isOSX                         = UI.IS_OSX;
   private final boolean            _isLinux                       = UI.IS_LINUX;

   private final TourData           _tourData;

   private final IDialogSettings    _state;
   private PixelConverter           _pc;

   /**
    * contains the controls which are displayed in the first column, these controls are used to get
    * the maximum width and set the first column within the different section to the same width
    */
   private final ArrayList<Control> _firstColumnControls           = new ArrayList<>();
   private final ArrayList<Control> _firstColumnContainerControls  = new ArrayList<>();
   private final ArrayList<Control> _secondColumnControls          = new ArrayList<>();

   private int                      _hintDefaultSpinnerWidth;

   private boolean                  _isUpdateUI                    = false;
   private boolean                  _isTemperatureManuallyModified = false;
   private boolean                  _isWindSpeedManuallyModified   = false;
   private float                    _unitValueDistance;
   private float                    _unitValueTemperature;

   /*
    * UI controls
    */
   private FormToolkit        _tk;
   private Form               _formContainer;

   private CLabel             _lblWeather_CloudIcon;

   private Button             _checkBox_Altitude_NewValue;
   private Button             _checkBox_Altitude_OffsetValue;

   private Combo              _comboLocation_Start;
   private Combo              _comboLocation_End;
   private Combo              _comboTitle;
   private Combo              _comboWeather_Clouds;
   private Combo              _comboWeather_Wind_DirectionText;
   private Combo              _comboNewValue_All;
   private Combo              _comboOffset_All;

   private Spinner            _spinBodyWeight;
   private Spinner            _spinFTP;
   private Spinner            _spinRestPuls;
   private Spinner            _spinCalories;
   private Spinner            _spinWeather_Temperature_Avg;
   private Spinner            _spinAltitudeValue;
   private Spinner            _spinHeartRateValue;
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

      setTitle(Messages.dialog_quick_edit_dialog_area_title);

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
         createUI_200_Values(tourContainer);
      }

      final Label label = new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(label);

      // compute width for all controls and equalize column width for the different sections
      tourContainer.layout(true, true);
      UI.setEqualizeColumWidths(_firstColumnControls);
      UI.setEqualizeColumWidths(_secondColumnControls);

      tourContainer.layout(true, true);
      UI.setEqualizeColumWidths(_firstColumnContainerControls);
   }

   private void createUI_142_Weather(final Composite parent) {

      final Composite container = _tk.createComposite(parent);
      GridDataFactory.fillDefaults().span(2, 1).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(5).applyTo(container);
      {
         {
            /*
             * Altitude
             */

            // label
            Label label = _tk.createLabel(container, Messages.tour_editor_label_wind_speed);
            label.setToolTipText(Messages.tour_editor_label_wind_speed_Tooltip);
            _firstColumnControls.add(label);

            // spinner
            _spinAltitudeValue = new Spinner(container, SWT.BORDER);
            GridDataFactory.fillDefaults()
                  .hint(_hintDefaultSpinnerWidth, SWT.DEFAULT)
                  .align(SWT.BEGINNING, SWT.CENTER)
                  .applyTo(_spinAltitudeValue);
            _spinAltitudeValue.setMinimum(0);
            _spinAltitudeValue.setMaximum(120);
            _spinAltitudeValue.setToolTipText(Messages.tour_editor_label_wind_speed_Tooltip);

            _spinAltitudeValue.addModifyListener(new ModifyListener() {
               @Override
               public void modifyText(final ModifyEvent e) {
                  if (_isUpdateUI) {
                     return;
                  }
               }
            });
            _spinAltitudeValue.addSelectionListener(new SelectionAdapter() {
               @Override
               public void widgetSelected(final SelectionEvent e) {
                  if (_isUpdateUI) {
                     return;
                  }
               }
            });
            _spinAltitudeValue.addMouseWheelListener(new MouseWheelListener() {
               @Override
               public void mouseScrolled(final MouseEvent event) {
                  Util.adjustSpinnerValueOnMouseScroll(event);
                  if (_isUpdateUI) {
                     return;
                  }
               }
            });

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

         {
            /*
             * Heart rate
             */

            // label
            Label label = _tk.createLabel(container, Messages.tour_editor_label_wind_speed);
            label.setToolTipText(Messages.tour_editor_label_wind_speed_Tooltip);
            _firstColumnControls.add(label);

            // spinner
            _spinHeartRateValue = new Spinner(container, SWT.BORDER);
            GridDataFactory.fillDefaults()
                  .hint(_hintDefaultSpinnerWidth, SWT.DEFAULT)
                  .align(SWT.BEGINNING, SWT.CENTER)
                  .applyTo(_spinAltitudeValue);
            _spinHeartRateValue.setMinimum(0);
            _spinHeartRateValue.setMaximum(120);
            _spinHeartRateValue.setToolTipText(Messages.tour_editor_label_wind_speed_Tooltip);

            _spinHeartRateValue.addModifyListener(new ModifyListener() {
               @Override
               public void modifyText(final ModifyEvent e) {
                  if (_isUpdateUI) {
                     return;
                  }
               }
            });
            _spinHeartRateValue.addSelectionListener(new SelectionAdapter() {
               @Override
               public void widgetSelected(final SelectionEvent e) {
                  if (_isUpdateUI) {
                     return;
                  }
               }
            });
            _spinHeartRateValue.addMouseWheelListener(new MouseWheelListener() {
               @Override
               public void mouseScrolled(final MouseEvent event) {
                  Util.adjustSpinnerValueOnMouseScroll(event);
                  if (_isUpdateUI) {
                     return;
                  }
               }
            });

            // label: m or ft
            label = _tk.createLabel(container, "bpm");

         }

         {
            /*
             * Cadence
             */

            // label
            Label label = _tk.createLabel(container, Messages.tour_editor_label_wind_speed);
            label.setToolTipText(Messages.tour_editor_label_wind_speed_Tooltip);
            _firstColumnControls.add(label);

            // spinner
            _spinCadenceValue = new Spinner(container, SWT.BORDER);
            GridDataFactory.fillDefaults()
                  .hint(_hintDefaultSpinnerWidth, SWT.DEFAULT)
                  .align(SWT.BEGINNING, SWT.CENTER)
                  .applyTo(_spinAltitudeValue);
            _spinCadenceValue.setMinimum(0);
            _spinCadenceValue.setMaximum(120);
            _spinCadenceValue.setToolTipText(Messages.tour_editor_label_wind_speed_Tooltip);

            _spinCadenceValue.addModifyListener(new ModifyListener() {
               @Override
               public void modifyText(final ModifyEvent e) {
                  if (_isUpdateUI) {
                     return;
                  }
               }
            });
            _spinCadenceValue.addSelectionListener(new SelectionAdapter() {
               @Override
               public void widgetSelected(final SelectionEvent e) {
                  if (_isUpdateUI) {
                     return;
                  }
               }
            });
            _spinCadenceValue.addMouseWheelListener(new MouseWheelListener() {
               @Override
               public void mouseScrolled(final MouseEvent event) {
                  Util.adjustSpinnerValueOnMouseScroll(event);
                  if (_isUpdateUI) {
                     return;
                  }
               }
            });

            // label: m or ft
            label = _tk.createLabel(container, "spm");

         }

         {
            /*
             * Temperature
             */

            // label
            Label label = _tk.createLabel(container, Messages.tour_editor_label_wind_speed);
            label.setToolTipText(Messages.tour_editor_label_wind_speed_Tooltip);
            _firstColumnControls.add(label);

            // spinner
            _spinTemperatureValue = new Spinner(container, SWT.BORDER);
            GridDataFactory.fillDefaults()
                  .hint(_hintDefaultSpinnerWidth, SWT.DEFAULT)
                  .align(SWT.BEGINNING, SWT.CENTER)
                  .applyTo(_spinAltitudeValue);
            _spinTemperatureValue.setMinimum(0);
            _spinTemperatureValue.setMaximum(120);
            _spinTemperatureValue.setToolTipText(Messages.tour_editor_label_wind_speed_Tooltip);

            _spinTemperatureValue.addModifyListener(new ModifyListener() {
               @Override
               public void modifyText(final ModifyEvent e) {
                  if (_isUpdateUI) {
                     return;
                  }
               }
            });
            _spinTemperatureValue.addSelectionListener(new SelectionAdapter() {
               @Override
               public void widgetSelected(final SelectionEvent e) {
                  if (_isUpdateUI) {
                     return;
                  }
               }
            });
            _spinTemperatureValue.addMouseWheelListener(new MouseWheelListener() {
               @Override
               public void mouseScrolled(final MouseEvent event) {
                  Util.adjustSpinnerValueOnMouseScroll(event);
                  if (_isUpdateUI) {
                     return;
                  }
               }
            });

            // label: m or ft
            label = _tk.createLabel(container, "spm");

         }

         {
            /*
             * wind direction
             */

            // label
            final Label label = _tk.createLabel(container, Messages.tour_editor_label_wind_direction);
            label.setToolTipText(Messages.tour_editor_label_wind_direction_Tooltip);
            _firstColumnControls.add(label);

            // combo: wind direction text
            _comboWeather_Wind_DirectionText = new Combo(container, SWT.READ_ONLY | SWT.BORDER);
            _tk.adapt(_comboWeather_Wind_DirectionText, true, false);
            _comboWeather_Wind_DirectionText.setToolTipText(Messages.tour_editor_label_WindDirectionNESW_Tooltip);
            _comboWeather_Wind_DirectionText.setVisibleItemCount(16);
            GridDataFactory.fillDefaults()
                  .align(SWT.BEGINNING, SWT.FILL)
                  .hint(_hintDefaultSpinnerWidth, SWT.DEFAULT)
                  .applyTo(_comboWeather_Wind_DirectionText);
            _comboWeather_Wind_DirectionText.addSelectionListener(new SelectionAdapter() {
               @Override
               public void widgetSelected(final SelectionEvent e) {

                  if (_isUpdateUI) {
                     return;
                  }
               }
            });

            // fill combobox
            for (final String fComboCloudsUIValue : IWeather.windDirectionText) {
               _comboWeather_Wind_DirectionText.add(fComboCloudsUIValue);
            }

            // spacer
            new Label(container, SWT.NONE);

            // label: direction unit = degree
            _tk.createLabel(container, Messages.Tour_Editor_Label_WindDirection_Unit);
         }
      }
   }

   private void createUI_200_Values(final Composite parent) {

      GridLayoutFactory.fillDefaults()
            .numColumns(2)
            .spacing(20, 5)
            .applyTo(parent);
      {
         createUI_142_Weather(parent);
      }
   }

   private void createUI_SectionSeparator(final Composite parent) {
      final Composite sep = _tk.createComposite(parent);
      GridDataFactory.fillDefaults().hint(SWT.DEFAULT, 5).applyTo(sep);
   }

   private void displayCloudIcon() {

      final int selectionIndex = _comboWeather_Clouds.getSelectionIndex();

      final String cloudKey = IWeather.cloudIcon[selectionIndex];
      final Image cloundIcon = UI.IMAGE_REGISTRY.get(cloudKey);

      _lblWeather_CloudIcon.setImage(cloundIcon);
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

      _firstColumnControls.clear();
      _secondColumnControls.clear();
      _firstColumnContainerControls.clear();
   }

   /**
    * update tourdata from the fields
    */
   private void updateModelFromUI() {

      _tourData.setTourTitle(_comboTitle.getText().trim());
      //TODO_tourData.s(_txtLatitude.getText().trim());

      _tourData.setTourStartPlace(_comboLocation_Start.getText().trim());
      _tourData.setTourEndPlace(_comboLocation_End.getText().trim());

      final float bodyWeight = UI.convertBodyWeightToMetric(_spinBodyWeight.getSelection());
      _tourData.setBodyWeight(bodyWeight / 10.0f);
      _tourData.setPower_FTP(_spinFTP.getSelection());
      _tourData.setRestPulse(_spinRestPuls.getSelection());
      _tourData.setCalories(_spinCalories.getSelection());

      if (_isWindSpeedManuallyModified) {
         /*
          * update the speed only when it was modified because when the measurement is changed
          * when the tour is being modified then the computation of the speed value can cause
          * rounding errors
          */
         _tourData.setWeatherWindSpeed((int) (_spinAltitudeValue.getSelection() * _unitValueDistance));
      }

      final int cloudIndex = _comboWeather_Clouds.getSelectionIndex();
      String cloudValue = IWeather.cloudIcon[cloudIndex];
      if (cloudValue.equals(UI.IMAGE_EMPTY_16)) {
         // replace invalid cloud key
         cloudValue = UI.EMPTY_STRING;
      }
      _tourData.setWeatherClouds(cloudValue);
      //TODO_tourData.setWeather(_txtLatitude.getText().trim());

      if (_isTemperatureManuallyModified) {
         float temperature = (float) _spinWeather_Temperature_Avg.getSelection() / 10;
         if (_unitValueTemperature != 1) {

            temperature = ((temperature - net.tourbook.ui.UI.UNIT_FAHRENHEIT_ADD)
                  / net.tourbook.ui.UI.UNIT_FAHRENHEIT_MULTI);
         }
         _tourData.setAvgTemperature(temperature);
      }

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
