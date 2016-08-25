package gsmith.eclipse.ui.screenshot;

import gsmith.eclipse.ui.UIActivator;
import gsmith.eclipse.ui.images.ImagesActivator;
import gsmith.eclipse.ui.images.editor.ImageDataEditorInput;
import gsmith.eclipse.ui.images.editor.ImageViewer;

import java.text.MessageFormat;
import java.util.concurrent.CompletableFuture;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;
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
        CompletableFuture<ImageData> f = getScreenshotImage(event);
        if (f != null) {
            Shell shell = HandlerUtil.getActiveShellChecked(event);
            // when the image is fetched
            f.whenComplete((im, ex) -> {
                // always in UI thread
                UIActivator.runInDisplayThread(() -> {
                    // show an error from generating the image
                    if (ex != null) {
                        IStatus status = null;
                        if (ex instanceof CoreException) {
                            status = ((CoreException)ex).getStatus();
                        }
                        else {
                            status = new Status(IStatus.ERROR, ImagesActivator.PLUGIN_ID, ex.getMessage(), ex);
                        }
                        ErrorDialog.openError(shell,
                            Messages.ScreenshotCommandHandler_openErrorTitle,
                            Messages.ScreenshotCommandHandler_openErrorMessage, status);
                    }
                    // otherwise, show the image
                    else if (im != null) {
                        handleImage(im);
                    }
                }, shell.getDisplay());
            });
        }
        return null;
    }

    /**
     * Return the image data of the screenshot, or null for none.
     */
    protected abstract CompletableFuture<ImageData> getScreenshotImage(ExecutionEvent event) throws ExecutionException;

    /**
     * Handle the image data. This implementation will open an ImageViewer on
     * it.
     * This can assume it's running in the SWT thread.
     */
    protected void handleImage(ImageData im) {
        ImageDataEditorInput input = new ImageDataEditorInput(im,
                MessageFormat.format(
                        Messages.ScreenshotCommandHandler_editorDisplayName,
                        counter++));
        IWorkbenchPage activePage = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
        try {
            IDE.openEditor(activePage, input, ImageViewer.ID);
        }
        catch (PartInitException ex) {
            ErrorDialog.openError(activePage.getWorkbenchWindow().getShell(),
                    Messages.ScreenshotCommandHandler_openErrorTitle,
                    Messages.ScreenshotCommandHandler_openErrorMessage, ex.getStatus());
        }
    }
}