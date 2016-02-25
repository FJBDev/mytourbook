/*******************************************************************************
 * Copyright (C) 2005, 2016 Wolfgang Schramm and Contributors
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
package net.tourbook.common.util;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import net.tourbook.common.Messages;
import net.tourbook.common.UI;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.AbstractColumnLayout;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnLayoutData;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Decorations;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.XMLMemento;

/**
 * Manages the columns for a tree/table-viewer
 * <p>
 * created: 2007-05-27 by Wolfgang Schramm
 */
public class ColumnManager {

	private static final String					XML_STATE_COLUMN_MANAGER			= "XML_STATE_COLUMN_MANAGER";			//$NON-NLS-1$
	//
	private static final String					TAG_ROOT							= "ColumnProfiles";					//$NON-NLS-1$
	private static final String					TAG_PROFILE							= "Profile";							//$NON-NLS-1$
	//
	private static final String					ATTR_IS_ACTIVE_PROFILE				= "isActiveProfile";					//$NON-NLS-1$
	private static final String					ATTR_NAME							= "name";								//$NON-NLS-1$
	private static final String					ATTR_VISIBLE_COLUMN_IDS				= "visibleColumnIds";					//$NON-NLS-1$
	private static final String					ATTR_VISIBLE_COLUMN_IDS_AND_WIDTH	= "visibleColumnIdsAndWidth";			//$NON-NLS-1$
	//
	static final String							COLUMN_CATEGORY_SEPARATOR			= "   \u00bb   ";						//$NON-NLS-1$
	static final String							COLUMN_TEXT_SEPARATOR				= "   \u00B7   ";						//$NON-NLS-1$

	/**
	 * Minimum column width, when the column width is 0, there was a bug that this happened.
	 */
	private static final int					MINIMUM_COLUMN_WIDTH				= 7;

	/**
	 * Contains all column definitions which are defined for the table/tree viewer.
	 * <p>
	 * The sequence how they are added is the default.
	 */
	private final ArrayList<ColumnDefinition>	_allDefinedColumnDefinitions		= new ArrayList<ColumnDefinition>();

	/** All column profiles */
	private ArrayList<ColumnProfile>			_allProfiles						= new ArrayList<>();

	/** Active column profile */
	private ColumnProfile						_activeProfile;

	private Comparator<ColumnProfile>			_profileSorter;

	private final ITourViewer					_tourViewer;
	private AbstractColumnLayout				_columnLayout;

	/**
	 * Viewer which is managed by this {@link ColumnManager}.
	 */
	private ColumnViewer						_columnViewer;

	/**
	 * Context menu listener.
	 */
	private Listener							_tableMenuDetectListener;
	private Listener							_treeMenuDetectListener;

	private final Listener						_colItemListener;
	{
		_colItemListener = new Listener() {
			@Override
			public void handleEvent(final Event event) {
				onSelectColumnItem(event);
			}
		};

		_profileSorter = new Comparator<ColumnProfile>() {
			@Override
			public int compare(final ColumnProfile colProfile1, final ColumnProfile colProfile2) {
				return colProfile1.name.compareTo(colProfile2.name);
			}
		};
	}

	public ColumnManager(final ITourViewer tourViewer, final IDialogSettings viewState) {

		_tourViewer = tourViewer;

		restoreState(viewState);
	}

	private void actionFitAllColumnSize() {

		// larger tables/trees needs more time to resize

		BusyIndicator.showWhile(_columnViewer.getControl().getDisplay(), new Runnable() {
			@Override
			public void run() {

				boolean isColumn0Visible = true;

				if (_tourViewer instanceof ITourViewer2) {
					isColumn0Visible = ((ITourViewer2) _tourViewer).isColumn0Visible(_columnViewer);
				}

				if (_columnViewer instanceof TableViewer) {

					final Table table = ((TableViewer) _columnViewer).getTable();
					if (table.isDisposed()) {
						return;
					}

					table.setRedraw(false);
					{
						final TableColumn[] allColumns = table.getColumns();

						for (int columnIndex = 0; columnIndex < allColumns.length; columnIndex++) {
							final TableColumn tableColumn = allColumns[columnIndex];
							if (columnIndex == 0) {

								if (isColumn0Visible) {
									tableColumn.pack();
								} else {
									tableColumn.setWidth(0);
								}
							} else {
								tableColumn.pack();
							}
						}
					}
					table.setRedraw(true);

				} else if (_columnViewer instanceof TreeViewer) {

					final Tree tree = ((TreeViewer) _columnViewer).getTree();
					if (tree.isDisposed()) {
						return;
					}

					tree.setRedraw(false);
					{
						final TreeColumn[] allColumns = tree.getColumns();
						for (final TreeColumn tableColumn : allColumns) {
							tableColumn.pack();
						}
					}
					tree.setRedraw(true);
				}
			}
		});
	}

	private void actionShowAllColumns() {

		setVisibleColumnIds_All();

		_columnViewer = _tourViewer.recreateViewer(_columnViewer);
	}

	private void actionShowDefaultColumns() {

		setVisibleColumnIds_Default();

		_columnViewer = _tourViewer.recreateViewer(_columnViewer);
	}

	public void addColumn(final ColumnDefinition colDef) {
		_allDefinedColumnDefinitions.add(colDef);
	}

	/**
	 * Removes all defined columns
	 */
	public void clearColumns() {
		_allDefinedColumnDefinitions.clear();
	}

	/**
	 * Creates the columns in the tree/table for all visible columns.
	 * 
	 * @param columnViewer
	 */
	public void createColumns(final ColumnViewer columnViewer) {

		_columnViewer = columnViewer;

		setVisibleColDefs(_activeProfile);

		if (columnViewer instanceof TableViewer) {

			// create all columns in the table

			for (final ColumnDefinition colDef : _activeProfile.visibleColumnDefinitions) {
				createTableColumn((TableColumnDefinition) colDef, (TableViewer) columnViewer);
			}

		} else if (columnViewer instanceof TreeViewer) {

			// create all columns in the tree

			for (final ColumnDefinition colDef : _activeProfile.visibleColumnDefinitions) {
				createTreeColumn((TreeColumnDefinition) colDef, (TreeViewer) columnViewer);
			}
		}
	}

	/**
	 * Create header context menu which has the action to modify columns
	 * 
	 * @param composite
	 * @param defaultContextMenu
	 * @return
	 */
	private Menu createHCM_0_Menu(final Composite composite, final Menu defaultContextMenu) {

		final Decorations shell = composite.getShell();
		final Menu headerContextMenu = new Menu(shell, SWT.POP_UP);

		/*
		 * IMPORTANT: Dispose the menus (only the current menu, when menu is set with setMenu() it
		 * will be disposed automatically)
		 */
		composite.addListener(SWT.Dispose, new Listener() {
			@Override
			public void handleEvent(final Event event) {

				headerContextMenu.dispose();

				if (defaultContextMenu != null) {
					defaultContextMenu.dispose();
				}
			}
		});

		return headerContextMenu;
	}

	private void createHCM_0_MenuItems(final Menu contextMenu) {

		setVisibleColumnIds_FromViewer();

		createHCM_10_Various(contextMenu);

		// separator
		new MenuItem(contextMenu, SWT.SEPARATOR);

		createHCM_20_Profiles(contextMenu);

		// separator
		new MenuItem(contextMenu, SWT.SEPARATOR);

		createHCM_30_Columns(contextMenu);
	}

	private void createHCM_10_Various(final Menu contextMenu) {

		/*
		 * Action: Size All Columns to Fit
		 */
		{
			final MenuItem fitMenuItem = new MenuItem(contextMenu, SWT.PUSH);
			fitMenuItem.setText(Messages.Action_App_SizeAllColumnsToFit);
			fitMenuItem.addListener(SWT.Selection, new Listener() {
				@Override
				public void handleEvent(final Event event) {
					actionFitAllColumnSize();
				}
			});
		}

		/*
		 * Action: Show all columns
		 */
		{
			final MenuItem allColumnsMenuItem = new MenuItem(contextMenu, SWT.PUSH);
			allColumnsMenuItem.setText(Messages.Action_ColumnManager_ShowAllColumns);
			allColumnsMenuItem.addListener(SWT.Selection, new Listener() {
				@Override
				public void handleEvent(final Event event) {
					actionShowAllColumns();
				}
			});
		}

		/*
		 * Action: Show default columns
		 */
		{
			final MenuItem defaultColumnsMenuItem = new MenuItem(contextMenu, SWT.PUSH);
			defaultColumnsMenuItem.setText(Messages.Action_ColumnManager_ShowDefaultColumns);
			defaultColumnsMenuItem.addListener(SWT.Selection, new Listener() {
				@Override
				public void handleEvent(final Event event) {
					actionShowDefaultColumns();
				}
			});
		}

		/*
		 * Action: &Customize Profiles/Columns...
		 */
		{
			final MenuItem configMenuItem = new MenuItem(contextMenu, SWT.PUSH);
			configMenuItem.setText(Messages.Action_App_CustomizeColumnsAndProfiles);
			configMenuItem.setImage(UI.IMAGE_REGISTRY.get(UI.IMAGE_CONFIGURE_COLUMNS));
			configMenuItem.addListener(SWT.Selection, new Listener() {
				@Override
				public void handleEvent(final Event event) {
					openColumnDialog();
				}
			});
		}
	}

	/**
	 * Action: Profiles
	 */
	private void createHCM_20_Profiles(final Menu contextMenu) {

		{
			final MenuItem menuItem = new MenuItem(contextMenu, SWT.PUSH);
			menuItem.setText(Messages.Action_ColumnManager_Profile_Info);
			menuItem.setEnabled(false);
		}

		/*
		 * Actions: All profiles
		 */
		Collections.sort(_allProfiles, _profileSorter);

		for (final ColumnProfile columnProfile : _allProfiles) {

			final boolean isChecked = columnProfile == _activeProfile;

			final String menuText = columnProfile.name
					+ COLUMN_TEXT_SEPARATOR
					+ Integer.toString(columnProfile.visibleColumnIds.length);

			final MenuItem menuItem = new MenuItem(contextMenu, SWT.CHECK);

			menuItem.setText(menuText);
//			menuItem.setEnabled(true);
			menuItem.setSelection(isChecked);

			menuItem.setData(columnProfile);
			menuItem.addListener(SWT.Selection, _colItemListener);
		}
	}

	/**
	 * Action: Columns
	 */
	private void createHCM_30_Columns(final Menu contextMenu) {

		{
			final MenuItem menuItem = new MenuItem(contextMenu, SWT.PUSH);
			menuItem.setText(Messages.Action_ColumnManager_Column_Info);
			menuItem.setEnabled(false);
		}

		/*
		 * Actions: All columns
		 */
		for (final ColumnDefinition colDef : getRearrangedColumns()) {

			final MenuItem colMenuItem = new MenuItem(contextMenu, SWT.CHECK);

			/*
			 * Create column text
			 */
			final String category = colDef.getColumnCategory();
			final String label = colDef.getColumnLabel();
			final String unit = colDef.getColumnUnit();

			final StringBuilder sb = new StringBuilder();
			if (category != null) {
				sb.append(category);
			}

			if (label != null) {
				if (sb.length() > 0) {
					sb.append(COLUMN_CATEGORY_SEPARATOR);
				}
				sb.append(label);
			}

			if (unit != null) {

				if (sb.length() > 0) {
					sb.append(COLUMN_TEXT_SEPARATOR);
				}

				sb.append(unit);
			}

			colMenuItem.setText(sb.toString());
			colMenuItem.setEnabled(colDef.canModifyVisibility());
			colMenuItem.setSelection(colDef.isCheckedInDialog());

			colMenuItem.setData(colDef);
			colMenuItem.addListener(SWT.Selection, _colItemListener);
		}
	}

	/**
	 * set context menu depending on the position of the mouse
	 * 
	 * @param table
	 * @param defaultContextMenu
	 *            can be <code>null</code>
	 */
	public void createHeaderContextMenu(final Table table, final Menu defaultContextMenu) {

		// remove old listener
		if (_tableMenuDetectListener != null) {
			table.removeListener(SWT.MenuDetect, _tableMenuDetectListener);
		}

		final Menu headerContextMenu = createHCM_0_Menu(table, defaultContextMenu);

		// add the context menu to the table
		_tableMenuDetectListener = new Listener() {
			@Override
			public void handleEvent(final Event event) {

				final Display display = table.getShell().getDisplay();
				final Point pt = display.map(null, table, new Point(event.x, event.y));

				final Rectangle tblClientArea = table.getClientArea();
				final int headerHeight = table.getHeaderHeight();

				final boolean isTableHeaderHit = tblClientArea.y <= pt.y && pt.y < (tblClientArea.y + headerHeight);

//				System.out.println((UI.timeStampNano() + " [" + getClass().getSimpleName() + "] ")
//						+ ("\tisTableHeaderHit: " + isTableHeaderHit)
//						+ ("\tclientArea: " + tblClientArea)
//						+ ("\theaderHeight: " + headerHeight)
//						+ ("\tpt: " + pt));
//				// TODO remove SYSTEM.OUT.PRINTLN
//
//
				/**
				 * When the context menu is opened with the keyboard, the context menu from the
				 * tree/table is displayed and not from the header when the mouse is hovering the
				 * header. "detail" contains the info which input devices requested the context
				 * menu.
				 * <p>
				 * Currently the context menu for the header with the keyboard is not supported.
				 */
//
//				if (lParam != -1) {
//					detail = SWT.MENU_MOUSE;
//				} else {
//					detail = SWT.MENU_KEYBOARD;
//				}

				table.setMenu(getContextMenu(isTableHeaderHit, headerContextMenu, defaultContextMenu));
			}
		};

		table.addListener(SWT.MenuDetect, _tableMenuDetectListener);
	}

	/**
	 * set context menu depending on the position of the mouse
	 * 
	 * @param tree
	 * @param defaultContextMenu
	 *            can be <code>null</code>
	 */
	public void createHeaderContextMenu(final Tree tree, final Menu defaultContextMenu) {

		// remove old listener
		if (_treeMenuDetectListener != null) {
			tree.removeListener(SWT.MenuDetect, _treeMenuDetectListener);
		}

		final Menu headerContextMenu = createHCM_0_Menu(tree, defaultContextMenu);

		// add the context menu to the tree viewer
		_treeMenuDetectListener = new Listener() {
			@Override
			public void handleEvent(final Event event) {

				final Decorations shell = tree.getShell();
				final Display display = shell.getDisplay();
				final Point pt = display.map(null, tree, new Point(event.x, event.y));
				final Rectangle clientArea = tree.getClientArea();

				final boolean isTreeHeaderHit = clientArea.y <= pt.y && pt.y < (clientArea.y + tree.getHeaderHeight());

				tree.setMenu(getContextMenu(isTreeHeaderHit, headerContextMenu, defaultContextMenu));
			}
		};
		tree.addListener(SWT.MenuDetect, _treeMenuDetectListener);
	}

	/**
	 * Creates a column in a table viewer
	 * 
	 * @param colDef
	 * @param tableViewer
	 */
	private void createTableColumn(final TableColumnDefinition colDef, final TableViewer tableViewer) {

		TableViewerColumn tvc;
		TableColumn tc;

		tvc = new TableViewerColumn(tableViewer, colDef.getColumnStyle());

		final CellLabelProvider cellLabelProvider = colDef.getCellLabelProvider();
		if (cellLabelProvider != null) {
			tvc.setLabelProvider(cellLabelProvider);
		}

		tvc.setEditingSupport(colDef.getEditingSupport());

		// get column widget
		tc = tvc.getColumn();

		final String columnText = colDef.getColumnHeaderText();
		if (columnText != null) {
			tc.setText(columnText);
		}

		final String columnToolTipText = colDef.getColumnHeaderToolTipText();
		if (columnToolTipText != null) {
			tc.setToolTipText(columnToolTipText);
		}

		/*
		 * set column width
		 */
		if (_columnLayout == null) {

			// set the column width with pixels

			tc.setWidth(getColumnWidth(colDef));

		} else {

			// use the column layout to set the width of the columns

			final ColumnLayoutData columnLayoutData = colDef.getColumnWeightData();

			if (columnLayoutData == null) {
				try {
					throw new Exception("ColumnWeightData is not set for the column: " + colDef); //$NON-NLS-1$
				} catch (final Exception e) {
					e.printStackTrace();
				}
			}

			if (columnLayoutData instanceof ColumnPixelData) {
				final ColumnPixelData columnPixelData = (ColumnPixelData) columnLayoutData;

				// overwrite the width
				columnPixelData.width = getColumnWidth(colDef);
				_columnLayout.setColumnData(tc, columnPixelData);
			} else {
				_columnLayout.setColumnData(tc, columnLayoutData);
			}
		}

		tc.setResizable(colDef.isColumnResizable());
		tc.setMoveable(colDef.isColumnMoveable());

		// keep reference to the column definition
		tc.setData(colDef);

		// keep tc ref
		colDef.setTableColumn(tc);

		// keep create index
		final int tcIndex = tableViewer.getTable().getColumnCount();
		colDef.setTableColumnIndex(tcIndex);

		// add selection listener
		final SelectionAdapter columnSelectionListener = colDef.getColumnSelectionListener();
		if (columnSelectionListener != null) {
			tc.addSelectionListener(columnSelectionListener);
		}

		// add resize/move listener
		final ControlListener columnControlListener = colDef.getColumnControlListener();
		if (columnControlListener != null) {
			tc.addControlListener(columnControlListener);
		}
	}

	/**
	 * Creates a column in a tree viewer
	 * 
	 * @param colDef
	 * @param treeViewer
	 */
	private void createTreeColumn(final TreeColumnDefinition colDef, final TreeViewer treeViewer) {

		TreeViewerColumn tvc;
		TreeColumn tc;

		tvc = new TreeViewerColumn(treeViewer, colDef.getColumnStyle());

		final CellLabelProvider cellLabelProvider = colDef.getCellLabelProvider();
		if (cellLabelProvider != null) {
			tvc.setLabelProvider(cellLabelProvider);
		}

		tc = tvc.getColumn();

		final String columnText = colDef.getColumnHeaderText();
		if (columnText != null) {
			tc.setText(columnText);
		}

		final String columnToolTipText = colDef.getColumnHeaderToolTipText();
		if (columnToolTipText != null) {
			tc.setToolTipText(columnToolTipText);
		}

		/*
		 * set column width
		 */
		int columnWidth = colDef.getColumnWidth();
		if (colDef.isColumnHidden()) {
			columnWidth = 0;
		} else {
			columnWidth = columnWidth < MINIMUM_COLUMN_WIDTH ? colDef.getDefaultColumnWidth() : columnWidth;
		}
		tc.setWidth(columnWidth);

		tc.setResizable(colDef.isColumnResizable());
		tc.setMoveable(colDef.isColumnMoveable());

		// keep reference to the column definition
		tc.setData(colDef);
		colDef.setTreeColumn(tc);

		// add selection listener
		final SelectionAdapter columnSelectionListener = colDef.getColumnSelectionListener();
		if (columnSelectionListener != null) {
			tc.addSelectionListener(columnSelectionListener);
		}

		// add resize/move listener
		final ControlListener columnControlListener = colDef.getColumnControlListener();
		if (columnControlListener != null) {
			tc.addControlListener(columnControlListener);
		}
	}

	/**
	 * @param columnId
	 *            column id
	 * @return Returns the column definition for the column id, or <code>null</code> when the column
	 *         for the column id is not available
	 */
	private ColumnDefinition getColDef_ByColumnId(final String columnId) {

		for (final ColumnDefinition colDef : _allDefinedColumnDefinitions) {
			if (colDef.getColumnId().compareTo(columnId) == 0) {
				return colDef;
			}
		}

		return null;
	}

	/**
	 * @param orderIndex
	 *            column create id
	 * @return Returns the column definition for the column create index, or <code>null</code> when
	 *         the column is not available
	 */
	private ColumnDefinition getColDef_ByCreateIndex(final int orderIndex) {

		for (final ColumnDefinition colDef : _activeProfile.visibleColumnDefinitions) {
			if (colDef.getCreateIndex() == orderIndex) {
				return colDef;
			}
		}

		return null;
	}

	/**
	 * @return Returns the columns in the format: id/width ...
	 */
	private String[] getColumns_FromViewer_IdAndWidth() {

		final ArrayList<String> columnIdsAndWidth = new ArrayList<String>();

		if (_columnViewer instanceof TableViewer) {

			final Table table = ((TableViewer) _columnViewer).getTable();
			if (table.isDisposed()) {
				return null;
			}

			for (final TableColumn column : table.getColumns()) {

				final String columnId = ((ColumnDefinition) column.getData()).getColumnId();
				final int columnWidth = column.getWidth();

				setColumnIdAndWidth(columnIdsAndWidth, columnId, columnWidth);
			}

		} else if (_columnViewer instanceof TreeViewer) {

			final Tree tree = ((TreeViewer) _columnViewer).getTree();
			if (tree.isDisposed()) {
				return null;
			}

			for (final TreeColumn column : tree.getColumns()) {

				final String columnId = ((TreeColumnDefinition) column.getData()).getColumnId();
				final int columnWidth = column.getWidth();

				setColumnIdAndWidth(columnIdsAndWidth, columnId, columnWidth);
			}
		}

		return columnIdsAndWidth.toArray(new String[columnIdsAndWidth.size()]);
	}

	/**
	 * Read the column order from a table/tree.
	 * 
	 * @return Returns <code>null</code> when table/tree cannot be accessed.
	 */
	private String[] getColumns_FromViewer_Ids() {

		final ArrayList<String> orderedColumnIds = new ArrayList<String>();

		int[] columnOrder = null;

		if (_columnViewer instanceof TableViewer) {

			final Table table = ((TableViewer) _columnViewer).getTable();
			if (table.isDisposed()) {
				return null;
			}
			columnOrder = table.getColumnOrder();

		} else if (_columnViewer instanceof TreeViewer) {

			final Tree tree = ((TreeViewer) _columnViewer).getTree();
			if (tree.isDisposed()) {
				return null;
			}
			columnOrder = tree.getColumnOrder();
		}

		if (columnOrder == null) {
			return null;
		}

		// create columns in the correct sort order
		for (final int createIndex : columnOrder) {

			final ColumnDefinition colDef = getColDef_ByCreateIndex(createIndex);

			if (colDef != null) {
				orderedColumnIds.add(colDef.getColumnId());
			}
		}

		return orderedColumnIds.toArray(new String[orderedColumnIds.size()]);
	}

	private int getColumnWidth(final String columnWidthId) {

		final String[] values = _activeProfile.visibleColumnIdsAndWidth;

		for (int columnIndex = 0; columnIndex < values.length; columnIndex++) {

			final String columnId = values[columnIndex];

			if (columnWidthId.equals(columnId)) {
				try {
					return Integer.parseInt(values[++columnIndex]);
				} catch (final Exception e) {
					// ignore format exception
				}
			}

			// skip width, advance to next id
			columnIndex++;
		}

		return 0;
	}

	private int getColumnWidth(final TableColumnDefinition colDef) {

		int columnWidth = colDef.getColumnWidth();

		if (colDef.isColumnHidden()) {
			columnWidth = 0;
		} else {
			columnWidth = columnWidth < MINIMUM_COLUMN_WIDTH ? colDef.getDefaultColumnWidth() : columnWidth;
		}

		return columnWidth;
	}

	private Menu getContextMenu(final boolean isHeaderHit, final Menu headerContextMenu, final Menu defaultContextMenu) {

		Menu contextMenu;
		if (isHeaderHit) {

			contextMenu = headerContextMenu;

			// recreate all menu items because the column order can be changed
			for (final MenuItem menuItem : contextMenu.getItems()) {
				menuItem.dispose();
			}

			createHCM_0_MenuItems(headerContextMenu);

		} else {

			contextMenu = defaultContextMenu;
		}

		return contextMenu;
	}

	/**
	 * Read the order/width for the columns, this is necessary because the user can have rearranged
	 * the columns and/or resized the columns with the mouse.
	 * 
	 * @return Returns ALL columns, first the visible then the hidden columns.
	 */
	private ArrayList<ColumnDefinition> getRearrangedColumns() {

		final ArrayList<ColumnDefinition> allRearrangedColumns = new ArrayList<ColumnDefinition>();
		final ArrayList<ColumnDefinition> allColDefClone = new ArrayList<ColumnDefinition>();

		try {
			for (final ColumnDefinition definedColDef : _allDefinedColumnDefinitions) {
				allColDefClone.add((ColumnDefinition) definedColDef.clone());
			}
		} catch (final CloneNotSupportedException e) {
			StatusUtil.log(e);
		}

		int[] columnOrder = null;

		/*
		 * get column order from viewer
		 */
		if (_columnViewer instanceof TableViewer) {

			final Table table = ((TableViewer) _columnViewer).getTable();
			if (table.isDisposed()) {
				return null;
			}
			columnOrder = table.getColumnOrder();

		} else if (_columnViewer instanceof TreeViewer) {

			final Tree tree = ((TreeViewer) _columnViewer).getTree();
			if (tree.isDisposed()) {
				return null;
			}
			columnOrder = tree.getColumnOrder();
		}

		/*
		 * Add visible columns in the sort order of the modify dialog
		 */
		for (final int createIndex : columnOrder) {

			final ColumnDefinition colDef = getColDef_ByCreateIndex(createIndex);
			if (colDef != null) {

				// check all visible columns in the dialog
				colDef.setIsCheckedInDialog(true);

				// set column width
				colDef.setColumnWidth(getColumnWidth(colDef._columnId));

				// keep the column
				allRearrangedColumns.add(colDef);

				allColDefClone.remove(colDef);
			}
		}

		/*
		 * Add remaining columns which are defined but not visible
		 */
		for (final ColumnDefinition colDef : allColDefClone) {

			// uncheck hidden columns
			colDef.setIsCheckedInDialog(false);

			// set column default width
			colDef.setColumnWidth(colDef.getDefaultColumnWidth());

			allRearrangedColumns.add(colDef);
		}

		return allRearrangedColumns;
	}

	private void onSelectColumnItem(final Event event) {

		if (event.widget instanceof MenuItem) {

			final MenuItem menuItem = (MenuItem) event.widget;

			final Object data = menuItem.getData();

			if (data instanceof ColumnDefinition) {

				updateColumns(menuItem.getParent().getItems());

			} else if (data instanceof ColumnProfile) {

				final ColumnProfile profile = (ColumnProfile) data;

				updateColumns(profile);
			}
		}
	}

	public void openColumnDialog() {

		setVisibleColumnIds_FromViewer();

		final DialogModifyColumns columnDialog = new DialogModifyColumns(
				Display.getCurrent().getActiveShell(),
				this,
				getRearrangedColumns(),
				_allDefinedColumnDefinitions,
				_activeProfile,
				_allProfiles);

		columnDialog.open();
	}

	/**
	 * Restore the column order and width from a memento
	 * 
	 * @param state
	 */
	private void restoreState(final IDialogSettings state) {

		ColumnProfile activeProfile = null;
		final ArrayList<ColumnProfile> allProfiles = new ArrayList<>();

		final String stateValue = Util.getStateString(state, XML_STATE_COLUMN_MANAGER, null);
		if (stateValue != null) {

			try {

				final Reader reader = new StringReader(stateValue);
				final XMLMemento xmlMemento = XMLMemento.createReadRoot(reader);

				for (final IMemento memento : xmlMemento.getChildren()) {

					final XMLMemento xmlProfile = (XMLMemento) memento;

					if (TAG_PROFILE.equals(xmlProfile.getType())) {

						final ColumnProfile currentProfile = new ColumnProfile();

						// name
						final String xmlName = xmlProfile.getString(ATTR_NAME);
						if (xmlName != null) {
							currentProfile.name = xmlName;
						}

						// active profile
						final Boolean xmlIsActive = xmlProfile.getBoolean(ATTR_IS_ACTIVE_PROFILE);
						if (xmlIsActive != null && xmlIsActive) {
							activeProfile = currentProfile;
						}

						// visible column id's
						final String xmlColumnIds = xmlProfile.getString(ATTR_VISIBLE_COLUMN_IDS);
						if (xmlColumnIds != null) {

							currentProfile.visibleColumnIds = StringToArrayConverter.convertStringToArray(xmlColumnIds);
						}

						// visible column id's and width
						final String xmlColumnIdsAndWidth = xmlProfile.getString(ATTR_VISIBLE_COLUMN_IDS_AND_WIDTH);
						if (xmlColumnIdsAndWidth != null) {

							currentProfile.visibleColumnIdsAndWidth = StringToArrayConverter
									.convertStringToArray(xmlColumnIdsAndWidth);
						}

						allProfiles.add(currentProfile);
					}
				}

			} catch (final WorkbenchException e) {
				// ignore
			}
		}

		// ensure 1 profile is available
		if (allProfiles.size() == 0) {

			// create default profile
			final ColumnProfile defaultProfile = new ColumnProfile();
			defaultProfile.name = Messages.Column_Profile_Name_Default;

			allProfiles.add(defaultProfile);
		}

		if (activeProfile == null) {

			// use 1st profile as default
			activeProfile = allProfiles.get(0);
		}

		_activeProfile = activeProfile;
		_allProfiles = allProfiles;
	}

	/**
	 * Save the column order and width into a memento
	 * 
	 * @param state
	 */
	public void saveState(final IDialogSettings state) {

		/*
		 * Update state for the active profile
		 */

		// save column sort order
		final String[] visibleColumnIds = getColumns_FromViewer_Ids();
		if (visibleColumnIds != null) {
			_activeProfile.visibleColumnIds = visibleColumnIds;
		}

		// save columns width and keep it for internal use
		final String[] visibleColumnIdsAndWidth = getColumns_FromViewer_IdAndWidth();
		if (visibleColumnIdsAndWidth != null) {
			_activeProfile.visibleColumnIdsAndWidth = visibleColumnIdsAndWidth;
		}

		/*
		 * Save profiles
		 */
		// Build the XML block for writing the bindings and active scheme.
		final XMLMemento xmlMemento = XMLMemento.createWriteRoot(TAG_ROOT);

		saveState_Profiles(xmlMemento);

		// Write the XML block to the state store.
		try (final Writer writer = new StringWriter()) {

			xmlMemento.save(writer);
			state.put(XML_STATE_COLUMN_MANAGER, writer.toString());

		} catch (final IOException e) {
			StatusUtil.log(e);
		}
	}

	private void saveState_Profiles(final XMLMemento xmlMemento) {

		for (final ColumnProfile profile : _allProfiles) {

			final IMemento xmlProfile = xmlMemento.createChild(TAG_PROFILE);

			xmlProfile.putString(ATTR_NAME, profile.name);

			if (profile == _activeProfile) {
				xmlProfile.putBoolean(ATTR_IS_ACTIVE_PROFILE, true);
			}

			final String[] visibleColumnIds = profile.visibleColumnIds;
			if (visibleColumnIds != null) {

				xmlProfile.putString(
						ATTR_VISIBLE_COLUMN_IDS,
						StringToArrayConverter.convertArrayToString(visibleColumnIds));
			}

			final String[] visibleColumnIdsAndWidth = profile.visibleColumnIdsAndWidth;
			if (visibleColumnIdsAndWidth != null) {

				xmlProfile.putString(
						ATTR_VISIBLE_COLUMN_IDS_AND_WIDTH,
						StringToArrayConverter.convertArrayToString(visibleColumnIdsAndWidth));
			}
		}
	}

	private void setColumnIdAndWidth(final ArrayList<String> columnIdsAndWidth, final String columnId, int columnWidth) {

		final ColumnDefinition colDef = getColDef_ByColumnId(columnId);
		if (colDef.isColumnHidden()) {

			// column is hidden

			columnWidth = 0;

		} else {

			// column is visible

			if (columnWidth == 0) {
				/*
				 * there is somewhere an error that the column width is 0,
				 */
				columnWidth = colDef.getDefaultColumnWidth();
				columnWidth = Math.max(MINIMUM_COLUMN_WIDTH, columnWidth);
			}
		}

		columnIdsAndWidth.add(columnId);
		columnIdsAndWidth.add(Integer.toString(columnWidth));
	}

	/**
	 * Sets the column layout for the viewer which is managed by the {@link ColumnManager}.
	 * <p>
	 * When the columnLayout is set, all columns must have a {@link ColumnWeightData}, otherwise it
	 * will fail
	 * 
	 * @param columnLayout
	 */
	public void setColumnLayout(final AbstractColumnLayout columnLayout) {
		_columnLayout = columnLayout;
	}

	/**
	 * Set column definitions in the {@link ColumnProfile} from the visible id's.
	 * 
	 * @param columnProfile
	 */
	void setVisibleColDefs(final ColumnProfile columnProfile) {

		final ArrayList<ColumnDefinition> visibleColDefs = columnProfile.visibleColumnDefinitions;

		visibleColDefs.clear();

		if (columnProfile.visibleColumnIds != null) {

			// create columns with the correct sort order

			int createIndex = 0;

			for (final String columnId : columnProfile.visibleColumnIds) {

				final ColumnDefinition colDef = getColDef_ByColumnId(columnId);
				if (colDef != null) {

					colDef.setCreateIndex(createIndex++);

					visibleColDefs.add(colDef);
				}
			}
		}

		final String[] visibleColumnIdsAndWidth = columnProfile.visibleColumnIdsAndWidth;
		if (visibleColumnIdsAndWidth != null) {

			// set the width for all columns

			for (int dataIdx = 0; dataIdx < visibleColumnIdsAndWidth.length; dataIdx++) {

				final String columnId = visibleColumnIdsAndWidth[dataIdx++];
				final int columnWidth = Integer.valueOf(visibleColumnIdsAndWidth[dataIdx]);

				final ColumnDefinition colDef = getColDef_ByColumnId(columnId);
				if (colDef != null) {
					colDef.setColumnWidth(columnWidth);
				}
			}
		}

		/*
		 * when no columns are visible (which is the first time), show only the default columns
		 * because every column reduces performance
		 */
		if ((visibleColDefs.size() == 0) && (_allDefinedColumnDefinitions.size() > 0)) {

			final ArrayList<String> columnIds = new ArrayList<String>();
			int createIndex = 0;

			for (final ColumnDefinition colDef : _allDefinedColumnDefinitions) {
				if (colDef.isDefaultColumn()) {

					colDef.setCreateIndex(createIndex++);

					visibleColDefs.add(colDef);
					columnIds.add(colDef.getColumnId());
				}
			}

			columnProfile.visibleColumnIds = columnIds.toArray(new String[columnIds.size()]);
		}

		/*
		 * when no default columns are set, use the first column
		 */
		if ((visibleColDefs.size() == 0) && (_allDefinedColumnDefinitions.size() > 0)) {

			final ColumnDefinition firstColumn = _allDefinedColumnDefinitions.get(0);
			firstColumn.setCreateIndex(0);

			visibleColDefs.add(firstColumn);

			columnProfile.visibleColumnIds = new String[1];
			columnProfile.visibleColumnIds[0] = firstColumn.getColumnId();
		}

		/*
		 * Ensure that all columns which must be visible, are also displayed. This case can happen
		 * when new columns are added.
		 */
		final ArrayList<ColumnDefinition> notAddedColumns = new ArrayList<ColumnDefinition>();

		for (final ColumnDefinition colDef : _allDefinedColumnDefinitions) {

			if (colDef.canModifyVisibility() == false) {

				if (visibleColDefs.contains(colDef) == false) {
					notAddedColumns.add(colDef);
				}
			}
		}

		if (notAddedColumns.size() > 0) {

			visibleColDefs.addAll(notAddedColumns);

			/*
			 * Set create index, otherwise save/restore do not work!!!
			 */
			int createIndex = 0;
			for (final ColumnDefinition colDef : visibleColDefs) {
				colDef.setCreateIndex(createIndex++);
			}

			/*
			 * Set visible id's
			 */
			final ArrayList<String> columnIds = new ArrayList<String>();

			for (final ColumnDefinition colDef : visibleColDefs) {
				columnIds.add(colDef._columnId);
			}

			columnProfile.visibleColumnIds = columnIds.toArray(new String[columnIds.size()]);
		}
	}

	private void setVisibleColumnIds_All() {

		final ArrayList<String> visibleColumnIds = new ArrayList<String>();
		final ArrayList<String> visibleIdsAndWidth = new ArrayList<String>();

		for (final ColumnDefinition colDef : _allDefinedColumnDefinitions) {

			// set visible columns
			visibleColumnIds.add(colDef.getColumnId());

			// set column id and width
			visibleIdsAndWidth.add(colDef.getColumnId());
			visibleIdsAndWidth.add(Integer.toString(colDef.getColumnWidth()));
		}

		_activeProfile.visibleColumnIds = visibleColumnIds.toArray(new String[visibleColumnIds.size()]);
		_activeProfile.visibleColumnIdsAndWidth = visibleIdsAndWidth.toArray(new String[visibleIdsAndWidth.size()]);
	}

	private void setVisibleColumnIds_Default() {

		final ArrayList<String> visibleColumnIds = new ArrayList<String>();
		final ArrayList<String> visibleIdsAndWidth = new ArrayList<String>();

		for (final ColumnDefinition colDef : _allDefinedColumnDefinitions) {

			if (colDef.isDefaultColumn()) {

				// set visible columns
				visibleColumnIds.add(colDef.getColumnId());

				// set column id and width
				visibleIdsAndWidth.add(colDef.getColumnId());
				visibleIdsAndWidth.add(Integer.toString(colDef.getColumnWidth()));
			}
		}

		_activeProfile.visibleColumnIds = visibleColumnIds.toArray(new String[visibleColumnIds.size()]);
		_activeProfile.visibleColumnIdsAndWidth = visibleIdsAndWidth.toArray(new String[visibleIdsAndWidth.size()]);
	}

	private void setVisibleColumnIds_FromMenu(final MenuItem[] menuItems) {

		final ArrayList<String> visibleColumnIds = new ArrayList<String>();
		final ArrayList<String> columnIdsAndWidth = new ArrayList<String>();

		// recreate columns in the correct sort order
		for (final MenuItem menuItem : menuItems) {

			final Object itemData = menuItem.getData();

			if (itemData instanceof ColumnDefinition) {

				final boolean isChecked = menuItem.getSelection();

				if (isChecked) {

					// data in the table item contains the input items for the viewer
					final ColumnDefinition colDef = (ColumnDefinition) itemData;

					// set the visible columns
					visibleColumnIds.add(colDef.getColumnId());

					// set column id and width
					columnIdsAndWidth.add(colDef.getColumnId());
					columnIdsAndWidth.add(Integer.toString(colDef.getColumnWidth()));
				}
			}
		}

		_activeProfile.visibleColumnIds = visibleColumnIds.toArray(new String[visibleColumnIds.size()]);
		_activeProfile.visibleColumnIdsAndWidth = columnIdsAndWidth.toArray(new String[columnIdsAndWidth.size()]);
	}

	/**
	 * Set the columns in {@link #_activeProfile._visibleColumnDefinitions} to the order of the <code>tableItems</code> in the
	 * {@link DialogModifyColumns}
	 * 
	 * @param tableItems
	 */
	void setVisibleColumnIds_FromModifyDialog(final ColumnProfile profile, final TableItem[] tableItems) {

		final ArrayList<String> visibleColumnIds = new ArrayList<String>();
		final ArrayList<String> columnIdsAndWidth = new ArrayList<String>();

		// recreate columns in the correct sort order
		for (final TableItem tableItem : tableItems) {

			if (tableItem.getChecked()) {

				// data in the table item contains the input items for the viewer
				final ColumnDefinition colDef = (ColumnDefinition) tableItem.getData();

				// set the visible columns
				visibleColumnIds.add(colDef.getColumnId());

				// set column id and width
				columnIdsAndWidth.add(colDef.getColumnId());
				columnIdsAndWidth.add(Integer.toString(colDef.getColumnWidth()));
			}
		}

		profile.visibleColumnIds = visibleColumnIds.toArray(new String[visibleColumnIds.size()]);
		profile.visibleColumnIdsAndWidth = columnIdsAndWidth.toArray(new String[columnIdsAndWidth.size()]);
	}

	/**
	 * Read the sorting order and column width from the viewer.
	 */
	private void setVisibleColumnIds_FromViewer() {

		// get the sorting order and column width from the viewer
		_activeProfile.visibleColumnIds = getColumns_FromViewer_Ids();
		_activeProfile.visibleColumnIdsAndWidth = getColumns_FromViewer_IdAndWidth();
	}

	private void updateColumns(final ColumnProfile profile) {

		_activeProfile = profile;

		_columnViewer = _tourViewer.recreateViewer(_columnViewer);
	}

	/**
	 * Update the viewer with the columns from the {@link DialogModifyColumns}
	 * 
	 * @param dialogActiveProfile
	 * @param tableItems
	 *            table item in the {@link DialogModifyColumns}
	 */
	void updateColumns(final ColumnProfile dialogActiveProfile, final TableItem[] tableItems) {

		_activeProfile = dialogActiveProfile;

		setVisibleColumnIds_FromModifyDialog(_activeProfile, tableItems);

		_columnViewer = _tourViewer.recreateViewer(_columnViewer);
	}

	private void updateColumns(final MenuItem[] menuItems) {

		setVisibleColumnIds_FromMenu(menuItems);

		_columnViewer = _tourViewer.recreateViewer(_columnViewer);
	}
}
