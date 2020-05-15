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
	private final Map<PTMCode, TDoubleList> individualProportionsByPTMCode = new THashMap<PTMCode, TDoubleList>();
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
					final PTMCode ptmCode = PTMCode.getByValue(Double.valueOf(split[0]));
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
	 * @param intensity
	 * @param peptide
	 */
	public void addValue(PTMCode ptmCode, double intensity, QuantifiedPeptideInterface peptide) {
		if (!peptideIntensitiesByPTMCode.containsKey(ptmCode)) {
			peptideIntensitiesByPTMCode.put(ptmCode, new TDoubleArrayList());
			peptidesByPTMCode.put(ptmCode, new ArrayList<QuantifiedPeptideInterface>());
			nonRedundantPeptidesByPTMCode.put(ptmCode, new THashSet<QuantifiedPeptideInterface>());
		}

		peptideIntensitiesByPTMCode.get(ptmCode).add(intensity);
		peptidesByPTMCode.get(ptmCode).add(peptide);
		nonRedundantPeptidesByPTMCode.get(ptmCode).add(peptide);
		coveredPeptides.add(peptide);

		final String key = GlycoPTMAnalyzerUtil.getPeptideKey(peptide, true);
		if (!peptidesByNoPTMPeptideKey.containsKey(key)) {
			final int positionInPeptide = GlycoPTMAnalyzerUtil.getPositionInPeptide(peptide, protein, getPosition());
			peptidesByNoPTMPeptideKey.put(key, new GroupedQuantifiedPeptide(peptide, protein, positionInPeptide));
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

	public double getProportionByPTMCode(PTMCode ptmCode, boolean sumIntensitiesAcrossReplicates) {
		if (!individualProportionsByPTMCode.containsKey(ptmCode)) {
			individualProportionsByPTMCode.clear();
			if (this.getPosition() == 357) {
				log.info("asdf");
			}
			for (final String peptideKey : peptidesByNoPTMPeptideKey.keySet()) {

				final GroupedQuantifiedPeptide peptides = peptidesByNoPTMPeptideKey.get(peptideKey);
				final Map<PTMCode, TDoubleList> individualProportions = GlycoPTMAnalyzerUtil
						.getIndividualProportionsByPTMCode(peptides, sumIntensitiesAcrossReplicates);

				for (final PTMCode ptmCode2 : individualProportions.keySet()) {
					if (!individualProportionsByPTMCode.containsKey(ptmCode2)) {
						individualProportionsByPTMCode.put(ptmCode2, new TDoubleArrayList());
					}
					individualProportionsByPTMCode.get(ptmCode2).addAll(individualProportions.get(ptmCode2));
				}
			}
		}
		if (individualProportionsByPTMCode.containsKey(ptmCode)) {
			return Maths.mean(individualProportionsByPTMCode.get(ptmCode));
		}
		return 0.0;
	}

	public TDoubleList getIndividualPeptideProportionsByPTMCode(PTMCode ptmCode,
			boolean sumIntensitiesAcrossReplicates) {
		getProportionByPTMCode(ptmCode, sumIntensitiesAcrossReplicates);
		return this.individualProportionsByPTMCode.get(ptmCode);
	}

	public double getSEMOfProportionsByPTMCode(PTMCode ptmCode, boolean sumIntensitiesAcrossReplicates) {

		final TDoubleList individualPeptidePercentages = getIndividualPeptideProportionsByPTMCode(ptmCode,
				sumIntensitiesAcrossReplicates);
		if (individualPeptidePercentages != null && !individualPeptidePercentages.isEmpty()) {
			return Maths.sem(this.individualProportionsByPTMCode.get(ptmCode));
		}
		return 0.0;
	}

	private String getValuesString() {
		final StringBuilder sb = new StringBuilder();
		for (final PTMCode ptmCode : PTMCode.values()) {
			sb.append("(" + ptmCode + ":" + getProportionByPTMCode(ptmCode, true) + "%) ");
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
		return GlycoPTMAnalyzerUtil.getGroupedPeptidesFromPeptides(getCoveredPeptides(), protein, getPosition())
				.values();
	}
}
