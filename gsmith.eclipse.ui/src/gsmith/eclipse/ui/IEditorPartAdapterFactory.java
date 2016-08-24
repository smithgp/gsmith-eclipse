package gsmith.eclipse.ui;

import java.util.Collections;

import org.eclipse.core.expressions.ICountable;
import org.eclipse.core.expressions.IIterable;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;

/**
 * Adapt some classes to IEditorParts, or the underlying IEditorPart.
 */
public class IEditorPartAdapterFactory implements IAdapterFactory {
    private static final Class<?>[] ADAPTER_LIST = {
            IFile.class, IResource.class, IIterable.class, ICountable.class
    };

    private static final ICountable COUNT_1 = () -> 1;

    @Override
    public <T> T getAdapter(final Object adaptableObject, Class<T> adapterType) {
        if (adaptableObject instanceof IEditorPart) {
            // do these on the editor part
            if (adapterType == IIterable.class) {
                return adapterType.cast((IIterable<IEditorPart>)Collections.singleton((IEditorPart)adaptableObject)::iterator);
            }
            else if (adapterType == ICountable.class) {
                return adapterType.cast(COUNT_1);
            }

            // otherwise, we're doing something on the editor input
            final IEditorInput input = ((IEditorPart)adaptableObject).getEditorInput();
            if (input != null) {
                return input.getAdapter(adapterType);
            }
        }
        return null;
    }

    @Override
    public Class<?>[] getAdapterList() {
        return ADAPTER_LIST;
    }
}