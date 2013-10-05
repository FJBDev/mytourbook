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
package net.tourbook.map3.action;

import gov.nasa.worldwind.awt.WorldWindowGLCanvas;

import java.util.Map;

import javax.media.opengl.GL;
import javax.media.opengl.GLCapabilities;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.map3.view.Map3Manager;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class ActionHandlerWorldWindDiagnostics extends AbstractHandler {

	private final IDialogSettings				_state		= TourbookPlugin.getStateSection(//
																	getClass().getCanonicalName());

	private static final WorldWindowGLCanvas	_wwCanvas	= Map3Manager.getWWCanvas();

	private static Attr[]						attrs		= new Attr[] {
			new Attr(GL.GL_STENCIL_BITS, "stencil bits"), //$NON-NLS-1$
			new Attr(GL.GL_DEPTH_BITS, "depth bits"), //$NON-NLS-1$
			new Attr(GL.GL_MAX_TEXTURE_UNITS, "max texture units"), //$NON-NLS-1$
			new Attr(GL.GL_MAX_TEXTURE_IMAGE_UNITS_ARB, "max texture image units"), //$NON-NLS-1$
			new Attr(GL.GL_MAX_TEXTURE_COORDS_ARB, "max texture coords"), //$NON-NLS-1$
			new Attr(GL.GL_MAX_TEXTURE_SIZE, "max texture size"), //$NON-NLS-1$
			new Attr(GL.GL_MAX_ELEMENTS_INDICES, "max elements indices"), //$NON-NLS-1$
			new Attr(GL.GL_MAX_ELEMENTS_VERTICES, "max elements vertices"), //$NON-NLS-1$
			new Attr(GL.GL_MAX_LIGHTS, "max lights")		};										//$NON-NLS-1$

	private static class Attr {

		private Object	attr;

		private String	name;

		private Attr(final Object attr, final String name) {
			this.attr = attr;
			this.name = name;
		}
	}

	private class DialogInfo extends Dialog {

		private Text	_txtInfo;

		protected DialogInfo(final Shell parentShell) {

			super(parentShell);

			// make dialog resizable
			setShellStyle(getShellStyle() | SWT.RESIZE | SWT.MAX);
		}

		@Override
		protected void configureShell(final Shell shell) {

			super.configureShell(shell);

//			Dialog_WorldWind_Diagnostics=World Wind Diagnostics

			shell.setText("World Wind Diagnostics"); //$NON-NLS-1$
		}

		@Override
		protected void createButtonsForButtonBar(final Composite parent) {

			// create OK button
			createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
		}

		@Override
		protected Control createDialogArea(final Composite parent) {

			final Control container = createUI(parent);

			BusyIndicator.showWhile(Display.getDefault(), new Runnable() {
				public void run() {
					getDiagnosticsData();
				}
			});

			return container;
		}

		private Control createUI(final Composite parent) {
			final Composite container = new Composite(parent, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
			GridLayoutFactory.swtDefaults().spacing(20, 0).applyTo(container);
			{
				_txtInfo = new Text(container, SWT.BORDER | SWT.MULTI | SWT.READ_ONLY | SWT.V_SCROLL | SWT.H_SCROLL);
				GridDataFactory.fillDefaults().grab(true, true).applyTo(_txtInfo);
			}

			return container;
		}

		private void getDiagnosticsData() {

			final StringBuilder sb = new StringBuilder();

			sb.append(gov.nasa.worldwind.Version.getVersion() + UI.NEW_LINE);

			getDiagText_GL(sb);
			getDiagText_JOGL(sb);
			getDiagText_System(sb);

			_txtInfo.setText(sb.toString());
		}

		private void getDiagText_GL(final StringBuilder sb) {

			//			final javax.media.opengl.GL gl = GLContext.getCurrent().getGL();

			final GL gl = _wwCanvas.getGL();

			final GLCapabilities caps = _wwCanvas.getChosenGLCapabilities();

			sb.append(UI.NEW_LINE3);
			sb.append("OpenGL Values"); //$NON-NLS-1$
			sb.append(UI.NEW_LINE2);

			final String oglVersion = gl.glGetString(GL.GL_VERSION);
			sb.append("OpenGL version: " + oglVersion + UI.NEW_LINE); //$NON-NLS-1$

			String value = UI.EMPTY_STRING;
			final int[] intVals = new int[1];
			for (final Attr attr : attrs) {
				if (attr.attr instanceof Integer) {
					gl.glGetIntegerv((Integer) attr.attr, intVals, 0);
					value = Integer.toString(intVals[0]);
				}

				sb.append(attr.name + ": " + value + UI.NEW_LINE); //$NON-NLS-1$
			}

			final String extensionString = gl.glGetString(GL.GL_EXTENSIONS);
			if (extensionString != null) {

				final String[] extensions = extensionString.split(UI.SPACE);
				sb.append("Extensions\n"); //$NON-NLS-1$
				for (final String ext : extensions) {
					sb.append(UI.SPACE4 + ext + UI.NEW_LINE);
				}
			}
		}

		private void getDiagText_JOGL(final StringBuilder sb) {

			sb.append(UI.NEW_LINE3);
			sb.append("JOGL Values"); //$NON-NLS-1$
			sb.append(UI.NEW_LINE2);

			final String pkgName = "javax.media.opengl"; //$NON-NLS-1$
			try {

				final Class<?> glClass = getClass().getClassLoader().loadClass(pkgName + ".GL"); //$NON-NLS-1$

				final Package p = Package.getPackage(pkgName);
				if (p == null) {
					sb.append("WARNING: Package.getPackage(" + pkgName + ") is null\n"); //$NON-NLS-1$ //$NON-NLS-2$
				} else {
					sb.append(p + UI.NEW_LINE);
					sb.append("Specification Title = " + p.getSpecificationTitle() + UI.NEW_LINE); //$NON-NLS-1$
					sb.append("Specification Vendor = " + p.getSpecificationVendor() + UI.NEW_LINE); //$NON-NLS-1$
					sb.append("Specification Version = " + p.getSpecificationVersion() + UI.NEW_LINE); //$NON-NLS-1$
					sb.append("Implementation Vendor = " + p.getImplementationVendor() + UI.NEW_LINE); //$NON-NLS-1$
					sb.append("Implementation Version = " + p.getImplementationVersion() + UI.NEW_LINE); //$NON-NLS-1$
				}
			} catch (final ClassNotFoundException e) {
				sb.append("Unable to load " + pkgName + UI.NEW_LINE); //$NON-NLS-1$
			}
		}

		private void getDiagText_System(final StringBuilder sb) {

			sb.append(UI.NEW_LINE3);
			sb.append("System Properties"); //$NON-NLS-1$
			sb.append(UI.NEW_LINE2);

			sb.append("Processors: " + Runtime.getRuntime().availableProcessors() + UI.NEW_LINE); //$NON-NLS-1$
			sb.append("Free memory: " + Runtime.getRuntime().freeMemory() + " bytes\n"); //$NON-NLS-1$ //$NON-NLS-2$
			sb.append("Max memory: " + Runtime.getRuntime().maxMemory() + " bytes\n"); //$NON-NLS-1$ //$NON-NLS-2$
			sb.append("Total memory: " + Runtime.getRuntime().totalMemory() + " bytes\n"); //$NON-NLS-1$ //$NON-NLS-2$

			for (final Map.Entry<?, ?> prop : System.getProperties().entrySet()) {
				sb.append(prop.getKey() + " = " + prop.getValue() + UI.NEW_LINE);//$NON-NLS-1$
			}
		}

		@Override
		protected IDialogSettings getDialogBoundsSettings() {
			// keep window size and position
			return _state;
		}

		@Override
		protected Point getInitialSize() {

			final Point calculatedSize = super.getInitialSize();

			if (calculatedSize.x < 600) {
				calculatedSize.x = 600;
			}
			if (calculatedSize.y < 600) {
				calculatedSize.y = 600;
			}

			return calculatedSize;
		}

		/**
		 * This code is found in jogl.jar: jogamp.opengl.awt.VersionApplet <code>
		 * 
		 * 
			<object classid="clsid:8AD9C840-044E-11D1-B3E9-00805F499D93"
			      width="800" height="600">
			   <param name="code" value="org.jdesktop.applet.util.JNLPAppletLauncher">
			   <param name="archive" value="jar/applet-launcher.jar,
			                                jar/gluegen-rt.jar,
			                                jar/jogl-all.jar">
			   <param name="codebase_lookup" value="false">
			   <param name="subapplet.classname" value="jogamp.opengl.awt.VersionApplet">
			   <param name="subapplet.displayname" value="JOGL Applet Version">
			   <param name="noddraw.check" value="true">
			   <param name="progressbar" value="true">
			   <param name="jnlpNumExtensions" value="1">
			   <param name="jnlpExtension1" value="jogl-all-awt.jnlp">
			   <param name="java_arguments" value="-Dsun.java2d.noddraw=true">
			   <param name="jnlp_href" value="jogl-applet-version.jnlp">
			   <comment>
			     <embed code="org.jdesktop.applet.util.JNLPAppletLauncher"
			          width="800" height="600"
			          type="application/x-java-applet;version=1.6"
			          pluginspage="http://java.sun.com/javase/downloads/ea.jsp"
			          archive="jar/applet-launcher.jar,
			                   jar/gluegen-rt.jar,
			                   jar/jogl-all.jar"
			          codebase_lookup" value="false"
			          subapplet.classname="jogamp.opengl.awt.VersionApplet"
			          subapplet.displayname="JOGL Applet Version"
			          noddraw.check="true"
			          progressbar="true"
			          jnlpNumExtensions="1"
			          jnlpExtension1="jogl-all-awt.jnlp"
			          java_arguments="-Dsun.java2d.noddraw=true"
			          jnlp_href="jogl-applet-version.jnlp">
			        <noembed>Sorry, no Java support detected.</noembed>
			     </embed>
			   </comment>
			</object>
		 * </code>
		 */
		private void GL2_Info() {

//		    final GLProfile glp = GLProfile.getDefault();
//		    final GLCapabilities glcaps = new GLCapabilities(glp);
//
//		    s = VersionUtil.getPlatformInfo().toString();
//		    System.err.println(s);
//
//		    s = GlueGenVersion.getInstance().toString();
//		    System.err.println(s);
//
//		    /*
//		    s = NativeWindowVersion.getInstance().toString();
//		    System.err.println(s);
//		    tareaVersion.append(NativeWindowVersion.getInstance().toString());
//		    */
//
//		    s = JoglVersion.getInstance().toString();
//		    System.err.println(s);
//
//		    final GLDrawableFactory factory = GLDrawableFactory.getFactory(glp);
//		    final List<GLCapabilitiesImmutable> availCaps = factory.getAvailableCapabilities(null);
//		    for(int i=0; i<availCaps.size(); i++) {
//		        s = ((GLCapabilitiesImmutable) availCaps.get(i)).toString();
//		        System.err.println(s);
//		        tareaCaps.append(s);
//		        tareaCaps.append(Platform.getNewline());
//		    }
//
//		    final Container grid = new Container();
//		    grid.setLayout(new GridLayout(2, 1));
//		    grid.add(tareaVersion);
//		    grid.add(tareaCaps);
//		    add(grid, BorderLayout.CENTER);
//
//		    canvas = new GLCanvas(glcaps);
//		    canvas.addGLEventListener(new GLInfo());
//		    canvas.setSize(10, 10);
//		    add(canvas, BorderLayout.SOUTH);
//		    validate();

		}

	}

	public Object execute(final ExecutionEvent event) throws ExecutionException {

		new DialogInfo(Display.getCurrent().getActiveShell()).open();

		return null;
	}

}
