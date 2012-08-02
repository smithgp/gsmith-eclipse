package gsmith.eclipse.ui.images;

import gsmith.eclipse.ui.UIActivator;
import gsmith.eclipse.ui.images.editor.ImageViewer;

import java.io.InputStream;
import java.text.MessageFormat;
import java.text.NumberFormat;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbenchPropertyPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PropertyPage;

/**
 * A property page for image data objects. The element must either adapt to
 * ImageData, or be an image.
 */
public class ImagePropertyPage extends PropertyPage implements IWorkbenchPropertyPage {
    public ImagePropertyPage() {
        super();
        noDefaultAndApplyButton();
    }

    @Override
    protected Control createContents(Composite parent) {
        ImageData imageData = null;
        try {
            imageData = getImageData();
        }
        catch (SWTException ex) {
            // bad image file, fall through
        }
        catch (CoreException ex) {
            // unable to read file, fall through
        }
        if (imageData == null) {
            Label l = new Label(parent, SWT.LEFT);
            l.setText(Messages.ImagePropertyPage_notAvailable);
            return l;
        }

        Composite main = new Composite(parent, SWT.NONE);
        main.setLayout(new GridLayout(2, false));
        NumberFormat numFormat = NumberFormat.getNumberInstance();

        // width, height, type
        createLabelAndText(main, Messages.ImagePropertyPage_widthLabel,
                MessageFormat.format(Messages.ImagePropertyPage_widthText,
                        numFormat.format(imageData.width)));
        createLabelAndText(main, Messages.ImagePropertyPage_heightLabel,
                MessageFormat.format(Messages.ImagePropertyPage_heightText,
                        numFormat.format(imageData.height)));
        createLabelAndText(main, Messages.ImagePropertyPage_imageTypeLabel,
                getImageFormatLabel(imageData.type));
        // raw byte size (from data.length)?
        createLabelAndText(main, Messages.ImagePropertyPage_imageSizeLabel,
                MessageFormat.format(Messages.ImagePropertyPage_imageSizeText,
                        numFormat.format(imageData.data.length)));

        Label sep = new Label(main, SWT.SEPARATOR | SWT.HORIZONTAL);
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = 2;
        sep.setLayoutData(gd);

        // depth
        createLabelAndText(main, Messages.ImagePropertyPage_colorDepthLabel,
                numFormat.format(imageData.depth));

        // transparent (from transparentPixel/alpha/alphaData)
        if (imageData.transparentPixel >= 0 && imageData.palette != null) {
            try {
                RGB color = imageData.palette.getRGB(imageData.transparentPixel);
                createLabelAndText(
                        main,
                        Messages.ImagePropertyPage_transparentPixelLabel,
                        MessageFormat.format(
                                Messages.ImagePropertyPage_transparentPixelText,
                                imageData.transparentPixel, color.red,
                                color.green, color.blue));
            }
            catch (SWTException ex) {
                createLabelAndText(main,
                        Messages.ImagePropertyPage_transparencyLabel,
                        Messages.ImagePropertyPage_noTransparency);
            }
        }
        else {
            // show this as a % since it's global
            if (imageData.alpha >= 0 && imageData.alpha <= 255) {
                int pct = (int)Math.round(100.0d * ((double)imageData.alpha / 255.0d));
                createLabelAndText(
                        main,
                        Messages.ImagePropertyPage_transparencyLabel,
                        MessageFormat.format(
                                Messages.ImagePropertyPage_transparencyPercentText,
                                pct));
            }
            else if (imageData.alphaData != null) {
                // REVIEWME: do something with alphaData, maybe calculate a
                // high/low/avg
                createLabelAndText(main,
                        Messages.ImagePropertyPage_transparencyLabel,
                        Messages.ImagePropertyPage_transparencyPerPixel);
            }
            else {
                createLabelAndText(main,
                        Messages.ImagePropertyPage_transparencyLabel,
                        Messages.ImagePropertyPage_noTransparency);
            }
        }

        // scanlinePad?
        // bytesPerLine (scanline)?

        // delayTime (for animated gif)


        return main;
    }

    private void createLabelAndText(Composite parent, String label, String text) {
        Label l = new Label(parent, SWT.LEFT);
        l.setText(label);
        Text t = new Text(parent, SWT.SINGLE | SWT.READ_ONLY);
        t.setBackground(parent.getBackground());
        t.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        t.setText(text);
    }

    /**
     * Get a text label for the image format type.
     */
    private static String getImageFormatLabel(int type) {
        String typeStr = ""; //$NON-NLS-1$
        switch (type) {
            case SWT.IMAGE_BMP:
                typeStr = Messages.ImagePropertyPage_bmpFormat;
                break;
            case SWT.IMAGE_BMP_RLE:
                typeStr = Messages.ImagePropertyPage_rleBmpFormat;
                break;
            case SWT.IMAGE_OS2_BMP:
                typeStr = Messages.ImagePropertyPage_os2BmpFormat;
                break;
            case SWT.IMAGE_GIF:
                typeStr = Messages.ImagePropertyPage_gifFormat;
                break;
            case SWT.IMAGE_ICO:
                typeStr = Messages.ImagePropertyPage_icoFormat;
                break;
            case SWT.IMAGE_JPEG:
                typeStr = Messages.ImagePropertyPage_jpgFormat;
                break;
            case SWT.IMAGE_PNG:
                typeStr = Messages.ImagePropertyPage_pngFormat;
                break;
            case SWT.IMAGE_TIFF:
                typeStr = Messages.ImagePropertyPage_tiffFormat;
                break;
            default:
                typeStr = Messages.ImagePropertyPage_unknownFormat;
                break;
        }
        return typeStr;
    }

    /**
     * Get the image data from the selected element.
     */
    private ImageData getImageData() throws CoreException, SWTException {
        // just adapt it, this will catch ImageDataEditorInputs
        ImageData data = UIActivator.adaptTo(getElement(), ImageData.class);
        if (data == null) {
            // try to get it from the file
            IFile f = UIActivator.adaptTo(getElement(), IFile.class, IResource.class);
            if (f != null) {
                // if open in an editor, use that to get the image data.
                for (IEditorReference ref : PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getEditorReferences()) {
                    if (ImageViewer.ID.equals(ref.getId())) {
                        IEditorInput input = ref.getEditorInput();
                        IFile inputFile = UIActivator.adaptTo(input, IFile.class, IResource.class);
                        if (inputFile != null && f.equals(inputFile)) {
                            IEditorPart editor = ref.getEditor(false);
                            if (editor instanceof ImageViewer) {
                                data = ((ImageViewer)editor).getImageData();
                                if (data != null) {
                                    //System.out.println("#!#! Reusing from editor"); //$NON-NLS-1$
                                    break;
                                }
                            }
                        }
                    }
                }
                // load it from the file
                if (data == null) {
                    InputStream in = null;
                    try {
                        in = f.getContents();
                        data = new ImageData(in);
                        //System.out.println("#!#! loaded from file"); //$NON-NLS-1$
                    }
                    finally {
                        UIActivator.close(in);
                    }
                }
            }
        }
        return data;
    }
}