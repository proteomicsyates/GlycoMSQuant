package edu.scripps.yates.glycomsquant.gui.secondary_frame;

import edu.scripps.yates.glycomsquant.gui.tables.run_table.AttachedRunsAttachedDialog;

public abstract class AbstractJFrameWithAttachedHelpAndAttachedRunsDialog extends AbstractJFrameWithAttachedHelpDialog
		implements HasRunsDialogAttached {
	private AttachedRunsAttachedDialog runsDialog;
	/**
	 * 
	 */
	private static final long serialVersionUID = 8313252409686378398L;

	public AbstractJFrameWithAttachedHelpAndAttachedRunsDialog(int maxWidth) {
		super(maxWidth);

	}

	@Override
	public void showAttachedRunsDialog() {
		final AttachedRunsAttachedDialog chartTypesHelpDialog = getRunsAttachedDialog();
		// TODO
		chartTypesHelpDialog.loadResultFolders();
		chartTypesHelpDialog.forceVisible();
	}

	@Override
	public AttachedRunsAttachedDialog getRunsAttachedDialog() {
		if (runsDialog == null) {
			runsDialog = new AttachedRunsAttachedDialog(this, this.maxWidth);
		}
		return runsDialog;
	}

}
