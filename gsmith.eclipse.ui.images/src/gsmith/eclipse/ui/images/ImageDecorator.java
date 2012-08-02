package gsmith.eclipse.ui.images;

import static gsmith.eclipse.ui.images.LightweightImageDecorator.getImageDescriptor;
import static gsmith.eclipse.ui.images.LightweightImageDecorator.getImageInfo;
import gsmith.eclipse.ui.images.LightweightImageDecorator.ImageInfo;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.ILabelDecorator;
import org.eclipse.swt.graphics.Image;

/**
 * An image decorator for showing thumbnails for image files. This will lose any
 * overlay decoration added by light weight decorators (e.g. the readOnly lock
 * badge, error badges, etc.), which is generally bad.
 */
public class ImageDecorator extends BaseLabelProvider implements ILabelDecorator {
    @Override
    public String decorateText(String text, Object element) {
        ImageInfo info = getImageInfo(element);
        if (info != null) {
            return text + info.getDecoratorSuffix();
        }
        else {
            return null;
        }
    }

    @Override
    public Image decorateImage(Image origImage, Object element) {
        ImageDescriptor descriptor = getImageDescriptor(element);
        if (descriptor != null) {
            return descriptor.createImage();
        }
        else {
            return null;
        }
    }
}