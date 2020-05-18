package edu.scripps.yates.glycomsquant.gui.tables.comparison;

import javax.swing.table.DefaultTableModel;

class MyComparisonTableModel extends DefaultTableModel {
	/**
	 * 
	 */
	private static final long serialVersionUID = -2316754407630751253L;

	public MyComparisonTableModel() {
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
