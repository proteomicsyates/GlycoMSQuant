package edu.scripps.yates.glycomsquant.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import edu.scripps.yates.census.read.model.interfaces.QuantifiedPeptideInterface;
import edu.scripps.yates.glycomsquant.CurrentInputParameters;
import edu.scripps.yates.glycomsquant.GlycoSite;
import edu.scripps.yates.glycomsquant.GroupedQuantifiedPeptide;
import edu.scripps.yates.glycomsquant.PTMCode;
import edu.scripps.yates.glycomsquant.ProteinSequences;
import edu.scripps.yates.utilities.maths.Maths;
import edu.scripps.yates.utilities.proteomicsmodel.Amount;
import edu.scripps.yates.utilities.proteomicsmodel.PTM;
import edu.scripps.yates.utilities.proteomicsmodel.PTMSite;
import edu.scripps.yates.utilities.proteomicsmodel.Peptide;
import edu.scripps.yates.utilities.proteomicsmodel.utils.ModelUtils;
import edu.scripps.yates.utilities.sequence.PTMInPeptide;
import edu.scripps.yates.utilities.strings.StringUtils;
import gnu.trove.list.TDoubleList;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.TMap;
import gnu.trove.map.TObjectDoubleMap;
import gnu.trove.map.hash.THashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectDoubleHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.THashSet;
import gnu.trove.set.hash.TIntHashSet;

public class GlycoPTMAnalyzerUtil {
	private final static Map<String, Pattern> patternsByRegexp = new THashMap<String, Pattern>();

	/**
	 * for a peptide sequence belonging to the protein of interest, we extend the
	 * sequence in the c-terminal by 2 aminoacids so we can detect cases like:<br>
	 * PEPTIDEN.K that actually are PEPTIDEN.KT or <br>
	 * PEPTIDENK.T<br>
	 * It returns a list of extended sequences because the peptide may be in
	 * multiple positions in the protein
	 * 
	 * @param sequence
	 * @return list of extended sequences
	 */
	private static List<String> getExtendedSequences(String sequence, String proteinOfInterestSequence) {
		final List<String> ret = new ArrayList<String>();
		String extendedSequence = sequence;
		// only if the last or the previous to the last aminoacids are N
		if (sequence.charAt(sequence.length() - 1) == 'N' || sequence.charAt(sequence.length() - 2) == 'N') {
			final TIntArrayList positions = StringUtils.allPositionsOf(proteinOfInterestSequence, sequence);
			for (final int position : positions.toArray()) {
				final int lastPositionOfPeptideInProtein = position + sequence.length() - 1;
				if (lastPositionOfPeptideInProtein + 2 <= proteinOfInterestSequence.length()) {
					extendedSequence += "" + proteinOfInterestSequence.charAt(lastPositionOfPeptideInProtein + 1 - 1)
							+ proteinOfInterestSequence.charAt(lastPositionOfPeptideInProtein + 2 - 1);
					ret.add(extendedSequence);
				}
			}

		} else {
			ret.add(extendedSequence);
		}
		if (ret.isEmpty()) {
			throw new IllegalArgumentException("This cannot happen");
		}
		return ret;

	}

	public static TIntList hasMotif(Peptide peptide) {
		final String proteinAcc = peptide.getProteins().stream().map(protein -> protein.getAccession()).findAny().get();
		final String proteinSequence = ProteinSequences.getInstance().getProteinSequence(proteinAcc);
		return hasMotif(peptide, proteinSequence, ProteinSequences.getInstance().getMotifRegexp());
	}

	/**
	 * Returns the positions in the peptide sequence in which the motif is found
	 * 
	 * @param peptide
	 * @param proteinSequence used for extending the peptide sequence to see if the
	 *                        following AAs make the motif
	 * @param motifRegexp
	 * @return
	 */
	public static TIntList hasMotif(Peptide peptide, String proteinSequence, String motifRegexp) {
		// get the extended sequences. They can be more than one if the peptide is found
		// more than once in the protein sequence
		final List<String> extendedSequences = getExtendedSequences(peptide.getSequence(), proteinSequence);
		final TIntList ret = new TIntArrayList();
		for (final String extendedSequence : extendedSequences) {

			final Matcher matcher = getPattern(motifRegexp).matcher(extendedSequence);
			while (matcher.find()) {
				final int position = matcher.start() + 1;
				if (position > peptide.getSequence().length()) {
					continue;
				}
				ret.add(position);
			}
		}
		return ret;
	}

	private static Pattern getPattern(String motifRegexp) {
		if (!patternsByRegexp.containsKey(motifRegexp)) {
			patternsByRegexp.put(motifRegexp, Pattern.compile(motifRegexp));

		}
		return patternsByRegexp.get(motifRegexp);
	}

	/**
	 * Gets a sorted list of replicates in which the peptide has an amount > 0.0
	 * 
	 * @param peptide
	 * @return
	 */
	public static List<String> getReplicateNamesFromPeptide(QuantifiedPeptideInterface peptide) {
		if (peptide.getAmounts() == null) {
			return Collections.emptyList();
		}
		return peptide.getAmounts().stream().filter(am -> am.getValue() > 0.0).map(am -> am.getCondition().getName())
				.sorted().distinct().collect(Collectors.toList());
	}

	/**
	 * Gets a sorted list of replicates in which the peptide has an amount > 0.0
	 * 
	 * @param peptide
	 * @return
	 */
	public static List<String> getReplicateNamesFromPeptide(GroupedQuantifiedPeptide peptides) {
		final Set<String> set = new THashSet<String>();
		peptides.stream().forEach(peptide -> set.addAll(getReplicateNamesFromPeptide(peptide)));
		final List<String> list = new ArrayList<String>();
		list.addAll(set);
		Collections.sort(list);
		return list;
	}

	/**
	 * Gets a non redundant list of {@link QuantifiedPeptideInterface} that cover
	 * the list of {@link GlycoSite}
	 * 
	 * @param glycoSites
	 * @return
	 */
	public static List<QuantifiedPeptideInterface> getPeptidesFromSites(List<GlycoSite> glycoSites) {
		final Set<QuantifiedPeptideInterface> set = new THashSet<QuantifiedPeptideInterface>();
		glycoSites.stream().forEach(site -> set.addAll(site.getCoveredPeptides()));
		final List<QuantifiedPeptideInterface> ret = new ArrayList<QuantifiedPeptideInterface>();
		ret.addAll(set);
		return ret;
	}

	/**
	 * Gets the peptide key with no PTMs of interest but keeping the rest of the
	 * potential PTMs to calculate proportions per peptideKey
	 * 
	 * @param peptide
	 * @param useCharge althought the charge is used for grouping the peptides for
	 *                  sure, I may want to get the key without it for displaying it
	 *                  in a table
	 * @return
	 */
	public static String getPeptideKey(QuantifiedPeptideInterface peptide, boolean useCharge) {
		// group peptides by their sequence + peptide, but not PTMs

		// we use the peptideKey with no PTMs of interest but keeping the rest of
		// potential PTMs to calculate proportions per peptideKey
		final String peptideSequence = peptide.getSequence();
		final List<PTM> ptmsForKey = new ArrayList<PTM>();
		if (peptide.getPTMs() != null) {
			for (final PTM ptm : peptide.getPTMs()) {
				final String ptmCodeString = String.valueOf(ptm.getMassShift());
				final PTMCode ptmCodeObj = PTMCode.getByValue(ptmCodeString);
				if (ptmCodeObj != null) {
					continue; // we dont include it in the key
				}
				ptmsForKey.add(ptm);
			}
		}
		final TIntObjectMap<PTM> ptmsByPosition = new TIntObjectHashMap<PTM>();
		for (final PTM ptm : ptmsForKey) {
			for (final PTMSite site : ptm.getPTMSites()) {
				ptmsByPosition.put(site.getPosition(), ptm);
			}
		}
		final StringBuilder peptideKeyBuilder = new StringBuilder();
		for (int position = 1; position <= peptideSequence.length(); position++) {
			peptideKeyBuilder.append(peptideSequence.charAt(position - 1));
			if (ptmsByPosition.containsKey(position)) {
				peptideKeyBuilder.append(
						"(" + ModelUtils.getPtmFormatter().format(ptmsByPosition.get(position).getMassShift()) + ")");

			}
		}
		if (useCharge) {
			// add the charge
			peptideKeyBuilder.append("-" + peptide.getPSMs().get(0).getChargeState());

		}
		final String key = peptideKeyBuilder.toString();
		return key;
	}

	public static Map<PTMCode, TDoubleList> getPercentagesByPTMCodeCalculatingPeptidesFirst(
			GroupedQuantifiedPeptide peptides) {
		// these peptides have the same sequence+charge, but different PTMs
		final Map<PTMCode, TDoubleList> ret = new THashMap<PTMCode, TDoubleList>();

		final List<String> replicates = getReplicateNamesFromPeptide(peptides);
		for (final String replicate : replicates) {

			// for each peptide, in each replicate, we calculate the percentages for each
			// PTMCode

			final Map<PTMCode, TDoubleList> peptideIntensitiesByPTMCodeInReplicate = getIntensitiesPerPTMCodeInReplicate(
					peptides, replicate);
			final TObjectDoubleMap<PTMCode> avgPeptideIntensityInReplicateByPTMCode = new TObjectDoubleHashMap<PTMCode>();
			for (final PTMCode ptmCode2 : peptideIntensitiesByPTMCodeInReplicate.keySet()) {
				final double avgIntensityOfPeptide = Maths.mean(peptideIntensitiesByPTMCodeInReplicate.get(ptmCode2));
				avgPeptideIntensityInReplicateByPTMCode.put(ptmCode2, avgIntensityOfPeptide);
			}
			final TObjectDoubleMap<PTMCode> peptidePercentagesInReplicateByPTMCode = getPercentages(
					avgPeptideIntensityInReplicateByPTMCode);

			for (final PTMCode ptmCode2 : peptidePercentagesInReplicateByPTMCode.keySet()) {
				if (!ret.containsKey(ptmCode2)) {
					ret.put(ptmCode2, new TDoubleArrayList());
				}
				ret.get(ptmCode2).add(peptidePercentagesInReplicateByPTMCode.get(ptmCode2));
			}
		}

		return ret;
	}

	public static int getNumIndividualPeptideMeasurements(Collection<GroupedQuantifiedPeptide> peptides,
			boolean calculateProportionsByPeptidesFirst) {
		int ret = -Integer.MAX_VALUE;
		for (final PTMCode ptmCode : PTMCode.values()) {
			final int num = getNumIndividualPeptideMeasurements(ptmCode, peptides, calculateProportionsByPeptidesFirst);
			if (num > ret) {
				ret = num;
			}
		}
		return ret;

	}

	public static int getNumIndividualPeptideMeasurements(PTMCode ptmCode,
			Collection<GroupedQuantifiedPeptide> peptides, boolean calculateProportionsByPeptidesFirst) {
		if (!calculateProportionsByPeptidesFirst) {
			return peptides.size();
		} else {
			int num = 0;
			for (final GroupedQuantifiedPeptide peptide : peptides) {
				num += GlycoPTMAnalyzerUtil.getPercentagesByPTMCodeCalculatingPeptidesFirst(peptide).get(ptmCode)
						.size();
			}
			return num;
		}
	}

	public static TObjectDoubleMap<PTMCode> getPercentagesByPTMCode(GroupedQuantifiedPeptide peptides,
			boolean calculateProportionsByPeptidesFirst) {
		if (calculateProportionsByPeptidesFirst) {
			final Map<PTMCode, TDoubleList> percentagesByPTM = getPercentagesByPTMCodeCalculatingPeptidesFirst(
					peptides);
			final TObjectDoubleMap<PTMCode> ret = new TObjectDoubleHashMap<PTMCode>();
			for (final PTMCode ptmCode : percentagesByPTM.keySet()) {
				ret.put(ptmCode, Maths.mean(percentagesByPTM.get(ptmCode)));
			}
			return ret;
		} else {
			return getPercentagesByPTMByAveragingIntensitiesFirst(peptides);
		}
	}

	public static TObjectDoubleMap<PTMCode> getPercentagesByPTMByAveragingIntensitiesFirst(
			GroupedQuantifiedPeptide peptides) {
		final TObjectDoubleMap<PTMCode> averagesByPTMCode = new TObjectDoubleHashMap<PTMCode>();
		double sumIntensity = 0.0;
		for (final PTMCode ptmCode2 : PTMCode.values()) {
			// per peptide (sequence+charge), we average all the intensities across
			// replicates and we calculate the proportions with that
			final double averageByPTMCode = getAverageIntensityByPTMCode(ptmCode2, peptides);
			averagesByPTMCode.put(ptmCode2, averageByPTMCode);
			sumIntensity += averageByPTMCode;
		}
		final TObjectDoubleMap<PTMCode> ret = new TObjectDoubleHashMap<PTMCode>();
		for (final PTMCode ptmCode : averagesByPTMCode.keySet()) {
			final double percentage = averagesByPTMCode.get(ptmCode) / sumIntensity;
			ret.put(ptmCode, percentage);
		}
		return ret;
	}

	private static double getAverageIntensityByPTMCode(PTMCode ptmCode, GroupedQuantifiedPeptide peptides) {
		final TDoubleList intensities = new TDoubleArrayList();
		final List<String> replicates = getReplicateNamesFromPeptide(peptides);
		for (final String replicate : replicates) {
			final Map<PTMCode, TDoubleList> intensitiesPerPTMCode = getIntensitiesPerPTMCodeInReplicate(peptides,
					replicate);
			if (intensitiesPerPTMCode.containsKey(ptmCode)) {
				intensities.addAll(intensitiesPerPTMCode.get(ptmCode));
			}
		}
		if (intensities.isEmpty()) {
			return 0.0;
		}
		return Maths.mean(intensities);
	}

	/**
	 * Gets the intensity from a peptide. The peptide is present in several
	 * replicates and therefore will have multiple ratios with the intensities of
	 * each replicate. In that case, the intensities will be averaged.
	 * 
	 * @param peptide
	 * @return
	 */
	private static double getAvgIntensityFromPeptideAcrossReplicates(QuantifiedPeptideInterface peptide) {
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

	/**
	 * It goes through the peptides PTMs and grab the ones that correspond to a
	 * PTMCode in a map by the position
	 * 
	 * @param peptide
	 * @return
	 */
	public static TIntObjectMap<PTMCode> getPTMCodesByPositionFromPeptide(QuantifiedPeptideInterface peptide) {
		final TIntObjectMap<PTMCode> ret = new TIntObjectHashMap<PTMCode>();
		final List<PTMInPeptide> positionsInPeptide = peptide.getPTMsInPeptide();
		for (final PTMInPeptide positionInpeptide : positionsInPeptide) {
			final int position = positionInpeptide.getPosition();
			final String ptmCode = String.valueOf(positionInpeptide.getDeltaMass());
			final PTMCode ptmCodeObj = PTMCode.getByValue(ptmCode);
			if (ptmCodeObj != null && !ret.containsKey(position)) {
				ret.put(position, ptmCodeObj);
			}
		}
		return ret;
	}

	/**
	 * It goes through the peptides PTMs and grab the ones that correspond to a
	 * PTMCode in a map by the position
	 * 
	 * @param peptide
	 * @return
	 */
	public static THashMap<PTMCode, TIntList> getPositionsByPTMCodesFromPeptide(QuantifiedPeptideInterface peptide) {
		final THashMap<PTMCode, TIntList> ret = new THashMap<PTMCode, TIntList>();
		final TIntObjectMap<PTMInPeptide> ptmPositionsInPeptide = getPTMsInPeptideByPosition(peptide);
		final TIntList motifsPositions = hasMotif(peptide);
		for (final int motifPosition : motifsPositions.toArray()) {
			PTMCode ptmCodeObj = null;
			if (ptmPositionsInPeptide.containsKey(motifPosition)) {
				final String ptmCode = String.valueOf(ptmPositionsInPeptide.get(motifPosition).getDeltaMass());
				ptmCodeObj = PTMCode.getByValue(ptmCode);

			} else {
				ptmCodeObj = PTMCode._0;
			}
			if (!ret.containsKey(ptmCodeObj)) {
				ret.put(ptmCodeObj, new TIntArrayList());
			}
			ret.get(ptmCodeObj).add(motifPosition);
		}
		return ret;
	}

	public static TIntObjectMap<PTMInPeptide> getPTMsInPeptideByPosition(QuantifiedPeptideInterface peptide) {
		final TIntObjectMap<PTMInPeptide> ret = new TIntObjectHashMap<PTMInPeptide>();
		peptide.getPTMsInPeptide().stream().forEach(ptm -> ret.put(ptm.getPosition(), ptm));
		return ret;
	}

	/**
	 * Calculates the percentages from the averages intensities of a peptide per
	 * PTMCode
	 * 
	 * @param avgPeptideIntensityByPTMCode
	 * @return
	 */
	private static TObjectDoubleMap<PTMCode> getPercentages(TObjectDoubleMap<PTMCode> avgPeptideIntensityByPTMCode) {
		final TObjectDoubleMap<PTMCode> ret = new TObjectDoubleHashMap<PTMCode>();
		if (avgPeptideIntensityByPTMCode.isEmpty()) {
			return ret;
		}
		final double sum = Maths.sum(avgPeptideIntensityByPTMCode.values());
		for (final PTMCode ptmCode : PTMCode.values()) {
			final double percentage = avgPeptideIntensityByPTMCode.get(ptmCode) / sum;
			ret.put(ptmCode, percentage);
		}
		return ret;
	}

	/**
	 * Gets the intensities that a peptide (seq+z) (represented by multiple peptides
	 * because PTMs are separated) have per PTMCode. It can have multiple
	 * intensities per PTMCode because sometimes we have here the peptide with an
	 * additional different PTMCodes and the peptide without it.
	 * 
	 * @param peptides  should have the same sequence+charge, they may have
	 *                  different PTMs. So basically is the same peptide.
	 * @param replicate
	 * @return
	 */
	private static Map<PTMCode, TDoubleList> getIntensitiesPerPTMCodeInReplicate(GroupedQuantifiedPeptide peptides,
			String replicate) {
		final Map<PTMCode, TDoubleList> ret = new THashMap<PTMCode, TDoubleList>();

		for (final PTMCode ptmCode : PTMCode.values()) {
			final TDoubleList valuesPerPTMCode = new TDoubleArrayList();
			// reduce list to have unique hashcodes
			final TIntSet hashCodes = new TIntHashSet();
			for (final QuantifiedPeptideInterface peptide : peptides) {
				if (hashCodes.contains(peptide.hashCode())) {
					continue;
				}
				hashCodes.add(peptide.hashCode());
				final THashMap<PTMCode, TIntList> positionsByPTMCodesFromPeptide = getPositionsByPTMCodesFromPeptide(
						peptide);
				// if the peptide has that ptmCode:
				if (positionsByPTMCodesFromPeptide.containsKey(ptmCode)) {
					final double intensityFromPeptideInReplicate = getIntensityFromPeptideInReplicate(peptide,
							replicate);
					// if it is NaN means that the peptide was not detected in that replicate
					if (!Double.isNaN(intensityFromPeptideInReplicate)) {
						valuesPerPTMCode.add(intensityFromPeptideInReplicate);
					}
				}
			}

			if (!valuesPerPTMCode.isEmpty()) {
				if (!ret.containsKey(ptmCode)) {
					ret.put(ptmCode, new TDoubleArrayList());
				}
				if (valuesPerPTMCode.size() > 1) {
					// it really can be more than one because a peptide for a PTMCode can have or
					// not another PTMCode in other site of the sequence
					// (I wrote this code in purpose)
					ret.get(ptmCode).addAll(valuesPerPTMCode);
				} else {
					ret.get(ptmCode).add(valuesPerPTMCode.get(0));
				}
			}
			// TODO it could be summing
//			ret.put(ptmCode, valuesPerPTMCode.sum());
		}
		return ret;
	}

	private static double getIntensityFromPeptideInReplicate(QuantifiedPeptideInterface peptide, String replicate) {
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
	 * creates a map of {@link GroupedQuantifiedPeptide} in which its key is the key
	 * used to group the {@link QuantifiedPeptideInterface}
	 * 
	 * @param peptides
	 * @return
	 */
	public static Map<String, GroupedQuantifiedPeptide> getGroupedPeptidesFromPeptides(
			Collection<QuantifiedPeptideInterface> peptides) {
		final Map<String, GroupedQuantifiedPeptide> ret = new THashMap<String, GroupedQuantifiedPeptide>();
		for (final QuantifiedPeptideInterface peptide : peptides) {
			final String peptideKey = getPeptideKey(peptide, true);
			if (!ret.containsKey(peptideKey)) {
				ret.put(peptideKey, new GroupedQuantifiedPeptide(peptide));
			} else {
				ret.get(peptideKey).add(peptide);
			}
		}
		return ret;
	}

	public static TIntObjectMap<GlycoSite> getSitesByPosition(List<GlycoSite> sites) {
		final TIntObjectMap<GlycoSite> ret = new TIntObjectHashMap<GlycoSite>();
		for (final GlycoSite glycoSite : sites) {
			ret.put(glycoSite.getPosition(), glycoSite);
		}
		return ret;
	}

	public static TMap<PTMCode, TDoubleList> getIntensitiesByPTMCode(
			GroupedQuantifiedPeptide groupedQuantifiedPeptide) {
		final TMap<PTMCode, TDoubleList> ret = new THashMap<PTMCode, TDoubleList>();
		final List<String> replicates = getReplicateNamesFromPeptide(groupedQuantifiedPeptide);
		for (final String replicate : replicates) {
			final Map<PTMCode, TDoubleList> intensitiesPerPTMCodeInReplicate = getIntensitiesPerPTMCodeInReplicate(
					groupedQuantifiedPeptide, replicate);
			for (final PTMCode ptmCode : PTMCode.values()) {
				if (!ret.containsKey(ptmCode)) {
					ret.put(ptmCode, new TDoubleArrayList());
				}
				if (intensitiesPerPTMCodeInReplicate.containsKey(ptmCode)) {
					ret.get(ptmCode).addAll(intensitiesPerPTMCodeInReplicate.get(ptmCode));
				}
			}
		}
		return ret;
	}
}
