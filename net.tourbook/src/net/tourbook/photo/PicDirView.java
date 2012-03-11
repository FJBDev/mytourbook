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

import java.io.File;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.photo.manager.ThumbnailStore;
import net.tourbook.ui.ViewerDetailForm;
import net.tourbook.util.Util;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Sash;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.part.ViewPart;

public class PicDirView extends ViewPart {

	static public final String				ID					= "net.tourbook.photo.PicDirView";							//$NON-NLS-1$

	private static final String				STATE_TREE_WIDTH	= "STATE_TREE_WIDTH";										//$NON-NLS-1$

	private static final IDialogSettings	_state				= TourbookPlugin.getDefault()//
																		.getDialogSettingsSection("PhotoDirectoryView");	//$NON-NLS-1$
	private static final IPreferenceStore	_prefStore			= TourbookPlugin.getDefault()//
																		.getPreferenceStore();

	private IPartListener2					_partListener;
	private IPropertyChangeListener			_prefChangeListener;

	private PicDirFolder					_picDirFolder;
	private PicDirImages					_picDirImages;

	/*
	 * UI controls
	 */
	private ViewerDetailForm				_containerMasterDetail;
	private Composite						_containerFolder;
	private Composite						_containerImages;

	static int compareFiles(final File file1, final File file2) {

//		boolean aIsDir = a.isDirectory();
//		boolean bIsDir = b.isDirectory();
//		if (aIsDir && ! bIsDir) return -1;
//		if (bIsDir && ! aIsDir) return 1;

		// sort case-sensitive files in a case-insensitive manner
		final String file1Name = file1.getName();
		final String file2Name = file2.getName();

		// try to sort by numbers
		try {

			final int file1No = Integer.parseInt(file1Name);
			final int file2No = Integer.parseInt(file2Name);

			return file1No - file2No;

		} catch (final Exception e) {
			// at least one filename co not contain a number, sort by string
		}

		int compare = file1Name.compareToIgnoreCase(file2Name);

		if (compare == 0) {
			compare = file1Name.compareTo(file2Name);
		}
		return compare;
	}

	/**
	 * Gets a directory listing
	 * 
	 * @param file
	 *            the directory to be listed
	 * @return an array of files this directory contains, may be empty but not null
	 */
	static File[] getDirectoryList(final File file) {
		final File[] list = file.listFiles();
		if (list == null) {
			return new File[0];
		}
		sortFiles(list);
		return list;
	}

	private static void sortBlock(final File[] files, final int start, final int end, final File[] mergeTemp) {
		final int length = end - start + 1;
		if (length < 8) {
			for (int i = end; i > start; --i) {
				for (int j = end; j > start; --j) {
					if (compareFiles(files[j - 1], files[j]) > 0) {
						final File temp = files[j];
						files[j] = files[j - 1];
						files[j - 1] = temp;
					}
				}
			}
			return;
		}
		final int mid = (start + end) / 2;
		sortBlock(files, start, mid, mergeTemp);
		sortBlock(files, mid + 1, end, mergeTemp);
		int x = start;
		int y = mid + 1;
		for (int i = 0; i < length; ++i) {
			if ((x > mid) || ((y <= end) && compareFiles(files[x], files[y]) > 0)) {
				mergeTemp[i] = files[y++];
			} else {
				mergeTemp[i] = files[x++];
			}
		}
		for (int i = 0; i < length; ++i) {
			files[i + start] = mergeTemp[i];
		}
	}

	/**
	 * Sorts files lexicographically by name.
	 * 
	 * @param files
	 *            the array of Files to be sorted
	 */
	static void sortFiles(final File[] files) {

		/* Very lazy merge sort algorithm */
		sortBlock(files, 0, files.length - 1, new File[files.length]);
	}

	private void addPartListener() {

		_partListener = new IPartListener2() {
			@Override
			public void partActivated(final IWorkbenchPartReference partRef) {}

			@Override
			public void partBroughtToTop(final IWorkbenchPartReference partRef) {}

			@Override
			public void partClosed(final IWorkbenchPartReference partRef) {
				if (partRef.getPart(false) == PicDirView.this) {
					ThumbnailStore.cleanupStoreFiles(false, false);
					saveState();
				}
			}

			@Override
			public void partDeactivated(final IWorkbenchPartReference partRef) {}

			@Override
			public void partHidden(final IWorkbenchPartReference partRef) {
//				if (partRef.getPart(false) == PicDirView.this) {
//					_picDirImages.onPartIsHidden();
//				}
			}

			@Override
			public void partInputChanged(final IWorkbenchPartReference partRef) {}

			@Override
			public void partOpened(final IWorkbenchPartReference partRef) {}

			@Override
			public void partVisible(final IWorkbenchPartReference partRef) {
//				if (partRef.getPart(false) == PicDirView.this) {
//					_picDirImages.onPartIsVisible();
//				}
			}
		};
		getViewSite().getPage().addPartListener(_partListener);
	}

	private void addPrefListener() {

		_prefChangeListener = new IPropertyChangeListener() {
			@Override
			public void propertyChange(final PropertyChangeEvent event) {

				_picDirFolder.handlePrefStoreModifications(event);
				_picDirImages.handlePrefStoreModifications(event);
			}
		};

		_prefStore.addPropertyChangeListener(_prefChangeListener);
	}

	@Override
	public void createPartControl(final Composite parent) {

		createUI(parent);

		addPartListener();
		addPrefListener();

		restoreState();
	}

	private void createUI(final Composite parent) {

		_picDirImages = new PicDirImages(this);
		_picDirFolder = new PicDirFolder(this, _picDirImages);

		final Composite masterDetailContainer = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(masterDetailContainer);
		GridLayoutFactory.fillDefaults().applyTo(masterDetailContainer);
		{
			// file folder
			_containerFolder = new Composite(masterDetailContainer, SWT.NONE);
			GridDataFactory.fillDefaults().applyTo(_containerFolder);
			GridLayoutFactory.fillDefaults().spacing(0, 0).applyTo(_containerFolder);
			{
				_picDirFolder.createUI(_containerFolder);
			}

			// sash
			final Sash sash = new Sash(masterDetailContainer, SWT.VERTICAL);

			// photos
			_containerImages = new Composite(masterDetailContainer, SWT.NONE);
			GridDataFactory.fillDefaults().applyTo(_containerImages);
			GridLayoutFactory.fillDefaults().applyTo(_containerImages);
//			_containerImages.setLayout(new FillLayout());
			{
				_picDirImages.createUI(_picDirFolder, _containerImages);
			}

			// master/detail form
			_containerMasterDetail = new ViewerDetailForm(
					masterDetailContainer,
					_containerFolder,
					sash,
					_containerImages);
		}
	}

	@Override
	public void dispose() {

		_picDirImages.dispose();

		getViewSite().getPage().removePartListener(_partListener);

		_prefStore.removePropertyChangeListener(_prefChangeListener);

		super.dispose();
	}

	private void restoreState() {

		_containerMasterDetail.setViewerWidth(Util.getStateInt(_state, STATE_TREE_WIDTH, 200));

		/*
		 * image restore must be done be for folder restore because folder restore is also loading
		 * the folder and updates folder history
		 */
		_picDirImages.restoreState(_state);
		_picDirFolder.restoreState(_state);
	}

	private void saveState() {

		if (_containerFolder.isDisposed()) {
			// this happened
			return;
		}

		// keep width of the dir folder view in the master detail container
		final Tree tree = _picDirFolder.getTree();
		if (tree != null) {
			_state.put(STATE_TREE_WIDTH, tree.getSize().x);
		}

		_picDirFolder.saveState(_state);
		_picDirImages.saveState(_state);
	}

	@Override
	public void setFocus() {
		_picDirFolder.getTree().setFocus();
	}

}
