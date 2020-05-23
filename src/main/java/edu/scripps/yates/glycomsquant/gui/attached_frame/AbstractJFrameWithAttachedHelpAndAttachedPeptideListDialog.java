package edu.scripps.yates.glycomsquant.gui.attached_frame;

import edu.scripps.yates.glycomsquant.gui.tables.GroupedPeptideListDialog;

public abstract class AbstractJFrameWithAttachedHelpAndAttachedPeptideListDialog
		extends AbstractJFrameWithAttachedHelpDialog implements HasPeptideListDialogAttached {
	private GroupedPeptideListDialog peptideListDialog;
	/**
	 * 
	 */
	private static final long serialVersionUID = 8313252409686378398L;

	public AbstractJFrameWithAttachedHelpAndAttachedPeptideListDialog(int maxWidth) {
		super(maxWidth);

	}

	@Override
	public void showAttachedPeptideListDialog() {
		final GroupedPeptideListDialog attacheDialod = getPeptideListAttachedDialog();

		attacheDialod.forceVisible();
	}

	@Override
	public void dispose() {
		// also dispose the attached dialogs
		getPeptideListAttachedDialog().dispose();

		super.dispose();
	}

	@Override
	public GroupedPeptideListDialog getPeptideListAttachedDialog() {
		if (peptideListDialog == null) {
			peptideListDialog = new GroupedPeptideListDialog(this, this.maxWidth);
		}
		return peptideListDialog;
	}

}
