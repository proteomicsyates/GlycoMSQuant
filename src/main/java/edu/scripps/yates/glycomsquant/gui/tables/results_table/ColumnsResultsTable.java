package edu.scripps.yates.glycomsquant.gui.tables.results_table;

import java.util.ArrayList;
import java.util.List;

enum ColumnsResultsTable {
	SITE("Glyco site", 3, "Position of the glyco-site in the protein sequence", Integer.class), //
	TOTAL_SPC("Total SPC", 5, "Total spectral counts", Integer.class),
	TOTAL_PEPTIDES("Total Peptides", 5, "Total number of peptides", Integer.class),
	//
	AVG_NoPTM("Avg No-PTM", 10, "Average of abundance of non-modified sites", Double.class), //
	STDEV_NoPTM("Stdev No-PTM", 10, "Standard deviation of abundance of non-modified sites", Double.class), //
	SEM_NoPTM("SEM No-PTM", 10, "Standard Error Mean of abundance of non-modified sites", Double.class), //
	PERCENT_NoPTM("% No-PTM", 10, "Percentage of abundance of non-modified sites", Double.class), //
	STDEV_PERCENT_NoPTM("Stdev(%) No-PTM", 10,
			"Standard deviation of the percentage of abundance of non-modified sites", Double.class, true), //
	SEM_PERCENT_NoPTM("SEM(%) No-PTM", 10, "Standard Error Mean of the percentage of abundance of non-modified sites",
			Double.class, true), //
	SPC_NoPTM("SPC with No-PTM", 5, "Spectral counts with no PTMs", Integer.class),
	PEPTIDES_NoPTM("Peptides with No-PTM", 5, "Number of peptides with no PTMs", Integer.class),
	//
	AVG_2("Avg 2.988", 10, "Average of abundance of sites modified with PTM 2.988", Double.class), //
	STDEV_2("Stdev 2.988", 10, "Standard deviation of abundance of sites modified with PTM 2.988", Double.class), //
	SEM_2("SEM 2.988", 10, "Standard Error Mean of abundance of sites modified with PTM 2.988", Double.class), //
	PERCENT_2("% 2.988", 10, "Percentage of abundance of sites modified with PTM 2.988", Double.class), //
	STDEV_PERCENT_2("Stdev(%) 2.988", 10,
			"Standard deviation of the percentage of abundance of sites modified with PTM 2.988", Double.class, true), //
	SEM_PERCENT_2("SEM(%) 2.988", 10,
			"Standard Error Mean of the percentage of abundance of sites modified with PTM 2.988", Double.class, true), //
	SPC_2("SPC with 2.988", 5, "Spectral counts with sites modified with PTM 2.988", Integer.class),
	PEPTIDES_2("Peptides with 2.988", 5, "Number of peptides with sites modified with PTM 2.988", Integer.class),
	//
	AVG_203("Avg 203.079", 10, "Average of abundance of sites modified with PTM 203.079", Double.class), //
	STDEV_203("Stdev 203.079", 10, "Standard deviation of abundance of sites modified with PTM 203.079", Double.class), //
	SEM_203("SEM 203.079", 10, "Standard Error Mean of abundance of sites modified with PTM 203.079", Double.class), //
	PERCENT_203("% 203.079", 10, "Percentage of abundance of sites modified with PTM 203.079", Double.class), //
	STDEV_PERCENT_203("Stdev(%) 203.079", 10,
			"Standard deviation of the percentage of abundance of sites modified with PTM 203.079", Double.class, true), //
	SEM_PERCENT_203("SEM(%) 203.079", 10,
			"Standard Error Mean of the percentage of abundance of sites modified with PTM 203.079", Double.class,
			true), //
	SPC_203("SPC with 203.079", 5, "Spectral counts with sites modified with PTM 203.079", Integer.class),
	PEPTIDES_203("Peptides with 203.079", 5, "Number of peptides with sites modified with PTM 203.079", Integer.class),;

	private final String name;
	private final int defaultWidth;
	private final String description;
	private final Class<?> clazz;
	private final boolean extra;

	ColumnsResultsTable(String name, int defaultWidth, String description, Class<?> clazz) {
		this(name, defaultWidth, description, clazz, false);
	}

	ColumnsResultsTable(String name, int defaultWidth, String description, Class<?> clazz, boolean extra) {
		this.name = name;
		this.defaultWidth = defaultWidth;
		this.description = description;
		this.clazz = clazz;
		this.extra = extra;
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

	public static List<ColumnsResultsTable> getColumns(boolean calculateProportionsByPeptidesFirst) {
		final List<ColumnsResultsTable> ret = new ArrayList<ColumnsResultsTable>();
		for (final ColumnsResultsTable exportedColumns : ColumnsResultsTable.values()) {
			if (!calculateProportionsByPeptidesFirst && exportedColumns.extra) {
				continue;
			}
			ret.add(exportedColumns);
		}

		return ret;
	}

	private static List<ColumnsResultsTable> getColumnsForTable(boolean calculateProportionsByPeptidesFirst) {
		return getColumns(calculateProportionsByPeptidesFirst);
	}

	public static List<String> getColumnsString(boolean calculateProportionsByPeptidesFirst) {
		final List<String> ret = new ArrayList<String>();
		final List<ColumnsResultsTable> columns = getColumns(calculateProportionsByPeptidesFirst);

		for (final ColumnsResultsTable exportedColumns : columns) {
			ret.add(exportedColumns.toString());
		}

		return ret;
	}

	public static List<String> getColumnsStringForTable(boolean calculateProportionsByPeptidesFirst) {
		final List<String> ret = new ArrayList<String>();

		final List<ColumnsResultsTable> columns = getColumnsForTable(calculateProportionsByPeptidesFirst);

		for (final ColumnsResultsTable exportedColumns : columns) {
			ret.add(exportedColumns.toString());
		}

		return ret;
	}

	public Class<?> getColumnClass() {
		return clazz;
	}

}
