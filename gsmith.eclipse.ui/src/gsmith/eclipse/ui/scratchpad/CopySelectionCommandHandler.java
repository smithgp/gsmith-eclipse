package gsmith.eclipse.ui.scratchpad;

import gsmith.eclipse.ui.UIActivator;

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.expressions.EvaluationContext;
import org.eclipse.core.expressions.EvaluationResult;
import org.eclipse.core.expressions.Expression;
import org.eclipse.core.expressions.ExpressionConverter;
import org.eclipse.core.expressions.IEvaluationContext;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * Copy the workbench selection to the scratch pad view.
 */
public class CopySelectionCommandHandler extends AbstractHandler {
    private static final String EXTENSION_ID = UIActivator.PLUGIN_ID + ".textConverters"; //$NON-NLS-1$

    /**
     * Execute the command. This will get the text from the workbench selection,
     * and send it to the scratch pad view.
     *
     * @see #getTextForSelection(ISelection)
     * @see ScratchPadView#insertText(String)
     */
    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        ISelection selection = HandlerUtil.getCurrentSelectionChecked(event);

        String text = selection != null ? getTextForSelection(selection) : null;
        if (text != null) {
            IWorkbenchPage page = HandlerUtil.getActiveWorkbenchWindowChecked(event).getActivePage();
            try {
                // show the scratch, but don't automatically give it focus
                IViewPart view = page.showView(ScratchPadView.ID, null, IWorkbenchPage.VIEW_VISIBLE);
                ((ScratchPadView)view).insertText(text);
            }
            catch (PartInitException ex) {
                throw new ExecutionException(ex.getMessage(), ex);
            }
        }

        return text;
    }

    /**
     * Represents a converter loaded from the extension point.
     */
    private static class WrappedTextConverter implements IScratchPadTextConverter {
        private IConfigurationElement element;
        private int priority = 100;

        private boolean delegateLoadFailed = false;
        private Reference<IScratchPadTextConverter> delegate = null;
        private boolean enablementLoadFailed = false;
        private Expression enablement = null;

        /**
         * Factory. This should be used, since it checks for valid elements.
         */
        private static WrappedTextConverter create(IConfigurationElement element) {
            if (element.getAttribute("class") == null) { //$NON-NLS-1$
                UIActivator.log(
                        UIActivator.getDefault(),
                        IStatus.WARNING,
                        MessageFormat.format(
                                Messages.CopySelectionCommandHandler_missingClassAttribute,
                                element.getDeclaringExtension().getNamespaceIdentifier(),
                                element.getNamespaceIdentifier()),
                        null);
                return null;
            }
            return new WrappedTextConverter(element);
        }

        private WrappedTextConverter(IConfigurationElement element) {
            this.element = element;
            String s = element.getAttribute("priority"); //$NON-NLS-1$
            if (s != null) {
                try {
                    priority = Integer.parseInt(s);
                }
                catch (NumberFormatException ignore) {
                }
            }
        }

        /**
         * Load the delegate converter from the extension.
         */
        private IScratchPadTextConverter getDelegate() {
            IScratchPadTextConverter d = delegate != null ? delegate.get() : null;
            if (d == null) {
                // try loading only once (if it fails, don't try again), and
                // if the element is still valid
                if (!delegateLoadFailed && element.isValid()) {
                    try {
                        d = (IScratchPadTextConverter)element.createExecutableExtension("class"); //$NON-NLS-1$
                        if (delegate != null) {
                            delegate.clear();
                        }
                        // save it in a SoftReference so it can go away if the
                        // classloader is released (e.g. the bundle is unloaded).
                        delegate = new SoftReference<>(d);
                    }
                    catch (Exception ex) {
                        delegateLoadFailed = true;
                        delegate = null;
                        UIActivator.log(
                                UIActivator.getDefault(),
                                IStatus.WARNING,
                                MessageFormat.format(
                                        Messages.CopySelectionCommandHandler_unableToLoadConverter,
                                        element.getAttribute("class"), //$NON-NLS-1$
                                        element.getNamespaceIdentifier()),
                                ex);
                    }
                }
            }
            return d;
        }

        /**
         * Load the enablement expression from the extension.
         */
        private Expression getEnablement() {
            // try to load it only once, and make sure the element is still
            // valid
            if (enablement == null && !enablementLoadFailed && element.isValid()) {
                IConfigurationElement[] kids = element.getChildren("enablement"); //$NON-NLS-1$
                if (kids != null && kids.length > 0) {
                    try {
                        enablement = ExpressionConverter.getDefault().perform(kids[0]);
                    }
                    catch (CoreException ex) {
                        enablementLoadFailed = true;
                        enablement = Expression.FALSE;
                        UIActivator.log(
                                UIActivator.getDefault(),
                                IStatus.WARNING,
                                MessageFormat.format(
                                        Messages.CopySelectionCommandHandler_uanbleToLoadEnablement,
                                        element.getContributor().getName()),
                                ex);
                    }
                }
                else {
                    // no <enablement> so always enabled
                    enablement = Expression.TRUE;
                }
            }
            return enablement;
        }

        /**
         * Check if this is enabled for the specified selection.
         */
        private boolean isEnabled(Object selection) {
            // disabled if we couldn't load the <enablement>
            if (enablementLoadFailed) {
                return false;
            }

            // run the enablement expression, if one was specified
            Expression enablement = getEnablement();
            if (enablement != null) {
                IEvaluationContext context = new EvaluationContext(null, selection);
                context.addVariable("selection", selection); //$NON-NLS-1$
                // REVIEWME: do something different here for Platform.class?
                // Platform.class is needed for the
                // org.eclipse.core.runtime.isBundleInstalled
                // test from PlatformPropertyTester.
                context.addVariable("platform", Platform.class); //$NON-NLS-1$
                try {
                    return enablement.evaluate(context) != EvaluationResult.FALSE;
                }
                catch (CoreException ex) {
                    UIActivator.log(
                            UIActivator.getDefault(),
                            IStatus.WARNING,
                            MessageFormat.format(
                                    Messages.CopySelectionCommandHandler_evalEnablementFailed,
                                    element.getNamespaceIdentifier()),
                            ex);
                    return false;
                }
            }
            // otherwise, always enabled
            return true;
        }

        @Override
        public String getText(Object selection) {
            // check if we're enabled for the selection
            if (!isEnabled(selection)) {
                return null;
            }
            IScratchPadTextConverter d = getDelegate();
            return d != null ? d.getText(selection) : null;
        }

        @Override
        public String toString() {
            StringBuilder buf = new StringBuilder(getClass().getName());
            buf.append("[class=").append(element.getAttribute("class")); //$NON-NLS-1$ //$NON-NLS-2$
            buf.append(",priority=").append(priority); //$NON-NLS-1$
            return buf.append(']').toString();
        }
    }

    private static Collection<WrappedTextConverter> converters = null;

    private static Collection<WrappedTextConverter> getConverters() {
        if (converters == null) {
            // TODO: listen to registry changes to reload extensions

            List<WrappedTextConverter> l = new ArrayList<>();

            // load the extensions
            IExtensionRegistry reg = Platform.getExtensionRegistry();
            IExtensionPoint point = reg.getExtensionPoint(EXTENSION_ID);
            IExtension[] exts = point != null ? point.getExtensions() : null;
            if (exts != null) {
                for (IExtension ext : exts) {
                    IConfigurationElement[] elements = ext.getConfigurationElements();
                    if (elements != null) {
                        for (IConfigurationElement element : elements) {
                            if ("converter".equals(element.getName())) { //$NON-NLS-1$
                                WrappedTextConverter c = WrappedTextConverter.create(element);
                                if (c != null) {
                                    l.add(c);
                                }
                            }
                        }
                    }
                }
            }

            // sort by priority
            Collections.sort(l, (o1, o2) -> o2.priority - o1.priority);

            converters = Collections.unmodifiableCollection(l);
        }

        return converters;
    }

    /**
     * Get the scratch pad text for the specified selection.
     *
     * @return the text, or null if none can be computed.
     */
    public static String getTextForSelection(ISelection origSelection) {
        Object[] selections;
        // support multiple selections
        if (origSelection instanceof IStructuredSelection) {
            selections = ((IStructuredSelection)origSelection).toArray();
        }
        else {
            selections = new Object[] { origSelection };
        }

        if (selections == null || selections.length <= 0) {
            return null;
        }

        // loop over them
        StringBuilder buf = new StringBuilder();
        for (Object selection : selections) {
            if (buf.length() > 0) {
                buf.append('\n');
            }
            // always handle ITextSelection directly
            if (selection instanceof ITextSelection) {
                buf.append(((ITextSelection)selection).getText());
            }
            else {
                String text = getTextFromConvertors(selection);
                if (text != null) {
                    buf.append(text);
                }
            }
        }

        return buf == null || buf.length() <= 0 ? null : buf.toString();
    }

    /**
     * Get the text for the selection from the converters.
     */
    private static String getTextFromConvertors(Object selection) {
        for (WrappedTextConverter c : getConverters()) {
            String text = null;
            try {
                text = c.getText(selection);
            }
            catch (RuntimeException ex) {
                UIActivator.log(UIActivator.getDefault(), IStatus.WARNING,
                        Messages.CopySelectionCommandHandler_textConvertFailed,
                        ex);
            }
            if (text != null) {
                return text;
            }
        }
        return null;
    }
}