package edu.scripps.yates.glycomsquant;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import edu.scripps.yates.census.read.model.interfaces.QuantParser;
import edu.scripps.yates.census.read.model.interfaces.QuantifiedPSMInterface;
import edu.scripps.yates.census.read.model.interfaces.QuantifiedPeptideInterface;
import edu.scripps.yates.census.read.util.QuantKeyUtils;
import edu.scripps.yates.glycomsquant.gui.MainFrame;
import edu.scripps.yates.utilities.maths.Maths;
import edu.scripps.yates.utilities.proteomicsmodel.Amount;
import gnu.trove.list.TDoubleList;
import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.map.TObjectDoubleMap;
import gnu.trove.map.hash.THashMap;
import gnu.trove.map.hash.TObjectDoubleHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.THashSet;
import gnu.trove.set.hash.TIntHashSet;

public class GlycoSite {
	private final static Logger log = Logger.getLogger(GlycoSite.class);
	public static final String GLYCOSITE = "GLYCOSITE";
	private final int position;
	private final Map<PTMCode, TDoubleList> peptideIntensitiesByPTMCode = new THashMap<PTMCode, TDoubleList>();
	private final Map<PTMCode, List<QuantifiedPeptideInterface>> peptidesByPTMCode = new THashMap<PTMCode, List<QuantifiedPeptideInterface>>();
	// there will be multiple peptides from the same sequence, but different ptms on
	// them
	private final Map<String, List<QuantifiedPeptideInterface>> peptidesByNoPTMPeptideKey = new THashMap<String, List<QuantifiedPeptideInterface>>();
	private final TObjectDoubleMap<PTMCode> avgPercentageBySecondMethodByPTMCode = new TObjectDoubleHashMap<PTMCode>();
	private final TObjectDoubleMap<PTMCode> semPercentageBySecondMethodByPTMCode = new TObjectDoubleHashMap<PTMCode>();
	private final TObjectDoubleMap<PTMCode> stdPercentageBySecondMethodByPTMCode = new TObjectDoubleHashMap<PTMCode>();
	private final String protein;
	private Integer totalSPC;
	private final Set<String> replicates = new THashSet<String>();

	public GlycoSite(int position, String protein) {
		super();
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
						final QuantifiedPeptideInterface peptide = quantParser.getPeptideMap().get(peptideKey);
						ret.addValue(ptmCode, intensity, peptide);
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
		}

		peptideIntensitiesByPTMCode.get(ptmCode).add(value);
//		if (!peptidesByPTMCode.get(ptmCode).contains(peptide)) {
		peptidesByPTMCode.get(ptmCode).add(peptide);
//		}
		final QuantifiedPSMInterface aPSM = peptide.getQuantifiedPSMs().iterator().next();
		// group peptides by their sequence + peptide, but not PTMs
		final boolean distinguishModifiedPeptides = false;
		final boolean chargeStateSensible = MainFrame.getInstance().isChargeStateSensible();
		final String peptideKey = QuantKeyUtils.getInstance().getSequenceChargeKey(aPSM, false, chargeStateSensible);
		final String key = peptide.getKey();
		if (!peptidesByNoPTMPeptideKey.containsKey(peptideKey)) {
			peptidesByNoPTMPeptideKey.put(peptideKey, new ArrayList<QuantifiedPeptideInterface>());
		}
		final boolean added = peptidesByNoPTMPeptideKey.get(peptideKey).add(peptide);

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

	public TDoubleList getPeptideIntensitiesByPTMCode(PTMCode ptmCode) {
		return peptideIntensitiesByPTMCode.get(ptmCode);
	}

	public Map<PTMCode, TDoubleList> getPeptideIntensitiesByPTMCode() {
		return this.peptideIntensitiesByPTMCode;
	}

	@Override
	public String toString() {
		return "HIVPosition [position=" + position + ", value=" + getValuesString() + "]";
	}

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
	public double getPercentageBySecondMethod(PTMCode ptmCode) {

		if (!avgPercentageBySecondMethodByPTMCode.containsKey(ptmCode)) {

			final THashMap<PTMCode, TDoubleList> percentagesToAverage = new THashMap<PTMCode, TDoubleList>();
			for (final String peptideKey : peptidesByNoPTMPeptideKey.keySet()) {
				final THashMap<PTMCode, TDoubleList> percentagesToAverageInReplicates = new THashMap<PTMCode, TDoubleList>();
				for (final String replicate : replicates) {
					if (this.getPosition() == 52) {
						log.info("asdf");
					}

					// for each peptide, in each replicate, we calculate the percentages for each
					// PTMCode
					final List<QuantifiedPeptideInterface> peptides = peptidesByNoPTMPeptideKey.get(peptideKey);
					// these peptides have the same sequence+charge, but different PTMs
					final TObjectDoubleMap<PTMCode> intensityByPTMCode = getSignalPerPTMCodeInReplicate(peptides,
							replicate);
					final TObjectDoubleMap<PTMCode> percentageByPTMCode = getPercentages(intensityByPTMCode);

					for (final PTMCode ptmCode2 : percentageByPTMCode.keySet()) {
						if (!percentagesToAverageInReplicates.containsKey(ptmCode2)) {
							percentagesToAverageInReplicates.put(ptmCode2, new TDoubleArrayList());
						}
						percentagesToAverageInReplicates.get(ptmCode2).add(percentageByPTMCode.get(ptmCode2));
					}
				}
				for (final PTMCode ptmCode2 : PTMCode.values()) {
					if (!percentagesToAverageInReplicates.containsKey(ptmCode2)) {
						continue;
					}
					if (!percentagesToAverage.containsKey(ptmCode2)) {
						percentagesToAverage.put(ptmCode2, new TDoubleArrayList());
					}
					percentagesToAverage.get(ptmCode2).add(Maths.mean(percentagesToAverageInReplicates.get(ptmCode2)));
				}
			}
			for (final PTMCode ptmCode2 : PTMCode.values()) {
				avgPercentageBySecondMethodByPTMCode.put(ptmCode2, Maths.mean(percentagesToAverage.get(ptmCode2)));
				stdPercentageBySecondMethodByPTMCode.put(ptmCode2, Maths.stddev(percentagesToAverage.get(ptmCode2)));
				semPercentageBySecondMethodByPTMCode.put(ptmCode2, Maths.sem(percentagesToAverage.get(ptmCode2)));
			}

		}
		return avgPercentageBySecondMethodByPTMCode.get(ptmCode);
	}

	private TObjectDoubleMap<PTMCode> getPercentages(TObjectDoubleMap<PTMCode> intensityByPTMCode) {
		final TObjectDoubleMap<PTMCode> ret = new TObjectDoubleHashMap<PTMCode>();
		if (intensityByPTMCode.isEmpty()) {
			return ret;
		}
		final double sum = Maths.sum(intensityByPTMCode.values());
		for (final PTMCode ptmCode : PTMCode.values()) {
			final double percentage = intensityByPTMCode.get(ptmCode) / sum;
			ret.put(ptmCode, percentage);
		}
		return ret;
	}

	/**
	 * 
	 * @param peptides  should have the same sequence+charge, they may have
	 *                  different PTMs
	 * @param replicate
	 * @return
	 */
	private TObjectDoubleMap<PTMCode> getSignalPerPTMCodeInReplicate(List<QuantifiedPeptideInterface> peptides,
			String replicate) {
		final TObjectDoubleMap<PTMCode> ret = new TObjectDoubleHashMap<PTMCode>();
		for (final PTMCode ptmCode : PTMCode.values()) {
			final TDoubleList valuesPerPTMCode = new TDoubleArrayList();
			// reduce list to have unique hashcodes
			final TIntSet hashCodes = new TIntHashSet();
			for (final QuantifiedPeptideInterface peptide : peptides) {
				if (hashCodes.contains(peptide.hashCode())) {
					continue;
				}
				hashCodes.add(peptide.hashCode());
				if (peptidesByPTMCode.containsKey(ptmCode) && peptidesByPTMCode.get(ptmCode).contains(peptide)) {
					final double intensityFromPeptideInReplicate = getIntensityFromPeptideInReplicate(peptide,
							replicate);
					// if it is NaN means that the peptide was not detected in that replicate
					if (!Double.isNaN(intensityFromPeptideInReplicate)) {
						valuesPerPTMCode.add(intensityFromPeptideInReplicate);
					}
				}
			}
			// MEAN
			if (!valuesPerPTMCode.isEmpty()) {
				if (valuesPerPTMCode.size() > 1) {
					log.info("asddf");
				}
				ret.put(ptmCode, Maths.mean(valuesPerPTMCode));
			}
			// TODO it could be summing
//			ret.put(ptmCode, valuesPerPTMCode.sum());
		}
		return ret;
	}

	private double getIntensityFromPeptideInReplicate(QuantifiedPeptideInterface peptide, String replicate) {
		final TDoubleList values = new TDoubleArrayList();
		for (final Amount amount : peptide.getAmounts()) {
			if (!amount.getCondition().getName().equals(replicate)) {
				continue;
			}
			if (amount.getAmountType() == CurrentInputParameters.getInstance().getInputParameters().getAmountType()) {

				if (Double.compare(amount.getValue(), 0.0) != 0) {
					values.add(amount.getValue());
				}
			}
		}
		if (values.size() > 1) {
			throw new IllegalArgumentException(
					"There should be only 1 intensity, because is from one peptide in one replicate");
		}
		if (values.isEmpty()) {
			return Double.NaN;
		}
		return values.get(0);
	}

	/**
	 * Gets the intensity from a peptide. The peptide is present in several
	 * replicates and therefore will have multiple ratios with the intensities of
	 * each replicate. In that case, the intensities will be averaged.
	 * 
	 * @param peptide
	 * @return
	 */
	private double getAvgIntensityFromPeptideAcrossReplicates(QuantifiedPeptideInterface peptide) {
		final TDoubleList values = new TDoubleArrayList();
		for (final Amount amount : peptide.getAmounts()) {
			if (amount.getAmountType() == CurrentInputParameters.getInstance().getInputParameters().getAmountType()) {
				if (Double.compare(amount.getValue(), 0.0) != 0) {
					values.add(amount.getValue());
				}
			}
		}
		return Maths.mean(values);
	}

	public double getPercentageByPTMCode(PTMCode ptmCode, boolean calculateProportionsByPeptidesFirst) {
		if (calculateProportionsByPeptidesFirst) {
			return getPercentageBySecondMethod(ptmCode);
		} else {
			return getPercentageOfAveragesByPTMCode(ptmCode);
		}
	}

	public double getSTDEVPercentageByPTMCode(PTMCode ptmCode) {

		getPercentageBySecondMethod(ptmCode);
		return this.stdPercentageBySecondMethodByPTMCode.get(ptmCode);
	}

	public double getSEMPercentageByPTMCode(PTMCode ptmCode) {

		getPercentageBySecondMethod(ptmCode);
		return this.semPercentageBySecondMethodByPTMCode.get(ptmCode);
	}

	public double getPercentageOfAveragesByPTMCode(PTMCode ptmCode) {
		double averagePTMCodeOfInterest = 0.0;
		final TDoubleList averages = new TDoubleArrayList();
		for (final PTMCode ptmCode2 : PTMCode.values()) {
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

	public int getNumPeptidesByPTMCode(PTMCode ptmCode) {
		if (!peptidesByPTMCode.containsKey(ptmCode)) {
			return 0;
		}
		final List<QuantifiedPeptideInterface> peptides = peptidesByPTMCode.get(ptmCode);
		return peptides.size();

	}

	public String getProtein() {
		return protein;
	}

	public int getTotalNumPeptides() {
		return peptidesByNoPTMPeptideKey.size();
	}

	public int getTotalSPC() {
		if (totalSPC == null) {
			final Set<QuantifiedPSMInterface> psms = new THashSet<QuantifiedPSMInterface>();
			for (final PTMCode ptmCode : PTMCode.values()) {
				final List<QuantifiedPeptideInterface> peptides = peptidesByPTMCode.get(ptmCode);
				if (peptides != null) {
					peptides.stream().forEach(p -> psms.addAll(p.getQuantifiedPSMs()));
				}
			}
			totalSPC = psms.size();
		}
		return totalSPC;
	}
}
