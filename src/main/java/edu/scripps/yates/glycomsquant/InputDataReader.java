package edu.scripps.yates.glycomsquant;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;

import edu.scripps.yates.census.read.QuantCompareParser;
import edu.scripps.yates.census.read.QuantParserException;
import edu.scripps.yates.census.read.model.QuantifiedPSM;
import edu.scripps.yates.census.read.model.QuantifiedPeptide;
import edu.scripps.yates.census.read.model.interfaces.QuantifiedPeptideInterface;
import edu.scripps.yates.glycomsquant.gui.MainFrame;
import edu.scripps.yates.glycomsquant.util.GlycoPTMAnalyzerUtil;
import edu.scripps.yates.utilities.luciphor.LuciphorReader;
import edu.scripps.yates.utilities.maths.Maths;
import edu.scripps.yates.utilities.proteomicsmodel.Amount;
import edu.scripps.yates.utilities.proteomicsmodel.PSM;
import edu.scripps.yates.utilities.proteomicsmodel.Score;
import edu.scripps.yates.utilities.proteomicsmodel.enums.AmountType;
import edu.scripps.yates.utilities.proteomicsmodel.factories.AmountEx;
import edu.scripps.yates.utilities.proteomicsmodel.utils.KeyUtils;
import edu.scripps.yates.utilities.sequence.PTMInPeptide;
import gnu.trove.list.TDoubleList;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.TObjectDoubleMap;
import gnu.trove.map.hash.THashMap;
import gnu.trove.map.hash.TObjectDoubleHashMap;
import gnu.trove.set.hash.THashSet;

/**
 * This script reads a Quant-Compare peptide-level file.<br>
 * A part from that, it requires to all PSMs to have a certain pattern of
 * modification:"<br>
 * N is modified as 2.988 or 203.079 and N should follow a motif which is:
 * NX[X/T]. If the N follow this motif but it is not modified with 2.988 or
 * 203.079, it inserts a (by default +0.1234) fake modification so that then PCQ
 * can group by non-modified N sites.<br>
 * Also, it ignores other lines that are not from a certain protein, by default:
 * <br>
 * BG505_SOSIP_gp140</b>
 * 
 * @author salvador
 *
 */
public class InputDataReader extends javax.swing.SwingWorker<List<QuantifiedPeptideInterface>, Object> {
	private final static Logger log = Logger.getLogger(InputDataReader.class);
	private final File inputFile;
	private final String proteinOfInterestACC;
	public static final String INPUT_DATA_READER_FINISHED = "Conversion_finished";
	public static final String NUM_VALID_PEPTIDES = "Num_valid_peptides";
	public static final String INPUT_DATA_READER_ERROR = "input data error";
	public static final String INPUT_DATA_READER_START = "input data reader";
	public static final String INPUT_DATA_READER_PEPTIDES_CORRECTED_BY_LUCIPHOR = "input data reader luciphor";
	private static final double MIN_LUCIPHOR_FDR = 0.05;
	private final double intensityThreshold;
	private final AmountType amountType;
	private final boolean normalizeExperimentsByProtein;
	private List<QuantifiedPeptideInterface> peptides;
	private final Map<File, QuantCompareParser> parsersByFile = new THashMap<File, QuantCompareParser>();
	private final String motifRegexp;
	private final String proteinOfInterestSequence;
	private final boolean discardWrongPositionedPTMs;
	private final File luciphorFile;
	private Map<String, QuantifiedPeptideInterface> peptideMap;

	/**
	 * Constructor in which the proteinOfInterestACC is the default
	 * 'BG505_SOSIP_gp140'
	 * 
	 * @param inputDataFile
	 */
	public InputDataReader(File inputDataFile, File luciphorFile, double intensityThreshold, AmountType amountType,
			boolean normalizeExperimentsByProtein, String motifRegexp, boolean discardWrongPositionedPTMs) {
		this(inputDataFile, luciphorFile, GlycoPTMAnalyzer.DEFAULT_PROTEIN_OF_INTEREST, intensityThreshold, amountType,
				normalizeExperimentsByProtein, motifRegexp, discardWrongPositionedPTMs);
	}

	/**
	 * Constructor in which the proteinOfInterestACC and the fakePTM can be
	 * customized
	 * 
	 * @param inputDataFile
	 * @param proteinOfInterestACC
	 * @param intensityThreshold
	 * @param amountType
	 * @param normalizeReplicates        if true, the intensities will be normalized
	 *                                   by dividing by the sum of all intensities
	 *                                   of the protein of interest and multiplying
	 *                                   by the average of the intensities of the
	 *                                   protein of interest<br>
	 * @param motifRegexp
	 * @param discardWrongPositionedPTMs
	 */
	public InputDataReader(File inputDataFile, File luciphorFile, String proteinOfInterestACC,
			double intensityThreshold, AmountType amountType, boolean normalizeReplicates, String motifRegexp,
			boolean discardWrongPositionedPTMs) {
		this.motifRegexp = motifRegexp;
		this.inputFile = inputDataFile;
		this.luciphorFile = luciphorFile;
		if (proteinOfInterestACC != null && !"".equals(proteinOfInterestACC)) {
			this.proteinOfInterestACC = proteinOfInterestACC;
		} else {
			this.proteinOfInterestACC = GlycoPTMAnalyzer.DEFAULT_PROTEIN_OF_INTEREST;
		}

		this.intensityThreshold = intensityThreshold;
		this.amountType = amountType;
		this.normalizeExperimentsByProtein = normalizeReplicates;
		this.proteinOfInterestSequence = ProteinSequences.getInstance().getProteinSequence(proteinOfInterestACC);
		this.discardWrongPositionedPTMs = discardWrongPositionedPTMs;
	}

	private QuantCompareParser getQuantCompareParser() {
		QuantCompareParser reader = null;
		if (parsersByFile.containsKey(inputFile)) {
			reader = parsersByFile.get(inputFile);
		} else {
			reader = new QuantCompareParser(inputFile);
			MainFrame.getInstance();
			// charge state sensible and ptm sensible
			reader.setChargeSensible(MainFrame.isChargeStateSensible());
			reader.setDecoyPattern(MainFrame.getDecoyPattern());
			reader.setDistinguishModifiedSequences(MainFrame.isDistinguishModifiedSequences());
			reader.setIgnoreTaxonomies(MainFrame.isIgnoreTaxonomies());
		}
		return reader;
	}

	/**
	 * Creates the output file that is PCQ compatible as input TSV file
	 * 
	 * @return
	 * @throws IOException
	 */
	public List<QuantifiedPeptideInterface> runReader() throws QuantParserException {
		final QuantCompareParser reader = getQuantCompareParser();
		peptides = filterData(reader);

		if (luciphorFile != null && luciphorFile.exists()) {
			final LuciphorReader luciphorReader = new LuciphorReader(luciphorFile);
			// merge both luciphor and quant compare
			final Map<String, PSM> luciphorPSMs = luciphorReader.getPSMs();
			// we keep a map of PSMs from luciphor using scan as key
			final Map<String, PSM> luciphorPSMsByScan = new THashMap<String, PSM>();
			luciphorPSMs.values().stream().forEach(psm -> luciphorPSMsByScan.put(psm.getScanNumber(), psm));
			// first we keep a map of PSMS from quant compare using scan as key
			final Map<String, QuantifiedPSM> psmsByScan = new THashMap<String, QuantifiedPSM>();
			for (final QuantifiedPeptideInterface peptide : peptides) {
				peptide.getPSMs().forEach(psm -> psmsByScan.put(psm.getScanNumber(), (QuantifiedPSM) psm));
			}

			mergeLuciphorPSMs(peptides, psmsByScan, luciphorPSMsByScan, MIN_LUCIPHOR_FDR);
			// in peptides is updated with the new peptides and non needed peptides were
			// remove

		}

		parsersByFile.put(inputFile, reader);

		return peptides;
	}

	private List<QuantifiedPeptideInterface> filterData(QuantCompareParser reader) throws QuantParserException {

		int peptidesDiscardedByWrongProtein = 0;
		int peptidesDiscardedByNotHavingMotifs = 0;
		int peptidesDiscardedByPTMInWrongMotif = 0;
		final List<QuantifiedPeptideInterface> ret = new ArrayList<QuantifiedPeptideInterface>();
		ret.addAll(reader.getPeptideMap().values());
		firePropertyChange("progress", null, "Input file readed. Working with " + ret.size() + " peptides.");
		firePropertyChange("progress", "", "Filtering list of peptides...");
		final int initialNumberOfPeptides = ret.size();
		final THashMap<String, TDoubleList> intensitiesByExperiment = new THashMap<String, TDoubleList>();
		Iterator<QuantifiedPeptideInterface> iterator = ret.iterator();
		while (iterator.hasNext()) {
			final QuantifiedPeptideInterface peptide = iterator.next();

			// only take peptides belonging to the protein of interest
			if (proteinOfInterestACC != null) {
				final Optional<String> proteinOfInterest = peptide.getQuantifiedProteins().stream()
						.map(p -> p.getAccession()).filter(acc -> acc.equalsIgnoreCase(proteinOfInterestACC)).findAny();
				if (!proteinOfInterest.isPresent()) {
					peptidesDiscardedByWrongProtein++;
					iterator.remove();
					continue;
				}
			}
			// discard if doesn't have a motif of interest
			final boolean hasMotif = !GlycoPTMAnalyzerUtil
					.getMotifPositions(peptide, proteinOfInterestSequence, motifRegexp).isEmpty();
			if (!hasMotif) {
				peptidesDiscardedByNotHavingMotifs++;
				iterator.remove();
				continue;
			} else {
			}
			if (discardWrongPositionedPTMs) {

				boolean valid = true;
				final TIntObjectMap<PTMInPeptide> ptmPositions = GlycoPTMAnalyzerUtil
						.getPTMsInPeptideByPosition(peptide);
				final TIntList motifs = GlycoPTMAnalyzerUtil.getMotifPositions(peptide, proteinOfInterestACC);
				for (final int position : ptmPositions.keys()) {
					final PTMInPeptide ptm = ptmPositions.get(position);
					final PTMCode ptmCode = PTMCode.getByValue(ptm.getDeltaMass());
					// if it is a PTM of interest
					if (ptmCode != null && !ptmCode.isEmptyPTM()) {
						// if there is no motif in this position:
						if (!motifs.contains(position)) {
							valid = false;
							break;
						}
					}
				}
				if (!valid) {
					iterator.remove();
					peptidesDiscardedByPTMInWrongMotif++;
					continue;
				}
			}

			if (normalizeExperimentsByProtein) {
				final List<Amount> intensities = peptide.getAmounts().stream()
						.filter(a -> a.getAmountType() == amountType).collect(Collectors.toList());
				for (final Amount intensity : intensities) {
					if (!intensitiesByExperiment.containsKey(intensity.getCondition().getName())) {
						intensitiesByExperiment.put(intensity.getCondition().getName(), new TDoubleArrayList());
					}
					intensitiesByExperiment.get(intensity.getCondition().getName()).add(intensity.getValue());
				}
			}
		}

		// if we need to normalize, modify all the intensities of the peptides
		// accordingly
		if (normalizeExperimentsByProtein) {
			// normalize dividing by the sum in each experiment and multiplying by the
			// average
			final TObjectDoubleMap<String> sumByExperiment = new TObjectDoubleHashMap<String>();
			final TObjectDoubleMap<String> avgByExperiment = new TObjectDoubleHashMap<String>();
			for (final String experiment : intensitiesByExperiment.keySet()) {
				sumByExperiment.put(experiment, intensitiesByExperiment.get(experiment).sum());
				avgByExperiment.put(experiment, Maths.mean(intensitiesByExperiment.get(experiment)));
			}
			iterator = ret.iterator();
			while (iterator.hasNext()) {
				final QuantifiedPeptideInterface peptide = iterator.next();

				final List<Amount> intensities = peptide.getAmounts().stream()
						.filter(a -> a.getAmountType() == amountType).collect(Collectors.toList());
				for (final Amount intensity : intensities) {
					final double sum = sumByExperiment.get(intensity.getCondition().getName());
					final double avg = avgByExperiment.get(intensity.getCondition().getName());
					if (intensity instanceof AmountEx) {
						final double normalizedIntensity = (intensity.getValue() / sum) * avg;
						final AmountEx newAmount = new AmountEx(normalizedIntensity, amountType,
								intensity.getCondition());
						peptide.addAmount(newAmount);
						// delete the previous amount
						peptide.getAmounts().remove(intensity);
					}
				}

			}
		}

		//
		int peptidesDiscardedByIntensityThreshold = 0;
		int intensitiesDiscardedByIntensityThreshold = 0;
		iterator = ret.iterator();
		while (iterator.hasNext()) {
			final QuantifiedPeptideInterface peptide = iterator.next();
			final List<Amount> intensities = peptide.getAmounts().stream().filter(a -> a.getAmountType() == amountType)
					.collect(Collectors.toList());
			boolean anyIntensityValid = false;
			for (final Amount intensity : intensities) {
				if (intensity.getValue() == 0.0 || intensity.getValue() < intensityThreshold) {
					intensitiesDiscardedByIntensityThreshold++;
					continue;
				}
				anyIntensityValid = true;
			}
			if (!anyIntensityValid) {
				iterator.remove();
				peptidesDiscardedByIntensityThreshold++;
				continue;
			}
		}
		firePropertyChange("progress", null, "Filtering done.");
		firePropertyChange(NUM_VALID_PEPTIDES, null, ret.size());
		firePropertyChange("progress", null, peptidesDiscardedByWrongProtein
				+ " peptides discarded because they do not belong to protein " + this.proteinOfInterestACC);
		if (discardWrongPositionedPTMs) {
			firePropertyChange("progress", null, peptidesDiscardedByPTMInWrongMotif
					+ " peptides discarded because they had a PTM of interest but in a wrong motif");
		}
		firePropertyChange("progress", null, peptidesDiscardedByNotHavingMotifs
				+ " peptides discarded because they do not contain motifs of interest");
		if (Double.compare(0.0, intensityThreshold) != 0) {
			firePropertyChange("progress", null, peptidesDiscardedByIntensityThreshold
					+ " peptides discarded because none of their intensities in the different experiments pass intensity threshold of '"
					+ intensityThreshold + "'");
			firePropertyChange("progress", null,
					intensitiesDiscardedByIntensityThreshold
							+ " intensities discarded because they do not pass intensity threshold of '"
							+ intensityThreshold + "'");
		}
		firePropertyChange("progress", null,
				ret.size() + " peptides valid for analysis out of " + initialNumberOfPeptides);
		if (ret.size() == 0) {
			throw new IllegalArgumentException("None valid peptides on input file");
		}
		return ret;
	}

//	/**
//	 * Modify sequence to include the fake PTM in the non modified motifs when
//	 * having non modified N followed by anything in the next position (+1) and an S
//	 * or an T in the following position (+2)
//	 * 
//	 * @param peptide
//	 * @return
//	 */
//	private void modifySequence(QuantifiedPeptideInterface peptide) {
//		final String sequence = peptide.getSequence();
//		int index = 0;
//		String triplet = null;
//		int position = 1;
//		boolean isPTM = false;
//		while (index + 2 < sequence.length()) {
//			triplet = sequence.substring(index, index + 3);
//			if (triplet.charAt(0) == '(') {
//				isPTM = true;
//			}
//			if (triplet.charAt(0) == 'N' // && triplet.charAt(1) != '.'
//					&& (triplet.charAt(2) == 'S' || triplet.charAt(2) == 'T')) {
//				// only if peptide has no PTM already in that position
//				boolean valid = true;
//				final List<PTM> ptMs = peptide.getPTMs();
//				if (ptMs != null) {
//					for (final PTM ptm : ptMs) {
//						final List<PTMSite> ptmSites = ptm.getPTMSites();
//						for (final PTMSite ptmSite : ptmSites) {
//							if (ptmSite.getPosition() == position) {
//								valid = false;
//								break;
//							}
//						}
//					}
//				}
//				if (valid) {
//					final PTMEx newPtm = new PTMEx(fakePTM, "N", position);
//					peptide.addPTM(newPtm);
//				}
//			}
//			index++;
//			if (!isPTM) {
//				position++;
//			}
//			if (triplet.charAt(0) == ')') {
//				isPTM = false;
//			}
//		}
//
//	}

	@Override
	protected List<QuantifiedPeptideInterface> doInBackground() throws Exception {
		try {
			firePropertyChange(INPUT_DATA_READER_START, null, null);
			final List<QuantifiedPeptideInterface> ret = runReader();
			firePropertyChange(INPUT_DATA_READER_FINISHED, null, ret);
			return ret;
		} catch (final Exception e) {
			e.printStackTrace();
			firePropertyChange(INPUT_DATA_READER_ERROR, null, "Error reading input data: " + e.getMessage());
		}
		return null;
	}

	public File getInputDataFile() {
		return this.inputFile;
	}

	public Map<String, QuantifiedPeptideInterface> getPeptideMap() throws QuantParserException {
		if (peptideMap == null || peptideMap.isEmpty()) {
			peptideMap = new THashMap<String, QuantifiedPeptideInterface>();

			// create the map from the list of peptides
			if (peptides == null || peptides.isEmpty()) {
				runReader();
			}
			peptides.stream().forEach(pep -> peptideMap.put(pep.getKey(), pep));

		}
		return peptideMap;
	}

	private void mergeLuciphorPSMs(List<QuantifiedPeptideInterface> peptideList, Map<String, QuantifiedPSM> psmsByScan,
			Map<String, PSM> luciphorPSMsByScan, double minLuciphorFDR) {
		// also we need the peptideMap
		final Map<String, QuantifiedPeptideInterface> peptideMap = new THashMap<String, QuantifiedPeptideInterface>();
		peptideList.stream().forEach(pep -> peptideMap.put(pep.getKey(), pep));
		int newPeptides = 0;
		final Set<String> newPeptideKeys = new THashSet<String>();
		for (final String scanNumber : luciphorPSMsByScan.keySet()) {
			if (psmsByScan.containsKey(scanNumber)) {
				final QuantifiedPSM psm = psmsByScan.get(scanNumber);
				final PSM luciphorPSM = luciphorPSMsByScan.get(scanNumber);
				if (!passLuciphorFDRThreshold(luciphorPSM, minLuciphorFDR)) {
					continue;
				}
				if (luciphorPSM.getFullSequence().equals(psm.getFullSequence())) {
					continue;
				}

				// there has been a change
				final String luciphorPeptideKey = KeyUtils.getInstance().getSequenceChargeKey(luciphorPSM,
						MainFrame.isDistinguishModifiedSequences(), MainFrame.isChargeStateSensible());
				final QuantifiedPeptideInterface peptide = psm.getQuantifiedPeptide();
				// change full sequence to psm
				psm.setFullSequence(luciphorPSM.getFullSequence());
				// create peptide with new full sequence.
				// psm and newPeptide will be pointing each other after this
				QuantifiedPeptide newPeptide = null;
				if (peptideMap.containsKey(luciphorPeptideKey)) {
					newPeptide = (QuantifiedPeptide) peptideMap.get(luciphorPeptideKey);
					// we add psm to it
					newPeptide.addPSM(psm, true);
				} else {
					newPeptide = new QuantifiedPeptide(psm, MainFrame.isIgnoreTaxonomies(),
							MainFrame.isDistinguishModifiedSequences(), MainFrame.isChargeStateSensible());
					final QuantifiedPeptide pep = newPeptide;
					// assign Amounts and Conditions to the new peptide from the old one
					peptide.getAmounts().stream().forEach(amount -> pep.addAmount(amount));
					peptide.getConditions().stream().forEach(condition -> pep.addCondition(condition));
				}
				newPeptideKeys.add(newPeptide.getKey());

				// remove previous peptide from peptideMap if doesn't have other psms that are
				// not modified by Luciphor
				boolean remove = true;
				for (final PSM psm2 : peptide.getPSMs()) {
					String scanNumber2 = psm2.getScanNumber();
					if (psm2.getScanNumber().contains("_")) {
						scanNumber2 = psm2.getScanNumber().split("_")[0];
					}
					if (!luciphorPSMsByScan.containsKey(scanNumber2)) {
						remove = false;
					}
				}
				if (remove) {
					peptideList.remove(peptide);
				}
				// add new peptide to peptide list
				peptideList.add(newPeptide);
				peptideMap.put(newPeptide.getKey(), newPeptide);
				newPeptides++;

			}
		}
		firePropertyChange("progress", null,
				newPeptides + " peptides were corrected by Luciphor with confidence FDR < " + minLuciphorFDR);
	}

	private boolean passLuciphorFDRThreshold(PSM luciphorPSM, double minLuciphorFDR) {
		for (final Score score : luciphorPSM.getScores()) {
			if (score.getScoreName().equalsIgnoreCase(LuciphorReader.GLOBAL_SCORE)
					|| score.getScoreName().equalsIgnoreCase(LuciphorReader.LOCAL_FDR)) {
				try {
					final double value = Double.valueOf(score.getValue());
					if (value < minLuciphorFDR) {
						return true;
					}
				} catch (final NumberFormatException e) {

				}
			}
		}
		return false;
	}

}
