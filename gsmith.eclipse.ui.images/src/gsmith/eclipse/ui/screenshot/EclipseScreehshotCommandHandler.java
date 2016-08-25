package gsmith.eclipse.ui.screenshot;

import java.util.concurrent.CompletableFuture;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * Screenshot command handler for taking a screenshot of the Eclipse window.
 */
public class EclipseScreehshotCommandHandler extends ScreenshotCommandHandler {
    @Override
    protected CompletableFuture<ImageData> getScreenshotImage(ExecutionEvent event) throws ExecutionException {
        Shell shell = HandlerUtil.getActiveShellChecked(event);
        // this will be the full (with trim) size of the window, at the display
        // location
        Rectangle bounds = shell.getBounds();
        // do the screenshot based on the display (not the shell), so we can
        // correctly pickup the trim
        GC gc = new GC(shell.getDisplay());
        Image image = null;
        try {
            image = new Image(shell.getDisplay(), bounds.width, bounds.height);
            gc.copyArea(image, bounds.x, bounds.y);
        }
        finally {
            gc.dispose();
        }
        return CompletableFuture.completedFuture(image.getImageData());
    }
}