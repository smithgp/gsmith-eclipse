package gsmith.eclipse.ui.commands;

import java.text.MessageFormat;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.progress.UIJob;

public class TouchFileCommandHandler extends BaseFileCommandHandler {
    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        final IFile f = getSelectedFile(event);
        if (f != null) {
            UIJob job = new UIJob(MessageFormat.format(
                    Messages.TouchFileCommandHandler_touchJobName,
                    f.getFullPath())) {
                @Override
                public IStatus runInUIThread(IProgressMonitor monitor) {
                    try {
                        // System.out.println("#!#! Touching " +
                        // f.getFullPath());
                        f.touch(monitor);
                        return Status.OK_STATUS;
                    }
                    catch (CoreException ex) {
                        return ex.getStatus();
                    }
                }
            };
            job.schedule();
        }
        return null;
    }
}