package gsmith.eclipse.ui;

import gsmith.eclipse.ui.commands.SetWindowSizeCommandHandler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

/**
 * Data structure representing a "width,height" pair. This also maintains the
 * user's default list of available window sizes.
 */
public final class WindowSize implements Comparable<WindowSize> {
    /**
     * Preference key in UIActivator's plugin scope for the if we should default
     * to putting the window in the top-left corner.
     */
    static final String TOP_LEFT_PREF_KEY = SetWindowSizeCommandHandler.class.getName() + ".topLeft"; //$NON-NLS-1$

    /**
     * Preference path in UIActivator's plugin scope for storing the default
     * available sizes.
     */
    private static final String SIZE_PREFS_PATH = "windowSizes"; //$NON-NLS-1$

    /**
     * The default default sizes.
     */
    private static final WindowSize[] DEFAULT_SIZES = {
            // new WindowSize(800, 600), // no one uses this
            new WindowSize(1024, 768), new WindowSize(1280, 1024),
            new WindowSize(1440, 900), new WindowSize(1600, 1050),
            new WindowSize(1600, 1200), new WindowSize(1920, 1200)
    };

    /**
     * Get the preferences node that stores the configured sizes.
     * 
     * @param create
     *            true to always create, false to not create if doesn't exist.
     * @return the preferences, or null if create is false and doesn't exist.
     */
    private static Preferences getPreferencesNode(boolean create) {
        IEclipsePreferences pluginPrefs = new InstanceScope().getNode(UIActivator.PLUGIN_ID);
        try {
            if (create || pluginPrefs.nodeExists(SIZE_PREFS_PATH)) {
                return pluginPrefs.node(SIZE_PREFS_PATH);
            }
        }
        catch (BackingStoreException ignore) {
        }
        return null;
    }

    /**
     * Load the default sizes from the preferences, if available.
     * 
     * @return the sizes, or null if not available in the preferences (i.e. use
     *         defaults).
     */
    private static Collection<WindowSize> getSizesFromPreferences() {
        Preferences prefs = getPreferencesNode(false);
        if (prefs != null) {
            try {
                Collection<WindowSize> l = new HashSet<WindowSize>();
                for (String key : prefs.keys()) {
                    if (key.startsWith("size")) //$NON-NLS-1$
                    {
                        String val = prefs.get(key, null);
                        WindowSize s = WindowSize.valueOf(val);
                        if (s != null) {
                            l.add(s);
                        }
                    }
                }
                return l;
            }
            catch (BackingStoreException ignore) {
            }
        }
        // no node, so use defaults
        return null;
    }

    /**
     * Get the default window sizes to offer.
     * 
     * @return array of { width, height }.
     */
    // REVIEWME: cache the windowsizes? Seems pretty snappy right now, so let's
    // not worry about it yet
    public static WindowSize[] getDefaultSizes() {
        Collection<WindowSize> sizes = getSizesFromPreferences();
        if (sizes != null) {
            WindowSize[] l = sizes.toArray(new WindowSize[sizes.size()]);
            Arrays.sort(l);
            return l;
        }
        return DEFAULT_SIZES;
    }

    /**
     * Set the default window sizes to offer.
     * 
     * @param sizes
     *            the list, or null to reset to the defaults.
     */
    public static void setDefaultSizes(Collection<WindowSize> sizes) {
        Preferences prefs = getPreferencesNode(true);
        try {
            // this resets to defaults
            if (sizes == null) {
                prefs.removeNode();
            }
            // otherwise, clear out and set them
            else {
                prefs.clear();
                int i = 0;
                for (WindowSize size : sizes) {
                    prefs.put("size" + (i++), size.toString()); //$NON-NLS-1$
                }
            }
        }
        catch (BackingStoreException ex) {
            // intentionally blank
        }
    }

    /**
     * Set the default window sizes to offer.
     * 
     * @param sizes
     *            the list, or null to reset to the defaults.
     */
    public static void setDefaultSizes(WindowSize... sizes) {
        setDefaultSizes(sizes != null ? Arrays.asList(sizes) : null);
    }

    /**
     * Add a new default window size.
     */
    public static void addDefaultSize(WindowSize size) {
        Collection<WindowSize> sizes = new ArrayList<WindowSize>(
                Arrays.asList(getDefaultSizes()));
        if (!sizes.contains(size)) {
            sizes.add(size);
            setDefaultSizes(sizes);
        }
    }

    /**
     * Get the state for putting the window in the top-left corner.
     */
    public static boolean getTopLeftCorner() {
        return UIActivator.getDefault().getPreferenceStore().getBoolean(
                TOP_LEFT_PREF_KEY);
    }

    /**
     * Get the state for putting the window in the top-left corner.
     */
    public static void setTopLeftCorner(boolean b) {
        UIActivator.getDefault().getPreferenceStore().setValue(
                TOP_LEFT_PREF_KEY, b);
    }

    /**
     * The width.
     */
    public final int width;

    /**
     * The height.
     */
    public final int height;

    public WindowSize(int width, int height) {
        this.width = width;
        this.height = height;
    }

    /**
     * Return a string format, valid can be parsed with {@link #valueOf}.
     */
    @Override
    public final String toString() {
        return toString(width, height);
    }

    /**
     * Return a string format, valid can be parsed with {@link #valueOf}.
     */
    public static String toString(int width, int height) {
        return new StringBuilder().append(width).append(',').append(height).toString();
    }

    /**
     * Parse a string, as from {@link #toString()}.
     * 
     * @return the WindowSize object, or null if the string isn't valid.
     */
    public static WindowSize valueOf(String s) {
        if (s != null) {
            int index = s.indexOf(',');
            if (index > 0) {
                try {
                    int w = Integer.parseInt(s.substring(0, index));
                    int h = Integer.parseInt(s.substring(index + 1));
                    return new WindowSize(w, h);
                }
                catch (NumberFormatException ignore) {
                }
            }
        }
        return null;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + height;
        result = prime * result + width;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        WindowSize other = (WindowSize)obj;
        if (height != other.height)
            return false;
        if (width != other.width)
            return false;
        return true;
    }

    @Override
    public int compareTo(WindowSize o) {
        // sort on width first, then height
        int i = width - o.width;
        if (i == 0) {
            i = height - o.height;
        }
        return i;
    }
}