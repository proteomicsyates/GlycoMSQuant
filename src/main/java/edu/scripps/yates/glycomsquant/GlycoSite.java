package edu.scripps.yates.glycomsquant;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import edu.scripps.yates.census.read.model.interfaces.QuantParser;
import edu.scripps.yates.census.read.model.interfaces.QuantifiedPSMInterface;
import edu.scripps.yates.census.read.model.interfaces.QuantifiedPeptideInterface;
import edu.scripps.yates.glycomsquant.util.GlycoPTMAnalyzerUtil;
import edu.scripps.yates.utilities.maths.Maths;
import edu.scripps.yates.utilities.proteomicsmodel.Amount;
import gnu.trove.list.TDoubleList;
import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.map.hash.THashMap;
import gnu.trove.set.hash.THashSet;

public class GlycoSite {
	private final static Logger log = Logger.getLogger(GlycoSite.class);
	public static final String GLYCOSITE = "GLYCOSITE";
	private final int position;

	// these 2 maps have list on the values because we need to keep the order and
	// the list of peptides is going to be redundant because one peptide will have
	// multiple intensities (from multiple replicates for example)
	private final Map<PTMCode, TDoubleList> peptideIntensitiesByPTMCode = new THashMap<PTMCode, TDoubleList>();
	private final Map<PTMCode, List<QuantifiedPeptideInterface>> peptidesByPTMCode = new THashMap<PTMCode, List<QuantifiedPeptideInterface>>();

	private final Map<PTMCode, Set<QuantifiedPeptideInterface>> nonRedundantPeptidesByPTMCode = new THashMap<PTMCode, Set<QuantifiedPeptideInterface>>();
	private final Set<QuantifiedPeptideInterface> coveredPeptides = new THashSet<QuantifiedPeptideInterface>();

	public Set<QuantifiedPeptideInterface> getCoveredPeptides() {
		return coveredPeptides;
	}

	// there will be multiple peptides from the same sequence, but different ptms on
	// them
	private final Map<String, GroupedQuantifiedPeptide> peptidesByNoPTMPeptideKey = new THashMap<String, GroupedQuantifiedPeptide>();
	private final Map<PTMCode, TDoubleList> percentageBySecondMethodByPTMCode = new THashMap<PTMCode, TDoubleList>();
	private final String protein;
	private Integer totalSPC;
	private final Set<String> replicates = new THashSet<String>();

	public GlycoSite(int position, String protein) {
		super();
		if (position == 167) {
			log.info("asdf");
		}
		this.position = position;
		this.protein = protein;

	}

	public String printOut() {
		final StringBuilder sb = new StringBuilder();
		sb.append(GLYCOSITE + "\t" + position + "\t" + protein + "\n");
		for (final PTMCode ptmCode : PTMCode.values()) {
			sb.append(ptmCode.getCode() + "\t");
			if (peptideIntensitiesByPTMCode.containsKey(ptmCode)) {
				final TDoubleList peptideIntensities = peptideIntensitiesByPTMCode.get(ptmCode);
				final List<QuantifiedPeptideInterface> peptides = peptidesByPTMCode.get(ptmCode);
				for (int i = 0; i < peptideIntensities.size(); i++) {
					final double intensity = peptideIntensities.get(i);
					final QuantifiedPeptideInterface peptide = peptides.get(i);
					sb.append(intensity + "\t" + peptide.getKey() + "\t");
				}
			}
			sb.append("\n");
		}
		return sb.toString();
	}

	public static GlycoSite readGlycoSiteFromString(String string, QuantParser quantParser) {
		final String[] lines = string.split("\n");
		GlycoSite ret = null;
		// first line has to start with GLYCOSITE
		if (!lines[0].startsWith(GLYCOSITE)) {
			throw new IllegalArgumentException(string + " is not readable as a Glycosite");
		}
		try {
			final Map<String, QuantifiedPeptideInterface> peptideMap = quantParser.getPeptideMap();
			for (final String line : lines) {
				final String[] split = line.split("\t");
				if (ret == null && line.startsWith(GLYCOSITE)) {
					final int position = Integer.valueOf(split[1]);
					final String protein = split[2];
					ret = new GlycoSite(position, protein);
				} else {
					// first column is PTMCode
					final PTMCode ptmCode = PTMCode.getByValue(split[0]);
					if (ptmCode == null) {
						throw new IllegalArgumentException(line + " is not readable as a PTMCode line");
					}
					for (int i = 1; i < split.length; i = i + 2) {
						final double intensity = Double.valueOf(split[i]);
						final String peptideKey = split[i + 1];
						if (peptideMap.containsKey(peptideKey)) {
							final QuantifiedPeptideInterface peptide = peptideMap.get(peptideKey);
							ret.addValue(ptmCode, intensity, peptide);
						} else {
							log.error("asdf");
						}
					}
				}

			}
			return ret;
		} catch (final Exception e) {
			e.printStackTrace();
			throw new IllegalArgumentException(string + " is not readable as a Glycosite", e);
		}
	}

	/**
	 * 
	 * @param ptmCode
	 * @param value
	 * @param peptide
	 */
	public void addValue(PTMCode ptmCode, double value, QuantifiedPeptideInterface peptide) {
		if (!peptideIntensitiesByPTMCode.containsKey(ptmCode)) {
			peptideIntensitiesByPTMCode.put(ptmCode, new TDoubleArrayList());
			peptidesByPTMCode.put(ptmCode, new ArrayList<QuantifiedPeptideInterface>());
			nonRedundantPeptidesByPTMCode.put(ptmCode, new THashSet<QuantifiedPeptideInterface>());
		}

		peptideIntensitiesByPTMCode.get(ptmCode).add(value);
		peptidesByPTMCode.get(ptmCode).add(peptide);
		nonRedundantPeptidesByPTMCode.get(ptmCode).add(peptide);
		coveredPeptides.add(peptide);

		final String key = GlycoPTMAnalyzerUtil.getPeptideKey(peptide, true);
		if (!peptidesByNoPTMPeptideKey.containsKey(key)) {
			peptidesByNoPTMPeptideKey.put(key, new GroupedQuantifiedPeptide(peptide));
		}
		final boolean added = peptidesByNoPTMPeptideKey.get(key).add(peptide);

		// reset total SPC to force to calculate it
		totalSPC = null;
		// keep replicates
		replicates.addAll(getReplicatesFromPeptide(peptide));
	}

	private Set<String> getReplicatesFromPeptide(QuantifiedPeptideInterface peptide) {
		final Set<String> ret = new THashSet<String>();
		final Set<Amount> amounts = peptide.getAmounts();
		for (final Amount amount : amounts) {
			final String repName = amount.getCondition().getName();
			ret.add(repName);
		}
		return ret;
	}

	public int getPosition() {
		return position;
	}

	private TDoubleList getPeptideIntensitiesByPTMCode(PTMCode ptmCode) {
		return peptideIntensitiesByPTMCode.get(ptmCode);
	}

	private Map<PTMCode, TDoubleList> getPeptideIntensitiesByPTMCode() {
		return this.peptideIntensitiesByPTMCode;
	}

	@Override
	public String toString() {
		return "HIVPosition [position=" + position + ", value=" + getValuesString() + "]";
	}

	/**
	 * Get the average of the intensities of all peptides across all replicates for
	 * this {@link GlycoSite}
	 * 
	 * @param ptmCode
	 * @return
	 */
	public double getAverageIntensityByPTMCode(PTMCode ptmCode) {
		if (peptideIntensitiesByPTMCode.containsKey(ptmCode)) {
			return Maths.mean(peptideIntensitiesByPTMCode.get(ptmCode));
		}

		return 0.0;
	}

	public double getSTDEVIntensityByPTMCode(PTMCode ptmCode) {
		if (peptideIntensitiesByPTMCode.containsKey(ptmCode)) {
			return Maths.stddev(peptideIntensitiesByPTMCode.get(ptmCode));
		}
		return 0.0;
	}

	public double getSEMIntensityByPTMCode(PTMCode ptmCode) {

		if (peptideIntensitiesByPTMCode.containsKey(ptmCode)) {
			return Maths.sem(peptideIntensitiesByPTMCode.get(ptmCode));
		}

		return 0.0;
	}

	/**
	 * Calculates the Standard Error of Mean
	 * 
	 * @param ptmCode
	 * @return
	 */
	public double getPSMsSEMByPTMCode(PTMCode ptmCode) {
		if (peptideIntensitiesByPTMCode.containsKey(ptmCode)) {
			return Maths.sem(peptideIntensitiesByPTMCode.get(ptmCode));
		}
		return 0.0;
	}

	/**
	 * Calculates the percentage of {@link PTMCode} by first calculating the
	 * percentages at peptide level and then averaging all.
	 * 
	 * @param ptmCode
	 * @return
	 */
	private double getProportionsByPTMCodeByPeptidesFirstMethod(PTMCode ptmCode) {

		if (!percentageBySecondMethodByPTMCode.containsKey(ptmCode)) {
			if (this.getPosition() == 357) {
				log.info("asdf");
			}
			final THashMap<PTMCode, TDoubleList> percentagesOfPeptidesToAverage = new THashMap<PTMCode, TDoubleList>();
			for (final String peptideKey : peptidesByNoPTMPeptideKey.keySet()) {
				// for each peptide key we average all intensities and then calculate the
				// percentages
				final GroupedQuantifiedPeptide peptides = peptidesByNoPTMPeptideKey.get(peptideKey);

				final Map<PTMCode, TDoubleList> percentages = GlycoPTMAnalyzerUtil
						.getPercentagesByPTMCodeCalculatingPeptidesFirst(peptides);

				for (final PTMCode ptmCode2 : percentages.keySet()) {
					if (!percentagesOfPeptidesToAverage.containsKey(ptmCode2)) {
						percentagesOfPeptidesToAverage.put(ptmCode2, new TDoubleArrayList());
					}
					percentagesOfPeptidesToAverage.get(ptmCode2).addAll(percentages.get(ptmCode2));
				}

			}
			for (final PTMCode ptmCode2 : PTMCode.values()) {
				if (percentagesOfPeptidesToAverage.containsKey(ptmCode2)) {
					percentageBySecondMethodByPTMCode.put(ptmCode2, percentagesOfPeptidesToAverage.get(ptmCode2));
				}
			}
		}
		if (percentageBySecondMethodByPTMCode.containsKey(ptmCode)) {
			return Maths.mean(percentageBySecondMethodByPTMCode.get(ptmCode));
		}
		return 0.0;
	}

	public double getPercentageByPTMCode(PTMCode ptmCode, boolean calculateProportionsByPeptidesFirst) {
		if (calculateProportionsByPeptidesFirst) {
			return getProportionsByPTMCodeByPeptidesFirstMethod(ptmCode);
		} else {
			return getPercentageByPTMCodeByAverageIntensitiesFirstMethod(ptmCode);
		}
	}

	public TDoubleList getIndividualPeptidePercentagesByPTMCode(PTMCode ptmCode) {
		getProportionsByPTMCodeByPeptidesFirstMethod(ptmCode);
		return this.percentageBySecondMethodByPTMCode.get(ptmCode);
	}

	public double getSTDEVPercentageByPTMCode(PTMCode ptmCode) {

		getProportionsByPTMCodeByPeptidesFirstMethod(ptmCode);
		if (percentageBySecondMethodByPTMCode.containsKey(ptmCode)) {
			return Maths.stddev(this.percentageBySecondMethodByPTMCode.get(ptmCode));
		}
		return 0.0;
	}

	public double getSEMPercentageByPTMCode(PTMCode ptmCode) {

		getProportionsByPTMCodeByPeptidesFirstMethod(ptmCode);
		if (percentageBySecondMethodByPTMCode.containsKey(ptmCode)) {
			return Maths.sem(this.percentageBySecondMethodByPTMCode.get(ptmCode));
		}
		return 0.0;
	}

	private double getPercentageByPTMCodeByAverageIntensitiesFirstMethod(PTMCode ptmCode) {
		double averagePTMCodeOfInterest = 0.0;
		final TDoubleList averages = new TDoubleArrayList();
		for (final PTMCode ptmCode2 : PTMCode.values()) {
			// per peptide (sequence+charge), we average all the intensities across
			// replicates and we calculate the proportions with that
			final double averageByPTMCode = getAverageIntensityByPTMCode(ptmCode2);
			averages.add(averageByPTMCode);
			if (ptmCode2 == ptmCode) {
				averagePTMCodeOfInterest = averageByPTMCode;
			}
		}
		final double percentage = averagePTMCodeOfInterest / averages.sum();
		return percentage;
	}

	private String getValuesString() {
		final StringBuilder sb = new StringBuilder();
		for (final PTMCode ptmCode : PTMCode.values()) {
			sb.append("(" + ptmCode + ":" + getPercentageByPTMCode(ptmCode, true) + "%) ");
		}
		return sb.toString();
	}

	public int getSPCByPTMCode(PTMCode ptmCode) {
		if (!peptidesByPTMCode.containsKey(ptmCode)) {
			return 0;
		}
		final List<QuantifiedPeptideInterface> peptides = peptidesByPTMCode.get(ptmCode);
		final Set<QuantifiedPSMInterface> psms = new THashSet<QuantifiedPSMInterface>();
		if (peptides != null) {
			peptides.stream().forEach(p -> psms.addAll(p.getQuantifiedPSMs()));
		}
		return psms.size();
	}

	public Set<QuantifiedPeptideInterface> getPeptidesByPTMCode(PTMCode ptmCode) {
		if (!nonRedundantPeptidesByPTMCode.containsKey(ptmCode)) {
			return Collections.emptySet();
		}
		final Set<QuantifiedPeptideInterface> peptides = nonRedundantPeptidesByPTMCode.get(ptmCode);
		return peptides;

	}

	public String getProtein() {
		return protein;
	}

	public int getTotalNumPeptides() {
		return coveredPeptides.size();
	}

	public int getTotalSPC() {
		if (totalSPC == null) {
			final Set<QuantifiedPSMInterface> psms = new THashSet<QuantifiedPSMInterface>();

			coveredPeptides.stream().forEach(p -> psms.addAll(p.getQuantifiedPSMs()));

			totalSPC = psms.size();
		}
		return totalSPC;
	}

	public Collection<GroupedQuantifiedPeptide> getCoveredGroupedPeptides() {
		return GlycoPTMAnalyzerUtil.getGroupedPeptidesFromPeptides(getCoveredPeptides()).values();
	}
}
