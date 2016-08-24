package gsmith.eclipse.ui.images;

import gsmith.eclipse.ui.UIActivator;

import java.io.InputStream;
import java.text.MessageFormat;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.DecorationContext;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ILightweightLabelDecorator;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IDecoratorManager;
import org.eclipse.ui.PlatformUI;

/**
 * A light weight workbench decorator for showing a thumbnail on image files
 */
public class LightweightImageDecorator implements ILightweightLabelDecorator {
    static final QualifiedName CACHE_KEY = new QualifiedName(
            LightweightImageDecorator.class.getName(), "imageCache"); //$NON-NLS-1$

    /**
     * Information about an image.
     */
    public static class ImageInfo {
        /**
         * The IResource modification stamp of the corresponding file at the
         * time the cache entry was loaded.
         */
        public final long modificationStamp;

        /**
         * The image thumbnail descriptor (can be null).
         */
        public final ImageDescriptor thumbnail;

        /**
         * The original image type from SWT.IMAGE_* (-1 if unknown).
         */
        public final int origType;

        /**
         * The original image width (-1 if unknown).
         */
        public final int origWidth;

        /**
         * The original image height (-1 if unknown).
         */
        public final int origHeight;

        ImageInfo(long modificationStamp, ImageDescriptor thumbnail, int origType, int origWidth, int origHeight) {
            this.modificationStamp = modificationStamp;
            this.thumbnail = thumbnail;
            this.origType = origType;
            this.origWidth = origWidth;
            this.origHeight = origHeight;
        }

        /**
         * Get the suffix text for a decorator.
         */
        public String getDecoratorSuffix() {
            return MessageFormat.format(
                    Messages.LightweightImageDecorator_suffix, origWidth,
                    origHeight,
                    ImageContentTypeDescriber.getImageTypeShortLabel(origType));
        }
    }

    @Override
    public void decorate(Object element, IDecoration decoration) {
        ImageInfo info = getImageInfo(element);
        // decorate if we found an image
        if (info != null && info.thumbnail != null) {
            IDecoratorManager mgr = PlatformUI.getWorkbench().getDecoratorManager();
            // if the ContentTypeDecorator is enabled, we need to hack around
            // its usage of IDecoration.REPLACE.
            boolean doHack = mgr.getBaseLabelProvider("org.eclipse.ui.ContentTypeDecorator") != null && //$NON-NLS-1$
                             mgr.getEnabled("org.eclipse.ui.ContentTypeDecorator"); //$NON-NLS-1$
            if (!doHack && DecorationContext.DEFAULT_CONTEXT instanceof DecorationContext) {
                ((DecorationContext)DecorationContext.DEFAULT_CONTEXT).putProperty(
                        IDecoration.ENABLE_REPLACE, Boolean.TRUE);
                decoration.addOverlay(info.thumbnail, IDecoration.REPLACE);
            }
            // otherwise, hack by putting it top-left, which should overlay the
            // whole image since our image should be the full size of the
            // display area
            else {
                decoration.addOverlay(info.thumbnail, IDecoration.TOP_LEFT);
            }
            if (info.origWidth >= 0 && info.origHeight >= 0) {
                decoration.addSuffix(info.getDecoratorSuffix());
            }
        }
    }

    @Override
    public void addListener(ILabelProviderListener listener) {
        // intentionally blank
    }

    @Override
    public void dispose() {
        // intentionally blank
    }

    @Override
    public boolean isLabelProperty(Object element, String property) {
        return false;
    }

    @Override
    public void removeListener(ILabelProviderListener listener) {
        // intentionally blank
    }

    /**
     * Get the image information for the specified file. This will cache it if
     * possible. The resulting thumbnail will be scaled to 16 wide or 16 tall.
     */
    public static ImageInfo getImageInfo(Object element) {
        // determine the file to decorate
        IFile file = UIActivator.adaptTo(element, IFile.class, IResource.class);
        if (file == null) {
            return null;
        }

        // check the cached image
        ImageInfo imageInfo = null;
        try {
            Object o = file.getSessionProperty(CACHE_KEY);
            if (o instanceof ImageInfo) {
                imageInfo = (ImageInfo)o;
            }
        }
        catch (CoreException ignore) {
        }

        long modificationStamp = file.getModificationStamp();
        int type = -1, width = -1, height = -1;
        // no cache or the file has changed since the cache
        if (imageInfo == null || imageInfo.modificationStamp != modificationStamp) {
            // try to load the image
            InputStream in = null;
            ImageData imageData = null;
            Image image = null;
            ImageDescriptor descriptor = null;
            try {
                in = file.getContents(true);
                Display display = UIActivator.getDisplay();
                // we have to load it via an ImageData; if we do new Image(display, in),
                // then the ImageData from image.getImageData() doesn't have type information.
                imageData = new ImageData(in);
                image = new Image(display, imageData);
            }
            catch (SWTException ignore) {
                // bad image
            }
            catch (CoreException ignore) {
                // io error
            }
            finally {
                UIActivator.close(in);
            }
            // scale the image down to 16x16 to minimize memory usage
            if (image != null) {
                try {
                    width = imageData.width;
                    height = imageData.height;
                    type = imageData.type;
                    image = scaleImage(image.getDevice(), image, 16, 16);
                    descriptor = ImageDescriptor.createFromImageData(image.getImageData());
                }
                finally {
                    UIActivator.dispose(image);
                }
            }

            // save it away (even if we didn't load an image)
            imageInfo = new ImageInfo(modificationStamp, descriptor, type, width, height);
            try {
                file.setSessionProperty(CACHE_KEY, imageInfo);
            }
            catch (CoreException ignore) {
            }
        }
        return imageInfo;
    }

    /**
     * Get the ImageDescriptor for the specified file.
     */
    public static ImageDescriptor getImageDescriptor(Object element) {
        ImageInfo imageInfo = getImageInfo(element);
        return imageInfo != null ? imageInfo.thumbnail : null;
    }

    /**
     * Scale an image to the size specified by the bounds. This will dispose of
     * the source image if a new is created.
     *
     * @param d
     *            the device to use if a new image needs to be created.
     * @param im
     *            the source image.
     * @param width
     *            the desired image width.
     * @param height
     *            the desired image height.
     * @return an image of the appropriate size.
     */
    public static Image scaleImage(Device d, Image im, int width, int height) {
        Rectangle curBounds = im.getBounds();
        // no change required
        if (curBounds.width == width && curBounds.height == height) {
            return im;
        }

        // create a new image
        Image newIm = new Image(d, width, height);
        GC gc = null;
        try {
            gc = new GC(newIm);

            // put up a border for debugging to help see where the image is
            // located in the available space
            //gc.drawRectangle(0, 0, width - 1, height - 1);

            // image is smaller than requested since, so center
            if (curBounds.width <= width && curBounds.height <= curBounds.height) {
                gc.drawImage(im, 0, 0, curBounds.width, curBounds.height,
                             (width - curBounds.width) / 2,
                             (height - curBounds.height) / 2, curBounds.width,
                             curBounds.height);
            }
            // too wide or too tall
            else {
                // shortcut if the image is perfectly proportional to avoid
                // some of the math below
                if (curBounds.width == curBounds.height) {
                    gc.drawImage(im, 0, 0, curBounds.width, curBounds.height, 0, 0, width, height);
                }
                // try to keep the proportions of the original image
                // wider than tall
                else if (curBounds.width > curBounds.height) {
                    // the proportional new height
                    int newHt = (int)(height * ((double)curBounds.height / (double)curBounds.width));
                    // and center that
                    gc.drawImage(im, 0, 0, curBounds.width, curBounds.height, 0, (height - newHt) / 2, width, newHt);
                }
                // taller than wide
                else {
                    // the proportional new width
                    int newWd = (int)(width * ((double)curBounds.width / (double)curBounds.height));
                    // and center that
                    gc.drawImage(im, 0, 0, curBounds.width, curBounds.height, (width - newWd) / 2, 0, newWd, height);

                }
            }
            // clear this up since we successfully created a new image
            UIActivator.dispose(im);
            return newIm;
        }
        catch (RuntimeException ex) {
            UIActivator.dispose(newIm);
            throw ex;
        }
        finally {
            UIActivator.dispose(gc);
        }
    }
}