/*******************************************************************************
 * Copyright (C) 2005, 2010  Wolfgang Schramm and Contributors
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
package net.tourbook.importdata;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.ui.views.rawData.RawDataView;
import net.tourbook.util.Util;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Display;

public class WizardImportData extends Wizard {

	private static final String			DIALOG_SETTINGS_SECTION		= "WizardImportData";			//$NON-NLS-1$

	public static final String			SYSPROPERTY_IMPORT_PERSON	= "mytourbook.import.person";	//$NON-NLS-1$

	private WizardPageImportSettings	_importSettings;
	private final List<File>			_receivedFiles				= new ArrayList<File>();

	/**
	 * contains the device which is used to read the data from it
	 */
	private ExternalDevice				_importDevice;

	private IRunnableWithProgress		_runnableReceiveData;

	WizardImportData() {

		setDialogSettings();
		setNeedsProgressMonitor(true);
	}

	@Override
	public void addPages() {

		_importSettings = new WizardPageImportSettings("import-settings"); //$NON-NLS-1$
		addPage(_importSettings);

	}

	@Override
	public boolean performFinish() {

		if (_importSettings.validatePage() == false) {
			return false;
		}

		receiveData();

		_importSettings.persistDialogSettings();

		return true;
	}

	/**
	 * @return Returns <code>true</code> when the import was successful
	 */
	private boolean receiveData() {

		final Combo comboPorts = _importSettings._cboPorts;

		if (comboPorts.isDisposed()) {
			return false;
		}

		/*
		 * get port name
		 */
		final int selectedComPort = comboPorts.getSelectionIndex();
		if (selectedComPort == -1) {
			return false;
		}

		final String portName = comboPorts.getItem(selectedComPort);

		/*
		 * when the Cancel button is pressed multiple times, the app calls this function each time
		 */
		if (_runnableReceiveData != null) {
			return false;
		}

		/*
		 * set the device which is used to read the data
		 */
		_importDevice = _importSettings.getSelectedDevice();
		if (_importDevice == null) {
			return false;
		}

		final RawDataManager rawDataManager = RawDataManager.getInstance();
		rawDataManager.setImportCanceled(false);

		/*
		 * receive data from the device
		 */
		try {
			_runnableReceiveData = _importDevice.createImportRunnable(portName, _receivedFiles);
			getContainer().run(true, true, _runnableReceiveData);
		} catch (final InvocationTargetException e) {
			e.printStackTrace();
		} catch (final InterruptedException e) {
			e.printStackTrace();
		}

		if (_receivedFiles.size() == 0 || _importDevice.isImportCanceled()) {
			// data has not been received or the user canceled the import
			return true;
		}

		// import received files
		final FileCollisionBehavior fileCollision = new FileCollisionBehavior();
		for (final File inFile : _receivedFiles) {
			rawDataManager.importRawData(
					inFile,
					_importSettings._pathEditor.getStringValue(),
					_importDevice.buildNewFileNames,
					fileCollision);
		}

		rawDataManager.updateTourDataFromDb(null);

		// show imported data in the raw data view
		final RawDataView importView = (RawDataView) Util.showView(RawDataView.ID);

		if (importView != null) {
			importView.reloadViewer();
		}

		return true;
	}

	public void setAutoDownload() {

		getContainer().getShell().addShellListener(new ShellAdapter() {
			@Override
			public void shellActivated(final ShellEvent e) {

				Display.getCurrent().asyncExec(new Runnable() {
					public void run() {

						// start downloading
						final boolean importResult = receiveData();

						_importSettings.persistDialogSettings();

						if (importResult) {
							getContainer().getShell().close();
						}
					}
				});
			}
		});

	}

	public void setDialogSettings() {

		final IDialogSettings pluginSettings = TourbookPlugin.getDefault().getDialogSettings();
		IDialogSettings wizardSettings = pluginSettings.getSection(DIALOG_SETTINGS_SECTION);

		if (wizardSettings == null) {
			wizardSettings = pluginSettings.addNewSection(DIALOG_SETTINGS_SECTION);
		}

		super.setDialogSettings(wizardSettings);
	}

}
