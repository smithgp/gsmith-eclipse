package gsmith.eclipse.ui.scratchpad;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.operations.AbstractOperation;
import org.eclipse.core.commands.operations.IOperationHistory;
import org.eclipse.core.commands.operations.IUndoContext;
import org.eclipse.core.commands.operations.IUndoableOperation;
import org.eclipse.core.commands.operations.ObjectUndoContext;
import org.eclipse.core.commands.operations.OperationHistoryFactory;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.custom.ExtendedModifyEvent;
import org.eclipse.swt.custom.ExtendedModifyListener;
import org.eclipse.swt.custom.StyledText;

/**
 * A utility class for implementing undo/redo logic against a StyledText widget
 * using the eclipse operations api. This only works against one StyledText at a
 * time.<br>
 * Subclasses can override {@link #operationsChanged()} to be notified of when
 * the undo/redo operations stack has been changed.
 */
public class StyledTextUndoManager {
    private IOperationHistory history;

    // setup in attach()
    private IUndoContext undoContext;
    private StyledText textArea = null;
    private ExtendedModifyListener modifyListener = null;

    private boolean inUndo = false;

    /**
     * Unattached undo manager. Use {@link #attach(StyledText, int)} when ready.
     */
    public StyledTextUndoManager() {
        history = OperationHistoryFactory.getOperationHistory();
    }

    /**
     * Attached undo manager.
     * 
     * @param textArea
     *            the StyledText.
     * @param maxUndo
     *            the maximum number of undos.
     */
    public StyledTextUndoManager(StyledText textArea, int maxUndo) {
        this();
        attach(textArea, maxUndo);
    }

    /**
     * Is this undo manager currently attached to a StyledText.
     */
    public boolean isAttached() {
        return textArea != null;
    }

    /**
     * Get the operation history this is using.
     */
    public IOperationHistory getOperationHistory() {
        return history;
    }

    /**
     * Get the undo context this is using, or null if not attached.
     */
    public IUndoContext getUndoContext() {
        return undoContext;
    }

    /**
     * Attached to the specified StyledText.
     * 
     * @param textArea
     *            the StyledText.
     * @param maxUndo
     *            the maximum number of undos.
     * @throws IllegalArgumentException
     *             if already attached.
     */
    public void attach(final StyledText textArea, int maxUndo) {
        if (isAttached()) {
            throw new IllegalArgumentException("Already attached"); //$NON-NLS-1$
        }
        this.textArea = textArea;
        if (undoContext == null) {
            undoContext = new ObjectUndoContext(this);
        }
        history.setLimit(undoContext, maxUndo);
        history.dispose(undoContext, true, true, false);

        modifyListener = new ExtendedModifyListener() {
            @Override
            public void modifyText(ExtendedModifyEvent event) {
                if (!inUndo) {
                    // the start index of the edit
                    final int start = event.start;
                    // the new text
                    final String text = textArea.getTextRange(event.start,
                            event.length);
                    // the old text being replaced
                    final String oldText = event.replacedText;
                    IUndoableOperation op = new AbstractOperation(
                            Messages.StyledTextUndoManager_opLabel) {
                        @Override
                        public IStatus undo(IProgressMonitor monitor,
                                IAdaptable info) {
                            inUndo = true;
                            try {
                                textArea.replaceTextRange(start, text.length(),
                                        oldText);
                            }
                            finally {
                                inUndo = false;
                            }
                            return Status.OK_STATUS;
                        }

                        @Override
                        public IStatus redo(IProgressMonitor monitor,
                                IAdaptable info) {
                            inUndo = true;
                            try {
                                textArea.replaceTextRange(start,
                                        oldText.length(), text);
                            }
                            finally {
                                inUndo = false;
                            }
                            return Status.OK_STATUS;
                        }

                        @Override
                        public IStatus execute(IProgressMonitor monitor,
                                IAdaptable info) {
                            return Status.OK_STATUS;
                        }
                    };
                    op.addContext(undoContext);
                    history.add(op);
                    operationsChanged();
                }
            }
        };
        this.textArea.addExtendedModifyListener(modifyListener);
    }

    /**
     * Unattached, if attached, and dispose of any resources.
     */
    public void dispose() {
        if (isAttached()) {
            if (!textArea.isDisposed()) {
                textArea.removeExtendedModifyListener(modifyListener);
            }
            history.dispose(undoContext, true, true, true);
            operationsChanged();
            undoContext = null;
            modifyListener = null;
            textArea = null;
        }
    }

    /**
     * A callback for whenever the undo/redo operations change via this object.
     */
    protected void operationsChanged() {
        // intentionally blank, subclasses can implement as needed
    }

    private void checkAttached() {
        if (!isAttached()) {
            throw new IllegalArgumentException("Not currently attached"); //$NON-NLS-1$
        }
    }

    /**
     * Undo the last change.
     * 
     * @throws IllegalArgumentException
     *             if not attached.
     */
    public void undo() {
        checkAttached();
        try {
            history.undo(undoContext, new NullProgressMonitor(), null);
        }
        catch (ExecutionException ex) {
        }
        finally {
            operationsChanged();
        }
    }

    /**
     * Returns if there is a last operation to undo.
     * 
     * @throws IllegalArgumentException
     *             if not attached.
     */
    public boolean canUndo() {
        checkAttached();
        return history.canUndo(undoContext);
    }

    /**
     * Redo the last change.
     * 
     * @throws IllegalArgumentException
     *             if not attached.
     */
    public void redo() {
        checkAttached();
        try {
            history.redo(undoContext, new NullProgressMonitor(), null);
        }
        catch (ExecutionException ex) {
        }
        finally {
            operationsChanged();
        }
    }

    /**
     * Returns if there is a last operation to redo.
     * 
     * @throws IllegalArgumentException
     *             if not attached.
     */
    public boolean canRedo() {
        checkAttached();
        return history.canRedo(undoContext);
    }
}