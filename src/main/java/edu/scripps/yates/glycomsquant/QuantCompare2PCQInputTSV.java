package edu.scripps.yates.glycomsquant;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;

import edu.scripps.yates.census.read.QuantCompareParser;
import edu.scripps.yates.census.read.model.interfaces.QuantifiedPeptideInterface;
import edu.scripps.yates.utilities.maths.Maths;
import edu.scripps.yates.utilities.proteomicsmodel.Amount;
import edu.scripps.yates.utilities.proteomicsmodel.enums.AmountType;
import edu.scripps.yates.utilities.proteomicsmodel.factories.AmountEx;
import edu.scripps.yates.utilities.proteomicsmodel.factories.PTMEx;
import edu.scripps.yates.utilities.sequence.PTMInPeptide;
import gnu.trove.list.TDoubleList;
import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.map.TObjectDoubleMap;
import gnu.trove.map.hash.THashMap;
import gnu.trove.map.hash.TObjectDoubleHashMap;

/**
 * This script reads a Quant-Compare PSM-level file and creates and input file
 * for PCQ.<br>
 * This input file is a TSV formatted file that uses the normalized intensities
 * from the PSMs in quant-compare instead of ratios.<br>
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
public class QuantCompare2PCQInputTSV extends javax.swing.SwingWorker<File, Object> {
	private final static Logger log = Logger.getLogger(QuantCompare2PCQInputTSV.class);
	private final File inputFile;
	private final String proteinOfInterestACC;
	public final static double DEFAULT_FAKE_PTM = 0.123;
	private final static DecimalFormat formatter = new DecimalFormat("+#.###");
	private final static String DEFAULT_PROTEIN_OF_INTEREST = "BG505_SOSIP_gp140";
	public static final String FINISHED = "Conversion_finished";
	public static final String NUM_VALID_PEPTIDES = "Num_valid_peptides";
	private final double fakePTM;
	private final double intensityThreshold;
	private int peptidesValid;
	private final AmountType amountType;
	private boolean normalizeExperimentsByProtein;
	private final String suffix;

	/**
	 * Constructor in which the proteinOfInterestACC is the default
	 * 'BG505_SOSIP_gp140' and the fakePTM is the default '0.123'
	 * 
	 * @param file
	 */
	public QuantCompare2PCQInputTSV(File file, double intensityThreshold, AmountType amountType,
			boolean normalizeExperimentsByProtein, String suffix) {
		this(file, DEFAULT_PROTEIN_OF_INTEREST, DEFAULT_FAKE_PTM, intensityThreshold, amountType,
				normalizeExperimentsByProtein, suffix);
	}

	/**
	 * Constructor in which the proteinOfInterestACC and the fakePTM can be
	 * customized
	 * 
	 * @param file
	 * @param proteinOfInterestACC
	 * @param fakePTM
	 * @param intensityThreshold
	 * @param amountType
	 * @param normalizeExperimentsByProtein if true, the intensities will be
	 *                                      normalized by dividing by the sum of all
	 *                                      intensities of the protein of interest
	 *                                      and multiplying by the average of the
	 *                                      intensities of the protein of interest
	 */
	public QuantCompare2PCQInputTSV(File file, String proteinOfInterestACC, Double fakePTM, double intensityThreshold,
			AmountType amountType, boolean normalizeExperimentsByProtein, String suffix) {
		this.inputFile = file;
		if (proteinOfInterestACC != null && !"".equals(proteinOfInterestACC)) {
			this.proteinOfInterestACC = proteinOfInterestACC;
		} else {
			this.proteinOfInterestACC = DEFAULT_PROTEIN_OF_INTEREST;
		}
		if (fakePTM != null) {
			this.fakePTM = fakePTM;
		} else {
			this.fakePTM = DEFAULT_FAKE_PTM;
		}
		this.intensityThreshold = intensityThreshold;
		this.amountType = amountType;
		this.normalizeExperimentsByProtein = normalizeExperimentsByProtein;
		this.suffix = suffix;
	}

	public static void main(String[] args) {
		File inputFile = new File(args[0]);
		String proteinOfInterestACC = args[1];
		double fakePTM = DEFAULT_FAKE_PTM;
		if (args.length > 2) {
			try {
				fakePTM = Double.valueOf(args[2]);
			} catch (NumberFormatException e) {
				e.printStackTrace();
				log.error(e.getMessage());
				System.exit(-1);
			}
		}
		boolean normlizeExperimentsByProtein = true;
		final QuantCompare2PCQInputTSV q = new QuantCompare2PCQInputTSV(inputFile, proteinOfInterestACC, fakePTM, 0.0,
				AmountType.INTENSITY, normlizeExperimentsByProtein, "");
		try {
			final File pcqFile = q.runConversion();
			if (pcqFile != null && pcqFile.exists()) {
				log.info("PCQ input file created succesfully at: " + pcqFile.getAbsolutePath());
			}
			log.info("Everything OK");
			System.exit(0);
		} catch (final IOException e) {
			e.printStackTrace();
			log.error(e.getMessage());
			System.exit(-1);
		}
	}

	/**
	 * Creates the output file that is PCQ compatible as input TSV file
	 * 
	 * @return
	 * @throws IOException
	 */
	public File runConversion() throws IOException {
		firePropertyChange("progress", null, "Reading input file...");
		final QuantCompareParser reader = new QuantCompareParser(inputFile);
		reader.setChargeSensible(true);
		reader.setDistinguishModifiedSequences(true);

		File outputFile = new File(inputFile.getParentFile().getAbsolutePath() + File.separator
				+ FilenameUtils.getBaseName(inputFile.getAbsolutePath()) + suffix + "_pcq.txt");
		writeFile(outputFile, reader);
		firePropertyChange(FINISHED, null, outputFile);
		return outputFile;
	}

	private void writeFile(File outputFile, QuantCompareParser reader) throws IOException {
		FileWriter fw = null;
		try {
			int peptidesDiscardedByWrongProtein = 0;
			int peptidesDiscardedByNotHavingMotifs = 0;
			fw = new FileWriter(outputFile);
			// write header
			fw.write("ID\tSEQUENCE\tINTENSITY\tweight\tPROTEIN\n");
			List<QuantifiedPeptideInterface> peptides = new ArrayList<QuantifiedPeptideInterface>();
			peptides.addAll(reader.getPeptideMap().values());
			firePropertyChange("progress", null, "Input file readed. Working with " + peptides.size() + " peptides.");
			firePropertyChange("progress", "", "Filtering list of peptides...");
			int initialNumberOfPeptides = peptides.size();
			THashMap<String, TDoubleList> intensitiesByExperiment = new THashMap<String, TDoubleList>();
			Iterator<QuantifiedPeptideInterface> iterator = peptides.iterator();
			while (iterator.hasNext()) {
				QuantifiedPeptideInterface peptide = iterator.next();
				// only take peptides belonging to the protein of interest
				Optional<String> proteinOfInterest = peptide.getQuantifiedProteins().stream().map(p -> p.getAccession())
						.filter(acc -> acc.equalsIgnoreCase(proteinOfInterestACC)).findAny();
				if (!proteinOfInterest.isPresent()) {
					peptidesDiscardedByWrongProtein++;
					iterator.remove();
					continue;
				}

				// Modify sequence to include the fake PTM in the non modified motifs
				modifySequence(peptide);
				// filter out peptides with no motifs or not ptms
				List<PTMInPeptide> ptmsInPeptide = peptide.getPTMsInPeptide();
				boolean valid = false;
				for (PTMInPeptide ptmInPeptide : ptmsInPeptide) {
					String ptmCode = String.valueOf(ptmInPeptide.getDeltaMass());
					PTMCode ptmCodeObj = PTMCode.getByValue(ptmCode);
					if (ptmCodeObj != null) {
						valid = true;
						break;
					}
				}
				if (!valid) {
					peptidesDiscardedByNotHavingMotifs++;
					iterator.remove();
					continue;
				}
				if (normalizeExperimentsByProtein) {
					List<Amount> intensities = peptide.getAmounts().stream()
							.filter(a -> a.getAmountType() == amountType).collect(Collectors.toList());
					for (Amount intensity : intensities) {
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
				TObjectDoubleMap<String> sumByExperiment = new TObjectDoubleHashMap<String>();
				TObjectDoubleMap<String> avgByExperiment = new TObjectDoubleHashMap<String>();
				for (String experiment : intensitiesByExperiment.keySet()) {
					sumByExperiment.put(experiment, intensitiesByExperiment.get(experiment).sum());
					avgByExperiment.put(experiment, Maths.mean(intensitiesByExperiment.get(experiment)));
				}
				iterator = peptides.iterator();
				while (iterator.hasNext()) {
					QuantifiedPeptideInterface peptide = iterator.next();

					List<Amount> intensities = peptide.getAmounts().stream()
							.filter(a -> a.getAmountType() == amountType).collect(Collectors.toList());
					for (Amount intensity : intensities) {
						double sum = sumByExperiment.get(intensity.getCondition().getName());
						double avg = avgByExperiment.get(intensity.getCondition().getName());
						if (intensity instanceof AmountEx) {
							double normalizedIntensity = (intensity.getValue() / sum) * avg;
							AmountEx newAmount = new AmountEx(normalizedIntensity, amountType,
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
			iterator = peptides.iterator();
			while (iterator.hasNext()) {
				QuantifiedPeptideInterface peptide = iterator.next();
				List<Amount> intensities = peptide.getAmounts().stream().filter(a -> a.getAmountType() == amountType)
						.collect(Collectors.toList());
				boolean anyIntensityValid = false;
				for (Amount intensity : intensities) {
					if (intensity.getValue() == 0.0 || intensity.getValue() < intensityThreshold) {
						intensitiesDiscardedByIntensityThreshold++;
						continue;
					}
					anyIntensityValid = true;
					fw.write(peptide.getKey() + "_" + intensity.getCondition().getName() + "\t"
							+ peptide.getFullSequence() + "\t" + intensity.getValue() + "\t" + "1" + "\t"
							+ proteinOfInterestACC + "\n");
				}
				if (!anyIntensityValid) {
					iterator.remove();
					peptidesDiscardedByIntensityThreshold++;
					continue;
				}
			}
			firePropertyChange("progress", null, "Filtering done.");
			peptidesValid = peptides.size();
			firePropertyChange(NUM_VALID_PEPTIDES, null, peptidesValid);
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
			log.info(peptides.size() + " peptides valid for analysis out of " + initialNumberOfPeptides);
			firePropertyChange("progress", null,
					peptides.size() + " peptides valid for analysis out of " + initialNumberOfPeptides);
			if (peptidesValid == 0) {
				throw new IllegalArgumentException("None valid peptides on input file");
			}
		} finally {
			if (fw != null) {
				fw.close();
			}
		}

	}

	public int getPsmsValid() {
		return peptidesValid;
	}

	/**
	 * Modify sequence to include the fake PTM in the non modified motifs
	 * 
	 * @param peptide
	 * @return
	 */
	private void modifySequence(QuantifiedPeptideInterface peptide) {
		String sequence = peptide.getFullSequence();
		int index = 0;
		String triplet = null;
		int position = 1;
		boolean isPTM = false;
		while (index + 2 < sequence.length()) {
			triplet = sequence.substring(index, index + 3);
			if (triplet.charAt(0) == '(') {
				isPTM = true;
			}
			if (triplet.charAt(0) == 'N' && triplet.charAt(1) != '.'
					&& (triplet.charAt(2) == 'S' || triplet.charAt(2) == 'T')) {
				PTMEx newPtm = new PTMEx(fakePTM, "N", position);
				peptide.addPTM(newPtm);
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
	protected File doInBackground() throws Exception {
		return runConversion();
	}

}
