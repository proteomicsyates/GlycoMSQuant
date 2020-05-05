package edu.scripps.yates.glycomsquant.gui.tables.grouped_peptides;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import edu.scripps.yates.glycomsquant.PTMCode;
import edu.scripps.yates.glycomsquant.util.ColorsUtil;

enum ColumnsGroupedPeptidesTable {
	SEQUENCE("Sequence", 300, "Sequence of the peptide with no modifications of interest", String.class), //
	CHARGE("charge", 20, "Charge state of the peptide", Integer.class),
	TOTAL_SPC("SPC", 20, "Spectral counts that contributed to this peptide", Integer.class),
	SPC_PER_REPLICATE("SPC/rep", 40, "Spectral counts that contributed to this peptide in each of the replicates",
			Integer.class),
	REPLICATES("Replicate", 80, "Replicate", Integer.class),

	SITES("Glyco site(s)", 60, "Site position(s) covered by the peptide in the protein sequence", String.class), //
	STARTING_POSITION("Start", 10, "Starting position of peptide in protein", String.class), //

	PERCENT_NoPTM("% No-PTM", 20, "Percentage of abundance of non-modified sites", Double.class, PTMCode._0), //
	PERCENT_2("% 2.988", 20, "Percentage of abundance of sites modified with PTM 2.988", Double.class, PTMCode._2),
	PERCENT_203("% 203.079", 20, "Percentage of abundance of sites modified with PTM 203.079", Double.class,
			PTMCode._203); //

	private final String name;
	private final int defaultWidth;
	private final String description;
	private final Class<?> clazz;
	private final PTMCode ptmCode;

	ColumnsGroupedPeptidesTable(String name, int defaultWidth, String description, Class<?> clazz) {
		this(name, defaultWidth, description, clazz, null);
	}

	ColumnsGroupedPeptidesTable(String name, int defaultWidth, String description, Class<?> clazz, PTMCode ptmCode) {
		this.name = name;
		this.defaultWidth = defaultWidth;
		this.description = description;
		this.clazz = clazz;
		this.ptmCode = ptmCode;
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

	public static List<ColumnsGroupedPeptidesTable> getColumns() {
		final List<ColumnsGroupedPeptidesTable> ret = new ArrayList<ColumnsGroupedPeptidesTable>();
		for (final ColumnsGroupedPeptidesTable exportedColumns : ColumnsGroupedPeptidesTable.values()) {

			ret.add(exportedColumns);
		}
		return ret;
	}

	private static List<ColumnsGroupedPeptidesTable> getColumnsForTable() {
		return getColumns();
	}

	public static List<String> getColumnsString() {
		final List<String> ret = new ArrayList<String>();
		final List<ColumnsGroupedPeptidesTable> columns = getColumns();

		for (final ColumnsGroupedPeptidesTable exportedColumns : columns) {
			ret.add(exportedColumns.toString());
		}

		return ret;
	}

	public static List<String> getColumnsStringForTable() {
		final List<String> ret = new ArrayList<String>();

		final List<ColumnsGroupedPeptidesTable> columns = getColumnsForTable();

		for (final ColumnsGroupedPeptidesTable exportedColumns : columns) {
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
