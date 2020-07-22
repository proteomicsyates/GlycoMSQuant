package edu.scripps.yates.glycomsquant.gui.tables.comparison;

import java.awt.BorderLayout;
import java.awt.SystemColor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import javax.swing.DefaultListSelectionModel;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;

import org.apache.log4j.Logger;

import edu.scripps.yates.glycomsquant.PTMCode;
import edu.scripps.yates.glycomsquant.comparison.MyMannWhitneyTestResult;
import edu.scripps.yates.glycomsquant.comparison.RunComparisonTest;
import edu.scripps.yates.utilities.swing.SwingUtils;
import gnu.trove.map.hash.THashMap;

public class ComparisonTableDialog extends JFrame {
	/**
	 * 
	 */
	private static final long serialVersionUID = -1119185903108814297L;
	private static final Logger log = Logger.getLogger(ComparisonTableDialog.class);

	private final JScrollPane scrollPane;
	private final ScrollableComparisonTable table;
	private RunComparisonTest comparisons;
	private Map<String, MyMannWhitneyTestResult> comparisonsByKey;

	public ComparisonTableDialog() {
		SwingUtils.setComponentPreferredSizeRelativeToScreen(this, 0.5, 3.0 / 4);
		// super(parentFrame, ModalityType.MODELESS);
		getContentPane().setBackground(SystemColor.info);

		setFocusableWindowState(true);
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (UnsupportedLookAndFeelException | ClassNotFoundException | InstantiationException
				| IllegalAccessException e) {
			e.printStackTrace();
		}

		scrollPane = new JScrollPane();
		scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scrollPane.getVerticalScrollBar().setUnitIncrement(20);
		getContentPane().add(scrollPane, BorderLayout.CENTER);
		// contentPanel.setBackground(SystemColor.info);
		table = new ScrollableComparisonTable();
		scrollPane.setViewportView(table);

		pack();
		final java.awt.Dimension screenSize = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
		final java.awt.Dimension dialogSize = getSize();
		setLocation((screenSize.width - dialogSize.width) / 2, (screenSize.height - dialogSize.height) / 2);
	}

	protected List<MyMannWhitneyTestResult> getSelectedComparisons(int[] selectedRows) {
		final List<MyMannWhitneyTestResult> ret = new ArrayList<MyMannWhitneyTestResult>();
		final List<ColumnsComparisonTable> columns = ColumnsComparisonTable.getColumns();
		final int positionColumnIndex = columns.indexOf(ColumnsComparisonTable.POSITION);
		final int ptmColumnIndex = columns.indexOf(ColumnsComparisonTable.PTM);
		for (final int selectedRow : selectedRows) {
			final int row = table.getTable().getRowSorter().convertRowIndexToModel(selectedRow);
			final int position = Integer
					.valueOf(table.getTable().getModel().getValueAt(row, positionColumnIndex).toString());
			final PTMCode ptm = PTMCode.valueOf(table.getTable().getModel().getValueAt(row, ptmColumnIndex).toString());
			final String key = position + "-" + ptm;
			log.info(key + " selected");
			ret.add(getComparisonByKey().get(key));
		}
		return ret;
	}

	private Map<String, MyMannWhitneyTestResult> getComparisonByKey() {
		if (comparisonsByKey == null) {
			comparisonsByKey = new THashMap<String, MyMannWhitneyTestResult>();
			final List<ColumnsComparisonTable> columns = ColumnsComparisonTable.getColumns();
			final int positionColumnIndex = columns.indexOf(ColumnsComparisonTable.POSITION);
			final int ptmColumnIndex = columns.indexOf(ColumnsComparisonTable.PTM);
			for (final MyMannWhitneyTestResult test : getComparisons()) {
				final int position = Integer.valueOf(ColumnsComparisonTableUtil.getInstance()
						.getMyMannWhitneyTestResultInfoList(test, columns).get(positionColumnIndex).toString());
				final PTMCode ptm = PTMCode.valueOf(ColumnsComparisonTableUtil.getInstance()
						.getMyMannWhitneyTestResultInfoList(test, columns).get(ptmColumnIndex).toString());
				final String key = position + "-" + ptm;
				if (comparisonsByKey.containsKey(key)) {
					throw new IllegalArgumentException(key + " should be unique");
				}
				comparisonsByKey.put(key, test);
			}
		}
		return comparisonsByKey;
	}

	private List<MyMannWhitneyTestResult> getComparisons() {
		if (this.comparisons != null) {
			return this.comparisons.getComparisons();
		}
		return Collections.emptyList();
	}

	public MyComparisonTable getTable() {
		return this.table.getTable();
	}

	public void loadTable(RunComparisonTest comparisons) {
		setTitle("Comparison between " + comparisons.getResults1().getResultProperties().getName() + " and "
				+ comparisons.getResults2().getResultProperties().getName());
		this.comparisons = comparisons;
		table.getTable().clearData();

		addColumnsInTable(table.getTable(), ColumnsComparisonTable.getColumnsStringForTable());

		if (!getComparisons().isEmpty()) {
			// sort results by position and ptm
			final List<MyMannWhitneyTestResult> list = new ArrayList<MyMannWhitneyTestResult>();
			list.addAll(getComparisons());
			Collections.sort(list, new Comparator<MyMannWhitneyTestResult>() {

				@Override
				public int compare(MyMannWhitneyTestResult o1, MyMannWhitneyTestResult o2) {
					int ret = Integer.compare(o1.getPosition(), o2.getPosition());
					if (ret == 0) {
						ret = Integer.compare(o1.getPtm().ordinal(), o2.getPtm().ordinal());
					}
					return ret;
				}
			});
			for (final MyMannWhitneyTestResult test : list) {
				final MyComparisonTableModel model = (MyComparisonTableModel) table.getTable().getModel();
				final List<Object> infoList = ColumnsComparisonTableUtil.getInstance()
						.getMyMannWhitneyTestResultInfoList(test, ColumnsComparisonTable.getColumns());
				model.addRow(infoList.toArray());
//				log.info("Table now with " + model.getRowCount() + " rows");

			}
			final DefaultListSelectionModel selectionModel = new DefaultListSelectionModel();
			selectionModel.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			this.table.getTable().setSelectionModel(selectionModel);
			// add listener to selection
			selectionModel.addListSelectionListener(new ListSelectionListener() {

				@Override
				public void valueChanged(ListSelectionEvent e) {
					final List<MyMannWhitneyTestResult> selectedTests = new ArrayList<MyMannWhitneyTestResult>();
					final int[] selectedRows = table.getTable().getSelectedRows();
					for (final int selectedRow : selectedRows) {
						final int rowInModel = table.getTable().convertRowIndexToModel(selectedRow);
						final MyMannWhitneyTestResult test = list.get(rowInModel);
						selectedTests.add(test);
					}
// TODO
//					comparisons.showChartsForTest(selectedTests, -1);
				}
			});
			log.info(getComparisons().size() + " peptides added to attached window");
		}
		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				table.getTable().repaint();
				table.initializeSorter();
			}

		});

	}

	private void addColumnsInTable(MyComparisonTable table, List<String> columnsStringList) {
		final DefaultTableModel defaultModel = (DefaultTableModel) table.getModel();
		log.info("Adding colums " + columnsStringList.size() + " columns");
		if (columnsStringList != null) {

			for (final String columnName : columnsStringList) {
				defaultModel.addColumn(columnName);
			}
			log.info("Added " + table.getColumnCount() + " colums");
			for (int i = 0; i < table.getColumnCount(); i++) {
				final TableColumn column = table.getColumnModel().getColumn(i);
				final ColumnsComparisonTable[] columHeaders = ColumnsComparisonTable.values();
				boolean set = false;
				for (final ColumnsComparisonTable header : columHeaders) {
					if (column.getHeaderValue().equals(header.getName())) {
						column.setPreferredWidth(header.getDefaultWidth());
						set = true;
					}
//					column.setMaxWidth(header.getDefaultWidth());
//					column.setMinWidth(header.getDefaultWidth());
				}
				if (!set) {
					log.info("äsdf ");
				}
				column.setResizable(true);
			}
		}
	}

}
