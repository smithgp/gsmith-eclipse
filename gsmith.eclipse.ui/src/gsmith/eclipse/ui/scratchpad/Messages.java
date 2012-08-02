package gsmith.eclipse.ui.scratchpad;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
    private static final String BUNDLE_NAME = "gsmith.eclipse.ui.scratchpad.messages"; //$NON-NLS-1$
    public static String CopySelectionCommandHandler_evalEnablementFailed;
    public static String CopySelectionCommandHandler_missingClassAttribute;
    public static String CopySelectionCommandHandler_textConvertFailed;
    public static String CopySelectionCommandHandler_uanbleToLoadEnablement;
    public static String CopySelectionCommandHandler_unableToLoadConverter;
    public static String ScratchPadTheme_bold;
    public static String ScratchPadTheme_italic;
    public static String ScratchPadTheme_previewText;
    public static String ScratchPadView_clearActionLabel;
    public static String ScratchPadView_clearActionTip;
    public static String ScratchPadView_readError;
    public static String ScratchPadView_saveJobName;
    public static String ScratchPadView_writeError;
    public static String StyledTextUndoManager_opLabel;
    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages() {
    }
}
