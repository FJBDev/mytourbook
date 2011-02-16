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
package net.tourbook.tag;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Set;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.data.TourData;
import net.tourbook.data.TourTag;
import net.tourbook.database.TourDatabase;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tour.TourEvent;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.ITourProvider;
import net.tourbook.ui.ITourProvider2;
import net.tourbook.ui.UI;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;

/**
 */
public class TagManager {

	public static final String[]			EXPAND_TYPE_NAMES				= {
			Messages.app_action_expand_type_flat,
			Messages.app_action_expand_type_year_day,
			Messages.app_action_expand_type_year_month_day					};

	public static final int[]				EXPAND_TYPES					= {
			TourTag.EXPAND_TYPE_FLAT,
			TourTag.EXPAND_TYPE_YEAR_DAY,
			TourTag.EXPAND_TYPE_YEAR_MONTH_DAY								};

	private static final String				SETTINGS_SECTION_RECENT_TAGS	= "TagManager.RecentTags";						//$NON-NLS-1$
	private static final String				SETTINGS_TAG_ID					= "tagId";										//$NON-NLS-1$

	private static final IPreferenceStore	_prefStore						= TourbookPlugin
																					.getDefault()
																					.getPreferenceStore();
	private static IDialogSettings			_state							= TourbookPlugin
																					.getDefault()
																					.getDialogSettingsSection(
																							SETTINGS_SECTION_RECENT_TAGS);

	private static boolean					_isInitialized					= false;

	/**
	 * number of tags which are displayed in the context menu or saved in the dialog settings, it's
	 * max number is 9 to have a unique accelerator key
	 */
	private static LinkedList<TourTag>		_recentTags						= new LinkedList<TourTag>();

	private static int						_maxRecentActions				= -1;
	private static ActionRecentTag[]		_actionsRecentTags;

	private static ITourProvider			_tourProvider;
	private static ActionSetTourTag			_actionSetTourTag;

	private static boolean					_isAddMode;
	private static boolean					_isSaveTour;

	private static IPropertyChangeListener	_prefChangeListener;

	private static class ActionRecentTag extends Action {

		private TourTag	_tag;

		@Override
		public void run() {

			if (_actionSetTourTag == null) {
				setTagIntoTour(_tag, _tourProvider, _isAddMode, _isSaveTour);
			} else {
				_actionSetTourTag.setTourTag(isChecked(), _tag);
			}
		}

		private void setTag(final TourTag tag, final String tagText) {
			setText(tagText);
			_tag = tag;
		}
	}

	private static void addActionToMenu(final Menu menu, final Action action) {

		final ActionContributionItem item = new ActionContributionItem(action);
		item.fill(menu, -1);
	}

	private static void addPrefListener() {

		// create pref listener
		_prefChangeListener = new IPropertyChangeListener() {
			public void propertyChange(final PropertyChangeEvent event) {
				final String property = event.getProperty();

				// check if the number of recent tags has changed
				if (property.equals(ITourbookPreferences.APPEARANCE_NUMBER_OF_RECENT_TAGS)) {
					setActions();
				}
			}
		};

		// add pref listener
		_prefStore.addPropertyChangeListener(_prefChangeListener);
	}

	/**
	 * Adds {@link TourTag}'s to the list of the recently used tags
	 * 
	 * @param tagCollection
	 */
	private static void addRecentTag(final Collection<TourTag> tagCollection) {

		for (final TourTag tag : tagCollection) {
			_recentTags.remove(tag);
			_recentTags.addFirst(tag);
		}
	}

	public static void enableRecentTagActions(final boolean isEnabled, final ArrayList<Long> allExistingTagIds) {

		if (_actionsRecentTags == null) {
			return;
		}

		final boolean isExistingTagIds = allExistingTagIds != null && allExistingTagIds.size() > 0;

		for (final ActionRecentTag actionRecentTag : _actionsRecentTags) {

			final TourTag actionTag = actionRecentTag._tag;
			if (actionTag == null) {
				actionRecentTag.setEnabled(false);
				continue;
			}

			if (isExistingTagIds && isEnabled) {

				// disable action when it's tag id is contained in allExistingTagIds

				boolean isExistTagId = false;

				final long recentTagId = actionTag.getTagId();

				for (final long existingTagId : allExistingTagIds) {
					if (recentTagId == existingTagId) {
						isExistTagId = true;
						break;
					}
				}

				actionRecentTag.setEnabled(isExistTagId == false);

			} else {
				actionRecentTag.setEnabled(isEnabled);
			}
		}
	}

	public static void enableRecentTagActions(final boolean isEnabled, final Set<TourTag> allExistingTags) {

		if (_actionsRecentTags == null) {
			return;
		}

		final boolean isExistingTags = allExistingTags != null && allExistingTags.size() > 0;

		for (final ActionRecentTag actionRecentTag : _actionsRecentTags) {

			final TourTag actionTag = actionRecentTag._tag;
			if (actionTag == null) {
				actionRecentTag.setEnabled(false);
				continue;
			}

			if (isExistingTags && isEnabled) {

				// disable action when it's tag id is contained in allExistingTags

				boolean isExistTagId = false;

				final long recentTagId = actionTag.getTagId();

				for (final TourTag existingTag : allExistingTags) {
					if (recentTagId == existingTag.getTagId()) {
						isExistTagId = true;
						break;
					}
				}

				actionRecentTag.setEnabled(isExistTagId == false);

			} else {
				actionRecentTag.setEnabled(isEnabled);
			}
		}
	}

	/**
	 * Create the menu entries for the recently used tags
	 * 
	 * @param menuMgr
	 * @param isSaveTour
	 */
	public static void fillMenuRecentTags(	final IMenuManager menuMgr,
											final ITourProvider tourProvider,
											final boolean isAddMode,
											final boolean isSaveTour) {

		if (_isInitialized == false) {
			initTagManager();
		}

		if (_recentTags.size() == 0) {
			return;
		}

		if (_maxRecentActions < 1) {
			return;
		}

		_tourProvider = tourProvider;
		_actionSetTourTag = null;

		_isAddMode = isAddMode;
		_isSaveTour = isSaveTour;

		// add tag's
		int tagIndex = 0;
		for (final ActionRecentTag actionRecentTag : _actionsRecentTags) {
			try {

				final TourTag tag = _recentTags.get(tagIndex);

				actionRecentTag.setTag(tag, (UI.SPACE4 + UI.MNEMONIC + (tagIndex + 1) + UI.SPACE2 + tag.getTagName()));

				menuMgr.add(actionRecentTag);

			} catch (final IndexOutOfBoundsException e) {
				// there are no more recent tags
				break;
			}

			tagIndex++;
		}
	}

	public static void fillMenuRecentTags(	final Menu menu,
											final ActionSetTourTag actionSetTourTag,
											final boolean isAddMode,
											final boolean isSaveTour) {

		if (_isInitialized == false) {
			initTagManager();
		}

		if (_recentTags.size() == 0) {
			return;
		}

		if (_maxRecentActions < 1) {
			return;
		}

		_tourProvider = null;
		_actionSetTourTag = actionSetTourTag;

		_isAddMode = isAddMode;
		_isSaveTour = isSaveTour;

		// add tag's
		int tagIndex = 0;
		for (final ActionRecentTag actionRecentTag : _actionsRecentTags) {
			try {

				final TourTag tag = _recentTags.get(tagIndex);

				actionRecentTag.setTag(tag, (UI.MNEMONIC + (tagIndex + 1) + UI.SPACE2 + tag.getTagName()));

				addActionToMenu(menu, actionRecentTag);

			} catch (final IndexOutOfBoundsException e) {
				// there are no more recent tags
				break;
			}

			tagIndex++;
		}
	}

	private static synchronized void initTagManager() {

		setActions();
		addPrefListener();

		_isInitialized = true;
	}

	public static void restoreState() {

		final String[] allStateTagIds = _state.getArray(SETTINGS_TAG_ID);
		if (allStateTagIds == null) {
			return;
		}

		final HashMap<Long, TourTag> allTags = TourDatabase.getAllTourTags();
		for (final String tagId : allStateTagIds) {
			try {
				final TourTag tag = allTags.get(Long.valueOf(tagId));
				if (tag != null) {
					_recentTags.add(tag);
				}
			} catch (final NumberFormatException e) {
				// ignore
			}
		}
	}

	/**
	 * Save modified tours and notify tour provider
	 * 
	 * @param modifiedTags
	 * @param modifiedTours
	 * @param tourProvider
	 * @param isSaveTour
	 */
	public static void saveAndNotify(	final HashMap<Long, TourTag> modifiedTags,
										ArrayList<TourData> modifiedTours,
										final ITourProvider tourProvider,
										final boolean isSaveTour) {
		if (isSaveTour) {

			// save all tours with the removed tags

			modifiedTours = TourManager.saveModifiedTours(modifiedTours);

		} else {

			// tours are not saved but the tour provider must be notified that tours has changed

			if (tourProvider instanceof ITourProvider2) {
				((ITourProvider2) tourProvider).toursAreModified(modifiedTours);
			} else {
				TourManager.fireEvent(TourEventId.TOUR_CHANGED, new TourEvent(modifiedTours));
			}
		}

		TourManager.fireEvent(TourEventId.NOTIFY_TAG_VIEW, new ChangedTags(modifiedTags, modifiedTours, false));
	}

	public static void saveState() {

		if (_maxRecentActions < 1) {
			// tour types are not initialized or not visible, do nothing
			return;
		}

		final String[] tagIds = new String[Math.min(_maxRecentActions, _recentTags.size())];
		int tagIndex = 0;

		for (final TourTag tag : _recentTags) {
			tagIds[tagIndex++] = Long.toString(tag.getTagId());

			if (tagIndex == _maxRecentActions) {
				break;
			}
		}

		_state.put(SETTINGS_TAG_ID, tagIds);
	}

	/**
	 * create actions for recenct tags
	 */
	private static void setActions() {

		_maxRecentActions = TourbookPlugin
				.getDefault()
				.getPreferenceStore()
				.getInt(ITourbookPreferences.APPEARANCE_NUMBER_OF_RECENT_TAGS);

		_actionsRecentTags = new ActionRecentTag[_maxRecentActions];

		for (int actionIndex = 0; actionIndex < _actionsRecentTags.length; actionIndex++) {
			_actionsRecentTags[actionIndex] = new ActionRecentTag();
		}
	}

	/**
	 * Set tour tag for multiple tours
	 * 
	 * @param modifiedTags
	 * @param tourProvider
	 * @param isAddMode
	 * @param isSaveTour
	 */
	public static void setTagIntoTour(	final HashMap<Long, TourTag> modifiedTags,
										final ITourProvider tourProvider,
										final boolean isAddMode,
										final boolean isSaveTour) {

		final Runnable runnable = new Runnable() {

			public void run() {

				final ArrayList<TourData> modifiedTours = tourProvider.getSelectedTours();

				// get tours which tag should be changed
				if (modifiedTours == null || modifiedTours.size() == 0) {
					return;
				}

				final Collection<TourTag> tagCollection = modifiedTags.values();

				// add the tag into all selected tours
				for (final TourData tourData : modifiedTours) {

					// set tag into tour
					final Set<TourTag> tourTags = tourData.getTourTags();

					if (isAddMode) {
						// add tag to the tour
						tourTags.addAll(tagCollection);
					} else {
						// remove tag from tour
						tourTags.removeAll(tagCollection);
					}
				}

				addRecentTag(tagCollection);

				saveAndNotify(modifiedTags, modifiedTours, tourProvider, isSaveTour);
			}
		};

		BusyIndicator.showWhile(Display.getCurrent(), runnable);
	}

	/**
	 * Set tour tag for one tour
	 * 
	 * @param tag
	 * @param tourProvider
	 * @param isAddMode
	 * @param isSaveTour
	 */
	public static void setTagIntoTour(	final TourTag tag,
										final ITourProvider tourProvider,
										final boolean isAddMode,
										final boolean isSaveTour) {

		final HashMap<Long, TourTag> tags = new HashMap<Long, TourTag>();
		tags.put(tag.getTagId(), tag);

		setTagIntoTour(tags, tourProvider, isAddMode, isSaveTour);
	}

	/**
	 * Update names of all recent tags
	 */
	public static void updateTagNames() {

		final HashMap<Long, TourTag> allTourTags = TourDatabase.getAllTourTags();

		for (final TourTag recentTag : _recentTags) {

			final TourTag tourTag = allTourTags.get(recentTag.getTagId());
			if (tourTag != null) {
				recentTag.setTagName(tourTag.getTagName());
			}
		}
	}
}
