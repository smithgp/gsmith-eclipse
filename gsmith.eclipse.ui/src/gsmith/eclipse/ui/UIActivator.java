package gsmith.eclipse.ui;

import java.io.Closeable;
import java.io.IOException;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.graphics.Resource;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class UIActivator extends AbstractUIPlugin {
    /**
     * The plug-in ID
     */
    public static final String PLUGIN_ID = "gsmith.eclipse.ui"; //$NON-NLS-1$

    // The shared instance
    private static UIActivator plugin;

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
     * Returns the shared instance
     */
    public static UIActivator getDefault() {
        return plugin;
    }

    /**
     * Get the closest swt display.
     */
    public static Display getDisplay() {
        Display d = Display.getCurrent();
        if (d == null) {
            if (PlatformUI.isWorkbenchRunning()) {
                d = PlatformUI.getWorkbench().getDisplay();
            }
            if (d == null) {
                d = Display.getDefault();
            }
        }
        return d;
    }

    /**
     * Run the code in the display thread.
     */
    public static void runInDisplayThread(Runnable r, Display d) {
        if (Display.getCurrent() == d) {
            r.run();
        }
        else {
            d.asyncExec(r);
        }
    }

    /**
     * Safely dispose an SWT resource.
     */
    public static void dispose(Resource r) {
        try {
            if (r != null) {
                r.dispose();
            }
        }
        catch (SWTException ignore) {
        }
    }

    /**
     * Safely close any closeable.
     */
    public static void close(Closeable c) {
        try {
            if (c != null) {
                c.close();
            }
        }
        catch (IOException ignore) {
        }
    }

    /**
     * Log a message with the given plugin.
     */
    public static void log(Plugin plugin, int severity, String mesg, Throwable ex) {
        IStatus st = new Status(severity, plugin.getBundle().getSymbolicName(), mesg, ex);
        plugin.getLog().log(st);
    }

    /**
     * Adapt the specific object to the specified class, supporting the
     * IAdaptable interface as well.
     */
    public static <T> T adaptTo(Object o, Class<T> cl) {
        return adaptTo(o, cl, cl);
    }

    /**
     * Adapt the specific object to the specified classes, supporting the
     * IAdaptable interface as well.
     *
     * @param o
     *            the object.
     * @param actualType
     *            the actual type that must be returned.
     * @param adapterType
     *            the adapter type to check for.
     */
    public static <T> T adaptTo(Object o, Class<T> actualType, Class<?> adapterType) {
        if (actualType.isInstance(o)) {
            return actualType.cast(o);
        }
        else if (o instanceof IAdaptable) {
            o = ((IAdaptable)o).getAdapter(adapterType);
            if (actualType.isInstance(o)) {
                return actualType.cast(o);
            }
        }
        return null;
    }
}
