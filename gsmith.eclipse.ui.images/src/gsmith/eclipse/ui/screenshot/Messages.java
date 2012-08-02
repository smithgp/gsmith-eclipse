package gsmith.eclipse.ui.screenshot;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
    private static final String BUNDLE_NAME = "gsmith.eclipse.ui.screenshot.messages"; //$NON-NLS-1$
    public static String ScreenshotCommandHandler_editorDisplayName;
    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages() {
    }
}
