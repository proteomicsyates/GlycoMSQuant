package edu.scripps.yates.glycomsquant.gui.tables.comparison;

import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import javax.swing.JTable;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableModel;

import org.apache.log4j.Logger;

import edu.scripps.yates.glycomsquant.gui.tables.SavesToFile;

public class MyComparisonTable extends JTable implements SavesToFile {
	/**
	 * 
	 */
	private static final long serialVersionUID = 8076770198048519994L;
	private static Logger log = Logger.getLogger(MyComparisonTable.class);

	public MyComparisonTable() {
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
		final ColumnsComparisonTable[] values = ColumnsComparisonTable.values();
		for (final ColumnsComparisonTable exportedColumns : values) {
			if (exportedColumns.getName().equals(columnName)) {
				return exportedColumns.getDescription();
			}
		}
		return null;
	}

	public void clearData() {
		log.info("Clearing data of the table");
		final TableModel model = getModel();
		if (model instanceof MyComparisonTableModel) {
			((MyComparisonTableModel) model).setRowCount(0);
			((MyComparisonTableModel) model).setColumnCount(0);
		}

	}

	@Override
	public void saveToFile(File outputFile) throws IOException {
		final FileWriter fw = new FileWriter(outputFile);
		final List<String> headers = ColumnsComparisonTable.getColumnsString();
		for (final String header : headers) {
			fw.write(header + "\t");
		}
		fw.write("\n");
		final int rowCount = getModel().getRowCount();
		final int columnCount = getModel().getColumnCount();
		for (int row = 0; row < rowCount; row++) {
			for (int col = 0; col < columnCount; col++) {
				final Object valueAt = getModel().getValueAt(row, col);
				String string = null;
				if (valueAt instanceof Double) {
					if (Double.isNaN((double) valueAt)) {
						string = "";
					} else {
						string = valueAt.toString();
					}
				} else {
					string = valueAt.toString();
				}
				fw.write(string + "\t");
			}
			fw.write("\n");
		}
		fw.close();
		log.info("Table saved at file: '" + outputFile.getAbsolutePath() + "'");
	}
}
