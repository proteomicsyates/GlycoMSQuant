package edu.scripps.yates.glycomsquant.gui.tables.individual_peptides;

import javax.swing.JScrollPane;

import org.apache.log4j.Logger;

public class ScrollablePeptidesTable extends JScrollPane {
	/**
	 * 
	 */
	private static final long serialVersionUID = 725114162692158140L;

	private static Logger log = Logger.getLogger(ScrollablePeptidesTable.class);

	private final MyPeptidesTable table;

	public ScrollablePeptidesTable(boolean extended) {
		table = new MyPeptidesTable(extended);

		this.setViewportView(table);

		super.repaint();
	}

	public MyPeptidesTable getTable() {
		return table;
	}

}
