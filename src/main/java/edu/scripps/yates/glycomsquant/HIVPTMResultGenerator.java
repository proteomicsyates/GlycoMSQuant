package edu.scripps.yates.glycomsquant;

import java.awt.Color;
import java.awt.Paint;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.statistics.DefaultStatisticalCategoryDataset;
import org.proteored.pacom.analysis.charts.BarChart;
import org.proteored.pacom.analysis.charts.StackedBarChart;

import edu.scripps.yates.utilities.maths.Maths;

/**
 * Writes table and/or graph
 * 
 * @author salvador
 *
 */
public class HIVPTMResultGenerator {
	private final static Logger log = Logger.getLogger(HIVPTMResultGenerator.class);
	private static final Paint[] DEFAULT_COLORS = new Paint[] { new Color(166, 166, 166), new Color(255, 0, 255),
			new Color(0, 176, 80) };
	private final File parentFolder;
	private final String prefix;
	private final String suffix;
	private final List<HIVPosition> hivPositions;
	private final double intensityThreshold;
	private final List<File> generatedImages = new ArrayList<File>();

	private enum ERROR_TYPE {
		STDEV, SEM
	};

	public HIVPTMResultGenerator(File parentFolder, String prefix, String suffix, List<HIVPosition> hivPositions,
			double intensityThreshold) {

		this.parentFolder = parentFolder;
		this.prefix = prefix;
		this.suffix = suffix;
		this.hivPositions = hivPositions;
		this.intensityThreshold = intensityThreshold;
	}

	public void generateResults(boolean generateTable, boolean generateGraph) throws IOException {
		if (generateTable) {
			writeResultTable();
		}
		if (generateGraph) {
			writeGraph();
		}
	}

	private final static DecimalFormat format = new DecimalFormat("#.#E0");

	private void writeGraph() throws IOException {

		String subtitle = "";
		if (prefix != null) {
			subtitle = prefix;
		}
		if (!"".equals(subtitle) && suffix != null) {
			subtitle += "_" + suffix;
		}
		// with PSMs on the names
		String title = "Site % abundaces (with PSMs)";
		boolean psms = true;
		generatedImages.add(writeStackedChart(title, subtitle, psms));

		// with PSMs on the names
		boolean makeLog = false;
		title = "Site abundaces with SEM";
		generatedImages.add(writeBarChartWithError(title, subtitle, psms, ERROR_TYPE.SEM, makeLog));
		title = "Site abundaces with STDEV";
		generatedImages.add(writeBarChartWithError(title, subtitle, psms, ERROR_TYPE.STDEV, makeLog));
	}

	private File writeBarChartWithError(String title, String subtitle, boolean psms, ERROR_TYPE errorType,
			boolean makeLog) throws IOException {
		String psmsString = "spc";
		if (!psms) {
			psmsString = "peps";
		}

		DefaultStatisticalCategoryDataset datasetWithErrors = createDatasetWithError(psms, makeLog, errorType);
		String logInsideString = "_log2";
		if (!makeLog) {
			logInsideString = "";
		}
		BarChart chart = new BarChart(title, subtitle, "site # (" + psmsString + ")",
				"AVG" + logInsideString + "_Intensity and SEM", datasetWithErrors, PlotOrientation.VERTICAL);
		// do not separate the bars on each site
		chart.getRenderer().setItemMargin(0);
		chart.getRenderer().setDefaultItemLabelsVisible(false);
		chart.setSeriesPaint(DEFAULT_COLORS);
		File file = new File(getFileName(logInsideString + "_" + errorType + "_errors_" + psmsString + "_"
				+ format.format(intensityThreshold) + ".png"));
		ChartUtils.saveChartAsPNG(file, chart.getChartPanel().getChart(), 1024, 600);
		log.info("Chart generated and saved at '" + file.getAbsolutePath() + "'");
		return file;
	}

	private File writeStackedChart(String title, String subtitle, boolean psms) throws IOException {
		String psmsString = "spc";
		if (!psms) {
			psmsString = "peps";
		}
		CategoryDataset dataset = createDataset(psms);
		StackedBarChart chart = new StackedBarChart(title, subtitle, "site # (" + psmsString + ")", "% abundance",
				dataset, PlotOrientation.VERTICAL, true);
		chart.setSeriesPaint(DEFAULT_COLORS);
		// {3} refers to the percentage
		chart.setNonIntegerItemLabels("{3}", "#.#");
		File file = new File(getFileName("_stacked_" + psmsString + "_" + format.format(intensityThreshold) + ".png"));
		ChartUtils.saveChartAsPNG(file, chart.getChartPanel().getChart(), 1024, 600);
		log.info("Chart generated and saved at '" + file.getAbsolutePath() + "'");
		return file;
	}

	private String translateCode(String code) {
		if (PTMCode._0.getCode().equals(code)) {
			return "NonPTM";
		}
		return code;
	}

	/**
	 * 
	 * @param extension ej: .txt or .png
	 * @return
	 */
	private String getFileName(String extension) {
		String fileName = parentFolder.getAbsolutePath() + File.separator + prefix + "_resultTable_" + suffix
				+ extension;
		return fileName;
	}

	private CategoryDataset createDataset(boolean psms) {
		final DefaultCategoryDataset dataset = new DefaultCategoryDataset();

		for (HIVPosition hivPosition : hivPositions) {
			String columnKey = null;
			if (psms) {
				columnKey = hivPosition.getPosition() + " (" + hivPosition.getSPCByPTMCode(PTMCode._0) + "/"
						+ hivPosition.getSPCByPTMCode(PTMCode._2) + "/" + hivPosition.getSPCByPTMCode(PTMCode._203)
						+ ")";
			} else {
				columnKey = hivPosition.getPosition() + " (" + hivPosition.getNumPeptidesByPTMCode(PTMCode._0) + "/"
						+ hivPosition.getNumPeptidesByPTMCode(PTMCode._2) + "/"
						+ hivPosition.getNumPeptidesByPTMCode(PTMCode._203) + ")";
			}
			for (PTMCode ptmCode : PTMCode.values()) {
				double avgIntensity = hivPosition.getAverageByPTMCode(ptmCode);
				dataset.addValue(avgIntensity, translateCode(ptmCode.getCode()), columnKey);
			}
		}

		return dataset;

	}

	private DefaultStatisticalCategoryDataset createDatasetWithError(boolean psms, boolean makeLog,
			ERROR_TYPE errorType) {
		final DefaultStatisticalCategoryDataset dataset = new DefaultStatisticalCategoryDataset();

		for (HIVPosition hivPosition : hivPositions) {
			String columnKey = null;
			if (psms) {
				columnKey = hivPosition.getPosition() + " (" + hivPosition.getSPCByPTMCode(PTMCode._0) + "/"
						+ hivPosition.getSPCByPTMCode(PTMCode._2) + "/" + hivPosition.getSPCByPTMCode(PTMCode._203)
						+ ")";
			} else {
				columnKey = hivPosition.getPosition() + " (" + hivPosition.getNumPeptidesByPTMCode(PTMCode._0) + "/"
						+ hivPosition.getNumPeptidesByPTMCode(PTMCode._2) + "/"
						+ hivPosition.getNumPeptidesByPTMCode(PTMCode._203) + ")";
			}

			for (PTMCode ptmCode : PTMCode.values()) {
				double average = hivPosition.getAverageByPTMCode(ptmCode);
				if (makeLog && Double.compare(0.0, average) != 0) {
					average = Maths.log(average, 2);
				}
				double error = 0.0;
				switch (errorType) {
				case SEM:
					error = hivPosition.getSEMByPTMCode(ptmCode);
					break;
				case STDEV:
					error = hivPosition.getSTDEVByPTMCode(ptmCode);
					break;
				default:
					break;
				}

				if (makeLog && Double.compare(0.0, error) != 0) {
					error = Maths.log(error, 2);
				}
				dataset.add(average, error, translateCode(ptmCode.getCode()), columnKey);
			}
		}

		return dataset;
	}

	private void writeResultTable() throws IOException {
		String fileName = getFileName(".txt");
		FileWriter fw = new FileWriter(fileName);

		fw.write("results for analysis: " + prefix + "-" + suffix + "\n");
		for (PTMCode ptmCode : PTMCode.values()) {
			fw.write("\tAVG_" + translateCode(ptmCode.getCode()) + "\tSTDEV_" + translateCode(ptmCode.getCode())
					+ "\tSEM_" + translateCode(ptmCode.getCode()) + "\t%_" + translateCode(ptmCode.getCode()) + "\tSPC_"
					+ translateCode(ptmCode.getCode()) + "\tPEP_" + translateCode(ptmCode.getCode()));
		}
		fw.write("\n");
		for (HIVPosition hivPosition : hivPositions) {
			fw.write(hivPosition.getPosition() + "\t");
			for (PTMCode ptmCode : PTMCode.values()) {
				fw.write(hivPosition.getAverageByPTMCode(ptmCode) + "\t");
				fw.write(hivPosition.getSTDEVByPTMCode(ptmCode) + "\t");
				fw.write(hivPosition.getSEMByPTMCode(ptmCode) + "\t");
				fw.write(hivPosition.getPercentageOfSumsByPTMCode(ptmCode) + "\t");
				int spc = hivPosition.getSPCByPTMCode(ptmCode);
				fw.write(spc + "\t");
				int numPeptides = hivPosition.getNumPeptidesByPTMCode(ptmCode);
				fw.write(numPeptides + "\t");
			}
			fw.write("\n");
		}
		fw.close();
	}

	public void deleteGraphs() {
		for (File file : this.generatedImages) {
			file.delete();
		}
	}

}
