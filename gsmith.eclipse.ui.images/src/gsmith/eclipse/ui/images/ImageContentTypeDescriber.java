package gsmith.eclipse.ui.images;

import java.io.IOException;
import java.io.InputStream;

import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.content.IContentDescriber;
import org.eclipse.core.runtime.content.IContentDescription;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.graphics.ImageData;

/**
 * Content type matcher for image files.
 */
public class ImageContentTypeDescriber implements IContentDescriber {
    /**
     * The namespace for the the content description properties this will set.
     */
    public static final String IMAGES_NS = ImagesActivator.PLUGIN_ID;

    /**
     * Name of the image height property this sets.
     */
    public static final QualifiedName HEIGHT = new QualifiedName(IMAGES_NS,
            "height"); //$NON-NLS-1$

    /**
     * Name of the image width property this sets.
     */
    public static final QualifiedName WIDTH = new QualifiedName(IMAGES_NS,
            "width"); //$NON-NLS-1$

    /**
     * Name of the image depth property this sets.
     */
    public static final QualifiedName DEPTH = new QualifiedName(IMAGES_NS,
            "depth"); //$NON-NLS-1$

    /**
     * Name fo the image type (from SWT.IMAGE_*) property this sets.
     */
    public static final QualifiedName TYPE = new QualifiedName(IMAGES_NS,
            "type"); //$NON-NLS-1$

    private static final QualifiedName[] SUPPORTED_OPTIONS = {
            WIDTH, HEIGHT, DEPTH, TYPE
    };

    @Override
    public int describe(InputStream contents, IContentDescription description)
            throws IOException {
        try {
            ImageData im = new ImageData(contents);
            if (description.isRequested(HEIGHT)) {
                description.setProperty(HEIGHT, im.height);
            }
            if (description.isRequested(WIDTH)) {
                description.setProperty(WIDTH, im.width);
            }
            if (description.isRequested(DEPTH)) {
                description.setProperty(DEPTH, im.depth);
            }
            if (description.isRequested(TYPE)) {
                description.setProperty(TYPE, im.type);
            }

            return VALID;
        }
        catch (SWTException ex) {
            return INVALID;
        }
    }

    @Override
    public QualifiedName[] getSupportedOptions() {
        return SUPPORTED_OPTIONS;
    }

    /**
     * Get a short text string for the specific image type.
     */
    public static String getImageTypeShortLabel(int type) {
        String typeStr = ""; //$NON-NLS-1$
        switch (type) {
            case SWT.IMAGE_BMP:
            case SWT.IMAGE_BMP_RLE:
            case SWT.IMAGE_OS2_BMP:
                typeStr = Messages.ImageContentTypeDescriber_bmpFormatName;
                break;
            case SWT.IMAGE_GIF:
                typeStr = Messages.ImageContentTypeDescriber_gifFormatName;
                break;
            case SWT.IMAGE_ICO:
                typeStr = Messages.ImageContentTypeDescriber_icoFormatName;
                break;
            case SWT.IMAGE_JPEG:
                typeStr = Messages.ImageContentTypeDescriber_jpgFormatName;
                break;
            case SWT.IMAGE_PNG:
                typeStr = Messages.ImageContentTypeDescriber_pngFormatName;
                break;
            case SWT.IMAGE_TIFF:
                typeStr = Messages.ImageContentTypeDescriber_tiffFormatName;
                break;
            default:
                typeStr = Messages.ImageContentTypeDescriber_unknownFormatName;
                break;
        }
        return typeStr;
    }
}