package gsmith.eclipse.ui.images.editor;

import org.eclipse.jface.action.IAction;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.actions.ActionDelegate;

/**
 * Base class for zoom-related ImageViewer actions.
 */
public abstract class ZoomAction extends ActionDelegate implements IEditorActionDelegate {
    protected ImageViewer currentEditor = null;

    @Override
    public void setActiveEditor(IAction action, IEditorPart targetEditor) {
        currentEditor = targetEditor instanceof ImageViewer ? (ImageViewer)targetEditor : null;
        action.setEnabled(currentEditor != null);
    }

    // TODO: make zoom in/out go in a better increment
    /**
     * Zoom in action.
     */
    public static class ZoomIn extends ZoomAction {
        @Override
        public void run(IAction action) {
            if (currentEditor != null) {
                currentEditor.setZoomFactor(currentEditor.getZoomFactor() * 1.1);
            }
        }
    }

    /**
     * Zoom out action.
     */
    public static class ZoomOut extends ZoomAction {
        @Override
        public void run(IAction action) {
            if (currentEditor != null) {
                currentEditor.setZoomFactor(currentEditor.getZoomFactor() * 0.9);
            }
        }
    }

    /**
     * Reset zoom action.
     */
    public static class ResetZoom extends ZoomAction {
        @Override
        public void run(IAction action) {
            if (currentEditor != null) {
                currentEditor.setZoomFactor(1.0d);
            }
        }
    }
}