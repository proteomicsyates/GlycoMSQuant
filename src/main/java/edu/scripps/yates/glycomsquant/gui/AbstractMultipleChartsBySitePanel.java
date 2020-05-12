package edu.scripps.yates.glycomsquant.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Paint;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.swing.JPanel;

import org.apache.log4j.Logger;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.title.LegendTitle;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.general.Dataset;

import edu.scripps.yates.glycomsquant.GlycoSite;
import edu.scripps.yates.glycomsquant.InputParameters;
import edu.scripps.yates.glycomsquant.util.ColorsUtil;
import gnu.trove.map.hash.THashMap;

public abstract class AbstractMultipleChartsBySitePanel extends JPanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8678280965340246718L;

	protected final List<GlycoSite> glycoSites;
	protected final boolean sumIntensitiesAcrossReplicates;

	protected static final int columns = 4;
	protected final Map<GlycoSite, ChartPanel> chartsBySite = new THashMap<GlycoSite, ChartPanel>();
	protected final File resultsFolder;

	protected final static int minChartSize = 175;
	protected final static int margin = 1;// marging between charts
	private static final Logger log = Logger.getLogger(AbstractMultipleChartsBySitePanel.class);
	public static Font itemsFont = new java.awt.Font("arial", Font.PLAIN, 8);
	protected Font titleFont = new java.awt.Font("arial", Font.BOLD, 12);
	protected Font subtitleFont = new java.awt.Font("arial", Font.BOLD, 10);
	protected Font legendFont = new Font("arial", Font.BOLD, 9);
	public static Font axisFont = new Font("arial", Font.BOLD, 10);
	protected final InputParameters inputParameters;

	public AbstractMultipleChartsBySitePanel(List<GlycoSite> glycoSites, File resultsFolder,
			InputParameters inputParameters) {
		super(true);
		this.glycoSites = glycoSites;
		this.sumIntensitiesAcrossReplicates = inputParameters.isSumIntensitiesAcrossReplicates();
		this.resultsFolder = resultsFolder;
		this.inputParameters = inputParameters;
		initComponents();
	}

	protected void initComponents() {
		final int rows = glycoSites.size() / columns;
		final int width = columns * minChartSize + margin * (columns + 1);
		final int height = rows * minChartSize + margin * (rows + 1);
		setPreferredSize(new Dimension(width, height));
//		final GridLayout gl = new GridLayout(rows, columns, margin, margin);
		this.setLayout(new FlowLayout(FlowLayout.LEFT, margin, margin));
		for (final GlycoSite glycoSite : glycoSites) {
			final ChartPanel chartPanel = createChart(glycoSite);
			final JFreeChart chart = chartPanel.getChart();
			final Plot plot = chart.getPlot();
			plot.setBackgroundPaint(Color.white);
			setSeriesPaint(chartPanel, ColorsUtil.getDefaultColorsSortedByPTMCode());
			final Dimension dimension = new Dimension(minChartSize, minChartSize);
			chartPanel.setPreferredSize(dimension);
			chartPanel.setSize(dimension);
			chartPanel.setMinimumDrawWidth(minChartSize);
			chartPanel.setMinimumDrawHeight(minChartSize);

			// title
			chart.getTitle().setFont(titleFont);
			// subtitle
			// check if there is subtitle (index==1)
			if (chart.getSubtitleCount() > 1) {
				final TextTitle subtitle = (TextTitle) chart.getSubtitle(1);
				subtitle.setFont(subtitleFont);
			}

			// set font for legend
			final LegendTitle legend = chart.getLegend();
			legend.setItemFont(legendFont);

			// add to map of chartPanels by position
			chartsBySite.put(glycoSite, chartPanel);
			this.add(chartPanel);
		}
	}

	protected abstract ChartPanel createChart(GlycoSite glycoSite);

	protected abstract void setSeriesPaint(ChartPanel chart, Paint[] paints);

	protected abstract Dataset createDataset(GlycoSite glycoSite);

	@Override
	public void updateUI() {
		if (chartsBySite != null) {
			for (final ChartPanel chartPanel : chartsBySite.values()) {
				chartPanel.updateUI();
			}
		}
		super.updateUI();
	}

	protected abstract String getImageFileFullPath(GlycoSite site);

	public List<File> saveImages() {
		final List<File> ret = new ArrayList<File>();
		for (final GlycoSite site : this.chartsBySite.keySet()) {
			final ChartPanel chart = this.chartsBySite.get(site);
			final String fileName = getImageFileFullPath(site);
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

	protected String getChartTitle(GlycoSite glycoSite) {
		return "Site " + glycoSite.getPosition();
	}
}
