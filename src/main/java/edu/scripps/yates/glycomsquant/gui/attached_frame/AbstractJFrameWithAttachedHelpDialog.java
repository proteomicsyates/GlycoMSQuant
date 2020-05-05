package edu.scripps.yates.glycomsquant.gui.attached_frame;

import java.util.List;

import javax.swing.JFrame;

public abstract class AbstractJFrameWithAttachedHelpDialog extends JFrame implements HasHelp {

	@Override
	public List<String> getHelpMessages() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void dispose() {
		// also dispose the attached dialogs
		getHelpDialog().dispose();

		super.dispose();
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = -3356292958061016878L;
	private AttachedHelpDialog help;
	protected final int maxWidth;

	public AbstractJFrameWithAttachedHelpDialog(int maxWidth) {
		this.maxWidth = maxWidth;
	}

	@Override
	public void showAttachedHelpDialog() {
		getHelpDialog().forceVisible();

	}

	@Override
	public AttachedHelpDialog getHelpDialog() {
		if (help == null) {
			help = new AttachedHelpDialog(this, this.maxWidth);
		}
		return help;
	}
}
