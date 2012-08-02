package gsmith.eclipse.ui.commands;

import java.util.StringTokenizer;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourceAttributes;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.IDecoratorManager;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.UIJob;

public class ToggleReadOnlyFileCommandHandler extends BaseFileCommandHandler {
    private static final String READ_ONLY_FILE_DECORATOR_ID = "gsmith.eclipse.ui.decorator.readOnlyFile"; //$NON-NLS-1$

    private Boolean forcedReadOnlyState = null;

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        IFile f = getSelectedFile(event);
        if (f != null) {
            // System.out.println("#!#! Setting readonly to " +
            // forcedReadOnlyState + " on " + f.getFullPath());
            ResourceAttributes attrs = f.getResourceAttributes();
            boolean newState = (forcedReadOnlyState == null) ? !attrs.isReadOnly()
                    : forcedReadOnlyState.booleanValue();

            if (newState != attrs.isReadOnly()) {
                attrs.setReadOnly(newState);
                try {
                    f.setResourceAttributes(attrs);
                    updateDecorators(f);
                }
                catch (CoreException ex) {
                    throw new ExecutionException(ex.getMessage(), ex);
                }
            }
        }
        return null;
    }

    private void updateDecorators(IFile f) {
        UIJob job = new UIJob(Messages.ToggleReadOnlyFileCommandHandler_updateDecorationsJobName) {
            @Override
            public IStatus runInUIThread(IProgressMonitor monitor) {
                IDecoratorManager mgr = PlatformUI.getWorkbench().getDecoratorManager();
                if (mgr.getEnabled(READ_ONLY_FILE_DECORATOR_ID)) {
                    mgr.update(READ_ONLY_FILE_DECORATOR_ID);
                }
                return Status.OK_STATUS;
            }
        };
        job.schedule();
    }

    @Override
    public void setInitializationData(IConfigurationElement config,
            String propertyName, Object data) throws CoreException {
        super.setInitializationData(config, propertyName, data);
        if (data instanceof String) {
            if (data.toString().trim().length() >= 0) {
                StringTokenizer toker = new StringTokenizer(data.toString(), ","); //$NON-NLS-1$
                while (toker.hasMoreTokens()) {
                    String token = toker.nextToken().trim();
                    if ("forceReadOnly".equalsIgnoreCase(token)) //$NON-NLS-1$
                    {
                        forcedReadOnlyState = Boolean.TRUE;
                    }
                    else if ("forceWritable".equalsIgnoreCase(token)) //$NON-NLS-1$
                    {
                        forcedReadOnlyState = Boolean.FALSE;
                    }
                    else if ("forceToggle".equalsIgnoreCase(token)) //$NON-NLS-1$
                    {
                        forcedReadOnlyState = null;
                    }
                }
            }
        }
    }
}