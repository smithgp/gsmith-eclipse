package gsmith.eclipse.ui.actions;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
    private static final String BUNDLE_NAME = "gsmith.eclipse.ui.actions.messages"; //$NON-NLS-1$
    public static String WindowSizesCompoundContributionItem_customSizeActionLabel;
    public static String SetWindowSizeAction_labelFormat;
    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages() {
    }
}
