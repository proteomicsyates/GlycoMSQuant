package edu.scripps.yates.glycomsquant.threshold_iteration;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import edu.scripps.yates.glycomsquant.GlycoSite;

public class IterativeThresholdAnalysis {
	private final static Logger log = Logger.getLogger(IterativeThresholdAnalysis.class);
	public static int MAX_ITERATIONS = 20;
	private double currentThreshold;
	private final double thresholdFactor;
	private int iterationNumber = 0;
	private final boolean calculateProportionsByPeptidesFirst;
	private final List<IterationData> iterationsData = new ArrayList<IterationData>();
	private final boolean hasErrors = false;

	public IterativeThresholdAnalysis(double initialIntensityThreshold, double intensityThresholdFactor,
			boolean calculateProportionsByPeptidesFirst) {
		this.currentThreshold = initialIntensityThreshold;
		this.thresholdFactor = intensityThresholdFactor;
		this.calculateProportionsByPeptidesFirst = calculateProportionsByPeptidesFirst;
	}

	/**
	 * Returns true if MAX_ITERATIONS has reached or if the number of peptides that
	 * pass the threshold is zero.
	 * 
	 * @return
	 */
	public boolean isLastIteration() {
		if (iterationNumber == MAX_ITERATIONS) {
			return true;
		}
		if (!iterationsData.isEmpty() && iterationsData.get(iterationsData.size() - 1).getNumPeptides() <= 0) {
			return true;
		}
		return false;
	}

	/**
	 * Gets the next intensity threshold
	 * 
	 * @return
	 */
	public double getNextThreshold() {
		return Math.max(this.currentThreshold, 1.0) * thresholdFactor;
	}

	public synchronized void addIterationData(double intensityThreshold, List<GlycoSite> glycoSites) {
		this.currentThreshold = intensityThreshold;
		final IterationData iterationData = new IterationData(++iterationNumber, intensityThreshold, glycoSites,
				calculateProportionsByPeptidesFirst);
		this.iterationsData.add(iterationData);
	}

	public boolean hasErrors() {

		return hasErrors;
	}

	public List<IterationData> getIterationsData() {
		return this.iterationsData;
	}

}
