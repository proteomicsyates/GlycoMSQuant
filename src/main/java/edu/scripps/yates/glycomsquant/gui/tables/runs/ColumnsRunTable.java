package edu.scripps.yates.glycomsquant.gui.tables.runs;

import java.util.ArrayList;
import java.util.List;

public enum ColumnsRunTable {
	NUMBER("#", 3, "", Integer.class), //
	RUN_PATH("Path", 50, "Path of the results", String.class), //
	NAME("Name", 50, "Name set when results were created", String.class),
	INPUT_DATA_FILE("Input", 50, "Input data file", String.class), //
	LUCIPHOR_FILE("Luciphor", 50, "Luciphor results file", String.class), //
	THRESHOLD("thr", 30, "Intensity threshold", Double.class),
	NORMALIZE_REPLICATES("norm", 5,
			"Normalize the intensity of the replicates by dividing by the total sum of intensities", Boolean.class),
	SUM_INTENSITIES_ACROSS_REPLICATES("sum", 5,
			"Sum intensities across replicates per peptide before calculating proportions", Boolean.class),
	DISCARD_PEPTIDES_WITH_PTMS_IN_WRONG_MOTIFS("dis", 5, "Discard peptides with PTMs in non-valid motifs",
			Boolean.class),
	FIX_PTM_POSITIONS("fix", 5, "Fix mislocalized PTMs (if possible)", Boolean.class),
	DISCARD_NON_UNIQUE_PEPTIDES("dnu", 5, "Discard non-unique peptides", Boolean.class),

	NOT_ALLOW_CONSECUTIVE_SITES("con", 5, "Don't allow consecutive motifs", Boolean.class),
	USE_REFERENCE_PROTEIN("ref", 5, "Use of reference protein", Boolean.class),
	USE_CHARGE("z", 5, "Use charge for grouping peptides and calculate proportions", Boolean.class)
	// RESEARCHER("researcher", 20, "Researcher name (internal data for the
	// Spanish HPP consortium)"), //
	// GENE_CLASSIFICATION("gene class", 15, "Gene classification (internal data
	// for the Spanish HPP consortium)")
	;

	private final String name;
	private final int defaultWidth;
	private final String description;
	private final Class<?> clazz;

	ColumnsRunTable(String name, int defaultWidth, String description, Class<?> clazz) {
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

	public static List<ColumnsRunTable> getColumns() {
		final List<ColumnsRunTable> ret = new ArrayList<ColumnsRunTable>();
		for (final ColumnsRunTable exportedColumns : ColumnsRunTable.values()) {
			ret.add(exportedColumns);
		}

		return ret;
	}

	public static List<String> getColumnsString() {
		final List<String> ret = new ArrayList<String>();
		final List<ColumnsRunTable> columns = getColumns();

		for (final ColumnsRunTable exportedColumns : columns) {
			ret.add(exportedColumns.toString());
		}

		return ret;
	}

	public Class<?> getColumnClass() {
		return clazz;
	}

}
