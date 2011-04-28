/*******************************************************************************
 * Copyright (C) 2005, 2011  Wolfgang Schramm and Contributors
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.Platform;

public class DeviceManager {

	public static final String			DEVICE_IS_NOT_SELECTED	= Messages.DeviceManager_Selection_device_is_not_selected;

	private static List<TourbookDevice>	_deviceList;
	private static List<ExternalDevice>	_externalDeviceList;

	/**
	 * Read devices from the extension registry which can import data
	 * 
	 * @return Returns a list with devices sorted by name
	 */
	@SuppressWarnings("unchecked")
	public static List<TourbookDevice> getDeviceList() {

		if (_deviceList == null) {

			_deviceList = readDeviceExtensions(TourbookPlugin.EXT_POINT_DEVICE_DATA_READER);

			// sort device list alphabetically
			Collections.sort(_deviceList, new Comparator<TourbookDevice>() {
				public int compare(final TourbookDevice o1, final TourbookDevice o2) {
					return o1.visibleName.compareTo(o2.visibleName);
				}
			});
		}

		return _deviceList;
	}

	/**
	 * Read external devices from the extension registry
	 * 
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static List<ExternalDevice> getExternalDeviceList() {

		if (_externalDeviceList == null) {

			_externalDeviceList = readDeviceExtensions(TourbookPlugin.EXT_POINT_EXTERNAL_DEVICE_DATA_READER);

			// sort device list alphabetically
			Collections.sort(_externalDeviceList, new Comparator<ExternalDevice>() {
				public int compare(final ExternalDevice o1, final ExternalDevice o2) {
					return o1.visibleName.compareTo(o2.visibleName);
				}
			});

		}
		return _externalDeviceList;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static ArrayList readDeviceExtensions(final String extensionPointName) {

		final ArrayList deviceList = new ArrayList();

		final IExtensionPoint extPoint = Platform.getExtensionRegistry()//
				.getExtensionPoint(TourbookPlugin.PLUGIN_ID, extensionPointName);

		if (extPoint != null) {

			for (final IExtension extension : extPoint.getExtensions()) {

				for (final IConfigurationElement configElement : extension.getConfigurationElements()) {

					if (configElement.getName().equalsIgnoreCase("device")) { //$NON-NLS-1$

						Object object;
						try {

							object = configElement.createExecutableExtension("class"); //$NON-NLS-1$

							if (object instanceof TourbookDevice) {

								final TourbookDevice device = (TourbookDevice) object;

								device.deviceId = configElement.getAttribute("id"); //$NON-NLS-1$
								device.visibleName = configElement.getAttribute("name"); //$NON-NLS-1$
								device.fileExtension = configElement.getAttribute("fileextension"); //$NON-NLS-1$

								final String extensionSortPriority = configElement
										.getAttribute("extensionSortPriority"); //$NON-NLS-1$

								if (extensionSortPriority != null) {
									try {
										device.extensionSortPriority = Integer.parseInt(extensionSortPriority);
									} catch (final Exception e) {
										// do nothing
									}
								}

								deviceList.add(device);
							}

							if (object instanceof ExternalDevice) {

								final ExternalDevice device = (ExternalDevice) object;

								device.deviceId = configElement.getAttribute("id"); //$NON-NLS-1$
								device.visibleName = configElement.getAttribute("name"); //$NON-NLS-1$

								deviceList.add(device);
							}

						} catch (final CoreException e) {
							e.printStackTrace();
						}
					}
				}
			}
		}

		return deviceList;
	}
}
