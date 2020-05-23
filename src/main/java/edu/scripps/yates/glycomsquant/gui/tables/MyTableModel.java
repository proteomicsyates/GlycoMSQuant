package edu.scripps.yates.glycomsquant.gui.tables;

import javax.swing.table.DefaultTableModel;

public abstract class MyTableModel extends DefaultTableModel {
	/**
	 * 
	 */
	private static final long serialVersionUID = -2316754407630751253L;
	private final boolean cellEditable;

	public MyTableModel() {
		this(true);
	}

	public MyTableModel(boolean cellEditable) {
		this.cellEditable = cellEditable;
	}

	@Override
	public boolean isCellEditable(int row, int column) {
		final Class<?> columnClass = getColumnClass(column);
		if (columnClass == Boolean.class) {
			return false;
		}
		return cellEditable;
	}

	@Override
	public Class<?> getColumnClass(int columnIndex) {
		return getMyColumnClass(columnIndex);

	}

	protected abstract Class<?> getMyColumnClass(int columnIndex);

//	@Override
//	public Class<?> getColumnClass(int columnIndex) {
//		final ColumnsResultsTable column = ColumnsResultsTable.getColumns(calculateProportionsByPeptidesFirst)
//				.get(columnIndex);
//		return column.getColumnClass();
//
//	}

}
