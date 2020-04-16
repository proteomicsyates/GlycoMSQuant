package edu.scripps.yates.glycomsquant;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.List;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.log4j.Logger;
import org.springframework.core.io.ClassPathResource;

import edu.scripps.yates.census.read.model.interfaces.QuantifiedPeptideInterface;
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
	public static String DEFAULT_PROTEIN_OF_INTEREST = "BG505_SOSIP_gp140";
	private final File inputFile;
	private final String proteinOfInterestACC;
	private final File fastaFile;
	private final double fakePTM;
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
	private final boolean calculateProportionsByPeptidesFirst;

	private static final DecimalFormat f = new DecimalFormat("#.#E0");
	public static final String DEFAULT_PROTEIN_OF_INTEREST_SEQUENCE = "TGAENLWVTVYYGVPVWKDAETTLFCASDAKAYETEKHNVWATHACVPTDPNPQEIHLENVTEEFNMWKNNMVEQMHTDIISLWDQSLKPCVKLTPLCVTLQCTNVTNNITDDMRGELKNCSFNMTTELRDKKQKVYSLFYRLDVVQINENQGNRSNNSNKEYRLINCNTSAITQACPKVSFEPIPIHYCAPAGFAILKCKDKKFNGTGPCPSVSTVQCTHGIKPVVSTQLLLNGSLAEEEVMIRSENITNNAKNILVQFNTPVQINCTRPNNNTRKSIRIGPGQAFYATGDIIGDIRQAHCNVSKATWNETLGKVVKQLRKHFGNNTIIRFANSSGGDLEVTTHSFNCGGEFFYCNTSGLFNSTWISNTSVQGSNSTGSNDSITLPCRIKQIINMWQRIGQAMYAPPIQGVIRCVSNITGLILTRDGGSTNSTTETFRPGGGDMRDNWRSELYKYKVVKIEPLGVAPTRCKRRVVGRRRRRRAVGIGAVFLGFLGAAGSTMGAASMTLTVQARNLLSGIVQQQSNLLRAPEAQQHLLKLTVWGIKQLQARVLAVERYLRDQQLLGIWGCSGKLICCTNVPWNSSWSNRNLSEIWDNMTWLQWDKEISNYTQIIYGLLEESQNQQEKNEQDLLALDGTKHHHHHH";

	public GlycoPTMAnalyzer(InputParameters inputParams) {
		CurrentInputParameters.getInstance().setInputParameters(inputParams);
		this.inputFile = inputParams.getInputFile();
		this.proteinOfInterestACC = inputParams.getProteinOfInterestACC();
		this.fastaFile = inputParams.getFastaFile();
		this.fakePTM = inputParams.getFakePTM();
		this.name = inputParams.getName();
		this.intensityThreshold = inputParams.getIntensityThreshold();
		this.amountType = inputParams.getAmountType();
		this.normalizeReplicates = inputParams.isNormalizeReplicates();
		this.calculateProportionsByPeptidesFirst = inputParams.isCalculateProportionsByPeptidesFirst();
		printWelcome();
	}

	public GlycoPTMAnalyzer(File inputFile, String proteinOfInterestACC, File fastaFile, double fakePTM, String prefix,
			String suffix, double intensityThreshold, AmountType amountType, boolean normalizeExperimentsByProtein,
			boolean calculateProportionsByPeptidesFirst) {
		this.inputFile = inputFile;
		this.proteinOfInterestACC = proteinOfInterestACC;
		this.fastaFile = fastaFile;
		this.fakePTM = fakePTM;
		this.name = prefix;
		this.intensityThreshold = intensityThreshold;
		this.amountType = amountType;
		this.normalizeReplicates = normalizeExperimentsByProtein;

		this.calculateProportionsByPeptidesFirst = calculateProportionsByPeptidesFirst;
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

		final Option option5 = new Option("int", "intensityThreshold", true,
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

		final Option option8 = new Option("pep", "analysis_by_peptides", true,
				"[OPTIONAL] If present, boolean parameters (true/false) to set whether to calculate PTM amount ratios per peptides or not.");
		option8.setRequired(false);
		options.addOption(option8);

	}

	public static void main(String[] args) {

		final AppVersion version = getVersion();
		System.out.println("Running HIVPTMAnalysis version " + version.toString());
		setupCommandLineOptions();
		final CommandLineParser parser = new BasicParser();
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

			final double fakePTM = QuantCompare2PCQInputTSV.DEFAULT_FAKE_PTM;

			String prefix = "";
			if (cmd.hasOption("pre")) {
				prefix = cmd.getOptionValue("pre");
			}
			String suffix = "";
			if (cmd.hasOption("suf")) {
				suffix = cmd.getOptionValue("suf");
			}
			double intensityThreshold = 0.0;
			if (cmd.hasOption("int")) {
				intensityThreshold = Double.valueOf(cmd.getOptionValue("int"));
			}

			final AmountType amountType = AmountType.INTENSITY;
			boolean normalizeExperimentsByProtein = false;
			if (cmd.hasOption("norm")) {
				normalizeExperimentsByProtein = true;
			}

			final boolean analysisByPeptides = true;
			if (cmd.hasOption("pep")) {
				normalizeExperimentsByProtein = Boolean.valueOf(cmd.getOptionValue("pep"));
			}

			final GlycoPTMAnalyzer analyzer = new GlycoPTMAnalyzer(inputFile, proteinOfInterestACC, fastaFile, fakePTM,
					prefix, suffix, intensityThreshold, amountType, normalizeExperimentsByProtein, analysisByPeptides);
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
		final QuantCompareReader q = new QuantCompareReader(inputFile, proteinOfInterestACC, fakePTM,
				intensityThreshold, amountType, normalizeReplicates);
		try {
			peptides = q.runReader();
		} catch (final IOException e) {
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
				proteinOfInterestACC, fastaFile, amountType);
		final List<GlycoSite> hivPositions = glycoPTMPeptideAnalyzer.getHIVPositions();
		log.info(
				"Analysis resulted in " + hivPositions.size() + " positions in protein '" + proteinOfInterestACC + "'");
		resultGenerator = new GlycoPTMResultGenerator(inputFile.getParentFile(), name, hivPositions,
				this.calculateProportionsByPeptidesFirst);
		resultGenerator.generateResults();
		this.averagePercentagesByPTMCode = calculateAveragePercentagesByPTMCode(hivPositions);

		log.info("Everything OK");
	}

	private TObjectDoubleMap<PTMCode> calculateAveragePercentagesByPTMCode(List<GlycoSite> hivPositions) {
		final TObjectDoubleMap<PTMCode> ret = new TObjectDoubleHashMap<PTMCode>();

		for (final PTMCode ptmCode : PTMCode.values()) {
			final TDoubleList list = new TDoubleArrayList();
			for (final GlycoSite hivPosition : hivPositions) {
				list.add(hivPosition.getPercentageByPTMCode(ptmCode, isCalculateProportionsByPeptidesFirst()));
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
	public double getFakePTM() {
		return this.fakePTM;
	}

	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public double getIntensityThreshold() {
		return this.intensityThreshold;
	}

	@Override
	public AmountType getAmountType() {
		return this.amountType;
	}

	@Override
	public boolean isNormalizeReplicates() {
		return this.normalizeReplicates;
	}

	@Override
	public boolean isCalculateProportionsByPeptidesFirst() {
		return this.calculateProportionsByPeptidesFirst;
	}
}
