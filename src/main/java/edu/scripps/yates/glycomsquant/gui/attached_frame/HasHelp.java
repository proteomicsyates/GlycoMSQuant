package edu.scripps.yates.glycomsquant.gui.attached_frame;

import java.util.List;

public interface HasHelp {
	public List<String> getHelpMessages();

	public void showAttachedHelpDialog();

	public AttachedHelpDialog getHelpDialog();
}
