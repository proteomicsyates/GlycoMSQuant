package edu.scripps.yates.glycomsquant.gui.tasks;

import java.util.List;

import javax.swing.SwingWorker;

import edu.scripps.yates.glycomsquant.GlycoSite;
import edu.scripps.yates.glycomsquant.threshold_iteration.IterationData;
import edu.scripps.yates.glycomsquant.threshold_iteration.IterationGraphPanel;

public class IterationGraphGenerator extends SwingWorker<Void, Void> {
	public static final String GRAPH_GENERATED = "IterativeThresholdAnalysis graph generated";
	public static final String ITERATIVE_ANALYSIS_ERROR = "Iterative analysis graph error";

	private List<IterationData> iterationsData = null;
	private final List<GlycoSite> glycoSites;

	public IterationGraphGenerator(List<IterationData> iterationsData, List<GlycoSite> glycoSites) {
		this.iterationsData = iterationsData;
		this.glycoSites = glycoSites;
	}

	@Override
	protected Void doInBackground() throws Exception {
		try {
			final IterationGraphPanel iterationGraphPanel = new IterationGraphPanel(iterationsData, glycoSites);
			firePropertyChange(GRAPH_GENERATED, null, iterationGraphPanel);

		} catch (final Exception e) {
			e.printStackTrace();
			firePropertyChange(ITERATIVE_ANALYSIS_ERROR, null, e.getMessage());
		}
		return null;
	}

}
