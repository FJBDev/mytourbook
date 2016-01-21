/*******************************************************************************
 * Copyright (C) 2005, 2013  Wolfgang Schramm and Contributors
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
package net.tourbook.photo.internal;

import java.io.File;
import java.io.FileFilter;
import java.util.concurrent.atomic.AtomicBoolean;

import net.tourbook.photo.PicDirView;

import org.eclipse.jface.viewers.TreeViewer;

public class TVIFolderFolder extends TVIFolder {

	File						_treeItemFolder;

	String						_folderName;
	private String				_folderPath;

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

	private boolean				_isFolderLoaded;

	private final FileFilter	_folderFilter;

	AtomicBoolean				_isInWaitingQueue	= new AtomicBoolean();

	FolderLoader				_folderLoader;

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
		_folderPath = folder.getAbsolutePath();
	}

	@Override
	public void clearChildren() {

		_folderCounter = 0;
		_fileCounter = 0;

		_isFolderLoaded = false;
		_folderChildren = null;

		super.clearChildren();
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof TVIFolderFolder)) {
			return false;
		}
		final TVIFolderFolder other = (TVIFolderFolder) obj;
		if (_folderPath == null) {
			if (other._folderPath != null) {
				return false;
			}
		} else if (!_folderPath.equals(other._folderPath)) {
			return false;
		}
		return true;
	}

	@Override
	protected void fetchChildren() {

		if (_isFolderLoaded == false) {
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

		if (_isFolderLoaded == false) {
			readFolderList();
		}

		return _fileCounter;
	}

	public int getFolderCounter() {

		if (_isFolderLoaded == false) {
			readFolderList();
		}

		return _folderCounter;
	}

	@Override
	public boolean hasChildren() {

		if (_isFolderLoaded == false) {
			readFolderList();
		}

		return _folderChildren == null ? false : _folderChildren.length > 0;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((_folderPath == null) ? 0 : _folderPath.hashCode());
		return result;
	}

	boolean isFolderLoaded() {
		return _isFolderLoaded;
	}

	private void readFolderList() {

		_folderChildren = _treeItemFolder.listFiles(_folderFilter);

		_isFolderLoaded = true;
	}

	@Override
	public String toString() {
		return "TVIFolderFolder: " + _treeItemFolder; //$NON-NLS-1$
	}

}
