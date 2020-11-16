package edu.scripps.yates.glycomsquant;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.log4j.Logger;
import org.springframework.core.io.ClassPathResource;

import edu.scripps.yates.census.read.QuantParserException;
import edu.scripps.yates.census.read.model.interfaces.QuantifiedPeptideInterface;
import edu.scripps.yates.glycomsquant.gui.reference.MappingToReferenceHXB2;
import edu.scripps.yates.utilities.appversion.AppVersion;
import edu.scripps.yates.utilities.maths.Maths;
import edu.scripps.yates.utilities.properties.PropertiesUtil;
import edu.scripps.yates.utilities.proteomicsmodel.enums.AmountType;
import gnu.trove.list.TDoubleList;
import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.map.TObjectDoubleMap;
import gnu.trove.map.hash.TObjectDoubleHashMap;

public class GlycoPTMAnalyzer implements InputParameters {
	private final static Logger log = Logger.getLogger(GlycoPTMAnalyzer.class);
	private static Options options;
	private static AppVersion version;
	public static final String DEFAULT_PROTEIN_OF_INTEREST = "BG505_SOSIP_gp140";
	public static final double DEFAULT_FAKE_PTM = 0.123;
	public static final String DEFAULT_PROTEIN_OF_INTEREST_SEQUENCE = "TGAENLWVTVYYGVPVWKDAETTLFCASDAKAYETEKHNVWATHACVPTDPNPQEIHLENVTEEFNMWKNNMVEQMHTDIISLWDQSLKPCVKLTPLCVTLQCTNVTNNITDDMRGELKNCSFNMTTELRDKKQKVYSLFYRLDVVQINENQGNRSNNSNKEYRLINCNTSAITQACPKVSFEPIPIHYCAPAGFAILKCKDKKFNGTGPCPSVSTVQCTHGIKPVVSTQLLLNGSLAEEEVMIRSENITNNAKNILVQFNTPVQINCTRPNNNTRKSIRIGPGQAFYATGDIIGDIRQAHCNVSKATWNETLGKVVKQLRKHFGNNTIIRFANSSGGDLEVTTHSFNCGGEFFYCNTSGLFNSTWISNTSVQGSNSTGSNDSITLPCRIKQIINMWQRIGQAMYAPPIQGVIRCVSNITGLILTRDGGSTNSTTETFRPGGGDMRDNWRSELYKYKVVKIEPLGVAPTRCKRRVVGRRRRRRAVGIGAVFLGFLGAAGSTMGAASMTLTVQARNLLSGIVQQQSNLLRAPEAQQHLLKLTVWGIKQLQARVLAVERYLRDQQLLGIWGCSGKLICCTNVPWNSSWSNRNLSEIWDNMTWLQWDKEISNYTQIIYGLLEESQNQQEKNEQDLLALDGTKHHHHHH";
// 	public static final String NEW_DEFAULT_MOTIF_REGEXP = "\\w*(N[^P][S|T])\\w*";
	public static final String NEW_DEFAULT_MOTIF_REGEXP = "(N[^P][S|T])";

	private final File inputFile;
	private final String proteinOfInterestACC;
	private final File fastaFile;
	private final String name;
	private final double intensityThreshold;
	/*
	 * the average of the percentages across all the {@link GlycoSite} for that
	 * {@link PTMCode}
	 */
	private TObjectDoubleMap<PTMCode> averagePercentagesByPTMCode = new TObjectDoubleHashMap<PTMCode>();
	private GlycoPTMResultGenerator resultGenerator;
	private int peptidesValid;
	private final AmountType amountType;
	private final boolean normalizeReplicates;
	private final boolean sumIntensitiesAcrossReplicates;
	private final String motifRegexp;
	private final boolean discardWrongPositionedPTMs;
	private final boolean fixWrongPositionedPTMs;
	private final Boolean discardNonUniquePeptides;
	private final Boolean dontAllowConsecutiveMotifs;
	private final String referenceProteinSequence;
	private final File luciphorFile;
	private final Boolean discardPeptidesWithNoMotifs;

	public GlycoPTMAnalyzer(InputParameters inputParams) {
		CurrentInputParameters.getInstance().setInputParameters(inputParams);
		this.inputFile = inputParams.getInputFile();
		this.proteinOfInterestACC = inputParams.getProteinOfInterestACC();
		this.fastaFile = inputParams.getFastaFile();
		this.luciphorFile = inputParams.getLuciphorFile();
		this.name = inputParams.getName();
		this.intensityThreshold = inputParams.getIntensityThreshold();
		this.amountType = inputParams.getAmountType();
		this.normalizeReplicates = inputParams.isNormalizeReplicates();
		this.sumIntensitiesAcrossReplicates = inputParams.isSumIntensitiesAcrossReplicates();
		this.motifRegexp = inputParams.getMotifRegexp();
		this.discardWrongPositionedPTMs = inputParams.isDiscardWrongPositionedPTMs();
		this.fixWrongPositionedPTMs = inputParams.isFixWrongPositionedPTMs();
		this.discardNonUniquePeptides = inputParams.isDiscardNonUniquePeptides();
		this.dontAllowConsecutiveMotifs = inputParams.isDontAllowConsecutiveMotifs();
		this.referenceProteinSequence = inputParams.getReferenceProteinSequence();
		this.discardPeptidesWithNoMotifs = inputParams.isDiscardPeptidesWithNoMotifs();
		printWelcome();
	}

	public GlycoPTMAnalyzer(File inputFile, String proteinOfInterestACC, File fastaFile, File luciphorFile,
			String prefix, String suffix, double intensityThreshold, AmountType amountType,
			boolean normalizeExperimentsByProtein, boolean sumIntensitiesAcrossReplicates, String motifRegexp,
			boolean discardWrongPositionedPTMs, boolean fixWrongPositionedPTMs, boolean discardPeptidesWithNoMotifs,
			boolean discardNonUniquePeptides, boolean dontAllowConsecutiveMotifs, boolean useReferenceProtein) {
		this.inputFile = inputFile;
		this.proteinOfInterestACC = proteinOfInterestACC;
		this.fastaFile = fastaFile;
		this.luciphorFile = luciphorFile;
		this.name = prefix;
		this.intensityThreshold = intensityThreshold;
		this.amountType = amountType;
		this.normalizeReplicates = normalizeExperimentsByProtein;
		this.motifRegexp = motifRegexp;
		this.sumIntensitiesAcrossReplicates = sumIntensitiesAcrossReplicates;
		this.discardWrongPositionedPTMs = discardWrongPositionedPTMs;
		this.fixWrongPositionedPTMs = fixWrongPositionedPTMs;
		this.discardPeptidesWithNoMotifs = discardPeptidesWithNoMotifs;
		this.discardNonUniquePeptides = discardNonUniquePeptides;
		this.dontAllowConsecutiveMotifs = dontAllowConsecutiveMotifs;
		if (useReferenceProtein) {
			this.referenceProteinSequence = MappingToReferenceHXB2.HXB2;
		} else {
			this.referenceProteinSequence = null;
		}
		printWelcome();

		CurrentInputParameters.getInstance().setInputParameters(this);
	}

	static void setupCommandLineOptions() {
		// create Options object
		options = new Options();

		final Option option1 = new Option("i", "input", true,
				"[MANDATORY] Full path to the input file, which is a text file from Census quant compare.");
		option1.setRequired(true);
		options.addOption(option1);

		final Option option2 = new Option("acc", "accession", true,
				"[OPTIONAL] Protein accession of the protein that will be analyzed. All the rest of the proteins in the input file will be ignored. If not provided, accession 'BG505_SOSIP_gp140' will be used.");
		option2.setRequired(false);
		options.addOption(option2);

		final Option option3 = new Option("pre", "prefix", true,
				"[OPTIONAL] Prefix that will be added to the name of all the output files. ");
		option3.setRequired(false);
		options.addOption(option3);

		final Option option4 = new Option("suf", "suffix", true,
				"[OPTIONAL] Suffix that will be added to the name of all the output files. ");
		option4.setRequired(false);
		options.addOption(option4);

		final Option option5 = new Option("thr", "intensityThreshold", true,
				"[OPTIONAL] Intensity threshold to discard all PSM with intensity below that value. If not provided, no threshold will be applied.");
		option5.setRequired(false);
		options.addOption(option5);

		final Option option6 = new Option("fas", "fasta", true,
				"[MANDATORY] Full path to the fasta file in which the sequence of the protein of interest is present.");
		option6.setRequired(true);
		options.addOption(option6);

		final Option option7 = new Option("norm", "normalized_intensity", false,
				"[OPTIONAL] If present, it will use normalized intensities instead of raw intensities. If not present, it will use raw intensities.");
		option7.setRequired(false);
		options.addOption(option7);

		final Option option8 = new Option("sum", "sum_across_replicates", true,
				"[OPTIONAL] If present, boolean parameter (true/false) to set whether to sum intensities per peptide across replicates before calculate the proportions or not.");
		option8.setRequired(false);
		options.addOption(option8);

		final Option option9 = new Option("dis", "discard_wrong_motifs", false,
				"[OPTIONAL] If present, peptides having PTMs of interest that are not in motifs are discarded regardless of having other positions with valid PTMs in motifs.");
		option9.setRequired(false);
		options.addOption(option9);

		final Option option92 = new Option("fix", "fix_wrong_motifs", false,
				"[OPTIONAL] If present, PTMs of interest positioned in non-valid motifs will be tried to be re-positioned to the correct position, if there is no ambiguities.");
		option92.setRequired(false);
		options.addOption(option92);

		final Option option93 = new Option("dis_others", "discard_other_peptides", false,
				"[OPTIONAL] If present, peptides not covering any motif of interest will be discarded.");
		option93.setRequired(false);
		options.addOption(option93);

		final Option option10 = new Option("dnu", "discard_non_unique_peptides", false,
				"[OPTIONAL] If present, peptides shared by multiple proteins will be discarded.");
		option10.setRequired(false);
		options.addOption(option10);

		final Option option11 = new Option("con", "consecutive_motifs", true,
				"[OPTIONAL] Determines if motifs in consecutive sites are allowed or not. If not, they will be flagged and peptides with PTMs in consecutive positions will be discarded.");
		option11.setRequired(false);
		options.addOption(option11);

		final Option option12 = new Option("ref", "use_ref_protein", true, "[OPTIONAL] Use of reference protein HXB2.");
		option12.setRequired(false);
		options.addOption(option12);

		final Option option13 = new Option("luc", "luciphor", true,
				"[OPTIONAL] Full path to the Luciphor results file that re-localizes and scores PTMs in peptides.");
		option13.setRequired(false);
		options.addOption(option13);
	}

	public static void main(String[] args) {

		final AppVersion version = getVersion();
		System.out.println("Running HIVPTMAnalysis version " + version.toString());
		setupCommandLineOptions();
		final CommandLineParser parser = new DefaultParser();
		try {
			final CommandLine cmd = parser.parse(options, args);

			final File inputFile = new File(cmd.getOptionValue('i'));
			if (!inputFile.exists()) {
				throw new IllegalArgumentException("Input file not found '" + inputFile.getAbsolutePath() + "'");
			}
			String proteinOfInterestACC = GlycoPTMAnalyzer.DEFAULT_PROTEIN_OF_INTEREST;
			if (cmd.hasOption("acc")) {
				proteinOfInterestACC = cmd.getOptionValue("acc");
			}

			final File fastaFile = new File(cmd.getOptionValue("fas"));

			String prefix = "";
			if (cmd.hasOption("pre")) {
				prefix = cmd.getOptionValue("pre");
			}
			String suffix = "";
			if (cmd.hasOption("suf")) {
				suffix = cmd.getOptionValue("suf");
			}
			double intensityThreshold = 0.0;
			if (cmd.hasOption("thr")) {
				intensityThreshold = Double.valueOf(cmd.getOptionValue("thr"));
			}

			final AmountType amountType = AmountType.INTENSITY;
			boolean normalizeExperimentsByProtein = false;
			if (cmd.hasOption("norm")) {
				normalizeExperimentsByProtein = true;
			}

			final boolean sumIntensitiesByReplicates = true;
			if (cmd.hasOption("sum")) {
				normalizeExperimentsByProtein = Boolean.valueOf(cmd.getOptionValue("sum"));
			}
			final String motifRegexp = GlycoPTMAnalyzer.NEW_DEFAULT_MOTIF_REGEXP;

			boolean discardWrongPositionedPTMs = false;
			if (cmd.hasOption("dis")) {
				discardWrongPositionedPTMs = Boolean.valueOf(cmd.getOptionValue("dis"));
			}
			boolean fixWrongPositionedPTMs = false;
			if (cmd.hasOption("fix")) {
				fixWrongPositionedPTMs = Boolean.valueOf(cmd.getOptionValue("fix"));
			}
			boolean discardPeptidesWithNoMotifs = false;
			if (cmd.hasOption("dis_others")) {
				discardPeptidesWithNoMotifs = Boolean.valueOf(cmd.getOptionValue("dis_others"));
			}
			boolean discardNonUniquePeptides = false;
			if (cmd.hasOption("dnu")) {
				discardNonUniquePeptides = Boolean.valueOf(cmd.getOptionValue("dnu"));
			}
			final boolean dontAllowConsecutiveMotifs = true;
//			boolean dontAllowConsecutiveMotifs = false;
//			if (cmd.hasOption("con")) {
//				dontAllowConsecutiveMotifs = Boolean.valueOf(cmd.getOptionValue("con"));
//			}
			boolean useReferenceProtein = false;
			if (cmd.hasOption("ref")) {
				useReferenceProtein = Boolean.valueOf(cmd.getOptionValue("ref"));
			}

			File luciphorFile = null;
			if (cmd.hasOption("luc")) {
				luciphorFile = new File(cmd.getOptionValue("luc"));
			}
			final GlycoPTMAnalyzer analyzer = new GlycoPTMAnalyzer(inputFile, proteinOfInterestACC, fastaFile,
					luciphorFile, prefix, suffix, intensityThreshold, amountType, normalizeExperimentsByProtein,
					sumIntensitiesByReplicates, motifRegexp, discardWrongPositionedPTMs, fixWrongPositionedPTMs,
					discardPeptidesWithNoMotifs, discardNonUniquePeptides, dontAllowConsecutiveMotifs,
					useReferenceProtein);
			analyzer.run();
			System.exit(0);
		} catch (final Exception e) {
			e.printStackTrace();
			log.error(e.getMessage());
			errorInParameters();

		}
	}

	/**
	 * run analysis generating table and graphs as output
	 * 
	 * @throws IOException
	 */
	public void run() throws IOException {
		run(true, true);
	}

	public int getValidPeptides() {
		return peptidesValid;
	}

	public void run(boolean generateTable, boolean generateGraphs) throws IOException {
		List<QuantifiedPeptideInterface> peptides = null;
		log.info("Reading input file '" + inputFile.getAbsolutePath() + "'...");
		final InputDataReader q = new InputDataReader(inputFile, luciphorFile, proteinOfInterestACC, intensityThreshold,
				amountType, normalizeReplicates, this.motifRegexp, this.discardWrongPositionedPTMs,
				this.fixWrongPositionedPTMs, this.discardPeptidesWithNoMotifs);
		try {
			peptides = q.runReader();
		} catch (final QuantParserException e) {
			e.printStackTrace();
			log.error(e.getMessage());
			System.exit(-1);
		}
		if (peptides != null && !peptides.isEmpty()) {
			log.info(peptides.size() + " peptides readed from input file.");
		} else {
			log.error("Some error occurrer because no peptides where readed.");
			System.exit(-1);
		}

		log.info("Now analyzing the " + peptides.size() + " peptides...");
		final GlycoPTMPeptideAnalyzer glycoPTMPeptideAnalyzer = new GlycoPTMPeptideAnalyzer(peptides,
				proteinOfInterestACC, amountType, this.motifRegexp, this.dontAllowConsecutiveMotifs,
				this.referenceProteinSequence);
		final List<GlycoSite> hivPositions = glycoPTMPeptideAnalyzer.getGlycoSites();
		log.info(
				"Analysis resulted in " + hivPositions.size() + " positions in protein '" + proteinOfInterestACC + "'");
		resultGenerator = new GlycoPTMResultGenerator(inputFile.getParentFile(), hivPositions, this);
		resultGenerator.generateResults();
		this.averagePercentagesByPTMCode = calculateAveragePercentagesByPTMCode(hivPositions);

		log.info("Everything OK");
	}

	private TObjectDoubleMap<PTMCode> calculateAveragePercentagesByPTMCode(List<GlycoSite> hivPositions) {
		final TObjectDoubleMap<PTMCode> ret = new TObjectDoubleHashMap<PTMCode>();

		for (final PTMCode ptmCode : PTMCode.values()) {
			final TDoubleList list = new TDoubleArrayList();
			for (final GlycoSite hivPosition : hivPositions) {
				list.add(hivPosition.getProportionByPTMCode(ptmCode, isSumIntensitiesAcrossReplicates()));
			}
			ret.put(ptmCode, Maths.mean(list));

		}
		return ret;
	}

	private void printWelcome() {
		final String implementationVersion = getClass().getPackage().getImplementationVersion();
		String header = "\n########################################\n" + "Running HIVPTMAnalyzer version "
				+ getVersion().toString();
		if (implementationVersion != null) {
			header += " version " + implementationVersion;
		}
		header += "...\n########################################";
		System.out.println(header);
	}

	public static AppVersion getVersion() {
		if (version == null) {
			try {
				final String tmp = PropertiesUtil
						.getProperties(new ClassPathResource(AppVersion.APP_PROPERTIES).getInputStream())
						.getProperty("assembly.dir");
				if (tmp.contains("v")) {
					version = new AppVersion(tmp.split("v")[1]);
				} else {
					version = new AppVersion(tmp);
				}
			} catch (final Exception e) {
				e.printStackTrace();
			}
		}
		return version;

	}

	public static void errorInParameters() {
		// automatically generate the help statement
		final HelpFormatter formatter = new HelpFormatter();

		formatter.printHelp("`java -jar HIVPTMAnalyzer.jar", "\n\n\n", options,
				"\n\nContact Salvador Martinez-Bartolome at salvador@scripps.edu for more help");

		System.exit(-1);
	}

	/**
	 * Gets the average of the percentages across all the {@link GlycoSite} for that
	 * {@link PTMCode}
	 * 
	 * @param ptmCode
	 * @return
	 */
	public double getAveragePercentage(PTMCode ptmCode) {
		return averagePercentagesByPTMCode.get(ptmCode);
	}

	public void deleteGraphs() {
		this.resultGenerator.deleteGraphs();
	}

	@Override
	public File getInputFile() {
		return this.inputFile;
	}

	@Override
	public String getProteinOfInterestACC() {
		return this.proteinOfInterestACC;
	}

	@Override
	public File getFastaFile() {
		return this.fastaFile;
	}

	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public Double getIntensityThreshold() {
		return this.intensityThreshold;
	}

	@Override
	public AmountType getAmountType() {
		return this.amountType;
	}

	@Override
	public Boolean isNormalizeReplicates() {
		return this.normalizeReplicates;
	}

	@Override
	public Boolean isSumIntensitiesAcrossReplicates() {
		return this.sumIntensitiesAcrossReplicates;
	}

	@Override
	public String getMotifRegexp() {
		return motifRegexp;
	}

	@Override
	public Boolean isDiscardWrongPositionedPTMs() {
		return this.discardWrongPositionedPTMs;
	}

	@Override
	public Boolean isDiscardNonUniquePeptides() {
		return this.discardNonUniquePeptides;
	}

	@Override
	public Boolean isDontAllowConsecutiveMotifs() {
		return this.dontAllowConsecutiveMotifs;
	}

	@Override
	public String getReferenceProteinSequence() {
		return this.referenceProteinSequence;
	}

	@Override
	public File getLuciphorFile() {
		return this.luciphorFile;
	}

	@Override
	public Boolean isFixWrongPositionedPTMs() {
		return this.fixWrongPositionedPTMs;
	}

	@Override
	public Boolean isDiscardPeptidesWithNoMotifs() {
		return this.discardPeptidesWithNoMotifs;
	}
}
