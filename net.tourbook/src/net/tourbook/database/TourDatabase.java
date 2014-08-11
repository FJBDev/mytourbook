/*******************************************************************************
 * Copyright (C) 2005, 2014  Wolfgang Schramm and Contributors
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
package net.tourbook.database;

import java.beans.PropertyVetoException;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;
import javax.persistence.Query;

import net.tourbook.Messages;
import net.tourbook.application.MyTourbookSplashHandler;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.Util;
import net.tourbook.data.SharedMarker;
import net.tourbook.data.TourBike;
import net.tourbook.data.TourData;
import net.tourbook.data.TourMarker;
import net.tourbook.data.TourPerson;
import net.tourbook.data.TourPersonHRZone;
import net.tourbook.data.TourPhoto;
import net.tourbook.data.TourReference;
import net.tourbook.data.TourTag;
import net.tourbook.data.TourTagCategory;
import net.tourbook.data.TourType;
import net.tourbook.data.TourWayPoint;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tag.TagCollection;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.TourTypeFilter;
import net.tourbook.ui.UI;

import org.apache.derby.drda.NetworkServerControl;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IPropertyListener;
import org.eclipse.ui.PlatformUI;
import org.joda.time.DateTime;

import com.mchange.v2.c3p0.ComboPooledDataSource;

public class TourDatabase {

	/**
	 * version for the database which is required that the tourbook application works successfully
	 */
	private static final int						TOURBOOK_DB_VERSION							= 25;										// 14.?

//	private static final int						TOURBOOK_DB_VERSION							= 25;	// 14.?
//	private static final int						TOURBOOK_DB_VERSION							= 24;	// 14.7
//	private static final int						TOURBOOK_DB_VERSION							= 23;	// 13.2.0
//	private static final int						TOURBOOK_DB_VERSION							= 22;	// 12.12.0
//	private static final int						TOURBOOK_DB_VERSION							= 21;	// 12.1.1
//	private static final int						TOURBOOK_DB_VERSION							= 20;	// 12.1
//	private static final int						TOURBOOK_DB_VERSION							= 19;	// 11.8
//	private static final int						TOURBOOK_DB_VERSION							= 18;	// 11.8
//	private static final int						TOURBOOK_DB_VERSION							= 17;	// 11.8
//	private static final int						TOURBOOK_DB_VERSION							= 16;	// 11.8
//	private static final int						TOURBOOK_DB_VERSION							= 15;	// 11.8
//	private static final int						TOURBOOK_DB_VERSION							= 14;	// 11.3
//	private static final int						TOURBOOK_DB_VERSION							= 13;	// 10.11
//	private static final int						TOURBOOK_DB_VERSION							= 12;	// 10.9.1
//	private static final int						TOURBOOK_DB_VERSION							= 11;	// 10.7.0 - 11-07-2010
//	private static final int						TOURBOOK_DB_VERSION							= 10;	// 10.5.0 not released
//	private static final int						TOURBOOK_DB_VERSION							= 9;	// 10.3.0
//	private static final int						TOURBOOK_DB_VERSION							= 8;	// 10.2.1 Mod by Kenny
//	private static final int						TOURBOOK_DB_VERSION							= 7;	// 9.01
//	private static final int						TOURBOOK_DB_VERSION							= 6;	// 8.12
//	private static final int						TOURBOOK_DB_VERSION							= 5;	// 8.11

	public static boolean							IS_POST_UPDATE_019_to_020					= false;

	private static final String						PERSISTENCE_UNIT_NAME						= "tourdatabase";							//$NON-NLS-1$

	private static final int						MAX_TRIES_TO_PING_SERVER					= 10;

	/**
	 * <b> !!! Table names are set to uppercase otherwise conn.getMetaData().getColumns() would not
	 * work !!! </b>
	 */
	public static final String						TABLE_SCHEMA								= "USER";									//$NON-NLS-1$

	private static final String						TABLE_DB_VERSION							= "DBVERSION";								//$NON-NLS-1$
	//
	public static final String						TABLE_SHARED_MARKER							= "SHAREDMARKER";							//$NON-NLS-1$
	public static final String						TABLE_TOUR_BIKE								= "TOURBIKE";								//$NON-NLS-1$
	public static final String						TABLE_TOUR_COMPARED							= "TOURCOMPARED";							//$NON-NLS-1$
	public static final String						TABLE_TOUR_DATA								= "TOURDATA";								//$NON-NLS-1$
	public static final String						TABLE_TOUR_MARKER							= "TOURMARKER";							//$NON-NLS-1$
	public static final String						TABLE_TOUR_PERSON							= "TOURPERSON";							//$NON-NLS-1$
	public static final String						TABLE_TOUR_PERSON_HRZONE					= "TOURPERSONHRZONE";						//$NON-NLS-1$
	public static final String						TABLE_TOUR_PHOTO							= "TOURPHOTO";								//$NON-NLS-1$
	public static final String						TABLE_TOUR_REFERENCE						= "TOURREFERENCE";							//$NON-NLS-1$
//	public static final String						TABLE_TOUR_SIGN								= "TOURSIGN";								//$NON-NLS-1$
//	public static final String						TABLE_TOUR_SIGN_CATEGORY					= "TOURSIGNCATEGORY";						//$NON-NLS-1$
	public static final String						TABLE_TOUR_TAG								= "TOURTAG";								//$NON-NLS-1$
	public static final String						TABLE_TOUR_TAG_CATEGORY						= "TOURTAGCATEGORY";						//$NON-NLS-1$
	public static final String						TABLE_TOUR_TYPE								= "TOURTYPE";								//$NON-NLS-1$
	public static final String						TABLE_TOUR_WAYPOINT							= "TOURWAYPOINT";							//$NON-NLS-1$
	//
	public static final String						JOINTABLE__TOURDATA__SHAREDMARKER			= TABLE_TOUR_DATA
																										+ "_" + TABLE_SHARED_MARKER;		//$NON-NLS-1$
	public static final String						JOINTABLE__TOURDATA__TOURTAG				= TABLE_TOUR_DATA
																										+ "_" + TABLE_TOUR_TAG;			//$NON-NLS-1$
	public static final String						JOINTABLE__TOURTAGCATEGORY_TOURTAG			= TABLE_TOUR_TAG_CATEGORY
																										+ "_" + TABLE_TOUR_TAG;			//$NON-NLS-1$
	public static final String						JOINTABLE__TOURTAGCATEGORY_TOURTAGCATEGORY	= TABLE_TOUR_TAG_CATEGORY
																										+ "_" + TABLE_TOUR_TAG_CATEGORY;	//$NON-NLS-1$
	/*
	 * Tables which never have been used, they are dropped in db version 24
	 */
	private static final String						JOINTABLE__TOURDATA__TOURMARKER				= TABLE_TOUR_DATA
																										+ "_" + TABLE_TOUR_MARKER;			//$NON-NLS-1$
	private static final String						JOINTABLE__TOURDATA__TOURPHOTO				= TABLE_TOUR_DATA
																										+ "_" + TABLE_TOUR_PHOTO;			//$NON-NLS-1$
	private static final String						JOINTABLE__TOURDATA__TOURREFERENCE			= TABLE_TOUR_DATA
																										+ "_" + TABLE_TOUR_REFERENCE;		//$NON-NLS-1$
	private static final String						JOINTABLE__TOURDATA__TOURWAYPOINT			= TABLE_TOUR_DATA
																										+ "_" + TABLE_TOUR_WAYPOINT;		//$NON-NLS-1$
	private static final String						JOINTABLE__TOURPERSON__TOURPERSON_HRZONE	= TABLE_TOUR_PERSON
																										+ "_" + TABLE_TOUR_PERSON_HRZONE;	//$NON-NLS-1$
	// never used tables, is needed to drop them
	private final static String						TABLE_TOUR_CATEGORY							= "TourCategory";							//$NON-NLS-1$
	private final static String						TABLE_TOURCATEGORY__TOURDATA				= TABLE_TOUR_CATEGORY
																										+ "_" + TABLE_TOUR_DATA;			//$NON-NLS-1$
	/**
	 * contains <code>-1</code> which is the Id for a not saved entity
	 */
	public static final int							ENTITY_IS_NOT_SAVED							= -1;
	//
	private static final String						ENTITY_ID_BIKE								= "BikeID";								//$NON-NLS-1$
	private static final String						ENTITY_ID_COMPARED							= "ComparedID";							//$NON-NLS-1$
	private static final String						ENTITY_ID_HR_ZONE							= "HrZoneID";								//$NON-NLS-1$
	private static final String						ENTITY_ID_MARKER							= "MarkerID";								//$NON-NLS-1$
	private static final String						ENTITY_ID_PERSON							= "PersonID";								//$NON-NLS-1$
	private static final String						ENTITY_ID_PHOTO								= "PhotoID";								//$NON-NLS-1$
	private static final String						ENTITY_ID_REF								= "RefID";									//$NON-NLS-1$
//	private static final String						ENTITY_ID_SIGN								= "SignID";								//$NON-NLS-1$
	private static final String						ENTITY_ID_SHARED_MARKER						= "SharedMarkerID";						//$NON-NLS-1$
	private static final String						ENTITY_ID_TAG								= "TagID";									//$NON-NLS-1$
	private static final String						ENTITY_ID_TAG_CATEGORY						= "TagCategoryID";							//$NON-NLS-1$
	private static final String						ENTITY_ID_TOUR								= "TourID";								//$NON-NLS-1$
	private static final String						ENTITY_ID_TYPE								= "TypeID";								//$NON-NLS-1$
	private static final String						ENTITY_ID_WAY_POINT							= "WayPointID";							//$NON-NLS-1$
	//
	private static final String						KEY_BIKE									= TABLE_TOUR_BIKE
																										+ "_" + ENTITY_ID_BIKE;			//$NON-NLS-1$
	private static final String						KEY_PERSON									= TABLE_TOUR_PERSON
																										+ "_" + ENTITY_ID_PERSON;			//$NON-NLS-1$
//	private static final String						KEY_SIGN									= TABLE_TOUR_SIGN
//																										+ "_" + ENTITY_ID_SIGN;			//$NON-NLS-1$
	private static final String						KEY_SHARED_MARKER							= TABLE_SHARED_MARKER
																										+ "_" + ENTITY_ID_SHARED_MARKER;	//$NON-NLS-1$
	private static final String						KEY_TAG										= TABLE_TOUR_TAG
																										+ "_" + ENTITY_ID_TAG;				//$NON-NLS-1$
	private static final String						KEY_TAG_CATEGORY							= TABLE_TOUR_TAG_CATEGORY
																										+ "_" + ENTITY_ID_TAG_CATEGORY;	//$NON-NLS-1$
	public static final String						KEY_TOUR									= TABLE_TOUR_DATA
																										+ "_" + ENTITY_ID_TOUR;			//$NON-NLS-1$
	private static final String						KEY_TYPE									= TABLE_TOUR_TYPE
																										+ "_" + ENTITY_ID_TYPE;			//$NON-NLS-1$
	//
	private static volatile TourDatabase			_instance;

	private static ArrayList<TourType>				_activeTourTypes;

	private static volatile ArrayList<TourType>		_tourTypes;

	/**
	 * Key is tag ID.
	 */
	private static volatile HashMap<Long, TourTag>	_tourTags;

	/**
	 * Key is category ID or <code>-1</code> for the root.
	 */
	private static HashMap<Long, TagCollection>		_tagCollections								= new HashMap<Long, TagCollection>();

	/*
	 * cached distinct fields
	 */
	private static TreeSet<String>					_dbTourTitles;

	private static TreeSet<String>					_dbTourStartPlace;
	private static TreeSet<String>					_dbTourEndPlace;
	private static TreeSet<String>					_dbTourMarkerNames;

	private static final IPreferenceStore			_prefStore									= TourbookPlugin
																										.getPrefStore();

	private boolean									_isTableChecked;
	private boolean									_isVersionChecked;

	private final ListenerList						_propertyListeners							= new ListenerList(
																										ListenerList.IDENTITY);

	private final String							_databasePath								= Platform
																										.getInstanceLocation()
																										.getURL()
																										.getPath()
																										+ "derby-database";				//$NON-NLS-1$

	private boolean									_isSQLUpdateError							= false;

	/**
	 * Database version before a db update is performed
	 */
	private int										_dbVersionBeforeUpdate;

	private static String							DERBY_DRIVER_CLASS;

	private static String							DERBY_URL;
	private static final String						DERBY_URL_CREATE_TRUE						= ";create=true";							//$NON-NLS-1$
	private boolean									_isDerbyEmbedded;

	private static NetworkServerControl				_server;

	private static volatile EntityManagerFactory	_emFactory;

	private static volatile ComboPooledDataSource	_pooledDataSource;
	{
		// set storage location for the database
		System.setProperty("derby.system.home", _databasePath); //$NON-NLS-1$

// set derby debug properties
//		System.setProperty("derby.language.logQueryPlan", "true"); //$NON-NLS-1$
//		System.setProperty("derby.language.logStatementText", "true"); //$NON-NLS-1$
	}

	private static final Object						DB_LOCK										= new Object();

	public static final double						DEFAULT_DOUBLE								= 1E-300;									//Float.MIN_VALUE;
	public static final double						DEFAULT_FLOAT								= 1E-40;

	private static final String						DOUBLE_MIN_VALUE;
	private static final String						FLOAT_MIN_VALUE;
	static {

//		!ENTRY net.tourbook.common 4 0 2014-07-30 11:05:18.419
//		!MESSAGE ALTER TABLE TOURMARKER	ADD COLUMN	latitude DOUBLE DEFAULT 4.9E-324
//
//		!ENTRY net.tourbook.common 4 0 2014-07-30 11:05:18.440
//		!MESSAGE SQLException
//
//		SQLState: 22003
//		Severity: 30000
//		Message: The resulting value is outside the range for the data type DOUBLE.
//
////////////////////////////////////////////////////////////////////////////////////////
//
//		Derby Limitations
//
//		DOUBLE value ranges:
//		� Smallest DOUBLE value: -1.79769E+308
//		� Largest DOUBLE value: 1.79769E+308
//		� Smallest positive DOUBLE value: 2.225E-307
//		� Largest negative DOUBLE value: -2.225E-307
//
//		DOUBLE_MIN_VALUE = (new Double(Double.MIN_VALUE)).toString();
//
		DOUBLE_MIN_VALUE = (new Double(DEFAULT_DOUBLE)).toString();
		FLOAT_MIN_VALUE = (new Float(DEFAULT_FLOAT)).toString();
	}

	/**
	 * SQL utilities.
	 */
	private static class SQL {

		@SuppressWarnings("unused")
		private static void AddCol_BigInt(	final Statement stmt,
											final String table,
											final String columnName,
											final String defaultValue) throws SQLException {

			final String sql = ""// 															//$NON-NLS-1$
					+ "ALTER TABLE " + table //													//$NON-NLS-1$
					+ "	ADD COLUMN	" + columnName + " BIGINT DEFAULT " + defaultValue; //		//$NON-NLS-1$ //$NON-NLS-2$

			exec(stmt, sql);

			return;
		}

		private static void AddCol_Double(	final Statement stmt,
											final String table,
											final String columnName,
											final String defaultValue) throws SQLException {

			final String sql = ""// 															//$NON-NLS-1$
					+ "ALTER TABLE " + table //													//$NON-NLS-1$
					+ "	ADD COLUMN	" + columnName + " DOUBLE DEFAULT " + defaultValue; //		//$NON-NLS-1$ //$NON-NLS-2$

			exec(stmt, sql);

			return;
		}

		private static void AddCol_Float(	final Statement stmt,
											final String table,
											final String columnName,
											final String defaultValue) throws SQLException {

			final String sql = ""// 															//$NON-NLS-1$
					+ "ALTER TABLE " + table //													//$NON-NLS-1$
					+ "	ADD COLUMN	" + columnName + " FLOAT DEFAULT  " + defaultValue; //		//$NON-NLS-1$ //$NON-NLS-2$

			exec(stmt, sql);

			return;
		}

		/**
		 * @param stmt
		 * @param table
		 * @param columnName
		 * @param defaultValue
		 *            Default value.
		 * @throws SQLException
		 */
		private static void AddCol_Int(	final Statement stmt,
										final String table,
										final String columnName,
										final String defaultValue) throws SQLException {

			final String sql = ""// 															//$NON-NLS-1$
					+ "ALTER TABLE " + table //													//$NON-NLS-1$
					+ "	ADD COLUMN	" + columnName + " INTEGER DEFAULT " + defaultValue; //		//$NON-NLS-1$ //$NON-NLS-2$

			exec(stmt, sql);

			return;
		}

		/**
		 * Creates a SQL statement to add a column for VARCHAR.
		 * 
		 * @param stmt
		 * @param table
		 * @param columnName
		 * @param columnWidth
		 * @return Returns this sql statement:
		 *         <p>
		 *         <code>
		 *         "ALTER TABLE " + table + " ADD COLUMN	" + columnName + " " + " VARCHAR(" + columnWidth + ")\n"
		 *         </code>
		 * @throws SQLException
		 */
		private static void AddCol_VarCar(	final Statement stmt,
											final String table,
											final String columnName,
											final int columnWidth) throws SQLException {

			final String sql = ""// //$NON-NLS-1$
					+ ("ALTER TABLE " + table) //$NON-NLS-1$
					+ ("	ADD COLUMN	" + columnName + "	VARCHAR(" + columnWidth + ")\n"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

			exec(stmt, sql);

			return;
		}

		private static void AlterCol_VarChar_Width(	final Statement stmt,
													final String table,
													final String field,
													final int newWidth) throws SQLException {

			final String sql = "" // //$NON-NLS-1$
					+ ("ALTER TABLE " + table + "\n") //$NON-NLS-1$ //$NON-NLS-2$
					+ ("	ALTER COLUMN " + field + "\n") //$NON-NLS-1$ //$NON-NLS-2$
					+ ("	SET DATA TYPE	VARCHAR(" + newWidth + ")\n"); //$NON-NLS-1$ //$NON-NLS-2$

			exec(stmt, sql);

			return;
		}

		private static void Cleanup_DropConstraint(	final Statement stmt,
													final String tableName,
													final String constraintName) throws SQLException {

			try {

				exec(stmt, "ALTER TABLE " + tableName + " DROP CONSTRAINT " + constraintName); //$NON-NLS-1$ //$NON-NLS-2$

			} catch (final SQLException e) {

				final String sqlState = e.getSQLState();
				if (sqlState.equals("42X86")) { //$NON-NLS-1$

					// Caused by: ERROR 42X86: ALTER TABLE failed. There is no constraint 'USER.FK_TOURDATA_TOURTAG_TOURTAG_TAGID' on table '"USER"."TOURDATA_TOURTAG"'.

					/*
					 * Ignore not existing constraints
					 */
					StatusUtil.log(e);

				} else {
					throw e;
				}
			}
		}

		private static void Cleanup_DropTable(final Statement stmt, final String tableName) throws SQLException {

			try {

				exec(stmt, "DROP TABLE " + tableName); //$NON-NLS-1$

			} catch (final SQLException e) {

				final String sqlState = e.getSQLState();
				if (sqlState.equals("42Y55")) { //$NON-NLS-1$

					// Caused by: ERROR 42Y55: 'DROP TABLE' cannot be performed on 'TOURCATEGORY' because it does not exist.

					/*
					 * This case occured because table TOURCATEGORY was created until version 1.6
					 * but do not exist in later versions.
					 */
					StatusUtil.log(e);

				} else {
					throw e;
				}
			}
		}

		/**
		 * Creates an ID field and set's the primary key.
		 * 
		 * @param fieldName
		 * @param isGenerateID
		 *            When <code>true</code> an identity ID is created.
		 * @return
		 */
		private static String CreateField_EntityId(final String fieldName, final boolean isGenerateID) {

			String generateID = UI.EMPTY_STRING;

			if (isGenerateID) {
				generateID = "GENERATED ALWAYS AS IDENTITY (START WITH 0 ,INCREMENT BY 1)"; //$NON-NLS-1$
			}

//			 SUBJECT CHAR(64) NOT NULL CONSTRAINT OUT_TRAY_PK PRIMARY KEY,

			return "	" //$NON-NLS-1$
					+ (fieldName + " BIGINT NOT NULL ") //$NON-NLS-1$
					+ generateID
					+ (" CONSTRAINT " + fieldName + "_pk PRIMARY KEY") //$NON-NLS-1$ //$NON-NLS-2$
					+ ",\n";//$NON-NLS-1$
		}

		/**
		 * @param stmt
		 * @param tableName
		 * @param indexAndColumnName
		 * @throws SQLException
		 */
		private static void CreateIndex(final Statement stmt, final String tableName, final String indexAndColumnName)
				throws SQLException {

			final String sql = "CREATE INDEX " + indexAndColumnName + " ON " + tableName + " (" + indexAndColumnName + ")"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

			exec(stmt, sql);
		}
	}

	private TourDatabase() {

		_isDerbyEmbedded = _prefStore.getBoolean(ITourbookPreferences.TOUR_DATABASE_IS_DERBY_EMBEDDED);

		if (_isDerbyEmbedded) {

			// use embedded server

			DERBY_DRIVER_CLASS = "org.apache.derby.jdbc.EmbeddedDriver"; //$NON-NLS-1$

			DERBY_URL = "jdbc:derby:tourbook"; //$NON-NLS-1$

		} else {

			// use network server

			DERBY_DRIVER_CLASS = "org.apache.derby.jdbc.ClientDriver"; //$NON-NLS-1$

			DERBY_URL = "jdbc:derby://localhost:1527/tourbook"; //$NON-NLS-1$
		}
	}

	/**
	 * removes all tour tags which are loaded from the database so the next time they will be
	 * reloaded
	 */
	public static synchronized void clearTourTags() {

		if (_tourTags != null) {
			_tourTags.clear();
			_tourTags = null;
		}

		if (_tagCollections != null) {
			_tagCollections.clear();
		}
	}

	/**
	 * remove all tour types and set their images dirty that the next time they have to be loaded
	 * from the database and the images are recreated
	 */
	public static synchronized void clearTourTypes() {

		if (_tourTypes != null) {
			_tourTypes.clear();
			_tourTypes = null;
		}

		UI.getInstance().setTourTypeImagesDirty();
	}

	public static void closeConnection(final Connection conn) {

		if (conn != null) {
			try {
				conn.close();
			} catch (final SQLException e) {
				UI.showSQLException(e);
			}
		}
	}

	private static void computeComputedValuesForAllTours(final IProgressMonitor monitor) {

		final ArrayList<Long> tourList = getAllTourIds();

		// loop: all tours, compute computed fields and save the tour
		int tourCounter = 1;
		for (final Long tourId : tourList) {

			if (monitor != null) {
				monitor.subTask(NLS.bind(Messages.Tour_Database_update_tour,//
						new Object[] { tourCounter++, tourList.size() }));
			}

			final TourData tourData = getTourFromDb(tourId);
			if (tourData != null) {

				tourData.computeComputedValues();
				saveTour(tourData, false);
			}
		}
	}

	/**
	 * @return
	 */
	/**
	 * @param runner
	 *            {@link IComputeTourValues} interface to compute values for one tour
	 * @param tourIds
	 *            Tour ID's which should be computed, when <code>null</code>, ALL tours will be
	 *            computed.
	 * @return
	 */
	public static boolean computeValuesForAllTours(final IComputeTourValues runner, final ArrayList<Long> tourIds) {

		final Shell shell = Display.getDefault().getActiveShell();

		final NumberFormat nf = NumberFormat.getNumberInstance();
		nf.setMinimumFractionDigits(0);
		nf.setMaximumFractionDigits(0);

		final int[] tourCounter = new int[] { 0 };
		final int[] tourListSize = new int[] { 0 };
		final boolean[] isCanceled = new boolean[] { false };

		final IRunnableWithProgress runnable = new IRunnableWithProgress() {
			@Override
			public void run(final IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {

				ArrayList<Long> tourList;
				if (tourIds == null) {
					tourList = getAllTourIds();
				} else {
					tourList = tourIds;
				}
				tourListSize[0] = tourList.size();

				long lastUIUpdateTime = 0;

				monitor.beginTask(Messages.tour_database_computeComputeValues_mainTask, tourList.size());

				// loop over all tours and compute values
				for (final Long tourId : tourList) {

					final TourData dbTourData = getTourFromDb(tourId);
					TourData savedTourData = null;

					if (dbTourData != null) {
						if (runner.computeTourValues(dbTourData)) {

							// ensure that all computed values are set
							dbTourData.computeComputedValues();

							savedTourData = saveTour(dbTourData, false);
						}
					}

					tourCounter[0]++;

					/*
					 * This must be called in every iteration because it can compute values !!!
					 */
					final String runnerSubTaskText = runner.getSubTaskText(savedTourData);

					final long currentTime = System.currentTimeMillis();
					if (currentTime > lastUIUpdateTime + 200) {

						lastUIUpdateTime = currentTime;

						// create sub task text
						final StringBuilder sb = new StringBuilder();
						sb.append(NLS.bind(Messages.tour_database_computeComputeValues_subTask,//
								new Object[] { tourCounter[0], tourListSize[0], }));

						sb.append(UI.DASH_WITH_DOUBLE_SPACE);
						sb.append(tourCounter[0] * 100 / tourListSize[0]);
						sb.append(UI.SYMBOL_PERCENTAGE);

						if (runnerSubTaskText != null) {
							sb.append(UI.DASH_WITH_DOUBLE_SPACE);
							sb.append(runnerSubTaskText);
						}

						monitor.subTask(sb.toString());
					}
					monitor.worked(1);

					// check if canceled
					if (monitor.isCanceled()) {
						isCanceled[0] = true;
						break;
					}

////				// debug test
//					if (tourCounter[0] > 0) {
//						break;
//					}
				}
			}
		};

		try {

			new ProgressMonitorDialog(shell).run(true, true, runnable);

		} catch (final InvocationTargetException e) {
			e.printStackTrace();
		} catch (final InterruptedException e) {
			e.printStackTrace();
		} finally {

			// create result text
			final StringBuilder sb = new StringBuilder();
			sb.append(NLS.bind(
					Messages.tour_database_computeComputedValues_resultMessage,
					tourCounter[0],
					tourListSize[0]));

			final String runnerResultText = runner.getResultText();
			if (runnerResultText != null) {
				sb.append(UI.NEW_LINE2);
				sb.append(runnerResultText);
			}

			MessageDialog.openInformation(
					shell,
					Messages.tour_database_computeComputedValues_resultTitle,
					sb.toString());
		}

		return isCanceled[0];
	}

	/**
	 * Remove a tour from the database
	 * 
	 * @param tourId
	 */
	public static boolean deleteTour(final long tourId) {

		boolean isRemoved = false;

		final EntityManager em = TourDatabase.getInstance().getEntityManager();
		final EntityTransaction ts = em.getTransaction();

		try {
			final TourData tourData = em.find(TourData.class, tourId);

			if (tourData != null) {
				ts.begin();
				em.remove(tourData);
				ts.commit();
			}

		} catch (final Exception e) {

			e.printStackTrace();

			/*
			 * an error could have been occured when loading the tour with em.find, remove the tour
			 * with sql commands
			 */
			deleteTour_WithSQL(tourId);

		} finally {
			if (ts.isActive()) {
				ts.rollback();
			} else {
				isRemoved = true;
			}
			em.close();
		}

		if (isRemoved) {

			deleteTour_WithSQL(tourId);
			TourManager.getInstance().removeTourFromCache(tourId);
		}

		return true;
	}

	/**
	 * Remove tour from all tables which contain data for the removed tour
	 * 
	 * @param tourId
	 *            Tour Id for the tour which is removed
	 */
	private static void deleteTour_WithSQL(final long tourId) {

		Connection conn = null;
		PreparedStatement prepStmt = null;

		String sql = UI.EMPTY_STRING;

		try {

			conn = TourDatabase.getInstance().getConnection();

			final String sqlWhere_TourId = " WHERE tourId=?";//$NON-NLS-1$
			final String sqlWhere_TourData_TourId = " WHERE " + TABLE_TOUR_DATA + "_tourId=?"; //$NON-NLS-1$ //$NON-NLS-2$

			final String allSql[] = {
					//
					"DELETE FROM " + TABLE_TOUR_DATA + sqlWhere_TourId, //$NON-NLS-1$
					//
					"DELETE FROM " + TABLE_TOUR_MARKER + sqlWhere_TourData_TourId, //$NON-NLS-1$
					//
					"DELETE FROM " + TABLE_TOUR_PHOTO + sqlWhere_TourData_TourId, //$NON-NLS-1$
					//
					"DELETE FROM " + TABLE_TOUR_WAYPOINT + sqlWhere_TourData_TourId, //$NON-NLS-1$
					//
					"DELETE FROM " + TABLE_TOUR_REFERENCE + sqlWhere_TourData_TourId, //$NON-NLS-1$
					//
					"DELETE FROM " + JOINTABLE__TOURDATA__TOURTAG + sqlWhere_TourData_TourId, //$NON-NLS-1$
					//
					"DELETE FROM " + TABLE_TOUR_COMPARED + sqlWhere_TourId, //$NON-NLS-1$
			//
			};

			for (final String sqlExec : allSql) {

				sql = sqlExec;

				prepStmt = conn.prepareStatement(sql);
				prepStmt.setLong(1, tourId);
				prepStmt.execute();
				prepStmt.close();
			}

		} catch (final SQLException e) {
			System.out.println(sql);
			UI.showSQLException(e);
		} finally {
			closeConnection(conn);
		}
	}

	/**
	 * Disable runtime statistics by putting this stagement after the result set was read
	 * 
	 * @param conn
	 * @throws SQLException
	 */
	public static void disableRuntimeStatistic(final Connection conn) throws SQLException {

		CallableStatement cs;

		cs = conn.prepareCall("VALUES SYSCS_UTIL.SYSCS_GET_RUNTIMESTATISTICS()"); //$NON-NLS-1$
		cs.execute();

		// log runtime statistics
		final ResultSet rs = cs.getResultSet();
		while (rs.next()) {
			System.out.println(rs.getString(1));
		}

		cs.close();

		cs = conn.prepareCall("CALL SYSCS_UTIL.SYSCS_SET_RUNTIMESTATISTICS(0)"); //$NON-NLS-1$
		cs.execute();
		cs.close();

		cs = conn.prepareCall("CALL SYSCS_UTIL.SYSCS_SET_STATISTICS_TIMING(0)"); //$NON-NLS-1$
		cs.execute();
		cs.close();
	}

	/**
	 * Get runtime statistics by putting this stagement before the query is executed
	 * 
	 * @param conn
	 * @throws SQLException
	 */
	public static void enableRuntimeStatistics(final Connection conn) throws SQLException {

		CallableStatement cs;

		cs = conn.prepareCall("CALL SYSCS_UTIL.SYSCS_SET_RUNTIMESTATISTICS(1)"); //$NON-NLS-1$
		cs.execute();
		cs.close();

		cs = conn.prepareCall("CALL SYSCS_UTIL.SYSCS_SET_STATISTICS_TIMING(1)"); //$NON-NLS-1$
		cs.execute();
		cs.close();
	}

	private static void ensureDbIsCreated() {

		Connection conn = null;

		// ensure driver is loaded
		try {
			Class.forName(DERBY_DRIVER_CLASS);
		} catch (final ClassNotFoundException e) {
			StatusUtil.showStatus(e);
		}

		/*
		 * Get a connection, this also creates the database when not yet available. The embedded
		 * driver displays a warning when database already exist.
		 */
		try {

			conn = DriverManager.getConnection(//
					DERBY_URL + DERBY_URL_CREATE_TRUE,
					TABLE_SCHEMA,
					TABLE_SCHEMA);

		} catch (final SQLException e) {
			UI.showSQLException(e);
		} finally {
			closeConnection(conn);
		}
	}

	private static void exec(final Statement stmt, final String sql) throws SQLException {

		StatusUtil.log(sql);
		System.out.println();

		stmt.execute(sql);
	}

	private static void exec(final Statement stmt, final String[] sqlStatements) throws SQLException {

		for (final String sql : sqlStatements) {
			exec(stmt, sql);
		}
	}

	private static void execUpdate(final Statement stmt, final String sql) throws SQLException {

		StatusUtil.log(sql);
		System.out.println();

		stmt.executeUpdate(sql);
	}

	/**
	 * @param tourTypeList
	 * @return Returns a list with all {@link TourType}'s which are currently used (with filter) to
	 *         display tours.<br>
	 *         Returns <code>null</code> when {@link TourType}'s are not defined.<br>
	 *         Return an empty list when the {@link TourType} is not set within the {@link TourData}
	 */
	public static ArrayList<TourType> getActiveTourTypes() {
		return _activeTourTypes;
	}

	private static ArrayList<Long> getAllTourIds() {

		final ArrayList<Long> tourIds = new ArrayList<Long>();

		Connection conn = null;
		Statement stmt = null;

		try {

			conn = getInstance().getConnection();
			stmt = conn.createStatement();

			final ResultSet result = stmt.executeQuery("SELECT tourId FROM " + TourDatabase.TABLE_TOUR_DATA); //$NON-NLS-1$

			while (result.next()) {
				tourIds.add(result.getLong(1));
			}

		} catch (final SQLException e) {
			UI.showSQLException(e);
		} finally {
			Util.closeSql(stmt);
			closeConnection(conn);
		}

		return tourIds;
	}

	public static TreeSet<String> getAllTourMarkerNames() {

		if (_dbTourMarkerNames == null) {
			_dbTourMarkerNames = getDistinctValues(TourDatabase.TABLE_TOUR_MARKER, "label"); //$NON-NLS-1$
		}

		return _dbTourMarkerNames;
	}

	/**
	 * Getting all tour place ends from the database sorted by alphabet and without any double
	 * entries.
	 * 
	 * @author Stefan F.
	 * @return places as string array.
	 */
	public static TreeSet<String> getAllTourPlaceEnds() {

		if (_dbTourEndPlace == null) {
			_dbTourEndPlace = getDistinctValues(TourDatabase.TABLE_TOUR_DATA, "tourEndPlace"); //$NON-NLS-1$
		}

		return _dbTourEndPlace;
	}

	/**
	 * Getting all tour start places from the database sorted by alphabet and without any double
	 * entries.
	 * 
	 * @author Stefan F.
	 * @return titles as string array.
	 */
	public static TreeSet<String> getAllTourPlaceStarts() {

		if (_dbTourStartPlace == null) {
			_dbTourStartPlace = getDistinctValues(TourDatabase.TABLE_TOUR_DATA, "tourStartPlace"); //$NON-NLS-1$
		}

		return _dbTourStartPlace;
	}

	/**
	 * this method is synchronized to conform to FindBugs
	 * 
	 * @return Returns all tour tags which are stored in the database, the hash key is the tag id
	 */
	public static HashMap<Long, TourTag> getAllTourTags() {

		if (_tourTags != null) {
			return _tourTags;
		}

		synchronized (DB_LOCK) {

			// check again, field must be volatile to work correctly
			if (_tourTags != null) {
				return _tourTags;
			}

			final EntityManager em = TourDatabase.getInstance().getEntityManager();
			if (em != null) {

				final Query emQuery = em.createQuery("" //$NON-NLS-1$
						+ "SELECT tourTag" //$NON-NLS-1$
						+ (" FROM " + TourTag.class.getSimpleName() + " AS tourTag")); //$NON-NLS-1$ //$NON-NLS-2$

				_tourTags = new HashMap<Long, TourTag>();

				final List<?> resultList = emQuery.getResultList();
				for (final Object result : resultList) {

					if (result instanceof TourTag) {
						final TourTag tourTag = (TourTag) result;
						_tourTags.put(tourTag.getTagId(), tourTag);
					}
				}

				em.close();
			}
		}

		return _tourTags;
	}

	/**
	 * Getting all tour titles from the database sorted by alphabet and without any double entries.
	 * 
	 * @author Stefan F.
	 * @return titles as string array.
	 */
	public static TreeSet<String> getAllTourTitles() {

		if (_dbTourTitles == null) {
			_dbTourTitles = getDistinctValues(TourDatabase.TABLE_TOUR_DATA, "tourTitle"); //$NON-NLS-1$
		}

		return _dbTourTitles;
	}

	/**
	 * @return Returns the backend of all tour types which are stored in the database sorted by name
	 */
	@SuppressWarnings("unchecked")
	public static ArrayList<TourType> getAllTourTypes() {

		if (_tourTypes != null) {
			return _tourTypes;
		}

		synchronized (DB_LOCK) {

			// check again, field must be volatile to work correctly
			if (_tourTypes != null) {
				return _tourTypes;
			}

			// create empty list
			_tourTypes = new ArrayList<TourType>();

			final EntityManager em = TourDatabase.getInstance().getEntityManager();
			if (em != null) {

				final Query emQuery = em.createQuery(//
						//
						"SELECT tourType" //$NON-NLS-1$
								+ (" FROM TourType AS tourType") //$NON-NLS-1$
								+ (" ORDER  BY tourType.name")); //$NON-NLS-1$

				_tourTypes = (ArrayList<TourType>) emQuery.getResultList();

				em.close();
			}
		}

		return _tourTypes;
	}

	/**
	 * Getting one row from the database sorted by alphabet and without any double entries.
	 * 
	 * @author Stefan F.
	 * @param sqlQuery
	 *            must look like: "SELECT tourTitle FROM " + TourDatabase.TABLE_TOUR_DATA +
	 *            " ORDER BY tourTitle"
	 * @return places as string array.
	 */
	private static TreeSet<String> getDistinctValues(final String db, final String fieldname) {

		final TreeSet<String> sortedValues = new TreeSet<String>(new Comparator<String>() {
			@Override
			public int compare(final String s1, final String s2) {
				// sort without case
				return s1.compareToIgnoreCase(s2);
			}
		});

		/*
		 * run in UI thread otherwise the busyindicator fails
		 */
		final Display display = Display.getDefault();

		display.syncExec(new Runnable() {
			public void run() {

				BusyIndicator.showWhile(display, new Runnable() {
					public void run() {

						Connection conn = null;
						Statement stmt = null;
						String sqlQuery = null;

						try {

							conn = getInstance().getConnection();
							stmt = conn.createStatement();

							sqlQuery = "SELECT" //$NON-NLS-1$
									+ " DISTINCT" //$NON-NLS-1$
									+ " " + fieldname //$NON-NLS-1$
									+ " FROM " + db //$NON-NLS-1$
									+ " ORDER BY " + fieldname; //$NON-NLS-1$

							final ResultSet result = stmt.executeQuery(sqlQuery);

							while (result.next()) {

								String dbValue = result.getString(1);
								if (dbValue != null) {

									dbValue = dbValue.trim();

									if (dbValue.length() > 0) {
										sortedValues.add(dbValue);
									}
								}
							}

						} catch (final SQLException e) {
							UI.showSQLException(e);
						} finally {
							Util.closeSql(stmt);
							closeConnection(conn);
						}

						/*
						 * log existing values
						 */
//						final StringBuilder sb = new StringBuilder();
//						for (final String text : sortedValues) {
//							sb.append(text);
//							sb.append(UI.NEW_LINE);
//						}
//						System.out.println(UI.NEW_LINE2);
//						System.out.println(sqlQuery);
//						System.out.println(UI.NEW_LINE);
//						System.out.println(sb.toString());
//						// TODO remove SYSTEM.OUT.PRINTLN
					}
				});
			}
		});

		return sortedValues;
	}

	public static TourDatabase getInstance() {

		if (_instance != null) {
			return _instance;
		}

		synchronized (DB_LOCK) {
			// check again
			if (_instance == null) {
				_instance = new TourDatabase();
			}
		}

		return _instance;
	}

	private static Connection getPooledConnection() throws SQLException {

		if (_pooledDataSource == null) {

			synchronized (DB_LOCK) {
				// check again
				if (_pooledDataSource == null) {

					ensureDbIsCreated();

					try {
						_pooledDataSource = new ComboPooledDataSource();

						//loads the jdbc driver
						_pooledDataSource.setDriverClass(DERBY_DRIVER_CLASS);
						_pooledDataSource.setJdbcUrl(DERBY_URL);
						_pooledDataSource.setUser(TABLE_SCHEMA);
						_pooledDataSource.setPassword(TABLE_SCHEMA);

						_pooledDataSource.setMaxPoolSize(100);
						_pooledDataSource.setMaxStatements(100);
						_pooledDataSource.setMaxStatementsPerConnection(20);

					} catch (final PropertyVetoException e) {
						StatusUtil.log(e);
					}
				}
			}
		}

		Connection conn = null;
		try {
			conn = _pooledDataSource.getConnection();
		} catch (final SQLException e) {
			UI.showSQLException(e);
		}

		return conn;
	}

	@SuppressWarnings("unchecked")
	public static TagCollection getRootTags() {

		final long rootTagId = -1L;

		TagCollection rootEntry = _tagCollections.get(Long.valueOf(rootTagId));
		if (rootEntry != null) {
			return rootEntry;
		}

		/*
		 * read root tags from the database
		 */
		final EntityManager em = TourDatabase.getInstance().getEntityManager();

		if (em == null) {
			return null;
		}

		rootEntry = new TagCollection();

		/*
		 * read tag categories from db
		 */
		Query emQuery = em.createQuery(//
				//
				"SELECT ttCategory" //$NON-NLS-1$
						+ (" FROM " + TourTagCategory.class.getSimpleName() + " AS ttCategory") //$NON-NLS-1$ //$NON-NLS-2$
						+ (" WHERE ttCategory.isRoot=1") //$NON-NLS-1$
						+ (" ORDER BY ttCategory.name")); //$NON-NLS-1$

		rootEntry.tourTagCategories = (ArrayList<TourTagCategory>) emQuery.getResultList();

		/*
		 * read tour tags from db
		 */
		emQuery = em.createQuery(//
				//
				"SELECT tourTag" //$NON-NLS-1$
						+ (" FROM " + TourTag.class.getSimpleName() + " AS tourTag ") //$NON-NLS-1$ //$NON-NLS-2$
						+ (" WHERE tourTag.isRoot=1") //$NON-NLS-1$
						+ (" ORDER BY tourTag.name")); //$NON-NLS-1$

		rootEntry.tourTags = (ArrayList<TourTag>) emQuery.getResultList();

		em.close();

		_tagCollections.put(rootTagId, rootEntry);

		return rootEntry;
	}

	/**
	 * @param categoryId
	 * @return Returns a {@link TagCollection} with all tags and categories for the category Id
	 */
	public static TagCollection getTagEntries(final long categoryId) {

		final Long categoryIdValue = Long.valueOf(categoryId);

		TagCollection categoryEntries = _tagCollections.get(categoryIdValue);
		if (categoryEntries != null) {
			return categoryEntries;
		}

		/*
		 * read tag entries from the database
		 */

		final EntityManager em = TourDatabase.getInstance().getEntityManager();

		if (em == null) {
			return null;
		}

		categoryEntries = new TagCollection();

		final TourTagCategory tourTagCategory = em.find(TourTagCategory.class, categoryIdValue);

		// get tags
		final Set<TourTag> lazyTourTags = tourTagCategory.getTourTags();
		categoryEntries.tourTags = new ArrayList<TourTag>(lazyTourTags);
		Collections.sort(categoryEntries.tourTags);

		// get categories
		final Set<TourTagCategory> lazyTourTagCategories = tourTagCategory.getTagCategories();
		categoryEntries.tourTagCategories = new ArrayList<TourTagCategory>(lazyTourTagCategories);
		Collections.sort(categoryEntries.tourTagCategories);

		em.close();

		_tagCollections.put(categoryIdValue, categoryEntries);

		return categoryEntries;
	}

	/**
	 * @param tagIds
	 * @return Returns the tag names separated with a comma or an empty string when tagIds are
	 *         <code>null</code>
	 */
	public static String getTagNames(final ArrayList<Long> tagIds) {

		if (tagIds == null) {
			return UI.EMPTY_STRING;
		}

		final HashMap<Long, TourTag> hashTags = getAllTourTags();
		final ArrayList<String> tagList = new ArrayList<String>();

		final StringBuilder sb = new StringBuilder();

		// get tag name for each tag id
		for (final Long tagId : tagIds) {
			final TourTag tag = hashTags.get(tagId);

			if (tag != null) {
				tagList.add(tag.getTagName());
			} else {
				try {
					throw new MyTourbookException("tag id '" + tagId + "' is not available"); //$NON-NLS-1$ //$NON-NLS-2$
				} catch (final MyTourbookException e) {
					e.printStackTrace();
				}
			}
		}

		// sort tags by name
		Collections.sort(tagList);

		// convert list into visible string
		int tagIndex = 0;
		for (final String tagName : tagList) {
			if (tagIndex++ > 0) {
				sb.append(", ");//$NON-NLS-1$
			}
			sb.append(tagName);
		}

		return sb.toString();
	}

	/**
	 * @return Returns all tour types in the db sorted by name
	 */
	@SuppressWarnings("unchecked")
	public static ArrayList<TourBike> getTourBikes() {

		ArrayList<TourBike> bikeList = new ArrayList<TourBike>();

		final EntityManager em = TourDatabase.getInstance().getEntityManager();

		if (em != null) {

			final Query emQuery = em.createQuery(//
					//
					"SELECT tourBike" //$NON-NLS-1$
							+ (" FROM TourBike AS tourBike ") //$NON-NLS-1$
							+ (" ORDER  BY tourBike.name")); //$NON-NLS-1$

			bikeList = (ArrayList<TourBike>) emQuery.getResultList();

			em.close();
		}

		return bikeList;
	}

	/**
	 * Get a tour from the database
	 * 
	 * @param tourId
	 * @return Returns the tour data or <code>null</code> if the tour is not in the database
	 */
	public static TourData getTourFromDb(final Long tourId) {

		final EntityManager em = TourDatabase.getInstance().getEntityManager();

		final TourData tourData = em.find(TourData.class, tourId);

		em.close();

		return tourData;
	}

	/**
	 * Get tour type from id
	 * 
	 * @param tourTypeId
	 * @return Returns a {@link TourType} from the id or <code>null</code> when tour type is not
	 *         available for the id.
	 */
	public static TourType getTourType(final long tourTypeId) {

		for (final TourType tourType : getAllTourTypes()) {
			if (tourType.getTypeId() == tourTypeId) {
				return tourType;
			}
		}

		return null;
	}

	/**
	 * @param typeId
	 * @return Returns the name for the {@link TourType} or an empty string when the tour type id
	 *         was not found
	 */
	public static String getTourTypeName(final long typeId) {

		String tourTypeName = Messages.ui_tour_not_defined;

		for (final TourType tourType : getAllTourTypes()) {
			if (tourType.getTypeId() == typeId) {
				tourTypeName = tourType.getName();
				break;
			}
		}

		return tourTypeName;
	}

	/**
	 * Checks if a field exceeds the max length
	 * 
	 * @param field
	 * @param maxLength
	 * @param uiFieldName
	 * @return Returns {@link FIELD_VALIDATION} status
	 */
	public static FIELD_VALIDATION isFieldValidForSave(final String field, final int maxLength, final String uiFieldName) {

		return isFieldValidForSave(field, maxLength, uiFieldName, false);
	}

	/**
	 * Checks if a field exceeds the max length
	 * 
	 * @param field
	 * @param maxLength
	 * @param uiFieldName
	 * @param isForceTruncation
	 * @return Returns {@link FIELD_VALIDATION} status
	 */
	public static FIELD_VALIDATION isFieldValidForSave(	final String field,
														final int maxLength,
														final String uiFieldName,
														final boolean isForceTruncation) {

		final FIELD_VALIDATION[] returnValue = { FIELD_VALIDATION.IS_VALID };

		if (field != null && field.length() > maxLength) {

			Display.getDefault().syncExec(new Runnable() {
				public void run() {

					if (isForceTruncation) {
						returnValue[0] = FIELD_VALIDATION.TRUNCATE;
						StatusUtil.log(new Exception(NLS.bind(
								"Field \"{0}\" with content \"{1}\" is truncated to {2} characters.",//$NON-NLS-1$
								new Object[] { uiFieldName, field, maxLength })));
						return;
					}

					if (MessageDialog.openConfirm(
							Display.getDefault().getActiveShell(),
							Messages.Tour_Database_Dialog_ValidateFields_Title,
							NLS.bind(Messages.Tour_Database_Dialog_ValidateFields_Message, //
									new Object[] { uiFieldName, field.length(), maxLength }))) {

						returnValue[0] = FIELD_VALIDATION.TRUNCATE;
					} else {
						returnValue[0] = FIELD_VALIDATION.IS_INVALID;
					}
				}
			});
		}

		return returnValue[0];
	}

	/**
	 * Persists an entity.
	 * <p>
	 * This method is <b>much faster</b> than using this
	 * {@link #saveEntity(Object, long, Class, EntityManager)}
	 * <p>
	 * 
	 * @param entity
	 * @param id
	 * @param entityClass
	 * @return Returns the saved entity.
	 */
	public static <T> T saveEntity(final T entity, final long id, final Class<?> entityClass) {

		final EntityManager em = TourDatabase.getInstance().getEntityManager();
		final EntityTransaction ts = em.getTransaction();

		T savedEntity = null;
		boolean isSaved = false;

		try {

			ts.begin();
			{
				final Object entityInDB = em.find(entityClass, id);

				if (entityInDB == null) {

					// entity is not persisted

					em.persist(entity);
					savedEntity = entity;

				} else {

					savedEntity = em.merge(entity);
				}
			}
			ts.commit();

		} catch (final Exception e) {
			StatusUtil.showStatus(e);
		} finally {
			if (ts.isActive()) {
				ts.rollback();
			} else {
				isSaved = true;
			}
			em.close();
		}

		if (isSaved == false) {
			MessageDialog.openError(Display.getCurrent().getActiveShell(),//
					"Error", "Error occured when saving an entity"); //$NON-NLS-1$ //$NON-NLS-2$
		}

		return savedEntity;
	}

	/**
	 * Persists an entity, an error is logged when saving fails.
	 * <p>
	 * This method is <b>much slower</b> than using this {@link #saveEntity(Object, long, Class)}
	 * method without using the same EntityManager.
	 * 
	 * @param entity
	 * @param id
	 * @param entityClass
	 * @return Returns the saved entity
	 */
	public static <T> T saveEntity(final T entity, final long id, final Class<T> entityClass, final EntityManager em) {

		final EntityTransaction ts = em.getTransaction();

		T savedEntity = null;
		boolean isSaved = false;

		try {

			ts.begin();
			{
				final T entityInDB = em.find(entityClass, id);

				if (entityInDB == null) {

					// entity is not persisted

					em.persist(entity);
					savedEntity = entity;

				} else {

					savedEntity = em.merge(entity);
				}
			}
			ts.commit();

		} catch (final Exception e) {
			StatusUtil.showStatus(e);
		} finally {
			if (ts.isActive()) {
				ts.rollback();
			} else {
				isSaved = true;
			}
		}

		if (isSaved == false) {
			MessageDialog.openError(Display.getCurrent().getActiveShell(),//
					"Error", "Error occured when saving an entity"); //$NON-NLS-1$ //$NON-NLS-2$
		}

		return savedEntity;
	}

	/**
	 * Persist {@link TourData} in the database and updates the tour data cache with the persisted
	 * tour<br>
	 * <br>
	 * When a tour has no person the tour will not be saved, a person must be set first before the
	 * tour can be saved
	 * 
	 * @param tourData
	 * @param isUpdateModifiedDate
	 *            When <code>true</code> the modified date is updated. For updating computed field
	 *            it does not make sence to set the modified date.
	 * @return persisted {@link TourData} or <code>null</code> when saving fails
	 */
	public static TourData saveTour(final TourData tourData, final boolean isUpdateModifiedDate) {

		/*
		 * prevent saving a tour which was deleted before
		 */
		if (tourData.isTourDeleted) {
			return null;
		}

		/*
		 * history tour cannot be saved
		 */
		if (tourData.isHistoryTour) {
			return null;
		}

		/*
		 * prevent saving a tour when a person is not set, this check is for internal use that all
		 * data are valid
		 */
		if (tourData.getTourPerson() == null) {
			StatusUtil.log("Cannot save a tour without a person: " + tourData); //$NON-NLS-1$
			return null;
		}

		/*
		 * check size of varcar fields
		 */
		if (tourData.isValidForSave() == false) {
			return null;
		}

		/**
		 * ensure HR zones are computed, it requires that a person is set which is not the case when
		 * a device importer calls the method {@link TourData#computeComputedValues()}
		 */
		tourData.getNumberOfHrZones();

		final DateTime dtNow = new DateTime();

		final long dtSaved = (dtNow.getYear() * 10000000000L)
				+ (dtNow.getMonthOfYear() * 100000000L)
				+ (dtNow.getDayOfMonth() * 1000000L)
				//
				+ (dtNow.getHourOfDay() * 10000L)
				+ (dtNow.getMinuteOfHour() * 100L)
				+ dtNow.getSecondOfMinute();

		EntityManager em = TourDatabase.getInstance().getEntityManager();

		TourData persistedEntity = null;

		if (em != null) {

			final EntityTransaction ts = em.getTransaction();

			try {

				tourData.onPrePersist();

				ts.begin();
				{
					final TourData tourDataEntity = em.find(TourData.class, tourData.getTourId());
					if (tourDataEntity == null) {

						// tour is not yet persisted

						tourData.setDateTimeCreated(dtSaved);

						em.persist(tourData);

						persistedEntity = tourData;

					} else {

						if (isUpdateModifiedDate) {
							tourData.setDateTimeModified(dtSaved);
						}

						persistedEntity = em.merge(tourData);
					}
				}
				ts.commit();

			} catch (final Exception e) {

				StatusUtil.showStatus(Messages.Tour_Database_TourSaveError, e);

			} finally {
				if (ts.isActive()) {
					ts.rollback();
				}
				em.close();
			}
		}

		if (persistedEntity != null) {

			em = TourDatabase.getInstance().getEntityManager();
			try {

				persistedEntity = em.find(TourData.class, tourData.getTourId());

			} catch (final Exception e) {
				StatusUtil.log(e);
			}

			em.close();

			TourManager.getInstance().updateTourInCache(persistedEntity);

			updateCachedFields(persistedEntity);
		}

		return persistedEntity;
	}

	public static void updateActiveTourTypeList(final TourTypeFilter tourTypeFilter) {

		switch (tourTypeFilter.getFilterType()) {
		case TourTypeFilter.FILTER_TYPE_SYSTEM:

			if (tourTypeFilter.getSystemFilterId() == TourTypeFilter.SYSTEM_FILTER_ID_ALL) {

				// all tour types are selected

				_activeTourTypes = _tourTypes;
				return;

			} else {

				// tour type is not defined

			}

			break;

		case TourTypeFilter.FILTER_TYPE_DB:

			_activeTourTypes = new ArrayList<TourType>();
			_activeTourTypes.add(tourTypeFilter.getTourType());

			return;

		case TourTypeFilter.FILTER_TYPE_TOURTYPE_SET:

			final Object[] tourTypes = tourTypeFilter.getTourTypeSet().getTourTypes();

			if (tourTypes.length != 0) {

				// create a list with all tour types from the set

				_activeTourTypes = new ArrayList<TourType>();

				for (final Object item : tourTypes) {
					_activeTourTypes.add((TourType) item);
				}
				return;
			}

			break;

		default:
			break;
		}

		// set default empty list
		_activeTourTypes = new ArrayList<TourType>();
	}

	private static void updateCachedFields(final TourData tourData) {

		// cache tour title
		final TreeSet<String> allTitles = getAllTourTitles();
		final String tourTitle = tourData.getTourTitle();
		if (tourTitle.length() > 0) {
			allTitles.add(tourTitle);
		}

		// cache tour start place
		final TreeSet<String> allPlaceStarts = getAllTourPlaceStarts();
		final String tourStartPlace = tourData.getTourStartPlace();
		if (tourStartPlace.length() > 0) {
			allPlaceStarts.add(tourStartPlace);
		}

		// cache tour end place
		final TreeSet<String> allPlaceEnds = getAllTourPlaceEnds();
		final String tourEndPlace = tourData.getTourEndPlace();
		if (tourEndPlace.length() > 0) {
			allPlaceEnds.add(tourEndPlace);
		}

		// cache tour marker names
		final TreeSet<String> allMarkerNames = getAllTourMarkerNames();
		final Set<TourMarker> allTourMarker = tourData.getTourMarkers();
		for (final TourMarker tourMarker : allTourMarker) {
			final String label = tourMarker.getLabel();
			if (label.length() > 0) {
				allMarkerNames.add(label);
			}
		}
	}

	/**
	 * Update calendar week for all tours
	 * 
	 * @param conn
	 * @param monitor
	 * @param firstDayOfWeek
	 * @param minimalDaysInFirstWeek
	 * @return Returns <code>true</code> when the week is computed
	 * @throws SQLException
	 */
	public static boolean updateTourWeek(	final Connection conn,
											final IProgressMonitor monitor,
											final int firstDayOfWeek,
											final int minimalDaysInFirstWeek) throws SQLException {

		final ArrayList<Long> tourList = getAllTourIds();

		boolean isUpdated = false;

		final PreparedStatement stmtSelect = conn.prepareStatement(//
				"SELECT" //							//$NON-NLS-1$
						+ " StartYear," // 				// 1 //$NON-NLS-1$
						+ " StartMonth," // 			// 2 //$NON-NLS-1$
						+ " StartDay" // 				// 3 //$NON-NLS-1$
						+ (" FROM " + TABLE_TOUR_DATA) //	$NON-NLS-1$ //$NON-NLS-1$
						+ " WHERE TourId=?"); //			$NON-NLS-1$ //$NON-NLS-1$

		final PreparedStatement stmtUpdate = conn.prepareStatement(//
				"UPDATE " + TABLE_TOUR_DATA//  //$NON-NLS-1$
						+ " SET" //$NON-NLS-1$
						+ " startWeek=?, " //$NON-NLS-1$
						+ " startWeekYear=? " //$NON-NLS-1$
						+ " WHERE tourId=?"); //$NON-NLS-1$

		int tourIdx = 1;
		final Calendar calendar = GregorianCalendar.getInstance();

		// set ISO 8601 week date
		calendar.setFirstDayOfWeek(firstDayOfWeek);
		calendar.setMinimalDaysInFirstWeek(minimalDaysInFirstWeek);

		// loop over all tours and calculate and set new columns
		for (final Long tourId : tourList) {

			if (monitor != null) {
				final String msg = NLS.bind(
						Messages.Tour_Database_Update_TourWeek,
						new Object[] { tourIdx++, tourList.size() });
				monitor.subTask(msg);
			}

			// get tour date
			stmtSelect.setLong(1, tourId);
//				stmtSelect.execute();

			final ResultSet result = stmtSelect.executeQuery();
			while (result.next()) {

				// get date from database
				final short dbYear = result.getShort(1);
				final short dbMonth = result.getShort(2);
				final short dbDay = result.getShort(3);

				calendar.set(dbYear, dbMonth - 1, dbDay);

				final short weekNo = (short) calendar.get(Calendar.WEEK_OF_YEAR);
				final short weekYear = (short) Util.getYearForWeek(calendar);

				// update week number/week year in the database
				stmtUpdate.setShort(1, weekNo);
				stmtUpdate.setShort(2, weekYear);
				stmtUpdate.setLong(3, tourId);
				stmtUpdate.executeUpdate();

				isUpdated = true;
			}
		}

		return isUpdated;
	}

	public void addPropertyListener(final IPropertyListener listener) {
		_propertyListeners.add(listener);
	}

	private boolean checkDb() {

		try {
			checkServer();
		} catch (final Throwable e) {
			StatusUtil.log(e);
			return false;
		}

		checkTable(null);

		if (checkVersion(null) == false) {
			return false;
		}

		return true;
	}

	/**
	 * Check if the server is available
	 * 
	 * @throws Throwable
	 * @throws MyTourbookException
	 */
	private void checkServer() throws Throwable {

		if (_isDerbyEmbedded) {
			return;
		}

		// when the server is started, nothing is to do here
		if (_server != null) {
			return;
		}

		// check if the derby driver can be loaded
		try {
			Class.forName(DERBY_DRIVER_CLASS);
		} catch (final ClassNotFoundException e) {
			StatusUtil.showStatus(e.getMessage(), e);
			return;
		}

		try {

			final MyTourbookSplashHandler splashHandler = TourbookPlugin.getSplashHandler();

			checkServerCreateRunnable().run(splashHandler == null ? null : splashHandler.getBundleProgressMonitor());

		} catch (final InvocationTargetException e) {

			StatusUtil.log(e);

			MessageDialog.openError(
					Display.getDefault().getActiveShell(),
					Messages.Tour_Database_CannotConnectToDerbyServer_Title,
					NLS.bind(Messages.Tour_Database_CannotConnectToDerbyServer_Message, e
							.getTargetException()
							.getMessage()));

			PlatformUI.getWorkbench().close();

			throw e.getTargetException();

		} catch (final InterruptedException e) {
			StatusUtil.log(e);
		}
	}

	/**
	 * Checks if the database server is running, if not it will start the server. startServerJob has
	 * a job when the server is not yet started
	 */
	private IRunnableWithProgress checkServerCreateRunnable() {

		// create runnable for stating the derby server

		final IRunnableWithProgress runnable = new IRunnableWithProgress() {
			@Override
			public void run(final IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {

				if (monitor != null) {
					monitor.subTask(createUIServerStateMessage(0));
				}

				try {
					_server = new NetworkServerControl(InetAddress.getByName("localhost"), 1527); //$NON-NLS-1$
				} catch (final UnknownHostException e) {
					StatusUtil.log(e);
				} catch (final Exception e) {
					StatusUtil.log(e);
				}

				try {

					/*
					 * check if another derby server is already running (this can happen during
					 * development)
					 */
					StatusUtil.logInfo("checking if derby server is already running before server.start");//$NON-NLS-1$
					_server.ping();

				} catch (final Exception e) {

					try {
						StatusUtil.logInfo("starting derby server");//$NON-NLS-1$

						_server.start(null);

					} catch (final Exception e2) {
						StatusUtil.log(e2);
					}

					StatusUtil.logInfo("checking if derby server is running after server.start");//$NON-NLS-1$

					int pingCounter = 1;
					final int threadSleepTime = 100;

					// wait until the server is started
					while (true) {

						try {

							if (monitor != null) {
								monitor.subTask(createUIServerStateMessage(pingCounter));
							}
							_server.ping();

							StatusUtil.logInfo("derby server has started");//$NON-NLS-1$

							break;

						} catch (final Exception e1) {

							if (pingCounter > MAX_TRIES_TO_PING_SERVER) {

								StatusUtil.log("Cannot connect to derby server", e1);//$NON-NLS-1$

								throw new InvocationTargetException(e1);
							}

							StatusUtil.logInfo(NLS.bind("...waiting ({0} ms) for derby server startup: {1}", //$NON-NLS-1$
									threadSleepTime,
									pingCounter++));

							try {
								Thread.sleep(threadSleepTime);
							} catch (final InterruptedException e2) {
								StatusUtil.log(e2);
							}
						}
					}

					// make the first connection, this takes longer as the subsequent ones
					try {

						if (monitor != null) {
							monitor.subTask(Messages.Database_Monitor_SetupPooledConnection);
						}

						final Connection connection = getPooledConnection();
						connection.close();

						// log database path
						StatusUtil.logInfo("Database path: " + _databasePath); //$NON-NLS-1$

					} catch (final SQLException e1) {
						UI.showSQLException(e1);
					}
				}
			}
		};

		return runnable;
	}

	/**
	 * Check if the table in the database exist
	 * 
	 * @param monitor
	 */
	private void checkTable(final IProgressMonitor monitor) {

		if (_isTableChecked) {
			return;
		}

		Connection conn = null;

		try {

			conn = getPooledConnection();

			/*
			 * Check if the tourdata table exists
			 */
			final DatabaseMetaData metaData = conn.getMetaData();
			final ResultSet tables = metaData.getTables(null, null, null, null);
			while (tables.next()) {
				if (tables.getString(3).equalsIgnoreCase(TABLE_TOUR_DATA)) {

					// table exists

					_isTableChecked = true;

					return;
				}
			}

			if (monitor != null) {
				monitor.subTask(Messages.Database_Monitor_CreateDatabase);
			}

			Statement stmt = null;

			try {

				stmt = conn.createStatement();

				createTable_TourData(stmt);

				createTable_TourPerson(stmt);
				createTable_TourPersonHRZone(stmt);
				createTable_TourType(stmt);
				createTable_TourMarker(stmt);
				createTable_TourPhoto(stmt);
				createTable_TourReference(stmt);
				createTable_TourCompared(stmt);
				createTable_TourBike(stmt);

				createTable_Version(stmt);

				createTable_TourTag(stmt);
				createTable_TourTagCategory(stmt);

				createTable_TourWayPoint(stmt);
				createTable_SharedMarker(stmt);

//				createTable_TourSign(stmt);
//				createTable_TourSignCategory(stmt);

			} catch (final SQLException e) {
				UI.showSQLException(e);
			} finally {
				Util.closeSql(stmt);
			}

		} catch (final SQLException e) {
			UI.showSQLException(e);
		} finally {
			try {
				if (conn != null) {
					conn.close();
				}
			} catch (final SQLException e) {
				UI.showSQLException(e);
			}
		}
	}

	/**
	 * @param monitor
	 *            Progress monitor or <code>null</code> when the monitor is not available
	 * @return
	 */
	private boolean checkVersion(final IProgressMonitor monitor) {

		if (_isVersionChecked) {
			return true;
		}

		if (_isSQLUpdateError) {
			return false;
		}

		Connection conn = null;
		Statement stmt1 = null;
		Statement stmt2 = null;

		try {

			conn = getPooledConnection();
			{
				String sql = "SELECT * FROM " + TABLE_DB_VERSION; //$NON-NLS-1$

				stmt1 = conn.createStatement();
				final ResultSet result = stmt1.executeQuery(sql);

				if (result.next()) {

					// version record was found, check if the database contains the correct version

					_dbVersionBeforeUpdate = result.getInt(1);

					StatusUtil.logInfo("Database version: " + _dbVersionBeforeUpdate); //$NON-NLS-1$

					if (_dbVersionBeforeUpdate < TOURBOOK_DB_VERSION) {

						if (updateDbDesign(conn, _dbVersionBeforeUpdate, monitor) == false) {
							return false;
						}

					} else if (_dbVersionBeforeUpdate > TOURBOOK_DB_VERSION) {

						MessageDialog.openInformation(
								Display.getCurrent().getActiveShell(),
								Messages.tour_database_version_info_title,
								NLS.bind(
										Messages.tour_database_version_info_message,
										_dbVersionBeforeUpdate,
										TOURBOOK_DB_VERSION));
					}

				} else {

					// a version record is not available

					/*
					 * insert the version for the current database design into the database
					 */
					sql = "INSERT INTO " + TABLE_DB_VERSION //								//$NON-NLS-1$
							+ " VALUES (" + Integer.toString(TOURBOOK_DB_VERSION) + ")"; //			//$NON-NLS-1$ //$NON-NLS-2$

					stmt2 = conn.createStatement();
					stmt2.executeUpdate(sql);
				}
			}

			_isVersionChecked = true;

		} catch (final SQLException e) {
			UI.showSQLException(e);
		} finally {
			try {
				conn.close();
			} catch (final SQLException e) {
				UI.showSQLException(e);
			}
			Util.closeSql(stmt1);
			Util.closeSql(stmt2);
		}

		return true;
	}

	/**
	 * Create index for {@link TourData} will dramatically improve performance *
	 * <p>
	 * since db version 5
	 * 
	 * @param stmt
	 * @throws SQLException
	 */
	private void createIndex_TourData_005(final Statement stmt) throws SQLException {

		String sql;

		/*
		 * CREATE INDEX YearMonth
		 */
		sql = "CREATE INDEX YearMonth" + " ON " + TABLE_TOUR_DATA + " (startYear, startMonth)"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		exec(stmt, sql);

		/*
		 * CREATE INDEX TourType
		 */
		sql = "CREATE INDEX TourType" + " ON " + TABLE_TOUR_DATA + " (" + KEY_TYPE + ")"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		exec(stmt, sql);

		/*
		 * CREATE INDEX TourPerson
		 */
		sql = "CREATE INDEX TourPerson" + " ON " + TABLE_TOUR_DATA + " (" + KEY_PERSON + ")"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		exec(stmt, sql);
	}

	/**
	 * Create index for {@link TourData} will dramatically improve performance *
	 * <p>
	 * 
	 * @param stmt
	 * @throws SQLException
	 * @since db version 22
	 */
	private void createIndex_TourData_022(final Statement stmt) throws SQLException {

		String sql;

		/*
		 * CREATE INDEX TourStartTime
		 */
		sql = "CREATE INDEX TourStartTime" + " ON " + TABLE_TOUR_DATA + " (TourStartTime)"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		exec(stmt, sql);

		/*
		 * CREATE INDEX TourEndTime
		 */
		sql = "CREATE INDEX TourEndTime" + " ON " + TABLE_TOUR_DATA + " (TourEndTime)"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		exec(stmt, sql);
	}

	/**
	 * Create table {@link #TABLE_SHARED_MARKER} for {@link SharedMarker} entities.
	 * 
	 * @param stmt
	 * @throws SQLException
	 * @since DB version 25
	 */
	private void createTable_SharedMarker(final Statement stmt) throws SQLException {

		/*
		 * Create table: SharedMarker
		 */
		exec(stmt, "CREATE TABLE " + TABLE_SHARED_MARKER + "	(											\n" //$NON-NLS-1$ //$NON-NLS-2$
				//
				+ SQL.CreateField_EntityId(ENTITY_ID_SHARED_MARKER, true)
				//
				+ "	name				VARCHAR(" + TourWayPoint.DB_LENGTH_NAME + "),						\n" //$NON-NLS-1$ //$NON-NLS-2$
				+ "	description			VARCHAR(" + TourWayPoint.DB_LENGTH_DESCRIPTION + "),				\n" //$NON-NLS-1$ //$NON-NLS-2$
				+ "	comment				VARCHAR(" + TourWayPoint.DB_LENGTH_COMMENT + "),					\n" //$NON-NLS-1$ //$NON-NLS-2$
				+ "	urlText				VARCHAR(" + TourMarker.DB_LENGTH_URL_TEXT + "),						\n" //$NON-NLS-1$ //$NON-NLS-2$
				+ "	urlAddress			VARCHAR(" + TourMarker.DB_LENGTH_URL_ADDRESS + "),					\n" //$NON-NLS-1$ //$NON-NLS-2$
				+ "	latitude 			DOUBLE NOT NULL,													\n" //$NON-NLS-1$
				+ "	longitude 			DOUBLE NOT NULL,													\n" //$NON-NLS-1$
				+ "	altitude			FLOAT																\n" //$NON-NLS-1$
				//
				+ ")"); //$NON-NLS-1$

		/**
		 * Create table: SHAREDMARKER_TOURDATA
		 */
		exec(stmt, "CREATE TABLE " + JOINTABLE__TOURDATA__SHAREDMARKER + "	(								\n" //$NON-NLS-1$ //$NON-NLS-2$
				//
				+ "	" + KEY_SHARED_MARKER + "	BIGINT NOT NULL,											\n"//$NON-NLS-1$ //$NON-NLS-2$
				+ "	" + KEY_TOUR + "			BIGINT NOT NULL												\n"//$NON-NLS-1$ //$NON-NLS-2$
				//
				+ ")"); //$NON-NLS-1$

		// Add Constraint
		final String fkName = "fk_" + JOINTABLE__TOURDATA__SHAREDMARKER + "_" + KEY_TOUR; //							//$NON-NLS-1$ //$NON-NLS-2$
		exec(stmt, "ALTER TABLE " + JOINTABLE__TOURDATA__SHAREDMARKER + "									\n" //$NON-NLS-1$ //$NON-NLS-2$
				+ "	ADD CONSTRAINT " + fkName + "															\n" //$NON-NLS-1$ //$NON-NLS-2$
				+ "	FOREIGN KEY (" + KEY_TOUR + ")															\n" //$NON-NLS-1$ //$NON-NLS-2$
				+ "	REFERENCES " + TABLE_TOUR_DATA + " (" + ENTITY_ID_TOUR + ")								"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	/**
	 * create table {@link #TABLE_TOUR_BIKE}
	 * 
	 * @param stmt
	 * @throws SQLException
	 */
	private void createTable_TourBike(final Statement stmt) throws SQLException {

		/*
		 * CREATE TABLE TourBike
		 */
		exec(stmt, "CREATE TABLE " + TABLE_TOUR_BIKE + "	(												\n" //$NON-NLS-1$ //$NON-NLS-2$
				//
				+ SQL.CreateField_EntityId(ENTITY_ID_BIKE, true)
				//
				+ "	Name			VARCHAR(" + TourBike.DB_LENGTH_NAME + "),								\n" //$NON-NLS-1$ //$NON-NLS-2$
				+ "	Weight 			FLOAT,																	\n" //$NON-NLS-1$ // kg
				+ "	TypeId 			INTEGER,																\n" //$NON-NLS-1$
				+ "	FrontTyreId	 	INTEGER,																\n" //$NON-NLS-1$
				+ "	RearTyreId 		INTEGER																	\n" //$NON-NLS-1$
				//
				+ ")");//$NON-NLS-1$
	}

	/**
	 * create table {@link #TABLE_TOUR_COMPARED}
	 * 
	 * @param stmt
	 * @throws SQLException
	 */
	private void createTable_TourCompared(final Statement stmt) throws SQLException {

		/*
		 * CREATE TABLE TourCompared
		 */
		exec(stmt, "CREATE TABLE " + TABLE_TOUR_COMPARED + "	(											\n" //$NON-NLS-1$ //$NON-NLS-2$
				//
				+ SQL.CreateField_EntityId(ENTITY_ID_COMPARED, true)
				//
				+ "	RefTourId		BIGINT,																	\n" //$NON-NLS-1$
				+ "	TourId			BIGINT,																	\n" //$NON-NLS-1$
				//
				+ "	StartIndex		INTEGER NOT NULL,														\n" //$NON-NLS-1$
				+ "	EndIndex 		INTEGER NOT NULL,														\n" //$NON-NLS-1$
				+ "	TourDate	 	DATE NOT NULL,															\n" //$NON-NLS-1$
				+ "	StartYear		INTEGER NOT NULL,														\n" //$NON-NLS-1$
				+ "	TourSpeed	 	FLOAT																	\n" //$NON-NLS-1$
				//
				+ ")"); //$NON-NLS-1$
	}

	/**
	 * create table {@link #TABLE_TOUR_DATA}
	 * 
	 * @param stmt
	 * @throws SQLException
	 */
	private void createTable_TourData(final Statement stmt) throws SQLException {

		/*
		 * CREATE TABLE TourData
		 */
		exec(stmt, "CREATE TABLE " + TABLE_TOUR_DATA + "	(												\n" //$NON-NLS-1$ //$NON-NLS-2$
				//
				+ SQL.CreateField_EntityId(ENTITY_ID_TOUR, false)
				//
				+ "	StartYear 				SMALLINT NOT NULL,												\n" //$NON-NLS-1$
				+ "	StartMonth 				SMALLINT NOT NULL,												\n" //$NON-NLS-1$
				+ "	StartDay 				SMALLINT NOT NULL,												\n" //$NON-NLS-1$
				+ "	StartHour 				SMALLINT NOT NULL,												\n" //$NON-NLS-1$
				+ "	StartMinute 			SMALLINT NOT NULL,												\n" //$NON-NLS-1$
				+ "	StartWeek 				SMALLINT NOT NULL,												\n" //$NON-NLS-1$
				+ "	StartDistance 			INTEGER NOT NULL,												\n" //$NON-NLS-1$
				+ "	Distance 				INTEGER NOT NULL,												\n" //$NON-NLS-1$
				+ "	StartAltitude 			SMALLINT NOT NULL,												\n" //$NON-NLS-1$
				+ "	StartPulse 				SMALLINT NOT NULL,												\n" //$NON-NLS-1$
				+ "	DpTolerance 			SMALLINT NOT NULL,												\n" //$NON-NLS-1$
				+ "	TourDistance 			INTEGER NOT NULL,												\n" //$NON-NLS-1$

// replaced with BIGINT values in version 22
//				+ "	tourRecordingTime 		INTEGER NOT NULL,												\n" //$NON-NLS-1$
//				+ "	tourDrivingTime 		INTEGER NOT NULL,												\n" //$NON-NLS-1$

				+ "	tourAltUp 				INTEGER NOT NULL,												\n" //$NON-NLS-1$
				+ "	tourAltDown 			INTEGER NOT NULL,												\n" //$NON-NLS-1$

				+ "	deviceTourType			VARCHAR(" + TourData.DB_LENGTH_DEVICE_TOUR_TYPE + "),			\n" //$NON-NLS-1$ //$NON-NLS-2$
				+ "	devicePluginId			VARCHAR(" + TourData.DB_LENGTH_DEVICE_PLUGIN_ID + "),			\n" //$NON-NLS-1$ //$NON-NLS-2$

				+ "	deviceTravelTime 		BIGINT NOT NULL,												\n" //$NON-NLS-1$
				+ "	deviceDistance 			INTEGER NOT NULL,												\n" //$NON-NLS-1$
				+ "	deviceWheel 			INTEGER NOT NULL,												\n" //$NON-NLS-1$
				+ "	deviceWeight 			INTEGER NOT NULL,												\n" //$NON-NLS-1$
				+ "	deviceTotalUp 			INTEGER NOT NULL,												\n" //$NON-NLS-1$
				+ "	deviceTotalDown 		INTEGER NOT NULL,												\n" //$NON-NLS-1$

				// version 3 start
				+ "	deviceMode 				SMALLINT,														\n" //$NON-NLS-1$
				+ "	deviceTimeInterval		SMALLINT,														\n" //$NON-NLS-1$
				// version 3 end

				// version 4 start

				// from markus
// replaced with FLOAT values in version 21
//				+ "	maxAltitude				INTEGER,														\n" //$NON-NLS-1$
//				+ "	maxPulse				INTEGER,														\n" //$NON-NLS-1$
//				+ "	avgPulse				INTEGER,														\n" //$NON-NLS-1$
//				+ "	avgCadence				INTEGER,														\n" //$NON-NLS-1$
//				+ "	avgTemperature			INTEGER,														\n" //$NON-NLS-1$

				+ "	maxSpeed				FLOAT,															\n" //$NON-NLS-1$
				+ "	tourTitle				VARCHAR(" + TourData.DB_LENGTH_TOUR_TITLE + "),					\n" //$NON-NLS-1$ //$NON-NLS-2$

// OLD			+ "	tourDescription			VARCHAR(4096),													\n"	// version <= 9
				+ "	tourDescription			VARCHAR(" + TourData.DB_LENGTH_TOUR_DESCRIPTION_V10 + "),		\n" // modified in version 10 //$NON-NLS-1$ //$NON-NLS-2$

				+ "	tourStartPlace			VARCHAR(" + TourData.DB_LENGTH_TOUR_START_PLACE + "),			\n" //$NON-NLS-1$ //$NON-NLS-2$
				+ "	tourEndPlace			VARCHAR(" + TourData.DB_LENGTH_TOUR_END_PLACE + "),				\n" //$NON-NLS-1$ //$NON-NLS-2$
				+ "	calories				INTEGER,														\n" //$NON-NLS-1$
				+ "	bikerWeight				FLOAT,															\n" //$NON-NLS-1$
				+ "	" + KEY_BIKE + "		BIGINT,															\n" //$NON-NLS-1$ //$NON-NLS-2$

				// from wolfgang
				+ "	devicePluginName		VARCHAR(" + TourData.DB_LENGTH_DEVICE_PLUGIN_NAME + "),			\n" //$NON-NLS-1$ //$NON-NLS-2$
				+ "	deviceModeName			VARCHAR(" + TourData.DB_LENGTH_DEVICE_MODE_NAME + "),			\n" //$NON-NLS-1$ //$NON-NLS-2$

				// version 4 end

				// version 5 start
				/**
				 * disabled because when two blob object's are deserialized then the error occures:
				 * <p>
				 * java.io.StreamCorruptedException: invalid stream header: 00ACED00
				 * <p>
				 * therefor the gpsData are put into the serieData object
				 */
				//	+ "gpsData 				BLOB,															\n" //$NON-NLS-1$
				//
				// version 5 end
				//
				+ "	" + KEY_TYPE + " 		BIGINT,															\n" //$NON-NLS-1$ //$NON-NLS-2$
				+ "	" + KEY_PERSON + " 		BIGINT,															\n" //$NON-NLS-1$ //$NON-NLS-2$

				// version 6 start
				//
				+ "	tourImportFilePath		VARCHAR(" + TourData.DB_LENGTH_TOUR_IMPORT_FILE_PATH + "),		\n" //$NON-NLS-1$ //$NON-NLS-2$
				//
				// version 6 end

				// version 7 start
				//
				+ "	mergeSourceTourId		BIGINT,															\n" //$NON-NLS-1$
				+ "	mergeTargetTourId		BIGINT,															\n" //$NON-NLS-1$
				+ "	mergedTourTimeOffset	INTEGER DEFAULT 0,												\n" //$NON-NLS-1$
				+ "	mergedAltitudeOffset	INTEGER DEFAULT 0,												\n" //$NON-NLS-1$
				+ "	startSecond	 			SMALLINT DEFAULT 0,												\n" //$NON-NLS-1$
				//
				// version 7 end

				// version 8 start
				//
				+ "	weatherWindDir			INTEGER DEFAULT 0,												\n" //$NON-NLS-1$
				+ "	weatherWindSpd			INTEGER DEFAULT 0,												\n" //$NON-NLS-1$
				+ "	weatherClouds			VARCHAR(" + TourData.DB_LENGTH_WEATHER_CLOUDS + "),				\n" //$NON-NLS-1$ //$NON-NLS-2$
				+ "	restPulse        	    INTEGER DEFAULT 0,												\n" //$NON-NLS-1$
				+ "	isDistanceFromSensor 	SMALLINT DEFAULT 0, 											\n" //$NON-NLS-1$
				//
				// version 8 end ----------

				// version 9 start
				//
				+ "	startWeekYear			SMALLINT DEFAULT 1977,											\n" //$NON-NLS-1$
				//
				// version 9 end ----------

				// version 10 start
				//
				// tourWayPoints is mapped in TourData
				//
				// version 10 end----------

				// version 11 start
				//
				+ "	DateTimeCreated			BIGINT DEFAULT 0,												\n" //$NON-NLS-1$
				+ "	DateTimeModified		BIGINT DEFAULT 0,												\n" //$NON-NLS-1$
				//
				// version 11 end ---------

				// version 12 start
				//
				+ "	IsPulseSensorPresent	INTEGER DEFAULT 0, 												\n" //$NON-NLS-1$
				+ "	IsPowerSensorPresent	INTEGER DEFAULT 0, 												\n" //$NON-NLS-1$
				+ "	DeviceAvgSpeed			FLOAT DEFAULT 0,												\n" //$NON-NLS-1$
				+ "	DeviceFirmwareVersion	VARCHAR(" + TourData.DB_LENGTH_DEVICE_FIRMWARE_VERSION + "),	\n" //$NON-NLS-1$ //$NON-NLS-2$
				//
				// version 12 end ---------

				// version 13 start
				//
				+ "	TemperatureScale		INTEGER DEFAULT 1, 												\n" //$NON-NLS-1$
				+ "	Weather					VARCHAR(" + TourData.DB_LENGTH_WEATHER + "),					\n" //$NON-NLS-1$ //$NON-NLS-2$
				//
				// version 13 end ---------

				// version 14 start
				//
				+ "	ConconiDeflection		INTEGER DEFAULT 0, 				\n" //$NON-NLS-1$
				//
				// version 14 end ---------

				// version 17 start
				//
				+ "	hrZone0					INTEGER DEFAULT -1,												\n" //$NON-NLS-1$
				+ "	hrZone1					INTEGER DEFAULT -1, 											\n" //$NON-NLS-1$
				+ "	hrZone2					INTEGER DEFAULT -1, 											\n" //$NON-NLS-1$
				+ "	hrZone3					INTEGER DEFAULT -1, 											\n" //$NON-NLS-1$
				+ "	hrZone4					INTEGER DEFAULT -1, 											\n" //$NON-NLS-1$
				+ "	hrZone5					INTEGER DEFAULT -1, 											\n" //$NON-NLS-1$
				+ "	hrZone6					INTEGER DEFAULT -1, 											\n" //$NON-NLS-1$
				+ "	hrZone7					INTEGER DEFAULT -1, 											\n" //$NON-NLS-1$
				+ "	hrZone8					INTEGER DEFAULT -1, 											\n" //$NON-NLS-1$
				+ "	hrZone9					INTEGER DEFAULT -1, 											\n" //$NON-NLS-1$
				//
				// version 17 end ---------

				// version 18 start
				//
				+ "	NumberOfHrZones			INTEGER DEFAULT 0, 												\n" //$NON-NLS-1$
				//
				// version 18 end ---------

				// version 21 start
				//
				+ "	maxAltitude				FLOAT DEFAULT 0,												\n" //$NON-NLS-1$
				+ "	maxPulse				FLOAT DEFAULT 0,												\n" //$NON-NLS-1$
				+ "	avgPulse				FLOAT DEFAULT 0,												\n" //$NON-NLS-1$
				+ "	avgCadence				FLOAT DEFAULT 0,												\n" //$NON-NLS-1$
				+ "	avgTemperature			FLOAT DEFAULT 0,												\n" //$NON-NLS-1$
				//
				// version 21 end ---------

				// version 22 start  -  12.12.0
				//
				+ "	TourStartTime			BIGINT DEFAULT 0,												\n" //$NON-NLS-1$
				+ "	TourEndTime				BIGINT DEFAULT 0,												\n" //$NON-NLS-1$

				+ "	TourRecordingTime 		BIGINT DEFAULT 0,												\n" //$NON-NLS-1$
				+ "	TourDrivingTime 		BIGINT DEFAULT 0,												\n" //$NON-NLS-1$
				//
				// version 22 end ---------

				// version 23 start  -  13.2.0
				//
				+ "	numberOfTimeSlices		INTEGER DEFAULT 0,												\n" //$NON-NLS-1$
				+ "	numberOfPhotos			INTEGER DEFAULT 0,												\n" //$NON-NLS-1$
				+ "	photoTimeAdjustment		INTEGER DEFAULT 0,												\n" //$NON-NLS-1$
				//
				// version 23 end ---------

				// version 25 start  -  >14.7.0
				//
				+ "	gpsState				INTEGER DEFAULT 0,												\n" //$NON-NLS-1$
				//
				// version 25 end ---------

				+ "	serieData				BLOB 															\n" //$NON-NLS-1$

				+ ")"); //$NON-NLS-1$

		createIndex_TourData_005(stmt);
		createIndex_TourData_022(stmt);
	}

	/**
	 * Create table {@link #TABLE_TOUR_MARKER} for {@link TourMarker}.
	 * 
	 * @param stmt
	 * @throws SQLException
	 */
	private void createTable_TourMarker(final Statement stmt) throws SQLException {

		/*
		 * CREATE TABLE TourMarker
		 */
		exec(stmt, "CREATE TABLE " + TABLE_TOUR_MARKER + "	(												\n" //$NON-NLS-1$ //$NON-NLS-2$
				//
				+ SQL.CreateField_EntityId(ENTITY_ID_MARKER, true)
				//
				+ "	" + KEY_TOUR + "		BIGINT,															\n" //$NON-NLS-1$ //$NON-NLS-2$
				//
				+ "	time 					INTEGER NOT NULL,												\n" //$NON-NLS-1$

				// before version 20
				// + "	distance 			INTEGER NOT NULL,												\n" //$NON-NLS-1$
				+ "	distance 				INTEGER,														\n" //$NON-NLS-1$

				// Version 20 - begin
				//
				+ "	distance20 				FLOAT DEFAULT 0,												\n" //$NON-NLS-1$
				//
				// Version 20 - end

				// Version 22 - begin
				//
				+ "	IsMarkerVisible			INTEGER DEFAULT 1, 												\n" //$NON-NLS-1$
				//
				// Version 22 - end
				//
				// Version 24 - begin
				//
				+ "	description				VARCHAR(" + TourWayPoint.DB_LENGTH_DESCRIPTION + "),			\n" //$NON-NLS-1$ //$NON-NLS-2$
				+ "	urlText					VARCHAR(" + TourMarker.DB_LENGTH_URL_TEXT + "),					\n" //$NON-NLS-1$ //$NON-NLS-2$
				+ "	urlAddress				VARCHAR(" + TourMarker.DB_LENGTH_URL_ADDRESS + "),				\n" //$NON-NLS-1$ //$NON-NLS-2$
				//
				// Version 24 - end
				//
				// Version 25 - begin
				//
				// When DEFAULT value is NOT set, this exception occures:
				//
				//java.lang.IllegalArgumentException: Can not set float field net.tourbook.data.TourMarker.altitude to null value
				//	at sun.reflect.UnsafeFieldAccessorImpl.throwSetIllegalArgumentException(UnsafeFieldAccessorImpl.java:176)
				//	at sun.reflect.UnsafeFieldAccessorImpl.throwSetIllegalArgumentException(UnsafeFieldAccessorImpl.java:180)
				//	at sun.reflect.UnsafeFloatFieldAccessorImpl.set(UnsafeFloatFieldAccessorImpl.java:92)
				//	at java.lang.reflect.Field.set(Field.java:753)
				//	at org.hibernate.property.DirectPropertyAccessor$DirectSetter.set(DirectPropertyAccessor.java:102)
				//
				+ "	altitude				FLOAT DEFAULT " + FLOAT_MIN_VALUE + ",							\n" //$NON-NLS-1$
				+ "	latitude 				DOUBLE DEFAULT " + DOUBLE_MIN_VALUE + ",						\n" //$NON-NLS-1$
				+ "	longitude 				DOUBLE DEFAULT " + DOUBLE_MIN_VALUE + ",						\n" //$NON-NLS-1$
				//
				// Version 25 - end
				//
				+ "	serieIndex 				INTEGER NOT NULL,												\n" //$NON-NLS-1$
				+ "	type 					INTEGER NOT NULL,												\n" //$NON-NLS-1$
				+ "	visualPosition			INTEGER NOT NULL,												\n" //$NON-NLS-1$
				+ "	label					VARCHAR(" + TourWayPoint.DB_LENGTH_NAME + "),					\n" //$NON-NLS-1$ //$NON-NLS-2$
				+ "	category				VARCHAR(" + TourWayPoint.DB_LENGTH_CATEGORY + "),				\n" //$NON-NLS-1$ //$NON-NLS-2$

				//
				// Version 2
				+ "	labelXOffset			INTEGER,														\n" //$NON-NLS-1$
				+ "	labelYOffset			INTEGER,														\n" //$NON-NLS-1$
				+ "	markerType				BIGINT															\n" //$NON-NLS-1$
				//
				+ ")"); //$NON-NLS-1$
	}

	/**
	 * create table {@link #TABLE_TOUR_PERSON}
	 * 
	 * @param stmt
	 * @throws SQLException
	 */
	private void createTable_TourPerson(final Statement stmt) throws SQLException {

		/*
		 * CREATE TABLE TourPerson
		 */
		exec(stmt, "CREATE TABLE " + TABLE_TOUR_PERSON + "	(												\n" //$NON-NLS-1$ //$NON-NLS-2$
				//
				+ SQL.CreateField_EntityId(ENTITY_ID_PERSON, true)
				//
				+ "	lastName			VARCHAR(" + TourPerson.DB_LENGTH_LAST_NAME + "),					\n" //$NON-NLS-1$ //$NON-NLS-2$
				+ "	firstName			VARCHAR(" + TourPerson.DB_LENGTH_FIRST_NAME + "),					\n" //$NON-NLS-1$ //$NON-NLS-2$
				+ "	weight 				FLOAT,																\n" //$NON-NLS-1$ // kg
				+ "	height 				FLOAT,																\n" //$NON-NLS-1$ // m

				// version 15 start
				//
				+ "	BirthDay			BIGINT DEFAULT 0,													\n" //$NON-NLS-1$
				//
				// version 15 end ---------

				// version 16 start
				//
				+ "	Gender				INTEGER DEFAULT 0,													\n" //$NON-NLS-1$
				+ "	RestPulse			INTEGER DEFAULT 0,													\n" //$NON-NLS-1$
				+ "	MaxPulse			INTEGER DEFAULT 0,													\n" //$NON-NLS-1$
				+ "	HrMaxFormula		INTEGER DEFAULT 0,													\n" //$NON-NLS-1$
				//
				// version 16 end ---------

				+ "	rawDataPath			VARCHAR(" + TourPerson.DB_LENGTH_RAW_DATA_PATH + "),				\n" //$NON-NLS-1$ //$NON-NLS-2$
				+ "	deviceReaderId		VARCHAR(" + TourPerson.DB_LENGTH_DEVICE_READER_ID + "),				\n" //$NON-NLS-1$ //$NON-NLS-2$
				//
				+ "	" + KEY_BIKE + " 	BIGINT																\n" //$NON-NLS-1$ //$NON-NLS-2$
				//
				+ ")"); //$NON-NLS-1$
	}

	/**
	 * Create table {@link #TABLE_TOUR_PERSON_HRZONE} for {@link TourPersonHRZone}.
	 * <p>
	 * Table is available since db version 16
	 * 
	 * @param stmt
	 * @throws SQLException
	 */
	private void createTable_TourPersonHRZone(final Statement stmt) throws SQLException {

		/*
		 * CREATE TABLE TourPersonHRZone
		 */
		exec(stmt, "CREATE TABLE " + TABLE_TOUR_PERSON_HRZONE + "	(										\n" //$NON-NLS-1$ //$NON-NLS-2$
				//
				+ SQL.CreateField_EntityId(ENTITY_ID_HR_ZONE, true)
				//
				+ "	" + KEY_PERSON + "	BIGINT,																\n" //$NON-NLS-1$ //$NON-NLS-2$
				//
				+ "	zoneName			VARCHAR(" + TourPersonHRZone.DB_LENGTH_ZONE_NAME + "),				\n" //$NON-NLS-1$ //$NON-NLS-2$
				+ "	nameShortcut		VARCHAR(" + TourPersonHRZone.DB_LENGTH_ZONE_NAME + "),				\n" //$NON-NLS-1$ //$NON-NLS-2$
				+ "	description			VARCHAR(" + TourPersonHRZone.DB_LENGTH_DESCRIPTION + "),			\n" //$NON-NLS-1$ //$NON-NLS-2$

				// version 18 start
				//
				+ "	ColorRed			INTEGER DEFAULT 0, 													\n" //$NON-NLS-1$
				+ "	ColorGreen			INTEGER DEFAULT 0, 													\n" //$NON-NLS-1$
				+ "	ColorBlue			INTEGER DEFAULT 0, 													\n" //$NON-NLS-1$
				//
				// version 18 end ---------

				//
				+ "	zoneMinValue		INTEGER NOT NULL,													\n" //$NON-NLS-1$
				+ "	zoneMaxValue		INTEGER NOT NULL													\n" //$NON-NLS-1$
				//
				+ ")"); //$NON-NLS-1$
	}

	/**
	 * Create table {@link #TABLE_TOUR_PHOTO}
	 * 
	 * @param stmt
	 * @throws SQLException
	 */
	private void createTable_TourPhoto(final Statement stmt) throws SQLException {

		/*
		 * CREATE TABLE TourPhoto
		 */
		exec(stmt, "CREATE TABLE " + TABLE_TOUR_PHOTO + "	(												\n" //$NON-NLS-1$ //$NON-NLS-2$
				//
				+ SQL.CreateField_EntityId(ENTITY_ID_PHOTO, true)
				//
				+ "	" + KEY_TOUR + "			BIGINT,														\n" //$NON-NLS-1$ //$NON-NLS-2$
				//
				// version 23 start
				//
				+ "	imageFileName				VARCHAR(" + TourPhoto.DB_LENGTH_FILE_PATH + "),				\n" //$NON-NLS-1$ //$NON-NLS-2$
				+ "	imageFileExt				VARCHAR(" + TourPhoto.DB_LENGTH_FILE_PATH + "),				\n" //$NON-NLS-1$ //$NON-NLS-2$
				+ "	imageFilePath				VARCHAR(" + TourPhoto.DB_LENGTH_FILE_PATH + "),				\n" //$NON-NLS-1$ //$NON-NLS-2$
				+ "	imageFilePathName			VARCHAR(" + TourPhoto.DB_LENGTH_FILE_PATH + "),				\n" //$NON-NLS-1$ //$NON-NLS-2$
				+ "	imageExifTime				BIGINT DEFAULT 0,											\n" //$NON-NLS-1$
				+ "	imageFileLastModified		BIGINT DEFAULT 0,											\n" //$NON-NLS-1$
				//
				+ "	adjustedTime				BIGINT DEFAULT 0,											\n" //$NON-NLS-1$
				//
				+ "	ratingStars					INT DEFAULT 0,												\n" //$NON-NLS-1$
				+ "	isGeoFromPhoto				INT DEFAULT 0,												\n" //$NON-NLS-1$
				+ "	latitude 					DOUBLE DEFAULT 0,											\n" //$NON-NLS-1$
				+ "	longitude 					DOUBLE DEFAULT 0											\n" //$NON-NLS-1$
				//
				// version 23 end
				+ ")"); //$NON-NLS-1$

		// Create index for {@link TourPhoto}, it will dramatically improve performance.
		SQL.CreateIndex(stmt, TABLE_TOUR_PHOTO, "ImageFilePathName"); //$NON-NLS-1$
	}

	/**
	 * create table {@link #TABLE_TOUR_REFERENCE}
	 * 
	 * @param stmt
	 * @throws SQLException
	 */
	private void createTable_TourReference(final Statement stmt) throws SQLException {

		/*
		 * CREATE TABLE TourReference
		 */
		exec(stmt, "CREATE TABLE " + TABLE_TOUR_REFERENCE + "	(											\n" //$NON-NLS-1$ //$NON-NLS-2$
				//
				+ SQL.CreateField_EntityId(ENTITY_ID_REF, true)
				//
				+ "	" + KEY_TOUR + "	BIGINT,																\n"//$NON-NLS-1$ //$NON-NLS-2$
				//
				+ "	startIndex			INTEGER NOT NULL,													\n" //$NON-NLS-1$
				+ "	endIndex 			INTEGER NOT NULL,													\n" //$NON-NLS-1$
				+ "	label				VARCHAR(" + TourReference.DB_LENGTH_LABEL + ")						\n" //$NON-NLS-1$ //$NON-NLS-2$
				+ ")"); //$NON-NLS-1$
	}

	/**
	 * Create table {@link #TABLE_TOUR_SIGN} for {@link TourSign}.
	 * 
	 * @param stmt
	 * @throws SQLException
	 * @since DB version 24
	 */
	private void createTable_TourSign_DISABLED(final Statement stmt) throws SQLException {

//		/*
//		 * Create table: TourSign
//		 */
//		exec(stmt, "CREATE TABLE " + TABLE_TOUR_SIGN + "	(												\n" //$NON-NLS-1$ //$NON-NLS-2$
//				//
//				+ SQL.CreateField_EntityId(ENTITY_ID_SIGN, true)
//				//
//				+ "	name				VARCHAR(" + TourSign.DB_LENGTH_NAME + "),							\n" //$NON-NLS-1$ //$NON-NLS-2$
//				+ "	imageFilePathName	VARCHAR(" + TourSign.DB_LENGTH_IMAGE_FILE_PATH + "),				\n" //$NON-NLS-1$ //$NON-NLS-2$
//				+ "	expandType			INT DEFAULT " + TourSign.EXPAND_TYPE_DEFAULT + ",					\n" //$NON-NLS-1$ //$NON-NLS-2$
//				//
//				+ "	isRoot 				INT DEFAULT 0														\n" //$NON-NLS-1$
//				//
//				+ ")"); //$NON-NLS-1$
	}

	/**
	 * Create table {@link #TABLE_TOUR_TAG} which contains {@link TourTag} entities.
	 * 
	 * @param stmt
	 * @throws SQLException
	 * @since DB version 5
	 */
	private void createTable_TourTag(final Statement stmt) throws SQLException {

		/*
		 * Create table: TOURTAG
		 */
		exec(stmt, "CREATE TABLE " + TABLE_TOUR_TAG + "	(													\n" //$NON-NLS-1$ //$NON-NLS-2$
				//
				+ SQL.CreateField_EntityId(ENTITY_ID_TAG, true)
				//
				+ "	isRoot 						INTEGER,													\n" //$NON-NLS-1$
				+ "	expandType 					INTEGER,													\n" //$NON-NLS-1$
				+ "	name						VARCHAR(" + TourTag.DB_LENGTH_NAME + ")						\n" //$NON-NLS-1$ //$NON-NLS-2$
				//
				+ ")"); //$NON-NLS-1$

		/**
		 * Create table: TOURDATA_TOURTAG
		 */
		exec(stmt, "CREATE TABLE " + JOINTABLE__TOURDATA__TOURTAG + "	(									\n" //$NON-NLS-1$ //$NON-NLS-2$
				//
				+ "	" + KEY_TAG + "				BIGINT NOT NULL,											\n"//$NON-NLS-1$ //$NON-NLS-2$
				+ "	" + KEY_TOUR + "			BIGINT NOT NULL												\n"//$NON-NLS-1$ //$NON-NLS-2$
				//
				+ ")"); //$NON-NLS-1$

		// Add Constraint
		final String fkName = "fk_" + JOINTABLE__TOURDATA__TOURTAG + "_" + KEY_TOUR; //							//$NON-NLS-1$ //$NON-NLS-2$
		exec(stmt, "ALTER TABLE " + JOINTABLE__TOURDATA__TOURTAG + "										\n" //$NON-NLS-1$ //$NON-NLS-2$
				+ "	ADD CONSTRAINT " + fkName + "															\n" //$NON-NLS-1$ //$NON-NLS-2$
				+ "	FOREIGN KEY (" + KEY_TOUR + ")															\n" //$NON-NLS-1$ //$NON-NLS-2$
				+ "	REFERENCES " + TABLE_TOUR_DATA + " (" + ENTITY_ID_TOUR + ")								"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	/**
	 * Create table {@link #TABLE_TOUR_TAG_CATEGORY} for which contains {@link TourTagCategory}
	 * entities.
	 * 
	 * @param stmt
	 * @throws SQLException
	 * @since DB version 5
	 */
	private void createTable_TourTagCategory(final Statement stmt) throws SQLException {

		/*
		 * Create table: TOURTAGCATEGORY
		 */
		exec(stmt, "CREATE TABLE " + TABLE_TOUR_TAG_CATEGORY + "	(										\n" //$NON-NLS-1$ //$NON-NLS-2$
				//
				+ SQL.CreateField_EntityId(ENTITY_ID_TAG_CATEGORY, true)
				//
				+ "	isRoot 			INTEGER,																\n" //$NON-NLS-1$
				+ "	name			VARCHAR(" + TourTagCategory.DB_LENGTH_NAME + ")							\n" //$NON-NLS-1$ //$NON-NLS-2$
				//
				+ ")" //$NON-NLS-1$
		);

		/*
		 * Create table: TOURTAGCATEGORY_TOURTAG
		 */
		final String jtabTag = JOINTABLE__TOURTAGCATEGORY_TOURTAG;

		exec(stmt, "CREATE TABLE " + jtabTag + "	(														\n" //$NON-NLS-1$ //$NON-NLS-2$
				//
				+ "	" + KEY_TAG + "					BIGINT NOT NULL,										\n"//$NON-NLS-1$ //$NON-NLS-2$
				+ "	" + KEY_TAG_CATEGORY + "		BIGINT NOT NULL											\n"//$NON-NLS-1$ //$NON-NLS-2$
				//
				+ ")"); //$NON-NLS-1$

		// add constraints
		final String fkTag = "fk_" + jtabTag + "_" + KEY_TAG; //											//$NON-NLS-1$ //$NON-NLS-2$
		final String fkCat = "fk_" + jtabTag + "_" + TABLE_TOUR_TAG_CATEGORY + "_" + KEY_TAG_CATEGORY; //			//$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

		exec(stmt, "ALTER TABLE " + jtabTag + "																\n"//$NON-NLS-1$ //$NON-NLS-2$
				+ "	ADD CONSTRAINT " + fkTag + "															\n"//$NON-NLS-1$ //$NON-NLS-2$
				+ "	FOREIGN KEY (" + KEY_TAG + ")															\n"//$NON-NLS-1$ //$NON-NLS-2$
				+ "	REFERENCES " + TABLE_TOUR_TAG + " (" + ENTITY_ID_TAG + ")								\n"//$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		);

		exec(stmt, "ALTER TABLE " + jtabTag + "																\n"//$NON-NLS-1$ //$NON-NLS-2$
				+ "	ADD CONSTRAINT " + fkCat + "															\n"//$NON-NLS-1$ //$NON-NLS-2$
				+ "	FOREIGN KEY (" + KEY_TAG_CATEGORY + ")													\n"//$NON-NLS-1$ //$NON-NLS-2$
				+ "	REFERENCES " + TABLE_TOUR_TAG_CATEGORY + " (" + ENTITY_ID_TAG_CATEGORY + ")				\n"//$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		);

		/*
		 * Create table: TOURTAGCATEGORY_TOURTAGCATEGORY
		 */
		final String jtabCategory = JOINTABLE__TOURTAGCATEGORY_TOURTAGCATEGORY;

		exec(stmt, "CREATE TABLE " + jtabCategory + "	(													\n"//$NON-NLS-1$ //$NON-NLS-2$
				//
				+ "	" + KEY_TAG_CATEGORY + "1	BIGINT NOT NULL,											\n"//$NON-NLS-1$ //$NON-NLS-2$
				+ "	" + KEY_TAG_CATEGORY + "2	BIGINT NOT NULL												\n"//$NON-NLS-1$ //$NON-NLS-2$
				//
				+ ")"); //$NON-NLS-1$

		// add constraints
		final String fk1 = "fk_" + jtabCategory + "_" + KEY_TAG_CATEGORY + "1"; //							//$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		final String fk2 = "fk_" + jtabCategory + "_" + KEY_TAG_CATEGORY + "2"; //							//$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

		exec(stmt, "ALTER TABLE " + jtabCategory + "														\n"//$NON-NLS-1$ //$NON-NLS-2$
				+ "	ADD CONSTRAINT " + fk1 + "																\n"//$NON-NLS-1$ //$NON-NLS-2$
				+ "	FOREIGN KEY (" + KEY_TAG_CATEGORY + "1)													\n"//$NON-NLS-1$ //$NON-NLS-2$
				+ "	REFERENCES " + TABLE_TOUR_TAG_CATEGORY + " (" + ENTITY_ID_TAG_CATEGORY + ")				\n"//$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		);

		exec(stmt, "ALTER TABLE " + jtabCategory + "														\n"//$NON-NLS-1$ //$NON-NLS-2$
				+ "	ADD CONSTRAINT " + fk2 + "																\n"//$NON-NLS-1$ //$NON-NLS-2$
				+ "	FOREIGN KEY (" + KEY_TAG_CATEGORY + "2)													\n"//$NON-NLS-1$ //$NON-NLS-2$
				+ "	REFERENCES " + TABLE_TOUR_TAG_CATEGORY + " (" + ENTITY_ID_TAG_CATEGORY + ")				\n"//$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		);
	}

	/**
	 * create table {@link #TABLE_TOUR_TYPE}
	 * 
	 * @param stmt
	 * @throws SQLException
	 */
	private void createTable_TourType(final Statement stmt) throws SQLException {

		/*
		 * CREATE TABLE TourType
		 */
		//															//$NON-NLS-1$

		exec(stmt, "CREATE TABLE " + TABLE_TOUR_TYPE + "	(												\n" //$NON-NLS-1$ //$NON-NLS-2$
				//
				+ SQL.CreateField_EntityId(ENTITY_ID_TYPE, true)
				//
				+ "	name				VARCHAR(" + TourType.DB_LENGTH_NAME + "),							\n" //$NON-NLS-1$ //$NON-NLS-2$
				//
				+ "	colorBrightRed 		SMALLINT NOT NULL,													\n" //$NON-NLS-1$
				+ "	colorBrightGreen 	SMALLINT NOT NULL,													\n" //$NON-NLS-1$
				+ "	colorBrightBlue 	SMALLINT NOT NULL,													\n" //$NON-NLS-1$
				+ "	colorDarkRed 		SMALLINT NOT NULL,													\n" //$NON-NLS-1$
				+ "	colorDarkGreen 		SMALLINT NOT NULL,													\n" //$NON-NLS-1$
				+ "	colorDarkBlue 		SMALLINT NOT NULL,													\n" //$NON-NLS-1$
				+ "	colorLineRed 		SMALLINT NOT NULL,													\n" //$NON-NLS-1$
				+ "	colorLineGreen 		SMALLINT NOT NULL,													\n" //$NON-NLS-1$
				+ "	colorLineBlue 		SMALLINT NOT NULL,													\n" //$NON-NLS-1$

				// version 19 start
				//
				+ "	colorTextRed 		SMALLINT DEFAULT 0,													\n" //$NON-NLS-1$
				+ "	colorTextGreen 		SMALLINT DEFAULT 0,													\n" //$NON-NLS-1$
				+ "	colorTextBlue 		SMALLINT DEFAULT 0													\n" //$NON-NLS-1$
				//
				// version 19 end ---------
				//
				+ ")"); //$NON-NLS-1$
	}

	/**
	 * create table {@link #TABLE_TOUR_WAYPOINT} *
	 * <p>
	 * since db version 10
	 * 
	 * @param stmt
	 * @throws SQLException
	 */
	private void createTable_TourWayPoint(final Statement stmt) throws SQLException {

		/*
		 * CREATE TABLE TourWayPoint
		 */
		exec(stmt, "CREATE TABLE " + TABLE_TOUR_WAYPOINT + "	(											\n" //$NON-NLS-1$ //$NON-NLS-2$
				//
				+ SQL.CreateField_EntityId(ENTITY_ID_WAY_POINT, true)
				//
				+ "	" + KEY_TOUR + "	BIGINT,																\n" //$NON-NLS-1$ //$NON-NLS-2$
				//
				+ "	latitude 			DOUBLE NOT NULL,													\n" //$NON-NLS-1$
				+ "	longitude 			DOUBLE NOT NULL,													\n" //$NON-NLS-1$
				+ "	time				BIGINT,																\n" //$NON-NLS-1$
				+ "	altitude			FLOAT,																\n" //$NON-NLS-1$
				+ "	name				VARCHAR(" + TourWayPoint.DB_LENGTH_NAME + "),						\n" //$NON-NLS-1$ //$NON-NLS-2$
				+ "	description			VARCHAR(" + TourWayPoint.DB_LENGTH_DESCRIPTION + "),				\n" //$NON-NLS-1$ //$NON-NLS-2$
				+ "	comment				VARCHAR(" + TourWayPoint.DB_LENGTH_COMMENT + "),					\n" //$NON-NLS-1$ //$NON-NLS-2$
				+ "	symbol				VARCHAR(" + TourWayPoint.DB_LENGTH_SYMBOL + "),						\n" //$NON-NLS-1$ //$NON-NLS-2$
				+ "	category			VARCHAR(" + TourWayPoint.DB_LENGTH_CATEGORY + ")					\n" //$NON-NLS-1$ //$NON-NLS-2$
				//
				+ ")"); //$NON-NLS-1$
	}

	/**
	 * create table {@link #TABLE_DB_VERSION}
	 * 
	 * @param stmt
	 * @throws SQLException
	 */
	private void createTable_Version(final Statement stmt) throws SQLException {

		/*
		 * CREATE TABLE Version
		 */
		exec(stmt, "CREATE TABLE " + TABLE_DB_VERSION + " (													\n" //$NON-NLS-1$ //$NON-NLS-2$
				//
				+ "	version 	INTEGER	NOT NULL															\n" //$NON-NLS-1$
				//
				+ ")"); //$NON-NLS-1$
	}

	private String createUIServerStateMessage(final int stateCounter) {

		final StringBuilder sb = new StringBuilder();

		for (int stateIndex = 1; stateIndex <= MAX_TRIES_TO_PING_SERVER + 1; stateIndex++) {
			sb.append(stateIndex <= stateCounter ? ':' : '.');
		}

		return NLS.bind(Messages.Database_Monitor_db_service_task, sb.toString());
	}

	public void firePropertyChange(final int propertyId) {
		final Object[] allListeners = _propertyListeners.getListeners();
		for (final Object allListener : allListeners) {
			final IPropertyListener listener = (IPropertyListener) allListener;
			listener.propertyChanged(TourDatabase.this, propertyId);
		}
	}

	public Connection getConnection() throws SQLException {

		if (checkDb()) {
			return getPooledConnection();
		} else {
			return null;
		}
	}

	/**
	 * Creates an entity manager which is used to persist entities
	 * 
	 * @return
	 */
	public EntityManager getEntityManager() {

		if (_emFactory == null) {
			getEntityManagerCreate();
		}

		if (_emFactory == null) {
			try {
				throw new Exception("Cannot get EntityManagerFactory"); //$NON-NLS-1$
			} catch (final Exception e) {
				StatusUtil.log(e);
			}
			return null;
		} else {
			return _emFactory.createEntityManager();
		}
	}

	private synchronized void getEntityManagerCreate() {

		try {

			final Map<String, Object> configOverrides = new HashMap<String, Object>();

			configOverrides.put("hibernate.connection.url", DERBY_URL); //$NON-NLS-1$
			configOverrides.put("hibernate.connection.driver_class", DERBY_DRIVER_CLASS); //$NON-NLS-1$

			final MyTourbookSplashHandler splashHandler = TourbookPlugin.getSplashHandler();

			if (splashHandler == null) {
				try {
					checkServer();
				} catch (final Throwable e) {
					StatusUtil.showStatus(e);
					return;
				}
				checkTable(null);
				checkVersion(null);

				_emFactory = Persistence.createEntityManagerFactory(PERSISTENCE_UNIT_NAME, configOverrides);

			} else {

				new IRunnableWithProgress() {
					@Override
					public void run(final IProgressMonitor monitor) throws InvocationTargetException,
							InterruptedException {

						try {
							checkServer();
						} catch (final Throwable e) {
							return;
						}

						checkTable(monitor);
						checkVersion(monitor);

						monitor.subTask(Messages.Database_Monitor_persistent_service_task);

						_emFactory = Persistence.createEntityManagerFactory(PERSISTENCE_UNIT_NAME, configOverrides);
					}
				}.run(splashHandler.getBundleProgressMonitor());
			}

		} catch (final InvocationTargetException e) {
			StatusUtil.log(e);
		} catch (final InterruptedException e) {
			StatusUtil.log(e);
		}
	}

	private boolean isColumnAvailable(final Connection conn, final String table, final String column) {

		try {

			final DatabaseMetaData meta = conn.getMetaData();

			final ResultSet result = meta.getColumns(null, TABLE_SCHEMA, table, column.toUpperCase());

			while (result.next()) {
				return true;
			}

			/*
			 * dump all columns
			 */
//			final DatabaseMetaData meta = conn.getMetaData();
//			final ResultSet result = meta.getColumns(null, TABLE_SCHEMA, table, NULL);
//
//			while (result.next()) {
//				System.out.println("  "
//						+ result.getString("TABLE_SCHEM")
//						+ ", "
//						+ result.getString("TABLE_NAME")
//						+ ", "
//						+ result.getString("COLUMN_NAME")
//						+ ", "
//						+ result.getString("TYPE_NAME")
//						+ ", "
//						+ result.getInt("COLUMN_SIZE")
//						+ ", "
//						+ result.getString("NULLABLE"));
//			}

		} catch (final SQLException e) {
			UI.showSQLException(e);
		}

		return false;
	}

	private boolean isTableAvailable(final Connection conn, final String table) {

		try {

			final DatabaseMetaData meta = conn.getMetaData();
			final ResultSet result = meta.getTables(null, TABLE_SCHEMA, table.toUpperCase(), null);

			while (result.next()) {
				return true;
			}

//			/*
//			 * dump all columns
//			 */
//			final DatabaseMetaData meta = conn.getMetaData();
////			final ResultSet result = meta.getTables(null, null, null, new String[] {"TABLE"});
//			final ResultSet result = meta.getTables(null, null, null, null);
//
//			while (result.next()) {
//				System.out.println(("   " + result.getString("TABLE_CAT"))
//						+ (", " + result.getString("TABLE_SCHEM"))
//						+ (", " + result.getString("TABLE_NAME"))
//						+ (", " + result.getString("TABLE_TYPE"))
//						+ (", " + result.getString("REMARKS")));
//			}
////				TABLE_CAT String => table catalog (may be null)
////				TABLE_SCHEM String => table schema (may be null)
////				TABLE_NAME String => table name
////				TABLE_TYPE String => table type. Typical types are "TABLE", "VIEW", "SYSTEM TABLE", "GLOBAL TEMPORARY", "LOCAL TEMPORARY", "ALIAS", "SYNONYM".
////				REMARKS String => explanatory comment on the table
////				TYPE_CAT String => the types catalog (may be null)
////				TYPE_SCHEM String => the types schema (may be null)
////				TYPE_NAME String => type name (may be null)
////				SELF_REFERENCING_COL_NAME String => name of the designated "identifier" column of a typed table (may be null)
////				REF_GENERATION String => specifies how values in SELF_REFERENCING_COL_NAME are created. Values are "SYSTEM", "USER", "DERIVED". (may be null)

		} catch (final SQLException e) {
			UI.showSQLException(e);
		}

		return false;
	}

	private void logDbUpdateEnd(final int dbVersion) {
		System.out.println(NLS.bind(Messages.Tour_Database_UpdateDone, dbVersion));
		System.out.println();
	}

	private void logDbUpdateStart(final int dbVersion) {
		System.out.println();
		System.out.println(NLS.bind(Messages.Tour_Database_Update, dbVersion));
	}

	private void modifyColumn_Type(	final String table,
									final String fieldName,
									final String newFieldType,
									final Statement stmt,
									final IProgressMonitor monitor,
									final int no,
									final int max) throws SQLException {

		if (monitor != null) {
			monitor.subTask(NLS.bind(Messages.Tour_Database_Update_ModifyColumn, new Object[] { no, max }));
		}

		//	"ALTER TABLE t ADD COLUMN c1_newtype NEWTYPE"
		//	"UPDATE t SET c1_newtype = c1"
		//	"ALTER TABLE t DROP COLUMN c1"
		//	"RENAME COLUMN t.c1_newtype TO c1"

		String sql;
		final String tempFieldName = fieldName + "_temp";//$NON-NLS-1$

		sql = "ALTER TABLE " + table + " ADD COLUMN " + tempFieldName + " " + newFieldType; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		exec(stmt, sql);

		sql = "UPDATE " + table + " SET " + tempFieldName + " = " + fieldName; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		execUpdate(stmt, sql);

		sql = "ALTER TABLE " + table + " DROP COLUMN " + fieldName; //$NON-NLS-1$ //$NON-NLS-2$
		exec(stmt, sql);

		sql = "RENAME COLUMN " + table + "." + tempFieldName + " TO " + fieldName; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		exec(stmt, sql);
	}

	public void removePropertyListener(final IPropertyListener listener) {
		_propertyListeners.remove(listener);
	}

	/**
	 * this must be implemented or updated when the database version must be updated
	 */
	private boolean updateDbDesign(final Connection conn, int currentDbVersion, final IProgressMonitor monitor) {

		/*
		 * confirm update
		 */

		// define buttons with default to NO
		final String[] buttons = new String[] { IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL };

		if ((new MessageDialog(
				Display.getDefault().getActiveShell(),
				Messages.Database_Confirm_update_title,
				null,
				NLS.bind(Messages.Database_Confirm_update, new Object[] {
						currentDbVersion,
						TOURBOOK_DB_VERSION,
						_databasePath }),
				MessageDialog.QUESTION,
				buttons,
				1).open()) != Window.OK) {

			// no update -> close application
			PlatformUI.getWorkbench().close();

			return false;
		}

		/*
		 * do an additional check because version 20 is restructuring the data series
		 */
		if (currentDbVersion < 20) {

			if ((new MessageDialog(
					Display.getDefault().getActiveShell(),
					Messages.Database_Confirm_update_title,
					null,
					NLS.bind(Messages.Database_Confirm_Update20, _databasePath),
					MessageDialog.QUESTION,
					buttons,
					1).open()) != Window.OK) {

				// no update -> close application
				PlatformUI.getWorkbench().close();

				return false;
			}
		}

		int newVersion = currentDbVersion;
		final int oldVersion = currentDbVersion;

		/*
		 * database update
		 */
		try {

			if (currentDbVersion == 1) {
				updateDbDesign_001_002(conn);
				currentDbVersion = newVersion = 2;
			}

			if (currentDbVersion == 2) {
				updateDbDesign_002_003(conn);
				currentDbVersion = newVersion = 3;
			}

			if (currentDbVersion == 3) {
				updateDbDesign_003_004(conn, monitor);
				currentDbVersion = newVersion = 4;
			}

			boolean isPostUpdate5 = false;
			if (currentDbVersion == 4) {
				updateDbDesign_004_005(conn, monitor);
				currentDbVersion = newVersion = 5;
				isPostUpdate5 = true;
			}

			if (currentDbVersion == 5) {
				updateDbDesign_005_006(conn, monitor);
				currentDbVersion = newVersion = 6;
			}

			if (currentDbVersion == 6) {
				updateDbDesign_006_007(conn, monitor);
				currentDbVersion = newVersion = 7;
			}

			if (currentDbVersion == 7) {
				updateDbDesign_007_008(conn, monitor);
				currentDbVersion = newVersion = 8;
			}

			boolean isPostUpdate9 = false;
			if (currentDbVersion == 8) {
				updateDbDesign_008_009(conn, monitor);
				currentDbVersion = newVersion = 9;
				isPostUpdate9 = true;
			}

			if (currentDbVersion == 9) {
				updateDbDesign_009_010(conn, monitor);
				currentDbVersion = newVersion = 10;
			}

			boolean isPostUpdate11 = false;
			if (currentDbVersion == 10) {
				currentDbVersion = newVersion = updateDbDesign_010_011(conn, monitor);
				isPostUpdate11 = true;
			}

			if (currentDbVersion == 11) {
				currentDbVersion = newVersion = updateDbDesign_011_012(conn, monitor);
			}

			boolean isPostUpdate13 = false;
			if (currentDbVersion == 12) {
				currentDbVersion = newVersion = updateDbDesign_012_013(conn, monitor);
				isPostUpdate13 = true;
			}

			if (currentDbVersion == 13) {
				currentDbVersion = newVersion = updateDbDesign_013_014(conn, monitor);
			}

			if (currentDbVersion == 14) {
				currentDbVersion = newVersion = updateDbDesign_014_015(conn, monitor);
			}

			if (currentDbVersion == 15) {
				currentDbVersion = newVersion = updateDbDesign_015_to_016(conn, monitor);
			}

			if (currentDbVersion == 16) {
				currentDbVersion = newVersion = updateDbDesign_016_to_017(conn, monitor);
			}

			if (currentDbVersion == 17) {
				currentDbVersion = newVersion = updateDbDesign_017_to_018(conn, monitor);
			}

			if (currentDbVersion == 18) {
				currentDbVersion = newVersion = updateDbDesign_018_to_019(conn, monitor);
			}

			boolean isPostUpdate20 = false;
			if (currentDbVersion == 19) {
				currentDbVersion = newVersion = updateDbDesign_019_to_020(conn, monitor);
				isPostUpdate20 = true;
			}

			/*
			 * 21
			 */
			if (currentDbVersion == 20) {
				currentDbVersion = newVersion = updateDbDesign_020_to_021(conn, monitor);
			}

			/*
			 * 22
			 */
			boolean isPostUpdate22 = false;

			if (currentDbVersion == 21) {
				currentDbVersion = newVersion = updateDbDesign_021_to_022(conn, monitor);
				isPostUpdate22 = true;
			}

			/*
			 * 23
			 */
			boolean isPostUpdate23 = false;

			if (currentDbVersion == 22) {
				currentDbVersion = newVersion = updateDbDesign_022_to_023(conn, monitor);
				isPostUpdate23 = true;
			}

			/*
			 * 24
			 */
			if (currentDbVersion == 23) {
				currentDbVersion = newVersion = updateDbDesign_023_to_024(conn, monitor);
			}

			/*
			 * 24 -> 25
			 */
			boolean isPostUpdate25 = false;

			if (currentDbVersion == 24) {
				isPostUpdate25 = true;
				currentDbVersion = newVersion = updateDbDesign_024_to_025(conn, monitor);
			}

			/*
			 * update version number
			 */
			updateDbVersionNumber(conn, newVersion);

			/**
			 * Do post update after the version number is updated because the post update uses
			 * connections and entitymanager which is checking the version number.
			 * <p>
			 * Also the data structure must be updated otherwise the entity manager fails because
			 * the data structure in the programm code MUST be the same as in the database.
			 */
			if (isPostUpdate5) {
				TourDatabase.computeComputedValuesForAllTours(monitor);
				TourManager.getInstance().removeAllToursFromCache();
			}
			if (isPostUpdate9) {
				updateDbDesign_008_009_PostUpdate(conn, monitor);
			}
			if (isPostUpdate11) {
				updateDbDesign_010_011_PostUpdate(conn, monitor);
			}
			if (isPostUpdate13) {
				updateDbDesign_012_013_PostUpdate(conn, monitor);
			}
			if (isPostUpdate20) {
				updateDbDesign_019_to_020_PostUpdate(conn, monitor);
			}
			if (isPostUpdate22) {
				updateDbDesign_021_to_022_PostUpdate(conn, monitor);
			}
			if (isPostUpdate23) {
				updateDbDesign_022_to_023_PostUpdate(conn, monitor);
			}
			if (isPostUpdate25) {
				updateDbDesign_024_to_025_PostUpdate(conn, monitor);
			}

		} catch (final SQLException e) {
			UI.showSQLException(e);
			_isSQLUpdateError = true;
			return false;
		}

		// display info for the successful update
		MessageDialog.openInformation(
				Display.getCurrent().getActiveShell(),
				Messages.tour_database_version_info_title,
				NLS.bind(Messages.Tour_Database_UpdateInfo, oldVersion, newVersion));

		return true;
	}

	private void updateDbDesign_001_002(final Connection conn) throws SQLException {

		final int dbVersion = 2;

		logDbUpdateStart(dbVersion);

		String sql;
		final Statement stmt = conn.createStatement();
		{
			sql = "ALTER TABLE " + TABLE_TOUR_MARKER + " ADD COLUMN labelXOffset	INTEGER DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
			exec(stmt, sql);

			sql = "ALTER TABLE " + TABLE_TOUR_MARKER + " ADD COLUMN labelYOffset	INTEGER DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
			exec(stmt, sql);

			sql = "ALTER TABLE " + TABLE_TOUR_MARKER + " ADD COLUMN markerType		BIGINT DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
			exec(stmt, sql);
		}
		stmt.close();

		logDbUpdateEnd(dbVersion);
	}

	private void updateDbDesign_002_003(final Connection conn) throws SQLException {

		final int dbVersion = 3;

		logDbUpdateStart(dbVersion);

		String sql;
		final Statement stmt = conn.createStatement();
		{
			sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN deviceMode			SMALLINT DEFAULT -1"; //$NON-NLS-1$ //$NON-NLS-2$
			exec(stmt, sql);

			sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN deviceTimeInterval	SMALLINT DEFAULT -1"; //$NON-NLS-1$ //$NON-NLS-2$
			exec(stmt, sql);
		}
		stmt.close();

		logDbUpdateEnd(dbVersion);
	}

	private void updateDbDesign_003_004(final Connection conn, final IProgressMonitor monitor) throws SQLException {

		final int dbVersion = 4;

		logDbUpdateStart(dbVersion);

		String sql;
		final Statement stmt = conn.createStatement();
		{
			sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN	maxAltitude			INTEGER DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
			exec(stmt, sql);

			sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN	maxPulse			INTEGER DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
			exec(stmt, sql);

			sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN	avgPulse			INTEGER DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
			exec(stmt, sql);

			sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN	avgCadence			INTEGER DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
			exec(stmt, sql);

			sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN	avgTemperature		INTEGER DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
			exec(stmt, sql);

			sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN	maxSpeed			FLOAT DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
			exec(stmt, sql);

			sql = ("ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN	tourTitle			VARCHAR(" + TourData.DB_LENGTH_TOUR_TITLE + ")\n"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			exec(stmt, sql);

			sql = ("ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN	tourDescription		VARCHAR(" + TourData.DB_LENGTH_TOUR_DESCRIPTION + ")\n"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			exec(stmt, sql);

			sql = ("ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN	tourStartPlace		VARCHAR(" + TourData.DB_LENGTH_TOUR_START_PLACE + ")\n"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			exec(stmt, sql);

			sql = ("ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN	tourEndPlace		VARCHAR(" + TourData.DB_LENGTH_TOUR_END_PLACE + ")\n"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			exec(stmt, sql);

			sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN	calories			INTEGER DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
			exec(stmt, sql);

			sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN	bikerWeight			FLOAT DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
			exec(stmt, sql);

			sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN	" + KEY_BIKE + "		BIGINT"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			exec(stmt, sql);

			// from wolfgang
			sql = ("ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN	devicePluginName	VARCHAR(" + TourData.DB_LENGTH_DEVICE_PLUGIN_NAME + ")\n"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			exec(stmt, sql);

			// from wolfgang
			sql = ("ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN	deviceModeName		VARCHAR(" + TourData.DB_LENGTH_DEVICE_MODE_NAME + ")\n"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			exec(stmt, sql);
		}

		stmt.close();

		// Create a EntityManagerFactory here, so we can access TourData with EJB
		if (monitor != null) {
			monitor.subTask(Messages.Database_Monitor_persistent_service_task);
		}
		_emFactory = Persistence.createEntityManagerFactory(PERSISTENCE_UNIT_NAME);

		if (monitor != null) {
			monitor.subTask(Messages.Tour_Database_load_all_tours);
		}
		final ArrayList<Long> tourList = getAllTourIds();

		// loop over all tours and calculate and set new columns
		int tourIdx = 1;
		for (final Long tourId : tourList) {

			final TourData tourData = getTourFromDb(tourId);

			if (monitor != null) {
				final String msg = NLS.bind(
						Messages.Tour_Database_update_tour,
						new Object[] { tourIdx++, tourList.size() });
				monitor.subTask(msg);
			}

			tourData.computeComputedValues();

			final TourPerson person = tourData.getTourPerson();
			tourData.setTourBike(person.getTourBike());
			tourData.setBikerWeight(person.getWeight());

			saveTour(tourData, false);
		}

		// cleanup everything as if nothing has happened
		_emFactory.close();
		_emFactory = null;

		logDbUpdateEnd(dbVersion);
	}

	private void updateDbDesign_004_005(final Connection conn, final IProgressMonitor monitor) throws SQLException {

		final int dbVersion = 5;

		logDbUpdateStart(dbVersion);

		if (monitor != null) {
			monitor.subTask(NLS.bind(Messages.Tour_Database_Update, 5));
		}

		final Statement stmt = conn.createStatement();
		{
			createTable_TourTag(stmt);
			createTable_TourTagCategory(stmt);
			createIndex_TourData_005(stmt);
		}
		stmt.close();

		logDbUpdateEnd(dbVersion);
	}

	private void updateDbDesign_005_006(final Connection conn, final IProgressMonitor monitor) throws SQLException {

		final int dbVersion = 6;

		logDbUpdateStart(dbVersion);

		if (monitor != null) {
			monitor.subTask(NLS.bind(Messages.Tour_Database_Update, 6));
		}

		String sql;
		final Statement stmt = conn.createStatement();
		{
			sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN	tourImportFilePath	VARCHAR(" + TourData.DB_LENGTH_TOUR_IMPORT_FILE_PATH + ")\n"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			exec(stmt, sql);
		}
		stmt.close();

		logDbUpdateEnd(dbVersion);
	}

	private void updateDbDesign_006_007(final Connection conn, final IProgressMonitor monitor) throws SQLException {

		final int dbVersion = 7;

		logDbUpdateStart(dbVersion);

		if (monitor != null) {
			monitor.subTask(NLS.bind(Messages.Tour_Database_Update, 7));
		}

		String sql;
		final Statement stmt = conn.createStatement();
		{
			sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN mergeSourceTourId		BIGINT"; //				//$NON-NLS-1$ //$NON-NLS-2$
			exec(stmt, sql);

			sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN mergeTargetTourId		BIGINT"; //				//$NON-NLS-1$ //$NON-NLS-2$
			exec(stmt, sql);

			sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN mergedTourTimeOffset	INTEGER DEFAULT 0"; //	//$NON-NLS-1$ //$NON-NLS-2$
			exec(stmt, sql);

			sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN mergedAltitudeOffset	INTEGER DEFAULT 0"; //	//$NON-NLS-1$ //$NON-NLS-2$
			exec(stmt, sql);

			sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN startSecond			SMALLINT DEFAULT 0"; //	//$NON-NLS-1$ //$NON-NLS-2$
			exec(stmt, sql);
		}
		stmt.close();

		logDbUpdateEnd(dbVersion);
	}

	private void updateDbDesign_007_008(final Connection conn, final IProgressMonitor monitor) throws SQLException {

		final int dbVersion = 8;

		logDbUpdateStart(dbVersion);

		if (monitor != null) {
			monitor.subTask(NLS.bind(Messages.Tour_Database_Update, 8));
		}

		String sql;
		final Statement stmt = conn.createStatement();
		{
			sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN	weatherWindDir			INTEGER DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
			exec(stmt, sql);

			sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN	weatherWindSpd			INTEGER DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
			exec(stmt, sql);

			sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN	isDistanceFromSensor	SMALLINT DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
			exec(stmt, sql);

			sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN	weatherClouds			VARCHAR(" + TourData.DB_LENGTH_WEATHER_CLOUDS + ")\n"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			exec(stmt, sql);

			sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN	restPulse    		    INTEGER DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
			exec(stmt, sql);
		}
		stmt.close();

		logDbUpdateEnd(dbVersion);
	}

	private void updateDbDesign_008_009(final Connection conn, final IProgressMonitor monitor) throws SQLException {

		final int dbVersion = 9;

		logDbUpdateStart(dbVersion);

		if (monitor != null) {
			monitor.subTask(NLS.bind(Messages.Tour_Database_Update, dbVersion));
		}

		String sql;
		final Statement stmt = conn.createStatement();
		{
			sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN startWeekYear		SMALLINT DEFAULT 1977 "; //$NON-NLS-1$ //$NON-NLS-2$
			exec(stmt, sql);
		}
		stmt.close();

		logDbUpdateEnd(dbVersion);
	}

	private void updateDbDesign_008_009_PostUpdate(final Connection conn, final IProgressMonitor monitor)
			throws SQLException {

		// set ISO 8601 week number
		final int firstDayOfWeek = Calendar.MONDAY;
		final int minimalDaysInFirstWeek = 4;

		if (updateTourWeek(conn, monitor, firstDayOfWeek, minimalDaysInFirstWeek)) {
			MessageDialog.openInformation(
					Display.getDefault().getActiveShell(),
					Messages.Database_Confirm_update_title,
					Messages.Tour_Database_Update_TourWeek_Info);
		}
	}

	private void updateDbDesign_009_010(final Connection conn, final IProgressMonitor monitor) throws SQLException {

		final int dbVersion = 10;

		logDbUpdateStart(dbVersion);

		if (monitor != null) {
			monitor.subTask(NLS.bind(Messages.Tour_Database_Update, dbVersion));
		}

		final Statement stmt = conn.createStatement();
		{
			createTable_TourWayPoint(stmt);

			/**
			 * Resize description column: ref derby docu page 24
			 * 
			 * <pre>
			 * 
			 * ALTER TABLE table-Name
			 * {
			 *     ADD COLUMN column-definition |
			 *     ADD CONSTRAINT clause |
			 *     DROP [ COLUMN ] column-name [ CASCADE | RESTRICT ]
			 *     DROP { PRIMARY KEY | FOREIGN KEY constraint-name | UNIQUE
			 *   		constraint-name | CHECK constraint-name | CONSTRAINT constraint-name }
			 *     ALTER [ COLUMN ] column-alteration |
			 *     LOCKSIZE { ROW | TABLE }
			 * 
			 *     column-alteration
			 * 
			 * 		column-Name SET DATA TYPE VARCHAR(integer) |
			 * 		column-Name SET DATA TYPE VARCHAR FOR BIT DATA(integer) |
			 * 		column-name SET INCREMENT BY integer-constant |
			 * 		column-name RESTART WITH integer-constant |
			 * 		column-name [ NOT ] NULL |
			 * 		column-name [ WITH | SET ] DEFAULT default-value |
			 * 		column-name DROP DEFAULT
			 * }
			 * </pre>
			 */

			final String sql = //
			"ALTER TABLE " + TABLE_TOUR_DATA + "															\n" //$NON-NLS-1$ //$NON-NLS-2$
					+ "	ALTER COLUMN tourDescription														\n" //$NON-NLS-1$
					+ "	SET DATA TYPE	VARCHAR(" + TourData.DB_LENGTH_TOUR_DESCRIPTION_V10 + ")			\n"; //$NON-NLS-1$ //$NON-NLS-2$

			exec(stmt, sql);
		}

		stmt.close();

		logDbUpdateEnd(dbVersion);
	}

	private int updateDbDesign_010_011(final Connection conn, final IProgressMonitor monitor) throws SQLException {

		final int dbVersion = 11;

		logDbUpdateStart(dbVersion);

		if (monitor != null) {
			monitor.subTask(NLS.bind(Messages.Tour_Database_Update, dbVersion));
		}

		String sql;
		final Statement stmt = conn.createStatement();
		{
			sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN DateTimeCreated		BIGINT	DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
			exec(stmt, sql);

			sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN DateTimeModified		BIGINT	DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
			exec(stmt, sql);
		}
		stmt.close();

		logDbUpdateEnd(dbVersion);

		return dbVersion;
	}

	/**
	 * Set create date/time from the tour date
	 * 
	 * @param conn
	 * @param monitor
	 * @throws SQLException
	 */
	private void updateDbDesign_010_011_PostUpdate(final Connection conn, final IProgressMonitor monitor)
			throws SQLException {

		final PreparedStatement stmtSelect = conn.prepareStatement(//
				//
				"SELECT" //									//$NON-NLS-1$
							//
						+ " StartYear," // 					// 1 //$NON-NLS-1$
						+ " StartMonth," // 				// 2 //$NON-NLS-1$
						+ " StartDay," // 					// 3 //$NON-NLS-1$
						+ " StartHour," // 					// 4 //$NON-NLS-1$
						+ " StartMinute," // 				// 5 //$NON-NLS-1$
						+ " StartSecond" // 				// 6 //$NON-NLS-1$
						//
						+ " FROM " + TABLE_TOUR_DATA //		//$NON-NLS-1$
						+ " WHERE TourId=?" //				$NON-NLS-1$ //$NON-NLS-1$
				);

		final PreparedStatement stmtUpdate = conn.prepareStatement(//
				//
				"UPDATE " + TABLE_TOUR_DATA //				//$NON-NLS-1$
						//
						+ " SET" //							//$NON-NLS-1$
						//
						+ " DateTimeCreated=?" //			// 1 //$NON-NLS-1$
						//
						+ " WHERE tourId=?"); //			// 2 //$NON-NLS-1$

		int tourIdx = 1;
		final ArrayList<Long> tourList = getAllTourIds();

		// loop: all tours
		for (final Long tourId : tourList) {

			if (monitor != null) {
				monitor.subTask(NLS.bind(//
						Messages.Tour_Database_PostUpdate011_SetTourCreateTime,
						new Object[] { tourIdx++, tourList.size() }));
			}

			// get tour date
			stmtSelect.setLong(1, tourId);
			final ResultSet result = stmtSelect.executeQuery();

			while (result.next()) {

				// get date from database
				final short dbYear = result.getShort(1);
				final short dbMonth = result.getShort(2);
				final short dbDay = result.getShort(3);
				final short dbHour = result.getShort(4);
				final short dbMinute = result.getShort(5);
				final short dbSecond = result.getShort(6);

				final long dtCreated = (dbYear * 10000000000L)
						+ (dbMonth * 100000000L)
						+ (dbDay * 1000000L)
						+ (dbHour * 10000L)
						+ (dbMinute * 100L)
						+ dbSecond;

				// update DateTimeCreated in the database
				stmtUpdate.setLong(1, dtCreated);
				stmtUpdate.setLong(2, tourId);
				stmtUpdate.executeUpdate();
			}
		}
	}

	private int updateDbDesign_011_012(final Connection conn, final IProgressMonitor monitor) throws SQLException {

		final int newDbVersion = 12;

		logDbUpdateStart(newDbVersion);

		if (monitor != null) {
			monitor.subTask(NLS.bind(Messages.Tour_Database_Update, newDbVersion));
		}

		String sql;
		final Statement stmt = conn.createStatement();
		{
//				+ "	IsPulseSensorPresent		INTEGER DEFAULT 0, 				\n" //$NON-NLS-1$
//				+ "	IsPowerSensorPresent		INTEGER DEFAULT 0, 				\n" //$NON-NLS-1$
//				+ "	DeviceAvgSpeed				FLOAT DEFAULT 0,				\n" //$NON-NLS-1$
//				+ "	DeviceFirmwareVersion	" + varCharKomma(TourData.DB_LENGTH_DEVICE_FIRMWARE_VERSION)) //$NON-NLS-1$

			sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN	IsPulseSensorPresent	INTEGER	DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
			exec(stmt, sql);

			sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN	IsPowerSensorPresent	INTEGER	DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
			exec(stmt, sql);

			sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN	DeviceAvgSpeed			FLOAT DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
			exec(stmt, sql);

			sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN	DeviceFirmwareVersion	VARCHAR(" + TourData.DB_LENGTH_DEVICE_FIRMWARE_VERSION + ")\n"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			exec(stmt, sql);
		}
		stmt.close();

		logDbUpdateEnd(newDbVersion);

		return newDbVersion;
	}

	private int updateDbDesign_012_013(final Connection conn, final IProgressMonitor monitor) throws SQLException {

		final int newDbVersion = 13;

		logDbUpdateStart(newDbVersion);

		if (monitor != null) {
			monitor.subTask(NLS.bind(Messages.Tour_Database_Update, newDbVersion));
		}

//			+ "	TemperatureScale			INTEGER DEFAULT 1, 				\n" //$NON-NLS-1$
//			+ " Weather 					" + varCharNoKomma(TourData.DB_LENGTH_WEATHER) //$NON-NLS-1$

		String sql;
		final Statement stmt = conn.createStatement();
		{
			sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN	TemperatureScale		INTEGER	DEFAULT 1"; //$NON-NLS-1$ //$NON-NLS-2$
			exec(stmt, sql);

			sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN	Weather					VARCHAR(" + TourData.DB_LENGTH_WEATHER + ")\n"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			exec(stmt, sql);
		}
		stmt.close();

		logDbUpdateEnd(newDbVersion);

		return newDbVersion;
	}

	/**
	 * Set temperature scale default value
	 * 
	 * @param conn
	 * @param monitor
	 * @throws SQLException
	 */
	private void updateDbDesign_012_013_PostUpdate(final Connection conn, final IProgressMonitor monitor)
			throws SQLException {

		final String sql = "UPDATE " + TABLE_TOUR_DATA + " SET TemperatureScale=1"; //$NON-NLS-1$ //$NON-NLS-2$

		System.out.println(sql);
		System.out.println();

		conn.createStatement().executeUpdate(sql);
	}

	private int updateDbDesign_013_014(final Connection conn, final IProgressMonitor monitor) throws SQLException {

		final int newDbVersion = 14;

		logDbUpdateStart(newDbVersion);

		if (monitor != null) {
			monitor.subTask(NLS.bind(Messages.Tour_Database_Update, newDbVersion));
		}

//		+ "	ConconiDeflection			INTEGER DEFAULT 0, 				\n" //$NON-NLS-1$

		String sql;
		final Statement stmt = conn.createStatement();
		{
			sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN ConconiDeflection			INTEGER DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
			exec(stmt, sql);
		}
		stmt.close();

		logDbUpdateEnd(newDbVersion);

		return newDbVersion;
	}

	private int updateDbDesign_014_015(final Connection conn, final IProgressMonitor monitor) throws SQLException {

		final int newDbVersion = 15;

		logDbUpdateStart(newDbVersion);

		if (monitor != null) {
			monitor.subTask(NLS.bind(Messages.Tour_Database_Update, newDbVersion));
		}

//		TOURPERSON TOURPERSON TOURPERSON TOURPERSON TOURPERSON TOURPERSON
//
//		+ "	BirthDay			BIGINT DEFAULT 0,						\n" //$NON-NLS-1$

		String sql;
		final Statement stmt = conn.createStatement();
		{
			sql = "ALTER TABLE " + TABLE_TOUR_PERSON + " ADD COLUMN BirthDay			BIGINT DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
			exec(stmt, sql);
		}
		stmt.close();

		logDbUpdateEnd(newDbVersion);

		return newDbVersion;
	}

	private int updateDbDesign_015_to_016(final Connection conn, final IProgressMonitor monitor) throws SQLException {

		final int newDbVersion = 16;

		logDbUpdateStart(newDbVersion);

		if (monitor != null) {
			monitor.subTask(NLS.bind(Messages.Tour_Database_Update, newDbVersion));
		}

		String sql;
		final Statement stmt = conn.createStatement();
		{

			createTable_TourPersonHRZone(stmt);

//		TOURPERSON TOURPERSON TOURPERSON TOURPERSON TOURPERSON TOURPERSON
//
//		+ "	Gender				INTEGER DEFAULT 0,						\n" //$NON-NLS-1$
//		+ "	RestPulse			INTEGER DEFAULT 0,						\n" //$NON-NLS-1$
//		+ "	MaxPulse			INTEGER DEFAULT 0,						\n" //$NON-NLS-1$
//		+ "	HrMaxFormula		INTEGER DEFAULT 0,						\n" //$NON-NLS-1$
//
//		TOURPERSON TOURPERSON TOURPERSON TOURPERSON TOURPERSON TOURPERSON

			sql = "ALTER TABLE " + TABLE_TOUR_PERSON + " ADD COLUMN Gender				INTEGER DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
			exec(stmt, sql);

			sql = "ALTER TABLE " + TABLE_TOUR_PERSON + " ADD COLUMN RestPulse			INTEGER DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
			exec(stmt, sql);

			sql = "ALTER TABLE " + TABLE_TOUR_PERSON + " ADD COLUMN MaxPulse			INTEGER DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
			exec(stmt, sql);

			sql = "ALTER TABLE " + TABLE_TOUR_PERSON + " ADD COLUMN HrMaxFormula		INTEGER DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
			exec(stmt, sql);
		}
		stmt.close();

		logDbUpdateEnd(newDbVersion);

		return newDbVersion;
	}

	private int updateDbDesign_016_to_017(final Connection conn, final IProgressMonitor monitor) throws SQLException {

		final int newDbVersion = 17;

		logDbUpdateStart(newDbVersion);

		String sql;
		final Statement stmt = conn.createStatement();
		{

//			TOURDATA	TOURDATA	TOURDATA	TOURDATA	TOURDATA	TOURDATA	TOURDATA	TOURDATA
//
//			+ "	hrZone0						INTEGER DEFAULT -1,				\n" //$NON-NLS-1$
//			+ "	hrZone1						INTEGER DEFAULT -1, 			\n" //$NON-NLS-1$
//			+ "	hrZone2						INTEGER DEFAULT -1, 			\n" //$NON-NLS-1$
//			+ "	hrZone3						INTEGER DEFAULT -1, 			\n" //$NON-NLS-1$
//			+ "	hrZone4						INTEGER DEFAULT -1, 			\n" //$NON-NLS-1$
//			+ "	hrZone5						INTEGER DEFAULT -1, 			\n" //$NON-NLS-1$
//			+ "	hrZone6						INTEGER DEFAULT -1, 			\n" //$NON-NLS-1$
//			+ "	hrZone7						INTEGER DEFAULT -1, 			\n" //$NON-NLS-1$
//			+ "	hrZone8						INTEGER DEFAULT -1, 			\n" //$NON-NLS-1$
//			+ "	hrZone9						INTEGER DEFAULT -1, 			\n" //$NON-NLS-1$
//
//			TOURDATA	TOURDATA	TOURDATA	TOURDATA	TOURDATA	TOURDATA	TOURDATA	TOURDATA

			if (monitor != null) {
				monitor.subTask(NLS.bind(Messages.Tour_Database_Update_Subtask, newDbVersion, 0));
			}
			sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN HrZone0		INTEGER DEFAULT -1"; //$NON-NLS-1$ //$NON-NLS-2$
			exec(stmt, sql);

			if (monitor != null) {
				monitor.subTask(NLS.bind(Messages.Tour_Database_Update_Subtask, newDbVersion, 1));
			}
			sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN HrZone1		INTEGER DEFAULT -1"; //$NON-NLS-1$ //$NON-NLS-2$
			exec(stmt, sql);

			if (monitor != null) {
				monitor.subTask(NLS.bind(Messages.Tour_Database_Update_Subtask, newDbVersion, 2));
			}
			sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN HrZone2		INTEGER DEFAULT -1"; //$NON-NLS-1$ //$NON-NLS-2$
			exec(stmt, sql);

			if (monitor != null) {
				monitor.subTask(NLS.bind(Messages.Tour_Database_Update_Subtask, newDbVersion, 3));
			}
			sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN HrZone3		INTEGER DEFAULT -1"; //$NON-NLS-1$ //$NON-NLS-2$
			exec(stmt, sql);

			if (monitor != null) {
				monitor.subTask(NLS.bind(Messages.Tour_Database_Update_Subtask, newDbVersion, 4));
			}
			sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN HrZone4		INTEGER DEFAULT -1"; //$NON-NLS-1$ //$NON-NLS-2$
			exec(stmt, sql);

			if (monitor != null) {
				monitor.subTask(NLS.bind(Messages.Tour_Database_Update_Subtask, newDbVersion, 5));
			}
			sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN HrZone5		INTEGER DEFAULT -1"; //$NON-NLS-1$ //$NON-NLS-2$
			exec(stmt, sql);

			if (monitor != null) {
				monitor.subTask(NLS.bind(Messages.Tour_Database_Update_Subtask, newDbVersion, 6));
			}
			sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN HrZone6		INTEGER DEFAULT -1"; //$NON-NLS-1$ //$NON-NLS-2$
			exec(stmt, sql);

			if (monitor != null) {
				monitor.subTask(NLS.bind(Messages.Tour_Database_Update_Subtask, newDbVersion, 7));
			}
			sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN HrZone7		INTEGER DEFAULT -1"; //$NON-NLS-1$ //$NON-NLS-2$
			exec(stmt, sql);

			if (monitor != null) {
				monitor.subTask(NLS.bind(Messages.Tour_Database_Update_Subtask, newDbVersion, 8));
			}
			sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN HrZone8		INTEGER DEFAULT -1"; //$NON-NLS-1$ //$NON-NLS-2$
			exec(stmt, sql);

			if (monitor != null) {
				monitor.subTask(NLS.bind(Messages.Tour_Database_Update_Subtask, newDbVersion, 9));
			}
			sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN HrZone9		INTEGER DEFAULT -1"; //$NON-NLS-1$ //$NON-NLS-2$
			exec(stmt, sql);
		}
		stmt.close();

		logDbUpdateEnd(newDbVersion);

		return newDbVersion;
	}

	private int updateDbDesign_017_to_018(final Connection conn, final IProgressMonitor monitor) throws SQLException {

		final int newDbVersion = 18;

		logDbUpdateStart(newDbVersion);

		if (monitor != null) {
			monitor.subTask(NLS.bind(Messages.Tour_Database_Update, newDbVersion));
		}

		String sql;
		final Statement stmt = conn.createStatement();
		{

			if (_dbVersionBeforeUpdate > 16) {

				/*
				 * db update 16 creates the HR zone db, doing this update causes an sql exception
				 * because the fields are already available
				 */

//				TABLE_TOUR_PERSON_HRZONE	TABLE_TOUR_PERSON_HRZONE	TABLE_TOUR_PERSON_HRZONE	TABLE_TOUR_PERSON_HRZONE
//
//				// version 18 start
//				//
//				+ "	ColorRed			INTEGER DEFAULT 0, 					\n" //$NON-NLS-1$
//				+ "	ColorGreen			INTEGER DEFAULT 0, 					\n" //$NON-NLS-1$
//				+ "	ColorBlue			INTEGER DEFAULT 0, 					\n" //$NON-NLS-1$
//				//
//				// version 18 end ---------
//
//				TABLE_TOUR_PERSON_HRZONE	TABLE_TOUR_PERSON_HRZONE	TABLE_TOUR_PERSON_HRZONE	TABLE_TOUR_PERSON_HRZONE

				sql = "ALTER TABLE " + TABLE_TOUR_PERSON_HRZONE + " ADD COLUMN ColorRed		INTEGER DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
				exec(stmt, sql);

				sql = "ALTER TABLE " + TABLE_TOUR_PERSON_HRZONE + " ADD COLUMN ColorGreen	INTEGER DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
				exec(stmt, sql);

				sql = "ALTER TABLE " + TABLE_TOUR_PERSON_HRZONE + " ADD COLUMN ColorBlue	INTEGER DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
				exec(stmt, sql);
			}

//			TOURDATA	TOURDATA	TOURDATA	TOURDATA	TOURDATA	TOURDATA	TOURDATA	TOURDATA
//
//			// version 18 start
//			//
//			+ "	NumberOfHrZones				INTEGER DEFAULT 0, 				\n" //$NON-NLS-1$
//			//
//			// version 18 end ---------
//
//			TOURDATA	TOURDATA	TOURDATA	TOURDATA	TOURDATA	TOURDATA	TOURDATA	TOURDATA

			sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN NumberOfHrZones	INTEGER DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
			exec(stmt, sql);

		}
		stmt.close();

		logDbUpdateEnd(newDbVersion);

		return newDbVersion;
	}

	private int updateDbDesign_018_to_019(final Connection conn, final IProgressMonitor monitor) throws SQLException {

		final int newDbVersion = 19;

		logDbUpdateStart(newDbVersion);

		if (monitor != null) {
			monitor.subTask(NLS.bind(Messages.Tour_Database_Update, newDbVersion));
		}

		String sql;
		final Statement stmt = conn.createStatement();
		{
//			TOUR_TYPE	TOUR_TYPE	TOUR_TYPE	TOUR_TYPE	TOUR_TYPE	TOUR_TYPE	TOUR_TYPE	TOUR_TYPE
//			//
//			// version 19 start
//			//
//			+ "	colorTextRed 		SMALLINT DEFAULT 0,						\n" //$NON-NLS-1$
//			+ "	colorTextGreen 		SMALLINT DEFAULT 0,						\n" //$NON-NLS-1$
//			+ "	colorTextBlue 		SMALLINT DEFAULT 0						\n" //$NON-NLS-1$
//			//
//			// version 19 end ---------
//			//
//			TOUR_TYPE	TOUR_TYPE	TOUR_TYPE	TOUR_TYPE	TOUR_TYPE	TOUR_TYPE	TOUR_TYPE	TOUR_TYPE

			sql = "ALTER TABLE " + TABLE_TOUR_TYPE + " ADD COLUMN colorTextRed		SMALLINT DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
			exec(stmt, sql);

			sql = "ALTER TABLE " + TABLE_TOUR_TYPE + " ADD COLUMN colorTextGreen	SMALLINT DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
			exec(stmt, sql);

			sql = "ALTER TABLE " + TABLE_TOUR_TYPE + " ADD COLUMN colorTextBlue		SMALLINT DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
			exec(stmt, sql);

		}
		stmt.close();

		logDbUpdateEnd(newDbVersion);

		return newDbVersion;
	}

	private int updateDbDesign_019_to_020(final Connection conn, final IProgressMonitor monitor) throws SQLException {

		final int newDbVersion = 20;

		logDbUpdateStart(newDbVersion);

		if (monitor != null) {
			monitor.subTask(NLS.bind(Messages.Tour_Database_Update, newDbVersion));
		}

		{
			updateDbDesign_019_to_020_10DataSerieBlobSize(conn);
			updateDbDesign_019_to_020_20AlterColumns(conn);
		}

		logDbUpdateEnd(newDbVersion);

		return newDbVersion;
	}

	/**
	 * Increase {@link TourData#serieData} blob size.
	 * 
	 * @param conn
	 * @throws SQLException
	 */
	private void updateDbDesign_019_to_020_10DataSerieBlobSize(final Connection conn) throws SQLException {

		final DatabaseMetaData meta = conn.getMetaData();

		final ResultSet rsColumns = meta.getColumns(null, TABLE_SCHEMA, TABLE_TOUR_DATA, "SERIEDATA"); //$NON-NLS-1$

		while (rsColumns.next()) {

			final int size = rsColumns.getInt("COLUMN_SIZE"); //$NON-NLS-1$
			if (size == 1048576) {

				/*
				 * database is from a derby version before 10.5 which creates BLOB's with a default
				 * size of 1M, increase size to 2G because a tour with 53000 can not be saves and
				 * causes an exception
				 */

				String sql;
				final Statement stmt = conn.createStatement();
				{
					// ALTER TABLE TourData ALTER COLUMN SerieData SET DATA TYPE BLOB(2G)

					sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ALTER COLUMN SerieData SET DATA TYPE BLOB(2G)"; //$NON-NLS-1$ //$NON-NLS-2$
					exec(stmt, sql);
				}
				stmt.close();
			}

			break;
		}
	}

	private void updateDbDesign_019_to_020_20AlterColumns(final Connection conn) throws SQLException {

		String sql;
		final Statement stmt = conn.createStatement();
		{
			// remove the NOT NULL constraint from the "distance" column
			sql = "ALTER TABLE " + TABLE_TOUR_MARKER + " ALTER COLUMN distance	NULL"; //$NON-NLS-1$ //$NON-NLS-2$
			exec(stmt, sql);

			//			+ "	distance20 					FLOAT DEFAULT 0,					\n" //$NON-NLS-1$
			sql = "ALTER TABLE " + TABLE_TOUR_MARKER + " ADD COLUMN distance20	FLOAT DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
			exec(stmt, sql);
		}
		stmt.close();
	}

	/**
	 * @param conn
	 * @param monitor
	 * @throws SQLException
	 */
	private void updateDbDesign_019_to_020_PostUpdate(final Connection conn, final IProgressMonitor monitor)
			throws SQLException {

		IS_POST_UPDATE_019_to_020 = true;

		final long startTime = System.currentTimeMillis();

		int tourIdx = 1;
		final ArrayList<Long> tourList = getAllTourIds();

		final EntityManager em = TourDatabase.getInstance().getEntityManager();
		try {

			// loop: all tours
			for (final Long tourId : tourList) {

				final long currentTime = System.currentTimeMillis();

				if (monitor != null) {

					final float durationInSeconds = (currentTime - startTime) / 1000;

					monitor.subTask(NLS.bind(//
							Messages.Tour_Database_PostUpdate020_ConvertIntToFloat,
							new Object[] { tourIdx, tourList.size(), (int) durationInSeconds }));

					tourIdx++;
				}

				final TourData tourData = em.find(TourData.class, tourId);
				if (tourData != null) {

					tourData.updateDatabase_019_to_020();

					TourDatabase.saveEntity(tourData, tourId, TourData.class);
				}

			}

		} catch (final Exception e) {
			e.printStackTrace();
		} finally {

			IS_POST_UPDATE_019_to_020 = false;

			em.close();
		}
	}

	private int updateDbDesign_020_to_021(final Connection conn, final IProgressMonitor monitor) throws SQLException {

		final int newDbVersion = 21;

		logDbUpdateStart(newDbVersion);

		if (monitor != null) {
			monitor.subTask(NLS.bind(Messages.Tour_Database_Update, newDbVersion));
		}

		final Statement stmt = conn.createStatement();
		{
//			TOURDATA	TOURDATA	TOURDATA	TOURDATA	TOURDATA	TOURDATA	TOURDATA	TOURDATA
//			// version 21 start
//			//
//			+ "	maxAltitude					FLOAT DEFAULT 0,				\n" //$NON-NLS-1$
//			+ "	maxPulse					FLOAT DEFAULT 0,				\n" //$NON-NLS-1$
//			+ "	avgPulse					FLOAT DEFAULT 0,				\n" //$NON-NLS-1$
//			+ "	avgCadence					FLOAT DEFAULT 0,				\n" //$NON-NLS-1$
//			+ "	avgTemperature				FLOAT DEFAULT 0,				\n" //$NON-NLS-1$
//			//
//			// version 21 end ---------
//			TOURDATA	TOURDATA	TOURDATA	TOURDATA	TOURDATA	TOURDATA	TOURDATA	TOURDATA

			int no = 0;
			final int max = 5;

			modifyColumn_Type(TABLE_TOUR_DATA, "maxAltitude", "FLOAT DEFAULT 0", stmt, monitor, ++no, max); //			//$NON-NLS-1$ //$NON-NLS-2$
			modifyColumn_Type(TABLE_TOUR_DATA, "maxPulse", "FLOAT DEFAULT 0", stmt, monitor, ++no, max); //				//$NON-NLS-1$ //$NON-NLS-2$
			modifyColumn_Type(TABLE_TOUR_DATA, "avgPulse", "FLOAT DEFAULT 0", stmt, monitor, ++no, max); //				//$NON-NLS-1$ //$NON-NLS-2$
			modifyColumn_Type(TABLE_TOUR_DATA, "avgCadence", "FLOAT DEFAULT 0", stmt, monitor, ++no, max); //			//$NON-NLS-1$ //$NON-NLS-2$
			modifyColumn_Type(TABLE_TOUR_DATA, "avgTemperature", "FLOAT DEFAULT 0", stmt, monitor, ++no, max); //		//$NON-NLS-1$ //$NON-NLS-2$
		}
		stmt.close();

		logDbUpdateEnd(newDbVersion);

		return newDbVersion;
	}

	private int updateDbDesign_021_to_022(final Connection conn, final IProgressMonitor monitor) throws SQLException {

		final int newDbVersion = 22;

		logDbUpdateStart(newDbVersion);

		if (monitor != null) {
			monitor.subTask(NLS.bind(Messages.Tour_Database_Update, newDbVersion));
		}

		final Statement stmt = conn.createStatement();
		{
			String sql;

			createTable_TourPhoto(stmt);

			if (isColumnAvailable(conn, TABLE_TOUR_DATA, "TourStartTime") == false) {//$NON-NLS-1$

				// table columns are not yet created

//			TOURDATA	TOURDATA	TOURDATA	TOURDATA	TOURDATA	TOURDATA	TOURDATA	TOURDATA
//			// version 22 start  -  12.12.0
//			//
//			+ "	TourStartTime				BIGINT DEFAULT 0,				\n" //$NON-NLS-1$
//			+ "	TourEndTime					BIGINT DEFAULT 0,				\n" //$NON-NLS-1$
//
//			+ "	TourRecordingTime 			BIGINT DEFAULT 0,				\n" //$NON-NLS-1$
//			+ "	TourDrivingTime 			BIGINT DEFAULT 0,				\n" //$NON-NLS-1$
				//
				// version 22 end ---------

				sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN TourStartTime		BIGINT DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
				exec(stmt, sql);

				sql = "ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN TourEndTime		BIGINT DEFAULT 0"; //$NON-NLS-1$ //$NON-NLS-2$
				exec(stmt, sql);

				/*
				 * modify columns
				 */
				int no = 0;
				final int max = 2;

				modifyColumn_Type(TABLE_TOUR_DATA, "TourRecordingTime", "BIGINT DEFAULT 0", stmt, monitor, ++no, max); //			//$NON-NLS-1$ //$NON-NLS-2$
				modifyColumn_Type(TABLE_TOUR_DATA, "TourDrivingTime", "BIGINT DEFAULT 0", stmt, monitor, ++no, max); //				//$NON-NLS-1$ //$NON-NLS-2$

				createIndex_TourData_022(stmt);
			}

			if (isColumnAvailable(conn, TABLE_TOUR_MARKER, "IsMarkerVisible") == false) {//$NON-NLS-1$

				// table columns are not yet created

//			TOURMARKER	TOURMARKER	TOURMARKER	TOURMARKER	TOURMARKER	TOURMARKER	TOURMARKER	TOURMARKER
//
//			// Version 22 - begin
//			//
//			+ "	IsMarkerVisible				INTEGER DEFAULT 0, 				\n" //$NON-NLS-1$
//			//
//			// Version 22 - end

				sql = "ALTER TABLE " + TABLE_TOUR_MARKER + " ADD COLUMN IsMarkerVisible		INTEGER DEFAULT 1"; //$NON-NLS-1$ //$NON-NLS-2$
				exec(stmt, sql);
			}
		}
		stmt.close();

		logDbUpdateEnd(newDbVersion);

		return newDbVersion;
	}

	/**
	 * Set tour start/end time from the tour date and duration
	 * 
	 * @param conn
	 * @param monitor
	 * @throws SQLException
	 */
	private void updateDbDesign_021_to_022_PostUpdate(final Connection conn, final IProgressMonitor monitor)
			throws SQLException {

		final PreparedStatement stmtSelect = conn.prepareStatement(//
				//
				"SELECT" //									//$NON-NLS-1$
							//
						+ " StartYear," // 					// 1 //$NON-NLS-1$
						+ " StartMonth," // 				// 2 //$NON-NLS-1$
						+ " StartDay," // 					// 3 //$NON-NLS-1$
						+ " StartHour," // 					// 4 //$NON-NLS-1$
						+ " StartMinute," // 				// 5 //$NON-NLS-1$
						+ " StartSecond," // 				// 6 //$NON-NLS-1$
						+ " tourRecordingTime" // 			// 7 //$NON-NLS-1$
						//
						+ " FROM " + TABLE_TOUR_DATA //		//$NON-NLS-1$
						+ " WHERE TourId=?" //				$NON-NLS-1$ //$NON-NLS-1$
				);

		final PreparedStatement stmtUpdate = conn.prepareStatement(//
				//
				"UPDATE " + TABLE_TOUR_DATA //				//$NON-NLS-1$
						//
						+ " SET" //							//$NON-NLS-1$
						//
						+ " TourStartTime=?," //			// 1 //$NON-NLS-1$
						+ " TourEndTime=?" //			// 2 //$NON-NLS-1$
						//
						+ " WHERE tourId=?"); //			// 3 //$NON-NLS-1$

		int tourIndex = 1;
		final ArrayList<Long> tourList = getAllTourIds();

		// loop: all tours
		for (final Long tourId : tourList) {

			if (monitor != null) {
				monitor.subTask(NLS.bind(//
						Messages.Tour_Database_PostUpdate021_SetTourStartEndTime,
						new Object[] { tourIndex++, tourList.size() }));
			}

			// get tour date for 1 tour
			stmtSelect.setLong(1, tourId);
			final ResultSet result = stmtSelect.executeQuery();

			while (result.next()) {

				// get date from database
				final short dbYear = result.getShort(1);
				final short dbMonth = result.getShort(2);
				final short dbDay = result.getShort(3);
				final short dbHour = result.getShort(4);
				final short dbMinute = result.getShort(5);
				final short dbSecond = result.getShort(6);

				final long recordingTime = result.getLong(7);

				final DateTime dtStart = new DateTime(dbYear, dbMonth, dbDay, dbHour, dbMinute, dbSecond);
				final DateTime dtEnd = dtStart.plus(recordingTime * 1000);

				// update tour start/end in the database
				stmtUpdate.setLong(1, dtStart.getMillis());
				stmtUpdate.setLong(2, dtEnd.getMillis());
				stmtUpdate.setLong(3, tourId);
				stmtUpdate.executeUpdate();
			}
		}
	}

	private int updateDbDesign_022_to_023(final Connection conn, final IProgressMonitor monitor) throws SQLException {

		final int newDbVersion = 23;

		logDbUpdateStart(newDbVersion);

		if (monitor != null) {
			monitor.subTask(NLS.bind(Messages.Tour_Database_Update, newDbVersion));
		}

		final Statement stmt = conn.createStatement();
		{

			if (isColumnAvailable(conn, TABLE_TOUR_PHOTO, "imageFileName") == false) {//$NON-NLS-1$

				// table columns are not yet created

//
//			TOUR_PHOTO	TOUR_PHOTO	TOUR_PHOTO	TOUR_PHOTO	TOUR_PHOTO	TOUR_PHOTO	TOUR_PHOTO	TOUR_PHOTO	TOUR_PHOTO
//
//			// version 23 start
//			//
//			+ ("	imageFileName			" + varCharKomma(TourPhoto.DB_LENGTH_FILE_PATH)) //$NON-NLS-1$
//			+ ("	imageFileExt			" + varCharKomma(TourPhoto.DB_LENGTH_FILE_PATH)) //$NON-NLS-1$
//			+ ("	imageFilePath			" + varCharKomma(TourPhoto.DB_LENGTH_FILE_PATH)) //$NON-NLS-1$
//			+ ("	imageFilePathName		" + varCharKomma(TourPhoto.DB_LENGTH_FILE_PATH)) //$NON-NLS-1$
//			//
//			+ "	imageExifTime				BIGINT DEFAULT 0,				\n" //$NON-NLS-1$
//			+ "	imageFileLastModified		BIGINT DEFAULT 0,				\n" //$NON-NLS-1$
//			+ "	adjustedTime				BIGINT DEFAULT 0,					\n" //$NON-NLS-1$
//			//
//			+ "	ratingStars					INT DEFAULT 0,					\n" //$NON-NLS-1$
//			//
//			+ "	isGeoFromPhoto				INT DEFAULT 0,					\n" //$NON-NLS-1$
//			+ "	latitude 					DOUBLE DEFAULT 0,				\n" //$NON-NLS-1$
//			+ "	longitude 					DOUBLE DEFAULT 0				\n" //$NON-NLS-1$
//			//
//			// version 23 end

				final String sqlTourPhoto[] = {
						//
						"ALTER TABLE " + TABLE_TOUR_PHOTO + " ADD COLUMN	imageFileName			VARCHAR(" + TourPhoto.DB_LENGTH_FILE_PATH + ")\n", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						"ALTER TABLE " + TABLE_TOUR_PHOTO + " ADD COLUMN	imageFileExt			VARCHAR(" + TourPhoto.DB_LENGTH_FILE_PATH + ")\n", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						"ALTER TABLE " + TABLE_TOUR_PHOTO + " ADD COLUMN	imageFilePath			VARCHAR(" + TourPhoto.DB_LENGTH_FILE_PATH + ")\n", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						"ALTER TABLE " + TABLE_TOUR_PHOTO + " ADD COLUMN	imageFilePathName		VARCHAR(" + TourPhoto.DB_LENGTH_FILE_PATH + ")\n", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						//
						"ALTER TABLE " + TABLE_TOUR_PHOTO + " ADD COLUMN	imageExifTime			BIGINT DEFAULT 0", //$NON-NLS-1$ //$NON-NLS-2$
						"ALTER TABLE " + TABLE_TOUR_PHOTO + " ADD COLUMN	imageFileLastModified	BIGINT DEFAULT 0", //$NON-NLS-1$ //$NON-NLS-2$
						"ALTER TABLE " + TABLE_TOUR_PHOTO + " ADD COLUMN	adjustedTime			BIGINT DEFAULT 0", //$NON-NLS-1$ //$NON-NLS-2$
						//
						"ALTER TABLE " + TABLE_TOUR_PHOTO + " ADD COLUMN	ratingStars				INT DEFAULT 0", //$NON-NLS-1$ //$NON-NLS-2$
						//
						"ALTER TABLE " + TABLE_TOUR_PHOTO + " ADD COLUMN	isGeoFromPhoto			INT DEFAULT 0", //$NON-NLS-1$ //$NON-NLS-2$
						"ALTER TABLE " + TABLE_TOUR_PHOTO + " ADD COLUMN	latitude 				DOUBLE DEFAULT 0", //$NON-NLS-1$ //$NON-NLS-2$
						"ALTER TABLE " + TABLE_TOUR_PHOTO + " ADD COLUMN	longitude 				DOUBLE DEFAULT 0", //$NON-NLS-1$ //$NON-NLS-2$
				};

				exec(stmt, sqlTourPhoto);
			}

			if (isColumnAvailable(conn, TABLE_TOUR_DATA, "numberOfTimeSlices") == false) {//$NON-NLS-1$

				// table columns are not yet created
//
//			TOURDATA	TOURDATA	TOURDATA	TOURDATA	TOURDATA	TOURDATA	TOURDATA	TOURDATA	TOURDATA
//
//			// version 23 start
//			//
//			+ " numberOfTimeSlices			INTEGER DEFAULT 0,				\n" //$NON-NLS-1$
//			+ " numberOfPhotos				INTEGER DEFAULT 0,				\n" //$NON-NLS-1$
//			+ " photoTimeAdjustment			INTEGER DEFAULT 0,				\n" //$NON-NLS-1$
//			//
//			// version 23 end ---------

				final String sqlTourData[] = {
						//
						"ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN	numberOfTimeSlices		INTEGER DEFAULT 0", //$NON-NLS-1$ //$NON-NLS-2$
						"ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN	numberOfPhotos			INTEGER DEFAULT 0", //$NON-NLS-1$ //$NON-NLS-2$
						"ALTER TABLE " + TABLE_TOUR_DATA + " ADD COLUMN	photoTimeAdjustment		INTEGER DEFAULT 0", //$NON-NLS-1$ //$NON-NLS-2$
				//
				};

				exec(stmt, sqlTourData);
			}
		}

		stmt.close();

		logDbUpdateEnd(newDbVersion);

		return newDbVersion;
	}

	/**
	 * @param conn
	 * @param monitor
	 * @throws SQLException
	 */
	private void updateDbDesign_022_to_023_PostUpdate(final Connection conn, final IProgressMonitor monitor)
			throws SQLException {

		int tourIdx = 1;
		final ArrayList<Long> tourList = getAllTourIds();

		final EntityManager em = TourDatabase.getInstance().getEntityManager();
		try {

			// loop: all tours
			for (final Long tourId : tourList) {

				if (monitor != null) {

					monitor.subTask(NLS.bind(//
							Messages.Tour_Database_PostUpdate023_SetTimeSliceNumbers,
							new Object[] { tourIdx, tourList.size() }));

					tourIdx++;
				}

				final TourData tourData = em.find(TourData.class, tourId);
				if (tourData != null) {

					// compute number of time slices
					tourData.onPrePersist();

					TourDatabase.saveEntity(tourData, tourId, TourData.class);
				}
			}

		} catch (final Exception e) {
			e.printStackTrace();
		} finally {

			em.close();
		}
	}

	private int updateDbDesign_023_to_024(final Connection conn, final IProgressMonitor monitor) throws SQLException {

		final int newDbVersion = 24;

		logDbUpdateStart(newDbVersion);

		if (monitor != null) {
			monitor.subTask(NLS.bind(Messages.Tour_Database_Update, newDbVersion));
		}

		final Statement stmt = conn.createStatement();
		{
			// check if db is updated to version 24
			if (isColumnAvailable(conn, TABLE_TOUR_MARKER, "description") == false) {//$NON-NLS-1$

				// description column is not yet created -> do db update 24

				/**
				 * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
				 * <p>
				 * Drop tables first, when something goes wrong the existing tables are not yet
				 * modified.
				 * <p>
				 * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
				 */

				/*
				 * Drop tables which will never be used, they exist since many years but it is
				 * unknows why they has been created.
				 */
				SQL.Cleanup_DropTable(stmt, TABLE_TOUR_CATEGORY);
				SQL.Cleanup_DropTable(stmt, TABLE_TOURCATEGORY__TOURDATA);

				SQL.Cleanup_DropTable(stmt, JOINTABLE__TOURDATA__TOURMARKER);
				SQL.Cleanup_DropTable(stmt, JOINTABLE__TOURDATA__TOURPHOTO);
				SQL.Cleanup_DropTable(stmt, JOINTABLE__TOURDATA__TOURREFERENCE);
				SQL.Cleanup_DropTable(stmt, JOINTABLE__TOURDATA__TOURWAYPOINT);
				SQL.Cleanup_DropTable(stmt, JOINTABLE__TOURPERSON__TOURPERSON_HRZONE);

				/*
				 * Table: TOURTAG
				 */
				{
					/**
					 * Changed TagCategory from @ManyToMany to @ManyToOne because a tag can be
					 * associated only with ONE category and not with multiple.
					 */
					SQL.Cleanup_DropConstraint(stmt, JOINTABLE__TOURDATA__TOURTAG, "FK_TOURDATA_TOURTAG_TOURTAG_TagID"); //$NON-NLS-1$
				}

				/*
				 * Table: TOURMARKER
				 */
				{
					/*
					 * Adjust column with to TourWayPoint width, that both have the same max size.
					 */
					SQL.AlterCol_VarChar_Width(stmt, TABLE_TOUR_MARKER, "label", TourWayPoint.DB_LENGTH_NAME); //$NON-NLS-1$
					SQL.AlterCol_VarChar_Width(stmt, TABLE_TOUR_MARKER, "category", TourWayPoint.DB_LENGTH_CATEGORY); //$NON-NLS-1$

					/*
					 * Add new columns
					 */
					SQL.AddCol_VarCar(stmt, TABLE_TOUR_MARKER, "description", TourWayPoint.DB_LENGTH_DESCRIPTION); //$NON-NLS-1$
					SQL.AddCol_VarCar(stmt, TABLE_TOUR_MARKER, "urlText", TourMarker.DB_LENGTH_URL_TEXT); //$NON-NLS-1$
					SQL.AddCol_VarCar(stmt, TABLE_TOUR_MARKER, "urlAddress", TourMarker.DB_LENGTH_URL_ADDRESS); //$NON-NLS-1$
				}
			}
		}
		stmt.close();

		logDbUpdateEnd(newDbVersion);

		return newDbVersion;
	}

	private int updateDbDesign_024_to_025(final Connection conn, final IProgressMonitor monitor) throws SQLException {

		final int newDbVersion = 25;

		logDbUpdateStart(newDbVersion);

		if (monitor != null) {
			monitor.subTask(NLS.bind(Messages.Tour_Database_Update, newDbVersion));
		}

		final Statement stmt = conn.createStatement();
		{
			// check if db is updated to version 25
			if (isTableAvailable(conn, TABLE_SHARED_MARKER) == false) {

				// table SharedMarker is not available -> do db update 25
				createTable_SharedMarker(stmt);

				// Table: TOURMARKER
				{
					// Add new columns
					SQL.AddCol_Float(stmt, TABLE_TOUR_MARKER, "altitude", FLOAT_MIN_VALUE); //$NON-NLS-1$
					SQL.AddCol_Double(stmt, TABLE_TOUR_MARKER, "latitude", DOUBLE_MIN_VALUE); //$NON-NLS-1$
					SQL.AddCol_Double(stmt, TABLE_TOUR_MARKER, "longitude", DOUBLE_MIN_VALUE); //$NON-NLS-1$
				}

				// Table: TOURDATA
				{
					// Add new columns
					SQL.AddCol_Int(stmt, TABLE_TOUR_DATA, "gpsState", "0"); //$NON-NLS-1$
				}
			}
		}
		stmt.close();

		logDbUpdateEnd(newDbVersion);

		return newDbVersion;
	}

	/**
	 * @param conn
	 * @param monitor
	 * @throws SQLException
	 */
	private void updateDbDesign_024_to_025_PostUpdate(final Connection conn, final IProgressMonitor monitor)
			throws SQLException {

		int tourIdx = 1;
		final ArrayList<Long> tourList = getAllTourIds();

		final EntityManager em = TourDatabase.getInstance().getEntityManager();
		try {

			// loop: all tours
			for (final Long tourId : tourList) {

				if (monitor != null) {

					monitor.subTask(NLS.bind(//
							Messages.Tour_Database_PostUpdate025_SetMarkerFields,
							new Object[] { tourIdx, tourList.size() }));

					tourIdx++;
				}

				final TourData tourData = em.find(TourData.class, tourId);
				if (tourData != null) {

					/*
					 * set lat/lon/altitude in the tour marker from tour data
					 */

					final float[] altitudeSerie = tourData.altitudeSerie;
					final double[] latitudeSerie = tourData.latitudeSerie;
					final double[] longitudeSerie = tourData.longitudeSerie;

					for (final TourMarker tourMarker : tourData.getTourMarkers()) {

						final int serieIndex = tourMarker.getSerieIndex();

						if (altitudeSerie != null) {
							tourMarker.setAltitude(altitudeSerie[serieIndex]);
						}

						if (latitudeSerie != null) {
							tourMarker.setGeoPosition(latitudeSerie[serieIndex], longitudeSerie[serieIndex]);
						}
					}

					// set GPS state
					tourData.setGpsState(latitudeSerie == null
							? TourData.GPS_STATE_WITHOUT_GPS
							: TourData.GPS_STATE_WITH_GPS);

					TourDatabase.saveEntity(tourData, tourId, TourData.class);
				}
			}

		} catch (final Exception e) {
			e.printStackTrace();
		} finally {

			em.close();
		}
	}

	private void updateDbVersionNumber(final Connection conn, final int newVersion) throws SQLException {

		final String sql = "UPDATE " + TABLE_DB_VERSION + " SET VERSION=" + newVersion; //$NON-NLS-1$ //$NON-NLS-2$

		conn.createStatement().executeUpdate(sql);
	}

}
