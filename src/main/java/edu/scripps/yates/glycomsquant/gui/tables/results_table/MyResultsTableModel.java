package edu.scripps.yates.glycomsquant.gui.tables.results_table;

import javax.swing.table.DefaultTableModel;

class MyResultsTableModel extends DefaultTableModel {
	private final boolean calculateProportionsByPeptidesFirst;
	/**
	 * 
	 */
	private static final long serialVersionUID = -2316754407630751253L;

	public MyResultsTableModel(boolean calculateProportionsByPeptidesFirst) {
		this.calculateProportionsByPeptidesFirst = calculateProportionsByPeptidesFirst;
	}

	@Override
	public boolean isCellEditable(int row, int column) {
		return false;
	}

	@Override
	public Class<?> getColumnClass(int columnIndex) {
		final ColumnsResultsTable column = ColumnsResultsTable.getColumns(calculateProportionsByPeptidesFirst)
				.get(columnIndex);
		return column.getColumnClass();

	}

}
