package gsmith.eclipse.ui.commands;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
    private static final String BUNDLE_NAME = "gsmith.eclipse.ui.commands.messages"; //$NON-NLS-1$
    public static String RefreshWorkspaceCommandHandler_refreshJobName;
    public static String SetWindowSizeCommandHandler_currentSizeLabel;
    public static String SetWindowSizeCommandHandler_deleteButtonTip;
    public static String SetWindowSizeCommandHandler_dialogTitle;
    public static String SetWindowSizeCommandHandler_heightLabel;
    public static String SetWindowSizeCommandHandler_rememberSizeLabel;
    public static String SetWindowSizeCommandHandler_resetDefaultSizesLink;
    public static String SetWindowSizeCommandHandler_sizesLabel;
    public static String SetWindowSizeCommandHandler_topLeftCheckLabel;
    public static String SetWindowSizeCommandHandler_widthLabel;
    public static String ToggleReadOnlyFileCommandHandler_updateDecorationsJobName;
    public static String TouchFileCommandHandler_touchJobName;
    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages() {
    }
}
