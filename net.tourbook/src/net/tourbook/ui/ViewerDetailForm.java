/*******************************************************************************
 * Copyright (C) 2005, 2009  Wolfgang Schramm and Contributors
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
package net.tourbook.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

/**
 * The class provides the ability to keep the width of the viewer when the parent is resized
 */
public class ViewerDetailForm {

	private static final int	MINIMUM_WIDTH	= 100;

	private Composite			fParent;

	private Control				fMaximizedControl;

	private Control				fViewer;
	private Control				fSash;
	private Control				fDetail;

	private Integer				fViewerWidth;
	private FormData			fSashLayoutData;

	private boolean				isInitialResize;

	public ViewerDetailForm(final Composite parent, final Control viewer, final Control sash, final Control detail) {
		this(parent, viewer, sash, detail, 50);
	}

	public ViewerDetailForm(final Composite parent,
							final Control viewer,
							final Control sash,
							final Control detail,
							final int leftWidth) {

		fParent = parent;
		fViewer = viewer;
		fDetail = detail;
		fSash = sash;

		parent.setLayout(new FormLayout());

		final FormAttachment topAttachment = new FormAttachment(0, 0);
		final FormAttachment bottomAttachment = new FormAttachment(100, 0);

		final FormData viewerLayoutData = new FormData();
		viewerLayoutData.left = new FormAttachment(0, 0);
		viewerLayoutData.right = new FormAttachment(sash, 0);
		viewerLayoutData.top = topAttachment;
		viewerLayoutData.bottom = bottomAttachment;
		viewer.setLayoutData(viewerLayoutData);

		fSashLayoutData = new FormData();
		fSashLayoutData.left = new FormAttachment(leftWidth, 0);
		fSashLayoutData.top = topAttachment;
		fSashLayoutData.bottom = bottomAttachment;
		sash.setLayoutData(fSashLayoutData);

		final FormData detailLayoutData = new FormData();
		detailLayoutData.left = new FormAttachment(sash, 0);
		detailLayoutData.right = new FormAttachment(100, 0);
		detailLayoutData.top = topAttachment;
		detailLayoutData.bottom = bottomAttachment;
		detail.setLayoutData(detailLayoutData);

		viewer.addControlListener(new ControlAdapter() {
			@Override
			public void controlResized(final ControlEvent e) {
				onResize();
			}
		});

		detail.addControlListener(new ControlAdapter() {
			@Override
			public void controlResized(final ControlEvent e) {
				onResize();
			}
		});

		sash.addListener(SWT.Selection, new Listener() {
			public void handleEvent(final Event e) {

				final Rectangle sashRect = sash.getBounds();
				final Rectangle parentRect = parent.getClientArea();

				final int right = parentRect.width - sashRect.width - MINIMUM_WIDTH;
				final int sashWidth = Math.max(Math.min(e.x, right), MINIMUM_WIDTH);

				if (sashWidth != sashRect.x) {
					fSashLayoutData.left = new FormAttachment(0, sashWidth);
					parent.layout();
				}

				fViewerWidth = sashWidth;
			}
		});
	}

	private void onResize() {

		if (isInitialResize == false) {

			/*
			 * set the initial width for the viewer sash, this is a bit of hacking but it works
			 */

			// execute only the first time
			isInitialResize = true;

			Integer viewerWidth = fViewerWidth;

			if (viewerWidth == null) {
				fViewerWidth = viewerWidth = fViewer.computeSize(SWT.DEFAULT, SWT.DEFAULT).x;
			}

			fSashLayoutData.left = new FormAttachment(0, viewerWidth);

			fParent.layout();

			// System.out.println("isInit==false: "+viewerWidth);

		} else {

			if (fMaximizedControl != null) {

				if (fMaximizedControl == fViewer) {

					fSashLayoutData.left = new FormAttachment(100, 0);
					fParent.layout();

				} else if (fMaximizedControl == fDetail) {
					fSashLayoutData.left = new FormAttachment(0, -fSash.getSize().x);
					fParent.layout();
				}

			} else {

				if (fViewerWidth == null) {
					fSashLayoutData.left = new FormAttachment(50, 0);
				} else {

					final Rectangle parentRect = fParent.getClientArea();

					// set the minimum width

					int viewerWidth = 0;

					if (fViewerWidth + MINIMUM_WIDTH >= parentRect.width) {

						viewerWidth = Math.max(parentRect.width - MINIMUM_WIDTH, 50);

					} else {
						viewerWidth = fViewerWidth;
					}

					fSashLayoutData.left = new FormAttachment(0, viewerWidth);
				}
				fParent.layout();
			}

			// System.out.println("isInit==true: "+fSashData.left);
		}
	}

	/**
	 * sets the control which is maximized, set <code>null</code> to reset the maximized control
	 * 
	 * @param control
	 */
	public void setMaximizedControl(final Control control) {
		fMaximizedControl = control;
		onResize();
	}

	/**
	 * @param viewerWidth
	 */
	public void setViewerWidth(final Integer viewerWidth) {
		fViewerWidth = viewerWidth == null ? null : Math.max(MINIMUM_WIDTH, viewerWidth);
		onResize();
	}
}
