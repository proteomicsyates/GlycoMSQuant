package edu.scripps.yates.glycomsquant.gui.tables.runs;

import java.util.ArrayList;
import java.util.List;

enum ColumnsRunTable {
	NUMBER("#", 3, "", Integer.class), //
	RUN_PATH("Path", 30, "Path of the results", String.class), //
	NAME("Name", 10, "Name set when results were created", String.class),
	INPUT_DATA_FILE("Input file", 30, "Input data file", String.class), //
	THRESHOLD("Int threshold", 10, "Intensity threshold", Double.class),
	NORMALIZE_REPLICATES("Normalize replicates", 10,
			"Normalize the intensity of the replicates by dividing by the total sum of intensities", Boolean.class),
	SUM_INTENSITIES_ACROSS_REPLICATES("Sum int rep", 10, "Sum intensities across replicates", Boolean.class),
	DISCARD_PEPTIDES_WITH_PTMS_IN_WRONG_MOTIFS("Dis. wrong motif peps", 10,
			"Discard peptides with PTMs of interest located in non-valid motifs", Boolean.class)
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

	private static List<ColumnsRunTable> getColumnsForTable() {
		return getColumns();
	}

	public static List<String> getColumnsString() {
		final List<String> ret = new ArrayList<String>();
		final List<ColumnsRunTable> columns = getColumns();

		for (final ColumnsRunTable exportedColumns : columns) {
			ret.add(exportedColumns.toString());
		}

		return ret;
	}

	public static List<String> getColumnsStringForTable() {
		final List<String> ret = new ArrayList<String>();

		final List<ColumnsRunTable> columns = getColumnsForTable();

		for (final ColumnsRunTable exportedColumns : columns) {
			ret.add(exportedColumns.toString());
		}

		return ret;
	}

	public Class<?> getColumnClass() {
		return clazz;
	}

}
