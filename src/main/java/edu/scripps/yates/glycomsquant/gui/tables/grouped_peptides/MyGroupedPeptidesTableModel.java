package edu.scripps.yates.glycomsquant.gui.tables.grouped_peptides;

import javax.swing.table.DefaultTableModel;

class MyGroupedPeptidesTableModel extends DefaultTableModel {
	/**
	 * 
	 */
	private static final long serialVersionUID = -2316754407630751253L;

	public MyGroupedPeptidesTableModel() {
	}

	@Override
	public boolean isCellEditable(int row, int column) {
		return false;
	}

//	@Override
//	public Class<?> getColumnClass(int columnIndex) {
//		final ColumnsResultsTable column = ColumnsResultsTable.getColumns(calculateProportionsByPeptidesFirst)
//				.get(columnIndex);
//		return column.getColumnClass();
//
//	}

}
