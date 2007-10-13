/*******************************************************************************
 * Copyright (C) 2005, 2007  Wolfgang Schramm and Contributors
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

import java.util.ArrayList;

import net.tourbook.Messages;
import net.tourbook.data.TourType;
import net.tourbook.database.TourDatabase;
import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.ui.CustomControlContribution;

import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.runtime.Preferences.IPropertyChangeListener;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.PlatformUI;

public class TourTypeContributionItem extends CustomControlContribution {

	private static final String		ID		= "net.tourbook.tourtype-selector"; //$NON-NLS-1$

	static TourbookPlugin			plugin	= TourbookPlugin.getDefault();

	/**
	 * tour types which are displayed in the tour type combobox, this list contains also the pseudo
	 * tour types
	 */
	private ArrayList<TourType>		fAllTourTypes;

	private IPropertyChangeListener	fPrefChangeListener;

	private Combo					fComboTourType;

	public TourTypeContributionItem() {
		this(ID);
	}

	protected TourTypeContributionItem(String id) {
		super(id);
	}

	/**
	 * listen for changes in the person list
	 */
	private void addPrefListener() {

		fPrefChangeListener = new Preferences.IPropertyChangeListener() {
			public void propertyChange(Preferences.PropertyChangeEvent event) {

				final String property = event.getProperty();

				if (property.equals(ITourbookPreferences.TOUR_TYPE_LIST_IS_MODIFIED)) {
					fillTourTypeComboBox();

					reselectTourType(plugin.getActiveTourType().getTypeId());
				}
			}

		};
		// register the listener
		plugin.getPluginPreferences().addPropertyChangeListener(fPrefChangeListener);
	}

	@Override
	protected Control createControl(Composite parent) {

		if (PlatformUI.getWorkbench().isClosing()) {
			return new Label(parent, SWT.NONE);
		}

		Composite container = createTourTypeComboBox(parent);

		addPrefListener();
		reselectLastTourType();

		return container;
	}

	private Composite createTourTypeComboBox(Composite parent) {

		Composite container = new Composite(parent, SWT.NONE);
		GridLayout gl = new GridLayout();
		gl.marginWidth = 0;
		gl.marginHeight = 0;
		gl.horizontalSpacing = 0;
		gl.verticalSpacing = 0;
		container.setLayout(gl);

		fComboTourType = new Combo(container, SWT.DROP_DOWN | SWT.READ_ONLY);
		fComboTourType.setLayoutData(new GridData(SWT.NONE, SWT.CENTER, false, true));
		fComboTourType.setVisibleItemCount(10);
		fComboTourType.setToolTipText(Messages.App_Tour_type_tooltip);

		fComboTourType.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				plugin.getPluginPreferences().removePropertyChangeListener(fPrefChangeListener);
			}
		});

		fComboTourType.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {

				setActiveTourType();

				// fire change event
				plugin.getPreferenceStore()
						.setValue(ITourbookPreferences.APP_DATA_FILTER_IS_MODIFIED, Math.random());

			}
		});

		// add tour type names to the ui list
		fillTourTypeComboBox();

		return container;
	}

	/**
	 * reads the tour types from the db, set the tour type list and fill the combo box
	 */
	private void fillTourTypeComboBox() {

		fAllTourTypes = new ArrayList<TourType>();

		// add entry where the tour type will be ignored
		fAllTourTypes.add(new TourType(Messages.App_Tour_type_item_all_types,
				TourType.TOUR_TYPE_ID_ALL));

		// add tour type for tours where the tour type is not defined
		fAllTourTypes.add(new TourType(Messages.App_Tour_type_item_not_defined,
				TourType.TOUR_TYPE_ID_NOT_DEFINED));

		/*
		 * get tour types from the db
		 */
		ArrayList<TourType> dbTourTypes = TourDatabase.getTourTypes();

//		if (dbTourTypes == null) {
//			return;
//		}

		for (TourType dbTourType : dbTourTypes) {
			fAllTourTypes.add(dbTourType);
		}

		// update combo box
		fComboTourType.removeAll();
		for (TourType tourType : fAllTourTypes) {
			fComboTourType.add(tourType.getName());
		}

		plugin.setAllTourTypes(fAllTourTypes);
		plugin.setDbTourTypes(dbTourTypes);
	}

	void saveState(IMemento memento) {

		// save: selected tour type
		int selectionIndex = fComboTourType.getSelectionIndex();
		if (selectionIndex != -1) {
			plugin.getDialogSettings().put(ITourbookPreferences.APP_LAST_SELECTED_TOUR_TYPE_ID,
					fAllTourTypes.get(selectionIndex).getTypeId());
		}
	}

	/**
	 * set the selected tour type
	 */
	private void setActiveTourType() {

		int selectionIndex = fComboTourType.getSelectionIndex();
		if (selectionIndex == -1) {
			// nothing is selected, select first entry
			selectionIndex = 0;
			fComboTourType.select(selectionIndex);
		}

		plugin.setAllTourTypes(fAllTourTypes);
		plugin.setActiveTourType(fAllTourTypes.get(selectionIndex));
	}

	/**
	 * reselect the tour type in the combo box and set the active tour type in the plugin
	 * 
	 * @param lastTourTypeId
	 */
	private void reselectTourType(Long lastTourTypeId) {

		// if (fTourTypes == null) {
		// fComboTourType.select(0);
		// return;
		// }

		TourType activeTourType = null;
		long activeTourTypeId = TourType.TOUR_TYPE_ID_ALL;

		// find the tour type in the combobox
		int tourTypeIndex = 0;

		for (TourType tourType : fAllTourTypes) {
			if (tourType.getTypeId() == lastTourTypeId) {
				// reselect last tour type
				activeTourTypeId = lastTourTypeId;
				activeTourType = tourType;
				fComboTourType.select(tourTypeIndex);
				break;
			}
			tourTypeIndex++;
		}

		if (activeTourTypeId == TourType.TOUR_TYPE_ID_ALL) {
			// the last tour type was not found, select first entry
			fComboTourType.select(0);
			activeTourType = fAllTourTypes.get(0);
		}

		// if (activeTourType != null) {
		plugin.setActiveTourType(activeTourType);
		// }
	}

	private void reselectLastTourType() {

		Long lastTourTypeId;
		try {

			lastTourTypeId = plugin.getDialogSettings()
					.getLong(ITourbookPreferences.APP_LAST_SELECTED_TOUR_TYPE_ID);

		} catch (NumberFormatException e) {
			// last tour type id was not found, select all
			lastTourTypeId = Long.valueOf(TourType.TOUR_TYPE_ID_ALL);
			// fComboTourType.select(0);
		}

		// try to reselect the last person
		reselectTourType(lastTourTypeId);
	}
}
