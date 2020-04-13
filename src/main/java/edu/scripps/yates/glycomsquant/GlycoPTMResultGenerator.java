package edu.scripps.yates.glycomsquant;

import java.awt.Color;
import java.awt.Paint;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.SwingWorker;

import org.apache.log4j.Logger;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.statistics.DefaultStatisticalCategoryDataset;
import org.proteored.pacom.analysis.charts.BarChart;
import org.proteored.pacom.analysis.charts.StackedBarChart;

import edu.scripps.yates.glycomsquant.gui.files.ResultsProperties;
import edu.scripps.yates.utilities.maths.Maths;

/**
 * Writes table and/or graph
 * 
 * @author salvador
 *
 */
public class GlycoPTMResultGenerator extends SwingWorker<Void, Object> {
	private final static Logger log = Logger.getLogger(GlycoPTMResultGenerator.class);
	private static final Paint[] DEFAULT_COLORS = new Paint[] { new Color(166, 166, 166), new Color(255, 0, 255),
			new Color(0, 176, 80) };
	private final File resultsFolder;
	private final String name;
	private final List<GlycoSite> glycoSites;
	private final List<File> generatedImages = new ArrayList<File>();
	public static final String CHART_GENERATED = "CHART_GENERATED";
	public static final String RESULS_TABLE_GENERATED = "RESULTS_TABLE_GENERATED";
	public static final String GLYCO_SITE_DATA_TABLE_GENERATED = "GLYCO_SITE_DATA_TABLE_GENERATED";
	public static final String RESULTS_GENERATOR_ERROR = "RESULTS_GENERATOR_ERROR";
	public static final String RESULTS_GENERATOR_FINISHED = "RESULTS_GENERATOR_FINISHED";
	private final List<JPanel> charts = new ArrayList<JPanel>();
	private final boolean calculateProportionsByPeptidesFirst;
	public static String RESULTS_TABLE_INFIX = "_resultTable_";
	public static String DATA_TABLE_INFIX = "_data_";
	private boolean generateTable = true;
	private boolean generateGraph = true;
	private boolean saveGraphsToFiles = true;

	private enum ERROR_TYPE {
		STDEV, SEM
	};

	public GlycoPTMResultGenerator(File resultsFolder, String name, List<GlycoSite> glycoSites,
			boolean calculateProportionsByPeptidesFirst) {
		this.resultsFolder = resultsFolder;
		this.name = name;
		this.glycoSites = glycoSites;
		this.calculateProportionsByPeptidesFirst = calculateProportionsByPeptidesFirst;
	}

	public GlycoPTMResultGenerator(List<GlycoSite> glycoSites) {
		this(null, null, glycoSites, false);
	}

	public void setGenerateTable(boolean generateTable) {
		this.generateTable = generateTable;
	}

	public void setGenerateGraph(boolean generateGraph) {
		this.generateGraph = generateGraph;
	}

	public void generateResults() throws IOException {

		if (generateTable) {
			final File tableFile = writeResultTable(calculateProportionsByPeptidesFirst);
			// update property
			ResultsProperties.getResultsProperties(resultsFolder).setResultsTableFile(tableFile);
			// generate glycoSiteDataTable
			final File dataTableFile = writeDataTable();
			// update property
			ResultsProperties.getResultsProperties(resultsFolder).setGlycoSitesTableFile(dataTableFile);
		}
		if (generateGraph) {
			writeGraph();
		}
		firePropertyChange(RESULTS_GENERATOR_FINISHED, null, null);
	}

	private File writeDataTable() throws IOException {
		final File file = new File(getFileName(".dat"));
		final FileWriter fw = new FileWriter(file);
		for (final GlycoSite glycoSite : this.glycoSites) {
			fw.write(glycoSite.printOut() + "\n");
		}
		fw.close();

		firePropertyChange(GLYCO_SITE_DATA_TABLE_GENERATED, null, file);
		return file;
	}

	private void writeGraph() throws IOException {

		String subtitle = "";
		if (name != null) {
			subtitle = name;
		}
		if (!"".equals(subtitle) && name != null) {
			subtitle += "_" + name;
		}
		// with PSMs on the names
		String title = "Site % abundaces (with PSMs)";
		final boolean psms = true;
		generatedImages.add(writeStackedChartOfPercentages(title, subtitle, psms));

		// with PSMs on the names
		final boolean makeLog = false;
		title = "Site abundaces with SEM";
		generatedImages.add(writeBarChartWithError(title, subtitle, psms, ERROR_TYPE.SEM, makeLog));
		title = "Site abundaces with STDEV";
		generatedImages.add(writeBarChartWithError(title, subtitle, psms, ERROR_TYPE.STDEV, makeLog));

		firePropertyChange(CHART_GENERATED, null, charts);
	}

	private File writeBarChartWithError(String title, String subtitle, boolean psms, ERROR_TYPE errorType,
			boolean makeLog) throws IOException {
		String psmsString = "spc";
		if (!psms) {
			psmsString = "peps";
		}

		final DefaultStatisticalCategoryDataset datasetWithErrors = createDatasetWithError(psms, makeLog, errorType);
		String logInsideString = "_log2";
		if (!makeLog) {
			logInsideString = "";
		}
		final BarChart chart = new BarChart(title, subtitle, "site # (" + psmsString + ")",
				"Avg" + logInsideString + "_Intensity and " + errorType, datasetWithErrors, PlotOrientation.VERTICAL);
		// do not separate the bars on each site
		chart.getRenderer().setItemMargin(0);
		chart.getRenderer().setDefaultItemLabelsVisible(false);
		chart.setSeriesPaint(DEFAULT_COLORS);
		this.charts.add(chart.getChartPanel());
		if (saveGraphsToFiles) {
			final File file = new File(
					getFileName(logInsideString + "_" + errorType + "_errors_" + psmsString + "_" + ".png"));
			ChartUtils.saveChartAsPNG(file, chart.getChartPanel().getChart(), 1024, 600);
			log.info("Chart generated and saved at '" + file.getAbsolutePath() + "'");
			return file;
		}
		return null;
	}

	private File writeStackedChartOfPercentages(String title, String subtitle, boolean psms) throws IOException {
		String psmsString = "spc";
		if (!psms) {
			psmsString = "peps";
		}
		final CategoryDataset dataset = createDatasetOfPercentages(psms);
		final StackedBarChart chart = new StackedBarChart(title, subtitle, "site # (" + psmsString + ")", "% abundance",
				dataset, PlotOrientation.VERTICAL, false);
		chart.setSeriesPaint(DEFAULT_COLORS);
		// {3} refers to the percentage
		chart.setNonIntegerItemLabels("{3}", "#.#");
		this.charts.add(chart.getChartPanel());
		if (saveGraphsToFiles) {
			final File file = new File(getFileName("_stacked_" + psmsString + "_" + ".png"));
			ChartUtils.saveChartAsPNG(file, chart.getChartPanel().getChart(), 1024, 600);
			log.info("Chart generated and saved at '" + file.getAbsolutePath() + "'");
			return file;
		}

		return null;
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
		final String fileName = resultsFolder.getAbsolutePath() + File.separator + name + RESULTS_TABLE_INFIX
				+ extension;
		return fileName;
	}

	private CategoryDataset createDatasetOfPercentages(boolean psms) {
		final DefaultCategoryDataset dataset = new DefaultCategoryDataset();

		for (final GlycoSite hivPosition : glycoSites) {
			if (hivPosition.getPosition() == 52) {
				log.info("asdf");
			}
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
			for (final PTMCode ptmCode : PTMCode.values()) {
				final double avgIntensity = hivPosition.getPercentageByPTMCode(ptmCode,
						calculateProportionsByPeptidesFirst);
				dataset.addValue(avgIntensity, translateCode(ptmCode.getCode()), columnKey);
			}
		}

		return dataset;

	}

	private DefaultStatisticalCategoryDataset createDatasetWithError(boolean psms, boolean makeLog,
			ERROR_TYPE errorType) {
		final DefaultStatisticalCategoryDataset dataset = new DefaultStatisticalCategoryDataset();

		for (final GlycoSite hivPosition : glycoSites) {
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

			for (final PTMCode ptmCode : PTMCode.values()) {
				double average = hivPosition.getAverageIntensityByPTMCode(ptmCode);
				if (makeLog && Double.compare(0.0, average) != 0) {
					average = Maths.log(average, 2);
				}
				double error = 0.0;
				switch (errorType) {
				case SEM:
					error = hivPosition.getSEMIntensityByPTMCode(ptmCode);
					break;
				case STDEV:
					error = hivPosition.getSTDEVIntensityByPTMCode(ptmCode);
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

	private File writeResultTable(boolean calculateProportionsByPeptidesFirst) throws IOException {
		final File file = new File(getFileName(".txt"));
		final FileWriter fw = new FileWriter(file);

		fw.write("results for analysis: " + name + "\n");
		for (final PTMCode ptmCode : PTMCode.values()) {
			fw.write("\tAVG_" + translateCode(ptmCode.getCode()) + "\tSTDEV_" + translateCode(ptmCode.getCode())
					+ "\tSEM_" + translateCode(ptmCode.getCode()) + "\t%_" + translateCode(ptmCode.getCode()));
			if (calculateProportionsByPeptidesFirst) {
				fw.write("STDEV(%)_" + translateCode(ptmCode.getCode()));
				fw.write("SEM(%)_" + translateCode(ptmCode.getCode()));
			}
			fw.write("\tSPC_" + translateCode(ptmCode.getCode()) + "\tPEP_" + translateCode(ptmCode.getCode()));
		}
		fw.write("\n");
		for (final GlycoSite glycoSite : glycoSites) {
			fw.write(glycoSite.getPosition() + "\t");
			for (final PTMCode ptmCode : PTMCode.values()) {

				fw.write(glycoSite.getAverageIntensityByPTMCode(ptmCode) + "\t");
				fw.write(glycoSite.getSTDEVIntensityByPTMCode(ptmCode) + "\t");
				fw.write(glycoSite.getSEMIntensityByPTMCode(ptmCode) + "\t");

				fw.write(glycoSite.getPercentageByPTMCode(ptmCode, calculateProportionsByPeptidesFirst) + "\t");
				if (calculateProportionsByPeptidesFirst) {
					fw.write(glycoSite.getSTDEVPercentageByPTMCode(ptmCode) + "\t");
					fw.write(glycoSite.getSEMPercentageByPTMCode(ptmCode) + "\t");
				}
				final int spc = glycoSite.getSPCByPTMCode(ptmCode);
				fw.write(spc + "\t");
				final int numPeptides = glycoSite.getNumPeptidesByPTMCode(ptmCode);
				fw.write(numPeptides + "\t");
			}
			fw.write("\n");
		}
		fw.close();
		firePropertyChange(RESULS_TABLE_GENERATED, false, file);
		return file;
	}

	public void deleteGraphs() {
		for (final File file : this.generatedImages) {
			file.delete();
		}
	}

	@Override
	protected Void doInBackground() throws Exception {
		try {
			generateResults();
		} catch (final Exception e) {
			e.printStackTrace();
			firePropertyChange(RESULTS_GENERATOR_ERROR, null, e.getMessage());
		}
		return null;
	}

	public void setSaveGraphsToFiles(boolean saveGraphsToFiles) {
		this.saveGraphsToFiles = saveGraphsToFiles;
	}
}
