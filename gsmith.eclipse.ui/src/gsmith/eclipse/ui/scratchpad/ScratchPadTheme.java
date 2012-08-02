package gsmith.eclipse.ui.scratchpad;

import java.text.MessageFormat;

import org.eclipse.jface.resource.FontRegistry;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.themes.ITheme;
import org.eclipse.ui.themes.IThemePreview;

public class ScratchPadTheme implements IThemePreview {
    /**
     * FontRegistry id for the text font.
     */
    public static final String FONT_KEY = "gsmith.eclipse.ui.scratchpad.textfont"; //$NON-NLS-1$

    // TODO: figure out how to get the default TextEditor colors
    // The Eclipse TextEditor doesn't use the theme stuff for fore/background
    // color
    /**
     * ColorRegistry id for the text color.
     */
    //public static final String FOREGROUND_KEY = "gsmith.eclipse.ui.scratchpad.foreground"; //$NON-NLS-1$

    /**
     * ColorRegistry id for the background color.
     */
    //public static final String BACKGROUND_KEY = "gsmith.eclipse.ui.scratchpad.background"; //$NON-NLS-1$

    /**
     * Apply the scratchpad colors and fonts from the theme to the widget.
     */
    public static void applyColorsAndFonts(Control textArea, ITheme theme) {
        textArea.setRedraw(false);
        try {
            //Color c = theme.getColorRegistry().get(FOREGROUND_KEY);
            //textArea.setForeground(c);
            //c = theme.getColorRegistry().get(BACKGROUND_KEY);
            //textArea.setBackground(c);
            Font f = theme.getFontRegistry().get(FONT_KEY);
            textArea.setFont(f);
        }
        finally {
            textArea.setRedraw(true);
        }
    }

    /**
     * A listener for IThemeManager, ITheme, FontRegistroy, or ColorRegistry.
     */
    public abstract static class Listener implements IPropertyChangeListener {
        @Override
        public void propertyChange(PropertyChangeEvent event) {
            if (isApplicable(event)) {
                applyColorsAndFonts();
            }
        }

        protected abstract void applyColorsAndFonts();

        protected boolean isApplicable(PropertyChangeEvent event) {
            // only listen to our color and font changes
            return /*(event.getSource() instanceof ColorRegistry &&
                   (FOREGROUND_KEY.equals(event.getProperty()) ||
                    BACKGROUND_KEY.equals(event.getProperty()))) ||*/
                   (event.getSource() instanceof FontRegistry && FONT_KEY.equals(event.getProperty()));
        }
    };

    private IPropertyChangeListener themeListener = null;
    private ITheme currentTheme = null;

    @Override
    public void createControl(Composite parent, final ITheme theme) {
        this.currentTheme = theme;
        final StyledText text = new StyledText(parent, SWT.READ_ONLY |
                SWT.FULL_SELECTION | SWT.MULTI | SWT.WRAP | SWT.BORDER |
                SWT.H_SCROLL | SWT.V_SCROLL);
        applyColorsAndFonts(text, theme);
        text.setText(MessageFormat.format(Messages.ScratchPadTheme_previewText,
                getFontDescription(text.getFont())));

        themeListener = new Listener() {
            @Override
            protected void applyColorsAndFonts() {
                ScratchPadTheme.applyColorsAndFonts(text, theme);
                text.setText(MessageFormat.format(
                        Messages.ScratchPadTheme_previewText,
                        getFontDescription(text.getFont())));
            }
        };
        currentTheme.addPropertyChangeListener(themeListener);
    }

    // copied from
    // org.eclipse.ui.internal.themes.ColorsAndFontsPreferencePage#setCurrentFont(),
    // which used with the default font theme preview widget.
    private static final String getFontDescription(Font f) {
        FontData[] fontData = f.getFontData();
        // recalculate sample text
        StringBuilder tmp = new StringBuilder();
        for (int i = 0; i < fontData.length; i++) {
            tmp.append(fontData[i].getName());
            tmp.append(' ');
            tmp.append(fontData[i].getHeight());

            int style = fontData[i].getStyle();
            if ((style & SWT.BOLD) != 0) {
                tmp.append(' ');
                tmp.append(Messages.ScratchPadTheme_bold);
            }
            if ((style & SWT.ITALIC) != 0) {
                tmp.append(' ');
                tmp.append(Messages.ScratchPadTheme_italic);
            }
            // this only happens on XWindows
            if (i != 0) {
                tmp.append('\n');
            }
        }
        return tmp.toString();
    }

    @Override
    public void dispose() {
        if (currentTheme != null && themeListener != null) {
            currentTheme.removePropertyChangeListener(themeListener);
        }
    }
}