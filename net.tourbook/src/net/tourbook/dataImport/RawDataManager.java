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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import net.tourbook.data.TourData;
import net.tourbook.database.TourDatabase;
import net.tourbook.device.DeviceData;
import net.tourbook.device.DeviceManager;
import net.tourbook.device.TourbookDevice;
import net.tourbook.plugin.TourbookPlugin;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;

public class RawDataManager {

	public static final String		TEMP_RAW_DATA_FILE	= "temp-device-data.txt";

	private static RawDataManager	instance			= null;

	/**
	 * contains the device which was used to get the fields:
	 * <code>fDeviceData</code>, <code>fTourData</code>
	 */
	private TourbookDevice			fDevice;

	/**
	 * contains the device data imported from the device/file
	 */
	private DeviceData				fDeviceData			= new DeviceData();

	/**
	 * contains the tour data imported from the device/file
	 */
	private ArrayList<TourData>		fTourData			= new ArrayList<TourData>();

	/**
	 * when set to <code>true</code> the data are imported from a device and
	 * stored in the temp raw data file
	 */
	private boolean					fIsDeviceImport;

	private String					fImportFileName;

	private RawDataManager() {}

	public static RawDataManager getInstance() {
		if (instance == null) {
			instance = new RawDataManager();
		}
		return instance;
	}

	/**
	 * @return Returns the file to the temp data file
	 */
	public static String getTempDataFileName() {

		return TourbookPlugin
				.getDefault()
				.getStateLocation()
				.append(TEMP_RAW_DATA_FILE)
				.toFile()
				.getAbsolutePath();
	}

	public DeviceData getDeviceData() {
		return fDeviceData;
	}

	public ArrayList<TourData> getTourData() {
		return fTourData;
	}

	public TourbookDevice getDevice() {
		return fDevice;
	}

	/**
	 * Import the raw data from a file and save the imported data in the fields
	 * <code>fDeviceData</code> and <code>fTourData</code>
	 * 
	 * @param fileName
	 * @param isDeviceImport
	 * @return Returns <code>true</code> when the import was successfully
	 */
	public boolean importRawData(String fileName) {

		ArrayList<TourbookDevice> deviceList = DeviceManager.getDeviceList();

		// find the file extension in the filename
		int dotPos = fileName.lastIndexOf(".");
		if (dotPos == -1) {
			return false;
		}
		String extension = fileName.substring(dotPos + 1);

		boolean isDataImported = false;

		/*
		 * try to import from all devices which have the same extension
		 */
		for (TourbookDevice device : deviceList) {

			if (device.fileExtension.equalsIgnoreCase(extension)) {

				// device file extension was found in the filename extension

				if (importRawDataFromFile(device, fileName)) {
					return true;
				}
			}
		}

		/*
		 * when data has not imported yet, try all available devices without
		 * checking the file extension
		 */
		for (TourbookDevice device : deviceList) {
			if (importRawDataFromFile(device, fileName)) {
				return true;
			}
		}

		if (!isDataImported) {
			showMsgBoxInvalidFormat(fileName);
		}

		return false;
	}

	/**
	 * import the raw data for the device
	 * 
	 * @param device
	 * @param fileName
	 * @return
	 */
	private boolean importRawDataFromFile(TourbookDevice device, String fileName) {

		if (device.validateRawData(fileName)) {

			// file contains valid raw data for the raw data reader

			if (device.processDeviceData(fileName, fDeviceData, fTourData)) {

				// keep the device and filename for the successful import
				fDevice = device;
				fImportFileName = fileName;

				fIsDeviceImport = RawDataManager.getTempDataFileName().equals(fileName);

				updatePersonInRawData();

				return true;
			} else {
				// import was not successful
				fDevice = null;
				fImportFileName = null;
			}
		}

		return false;
	}

	private void showMsgBoxInvalidFormat(String fileName) {
		{
			MessageBox msgBox = new MessageBox(
					Display.getCurrent().getActiveShell(),
					SWT.ICON_ERROR | SWT.OK);
			msgBox.setMessage("Data format in \"" + fileName + "\" is not valid");
			msgBox.open();
		}
	}

	/**
	 * @return Returns <code>true</code> when the device/tour data has been
	 *         imported from a device, <code>false</code> when the data are
	 *         imported from a file
	 */
	public boolean isDeviceImport() {
		return fIsDeviceImport;
	}

	/**
	 * Set the device from which the tour data has been imported
	 * 
	 * @param importDevice
	 */
	public void setDevice(TourbookDevice importDevice) {
		fDevice = importDevice;
	}

	public void setDeviceData(DeviceData deviceData) {
		fDeviceData = deviceData;
	}

	/**
	 * set the status the data has been imported from a device
	 */
	public void setIsDeviceImport() {
		fIsDeviceImport = true;
	}

	/**
	 * set the status the data has been imported from a file
	 */
	// public void setIsFileImport() {
	// fIsDeviceImport = false;
	// }
	/**
	 * set the status for each tour if it is saved in the database
	 */
	public void OLDupdateTourSaveStatus() {

		String sqlString = "SELECT TOURID "
				+ ("FROM " + TourDatabase.TABLE_TOUR_DATA)
				+ (" WHERE TOURID = ?");

		try {

			Connection conn = TourDatabase.getInstance().getConnection();
			PreparedStatement statement = conn.prepareStatement(sqlString);

			for (TourData tourData : fTourData) {

				statement.setLong(1, tourData.getTourId());
				// tourData.fIsTourSavedInDb = statement.executeQuery().next();
			}

			conn.close();

		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Set the person in the current raw data, which owns the tour data
	 */
	public void updatePersonInRawData() {

		BusyIndicator.showWhile(Display.getCurrent(), new Runnable() {
			public void run() {

				EntityManager em = TourDatabase.getInstance().getEntityManager();

				if (em != null) {

					Query query = em.createQuery("SELECT TourData "
							+ ("FROM " + TourDatabase.TABLE_TOUR_DATA + " TourData ")
							+ (" WHERE tourId = :tourId"));

					for (TourData tourData : fTourData) {

						query.setParameter("tourId", tourData.getTourId());

						List peopleList = query.getResultList();
						if (peopleList.size() == 0) {
							tourData.setTourPerson(null);
						} else {
							tourData.setTourPerson(((TourData) peopleList.get(0)).getTourPerson());
						}
					}

					em.close();
				}
			}
		});
	}

	public String getImportFileName() {
		return fImportFileName;
	}

	public void setImportFileName(String fileName) {
		fImportFileName = fileName;
	}

}
