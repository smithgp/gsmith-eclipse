package gsmith.eclipse.ui.images;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
    private static final String BUNDLE_NAME = "gsmith.eclipse.ui.images.messages"; //$NON-NLS-1$
    public static String LightweightImageDecorator_suffix;
    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    public static String ImageContentTypeDescriber_bmpFormatName;
    public static String ImageContentTypeDescriber_gifFormatName;
    public static String ImageContentTypeDescriber_icoFormatName;
    public static String ImageContentTypeDescriber_jpgFormatName;
    public static String ImageContentTypeDescriber_pngFormatName;
    public static String ImageContentTypeDescriber_tiffFormatName;
    public static String ImageContentTypeDescriber_unknownFormatName;

    public static String ImagePropertyPage_bmpFormat;
    public static String ImagePropertyPage_colorDepthLabel;
    public static String ImagePropertyPage_gifFormat;
    public static String ImagePropertyPage_heightLabel;
    public static String ImagePropertyPage_heightText;
    public static String ImagePropertyPage_icoFormat;
    public static String ImagePropertyPage_imageSizeLabel;
    public static String ImagePropertyPage_imageSizeText;
    public static String ImagePropertyPage_imageTypeLabel;
    public static String ImagePropertyPage_jpgFormat;
    public static String ImagePropertyPage_notAvailable;
    public static String ImagePropertyPage_noTransparency;
    public static String ImagePropertyPage_os2BmpFormat;
    public static String ImagePropertyPage_pngFormat;
    public static String ImagePropertyPage_previewLabel;
    public static String ImagePropertyPage_rleBmpFormat;
    public static String ImagePropertyPage_tiffFormat;
    public static String ImagePropertyPage_transparencyLabel;
    public static String ImagePropertyPage_transparencyPercentText;
    public static String ImagePropertyPage_transparencyPerPixel;
    public static String ImagePropertyPage_transparentPixelLabel;
    public static String ImagePropertyPage_transparentPixelText;
    public static String ImagePropertyPage_unknownFormat;
    public static String ImagePropertyPage_widthLabel;
    public static String ImagePropertyPage_widthText;

    private Messages() {
    }
}
