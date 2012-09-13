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

import net.tourbook.Messages;
import net.tourbook.common.UI;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.Util;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.window.ToolTip;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Monitor;
import org.eclipse.swt.widgets.Shell;

/**
 * Part of this tooltip is copied from {@link ToolTip}.
 */
public abstract class PhotoToolTipShell {

	/**
	 * how long each tick is when fading out (in ms)
	 */
	private static final int		FADE_TIME_INTERVAL					= 10;

	/**
	 * Number of steps when fading in
	 */
	private static final int		FADE_IN_STEPS						= 20;

	/**
	 * Number of steps when fading out
	 */
	private static final int		FADE_OUT_STEPS						= 40;

	/**
	 * Number of steps before fading out
	 */
	private static final int		FADE_OUT_DELAY_STEPS				= 30;

	private static final int		MOVE_STEPS							= 20;

	private static final int		ALPHA_OPAQUE						= 0xff;

	private static final String		STATE_PHOTO_HORIZ_TOOL_TIP_WIDTH	= "STATE_PHOTO_HORIZ_TOOL_TIP_WIDTH";	//$NON-NLS-1$

	private static final String		STATE_PHOTO_HORIZ_TOOL_TIP_HEIGHT	= "STATE_PHOTO_HORIZ_TOOL_TIP_HEIGHT";	//$NON-NLS-1$

	private static final String		STATE_PHOTO_VERT_TOOL_TIP_WIDTH		= "STATE_PHOTO_VERT_TOOL_TIP_WIDTH";	//$NON-NLS-1$
	private static final String		STATE_PHOTO_VERT_TOOL_TIP_HEIGHT	= "STATE_PHOTO_VERT_TOOL_TIP_HEIGHT";	//$NON-NLS-1$
	private static final int		MIN_SHELL_HORIZ_HEIGHT				= 60;
	private static final int		MIN_SHELL_HORIZ_WIDTH				= 100;

	private static final int		MIN_SHELL_VERT_HEIGHT				= 150;
	private static final int		MIN_SHELL_VERT_WIDTH				= 100;
	private OwnerControlListener	_ownerControlListener;
	private OwnerShellListener		_ownerShellListener;

	private ToolTipShellListener	_ttShellListener;
	private ToolTipControlListener	_ttControlListener;

	private ToolTipDisplayListener	_ttDisplayListener;
	private boolean					_isShellToggled;
	private boolean					_isShellFadingOut;

	private boolean					_isShellFadingIn;
	private Point					_shellStartLocation;
	private Point					_shellEndLocation;

	private int						_animationStepCounter;
	private int						_fadeOutDelayCounter;
	private boolean					_isShellMovingEnabled;
	private int						_horizContentWidth					= MIN_SHELL_HORIZ_WIDTH;

	private int						_horizContentHeight					= MIN_SHELL_HORIZ_HEIGHT;
	private int						_vertContentWidth					= MIN_SHELL_VERT_WIDTH;
	private int						_vertContentHeight					= MIN_SHELL_VERT_HEIGHT;
	private final AnimationTimer	_animationTimer;

	private boolean					_isInShellResize;
	/*
	 * UI resources
	 */
	private Display					_display;

	private ImageGallery			_imageGallery;

	private Composite				_ttContentArea;

	/**
	 * Tooltip shell which is currently be visible
	 */
	private Shell					_visibleShell;

	private AbstractRRShell			_rrShellWithResize;

	private AbstractRRShell			_rrShellNoResize;
	private Control					_ownerControl;

	private final class AnimationTimer implements Runnable {
		@Override
		public void run() {
			animation20_Runnable();
		}
	}

	private class OwnerControlListener implements Listener {
		public void handleEvent(final Event event) {
			onOwnerControlEvent(event);
		}
	}

	private final class OwnerShellListener implements Listener {
		public void handleEvent(final Event event) {
			onOwnerShellEvent(event);
		}
	}

	public class RRShell extends AbstractRRShell {

		public RRShell(final Shell parentShell, final int style, final String shellTitle, final boolean isResizeable) {
			super(parentShell, style, shellTitle, isResizeable);
		}

		@Override
		protected Point getContentSize() {
			return PhotoToolTipShell.this.getContentSize();
		}
	}

	/**
	 * This listener is added to ALL widgets within the tooltip shell.
	 */
	private class ToolTipControlListener implements Listener {
		public void handleEvent(final Event event) {
			onTTControlEvent(event);
		}

	}

	private class ToolTipDisplayListener implements Listener {
		public void handleEvent(final Event event) {

			switch (event.type) {
			case SWT.MouseMove:
				onTTDisplayMouseMove(event);
				break;
			}
		}
	}

	private final class ToolTipShellListener implements Listener {
		public void handleEvent(final Event event) {
			onTTShellEvent(event);
		}
	}

	/**
	 * Create new instance which add TooltipSupport to the widget
	 * 
	 * @param state
	 * @param ownerControl
	 *            the control on whose action the tooltip is shown
	 */
	public PhotoToolTipShell(final Control ownerControl) {

		_ownerControl = ownerControl;
		_display = _ownerControl.getDisplay();

		_ttControlListener = new ToolTipControlListener();
		_ttShellListener = new ToolTipShellListener();
		_ttDisplayListener = new ToolTipDisplayListener();

		_ownerControlListener = new OwnerControlListener();
		_ownerShellListener = new OwnerShellListener();

		_animationTimer = new AnimationTimer();

		ownerControlAddListener();

		initUI(ownerControl);
	}

	/**
	 * This is called after the shell and content area are created.
	 * 
	 * @param shell
	 */
	protected abstract void afterCreateShell(Shell shell);

	private void animation10_Start() {

		final int a = 1;

		if (a == 0) {
			animation10_Start_Simple();
		} else {
			animation10_StartKomplex();
		}

	}

	private void animation10_Start_Simple() {

		if (_isShellFadingIn) {

			// show tool tip

			final Point size = getContentSize();

			final Point defaultLocation = getLocation(size);

			final Point shellLocation = fixupDisplayBounds(size, defaultLocation);

			_visibleShell.setLocation(shellLocation);

//			if (_visibleShell == _shellNoResize) {
			/*
			 * NoResize shell size is not set during resize event because the shell is empty and
			 * size is set to 2,2
			 */
//			_rrShellNoResize.setSize(size);
//			_shellNoResize.pack();
//			_shellNoResize.layout();

//			}

			reparentShell(_rrShellNoResize);

			_rrShellNoResize.setAlpha(0xff);

			setShellVisible(true);

		} else {

			// hide tooltip

			setShellVisible(false);
		}
	}

	private void animation10_StartKomplex() {

		// fading out has no movement

		if (_isShellFadingIn) {

			// set fading in location

			final Point size = _visibleShell.getSize();

			_shellEndLocation = fixupDisplayBounds(size, getLocation(size));

			if (_visibleShell.isVisible()) {

				// shell is already visible, move from the current position to the target position

				_shellStartLocation = _visibleShell.getLocation();

			} else {

				// shell is not visible, set position directly without moving animation, do only fading animation

				_shellStartLocation = _shellEndLocation;

				_visibleShell.setLocation(_shellStartLocation);

				reparentShell(_rrShellNoResize);

				setShellVisible(true);
			}

			_animationStepCounter = 0;

		}

		if (_isShellFadingOut) {

			_fadeOutDelayCounter = 0;
		}

		_isShellMovingEnabled = true;

		_display.timerExec(FADE_TIME_INTERVAL, _animationTimer);
	}

	private void animation20_Runnable() {

		try {

			if (_visibleShell == null || _visibleShell.isDisposed()) {
				return;
			}

			final boolean isShellHidden = _visibleShell.isVisible() == false;
			if (isShellHidden) {
				return;
			}

			final int currentAlpha = _visibleShell.getAlpha();
			int newAlpha = ALPHA_OPAQUE;

			if (_isShellFadingIn) {

				final int shellStartX = _shellStartLocation.x;
				final int shellStartY = _shellStartLocation.y;
				final int shellEndX = _shellEndLocation.x;
				final int shellEndY = _shellEndLocation.y;

				final Point shellCurrentLocation = _visibleShell.getLocation();

				final boolean isInTarget = shellCurrentLocation.x == shellEndX && shellCurrentLocation.y == shellEndY;

				final int diffAlpha = ALPHA_OPAQUE / FADE_IN_STEPS;

				newAlpha = currentAlpha + diffAlpha;
				if (newAlpha > ALPHA_OPAQUE) {
					newAlpha = ALPHA_OPAQUE;
				}

				if (isInTarget && newAlpha == ALPHA_OPAQUE) {

					// target is reached and fully visible, stop animation

					_visibleShell.setAlpha(ALPHA_OPAQUE);

					_isShellFadingIn = false;

					return;

				} else {

					// move to target

					_animationStepCounter++;

					final int diffX = shellStartX - shellEndX;
					final int diffY = shellStartY - shellEndY;

					final double moveX = (double) diffX / MOVE_STEPS * _animationStepCounter;
					final double moveY = (double) diffY / MOVE_STEPS * _animationStepCounter;

					final int shellCurrentX = (int) (shellStartX - moveX);
					final int shellCurrentY = (int) (shellStartY - moveY);

					if (_isShellMovingEnabled) {

						// when mouse is over this tooltip the shell is not moved

						_visibleShell.setLocation(new Point(shellCurrentX, shellCurrentY));
					}

				}

			} else if (_isShellFadingOut) {

				if (_fadeOutDelayCounter++ < FADE_OUT_DELAY_STEPS) {

					// delay fade out

					_display.timerExec(FADE_TIME_INTERVAL, _animationTimer);

					return;
				}

				final int alphaDiff = ALPHA_OPAQUE / FADE_OUT_STEPS;

				newAlpha = currentAlpha - alphaDiff;

				if (newAlpha <= 0) {

					// shell is not visible any more, hide it now

					_visibleShell.setAlpha(0);
					setShellVisible(false);

					_isShellFadingOut = false;

					return;
				}
			}

			_visibleShell.setAlpha(newAlpha);

			_display.timerExec(FADE_TIME_INTERVAL, _animationTimer);

		} catch (final Exception err) {
			StatusUtil.log(err);
		}
	}

	/**
	 * Creates the content area of the the tooltip.
	 * 
	 * @param parent
	 *            the parent of the content area
	 * @return the content area created
	 */
	protected abstract Composite createToolTipContentArea(Composite parent);

	/**
	 * Create a shell but do not display it
	 * 
	 * @return Returns <code>true</code> when shell is created.
	 */
	private void createUI() {

		if (_visibleShell != null && !_visibleShell.isDisposed()) {
			// shell is already created
			return;
		}

		/*
		 * resize shell
		 */
		_rrShellWithResize = new RRShell(_ownerControl.getShell(), //
				SWT.ON_TOP //
//						| SWT.TOOL
						| SWT.RESIZE
						| SWT.NO_FOCUS,
				Messages.Photo_Tooltip_Label_ShellWithResize,
				true);

		final Shell shellWithResize = _rrShellWithResize.getShell();
		shellWithResize.addControlListener(new ControlAdapter() {
			@Override
			public void controlResized(final ControlEvent e) {
				onTTShellResize(e);
			}
		});

		ttShellAddListener(shellWithResize);

		/*
		 * no resize shell
		 */
		_rrShellNoResize = new RRShell(_ownerControl.getShell(), //
				SWT.ON_TOP //
//						| SWT.TOOL
						| SWT.NO_FOCUS,
				Messages.Photo_Tooltip_Label_ShellNoResize,
				false);

		ttShellAddListener(_rrShellNoResize.getShell());

		_visibleShell = _rrShellNoResize.getShell();

		ownerShellAddListener();

		// create UI
		_ttContentArea = createToolTipContentArea(_visibleShell);

		ttControlsAddListener(_visibleShell);

		afterCreateShell(_visibleShell);
	}

	private void delay() {
		try {
			Thread.sleep(500);
		} catch (final InterruptedException e) {
			e.printStackTrace();
		}
	}

	private Point fixupDisplayBounds(final Point tipSize, final Point location) {

		final Rectangle displayBounds = getDisplayBounds(location);
		final Point rightBottomBounds = new Point(tipSize.x + location.x, tipSize.y + location.y);

		if (!(displayBounds.contains(location) && displayBounds.contains(rightBottomBounds))) {

			if (rightBottomBounds.x > displayBounds.x + displayBounds.width) {
				location.x -= rightBottomBounds.x - (displayBounds.x + displayBounds.width);
			}

			if (rightBottomBounds.y > displayBounds.y + displayBounds.height) {
// ignore when tt is below the bottom, force the user to resize the tt
//				location.y -= rightBottomBounds.y - (displayBounds.y + displayBounds.height);
			}

			if (location.x < displayBounds.x) {
				location.x = displayBounds.x;
			}

			if (location.y < displayBounds.y) {
				location.y = displayBounds.y;
			}
		}

		return location;
	}

	/**
	 * @return Returns size of the tooltip content
	 */
	Point getContentSize() {
		if (isVerticalGallery()) {
			return new Point(_vertContentWidth, _vertContentHeight);
		} else {
			return new Point(_horizContentWidth, _horizContentHeight);
		}
	}

	private Rectangle getDisplayBounds(final Point location) {

		Rectangle displayBounds;
		final Monitor[] allMonitors = _ownerControl.getDisplay().getMonitors();

		if (allMonitors.length > 1) {
			// By default present in the monitor of the control
			displayBounds = _ownerControl.getMonitor().getBounds();
			final Point p = new Point(location.x, location.y);

			// Search on which monitor the event occurred
			Rectangle tmp;
			for (final Monitor element : allMonitors) {
				tmp = element.getBounds();
				if (tmp.contains(p)) {
					displayBounds = tmp;
					break;
				}
			}

		} else {
			displayBounds = _ownerControl.getDisplay().getBounds();
		}

		return displayBounds;
	}

	/**
	 * Get tooltip location.
	 * 
	 * @param size
	 *            Tooltip size
	 * @return Returns location relative to the device.
	 */
	protected abstract Point getLocation(Point size);

	protected Shell getToolTipShell() {
		return _visibleShell;
	}

	/**
	 * Hide the currently active tool tip
	 */
	public void hide() {
		ttHide(null);
	}

	private void initUI(final Control ownerControl) {

//		final PixelConverter pc = new PixelConverter(ownerControl);

	}

	abstract boolean isVerticalGallery();

	private void onOwnerControlEvent(final Event event) {

		if (_ownerControl == null || _ownerControl.isDisposed()) {
			return;
		}

		switch (event.type) {
		case SWT.Dispose:

			ttDispose(event);

			ownerControlsRemoveListener();

			break;

		case SWT.Resize:

			showShellWhenVisible();

			break;
		}
	}

	private void onOwnerShellEvent(final Event event) {

		if (_visibleShell == null || _visibleShell.isDisposed()) {
			return;
		}

		switch (event.type) {
		case SWT.Deactivate:

			_display.asyncExec(new Runnable() {

				public void run() {

					// hide tooltip when another shell is activated

					if (_display.getActiveShell() != _visibleShell) {
						ttHide(event);
					}
				}
			});
			break;

		case SWT.Move:

			showShellWhenVisible();
			break;
		}
	}

	protected abstract void onStartHide();

	private void onTTControlEvent(final Event event) {

		if (_visibleShell == null || _visibleShell.isDisposed()) {
			return;
		}

		switch (event.type) {
		case SWT.MouseEnter:

			// stop animation
			if (_isShellFadingIn || _isShellFadingOut) {
				_isShellMovingEnabled = false;
			}

			reparentShell(_rrShellWithResize);

			break;

		case SWT.MouseExit:

			if (_isShellToggled) {

				// do this only once

				_isShellToggled = false;
			}

			break;
		}
	}

	private void onTTDisplayMouseMove(final Event event) {

		/*
		 * check tooltip area as it is done in the original code because the current tooltip shell
		 * check is not working always (often but sometimes) correctly
		 */

		/**
		 * !!! this adjustment do not work on Linux because the tooltip gets hidden when the mouse
		 * tries to mover over the tooltip <br>
		 * <br>
		 * it seems to work on windows and linux with margin 1, when set to 0 the tooltip do
		 * sometime not be poped up again and the i-icons is not deaktivated<br>
		 * wolfgang 2010-07-23
		 */

		final Rectangle ttShellRect = _visibleShell.getBounds();
		final int margin = 10;

		ttShellRect.x -= margin;
		ttShellRect.y -= margin;
		ttShellRect.width += 2 * margin;
		ttShellRect.height += 2 * margin;

		final Point cursorLocation = _display.getCursorLocation();

		if (!ttShellRect.contains(cursorLocation)) {

			// mouse is not within the tooltip shell rectangle

			reparentShell(_rrShellNoResize);

			ttHide(event);
		}
	}

	private void onTTDisplayMouseMove_OLD(final Event event) {

		boolean isHide = false;

		// get control which is hovered with the mouse after the exit, can be null
		final Control hoveredControl = _display.getCursorControl();

		if (hoveredControl == null) {

//			System.out.println(UI.timeStampNano() + " exit 0");
//			// TODO remove SYSTEM.OUT.PRINTLN

			isHide = true;

		} else {

			/*
			 * check if the hovered control is the owner control, if not, hide the tooltip
			 */
			Control hoveredParent = hoveredControl;
//				final Control hoveredToolTip = _imageGallery.getGalleryToolTipShell();

			boolean isKeepVisible = false;

			// move up child-parent hierarchy until shell is reached
			while (true) {

				if (hoveredParent == _visibleShell) {

					// mouse is hovering in this tooltip

					isKeepVisible = true;

					System.out.println(UI.timeStampNano() + " exit 1 no hide");
					// TODO remove SYSTEM.OUT.PRINTLN

					break;
				}

				if (hoveredParent == _ownerControl) {

					// mouse is over the owner control

					isKeepVisible = true;

					System.out.println(UI.timeStampNano() + " exit 2 no hide");
					// TODO remove SYSTEM.OUT.PRINTLN

					break;
				}

				hoveredParent = hoveredParent.getParent();

				if (hoveredParent == null) {

					// mouse has left the tooltip and the owner control

					System.out.println(UI.timeStampNano() + " exit 3 hide");
					// TODO remove SYSTEM.OUT.PRINTLN

					isHide = true;
					break;
				}
			}

//			if (isKeepVisible == false && isHide == false) {
//
//				/*
//				 * check tooltip area as it is done in the original code because the current tooltip
//				 * shell check is not working always (often but sometimes) correctly
//				 */
//
//				/**
//				 * !!! this adjustment do not work on Linux because the tooltip gets hidden when the
//				 * mouse tries to mover over the tooltip <br>
//				 * <br>
//				 * it seems to work on windows and linux with margin 1, when set to 0 the tooltip do
//				 * sometime not be poped up again and the i-icons is not deaktivated<br>
//				 * wolfgang 2010-07-23
//				 */
//
//				final Rectangle ttShellRect = _visibleShell.getBounds();
//				final int margin = 5;
//
//				ttShellRect.x -= margin;
//				ttShellRect.y -= margin;
//				ttShellRect.width += 2 * margin;
//				ttShellRect.height += 2 * margin;
//
//				final Point cursorLocation = _display.getCursorLocation();
//
//				if (!ttShellRect.contains(cursorLocation)) {
//
//					// mouse is not within the tooltip shell rectangle
//
//					System.out.println(UI.timeStampNano() + "\texit 4 hide"); //$NON-NLS-1$
//					// TODO remove SYSTEM.OUT.PRINTLN
//
//					isHide = true;
//
//				} else {
//
//					System.out.println(UI.timeStampNano() + "\texit 4 no hide"); //$NON-NLS-1$
//					// TODO remove SYSTEM.OUT.PRINTLN
//				}
//			}
		}

		if (isHide) {
			ttHide(event);
		}

	}

	private void onTTShellEvent(final Event event) {

		switch (event.type) {
		case SWT.Deactivate:

			if (_visibleShell != null
					&& !_visibleShell.isDisposed()
					&& _ownerControl != null
					&& !_ownerControl.isDisposed()) {

				_display.asyncExec(new Runnable() {

					public void run() {

						// hide tooltip when another shell is activated

						// check again
						if (_visibleShell == null
								|| _visibleShell.isDisposed()
								|| _ownerControl == null
								|| _ownerControl.isDisposed()) {
							return;
						}

						if (_ownerControl.getShell() == _visibleShell.getDisplay().getActiveShell()) {

							// don't hide when main window is active
							return;
						}

						ttHide(event);
					}
				});
			}

			break;

		case SWT.Dispose:

			break;

		}

	}

	private void onTTShellResize(final ControlEvent event) {

		if (_isInShellResize) {
			return;
		}

		System.out.println(UI.timeStampNano() + " onTTShellResize()"); //$NON-NLS-1$
		// TODO remove SYSTEM.OUT.PRINTLN

		final Shell resizeShell = _rrShellWithResize.getShell();

		final Rectangle shellClientArea = resizeShell.getClientArea();

		int newContentWidth = shellClientArea.width;
		int newContentHeight = shellClientArea.height;

		final Point newShellLocation = resizeShell.getLocation();

		final Rectangle displayBounds = getDisplayBounds(newShellLocation);

		// ensure tooltip is not too large
		final double maxHeight = displayBounds.height * 0.8;
		final double maxWidth = displayBounds.width * 0.95;

		boolean isResizeAdjusted = false;

		if (newContentHeight > maxHeight) {
			newContentHeight = (int) maxHeight;
			isResizeAdjusted = true;
		} else if (newContentHeight < MIN_SHELL_HORIZ_HEIGHT) {
			newContentHeight = MIN_SHELL_HORIZ_HEIGHT;
			isResizeAdjusted = true;
		}

		if (newContentWidth > maxWidth) {
			newContentWidth = (int) maxWidth;
			isResizeAdjusted = true;
		} else if (newContentWidth < MIN_SHELL_HORIZ_WIDTH) {
			newContentWidth = MIN_SHELL_HORIZ_WIDTH;
			isResizeAdjusted = true;
		}

		if (isVerticalGallery()) {
			_vertContentWidth = newContentWidth;
			_vertContentHeight = newContentHeight;
		} else {
			_horizContentWidth = newContentWidth;
			_horizContentHeight = newContentHeight;
		}

		if (isResizeAdjusted) {
			_isInShellResize = true;
			{
				_rrShellWithResize.setSize(newContentWidth, newContentHeight);
			}
			_isInShellResize = false;
		}
	}

	/**
	 * Activate tooltip support for this control
	 */
	private void ownerControlAddListener() {

		ownerControlsRemoveListener();

		_ownerControl.addListener(SWT.Dispose, _ownerControlListener);
		_ownerControl.addListener(SWT.Resize, _ownerControlListener);
	}

	/**
	 * Deactivate tooltip support for the underlying control
	 */
	private void ownerControlsRemoveListener() {

		_ownerControl.removeListener(SWT.Dispose, _ownerControlListener);
		_ownerControl.removeListener(SWT.Resize, _ownerControlListener);
	}

	private void ownerShellAddListener() {

		final Shell ownerShell = _ownerControl.getShell();

		ownerShell.addListener(SWT.Deactivate, _ownerShellListener);
		ownerShell.addListener(SWT.Move, _ownerShellListener);
	}

	private void ownerShellRemoveListener() {

		final Shell ownerShell = _ownerControl.getShell();

		ownerShell.removeListener(SWT.Deactivate, _ownerShellListener);
		ownerShell.removeListener(SWT.Move, _ownerShellListener);
	}

	private void passOnEvent(final Shell shell, final Event event) {

		if (_ownerControl != null
				&& !_ownerControl.isDisposed()
				&& event != null
				&& event.widget != _ownerControl
				&& event.type == SWT.MouseDown) {

			// the following was left in order to fix bug 298770 with minimal change. In 3.7, the complete method should be removed.
			shell.close();
		}
	}

	/**
	 * Reparent shell
	 * 
	 * @param newReparentedShell
	 *            Shell which should be used to display {@link #_ttContentArea}.
	 */
	private void reparentShell(final AbstractRRShell newReparentedShell) {

		if (_visibleShell == newReparentedShell.getShell()) {
			// shell is already visible
			return;
		}

		final Shell prevShell = _visibleShell;

		ttControlsRemoveListener(prevShell);

		_visibleShell = newReparentedShell.getShell();

		if (newReparentedShell == _rrShellWithResize) {

			// setup resize shell

			_rrShellWithResize.reparentFromOtherShell(_rrShellNoResize, _ttContentArea);

		} else {

			// setup no resize shell

			_rrShellNoResize.reparentFromOtherShell(_rrShellWithResize, _ttContentArea);

//			final Rectangle prevShellBounds = prevShell.getBounds();
//
//			final int currentAlpha = _rrShellNoResize.getAlpha();
//			_rrShellNoResize.setAlpha(0x0);
//
//			_rrShellNoResize.setVisible(true);
//
//			_rrShellNoResize.setLocation(//
//					prevShellBounds.x - _shellTrimWidth - 0,
//					prevShellBounds.y - _shellTrimHeight - 0 //
//			);
//			_rrShellNoResize.setSize(getContentSize());
//
//			// reparent UI container
//			_ttContentArea.setParent(_rrShellNoResize);
//
//			_rrShellNoResize.setAlpha(currentAlpha);
//
//			prevShell.setAlpha(0);
		}

		// hide previous shell
		prevShell.setVisible(false);

		ttControlsAddListener(newReparentedShell.getShell());
	}

	protected void restoreState(final IDialogSettings state) {

		/*
		 * get horizontal gallery values
		 */
		_horizContentWidth = Util.getStateInt(state, STATE_PHOTO_HORIZ_TOOL_TIP_WIDTH, 300);
		_horizContentHeight = Util.getStateInt(state, STATE_PHOTO_HORIZ_TOOL_TIP_HEIGHT, 150);

		// ensure min values
		if (_horizContentWidth < MIN_SHELL_HORIZ_WIDTH) {
			_horizContentWidth = MIN_SHELL_HORIZ_WIDTH;
		}

		if (_horizContentHeight < MIN_SHELL_HORIZ_HEIGHT) {
			_horizContentHeight = MIN_SHELL_HORIZ_HEIGHT;
		}

		/*
		 * get vertical gallery values
		 */
		_vertContentWidth = Util.getStateInt(state, STATE_PHOTO_VERT_TOOL_TIP_WIDTH, 400);
		_vertContentHeight = Util.getStateInt(state, STATE_PHOTO_VERT_TOOL_TIP_HEIGHT, 250);

		// ensure min values
		if (_vertContentWidth < MIN_SHELL_VERT_WIDTH) {
			_vertContentWidth = MIN_SHELL_VERT_WIDTH;
		}

		if (_vertContentHeight < MIN_SHELL_VERT_HEIGHT) {
			_vertContentHeight = MIN_SHELL_VERT_HEIGHT;
		}
	}

	protected void saveState(final IDialogSettings state) {

		state.put(STATE_PHOTO_HORIZ_TOOL_TIP_WIDTH, _horizContentWidth);
		state.put(STATE_PHOTO_HORIZ_TOOL_TIP_HEIGHT, _horizContentHeight);
		state.put(STATE_PHOTO_VERT_TOOL_TIP_WIDTH, _vertContentWidth);
		state.put(STATE_PHOTO_VERT_TOOL_TIP_HEIGHT, _vertContentHeight);
	}

	/**
	 * Set image gallery, this is done, after {@link #createToolTipContentArea(Event, Composite)} is
	 * executed.
	 * 
	 * @param imageGallery
	 */
	protected void setImageGallery(final ImageGallery imageGallery) {
		_imageGallery = imageGallery;
	}

	protected void setIsShellToggle() {
		_isShellToggled = true;
	}

	private void setShellVisible(final boolean isVisible) {

		_visibleShell.setVisible(isVisible);

		if (isVisible) {
			ttDisplayAddListener();
		} else {
			ttDisplayRemoveListener();
		}
	}

	protected void showAtDefaultLocation() {

		if (_visibleShell == null || _visibleShell.isDisposed()) {
			return;
		}

		final Point size = _visibleShell.getSize();
		final Point fixedLocation = fixupDisplayBounds(size, getLocation(size));

		_visibleShell.setLocation(fixedLocation);
	}

	protected boolean showShell() {

		/*
		 * show tooltip only when this is the active shell, this check is necessary that when a tour
		 * chart is opened in a dialog (e.g. adjust altitude) that a hidden tour chart tooltip in
		 * the tour chart view is also displayed
		 */
//		if (_display.getActiveShell() != _ownerControl.getShell() || _ownerControl.isVisible() == false) {
//			return false;
//		}

		createUI();

		ttShow();

		return true;
	}

	private void showShellWhenVisible() {

		if (_visibleShell == null || _visibleShell.isDisposed() || _visibleShell.isVisible() == false) {
			return;
		}

		ttShow();
	}

	/**
	 * ########################### Recursive #########################################<br>
	 * <p>
	 * Add listener to all controls within the tooltip
	 * <p>
	 * ########################### Recursive #########################################<br>
	 * 
	 * @param control
	 */
	private void ttControlsAddListener(final Control control) {

		control.addListener(SWT.MouseDown, _ttControlListener);
		control.addListener(SWT.MouseUp, _ttControlListener);
		control.addListener(SWT.MouseMove, _ttControlListener);
		control.addListener(SWT.MouseExit, _ttControlListener);
		control.addListener(SWT.MouseEnter, _ttControlListener);

		if (control instanceof Composite) {
			final Control[] children = ((Composite) control).getChildren();
			for (final Control child : children) {
				ttControlsAddListener(child);
			}
		}
	}

	/**
	 * ########################### Recursive #########################################<br>
	 * <p>
	 * Removes listener from all controls within the tooltip
	 * <p>
	 * ########################### Recursive #########################################<br>
	 * 
	 * @param control
	 */
	private void ttControlsRemoveListener(final Control control) {

		control.removeListener(SWT.MouseDown, _ttControlListener);
		control.removeListener(SWT.MouseUp, _ttControlListener);
		control.removeListener(SWT.MouseMove, _ttControlListener);
		control.removeListener(SWT.MouseExit, _ttControlListener);
		control.removeListener(SWT.MouseEnter, _ttControlListener);

		if (control instanceof Composite) {
			final Control[] children = ((Composite) control).getChildren();
			for (final Control child : children) {
				ttControlsRemoveListener(child);
			}
		}
	}

	private void ttDisplayAddListener() {

		_display.addFilter(SWT.MouseMove, _ttDisplayListener);
	}

	private void ttDisplayRemoveListener() {

		_display.removeFilter(SWT.MouseMove, _ttDisplayListener);
	}

	private void ttDispose(final Event event) {

		if (_visibleShell == null || _visibleShell.isDisposed()) {
			return;
		}

		// hide tooltip definitively

		ownerShellRemoveListener();
		ttDisplayRemoveListener();

		passOnEvent(_rrShellWithResize.getShell(), event);
		_rrShellWithResize.dispose();

		passOnEvent(_rrShellNoResize.getShell(), event);
		_rrShellNoResize.dispose();
	}

	private void ttHide(final Event event) {

		onStartHide();

		if (_visibleShell == null || _visibleShell.isDisposed() || _visibleShell.isVisible() == false) {
			return;
		}

		if (_isShellFadingOut) {

			// shell is already fading out
			return;
		}

		// shell is not yet fading out

		_isShellFadingIn = false;
		_isShellFadingOut = true;

		animation10_Start();
	}

	private void ttShellAddListener(final Shell shell) {
		// hide tooltip if user selects outside of the shell
		shell.addListener(SWT.Deactivate, _ttShellListener);
		shell.addListener(SWT.Dispose, _ttShellListener);
	}

	private void ttShow() {

		// shell is not yet fading in

		_isShellFadingIn = true;
		_isShellFadingOut = false;

		animation10_Start();
	}
}
