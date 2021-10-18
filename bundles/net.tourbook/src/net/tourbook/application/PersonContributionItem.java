/*******************************************************************************
 * Copyright (C) 2005, 2021 Wolfgang Schramm and Contributors
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

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.tourbook.Messages;
import net.tourbook.common.UI;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.data.TourPerson;
import net.tourbook.database.PersonManager;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.ui.CustomControlContribution;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

public class PersonContributionItem extends CustomControlContribution {

   private static final String     ID         = "net.tourbook.clientselector";  //$NON-NLS-1$

   private static TourbookPlugin   _activator = TourbookPlugin.getDefault();

   private final IDialogSettings   _state     = _activator.getDialogSettings();
   private final IPreferenceStore  _prefStore = _activator.getPreferenceStore();

   private IPropertyChangeListener _prefChangeListener;

   private ArrayList<TourPerson>   _allPeople;

   private Combo                   _cboPeople;

   public PersonContributionItem() {
      this(ID);
   }

   protected PersonContributionItem(final String id) {
      super(id);
   }

   /**
    * listen for changes in the person list
    */
   private void addPrefListener() {

      _prefChangeListener = new IPropertyChangeListener() {
         @Override
         public void propertyChange(final PropertyChangeEvent event) {

            final String property = event.getProperty();

            if (property.equals(ITourbookPreferences.TOUR_PERSON_LIST_IS_MODIFIED)) {

               // fill people combobox with modified people list
               fillPeopleComboBox();

               final TourPerson currentPerson = TourbookPlugin.getActivePerson();

               // reselect the person which was selected before
               if (currentPerson == null) {
                  _cboPeople.select(0);
               } else {
                  // try to set and select the old person
                  final long previousPersonId = currentPerson.getPersonId();
                  reselectPerson(previousPersonId);
               }
            }
         }
      };
      // register the listener
      _prefStore.addPropertyChangeListener(_prefChangeListener);
   }

<<<<<<< HEAD
   private void addTourPersonListener(final TourPerson tourPerson) {
=======
   @Override
   protected Control createControl(final Composite parent) {
>>>>>>> refs/remotes/origin/main

<<<<<<< HEAD
      tourPerson.addChangeListener(new ChangeListener() {
=======
      Composite content;
>>>>>>> refs/remotes/origin/main

<<<<<<< HEAD
         @Override
         public void stateChanged(final ChangeEvent e) {
=======
      if (UI.IS_OSX) {
>>>>>>> refs/remotes/origin/main

<<<<<<< HEAD
            final long currentPersonId = (long) e.getSource();
=======
         content = createPeopleComboBox(parent);
>>>>>>> refs/remotes/origin/main

<<<<<<< HEAD
            _allPeople = PersonManager.getTourPeople();
=======
      } else {
>>>>>>> refs/remotes/origin/main

<<<<<<< HEAD
            final int selectedIndex = _cboPeople.getSelectionIndex();
            if (selectedIndex == 0) {
               return;
            }
=======
         /*
          * on win32 a few pixel above and below the combobox are drawn, wrapping it into a
          * composite removes the pixels
          */
         content = new Composite(parent, SWT.NONE);
         GridLayoutFactory.fillDefaults().spacing(0, 0).applyTo(content);
>>>>>>> refs/remotes/origin/main

<<<<<<< HEAD
            //The selected person has changed, we need to load it again
            for (final TourPerson tourPerson : _allPeople) {
               if (tourPerson.getPersonId() == currentPersonId) {
                  reselectPerson(currentPersonId);
               }
            }
=======
         final Composite control = createPeopleComboBox(content);
         control.setLayoutData(new GridData(SWT.NONE, SWT.CENTER, false, true));
      }
>>>>>>> refs/remotes/origin/main

<<<<<<< HEAD
         }
      });
=======
      addPrefListener();
      reselectLastPerson();
>>>>>>> refs/remotes/origin/main

<<<<<<< HEAD
=======
      return content;
>>>>>>> refs/remotes/origin/main
   }

<<<<<<< HEAD
   @Override
   protected Control createControl(final Composite parent) {
=======
   private Composite createPeopleComboBox(final Composite parent) {
>>>>>>> refs/remotes/origin/main

<<<<<<< HEAD
      Composite content;
=======
      _cboPeople = new Combo(parent, SWT.DROP_DOWN | SWT.READ_ONLY);
>>>>>>> refs/remotes/origin/main

<<<<<<< HEAD
      if (UI.IS_OSX) {
=======
      _cboPeople.setVisibleItemCount(20);
      _cboPeople.setToolTipText(Messages.App_People_tooltip);
>>>>>>> refs/remotes/origin/main

<<<<<<< HEAD
         content = createPeopleComboBox(parent);
=======
      _cboPeople.addDisposeListener(new DisposeListener() {
         @Override
         public void widgetDisposed(final DisposeEvent e) {
            if (_prefChangeListener != null) {
               _prefStore.removePropertyChangeListener(_prefChangeListener);
            }
         }
      });
>>>>>>> refs/remotes/origin/main

<<<<<<< HEAD
      } else {
=======
      _cboPeople.addSelectionListener(new SelectionAdapter() {
         @Override
         public void widgetSelected(final SelectionEvent e) {
            onSelectPerson();
         }
      });
>>>>>>> refs/remotes/origin/main

<<<<<<< HEAD
         /*
          * on win32 a few pixel above and below the combobox are drawn, wrapping it into a
          * composite removes the pixels
          */
         content = new Composite(parent, SWT.NONE);
         GridLayoutFactory.fillDefaults().spacing(0, 0).applyTo(content);
=======
      fillPeopleComboBox();
>>>>>>> refs/remotes/origin/main

<<<<<<< HEAD
         final Composite control = createPeopleComboBox(content);
         control.setLayoutData(new GridData(SWT.NONE, SWT.CENTER, false, true));
      }
=======
      return _cboPeople;
   }
>>>>>>> refs/remotes/origin/main

<<<<<<< HEAD
      addPrefListener();
      reselectLastPerson();
=======
   private void fillPeopleComboBox() {
>>>>>>> refs/remotes/origin/main

<<<<<<< HEAD
      return content;
   }
=======
      _cboPeople.removeAll();
>>>>>>> refs/remotes/origin/main

<<<<<<< HEAD
   private Composite createPeopleComboBox(final Composite parent) {
=======
      /*
       * removed the dash in the "All People" string because the whole item was not displayed on mac
       * osx
       */
      _cboPeople.add(Messages.App_People_item_all);
>>>>>>> refs/remotes/origin/main

<<<<<<< HEAD
      _cboPeople = new Combo(parent, SWT.DROP_DOWN | SWT.READ_ONLY);
=======
      _allPeople = PersonManager.getTourPeople();
>>>>>>> refs/remotes/origin/main

<<<<<<< HEAD
      _cboPeople.setVisibleItemCount(20);
      _cboPeople.setToolTipText(Messages.App_People_tooltip);
=======
      if (_allPeople == null) {
         return;
      }
>>>>>>> refs/remotes/origin/main

<<<<<<< HEAD
      _cboPeople.addDisposeListener(new DisposeListener() {
         @Override
         public void widgetDisposed(final DisposeEvent e) {
            if (_prefChangeListener != null) {
               _prefStore.removePropertyChangeListener(_prefChangeListener);
            }
         }
      });
=======
      for (final TourPerson person : _allPeople) {
         String lastName = person.getLastName();
         lastName = StringUtils.isNullOrEmpty(lastName) ? UI.EMPTY_STRING : UI.SPACE + lastName;
         _cboPeople.add(person.getFirstName() + lastName);
      }
   }
>>>>>>> refs/remotes/origin/main

<<<<<<< HEAD
      _cboPeople.addSelectionListener(new SelectionAdapter() {
         @Override
         public void widgetSelected(final SelectionEvent e) {
            onSelectPerson();
         }
      });
=======
   /**
    * fire event that person has changed
    */
   private void fireEventNewPersonIsSelected() {
      _prefStore.setValue(ITourbookPreferences.APP_DATA_FILTER_IS_MODIFIED, Math.random());
   }
>>>>>>> refs/remotes/origin/main

<<<<<<< HEAD
      fillPeopleComboBox();
=======
   private void onSelectPerson() {
>>>>>>> refs/remotes/origin/main

<<<<<<< HEAD
      return _cboPeople;
   }
=======
      final int selectedIndex = _cboPeople.getSelectionIndex();
      if (selectedIndex == -1) {
         return;
      }
>>>>>>> refs/remotes/origin/main

<<<<<<< HEAD
   private void fillPeopleComboBox() {
=======
      if (selectedIndex == 0) {
         // all people are selected
         TourbookPlugin.setActivePerson(null);
      } else {
         // a person is selected
         TourbookPlugin.setActivePerson(_allPeople.get(selectedIndex - 1));
      }
>>>>>>> refs/remotes/origin/main

<<<<<<< HEAD
      _cboPeople.removeAll();
=======
      fireEventNewPersonIsSelected();
   }
>>>>>>> refs/remotes/origin/main

<<<<<<< HEAD
      /*
       * removed the dash in the "All People" string because the whole item was not displayed on mac
       * osx
       */
      _cboPeople.add(Messages.App_People_item_all);
=======
   /**
    * select the person which was set in the dialog settings
    */
   private void reselectLastPerson() {
>>>>>>> refs/remotes/origin/main

<<<<<<< HEAD
      _allPeople = PersonManager.getTourPeople();
=======
      try {
>>>>>>> refs/remotes/origin/main

<<<<<<< HEAD
      if (_allPeople == null) {
         return;
      }
=======
         final long lastPersonId = _state.getLong(ITourbookPreferences.APP_LAST_SELECTED_PERSON_ID);
>>>>>>> refs/remotes/origin/main

<<<<<<< HEAD
      for (final TourPerson person : _allPeople) {
         String lastName = person.getLastName();
         lastName = lastName.equals(UI.EMPTY_STRING) ? UI.EMPTY_STRING : UI.SPACE + lastName;
         _cboPeople.add(person.getFirstName() + lastName);
      }
   }
=======
         // try to reselect the last person
         reselectPerson(lastPersonId);
>>>>>>> refs/remotes/origin/main

<<<<<<< HEAD
   /**
    * fire event that person has changed
    */
   private void fireEventNewPersonIsSelected() {
      _prefStore.setValue(ITourbookPreferences.APP_DATA_FILTER_IS_MODIFIED, Math.random());
=======
      } catch (final NumberFormatException e) {
         // no last person id, select all
         _cboPeople.select(0);
      }
>>>>>>> refs/remotes/origin/main
   }

<<<<<<< HEAD
   private void onSelectPerson() {
=======
   private void reselectPerson(final long previousPersonId) {
>>>>>>> refs/remotes/origin/main

<<<<<<< HEAD
      final int selectedIndex = _cboPeople.getSelectionIndex();
      if (selectedIndex == -1) {
=======
      if (_allPeople == null) {
         _cboPeople.select(0);
>>>>>>> refs/remotes/origin/main
         return;
      }

<<<<<<< HEAD
      if (selectedIndex == 0) {
         // all people are selected
         TourbookPlugin.setActivePerson(null);
      } else {
         // a person is selected
         TourbookPlugin.setActivePerson(_allPeople.get(selectedIndex - 1));
      }
=======
      TourPerson currentPerson = null;
      int personIndex = 1;
>>>>>>> refs/remotes/origin/main

<<<<<<< HEAD
      fireEventNewPersonIsSelected();
   }
=======
      for (final TourPerson person : _allPeople) {
         if (previousPersonId == person.getPersonId()) {
            // previous person was found
            _cboPeople.select(personIndex);
            currentPerson = person;
            break;
         }
         personIndex++;
      }
>>>>>>> refs/remotes/origin/main

<<<<<<< HEAD
   /**
    * select the person which was set in the dialog settings
    */
   private void reselectLastPerson() {
=======
      if (currentPerson == null) {
         // old person was not found in the new list
         _cboPeople.select(0);
      }
>>>>>>> refs/remotes/origin/main

<<<<<<< HEAD
      try {
=======
      TourbookPlugin.setActivePerson(currentPerson);
   }
>>>>>>> refs/remotes/origin/main

<<<<<<< HEAD
         final long lastPersonId = _state.getLong(ITourbookPreferences.APP_LAST_SELECTED_PERSON_ID);
=======
   /**
    * save current person id in the dialog settings
    */
   void saveState() {
>>>>>>> refs/remotes/origin/main

<<<<<<< HEAD
         // try to reselect the last person
         reselectPerson(lastPersonId);
=======
      if (_cboPeople == null || _cboPeople.isDisposed()) {
         StatusUtil.logError("Cannot save selected person, _cboPeople.isDisposed()");//$NON-NLS-1$
         return;
      }
>>>>>>> refs/remotes/origin/main

<<<<<<< HEAD
      } catch (final NumberFormatException e) {
         // no last person id, select all
         _cboPeople.select(0);
      }
   }
=======
      final int selectedIndex = _cboPeople.getSelectionIndex();
>>>>>>> refs/remotes/origin/main

<<<<<<< HEAD
   private void reselectPerson(final long previousPersonId) {
=======
      long personId = -1;
      if (selectedIndex > 0) {
         personId = _allPeople.get(selectedIndex - 1).getPersonId();
      }
>>>>>>> refs/remotes/origin/main

<<<<<<< HEAD
      if (_allPeople == null) {
         _cboPeople.select(0);
         return;
      }
=======
      _state.put(ITourbookPreferences.APP_LAST_SELECTED_PERSON_ID, personId);
   }
>>>>>>> refs/remotes/origin/main

<<<<<<< HEAD
      TourPerson currentPerson = null;
      int personIndex = 1;
=======
   void selectFirstPerson() {
>>>>>>> refs/remotes/origin/main

<<<<<<< HEAD
      for (final TourPerson person : _allPeople) {
         if (previousPersonId == person.getPersonId()) {
            // previous person was found
            _cboPeople.select(personIndex);
            currentPerson = person;
            addTourPersonListener(currentPerson);
            break;
         }
         personIndex++;
      }
=======
      final int peopleCount = _cboPeople.getItemCount();
>>>>>>> refs/remotes/origin/main

<<<<<<< HEAD
      if (currentPerson == null) {
         // old person was not found in the new list
         _cboPeople.select(0);
      }

      TourbookPlugin.setActivePerson(currentPerson);

   }

   /**
    * save current person id in the dialog settings
    */
   void saveState() {

      if (_cboPeople == null || _cboPeople.isDisposed()) {
         StatusUtil.log("cannot save selected person, _cboPeople.isDisposed()");//$NON-NLS-1$
         return;
      }

      final int selectedIndex = _cboPeople.getSelectionIndex();

      long personId = -1;
      if (selectedIndex > 0) {
         personId = _allPeople.get(selectedIndex - 1).getPersonId();
      }

      _state.put(ITourbookPreferences.APP_LAST_SELECTED_PERSON_ID, personId);
   }

   void selectFirstPerson() {

      final int peopleCount = _cboPeople.getItemCount();

=======
>>>>>>> refs/remotes/origin/main
      if (peopleCount > 1) {
         _cboPeople.select(1);
      }
   }

}
