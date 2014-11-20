/*******************************************************************************
 * Copyright (C) 2005, 2014 Wolfgang Schramm and Contributors
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
package net.tourbook.search;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.color.IColorSelectorListener;
import net.tourbook.common.tooltip.AnimatedToolTipShell;
import net.tourbook.common.util.Util;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseTrackAdapter;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.ToolBar;

/**
 * Tour chart marker properties slideout.
 */
public class SlideoutSearchOptions extends AnimatedToolTipShell implements IColorSelectorListener {

	private final IDialogSettings	_state				= TourbookPlugin.getState(SearchView.ID);

	// initialize with default values which are (should) never be used
	private Rectangle				_toolTipItemBounds	= new Rectangle(0, 0, 50, 50);

	private final WaitTimer			_waitTimer			= new WaitTimer();

	private boolean					_isWaitTimerStarted;
	private boolean					_canOpenToolTip;
	private boolean					_isAnotherDialogOpened;

	private SearchView				_searchView;

	private SelectionAdapter		_selectionAdapter_Default;
	private SelectionAdapter		_selectionAdapter_WithNewSearch;
	{
		_selectionAdapter_Default = new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onChangeUI(false);
			}
		};

		_selectionAdapter_WithNewSearch = new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onChangeUI(true);
			}
		};
	}

	/*
	 * UI controls
	 */
	private Composite				_shellContainer;

	private Button					_chkShowDateTime;
	private Button					_chkShowItemNumber;
	private Button					_chkShowLuceneDocId;
//	private Button					_chkShowScore;
	private Button					_chkTopNavigator;

	private Button					_rdoTimeAscending;
	private Button					_rdoTimeDescending;

	private Spinner					_spinnerDisplayedResults;
	private Spinner					_spinnerNumberOfPages;

	private final class WaitTimer implements Runnable {
		@Override
		public void run() {
			open_Runnable();
		}
	}

	public SlideoutSearchOptions(	final Control ownerControl,
									final ToolBar toolBar,
									final IDialogSettings state,
									final SearchView searchView) {

		super(ownerControl);

		_searchView = searchView;

		addListener(ownerControl, toolBar);

		setToolTipCreateStyle(AnimatedToolTipShell.TOOLTIP_STYLE_KEEP_CONTENT);
		setBehaviourOnMouseOver(AnimatedToolTipShell.MOUSE_OVER_BEHAVIOUR_IGNORE_OWNER);
		setIsKeepShellOpenWhenMoved(false);
		setFadeInSteps(1);
		setFadeOutSteps(10);
		setFadeOutDelaySteps(1);
	}

	private void addListener(final Control ownerControl, final ToolBar toolBar) {

		toolBar.addMouseTrackListener(new MouseTrackAdapter() {
			@Override
			public void mouseExit(final MouseEvent e) {

				// prevent to open the tooltip
				_canOpenToolTip = false;
			}
		});
	}

	@Override
	protected void beforeHideToolTip() {

	}

	@Override
	protected boolean canCloseToolTip() {

		/*
		 * Do not hide this dialog when the color selector dialog or other dialogs are opened
		 * because it will lock the UI completely !!!
		 */

		final boolean isCanClose = _isAnotherDialogOpened == false;

		return isCanClose;
	}

	@Override
	protected boolean canShowToolTip() {
		return true;
	}

	@Override
	protected boolean closeShellAfterHidden() {

		/*
		 * Close the tooltip that the state is saved.
		 */

		return true;
	}

	@Override
	public void colorDialogOpened(final boolean isDialogOpened) {

		_isAnotherDialogOpened = isDialogOpened;
	}

	@Override
	protected Composite createToolTipContentArea(final Composite parent) {

		final Composite ui = createUI(parent);

		restoreState();

		return ui;
	}

	private Composite createUI(final Composite parent) {

		_shellContainer = new Composite(parent, SWT.NONE);
		GridLayoutFactory.swtDefaults().applyTo(_shellContainer);
		{
			final Composite container = new Composite(_shellContainer, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
			GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
			{
				createUI_10_Sorting(container);
				createUI_50_ResultItems(container);
				createUI_90_PageNavigator(container);
			}

			createUI_99_RestoreDefaults(_shellContainer);
		}

		return _shellContainer;
	}

	private void createUI_10_Sorting(final Composite parent) {

		final Group group = new Group(parent, SWT.NONE);
		GridDataFactory.fillDefaults().span(2, 1).applyTo(group);
		GridLayoutFactory.swtDefaults().numColumns(4).applyTo(group);
		group.setText(Messages.Slideout_SearchViewOptions_Group_Sorting);
		{
			/*
			 * Sort by date/time
			 */
			{
				Label label = new Label(group, SWT.NONE);
				label.setText(Messages.Slideout_SearchViewOptions_Label_SortByDate);

				// spacer - NEEDS A BETTER SOLUTION WHEN SORTING WITH MULTIPLE FIELDS
				label = new Label(group, SWT.NONE);
				GridDataFactory.fillDefaults().indent(20, 0).applyTo(label);

				_rdoTimeAscending = new Button(group, SWT.RADIO);
				_rdoTimeAscending.setText(Messages.Slideout_SearchViewOptions_Label_SortAscending);
				_rdoTimeAscending.addSelectionListener(_selectionAdapter_WithNewSearch);

				_rdoTimeDescending = new Button(group, SWT.RADIO);
				_rdoTimeDescending.setText(Messages.Slideout_SearchViewOptions_Label_SortDescending);
				_rdoTimeDescending.addSelectionListener(_selectionAdapter_WithNewSearch);
			}
		}
	}

	private void createUI_50_ResultItems(final Composite parent) {

		final Group group = new Group(parent, SWT.NONE);
		GridDataFactory.fillDefaults().span(2, 1).applyTo(group);
		GridLayoutFactory.swtDefaults().numColumns(2).applyTo(group);
		group.setText(Messages.Slideout_SearchViewOptions_Group_Result);
		{
			/*
			 * Show date/time
			 */
			{
				_chkShowDateTime = new Button(group, SWT.CHECK);
				GridDataFactory.fillDefaults()//
						.span(2, 1)
						.applyTo(_chkShowDateTime);
				_chkShowDateTime.setText(Messages.Slideout_SearchViewOptions_Checkbox_IsShowDateTime);
				_chkShowDateTime.addSelectionListener(_selectionAdapter_Default);
			}

			/*
			 * Show score
			 */
//			{
//				_chkShowScore = new Button(group, SWT.CHECK);
//				GridDataFactory.fillDefaults()//
//						.span(2, 1)
//						.applyTo(_chkShowScore);
//				_chkShowScore.setText(Messages.Slideout_SearchViewOptions_Checkbox_IsShowScore);
//				_chkShowScore.addSelectionListener(_selectionAdapter_Default);
//			}

			/*
			 * Show item number
			 */
			{
				_chkShowItemNumber = new Button(group, SWT.CHECK);
				GridDataFactory.fillDefaults()//
						.span(2, 1)
						.applyTo(_chkShowItemNumber);
				_chkShowItemNumber.setText(Messages.Slideout_SearchViewOptions_Checkbox_IsShowItemNumber);
				_chkShowItemNumber.addSelectionListener(_selectionAdapter_Default);
			}

			/*
			 * Show lucene doc id
			 */
			{
				_chkShowLuceneDocId = new Button(group, SWT.CHECK);
				GridDataFactory.fillDefaults()//
						.span(2, 1)
						.applyTo(_chkShowLuceneDocId);
				_chkShowLuceneDocId.setText(Messages.Slideout_SearchViewOptions_Checkbox_IsShowDocId);
				_chkShowLuceneDocId.addSelectionListener(_selectionAdapter_Default);
			}

			/*
			 * Hits per page
			 */
			{
				// checkbox
				final Label label = new Label(group, SWT.CHECK);
				label.setText(Messages.Slideout_SearchViewOptions_Label_NumberOfDisplayedResults);

				// spinner
				_spinnerDisplayedResults = new Spinner(group, SWT.BORDER);
				GridDataFactory.fillDefaults() //
						.align(SWT.END, SWT.CENTER)
						.applyTo(_spinnerDisplayedResults);
				_spinnerDisplayedResults.setMinimum(1);
				_spinnerDisplayedResults.setMaximum(1000);
				_spinnerDisplayedResults.addMouseWheelListener(new MouseWheelListener() {
					public void mouseScrolled(final MouseEvent event) {
						UI.adjustSpinnerValueOnMouseScroll(event);
						onChangeUI(true);
					}
				});
				_spinnerDisplayedResults.addSelectionListener(_selectionAdapter_WithNewSearch);
			}
		}
	}

	private void createUI_90_PageNavigator(final Composite parent) {

		final Group group = new Group(parent, SWT.NONE);
		GridDataFactory.fillDefaults().span(2, 1).applyTo(group);
		GridLayoutFactory.swtDefaults().numColumns(2).applyTo(group);
		group.setText(Messages.Slideout_SearchViewOptions_Group_PageNavigation);
		{
			/*
			 * Show top navigator
			 */
			{
				_chkTopNavigator = new Button(group, SWT.CHECK);
				GridDataFactory.fillDefaults()//
						.span(2, 1)
						.applyTo(_chkTopNavigator);
				_chkTopNavigator.setText(Messages.Slideout_SearchViewOptions_Checkbox_IsShowTopNavigator);
				_chkTopNavigator.addSelectionListener(_selectionAdapter_Default);
			}

			/*
			 * Number of pages in the page navigator
			 */
			{
				// checkbox
				final Label label = new Label(group, SWT.CHECK);
				label.setText(Messages.Slideout_SearchViewOptions_Label_NumberOfPages);

				// spinner
				_spinnerNumberOfPages = new Spinner(group, SWT.BORDER);
				GridDataFactory.fillDefaults() //
						.align(SWT.END, SWT.CENTER)
						.applyTo(_spinnerNumberOfPages);
				_spinnerNumberOfPages.setMinimum(3);
				_spinnerNumberOfPages.setMaximum(50);
				_spinnerNumberOfPages.addMouseWheelListener(new MouseWheelListener() {
					public void mouseScrolled(final MouseEvent event) {
						UI.adjustSpinnerValueOnMouseScroll(event);
						onChangeUI(true);
					}
				});
				_spinnerNumberOfPages.addSelectionListener(_selectionAdapter_WithNewSearch);
			}
		}
	}

	private void createUI_99_RestoreDefaults(final Composite parent) {

		final Button btnReset = new Button(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).align(SWT.END, SWT.FILL).applyTo(btnReset);
		btnReset.setText(Messages.App_Action_RestoreDefaults);
		btnReset.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				setDefaults();
			}
		});
	}

	public Shell getShell() {

		if (_shellContainer == null) {
			return null;
		}

		return _shellContainer.getShell();
	}

	@Override
	public Point getToolTipLocation(final Point tipSize) {

		final int itemHeight = _toolTipItemBounds.height;

		final int devX = _toolTipItemBounds.x;
		final int devY = _toolTipItemBounds.y + itemHeight;

		return new Point(devX, devY);
	}

	@Override
	protected Rectangle noHideOnMouseMove() {

		return _toolTipItemBounds;
	}

	private void onChangeUI(final boolean isStartSearch) {

		saveState();

		_searchView.onChangeUI(isStartSearch);
	}

	/**
	 * @param toolTipItemBounds
	 * @param isOpenDelayed
	 */
	public void open(final Rectangle toolTipItemBounds, final boolean isOpenDelayed) {

		if (isToolTipVisible()) {

			return;
		}

		if (isOpenDelayed == false) {

			if (toolTipItemBounds != null) {

				_toolTipItemBounds = toolTipItemBounds;

				showToolTip();
			}

		} else {

			if (toolTipItemBounds == null) {

				// item is not hovered any more

				_canOpenToolTip = false;

				return;
			}

			_toolTipItemBounds = toolTipItemBounds;
			_canOpenToolTip = true;

			if (_isWaitTimerStarted == false) {

				_isWaitTimerStarted = true;

				Display.getCurrent().timerExec(50, _waitTimer);
			}
		}
	}

	private void open_Runnable() {

		_isWaitTimerStarted = false;

		if (_canOpenToolTip) {
			showToolTip();
		}
	}

	private void restoreState() {

		_chkShowDateTime.setSelection(Util.getStateBoolean(
				_state,
				SearchView.STATE_IS_SHOW_DATE_TIME,
				SearchView.STATE_IS_SHOW_DATE_TIME_DEFAULT));

		_chkShowLuceneDocId.setSelection(Util.getStateBoolean(
				_state,
				SearchView.STATE_IS_SHOW_LUCENE_DOC_ID,
				SearchView.STATE_IS_SHOW_LUCENE_DOC_ID_DEFAULT));

		_chkShowItemNumber.setSelection(Util.getStateBoolean(
				_state,
				SearchView.STATE_IS_SHOW_ITEM_NUMBER,
				SearchView.STATE_IS_SHOW_ITEM_NUMBER_DEFAULT));

//		_chkShowScore.setSelection(Util.getStateBoolean(
//				_state,
//				SearchView.STATE_IS_SHOW_SCORE,
//				SearchView.STATE_IS_SHOW_SCORE_DEFAULT));

		_chkTopNavigator.setSelection(Util.getStateBoolean(
				_state,
				SearchView.STATE_IS_SHOW_TOP_NAVIGATOR,
				SearchView.STATE_IS_SHOW_TOP_NAVIGATOR_DEFAULT));

		// sort by date
		final boolean isSortDateAscending = Util.getStateBoolean(
				_state,
				SearchView.STATE_IS_SORT_DATE_ASCENDING,
				SearchView.STATE_IS_SORT_DATE_ASCENDING_DEFAULT);
		_rdoTimeAscending.setSelection(isSortDateAscending);
		_rdoTimeDescending.setSelection(isSortDateAscending == false);

		_spinnerDisplayedResults.setSelection(Util.getStateInt(
				_state,
				SearchView.STATE_HITS_PER_PAGE,
				SearchView.STATE_HITS_PER_PAGE_DEFAULT));

		_spinnerNumberOfPages.setSelection(Util.getStateInt(
				_state,
				SearchView.STATE_NUMBER_OF_PAGES,
				SearchView.STATE_NUMBER_OF_PAGES_DEFAULT));
	}

	private void saveState() {

		_state.put(SearchView.STATE_IS_SHOW_DATE_TIME, _chkShowDateTime.getSelection());
		_state.put(SearchView.STATE_IS_SHOW_ITEM_NUMBER, _chkShowItemNumber.getSelection());
		_state.put(SearchView.STATE_IS_SHOW_LUCENE_DOC_ID, _chkShowLuceneDocId.getSelection());
//		_state.put(SearchView.STATE_IS_SHOW_SCORE, _chkShowScore.getSelection());
		_state.put(SearchView.STATE_IS_SHOW_TOP_NAVIGATOR, _chkTopNavigator.getSelection());
		_state.put(SearchView.STATE_IS_SORT_DATE_ASCENDING, _rdoTimeAscending.getSelection());
		_state.put(SearchView.STATE_HITS_PER_PAGE, _spinnerDisplayedResults.getSelection());
		_state.put(SearchView.STATE_NUMBER_OF_PAGES, _spinnerNumberOfPages.getSelection());
	}

	private void setDefaults() {

		_state.put(SearchView.STATE_IS_SHOW_DATE_TIME, SearchView.STATE_IS_SHOW_DATE_TIME_DEFAULT);
		_state.put(SearchView.STATE_IS_SHOW_ITEM_NUMBER, SearchView.STATE_IS_SHOW_ITEM_NUMBER_DEFAULT);
		_state.put(SearchView.STATE_IS_SHOW_LUCENE_DOC_ID, SearchView.STATE_IS_SHOW_LUCENE_DOC_ID_DEFAULT);
//		_state.put(SearchView.STATE_IS_SHOW_SCORE, SearchView.STATE_IS_SHOW_SCORE_DEFAULT);
		_state.put(SearchView.STATE_IS_SHOW_TOP_NAVIGATOR, SearchView.STATE_IS_SHOW_TOP_NAVIGATOR_DEFAULT);
		_state.put(SearchView.STATE_IS_SORT_DATE_ASCENDING, SearchView.STATE_IS_SORT_DATE_ASCENDING_DEFAULT);
		_state.put(SearchView.STATE_HITS_PER_PAGE, SearchView.STATE_HITS_PER_PAGE_DEFAULT);
		_state.put(SearchView.STATE_NUMBER_OF_PAGES, SearchView.STATE_NUMBER_OF_PAGES_DEFAULT);

		restoreState();

		// update UI
		onChangeUI(true);
	}

}
