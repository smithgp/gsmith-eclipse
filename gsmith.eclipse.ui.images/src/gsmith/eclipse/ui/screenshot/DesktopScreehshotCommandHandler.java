package gsmith.eclipse.ui.screenshot;

import java.util.concurrent.CompletableFuture;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * Screenshot command handler for taking a screenshot of the desktop.
 */
public class DesktopScreehshotCommandHandler extends ScreenshotCommandHandler {
    @Override
    protected CompletableFuture<ImageData> getScreenshotImage(ExecutionEvent event) throws ExecutionException {
        Shell shell = HandlerUtil.getActiveShellChecked(event);
        Display display = shell.getDisplay();
        GC gc = new GC(display);
        Image image = null;
        try {
            image = new Image(display, display.getBounds());
            gc.copyArea(image, 0, 0);
        }
        finally {
            gc.dispose();
        }
        return CompletableFuture.completedFuture(image.getImageData());
    }

    /**
     * Screenshot handler for doing a desktop screenshot, but hiding the Eclipse
     * window first.
     */
    public static class NoEclipse extends DesktopScreehshotCommandHandler {
        @Override
        protected CompletableFuture<ImageData> getScreenshotImage(ExecutionEvent event) throws ExecutionException {
            final Shell shell = HandlerUtil.getActiveShellChecked(event);
            // hide the eclipse window now
            shell.setVisible(false);
            CompletableFuture<ImageData> f = new CompletableFuture<>();
            // run the super method later, sending the result to our return future
            shell.getDisplay().asyncExec(() -> {
                try {
                    CompletableFuture<ImageData> superf = super.getScreenshotImage(event);
                    if (superf != null) {
                        superf.whenComplete((im, ex) -> {
                           if (ex != null) {
                               f.completeExceptionally(ex);
                           }
                           else {
                               f.complete(im);
                           }
                        });
                    }
                    else {
                        f.complete(null);
                    }
                }
                catch (Exception ex) {
                    f.completeExceptionally(ex);
                }
                finally {
                    f.complete(null); // just in case, always finish it out so the eclipse will get shown again
                }
            });

            // always re-show the eclipse window after the image is generated
            return f.whenComplete((im, ex) -> shell.getDisplay().asyncExec(() -> shell.setVisible(true)));
        }
    }
}