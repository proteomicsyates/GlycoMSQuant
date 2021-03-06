package edu.scripps.yates.glycomsquant.gui;

import java.awt.Paint;
import java.io.File;
import java.util.Collection;
import java.util.List;

import org.apache.log4j.Logger;
import org.jfree.chart.ChartPanel;
import org.jfree.data.general.Dataset;

import edu.scripps.yates.glycomsquant.GlycoSite;
import edu.scripps.yates.glycomsquant.GroupedQuantifiedPeptide;
import edu.scripps.yates.glycomsquant.InputParameters;
import edu.scripps.yates.glycomsquant.gui.charts.ChartUtils;
import edu.scripps.yates.glycomsquant.gui.files.FileManager;
import edu.scripps.yates.glycomsquant.util.GlycoPTMAnalyzerUtil;

public class ProportionsPeptidesScatterPlotChartsPanel extends AbstractMultipleChartsBySitePanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 9209240955358988792L;
	private static final Logger log = Logger.getLogger(ProportionsPeptidesScatterPlotChartsPanel.class);

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
		final Collection<GroupedQuantifiedPeptide> coveredPeptides = glycoSite.getCoveredGroupedPeptides();
		final int numMeasurements = GlycoPTMAnalyzerUtil.getNumIndividualIntensities(coveredPeptides);
		final String subtitle = glycoSite.getTotalSPC()
				+ " SPC / " + glycoSite.getTotalNumPeptides() + " Peps / " + numMeasurements + " Meas";
		final String chartTitle = super.getChartTitle(glycoSite);
		return ChartUtils.createScatterPlotChartForGlycoSite(
				glycoSite, sumIntensitiesAcrossReplicates, chartTitle, subtitle,
				null, null);

	}

	@Override
	protected void setSeriesPaint(ChartPanel chart, Paint[] paints) {
		// do nothing...the colours are set in a different way at createChart method.
	}

}
