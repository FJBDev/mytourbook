/*******************************************************************************
 * Copyright (C) 2005, 2014  Wolfgang Schramm and Contributors
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
package net.tourbook.preferences;

import java.util.ArrayList;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.color.Map3ColorDefinition;
import net.tourbook.common.color.Map3ColorManager;
import net.tourbook.common.color.Map3ColorProfile;
import net.tourbook.map3.ui.DialogSelectMap3Color;
import net.tourbook.ui.UI;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.TreeColumnLayout;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.ITreeViewerListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.TreeExpansionEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class PrefPageMap3Color extends PreferencePage implements IWorkbenchPreferencePage {

	private final IPreferenceStore	_prefStore	= TourbookPlugin.getDefault().getPreferenceStore();

	private boolean					_isModified;

	private TreeViewer				_colorProfileViewer;

// is disabled to do easier navigation
//	private Map3ColorDefinition		_expandedItem;

	private boolean					_isTreeExpading;

	/*
	 * UI controls
	 */
	private Composite				_colorProfileViewerContainer;

	private Button					_btnAddProfile;
	private Button					_btnDuplicateProfile;
	private Button					_btnEditProfile;
	private Button					_btnRemoveProfile;

// this is currently disabled because it's not a 1 minute implementation
//	private Button					_chkAutoExpandCollapse;

	/**
	 * the color content provider has the following structure<br>
	 * 
	 * <pre>
	 * {@link Map3ColorDefinition}
	 *    {@link Map3ColorProfile}
	 *    {@link Map3ColorProfile}
	 *    ...
	 *    {@link Map3ColorProfile}
	 * 
	 *    ...
	 * 
	 * {@link Map3ColorDefinition}
	 *    {@link Map3ColorProfile}
	 *    {@link Map3ColorProfile}
	 *    ...
	 *    {@link Map3ColorProfile}
	 * </pre>
	 */
	private static class ColorContentProvider implements ITreeContentProvider {

		public void dispose() {}

		public Object[] getChildren(final Object parentElement) {
			if (parentElement instanceof Map3ColorDefinition) {

				final ArrayList<Map3ColorProfile> colorProfiles = ((Map3ColorDefinition) parentElement)
						.getColorProfiles();

				return colorProfiles.toArray(new Map3ColorProfile[colorProfiles.size()]);
			}

			return null;
		}

		public Object[] getElements(final Object inputElement) {
			if (inputElement instanceof PrefPageMap3Color) {

				final ArrayList<Map3ColorDefinition> colorDefinitions = Map3ColorManager.getMapColorDefinitions();

				return colorDefinitions.toArray(new Map3ColorDefinition[colorDefinitions.size()]);
			}

			return null;
		}

		public Object getParent(final Object element) {
			return null;
		}

		public boolean hasChildren(final Object element) {
			if (element instanceof Map3ColorDefinition) {
				return true;
			}
			return false;
		}

		public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {}
	}

	private void autoExpandCollapse(final Map3ColorDefinition treeItem) {

		if (_isTreeExpading) {

			// prevent runtime exception: Ignored reentrant call while viewer is busy.
			return;
		}

//		if (_colorProfileViewer.getExpandedState(treeItem)) {
//
//			_colorProfileViewer.collapseToLevel(treeItem, 1);
//
//		} else {
//
//			if (_expandedItem != null) {
//				_colorProfileViewer.collapseToLevel(_expandedItem, 1);
//			}
//			_colorProfileViewer.expandToLevel(treeItem, 1);
//			_expandedItem = treeItem;
//
//			// expanding the treeangle, the layout is correctly done but not with double click
//			_colorProfileViewerContainer.layout(true, true);
//		}
	}

	@Override
	protected Control createContents(final Composite parent) {

		final Composite ui = createUI(parent);

		restoreState();

		enableControls();

		_colorProfileViewer.setInput(this);

		// expand all to do easier navigation when only the default profiles are defined
		_colorProfileViewer.expandAll();

		return ui;
	}

	private Composite createUI(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
		{
			createUI_10_ColorViewer(container);
			createUI_20_Actions(container);

			createUI_50_Options(container);
		}

		return container;
	}

	private void createUI_10_ColorViewer(final Composite parent) {

		/*
		 * create tree layout
		 */
		_colorProfileViewerContainer = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults()//
				.grab(true, true)
				.hint(200, 100)
				.applyTo(_colorProfileViewerContainer);

		final TreeColumnLayout treeLayout = new TreeColumnLayout();
		_colorProfileViewerContainer.setLayout(treeLayout);

		/*
		 * create viewer
		 */
		final Tree tree = new Tree(_colorProfileViewerContainer, SWT.H_SCROLL
				| SWT.V_SCROLL
				| SWT.BORDER
				| SWT.FULL_SELECTION);

		tree.setHeaderVisible(false);
		tree.setLinesVisible(_prefStore.getBoolean(ITourbookPreferences.VIEW_LAYOUT_DISPLAY_LINES));

		_colorProfileViewer = new TreeViewer(tree);
		defineAllColumns(treeLayout, tree);

		_colorProfileViewer.setContentProvider(new ColorContentProvider());

//		_graphColorPainter = new GraphColorPainter(this);

		_colorProfileViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(final SelectionChangedEvent event) {
				onSelectColorViewer();
			}
		});

		_colorProfileViewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(final DoubleClickEvent event) {
				onDoubleClickColorViewer();
			}

		});

		_colorProfileViewer.addTreeListener(new ITreeViewerListener() {

			public void treeCollapsed(final TreeExpansionEvent event) {

//				if (event.getElement() instanceof Map3ColorDefinition) {
//					_expandedItem = null;
//				}
			}

			public void treeExpanded(final TreeExpansionEvent event) {

//				final Object element = event.getElement();
//
//				if (element instanceof Map3ColorDefinition) {
//
//					if (_expandedItem != null) {
//
//						_isTreeExpading = true;
//						{
//							_colorProfileViewer.collapseToLevel(_expandedItem, 1);
//						}
//						_isTreeExpading = false;
//					}
//
//					Display.getCurrent().asyncExec(new Runnable() {
//						public void run() {
//
//							final Map3ColorDefinition treeItem = (Map3ColorDefinition) element;
//
//							_colorProfileViewer.expandToLevel(treeItem, 1);
//							_expandedItem = treeItem;
//						}
//					});
//				}
			}
		});
	}

	private void createUI_20_Actions(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().applyTo(container);
		GridLayoutFactory.fillDefaults().applyTo(container);
		{
			/*
			 * button: edit profile
			 */
			_btnEditProfile = new Button(container, SWT.NONE);
			_btnEditProfile.setText(Messages.App_Action_Edit);
			_btnEditProfile.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					onEditProfile();
				}
			});
			setButtonLayoutData(_btnEditProfile);

			/*
			 * button: add profile
			 */
			_btnAddProfile = new Button(container, SWT.NONE);
			_btnAddProfile.setText(Messages.App_Action_Add);
			_btnAddProfile.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
//					onAddProfile();
				}
			});
			setButtonLayoutData(_btnAddProfile);

			/*
			 * button: remove profile
			 */
			_btnRemoveProfile = new Button(container, SWT.NONE);
			_btnRemoveProfile.setText(Messages.App_Action_Remove);
			_btnRemoveProfile.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
//					onRemoveProfile();
				}
			});
			setButtonLayoutData(_btnRemoveProfile);

			/*
			 * button: duplicate profile
			 */
			_btnDuplicateProfile = new Button(container, SWT.NONE);
			_btnDuplicateProfile.setText(Messages.App_Action_Duplicate);
			_btnDuplicateProfile.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
//					onDuplicateProfile();
				}
			});
			setButtonLayoutData(_btnDuplicateProfile);

			/*
			 * button: adjust columns
			 */
			final Button btnAdjustColumns = new Button(container, SWT.NONE);
			btnAdjustColumns.setText(Messages.App_Action_Columns);
			btnAdjustColumns.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
//					_columnManager.openColumnDialog();
				}
			});
			setButtonLayoutData(btnAdjustColumns);
			final GridData gd = (GridData) btnAdjustColumns.getLayoutData();
			gd.verticalIndent = 20;
		}
	}

	private void createUI_50_Options(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults()//
				.grab(true, false)
				.span(2, 1)
				.applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
		{
//			/*
//			 * Checkbox: Autoexpand/collapse viewer
//			 */
//			_chkAutoExpandCollapse = new Button(container, SWT.CHECK);
//			_chkAutoExpandCollapse.setText(Messages.PrefPage_Map3Color_Checkbox_AutoExpandCollapse);
		}
	}

	/**
	 * create columns
	 */
	private void defineAllColumns(final TreeColumnLayout treeLayout, final Tree tree) {

		TreeViewerColumn tvc;
		TreeColumn tc;
		final int colorWidth = (tree.getItemHeight() + 0) * 5 + 10;

		/*
		 * 1. column: color item/color definition
		 */
		tvc = new TreeViewerColumn(_colorProfileViewer, SWT.TRAIL);
		tc = tvc.getColumn();
		tvc.setLabelProvider(new StyledCellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final Object element = cell.getElement();

				if (element instanceof Map3ColorDefinition) {
					cell.setText(((Map3ColorDefinition) (element)).getVisibleName());
				} else if (element instanceof Map3ColorProfile) {
					cell.setText(((Map3ColorProfile) (element)).getProfileName());
				} else {
					cell.setText(UI.EMPTY_STRING);
				}
			}
		});
		treeLayout.setColumnData(tc, new ColumnWeightData(1, true));

		/*
		 * 2. column: color for definition/item
		 */
		tvc = new TreeViewerColumn(_colorProfileViewer, SWT.TRAIL);
		tc = tvc.getColumn();
		tvc.setLabelProvider(new StyledCellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				final Object element = cell.getElement();

//				if (element instanceof Map3ColorDefinition) {
//					cell.setImage(_graphColorPainter.drawDefinitionImage((Map3ColorDefinition) element));
//				} else if (element instanceof Map3ColorItem) {
//					cell.setImage(_graphColorPainter.drawColorImage((Map3ColorItem) element));
//				} else {
//					cell.setImage(null);
//				}
			}
		});
		treeLayout.setColumnData(tc, new ColumnPixelData(colorWidth, true));
	}

	private void enableControls() {

	}

	private void fireModifyEvent() {

		if (_isModified) {

			_isModified = false;

			// fire event
			getPreferenceStore().setValue(ITourbookPreferences.MAP3_COLOR_IS_MODIFIED, Math.random());
		}
	}

	public void init(final IWorkbench workbench) {}

	@Override
	public boolean okToLeave() {

		if (_isModified) {

			saveState();

			// save the colors in the pref store
			super.performOk();

			fireModifyEvent();
		}

		return super.okToLeave();
	}

	private void onDoubleClickColorViewer() {

		final Object selection = ((IStructuredSelection) _colorProfileViewer.getSelection()).getFirstElement();

		if (selection instanceof Map3ColorDefinition) {

			// expand/collapse current item

			autoExpandCollapse((Map3ColorDefinition) selection);

		} else if (selection instanceof Map3ColorProfile) {

			// edit selected color
			onEditProfile();
		}
	}

	private void onEditProfile() {

		final Object firstElement = ((StructuredSelection) _colorProfileViewer.getSelection()).getFirstElement();
		if (firstElement instanceof Map3ColorProfile) {

			final Map3ColorProfile originalProfile = (Map3ColorProfile) firstElement;

			// open color chooser dialog
			final DialogSelectMap3Color dialog = new DialogSelectMap3Color(
					Display.getCurrent().getActiveShell(),
					this,
					originalProfile,
					false);

			if (dialog.open() == Window.OK) {
//				saveProfileModified(originalProfile, dialogProfile);
			}
		}
	}

	/**
	 * Is called when the color in the color viewer was selected.
	 */
	private void onSelectColorViewer() {

		final IStructuredSelection selection = (IStructuredSelection) _colorProfileViewer.getSelection();

//		_btnLegend.setEnabled(false);
//		_colorSelector.setEnabled(false);

		final Object firstSelectedItem = selection.getFirstElement();

		if (firstSelectedItem instanceof Map3ColorProfile) {

			// graph color is selected

			final Map3ColorProfile colorProfile = (Map3ColorProfile) firstSelectedItem;

//			// keep selected color
//			_selectedColor = colorProfile;
//
//			if (colorProfile.isMapColor()) {
//
//				// legend color is selected
//
//				_btnLegend.setEnabled(true);
//
//			} else {
//
//				// 'normal' color is selected
//
//				// prepare color selector
//				_colorSelector.setColorValue(colorProfile.getNewRGB());
//				_colorSelector.setEnabled(true);
//			}

		} else if (firstSelectedItem instanceof Map3ColorDefinition) {

			// color definition is selected

// this feature is annoying when using keyboard -> disabled
//			autoExpandCollapse((Map3ColorDefinition) firstSelectedItem);
		}
	}

	@Override
	protected void performDefaults() {

		_isModified = true;

//		_chkAutoExpandCollapse.setSelection(_prefStore.getDefaultBoolean(//
//				ITourbookPreferences.MAP3_COLOR_IS_AUTO_EXPAND_COLLAPSE));

		// set editor defaults
		super.performDefaults();

		enableControls();
	}

	@Override
	public boolean performOk() {

		saveState();

		// store editor fields
		final boolean isOK = super.performOk();

		if (isOK) {
			fireModifyEvent();
		}

		return isOK;
	}

	private void restoreState() {

//		_chkAutoExpandCollapse.setSelection(//
//				_prefStore.getBoolean(ITourbookPreferences.MAP3_COLOR_IS_AUTO_EXPAND_COLLAPSE));

	}

	private void saveState() {

//		_prefStore.setValue(//
//				ITourbookPreferences.MAP3_COLOR_IS_AUTO_EXPAND_COLLAPSE,
//				_chkAutoExpandCollapse.getSelection());

	}

}
