package edu.scripps.yates.glycomsquant.gui.tables.grouped_peptides;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import javax.swing.DefaultListSelectionModel;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.apache.log4j.Logger;

import edu.scripps.yates.census.read.model.interfaces.QuantifiedPeptideInterface;
import edu.scripps.yates.glycomsquant.GlycoSite;
import edu.scripps.yates.glycomsquant.GroupedQuantifiedPeptide;
import edu.scripps.yates.glycomsquant.gui.ProteinSequenceDialog;
import edu.scripps.yates.glycomsquant.gui.tables.MyAbstractTable;
import edu.scripps.yates.glycomsquant.gui.tables.MyTableModel;
import edu.scripps.yates.glycomsquant.gui.tables.individual_peptides.MyPeptidesTable;
import edu.scripps.yates.glycomsquant.gui.tables.scrollables.ScrollableTable;
import edu.scripps.yates.glycomsquant.util.GlycoPTMAnalyzerUtil;
import gnu.trove.map.hash.THashMap;

public class MyGroupedPeptidesTable extends MyAbstractTable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 8076770198048519994L;
	private static Logger log = Logger.getLogger(MyGroupedPeptidesTable.class);
	private Map<String, GroupedQuantifiedPeptide> peptidesByPeptideKey;
	private final ProteinSequenceDialog proteinSequenceDialog;
	private List<GroupedQuantifiedPeptide> groupedPeptideList;

	public MyGroupedPeptidesTable(ProteinSequenceDialog proteinSequenceDialog) {
		super(new MyTableModel() {

			@Override
			protected Class<?> getMyColumnClass(int columnIndex) {
				return Object.class;
			}
		});
		this.proteinSequenceDialog = proteinSequenceDialog;
		// add listener to selection
		selectionModel.addListSelectionListener(new ListSelectionListener() {

			@Override
			public void valueChanged(ListSelectionEvent e) {
				final List<GroupedQuantifiedPeptide> selectedPeptides = new ArrayList<GroupedQuantifiedPeptide>();
				final int[] selectedRows = getSelectedRows();
				for (final int selectedRow : selectedRows) {
					final int rowInModel = convertRowIndexToModel(selectedRow);

					final GroupedQuantifiedPeptide peptide = groupedPeptideList.get(rowInModel);
					selectedPeptides.add(peptide);
				}
				proteinSequenceDialog.highlightPeptidesOnSequence(selectedPeptides, null);
				proteinSequenceDialog.showChartsFromPeptides(selectedPeptides, -1);
			}
		});
		// Set renderer for painting different background colors
		setDefaultRenderer(Object.class, new MyGroupedPeptidesTableCellRenderer());
		setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
		setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
	}

	@Override
	public void clearData() {
		log.info("Clearing data of the table");
		peptidesByPeptideKey = null;
		// get listeners
		final ListSelectionListener[] listSelectionListeners = ((DefaultListSelectionModel) getSelectionModel())
				.getListSelectionListeners();
		// remove listeners
		for (final ListSelectionListener listSelectionListener : listSelectionListeners) {
			getSelectionModel().removeListSelectionListener(listSelectionListener);
		}
		super.clearData();
		// add listeners
		for (final ListSelectionListener listSelectionListener : listSelectionListeners) {
			getSelectionModel().addListSelectionListener(listSelectionListener);
		}

	}

	public void setSelectionListenerIndividualPeptidesTable(ScrollableTable<MyPeptidesTable> individualPeptidesTable) {
		selectionModel.addListSelectionListener(new ListSelectionListener() {

			@Override
			public void valueChanged(ListSelectionEvent e) {
				final int[] selectedRows = getSelectedRows();
				final List<GroupedQuantifiedPeptide> selectedPeptides = getSelectedPeptides(selectedRows);
				final List<QuantifiedPeptideInterface> individualPeptides = GlycoPTMAnalyzerUtil
						.getPeptidesFromGroupedPeptides(selectedPeptides);
				individualPeptidesTable.getTable().loadResultTable(individualPeptides);
			}
		});
	}

	private List<GroupedQuantifiedPeptide> getSelectedPeptides(int[] selectedRows) {
		final List<GroupedQuantifiedPeptide> ret = new ArrayList<GroupedQuantifiedPeptide>();
		final List<ColumnsGroupedPeptidesTable> columns = ColumnsGroupedPeptidesTable.getColumns();
		final int peptideKeyIndex = columns.indexOf(ColumnsGroupedPeptidesTable.SEQUENCE);
		for (final int selectedRow : selectedRows) {
			final int row = getRowSorter().convertRowIndexToModel(selectedRow);
			final String peptideKey = getModel().getValueAt(row, peptideKeyIndex).toString();
			log.info(peptideKey + " selected");
			if (getPeptidesBySequenceAndCharge().containsKey(peptideKey)) {
				ret.add(getPeptidesBySequenceAndCharge().get(peptideKey));
			}
		}
		return ret;
	}

	private Map<String, GroupedQuantifiedPeptide> getPeptidesBySequenceAndCharge() {
		if (peptidesByPeptideKey == null) {
			peptidesByPeptideKey = new THashMap<String, GroupedQuantifiedPeptide>();
			final List<ColumnsGroupedPeptidesTable> columns = ColumnsGroupedPeptidesTable.getColumns();
			final int fullSequenceIndex = columns.indexOf(ColumnsGroupedPeptidesTable.SEQUENCE);
			for (final GroupedQuantifiedPeptide peptide : getPeptides()) {
				final String peptideKey = ColumnsGroupedPeptidesTableUtil.getInstance()
						.getPeptideInfoList(peptide, getGlycoSites(), columns, getProteinSequence())
						.get(fullSequenceIndex).toString();

				if (peptidesByPeptideKey.containsKey(peptideKey)) {
					throw new IllegalArgumentException(peptideKey + " should be unique");
				}
				peptidesByPeptideKey.put(peptideKey, peptide);
			}
		}
		return peptidesByPeptideKey;
	}

	public void loadTable(Collection<GroupedQuantifiedPeptide> peptidesToLoad, Integer positionInProtein) {
		clearData();

		addColumnsInTable(ColumnsGroupedPeptidesTable.getColumnsString());

		if (getGlycoSites() != null && peptidesToLoad != null) {
			// sort peptides by key with sequence and charge
			groupedPeptideList = new ArrayList<GroupedQuantifiedPeptide>();
			groupedPeptideList.addAll(peptidesToLoad);

			Collections.sort(groupedPeptideList, new Comparator<GroupedQuantifiedPeptide>() {

				@Override
				public int compare(GroupedQuantifiedPeptide o1, GroupedQuantifiedPeptide o2) {
					return o1.getKey(false).compareTo(o2.getKey(false));
				}
			});
			for (final GroupedQuantifiedPeptide peptide : groupedPeptideList) {
				final MyTableModel model = getModel();
				final List<Object> glycoSiteInfoList = ColumnsGroupedPeptidesTableUtil.getInstance().getPeptideInfoList(
						peptide, getGlycoSites(), ColumnsGroupedPeptidesTable.getColumns(), getProteinSequence());
				model.addRow(glycoSiteInfoList.toArray());
//					log.info("Table now with " + model.getRowCount() + " rows");

			}

			log.info(getPeptides().size() + " peptides added to attached window");
		}
		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				repaint();
				initializeSorter();
			}
		});
		this.proteinSequenceDialog.highlightPeptidesOnSequence(peptidesToLoad, positionInProtein);

	}

	private List<GlycoSite> getGlycoSites() {
		return this.proteinSequenceDialog.getGlycoSites();
	}

	private List<GroupedQuantifiedPeptide> getPeptides() {
		return this.groupedPeptideList;
	}

	private String getProteinSequence() {
		return this.proteinSequenceDialog.getProteinSequence();
	}

	@Override
	public List<String> getColumnNames() {
		return ColumnsGroupedPeptidesTable.getColumnsString();
	}

	@Override
	public String getColumnDescription(String columnName) {
		final ColumnsGroupedPeptidesTable column = getColumnByName(columnName);
		if (column != null) {
			return column.getDescription();
		}
		return null;
	}

	public ColumnsGroupedPeptidesTable getColumnByName(String columnName) {
		return ColumnsGroupedPeptidesTable.getColumns().stream().filter(c -> c.getName().equals(columnName)).findAny()
				.get();
	}

	@Override
	public int getColumnDefaultWidth(String columnName) {
		final ColumnsGroupedPeptidesTable column = getColumnByName(columnName);
		if (column != null) {
			return column.getDefaultWidth();
		}
		return 0;
	}

}
