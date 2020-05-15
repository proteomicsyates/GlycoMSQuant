package edu.scripps.yates.glycomsquant.gui.attached_frame;

import edu.scripps.yates.glycomsquant.gui.tables.runs.AttachedRunsDialog;
import edu.scripps.yates.glycomsquant.util.GuiUtils;

public abstract class AbstractJFrameWithAttachedHelpAndAttachedRunsDialog extends AbstractJFrameWithAttachedHelpDialog
		implements HasRunsDialogAttached {
	private AttachedRunsDialog runsDialog;
	/**
	 * 
	 */
	private static final long serialVersionUID = 8313252409686378398L;

	public AbstractJFrameWithAttachedHelpAndAttachedRunsDialog(int maxWidth) {
		super(maxWidth);

	}

	@Override
	public void dispose() {
		// also dispose the attached dialogs
		getRunsAttachedDialog().dispose();

		super.dispose();
	}

	@Override
	public void showAttachedRunsDialog() {
		final AttachedRunsDialog chartTypesHelpDialog = getRunsAttachedDialog();
		// TODO
		chartTypesHelpDialog.loadResultFolders();
		chartTypesHelpDialog.forceVisible();
		// move to the left if necessary
		final double totalWidth = this.getLocationOnScreen().getX() + this.getWidth() + chartTypesHelpDialog.getWidth();
		final double screenWidth = GuiUtils.getScreenDimension().getWidth();
		if (totalWidth > screenWidth) {
			final double offset = totalWidth - screenWidth;
			final int newX = Double.valueOf(Math.max(0.0, this.getLocationOnScreen().getX() - offset)).intValue();
			final int y = Double.valueOf(this.getLocationOnScreen().getY()).intValue();
			this.setLocation(newX,
					y);
		}
	}

	@Override
	public AttachedRunsDialog getRunsAttachedDialog() {
		if (runsDialog == null) {
			runsDialog = new AttachedRunsDialog(this, this.maxWidth);
		}
		return runsDialog;
	}

}
