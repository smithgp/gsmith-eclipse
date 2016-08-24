package gsmith.eclipse.ui.commands;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

public class RefreshWorkspaceCommandHandler extends AbstractHandler {
    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        Job job = new WorkspaceJob(Messages.RefreshWorkspaceCommandHandler_refreshJobName) {
            @Override
            public IStatus runInWorkspace(IProgressMonitor monitor)throws CoreException {
                ResourcesPlugin.getWorkspace().getRoot().refreshLocal(IResource.DEPTH_INFINITE, monitor);
                return Status.OK_STATUS;
            }

            @Override
            public boolean belongsTo(Object family) {
                return family == ResourcesPlugin.FAMILY_MANUAL_REFRESH;
            }
        };
        job.setRule(ResourcesPlugin.getWorkspace().getRuleFactory().refreshRule(ResourcesPlugin.getWorkspace().getRoot()));
        job.setSystem(false);
        job.setUser(true);
        job.schedule();
        return null;
    }
}