package edu.scripps.yates.glycomsquant.gui.tables.individual_peptides;

import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.table.JTableHeader;

import org.apache.log4j.Logger;

import edu.scripps.yates.census.read.model.interfaces.QuantifiedPeptideInterface;
import edu.scripps.yates.glycomsquant.GlycoSite;
import edu.scripps.yates.glycomsquant.gui.tables.MyAbstractTable;
import edu.scripps.yates.glycomsquant.gui.tables.MyTableModel;

public class MyPeptidesTable extends MyAbstractTable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2961310159741816136L;
	private static Logger log = Logger.getLogger(MyPeptidesTable.class);
	private final boolean extended;

	public MyPeptidesTable(boolean extended) {
		super(new MyTableModel() {

			@Override
			protected Class<?> getMyColumnClass(int columnIndex) {
				return ColumnsPeptidesTable.getColumns(extended).get(columnIndex).getColumnClass();
			}
		});

		// Set renderer for painting different background colors
		setDefaultRenderer(Object.class, new MyPeptidesTableCellRenderer(extended));

		//
		// Turn off JTable's auto resize so that JScrollpane
		// will show a horizontal scroll bar.
		//
		setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
		setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		this.extended = extended;
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
		final ColumnsPeptidesTable[] values = ColumnsPeptidesTable.values();
		for (final ColumnsPeptidesTable exportedColumns : values) {
			if (exportedColumns.getName().equals(columnName)) {
				return exportedColumns.getDescription();
			}
		}
		return null;
	}

	public void loadResultTable(List<QuantifiedPeptideInterface> peptides) {
		loadResultTable(peptides, null, null);
	}

	public void loadResultTable(List<QuantifiedPeptideInterface> peptides, List<GlycoSite> glycoSites,
			String proteinSequence) {
		clearData();
		addColumnsInTable(ColumnsPeptidesTable.getColumnsString(extended));
		log.info("Loading result peptide table with " + peptides.size() + " peptides");
		final MyTableModel model = getModel();

		for (int i = 0; i < peptides.size(); i++) {
			final QuantifiedPeptideInterface peptide = peptides.get(i);

			final List<Object> glycoSiteInfoList = ColumnsPeptidesTableUtil.getInstance().getPeptideInfoList(peptide,
					glycoSites, ColumnsPeptidesTable.getColumns(extended), proteinSequence);
			model.addRow(glycoSiteInfoList.toArray());
//			log.info("Table now with " + model.getRowCount() + " rows");

		}

		log.info(peptides.size() + " peptides added to attached window");

		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				repaint();
				initializeSorter();
			}
		});
	}

	@Override
	public List<String> getColumnNames() {
		return ColumnsPeptidesTable.getColumnsString(extended);
	}

	@Override
	public String getColumnDescription(String columnName) {
		final ColumnsPeptidesTable column = getColumnByName(columnName);
		if (column != null) {
			return column.getDescription();
		}
		return null;
	}

	@Override
	public int getColumnDefaultWidth(String columnName) {
		final ColumnsPeptidesTable column = getColumnByName(columnName);
		if (column != null) {
			return column.getDefaultWidth();
		}
		return 0;
	}

	public ColumnsPeptidesTable getColumnByName(String columnName) {
		return ColumnsPeptidesTable.getColumns(extended).stream().filter(c -> c.getName().equals(columnName)).findAny()
				.get();
	}
}
