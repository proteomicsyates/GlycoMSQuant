package edu.scripps.yates.glycomsquant.gui.tables.sites;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import edu.scripps.yates.glycomsquant.PTMCode;
import edu.scripps.yates.glycomsquant.util.ColorsUtil;

public enum ColumnsSitesTable {
	SITE("Pos", 3, "Position of the glyco-site in the protein sequence", Integer.class), //
	REFERENCE_SITE("Ref_Pos", 3,
			"Position of the glyco-site mapped to reference protein if available.<br>Same as Pos column if not reference protein was used.",
			String.class), //
	TOTAL_SPC("Total SPC", 5, "Total spectral counts", Integer.class),
	TOTAL_PEPTIDES("Total Peptides", 5, "Total number of peptides", Integer.class),
	//
	AVG_NoPTM("Avg(Int) No-PTM", 10, "Average of intensities of non-modified sites", Double.class, PTMCode._0), //
//	STDEV_NoPTM("Stdev No-PTM", 10, "Standard deviation of intensities of non-modified sites", Double.class,
//			PTMCode._0), //
	SEM_NoPTM("SEM(Int) No-PTM", 10, "Standard Error Mean of intensities of non-modified sites", Double.class,
			PTMCode._0), //
	AVG_PROPORTIONS_NoPTM("Avg % No-PTM", 10, "Average of proportions of non-modified sites", Double.class, true,
			PTMCode._0), //
	MEDIAN_PROPORTIONS_NoPTM("Med % No-PTM", 10, "Median of proportions of non-modified sites", Double.class, true,
			PTMCode._0), //
//	STDEV_PERCENT_NoPTM("Stdev(%) No-PTM", 10,
//			"Standard deviation of the percentage of abundance of non-modified sites", Double.class, true, PTMCode._0), //
	SEM_PERCENT_NoPTM("SEM(%) No-PTM", 10, "Standard Error Mean of the percentage of abundance of non-modified sites",
			Double.class, true, PTMCode._0), //
	SPC_NoPTM("SPC No-PTM", 5, "Spectral counts with no PTMs", Integer.class, PTMCode._0),
	PEPTIDES_NoPTM("Peps. No-PTM", 5, "Number of peptides with no PTMs", Integer.class, PTMCode._0),
	//
	AVG_2("Avg(Int) 2.988", 10, "Average of intensities of sites modified with PTM 2.988", Double.class, PTMCode._2), //
//	STDEV_2("Stdev 2.988", 10, "Standard deviation of intensities of sites modified with PTM 2.988", Double.class,
//			PTMCode._2), //
	SEM_2("SEM(Int) 2.988", 10, "Standard Error Mean of intensities of sites modified with PTM 2.988", Double.class,
			PTMCode._2), //
	AVG_PROPORTIONS_2("Avg % 2.988", 10, "Average of proportions of sites modified with PTM 2.988", Double.class, true,
			PTMCode._2), //
	MEDIAN_PROPORTIONS_2("Med % 2.988", 10, "Median of proportions of sites modified with PTM 2.988", Double.class,
			true, PTMCode._2), //
//	STDEV_PERCENT_2("Stdev(%) 2.988", 10,
//			"Standard deviation of the percentage of abundance of sites modified with PTM 2.988", Double.class, true,
//			PTMCode._2), //
	SEM_PERCENT_2("SEM(%) 2.988", 10,
			"Standard Error Mean of the percentage of abundance of sites modified with PTM 2.988", Double.class, true,
			PTMCode._2), //
	SPC_2("SPC 2.988", 5, "Spectral counts with sites modified with PTM 2.988", Integer.class, PTMCode._2),
	PEPTIDES_2("Peps. 2.988", 5, "Number of peptides with sites modified with PTM 2.988", Integer.class, PTMCode._2),
	//
	AVG_203("Avg(Int) 203.079", 10, "Average of intensities of sites modified with PTM 203.079", Double.class,
			PTMCode._203), //
//	STDEV_203("Stdev 203.079", 10, "Standard deviation of intensities of sites modified with PTM 203.079", Double.class,
//			PTMCode._203), //
	SEM_203("SEM(Int) 203.079", 10, "Standard Error Mean of intensities of sites modified with PTM 203.079",
			Double.class, PTMCode._203), //
	AVG_PROPORTIONS_203("Avg % 203.079", 10, "Average of proportions of sites modified with PTM 203.079", Double.class,
			true, PTMCode._203), //
	MEDIAN_PROPORTIONS_203("Med % 203.079", 10, "Median of proportions of sites modified with PTM 203.079",
			Double.class, true, PTMCode._203), //
//	STDEV_PERCENT_203("Stdev(%) 203.079", 10,
//			"Standard deviation of the percentage of abundance of sites modified with PTM 203.079", Double.class, true,
//			PTMCode._203), //
	SEM_PERCENT_203("SEM(%) 203.079", 10,
			"Standard Error Mean of the percentage of abundance of sites modified with PTM 203.079", Double.class, true,
			PTMCode._203), //
	SPC_203("SPC 203.079", 5, "Spectral counts with sites modified with PTM 203.079", Integer.class, PTMCode._203),
	PEPTIDES_203("Peps. 203.079", 5, "Number of peptides with sites modified with PTM 203.079", Integer.class,
			PTMCode._203),
	//
	ISSUE("ISSUE", 40, "Issue related to this site", String.class)

	;

	private final String name;
	private final int defaultWidth;
	private final String description;
	private final Class<?> clazz;
	private final boolean isPercentage;
	private final PTMCode ptmCode;

	ColumnsSitesTable(String name, int defaultWidth, String description, Class<?> clazz, PTMCode ptmCode) {
		this(name, defaultWidth, description, clazz, false, ptmCode);
	}

	ColumnsSitesTable(String name, int defaultWidth, String description, Class<?> clazz) {
		this(name, defaultWidth, description, clazz, false);
	}

	ColumnsSitesTable(String name, int defaultWidth, String description, Class<?> clazz, boolean isPercentage) {
		this(name, defaultWidth, description, clazz, isPercentage, null);
	}

	ColumnsSitesTable(String name, int defaultWidth, String description, Class<?> clazz, boolean isPercentage,
			PTMCode ptmCode) {
		this.name = name;
		this.defaultWidth = defaultWidth;
		this.description = description;
		this.clazz = clazz;
		this.isPercentage = isPercentage;
		this.ptmCode = ptmCode;
	}

	public boolean isPercentage() {
		return isPercentage;
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

	public static List<ColumnsSitesTable> getColumns() {
		final List<ColumnsSitesTable> ret = new ArrayList<ColumnsSitesTable>();
		for (final ColumnsSitesTable exportedColumns : ColumnsSitesTable.values()) {
			ret.add(exportedColumns);
		}

		return ret;
	}

	public static List<String> getColumnsString() {
		final List<String> ret = new ArrayList<String>();
		final List<ColumnsSitesTable> columns = getColumns();

		for (final ColumnsSitesTable exportedColumns : columns) {
			ret.add(exportedColumns.toString());
		}

		return ret;
	}

	public Class<?> getColumnClass() {
		return clazz;
	}

	public Color getColor() {
		return ColorsUtil.getColorForTableByPTMCode(getAssociatedPTMCode());
	}

	private PTMCode getAssociatedPTMCode() {
		return this.ptmCode;
	}

}
