package edu.scripps.yates.glycomsquant.gui;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Paint;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;

import org.apache.log4j.Logger;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.labels.StandardPieSectionLabelGenerator;
import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.general.PieDataset;

import edu.scripps.yates.glycomsquant.GlycoSite;
import edu.scripps.yates.glycomsquant.PTMCode;
import edu.scripps.yates.glycomsquant.gui.charts.PieChart;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;

public class ProportionsPieChartsPanel extends JPanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8678280965340246718L;

	private final List<GlycoSite> glycoSites;
	private final boolean calculatePeptideProportionsFirst;

	private final int columns = 4;
	private final TIntObjectMap<ChartPanel> charts = new TIntObjectHashMap<ChartPanel>();
	private final File resultsFolder;

	private final static int minChartSize = 200;
	private final static int margin = 2;// marging between charts
	private static final Logger log = Logger.getLogger(ProportionsPieChartsPanel.class);

	public ProportionsPieChartsPanel(List<GlycoSite> glycoSites, boolean calculatePeptideProportionsFirst,
			File resultsFolder) {
		super(true);
		this.glycoSites = glycoSites;
		this.calculatePeptideProportionsFirst = calculatePeptideProportionsFirst;
		this.resultsFolder = resultsFolder;
		initComponents();
	}

	private void initComponents() {
		final int rows = glycoSites.size() / columns;
		final int width = columns * minChartSize + margin * (columns + 1);
		final int height = rows * minChartSize + margin * (rows + 1);
		setPreferredSize(new Dimension(width, height));
//		final GridLayout gl = new GridLayout(rows, columns, margin, margin);
		this.setLayout(new FlowLayout(FlowLayout.LEFT, margin, margin));
		for (final GlycoSite glycoSite : glycoSites) {
			final ChartPanel pieChart = createProportionsPieChart(glycoSite);
			charts.put(glycoSite.getPosition(), pieChart);
			this.add(pieChart);
		}
	}

	private ChartPanel createProportionsPieChart(GlycoSite glycoSite) {
		final PieDataset dataset = createProportionsPieDataset(glycoSite);
		final PieChart chart = new PieChart("Site " + glycoSite.getPosition(),
				glycoSite.getTotalSPC() + " SPC / " + glycoSite.getTotalNumPeptides() + " Peptides", dataset, false);
		final Dimension dimension = new Dimension(minChartSize, minChartSize);
		chart.getChartPanel().setPreferredSize(dimension);
		chart.getChartPanel().setSize(dimension);
		chart.getChartPanel().setMinimumDrawWidth(minChartSize);
		chart.getChartPanel().setMinimumDrawHeight(minChartSize);
		setSeriesPaint(chart, ColorsUtil.DEFAULT_COLORS);
		final PiePlot plot = (PiePlot) chart.getChart().getPlot();
		plot.setLabelGenerator(new StandardPieSectionLabelGenerator("{2}", NumberFormat.getIntegerInstance(),
				new DecimalFormat("#.# %")));
		final Font itemsFont = new java.awt.Font("arial", Font.PLAIN, 8);
		plot.setLabelFont(itemsFont);
		final Font titleFont = new java.awt.Font("arial", Font.BOLD, 12);
		chart.getChart().getTitle().setFont(titleFont);
		final Font subtitleFont = new java.awt.Font("arial", Font.BOLD, 8);
		final TextTitle subtitle = (TextTitle) chart.getChart().getSubtitle(1);
		subtitle.setFont(subtitleFont);
		return chart.getChartPanel();
	}

	private void setSeriesPaint(PieChart chart, Paint[] paints) {
		final PiePlot plot = (PiePlot) chart.getChart().getPlot();

		for (int series = 0; series < paints.length; series++) {

			final Paint paint = paints[series];
			final String key = GuiUtils.translateCode(PTMCode.values()[series].getCode());
			plot.setSectionPaint(key, paint);
		}
	}

	private PieDataset createProportionsPieDataset(GlycoSite glycoSite) {
		final DefaultPieDataset dataset = new DefaultPieDataset();
		for (final PTMCode ptmCode : PTMCode.values()) {
			final double percetange = glycoSite.getPercentageByPTMCode(ptmCode, calculatePeptideProportionsFirst);
			dataset.setValue(GuiUtils.translateCode(ptmCode.getCode()), percetange);
		}
		return dataset;
	}

	@Override
	public void updateUI() {
		if (charts != null) {
			for (final ChartPanel chartPanel : charts.valueCollection()) {
				chartPanel.updateUI();
			}
		}
		super.updateUI();
	}

	public List<File> saveImages() {
		final List<File> ret = new ArrayList<File>();
		for (final int position : this.charts.keys()) {
			final ChartPanel chart = this.charts.get(position);
			final String fileName = resultsFolder.getAbsolutePath() + File.separator + "Site_" + position
					+ "_pieChart.png";
			final File file = new File(fileName);
			try {
				ChartUtils.saveChartAsPNG(file, chart.getChart(), 1024, 600);
			} catch (final IOException e) {
				e.printStackTrace();
				log.error(e.getMessage(), e);
			}
			log.info("Chart generated and saved at '" + file.getAbsolutePath() + "'");
			ret.add(file);
		}
		return ret;
	}
}
