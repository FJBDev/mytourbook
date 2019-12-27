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

import org.eclipse.core.databinding.conversion.StringToNumberConverter;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.IMessageProvider;
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
   private Button             _checkBox_Altitude_NewValue;
   private Button             _checkBox_Altitude_OffsetValue;
   private Button             _checkBox_Pulse_NewValue;
   private Button             _checkBox_Pulse_OffsetValue;
   private Button             _checkBox_Cadence_NewValue;
   private Button             _checkBox_Cadence_OffsetValue;
   private Button             _checkBox_Temperature_NewValue;
   private Button             _checkBox_Temperature_OffsetValue;

   private Text               _textAltitudeValue;
   private Text               _textPulseValue;
   private Text               _textCadenceValue;
   private Text               _textTemperatureValue;

   private boolean            _isAltitudeValueValid       = true;
   private boolean            _isPulseValueValid          = true;
   private boolean            _isCadenceValueValid        = true;
   private boolean            _isTemperatureValueValid    = true;

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

      final Button saveButton = getButton(IDialogConstants.OK_ID);
      saveButton.setText(okText);
      saveButton.setEnabled(false);
   }

   @Override
   protected Control createDialogArea(final Composite parent) {

      final Composite dlgAreaContainer = (Composite) super.createDialogArea(parent);

      // create ui
      createUI(dlgAreaContainer);

      updateUIFromModel();
      enableControls();

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
            _checkBox_NewValues.setSelection(true);
            _checkBox_NewValues.addSelectionListener(new SelectionListener() {

               @Override
               public void widgetDefaultSelected(final SelectionEvent arg0) {}

               @Override
               public void widgetSelected(final SelectionEvent arg0) {

                  if (_checkBox_NewValues.getSelection()) {
                     _checkBox_OffsetValues.setSelection(false);
                     ToggleCheckBoxes(true);
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

                  if (_checkBox_OffsetValues.getSelection()) {
                     _checkBox_NewValues.setSelection(false);
                     ToggleCheckBoxes(false);
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
                  try {
                     Float.parseFloat(_textAltitudeValue.getText());
                     _isAltitudeValueValid = true;
                  } catch (final NumberFormatException e) {
                     _isAltitudeValueValid = false;
                  }
                  enableSaveButton();
               }
            });

            // label: m or ft
            label = _tk.createLabel(container, UI.UNIT_LABEL_ALTITUDE);

            /*
             * checkbox: new value
             */
            _checkBox_Altitude_NewValue = new Button(container, SWT.CHECK);
            GridDataFactory.fillDefaults().align(SWT.CENTER, SWT.FILL).applyTo(_checkBox_Altitude_NewValue);
            _checkBox_Altitude_NewValue.setToolTipText("toto");
            _checkBox_Altitude_NewValue.setSelection(true);
            _checkBox_Altitude_NewValue.addSelectionListener(new SelectionListener() {

               @Override
               public void widgetDefaultSelected(final SelectionEvent arg0) {}

               @Override
               public void widgetSelected(final SelectionEvent arg0) {
                  if (_checkBox_Altitude_NewValue.getSelection()) {
                     _checkBox_Altitude_OffsetValue.setSelection(false);
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
            _textPulseValue = new Text(container, SWT.BORDER);
            GridDataFactory.fillDefaults()
                  .hint(_hintDefaultSpinnerWidth, SWT.DEFAULT)
                  .align(SWT.BEGINNING, SWT.CENTER)
                  .applyTo(_textPulseValue);
            _textPulseValue.setToolTipText(Messages.tour_editor_label_wind_speed_Tooltip);
            _textPulseValue.addMouseWheelListener(_mouseWheelListener);
            _textPulseValue.addModifyListener(new ModifyListener() {

               @Override
               public void modifyText(final ModifyEvent arg0) {
                  try {
                     final String toto = _textPulseValue.getText();
                     Float.parseFloat(_textPulseValue.getText());
                     _isPulseValueValid = true;
                  } catch (final NumberFormatException e) {
                     _isPulseValueValid = false;
                  }
                  final Text titi = (Text) arg0.getSource();
                  final String tata = titi.getText();

                  //reuse this
                  if (_isSetField || _isSavingInProgress) {
                     return;
                  }

                  final Text widget = (Text) event.widget;
                  final String valueText = widget.getText().trim();

                  if (valueText.length() > 0) {
                     try {

                        // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
                        //
                        // Float.parseFloat() ignores localized strings therefore the databinding converter is used
                        // which provides also a good error message
                        //
                        // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

                        StringToNumberConverter.toFloat(true).convert(valueText);

                        _messageManager.removeMessage(widget.getData(WIDGET_KEY), widget);

                     } catch (final IllegalArgumentException e) {

                        // wrong characters are entered, display an error message

                        _messageManager.addMessage(
                              widget.getData(WIDGET_KEY),
                              e.getLocalizedMessage(),
                              null,
                              IMessageProvider.ERROR,
                              widget);
                     }
                  }

                  /*
                   * tour dirty must be set after validation because an error can occur which
                   * enables
                   * actions
                   */
                  if (_isTourDirty) {
                     /*
                      * when an error occurred previously and is now solved, the save action must be
                      * enabled
                      */
                     enableActions();
                  } else {
                     setTourDirty();
                  }
                  enableSaveButton();
               }
            });

            // label: bpm
            label = _tk.createLabel(container, "bpm");

            /*
             * checkbox: new value
             */
            _checkBox_Pulse_NewValue = new Button(container, SWT.CHECK);
            GridDataFactory.fillDefaults().align(SWT.CENTER, SWT.FILL).applyTo(_checkBox_Pulse_NewValue);
            //_radioButton_Pulse_NewValue.setToolTipText(Messages.Dialog_HRZone_Label_Trash_Tooltip);
            _checkBox_Pulse_NewValue.setSelection(true);
            _checkBox_Pulse_NewValue.addSelectionListener(new SelectionListener() {

               @Override
               public void widgetDefaultSelected(final SelectionEvent arg0) {}

               @Override
               public void widgetSelected(final SelectionEvent arg0) {
                  if (_checkBox_Pulse_NewValue.getSelection()) {
                     _checkBox_Pulse_OffsetValue.setSelection(false);
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
            _textCadenceValue = new Text(container, SWT.BORDER);
            GridDataFactory.fillDefaults()
                  .hint(_hintDefaultSpinnerWidth, SWT.DEFAULT)
                  .align(SWT.BEGINNING, SWT.CENTER)
                  .applyTo(_textCadenceValue);
            _textCadenceValue.addMouseWheelListener(_mouseWheelListener);
            _textCadenceValue.addModifyListener(new ModifyListener() {

               @Override
               public void modifyText(final ModifyEvent arg0) {
                  try {
                     Float.parseFloat(_textCadenceValue.getText());
                     _isCadenceValueValid = true;
                  } catch (final NumberFormatException e) {
                     _isCadenceValueValid = false;
                  }
                  enableSaveButton();
               }
            });

            // label: m or ft
            label = _tk.createLabel(container, "spm");

            /*
             * checkbox: new value
             */
            _checkBox_Cadence_NewValue = new Button(container, SWT.CHECK);
            GridDataFactory.fillDefaults().align(SWT.CENTER, SWT.FILL).applyTo(_checkBox_Cadence_NewValue);
            _checkBox_Cadence_NewValue.setToolTipText(Messages.Dialog_HRZone_Label_Trash_Tooltip);
            _checkBox_Cadence_NewValue.setSelection(true);
            _checkBox_Cadence_NewValue.addSelectionListener(new SelectionListener() {

               @Override
               public void widgetDefaultSelected(final SelectionEvent arg0) {}

               @Override
               public void widgetSelected(final SelectionEvent arg0) {
                  if (_checkBox_Cadence_NewValue.getSelection()) {
                     _checkBox_Cadence_OffsetValue.setSelection(false);
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
                  try {
                     Float.parseFloat(_textTemperatureValue.getText());
                     _isTemperatureValueValid = true;
                  } catch (final NumberFormatException e) {
                     _isTemperatureValueValid = false;
                  }
                  enableSaveButton();
               }
            });

            // label: Celsius or Fahrenheit
            label = _tk.createLabel(container, UI.UNIT_LABEL_TEMPERATURE);

            /*
             * checkbox: new value
             */
            _checkBox_Temperature_NewValue = new Button(container, SWT.CHECK);
            GridDataFactory.fillDefaults().align(SWT.CENTER, SWT.FILL).applyTo(_checkBox_Temperature_NewValue);
            _checkBox_Temperature_NewValue.setToolTipText(Messages.Dialog_HRZone_Label_Trash_Tooltip);
            _checkBox_Temperature_NewValue.setSelection(true);
            _checkBox_Temperature_NewValue.addSelectionListener(new SelectionListener() {

               @Override
               public void widgetDefaultSelected(final SelectionEvent arg0) {}

               @Override
               public void widgetSelected(final SelectionEvent arg0) {
                  if (_checkBox_Temperature_NewValue.getSelection()) {
                     _checkBox_Temperature_OffsetValue.setSelection(false);
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
                  }
               }
            });
         }
      }
   }

   private void enableAltitudeControls(final boolean enableText, final boolean enableRadioButtons) {
      _textAltitudeValue.setEnabled(enableText);
      _checkBox_Altitude_NewValue.setEnabled(enableRadioButtons);
      _checkBox_Altitude_OffsetValue.setEnabled(enableRadioButtons);

   }

   private void enableCadenceControls(final boolean enableText, final boolean enableRadioButtons) {
      _textCadenceValue.setEnabled(enableText);
      _checkBox_Cadence_NewValue.setEnabled(enableRadioButtons);
      _checkBox_Cadence_OffsetValue.setEnabled(enableRadioButtons);

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

      if (_tourData.pulseSerie == null) {
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
      _checkBox_Pulse_NewValue.setEnabled(enableRadioButtons);
      _checkBox_Pulse_OffsetValue.setEnabled(enableRadioButtons);
   }

   private void enableSaveButton() {
      final Button saveButton = getButton(IDialogConstants.OK_ID);
      if (saveButton != null) {

         final boolean isEnabled = _isAltitudeValueValid && _isPulseValueValid && _isCadenceValueValid && _isTemperatureValueValid;
         saveButton.setEnabled(isEnabled);
      }
   }

   private void enableTemperatureControls(final boolean enableText, final boolean enableRadioButtons) {
      _textTemperatureValue.setEnabled(enableText);
      _checkBox_Temperature_NewValue.setEnabled(enableRadioButtons);
      _checkBox_Temperature_OffsetValue.setEnabled(enableRadioButtons);

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

      _isAltitudeValueOffset = _checkBox_OffsetValues.getSelection() || _checkBox_Altitude_OffsetValue.getSelection();
      _isPulseValueOffset = _checkBox_OffsetValues.getSelection() || _checkBox_Pulse_OffsetValue.getSelection();
      _isCadenceValueOffset = _checkBox_OffsetValues.getSelection() || _checkBox_Cadence_OffsetValue.getSelection();
      _isTemperatureValueOffset = _checkBox_OffsetValues.getSelection() || _checkBox_Temperature_OffsetValue.getSelection();

      super.okPressed();
   }

   private void onDispose() {

      if (_tk != null) {
         _tk.dispose();
      }

   }

   private void ToggleCheckBoxes(final boolean checkAllNewValues) {
      _checkBox_Altitude_NewValue.setSelection(checkAllNewValues);
      _checkBox_Pulse_NewValue.setSelection(checkAllNewValues);
      _checkBox_Cadence_NewValue.setSelection(checkAllNewValues);
      _checkBox_Temperature_NewValue.setSelection(checkAllNewValues);

      _checkBox_Altitude_OffsetValue.setSelection(!checkAllNewValues);
      _checkBox_Pulse_OffsetValue.setSelection(!checkAllNewValues);
      _checkBox_Cadence_OffsetValue.setSelection(!checkAllNewValues);
      _checkBox_Temperature_OffsetValue.setSelection(!checkAllNewValues);

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
