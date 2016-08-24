package gsmith.eclipse.ui.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import gsmith.eclipse.ui.UIActivator;
import gsmith.eclipse.ui.WindowSize;
import gsmith.eclipse.ui.actions.SetWindowSizeAction;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * Command handler to resize the workbench window.
 */
public class SetWindowSizeCommandHandler extends AbstractHandler {
    private static final String REMEMBER_SIZE_PREF_KEY =
        SetWindowSizeCommandHandler.class.getName() + ".rememberSize"; //$NON-NLS-1$

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        Shell shell = HandlerUtil.getActiveShellChecked(event);
        promptResize(shell);
        return null;
    }

    /**
     * Prompt the user to resize the specified window.
     */
    public static void promptResize(Shell shell) {
        WindowSizeDialog d = new WindowSizeDialog(shell);
        if (d.open() == WindowSizeDialog.OK) {
            // save some stuff off
            WindowSize size = new WindowSize(d.getSelectedWidth(), d.getSelectedHeight());
            if (d.getAddSize()) {
                WindowSize.addDefaultSize(size);
            }
            WindowSize.setTopLeftCorner(d.getSelectedTopLeftCorner());
            UIActivator.getDefault().getPreferenceStore().setValue(REMEMBER_SIZE_PREF_KEY, d.getAddSize());
            // resize the window
            SetWindowSizeAction.resize(shell, size.width, size.height, d.getSelectedTopLeftCorner());
        }
    }

    private static class WindowSizeDialog extends Dialog {
        private Shell shell;
        private Spinner widthSpinner;
        private Spinner heightSpinner;
        private Button topLeftCheck;
        private Button addSizeCheck;

        private Integer selectedWidth = null;
        private Integer selectedHeight = null;
        private Boolean selectedTopLeftCorner = null;
        private Boolean addSize = null;

        public WindowSizeDialog(Shell parentShell, Shell shell) {
            super(parentShell);
            this.shell = parentShell;
        }

        public WindowSizeDialog(Shell parentShell) {
            this(parentShell, parentShell);
        }

        @Override
        protected void configureShell(Shell newShell) {
            super.configureShell(newShell);
            newShell.setText(Messages.SetWindowSizeCommandHandler_dialogTitle);
        }

        @Override
        protected Control createDialogArea(Composite parent) {
            Composite main = (Composite)super.createDialogArea(parent);
            ((GridLayout)main.getLayout()).numColumns = 3;

            Label l = new Label(main, SWT.RIGHT);
            l.setText(Messages.SetWindowSizeCommandHandler_sizesLabel);
            GridData gd = new GridData();
            l.setLayoutData(gd);
            final Combo sizes = new Combo(main, SWT.BORDER | SWT.READ_ONLY | SWT.DROP_DOWN);
            gd = new GridData();
            gd.grabExcessHorizontalSpace = true;
            gd.horizontalAlignment = SWT.FILL;
            sizes.setLayoutData(gd);
            final Button deleteButton = new Button(main, SWT.PUSH);
            deleteButton.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_TOOL_DELETE));
            deleteButton.setToolTipText(Messages.SetWindowSizeCommandHandler_deleteButtonTip);
            deleteButton.setLayoutData(new GridData());

            Link resetLink = new Link(main, SWT.RIGHT);
            resetLink.setText(Messages.SetWindowSizeCommandHandler_resetDefaultSizesLink);
            // make it a little smaller font
            Font font = resetLink.getFont();
            FontData[] fontData = font.getFontData();
            for (int i = 0; i < fontData.length; i++) {
                if (fontData[i].getHeight() > 4) {
                    fontData[i].setHeight(fontData[i].getHeight() - 1);
                }
            }
            resetLink.setFont(new Font(font.getDevice(), fontData));
            gd = new GridData();
            gd.grabExcessHorizontalSpace = true;
            gd.horizontalAlignment = SWT.END;
            gd.horizontalSpan = 3;
            resetLink.setLayoutData(gd);

            l = new Label(main, SWT.SEPARATOR | SWT.HORIZONTAL);
            gd = new GridData();
            gd.grabExcessHorizontalSpace = true;
            gd.horizontalAlignment = SWT.FILL;
            gd.horizontalSpan = 3;
            l.setLayoutData(gd);

            l = new Label(main, SWT.RIGHT);
            l.setText(Messages.SetWindowSizeCommandHandler_widthLabel);
            gd = new GridData();
            l.setLayoutData(gd);
            widthSpinner = new Spinner(main, SWT.WRAP | SWT.BORDER);
            widthSpinner.setDigits(0);
            widthSpinner.setMinimum(100);
            widthSpinner.setMaximum(Integer.MAX_VALUE);
            gd = new GridData();
            gd.grabExcessHorizontalSpace = true;
            gd.horizontalAlignment = SWT.FILL;
            gd.horizontalSpan = 2;
            widthSpinner.setLayoutData(gd);

            l = new Label(main, SWT.RIGHT);
            l.setText(Messages.SetWindowSizeCommandHandler_heightLabel);
            gd = new GridData();
            l.setLayoutData(gd);
            heightSpinner = new Spinner(main, SWT.WRAP | SWT.BORDER);
            heightSpinner.setDigits(0);
            heightSpinner.setMinimum(100);
            heightSpinner.setMaximum(Integer.MAX_VALUE);
            gd = new GridData();
            gd.grabExcessHorizontalSpace = true;
            gd.horizontalAlignment = SWT.FILL;
            gd.horizontalSpan = 2;
            heightSpinner.setLayoutData(gd);

            topLeftCheck = new Button(main, SWT.CHECK);
            topLeftCheck.setText(Messages.SetWindowSizeCommandHandler_topLeftCheckLabel);
            gd = new GridData();
            gd.grabExcessHorizontalSpace = true;
            gd.horizontalAlignment = SWT.FILL;
            gd.horizontalSpan = 3;
            topLeftCheck.setLayoutData(gd);

            addSizeCheck = new Button(main, SWT.CHECK);
            addSizeCheck.setText(Messages.SetWindowSizeCommandHandler_rememberSizeLabel);
            gd = new GridData();
            gd.grabExcessHorizontalSpace = true;
            gd.horizontalAlignment = SWT.FILL;
            gd.horizontalSpan = 3;
            addSizeCheck.setLayoutData(gd);

            // fill up the dropdown
            final Rectangle currentBounds = shell.getBounds();
            final List<WindowSize> defaultSizes = new ArrayList<>(Arrays.asList(WindowSize.getDefaultSizes()));
            fillSizesCombo(sizes, defaultSizes, currentBounds);

            // update ui as the dropdown selection changes
            sizes.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    int index = sizes.getSelectionIndex();
                    // current
                    if (index == 0) {
                        widthSpinner.setSelection(currentBounds.width);
                        heightSpinner.setSelection(currentBounds.height);
                    }
                    // one of the default sizes
                    else if (index >= 1 && index < defaultSizes.size() + 1) {
                        WindowSize size = defaultSizes.get(index - 1);
                        widthSpinner.setSelection(size.width);
                        heightSpinner.setSelection(size.height);
                    }

                    deleteButton.setEnabled(index > 0);
                }
            });
            deleteButton.setEnabled(sizes.getSelectionIndex() > 0);

            // hook up delete
            final SelectionListener deleteListener = new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    // don't use the event, since we're going to call this
                    // directly
                    int index = sizes.getSelectionIndex();
                    if (index > 0) {
                        defaultSizes.remove(index - 1);
                        WindowSize.setDefaultSizes(defaultSizes);
                        sizes.remove(index);
                        // select the one after, if there is one
                        if (index < sizes.getItemCount()) {
                            sizes.select(index);
                        }
                        // otherwise, the one above
                        else {
                            sizes.select(index - 1);
                        }
                    }
                }
            };
            deleteButton.addSelectionListener(deleteListener);
            sizes.addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    if (e.character == SWT.DEL) {
                        if (deleteButton.isEnabled()) {
                            deleteListener.widgetSelected(null);
                        }
                    }
                }
            });

            resetLink.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    // save off the current selection
                    int curHeight = heightSpinner.getSelection();
                    int curWidth = widthSpinner.getSelection();

                    // update the preferences
                    WindowSize.setDefaultSizes((WindowSize[])null);

                    // redo the drop-down
                    defaultSizes.clear();
                    defaultSizes.addAll(Arrays.asList(WindowSize.getDefaultSizes()));
                    fillSizesCombo(sizes, defaultSizes, currentBounds);

                    // reset the current selection
                    heightSpinner.setSelection(curHeight);
                    widthSpinner.setSelection(curWidth);
                }
            });

            widthSpinner.setSelection(currentBounds.width);
            heightSpinner.setSelection(currentBounds.height);

            // initialize from preferences
            topLeftCheck.setSelection(WindowSize.getTopLeftCorner());
            addSizeCheck.setSelection(UIActivator.getDefault().getPreferenceStore().getBoolean(REMEMBER_SIZE_PREF_KEY));

            return main;
        }

        private void fillSizesCombo(Combo sizes, List<WindowSize> defaultSizes, Rectangle currentBounds) {
            try {
                sizes.setRedraw(false);
                sizes.removeAll();
                sizes.add(Messages.SetWindowSizeCommandHandler_currentSizeLabel);
                sizes.select(0);
                for (int i = 0; i < defaultSizes.size(); i++) {
                    WindowSize size = defaultSizes.get(i);
                    sizes.add(SetWindowSizeAction.getSizeDisplayLabel(size.width, size.height));
                    if (size.width == currentBounds.width && size.height == currentBounds.height) {
                        sizes.select(i + 1);
                    }
                }
            }
            finally {
                sizes.setRedraw(true);
            }
        }

        @Override
        protected void cancelPressed() {
            selectedWidth = null;
            selectedHeight = null;
            selectedTopLeftCorner = null;
            addSize = null;
            super.cancelPressed();
        }

        @Override
        protected void okPressed() {
            selectedWidth = widthSpinner.getSelection();
            selectedHeight = heightSpinner.getSelection();
            selectedTopLeftCorner = topLeftCheck.getSelection();
            addSize = addSizeCheck.getSelection();
            super.okPressed();
        }

        public int getSelectedWidth() {
            return selectedWidth != null ? selectedWidth.intValue() : -1;
        }

        public int getSelectedHeight() {
            return selectedHeight != null ? selectedHeight.intValue() : -1;
        }

        public boolean getSelectedTopLeftCorner() {
            return selectedTopLeftCorner != null && selectedTopLeftCorner.booleanValue();
        }

        public boolean getAddSize() {
            return addSize != null && addSize.booleanValue();
        }
    }
}