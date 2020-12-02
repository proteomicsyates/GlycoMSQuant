package edu.scripps.yates.glycomsquant.gui.tables;

import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.swing.JTable;
import javax.swing.RowFilter;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import org.apache.log4j.Logger;

import gnu.trove.list.array.TIntArrayList;

public abstract class MyAbstractTable extends JTable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -4577194336119822155L;
	/**
	 * 
	 */
	private static Logger log = Logger.getLogger(MyAbstractTable.class);
	private Comparator<Object> comp;
	private TableRowSorter<MyTableModel> sorter = null;

	public MyAbstractTable(MyTableModel model) {
		super(model);
	}

	@Override
	public void setModel(TableModel dataModel) {
		super.setModel(dataModel);
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
				if (tip != null) {
					return "<html>" + tip + "</html>";
				} else {
					return super.getToolTipText(e);
				}
			}
		};
	}

	private String getToolTipTextForColumn(String columnName) {

		for (final String name : getColumnNames()) {
			if (name.equals(columnName)) {
				return getColumnDescription(name);
			}
		}
		return null;
	}

	/**
	 * final ColumnsGroupedPeptidesTable[] values =
	 * ColumnsGroupedPeptidesTable.values(); for (final ColumnsGroupedPeptidesTable
	 * exportedColumns : values) { if (exportedColumns.getName()
	 * 
	 * @return
	 */
	public abstract List<String> getColumnNames();

	public abstract String getColumnDescription(String columnName);

	public abstract int getColumnDefaultWidth(String columnName);

	@Override
	public MyTableModel getModel() {
		return (MyTableModel) super.getModel();
	}

	public void clearData() {
		log.info("Clearing data of the table");
		final MyTableModel model = getModel();

		model.setRowCount(0);
		model.setColumnCount(0);

	}

	public final void initializeSorter() {
		sorter = new TableRowSorter<MyTableModel>(getModel());
		final int columnCount = getModel().getColumnCount();
		for (int i = 0; i < columnCount; i++) {
			sorter.setComparator(i, getMyComparator2());
		}
		setRowSorter(sorter);
	}

	public final void addColumnsInTable(List<String> columnsStringList) {
		final MyTableModel model = getModel();
		log.info("Adding colums " + columnsStringList.size() + " columns");
		if (columnsStringList != null) {

			for (final String columnName : columnsStringList) {
				model.addColumn(columnName);
			}
			log.info("Added " + getColumnCount() + " colums");
			for (int i = 0; i < getColumnCount(); i++) {
				final TableColumn column = getColumnModel().getColumn(i);
				final List<String> columHeaders = getColumnNames();
				boolean set = false;
				for (final String header : columHeaders) {
					if (column.getHeaderValue().equals(header)) {
						column.setPreferredWidth(getColumnDefaultWidth(header));
						set = true;
					}
//					column.setMaxWidth(header.getDefaultWidth());
//					column.setMinWidth(header.getDefaultWidth());
				}

				column.setResizable(true);
			}
		}
	}

	private Comparator<?> getMyComparator2() {
		if (comp == null)
			comp = new Comparator<Object>() {

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

	public void saveToFile(File outputFile) throws IOException {
		final FileWriter fw = new FileWriter(outputFile);
		final List<String> headers = getColumnNames();
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
