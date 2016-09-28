package gsmith.eclipse.ui.scratchpad;

import gsmith.eclipse.ui.UIActivator;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.MultiRule;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.util.SafeRunnable;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.actions.ActionFactory.IWorkbenchAction;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.progress.IProgressService;
import org.eclipse.ui.themes.IThemeManager;

/**
 * Scratch pad view for storing arbitrary text.
 */
public class ScratchPadView extends ViewPart {
    /**
     * The view id.
     */
    public static final String ID = "gsmith.eclipse.ui.scratchpad"; //$NON-NLS-1$

    /**
     * Scheduling rule to prevent collisions while reading/writing the scratch
     * pad text.
     */
    private static class SaveRule implements ISchedulingRule {
        private final IPath path;

        private SaveRule(IPath path) {
            this.path = path;
        }

        private SaveRule(String filename) {
            this(getScratchTextFilePath(filename));
        }

        @Override
        public boolean isConflicting(ISchedulingRule rule) {
            return rule instanceof SaveRule &&
                    (this.path.isPrefixOf(((SaveRule)rule).path) || ((SaveRule)rule).path.isPrefixOf(this.path));
        }

        @Override
        public boolean contains(ISchedulingRule rule) {
            if (rule instanceof MultiRule) {
                MultiRule multi = (MultiRule)rule;
                ISchedulingRule[] children = multi.getChildren();
                for (int i = 0; i < children.length; i++) {
                    if (!contains(children[i])) {
                        return false;
                    }
                }
                return true;
            }

            return rule instanceof SaveRule &&
                    (this.path.equals(((SaveRule)rule).path) || this.path.isPrefixOf(((SaveRule)rule).path));
        }

        @Override
        public String toString() {
            StringBuilder buf = new StringBuilder(super.toString());
            buf.append('[').append(path.toString()).append(']');
            return buf.toString();
        }
    }

    private StyledText textArea;

    // TODO: get this from somewhere (Memento?)
    private String filename = "scratch.txt"; //$NON-NLS-1$

    private SaveJob saveJob = null;

    private IPropertyChangeListener themeListener = null;

    private SelectionProvider selectionProvider = null;

    private StyledTextUndoManager undoManager = null;

    private Map<String, IWorkbenchAction> workbenchActions;

    @Override
    public void init(IViewSite site) throws PartInitException {
        super.init(site);

        // support workbench-level selection management, tied to our text area
        selectionProvider = new SelectionProvider();
        site.setSelectionProvider(selectionProvider);
    }

    @Override
    public void createPartControl(Composite parent) {
        textArea = new StyledText(parent,
                SWT.FULL_SELECTION | SWT.MULTI | SWT.WRAP | SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
        textArea.setLayoutData(new GridData(GridData.FILL_BOTH));

        // hook up the theme fonts and colors
        final IThemeManager themes = getSite().getWorkbenchWindow().getWorkbench().getThemeManager();
        ScratchPadTheme.applyColorsAndFonts(textArea, themes.getCurrentTheme());
        themeListener = new ScratchPadTheme.Listener() {
            @Override
            protected void applyColorsAndFonts() {
                ScratchPadTheme.applyColorsAndFonts(textArea, themes.getCurrentTheme());
            }

            @Override
            protected boolean isApplicable(PropertyChangeEvent event) {
                // also listen to if the entire theme changes
                return IThemeManager.CHANGE_CURRENT_THEME.equals(event.getProperty()) || super.isApplicable(event);
            }
        };
        themes.addPropertyChangeListener(themeListener);

        // nicely load the initial text, with progress and stuff
        final boolean[] loaded = { false };
        try {
            IProgressService svc = getSite().getService(IProgressService.class);
            if (svc != null) {
                svc.run(true, true, monitor -> {
                    if (monitor.isCanceled()) {
                        throw new InterruptedException();
                    }
                    textArea.setText(loadScratchText(filename, monitor));
                    loaded[0] = true;
                });
            }
        }
        catch (InvocationTargetException ex) {
            // intentionally blank -- fall through
        }
        catch (InterruptedException ex) {
            // if interrupted/canceled, don't then synchronously load
            loaded[0] = true;
        }
        // something happened, so load is synchronously
        if (!loaded[0]) {
            textArea.setCursor(textArea.getDisplay().getSystemCursor(SWT.CURSOR_WAIT));
            try {
                textArea.setText(loadScratchText(filename, new NullProgressMonitor()));
            }
            finally {
                textArea.setCursor(null);
            }
        }

        // start listening to selections to bind to our selection provider
        textArea.addSelectionListener(selectionProvider);

        // an action to clear all the text
        final IAction clearAction = new Action(Messages.ScratchPadView_clearActionLabel) {
            @Override
            public void run() {
                textArea.setText(""); //$NON-NLS-1$
            }
        };
        clearAction.setToolTipText(Messages.ScratchPadView_clearActionTip);
        clearAction.setImageDescriptor(getSite().getWorkbenchWindow().getWorkbench().getSharedImages().getImageDescriptor(
                ISharedImages.IMG_ETOOL_CLEAR));
        clearAction.setDisabledImageDescriptor(getSite().getWorkbenchWindow().getWorkbench().getSharedImages().getImageDescriptor(
                ISharedImages.IMG_ETOOL_CLEAR_DISABLED));
        textArea.addModifyListener(e -> clearAction.setEnabled(textArea.getCharCount() > 0));
        clearAction.setEnabled(textArea.getCharCount() > 0);

        // hookup to the global cut, copy, paste, etc. actions
        IActionBars actionBars = getViewSite().getActionBars();
        setupGlobalActionHandlers(actionBars);

        // create a popup menu
        MenuManager popup = new MenuManager();
        popup.add(new Separator("scratchpad.undo")); //$NON-NLS-1$
        popup.add(getAction(ActionFactory.UNDO));
        popup.add(getAction(ActionFactory.REDO));
        popup.add(new Separator("scratchpad.edit")); //$NON-NLS-1$
        popup.add(getAction(ActionFactory.CUT));
        popup.add(getAction(ActionFactory.COPY));
        popup.add(getAction(ActionFactory.PASTE));
        popup.add(new Separator("scratchpad.delete")); //$NON-NLS-1$
        popup.add(getAction(ActionFactory.DELETE));
        popup.add(clearAction);
        popup.add(getAction(ActionFactory.SELECT_ALL));
        popup.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
        textArea.setMenu(popup.createContextMenu(textArea));
        // this makes it so objectContributions can get into the menu
        getViewSite().registerContextMenu(popup, selectionProvider);

        // setup our view's toolbar
        actionBars.getToolBarManager().add(new Separator("scratchpad.undo")); //$NON-NLS-1$
        actionBars.getToolBarManager().add(getAction(ActionFactory.UNDO));
        actionBars.getToolBarManager().add(getAction(ActionFactory.REDO));
        actionBars.getToolBarManager().add(new Separator("scratchpad.edit")); //$NON-NLS-1$
        actionBars.getToolBarManager().add(getAction(ActionFactory.CUT));
        actionBars.getToolBarManager().add(getAction(ActionFactory.COPY));
        actionBars.getToolBarManager().add(getAction(ActionFactory.PASTE));
        actionBars.getToolBarManager().add(new Separator("scratchpad.delete")); //$NON-NLS-1$
        actionBars.getToolBarManager().add(getAction(ActionFactory.DELETE));
        actionBars.getToolBarManager().add(clearAction);
        actionBars.getToolBarManager().add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));

        // setup our view's menu
        actionBars.getMenuManager().add(new Separator("scratchpad.undo")); //$NON-NLS-1$
        actionBars.getMenuManager().add(getAction(ActionFactory.UNDO));
        actionBars.getMenuManager().add(getAction(ActionFactory.REDO));
        actionBars.getMenuManager().add(new Separator("scratchpad.edit")); //$NON-NLS-1$
        actionBars.getMenuManager().add(getAction(ActionFactory.CUT));
        actionBars.getMenuManager().add(getAction(ActionFactory.COPY));
        actionBars.getMenuManager().add(getAction(ActionFactory.PASTE));
        actionBars.getMenuManager().add(new Separator("scratchpad.delete")); //$NON-NLS-1$
        actionBars.getMenuManager().add(getAction(ActionFactory.DELETE));
        actionBars.getMenuManager().add(clearAction);
        actionBars.getMenuManager().add(getAction(ActionFactory.SELECT_ALL));
        actionBars.getMenuManager().add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));

        // make sure everything gets pushed out to the UI
        actionBars.updateActionBars();

        // start listening to changes, to save the scratch text
        textArea.addModifyListener(e -> saveScratchText(textArea.getText()));
    }

    @Override
    public void setFocus() {
        this.textArea.setFocus();
    }

    @Override
    public void dispose() {
        if (undoManager != null) {
            undoManager.dispose();
        }
        super.dispose();
        if (themeListener != null) {
            getSite().getWorkbenchWindow().getWorkbench().getThemeManager().removePropertyChangeListener(themeListener);
        }
        if (workbenchActions != null) {
            for (IWorkbenchAction a : workbenchActions.values()) {
                a.dispose();
            }
            workbenchActions.clear();
            workbenchActions = null;
        }
    }

    /**
     * Insert the specified text.
     */
    public void insertText(String text) {
        if (textArea != null) {
            Point selection = textArea.getSelectionRange();
            // if there's selected text, replace it
            if (selection.y > 0) {
                textArea.replaceTextRange(selection.x, selection.y, text);
                // select the inserted text (which also assures it's visible)
                textArea.setSelection(selection.x, selection.x + text.length());
            }
            else {
                // otherwise, insert at the caret (or the start if empty)
                // TODO: insert newline at beginning or end?
                int caret = Math.max(textArea.getCaretOffset(), 0);
                textArea.replaceTextRange(caret, 0, text);
                // and move caret to the end of the new text
                textArea.setCaretOffset(caret + text.length());
                // and make sure it's visible
                textArea.showSelection();
            }
        }
    }

    /**
     * Get or create a workbench action for the specific action factory. This
     * will also maintain {@link #workbenchActions}.
     */
    private IWorkbenchAction getAction(ActionFactory f) {
        IWorkbenchAction a = workbenchActions != null ? workbenchActions.get(f.getId()) : null;
        if (a == null) {
            a = f.create(getViewSite().getWorkbenchWindow());
            if (workbenchActions == null) {
                workbenchActions = new HashMap<>();
            }
            workbenchActions.put(f.getId(), a);
        }
        return a;
    }

    /**
     * Setup up global action handlers tied to our text area.
     */
    private void setupGlobalActionHandlers(IActionBars actionBars) {
        // wireup cut, copy, paste, delete, select all, undo, redo actions.
        // it appears that Eclipse does something for the
        // cut, copy, paste, and select all actions, but they don't seem to
        // be actual IWorkbenchActions, and they don't work right with
        // getAction(ActionFactory).
        // So, we do need to make our own.
        final IAction cutAction = new Action() {
            @Override
            public void run() {
                textArea.cut();
            }
        };
        actionBars.setGlobalActionHandler(ActionFactory.CUT.getId(), cutAction);

        final IAction copyAction = new Action() {
            @Override
            public void run() {
                textArea.copy();
            }
        };
        actionBars.setGlobalActionHandler(ActionFactory.COPY.getId(), copyAction);

        final IAction deleteAction = new Action() {
            @Override
            public void run() {
                Point sel = textArea.getSelection();
                if (sel != null && (sel.y - sel.x) > 0) {
                    textArea.replaceTextRange(sel.x, sel.y - sel.x, ""); //$NON-NLS-1$
                }
            }
        };
        actionBars.setGlobalActionHandler(ActionFactory.DELETE.getId(), deleteAction);

        // actions whose enabled is based on the selection of the textArea
        final IAction[] selectionActions = {
                cutAction, copyAction, deleteAction
        };
        // initially no selection, so these are disabled
        setActionEnableds(false, selectionActions);

        // tie enabled to selection-ness
        textArea.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                boolean hasSelection = (e.y - e.x) > 0;
                setActionEnableds(hasSelection, selectionActions);
            }
        });

        final IAction pasteAction = new Action() {
            @Override
            public void run() {
                textArea.paste();
            }
        };
        actionBars.setGlobalActionHandler(ActionFactory.PASTE.getId(), pasteAction);

        final IAction selectAllAction = new Action() {
            @Override
            public void run() {
                textArea.selectAll();
                // StyledText.selectAll() doesn't fire events, so do it manually
                setActionEnableds(textArea.getSelectionCount() > 0, selectionActions);
            }
        };
        actionBars.setGlobalActionHandler(ActionFactory.SELECT_ALL.getId(), selectAllAction);
        textArea.addModifyListener(e -> selectAllAction.setEnabled(textArea.getCharCount() > 0));
        selectAllAction.setEnabled(textArea.getCharCount() > 0);

        final IAction undoAction = new Action() {
            @Override
            public void run() {
                if (undoManager != null && undoManager.canUndo()) {
                    undoManager.undo();
                }
            }
        };
        actionBars.setGlobalActionHandler(ActionFactory.UNDO.getId(), undoAction);
        undoAction.setEnabled(false);

        final IAction redoAction = new Action() {
            @Override
            public void run() {
                if (undoManager != null && undoManager.canRedo()) {
                    undoManager.redo();
                }
            }
        };
        actionBars.setGlobalActionHandler(ActionFactory.REDO.getId(), redoAction);
        redoAction.setEnabled(false);

        // set up undo/redo operations
        // TODO: make maxUndo configurable, or read text-editor preference
        undoManager = new StyledTextUndoManager(textArea, 50) {
            // wireup to update the undo/redo actions
            @Override
            public void operationsChanged() {
                if (undoAction != null) {
                    undoAction.setEnabled(undoManager.canUndo());
                }
                if (redoAction != null) {
                    redoAction.setEnabled(undoManager.canRedo());
                }
            }
        };
    }

    private static void setActionEnableds(boolean e, IAction... actions) {
        for (IAction action : actions) {
            action.setEnabled(e);
        }
    }

    /**
     * Get the path to the scratch text file.
     */
    private static IPath getScratchTextFilePath(String filename) {
        IPath path = UIActivator.getDefault().getStateLocation();
        path = path.append(filename);
        return path;
    }

    /**
     * Read the saved scratch pad text.
     */
    private static String loadScratchText(String filename, IProgressMonitor monitor) {
        if (monitor == null) {
            monitor = new NullProgressMonitor();
        }
        monitor.beginTask("", IProgressMonitor.UNKNOWN); //$NON-NLS-1$
        StringBuilder str = new StringBuilder();
        try {
            IPath path = getScratchTextFilePath(filename);
            ISchedulingRule rule = new SaveRule(path);
            Job.getJobManager().beginRule(rule, new SubProgressMonitor(monitor, 100));
            BufferedReader in = null;
            try {
                File f = path.toFile();
                if (f != null && f.exists()) {
                    // we're canceled, so just leave
                    if (monitor.isCanceled()) {
                        return ""; //$NON-NLS-1$
                    }
                    in = new BufferedReader(new InputStreamReader(new FileInputStream(f), "UTF-8")); //$NON-NLS-1$
                    char[] buf = new char[256];
                    for (int len = in.read(buf); len >= 0; len = in.read(buf)) {
                        if (monitor.isCanceled()) {
                            break;
                        }
                        str.append(buf, 0, len);
                    }
                }
            }
            catch (IOException ex) {
                UIActivator.log(UIActivator.getDefault(), IStatus.ERROR,
                        MessageFormat.format(Messages.ScratchPadView_readError, path),
                        ex);
            }
            finally {
                UIActivator.close(in);
                Job.getJobManager().endRule(rule);
            }
        }
        finally {
            monitor.done();
        }
        return str.toString();
    }

    /**
     * Save the scratch text in a background thread.
     */
    private void saveScratchText(final String text) {
        if (saveJob == null) {
            saveJob = new SaveJob(filename);
        }
        saveJob.setText(text);
    }

    /**
     * Save the scratch text immediately.
     */
    private static void saveScratchTextImmediate(String filename, final String text, IProgressMonitor monitor) {
        if (monitor == null) {
            monitor = new NullProgressMonitor();
        }
        monitor.beginTask("", IProgressMonitor.UNKNOWN); //$NON-NLS-1$
        try {
            IPath path = getScratchTextFilePath(filename);
            ISchedulingRule rule = new SaveRule(path);
            Job.getJobManager().beginRule(rule, new SubProgressMonitor(monitor, 100));
            BufferedWriter out = null;
            try {
                File f = path.toFile();
                if (f != null) {
                    out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f), "UTF-8")); //$NON-NLS-1$
                    out.write(text);
                }
            }
            catch (IOException ex) {
                UIActivator.log(UIActivator.getDefault(), IStatus.ERROR,
                        MessageFormat.format(Messages.ScratchPadView_writeError, path),
                        ex);
            }
            finally {
                UIActivator.close(out);
                Job.getJobManager().endRule(rule);
            }
        }
        finally {
            monitor.done();
        }
    }

    /**
     * A reusable job to save the text after a brief delay (to avoid overloading
     * the save logic on every keystroke).
     */
    private static class SaveJob extends Job {
        private static final int DELAY = 500;
        private String text;
        private String filename;

        private SaveJob(String filename) {
            super(Messages.ScratchPadView_saveJobName);
            this.filename = filename;
            setRule(new SaveRule(filename));
            setSystem(true);
            setUser(false);
        }

        /**
         * Set the text to save. This will reschedule the job if the text
         * changed.
         */
        private void setText(String text) {
            synchronized (this) {
                if (this.text == null || !text.equals(this.text)) {
                    this.text = text;
                    // try to cancel and reschedule -- cancel() will be false
                    // if it's currently running, in which case, the end of
                    // run() will reschedule
                    if (cancel()) {
                        schedule(DELAY);
                    }
                }
            }
        }

        @Override
        protected IStatus run(IProgressMonitor monitor) {
            // get the text and save it away
            String text;
            synchronized (this) {
                text = this.text;
                this.text = null; // ready for next run
            }

            saveScratchTextImmediate(filename, text, monitor);

            // schedule again if the text got set will this was run()'ing
            synchronized (this) {
                if (this.text != null) {
                    schedule(DELAY);
                }
            }
            return Status.OK_STATUS;
        }
    }

    /**
     * A workbench selection provider that can be bound to a StyledText.
     */
    private class SelectionProvider implements ISelectionProvider, SelectionListener {
        private ListenerList listeners = new ListenerList(ListenerList.IDENTITY);

        ISelection selection = StructuredSelection.EMPTY;

        @Override
        public void addSelectionChangedListener(ISelectionChangedListener listener) {
            if (listener != null) {
                this.listeners.add(listener);
            }
        }

        @Override
        public ISelection getSelection() {
            return this.selection;
        }

        @Override
        public void removeSelectionChangedListener(ISelectionChangedListener listener) {
            if (listener != null) {
                this.listeners.remove(listener);
            }
        }

        @Override
        public void setSelection(ISelection selection) {
            // external is not allowed to selection
        }

        private void setSelectionInternal(ISelection selection) {
            if (selection == null) {
                selection = StructuredSelection.EMPTY;
            }
            if (!selection.equals(this.selection)) {
                this.selection = selection;
                SelectionChangedEvent event = new SelectionChangedEvent(this, selection);
                fireSelectionChanged(event);
            }
        }

        protected void fireSelectionChanged(final SelectionChangedEvent event) {
            Object[] listeners = this.listeners.getListeners();
            for (int i = 0; i < listeners.length; ++i) {
                final ISelectionChangedListener l = (ISelectionChangedListener)listeners[i];
                SafeRunner.run(new SafeRunnable() {
                    @Override
                    public void run() {
                        l.selectionChanged(event);
                    }
                });
            }
        }

        @Override
        public void widgetSelected(SelectionEvent e) {
            if (e.getSource() instanceof StyledText) {
                setSelectionInternal(new TextSelection((StyledText)e.getSource()));
            }
        }

        @Override
        public void widgetDefaultSelected(SelectionEvent e) {
            // intentionally blank
        }
    }

    /**
     * A text selection implementation from a StyledText.
     */
    private static class TextSelection implements ITextSelection {
        private String text;
        private int offset;
        private int length;

        private TextSelection(StyledText textArea) {
            text = textArea.getSelectionText();
            Point p = textArea.getSelectionRange();
            offset = p.x;
            length = text.length();
        }

        @Override
        public boolean isEmpty() {
            return text != null && text.length() > 0;
        }

        @Override
        public int getOffset() {
            return offset;
        }

        @Override
        public int getLength() {
            return length;
        }

        @Override
        public int getStartLine() {
            return -1;
        }

        @Override
        public int getEndLine() {
            return -1;
        }

        @Override
        public String getText() {
            return text;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((text == null) ? 0 : text.hashCode());
            result = prime * result + length;
            result = prime * result + offset;
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            TextSelection other = (TextSelection)obj;
            if (text == null) {
                if (other.text != null) {
                    return false;
                }
            }
            else if (!text.equals(other.text)) {
                return false;
            }
            if (length != other.length) {
                return false;
            }
            if (offset != other.offset) {
                return false;
            }
            return true;
        }

        @Override
        public String toString() {
            return "TextSelection [text=" + text + ", offset=" + offset + //$NON-NLS-1$ //$NON-NLS-2$
                    ", length=" + length + "]"; //$NON-NLS-1$ //$NON-NLS-2$
        }
    }
}