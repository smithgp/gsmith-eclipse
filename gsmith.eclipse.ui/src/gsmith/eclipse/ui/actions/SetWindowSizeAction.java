package gsmith.eclipse.ui.actions;

import java.text.MessageFormat;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.window.IShellProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Shell;

/**
 * Set the size of a window.
 */
public class SetWindowSizeAction extends Action {
    private int width;
    private int height;
    private IShellProvider window;

    public SetWindowSizeAction(int width, int height, IShellProvider window) {
        super(getSizeDisplayLabel(width, height));
        this.width = width;
        this.height = height;
        this.window = window;
    }

    public static String getSizeDisplayLabel(int width, int height) {
        return MessageFormat.format(Messages.SetWindowSizeAction_labelFormat,
                String.valueOf(width), String.valueOf(height));
    }

    @Override
    public void runWithEvent(Event event) {
        resize(window.getShell(), this.width, this.height,
               // if CTRL is held down, always reset to the monitor's top-left corner
               event != null && (event.stateMask & SWT.CONTROL) == SWT.CONTROL);
    }

    /**
     * Appropriately resize the specified window.
     *
     * @param shell
     *            the window.
     * @param width
     *            the new width.
     * @param height
     *            the new height.
     * @param topLeftCorner
     *            true to always put the window in the top-left corner.
     */
    public static void resize(Shell shell, int width, int height, boolean topLeftCorner) {
        if (shell != null) {
            Rectangle cur = shell.getBounds();
            Rectangle monitor = shell.getMonitor().getBounds();
            // same size and location, skip out
            if (cur.width == width &&
                cur.height == height &&
                (!topLeftCorner || (cur.x == monitor.x && cur.y == monitor.y))) {
                return;
            }
            int x = cur.x;
            int y = cur.y;
            int wd = width;
            int ht = height;

            if (topLeftCorner) {
                x = monitor.x;
                y = monitor.y;
            }
            else {
                // if the new size puts it off the right of the screen,
                // adjust
                if ((x + wd) > (monitor.x + monitor.width)) {
                    x = (monitor.x + monitor.width) - wd;
                }
                // now, if that sent off the left-side, adjust
                if (x < monitor.x) {
                    x = monitor.x;
                }

                // if the new size puts it off the bottom of the screen,
                // adjust
                if ((y + ht) > (monitor.y + monitor.height)) {
                    y = (monitor.y + monitor.height) - ht;
                }
                // now, if that sent off the top, adjust
                if (y < monitor.y) {
                    y = monitor.y;
                }
            }

            shell.setBounds(x, y, wd, ht);
        }
    }
}