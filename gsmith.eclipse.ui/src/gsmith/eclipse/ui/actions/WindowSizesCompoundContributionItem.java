package gsmith.eclipse.ui.actions;

import gsmith.eclipse.ui.WindowSize;

import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.Separator;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.CompoundContributionItem;
import org.eclipse.ui.menus.CommandContributionItem;
import org.eclipse.ui.menus.CommandContributionItemParameter;

/**
 * Contribution item to add a set of window resizing actions, plus one to allow
 * a user-specified size.
 */
public class WindowSizesCompoundContributionItem extends CompoundContributionItem {
    /**
     * Command id to prompt for a custom window size.
     */
    public static final String WINDOW_SIZE_COMMAND_ID = "gsmith.eclipse.ui.command.windowSize"; //$NON-NLS-1$

    @Override
    protected IContributionItem[] getContributionItems() {
        // put up items for the default sizes
        WindowSize[] defaultSizes = WindowSize.getDefaultSizes();
        IContributionItem[] items = new IContributionItem[defaultSizes.length > 0 ? defaultSizes.length + 2 : 1];
        int i = 0;
        final IWorkbenchWindow shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        if (defaultSizes.length > 0) {
            for (i = 0; i < defaultSizes.length; i++) {
                items[i] = new ActionContributionItem(new SetWindowSizeAction(
                        defaultSizes[i].width, defaultSizes[i].height, shell));
            }

            // separator
            items[i++] = new Separator();
        }
        // use the command to do a user-prompt
        CommandContributionItemParameter commandParm = new CommandContributionItemParameter(
                shell, null, WINDOW_SIZE_COMMAND_ID,
                CommandContributionItem.STYLE_PUSH);
        // but fix the label for the menu
        commandParm.label = Messages.WindowSizesCompoundContributionItem_customSizeActionLabel;
        items[i++] = new CommandContributionItem(commandParm);

        return items;
    }
}