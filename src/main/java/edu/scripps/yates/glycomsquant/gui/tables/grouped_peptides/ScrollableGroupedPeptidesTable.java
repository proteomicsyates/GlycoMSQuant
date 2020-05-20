package edu.scripps.yates.glycomsquant.gui.tables.grouped_peptides;

import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;

import org.apache.log4j.Logger;

import edu.scripps.yates.glycomsquant.gui.ProteinSequenceDialog;

public class ScrollableGroupedPeptidesTable extends JScrollPane {
	/**
	 * 
	 */
	private static final long serialVersionUID = 725114162692158140L;

	private static Logger log = Logger.getLogger(ScrollableGroupedPeptidesTable.class);

	private final MyGroupedPeptidesTable table;

	public ScrollableGroupedPeptidesTable(ProteinSequenceDialog proteinSequenceDialog) {
		table = new MyGroupedPeptidesTable(proteinSequenceDialog);
		table.setModel(new MyGroupedPeptidesTableModel());

		// Set renderer for painting different background colors
		table.setDefaultRenderer(Object.class, new MyGroupedPeptidesTableCellRenderer());
		initializeUI();
	}

	private void initializeUI() {
//		setLayout(new BorderLayout());

		//
		// Turn off JTable's auto resize so that JScrollpane
		// will show a horizontal scroll bar.
		//
		table.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
		table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

		this.setViewportView(table);

		super.repaint();
	}

	public MyGroupedPeptidesTable getTable() {
		return table;
	}

}
