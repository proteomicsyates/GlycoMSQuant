package edu.scripps.yates.glycomsquant.gui.attached_frame;

import edu.scripps.yates.glycomsquant.gui.tables.grouped_peptides.AttachedPeptideListDialog;

public interface HasPeptideListDialogAttached {

	public void showAttachedPeptideListDialog();

	public AttachedPeptideListDialog getPeptideListAttachedDialog();
}
