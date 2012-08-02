package gsmith.eclipse.ui.images.editor;

import gsmith.eclipse.ui.UIActivator;
import gsmith.eclipse.ui.images.ImagesActivator;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceRuleFactory;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.MultiRule;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.util.SafeRunnable;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.MouseTrackAdapter;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IReusableEditor;
import org.eclipse.ui.IStorageEditorInput;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.eclipse.ui.dialogs.ContainerGenerator;
import org.eclipse.ui.part.EditorPart;
import org.eclipse.ui.part.FileEditorInput;

/**
 * A simple image viewer editor.
 */
public class ImageViewer extends EditorPart implements IReusableEditor {
    /**
     * The eclipse workbench id of this editor.
     */
    public static final String ID = "gsmith.eclipse.ui.images.editor.ImageViewer"; //$NON-NLS-1$

    /**
     * Property change event name for {@link #getCurrentImageInformation()
     * current image information}. For events on this, oldVal will always be
     * null.
     */
    public static final String PROP_CURRENT_IMAGE_INFORMATION = "currentImageInformation"; //$NON-NLS-1$

    /**
     * Property change event name for changes to the mouse pixel location and
     * corresponding to {@link #getImageRGB(int, int) RGB}. For events on this,
     * oldVal will always be null.
     */
    public static final String PROP_PIXEL_LOCATION = "mousePixelLocation"; //$NON-NLS-1$

    /**
     * Property change event name for change to the zoom factor.
     */
    public static final String PROP_ZOOM_FACTOR = "zoomFactor"; //$NON-NLS-1$

    /**
     * The IFile session property for the last zoom factor.
     */
    private static final QualifiedName LAST_ZOOM_FACTOR_KEY = new QualifiedName(
            ImageViewer.class.getName(), PROP_ZOOM_FACTOR);

    /**
     * The image we're showing.
     * 
     * @see #showImage()
     */
    private Image image;

    /**
     * The image data we're showing.
     * 
     * @see #loadImageData()
     */
    private ImageData imageData;

    // the UI
    private ScrolledComposite scroll;
    private Canvas imageCanvas;

    /**
     * The zoom factor to use when displaying. -1 here is a marker for
     * setInput() to try to read the last zoom factor from the file's session
     * property; otherwise, it will default to 1.0 (100%).
     */
    private double zoomFactor = -1.0d;

    /**
     * The maximum possible zoom factor for the current image.
     */
    private double maxZoomFactor = 1.0d;

    /**
     * A file listener on our possible file input.
     * 
     * @see #registerResourceListener(IEditorInput)
     * @see #unregisterResourceListener(IEditorInput)
     */
    private ImageResourceChangeListener inputListener = null;

    /**
     * Property change listeners.
     * 
     * @see #addPropertyChangeListener(IPropertyChangeListener)
     */
    private ListenerList propChangeListeners = null;

    /**
     * A mouse listener for letting the user make a selection of the image.
     */
    // FIXME: comment out the selection listener for now
    /*private Listener selectionMouseListener = new Listener() {
        Point initial = null;
        Tracker tracker = null;

        @Override
        public void handleEvent(Event event) {
            switch (event.type) {
                case SWT.MouseDown: // first click
                {
                    initial = new Point(event.x, event.y);
                    break;
                }
                case SWT.MouseMove: // move enough to start it
                {
                    if (initial == null)
                        return;
                    int deltaX = initial.x - event.x, deltaY = initial.y -
                            event.y;
                    // check for jitter
                    if (Math.abs(deltaX) < 2 && Math.abs(deltaY) < 2) {
                        return;
                    }
                    tracker = new Tracker((Composite)event.widget, SWT.RESIZE);
                    Rectangle rect = new Rectangle(initial.x, initial.y,
                            deltaX, deltaY);
                    tracker.setRectangles(new Rectangle[] { rect });
                    tracker.open();
                    // TODO: have changes to the tracker also update
                    // PROP_PIXEL_LOCATION

                    // open() will wait until UI is done, so fall through to
                    // the MouseUp done logic
                }
                case SWT.MouseUp: // done
                {
                    initial = null;
                    if (tracker != null) {
                        // TODO: do something with the selection --
                        // set the editor up with a SelectionProvider and
                        // set this in that; then a highlighting box at the
                        // selection until ESC is pressed or another selection
                        // is made
                        System.out.println("#!#! selection=" + tracker.getRectangles()[0]); //$NON-NLS-1$
                        tracker.dispose();
                        tracker = null;
                    }
                }
            }
        }
    };*/

    @Override
    public void init(IEditorSite site, IEditorInput input)
            throws PartInitException {
        // we need either an IStorage or an input that can return an ImageData
        if (!(input instanceof IStorageEditorInput) &&
                input.getAdapter(ImageData.class) == null) {
            throw new PartInitException("Unable to read input: " + input); //$NON-NLS-1$
        }
        setSite(site);
        setInput(input, false);
    }

    @Override
    public void setInput(IEditorInput input) {
        setInput(input, true);
    }

    private void setInput(IEditorInput input, boolean notify) {
        IEditorInput old = getEditorInput();
        if (input != old) {
            unregisterResourceListener(old);
            setPartName(input);
            // initialize the zoom factor, trying to use the last know setting
            if (zoomFactor < 0.0) {
                IFile f = getFileFor(input);
                if (f != null) {
                    try {
                        Object prop = f.getSessionProperty(LAST_ZOOM_FACTOR_KEY);
                        if (prop instanceof Number) {
                            zoomFactor = ((Number)prop).doubleValue();
                        }
                        else if (prop instanceof String) {
                            try {
                                zoomFactor = Double.parseDouble((String)prop);
                            }
                            catch (NumberFormatException ex) {
                                // intentionally blank, fall through
                            }
                        }
                    }
                    catch (CoreException ex) {
                        // intentionally blank, fall through
                    }
                }

                if (zoomFactor < 0.0) {
                    zoomFactor = 1.0d;
                }
            }
            if (notify) {
                super.setInputWithNotify(input);
            }
            else {
                super.setInput(input);
            }

            // start the image load job, after we set the input
            startImageLoad();
            // start listing after we start the load
            registerResourceListener(input);
        }
    }

    /**
     * Set the part name based on the editor input.
     */
    private void setPartName(IEditorInput input) {
        String imageName = null;
        if (input instanceof IStorageEditorInput) {
            try {
                imageName = ((IStorageEditorInput)input).getStorage().getName();
            }
            catch (CoreException ex) {
                // intentionally blank
            }
        }
        // this will catch ImageDataEditorInput as well
        if (imageName == null) {
            imageName = input.getName();
        }
        if (imageName == null) {
            imageName = getSite().getRegisteredName();
        }
        setPartName(imageName);
    }

    /**
     * Get the IFile corresponding to the specified editor input, or null for
     * none.
     */
    private IFile getFileFor(IEditorInput input) {
        if (input instanceof IFileEditorInput) {
            return ((IFileEditorInput)input).getFile();
        }
        else if (input instanceof IStorageEditorInput) {
            try {
                IStorage storage = ((IStorageEditorInput)input).getStorage();
                if (storage instanceof IFile) {
                    return (IFile)storage;
                }
            }
            catch (CoreException ignore) {
                // intentionally blank
            }
        }
        return null;
    }

    /**
     * Unregister any change listeners for the specified input.
     */
    protected void unregisterResourceListener(IEditorInput input) {
        if (input != null && inputListener != null) {
            inputListener.stop();
            inputListener = null;
        }
    }

    /**
     * Register any change listeners on the specified new input.
     */
    protected void registerResourceListener(IEditorInput input) {
        if (input != null) {
            if (inputListener != null) {
                inputListener.stop();
            }
            inputListener = null;
            IFile file = getFileFor(input);
            if (file != null) {
                inputListener = new ImageResourceChangeListener(file);
                inputListener.start();
            }
        }
    }

    /**
     * Initialize the UI.
     */
    @Override
    public void createPartControl(Composite parent) {
        scroll = new ScrolledComposite(parent, SWT.BORDER | SWT.H_SCROLL |
                SWT.V_SCROLL);
        // TODO: fixup scrolling (page) increment as things resize
        scroll.getHorizontalBar().setIncrement(10);
        scroll.getHorizontalBar().setPageIncrement(100);
        scroll.getVerticalBar().setIncrement(10);
        scroll.getVerticalBar().setPageIncrement(100);

        imageCanvas = new Canvas(scroll, SWT.NONE);
        scroll.setContent(imageCanvas);
        imageCanvas.setSize(0, 0);
        // make the canvas paint the image, if we have one
        imageCanvas.addPaintListener(new PaintListener() {
            @Override
            public void paintControl(PaintEvent e) {
                Rectangle bounds = imageCanvas.getBounds();
                // showImage() should be setting the imageCanvas bounds to the
                // zoomed size
                if (image != null) {
                    Rectangle imBounds = image.getBounds();
                    e.gc.drawImage(image, 0, 0, imBounds.width,
                            imBounds.height, 0, 0, bounds.width, bounds.height);
                }
                else {
                    e.gc.setBackground(e.display.getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
                    e.gc.fillRectangle(bounds);
                }
            }
        });
        // fire property change events for mouse motion
        imageCanvas.addMouseMoveListener(new MouseMoveListener() {
            @Override
            public void mouseMove(MouseEvent e) {
                firePropertyChangeEvent(PROP_PIXEL_LOCATION, null,
                        convertImageCanvasLocationToPixel(new Point(e.x, e.y)));
            }
        });
        imageCanvas.addMouseTrackListener(new MouseTrackAdapter() {
            @Override
            public void mouseEnter(MouseEvent e) {
                firePropertyChangeEvent(PROP_PIXEL_LOCATION, null,
                        convertImageCanvasLocationToPixel(new Point(e.x, e.y)));
            }

            @Override
            public void mouseExit(MouseEvent e) {
                firePropertyChangeEvent(PROP_PIXEL_LOCATION, null, null);
            }
        });

        // hook up selecting a rectangular region
        // FIXME: disable this for now.
        //imageCanvas.addListener(SWT.MouseUp, selectionMouseListener);
        //imageCanvas.addListener(SWT.MouseDown, selectionMouseListener);
        //imageCanvas.addListener(SWT.MouseMove, selectionMouseListener);

        // initiate an image load
        startImageLoad();
    }

    /**
     * Convert an imageCanvas location to a pixel point in the image, according
     * to the zoomFactor.
     * 
     * @return p, updated.
     */
    private Point convertImageCanvasLocationToPixel(Point p) {
        p.x = (int)((double)p.x / zoomFactor);
        p.y = (int)((double)p.y / zoomFactor);
        return p;
    }

    /**
     * This will start a job to load the image for the current editor input.
     * This can be started from any thread.
     */
    private void startImageLoad() {
        // skip if the UI hasn't been initialized yet, because
        // createPartControl() will do this
        if (imageCanvas == null) {
            return;
        }
        // clear out the current image
        Runnable r = new Runnable() {
            public void run() {
                if (image != null) {
                    image.dispose();
                    imageData = null;
                    image = null;
                    imageCanvas.setSize(0, 0);
                    scroll.redraw();
                    firePropertyChangeEvent(PROP_CURRENT_IMAGE_INFORMATION,
                            null, null);
                    firePartPropertyChanged(PROP_PIXEL_LOCATION, null, null);
                }
            }
        };
        imageCanvas.getDisplay().asyncExec(r);

        // load the image in the background to keep the ui fresh
        Job job = new Job(MessageFormat.format(
                Messages.ImageViewer_loadImageTask, getPartName())) {
            @Override
            protected IStatus run(IProgressMonitor monitor) {
                monitor.beginTask(getName(), IProgressMonitor.UNKNOWN);
                try {
                    loadImageData();

                    // show the image on the next SWT exec
                    Runnable r = new Runnable() {
                        @Override
                        public void run() {
                            showImage(true);
                        }
                    };
                    imageCanvas.getDisplay().asyncExec(r);

                    return Status.OK_STATUS;
                }
                catch (CoreException ex) {
                    return ex.getStatus();
                }
                catch (SWTException ex) {
                    return new Status(IStatus.ERROR, UIActivator.PLUGIN_ID,
                            ex.getMessage());
                }
                finally {
                    monitor.done();
                }
            }
        };
        job.setUser(true);
        job.schedule();
    }

    /**
     * Load the image data from the current editor input. This operation can
     * take time and should not be called on the ui thread.
     */
    private void loadImageData() throws CoreException {
        IEditorInput input = getEditorInput();
        Object o = input.getAdapter(ImageData.class);
        if (o instanceof ImageData) {
            imageData = (ImageData)o;
        }
        else if (input instanceof IStorageEditorInput) {
            IStorage storage = ((IStorageEditorInput)input).getStorage();
            InputStream in = storage.getContents();
            try {
                ImageLoader loader = new ImageLoader();
                ImageData[] images = loader.load(in);
                if (images != null && images.length > 0) {
                    imageData = images[0];
                }
            }
            finally {
                UIActivator.close(in);
            }
        }
        else {
            imageData = null;
        }
        // save this away so we don't compute it all the time
        this.maxZoomFactor = determineMaxZoomFactor();
    }

    /**
     * Refresh the ui to display the current image. This needs to be run in the
     * SWT thread.
     * 
     * @param createImage
     *            true to (re)create the image object from the imageData, false
     *            to reuse.
     */
    private void showImage(boolean createImage) {
        if (imageData != null) {
            imageCanvas.setCursor(imageCanvas.getDisplay().getSystemCursor(
                    SWT.CURSOR_WAIT));
            try {
                if (createImage || image == null) {
                    // dispose of the old image
                    if (image != null && !image.isDisposed()) {
                        image.dispose();
                        image = null;
                    }
                    image = new Image(imageCanvas.getDisplay(), imageData);
                }
                Rectangle imageSize = image.getBounds();
                // apply the zoomFactor -- imageCanvas' repaint listener will
                // use the imageCanvas size to do the image dithering
                imageCanvas.setSize(
                        (int)((double)imageSize.width * zoomFactor),
                        (int)((double)imageSize.height * zoomFactor));
                scroll.redraw();
                //double maxZoom = getMaxZoomFactor();
                //System.out.println("#!#! " + getPartName() + " maxZoom=" +
                //    maxZoom / 100 + "%");
                //System.out.println("     maxSize=" + (int)(imageData.width *
                //    maxZoom) + "x" +
                //    (int)(imageData.height * maxZoom));
            }
            finally {
                imageCanvas.setCursor(null);
            }
            firePropertyChangeEvent(PROP_CURRENT_IMAGE_INFORMATION, null,
                    getCurrentImageInformation());
            // clear this out, it'll update on the next mouse move
            firePropertyChangeEvent(PROP_PIXEL_LOCATION, null, null);
        }
    }

    @Override
    public void setFocus() {
        if (scroll != null && !scroll.isDisposed()) {
            scroll.setFocus();
        }
    }

    @Override
    public void dispose() {
        super.dispose();
        // do this directly
        if (this.inputListener != null) {
            inputListener.stop();
            inputListener = null;
        }
        if (image != null && !image.isDisposed()) {
            image.dispose();
            image = null;
        }
        this.propChangeListeners = null;
    }

    @Override
    public boolean isDirty() {
        return false;
    }

    @Override
    public void doSave(IProgressMonitor monitor) {
    }

    @Override
    public void doSaveAs() {
        // get a new path from the user
        SaveImageAsDialog d = new SaveImageAsDialog(getSite().getShell());
        // initialize the dialog path and file, as best as possible, including
        // pre-selecting the image type.
        int origImageType = imageData.type;
        // default to PNG, if the imageData doesn't yet have a type (i.e. as
        // from screenshot)
        if (origImageType < 0) {
            origImageType = SWT.IMAGE_PNG;
        }
        IFile origFile = getFileFor(getEditorInput());
        if (origFile != null) {
            d.setOriginalFile(origFile, origImageType);
        }
        else {
            IPath initialFileName = Path.fromPortableString(getPartName()).removeFileExtension();
            d.setOriginalName(initialFileName.toPortableString(), origImageType);
        }
        d.create();
        if (d.open() != SaveAsDialog.OK) {
            return;
        }

        // get the selected file path
        IPath path = d.getResult();
        if (path == null) {
            return;
        }
        // add a file extension if there isn't one
        if (path.getFileExtension() == null) {
            path = path.addFileExtension(d.getSaveAsImageExt());
        }

        final IFile dest = ResourcesPlugin.getWorkspace().getRoot().getFile(path);
        if (dest == null || (origFile != null && dest.equals(origFile))) {
            return;
        }
        final int imageType = d.getSaveAsImageType();

        // create a scheduling rule for the file edit/creation
        IResourceRuleFactory ruleFactory = dest.getWorkspace().getRuleFactory();
        ISchedulingRule rule = null;
        if (dest.exists()) {
            rule = ruleFactory.modifyRule(dest);
            rule = MultiRule.combine(rule,
                    ruleFactory.validateEditRule(new IResource[] { dest }));
        }
        else {
            rule = ruleFactory.createRule(dest);
            // this might end up creating some folders, so include those, too
            IContainer parent = dest.getParent();
            while (parent != null && !(parent instanceof IProject) &&
                    !parent.exists()) {
                rule = MultiRule.combine(rule, ruleFactory.createRule(parent));
                parent = parent.getParent();
            }
        }
        // create the file
        WorkspaceModifyOperation op = new WorkspaceModifyOperation(rule) {
            @Override
            protected void execute(IProgressMonitor monitor)
                    throws CoreException, InvocationTargetException,
                    InterruptedException {
                try {
                    if (dest.exists()) {
                        if (!dest.getWorkspace().validateEdit(new IFile[] { dest }, getSite().getShell()).isOK()) {
                            return;
                        }
                    }
                    saveTo(imageData, dest, imageType, monitor);
                }
                catch (IOException ex) {
                    throw new InvocationTargetException(ex);
                }
            }
        };
        ProgressMonitorDialog pmd = new ProgressMonitorDialog(
                getSite().getShell());
        try {
            pmd.run(true, true, op);
            // reset our editor input to the file, if weren't not open on a
            // file.
            if (getFileFor(getEditorInput()) == null) {
                setInput(new FileEditorInput(dest));
            }
        }
        catch (InvocationTargetException ex) {
            Throwable t = ex.getCause();

            String title = Messages.ImageViewer_saveErrorTitle;
            String mesg = MessageFormat.format(
                    Messages.ImageViewer_saveErrorMessage,
                    path.toPortableString());
            // ImagesActivator.getDefault().log(IStatus.WARNING, mesg, t);
            IStatus st = null;
            if (t instanceof CoreException) {
                st = ((CoreException)t).getStatus();
            }
            else {
                st = new Status(IStatus.ERROR, ImagesActivator.PLUGIN_ID, 0,
                        t.toString(), t);
            }
            if (st.getSeverity() != IStatus.CANCEL) {
                ErrorDialog.openError(getSite().getShell(), title, mesg, st);
            }
        }
        catch (InterruptedException ex) {
            // ignore
        }
    }

    private void saveTo(ImageData imageData, final IFile dest,
            final int imageType, IProgressMonitor monitor)
            throws CoreException, InterruptedException, IOException {
        //int taskSize = 500;
        //if (!dest.getParent().exists())
        //{
        //    taskSize += 500;
        //}
        // do an indeterminate progress monitor so that something shows, since
        // the generation of the image data doesn't report progress
        monitor.beginTask(dest.getFullPath().toPortableString(),
                IProgressMonitor.UNKNOWN/* taskSize */);
        try {
            if (!dest.getParent().exists()) {
                ContainerGenerator gen = new ContainerGenerator(
                        dest.getFullPath().removeLastSegments(1));
                gen.generateContainer(new SubProgressMonitor(monitor, 500));
                if (monitor.isCanceled()) {
                    throw new InterruptedException();
                }
            }
            final ImageLoader loader = new ImageLoader();
            loader.data = new ImageData[] {
                imageData
            };

            // this uses a byte array
            //ByteArrayOutputStream bout = new ByteArrayOutputStream();
            //loader.save(bout, imageType);
            //byte[] bytes = bout.toByteArray();
            //ByteArrayInputStream in = new ByteArrayInputStream(bytes);

            // but, let's use pipes instead so we don't have to buffer the whole
            // thing unnecessarily in memory
            PipedInputStream pin = new PipedInputStream();
            final PipedOutputStream pout = new PipedOutputStream(pin);
            // the write to the pipe has to happen in a different thread or
            // else we get deadlock
            Job writeJob = new Job(Messages.ImageViewer_saveAsPipeJobName) {
                @Override
                protected IStatus run(IProgressMonitor monitor) {
                    IStatus status = Status.OK_STATUS;
                    try {
                        loader.save(pout, imageType);
                        pout.flush();
                    }
                    // SWTException from loader.save(),
                    // IOException from pout.flush()
                    catch (Exception ex) {
                        status = new Status(
                                IStatus.ERROR,
                                ImagesActivator.PLUGIN_ID,
                                MessageFormat.format(
                                        Messages.ImageViewer_saveAsLoadImageDataError,
                                        dest.getFullPath()), ex);
                    }
                    finally {
                        UIActivator.close(pout);
                    }
                    // always do our own error dialog
                    if (!status.isOK()) {
                        ImagesActivator.getDefault().getLog().log(status);
                        final IStatus fstatus = status;
                        getSite().getShell().getDisplay().asyncExec(
                                new Runnable() {
                                    @Override
                                    public void run() {
                                        ErrorDialog.openError(
                                                getSite().getShell(),
                                                Messages.ImageViewer_saveErrorTitle,
                                                MessageFormat.format(
                                                        Messages.ImageViewer_saveErrorMessage,
                                                        dest.getFullPath()),
                                                fstatus);
                                    }
                                });
                    }
                    return Status.OK_STATUS;
                }
            };
            writeJob.setSystem(true);
            writeJob.setUser(false);
            writeJob.schedule();

            BufferedInputStream in = new BufferedInputStream(pin);
            try {
                // try reading one byte to make sure that loader.save() actually
                // worked before we destroy or create a file.
                in.mark(1);
                int first = in.read();
                // the Job should have shown the error dialog if we don't get a
                // first byte
                if (first != -1) {
                    in.reset();
                    if (dest.exists()) {
                        dest.setContents(in, true, true,
                                new SubProgressMonitor(monitor, 500));
                    }
                    else {
                        dest.create(in, true, new SubProgressMonitor(monitor,
                                500));
                    }
                }
            }
            finally {
                UIActivator.close(in);
            }
        }
        finally {
            monitor.done();
        }
    }

    @Override
    public boolean isSaveAsAllowed() {
        return true;
    }

    /**
     * Add a property change listener.
     * 
     * @see #PROP_CURRENT_IMAGE_INFORMATION
     * @see #PROP_PIXEL_LOCATION
     */
    public void addPropertyChangeListener(IPropertyChangeListener l) {
        if (l != null) {
            if (propChangeListeners == null) {
                propChangeListeners = new ListenerList();
            }
            propChangeListeners.add(l);
        }
    }

    /**
     * Remove a property change listener.
     */
    public void removePropertyChangeListener(IPropertyChangeListener l) {
        if (l != null) {
            if (propChangeListeners != null) {
                propChangeListeners.remove(l);
                if (propChangeListeners.isEmpty()) {
                    propChangeListeners = null;
                }
            }
        }
    }

    /**
     * Fire the specified property change event tothe listeners.
     */
    private void firePropertyChangeEvent(String property, Object oldVal,
            Object newVal) {
        if (propChangeListeners != null && !propChangeListeners.isEmpty()) {
            Object[] listeners = propChangeListeners.getListeners();
            final PropertyChangeEvent evt = new PropertyChangeEvent(this,
                    property, oldVal, newVal);
            for (final Object l : listeners) {
                if (l instanceof IPropertyChangeListener) {
                    SafeRunner.run(new SafeRunnable() {
                        @Override
                        public void run() throws Exception {
                            ((IPropertyChangeListener)l).propertyChange(evt);
                        }
                    });
                }
            }
        }
    }

    public ImageData getImageData() {
        return imageData;
    }

    /**
     * Get the current image information.
     * 
     * @return { SWT.IMAGE_* type, width, height } or null for no image
     */
    public int[] getCurrentImageInformation() {
        if (imageData != null) {
            return new int[] {
                    imageData.type, imageData.width, imageData.height
            };
        }
        return null;
    }

    /**
     * Get the RGB for the specified pixel, or null if no image.
     */
    public RGB getImageRGB(int x, int y) {
        // make sure the requested pixel location is valid, otherwise you get
        // an SWT IllegalArgumentException
        if (imageData != null && x >= 0 && x < imageData.width && y >= 0 &&
                y < imageData.height) {
            return imageData.palette.getRGB(imageData.getPixel(x, y));
        }
        return null;
    }

    /**
     * Determine the max zoom factor for the current image size.
     */
    private double determineMaxZoomFactor() {
        if (imageData != null) {
            double maxWidth = ((double)Integer.MAX_VALUE) / imageData.width;
            double maxHeight = ((double)Integer.MAX_VALUE) / imageData.height;
            return Math.min(maxWidth, maxHeight);
        }
        return 1.0d;
    }

    /**
     * Get the maximum zoom factor for the current image.
     */
    public double getMaxZoomFactor() {
        return maxZoomFactor;
    }

    /**
     * Get the current zoom factor.
     */
    public double getZoomFactor() {
        return this.zoomFactor;
    }

    /**
     * Update the zoom factor. This can safely called from any thread. It will
     * trigger an image redraw is needed. If the passed in value is larger than
     * the {@link #getMaxZoomFactor() max zoom factor}, the max zoom factor will
     * used instead.
     */
    public void setZoomFactor(double newZoom) {
        // don't go bigger than the maz zoom
        newZoom = Math.min(newZoom, getMaxZoomFactor());
        if (zoomFactor != newZoom && newZoom > 0.0d) {
            Double old = Double.valueOf(zoomFactor);
            this.zoomFactor = newZoom;
            // save it away
            IFile f = getFileFor(getEditorInput());
            if (f != null) {
                try {
                    f.setSessionProperty(LAST_ZOOM_FACTOR_KEY,
                            Double.valueOf(newZoom));
                }
                catch (CoreException ignore) {
                    // just ignore it
                }
            }
            // tell everyone
            firePropertyChangeEvent(PROP_ZOOM_FACTOR, old,
                    Double.valueOf(newZoom));
            // redraw the image
            if (imageCanvas != null) {
                Runnable r = new Runnable() {
                    public void run() {
                        showImage(false);
                    }
                };
                UIActivator.runInDisplayThread(r, imageCanvas.getDisplay());
            }
        }
    }

    /**
     * This handles changes to a file-based editor input.
     */
    private class ImageResourceChangeListener implements IResourceChangeListener {
        IResource imageFile;

        public ImageResourceChangeListener(IResource imageFile) {
            this.imageFile = imageFile;
        }

        /**
         * Start listening to file changes.
         */
        void start() {
            imageFile.getWorkspace().addResourceChangeListener(this);
        }

        /**
         * Stop listening to file changes.
         */
        void stop() {
            imageFile.getWorkspace().removeResourceChangeListener(this);
        }

        @Override
        public void resourceChanged(IResourceChangeEvent event) {
            IResourceDelta delta = event.getDelta().findMember(
                    imageFile.getFullPath());
            if (delta != null) {
                // file deleted -- close the editor
                if (delta.getKind() == IResourceDelta.REMOVED) {
                    Runnable r = new Runnable() {
                        public void run() {
                            // this needs to be run in the SWT thread
                            getSite().getPage().closeEditor(ImageViewer.this,
                                    false);
                        }
                    };
                    getSite().getShell().getDisplay().asyncExec(r);
                }
                // file changed -- reload image
                else if (delta.getKind() == IResourceDelta.CHANGED) {
                    int flags = delta.getFlags();
                    if ((flags & IResourceDelta.CONTENT) != 0 ||
                            (flags & IResourceDelta.LOCAL_CHANGED) != 0) {
                        startImageLoad();
                    }
                }
                // TODO: handle file rename to setPartName()
            }
        }
    }
}