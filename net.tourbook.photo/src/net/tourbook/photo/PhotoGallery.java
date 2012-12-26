/*******************************************************************************
 * Copyright (C) 2005, 2012  Wolfgang Schramm and Contributors
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
package net.tourbook.photo;

import net.tourbook.common.UI;
import net.tourbook.common.util.Util;
import net.tourbook.photo.internal.ActionImageFilterGPS;
import net.tourbook.photo.internal.ActionImageFilterNoGPS;
import net.tourbook.photo.internal.ActionShowGPSAnnotations;
import net.tourbook.photo.internal.ActionShowPhotoDate;
import net.tourbook.photo.internal.ActionShowPhotoName;
import net.tourbook.photo.internal.ActionShowPhotoTooltip;
import net.tourbook.photo.internal.ActionSortByFileDate;
import net.tourbook.photo.internal.ActionSortByFileName;
import net.tourbook.photo.internal.Activator;
import net.tourbook.photo.internal.ImageFilter;
import net.tourbook.photo.internal.Messages;
import net.tourbook.photo.internal.PhotoDateInfo;
import net.tourbook.photo.internal.manager.GallerySorting;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.widgets.Composite;

public class PhotoGallery extends ImageGallery {

	private static final String			STATE_GALLERY_SORTING				= "STATE_GALLERY_SORTING";					//$NON-NLS-1$

	private static final String			STATE_IMAGE_FILTER					= "STATE_IMAGE_FILTER";					//$NON-NLS-1$
	private static final String			STATE_PHOTO_INFO_DATE				= "STATE_PHOTO_INFO_DATE";					//$NON-NLS-1$
	private static final String			STATE_IS_SHOW_PHOTO_NAME_IN_GALLERY	= "STATE_IS_SHOW_PHOTO_NAME_IN_GALLERY";	//$NON-NLS-1$
	private static final String			STATE_IS_SHOW_PHOTO_GPS_ANNOTATION	= "STATE_IS_SHOW_PHOTO_GPS_ANNOTATION";	//$NON-NLS-1$
	private static final String			STATE_IS_SHOW_PHOTO_TOOLTIP			= "STATE_IS_SHOW_PHOTO_TOOLTIP";			//$NON-NLS-1$

	public static final String			IMAGE_PHOTO_FILTER_GPS				= "IMAGE_PHOTO_FILTER_GPS";				//$NON-NLS-1$
	public static final String			IMAGE_PHOTO_FILTER_NO_GPS			= "IMAGE_PHOTO_FILTER_NO_GPS";				//$NON-NLS-1$

	private IDialogSettings				_state;
	private PhotoDateInfo				_photoDateInfo;

	private boolean						_isShowActionFiltering				= true;
	private boolean						_isShowActionSorting				= true;

	private ActionImageFilterGPS		_actionImageFilterGPS;
	private ActionImageFilterNoGPS		_actionImageFilterNoGPS;
	private ActionShowPhotoName			_actionShowPhotoName;
	private ActionShowPhotoDate			_actionShowPhotoDate;
	private ActionShowPhotoTooltip		_actionShowPhotoTooltip;
	private ActionShowGPSAnnotations	_actionShowGPSAnnotation;
	private ActionSortByFileDate		_actionSortFileByDate;
	private ActionSortByFileName		_actionSortByFileName;
	private ImageFilter					_currentImageFilter					= ImageFilter.NoFilter;

	private GallerySorting				_gallerySorting;

	static {
		UI.IMAGE_REGISTRY.put(//
				IMAGE_PHOTO_FILTER_GPS,
				Activator.getImageDescriptor(Messages.Image__PhotoFilterGPS));
		UI.IMAGE_REGISTRY.put(//
				IMAGE_PHOTO_FILTER_NO_GPS,
				Activator.getImageDescriptor(Messages.Image__PhotoFilterNoGPS));
	}

	public PhotoGallery(final IDialogSettings state) {
		_state = state;
	}

	public void actionImageFilter(final Action actionImageFilter) {

		/*
		 * get selected filter, uncheck other
		 */
		if (actionImageFilter == _actionImageFilterGPS) {

			_currentImageFilter = actionImageFilter.isChecked() ? ImageFilter.GPS : ImageFilter.NoFilter;

			_actionImageFilterNoGPS.setChecked(false);

		} else if (actionImageFilter == _actionImageFilterNoGPS) {

			_currentImageFilter = actionImageFilter.isChecked() ? ImageFilter.NoGPS : ImageFilter.NoFilter;

			_actionImageFilterGPS.setChecked(false);
		}

		// update gallery

		filterGallery(_currentImageFilter);
	}

	public void actionShowPhotoGallery(final Action action) {
		// TODO Auto-generated method stub

	}

	public void actionShowPhotoInfo(final Action action) {

		if (action == _actionShowPhotoDate) {

			// toggle date info

			if (_photoDateInfo == PhotoDateInfo.NoDateTime) {

				// nothing -> date

				_photoDateInfo = PhotoDateInfo.Date;

			} else if (_photoDateInfo == PhotoDateInfo.Date) {

				// date -> time

				_photoDateInfo = PhotoDateInfo.Time;

			} else if (_photoDateInfo == PhotoDateInfo.Time) {

				// time -> date/time

				_photoDateInfo = PhotoDateInfo.DateTime;

			} else {

				// time -> nothing

				_photoDateInfo = PhotoDateInfo.NoDateTime;
			}

			_actionShowPhotoDate.setChecked(_photoDateInfo != PhotoDateInfo.NoDateTime);
		}

		showInfo(//
				_actionShowPhotoName.isChecked(),
				_photoDateInfo,
				_actionShowGPSAnnotation.isChecked(),
				_actionShowPhotoTooltip.isChecked());
	}

	public void actionSortByDate() {

		final boolean isChecked = _actionSortFileByDate.isChecked();

		if (isChecked) {
			_gallerySorting = GallerySorting.FILE_DATE;
			_actionSortByFileName.setChecked(false);
		} else {
			_gallerySorting = GallerySorting.FILE_NAME;
			_actionSortByFileName.setChecked(true);
		}

		sortGallery(_gallerySorting);
	}

	public void actionSortByName() {

		final boolean isChecked = _actionSortByFileName.isChecked();

		if (isChecked) {
			_gallerySorting = GallerySorting.FILE_NAME;
			_actionSortFileByDate.setChecked(false);
		} else {
			_gallerySorting = GallerySorting.FILE_DATE;
			_actionSortFileByDate.setChecked(true);
		}

		sortGallery(_gallerySorting);
	}

	/**
	 * Creates action bar with default actions.
	 */
	public void createActionBar() {
		createActions();
		fillActionBars();
	}

	private void createActions() {

		_actionImageFilterGPS = new ActionImageFilterGPS(this);
		_actionImageFilterNoGPS = new ActionImageFilterNoGPS(this);

		_actionShowGPSAnnotation = new ActionShowGPSAnnotations(this);

		_actionShowPhotoName = new ActionShowPhotoName(this);
		_actionShowPhotoDate = new ActionShowPhotoDate(this);
		_actionShowPhotoTooltip = new ActionShowPhotoTooltip(this);

		_actionSortByFileName = new ActionSortByFileName(this);
		_actionSortFileByDate = new ActionSortByFileDate(this);
	}

	public void createPhotoGallery(	final Composite parent,
									final int style,
									final IPhotoGalleryProvider photoGalleryProvider) {

		super.createImageGallery(parent, style, photoGalleryProvider);
	}

	private void enableActions() {

//		final boolean isEnableGalleryText = _thumbnailSize >= _textMinThumbSize;
//
//		_actionShowPhotoName.setEnabled(isEnableGalleryText);
//		_actionShowPhotoDate.setEnabled(isEnableGalleryText);
	}

	/**
	 * fill view toolbar
	 */
	private void fillActionBars() {

		/*
		 * fill view toolbar
		 */
		final IToolBarManager tbm = _photoGalleryProvider.getToolBarManager();

		if (tbm == null) {
			return;
		}

		tbm.add(_actionShowPhotoDate);
		tbm.add(_actionShowPhotoName);
		tbm.add(_actionShowPhotoTooltip);
		tbm.add(_actionShowGPSAnnotation);

		if (_isShowActionFiltering) {
			tbm.add(new Separator());
			tbm.add(_actionImageFilterGPS);
			tbm.add(_actionImageFilterNoGPS);
		}

		if (_isShowActionSorting) {
			tbm.add(new Separator());
			tbm.add(_actionSortFileByDate);
			tbm.add(_actionSortByFileName);
		}
	}

	public void hideActionFiltering() {
		_isShowActionFiltering = false;
	}

	public void hideActionSorting() {
		_isShowActionSorting = false;
	}

	@Override
	public void restoreState() {

		/*
		 * image filter
		 */
		final String prefImageFilter = Util.getStateString(_state, STATE_IMAGE_FILTER, ImageFilter.NoFilter.name());
		try {
			_currentImageFilter = ImageFilter.valueOf(prefImageFilter);
		} catch (final Exception e) {
			_currentImageFilter = ImageFilter.NoFilter;
		}
		_actionImageFilterGPS.setChecked(_currentImageFilter == ImageFilter.GPS);
		_actionImageFilterNoGPS.setChecked(_currentImageFilter == ImageFilter.NoGPS);

		/*
		 * photo date / time / name / tooltip / annotation
		 */
		final PhotoDateInfo photoDateDefault = PhotoDateInfo.NoDateTime;
		final String prefDateInfo = Util.getStateString(_state, STATE_PHOTO_INFO_DATE, photoDateDefault.name());
		try {
			_photoDateInfo = PhotoDateInfo.valueOf(prefDateInfo);
		} catch (final Exception e) {
			_photoDateInfo = photoDateDefault;
		}

		final boolean isShowPhotoName = Util.getStateBoolean(_state, STATE_IS_SHOW_PHOTO_NAME_IN_GALLERY, false);
		final boolean isShowTooltip = Util.getStateBoolean(_state, STATE_IS_SHOW_PHOTO_TOOLTIP, true);
		final boolean isShowPhotoAnnotations = Util.getStateBoolean(_state, //
				STATE_IS_SHOW_PHOTO_GPS_ANNOTATION,
				true);

		_actionShowPhotoName.setChecked(isShowPhotoName);
		_actionShowPhotoDate.setChecked(_photoDateInfo != PhotoDateInfo.NoDateTime);
		_actionShowPhotoTooltip.setChecked(isShowTooltip);
		_actionShowGPSAnnotation.setChecked(isShowPhotoAnnotations);

		restoreInfo(isShowPhotoName, _photoDateInfo, isShowPhotoAnnotations, isShowTooltip);

		/*
		 * gallery sorting
		 */
		final String prefSorting = Util.getStateString(_state, STATE_GALLERY_SORTING, GallerySorting.FILE_DATE.name());
		try {
			_gallerySorting = GallerySorting.valueOf(prefSorting);
		} catch (final Exception e) {
			_gallerySorting = GallerySorting.FILE_DATE;
		}
		_actionSortFileByDate.setChecked(_gallerySorting == GallerySorting.FILE_DATE);
		_actionSortByFileName.setChecked(_gallerySorting == GallerySorting.FILE_NAME);

		super.restoreState();

		// !!! overwrite super settings !!!
		setSorting(_gallerySorting);
		setFilter(_currentImageFilter);

		enableActions();

	}

	@Override
	public void saveState() {

		/*
		 * gallery sorting
		 */
		_state.put(STATE_GALLERY_SORTING, _actionSortFileByDate.isChecked()
				? GallerySorting.FILE_DATE.name()
				: GallerySorting.FILE_NAME.name());

		_state.put(STATE_IS_SHOW_PHOTO_NAME_IN_GALLERY, _actionShowPhotoName.isChecked());
		_state.put(STATE_IS_SHOW_PHOTO_TOOLTIP, _actionShowPhotoTooltip.isChecked());
		_state.put(STATE_IS_SHOW_PHOTO_GPS_ANNOTATION, _actionShowGPSAnnotation.isChecked());

		_state.put(STATE_PHOTO_INFO_DATE, _photoDateInfo.name());
		_state.put(STATE_IMAGE_FILTER, _currentImageFilter.name());

		super.saveState();
	}
}
