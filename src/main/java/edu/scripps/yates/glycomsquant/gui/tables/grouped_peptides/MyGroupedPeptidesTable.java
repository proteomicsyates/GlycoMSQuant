package edu.scripps.yates.glycomsquant.gui.tables.grouped_peptides;

import java.awt.event.MouseEvent;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import javax.swing.DefaultListSelectionModel;
import javax.swing.JTable;
import javax.swing.RowFilter;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import org.apache.log4j.Logger;

import edu.scripps.yates.census.read.model.interfaces.QuantifiedPeptideInterface;
import edu.scripps.yates.glycomsquant.GlycoSite;
import edu.scripps.yates.glycomsquant.GroupedQuantifiedPeptide;
import edu.scripps.yates.glycomsquant.gui.ProteinSequenceDialog;
import edu.scripps.yates.glycomsquant.gui.tables.individual_peptides.ScrollablePeptidesTable;
import edu.scripps.yates.glycomsquant.util.GlycoPTMAnalyzerUtil;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.THashMap;

public class MyGroupedPeptidesTable extends JTable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 8076770198048519994L;
	private static Logger log = Logger.getLogger(MyGroupedPeptidesTable.class);
	private Map<String, GroupedQuantifiedPeptide> peptidesByPeptideKey;
	private final ProteinSequenceDialog proteinSequenceDialog;
	private TableRowSorter<TableModel> sorter = null;
	private Comparator comp;
	private List<GroupedQuantifiedPeptide> groupedPeptideList;

	public MyGroupedPeptidesTable(ProteinSequenceDialog proteinSequenceDialog) {
		super();
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
		peptidesByPeptideKey = null;
		final TableModel model = getModel();
		if (model instanceof MyGroupedPeptidesTableModel) {
			// get listeners
			final ListSelectionListener[] listSelectionListeners = ((DefaultListSelectionModel) getSelectionModel())
					.getListSelectionListeners();
			// remove listeners
			for (final ListSelectionListener listSelectionListener : listSelectionListeners) {
				getSelectionModel().removeListSelectionListener(listSelectionListener);
			}
			((MyGroupedPeptidesTableModel) model).setRowCount(0);
			((MyGroupedPeptidesTableModel) model).setColumnCount(0);
			// add listeners
			for (final ListSelectionListener listSelectionListener : listSelectionListeners) {
				getSelectionModel().addListSelectionListener(listSelectionListener);
			}
		}
	}

	public void setSelectionListenerIndividualPeptidesTable(ScrollablePeptidesTable individualPeptidesTable) {
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

		addColumnsInTable(this, ColumnsGroupedPeptidesTable.getColumnsStringForTable());

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
				final MyGroupedPeptidesTableModel model = (MyGroupedPeptidesTableModel) getModel();
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

	public void initializeSorter() {
		sorter = new TableRowSorter<TableModel>(getModel());
		final int columnCount = getModel().getColumnCount();
		for (int i = 0; i < columnCount; i++) {
			sorter.setComparator(i, getMyComparator2());
		}
		setRowSorter(sorter);
	}

	private void addColumnsInTable(MyGroupedPeptidesTable table, List<String> columnsStringList) {
		final DefaultTableModel defaultModel = (DefaultTableModel) table.getModel();
		log.info("Adding colums " + columnsStringList.size() + " columns");
		if (columnsStringList != null) {

			for (final String columnName : columnsStringList) {
				defaultModel.addColumn(columnName);
			}
			log.info("Added " + table.getColumnCount() + " colums");
			for (int i = 0; i < table.getColumnCount(); i++) {
				final TableColumn column = table.getColumnModel().getColumn(i);
				final ColumnsGroupedPeptidesTable[] columHeaders = ColumnsGroupedPeptidesTable.values();
				boolean set = false;
				for (final ColumnsGroupedPeptidesTable header : columHeaders) {
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

	private List<GlycoSite> getGlycoSites() {
		return this.proteinSequenceDialog.getGlycoSites();
	}

	private List<GroupedQuantifiedPeptide> getPeptides() {
		return this.groupedPeptideList;
	}

	private String getProteinSequence() {
		return this.proteinSequenceDialog.getProteinSequence();
	}

	private Comparator<?> getMyComparator2() {
		if (comp == null)
			comp = new Comparator() {

				@Override
				public int compare(Object obj1, Object obj2) {
					try {
						final Number n1 = NumberFormat.getInstance().parse(obj1.toString());
						final Number n2 = NumberFormat.getInstance().parse(obj2.toString());
						final Double d1 = getDouble(obj1);
						final Double d2 = getDouble(obj2);
						return d1.compareTo(d2);
					} catch (final java.text.ParseException e1) {

						if (obj1 instanceof String && obj2 instanceof String) {
							final String n1 = (String) obj1;
							final String n2 = (String) obj2;

							final String n3 = getHighesNumberIfAreCommaSeparated(n1);
							final String n4 = getHighesNumberIfAreCommaSeparated(n2);
							if (n3 != null && n4 != null)
								return compare(n3, n4);
							return n1.compareTo(n2);

						} else if (obj1 instanceof String && obj2 instanceof Double) {
							final String n1 = (String) obj1;
							final String n2 = String.valueOf(obj2);
							return n1.compareTo(n2);
						} else if (obj2 instanceof String && obj1 instanceof Double) {
							final String n2 = (String) obj2;
							final String n1 = String.valueOf(obj1);
							return n1.compareTo(n2);
						} else {
							final String n1 = obj1.toString();
							final String n2 = obj2.toString();
							return n1.compareTo(n2);
						}

					}

				}

				private String getHighesNumberIfAreCommaSeparated(String string) {
					if (string.contains(";")) {
						final String[] split = string.split(";");
						try {
							final TIntArrayList ints = new TIntArrayList();
							for (final String string2 : split) {
								ints.add(Integer.valueOf(string2));
							}
							return String.valueOf(ints.max());
						} catch (final NumberFormatException e) {
							try {
								final String[] split2 = string.split(";");
								final List<Double> doubles = new ArrayList<Double>();
								for (final String string2 : split2) {
									doubles.add(getDouble(string2));
								}
								return String.valueOf(getMaxFromDoubles(doubles));
							} catch (final NumberFormatException e2) {
							} catch (final ParseException e3) {

							}
						}
					}

					return null;
				}

				private Double getDouble(Object value) throws ParseException {
					final Number n1 = NumberFormat.getInstance().parse(value.toString());
					return n1.doubleValue();
				}

				private String getMaxFromDoubles(List<Double> doubles) {
					double max = Double.MIN_VALUE;
					for (final Double dou : doubles) {
						if (max < dou)
							max = dou;
					}
					return String.valueOf(max);
				}

				private String getMaxFromIntegers(List<Integer> ints) {
					int max = Integer.MIN_VALUE;
					for (final Integer integer : ints) {
						if (max < integer)
							max = integer;
					}
					return String.valueOf(max);
				}
			};
		return comp;
	}

	public void setFilter(String columnName, String regexp) {

		try {

			final RowFilter<Object, Object> paginatorFilter = getColumnFilter(columnName, regexp);
			// if (paginatorFilter != null)
			// filters.add(paginatorFilter);

			if (sorter != null) {
				sorter.setRowFilter(paginatorFilter);
				setRowSorter(sorter);
			}
		} catch (final java.util.regex.PatternSyntaxException e) {
			return;
		}
	}

	private RowFilter<Object, Object> getColumnFilter(final String columnName, final String regexp) {
		if (regexp != null && !"".equals(regexp)) {
			final int columnIndex = getColumnIndex(columnName);
			if (columnIndex >= 0)
				return RowFilter.regexFilter(regexp, columnIndex);
		}
		return null;
	}

	public int getColumnIndex(String columnName) {
		for (int i = 0; i < getColumnCount(); i++) {
			if (getColumnName(i).equals(columnName))
				return i;
		}
		return -1;
	}

}
