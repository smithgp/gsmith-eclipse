package gsmith.eclipse.ui.scratchpad;

import org.eclipse.jface.viewers.ISelection;

/**
 * Converts a selection to scratch pad text.
 *
 * @see CopySelectionCommandHandler#getTextForSelection(ISelection)
 */
@FunctionalInterface
public interface IScratchPadTextConverter {
    /**
     * Get the scratch pad text.
     *
     * @param selection
     *            the single selection (should not be IStructuredSelection).
     * @return the text, or null for none.
     */
    public String getText(Object selection);
}
