/*******************************************************************************
 * Copyright (C) 2005, 2015 Wolfgang Schramm and Contributors
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
package net.tourbook.importdata;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.action.ActionOpenPrefDialog;
import net.tourbook.common.util.ColumnManager;
import net.tourbook.common.util.ITourViewer;
import net.tourbook.common.util.TableColumnDefinition;
import net.tourbook.common.util.Util;
import net.tourbook.common.widgets.ComboEnumEntry;
import net.tourbook.data.TourData;
import net.tourbook.data.TourType;
import net.tourbook.database.TourDatabase;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.views.rawData.RawDataView;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerDropAdapter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DragSourceListener;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.part.PageBook;

/**
 * This is a template for a title area dialog
 */
public class DialogImportConfig extends TitleAreaDialog implements ITourViewer {

	private static final String			ID							= "net.tourbook.importdata.DialogAutomatedImportConfig";	//$NON-NLS-1$
	//
	private static final String			STATE_SELECTED_CONFIG_NAME	= "STATE_SELECTED_CONFIG_NAME";							//$NON-NLS-1$
	//
	private static final String			DATA_KEY_TOUR_TYPE_ID		= "DATA_KEY_TOUR_TYPE_ID";									//$NON-NLS-1$
	private static final String			DATA_KEY_VERTEX_INDEX		= "DATA_KEY_VERTEX_INDEX";									//$NON-NLS-1$
	//
	//
	private final IDialogSettings		_state						= TourbookPlugin.getState(ID);
	//
	private SelectionAdapter			_defaultSelectionListener;
	private SelectionAdapter			_vertexTourTypeListener;
	private MouseWheelListener			_defaultMouseWheelListener;
	//
	private ActionAddVertex				_action_VertexAdd;
	private ActionDeleteVertex[]		_actionVertex_Delete;
	private ActionOpenPrefDialog		_actionOpenTourTypePrefs;
	private ActionSortVertices			_action_VertexSort;
	//

	private PixelConverter				_pc;

	private final static SpeedVertex[]	DEFAULT_VERTICES;

	static {

		DEFAULT_VERTICES = new SpeedVertex[] {
			//
			new SpeedVertex(10),
			new SpeedVertex(30),
			new SpeedVertex(150),
			new SpeedVertex(300)
		//
		};
	}

	/** Model for all configs. */
	private ImportConfig				_dialogConfig;

	/** Model for the currently selected config. */
	private ImportConfigItem			_currentConfigItem;

	private RawDataView					_rawDataView;

	private TableViewer					_configViewer;

	private ColumnManager				_columnManager;
	private TableColumnDefinition		_colDefProfileImage;
	private int							_columnIndexConfigImage;

	private ImportConfigItem			_initialConfig;

	private HashMap<Long, Image>		_configImages				= new HashMap<>();
	private HashMap<Long, Integer>		_configImageHash			= new HashMap<>();

	private long						_dragStart;

	/*
	 * UI controls
	 */
	private Composite					_parent;

	private Composite					_vertexOuterContainer;
	private Composite					_vertexContainer;
	private Composite					_viewerContainer;

	private ScrolledComposite			_vertexScrolledContainer;

	private PageBook					_pagebookTourType;
	private Label						_pageTourType_NotUsed;
	private Composite					_pageTourType_OneForAll;
	private Composite					_pageTourType_BySpeed;

	private Button						_btnConfig_Copy;
	private Button						_btnConfig_New;
	private Button						_btnConfig_Remove;
	private Button						_btnSelectBackupFolder;
	private Button						_btnSelectDeviceFolder;

	private Combo						_comboBackupPath;
	private Combo						_comboDevicePath;
	private Combo						_comboTourTypeConfig;

	private Label						_lblBackupFolder;
	private Label						_lblConfigName;
	private Label						_lblDeviceFolder;
	private Label						_lblOneTourTypeIcon;
	private Label						_lblTourType;
	private Label[]						_lblVertex_SpeedUnit;
	private Label[]						_lblVertex_TourTypeIcon;

	private Link[]						_linkVertex_TourType;
	private Link						_linkOneTourType;

	private Spinner						_spinnerBgOpacity;
	private Spinner						_spinnerUIColumns;
	private Spinner[]					_spinnerVertex_AvgSpeed;

	private Text						_txtConfigName;

	private class ActionAddVertex extends Action {

		public ActionAddVertex() {

			super(null, AS_PUSH_BUTTON);

			setToolTipText(Messages.Dialog_ImportConfig_Action_AddSpeed_Tooltip);
			setImageDescriptor(TourbookPlugin.getImageDescriptor(net.tourbook.Messages.Image__App_Add));
		}

		@Override
		public void run() {
			onVertex_Add();
		}
	}

	private class ActionDeleteVertex extends Action {

		private int	_vertexIndex;

		public ActionDeleteVertex() {

			super(null, AS_PUSH_BUTTON);

			setToolTipText(Messages.Dialog_ImportConfig_Action_RemoveSpeed_Tooltip);

			setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__App_Trash));
			setDisabledImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__App_Trash_Disabled));
		}

		@Override
		public void run() {
			onVertex_Remove(_vertexIndex);
		}

		public void setData(final String key, final int vertexIndex) {

			_vertexIndex = vertexIndex;
		}
	}

	private class ActionSetOneTourType extends Action {

		private TourType	__tourType;

		/**
		 * @param tourType
		 * @param vertexIndex
		 * @param isSaveTour
		 *            when <code>true</code> the tour will be saved and a
		 *            {@link TourManager#TOUR_CHANGED} event is fired, otherwise the
		 *            {@link TourData} from the tour provider is only updated
		 */
		public ActionSetOneTourType(final TourType tourType, final boolean isChecked) {

			super(tourType.getName(), AS_CHECK_BOX);

			if (isChecked == false) {

				// show image when tour type can be selected, disabled images look ugly on win
				final Image tourTypeImage = net.tourbook.ui.UI.getInstance().getTourTypeImage(tourType.getTypeId());
				setImageDescriptor(ImageDescriptor.createFromImage(tourTypeImage));
			}

			setChecked(isChecked);
			setEnabled(isChecked == false);

			__tourType = tourType;
		}

		@Override
		public void run() {
			updateUI_OneTourType(__tourType);
		}
	}

	private class ActionSetSpeedTourType extends Action {

		private int			_vertexIndex;
		private TourType	_tourType;

		/**
		 * @param tourType
		 * @param vertexIndex
		 * @param isSaveTour
		 *            when <code>true</code> the tour will be saved and a
		 *            {@link TourManager#TOUR_CHANGED} event is fired, otherwise the
		 *            {@link TourData} from the tour provider is only updated
		 */
		public ActionSetSpeedTourType(final TourType tourType, final boolean isChecked, final int vertexIndex) {

			super(tourType.getName(), AS_CHECK_BOX);

			_vertexIndex = vertexIndex;

			if (isChecked == false) {

				// show image when tour type can be selected, disabled images look ugly on win
				final Image tourTypeImage = net.tourbook.ui.UI.getInstance().getTourTypeImage(tourType.getTypeId());
				setImageDescriptor(ImageDescriptor.createFromImage(tourTypeImage));
			}

			setChecked(isChecked);
			setEnabled(isChecked == false);

			_tourType = tourType;
		}

		@Override
		public void run() {
			onVertex_SetTourType(_vertexIndex, _tourType);
		}
	}

	private class ActionSortVertices extends Action {

		public ActionSortVertices() {

			super(null, AS_PUSH_BUTTON);

			setToolTipText(Messages.Dialog_ImportConfig_Action_SortVertices_Tooltip);

			setImageDescriptor(TourbookPlugin.getImageDescriptor(net.tourbook.Messages.Image__App_Sort));
			setDisabledImageDescriptor(TourbookPlugin
					.getImageDescriptor(net.tourbook.Messages.Image__App_Sort_Disabled));
		}

		@Override
		public void run() {
			onVertex_Sort();
		}
	}

	private class ClientsContentProvider implements IStructuredContentProvider {

		public ClientsContentProvider() {}

		@Override
		public void dispose() {}

		@Override
		public Object[] getElements(final Object parent) {

			final ArrayList<ImportConfigItem> configItems = _dialogConfig.configItems;

			return configItems.toArray(new ImportConfigItem[configItems.size()]);
		}

		@Override
		public void inputChanged(final Viewer v, final Object oldInput, final Object newInput) {

		}
	}

	public DialogImportConfig(final Shell parentShell, final ImportConfig importConfig, final RawDataView rawDataView) {

		super(parentShell);

		_rawDataView = rawDataView;

		// make dialog resizable
		setShellStyle(getShellStyle() | SWT.RESIZE);

		setDefaultImage(TourbookPlugin.getImageDescriptor(Messages.Image__options).createImage());

		setupConfigs(importConfig);
	}

	@Override
	public boolean close() {

		saveState();

		return super.close();
	}

	@Override
	protected void configureShell(final Shell shell) {

		super.configureShell(shell);

		shell.setText(Messages.Dialog_ImportConfig_Dialog_Title);

//		shell.addListener(SWT.Resize, new Listener() {
//			@Override
//			public void handleEvent(final Event event) {
//
//				// ensure that the dialog is not smaller than the default size
//
//				final Point shellDefaultSize = shell.computeSize(SWT.DEFAULT, SWT.DEFAULT);
//
//				final Point shellSize = shell.getSize();
//
//				shellSize.x = shellSize.x < shellDefaultSize.x ? shellDefaultSize.x : shellSize.x;
//				shellSize.y = shellSize.y < shellDefaultSize.y ? shellDefaultSize.y : shellSize.y;
//
//				shell.setSize(shellSize);
//			}
//		});
	}

	@Override
	public void create() {

		super.create();

		setTitle(Messages.Dialog_ImportConfig_Dialog_Title);
		setMessage(Messages.Dialog_ImportConfig_Dialog_Message);

		restoreState();

		_configViewer.setInput(new Object());

		final Table table = _configViewer.getTable();
//
//		// prevent that the horizontal scrollbar is visible
//		table.getParent().layout();

		// select first config
		_configViewer.setSelection(new StructuredSelection(_initialConfig));

		// ensure that the selected also has the focus, these are 2 different things
		table.setSelection(table.getSelectionIndex());

		// set focus
		if (_dialogConfig.configItems.size() == 1) {
			_txtConfigName.setFocus();
		} else {
			table.setFocus();
		}
	}

	private void createActions() {

		_action_VertexAdd = new ActionAddVertex();
		_action_VertexSort = new ActionSortVertices();

		_actionOpenTourTypePrefs = new ActionOpenPrefDialog(
				Messages.action_tourType_modify_tourTypes,
				ITourbookPreferences.PREF_PAGE_TOUR_TYPE);
	}

	/**
	 * create the views context menu
	 */
	private void createContextMenu() {

		final MenuManager menuMgr = new MenuManager();

		menuMgr.setRemoveAllWhenShown(true);

//		menuMgr.addMenuListener(new IMenuListener() {
//			public void menuAboutToShow(final IMenuManager menuMgr2) {
////				fillContextMenu(menuMgr2);
//			}
//		});

		final Table table = _configViewer.getTable();
		final Menu tableContextMenu = menuMgr.createContextMenu(table);

		table.setMenu(tableContextMenu);

		_columnManager.createHeaderContextMenu(table, tableContextMenu);
	}

	/**
	 * Creates a configuration from the {@link #DEFAULT_VERTICES}.
	 * 
	 * @return Returns the created config.
	 */
	private ImportConfigItem createDefaultConfig() {

		final ImportConfigItem defaultConfig = new ImportConfigItem();
		final ArrayList<SpeedVertex> speedVertices = defaultConfig.speedVertices;

		for (final SpeedVertex speedVertex : DEFAULT_VERTICES) {
			speedVertices.add(speedVertex.clone());
		}

		_dialogConfig.configItems.add(defaultConfig);

		return defaultConfig;
	}

	@Override
	protected Control createDialogArea(final Composite parent) {

		_parent = parent;

		final Composite ui = (Composite) super.createDialogArea(parent);

		initUI(ui);
		createActions();

		createUI(ui);
		createMenus();

		return ui;
	}

	/**
	 * create the drop down menus, this must be created after the parent control is created
	 */
	private void createMenus() {

		/*
		 * Context menu: Tour type
		 */
		final MenuManager menuMgr = new MenuManager();
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			@Override
			public void menuAboutToShow(final IMenuManager menuMgr) {
				fillTourTypeMenu(menuMgr);
			}
		});
		final Menu ttContextMenu = menuMgr.createContextMenu(_linkOneTourType);
		_linkOneTourType.setMenu(ttContextMenu);
	}

	/**
	 * create the drop down menus, this must be created after the parent control is created
	 */

	private void createUI(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
		GridLayoutFactory.swtDefaults()//
				.applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GREEN));
		{
			final Composite configContainer = new Composite(container, SWT.NONE);
			GridDataFactory.fillDefaults()//
					.grab(true, true)
					.applyTo(configContainer);
			GridLayoutFactory.fillDefaults()//
					.numColumns(2)
					.applyTo(configContainer);
			{
				createUI_20_ConfigTitle(configContainer);
				createUI_22_ConfigViewer_Container(configContainer);
				createUI_24_ConfigActions(configContainer);
			}
			createUI_26_DragDropHint(container);

			final Group group = new Group(container, SWT.NONE);
			GridDataFactory.fillDefaults()//
					.grab(true, false)
//					.hint(SWT.DEFAULT, convertVerticalDLUsToPixels(188))
					.indent(0, convertVerticalDLUsToPixels(8))
					.applyTo(group);
			GridLayoutFactory.swtDefaults().numColumns(2).applyTo(group);
			group.setText(Messages.Dialog_ImportConfig_Group_Configuration);
//			vertexContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_CYAN));
			{
				createUI_40_Name(group);
				createUI_50_DeviceFolder(group);
				createUI_52_BackupFolder(group);
				createUI_80_TourType(group);
			}

			createUI_90_ConfigUI(container);
		}
	}

	private void createUI_20_ConfigTitle(final Composite parent) {

		final Label label = new Label(parent, SWT.NONE);
		GridDataFactory.fillDefaults().span(2, 1).applyTo(label);
		label.setText(Messages.Dialog_ImportConfig_Label_ConfigTitle);
	}

	private void createUI_22_ConfigViewer_Container(final Composite parent) {

		// define all columns for the viewer
		_columnManager = new ColumnManager(this, _state);
		defineAllColumns();

		_viewerContainer = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults()//
				.grab(true, true)
				.applyTo(_viewerContainer);
		GridLayoutFactory.fillDefaults().applyTo(_viewerContainer);
//		_viewerContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
		{
			createUI_24_ConfigViewer_Table(_viewerContainer);
		}
	}

	private void createUI_24_ConfigActions(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults()//
//				.grab(false, true)
				.applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
		{
			/*
			 * Button: New
			 */
			_btnConfig_New = new Button(container, SWT.NONE);
			_btnConfig_New.setText(Messages.App_Action_New);
			_btnConfig_New.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					onConfig_Add(false);
				}
			});
			setButtonLayoutData(_btnConfig_New);

			/*
			 * Button: Copy
			 */
			_btnConfig_Copy = new Button(container, SWT.NONE);
			_btnConfig_Copy.setText(Messages.App_Action_Copy);
			_btnConfig_Copy.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					onConfig_Add(true);
				}
			});
			setButtonLayoutData(_btnConfig_Copy);

			/*
			 * button: remove
			 */
			_btnConfig_Remove = new Button(container, SWT.NONE);
			_btnConfig_Remove.setText(Messages.App_Action_Remove_Immediate);
			_btnConfig_Remove.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					onConfig_Remove();
				}
			});
			setButtonLayoutData(_btnConfig_Remove);

			// align to the bottom
			final GridData gd = (GridData) _btnConfig_Remove.getLayoutData();
			gd.grabExcessVerticalSpace = true;
			gd.verticalAlignment = SWT.END;

		}
	}

	private void createUI_24_ConfigViewer_Table(final Composite parent) {

		/*
		 * Create tree
		 */
		final Table table = new Table(parent, //
				SWT.H_SCROLL //
						| SWT.V_SCROLL
						| SWT.BORDER
						| SWT.FULL_SELECTION);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(table);

		table.setHeaderVisible(true);

		/*
		 * NOTE: MeasureItem, PaintItem and EraseItem are called repeatedly. Therefore, it is
		 * critical for performance that these methods be as efficient as possible.
		 */
		final Listener paintListener = new Listener() {
			@Override
			public void handleEvent(final Event event) {

				if (event.type == SWT.MeasureItem || event.type == SWT.PaintItem) {
					onPaintViewer(event);
				}
			}
		};
		table.addListener(SWT.MeasureItem, paintListener);
		table.addListener(SWT.PaintItem, paintListener);

		/*
		 * Create tree viewer
		 */
		_configViewer = new TableViewer(table);

		_columnManager.createColumns(_configViewer);

		_columnIndexConfigImage = _colDefProfileImage.getCreateIndex();

		_configViewer.setUseHashlookup(true);
		_configViewer.setContentProvider(new ClientsContentProvider());

		_configViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(final SelectionChangedEvent event) {
				onSelectConfig(event.getSelection());
			}
		});

		_configViewer.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(final DoubleClickEvent event) {
				onConfig_DblClick();
			}
		});

		createContextMenu();

		/*
		 * set drag adapter
		 */
		_configViewer.addDragSupport(
				DND.DROP_MOVE,
				new Transfer[] { LocalSelectionTransfer.getTransfer() },
				new DragSourceListener() {

					@Override
					public void dragFinished(final DragSourceEvent event) {

						final LocalSelectionTransfer transfer = LocalSelectionTransfer.getTransfer();

						if (event.doit == false) {
							return;
						}

						transfer.setSelection(null);
						transfer.setSelectionSetTime(0);
					}

					@Override
					public void dragSetData(final DragSourceEvent event) {
						// data are set in LocalSelectionTransfer
					}

					@Override
					public void dragStart(final DragSourceEvent event) {

						final LocalSelectionTransfer transfer = LocalSelectionTransfer.getTransfer();
						final ISelection selection = _configViewer.getSelection();

						transfer.setSelection(selection);
						transfer.setSelectionSetTime(_dragStart = event.time & 0xFFFFFFFFL);

						event.doit = !selection.isEmpty();
					}
				});

		/*
		 * set drop adapter
		 */
		final ViewerDropAdapter viewerDropAdapter = new ViewerDropAdapter(_configViewer) {

			private Widget	_tableItem;

			@Override
			public void dragOver(final DropTargetEvent dropEvent) {

				// keep table item
				_tableItem = dropEvent.item;

				super.dragOver(dropEvent);
			}

			@Override
			public boolean performDrop(final Object data) {

				if (data instanceof StructuredSelection) {
					final StructuredSelection selection = (StructuredSelection) data;

					if (selection.getFirstElement() instanceof ImportConfigItem) {

						final ImportConfigItem filterItem = (ImportConfigItem) selection.getFirstElement();

						final int location = getCurrentLocation();
						final Table filterTable = _configViewer.getTable();

						/*
						 * check if drag was startet from this filter, remove the filter item before
						 * the new filter is inserted
						 */
						if (LocalSelectionTransfer.getTransfer().getSelectionSetTime() == _dragStart) {
							_configViewer.remove(filterItem);
						}

						int filterIndex;

						if (_tableItem == null) {

							_configViewer.add(filterItem);
							filterIndex = filterTable.getItemCount() - 1;

						} else {

							// get index of the target in the table
							filterIndex = filterTable.indexOf((TableItem) _tableItem);
							if (filterIndex == -1) {
								return false;
							}

							if (location == LOCATION_BEFORE) {
								_configViewer.insert(filterItem, filterIndex);
							} else if (location == LOCATION_AFTER || location == LOCATION_ON) {
								_configViewer.insert(filterItem, ++filterIndex);
							}
						}

						// reselect filter item
						_configViewer.setSelection(new StructuredSelection(filterItem));

						// set focus to selection
						filterTable.setSelection(filterIndex);
						filterTable.setFocus();

						return true;
					}
				}

				return false;
			}

			@Override
			public boolean validateDrop(final Object target, final int operation, final TransferData transferType) {

				final ISelection selection = LocalSelectionTransfer.getTransfer().getSelection();
				if (selection instanceof StructuredSelection) {
					final Object dragFilter = ((StructuredSelection) selection).getFirstElement();
					if (target == dragFilter) {
						return false;
					}
				}

				if (LocalSelectionTransfer.getTransfer().isSupportedType(transferType) == false) {
					return false;
				}

				return true;
			}

		};

		_configViewer.addDropSupport(
				DND.DROP_MOVE,
				new Transfer[] { LocalSelectionTransfer.getTransfer() },
				viewerDropAdapter);
	}

	private void createUI_26_DragDropHint(final Composite parent) {

		final Label label = new Label(parent, SWT.WRAP);
//		GridDataFactory.fillDefaults().span(2, 1).applyTo(label);
		label.setText(Messages.Dialog_ImportConfig_Info_ConfigDragDrop);
	}

	private void createUI_40_Name(final Composite parent) {

		/*
		 * Config name
		 */

		// label
		_lblConfigName = new Label(parent, SWT.NONE);
		GridDataFactory.fillDefaults()//
				.align(SWT.FILL, SWT.CENTER)
				.applyTo(_lblConfigName);
		_lblConfigName.setText(Messages.Dialog_ImportConfig_Label_ConfigName);

		// text
		_txtConfigName = new Text(parent, SWT.BORDER);
		GridDataFactory.fillDefaults()//
				.grab(true, false)
				.applyTo(_txtConfigName);

		_txtConfigName.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(final ModifyEvent e) {
				onConfig_ModifyName();
			}
		});
	}

	private void createUI_50_DeviceFolder(final Composite parent) {

		/*
		 * Label: device folder
		 */
		_lblDeviceFolder = new Label(parent, SWT.NONE);
		GridDataFactory.fillDefaults()//
				.align(SWT.FILL, SWT.CENTER)
				.applyTo(_lblDeviceFolder);
		_lblDeviceFolder.setText(Messages.Dialog_ImportConfig_Label_DeviceFolder);

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
		{
			/*
			 * Combo: path
			 */
			_comboDevicePath = new Combo(container, SWT.SINGLE | SWT.BORDER);
			GridDataFactory.fillDefaults()//
					.grab(true, false)
					.align(SWT.FILL, SWT.CENTER)
//					.hint(_pc.convertWidthInCharsToPixels(40), SWT.DEFAULT)
					.applyTo(_comboDevicePath);
			_comboDevicePath.setVisibleItemCount(20);
			_comboDevicePath.addModifyListener(new ModifyListener() {
				@Override
				public void modifyText(final ModifyEvent e) {
					validateFields();
				}
			});
			_comboDevicePath.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					validateFields();
				}
			});

			/*
			 * Button: browse...
			 */
			_btnSelectDeviceFolder = new Button(container, SWT.PUSH);
			GridDataFactory.fillDefaults()//
					.align(SWT.FILL, SWT.CENTER)
					.applyTo(_btnSelectDeviceFolder);
			_btnSelectDeviceFolder.setText(Messages.app_btn_browse);
			_btnSelectDeviceFolder.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					onData_SelectBrowseDirectory(e.widget);
					validateFields();
				}
			});
			setButtonLayoutData(_btnSelectDeviceFolder);
		}
	}

	private void createUI_52_BackupFolder(final Composite parent) {

		/*
		 * Label: Backup folder
		 */
		_lblBackupFolder = new Label(parent, SWT.NONE);
		GridDataFactory.fillDefaults()//
				.align(SWT.FILL, SWT.CENTER)
				.applyTo(_lblBackupFolder);
		_lblBackupFolder.setText(Messages.Dialog_ImportConfig_Label_BackupFolder);

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
		{
			/*
			 * Combo: path
			 */
			_comboBackupPath = new Combo(container, SWT.SINGLE | SWT.BORDER);
			GridDataFactory.fillDefaults()//
					.grab(true, false)
					.align(SWT.FILL, SWT.CENTER)
					.applyTo(_comboBackupPath);
			_comboBackupPath.setVisibleItemCount(20);
			_comboBackupPath.addModifyListener(new ModifyListener() {
				@Override
				public void modifyText(final ModifyEvent e) {
					validateFields();
				}
			});
			_comboBackupPath.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					validateFields();
				}
			});

			/*
			 * Button: browse...
			 */
			_btnSelectBackupFolder = new Button(container, SWT.PUSH);
			GridDataFactory.fillDefaults()//
					.align(SWT.FILL, SWT.CENTER)
					.applyTo(_btnSelectBackupFolder);
			_btnSelectBackupFolder.setText(Messages.app_btn_browse);
			_btnSelectBackupFolder.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					onData_SelectBrowseDirectory(e.widget);
					validateFields();
				}
			});
			setButtonLayoutData(_btnSelectBackupFolder);
		}
	}

	private void createUI_80_TourType(final Composite parent) {

		_lblTourType = new Label(parent, SWT.NONE);
		GridDataFactory.fillDefaults()//
				.align(SWT.FILL, SWT.CENTER)
				.applyTo(_lblTourType);
		_lblTourType.setText(Messages.Dialog_ImportConfig_Label_TourType);

		_comboTourTypeConfig = new Combo(parent, SWT.READ_ONLY);
		_comboTourTypeConfig.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onSelectTourType();
			}
		});

		// fill combo
		for (final ComboEnumEntry<?> tourTypeItem : RawDataManager.ALL_IMPORT_TOUR_TYPE_CONFIG) {
			_comboTourTypeConfig.add(tourTypeItem.label);
		}

		// fill left column
		new Label(parent, SWT.NONE);

		_pagebookTourType = new PageBook(parent, SWT.NONE);
		GridDataFactory.fillDefaults()//
				.grab(true, true)
				.hint(SWT.DEFAULT, convertVerticalDLUsToPixels(96))
				.indent(0, 8)
				.applyTo(_pagebookTourType);
		{
			_pageTourType_NotUsed = createUI_81_Page_NotUsed(_pagebookTourType);
			_pageTourType_OneForAll = createUI_82_Page_OneForAll(_pagebookTourType);
			_pageTourType_BySpeed = createUI_83_Page_BySpeed(_pagebookTourType);
		}
	}

	/**
	 * Page: Not used
	 * 
	 * @return
	 */
	private Label createUI_81_Page_NotUsed(final PageBook parent) {

		final Label label = new Label(parent, SWT.NONE);
		GridDataFactory.fillDefaults().applyTo(label);
		label.setText(UI.EMPTY_STRING);

		return label;
	}

	private Composite createUI_82_Page_OneForAll(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
		{
			_lblOneTourTypeIcon = new Label(container, SWT.NONE);
			GridDataFactory.fillDefaults()//
					.hint(16, 16)
					.applyTo(_lblOneTourTypeIcon);
			_lblOneTourTypeIcon.setText(UI.EMPTY_STRING);

			/*
			 * tour type
			 */
			_linkOneTourType = new Link(container, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(_linkOneTourType);
			_linkOneTourType.setText(Messages.Dialog_ImportConfig_Link_TourType);
			_linkOneTourType.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					net.tourbook.common.UI.openControlMenu(_linkOneTourType);
				}
			});
		}

		return container;
	}

	private Composite createUI_83_Page_BySpeed(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
		{

			createUI_84_Vertices(container);
			createUI_86_VertexActions(container);
		}

		return container;
	}

	private void createUI_84_Vertices(final Composite parent) {

		/*
		 * vertex fields container
		 */
		_vertexOuterContainer = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults()//
				.grab(true, true)
//				.indent(_pc.convertHorizontalDLUsToPixels(10), 0)
				.applyTo(_vertexOuterContainer);

		GridLayoutFactory.fillDefaults().applyTo(_vertexOuterContainer);
//		_vertexOuterContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));

		createUI_88_VertexFields();
	}

	private void createUI_86_VertexActions(final Composite parent) {

		final ToolBar toolbar = new ToolBar(parent, SWT.FLAT);

		final ToolBarManager tbm = new ToolBarManager(toolbar);

		tbm.add(_action_VertexAdd);
		tbm.add(_action_VertexSort);

		tbm.update(true);
	}

	/**
	 * Create the vertex fields from the vertex list
	 * 
	 * @param parent
	 */
	private void createUI_88_VertexFields() {

		if (_currentConfigItem == null) {

			updateUI_ClearVertices();

			return;
		}

		final int vertexSize = _currentConfigItem.speedVertices.size();

		// check if required vertex fields are already available
		if (_spinnerVertex_AvgSpeed != null && _spinnerVertex_AvgSpeed.length == vertexSize) {
			return;
		}

		Point scrollOrigin = null;

		// dispose previous content
		if (_vertexScrolledContainer != null) {

			// get current scroll position
			scrollOrigin = _vertexScrolledContainer.getOrigin();

			_vertexScrolledContainer.dispose();
		}

		_vertexContainer = createUI_89_VertexScrolledContainer(_vertexOuterContainer);
//		_vertexContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_GREEN));

		/*
		 * fields
		 */
		_actionVertex_Delete = new ActionDeleteVertex[vertexSize];
		_lblVertex_TourTypeIcon = new Label[vertexSize];
		_lblVertex_SpeedUnit = new Label[vertexSize];
		_linkVertex_TourType = new Link[vertexSize];
		_spinnerVertex_AvgSpeed = new Spinner[vertexSize];

		_vertexContainer.setRedraw(false);
		{
			for (int vertexIndex = 0; vertexIndex < vertexSize; vertexIndex++) {

				/*
				 * Spinner: Speed value
				 */
				final Spinner spinnerValue = new Spinner(_vertexContainer, SWT.BORDER);
				GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(spinnerValue);
				spinnerValue.setMinimum(RawDataManager.CONFIG_SPEED_MIN);
				spinnerValue.setMaximum(RawDataManager.CONFIG_SPEED_MAX);
				spinnerValue.setToolTipText(Messages.Dialog_ImportConfig_Spinner_Speed_Tooltip);
				spinnerValue.addMouseWheelListener(_defaultMouseWheelListener);

				/*
				 * Label: Speed unit
				 */
				final Label lblUnit = new Label(_vertexContainer, SWT.NONE);
				GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(lblUnit);
				lblUnit.setText(UI.UNIT_LABEL_SPEED);

				/*
				 * Label with icon: Tour type (CLabel cannot be disabled !!!)
				 */
				final Label lblTourTypeIcon = new Label(_vertexContainer, SWT.NONE);
				GridDataFactory.fillDefaults()//
						.align(SWT.FILL, SWT.CENTER)
						.hint(16, 16)
						.applyTo(lblTourTypeIcon);

				/*
				 * Link: Tour type
				 */
				final Link linkTourType = new Link(_vertexContainer, SWT.NONE);
				GridDataFactory.fillDefaults()//
						.grab(true, false)
						.align(SWT.FILL, SWT.CENTER)
						.applyTo(linkTourType);
				linkTourType.setText(Messages.tour_editor_label_tour_type);
				linkTourType.addSelectionListener(_vertexTourTypeListener);

				/*
				 * Context menu: Tour type
				 */
				final MenuManager menuMgr = new MenuManager();
				menuMgr.setRemoveAllWhenShown(true);
				menuMgr.addMenuListener(new IMenuListener() {
					@Override
					public void menuAboutToShow(final IMenuManager menuMgr) {
						fillTourTypeMenu(menuMgr, linkTourType);
					}
				});
				final Menu ttContextMenu = menuMgr.createContextMenu(linkTourType);
				linkTourType.setMenu(ttContextMenu);

				/*
				 * Action: Delete vertex
				 */
				final ActionDeleteVertex actionDeleteVertex = new ActionDeleteVertex();
				createUI_ActionButton(_vertexContainer, actionDeleteVertex);

				/*
				 * Keep vertex controls
				 */
				_actionVertex_Delete[vertexIndex] = actionDeleteVertex;
				_lblVertex_TourTypeIcon[vertexIndex] = lblTourTypeIcon;
				_lblVertex_SpeedUnit[vertexIndex] = lblUnit;
				_linkVertex_TourType[vertexIndex] = linkTourType;
				_spinnerVertex_AvgSpeed[vertexIndex] = spinnerValue;
			}
		}
		_vertexContainer.setRedraw(true);

		_vertexOuterContainer.layout(true);

		// set scroll position to previous position
		if (scrollOrigin != null) {
			_vertexScrolledContainer.setOrigin(scrollOrigin);
		}
	}

	private Composite createUI_89_VertexScrolledContainer(final Composite parent) {

		// scrolled container
		_vertexScrolledContainer = new ScrolledComposite(parent, SWT.V_SCROLL);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(_vertexScrolledContainer);
		_vertexScrolledContainer.setExpandVertical(true);
		_vertexScrolledContainer.setExpandHorizontal(true);

		// vertex container
		final Composite vertexContainer = new Composite(_vertexScrolledContainer, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(vertexContainer);
		GridLayoutFactory.fillDefaults()//
				.numColumns(5)
				.applyTo(vertexContainer);
//		vertexContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_GRAY));

		_vertexScrolledContainer.setContent(vertexContainer);
		_vertexScrolledContainer.addControlListener(new ControlAdapter() {
			@Override
			public void controlResized(final ControlEvent e) {
				_vertexScrolledContainer.setMinSize(vertexContainer.computeSize(SWT.DEFAULT, SWT.DEFAULT));
			}
		});

		return vertexContainer;
	}

	private void createUI_90_ConfigUI(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults()//
				.grab(true, false)
				.indent(0, 10)
				.applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
		{
			{
				/*
				 * Number of columns
				 */
				// label
				final Label label = new Label(container, SWT.NONE);
				label.setText(Messages.Dialog_ImportConfig_Label_ImportColumns);
				label.setToolTipText(Messages.Dialog_ImportConfig_Label_ImportColumns_Tooltip);
				GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(label);

				// spinner
				_spinnerUIColumns = new Spinner(container, SWT.BORDER);
				_spinnerUIColumns.setMinimum(1);
				_spinnerUIColumns.setMaximum(10);
				_spinnerUIColumns.addMouseWheelListener(_defaultMouseWheelListener);
				GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(_spinnerUIColumns);
			}
			{
				/*
				 * Background opacity
				 */
				// label
				final Label label = new Label(container, SWT.NONE);
				label.setText(Messages.Dialog_ImportConfig_Label_ImportBackgroundOpacity);
				label.setToolTipText(Messages.Dialog_ImportConfig_Label_ImportBackgroundOpacity_Tooltip);
				GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(label);

				// spinner
				_spinnerBgOpacity = new Spinner(container, SWT.BORDER);
				_spinnerBgOpacity.setMinimum(0);
				_spinnerBgOpacity.setMaximum(100);
				_spinnerBgOpacity.addMouseWheelListener(new MouseWheelListener() {
					@Override
					public void mouseScrolled(final MouseEvent event) {
						UI.adjustSpinnerValueOnMouseScroll(event);
						onSelectBackgroundOpacity();
					}
				});
				_spinnerBgOpacity.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(final SelectionEvent e) {
						onSelectBackgroundOpacity();
					}
				});
				GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(_spinnerBgOpacity);
			}
		}
	}

	/**
	 * Creates an action in a toolbar.
	 * 
	 * @param parent
	 * @param action
	 */
	private void createUI_ActionButton(final Composite parent, final Action action) {

		final ToolBar toolbar = new ToolBar(parent, SWT.FLAT);

		final ToolBarManager tbm = new ToolBarManager(toolbar);
		tbm.add(action);
		tbm.update(true);
	}

	private void defineAllColumns() {

		defineColumn_10_ProfileName();
		defineColumn_20_ColorImage();
	}

	/**
	 * column: profile name
	 */
	private void defineColumn_10_ProfileName() {

		final TableColumnDefinition colDef = new TableColumnDefinition(_columnManager, "configName", SWT.LEAD); //$NON-NLS-1$

		colDef.setColumnLabel(Messages.Dialog_ImportConfig_Column_Name);
		colDef.setColumnHeaderText(Messages.Dialog_ImportConfig_Column_Name);

		colDef.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(10));
		colDef.setColumnWeightData(new ColumnWeightData(10));

		colDef.setIsDefaultColumn();
		colDef.setIsColumnMoveable(false);
		colDef.setCanModifyVisibility(false);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				cell.setText(((ImportConfigItem) cell.getElement()).name);
			}
		});
	}

	/**
	 * Column: Color image
	 */
	private void defineColumn_20_ColorImage() {

		final TableColumnDefinition colDef = new TableColumnDefinition(_columnManager, "colorImage", SWT.LEAD); //$NON-NLS-1$
		_colDefProfileImage = colDef;

		colDef.setColumnLabel(Messages.Dialog_ImportConfig_Column_TourType);
		colDef.setColumnHeaderText(Messages.Dialog_ImportConfig_Column_TourType);

		colDef.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(20));
		colDef.setColumnWeightData(new ColumnWeightData(3));

		colDef.setIsDefaultColumn();
		colDef.setIsColumnMoveable(false);
		colDef.setCanModifyVisibility(false);
		colDef.setLabelProvider(new CellLabelProvider() {

			// !!! set dummy label provider, otherwise an error occures !!!
			@Override
			public void update(final ViewerCell cell) {}
		});

		colDef.setControlListener(new ControlAdapter() {
			@Override
			public void controlResized(final ControlEvent e) {
//				onResizeImageColumn();
			}
		});
	}

	private void disposeConfigImages() {

		for (final Image configImage : _configImages.values()) {

			if (configImage != null) {
				configImage.dispose();
			}
		}

		_configImages.clear();
		_configImageHash.clear();
	}

	private void enableControls() {

		final Object selectedConfig = ((StructuredSelection) _configViewer.getSelection()).getFirstElement();

		final boolean isConfigSelected = selectedConfig != null;

		final Enum<TourTypeConfig> selectedTourTypeConfig = getSelectedTourTypeConfig();
		if (selectedTourTypeConfig.equals(TourTypeConfig.TOUR_TYPE_CONFIG_BY_SPEED)) {

			if (_actionVertex_Delete != null) {

				for (final ActionDeleteVertex action : _actionVertex_Delete) {
					action.setEnabled(isConfigSelected);
				}

				for (final Spinner spinner : _spinnerVertex_AvgSpeed) {
					spinner.setEnabled(isConfigSelected);
				}

				for (final Link link : _linkVertex_TourType) {
					link.setEnabled(isConfigSelected);
				}

				for (final Label label : _lblVertex_SpeedUnit) {
					label.setEnabled(isConfigSelected);
				}

				for (final Label label : _lblVertex_TourTypeIcon) {

					if (isConfigSelected) {

						final Integer vertexIndex = (Integer) label.getData(DATA_KEY_VERTEX_INDEX);

						final SpeedVertex vertex = _currentConfigItem.speedVertices.get(vertexIndex);
						final long tourTypeId = vertex.tourTypeId;

						label.setImage(net.tourbook.ui.UI.getInstance().getTourTypeImage(tourTypeId));

					} else {

						// the disabled image looks very ugly
						label.setImage(null);
					}
				}
			}

			_action_VertexAdd.setEnabled(isConfigSelected);
			_action_VertexSort.setEnabled(isConfigSelected && _spinnerVertex_AvgSpeed.length > 1);

		} else if (selectedTourTypeConfig.equals(TourTypeConfig.TOUR_TYPE_CONFIG_ONE_FOR_ALL)) {

			_linkOneTourType.setEnabled(isConfigSelected);

		} else {

			// this is the default or TourTypeConfig.TOUR_TYPE_CONFIG_NOT_USED

		}

		_btnConfig_Copy.setEnabled(isConfigSelected);
		_btnConfig_Remove.setEnabled(isConfigSelected);
		_btnSelectBackupFolder.setEnabled(isConfigSelected);
		_btnSelectDeviceFolder.setEnabled(isConfigSelected);

		_comboBackupPath.setEnabled(isConfigSelected);
		_comboDevicePath.setEnabled(isConfigSelected);
		_comboTourTypeConfig.setEnabled(isConfigSelected);

		_lblBackupFolder.setEnabled(isConfigSelected);
		_lblConfigName.setEnabled(isConfigSelected);
		_lblDeviceFolder.setEnabled(isConfigSelected);
		_lblTourType.setEnabled(isConfigSelected);

		_txtConfigName.setEnabled(isConfigSelected);
	}

	private void fillTourTypeMenu(final IMenuManager menuMgr) {

		// get tour type which will be checked in the menu
		final TourType checkedTourType = _currentConfigItem.oneTourType;

		// add all tour types to the menu
		final ArrayList<TourType> tourTypes = TourDatabase.getAllTourTypes();

		for (final TourType tourType : tourTypes) {

			boolean isChecked = false;
			if (checkedTourType != null && checkedTourType.getTypeId() == tourType.getTypeId()) {
				isChecked = true;
			}

			final ActionSetOneTourType action = new ActionSetOneTourType(tourType, isChecked);

			menuMgr.add(action);
		}

		menuMgr.add(new Separator());
		menuMgr.add(_actionOpenTourTypePrefs);
	}

	private void fillTourTypeMenu(final IMenuManager menuMgr, final Link linkTourType) {

		// get tour type which will be checked in the menu
		final TourType checkedTourType = null;

		final int vertexIndex = (int) linkTourType.getData(DATA_KEY_VERTEX_INDEX);

		// add all tour types to the menu
		final ArrayList<TourType> tourTypes = TourDatabase.getAllTourTypes();

		for (final TourType tourType : tourTypes) {

			boolean isChecked = false;
			if (checkedTourType != null && checkedTourType.getTypeId() == tourType.getTypeId()) {
				isChecked = true;
			}

			final ActionSetSpeedTourType action = new ActionSetSpeedTourType(tourType, isChecked, vertexIndex);

			menuMgr.add(action);
		}

		menuMgr.add(new Separator());
		menuMgr.add(_actionOpenTourTypePrefs);
	}

	@Override
	public ColumnManager getColumnManager() {
		return _columnManager;
	}

	@Override
	protected IDialogSettings getDialogBoundsSettings() {

		// keep window size and position
		return _state;
//		return null;
	}

	public ImportConfig getModifiedConfig() {
		return _dialogConfig;
	}

	@SuppressWarnings("unchecked")
	private Enum<TourTypeConfig> getSelectedTourTypeConfig() {

		final int configIndex = _comboTourTypeConfig.getSelectionIndex();

		final ComboEnumEntry<?> selectedItem = RawDataManager.ALL_IMPORT_TOUR_TYPE_CONFIG[configIndex];

		return (Enum<TourTypeConfig>) selectedItem.value;
	}

	private int getTourTypeConfigIndex(final Enum<TourTypeConfig> tourTypeConfig) {

		if (tourTypeConfig != null) {

			final ComboEnumEntry<?>[] allImportTourTypeConfig = RawDataManager.ALL_IMPORT_TOUR_TYPE_CONFIG;

			for (int configIndex = 0; configIndex < allImportTourTypeConfig.length; configIndex++) {

				final ComboEnumEntry<?> tourtypeConfig = allImportTourTypeConfig[configIndex];

				if (tourtypeConfig.value.equals(tourTypeConfig)) {
					return configIndex;
				}
			}
		}

		return 0;
	}

	@Override
	public ColumnViewer getViewer() {
		return _configViewer;
	}

	private void initUI(final Composite parent) {

		_pc = new PixelConverter(parent);

		parent.addDisposeListener(new DisposeListener() {

			@Override
			public void widgetDisposed(final DisposeEvent e) {
				onDispose();
			}
		});

		/*
		 * Field listener
		 */
		_defaultSelectionListener = new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onSelectDefault();
			}
		};

		/*
		 * Vertex listener
		 */
		_defaultMouseWheelListener = new MouseWheelListener() {
			@Override
			public void mouseScrolled(final MouseEvent event) {
				UI.adjustSpinnerValueOnMouseScroll(event);
			}
		};

		_vertexTourTypeListener = new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent event) {
				UI.openControlMenu((Link) event.widget);
			}
		};
	}

	@Override
	protected void okPressed() {

		update_Model_From_UI();

		super.okPressed();
	}

	private void onConfig_Add(final boolean isCopy) {

		// keep modifications
		update_Model_From_UI();

		// update model
		final ArrayList<ImportConfigItem> configItems = _dialogConfig.configItems;
		ImportConfigItem newConfig;

		if (configItems.size() == 0) {

			/*
			 * Setup default config
			 */

			newConfig = createDefaultConfig();

		} else {

			if (isCopy) {

				newConfig = _currentConfigItem.clone();

				// make the close more visible
				newConfig.name = newConfig.name + UI.SPACE + newConfig.getCreateId();

			} else {

				newConfig = new ImportConfigItem();
			}

			configItems.add(newConfig);
		}

		// update UI
		_configViewer.refresh();

		// prevent that the horizontal scrollbar is visible
		_configViewer.getTable().getParent().layout();

		_configViewer.setSelection(new StructuredSelection(newConfig), true);

		_txtConfigName.setFocus();

		if (isCopy) {
			_txtConfigName.selectAll();
		}
	}

	private void onConfig_DblClick() {

		_txtConfigName.setFocus();
		_txtConfigName.selectAll();
	}

	private void onConfig_ModifyName() {

		if (_currentConfigItem == null) {
			return;
		}

		// update model
		_currentConfigItem.name = _txtConfigName.getText();

		// update UI
		_configViewer.update(_currentConfigItem, null);
	}

	private void onConfig_Remove() {

		final StructuredSelection selection = (StructuredSelection) _configViewer.getSelection();
		final ImportConfigItem selectedConfig = (ImportConfigItem) selection.getFirstElement();

		int selectedIndex = 0;
		final ArrayList<ImportConfigItem> configItems = _dialogConfig.configItems;

		// get index of the selected config
		for (int configIndex = 0; configIndex < configItems.size(); configIndex++) {

			final ImportConfigItem config = configItems.get(configIndex);

			if (config.equals(selectedConfig)) {
				selectedIndex = configIndex;
				break;
			}
		}

		// update model
		configItems.remove(selectedIndex);

		// update UI
		_configViewer.refresh();

		// select config at the same position

		if (configItems.size() == 0) {

			// all configs are removed, setup empty UI

			_currentConfigItem = null;

			_comboDevicePath.setText(UI.EMPTY_STRING);
			_txtConfigName.setText(UI.EMPTY_STRING);

			// remove vertex fields
			createUI_88_VertexFields();

			enableControls();

		} else {

			if (selectedIndex >= configItems.size()) {
				selectedIndex--;
			}

			final ImportConfigItem nextConfig = configItems.get(selectedIndex);

			_configViewer.setSelection(new StructuredSelection(nextConfig), true);
		}

		_configViewer.getTable().setFocus();
	}

	private void onData_SelectBrowseDirectory(final Widget widget) {

		final DirectoryDialog dialog = new DirectoryDialog(_parent.getShell(), SWT.SAVE);
		dialog.setText(Messages.dialog_export_dir_dialog_text);
		dialog.setMessage(Messages.dialog_export_dir_dialog_message);

//		dialog.setFilterPath(getExportPathName());

		final String selectedDirectoryName = dialog.open();

		if (selectedDirectoryName != null) {
			setErrorMessage(null);
			_comboDevicePath.setText(selectedDirectoryName);
		}
	}

	private void onDispose() {

		disposeConfigImages();
	}

	private void onPaintViewer(final Event event) {

		if (event.index != _columnIndexConfigImage) {
			return;
		}

		final TableItem item = (TableItem) event.item;
		final ImportConfigItem importConfig = (ImportConfigItem) item.getData();

		switch (event.type) {
		case SWT.MeasureItem:

			/*
			 * Set height also for color def, when not set and all is collapsed, the color def size
			 * will be adjusted when an item is expanded.
			 */

			event.width += importConfig.imageWidth;
//			event.height = PROFILE_IMAGE_HEIGHT;

			break;

		case SWT.PaintItem:

			final Image image = _rawDataView.getImportConfigImage(importConfig);

			if (image != null && !image.isDisposed()) {

				final Rectangle rect = image.getBounds();

				final int x = event.x + event.width;
				final int yOffset = Math.max(0, (event.height - rect.height) / 2);

				event.gc.drawImage(image, x, event.y + yOffset);
			}

			break;
		}
	}

	/**
	 * Do live update for this feature.
	 */
	private void onSelectBackgroundOpacity() {

		final int selectedOpacity = _spinnerBgOpacity.getSelection();

		_rawDataView.doLiveUpdate(selectedOpacity);
	}

	private void onSelectConfig(final ISelection selection) {

		final ImportConfigItem selectedConfig = (ImportConfigItem) ((StructuredSelection) selection).getFirstElement();

		if (_currentConfigItem == selectedConfig) {
			// this is already selected
			return;
		}

		// update model from the old selected config
		update_Model_From_UI();

		// set new model
		_currentConfigItem = selectedConfig;

		update_UI_From_Model();

		enableControls();
	}

	private void onSelectDefault() {

		enableControls();
	}

	private void onSelectTourType() {

		final Enum<TourTypeConfig> selectedConfig = getSelectedTourTypeConfig();

		selectTourTypePage(selectedConfig);

		update_Model_From_UI();
		update_UI_From_Model();
	}

	private void onVertex_Add() {

		update_Model_From_UI();

		final ArrayList<SpeedVertex> speedVertices = _currentConfigItem.speedVertices;

		// update model
		speedVertices.add(0, new SpeedVertex());

		// sort vertices by value
		Collections.sort(speedVertices);

		// update UI + model
		update_UI_From_Model();

		enableControls();

		// set focus to the new vertex
		_spinnerVertex_AvgSpeed[0].setFocus();
	}

	private void onVertex_Remove(final int vertexIndex) {

		// update model
		update_Model_From_UI();

		final ArrayList<SpeedVertex> speedVertices = _currentConfigItem.speedVertices;

		final SpeedVertex removedVertex = speedVertices.get(vertexIndex);

		speedVertices.remove(removedVertex);

		// update UI
		update_UI_From_Model();

		enableControls();
	}

	private void onVertex_SetTourType(final int vertexIndex, final TourType tourType) {

		/*
		 * Update UI
		 */
		final Image image = net.tourbook.ui.UI.getInstance().getTourTypeImage(tourType.getTypeId());
		final Label ttIcon = _lblVertex_TourTypeIcon[vertexIndex];
		final Link ttLink = _linkVertex_TourType[vertexIndex];

		ttIcon.setImage(image);

		ttLink.setText(UI.LINK_TAG_START + tourType.getName() + UI.LINK_TAG_END);
		ttLink.setData(DATA_KEY_TOUR_TYPE_ID, tourType.getTypeId());

		_vertexOuterContainer.layout();
	}

	private void onVertex_Sort() {

		update_Model_From_UI();
		update_UI_From_Model();
	}

	@Override
	public ColumnViewer recreateViewer(final ColumnViewer columnViewer) {

		_viewerContainer.setRedraw(false);
		{
			final ISelection selection = _configViewer.getSelection();

			_configViewer.getTable().dispose();

			createUI_24_ConfigViewer_Table(_viewerContainer);
			_viewerContainer.layout();

			// update viewer
			reloadViewer();

			_configViewer.setSelection(selection);
		}
		_viewerContainer.setRedraw(true);

		return _configViewer;
	}

	@Override
	public void reloadViewer() {

		_configViewer.setInput(this);
	}

	private void restoreState() {

		/*
		 * Reselect previous selected config
		 */
		final String stateConfigName = Util.getStateString(_state, STATE_SELECTED_CONFIG_NAME, UI.EMPTY_STRING);
		final ArrayList<ImportConfigItem> configItems = _dialogConfig.configItems;

		for (final ImportConfigItem config : configItems) {

			if (config.name.equals(stateConfigName)) {

				_initialConfig = config;
				break;
			}
		}

		if (_initialConfig == null) {

			_initialConfig = configItems.get(0);
		}

		_spinnerBgOpacity.setSelection(_dialogConfig.backgroundOpacity);
		_spinnerUIColumns.setSelection(_dialogConfig.numUIColumns);
	}

	private void saveState() {

		_state.put(STATE_SELECTED_CONFIG_NAME, _currentConfigItem == null ? UI.EMPTY_STRING : _currentConfigItem.name);

		_columnManager.saveState(_state);

		/*
		 * Create config list in the table sort order
		 */
		final ArrayList<ImportConfigItem> tableConfigs = new ArrayList<>();

		for (final TableItem tableItem : _configViewer.getTable().getItems()) {

			final Object itemData = tableItem.getData();

			if (itemData instanceof ImportConfigItem) {
				tableConfigs.add((ImportConfigItem) itemData);
			}
		}

		final ArrayList<ImportConfigItem> configItems = _dialogConfig.configItems;
		configItems.clear();
		configItems.addAll(tableConfigs);

		_dialogConfig.backgroundOpacity = _spinnerBgOpacity.getSelection();
		_dialogConfig.numUIColumns = _spinnerUIColumns.getSelection();
	}

	private void selectTourTypePage(final Enum<TourTypeConfig> selectedConfig) {

		if (TourTypeConfig.TOUR_TYPE_CONFIG_BY_SPEED.equals(selectedConfig)) {

			_pagebookTourType.showPage(_pageTourType_BySpeed);

		} else if (TourTypeConfig.TOUR_TYPE_CONFIG_ONE_FOR_ALL.equals(selectedConfig)) {

			_pagebookTourType.showPage(_pageTourType_OneForAll);

		} else {

			// this is the default or TourTypeConfig.TOUR_TYPE_CONFIG_NOT_USED

			_pagebookTourType.showPage(_pageTourType_NotUsed);
		}
	}

	private void setupConfigs(final ImportConfig importConfig) {

		/*
		 * Clone original configs, only the backup will be modified in the dialog.
		 */
		_dialogConfig = new ImportConfig();
		final ArrayList<ImportConfigItem> configItems = _dialogConfig.configItems = new ArrayList<>();

		for (final ImportConfigItem autoImportConfig : importConfig.configItems) {
			configItems.add(autoImportConfig.clone());
		}

		if (configItems.size() == 0) {

			// Setup default config
			createDefaultConfig();
		}

		_dialogConfig.numUIColumns = importConfig.numUIColumns;
		_dialogConfig.backgroundOpacity = importConfig.backgroundOpacity;
	}

	/**
	 * Set data from the UI into the model.
	 */
	private void update_Model_From_UI() {

		if (_currentConfigItem == null) {
			return;
		}

		final Enum<TourTypeConfig> selectedTourTypeConfig = getSelectedTourTypeConfig();

		_currentConfigItem.backupFolder = _comboBackupPath.getText();
		_currentConfigItem.deviceFolder = _comboDevicePath.getText();
		_currentConfigItem.tourTypeConfig = selectedTourTypeConfig;
		_currentConfigItem.name = _txtConfigName.getText();

		/*
		 * Set tour type data
		 */
		if (selectedTourTypeConfig.equals(TourTypeConfig.TOUR_TYPE_CONFIG_BY_SPEED)) {

			final ArrayList<SpeedVertex> speedVertices = _currentConfigItem.speedVertices;

			if (_spinnerVertex_AvgSpeed != null) {

				final ArrayList<SpeedVertex> newVertices = new ArrayList<SpeedVertex>();

				for (int vertexIndex = 0; vertexIndex < speedVertices.size(); vertexIndex++) {

					/*
					 * create vertices from UI controls
					 */
					final Spinner spinnerAvgSpeed = _spinnerVertex_AvgSpeed[vertexIndex];
					final Link linkTourType = _linkVertex_TourType[vertexIndex];

					final SpeedVertex speedVertex = new SpeedVertex();

					speedVertex.avgSpeed = spinnerAvgSpeed.getSelection();

					final Object tourTypeId = linkTourType.getData(DATA_KEY_TOUR_TYPE_ID);
					if (tourTypeId instanceof Long) {
						speedVertex.tourTypeId = (long) tourTypeId;
					} else {
						speedVertex.tourTypeId = TourDatabase.ENTITY_IS_NOT_SAVED;
					}

					newVertices.add(speedVertex);
				}

				// sort vertices by value
				Collections.sort(newVertices);

				// update model
				speedVertices.clear();
				speedVertices.addAll(newVertices);
			}

			_currentConfigItem.setupConfigImage();

		} else if (selectedTourTypeConfig.equals(TourTypeConfig.TOUR_TYPE_CONFIG_ONE_FOR_ALL)) {

			update_Model_From_UI_OneTourType();

		} else {

			// this is the default or TourTypeConfig.TOUR_TYPE_CONFIG_NOT_USED

			_currentConfigItem.setupConfigImage();
		}

	}

	private void update_Model_From_UI_OneTourType() {

		final Object tourTypeId = _linkOneTourType.getData(DATA_KEY_TOUR_TYPE_ID);

		if (tourTypeId instanceof Long) {
			_currentConfigItem.oneTourType = TourDatabase.getTourType((long) tourTypeId);
		} else {

			_currentConfigItem.oneTourType = null;
		}

		_currentConfigItem.setupConfigImage();
	}

	private void update_UI_From_Model() {

		if (_currentConfigItem == null) {
			return;
		}

		final Enum<TourTypeConfig> tourTypeConfig = _currentConfigItem.tourTypeConfig;

		_configViewer.update(_currentConfigItem, null);

		_comboBackupPath.setText(_currentConfigItem.backupFolder);
		_comboDevicePath.setText(_currentConfigItem.deviceFolder);
		_comboTourTypeConfig.select(getTourTypeConfigIndex(tourTypeConfig));
		_txtConfigName.setText(_currentConfigItem.name);

		/*
		 * Setup tour type UI
		 */
		selectTourTypePage(tourTypeConfig);

		if (TourTypeConfig.TOUR_TYPE_CONFIG_BY_SPEED.equals(tourTypeConfig)) {

			_vertexOuterContainer.setRedraw(false);
			{
				// check and create vertex fields
				createUI_88_VertexFields();

				final ArrayList<SpeedVertex> speedVertices = _currentConfigItem.speedVertices;

				final int vertexSize = speedVertices.size();

				final net.tourbook.ui.UI uiInstance = net.tourbook.ui.UI.getInstance();

				for (int vertexIndex = 0; vertexIndex < vertexSize; vertexIndex++) {

					final SpeedVertex vertex = speedVertices.get(vertexIndex);
					final long tourTypeId = vertex.tourTypeId;

					final Spinner spinnerAvgSpeed = _spinnerVertex_AvgSpeed[vertexIndex];
					final Link linkTourType = _linkVertex_TourType[vertexIndex];
					final Label labelTourTypeIcon = _lblVertex_TourTypeIcon[vertexIndex];

					// update UI
					spinnerAvgSpeed.setSelection(vertex.avgSpeed);

					if (tourTypeId == TourDatabase.ENTITY_IS_NOT_SAVED) {

						// tour type is not yet set

						linkTourType.setData(DATA_KEY_TOUR_TYPE_ID, null);
						linkTourType.setText(Messages.Dialog_ImportConfig_Link_TourType);
						labelTourTypeIcon.setImage(null);

					} else {

						linkTourType.setData(DATA_KEY_TOUR_TYPE_ID, tourTypeId);
						linkTourType.setText(UI.LINK_TAG_START
								+ uiInstance.getTourTypeLabel(tourTypeId)
								+ UI.LINK_TAG_END);
						labelTourTypeIcon.setImage(uiInstance.getTourTypeImage(tourTypeId));
					}

					// keep vertex references
					labelTourTypeIcon.setData(DATA_KEY_VERTEX_INDEX, vertexIndex);
					linkTourType.setData(DATA_KEY_VERTEX_INDEX, vertexIndex);
					spinnerAvgSpeed.setData(DATA_KEY_VERTEX_INDEX, vertexIndex);
					_actionVertex_Delete[vertexIndex].setData(DATA_KEY_VERTEX_INDEX, vertexIndex);

				}
			}
			_vertexOuterContainer.setRedraw(true);

		} else if (TourTypeConfig.TOUR_TYPE_CONFIG_ONE_FOR_ALL.equals(tourTypeConfig)) {

			TourType tourType = null;

			final TourType oneTourType = _currentConfigItem.oneTourType;
			if (oneTourType != null) {

				final long tourTypeId = oneTourType.getTypeId();
				tourType = TourDatabase.getTourType(tourTypeId);
			}

			updateUI_OneTourType(tourType);

		} else {

			// this is the default or TourTypeConfig.TOUR_TYPE_CONFIG_NOT_USED
		}

		/*
		 * Clear UI for the other tour type configs that they do not be displayed when another
		 * import config is selected.
		 */
		if (TourTypeConfig.TOUR_TYPE_CONFIG_BY_SPEED.equals(tourTypeConfig)) {

			updateUI_OneTourType(null);

		} else if (TourTypeConfig.TOUR_TYPE_CONFIG_ONE_FOR_ALL.equals(tourTypeConfig)) {

			updateUI_ClearVertices();

		} else {

			// this is the default or TourTypeConfig.TOUR_TYPE_CONFIG_NOT_USED

			updateUI_ClearVertices();
			updateUI_OneTourType(null);
		}
	}

	private void updateUI_ClearVertices() {

		if (_vertexScrolledContainer != null) {

			_vertexScrolledContainer.dispose();
			_vertexScrolledContainer = null;

			_actionVertex_Delete = null;
			_lblVertex_TourTypeIcon = null;
			_lblVertex_SpeedUnit = null;
			_linkVertex_TourType = null;
			_spinnerVertex_AvgSpeed = null;
		}
	}

	private void updateUI_OneTourType(final TourType tourType) {

		if (tourType == null) {

			_lblOneTourTypeIcon.setImage(null);

			_linkOneTourType.setText(Messages.Dialog_ImportConfig_Link_TourType);
			_linkOneTourType.setData(DATA_KEY_TOUR_TYPE_ID, null);

		} else {

			final Image image = net.tourbook.ui.UI.getInstance().getTourTypeImage(tourType.getTypeId());

			_lblOneTourTypeIcon.setImage(image);

			_linkOneTourType.setText(UI.LINK_TAG_START + tourType.getName() + UI.LINK_TAG_END);
			_linkOneTourType.setData(DATA_KEY_TOUR_TYPE_ID, tourType.getTypeId());
		}

		// update the model that the table displays the correct image
		update_Model_From_UI_OneTourType();

		_configViewer.getTable().redraw();
	}

	private void validateFields() {
		// TODO Auto-generated method stub

	}

}
