package edu.scripps.yates.glycomsquant.gui.attached_frame;

import edu.scripps.yates.glycomsquant.gui.tables.runs.AttachedRunsDialog;

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
	}

	@Override
	public AttachedRunsDialog getRunsAttachedDialog() {
		if (runsDialog == null) {
			runsDialog = new AttachedRunsDialog(this, this.maxWidth);
		}
		return runsDialog;
	}

}
