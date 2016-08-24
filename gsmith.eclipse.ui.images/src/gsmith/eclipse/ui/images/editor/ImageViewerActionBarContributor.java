package gsmith.eclipse.ui.images.editor;

import gsmith.eclipse.ui.images.ImageContentTypeDescriber;

import java.text.MessageFormat;

import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.action.StatusLineContributionItem;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.util.SafeRunnable;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.IShellProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.dialogs.PropertyDialogAction;
import org.eclipse.ui.part.EditorActionBarContributor;

/**
 * Contributes status line items for ImageViewers.
 */
public class ImageViewerActionBarContributor extends EditorActionBarContributor {
    /**
     * Status line field to show image information
     */
    private StatusLineContributionItem imageInfoStatusItem;

    /**
     * Status line field for mouse location
     */
    private StatusLineContributionItem mouseLocationStatusItem;

    /**
     * Status line field for RGB.
     */
    private StatusLineContributionItem rgbStatusItem;

    /**
     * Status line field to zoom factor.
     */
    private StatusLineContributionItem zoomStatusItem;

    /**
     * Action for showing properties dialog.
     */
    private PropertyDialogAction propertiesAction;

    /**
     * A selection provider for the editors' inputs.
     */
    private ISelectionProvider editorInputSelectionProvider;

    /**
     * The current editor.
     */
    private ImageViewer currentEditor;

    /**
     * Listener to changes to the editor properties
     */
    private IPropertyChangeListener editorListener = new IPropertyChangeListener() {
        @Override
        public void propertyChange(final PropertyChangeEvent evt) {
            if (currentEditor != null && evt.getSource() == currentEditor) {
                Runnable r = null;
                if (ImageViewer.PROP_CURRENT_IMAGE_INFORMATION.equals(evt.getProperty())) {
                    r = () -> updateImageInformation((int[])evt.getNewValue());
                }
                else if (ImageViewer.PROP_PIXEL_LOCATION.equals(evt.getProperty())) {
                    r = () -> updateMouseLocation((Point)evt.getNewValue());
                }
                else if (ImageViewer.PROP_ZOOM_FACTOR.equals(evt.getProperty())) {
                    final Object zoom = evt.getNewValue();
                    if (zoom instanceof Double) {
                        r = () -> updateZoomFactor((Double)zoom);
                    }
                }
                if (r != null) {
                    currentEditor.getSite().getShell().getDisplay().asyncExec(r);
                }
            }
        }
    };

    @Override
    public void init(IActionBars bars) {
        super.init(bars);

        // create a selection provider on our currentEditor's input (which
        // should adapt to the stuff required by our property page).
        // NOTE: if ImageViewer ends up utilizing a selection provider, this
        // should use that instead
        editorInputSelectionProvider = new ISelectionProvider() {
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
                if (selection == null) {
                    selection = StructuredSelection.EMPTY;
                }
                if (!selection.equals(this.selection)) {
                    this.selection = selection;
                    final SelectionChangedEvent event = new SelectionChangedEvent(this, selection);
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
            }
        };
        updateEditorInputSelectionProvider(currentEditor);

        // hook up File|Properties (ALT+ENTER) so that it works from within
        // the editor
        IShellProvider shellProvider = () -> currentEditor.getSite().getShell();
        propertiesAction = new PropertyDialogAction(shellProvider, editorInputSelectionProvider) {
            // override to include isApplicableForSelection in the calculation.
            // this is to disable the action on editor inputs that our
            // property page(s) don't support, so we don't get the silly
            // "No property pages for ." info dialog.
            @Override
            public void selectionChanged(IStructuredSelection selection) {
                setEnabled(isApplicableForSelection(selection));
            }
        };
        bars.setGlobalActionHandler(ActionFactory.PROPERTIES.getId(), propertiesAction);
    }

    @Override
    public void dispose() {
        super.dispose();
        if (propertiesAction != null) {
            propertiesAction.dispose();
        }
    }

    @Override
    public void contributeToStatusLine(IStatusLineManager statusLineManager) {
        // order matters -- these 2 can disappear as the mouse moves, so putting
        // imageInfo last cause the least flashing
        mouseLocationStatusItem = new StatusLineContributionItem(
                "image.mouse.location", //$NON-NLS-1$
                getMouseLocationString(Integer.MAX_VALUE / 2, Integer.MAX_VALUE / 2).length());
        statusLineManager.add(mouseLocationStatusItem);

        rgbStatusItem = new StatusLineContributionItem("image.mouse.rgb", //$NON-NLS-1$
                getRGBString(1000, 1000, 1000).length());
        statusLineManager.add(rgbStatusItem);

        imageInfoStatusItem = new StatusLineContributionItem("image.info", //$NON-NLS-1$
                getImageInfoString(SWT.IMAGE_JPEG, Integer.MAX_VALUE,
                        Integer.MAX_VALUE).length());
        statusLineManager.add(imageInfoStatusItem);

        zoomStatusItem = new StatusLineContributionItem("zoom", 10); //$NON-NLS-1$
        statusLineManager.add(zoomStatusItem);
    }

    @Override
    public void setActiveEditor(IEditorPart targetEditor) {
        if (targetEditor != currentEditor) {
            if (currentEditor != null) {
                currentEditor.removePropertyChangeListener(editorListener);
            }

            if (targetEditor instanceof ImageViewer) {
                currentEditor = (ImageViewer)targetEditor;
            }
            else {
                currentEditor = null;
            }

            updateImageInformation(currentEditor != null ? currentEditor.getCurrentImageInformation() : null);
            // we get it on next mouse move
            updateMouseLocation(null);

            updateZoomFactor(currentEditor != null ? currentEditor.getZoomFactor() : null);

            updateEditorInputSelectionProvider(currentEditor);

            if (currentEditor != null) {
                currentEditor.addPropertyChangeListener(editorListener);
            }
        }
    }

    private void updateEditorInputSelectionProvider(IEditorPart editor) {
        if (editorInputSelectionProvider != null) {
            ISelection sel = null;
            if (editor != null) {
                sel = new StructuredSelection(editor.getEditorInput());
            }
            editorInputSelectionProvider.setSelection(sel);
        }
    }

    /**
     * Update the image info status line with the specified { SWT.IMAGE_* type,
     * width, height }.
     */
    private void updateImageInformation(int[] imageInfo) {
        if (imageInfoStatusItem != null) {
            if (imageInfo != null && imageInfo.length >= 3) {
                imageInfoStatusItem.setText(getImageInfoString(imageInfo[0], imageInfo[1], imageInfo[2]));
            }
            else {
                imageInfoStatusItem.setText(""); //$NON-NLS-1$
            }
        }
    }

    /**
     * Get the image info string.
     *
     * @param type
     *            the SWT.IMAGE_* image type.
     * @param width
     *            the image width.
     * @param height
     *            the image height.
     */
    private String getImageInfoString(int type, int width, int height) {
        String typeStr = ImageContentTypeDescriber.getImageTypeShortLabel(type);
        return MessageFormat.format(
                Messages.ImageViewerActionBarContributor_imageInfo, typeStr,
                width, height);
    }

    /**
     * Update the mouse location and RGB status line items.
     */
    private void updateMouseLocation(Point p) {
        if (mouseLocationStatusItem != null) {
            if (p != null) {
                mouseLocationStatusItem.setText(getMouseLocationString(p.x, p.y));
            }
            else {
                mouseLocationStatusItem.setText(""); //$NON-NLS-1$
            }
        }
        if (rgbStatusItem != null) {
            RGB rgb = currentEditor != null && p != null ? currentEditor.getImageRGB(p.x, p.y) : null;
            if (rgb != null) {
                rgbStatusItem.setText(getRGBString(rgb.red, rgb.blue, rgb.green));
            }
            else {
                rgbStatusItem.setText(""); //$NON-NLS-1$
            }
        }
    }

    private void updateZoomFactor(Double d) {
        if (zoomStatusItem != null) {
            if (d != null) {
                zoomStatusItem.setText(String.format("%3d%%", (int)(d * 100.0))); //$NON-NLS-1$
            }
            else {
                zoomStatusItem.setText(""); //$NON-NLS-1$
            }
        }
    }

    /**
     * Get the mouse location string.
     */
    private String getMouseLocationString(int x, int y) {
        return MessageFormat.format(Messages.ImageViewerActionBarContributor_mouseLocation, x, y);
    }

    /**
     * Get the RGB display string.
     */
    private String getRGBString(int red, int blue, int green) {
        return MessageFormat.format(Messages.ImageViewerActionBarContributor_rgb, red, green, blue);
    }
}