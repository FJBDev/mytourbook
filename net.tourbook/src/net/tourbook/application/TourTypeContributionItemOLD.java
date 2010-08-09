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
package net.tourbook.application;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import net.tourbook.Messages;
import net.tourbook.data.TourType;
import net.tourbook.database.TourDatabase;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.ui.CustomControlContribution;
import net.tourbook.ui.TourTypeFilter;
import net.tourbook.ui.TourTypeFilterSet;
import net.tourbook.ui.UI;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.nebula.jface.tablecomboviewer.TableComboViewer;
import org.eclipse.nebula.widgets.tablecombo.TableCombo;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.XMLMemento;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class TourTypeContributionItemOLD extends CustomControlContribution {

	private static final String								ID							= "net.tourbook.tourtypefilter";				//$NON-NLS-1$

	private static final String								TAG_NAME					= "name";										//$NON-NLS-1$
	private static final String								TAG_FILTER_TYPE				= "filterType";								//$NON-NLS-1$
	private static final String								TAG_SYSTEM_ID				= "systemId";									//$NON-NLS-1$
	private static final String								TAG_TOUR_TYPE_ID			= "tourTypeId";								//$NON-NLS-1$

	private static final String								MEMENTO_ROOT_FILTER_LIST	= "filterlist";								//$NON-NLS-1$
	private static final String								MEMENTO_CHILD_FILTER		= "filter";									//$NON-NLS-1$
	private static final String								MEMENTO_CHILD_TOURTYPE		= "tourtype";									//$NON-NLS-1$

	private static final String								MEMENTO_FILTER_LIST_FILE	= "filterlist.xml";							//$NON-NLS-1$

	private IPreferenceStore								_prefStore					= TourbookPlugin.getDefault() //
																								.getPreferenceStore();

	private final IDialogSettings							_state						= TourbookPlugin.getDefault() //
																								.getDialogSettingsSection(
																										"TourTypeContributionItem");	//$NON-NLS-1$
	private org.eclipse.jface.util.IPropertyChangeListener	_prefChangeListener;

	/**
	 * contains the tour type filters which are displayed in the combobox
	 */
	private ArrayList<TourTypeFilter>						_tourTypeFilters;

	private double											_propertyValue;

	private TableComboViewer								_tourTypeComboViewer;

	private static class SingleImageItemLabelProvider extends LabelProvider implements ITableLabelProvider {

		public Image getColumnImage(final Object element, final int columnIndex) {

			final TourTypeFilter item = (TourTypeFilter) element;

			if (columnIndex == 0) {
				return getFilterImage(item);
			}

			return null;
		}

		/**
		 * @see org.eclipse.jface.viewers.ITableLabelProvider#getColumnText(java.lang.Object, int)
		 */
		public String getColumnText(final Object element, final int columnIndex) {

			final TourTypeFilter item = (TourTypeFilter) element;

			switch (columnIndex) {
			case 0:
				return item.getFilterName();
			}

			return UI.EMPTY_STRING;
		}
	}

	public TourTypeContributionItemOLD() {
		this(ID);
	}

	protected TourTypeContributionItemOLD(final String id) {
		super(id);
	}

	private static Image getFilterImage(final TourTypeFilter filter) {

		final int filterType = filter.getFilterType();

		Image filterImage = null;

		// set filter name/image
		switch (filterType) {
		case TourTypeFilter.FILTER_TYPE_DB:
			final TourType tourType = filter.getTourType();
			filterImage = UI.getInstance().getTourTypeImage(tourType.getTypeId());
			break;

		case TourTypeFilter.FILTER_TYPE_SYSTEM:
			filterImage = UI.IMAGE_REGISTRY.get(UI.IMAGE_TOUR_TYPE_FILTER_SYSTEM);
			break;

		case TourTypeFilter.FILTER_TYPE_TOURTYPE_SET:
			filterImage = UI.IMAGE_REGISTRY.get(UI.IMAGE_TOUR_TYPE_FILTER);
			break;

		default:
			break;
		}

		return filterImage;
	}

	/**
	 * @return Returns a list with all tour type filters
	 */
	@SuppressWarnings("unchecked")
	public static ArrayList<TourTypeFilter> getTourTypeFilters() {

		final ArrayList<TourTypeFilter> filterList = readXMLFilterFile();

		final ArrayList<TourType> tourTypes = TourDatabase.getAllTourTypes();
		final ArrayList<TourType> tourTypesNotDisplayed = (ArrayList<TourType>) tourTypes.clone();

		/*
		 * check if all system filters are visible
		 */
		boolean isSysFilterAll = false;
		boolean isSysFilterNotDefined = false;

		for (final TourTypeFilter tourTypeFilter : filterList) {
			if (tourTypeFilter.getFilterType() == TourTypeFilter.FILTER_TYPE_SYSTEM) {
				switch (tourTypeFilter.getSystemFilterId()) {
				case TourTypeFilter.SYSTEM_FILTER_ID_ALL:
					isSysFilterAll = true;
					break;
				case TourTypeFilter.SYSTEM_FILTER_ID_NOT_DEFINED:
					isSysFilterNotDefined = true;
					break;
				default:
					break;
				}
			}
		}

		if (isSysFilterAll == false) {
			filterList.add(new TourTypeFilter(
					TourTypeFilter.SYSTEM_FILTER_ID_ALL,
					Messages.App_Tour_type_item_all_types));
		}
		if (isSysFilterNotDefined == false) {
			filterList.add(new TourTypeFilter(
					TourTypeFilter.SYSTEM_FILTER_ID_NOT_DEFINED,
					Messages.App_Tour_type_item_not_defined));
		}

		/*
		 * ensure that all available tour types are visible
		 */
		for (final TourTypeFilter tourTypeFilter : filterList) {
			final TourType tourType = tourTypeFilter.getTourType();
			if (tourType != null) {
				tourTypesNotDisplayed.remove(tourType);
			}
		}

		for (final TourType tourType : tourTypesNotDisplayed) {
			filterList.add(new TourTypeFilter(tourType));
		}

		return filterList;
	}

	private static XMLMemento getXMLMementoRoot() {
		Document document;
		try {
			document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
			final Element element = document.createElement(MEMENTO_ROOT_FILTER_LIST);
			element.setAttribute("version", "1"); //$NON-NLS-1$ //$NON-NLS-2$
			document.appendChild(element);

			return new XMLMemento(document, element);

		} catch (final ParserConfigurationException e) {
			throw new Error(e.getMessage());
		}
	}

	/**
	 * Read filter list from xml file
	 * 
	 * @return Returns a list with all filters from the xml file
	 */
	private static ArrayList<TourTypeFilter> readXMLFilterFile() {

		final ArrayList<TourType> tourTypes = TourDatabase.getAllTourTypes();
		final ArrayList<TourTypeFilter> filterList = new ArrayList<TourTypeFilter>();

		final IPath stateLocation = Platform.getStateLocation(TourbookPlugin.getDefault().getBundle());
		final String filename = stateLocation.append(MEMENTO_FILTER_LIST_FILE).toFile().getAbsolutePath();

		// check if filter file is available
		final File inputFile = new File(filename);
		if (inputFile.exists() == false) {
			return filterList;
		}

		InputStreamReader reader = null;
		long tourTypeId;

		try {
			reader = new InputStreamReader(new FileInputStream(inputFile), "UTF-8"); //$NON-NLS-1$
			final XMLMemento mementoFilterList = XMLMemento.createReadRoot(reader);

			final IMemento[] mementoFilters = mementoFilterList.getChildren(MEMENTO_CHILD_FILTER);

			for (final IMemento mementoFilter : mementoFilters) {

				final Integer filterType = mementoFilter.getInteger(TAG_FILTER_TYPE);
				final String filterName = mementoFilter.getString(TAG_NAME);

				if (filterType == null || filterName == null) {
					continue;
				}

				switch (filterType) {
				case TourTypeFilter.FILTER_TYPE_SYSTEM:
					final Integer systemId = mementoFilter.getInteger(TAG_SYSTEM_ID);
					if (systemId == null) {
						continue;
					}

					filterList.add(new TourTypeFilter(systemId, filterName));

					break;

				case TourTypeFilter.FILTER_TYPE_DB:

					String tourTypeIdString = mementoFilter.getString(TAG_TOUR_TYPE_ID);

					if (tourTypeIdString == null) {
						continue;
					}

					tourTypeId = Long.parseLong(tourTypeIdString);

					// find the tour type in the available tour types
					for (final TourType tourType : tourTypes) {
						if (tourType.getTypeId() == tourTypeId) {
							filterList.add(new TourTypeFilter(tourType));
							break;
						}
					}

					break;

				case TourTypeFilter.FILTER_TYPE_TOURTYPE_SET:

					final ArrayList<TourType> tourTypesInFilter = new ArrayList<TourType>();
					final IMemento[] mementoTourTypes = mementoFilter.getChildren(MEMENTO_CHILD_TOURTYPE);

					// get all tour types
					for (final IMemento mementoTourType : mementoTourTypes) {

						tourTypeIdString = mementoTourType.getString(TAG_TOUR_TYPE_ID);
						if (tourTypeIdString == null) {
							continue;
						}

						tourTypeId = Long.parseLong(tourTypeIdString);

						// find the tour type in the available tour types
						for (final TourType tourType : tourTypes) {
							if (tourType.getTypeId() == tourTypeId) {
								tourTypesInFilter.add(tourType);
								break;
							}
						}
					}

					final TourTypeFilterSet filterSet = new TourTypeFilterSet();
					filterSet.setName(filterName);
					filterSet.setTourTypes(tourTypesInFilter.toArray());

					filterList.add(new TourTypeFilter(filterSet));

					break;

				default:
					break;
				}
			}

		} catch (final UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (final FileNotFoundException e) {
			e.printStackTrace();
		} catch (final WorkbenchException e) {
			e.printStackTrace();
		} catch (final NumberFormatException e) {
			e.printStackTrace();
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (final IOException e) {
					e.printStackTrace();
				}
			}
		}

		return filterList;
	}

	/**
	 * write the filters from the viewer into an xml file
	 * 
	 * @param filterViewer
	 */
	public static void writeXMLFilterFile(final TableViewer filterViewer) {

		BufferedWriter writer = null;

		try {

			final IPath stateLocation = Platform.getStateLocation(TourbookPlugin.getDefault().getBundle());
			final File file = stateLocation.append(MEMENTO_FILTER_LIST_FILE).toFile();

			writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8")); //$NON-NLS-1$

			final XMLMemento xmlMemento = getXMLMementoRoot();

			for (final TableItem tableItem : filterViewer.getTable().getItems()) {

				final Object itemData = tableItem.getData();
				if (itemData instanceof TourTypeFilter) {

					final TourTypeFilter filter = (TourTypeFilter) itemData;

					final IMemento mementoFilter = xmlMemento.createChild(MEMENTO_CHILD_FILTER);

					final int filterType = filter.getFilterType();

					mementoFilter.putInteger(TAG_FILTER_TYPE, filterType);
					mementoFilter.putString(TAG_NAME, filter.getFilterName());

					switch (filterType) {
					case TourTypeFilter.FILTER_TYPE_SYSTEM:
						mementoFilter.putInteger(TAG_SYSTEM_ID, filter.getSystemFilterId());
						break;

					case TourTypeFilter.FILTER_TYPE_DB:
						mementoFilter.putString(TAG_TOUR_TYPE_ID, Long.toString(filter.getTourType().getTypeId()));
						break;

					case TourTypeFilter.FILTER_TYPE_TOURTYPE_SET:

						final TourTypeFilterSet filterSet = filter.getTourTypeSet();

						final Object[] tourTypes = filterSet.getTourTypes();
						if (tourTypes != null) {
							for (final Object item : tourTypes) {

								final TourType tourType = (TourType) item;
								final IMemento mementoTourType = mementoFilter.createChild(MEMENTO_CHILD_TOURTYPE);
								{
									mementoTourType.putString(TAG_TOUR_TYPE_ID, Long.toString(tourType.getTypeId()));
								}
							}
						}
						break;

					default:
						break;
					}
				}
			}

			xmlMemento.save(writer);

		} catch (final IOException e) {
			e.printStackTrace();
		} finally {
			if (writer != null) {
				try {
					writer.close();
				} catch (final IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * listen for changes in the person list
	 */
	private void addPrefListener() {

		_prefChangeListener = new IPropertyChangeListener() {

			@Override
			public void propertyChange(final PropertyChangeEvent event) {

				final String property = event.getProperty();

				if (property.equals(ITourbookPreferences.APP_DATA_FILTER_IS_MODIFIED)
						|| property.equals(ITourbookPreferences.TOUR_TYPE_LIST_IS_MODIFIED)) {

					final String newValue = event.getNewValue().toString();
					final double propertyValue = Double.parseDouble(newValue);

					// check if the event was originated from this tour type
					// combobox
					if (_propertyValue != propertyValue) {

						TourTypeFilter activeTourTypeFilter = TourbookPlugin.getActiveTourTypeFilter();

						fillFilterComboBox();

						reselectTourType(activeTourTypeFilter);
					}
				}
			}
		};

		_prefStore.addPropertyChangeListener(_prefChangeListener);
	}

	@Override
	protected Control createControl(final Composite parent) {

		if (PlatformUI.getWorkbench().isClosing()) {
			return new Label(parent, SWT.NONE);
		}

		final Composite container = createUI(parent);
		addPrefListener();

		fillFilterComboBox();

		reselectLastTourType();

		return container;
	}

	private Composite createUI(final Composite parent) {

		// create TableCombo
		_tourTypeComboViewer = new TableComboViewer(parent, SWT.READ_ONLY | SWT.BORDER | SWT.FLAT);

		final TableCombo tableCombo = _tourTypeComboViewer.getTableCombo();

//		tableCombo.setLayoutData(new GridData(75, SWT.DEFAULT));
		tableCombo.setVisibleItemCount(50);
		tableCombo.setToolTipText(Messages.App_Tour_type_tooltip);

		// set the content provider
		_tourTypeComboViewer.setContentProvider(ArrayContentProvider.getInstance());

		// set the label provider
		_tourTypeComboViewer.setLabelProvider(new SingleImageItemLabelProvider());

		// add listener
		_tourTypeComboViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(final SelectionChangedEvent event) {

				setActiveTourTypeFilter();

				/*
				 * fire in an asynch that the combo box drop down is hidden and the combo text ist
				 * displayed
				 */
				Display.getCurrent().asyncExec(new Runnable() {
					public void run() {

						// fire change event
						_propertyValue = Math.random();
						_prefStore.setValue(ITourbookPreferences.APP_DATA_FILTER_IS_MODIFIED, _propertyValue);
					}
				});
			}
		});

		_tourTypeComboViewer.getControl().addDisposeListener(new DisposeListener() {
			public void widgetDisposed(final DisposeEvent e) {
				_prefStore.removePropertyChangeListener(_prefChangeListener);
			}
		});

		return tableCombo;
	}

	private void fillFilterComboBox() {

		_tourTypeFilters = getTourTypeFilters();

		_tourTypeComboViewer.setInput(_tourTypeFilters);
	}

	private void reselectLastTourType() {

		final String lastTourTypeFilterName = _state.get(ITourbookPreferences.APP_LAST_SELECTED_TOUR_TYPE_FILTER);

		TourTypeFilter selectTourTypeFilter = null;

		if (lastTourTypeFilterName != null) {

			// find the name in the filter list

			for (final TourTypeFilter tourTypeFilter : _tourTypeFilters) {
				if (tourTypeFilter.getFilterName().equals(lastTourTypeFilterName)) {
					selectTourTypeFilter = tourTypeFilter;
					break;
				}
			}
		}

		if (selectTourTypeFilter == null) {

			// get the filter which selects all tour types

			for (final TourTypeFilter tourTypeFilter : _tourTypeFilters) {
				if (tourTypeFilter.getFilterType() == TourTypeFilter.FILTER_TYPE_SYSTEM
						&& tourTypeFilter.getSystemFilterId() == TourTypeFilter.SYSTEM_FILTER_ID_ALL) {
					selectTourTypeFilter = tourTypeFilter;
					break;
				}
			}
		}

		// try to reselect the last person
		reselectTourType(selectTourTypeFilter);
	}

	/**
	 * reselect the tour type in the combo box and set the active tour type in the plugin
	 * 
	 * @param tourTypeFilter
	 */
	private void reselectTourType(final TourTypeFilter selectTourTypeFilter) {

		final String selectedFilterName = selectTourTypeFilter.getFilterName();
		TourTypeFilter activeTourTypeFilter = null;

		// find the tour type filter in the combobox
		for (final TourTypeFilter tourTypeFilter : _tourTypeFilters) {
			if (tourTypeFilter.getFilterName().equals(selectedFilterName)) {
				activeTourTypeFilter = tourTypeFilter;
				_tourTypeComboViewer.setSelection(new StructuredSelection(activeTourTypeFilter), true);
				break;
			}
		}

		if (activeTourTypeFilter == null) {
			// filter was not found, set first filter as active filter
			activeTourTypeFilter = _tourTypeFilters.get(0);
			_tourTypeComboViewer.setSelection(new StructuredSelection(activeTourTypeFilter), true);
		}

		TourbookPlugin.setActiveTourTypeFilter(activeTourTypeFilter);
	}

	void saveState(final IMemento memento) {

		TourTypeFilter selectedTourTypeFilter = getSelectedTourTypeFilter();

		_state.put(ITourbookPreferences.APP_LAST_SELECTED_TOUR_TYPE_FILTER, selectedTourTypeFilter.getFilterName());
	}

	private TourTypeFilter getSelectedTourTypeFilter() {

		StructuredSelection selection = (StructuredSelection) _tourTypeComboViewer.getSelection();
		TourTypeFilter selectedTourTypeFilter = (TourTypeFilter) selection.getFirstElement();

		return selectedTourTypeFilter;
	}

	/**
	 * set the selected tour type
	 */
	private void setActiveTourTypeFilter() {

		TourTypeFilter selectedTourTypeFilter = getSelectedTourTypeFilter();

		if (selectedTourTypeFilter == null) {
			selectedTourTypeFilter = _tourTypeFilters.get(0);
		}

		TourbookPlugin.setActiveTourTypeFilter(selectedTourTypeFilter);
	}
}
