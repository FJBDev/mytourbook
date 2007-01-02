/*******************************************************************************
 * Copyright (C) 2006, 2007  Wolfgang Schramm
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
package net.tourbook.dataImport;

import java.util.ArrayList;
import java.util.Enumeration;

import javax.comm.CommPortIdentifier;

import net.tourbook.data.TourPerson;
import net.tourbook.database.TourDatabase;
import net.tourbook.device.DeviceManager;
import net.tourbook.device.TourbookDevice;
import net.tourbook.preferences.ITourbookPreferences;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.preference.DirectoryFieldEditor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

public class WizardPageImportSettings extends WizardPage {

	// dialog settings
	private static final String			COMBO_DEVICE_ID		= "combo.device-id";
	private static final String			COMBO_PERSON_ID		= "combo.person-id";

	public static final String			COMBO_SERIAL_PORT	= "combo.serial-port";
	private static final String			CHECK_AUTO_SAVE		= "check.auto-save";
	private static final String			TEXT_AUTO_SAVE_PATH	= "text.auto-save-path";

	Combo								fComboDevice;
	Combo								fComboPorts;

	private boolean						fIsPortListAvailable;

	private ArrayList<TourPerson>		fPeopleList;
	private ArrayList<TourbookDevice>	fDeviceList;
	private ArrayList<TourbookDevice>	fDeviceListWithDeviceImport;

	private ArrayList<String>			fPortList;
	private Combo						fComboPerson;
	DirectoryFieldEditor				fAutoSavePathEditor;
	Button								fCheckAutoSave;
	private Composite					fGroupContainer;
	private Label						fLblDevice;
	protected boolean					fIsDeviceAvailable;

	protected WizardPageImportSettings(final String pageName) {
		super(pageName);
		setTitle("Import");
		setMessage("Import tour data from the device.");
	}

	public void createControl(final Composite parent) {

		initializeDialogUnits(parent);

		fGroupContainer = new Composite(parent, SWT.NONE);
		final GridData gdFillHorizontal = new GridData(SWT.FILL, SWT.NONE, true, false);
		fGroupContainer.setLayoutData(gdFillHorizontal);
		final GridLayout gl = new GridLayout(3, false);
		fGroupContainer.setLayout(gl);

		createFieldPerson();
		createFieldDevice();
		createFieldAutoSave();
		createFieldSerialPort();

		restoreDialogSettings();

		enableControls();
		validatePage();

		// control must be set, otherwise nothing is displayed
		setControl(fGroupContainer);
	}

	/**
	 * auto save after import
	 */
	private void createFieldAutoSave() {

		Label label;
		fCheckAutoSave = new Button(fGroupContainer, SWT.CHECK);
		fCheckAutoSave.setText("Autosave imported data");
		final GridData gdAutoSave = new GridData(SWT.FILL, SWT.NONE, true, false);
		gdAutoSave.horizontalSpan = 3;
		gdAutoSave.verticalIndent = 10;
		fCheckAutoSave.setLayoutData(gdAutoSave);
		fCheckAutoSave.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				enableControls();
				validatePage();
			}
		});

		/*
		 * path to save raw tour data
		 */
		fAutoSavePathEditor = new DirectoryFieldEditor(
				ITourbookPreferences.DUMMY_FIELD,
				"Path:",
				fGroupContainer);

		label = fAutoSavePathEditor.getLabelControl(fGroupContainer);
		final GridData gdPath = new GridData(SWT.NONE, SWT.NONE, false, false);
		gdPath.horizontalIndent = 20;
		label.setLayoutData(gdPath);

		fAutoSavePathEditor.setPropertyChangeListener(new IPropertyChangeListener() {
			public void propertyChange(final PropertyChangeEvent event) {
				validatePage();
			}
		});
	}

	/**
	 * device
	 */
	private void createFieldDevice() {

		fLblDevice = new Label(fGroupContainer, SWT.NONE);
		fLblDevice.setText("Device:");

		fComboDevice = new Combo(fGroupContainer, SWT.READ_ONLY | SWT.DROP_DOWN);
		fComboDevice.setVisibleItemCount(10);
		final GridData gdDevice = new GridData(SWT.FILL, SWT.NONE, true, false);
		gdDevice.verticalIndent = 10;
		fComboDevice.setLayoutData(gdDevice);

		fComboDevice.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				validatePage();
			}
		});

		// create device list
		fDeviceList = new ArrayList<TourbookDevice>();
		fDeviceList.addAll(DeviceManager.getDeviceList());

		fDeviceListWithDeviceImport = new ArrayList<TourbookDevice>();

		fComboDevice.add(DeviceManager.DEVICE_IS_NOT_SELECTED);
		for (final TourbookDevice device : fDeviceList) {
			if (device.canReadFromDevice) {

				// populate the device listbox
				fComboDevice.add(device.visibleName);

				// keep the device from where the data can be read
				fDeviceListWithDeviceImport.add(device);
			}
		}

		// filler
		final Label label = new Label(fGroupContainer, SWT.NONE);
	}

	/**
	 * person
	 */
	private void createFieldPerson() {
		Label label;
		label = new Label(fGroupContainer, SWT.NONE);
		label.setText("Use Settings from:");

		fComboPerson = new Combo(fGroupContainer, SWT.READ_ONLY | SWT.DROP_DOWN);
		fComboPerson.setVisibleItemCount(10);
		final GridData gdPerson = new GridData(SWT.FILL, SWT.NONE, true, false);
		fComboPerson.setLayoutData(gdPerson);
		fComboPerson.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				int personIndex = fComboPerson.getSelectionIndex();

				if (personIndex == 0) {
					// person is not selected
					fIsDeviceAvailable = false;
				} else {
					final TourPerson person = fPeopleList.get(--personIndex);
					selectPersonDevice(person);
					fAutoSavePathEditor.setStringValue(person.getRawDataPath());
				}
				enableControls();
				validatePage();
			}
		});

		fComboPerson.add("<Custom>");

		// add people to list
		fPeopleList = TourDatabase.getTourPeople();
		for (final TourPerson person : fPeopleList) {
			String lastName = person.getLastName();
			lastName = lastName.equals("") ? "" : " " + lastName;
			fComboPerson.add(person.getFirstName() + lastName);
		}

		// filler
		label = new Label(fGroupContainer, SWT.NONE);
	}

	/**
	 * serial port
	 */
	private void createFieldSerialPort() {

		final Label label = new Label(fGroupContainer, SWT.NONE);
		label.setText("Serial Port:");

		fComboPorts = new Combo(fGroupContainer, SWT.READ_ONLY | SWT.DROP_DOWN);
		fComboPorts.setVisibleItemCount(10);
		final GridData gdPorts = new GridData(SWT.NONE, SWT.NONE, false, false);
		gdPorts.horizontalSpan = 2;
		gdPorts.verticalIndent = 10;
		fComboPorts.setLayoutData(gdPorts);
		fComboPorts.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				validatePage();
			}
		});

		fPortList = new ArrayList<String>();

		// read serial ports from the system
		final Enumeration ports = CommPortIdentifier.getPortIdentifiers();
		fIsPortListAvailable = ports.hasMoreElements();

		if (fIsPortListAvailable) {
			// ports are available
			while (ports.hasMoreElements()) {
				final CommPortIdentifier port = (CommPortIdentifier) ports.nextElement();
				if (port.getPortType() == CommPortIdentifier.PORT_SERIAL) {
					final String portName = port.getName();
					fPortList.add(portName);
					fComboPorts.add(portName);
				}
			}

		} else {
			// no ports are available
			fComboPorts.add("COM ports are not available");
			fComboPorts.select(0);
			fComboPorts.setEnabled(false);
		}
	}

	private void enableControls() {

//		boolean isPersonSelected = fComboPerson.getSelectionIndex() > 0;

		fLblDevice.setEnabled(true);
		fComboDevice.setEnabled(true);
		fAutoSavePathEditor.setEnabled(true, fGroupContainer);

//		fLblDevice.setEnabled(!fIsDeviceAvailable);
//		fComboDevice.setEnabled(!fIsDeviceAvailable);
//		
//		if (fIsDeviceAvailable) {
//			fAutoSavePathEditor.setEnabled(false, fGroupContainer);
//		} else {
//			fAutoSavePathEditor.setEnabled(fCheckAutoSave.getSelection(), fGroupContainer);
//		}
	}
	void persistDialogSettings() {

		final IDialogSettings settings = getDialogSettings();

		// save person id
		int personIndex = fComboPerson.getSelectionIndex();
		if (personIndex <= 0) {
			settings.put(COMBO_PERSON_ID, -1);
		} else {
			final Long personId = fPeopleList.get(--personIndex).getPersonId();
			settings.put(COMBO_PERSON_ID, personId);
		}

		// save device id
		int deviceIndex = fComboDevice.getSelectionIndex();
		if (deviceIndex <= 0) {
			settings.put(COMBO_DEVICE_ID, -1);
		} else {
			final String deviceId = fDeviceList.get(--deviceIndex).deviceId;
			settings.put(COMBO_DEVICE_ID, deviceId);
		}

		// save port
		if (fIsPortListAvailable && fComboPorts.getSelectionIndex() != -1) {
			settings.put(COMBO_SERIAL_PORT, fComboPorts.getItem(fComboPorts.getSelectionIndex()));
		}

		// save auto save settings
		settings.put(CHECK_AUTO_SAVE, fCheckAutoSave.getSelection());
		settings.put(TEXT_AUTO_SAVE_PATH, fAutoSavePathEditor.getStringValue());

	}

	private void restoreDialogSettings() {

		final IDialogSettings settings = getDialogSettings();

		// restore person
		fComboPerson.select(0);
		Long savedPersonId;
		try {
			savedPersonId = settings.getLong(COMBO_PERSON_ID);
			if (savedPersonId != null) {
				int personIndex = 1;
				for (final TourPerson person : fPeopleList) {

					if (person.getPersonId() == savedPersonId) {
						fComboPerson.select(personIndex);
						break;
					}
					personIndex++;
				}
			}
		} catch (final NumberFormatException e) {
			// irgnore
		}

		// restore device
		final String savedDeviceId = settings.get(COMBO_DEVICE_ID);
		fComboDevice.select(0);
		if (savedDeviceId != null) {
			int deviceIndex = 1;
			for (final TourbookDevice device : fDeviceList) {

				if (device.deviceId.equals(savedDeviceId)) {
					fComboDevice.select(deviceIndex);
					break;
				}
				deviceIndex++;
			}
		}

		// restore port
		if (fIsPortListAvailable) {
			// select the port which is stored in the settings
			final String savedPort = settings.get(COMBO_SERIAL_PORT);

			if (savedPort != null) {
				int portIndex = 0;
				for (final String port : fPortList) {
					if (port.equalsIgnoreCase(savedPort)) {
						fComboPorts.select(portIndex);
						break;
					}
					portIndex++;
				}
			}
		}

		// restore auto save checkbox
		fCheckAutoSave.setSelection(settings.getBoolean(CHECK_AUTO_SAVE));
		fAutoSavePathEditor.setStringValue(settings.get(TEXT_AUTO_SAVE_PATH));

	}

	/**
	 * select device in the combo box
	 */
	private void selectPersonDevice(final TourPerson person) {

		int deviceIndex = 0;
		int selectIndex = 0;
		final String deviceId = person.getDeviceReaderId();

		if (deviceId == null) {
			fComboDevice.select(0);
		} else {
			for (final TourbookDevice device : fDeviceListWithDeviceImport) {
				if (deviceId.equals(device.deviceId)) {

					// skip first entry
					deviceIndex++;

					selectIndex = deviceIndex;
					break;
				}
				deviceIndex++;
			}

			/*
			 * when the device was not found or when the data can't be read from
			 * the device, select the "<Not Selected>" entry
			 */
			if (selectIndex == 0) {
				fComboDevice.select(0);
//				fComboDevice.setEnabled(true);
				fIsDeviceAvailable = false;
			} else {
				fComboDevice.select(selectIndex);
//				fComboDevice.setEnabled(false);
				fIsDeviceAvailable = true;
			}
		}
	}

	private boolean validatePage() {

		// validate device
		if (fComboDevice.getSelectionIndex() <= 0) {
			setPageComplete(false);
			setErrorMessage("Select a device");
			return false;
		}

		// validate ports
		if (!fIsPortListAvailable || fComboPorts.getSelectionIndex() == -1) {
			setPageComplete(false);
			setErrorMessage("COM port is required");
			return false;
		}

		// validate autosave path
		if (fCheckAutoSave.getSelection() && !fAutoSavePathEditor.isValid()) {
			setPageComplete(false);
			setErrorMessage("Path is invalid");
			return false;
		}

		setPageComplete(true);
		setErrorMessage(null);
		return true;
	}

	/**
	 * @return Return the device which is selected in the device list or
	 *         <code>null</code> when no device is selected
	 */
	public TourbookDevice getSelectedDevice() {

		int deviceIndex = fComboDevice.getSelectionIndex();
		if (deviceIndex <= 0) {
			return null;
		} else {
			return fDeviceListWithDeviceImport.get(--deviceIndex);
		}
	}

}
