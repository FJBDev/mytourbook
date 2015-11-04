/*******************************************************************************
 * Copyright (C) 2005, 2015 Wolfgang Schramm and Contributors
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
import java.nio.file.FileStore;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;

import net.tourbook.Messages;
import net.tourbook.common.NIO;
import net.tourbook.common.UI;
import net.tourbook.common.util.Util;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Label;

/**
 * Manage combo box folder items.
 */
class HistoryItems {

	private static final String		NO_DEVICE_NAME				= "[?]";													//$NON-NLS-1$

	private static final int		COMBO_HISTORY_LENGTH		= 20;
	private static final String		COMBO_SEPARATOR				= "- - - - - - - - - - - - - - - - - - - - - - - - - - -";	//$NON-NLS-1$

	private LinkedHashSet<String>	_folderItems				= new LinkedHashSet<>();

	/** Contains paths with the device name and not the drive letter (only for Windows). */
	private LinkedHashSet<String>	_deviceNameItems			= new LinkedHashSet<>();

	private Label					_labelFolderInfo;
	private Combo					_combo;



	private String cleanupFolderDeviceName(final String deviceNameFolder) {

		final String cleanedDeviceNameFolder = deviceNameFolder.replace(
				Messages.Dialog_ImportConfig_Info_NoDeviceName,
				UI.EMPTY_STRING);

		return cleanedDeviceNameFolder;
	}

	private String convertTo_DeviceNameFolder(final String osFolder) {

		final Path newPath = Paths.get(osFolder);

		final String deviceName = getDeviceName(newPath);

		if (deviceName == null) {
			return null;
		}

		final String deviceFolderName = createDeviceNameFolder(newPath, deviceName);

		return deviceFolderName;
	}

	private String createDeviceNameFolder(final Path folderPath, final String deviceName) {

		final int nameCount = folderPath.getNameCount();
		final Comparable<?> subPath = nameCount > 0 ? folderPath.subpath(0, nameCount) : UI.EMPTY_STRING;

		String deviceFolder = null;

		// construct device name folder
		if (deviceName == null) {

			deviceFolder = NO_DEVICE_NAME + File.separator + subPath;

		} else {

			if (deviceName.trim().length() == 0) {

				deviceFolder = '[' + Messages.Dialog_ImportConfig_Info_NoDeviceName + ']' + File.separator + subPath;

			} else {

				deviceFolder = '[' + deviceName + ']' + File.separator + subPath;
			}
		}

		return deviceFolder;
	}

	private void fillControls(final String newFolder, final String newDeviceNameFolder, final String selectedFolder) {

		_combo.removeAll();

		String folderText = UI.EMPTY_STRING;
		String folderInfo = UI.EMPTY_STRING;

		if (selectedFolder != null) {

			folderText = selectedFolder;

			folderInfo = NIO.isDeviceNameFolder(selectedFolder)
					? newFolder == null ? UI.EMPTY_STRING : newFolder
					: newDeviceNameFolder == null ? UI.EMPTY_STRING : newDeviceNameFolder;
		}

		_labelFolderInfo.setText(folderInfo);
		_combo.setText(folderText);

		boolean isAdded = false;

		/*
		 * Combo items
		 */
		if (newFolder != null) {
			_combo.add(newFolder);
			isAdded = true;
		}

		if (newDeviceNameFolder != null) {
			_combo.add(newDeviceNameFolder);
			isAdded = true;
		}

		if (_deviceNameItems.size() > 0) {

			if (isAdded) {
				_combo.add(COMBO_SEPARATOR);
			}

			isAdded = true;

			for (final String deviceFolder : reverseHistory(_deviceNameItems)) {
				_combo.add(deviceFolder);
			}
		}

		if (_folderItems.size() > 0) {

			if (isAdded) {
				_combo.add(COMBO_SEPARATOR);
			}

			isAdded = true;

			for (final String driveFolder : reverseHistory(_folderItems)) {
				_combo.add(driveFolder);
			}
		}
	}

	/**
	 * @param deviceRoot
	 * @return Returns the device name for the drive or <code>null</code> when not available
	 */
	private String getDeviceName(final Path path) {

		/*
		 * This feature is available only for windows.
		 */
		if (!UI.IS_WIN) {
			return null;
		}

		final Path root = path.getRoot();

		if (root == null) {
			return null;
		}

		String deviceDrive = root.toString();
		deviceDrive = deviceDrive.substring(0, 2);

		final Iterable<FileStore> fileStores = FileSystems.getDefault().getFileStores();

		for (final FileStore store : fileStores) {

			final String drive = NIO.parseDriveLetter(store);

			if (deviceDrive.equalsIgnoreCase(drive)) {

				return store.name();
			}
		}

		return null;
	}

	String getOSPath(final String defaultFolder, final String configFolder) {

		String osPath = null;

		if (defaultFolder != null) {
			osPath = NIO.convertToOSPath(defaultFolder);
		}

		if (osPath == null) {
			osPath = NIO.convertToOSPath(configFolder);
		}

		return osPath;
	}

	private void keepOldPathInHistory() {

		final String oldFolder = _combo.getText().trim();

		if (oldFolder.length() == 0) {
			return;
		}

		if (oldFolder.trim().startsWith(NIO.DEVICE_FOLDER_NAME_START)) {

			// this is a device name folder

			final String cleanHistoryItem = cleanupFolderDeviceName(oldFolder);
			_deviceNameItems.remove(cleanHistoryItem);
			_deviceNameItems.add(cleanHistoryItem);

		} else {

			_folderItems.remove(oldFolder);
			_folderItems.add(oldFolder);
		}
	}

	/**
	 * A new folder is selected in the system folder dialog.
	 * 
	 * @param newFolder
	 */
	void onSelectFolderInDialog(final String newFolder) {

		final Path newPath = Paths.get(newFolder);

		final String deviceName = getDeviceName(newPath);
		final String deviceNameFolder = createDeviceNameFolder(newPath, deviceName);

		updateModel(newFolder, deviceNameFolder);
		fillControls(newFolder, deviceNameFolder, newFolder);
	}

	/**
	 * Remove item from history.
	 * 
	 * @param text
	 */
	void removeFromHistory(final String itemText) {

		if (NIO.isDeviceNameFolder(itemText)) {

			// this is a device name folder

			_deviceNameItems.remove(itemText);

		} else {

			_folderItems.remove(itemText);
		}

		fillControls(null, null, null);
	}

	void restoreState(final String[] restoredFolderItems, final String[] restoredDeviceItems, final String configFolder) {

		final boolean isDeviceNameFolder = NIO.isDeviceNameFolder(configFolder);

		if (configFolder.trim().length() > 0) {

			if (isDeviceNameFolder) {

				// this is a device folder name

				_deviceNameItems.add(cleanupFolderDeviceName(configFolder));

			} else {

				_folderItems.add(configFolder);
			}
		}

		_folderItems.addAll(Arrays.asList(restoredFolderItems));
		_deviceNameItems.addAll(Arrays.asList(restoredDeviceItems));

		String itemFolder = null;
		String deviceNameFolder = null;

		if (isDeviceNameFolder) {

			// this is a device name folder

			itemFolder = NIO.convertToOSPath(configFolder);
			deviceNameFolder = configFolder;

		} else {

			itemFolder = configFolder;
			deviceNameFolder = convertTo_DeviceNameFolder(configFolder);
		}

		fillControls(itemFolder, deviceNameFolder, configFolder);
	}

	private String[] reverseHistory(final LinkedHashSet<String> folderHistory) {

		final String[] folterItems = folderHistory.toArray(new String[folderHistory.size()]);
		final String[] reversedArray = (String[]) Util.arrayReverse(folterItems);

		return reversedArray;
	}

	void saveState(	final IDialogSettings state,
					final String stateFolderHistoryItems,
					final String stateDeviceHistoryItems) {

		state.put(stateFolderHistoryItems, _folderItems.toArray(new String[_folderItems.size()]));
		state.put(stateDeviceHistoryItems, _deviceNameItems.toArray(new String[_deviceNameItems.size()]));
	}

	void setControls(final Combo comboFolder, final Label lblFolderPath) {

		_combo = comboFolder;
		_labelFolderInfo = lblFolderPath;
	}

	private void updateHistory(final LinkedHashSet<String> historyItems, final String newItem) {

		if (newItem == null || newItem.trim().length() == 0) {
			// there is no new item
			return;
		}

		// move the new folder path to the top of the history
		final String cleanHistoryItem = cleanupFolderDeviceName(newItem);
		historyItems.remove(cleanHistoryItem);
		historyItems.add(cleanHistoryItem);

		if (historyItems.size() < COMBO_HISTORY_LENGTH) {
			return;
		}

		// force history length
		final ArrayList<String> removedItems = new ArrayList<>();

		int numFolder = 0;

		for (final String folderItem : historyItems) {
			if (++numFolder < COMBO_HISTORY_LENGTH) {
				continue;
			} else {
				removedItems.add(folderItem);
			}
		}

		historyItems.removeAll(removedItems);
	}

	private void updateModel(final String folderPath, final String deviceNamePath) {

		keepOldPathInHistory();

		updateHistory(_folderItems, folderPath);
		updateHistory(_deviceNameItems, deviceNamePath);
	}

	void validateModifiedPath(final DialogImportConfig dialogImportConfig) {

		boolean isFolderValid = false;

		final String modifiedFolder = _combo.getText().trim();

		if (modifiedFolder.length() == 0) {

			isFolderValid = true;
			_labelFolderInfo.setText(UI.EMPTY_STRING);

		} else {

			final String cleanedFolderName = cleanupFolderDeviceName(modifiedFolder);

			final String osFolder = NIO.convertToOSPath(cleanedFolderName);

			if (osFolder != null) {

				try {

					final Path osPath = Paths.get(osFolder);

					isFolderValid = Files.exists(osPath);

					if (isFolderValid) {

						if (NIO.isDeviceNameFolder(cleanedFolderName)) {

							// this is a device folder name

							_labelFolderInfo.setText(osFolder);

						} else {

							final String deviceFolder = convertTo_DeviceNameFolder(osFolder);

							if (deviceFolder == null) {
								isFolderValid = false;
							} else {
								_labelFolderInfo.setText(deviceFolder);
							}
						}
					}

				} catch (final Exception e) {
					isFolderValid = false;
				}
			}
		}

		if (isFolderValid) {

			dialogImportConfig.setErrorMessage(null);

		} else {

			_labelFolderInfo.setText(UI.EMPTY_STRING);
			dialogImportConfig.setErrorMessage(Messages.Dialog_ImportConfig_Error_FolderIsInvalid);
		}
	}
}
