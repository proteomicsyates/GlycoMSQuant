package edu.scripps.yates.glycomsquant;

import java.awt.Font;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.SwingWorker;

import org.apache.log4j.Logger;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.CategoryItemRenderer;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.statistics.DefaultStatisticalCategoryDataset;

import edu.scripps.yates.glycomsquant.gui.ProportionsPeptidesScatterPlotChartsPanel;
import edu.scripps.yates.glycomsquant.gui.ProportionsPieChartsPanel;
import edu.scripps.yates.glycomsquant.gui.charts.BarChart;
import edu.scripps.yates.glycomsquant.gui.charts.ChartUtils;
import edu.scripps.yates.glycomsquant.gui.charts.ErrorType;
import edu.scripps.yates.glycomsquant.gui.charts.StackedBarChart;
import edu.scripps.yates.glycomsquant.gui.files.FileManager;
import edu.scripps.yates.glycomsquant.gui.files.ResultsProperties;
import edu.scripps.yates.glycomsquant.util.ColorsUtil;
import edu.scripps.yates.glycomsquant.util.GuiUtils;

/**
 * Writes table and/or graph
 * 
 * @author salvador
 *
 */
public class GlycoPTMResultGenerator extends SwingWorker<Void, Object> {
	private final static Logger log = Logger.getLogger(GlycoPTMResultGenerator.class);

	private final File resultsFolder;
	private final List<GlycoSite> glycoSites;
	private final List<File> generatedImages = new ArrayList<File>();
	public static final String CHART_GENERATED = "CHART_GENERATED";
	public static final String RESULS_TABLE_GENERATED = "RESULTS_TABLE_GENERATED";
	public static final String GLYCO_SITE_DATA_TABLE_GENERATED = "GLYCO_SITE_DATA_TABLE_GENERATED";
	public static final String RESULTS_GENERATOR_ERROR = "RESULTS_GENERATOR_ERROR";
	public static final String RESULTS_GENERATOR_FINISHED = "RESULTS_GENERATOR_FINISHED";

//	private static final int DEFAULT_GRAPH_SIZE = 500;
	private final List<JPanel> charts = new ArrayList<JPanel>();

	private boolean generateTable = true;
	private boolean generateGraph = true;
	private boolean saveGraphsToFiles = true;
	private final InputParameters inputParameters;

	public GlycoPTMResultGenerator(File resultsFolder, List<GlycoSite> glycoSites, InputParameters inputParameters) {
		this.inputParameters = inputParameters;
		this.resultsFolder = resultsFolder;
		this.glycoSites = glycoSites;
	}

	public GlycoPTMResultGenerator(List<GlycoSite> glycoSites, InputParameters inputParameters) {
		this(null, glycoSites, inputParameters);
	}

	public void setGenerateTable(boolean generateTable) {
		this.generateTable = generateTable;
	}

	public void setGenerateGraph(boolean generateGraph) {
		this.generateGraph = generateGraph;
	}

	public void generateResults() throws IOException {

		if (generateTable) {
			final File tableFile = writeResultTable(inputParameters.isCalculateProportionsByPeptidesFirst());
			// update property
			final ResultsProperties resultsProperties = new ResultsProperties(resultsFolder);
			resultsProperties.setResultsTableFile(tableFile);
			// generate glycoSiteDataTable
			final File dataTableFile = writeDataTable();
			// update property
			resultsProperties.setGlycoSitesTableFile(dataTableFile);
		}
		if (generateGraph) {
			firePropertyChange("progress", null, "Generating charts...");
			writeGraphs();
		}
		firePropertyChange(RESULTS_GENERATOR_FINISHED, null, null);
	}

	private File writeDataTable() throws IOException {
		final File file = new File(FileManager.getDataTableFileName(resultsFolder, inputParameters));
		final FileWriter fw = new FileWriter(file);
		for (final GlycoSite glycoSite : this.glycoSites) {
			fw.write(glycoSite.printOut() + "\n");
		}
		fw.close();

		firePropertyChange(GLYCO_SITE_DATA_TABLE_GENERATED, null, file);
		return file;
	}

	private void writeGraphs() throws IOException {

		String subtitle = "";
		final String name = inputParameters.getName();
		if (name != null) {
			subtitle = name;
		}

		// with PSMs on the names
		String title = "Site % abundaces";
		final boolean psms = false;
		generatedImages.add(writeStackedChartOfPercentages(title, subtitle, psms));

		if (inputParameters.isCalculateProportionsByPeptidesFirst()) {
			// proportion error graphs
			title = "SEM of %";
			generatedImages.add(writeProportionsErrorBarChart(title, subtitle, psms, ErrorType.SEM));
			title = "STDEV of %";
			generatedImages.add(writeProportionsErrorBarChart(title, subtitle, psms, ErrorType.STDEV));
			generatedImages.add(writeProportionsBoxAndWhiskerChart(title, subtitle, psms));

		} else {
			// intensity error graphs
			// with PSMs on the names
			final boolean makeLog = false;
			title = "SEM of Intensities";
			generatedImages.add(writeIntensitiesErrorBarChart(title, subtitle, psms, ErrorType.SEM, makeLog));
			title = "STDEV of Intensities";
			generatedImages.add(writeIntensitiesErrorBarChart(title, subtitle, psms, ErrorType.STDEV, makeLog));
		}
		// pie charts with averaged proportions
		final ProportionsPieChartsPanel pieCharts = new ProportionsPieChartsPanel(glycoSites, resultsFolder,
				inputParameters);
		charts.add(pieCharts);
		if (saveGraphsToFiles) {
			generatedImages.addAll(pieCharts.saveImages());
		}

		// scatter plot with peptide individual proportions
		final ProportionsPeptidesScatterPlotChartsPanel scatterPlots = new ProportionsPeptidesScatterPlotChartsPanel(
				glycoSites, resultsFolder, inputParameters);
		charts.add(scatterPlots);
		if (saveGraphsToFiles) {
			generatedImages.addAll(scatterPlots.saveImages());
		}

		firePropertyChange(CHART_GENERATED, null, charts);
	}

	private File writeIntensitiesErrorBarChart(String title, String subtitle, boolean psms, ErrorType errorType,
			boolean makeLog) throws IOException {

		final ChartPanel chartPanel = ChartUtils.createIntensitiesErrorBarChartForSites(glycoSites, title, subtitle, psms,
				makeLog, errorType);

		this.charts.add(chartPanel);
		if (saveGraphsToFiles) {
			final File file = new File(
					FileManager.getGraphFileNameForIntensitiesErrorBarChart(resultsFolder, errorType, inputParameters));

			org.jfree.chart.ChartUtils.saveChartAsPNG(file, chartPanel.getChart(), 1024, 600);
			log.info("Chart generated and saved at '" + file.getAbsolutePath() + "'");
			return file;
		}
		return null;
	}

	private File writeProportionsBoxAndWhiskerChart(String title, String subtitle, boolean psms) throws IOException {
		final ChartPanel chartPanel = ChartUtils.createProportionsBoxAndWhiskerChartForGlycoSites(glycoSites, title,
				subtitle, psms);
		this.charts.add(chartPanel);
		if (saveGraphsToFiles) {
			final File file = new File(
					FileManager.getGraphFileNameForBoxWhiskerPeptideProportions(this.resultsFolder, inputParameters));
			org.jfree.chart.ChartUtils.saveChartAsPNG(file, chartPanel.getChart(), 1024, 600);
			log.info("Chart generated and saved at '" + file.getAbsolutePath() + "'");
			return file;
		}
		return null;
	}

	private File writeProportionsErrorBarChart(String title, String subtitle, boolean psms, ErrorType errorType)
			throws IOException {
		String psmsString = "spc";
		if (!psms) {
			psmsString = "peps";
		}

		final DefaultStatisticalCategoryDataset datasetWithErrors = createProportionsErrorDataset(psms, errorType);

		final BarChart chart = new BarChart(title, subtitle, "site # (" + psmsString + ")",
				"% abundance and " + errorType, datasetWithErrors, PlotOrientation.VERTICAL);
//		chart.getChartPanel().setSize(new Dimension(DEFAULT_GRAPH_SIZE, DEFAULT_GRAPH_SIZE));
		// do not separate the bars on each site
		chart.getRenderer().setItemMargin(0);
		chart.getRenderer().setDefaultItemLabelsVisible(false);
		chart.setSeriesPaint(ColorsUtil.getDefaultColorsSortedByPTMCode());
		// {3} refers to the percentage
		chart.setNonIntegerItemLabels("{3}", "#.#");
		// x labels with 1 decimal
		final CategoryPlot plot = (CategoryPlot) chart.getChart().getPlot();
		final CategoryItemRenderer renderer = plot.getRenderer();
		final Font itemsFont = new java.awt.Font("arial", Font.PLAIN, 8);
		renderer.setDefaultItemLabelFont(itemsFont);
		renderer.setDefaultItemLabelsVisible(true);
		final Font tickLabelsFont = new java.awt.Font("arial", Font.PLAIN, 10);
		plot.getDomainAxis().setTickLabelFont(tickLabelsFont);

		plot.getRangeAxis(0).setMinorTickCount(10);
		final NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
//		rangeAxis.setRange(0.0, 1.0);
		rangeAxis.setTickUnit(new NumberTickUnit(0.1, new DecimalFormat("#.#"), 0));
		rangeAxis.setMinorTickMarksVisible(true);
		rangeAxis.setMinorTickCount(2);

		this.charts.add(chart.getChartPanel());
		if (saveGraphsToFiles) {
			final File file = new File(FileManager.getGraphFileNameForProportionsErrorBarChart(this.resultsFolder,
					errorType, inputParameters));
			org.jfree.chart.ChartUtils.saveChartAsPNG(file, chart.getChartPanel().getChart(), 1024, 600);
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
//		chart.getChartPanel().setSize(new Dimension(DEFAULT_GRAPH_SIZE, DEFAULT_GRAPH_SIZE));
//		chart.getChartPanel().setPreferredSize(new Dimension(DEFAULT_GRAPH_SIZE, DEFAULT_GRAPH_SIZE));
		chart.setSeriesPaint(ColorsUtil.getDefaultColorsSortedByPTMCode());
		// {3} refers to the percentage
		chart.setNonIntegerItemLabels("{3}", "#.#");
		// x labels with 1 decimal
		final CategoryPlot plot = (CategoryPlot) chart.getChartPanel().getChart().getPlot();
		final CategoryItemRenderer renderer = plot.getRenderer();

		final Font itemsFont = new java.awt.Font("arial", Font.PLAIN, 8);
		renderer.setDefaultItemLabelFont(itemsFont);
		renderer.setDefaultItemLabelsVisible(true);
		final Font tickLabelsFont = new java.awt.Font("arial", Font.PLAIN, 10);
		plot.getDomainAxis().setTickLabelFont(tickLabelsFont);

		plot.getRangeAxis(0).setMinorTickCount(10);
		final NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
		rangeAxis.setRange(0.0, 1.0);
		rangeAxis.setTickUnit(new NumberTickUnit(0.1, new DecimalFormat("#.#"), 0));
		rangeAxis.setMinorTickMarksVisible(true);
		rangeAxis.setMinorTickCount(2);

		this.charts.add(chart.getChartPanel());
		if (saveGraphsToFiles) {
			final File file = new File(
					FileManager.getGraphFileNameForProportionsStackedBarChart(this.resultsFolder, inputParameters));
			org.jfree.chart.ChartUtils.saveChartAsPNG(file, chart.getChartPanel().getChart(), 1024, 600);
			log.info("Chart generated and saved at '" + file.getAbsolutePath() + "'");
			return file;
		}

		return null;
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
				columnKey = hivPosition.getPosition() + " (" + hivPosition.getPeptidesByPTMCode(PTMCode._0).size() + "/"
						+ hivPosition.getPeptidesByPTMCode(PTMCode._2).size() + "/"
						+ hivPosition.getPeptidesByPTMCode(PTMCode._203).size() + ")";
			}
			for (final PTMCode ptmCode : PTMCode.values()) {
				final double avgIntensity = hivPosition.getPercentageByPTMCode(ptmCode,
						inputParameters.isCalculateProportionsByPeptidesFirst());
				dataset.addValue(avgIntensity, GuiUtils.translateCode(ptmCode.getCode()), columnKey);
			}
		}

		return dataset;

	}

	private DefaultStatisticalCategoryDataset createProportionsErrorDataset(boolean psms, ErrorType errorType) {
		final DefaultStatisticalCategoryDataset dataset = new DefaultStatisticalCategoryDataset();

		for (final GlycoSite hivPosition : glycoSites) {
			String columnKey = null;
			if (psms) {
				columnKey = hivPosition.getPosition() + " (" + hivPosition.getSPCByPTMCode(PTMCode._0) + "/"
						+ hivPosition.getSPCByPTMCode(PTMCode._2) + "/" + hivPosition.getSPCByPTMCode(PTMCode._203)
						+ ")";
			} else {
				columnKey = hivPosition.getPosition() + " (" + hivPosition.getPeptidesByPTMCode(PTMCode._0).size() + "/"
						+ hivPosition.getPeptidesByPTMCode(PTMCode._2).size() + "/"
						+ hivPosition.getPeptidesByPTMCode(PTMCode._203).size() + ")";
			}

			for (final PTMCode ptmCode : PTMCode.values()) {
				final double average = hivPosition.getPercentageByPTMCode(ptmCode,
						inputParameters.isCalculateProportionsByPeptidesFirst());
				double error = 0.0;
				switch (errorType) {
				case SEM:
					error = hivPosition.getSEMPercentageByPTMCode(ptmCode);
					break;
				case STDEV:
					error = hivPosition.getSTDEVPercentageByPTMCode(ptmCode);
					break;
				default:
					break;
				}

				dataset.add(average, error, GuiUtils.translateCode(ptmCode.getCode()), columnKey);
			}
		}

		return dataset;
	}

	private File writeResultTable(boolean calculateProportionsByPeptidesFirst) throws IOException {
		final File file = new File(FileManager.getResultsTableFileName(resultsFolder, inputParameters));
		final FileWriter fw = new FileWriter(file);

		fw.write("results for analysis: " + inputParameters.getName() + "\n");
		for (final PTMCode ptmCode : PTMCode.values()) {
			fw.write("\tAVG_" + GuiUtils.translateCode(ptmCode.getCode()) + "\tSTDEV_"
					+ GuiUtils.translateCode(ptmCode.getCode()) + "\tSEM_" + GuiUtils.translateCode(ptmCode.getCode())
					+ "\t%_" + GuiUtils.translateCode(ptmCode.getCode()));
			if (calculateProportionsByPeptidesFirst) {
				fw.write("STDEV(%)_" + GuiUtils.translateCode(ptmCode.getCode()));
				fw.write("SEM(%)_" + GuiUtils.translateCode(ptmCode.getCode()));
			}
			fw.write("\tSPC_" + GuiUtils.translateCode(ptmCode.getCode()) + "\tPEP_"
					+ GuiUtils.translateCode(ptmCode.getCode()));
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
				final int numPeptides = glycoSite.getPeptidesByPTMCode(ptmCode).size();
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
