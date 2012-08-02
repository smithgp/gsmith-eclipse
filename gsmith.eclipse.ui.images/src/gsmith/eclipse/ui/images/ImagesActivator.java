package gsmith.eclipse.ui.images;

import gsmith.eclipse.ui.UIActivator;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class ImagesActivator extends AbstractUIPlugin {
    /**
     * The plug-in ID.
     */
    public static final String PLUGIN_ID = "gsmith.eclipse.ui.images"; //$NON-NLS-1$

    // The shared instance
    private static ImagesActivator plugin;

    @Override
    public void start(BundleContext context) throws Exception {
        super.start(context);
        plugin = this;
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        plugin = null;
        super.stop(context);
    }

    /**
     * Log a warning message.
     */
    public void logWarning(String mesg, Throwable ex) {
        UIActivator.log(this, IStatus.WARNING, mesg, ex);
    }

    /**
     * Log an error message.
     */
    public void logError(String mesg, Throwable ex) {
        UIActivator.log(this, IStatus.ERROR, mesg, ex);
    }

    /**
     * Returns the shared instance
     */
    public static ImagesActivator getDefault() {
        return plugin;
    }
}
