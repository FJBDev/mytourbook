package net.tourbook.printing;

import org.eclipse.osgi.internal.framework.EquinoxBundle;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Version;

/**
 * The activator class controls the plug-in life cycle
 */
@SuppressWarnings("restriction")
public class Activator extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "net.tourbook.printing"; //$NON-NLS-1$

	// The shared instance
	private static Activator plugin;
	
	private Version				version;

	/**
	 * The constructor
	 */
	public Activator() {
		plugin = this;
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static Activator getDefault() {
		return plugin;
	}

	public Version getVersion() {
		return version;
	}

	@Override
	public void start(final BundleContext context) throws Exception {
		final Bundle bundle = context.getBundle();
		if (bundle instanceof EquinoxBundle) {
			final EquinoxBundle abstractBundle = (EquinoxBundle) bundle;
			version = abstractBundle.getVersion();
		}
		super.start(context);
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	@Override
	public void stop(final BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

}
