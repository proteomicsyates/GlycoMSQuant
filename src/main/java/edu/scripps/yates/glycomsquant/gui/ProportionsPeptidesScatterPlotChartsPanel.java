package edu.scripps.yates.glycomsquant.gui;

import java.awt.Color;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.log4j.Logger;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.general.Dataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import edu.scripps.yates.glycomsquant.GlycoSite;
import edu.scripps.yates.glycomsquant.InputParameters;
import edu.scripps.yates.glycomsquant.PTMCode;
import edu.scripps.yates.glycomsquant.gui.charts.MyXYItemLabelGenerator;
import edu.scripps.yates.glycomsquant.gui.files.FileManager;
import edu.scripps.yates.utilities.util.Pair;
import gnu.trove.list.TDoubleList;
import gnu.trove.map.hash.THashMap;

public class ProportionsPeptidesScatterPlotChartsPanel extends AbstractProportionsChartsPanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 9209240955358988792L;
	private static final Logger log = Logger.getLogger(ProportionsPeptidesScatterPlotChartsPanel.class);
	private final static Random r = new Random();

	public ProportionsPeptidesScatterPlotChartsPanel(List<GlycoSite> glycoSites, File resultsFolder,
			InputParameters inputParameters) {
		super(glycoSites, resultsFolder, inputParameters);

	}

	@Override
	protected Dataset createDataset(GlycoSite glycoSite) {
		return null;
		// not used
	}

	@Override
	protected String getImageFileFullPath(GlycoSite glycoSite) {
		return FileManager.getGraphFileNameForScatterPlot(resultsFolder, inputParameters, glycoSite);

	}

	@Override
	protected ChartPanel createChart(GlycoSite glycoSite) {
		final Pair<XYDataset, MyXYItemLabelGenerator> pair = createProportionsScatteredDataSet(glycoSite);
		final XYDataset dataset = pair.getFirstelement();
		final MyXYItemLabelGenerator tooltipGenerator = pair.getSecondElement();
		final ChartPanel chartPanel = new ChartPanel(
				ChartFactory.createXYLineChart(super.getChartTitle(glycoSite), "peptides", "% abundance", dataset));
		chartPanel.getChart().addSubtitle(
				new TextTitle(glycoSite.getTotalSPC() + " SPC / " + glycoSite.getTotalNumPeptides() + " Peptides"));
		final XYPlot plot = (XYPlot) chartPanel.getChart().getPlot();

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
		rangeAxis.setLabelFont(axisFont);
		rangeAxis.setTickLabelFont(axisFont);
		final ValueAxis domainAxis = plot.getDomainAxis();
		domainAxis.setLabelFont(axisFont);
		domainAxis.setTickLabelFont(axisFont);
		domainAxis.setRange(-1.0, 1.0);
		domainAxis.setVisible(false);
		if (tooltipGenerator != null) {
			renderer.setDefaultToolTipGenerator(tooltipGenerator);
		}
		return chartPanel;
	}

	@Override
	protected void setSeriesPaint(ChartPanel chart, Paint[] paints) {
		// do nothing...the colours are set in a different way at createChart method.
	}

	private Pair<XYDataset, MyXYItemLabelGenerator> createProportionsScatteredDataSet(GlycoSite glycoSite) {

		final XYSeriesCollection xySeriesCollection = new XYSeriesCollection();

		final Map<String, String> tooltipValues = new THashMap<String, String>();

		for (final PTMCode ptmCode : PTMCode.values()) {
			final String code = GuiUtils.translateCode(ptmCode.getCode());
			final XYSeries series = new XYSeries(code);

//			final int factor = 1000;
//			final int maxoffset = Double.valueOf(factor / 2.0).intValue();
//			final int minoffset = -maxoffset;

			final TDoubleList individualPeptidePercentagesByPTMCode = glycoSite
					.getIndividualPeptidePercentagesByPTMCode(ptmCode);
			// we will spread these values over the x range [-1,1]
			final double step = 2.0 / (individualPeptidePercentagesByPTMCode.size() + 1);
			double x = -1;
			for (final double percentage : individualPeptidePercentagesByPTMCode.toArray()) {

//				final int nextInt = r.nextInt(maxoffset - minoffset) + minoffset;
//				final double offset = 1.0 * nextInt / factor;
				x += step;
				series.add(x, percentage);
				tooltipValues.put(code, "pep1");
			}

			xySeriesCollection.addSeries(series);
		}
		return new Pair<XYDataset, MyXYItemLabelGenerator>(xySeriesCollection,
				new MyXYItemLabelGenerator(tooltipValues));

	}

}
