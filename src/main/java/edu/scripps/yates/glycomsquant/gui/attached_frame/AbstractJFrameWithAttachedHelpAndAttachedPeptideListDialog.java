package edu.scripps.yates.glycomsquant.gui.attached_frame;

import edu.scripps.yates.glycomsquant.gui.tables.grouped_peptides.AttachedPeptideListDialog;

public abstract class AbstractJFrameWithAttachedHelpAndAttachedPeptideListDialog
		extends AbstractJFrameWithAttachedHelpDialog implements HasPeptideListDialogAttached {
	private AttachedPeptideListDialog peptideListDialog;
	/**
	 * 
	 */
	private static final long serialVersionUID = 8313252409686378398L;

	public AbstractJFrameWithAttachedHelpAndAttachedPeptideListDialog(int maxWidth) {
		super(maxWidth);

	}

	@Override
	public void showAttachedPeptideListDialog() {
		final AttachedPeptideListDialog attacheDialod = getPeptideListAttachedDialog();

		attacheDialod.forceVisible();
	}

	@Override
	public void dispose() {
		// also dispose the attached dialogs
		getPeptideListAttachedDialog().dispose();

		super.dispose();
	}

	@Override
	public AttachedPeptideListDialog getPeptideListAttachedDialog() {
		if (peptideListDialog == null) {
			peptideListDialog = new AttachedPeptideListDialog(this, this.maxWidth);
		}
		return peptideListDialog;
	}

}
