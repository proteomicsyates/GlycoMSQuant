package edu.scripps.yates.glycomsquant.gui;

import java.awt.Paint;
import java.io.File;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.List;

import org.apache.log4j.Logger;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.labels.StandardPieSectionLabelGenerator;
import org.jfree.chart.plot.PiePlot;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.general.PieDataset;

import edu.scripps.yates.glycomsquant.GlycoSite;
import edu.scripps.yates.glycomsquant.InputParameters;
import edu.scripps.yates.glycomsquant.PTMCode;
import edu.scripps.yates.glycomsquant.gui.charts.PieChart;
import edu.scripps.yates.glycomsquant.gui.files.FileManager;

public class ProportionsPieChartsPanel extends AbstractProportionsChartsPanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5712566281269329202L;
	private static final Logger log = Logger.getLogger(ProportionsPieChartsPanel.class);

	public ProportionsPieChartsPanel(List<GlycoSite> glycoSites, File resultsFolder, InputParameters inputParameters) {
		super(glycoSites, resultsFolder, inputParameters);

	}

	@Override
	protected ChartPanel createChart(GlycoSite glycoSite) {
		final PieDataset dataset = createDataset(glycoSite);
		final PieChart chartPanel = new PieChart(super.getChartTitle(glycoSite),
				glycoSite.getTotalSPC() + " SPC / " + glycoSite.getTotalNumPeptides() + " Peptides", dataset, false);

		final PiePlot plot = (PiePlot) chartPanel.getChart().getPlot();
		plot.setLabelGenerator(new StandardPieSectionLabelGenerator("{2}", NumberFormat.getIntegerInstance(),
				new DecimalFormat("#.# %")));
		plot.setLabelFont(itemsFont);

		return chartPanel.getChartPanel();
	}

	@Override
	protected void setSeriesPaint(ChartPanel chart, Paint[] paints) {
		final PiePlot plot = (PiePlot) chart.getChart().getPlot();

		for (int series = 0; series < paints.length; series++) {
			final Paint paint = paints[series];
			final String key = GuiUtils.translateCode(PTMCode.values()[series].getCode());
			plot.setSectionPaint(key, paint);
		}
	}

	@Override
	protected PieDataset createDataset(GlycoSite glycoSite) {
		final DefaultPieDataset dataset = new DefaultPieDataset();
		for (final PTMCode ptmCode : PTMCode.values()) {
			final double percetange = glycoSite.getPercentageByPTMCode(ptmCode, calculatePeptideProportionsFirst);
			dataset.setValue(GuiUtils.translateCode(ptmCode.getCode()), percetange);
		}
		return dataset;
	}

	@Override
	protected String getImageFileFullPath(GlycoSite site) {
		return FileManager.getGraphFileNameForPieChart(resultsFolder, inputParameters, site);
	}
}
