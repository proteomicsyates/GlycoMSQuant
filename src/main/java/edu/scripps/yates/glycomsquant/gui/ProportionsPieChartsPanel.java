package edu.scripps.yates.glycomsquant.gui;

import java.awt.Paint;
import java.io.File;
import java.util.Collection;
import java.util.List;

import org.apache.log4j.Logger;
import org.jfree.chart.ChartPanel;
import org.jfree.data.general.PieDataset;

import edu.scripps.yates.glycomsquant.GlycoSite;
import edu.scripps.yates.glycomsquant.GroupedQuantifiedPeptide;
import edu.scripps.yates.glycomsquant.InputParameters;
import edu.scripps.yates.glycomsquant.gui.charts.ChartUtils;
import edu.scripps.yates.glycomsquant.gui.files.FileManager;
import edu.scripps.yates.glycomsquant.util.GlycoPTMAnalyzerUtil;

public class ProportionsPieChartsPanel extends AbstractMultipleChartsBySitePanel {

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
		final String title = getChartTitle(glycoSite);
		final Collection<GroupedQuantifiedPeptide> coveredPeptides = glycoSite.getCoveredGroupedPeptides();
		final int numMeasurements = GlycoPTMAnalyzerUtil.getNumIndividualIntensities(coveredPeptides);
		final String subtitle = glycoSite.getTotalSPC() + " SPC / " + glycoSite.getTotalNumPeptides() + " Peps / "
				+ numMeasurements + " Meas";
		return ChartUtils.createProportionsPieChartForGlycoSite(glycoSite, title, subtitle,
				sumIntensitiesAcrossReplicates, null, null);

	}

	@Override
	protected void setSeriesPaint(ChartPanel chart, Paint[] paints) {
		// do nothign is already done in the GraphUtils
	}

	@Override
	protected PieDataset createDataset(GlycoSite glycoSite) {
		return ChartUtils.createProportionsPieDatasetForGlycoSite(glycoSite, sumIntensitiesAcrossReplicates);

	}

	@Override
	protected String getImageFileFullPath(GlycoSite site) {
		return FileManager.getGraphFileNameForPieChart(resultsFolder, inputParameters, site);
	}
}
