package edu.scripps.yates.glycomsquant;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;

import edu.scripps.yates.census.read.QuantCompareParser;
import edu.scripps.yates.census.read.model.interfaces.QuantifiedPeptideInterface;
import edu.scripps.yates.glycomsquant.gui.MainFrame;
import edu.scripps.yates.glycomsquant.util.GlycoPTMAnalyzerUtil;
import edu.scripps.yates.utilities.maths.Maths;
import edu.scripps.yates.utilities.proteomicsmodel.Amount;
import edu.scripps.yates.utilities.proteomicsmodel.PTM;
import edu.scripps.yates.utilities.proteomicsmodel.PTMSite;
import edu.scripps.yates.utilities.proteomicsmodel.enums.AmountType;
import edu.scripps.yates.utilities.proteomicsmodel.factories.AmountEx;
import edu.scripps.yates.utilities.proteomicsmodel.factories.PTMEx;
import gnu.trove.list.TDoubleList;
import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.map.TObjectDoubleMap;
import gnu.trove.map.hash.THashMap;
import gnu.trove.map.hash.TObjectDoubleHashMap;

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
public class QuantCompareReader extends javax.swing.SwingWorker<List<QuantifiedPeptideInterface>, Object> {
	private final static Logger log = Logger.getLogger(QuantCompareReader.class);
	private final File inputFile;
	private final String proteinOfInterestACC;
	private final static DecimalFormat formatter = new DecimalFormat("+#.###");
	public static final String INPUT_DATA_READER_FINISHED = "Conversion_finished";
	public static final String NUM_VALID_PEPTIDES = "Num_valid_peptides";
	public static final String INPUT_DATA_READER_ERROR = "input data error";
	public static final String INPUT_DATA_READER_START = "input data reader";
	private final double fakePTM;
	private final double intensityThreshold;
	private final AmountType amountType;
	private final boolean normalizeExperimentsByProtein;
	private List<QuantifiedPeptideInterface> peptides;
	private final Map<File, QuantCompareParser> parsersByFile = new THashMap<File, QuantCompareParser>();
	private final String motifRegexp;
	private Pattern pattern;
	private final String proteinOfInterestSequence;

	/**
	 * Constructor in which the proteinOfInterestACC is the default
	 * 'BG505_SOSIP_gp140' and the fakePTM is the default '0.123'
	 * 
	 * @param file
	 */
	public QuantCompareReader(File file, double intensityThreshold, AmountType amountType,
			boolean normalizeExperimentsByProtein, String motifRegexp) {
		this(file, GlycoPTMAnalyzer.DEFAULT_PROTEIN_OF_INTEREST, GlycoPTMAnalyzer.DEFAULT_PROTEIN_OF_INTEREST_SEQUENCE,
				GlycoPTMAnalyzer.DEFAULT_FAKE_PTM, intensityThreshold, amountType, normalizeExperimentsByProtein,
				motifRegexp);
	}

	/**
	 * Constructor in which the proteinOfInterestACC and the fakePTM can be
	 * customized
	 * 
	 * @param inputDataFile
	 * @param proteinOfInterestACC
	 * @param proteinOfInterestSequence
	 * @param fakePTM
	 * @param intensityThreshold
	 * @param amountType
	 * @param normalizeReplicates       if true, the intensities will be normalized
	 *                                  by dividing by the sum of all intensities of
	 *                                  the protein of interest and multiplying by
	 *                                  the average of the intensities of the
	 *                                  protein of interest
	 */
	public QuantCompareReader(File inputDataFile, String proteinOfInterestACC, String proteinOfInterestSequence,
			Double fakePTM, double intensityThreshold, AmountType amountType, boolean normalizeReplicates,
			String motifRegexp) {
		this.motifRegexp = motifRegexp;
		this.inputFile = inputDataFile;
		if (proteinOfInterestACC != null && !"".equals(proteinOfInterestACC)) {
			this.proteinOfInterestACC = proteinOfInterestACC;
		} else {
			this.proteinOfInterestACC = GlycoPTMAnalyzer.DEFAULT_PROTEIN_OF_INTEREST;
		}
		if (fakePTM != null) {
			this.fakePTM = fakePTM;
		} else {
			this.fakePTM = GlycoPTMAnalyzer.DEFAULT_FAKE_PTM;
		}
		this.intensityThreshold = intensityThreshold;
		this.amountType = amountType;
		this.normalizeExperimentsByProtein = normalizeReplicates;
		this.proteinOfInterestSequence = proteinOfInterestSequence;
	}

	/**
	 * Creates the output file that is PCQ compatible as input TSV file
	 * 
	 * @return
	 * @throws IOException
	 */
	public List<QuantifiedPeptideInterface> runReader() throws IOException {
		QuantCompareParser reader = null;
		if (parsersByFile.containsKey(inputFile)) {
			reader = parsersByFile.get(inputFile);
		} else {
			reader = new QuantCompareParser(inputFile);
			// charge state sensible and ptm sensible
			reader.setChargeSensible(MainFrame.getInstance().isChargeStateSensible());
			reader.setDistinguishModifiedSequences(MainFrame.getInstance().isDistinguishModifiedSequences());
			reader.setIgnoreTaxonomies(MainFrame.getInstance().isIgnoreTaxonomies());
		}
		peptides = filterData(reader);
		parsersByFile.put(inputFile, reader);

		return peptides;
	}

	private List<QuantifiedPeptideInterface> filterData(QuantCompareParser reader) throws IOException {

		int peptidesDiscardedByWrongProtein = 0;
		int peptidesDiscardedByNotHavingMotifs = 0;
		final List<QuantifiedPeptideInterface> ret = new ArrayList<QuantifiedPeptideInterface>();
		ret.addAll(reader.getPeptideMap().values());
		firePropertyChange("progress", null, "Input file readed. Working with " + ret.size() + " peptides.");
		firePropertyChange("progress", "", "Filtering list of peptides...");
		final int initialNumberOfPeptides = ret.size();
		final THashMap<String, TDoubleList> intensitiesByExperiment = new THashMap<String, TDoubleList>();
		Iterator<QuantifiedPeptideInterface> iterator = ret.iterator();
		while (iterator.hasNext()) {
			final QuantifiedPeptideInterface peptide = iterator.next();
			if (peptide.getFullSequence().equals("HAVPN(203.079373)GTIVK")) {
				log.info("asd");
			}
			// only take peptides belonging to the protein of interest
			final Optional<String> proteinOfInterest = peptide.getQuantifiedProteins().stream()
					.map(p -> p.getAccession()).filter(acc -> acc.equalsIgnoreCase(proteinOfInterestACC)).findAny();
			if (!proteinOfInterest.isPresent()) {
				peptidesDiscardedByWrongProtein++;
				iterator.remove();
				continue;
			}

			// discard if doesn't have a motif of interest

			final boolean hasMotif = !GlycoPTMAnalyzerUtil.hasMotif(peptide, proteinOfInterestSequence, motifRegexp)
					.isEmpty();

//			final TIntArrayList allPositionsOf = StringUtils.allPositionsOf(sequence, 'N');
//			if (!allPositionsOf.isEmpty()) {
//				for (final int position : allPositionsOf.toArray()) {
//					if (position + 2 <= sequence.length()) {
//						final char charAt = sequence.charAt(position + 2 - 1);
//						if (charAt == 'S' || charAt == 'T') {
//							hasMotif = true;
//						}
//					}
//				}
//			}
//
//			
			if (!hasMotif) {
//				if (hasMotif(extendedSequence)) {
//					log.info("ASDF");
//				}
				peptidesDiscardedByNotHavingMotifs++;
				iterator.remove();
				continue;
			} else {
//				if (!hasMotif(extendedSequence)) {
//					log.info("ASDF");
//				}
			}
			// Modify sequence to include the fake PTM in the non modified motifs
//			modifySequence(peptide);

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
		log.info(peptidesDiscardedByWrongProtein + " peptides discarded because they do not belong to protein "
				+ this.proteinOfInterestACC);
		log.info(peptidesDiscardedByNotHavingMotifs
				+ " peptides discarded because they do not contain motifs of interest");
		log.info(peptidesDiscardedByIntensityThreshold
				+ " peptides discarded because none of their intensities in the different experiments pass intensity threshold of '"
				+ intensityThreshold + "'");
		log.info(intensitiesDiscardedByIntensityThreshold
				+ " intensities discarded because they do not pass intensity threshold of '" + intensityThreshold
				+ "'");
		log.info(ret.size() + " peptides valid for analysis out of " + initialNumberOfPeptides);
		firePropertyChange("progress", null,
				ret.size() + " peptides valid for analysis out of " + initialNumberOfPeptides);
		if (ret.size() == 0) {
			throw new IllegalArgumentException("None valid peptides on input file");
		}
		return ret;
	}

	/**
	 * Modify sequence to include the fake PTM in the non modified motifs when
	 * having non modified N followed by anything in the next position (+1) and an S
	 * or an T in the following position (+2)
	 * 
	 * @param peptide
	 * @return
	 */
	private void modifySequence(QuantifiedPeptideInterface peptide) {
		final String sequence = peptide.getSequence();
		int index = 0;
		String triplet = null;
		int position = 1;
		boolean isPTM = false;
		while (index + 2 < sequence.length()) {
			triplet = sequence.substring(index, index + 3);
			if (triplet.charAt(0) == '(') {
				isPTM = true;
			}
			if (triplet.charAt(0) == 'N' // && triplet.charAt(1) != '.'
					&& (triplet.charAt(2) == 'S' || triplet.charAt(2) == 'T')) {
				// only if peptide has no PTM already in that position
				boolean valid = true;
				final List<PTM> ptMs = peptide.getPTMs();
				if (ptMs != null) {
					for (final PTM ptm : ptMs) {
						final List<PTMSite> ptmSites = ptm.getPTMSites();
						for (final PTMSite ptmSite : ptmSites) {
							if (ptmSite.getPosition() == position) {
								valid = false;
								break;
							}
						}
					}
				}
				if (valid) {
					final PTMEx newPtm = new PTMEx(fakePTM, "N", position);
					peptide.addPTM(newPtm);
				}
			}
			index++;
			if (!isPTM) {
				position++;
			}
			if (triplet.charAt(0) == ')') {
				isPTM = false;
			}
		}

	}

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

}
