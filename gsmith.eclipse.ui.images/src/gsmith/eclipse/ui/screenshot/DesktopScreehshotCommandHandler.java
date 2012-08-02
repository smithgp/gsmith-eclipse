package gsmith.eclipse.ui.screenshot;

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
    protected ImageData getScreenshotImage(ExecutionEvent event)
            throws ExecutionException {
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
        return image.getImageData();
    }

    /**
     * Screenshot handler for doing a desktop screenshot, but hiding the Eclipse
     * window first.
     */
    public static class NoEclipse extends DesktopScreehshotCommandHandler {
        @Override
        protected ImageData getScreenshotImage(ExecutionEvent event)
                throws ExecutionException {
            final Shell shell = HandlerUtil.getActiveShellChecked(event);
            shell.setVisible(false);
            // TODO: figure out something better than just sleeping.
            // if we don't do this, then we see paint remnants of the Eclipse
            // window in the screenshot
            try {
                Thread.sleep(1000);
            }
            catch (InterruptedException ex) {
                // ignore
            }
            try {
                return super.getScreenshotImage(event);
            }
            finally {
                shell.getDisplay().asyncExec(new Runnable() {
                    public void run() {
                        shell.setVisible(true);
                    }
                });
            }
        }
    }
}