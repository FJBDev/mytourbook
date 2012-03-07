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
import java.io.IOException;
import java.util.ArrayList;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.ui.TreeViewerItem;
import net.tourbook.ui.UI;
import net.tourbook.util.StatusUtil;
import net.tourbook.util.Util;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.TreeColumnLayout;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IElementComparer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.dialogs.PreferencesUtil;

/**
 * This folder viewer is from org.eclipse.swt.examples.fileviewer but with many modifications.
 */
class PicDirFolder {

	static String								WIN_PROGRAMFILES						= System.getenv("programfiles");			//$NON-NLS-1$
	static String								FILE_SEPARATOR							= System
																								.getProperty("file.separator");	//$NON-NLS-1$

	private static final String					STATE_SELECTED_FOLDER					= "STATE_SELECTED_FOLDER";					//$NON-NLS-1$
	private static final String					STATE_IS_SINGLE_CLICK_EXPAND			= "STATE_IS_SINGLE_CLICK_EXPAND";			//$NON-NLS-1$
	private static final String					STATE_IS_SINGLE_EXPAND_COLLAPSE_OTHERS	= "STATE_IS_SINGLE_EXPAND_COLLAPSE_OTHERS"; //$NON-NLS-1$

	private final IPreferenceStore				_prefStore								= TourbookPlugin.getDefault() //
																								.getPreferenceStore();

//	private PicDirView							_picDirView;
	private PicDirImages						_picDirImages;

	private long								_expandRunnableCounter;
	private boolean								_isExpandingSelection;

	private boolean								_isBehaviourSingleClickExpand;
	private boolean								_isBehaviourSingleExpandCollapseOthers;
	private boolean								_isStateShowFileFolderInFolderItem;

	/**
	 * Is true when the mouse click is for the context menu
	 */
	private boolean								_isMouseContextMenu;
	private boolean								_doAutoCollapseExpand;
	private boolean								_isNavigation;

	private TVIFolderRoot						_rootItem;
	private TVIFolderFolder						_selectedTVIFolder;
	private File								_selectedFolder;

	private ActionRefreshFolder					_actionRefreshFolder;
	private ActionRunPhotoViewer				_actionRunPhotoViewer;
	private ActionPreferences					_actionPreferences;
	private ActionSingleClickExpand				_actionSingleClickExpand;
	private ActionSingleExpandCollapseOthers	_actionSingleExpandCollapseOthers;

	/*
	 * UI controls
	 */
	private Display								_display;
	private TreeViewer							_folderViewer;

	private static final class FolderComparer implements IElementComparer {

		@Override
		public boolean equals(final Object a, final Object b) {

			if (a == b) {
				return true;
			}

			if (a instanceof TVIFolderFolder && b instanceof TVIFolderFolder) {

				final TVIFolderFolder item1 = (TVIFolderFolder) a;
				final TVIFolderFolder item2 = (TVIFolderFolder) b;

				final String folder1Name = item1._treeItemFolder.getName();
				final String folder2Name = item2._treeItemFolder.getName();

				return folder1Name.equals(folder2Name);
			}
			return false;
		}

		@Override
		public int hashCode(final Object element) {
			return element.hashCode();
		}
	}

	private class FolderContentProvicer implements ITreeContentProvider {

		public void dispose() {}

		public Object[] getChildren(final Object parentElement) {

			/*
			 * force to get children so that the user can see if a folder can be expanded or not
			 */

			return ((TreeViewerItem) parentElement).getFetchedChildrenAsArray();
		}

		public Object[] getElements(final Object inputElement) {
			return _rootItem.getFetchedChildrenAsArray();
		}

		public Object getParent(final Object element) {
			return ((TreeViewerItem) element).getParentItem();
		}

		public boolean hasChildren(final Object element) {
			return ((TreeViewerItem) element).hasChildren();
		}

		public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {}
	}

	PicDirFolder(final PicDirView picDirView, final PicDirImages picDirImages) {
//		_picDirView = picDirView;
		_picDirImages = picDirImages;
	}

	void actionRefreshFolder() {

		if (_selectedTVIFolder == null) {
			return;
		}

		BusyIndicator.showWhile(_display, new Runnable() {
			public void run() {

				final boolean isExpanded = _folderViewer.getExpandedState(_selectedTVIFolder);

				_folderViewer.collapseToLevel(_selectedTVIFolder, 1);

				final Tree tree = _folderViewer.getTree();
				tree.setRedraw(false);
				{
					final TreeItem topItem = tree.getTopItem();

					// remove children from viewer
					final ArrayList<TreeViewerItem> unfetchedChildren = _selectedTVIFolder.getUnfetchedChildren();
					if (unfetchedChildren != null) {
						_folderViewer.remove(unfetchedChildren.toArray());
					}

					// remove children from model
					_selectedTVIFolder.clearChildren();

					// update viewer
					_folderViewer.refresh(_selectedTVIFolder);

					// expand selected folder
					if (isExpanded) {
						_folderViewer.setExpandedState(_selectedTVIFolder, true);
					}

					// position tree items to the old position
					tree.setTopItem(topItem);
				}
				tree.setRedraw(true);
			}
		});
	}

	void actionRunExternalPhotoViewer() {

		if (_selectedTVIFolder == null) {
			return;
		}

		final String prefPhotoViewer = _prefStore.getString(ITourbookPreferences.PHOTO_EXTERNAL_PHOTO_VIEWER);

		if (prefPhotoViewer.length() == 0) {
			MessageDialog.openInformation(
					Display.getCurrent().getActiveShell(),
					Messages.Pic_Dir_Dialog_ExternalPhotoViewer_Title,
					Messages.Pic_Dir_Dialog_ExternalPhotoViewer_Message);

			PreferencesUtil.createPreferenceDialogOn(
					Display.getCurrent().getActiveShell(),
					PrefPagePhotoViewer.ID,
					null,
					null).open();
			return;
		}

		if (UI.IS_WIN) {

			final String folder = _selectedTVIFolder._treeItemFolder.getAbsolutePath();

			final String[] commands = { "cmd.exe", //$NON-NLS-1$
					"/c", //$NON-NLS-1$
					"\"" + prefPhotoViewer + "\"", //$NON-NLS-1$ //$NON-NLS-2$
					folder
//					"\"" + folder + "\""
			//
			};
			try {

//			"\"C:\\Program Files (x86)\\FastStone Image Viewer\\FSViewer.exe\"",
//			"\"C:\\Program Files\\Q-Dir\\Q-Dir.exe\"";
//				System.out.println("\t");
//				for (final String cmd : commands) {
//					System.out.println(cmd);
//				}

				Runtime.getRuntime().exec(commands);

			} catch (final IOException e) {
				StatusUtil.showStatus(e);
			}
		}

	}

	void actionSingleClickExpand() {
		_isBehaviourSingleClickExpand = _actionSingleClickExpand.isChecked();
	}

	void actionSingleExpandCollapseOthers() {
		_isBehaviourSingleExpandCollapseOthers = _actionSingleExpandCollapseOthers.isChecked();
	}

	private void createActions() {

		_actionPreferences = new ActionPreferences();
		_actionRefreshFolder = new ActionRefreshFolder(this);
		_actionRunPhotoViewer = new ActionRunPhotoViewer(this);
		_actionSingleClickExpand = new ActionSingleClickExpand(this);
		_actionSingleExpandCollapseOthers = new ActionSingleExpandCollapseOthers(this);
	}

	/**
	 * create the views context menu
	 */
	private void createContextMenu() {

		final MenuManager menuMgr = new MenuManager();
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			@Override
			public void menuAboutToShow(final IMenuManager manager) {
				fillContextMenu(manager);
			}
		});

		final Tree tree = _folderViewer.getTree();
		final Menu contextMenu = menuMgr.createContextMenu(tree);

		tree.setMenu(contextMenu);
	}

	void createUI(final Composite parent) {

		createUI_0(parent);

		createActions();
		createContextMenu();

		// update UI from pref store
		_folderViewer.getTree().setLinesVisible(_prefStore.getBoolean(ITourbookPreferences.VIEW_LAYOUT_DISPLAY_LINES));
	}

	private void createUI_0(final Composite parent) {

		_display = parent.getDisplay();

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(1).spacing(0, 0).applyTo(container);
		{
			createUI_10_TreeView(container);
		}
	}

	private void createUI_10_TreeView(final Composite parent) {

		/*
		 * create tree layout
		 */

		final Composite layoutContainer = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults()//
				.grab(true, true)
				.hint(200, 100)
				.applyTo(layoutContainer);

		final TreeColumnLayout treeLayout = new TreeColumnLayout();
		layoutContainer.setLayout(treeLayout);

		/*
		 * create viewer
		 */
		final Tree tree = new Tree(layoutContainer, SWT.V_SCROLL | SWT.SINGLE | SWT.FULL_SELECTION);

		tree.setHeaderVisible(false);

		tree.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDown(final MouseEvent e) {
				_doAutoCollapseExpand = true;
				_isMouseContextMenu = e.button == 3;
			}
		});

		_folderViewer = new TreeViewer(tree);

		_folderViewer.setContentProvider(new FolderContentProvicer());
		_folderViewer.setComparer(new FolderComparer());
		_folderViewer.setUseHashlookup(true);

		_folderViewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(final DoubleClickEvent event) {

				// expand/collapse current item
				final Object selection = ((IStructuredSelection) _folderViewer.getSelection()).getFirstElement();

				final TreeViewerItem treeItem = (TreeViewerItem) selection;

				expandCollapseFolder(treeItem);
			}
		});

		_folderViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(final SelectionChangedEvent event) {
				onSelectFolder((ITreeSelection) event.getSelection());
			}
		});

		/*
		 * create columns
		 */
		TreeViewerColumn tvc;
		TreeColumn tvcColumn;

		// column: os folder
		tvc = new TreeViewerColumn(_folderViewer, SWT.TRAIL);
		tvcColumn = tvc.getColumn();
		tvc.setLabelProvider(new StyledCellLabelProvider() {

			@Override
			public void update(final ViewerCell cell) {

				final Object element = cell.getElement();

				if (element instanceof TVIFolderFolder) {
					final TVIFolderFolder folderItem = (TVIFolderFolder) element;

					final StyledString styledString = new StyledString();

					styledString.append(folderItem._folderName);

					if (_isStateShowFileFolderInFolderItem) {

						// force that file list is loaded and number of files is available
						folderItem.hasChildren();

						final int folderCounter = folderItem.getFolderCounter();
						if (folderCounter > 0) {
							styledString.append(UI.SPACE2);
							styledString.append(Integer.toString(folderCounter), UI.PHOTO_FOLDER_STYLER);
						}

						final int fileCounter = folderItem.getFileCounter();
						if (fileCounter > 0) {
							styledString.append(UI.SPACE2);
							styledString.append(Integer.toString(fileCounter), UI.PHOTO_FILE_STYLER);
						}
					}

					cell.setText(styledString.getString());
					cell.setStyleRanges(styledString.getStyleRanges());
				}
			}
		});
		treeLayout.setColumnData(tvcColumn, new ColumnWeightData(100, true));
	}

	private void displayFolderImages(final TVIFolderFolder tviFolder, final boolean isNavigation) {

		final File selectedFolder = tviFolder._treeItemFolder;

		// optimize, don't select the same folder again
		if (_selectedFolder != null && selectedFolder.equals(_selectedFolder)) {
			return;
		}

		_selectedFolder = selectedFolder;
		_selectedTVIFolder = tviFolder;

		// display imaged for the selected folder
		_picDirImages.showImages(selectedFolder, isNavigation);
	}

	private void expandCollapseFolder(final TreeViewerItem treeItem) {

		if (_folderViewer.getExpandedState(treeItem)) {
			_folderViewer.collapseToLevel(treeItem, 1);
		} else {

			if (treeItem.hasChildren()) {
				_folderViewer.expandToLevel(treeItem, 1);
			}
		}
	}

	private void fillContextMenu(final IMenuManager menuMgr) {

		menuMgr.add(_actionRunPhotoViewer);
		menuMgr.add(_actionRefreshFolder);

		menuMgr.add(new Separator());
		menuMgr.add(_actionSingleClickExpand);
		menuMgr.add(_actionSingleExpandCollapseOthers);

		menuMgr.add(new Separator());
		menuMgr.add(_actionPreferences);
	}

	/**
	 * Gets filesystem root entries
	 * 
	 * @return an array of Files corresponding to the root directories on the platform, may be empty
	 *         but not null
	 */
	private File[] getRootsSorted() {

		final File[] roots = File.listRoots();

		PicDirView.sortFiles(roots);

		return roots;

		/*
		 * On JDK 1.22 only...
		 */
		// return File.listRoots();

		/*
		 * On JDK 1.1.7 and beyond... -- PORTABILITY ISSUES HERE --
		 */
//		if (System.getProperty("os.name").indexOf("Windows") != -1) {
//
//			final ArrayList<File> list = new ArrayList<File>();
//
//			for (char i = 'c'; i <= 'z'; ++i) {
//
//				final File drive = new File(i + ":" + File.separator);
//
//				if (drive.isDirectory() && drive.exists()) {
//
//					list.add(drive);
//
//					if (initial && i == 'c') {
//						_selectedFolder = drive;
//						initial = false;
//					}
//				}
//			}
//
//			final File[] roots = list.toArray(new File[list.size()]);
//
//			PicDirView.sortFiles(roots);
//
//			return roots;
//
//		} else {
//
//			final File root = new File(File.separator);
//			if (initial) {
//				_selectedFolder = root;
//				initial = false;
//			}
//			return new File[] { root };
//		}
	}

	Tree getTree() {
		return _folderViewer.getTree();
	}

	void handlePrefStoreModifications(final PropertyChangeEvent event) {

		final String property = event.getProperty();
		boolean isViewerRefresh = false;

		if (property.equals(ITourbookPreferences.VIEW_LAYOUT_CHANGED)) {

			_folderViewer.getTree().setLinesVisible(
					_prefStore.getBoolean(ITourbookPreferences.VIEW_LAYOUT_DISPLAY_LINES));

			isViewerRefresh = true;

		} else if (property.equals(ITourbookPreferences.PHOTO_VIEWER_PREF_STORE_EVENT)) {

			updateColors();

			isViewerRefresh = true;
		}

		if (isViewerRefresh) {

			_folderViewer.refresh();

			/*
			 * the tree must be redrawn because the styled text does not show with the new color
			 */
			_folderViewer.getTree().redraw();
		}
	}

	/**
	 * Do the actions when a folder is selected
	 * 
	 * @param iSelection
	 */
	private void onSelectFolder(final ITreeSelection treeSelection) {

		if (_isExpandingSelection) {
			// prevent entless loops
			return;
		}

		// keep & reset mouse event
		final boolean doAutoCollapseExpand = _doAutoCollapseExpand;
		_doAutoCollapseExpand = false;

		final TreePath[] selectedTreePaths = treeSelection.getPaths();
		if (selectedTreePaths.length == 0) {
			return;
		}
		final TreePath selectedTreePath = selectedTreePaths[0];
		if (selectedTreePath == null) {
			return;
		}

		final TVIFolderFolder tviFolder = (TVIFolderFolder) selectedTreePath.getLastSegment();

		if (_isMouseContextMenu) {

			// context menu has been opened, do no expand/collapse

			displayFolderImages(tviFolder, _isNavigation);

		} else {

			if (doAutoCollapseExpand) {
				onSelectFolder_10_AutoExpandCollapse(treeSelection, selectedTreePath, tviFolder);
			} else {
				displayFolderImages(tviFolder, _isNavigation);
			}
		}

		// reset navigation state, this is a bit of a complex behaviour
		_isNavigation = false;
	}

	/**
	 * This is not yet working thoroughly because the expanded position moves up or down and all
	 * expanded childrens are not visible (but they could) like when the triangle (+/-) icon in the
	 * tree is clicked.
	 * 
	 * @param treeSelection
	 * @param selectedTreePath
	 * @param tviFolder
	 */
	private void onSelectFolder_10_AutoExpandCollapse(	final ITreeSelection treeSelection,
														final TreePath selectedTreePath,
														final TVIFolderFolder tviFolder) {

		if (_isBehaviourSingleExpandCollapseOthers) {

			/*
			 * run async because this is doing a reselection which cannot be done within the current
			 * selection event
			 */
			Display.getCurrent().asyncExec(new Runnable() {

				private long			__expandRunnableCounter	= ++_expandRunnableCounter;

				private TVIFolderFolder	__selectedFolderItem	= tviFolder;
				private ITreeSelection	__treeSelection			= treeSelection;
				private TreePath		__selectedTreePath		= selectedTreePath;
				private boolean			__isNavigation			= _isNavigation;

				public void run() {

					// check if a newer expand event occured
					if (__expandRunnableCounter != _expandRunnableCounter) {
						return;
					}

					onSelectFolder_10_AutoExpandCollapse_Runnable(
							__selectedFolderItem,
							__treeSelection,
							__selectedTreePath,
							__isNavigation);
				}
			});

		} else {

			if (_isBehaviourSingleClickExpand) {

				// expand folder with one mouse click but not with the keyboard
				expandCollapseFolder(tviFolder);
			}

			displayFolderImages(tviFolder, _isNavigation);
		}
	}

	/**
	 * This behavior is complex and still have possible problems.
	 * 
	 * @param selectedFolderItem
	 * @param treeSelection
	 * @param selectedTreePath
	 * @param isNavigation
	 */
	private void onSelectFolder_10_AutoExpandCollapse_Runnable(	final TVIFolderFolder selectedFolderItem,
																final ITreeSelection treeSelection,
																final TreePath selectedTreePath,
																final boolean isNavigation) {
		_isExpandingSelection = true;
		{
			final Tree tree = _folderViewer.getTree();

			tree.setRedraw(false);
			{
				final TreeItem topItem = tree.getTopItem();

				final boolean isExpanded = _folderViewer.getExpandedState(selectedTreePath);

				/*
				 * collapse all tree paths
				 */
				final TreePath[] allExpandedTreePaths = _folderViewer.getExpandedTreePaths();
				for (final TreePath treePath : allExpandedTreePaths) {
					_folderViewer.setExpandedState(treePath, false);
				}

				/*
				 * expand and select selected folder
				 */
				_folderViewer.setExpandedTreePaths(new TreePath[] { selectedTreePath });
				_folderViewer.setSelection(treeSelection, true);

				if (_isBehaviourSingleClickExpand && isExpanded) {

					// auto collapse expanded folder
					_folderViewer.setExpandedState(selectedTreePath, false);
				}

				/**
				 * set top item to the previous top item, otherwise the expanded/collapse item is
				 * positioned at the bottom and the UI is jumping all the time
				 * <p>
				 * win behaviour: when an item is set to top which was collapsed bevore, it will be
				 * expanded
				 */
				if (topItem.isDisposed() == false) {
					tree.setTopItem(topItem);
				}
			}
			tree.setRedraw(true);
		}
		_isExpandingSelection = false;

		displayFolderImages(selectedFolderItem, isNavigation);
	}

	private void restoreFolder(final String restoreFolderName) {

		BusyIndicator.showWhile(_display, new Runnable() {
			public void run() {

				// set root item
				_rootItem = new TVIFolderRoot(_folderViewer, getRootsSorted());

				_folderViewer.setInput(new Object());

				_selectedFolder = null;
				_selectedTVIFolder = null;

				selectFolder(restoreFolderName, true, false);
			}
		});
	}

	void restoreState(final IDialogSettings state) {

		_isBehaviourSingleClickExpand = Util.getStateBoolean(state, STATE_IS_SINGLE_CLICK_EXPAND, false);
		_actionSingleClickExpand.setChecked(_isBehaviourSingleClickExpand);

		_isBehaviourSingleExpandCollapseOthers = Util.getStateBoolean(
				state,
				STATE_IS_SINGLE_EXPAND_COLLAPSE_OTHERS,
				false);
		_actionSingleExpandCollapseOthers.setChecked(_isBehaviourSingleExpandCollapseOthers);

		updateColors();

		final String previousSelectedFolder = Util.getStateString(state, STATE_SELECTED_FOLDER, null);
		restoreFolder(previousSelectedFolder);
	}

	void saveState(final IDialogSettings state) {

		// selected folder
		if (_selectedFolder != null) {
			state.put(STATE_SELECTED_FOLDER, _selectedFolder.getAbsolutePath());
		}

		state.put(STATE_IS_SINGLE_CLICK_EXPAND, _actionSingleClickExpand.isChecked());
		state.put(STATE_IS_SINGLE_EXPAND_COLLAPSE_OTHERS, _actionSingleExpandCollapseOthers.isChecked());
	}

	/**
	 * @param requestedFolderName
	 * @param isMoveUpHierarchyWhenFolderIsInvalid
	 * @param isNavigation
	 *            Set <code>true</code> when the folder was selected from the navigations history
	 *            which prevents that the naviation history is updated.
	 * @return Return <code>false</code> when the folder which should be selected is not available
	 */
	boolean selectFolder(	final String requestedFolderName,
							final boolean isMoveUpHierarchyWhenFolderIsInvalid,
							final boolean isNavigation) {

		_isNavigation = isNavigation;

		boolean isRequestedFolderAvailable = false;
		File selectedFolder = null;

		if (requestedFolderName != null) {

			final File folderFile = new File(requestedFolderName);
			if (folderFile.isDirectory()) {
				selectedFolder = folderFile;
				isRequestedFolderAvailable = true;
			}

			if (selectedFolder == null && isMoveUpHierarchyWhenFolderIsInvalid) {

				// previously selected folder is not available, try to move up the hierarchy

				IPath folderPath = new Path(requestedFolderName);

				final int segmentCount = folderPath.segmentCount();
				for (int segmentIndex = segmentCount; segmentIndex > 0; segmentIndex--) {

					folderPath = folderPath.removeLastSegments(1);

					final File folderPathFile = new File(folderPath.toOSString());
					if (folderPathFile.isDirectory()) {
						selectedFolder = folderPathFile;
						break;
					}
				}
			}
		}

		if (requestedFolderName != null && isRequestedFolderAvailable == false) {

			// restored folder is not available

			MessageDialog.openInformation(
					_display.getActiveShell(),
					Messages.Pic_Dir_Dialog_FolderIsNotAvailable_Title,
					NLS.bind(Messages.Pic_Dir_Dialog_FolderIsNotAvailable_Message, requestedFolderName));
		}

		if (selectedFolder == null) {

			// previously selected folder is not available
			return false;
		}

		final String restorePathName = selectedFolder.getAbsolutePath();

		final IPath restorePath = new Path(restorePathName);
		final IPath restoreRoot = new Path(restorePathName).removeFirstSegments(9999);

		final String[] folderSegments = restorePath.segments();
		final ArrayList<String> allFolderSegments = new ArrayList<String>();

		allFolderSegments.add(restoreRoot.toOSString());
		for (final String folderSegmentName : folderSegments) {
			allFolderSegments.add(folderSegmentName);
		}

		final ArrayList<TVIFolder> treePathItems = new ArrayList<TVIFolder>();
		TVIFolder folderSegmentItem = _rootItem;
		treePathItems.add(folderSegmentItem);

		// create tree path for each folder segment
		for (final String folderSegmentName : allFolderSegments) {

			boolean isPathSegmentAvailable = false;

			final ArrayList<TreeViewerItem> tviChildren = folderSegmentItem.getFetchedChildren();
			for (final TreeViewerItem tviChild : tviChildren) {

				final TVIFolderFolder childFolder = (TVIFolderFolder) tviChild;
				String childFolderName;

				if (childFolder._isRootFolder) {

					if (UI.IS_WIN) {
						// remove \ from device name
						childFolderName = childFolder._folderName.substring(0, 2);
					} else {
						childFolderName = childFolder._folderName;
					}

				} else {

					childFolderName = childFolder._folderName;
				}

				if (folderSegmentName.equals(childFolderName)) {

					isPathSegmentAvailable = true;

					treePathItems.add(childFolder);
					folderSegmentItem = childFolder;

					break;
				}
			}

			if (isPathSegmentAvailable == false) {
				// requested path is not available, select partial path in the viewer
				break;
			}
		}

		if (treePathItems.size() == 0) {
			// there is nothing which can be selected
			return false;
		}

		final TVIFolder[] treePathArray = treePathItems.toArray(new TVIFolder[treePathItems.size()]);
		final TreePath treePath = new TreePath(treePathArray);
		final ITreeSelection treeSelection = new TreeSelection(treePath);

		_doAutoCollapseExpand = true;

		// select folder in the tree viewer, this triggers onSelectFolder(...)
		_folderViewer.setSelection(treeSelection, true);

		return true;
	}

	private void updateColors() {

		_isStateShowFileFolderInFolderItem = _prefStore.getBoolean(//
				ITourbookPreferences.PHOTO_VIEWER_IS_SHOW_FILE_FOLDER);

		final ColorRegistry colorRegistry = JFaceResources.getColorRegistry();
		final Color fgColor = colorRegistry.get(ITourbookPreferences.PHOTO_VIEWER_COLOR_FOREGROUND);
		final Color bgColor = colorRegistry.get(ITourbookPreferences.PHOTO_VIEWER_COLOR_BACKGROUND);

		final Tree tree = _folderViewer.getTree();
		tree.setForeground(fgColor);
		tree.setBackground(bgColor);

		_picDirImages.updateColors(fgColor, bgColor);
	}

}
