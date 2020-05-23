package edu.scripps.yates.glycomsquant.gui.attached_frame;

import edu.scripps.yates.glycomsquant.gui.tables.GroupedPeptideListDialog;

public interface HasPeptideListDialogAttached {

	public void showAttachedPeptideListDialog();

	public GroupedPeptideListDialog getPeptideListAttachedDialog();
}
