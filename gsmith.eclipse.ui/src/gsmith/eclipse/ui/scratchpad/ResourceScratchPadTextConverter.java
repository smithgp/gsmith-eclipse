package gsmith.eclipse.ui.scratchpad;

import gsmith.eclipse.ui.UIActivator;

import org.eclipse.core.resources.IResource;

/**
 * Text converter for things that adapt to IResource. This will return the full
 * workspace path of the resource.
 */
public class ResourceScratchPadTextConverter implements IScratchPadTextConverter {
    @Override
    public String getText(Object selection) {
        IResource r = UIActivator.adaptTo(selection, IResource.class);
        return r != null ? r.getFullPath().toPortableString() : null;
    }
}