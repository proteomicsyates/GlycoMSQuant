package edu.scripps.yates.glycomsquant.gui.tables.individual_peptides;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import edu.scripps.yates.glycomsquant.PTMCode;
import edu.scripps.yates.glycomsquant.util.ColorsUtil;

enum ColumnsPeptidesTable {
	FULL_SEQUENCE("Sequence", 300, "Sequence of the peptide", String.class), //
	KEY("key", 250, "Unmodified sequence + charge", String.class, true), //
	CHARGE("z", 20, "Charge state of the peptide", Integer.class),
	TOTAL_SPC("SPC", 20, "Spectral counts that contributed to this peptide", Integer.class),
	SPC_PER_REPLICATE("SPC/rep", 40, "Spectral counts that contributed to this peptide in each of the replicates",
			Integer.class),
	REPLICATES("Replicate", 80, "Replicate", Integer.class),

	LENGTH("Len", 20, "Length of the peptide", Integer.class, true), //
	SITES("Site(s)", 60, "Position(s) with PTMs of interest covered by the peptide in the protein sequence",
			String.class, true), //
	REF_SITES("Ref Site(s)", 60,
			"Position(s) with PTMs of interest covered by the peptide mapped to reference protein sequence",
			String.class, true), //
	STARTING_POSITION("Start", 20, "Starting position of peptide in protein", String.class, true), //
	ENDING_POSITION("End", 20, "Ending position of peptide in protein", String.class, true), //

	INTENSITY("Intensity", 80, "Intensity measured (usually as area under the curve) of the peptide in the replicate",
			String.class), //
//	PERCENT_2("% 2.988", 10, "Percentage of abundance of sites modified with PTM 2.988", Double.class, true,
//			PTMCode._2),
//	PERCENT_203("% 203.079", 10, "Percentage of abundance of sites modified with PTM 203.079", Double.class, true,
//			PTMCode._203); //
	;

	private final String name;
	private final int defaultWidth;
	private final String description;
	private final Class<?> clazz;
	private final boolean isExtended;
	private final PTMCode ptmCode;

	ColumnsPeptidesTable(String name, int defaultWidth, String description, Class<?> clazz, PTMCode ptmCode) {
		this(name, defaultWidth, description, clazz, false, ptmCode);
	}

	ColumnsPeptidesTable(String name, int defaultWidth, String description, Class<?> clazz) {
		this(name, defaultWidth, description, clazz, false);
	}

	ColumnsPeptidesTable(String name, int defaultWidth, String description, Class<?> clazz, boolean isExtended) {
		this(name, defaultWidth, description, clazz, isExtended, null);
	}

	ColumnsPeptidesTable(String name, int defaultWidth, String description, Class<?> clazz, boolean isExtended,
			PTMCode ptmCode) {
		this.name = name;
		this.defaultWidth = defaultWidth;
		this.description = description;
		this.clazz = clazz;
		this.isExtended = isExtended;
		this.ptmCode = ptmCode;
	}

	public boolean isExtended() {
		return isExtended;
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

	public static List<ColumnsPeptidesTable> getColumns(boolean extended) {
		final List<ColumnsPeptidesTable> ret = new ArrayList<ColumnsPeptidesTable>();
		for (final ColumnsPeptidesTable exportedColumns : ColumnsPeptidesTable.values()) {
			if (!extended && exportedColumns.isExtended()) {
				continue;
			}
			ret.add(exportedColumns);
		}
		return ret;
	}

	public static List<String> getColumnsString(boolean extended) {
		final List<String> ret = new ArrayList<String>();
		final List<ColumnsPeptidesTable> columns = getColumns(extended);

		for (final ColumnsPeptidesTable exportedColumns : columns) {
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
