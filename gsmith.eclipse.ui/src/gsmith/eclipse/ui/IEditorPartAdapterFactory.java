package gsmith.eclipse.ui;

import java.util.Collections;
import java.util.Iterator;

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

    private static final ICountable COUNT_1 = new ICountable() {
        @Override
        public int count() {
            return 1;
        }
    };

    @Override
    public Object getAdapter(final Object adaptableObject,
            @SuppressWarnings("rawtypes") Class adapterType) {
        if (adaptableObject instanceof IEditorPart) {
            // do these on the editor part
            if (adapterType == IIterable.class) {
                return new IIterable() {
                    @SuppressWarnings("rawtypes")
                    @Override
                    public Iterator iterator() {
                        return Collections.singleton(adaptableObject).iterator();
                    }
                };
            }
            else if (adapterType == ICountable.class) {
                return COUNT_1;
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