package gsmith.eclipse.ui.screenshot;

import gsmith.eclipse.ui.images.editor.ImageDataEditorInput;
import gsmith.eclipse.ui.images.editor.ImageViewer;

import java.text.MessageFormat;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;

/**
 * Base class for all screenshot command-handlers. This takes care of open an
 * editor on the screenshot.
 */
public abstract class ScreenshotCommandHandler extends AbstractHandler {
    /**
     * Used to generate editor input display names.
     */
    private static int counter = 0;

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        ImageData im = getScreenshotImage(event);
        if (im != null) {
            handleImage(im);
        }
        return null;
    }

    /**
     * Return the image data of the screenshot, or null for none.
     */
    protected abstract ImageData getScreenshotImage(ExecutionEvent event) throws ExecutionException;

    /**
     * Handle the image data. This implementation will open an ImageViewer on
     * it.
     */
    protected void handleImage(ImageData im) throws ExecutionException {
        ImageDataEditorInput input = new ImageDataEditorInput(im,
                MessageFormat.format(
                        Messages.ScreenshotCommandHandler_editorDisplayName,
                        counter++));
        IWorkbenchPage activePage = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
        try {
            IDE.openEditor(activePage, input, ImageViewer.ID);
        }
        catch (PartInitException ex) {
            throw new ExecutionException(ex.getMessage(), ex);
        }
    }
}