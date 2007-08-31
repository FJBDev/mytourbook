/*******************************************************************************
 * Copyright (C) 2005, 2007  Wolfgang Schramm
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

import net.tourbook.Messages;
import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.ui.views.tourMap.WizardTourComparer;
import net.tourbook.util.PositionedWizardDialog;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.ICoolBarManager;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarContributionItem;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.actions.ContributionItemFactory;
import org.eclipse.ui.actions.ActionFactory.IWorkbenchAction;
import org.eclipse.ui.application.ActionBarAdvisor;
import org.eclipse.ui.application.IActionBarConfigurer;

/**
 * An action bar advisor is responsible for creating, adding, and disposing of the fGraphActions
 * added to a workbench window. Each window will be populated with new fGraphActions.
 */
public class ApplicationActionBarAdvisor extends ActionBarAdvisor {

//	private ActionOpenView		fActionRawDataView;
//	private ActionOpenView		fActionStatisticsView;
//	private ActionOpenView		fActionTourBookView;
//	private ActionOpenView		fActionTourMapView;
//	private ActionOpenView		fActionTourChartView;
//	private ActionOpenView		fActionTourCompareView;

	private Action				fActionTourCompareWizard;

	private IWorkbenchAction	fActionSave;
	private IWorkbenchAction	fActionSaveAll;
	private IContributionItem	fActionViewShortList;
	private IWorkbenchAction	fActionPreferences;

	PersonContributionItem		personSelector;
	TourTypeContributionItem	tourTypeSelector;
	private IWorkbenchAction	fActionAbout;
	private IWorkbenchAction	fActionQuit;

	public ApplicationActionBarAdvisor(IActionBarConfigurer configurer) {
		super(configurer);
	}

	@Override
	public IStatus saveState(IMemento memento) {

		personSelector.saveState(memento);
		tourTypeSelector.saveState(memento);

		return super.saveState(memento);
	}

	@Override
	protected void makeActions(final IWorkbenchWindow window) {

//		final IWorkbenchAction action;

		personSelector = new PersonContributionItem();
		tourTypeSelector = new TourTypeContributionItem();

		fActionQuit = ActionFactory.QUIT.create(window);
		register(fActionQuit);

		fActionAbout = ActionFactory.ABOUT.create(window);
		fActionAbout.setText(Messages.Action_About);
		register(fActionAbout);

//		fActionTourChartView = new ActionOpenView(window,
//				"Tour Chart",
//				"Shows a chart for the currently selected tour",
//				TourChartView.ID,
//				ICommandIds.CMD_OPENVIEW_TOURCHART,
//				"tour-chart.gif");
//
//		fActionRawDataView = new ActionOpenView(window,
//				Messages.Action_openview_rawdata,
//				Messages.Action_openview_rawdata_tooltip,
//				RawDataView.ID,
//				ICommandIds.CMD_OPENVIEW_IMPORTEDDATA,
//				Messages.Image_view_rawdata);
//
//		fActionStatisticsView = new ActionOpenView(window,
//				"",
//				"",
//				TourStatisticsView.ID,
//				ICommandIds.CMD_OPENVIEW_STATISTICS,
//				Messages.Image_show_statistics);
//
//		fActionTourBookView = new ActionOpenView(window,
//				Messages.Action_openview_tourbook,
//				Messages.Action_openview_tourbook_tooltip,
//				TourBookView.ID,
//				ICommandIds.CMD_OPENVIEW_TOURLIST,
//				Messages.Image_view_tourbool);
//
//		fActionTourMapView = new ActionOpenView(window,
//				Messages.Action_openview_tourmap,
//				Messages.Action_openview_tourmap_tooltip,
//				TourMapView.ID,
//				ICommandIds.CMD_OPENVIEW_TOURMAP,
//				Messages.Image_view_tourmap);
//
//		fActionTourCompareView = new ActionOpenView(window,
//				Messages.Action_openview_compare_result,
//				Messages.Action_openview_compare_result_tooltip,
//				CompareResultView.ID,
//				ICommandIds.CMD_OPENVIEW_TOURCOMPARER,
//				Messages.Image_view_compare_result);

		fActionPreferences = ActionFactory.PREFERENCES.create(window);
		fActionPreferences.setText(Messages.Action_open_preferences);
		register(fActionPreferences);

		fActionSave = ActionFactory.SAVE.create(window);
		register(fActionSave);

		fActionSaveAll = ActionFactory.SAVE_ALL.create(window);
		register(fActionSaveAll);

		fActionViewShortList = ContributionItemFactory.VIEWS_SHORTLIST.create(window);

		fActionTourCompareWizard = new Action() {

			{
				setText(Messages.Action_open_compare_wizard);
				setToolTipText(Messages.Action_open_compare_wizard_tooltip);
				setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image_view_compare_wizard));
			}

			@Override
			public void run() {
				Wizard wizard = new WizardTourComparer();

				final WizardDialog dialog = new PositionedWizardDialog(window.getShell(),
						wizard,
						WizardTourComparer.DIALOG_SETTINGS_SECTION,
						800,
						600);

				BusyIndicator.showWhile(null, new Runnable() {
					public void run() {
						dialog.open();
					}
				});
			}
		};
	}

	@Override
	protected void fillMenuBar(IMenuManager menuBar) {

		/*
		 * create menu bar
		 */
		menuBar.add(createFileMenu());
		menuBar.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));
		menuBar.add(createViewMenu());
		menuBar.add(createHelpMenu());
	}

	private MenuManager createHelpMenu() {
		/*
		 * help - menu
		 */
		MenuManager helpMenu = new MenuManager(Messages.Action_Menu_help,
				IWorkbenchActionConstants.M_HELP);

		helpMenu.add(getAction(ActionFactory.ABOUT.getId()));

		return helpMenu;
	}

	private MenuManager createViewMenu() {

		MenuManager tourMenu = new MenuManager(Messages.Action_Menu_view, null);

		tourMenu.add(fActionViewShortList);

//		tourMenu.add(fActionTourChartView);
//		tourMenu.add(new Separator());
//
//		tourMenu.add(fActionRawDataView);
//		tourMenu.add(fActionTourBookView);
//		tourMenu.add(fActionTourMapView);
//		tourMenu.add(fActionStatisticsView);
//		tourMenu.add(new Separator());

//		tourMenu.add(fActionTourCompareWizard);
//		tourMenu.add(fActionTourCompareView);

		return tourMenu;
	}

	private MenuManager createFileMenu() {

		MenuManager fileMenu = new MenuManager(Messages.Action_Menu_file,
				IWorkbenchActionConstants.M_FILE);

		fileMenu.add(fActionSave);
		fileMenu.add(fActionSaveAll);
		fileMenu.add(new Separator());

		fileMenu.add(new Separator());

		// If we're on OS X we shouldn't show this command in the File menu. It
		// should be invisible to the user. However, we should not remove it -
		// the carbon UI code will do a search through our menu structure
		// looking for it when Cmd-Q is invoked (or Quit is chosen from the
		// application menu.
		ActionContributionItem prefItem = new ActionContributionItem(fActionPreferences);
		prefItem.setVisible(!"carbon".equals(SWT.getPlatform())); //$NON-NLS-1$
		fileMenu.add(prefItem);

		fileMenu.add(new Separator("update")); //$NON-NLS-1$
		fileMenu.add(new Separator());

		// If we're on OS X we shouldn't show this command in the File menu. It
		// should be invisible to the user. However, we should not remove it -
		// the carbon UI code will do a search through our menu structure
		// looking for it when Cmd-Q is invoked (or Quit is chosen from the
		// application menu.
		ActionContributionItem quitItem = new ActionContributionItem(fActionQuit);
		quitItem.setVisible(!"carbon".equals(SWT.getPlatform())); //$NON-NLS-1$
		fileMenu.add(quitItem);

		return fileMenu;
	}

	@Override
	protected void fillCoolBar(ICoolBarManager coolBar) {

		IToolBarManager tbmPeople = new ToolBarManager(SWT.FLAT | SWT.RIGHT);
		tbmPeople.add(personSelector);

		coolBar.add(new ToolBarContributionItem(tbmPeople, "people")); //$NON-NLS-1$

		// ---------------------------------------------------------

		IToolBarManager tbmTourType = new ToolBarManager(SWT.FLAT | SWT.RIGHT);
		tbmTourType.add(tourTypeSelector);

		coolBar.add(new ToolBarContributionItem(tbmTourType, "tourtype")); //$NON-NLS-1$

		// ---------------------------------------------------------

//		IToolBarManager tbmImport = new ToolBarManager(SWT.FLAT | SWT.RIGHT);
//		tbmImport.add(fActionImportFromFile);
//		tbmImport.add(fActionImportFromDevice);
//		tbmImport.add(fActionImportFromDeviceDirect);
//
//		coolBar.add(new ToolBarContributionItem(tbmImport, "import")); //$NON-NLS-1$

		// ---------------------------------------------------------

//		IToolBarManager openToolbar = new ToolBarManager(SWT.FLAT | SWT.RIGHT);
//
//		openToolbar.add(new Separator());
//		openToolbar.add(fActionTourChartView);
//
//		openToolbar.add(new Separator());
//		openToolbar.add(fActionRawDataView);
//		openToolbar.add(fActionTourBookView);
//		openToolbar.add(fActionTourMapView);
//		openToolbar.add(fActionStatisticsView);
//
//		coolBar.add(new ToolBarContributionItem(openToolbar, "main")); //$NON-NLS-1$

		// ---------------------------------------------------------

//		IToolBarManager tbmCompare = new ToolBarManager(SWT.FLAT | SWT.RIGHT);
//
//		tbmCompare.add(new Separator());
//		tbmCompare.add(fActionTourCompareWizard);
//		tbmCompare.add(fActionTourCompareView);
//
//		coolBar.add(new ToolBarContributionItem(tbmCompare, "compare")); //$NON-NLS-1$

		// ---------------------------------------------------------

//		IToolBarManager prefToolbar = new ToolBarManager(SWT.FLAT | SWT.RIGHT);
//
//		prefToolbar.add(new Separator());
//		prefToolbar.add(fActionPreferences);
//
//		coolBar.add(new ToolBarContributionItem(prefToolbar, "pref")); //$NON-NLS-1$
	}

}
