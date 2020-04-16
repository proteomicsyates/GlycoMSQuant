package edu.scripps.yates.glycomsquant.threshold_iteration;

import java.util.List;
import java.util.Set;

import edu.scripps.yates.census.read.model.interfaces.QuantifiedPeptideInterface;
import edu.scripps.yates.glycomsquant.GlycoSite;
import edu.scripps.yates.glycomsquant.PTMCode;
import edu.scripps.yates.utilities.maths.Maths;
import gnu.trove.list.TDoubleList;
import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.map.TObjectDoubleMap;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectDoubleHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.set.hash.THashSet;

public class IterationData {

	private final TObjectDoubleMap<PTMCode> averagePercentageByPTMCode = new TObjectDoubleHashMap<PTMCode>();
	private final TObjectIntMap<PTMCode> numPeptidesByPTMCode = new TObjectIntHashMap<PTMCode>();
	private final int iterationNumber;
	private final int numPeptides;
	private final double intensityThreshold;

	public IterationData(int iterationNumber, double intensityThreshold, List<GlycoSite> glycoSites,
			boolean calculateProportionsByPeptidesFirst) {
		this.iterationNumber = iterationNumber;
		this.intensityThreshold = intensityThreshold;
		final Set<QuantifiedPeptideInterface> totalPeptides = new THashSet<QuantifiedPeptideInterface>();
		for (final PTMCode ptmCode : PTMCode.values()) {
			final Set<QuantifiedPeptideInterface> peptides = new THashSet<QuantifiedPeptideInterface>();
			final TDoubleList toAverage = new TDoubleArrayList();
			if (glycoSites != null) {
				for (final GlycoSite glycoSite : glycoSites) {
					toAverage.add(glycoSite.getPercentageByPTMCode(ptmCode, calculateProportionsByPeptidesFirst));
					peptides.addAll(glycoSite.getPeptidesByPTMCode(ptmCode));
				}
			}
			if (!toAverage.isEmpty()) {
				this.averagePercentageByPTMCode.put(ptmCode, Maths.mean(toAverage));
			} else {
				this.averagePercentageByPTMCode.put(ptmCode, 0.0);
			}
			this.numPeptidesByPTMCode.put(ptmCode, peptides.size());
			totalPeptides.addAll(peptides);
		}
		// get total peptides
		this.numPeptides = totalPeptides.size();
	}

	public int getNumPeptidesByPTMCode(PTMCode ptmCode) {
		if (this.numPeptidesByPTMCode.containsKey(ptmCode)) {
			return numPeptidesByPTMCode.get(ptmCode);
		}
		return 0;
	}

	public double getAveragePercentageByPTMCode(PTMCode ptmCode) {
		return this.averagePercentageByPTMCode.get(ptmCode);
	}

	public int getIterationNumber() {
		return iterationNumber;
	}

	public int getNumPeptides() {
		return numPeptides;
	}

	public double getIntensityThreshold() {
		return this.intensityThreshold;
	}
}
