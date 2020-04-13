package edu.scripps.yates.glycomsquant.gui.tables.run_table;

import javax.swing.table.DefaultTableModel;

class MyRunsTableModel extends DefaultTableModel {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2316754407630751253L;

	@Override
	public boolean isCellEditable(int row, int column) {
		return false;
	}

	@Override
	public Class<?> getColumnClass(int columnIndex) {
		final ColumnsRunTable column = ColumnsRunTable.getColumns().get(columnIndex);
		return column.getColumnClass();

	}

}
