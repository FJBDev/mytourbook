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
import java.io.FileFilter;

import org.eclipse.jface.viewers.TreeViewer;

public class TVIFolderFolder extends TVIFolder {

	File						_treeItemFolder;
	String						_folderName;

	boolean						_isRootFolder;

	private File[]				_folderChildren;

	/**
	 * Number of folder in this folder
	 */
	private int					_folderCounter;

	/**
	 * Number of files in this folder
	 */
	private int					_fileCounter;

	private boolean				_isFolderChecked;

//	private static int			_allCounter;

	private final FileFilter	_folderFilter;
	{
		_folderFilter = new FileFilter() {
			@Override
			public boolean accept(final File pathname) {

				// get only visible folder
				if (pathname.isDirectory() && pathname.isHidden() == false) {

					// count folder
					_folderCounter++;

					return true;

				} else {
					// count files
					_fileCounter++;
				}

//				_allCounter++;
//
//				// update UI that files are retrieved from the filesystem
//				if (_allCounter % 500 == 0) {
//
////					System.out.println(NLS.bind("Retrieving... {0}  Folder: {1}  Files: {2}", //
////							new Object[] { _folderName, _folderCounter, _fileCounter }));
////					// TODO remove SYSTEM.OUT.PRINTLN
//
////					_picDirFolder.updateUI_RetrievedFileFolder(_folderName, _folderCounter, _fileCounter);
//				}

				return false;
			}
		};
	}

	public TVIFolderFolder(	final PicDirFolder picDirFolder,
							final TreeViewer folderViewer,
							final File folder,
							final boolean isRootFolder) {

		super(picDirFolder, folderViewer);

		_treeItemFolder = folder;

		_isRootFolder = isRootFolder;
		_folderName = _isRootFolder ? folder.getPath() : folder.getName();
	}

	@Override
	public void clearChildren() {

		_folderCounter = 0;
		_fileCounter = 0;

		_isFolderChecked = false;
		_folderChildren = null;

		super.clearChildren();
	}

	@Override
	protected void fetchChildren() {

		if (_isFolderChecked == false) {
			// read folder files
			readFolderList();
		}

		if (_folderChildren != null) {

			// create tvi children

			PicDirView.sortFiles(_folderChildren);

			for (final File childFolder : _folderChildren) {
				addChild(new TVIFolderFolder(_picDirFolder, _folderViewer, childFolder, false));
			}
		}
	}

	int getFileCounter() {

		if (_isFolderChecked == false) {
			readFolderList();
		}

		return _fileCounter;
	}

	public int getFolderCounter() {

		if (_isFolderChecked == false) {
			readFolderList();
		}

		return _folderCounter;
	}

	@Override
	public boolean hasChildren() {

		if (_isFolderChecked == false) {
			readFolderList();
		}

		return _folderChildren == null ? false : _folderChildren.length > 0;
	}

	private void readFolderList() {

		_folderChildren = _treeItemFolder.listFiles(_folderFilter);

		_isFolderChecked = true;
	}

	@Override
	protected void remove() {}

	@Override
	public String toString() {
		return "TVIFolderFolder: " + _treeItemFolder; //$NON-NLS-1$
	}

}
