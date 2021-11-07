package edu.scripps.yates.glycomsquant.gui.reference;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;

import edu.scripps.yates.glycomsquant.ProteinSequences;

public class ProxyAction extends AbstractAction {

	/**
	 * 
	 */
	private static final long serialVersionUID = 9036486097270179089L;
	private final Action action;

	public ProxyAction(Action action) {
		this.action = action;

	}

	@Override
	public void actionPerformed(ActionEvent e) {
		action.actionPerformed(e);
		ProteinSequences.REFERENCE = "custom";
		System.out.println("Custom protein sequence pasted!");
	}

}
