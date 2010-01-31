/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/

package net.tourbook.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.statushandlers.StatusManager;


/*
 * this class is copied from org.eclipse.ui.internal.misc.StatusUtil and modified
 */

/**
 * Utility class to create status objects.
 * 
 * @private - This class is an internal implementation class and should
 *          not be referenced or subclassed outside of the workbench
 */
public class StatusUtil {

	public static Throwable getCause(final Throwable exception) {

		// Figure out which exception should actually be logged -- if the given exception is
		// a wrapper, unwrap it
		Throwable cause = null;
		if (exception != null) {
			if (exception instanceof CoreException) {
				// Workaround: CoreException contains a cause, but does not actually implement getCause(). 
				// If we get a CoreException, we need to manually unpack the cause. Otherwise, use
				// the general-purpose mechanism. Remove this branch if CoreException ever implements
				// a correct getCause() method.
				final CoreException ce = (CoreException) exception;
				cause = ce.getStatus().getException();

			} else {

				// use reflect instead of a direct call to getCause(), to allow compilation against JCL Foundation (bug 80053)
				try {
					final Method causeMethod = exception.getClass().getMethod("getCause", new Class[0]); //$NON-NLS-1$
					final Object o = causeMethod.invoke(exception, new Object[0]);
					if (o instanceof Throwable) {
						cause = (Throwable) o;
					}
				} catch (final NoSuchMethodException e) {
					// ignore
				} catch (final IllegalArgumentException e) {
					// ignore
				} catch (final IllegalAccessException e) {
					// ignore
				} catch (final InvocationTargetException e) {
					// ignore
				}
			}

			if (cause == null) {
				cause = exception;
			}
		}

		return cause;
	}

	/**
	 * Utility method for handling status.
	 */
	public static void handleStatus(final String message, final int style) {
		handleStatus(message, null, style);
	}

	/**
	 * Utility method for handling status.
	 */
	public static void handleStatus(final String message, final Throwable e, final int style) {
		StatusManager.getManager().handle(newStatus(Activator.PLUGIN_ID, message, e), style | StatusManager.LOG);
	}

	/**
	 * Log exception into the status log
	 * 
	 * @param message
	 * @param exception
	 */
	public static void log(final String message, Throwable exception) {

		/*
		 * create an exception to see in the log the location where the logging occured
		 */
		if (exception == null) {
			exception = new Exception();
		}

		handleStatus(message, exception, StatusManager.LOG);
	}

	/**
	 * Log into log
	 * 
	 * @param exception
	 */
	public static void log(final Throwable exception) {
		handleStatus(exception.getMessage(), exception, StatusManager.LOG);
	}

	/**
	 * Utility method for creating status.
	 */
	public static IStatus newStatus(final int severity, final String message, final Throwable exception) {

		String statusMessage = message;
		if (message == null || message.trim().length() == 0) {
			if (exception.getMessage() == null) {
				statusMessage = exception.toString();
			} else {
				statusMessage = exception.getMessage();
			}
		}

		return new Status(severity, Activator.PLUGIN_ID, severity, statusMessage, getCause(exception));
	}

	public static IStatus newStatus(final String pluginId, final String message, final Throwable exception) {
		return new Status(IStatus.ERROR, pluginId, IStatus.OK, message, getCause(exception));
	}

	public static void showStatus(final String message, final Throwable exception) {
		handleStatus(message, exception, StatusManager.SHOW);
	}

}
