package edu.scripps.yates.glycomsquant.gui.tables.sites;

import java.util.List;

import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;

import org.apache.log4j.Logger;

import edu.scripps.yates.glycomsquant.GlycoSite;
import edu.scripps.yates.glycomsquant.gui.tables.MyAbstractTable;
import edu.scripps.yates.glycomsquant.gui.tables.MyTableModel;

public class MySitesTable extends MyAbstractTable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7966217677927412036L;
	private static Logger log = Logger.getLogger(MySitesTable.class);

	public MySitesTable() {
		super(new MyTableModel() {

			@Override
			protected Class<?> getMyColumnClass(int columnIndex) {
				return Object.class;
			}
		});
		// Set renderer for painting different background colors
		setDefaultRenderer(Object.class, new MySitesTableCellRenderer());
		setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
	}

	@Override
	public List<String> getColumnNames() {
		return ColumnsSitesTable.getColumnsString();
	}

	@Override
	public String getColumnDescription(String columnName) {
		final ColumnsSitesTable column = getColumnByName(columnName);
		if (column != null) {
			return column.getDescription();
		}
		return null;
	}

	@Override
	public int getColumnDefaultWidth(String columnName) {
		final ColumnsSitesTable column = getColumnByName(columnName);
		if (column != null) {
			return column.getDefaultWidth();
		}
		return 0;
	}

	public ColumnsSitesTable getColumnByName(String columnName) {
		return ColumnsSitesTable.getColumns().stream().filter(c -> c.getName().equals(columnName)).findAny().get();
	}

	public void loadResultTable(List<GlycoSite> glycoSites, boolean sumIntensitiesAcrossReplicates) {
		clearData();
		addColumnsInTable(ColumnsSitesTable.getColumnsString());

		if (glycoSites != null) {
			for (int i = 0; i < glycoSites.size(); i++) {
				final GlycoSite glycoSite = glycoSites.get(i);
				final MyTableModel model = getModel();

				final List<Object> glycoSiteInfoList = ColumnsSitesTableUtil.getInstance().getGlycoSiteInfoList(
						glycoSite, sumIntensitiesAcrossReplicates, ColumnsSitesTable.getColumns());
				model.addRow(glycoSiteInfoList.toArray());
				log.info("Table now with " + model.getRowCount() + " rows");
			}

			log.info(glycoSites.size() + " glycoSites added to attached window");
		}
		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				repaint();
				initializeSorter();
			}
		});
	}
}
