package gsmith.eclipse.ui.commands;

import gsmith.eclipse.ui.UIActivator;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.StringTokenizer;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.ISources;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * Base class for our file commands.
 */
public abstract class BaseFileCommandHandler extends AbstractHandler implements
        IExecutableExtension {
    /**
     * The places from which to try to get an IFile.
     */
    protected static enum FileGettingType {
        /**
         * Look in the selection.
         */
        SELECTION,

        /**
         * Look in the editor input.
         */
        EDITOR_INPUT,

        /**
         * Look in the editor.
         */
        EDITOR,

        /**
         * Look in the selection for menus.
         */
        MENU_SELECTION,

        /**
         * Look in the editor the menus.
         */
        MENU_EDITOR
    };

    private Collection<FileGettingType> howToGet = null;

    /**
     * Get the ordered set to how to find the IFile.
     */
    protected Collection<FileGettingType> getFileGettingTypes() {
        return howToGet;
    }

    /**
     * Set the ordered set to how to find the IFile.
     */
    protected void setFileGettingTypes(Collection<FileGettingType> howToGet) {
        this.howToGet = howToGet;
    }

    /**
     * Get the file from the execution context, using the
     * {@link #getFileGettingTypes() control}.
     */
    protected IFile getSelectedFile(ExecutionEvent event)
            throws ExecutionException {
        // default behavior is to use the workbench's active editor (if it's the
        // active part)
        Collection<FileGettingType> howToGet = getFileGettingTypes();
        if (howToGet == null || howToGet.isEmpty()) {
            IEditorPart editor = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
            if (editor != null && editor.getEditorInput() != null &&
                    editor.equals(editor.getSite().getPage().getActivePart())) {
                Object o = editor.getEditorInput().getAdapter(IResource.class);
                if (o instanceof IFile) {
                    return (IFile)o;
                }
            }
            return null;
        }
        for (FileGettingType type : howToGet) {
            // System.out.println("#!#! (" + type + ") Checking...");
            IFile f = getSelectionFile(event, type);
            if (f != null) {
                // System.out.println("#!#! (" + type + " found: " +
                // f.getFullPath());
                return f;
            }
        }
        // System.out.println("#!#! Didn't find file");
        return null;
    }

    /**
     * Try to get the selected file, according to the type.
     */
    // REVIEWME: move to a method on the FileGettingType enum
    private IFile getSelectionFile(ExecutionEvent event, FileGettingType type)
            throws ExecutionException {
        switch (type) {
            case SELECTION: {
                return getFile(HandlerUtil.getCurrentSelection(event));
            }
            case EDITOR_INPUT: {
                Object input = HandlerUtil.getVariable(event,
                        ISources.ACTIVE_EDITOR_INPUT_NAME);
                if (input instanceof IEditorInput) {
                    Object o = ((IEditorInput)input).getAdapter(IResource.class);
                    if (o instanceof IFile) {
                        return (IFile)o;
                    }
                }
                break;
            }
            case EDITOR: {
                IEditorPart editor = HandlerUtil.getActiveEditor(event);
                if (editor != null && editor.getEditorInput() != null) {
                    Object o = editor.getEditorInput().getAdapter(
                            IResource.class);
                    if (o instanceof IFile) {
                        return (IFile)o;
                    }
                }
                break;
            }
            case MENU_EDITOR: {
                return getFile(HandlerUtil.getActiveMenuEditorInput(event));
            }
            case MENU_SELECTION: {
                return getFile(HandlerUtil.getActiveMenuSelection(event));
            }
        }
        return null;
    }

    /**
     * Get the file for the selection.
     */
    protected IFile getFile(ISelection sel) {
        if (sel instanceof IStructuredSelection) {
            Object o = ((IStructuredSelection)sel).getFirstElement();
            return UIActivator.adaptTo(o, IFile.class, IResource.class);
        }
        return null;
    }

    /**
     * Read the initialization data from the extension. This will initialize
     * {@link #getFileGettingTypes() control} for how this will look for the
     * file.
     */
    @Override
    public void setInitializationData(IConfigurationElement config,
            String propertyName, Object data) throws CoreException {
        if (data instanceof String) {
            if (data.toString().trim().length() >= 0) {
                // EnumSet doesn't maintain add() order -- it uses a bit buffer
                // based on the enum ordinals, so iterator() returns in the
                // order of the enum value declarations.
                // Use an ordered collection instead
                Set<FileGettingType> howToGet = new LinkedHashSet<FileGettingType>();// EnumSet.noneOf(FileGettingType.class);
                StringTokenizer toker = new StringTokenizer(data.toString(), ","); //$NON-NLS-1$
                while (toker.hasMoreTokens()) {
                    String token = toker.nextToken().trim();
                    try {
                        FileGettingType value = FileGettingType.valueOf(token.toUpperCase());
                        if (value != null) {
                            howToGet.add(value);
                        }
                    }
                    catch (IllegalArgumentException ignore) {
                        // ex.printStackTrace();
                    }
                }
                // only set if we found some valid directives
                if (!howToGet.isEmpty()) {
                    setFileGettingTypes(howToGet);
                }
            }
        }
    }
}