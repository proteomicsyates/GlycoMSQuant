package edu.scripps.yates.glycomsquant.gui.tables.grouped_peptides;

import java.awt.event.MouseEvent;

import javax.swing.JTable;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableModel;

import org.apache.log4j.Logger;

public class MyGroupedPeptidesTable extends JTable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 8076770198048519994L;
	private static Logger log = Logger.getLogger(MyGroupedPeptidesTable.class);

	public MyGroupedPeptidesTable() {
		super();
	}

	@Override
	protected JTableHeader createDefaultTableHeader() {
		return new JTableHeader(columnModel) {
			/**
			 * 
			 */
			private static final long serialVersionUID = 5284334431623105059L;

			@Override
			public String getToolTipText(MouseEvent e) {
				final java.awt.Point p = e.getPoint();
				final int index = columnModel.getColumnIndexAtX(p.x);
				// int realIndex =
				// columnModel.getColumn(index).getModelIndex();
				final String columnName = (String) columnModel.getColumn(index).getHeaderValue();
				final String tip = getToolTipTextForColumn(columnName);
				// log.info("Tip = " + tip);
				if (tip != null)
					return tip;
				else
					return super.getToolTipText(e);
			}
		};
	}

	private String getToolTipTextForColumn(String columnName) {
		final ColumnsGroupedPeptidesTable[] values = ColumnsGroupedPeptidesTable.values();
		for (final ColumnsGroupedPeptidesTable exportedColumns : values) {
			if (exportedColumns.getName().equals(columnName)) {
				return exportedColumns.getDescription();
			}
		}
		return null;
	}

	public void clearData() {
		log.info("Clearing data of the table");
		final TableModel model = getModel();
		if (model instanceof MyGroupedPeptidesTableModel) {
			((MyGroupedPeptidesTableModel) model).setRowCount(0);
			((MyGroupedPeptidesTableModel) model).setColumnCount(0);
		}

	}
}
