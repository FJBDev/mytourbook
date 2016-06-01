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
package net.tourbook.statistic;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.util.StringToArrayConverter;
import net.tourbook.data.TourPerson;
import net.tourbook.database.TourDatabase;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.ui.SQLFilter;
import net.tourbook.ui.TourTypeFilter;
import net.tourbook.ui.UI;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.viewers.IPostSelectionProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.part.PageBook;

public class StatContainer extends Composite {

	private final boolean						_isOSX						= net.tourbook.common.UI.IS_OSX;
	private final boolean						_isLinux					= net.tourbook.common.UI.IS_LINUX;

	private static final String					STATE_SELECTED_STATISTIC	= "statistic.container.selected_statistic"; //$NON-NLS-1$
	private static final String					STATE_SELECTED_YEAR			= "statistic.container.selected-year";		//$NON-NLS-1$
	private static final String					STATE_NUMBER_OF_YEARS		= "statistic.container.number_of_years";	//$NON-NLS-1$

	private static ArrayList<TourbookStatistic>	_statisticExtensionPoints;

	private IDialogSettings						_state;

	private final Calendar						_calendar					= GregorianCalendar.getInstance();

	private int									_selectedYear				= -1;

	private TourbookStatistic					_activeStatistic;
	private TourPerson							_activePerson;
	private TourTypeFilter						_activeTourTypeFilter;

	/**
	 * contains all years which have tours for the selected tour type and person
	 */
	private ArrayList<Integer>					_availableYears;

	/**
	 * contains the statistics in the same sort order as the statistic combo box
	 */
	private ArrayList<TourbookStatistic>		_allStatisticProvider;

	private ActionChartOptions					_actionChartOptions;
	private ActionSynchChartScale				_actionSynchChartScale;

	private boolean								_isSynchScaleEnabled;

	private final IPostSelectionProvider		_postSelectionProvider;

	/*
	 * UI controls
	 */
	private Composite							_statContainer;
	private Combo								_cboYear;
	private Combo								_cboStatistics;
	private Combo								_cboNumberOfYears;
	private Combo								_cboBarVerticalOrder;

	private PageBook							_pageBookStatistic;

	private ToolBarManager						_tbm;
	private ToolBar								_toolBar;

	private final IViewSite						_viewSite;
	private boolean								_isVerticalOrderDisabled;

	StatContainer(	final Composite parent,
					final IViewSite viewSite,
					final IPostSelectionProvider selectionProvider,
					final int style) {

		super(parent, style);

		_viewSite = viewSite;
		_postSelectionProvider = selectionProvider;

		createActions();

		createUI(this);
		updateUI();

		addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(final DisposeEvent e) {

				// dispose all statistic resources
				for (final TourbookStatistic statistic : getComboStatistics()) {
					statistic.dispose();
				}

				_activeStatistic = null;
			}
		});
	}

	/**
	 * this method is synchronized to conform to FindBugs
	 * 
	 * @return Returns statistics from the extension registry in the sort order of the registry
	 */
	public static synchronized ArrayList<TourbookStatistic> getStatisticExtensionPoints() {

		if (_statisticExtensionPoints != null) {
			return _statisticExtensionPoints;
		}

		_statisticExtensionPoints = new ArrayList<TourbookStatistic>();

		final IExtensionPoint extPoint = Platform.getExtensionRegistry().getExtensionPoint(
				TourbookPlugin.PLUGIN_ID,
				TourbookPlugin.EXT_POINT_STATISTIC_YEAR);

		if (extPoint != null) {

			for (final IExtension extension : extPoint.getExtensions()) {

				for (final IConfigurationElement configElement : extension.getConfigurationElements()) {

					if (configElement.getName().equalsIgnoreCase("statistic")) { //$NON-NLS-1$

						Object object;
						try {
							object = configElement.createExecutableExtension("class"); //$NON-NLS-1$
							if (object instanceof TourbookStatistic) {

								final TourbookStatistic statisticItem = (TourbookStatistic) object;

								statisticItem.visibleName = configElement.getAttribute("name"); //$NON-NLS-1$
								statisticItem.statisticId = configElement.getAttribute("id"); //$NON-NLS-1$

								_statisticExtensionPoints.add(statisticItem);
							}
						} catch (final CoreException e) {
							e.printStackTrace();
						}
					}
				}
			}
		}

		return _statisticExtensionPoints;
	}

	/**
	 * @return Returns statistic providers with the custom sort order
	 */
	public static ArrayList<TourbookStatistic> getStatisticProviders() {

		final ArrayList<TourbookStatistic> availableStatistics = getStatisticExtensionPoints();
		final ArrayList<TourbookStatistic> visibleStatistics = new ArrayList<TourbookStatistic>();

		final String[] prefStoreStatisticIds = StringToArrayConverter.//
				convertStringToArray(TourbookPlugin
						.getDefault()
						.getPreferenceStore()
						.getString(ITourbookPreferences.STATISTICS_STATISTIC_PROVIDER_IDS));

		// get all statistics which are saved in the pref store
		for (final String statisticId : prefStoreStatisticIds) {

			// get statistic item from the id
			for (final TourbookStatistic tourbookStatistic : availableStatistics) {
				if (statisticId.equals(tourbookStatistic.statisticId)) {
					visibleStatistics.add(tourbookStatistic);
					break;
				}
			}
		}

		// get statistics which are available but not saved in the prefstore
		for (final TourbookStatistic availableStatistic : availableStatistics) {

			if (visibleStatistics.contains(availableStatistic) == false) {
				visibleStatistics.add(availableStatistic);
			}
		}

		return visibleStatistics;
	}

	void actionSynchScale(final boolean isEnabled) {

		_isSynchScaleEnabled = isEnabled;
		_activeStatistic.setSynchScale(_isSynchScaleEnabled);

		if (_activeStatistic instanceof IYearStatistic) {
			((IYearStatistic) _activeStatistic).updateStatistic(new StatisticContext(
					_activePerson,
					_activeTourTypeFilter,
					_selectedYear,
					getNumberOfYears(),
					false));
		}
	}

	private void createActions() {

		_actionChartOptions = new ActionChartOptions(this);
		_actionSynchChartScale = new ActionSynchChartScale(this);
	}

	private void createUI(final StatContainer parent) {

		GridLayoutFactory.fillDefaults().applyTo(parent);

		/*
		 * container: statistic combo
		 */
		_statContainer = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(_statContainer);
		GridLayoutFactory.fillDefaults().spacing(0, 0).applyTo(_statContainer);
		_statContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
		{
			createUI_10_Toolbar(_statContainer);

			// pagebook: statistics
			_pageBookStatistic = new PageBook(_statContainer, SWT.NONE);
			_pageBookStatistic.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		}
	}

	private void createUI_10_Toolbar(final Composite parent) {

		final PixelConverter pc = new PixelConverter(this);

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(6).applyTo(container);
		{
			/*
			 * combo: year
			 */
			_cboYear = new Combo(container, SWT.DROP_DOWN | SWT.READ_ONLY);
			GridDataFactory.fillDefaults()//
					.hint(pc.convertWidthInCharsToPixels(_isOSX ? 12 : _isLinux ? 12 : 5), SWT.DEFAULT)
					.applyTo(_cboYear);
			_cboYear.setToolTipText(Messages.Tour_Book_Combo_year_tooltip);
			_cboYear.setVisibleItemCount(50);
			_cboYear.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					onSelectYear();
				}
			});

			/*
			 * label: number of years
			 */
			final Label label = new Label(container, SWT.NONE);
			GridDataFactory.fillDefaults()//
					.align(SWT.FILL, SWT.CENTER)
					.indent(10, 0)
					.applyTo(label);
			label.setText(Messages.tour_statistic_label_years);

			/*
			 * combo: year numbers
			 */
			_cboNumberOfYears = new Combo(container, SWT.DROP_DOWN | SWT.READ_ONLY);
			GridDataFactory.fillDefaults()//
					.indent(2, 0)
					.hint(pc.convertWidthInCharsToPixels(_isOSX ? 8 : _isLinux ? 8 : 4), SWT.DEFAULT)
					.applyTo(_cboNumberOfYears);
			_cboNumberOfYears.setToolTipText(Messages.tour_statistic_number_of_years);
			_cboNumberOfYears.setVisibleItemCount(20);
			_cboNumberOfYears.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					onSelectYear();
				}
			});

			/*
			 * combo: statistics
			 */
			_cboStatistics = new Combo(container, SWT.DROP_DOWN | SWT.READ_ONLY);
			GridDataFactory.fillDefaults()//
					.indent(15, 0)
					.applyTo(_cboStatistics);
			_cboStatistics.setToolTipText(Messages.Tour_Book_Combo_statistic_tooltip);
			_cboStatistics.setVisibleItemCount(50);
			_cboStatistics.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					onSelectStatistic();
				}
			});

			/*
			 * combo: sequence for stacked charts
			 */

			final GC gc = new GC(this);
			final Point defaultTextSize = gc.textExtent(Messages.Tour_Statistic_Combo_BarVOrder_InfoItem);
			gc.dispose();

			_cboBarVerticalOrder = new Combo(container, SWT.DROP_DOWN | SWT.READ_ONLY);
			GridDataFactory.fillDefaults()//
					.indent(15, 0)
					.hint(defaultTextSize.x, SWT.DEFAULT)
					.applyTo(_cboBarVerticalOrder);
			_cboBarVerticalOrder.setToolTipText(Messages.Tour_Statistic_Combo_BarVOrder_Tooltip);
			_cboBarVerticalOrder.setVisibleItemCount(50);
			_cboBarVerticalOrder.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					onSelectBarVerticalOrder();
				}
			});

			/*
			 * toolbar
			 */
			_toolBar = new ToolBar(container, SWT.FLAT | SWT.WRAP | SWT.RIGHT);
			GridDataFactory.fillDefaults()//
					.align(SWT.FILL, SWT.END)
					.grab(true, false)
					.applyTo(_toolBar);

			_tbm = new ToolBarManager(_toolBar);
		}
	}

	/**
	 * @param defaultYear
	 * @return Returns the index for the active year or <code>-1</code> when there are no years
	 *         available
	 */
	private int getActiveYearComboboxIndex(final int defaultYear) {

		int selectedYearIndex = -1;

		if (_availableYears == null) {
			return selectedYearIndex;
		}

		/*
		 * try to get the year index for the default year
		 */
		if (defaultYear != -1) {

			int yearIndex = 0;
			for (final Integer year : _availableYears) {

				if (year == defaultYear) {

					_selectedYear = defaultYear;

					return yearIndex;
				}
				yearIndex++;
			}
		}

		/*
		 * try to get year index of the selected year
		 */
		int yearIndex = 0;
		for (final Integer year : _availableYears) {
			if (year == _selectedYear) {
				selectedYearIndex = yearIndex;
				break;
			}
			yearIndex++;
		}

		return selectedYearIndex;
	}

	/**
	 * @return Returns all statistic plugins which are displayed in the statistic combo box
	 */
	private ArrayList<TourbookStatistic> getComboStatistics() {

		if (_allStatisticProvider == null) {
			_allStatisticProvider = getStatisticProviders();
		}

		return _allStatisticProvider;
	}

	/**
	 * @return Returns number of years which are selected in the combobox
	 */
	private int getNumberOfYears() {

		int numberOfYears = 1;
		final int selectedIndex = _cboNumberOfYears.getSelectionIndex();

		if (selectedIndex != -1) {
			numberOfYears = selectedIndex + 1;
		}

		return numberOfYears;
	}

	/**
	 * @return Returns the visible statistic {@link TourbookStatistic} or <code>null</code> when a
	 *         statistic is not available
	 */
	TourbookStatistic getSelectedStatistic() {
		return _activeStatistic;
	}

	private void onSelectBarVerticalOrder() {

		if (_activeStatistic == null) {
			return;
		}

		_activeStatistic.setBarVerticalOrder(_cboBarVerticalOrder.getSelectionIndex());
	}

	private void onSelectStatistic() {

		if (setActiveStatistic() == false) {
			return;
		}

		updateStatistic_10_NoReload(_activePerson, _activeTourTypeFilter, _selectedYear);
	}

	private void onSelectYear() {

		final int selectedItem = _cboYear.getSelectionIndex();
		if (selectedItem != -1) {

			_selectedYear = Integer.parseInt(_cboYear.getItem(selectedItem));

			updateStatistic_10_NoReload(_activePerson, _activeTourTypeFilter, _selectedYear);
		}
	}

	void refreshStatisticProvider() {

		if (setActiveStatistic() == false) {
			return;
		}

		_allStatisticProvider = getStatisticProviders();

		_cboStatistics.removeAll();
		int indexCounter = 0;
		int selectedIndex = 0;

		// fill combobox with statistic names
		for (final TourbookStatistic statistic : getComboStatistics()) {

			_cboStatistics.add(statistic.visibleName);

			if (_activeStatistic != null && _activeStatistic.statisticId.equals(statistic.statisticId)) {
				selectedIndex = indexCounter;
			}

			indexCounter++;
		}

		// reselect stat
		_cboStatistics.select(selectedIndex);
		onSelectStatistic();
	}

	/**
	 * create the year list for all tours and fill the year combobox with the available years
	 */
	private void refreshYearCombobox() {

		final SQLFilter sqlFilter = new SQLFilter();

		final String sqlString = //
		"SELECT" // 												//$NON-NLS-1$
				+ " startYear " //									//$NON-NLS-1$
				+ " FROM " + TourDatabase.TABLE_TOUR_DATA //		//$NON-NLS-1$
				+ " WHERE 1=1 " + sqlFilter.getWhereClause() //		//$NON-NLS-1$
				+ " GROUP BY STARTYEAR ORDER BY STARTYEAR"; //		//$NON-NLS-1$

		_availableYears = new ArrayList<Integer>();

		try {
			final Connection conn = TourDatabase.getInstance().getConnection();
			final PreparedStatement statement = conn.prepareStatement(sqlString);
			sqlFilter.setParameters(statement, 1);

			final ResultSet result = statement.executeQuery();

			while (result.next()) {
				_availableYears.add(result.getInt(1));
			}

			conn.close();

		} catch (final SQLException e) {
			UI.showSQLException(e);
		}

		_cboYear.removeAll();

		/*
		 * add all years of the tours and the current year
		 */
		_calendar.setTime(new Date());
		final int thisYear = _calendar.get(Calendar.YEAR);

		boolean isThisYearSet = false;

		for (final Integer year : _availableYears) {

			if (year.intValue() == thisYear) {
				isThisYearSet = true;
			}

			_cboYear.add(year.toString());
		}

		// add currenty year if not set
		if (isThisYearSet == false) {
			_availableYears.add(thisYear);
			_cboYear.add(Integer.toString(thisYear));
		}
	}

	/**
	 * Restore selected statistic
	 * 
	 * @param state
	 * @param activeTourTypeFilter
	 * @param activePerson
	 */
	void restoreStatistics(	final IDialogSettings state,
							final TourPerson activePerson,
							final TourTypeFilter activeTourTypeFilter) {

		_state = state;
		_activePerson = activePerson;
		_activeTourTypeFilter = activeTourTypeFilter;

		final ArrayList<TourbookStatistic> allAvailableStatistics = getComboStatistics();
		if (allAvailableStatistics.size() == 0) {
			return;
		}

		// select statistic
		int prevStatIndex = 0;
		final String mementoStatisticId = state.get(STATE_SELECTED_STATISTIC);
		if (mementoStatisticId != null) {
			int statIndex = 0;
			for (final TourbookStatistic statistic : allAvailableStatistics) {
				if (mementoStatisticId.equalsIgnoreCase(statistic.statisticId)) {
					prevStatIndex = statIndex;
					break;
				}
				statIndex++;
			}
		}

		// select number of years
		try {
			final int numberOfYears = state.getInt(STATE_NUMBER_OF_YEARS);
			_cboNumberOfYears.select(numberOfYears);
		} catch (final NumberFormatException e) {
			// select one year
			_cboNumberOfYears.select(0);
		}

		// select year
		int defaultYear;
		try {
			defaultYear = state.getInt(STATE_SELECTED_YEAR);
		} catch (final NumberFormatException e) {
			defaultYear = -1;
		}
		refreshYearCombobox();
		selectYear(defaultYear);

		// select statistic item
		_cboStatistics.select(prevStatIndex);
		onSelectStatistic();

		// restore statistic state, like reselect previous selection
		if (state != null) {
			allAvailableStatistics.get(prevStatIndex).restoreState(state);
		}
	}

	/**
	 * save statistic
	 */
	void saveState(final IDialogSettings state) {

		final ArrayList<TourbookStatistic> allAvailableStatistics = getComboStatistics();
		if (allAvailableStatistics.size() == 0) {
			return;
		}

		// keep statistic id for the selected statistic
		final int selectionIndex = _cboStatistics.getSelectionIndex();
		if (selectionIndex != -1) {
			state.put(STATE_SELECTED_STATISTIC, allAvailableStatistics.get(selectionIndex).statisticId);
		}

		for (final TourbookStatistic tourbookStatistic : allAvailableStatistics) {
			tourbookStatistic.saveState(state);
		}

		state.put(STATE_NUMBER_OF_YEARS, _cboNumberOfYears.getSelectionIndex());
		state.put(STATE_SELECTED_YEAR, _selectedYear);
	}

	private void selectYear(final int defaultYear) {

		int selectedYearIndex = getActiveYearComboboxIndex(defaultYear);
		if (selectedYearIndex == -1) {

			/*
			 * the active year was not found in the combo box, it's possible that the combo box
			 * needs to be update
			 */

			refreshYearCombobox();
			selectedYearIndex = getActiveYearComboboxIndex(defaultYear);

			if (selectedYearIndex == -1) {

				// year is still not selected
				final int yearCount = _cboYear.getItemCount();

				// reselect the youngest year if years are available
				if (yearCount > 0) {
					selectedYearIndex = yearCount - 1;
					_selectedYear = Integer.parseInt(_cboYear.getItem(yearCount - 1));
				}
			}
		}

		_cboYear.select(selectedYearIndex);
	}

	/**
	 * @return Returns <code>true</code> when a statistic is selected and {@link #_activeStatistic}
	 *         is valid.
	 */
	private boolean setActiveStatistic() {

		// get selected statistic
		final int selectedIndex = _cboStatistics.getSelectionIndex();
		if (selectedIndex == -1) {
			_activeStatistic = null;
			return false;
		}

		final ArrayList<TourbookStatistic> allAvailableStatistics = getComboStatistics();
		if (allAvailableStatistics.size() == 0) {
			return false;
		}

		final TourbookStatistic statistic = allAvailableStatistics.get(selectedIndex);

		// get statistic control
		Composite statContainer = statistic.getControl();
		if (statContainer != null) {
			_activeStatistic = statistic;
			return true;
		}

		// create statistic UI in the pagebook for the selected statistic
		statContainer = new Composite(_pageBookStatistic, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(statContainer);
		GridLayoutFactory.fillDefaults().applyTo(statContainer);
		{
			statistic.createControl(statContainer, _viewSite, _postSelectionProvider);
			statistic.setContainer(statContainer);

			final Composite statControl = statistic.getControl();
			GridDataFactory.fillDefaults().grab(true, true).applyTo(statControl);

			statistic.restoreStateEarly(_state);
		}

		_activeStatistic = statistic;

		return true;
	}

	@Override
	public boolean setFocus() {
		return super.setFocus();
	}

	/**
	 * Update all statistics which have been created because person or tour type could be changed
	 * and reload data.
	 * 
	 * @param person
	 * @param tourTypeFilter
	 */
	void updateStatistic(final TourPerson person, final TourTypeFilter tourTypeFilter) {

		if (setActiveStatistic() == false) {
			return;
		}

		_activePerson = person;
		_activeTourTypeFilter = tourTypeFilter;

		refreshYearCombobox();
		selectYear(-1);

		// tell all existing statistics the data have changed
		for (final TourbookStatistic statistic : getComboStatistics()) {

			if (statistic.getControl() != null) {
				if (statistic instanceof IYearStatistic) {

					statistic.setSynchScale(_isSynchScaleEnabled);
					statistic.setDataDirty();
				}
			}
		}

		// refresh current statistic
		final StatisticContext statContext = new StatisticContext(
				_activePerson,
				_activeTourTypeFilter,
				_selectedYear,
				getNumberOfYears(),
				true);

		statContext.inVerticalBarIndex = _cboBarVerticalOrder.getSelectionIndex();

		((IYearStatistic) _activeStatistic).updateStatistic(statContext);

		updateStatistic_20_PostRefresh(statContext);
	}

	/**
	 * @param person
	 * @param activeTourTypeFilter
	 * @param selectedYear
	 */
	private void updateStatistic_10_NoReload(	final TourPerson person,
												final TourTypeFilter activeTourTypeFilter,
												final int selectedYear) {

		_activePerson = person;
		_activeTourTypeFilter = activeTourTypeFilter;

		// keep current year
		if (selectedYear == -1) {
			return;
		}
		_selectedYear = selectedYear;

		if (setActiveStatistic() == false) {
			// statistic is not available
			return;
		}

		// display selected statistic
		_pageBookStatistic.showPage(_activeStatistic.getControl());

		selectYear(-1);

		_activeStatistic.setSynchScale(_isSynchScaleEnabled);

		final StatisticContext statContext = new StatisticContext(
				_activePerson,
				_activeTourTypeFilter,
				selectedYear,
				getNumberOfYears(),
				false);

		statContext.inVerticalBarIndex = _cboBarVerticalOrder.getSelectionIndex();

		((IYearStatistic) _activeStatistic).updateStatistic(statContext);

		updateStatistic_20_PostRefresh(statContext);
		updateUI_Toolbar();
	}

	private void updateStatistic_20_PostRefresh(final StatisticContext statContext) {

		if (statContext.outIsBarReorderingSupported) {

			updateStatistic_30_BarOrdering(statContext);

			// vertical order feature is used
			_isVerticalOrderDisabled = false;

			_cboBarVerticalOrder.setVisible(true);

		} else {

			if (_isVerticalOrderDisabled == false) {

				// disable vertical order feature

				_cboBarVerticalOrder.setVisible(false);

				_isVerticalOrderDisabled = true;
			}
		}
	}

	private void updateStatistic_30_BarOrdering(final StatisticContext statContext) {

		if (statContext.outIsUpdateBarNames) {
			updateUI_VerticalBarOrder(statContext);
		}

	}

	private void updateUI() {

		// fill combobox with number of years
		for (int years = 1; years <= 50; years++) {
			_cboNumberOfYears.add(Integer.toString(years));
		}

		// fill combobox with statistic names
		for (final TourbookStatistic statistic : getComboStatistics()) {
			_cboStatistics.add(statistic.visibleName);
		}
	}

	/**
	 * Each statistic has it's own toolbar
	 */
	private void updateUI_Toolbar() {

		// update view toolbar
		final IToolBarManager tbm = _viewSite.getActionBars().getToolBarManager();
		tbm.removeAll();

		tbm.add(_actionSynchChartScale);
		tbm.add(_actionChartOptions);

		// add actions from the statistic
		_activeStatistic.updateToolBar(true);

		_tbm.update(false);
		_statContainer.layout();
	}

	private void updateUI_VerticalBarOrder(final StatisticContext statContext) {

		final String[] stackedNames = statContext.outBarNames;

		if (stackedNames == null) {
			_cboBarVerticalOrder.setVisible(false);
			_isVerticalOrderDisabled = true;

			return;
		}

		_cboBarVerticalOrder.removeAll();

		for (final String name : stackedNames) {
			_cboBarVerticalOrder.add(name);
		}

		final int selectedIndex = statContext.outVerticalBarIndex;

		_cboBarVerticalOrder.select(selectedIndex >= _cboBarVerticalOrder.getItemCount() ? 0 : selectedIndex);
		_cboBarVerticalOrder.setEnabled(true);
	}
}
