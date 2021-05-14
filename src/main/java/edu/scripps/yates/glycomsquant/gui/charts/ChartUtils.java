package edu.scripps.yates.glycomsquant.gui.charts;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.labels.BoxAndWhiskerToolTipGenerator;
import org.jfree.chart.labels.StandardPieSectionLabelGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.category.BoxAndWhiskerRenderer;
import org.jfree.chart.renderer.category.CategoryItemRendererState;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.general.PieDataset;
import org.jfree.data.statistics.BoxAndWhiskerCategoryDataset;
import org.jfree.data.statistics.DefaultBoxAndWhiskerCategoryDataset;
import org.jfree.data.statistics.DefaultStatisticalCategoryDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import edu.scripps.yates.census.read.model.interfaces.QuantifiedPeptideInterface;
import edu.scripps.yates.glycomsquant.GlycoSite;
import edu.scripps.yates.glycomsquant.GroupedQuantifiedPeptide;
import edu.scripps.yates.glycomsquant.PTMCode;
import edu.scripps.yates.glycomsquant.gui.AbstractMultipleChartsBySitePanel;
import edu.scripps.yates.glycomsquant.util.ColorsUtil;
import edu.scripps.yates.glycomsquant.util.GlycoPTMAnalyzerUtil;
import edu.scripps.yates.glycomsquant.util.GuiUtils;
import edu.scripps.yates.utilities.maths.Maths;
import edu.scripps.yates.utilities.util.Pair;
import gnu.trove.list.TDoubleList;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.map.hash.THashMap;

public class ChartUtils {
	public static BoxAndWhiskerCategoryDataset createProportionsBoxAndWhiskerForSites(List<GlycoSite> glycoSites,
			boolean psms, boolean sumIntensitiesAcrossReplicates, String singleDomainAxisKey) {
		final DefaultBoxAndWhiskerCategoryDataset dataset = new DefaultBoxAndWhiskerCategoryDataset();

		for (final GlycoSite site : glycoSites) {
			String columnKey = null;

			if (singleDomainAxisKey != null) {
				columnKey = singleDomainAxisKey;
			} else {

				if (psms) {
					columnKey = site.getReferencePosition() + " (" + site.getSPCByPTMCode(PTMCode._0) + "/"
							+ site.getSPCByPTMCode(PTMCode._2) + "/" + site.getSPCByPTMCode(PTMCode._203) + ")";
				} else {
					columnKey = site.getReferencePosition() + " (" + site.getPeptidesByPTMCode(PTMCode._0).size() + "/"
							+ site.getPeptidesByPTMCode(PTMCode._2).size() + "/"
							+ site.getPeptidesByPTMCode(PTMCode._203).size() + ")";
				}
			}
			for (final PTMCode ptmCode : PTMCode.values()) {

				final TDoubleList doublelist = site.getIndividualPeptideProportionsByPTMCode(ptmCode,
						sumIntensitiesAcrossReplicates);
				if (doublelist == null) {
					continue;// means this site doesn't have peptides covering it
				}
				final List<Double> list = new ArrayList<Double>();
				for (final double double1 : doublelist.toArray()) {
					list.add(double1);
				}
				dataset.add(list, GuiUtils.translateCode(ptmCode.getCode()), columnKey);
			}
		}

		return dataset;
	}

	public static ChartPanel createProportionsBoxAndWhiskerChartForGlycoSites(List<GlycoSite> glycoSites, String title,
			String subtitle, boolean psms, boolean sumIntensitiesAcrossReplicates, String singleDomainAxisKey) {

		final BoxAndWhiskerCategoryDataset dataset = ChartUtils.createProportionsBoxAndWhiskerForSites(glycoSites, psms,
				sumIntensitiesAcrossReplicates, singleDomainAxisKey);
		final BoxAndWhiskerRenderer renderer = new BoxAndWhiskerRenderer();
		renderer.setFillBox(true);
		renderer.setDefaultToolTipGenerator(new BoxAndWhiskerToolTipGenerator());
		renderer.setUseOutlinePaintForWhiskers(true);
		for (int i = 0; i < ColorsUtil.getDefaultColorsSortedByPTMCode().length; i++) {
			final Paint paint = ColorsUtil.getDefaultColorsSortedByPTMCode()[i];
			renderer.setSeriesFillPaint(i, paint);
			renderer.setSeriesPaint(i, paint);
			renderer.setSeriesOutlinePaint(i, ColorsUtil.getBlack());
		}
		String label = "site #";
		if (singleDomainAxisKey != null) {
			label = singleDomainAxisKey;
		}

		final CategoryAxis xAxis = new CategoryAxis(label);
		xAxis.setCategoryLabelPositions(CategoryLabelPositions.createUpRotationLabelPositions(Math.PI / 6.0));

		final NumberAxis yAxis = new NumberAxis("% abundance");
		final CategoryPlot plot = new CategoryPlot(dataset, xAxis, yAxis, renderer);
		plot.setBackgroundPaint(Color.white);
		final JFreeChart chart = new JFreeChart(title, JFreeChart.DEFAULT_TITLE_FONT, plot, true);
		if (subtitle != null) {
			final TextTitle textSubtitle = new TextTitle(subtitle);
			chart.addSubtitle(textSubtitle);
		}
		chart.setBackgroundPaint(Color.white);
		final ChartPanel chartPanel = new ChartPanel(chart);
		chartPanel.setFillZoomRectangle(true);
		chartPanel.setMouseWheelEnabled(true);
		final Dimension dimension = new Dimension(ChartProperties.DEFAULT_CHART_WIDTH,
				ChartProperties.DEFAULT_CHART_HEIGHT);
		chartPanel.setPreferredSize(dimension);
		chartPanel.setSize(dimension);
		chartPanel.setBackground(Color.white);
		return chartPanel;
	}

	public static ChartPanel createProportionsBoxAndWhiskerChartForGroupedPeptides(
			Collection<GroupedQuantifiedPeptide> peptides, String title, String subtitle,
			boolean sumIntensitiesAcrossReplicates, int width, int height) {

		final BoxAndWhiskerCategoryDataset dataset = createProportionsErrorDatasetBoxAndWhiskerforGroupedPeptides(
				peptides, sumIntensitiesAcrossReplicates);
		final BoxAndWhiskerRenderer renderer = new BoxAndWhiskerRenderer() {
			private static final long serialVersionUID = 7752611432598679547L;

			@Override
			public CategoryItemRendererState initialise(Graphics2D g2, Rectangle2D dataArea, CategoryPlot plot,
					int rendererIndex, PlotRenderingInfo info) {
				final CategoryItemRendererState state = super.initialise(g2, dataArea, plot, rendererIndex, info);
				if (state.getBarWidth() > 20)
					state.setBarWidth(20); // Keeps the circle and chart from being huge
				return state;
			}
		};

		renderer.setFillBox(true);
		renderer.setDefaultEntityRadius(1);
		renderer.setWhiskerWidth(0.7);

		renderer.setDefaultToolTipGenerator(new BoxAndWhiskerToolTipGenerator());
		for (int i = 0; i < ColorsUtil.getDefaultColorsSortedByPTMCode().length; i++) {
			final Paint paint = ColorsUtil.getDefaultColorsSortedByPTMCode()[i];
			renderer.setSeriesFillPaint(i, paint);
			renderer.setSeriesPaint(i, paint);
			renderer.setSeriesOutlinePaint(i, ColorsUtil.getBlack());
		}
		final CategoryAxis xAxis = new CategoryAxis("PTM state");
		xAxis.setCategoryLabelPositions(CategoryLabelPositions.createUpRotationLabelPositions(Math.PI / 6.0));

		final NumberAxis yAxis = new NumberAxis("% abundance");
		final CategoryPlot plot = new CategoryPlot(dataset, xAxis, yAxis, renderer);
		plot.setBackgroundPaint(Color.white);
		plot.setOutlinePaint(Color.white);
		plot.setOutlineVisible(false);
		final JFreeChart chart = new JFreeChart(title, GuiUtils.getFontForSmallChartTitle(), plot, true);
		if (subtitle != null) {
			final TextTitle textSubtitle = new TextTitle(subtitle);
			chart.addSubtitle(textSubtitle);
		}
		chart.setBackgroundPaint(Color.white);
		chart.setBorderVisible(false);
		final ChartPanel chartPanel = new ChartPanel(chart);
		final Dimension dimension = new Dimension(width, height);
		chartPanel.setPreferredSize(dimension);
		chartPanel.setSize(dimension);
		return chartPanel;
	}

	public static ChartPanel createIntensitiesBoxAndWhiskerChartForGroupedPeptides(
			Collection<GroupedQuantifiedPeptide> peptides, int positionInProtein, String proteinAcc, String title,
			String subtitle, boolean sumIntensitiesAcrossReplicates, int width, int height) {

		final BoxAndWhiskerCategoryDataset dataset = createIntensitiesErrorDatasetBoxAndWhiskerforGroupedPeptides(
				peptides, positionInProtein, proteinAcc, sumIntensitiesAcrossReplicates);
		final BoxAndWhiskerRenderer renderer = new BoxAndWhiskerRenderer() {
			private static final long serialVersionUID = 7752611432598679547L;

			@Override
			public CategoryItemRendererState initialise(Graphics2D g2, Rectangle2D dataArea, CategoryPlot plot,
					int rendererIndex, PlotRenderingInfo info) {
				final CategoryItemRendererState state = super.initialise(g2, dataArea, plot, rendererIndex, info);
				if (state.getBarWidth() > 20)
					state.setBarWidth(20); // Keeps the circle and chart from being huge
				return state;
			}
		};

		renderer.setFillBox(true);
		renderer.setDefaultEntityRadius(1);
		renderer.setWhiskerWidth(0.7);

		renderer.setDefaultToolTipGenerator(new BoxAndWhiskerToolTipGenerator());
		for (int i = 0; i < ColorsUtil.getDefaultColorsSortedByPTMCode().length; i++) {
			final Paint paint = ColorsUtil.getDefaultColorsSortedByPTMCode()[i];
			renderer.setSeriesFillPaint(i, paint);
			renderer.setSeriesPaint(i, paint);
			renderer.setSeriesOutlinePaint(i, ColorsUtil.getBlack());
		}
		final CategoryAxis xAxis = new CategoryAxis("PTM state");
		xAxis.setCategoryLabelPositions(CategoryLabelPositions.createUpRotationLabelPositions(Math.PI / 6.0));

		final NumberAxis yAxis = new NumberAxis("Intensity");
		final CategoryPlot plot = new CategoryPlot(dataset, xAxis, yAxis, renderer);
		plot.setBackgroundPaint(Color.white);
		plot.setOutlinePaint(Color.white);
		plot.setOutlineVisible(false);
		final JFreeChart chart = new JFreeChart(title, GuiUtils.getFontForSmallChartTitle(), plot, true);
		if (subtitle != null) {
			final TextTitle textSubtitle = new TextTitle(subtitle);
			chart.addSubtitle(textSubtitle);
		}
		chart.setBackgroundPaint(Color.white);
		chart.setBorderVisible(false);
		final ChartPanel chartPanel = new ChartPanel(chart);
		final Dimension dimension = new Dimension(width, height);
		chartPanel.setPreferredSize(dimension);
		chartPanel.setSize(dimension);
		return chartPanel;
	}

	public static BoxAndWhiskerCategoryDataset createProportionsErrorDatasetBoxAndWhiskerforGroupedPeptides(
			Collection<GroupedQuantifiedPeptide> peptides, boolean sumIntensitiesAcrossReplicates) {
		final DefaultBoxAndWhiskerCategoryDataset dataset = new DefaultBoxAndWhiskerCategoryDataset();

		final Map<PTMCode, List<Double>> valuesPerPTMCode = new THashMap<PTMCode, List<Double>>();
		for (final GroupedQuantifiedPeptide groupedPeptide : peptides) {

			final Map<PTMCode, TDoubleList> proportionsByPTM = GlycoPTMAnalyzerUtil
					.getIndividualProportionsByPTMCode(groupedPeptide, sumIntensitiesAcrossReplicates);
			for (final PTMCode ptmCode : PTMCode.values()) {

				final TDoubleList proportions = proportionsByPTM.get(ptmCode);
				if (!valuesPerPTMCode.containsKey(ptmCode)) {
					valuesPerPTMCode.put(ptmCode, new ArrayList<Double>());
				}
				for (final double percentage : proportions.toArray()) {
					valuesPerPTMCode.get(ptmCode).add(percentage);
				}
			}

		}
		for (final PTMCode ptmCode : PTMCode.values()) {
			final String columnKey = "";
			final List<Double> list = valuesPerPTMCode.get(ptmCode);
			dataset.add(list, GuiUtils.translateCode(ptmCode.getCode()), columnKey);
		}

		return dataset;
	}

	public static BoxAndWhiskerCategoryDataset createIntensitiesErrorDatasetBoxAndWhiskerforGroupedPeptides(
			Collection<GroupedQuantifiedPeptide> peptides, int positionInProtein, String proteinAcc,
			boolean sumIntensitiesAcrossReplicates) {
		final DefaultBoxAndWhiskerCategoryDataset dataset = new DefaultBoxAndWhiskerCategoryDataset();

		final Map<PTMCode, List<Double>> valuesPerPTMCode = new THashMap<PTMCode, List<Double>>();
		boolean useCharge = true;
		for (final GroupedQuantifiedPeptide groupedPeptide : peptides) {
			useCharge = groupedPeptide.isUseCharge();
			for (final PTMCode ptmCode : PTMCode.values()) {
				// only get the intensities of the ptmCodes that are present in the position
				// corresponding to the positionInProtein
				for (final QuantifiedPeptideInterface peptide : groupedPeptide) {
					try {
						final int positionInPeptide = GlycoPTMAnalyzerUtil.getPositionInPeptide(peptide, proteinAcc,
								positionInProtein);
						final TIntList positionsInProtein = GlycoPTMAnalyzerUtil
								.getPositionsByPTMCodesFromPeptide(peptide, proteinAcc).get(ptmCode);
						if (positionsInProtein != null && positionsInProtein.contains(positionInPeptide)) {

							// then, we have a peptide with the modification we want in the position we want
							// we create a fakeGroupedQuantifiedPeptide just to get the intensity
							final GroupedQuantifiedPeptide fakeQuantifiedPeptide = new GroupedQuantifiedPeptide(peptide,
									proteinAcc, positionInPeptide, useCharge, positionInProtein);
							final TDoubleList intensities = fakeQuantifiedPeptide.getIntensitiesByPTMCode(ptmCode);
							if (!valuesPerPTMCode.containsKey(ptmCode)) {
								valuesPerPTMCode.put(ptmCode, new ArrayList<Double>());
							}
							for (final double intensity : intensities.toArray()) {
								valuesPerPTMCode.get(ptmCode).add(intensity);
							}
						}
					} catch (final IllegalArgumentException e) {
						// this happens when selecting a position in the protein, the peptides covering
						// that position are selected, and then the sites covered by these peptides are
						// selected, but obviously not all peptides cover all sites
					}
				}

//				final TDoubleList intensities = groupedPeptide.getIntensitiesByPTMCode(ptmCode);
//				if (!valuesPerPTMCode.containsKey(ptmCode)) {
//					valuesPerPTMCode.put(ptmCode, new ArrayList<Double>());
//				}
//				for (final double intensity : intensities.toArray()) {
//					valuesPerPTMCode.get(ptmCode).add(intensity);
//				}
			}

		}
		for (final PTMCode ptmCode : PTMCode.values()) {
			final String columnKey = "";
			if (valuesPerPTMCode.containsKey(ptmCode)) {
				final List<Double> list = valuesPerPTMCode.get(ptmCode);
				dataset.add(list, GuiUtils.translateCode(ptmCode.getCode()), columnKey);
			} else {
				dataset.add(Collections.emptyList(), GuiUtils.translateCode(ptmCode.getCode()), columnKey);
			}
		}

		return dataset;
	}

	public static ChartPanel createProportionsPieChartForGroupedPeptides(Collection<GroupedQuantifiedPeptide> peptides,
			String title, String subtitle, boolean calculatePeptideProportionsFirst, int width, int height) {
		final PieDataset dataset = createProportionsPieDatasetForGroupedPeptide(peptides,
				calculatePeptideProportionsFirst);
		final PieChart chart = new PieChart(title, subtitle, dataset, false);
		chart.getChart().getTitle().setFont(GuiUtils.getFontForSmallChartTitle());
		final PiePlot plot = (PiePlot) chart.getChart().getPlot();
		plot.setLabelGenerator(new StandardPieSectionLabelGenerator("{2}", NumberFormat.getIntegerInstance(),
				new DecimalFormat("#.# %")));
		plot.setLabelFont(AbstractMultipleChartsBySitePanel.itemsFont);
		plot.setBackgroundPaint(Color.white);
// colors

		final Paint[] paints = ColorsUtil.getDefaultColorsSortedByPTMCode();
		for (int series = 0; series < paints.length; series++) {
			final Paint paint = paints[series];
			final String key = GuiUtils.translateCode(PTMCode.values()[series].getCode());
			plot.setSectionPaint(key, paint);
		}
		plot.setOutlineVisible(false);
		final ChartPanel chartPanel = chart.getChartPanel();
		final Dimension dimension = new Dimension(width, height);
		chartPanel.setPreferredSize(dimension);
		chartPanel.setSize(dimension);
		return chartPanel;
	}

	public static PieDataset createProportionsPieDatasetForGroupedPeptide(Collection<GroupedQuantifiedPeptide> peptides,
			boolean calculatePeptideProportionsFirst) {
		final DefaultPieDataset dataset = new DefaultPieDataset();
		for (final PTMCode ptmCode : PTMCode.values()) {
			final TDoubleList toAverage = new TDoubleArrayList();
			for (final GroupedQuantifiedPeptide peptide : peptides) {

				final TDoubleList percetanges = peptide.getProportionsByPTMCode(ptmCode,
						calculatePeptideProportionsFirst);
				toAverage.addAll(percetanges);
			}
			dataset.setValue(GuiUtils.translateCode(ptmCode.getCode()), Maths.mean(toAverage));
		}
		return dataset;
	}

	public static ChartPanel createProportionsPieChartForGlycoSite(GlycoSite glycoSite, String title, String subtitle,
			boolean sumIntensitiesAcrossReplicates, Integer width, Integer height) {
		final PieDataset dataset = createProportionsPieDatasetForGlycoSite(glycoSite, sumIntensitiesAcrossReplicates);
		final PieChart chart = new PieChart(title, subtitle, dataset, false);

		final PiePlot plot = (PiePlot) chart.getChart().getPlot();
		plot.setLabelGenerator(new StandardPieSectionLabelGenerator("{2}", NumberFormat.getIntegerInstance(),
				new DecimalFormat("#.# %")));
		plot.setLabelFont(AbstractMultipleChartsBySitePanel.itemsFont);
		plot.setOutlineVisible(false);
		final Paint[] paints = ColorsUtil.getDefaultColorsSortedByPTMCode();
		for (int series = 0; series < paints.length; series++) {
			final Paint paint = paints[series];
			final String key = GuiUtils.translateCode(PTMCode.values()[series].getCode());
			plot.setSectionPaint(key, paint);
		}

		final ChartPanel chartPanel = chart.getChartPanel();
		if (width != null && height != null) {
			final Dimension dimension = new Dimension(width, height);
			chartPanel.setPreferredSize(dimension);
			chartPanel.setSize(dimension);
		}
		return chartPanel;
	}

	public static PieDataset createProportionsPieDatasetForGlycoSite(GlycoSite glycoSite,
			boolean sumIntensitiesAcrossReplicates) {
		final DefaultPieDataset dataset = new DefaultPieDataset();
		for (final PTMCode ptmCode : PTMCode.values()) {
			final double percetange = glycoSite.getAvgProportionByPTMCode(ptmCode, sumIntensitiesAcrossReplicates);
			dataset.setValue(GuiUtils.translateCode(ptmCode.getCode()), percetange);
		}
		return dataset;
	}

	public static ChartPanel createScatterPlotChartForGlycoSite(GlycoSite glycoSite,
			boolean sumIntensitiesAcrossReplicates, String title, String subtitle, Integer width, Integer height) {
		final Pair<XYDataset, MyXYItemLabelGenerator> pair = createProportionsScatteredDataSetForGlycoSite(glycoSite,
				sumIntensitiesAcrossReplicates);
		final XYDataset dataset = pair.getFirstelement();
		final MyXYItemLabelGenerator tooltipGenerator = pair.getSecondElement();
		final ChartPanel chartPanel = new ChartPanel(
				ChartFactory.createXYLineChart(title, "peptides", "% abundance", dataset));
		final JFreeChart chart = chartPanel.getChart();
		if (subtitle != null) {
			chart.addSubtitle(new TextTitle(subtitle));
		}
		final XYPlot plot = (XYPlot) chart.getPlot();
		plot.setBackgroundPaint(Color.white);
		plot.setOutlineVisible(false);
//		plot.setRangeGridlinePaint(Color.lightGray);
		final XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) plot.getRenderer();
		renderer.setDefaultShapesVisible(true);
		renderer.setDefaultLinesVisible(false);
		for (int i = 0; i < PTMCode.values().length; i++) {
			final Paint paint = ColorsUtil.getDefaultColorByPTMCode(PTMCode.values()[i]);
			renderer.setSeriesPaint(i, paint);
			final double size = 2.0;
			final double delta = size / 2.0;
			final Shape shape1 = new Ellipse2D.Double(-delta, -delta, size, size);
			renderer.setSeriesShape(i, shape1);
		}

		final ValueAxis rangeAxis = plot.getRangeAxis();
		// font for the axis
		rangeAxis.setLabelFont(AbstractMultipleChartsBySitePanel.axisFont);
		rangeAxis.setTickLabelFont(AbstractMultipleChartsBySitePanel.axisFont);
		final ValueAxis domainAxis = plot.getDomainAxis();
		domainAxis.setLabelFont(AbstractMultipleChartsBySitePanel.axisFont);
		domainAxis.setTickLabelFont(AbstractMultipleChartsBySitePanel.axisFont);
//		domainAxis.setRange(-1.0, 1.0);
		domainAxis.setVisible(false);
		if (tooltipGenerator != null) {
			renderer.setDefaultToolTipGenerator(tooltipGenerator);
		}
		if (width != null && height != null) {
			final Dimension dimension = new Dimension(width, height);
			chartPanel.setPreferredSize(dimension);
			chartPanel.setSize(dimension);
		}
		return chartPanel;
	}

	public static ChartPanel createProportionsScatterPlotChartForPeptides(Collection<GroupedQuantifiedPeptide> peptides,
			boolean sumIntensitiesAcrossReplicates, String title, String subtitle, Integer width, Integer height) {
		final Pair<XYDataset, MyXYItemLabelGenerator> pair = createProportionsScatteredDataSetForPeptides(peptides,
				sumIntensitiesAcrossReplicates);
		final XYDataset dataset = pair.getFirstelement();
		final MyXYItemLabelGenerator tooltipGenerator = pair.getSecondElement();
		final ChartPanel chartPanel = new ChartPanel(
				ChartFactory.createXYLineChart(title, "peptides", "% abundance", dataset));
		if (subtitle != null) {
			chartPanel.getChart().addSubtitle(new TextTitle(subtitle));
		}
		final XYPlot plot = (XYPlot) chartPanel.getChart().getPlot();
		plot.setBackgroundPaint(Color.white);
		plot.setOutlineVisible(false);
		plot.setRangeGridlinePaint(Color.lightGray);
		final XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) plot.getRenderer();
		renderer.setDefaultShapesVisible(true);
		renderer.setDefaultLinesVisible(false);
		for (int i = 0; i < PTMCode.values().length; i++) {
			final Paint paint = ColorsUtil.getDefaultColorByPTMCode(PTMCode.values()[i]);
			renderer.setSeriesPaint(i, paint);
			final double size = 2.0;
			final double delta = size / 2.0;
			final Shape shape1 = new Ellipse2D.Double(-delta, -delta, size, size);
			renderer.setSeriesShape(i, shape1);
		}

		final ValueAxis rangeAxis = plot.getRangeAxis();
		// font for the axis
		rangeAxis.setLabelFont(AbstractMultipleChartsBySitePanel.axisFont);
		rangeAxis.setTickLabelFont(AbstractMultipleChartsBySitePanel.axisFont);
		final ValueAxis domainAxis = plot.getDomainAxis();
		domainAxis.setLabelFont(AbstractMultipleChartsBySitePanel.axisFont);
		domainAxis.setTickLabelFont(AbstractMultipleChartsBySitePanel.axisFont);
//		domainAxis.setRange(-1.0, 1.0);
		domainAxis.setVisible(false);
		if (tooltipGenerator != null) {
			renderer.setDefaultToolTipGenerator(tooltipGenerator);
		}
		if (width != null && height != null) {
			final Dimension dimension = new Dimension(width, height);
			chartPanel.setPreferredSize(dimension);
			chartPanel.setSize(dimension);
		}
		return chartPanel;
	}

	private static Pair<XYDataset, MyXYItemLabelGenerator> createProportionsScatteredDataSetForGlycoSite(
			GlycoSite glycoSite, boolean sumIntensitiesAcrossReplicates) {

		return createProportionsScatteredDataSetForPeptides(glycoSite.getCoveredGroupedPeptides(),
				sumIntensitiesAcrossReplicates);
	}

	private static Pair<XYDataset, MyXYItemLabelGenerator> createProportionsScatteredDataSetForPeptides(
			Collection<GroupedQuantifiedPeptide> peptides, boolean sumIntensitiesAcrossReplicates) {

		final XYSeriesCollection xySeriesCollection = new XYSeriesCollection();

		final Map<String, String> tooltipValues = new THashMap<String, String>();

		final Map<PTMCode, XYSeries> seriesByPTMCode = new THashMap<PTMCode, XYSeries>();
		for (final PTMCode ptmCode : PTMCode.values()) {
			final String code = GuiUtils.translateCode(ptmCode.getCode());
			final XYSeries series = new XYSeries(code);
			seriesByPTMCode.put(ptmCode, series);
			xySeriesCollection.addSeries(series);
		}

		double x = 0;
//			final double step = 2.0 / (GlycoPTMAnalyzerUtil.getNumIndividualProportions(ptmCode, peptides,
//					sumIntensitiesAcrossReplicates) + 1);
		final double step = 1;

		XYSeries series;
		for (final GroupedQuantifiedPeptide peptide : peptides) {
			final Map<PTMCode, TDoubleList> percentagesByPTMCode = GlycoPTMAnalyzerUtil
					.getIndividualProportionsByPTMCode(peptide, sumIntensitiesAcrossReplicates);
			for (final PTMCode ptmCode : percentagesByPTMCode.keySet()) {
				series = seriesByPTMCode.get(ptmCode);
				final Comparable code = series.getKey();
				final TDoubleList percentages = percentagesByPTMCode.get(ptmCode);
				for (final double percentage : percentages.toArray()) {
					x += step;
					series.add(x, percentage);
					tooltipValues.put(code.toString(), peptide.getKey(false) + "-" + code.toString());
				}
			}

		}
		return new Pair<XYDataset, MyXYItemLabelGenerator>(xySeriesCollection,
				new MyXYItemLabelGenerator(tooltipValues));

	}

	public static ChartPanel createIntensitiesErrorBarChartForSites(Collection<GlycoSite> glycoSites, String title,
			String subtitle, boolean psms, boolean makeLog, ErrorType errorType) {
		final DefaultStatisticalCategoryDataset datasetWithErrors = createIntensityErrorDatasetForSites(glycoSites,
				psms, makeLog, errorType);
		String logInsideString = "(log2) ";
		if (!makeLog) {
			logInsideString = "";
		}
		String psmsString = "spc";
		if (!psms) {
			psmsString = "peps";
		}
		final BarChart chart = new BarChart(title, subtitle, "site # (" + psmsString + ")",
				"Averaged " + logInsideString + "intensity and " + errorType, datasetWithErrors,
				PlotOrientation.VERTICAL);
		// do not separate the bars on each site
		chart.getRenderer().setItemMargin(0);
		chart.getRenderer().setDefaultItemLabelsVisible(false);
		chart.setSeriesPaint(ColorsUtil.getDefaultColorsSortedByPTMCode());
		return chart.getChartPanel();
	}

	public static ChartPanel createIntensitiesErrorBarChartForPeptides(Collection<GroupedQuantifiedPeptide> peptides,
			int positionInProtein, String proteinAcc, String title, String subtitle, boolean makeLog,
			ErrorType errorType, Integer width, Integer height) {
		final DefaultStatisticalCategoryDataset datasetWithErrors = createIntensityErrorDatasetForPeptides(peptides,
				positionInProtein, proteinAcc, makeLog, errorType);

		final BarChart chart = new BarChart(title, subtitle, "", "Intensity", datasetWithErrors,
				PlotOrientation.VERTICAL);
		// do not separate the bars on each site
		chart.getRenderer().setItemMargin(0);
		chart.getRenderer().setDefaultItemLabelsVisible(false);
		chart.setSeriesPaint(ColorsUtil.getDefaultColorsSortedByPTMCode());
		final CategoryPlot plot = (CategoryPlot) chart.getChart().getPlot();
		plot.setOutlineVisible(false);
		plot.getDomainAxis().setLabel("PTM state");
		final ChartPanel chartPanel = chart.getChartPanel();
		if (width != null && height != null) {
			final Dimension dimension = new Dimension(width, height);
			chartPanel.setPreferredSize(dimension);
			chartPanel.setSize(dimension);
		}
		return chartPanel;
	}

	private static DefaultStatisticalCategoryDataset createIntensityErrorDatasetForSites(
			Collection<GlycoSite> glycoSites, boolean psms, boolean makeLog, ErrorType errorType) {
		final DefaultStatisticalCategoryDataset dataset = new DefaultStatisticalCategoryDataset();

		for (final GlycoSite hivPosition : glycoSites) {
			String columnKey = "";
			if (psms) {
				columnKey = hivPosition.getReferencePosition() + " (" + hivPosition.getSPCByPTMCode(PTMCode._0) + "/"
						+ hivPosition.getSPCByPTMCode(PTMCode._2) + "/" + hivPosition.getSPCByPTMCode(PTMCode._203)
						+ ")";
			} else {
				columnKey = hivPosition.getReferencePosition() + " ("
						+ hivPosition.getPeptidesByPTMCode(PTMCode._0).size() + "/"
						+ hivPosition.getPeptidesByPTMCode(PTMCode._2).size() + "/"
						+ hivPosition.getPeptidesByPTMCode(PTMCode._203).size() + ")";
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
					if (Double.isNaN(error)) {
						error = 0.0;
					}
					break;
//				case STDEV:
//					error = hivPosition.getSTDEVIntensityByPTMCode(ptmCode);
//					break;
				default:
					break;
				}

				if (makeLog && Double.compare(0.0, error) != 0) {
					error = Maths.log(error, 2);
				}
				dataset.add(average, error, GuiUtils.translateCode(ptmCode.getCode()), columnKey);
			}
		}

		return dataset;
	}

	private static DefaultStatisticalCategoryDataset createIntensityErrorDatasetForPeptides(
			Collection<GroupedQuantifiedPeptide> peptides, int positionInProtein, String proteinAcc, boolean makeLog,
			ErrorType errorType) {
		final DefaultStatisticalCategoryDataset dataset = new DefaultStatisticalCategoryDataset();
		boolean useCharge = true;
		for (final PTMCode ptmCode : PTMCode.values()) {
			final TDoubleList intensities = new TDoubleArrayList();

			for (final GroupedQuantifiedPeptide groupedPeptide : peptides) {
				useCharge = groupedPeptide.isUseCharge();
				// only get the intensities of the ptmCodes that are present in the position
				// corresponding to the positionInProtein
				for (final QuantifiedPeptideInterface peptide : groupedPeptide) {
					try {
						final int positionInPeptide = GlycoPTMAnalyzerUtil.getPositionInPeptide(peptide, proteinAcc,
								positionInProtein);
						final TIntList positionsInPeptide = GlycoPTMAnalyzerUtil
								.getPositionsByPTMCodesFromPeptide(peptide, proteinAcc).get(ptmCode);
						if (positionsInPeptide != null && positionsInPeptide.contains(positionInPeptide)) {

							// then, we have a peptide with the modification we want in the position we want
							// we create a fakeGroupedQuantifiedPeptide just to get the intensity
							final Integer nullNumber = null;
							final GroupedQuantifiedPeptide fakeQuantifiedPeptide = new GroupedQuantifiedPeptide(peptide,
									proteinAcc, positionInPeptide, useCharge, nullNumber);
							final TDoubleList intensitiesByPTMCode = fakeQuantifiedPeptide
									.getIntensitiesByPTMCode(ptmCode);

//				final TDoubleList intensitiesByPTMCode = groupedPeptide.getIntensitiesByPTMCode(ptmCode);
							if (intensitiesByPTMCode != null) {
								intensities.addAll(intensitiesByPTMCode);
							}

						}
					} catch (final IllegalArgumentException e) {
						// this happens when selecting a position in the protein, the peptides covering
						// that position are selected, and then the sites covered by these peptides are
						// selected, but obviously not all peptides cover all sites
					}
				}
			}
			double average = Maths.mean(intensities);
			if (makeLog && Double.compare(0.0, average) != 0) {
				average = Maths.log(average, 2);
			}
			double error = 0.0;
			switch (errorType) {
			case SEM:
				error = Maths.sem(intensities);
				break;
//			case STDEV:
//				error = Maths.stddev(intensities);
//				break;
			default:
				break;
			}

			if (makeLog && Double.compare(0.0, error) != 0) {
				error = Maths.log(error, 2);
			}
			dataset.add(average, error, GuiUtils.translateCode(ptmCode.getCode()), "");
		}

		return dataset;
	}
}
