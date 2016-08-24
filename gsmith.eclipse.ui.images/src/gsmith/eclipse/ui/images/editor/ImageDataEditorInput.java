package gsmith.eclipse.ui.images.editor;

import gsmith.eclipse.ui.images.ImagesActivator;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;

/**
 * An editor input for directly displaying ImageData in the ImageViewer.
 */
public class ImageDataEditorInput implements IEditorInput, IAdaptable {
    private final ImageData imageData;

    private final String name;

    private final ImageDescriptor icon;

    private final String toolTipText;

    /**
     * Constructor.
     *
     * @param imageData
     *            the image data (required).
     * @param name
     *            the editor display name (required).
     * @param toolTipText
     *            the editor tool tip text (optional).
     * @param icon
     *            the editor display icon (optional).
     */
    public ImageDataEditorInput(ImageData imageData, String name,
            String toolTipText, ImageDescriptor icon) {
        this.imageData = imageData;
        this.name = name;
        this.toolTipText = toolTipText;
        this.icon = icon != null ? icon : ImagesActivator.imageDescriptorFromPlugin(ImagesActivator.PLUGIN_ID, "icons/image.png"); //$NON-NLS-1$
    }

    /**
     * Constructor. This will use the default icon and use the name of the tool
     * tip text.
     *
     * @param imageData
     *            the image data (required).
     * @param name
     *            the editor display name (required).
     */
    public ImageDataEditorInput(ImageData imageData, String name) {
        this(imageData, name, name, null);
    }

    @Override
    public <T> T getAdapter(Class<T> adapter) {
        if (adapter == ImageData.class) {
            return adapter.cast(imageData);
        }

        return Platform.getAdapterManager().getAdapter(this, adapter);
    }

    @Override
    public boolean exists() {
        return false;
    }

    @Override
    public ImageDescriptor getImageDescriptor() {
        return icon;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public IPersistableElement getPersistable() {
        // this editor state cannot be persisted
        return null;
    }

    @Override
    public String getToolTipText() {
        return toolTipText;
    }
}