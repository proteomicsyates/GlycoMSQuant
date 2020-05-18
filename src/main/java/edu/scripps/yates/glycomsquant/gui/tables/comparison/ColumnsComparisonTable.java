package edu.scripps.yates.glycomsquant.gui.tables.comparison;

import java.util.ArrayList;
import java.util.List;

enum ColumnsComparisonTable {
	POSITION("Position", 20, "Position in the protein sequence", Integer.class), //
	PTM("PTM", 50, "PTM type", String.class), PROPORTION_1("% 1", 40, "Averaged proportion in 1", Double.class),
	PROPORTION_2("% 2", 40, "Averaged proportion in 2", Double.class),
	SEM_1("SEM 1", 40, "Squared Error of Mean of the proportions in 1", Double.class),
	SEM_2("SEM 2", 40, "Squared Error of Mean of the proportions in 2", Double.class),
	P_VALUE("p-value", 60, "Mann Whitney Test result p-value", Double.class), //
	ADJUSTED_P_VALUE("Adj. p-value", 60, "Adjusted p-value using BH correction", Double.class), //

	SIGNIFICANCY("Sign.", 20, "Significancy. *->p<0.05, **->p<0.01, ***->p<0.001", Double.class); //

	private final String name;
	private final int defaultWidth;
	private final String description;
	private final Class<?> clazz;

	ColumnsComparisonTable(String name, int defaultWidth, String description, Class<?> clazz) {
		this.name = name;
		this.defaultWidth = defaultWidth;
		this.description = description;
		this.clazz = clazz;
	}

	public String getName() {
		return name;
	}

	public int getDefaultWidth() {
		return defaultWidth;
	}

	public String getDescription() {
		return this.description;
	}

	@Override
	public String toString() {
		return this.name;
	}

	public static List<ColumnsComparisonTable> getColumns() {
		final List<ColumnsComparisonTable> ret = new ArrayList<ColumnsComparisonTable>();
		for (final ColumnsComparisonTable exportedColumns : ColumnsComparisonTable.values()) {

			ret.add(exportedColumns);
		}
		return ret;
	}

	private static List<ColumnsComparisonTable> getColumnsForTable() {
		return getColumns();
	}

	public static List<String> getColumnsString() {
		final List<String> ret = new ArrayList<String>();
		final List<ColumnsComparisonTable> columns = getColumns();

		for (final ColumnsComparisonTable exportedColumns : columns) {
			ret.add(exportedColumns.toString());
		}

		return ret;
	}

	public static List<String> getColumnsStringForTable() {
		final List<String> ret = new ArrayList<String>();

		final List<ColumnsComparisonTable> columns = getColumnsForTable();

		for (final ColumnsComparisonTable exportedColumns : columns) {
			ret.add(exportedColumns.toString());
		}

		return ret;
	}

	public Class<?> getColumnClass() {
		return clazz;
	}

}
