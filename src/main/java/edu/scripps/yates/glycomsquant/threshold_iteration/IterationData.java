package edu.scripps.yates.glycomsquant.threshold_iteration;

import java.util.List;
import java.util.Set;

import edu.scripps.yates.census.read.model.interfaces.QuantifiedPeptideInterface;
import edu.scripps.yates.glycomsquant.GlycoSite;
import edu.scripps.yates.glycomsquant.PTMCode;
import edu.scripps.yates.utilities.maths.Maths;
import gnu.trove.list.TDoubleList;
import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.TObjectDoubleMap;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectDoubleHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.set.hash.THashSet;

public class IterationData {

	private final TObjectDoubleMap<PTMCode> averagePercentageByPTMCode = new TObjectDoubleHashMap<PTMCode>();
	private final TObjectIntMap<PTMCode> numPeptidesByPTMCode = new TObjectIntHashMap<PTMCode>();
	private final int iterationNumber;
	private final int numPeptides;
	private final double intensityThreshold;
	private final TIntObjectMap<TObjectDoubleMap<PTMCode>> percentagesBySite = new TIntObjectHashMap<TObjectDoubleMap<PTMCode>>();
	private final TIntObjectMap<TObjectIntMap<PTMCode>> peptidesBySite = new TIntObjectHashMap<TObjectIntMap<PTMCode>>();
	private final TIntIntMap numPeptidesBySite = new TIntIntHashMap();

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
					final double sitePercentageByPTMCode = glycoSite.getPercentageByPTMCode(ptmCode,
							calculateProportionsByPeptidesFirst);
					toAverage.add(sitePercentageByPTMCode);
					if (!percentagesBySite.containsKey(glycoSite.getPosition())) {
						percentagesBySite.put(glycoSite.getPosition(), new TObjectDoubleHashMap<PTMCode>());
					}
					percentagesBySite.get(glycoSite.getPosition()).put(ptmCode, sitePercentageByPTMCode);

					final Set<QuantifiedPeptideInterface> sitePeptidesByPTMCode = glycoSite
							.getPeptidesByPTMCode(ptmCode);
					peptides.addAll(sitePeptidesByPTMCode);
					if (!peptidesBySite.containsKey(glycoSite.getPosition())) {
						peptidesBySite.put(glycoSite.getPosition(), new TObjectIntHashMap<PTMCode>());
					}
					peptidesBySite.get(glycoSite.getPosition()).put(ptmCode, sitePeptidesByPTMCode.size());
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
		// calculate the num peptides by sites
		if (glycoSites != null) {
			for (final GlycoSite glycoSite : glycoSites) {
				final Set<QuantifiedPeptideInterface> peptides = new THashSet<QuantifiedPeptideInterface>();
				for (final PTMCode ptmCode : PTMCode.values()) {
					peptides.addAll(glycoSite.getPeptidesByPTMCode(ptmCode));
				}
				this.numPeptidesBySite.put(glycoSite.getPosition(), peptides.size());
			}
		}
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

	public TObjectDoubleMap<PTMCode> getPercentagesBySite(int position) {
		if (this.percentagesBySite.containsKey(position)) {
			return this.percentagesBySite.get(position);
		}
		return null;
	}

	public double getPercentageBySiteAndPTMCode(int position, PTMCode code) {
		final TObjectDoubleMap<PTMCode> percentagesByPTMCode = getPercentagesBySite(position);
		if (percentagesByPTMCode != null && percentagesByPTMCode.containsKey(code)) {
			return percentagesByPTMCode.get(code);
		}
		return 0.0;
	}

	public TObjectIntMap<PTMCode> getPeptidesBySite(int position) {
		if (this.peptidesBySite.containsKey(position)) {
			return this.peptidesBySite.get(position);
		}
		return null;
	}

	public int getNumPeptidesBySiteAndPTMCode(int position, PTMCode ptmCode) {
		final TObjectIntMap<PTMCode> peptidesByPTMCode = getPeptidesBySite(position);
		if (peptidesByPTMCode != null && peptidesByPTMCode.containsKey(ptmCode)) {
			return peptidesByPTMCode.get(ptmCode);
		}
		return 0;
	}

	public int getNumPeptidesBySite(int position) {
		if (this.numPeptidesBySite.containsKey(position)) {
			return this.numPeptidesBySite.get(position);
		}
		return 0;

	}
}
