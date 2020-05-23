package edu.scripps.yates.glycomsquant.gui.tables.runs;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JTable;
import javax.swing.SwingUtilities;

import org.apache.log4j.Logger;

import edu.scripps.yates.glycomsquant.gui.files.FileManager;
import edu.scripps.yates.glycomsquant.gui.tables.MyAbstractTable;
import edu.scripps.yates.glycomsquant.gui.tables.MyTableModel;

public class MyRunsTable extends MyAbstractTable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8869264818162494905L;
	private static Logger log = Logger.getLogger(MyRunsTable.class);

	public MyRunsTable() {
		super(new MyTableModel() {

			@Override
			protected Class<?> getMyColumnClass(int columnIndex) {
				return ColumnsRunTable.getColumns().get(columnIndex).getClass();
			}
		});
		// Set renderer for painting different background colors
		setDefaultRenderer(Object.class, new MyRunsTableCellRenderer());
		setDefaultRenderer(Boolean.class, new MyRunsTableBooleanCellRenderer());
		setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
	}

	@Override
	public List<String> getColumnNames() {
		return ColumnsRunTable.getColumnsString();
	}

	@Override
	public String getColumnDescription(String columnName) {
		final ColumnsRunTable column = getColumnByName(columnName);
		if (column != null) {
			return column.getDescription();
		}
		return null;
	}

	@Override
	public int getColumnDefaultWidth(String columnName) {
		final ColumnsRunTable column = getColumnByName(columnName);
		if (column != null) {
			return column.getDefaultWidth();
		}
		return 0;
	}

	public ColumnsRunTable getColumnByName(String columnName) {
		return ColumnsRunTable.getColumns().stream().filter(c -> c.getName().equals(columnName)).findAny().get();
	}

	public void loadRunsToTable() {

		clearData();

		addColumnsInTable(ColumnsRunTable.getColumnsString());
		final List<File> resultFolders = FileManager.getResultFolders();
		if (resultFolders != null) {
			for (int i = 0; i < resultFolders.size(); i++) {
				final File folder = resultFolders.get(i);
				final MyTableModel model = getModel();

				final List<Object> runInfoList = ColumnsRunTableUtil.getInstance(folder).getRunInfoList(folder,
						ColumnsRunTable.getColumns(), i + 1);
				model.addRow(runInfoList.toArray());
				log.info("Table now with " + model.getRowCount() + " rows");
			}

			log.info(resultFolders.size() + " folders added to attached window");
		}
		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				repaint();
				initializeSorter();
			}
		});
	}

	public List<String> getSelectedRunPaths() {
		final List<ColumnsRunTable> columns = ColumnsRunTable.getColumns();
		final int runPathIndex = columns.indexOf(ColumnsRunTable.RUN_PATH);
		final int[] selectedRows = getSelectedRows();
		final List<String> ret = new ArrayList<String>();
		for (final int row : selectedRows) {
			final int rowInModel = getRowSorter().convertRowIndexToModel(row);
			String runPath = getModel().getValueAt(rowInModel, runPathIndex).toString();
			runPath = runPath.replaceAll(ColumnsRunTableUtil.PREFIX, "");
			ret.add(runPath);
		}
		return ret;
	}
}
