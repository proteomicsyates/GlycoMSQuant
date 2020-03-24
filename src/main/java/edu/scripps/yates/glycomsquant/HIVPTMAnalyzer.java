package edu.scripps.yates.glycomsquant;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;
import org.springframework.core.io.ClassPathResource;

import com.google.common.io.Files;

import edu.scripps.yates.pcq.ProteinClusterQuant;
import edu.scripps.yates.pcq.model.PCQPeptideNode;
import edu.scripps.yates.utilities.appversion.AppVersion;
import edu.scripps.yates.utilities.maths.Maths;
import edu.scripps.yates.utilities.properties.PropertiesUtil;
import edu.scripps.yates.utilities.proteomicsmodel.enums.AmountType;
import gnu.trove.list.TDoubleList;
import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.map.TObjectDoubleMap;
import gnu.trove.map.hash.TObjectDoubleHashMap;

public class HIVPTMAnalyzer {
	private final static Logger log = Logger.getLogger(HIVPTMAnalyzer.class);
	private static final String DEFAULT_PCQ_FILE = "pcq.params";
	private static Options options;
	private static AppVersion version;
	public static String DEFAULT_PROTEIN_OF_INTEREST = "BG505_SOSIP_gp140";
	private final File inputFile;
	private final String proteinOfInterestACC;
	private final File fastaFile;
	private final double fakePTM;
	private final String prefix;
	private final String suffix;
	private final double intensityThreshold;
	private TObjectDoubleMap<PTMCode> averagePercentages = new TObjectDoubleHashMap<PTMCode>();
	private ProteinClusterQuant pcq;
	private HIVPTMResultGenerator resultGenerator;
	private int peptidesValid;
	private final AmountType amountType;
	private final boolean normalizeExperimentsByProtein;

	private static final DecimalFormat f = new DecimalFormat("#.#E0");
	public static final String DEFAULT_PROTEIN_OF_INTEREST_SEQUENCE = "TGAENLWVTVYYGVPVWKDAETTLFCASDAKAYETEKHNVWATHACVPTDPNPQEIHLENVTEEFNMWKNNMVEQMHTDIISLWDQSLKPCVKLTPLCVTLQCTNVTNNITDDMRGELKNCSFNMTTELRDKKQKVYSLFYRLDVVQINENQGNRSNNSNKEYRLINCNTSAITQACPKVSFEPIPIHYCAPAGFAILKCKDKKFNGTGPCPSVSTVQCTHGIKPVVSTQLLLNGSLAEEEVMIRSENITNNAKNILVQFNTPVQINCTRPNNNTRKSIRIGPGQAFYATGDIIGDIRQAHCNVSKATWNETLGKVVKQLRKHFGNNTIIRFANSSGGDLEVTTHSFNCGGEFFYCNTSGLFNSTWISNTSVQGSNSTGSNDSITLPCRIKQIINMWQRIGQAMYAPPIQGVIRCVSNITGLILTRDGGSTNSTTETFRPGGGDMRDNWRSELYKYKVVKIEPLGVAPTRCKRRVVGRRRRRRAVGIGAVFLGFLGAAGSTMGAASMTLTVQARNLLSGIVQQQSNLLRAPEAQQHLLKLTVWGIKQLQARVLAVERYLRDQQLLGIWGCSGKLICCTNVPWNSSWSNRNLSEIWDNMTWLQWDKEISNYTQIIYGLLEESQNQQEKNEQDLLALDGTKHHHHHH";

	public HIVPTMAnalyzer(File inputFile, String proteinOfInterestACC, File fastaFile, double fakePTM, String prefix,
			String suffix, double intensityThreshold, AmountType amountType, boolean normalizeExperimentsByProtein) {
		this.inputFile = inputFile;
		this.proteinOfInterestACC = proteinOfInterestACC;
		this.fastaFile = fastaFile;
		this.fakePTM = fakePTM;
		this.prefix = prefix;
		this.intensityThreshold = intensityThreshold;
		String suffixString = suffix + "_" + f.format(intensityThreshold);
		this.amountType = amountType;
		this.normalizeExperimentsByProtein = normalizeExperimentsByProtein;
		if (normalizeExperimentsByProtein) {
			this.suffix = suffixString + "_norm";
		} else {
			this.suffix = suffixString;
		}
		printWelcome();

	}

	static void setupCommandLineOptions() {
		// create Options object
		options = new Options();

		Option option1 = new Option("i", "input", true,
				"[MANDATORY] Full path to the input file, which is a text file from Census quant compare.");
		option1.setRequired(true);
		options.addOption(option1);

		Option option2 = new Option("acc", "accession", true,
				"[OPTIONAL] Protein accession of the protein that will be analyzed. All the rest of the proteins in the input file will be ignored. If not provided, accession 'BG505_SOSIP_gp140' will be used.");
		option2.setRequired(false);
		options.addOption(option2);

		Option option3 = new Option("pre", "prefix", true,
				"[OPTIONAL] Prefix that will be added to the name of all the output files. ");
		option3.setRequired(false);
		options.addOption(option3);

		Option option4 = new Option("suf", "suffix", true,
				"[OPTIONAL] Suffix that will be added to the name of all the output files. ");
		option4.setRequired(false);
		options.addOption(option4);

		Option option5 = new Option("int", "intensityThreshold", true,
				"[OPTIONAL] Intensity threshold to discard all PSM with intensity below that value. If not provided, no threshold will be applied.");
		option5.setRequired(false);
		options.addOption(option5);

		Option option6 = new Option("fas", "fasta", true,
				"[MANDATORY] Full path to the fasta file in which the sequence of the protein of interest is present.");
		option6.setRequired(true);
		options.addOption(option6);

		Option option7 = new Option("norm", "normalized_intensity", false,
				"[OPTIONAL] If present, it will use normalized intensities instead of raw intensities. If not present, it will use raw intensities.");
		option7.setRequired(false);
		options.addOption(option7);

	}

	public static void main(String[] args) {

		final AppVersion version = getVersion();
		System.out.println("Running HIVPTMAnalysis version " + version.toString());
		setupCommandLineOptions();
		final CommandLineParser parser = new BasicParser();
		try {
			final CommandLine cmd = parser.parse(options, args);

			File inputFile = new File(cmd.getOptionValue('i'));
			if (!inputFile.exists()) {
				throw new IllegalArgumentException("Input file not found '" + inputFile.getAbsolutePath() + "'");
			}
			String proteinOfInterestACC = HIVPTMAnalyzer.DEFAULT_PROTEIN_OF_INTEREST;
			if (cmd.hasOption("acc")) {
				proteinOfInterestACC = cmd.getOptionValue("acc");
			}

			File fastaFile = new File(cmd.getOptionValue("fas"));

			double fakePTM = QuantCompare2PCQInputTSV.DEFAULT_FAKE_PTM;

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

			AmountType amountType = AmountType.INTENSITY;
			boolean normalizeExperimentsByProtein = false;
			if (cmd.hasOption("norm")) {
				normalizeExperimentsByProtein = true;
			}
			HIVPTMAnalyzer analyzer = new HIVPTMAnalyzer(inputFile, proteinOfInterestACC, fastaFile, fakePTM, prefix,
					suffix, intensityThreshold, amountType, normalizeExperimentsByProtein);
			analyzer.run();
			System.exit(0);
		} catch (Exception e) {
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
		File pcqInputFile = null;
		log.info("Reading input file '" + inputFile.getAbsolutePath() + "'...");
		final QuantCompare2PCQInputTSV q = new QuantCompare2PCQInputTSV(inputFile, proteinOfInterestACC, fakePTM,
				intensityThreshold, amountType, normalizeExperimentsByProtein, suffix);
		try {
			pcqInputFile = q.run();
			peptidesValid = q.getPsmsValid();
		} catch (final IOException e) {
			e.printStackTrace();
			log.error(e.getMessage());
			System.exit(-1);
		}
		if (pcqInputFile != null && pcqInputFile.exists()) {
			log.info("PCQ input file created succesfully at: " + pcqInputFile.getAbsolutePath());
		} else {
			log.error("Some error occurrer because pcq input file was not generated");
			System.exit(-1);
		}
		log.info("Creating PCQ parameters file...");
		// create PCQ param file
		PCQParameterCreatorForHIVAnalysis pcqParamCreator = new PCQParameterCreatorForHIVAnalysis(pcqInputFile,
				fastaFile, prefix, suffix);
		File setupPropertiesFile = pcqParamCreator.createPCQInputParameterFile();
		log.info("PCQ parameters file created at '" + setupPropertiesFile.getAbsolutePath() + "'");

		// run pcq
		log.info("Running PCQ...");
		pcq = new ProteinClusterQuant(setupPropertiesFile, true);
		pcq.run();

		File peptideNodeTableFile = pcq.getFinalPeptideNodeTableFile();
		if (peptideNodeTableFile == null || !peptideNodeTableFile.exists()) {
			throw new IllegalArgumentException("PCQ output file is not present. Some error may have ocurred.");
		}
		// copy file to the main folder
		File to = new File(setupPropertiesFile.getParentFile().getAbsolutePath() + File.separator
				+ FilenameUtils.getBaseName(peptideNodeTableFile.getAbsolutePath()) + ".txt");
		Files.copy(peptideNodeTableFile, to);
		log.info("PCQ run correctly and output Peptide Node table file is saved to '" + to.getAbsolutePath() + "'");

		List<PCQPeptideNode> peptideNodes = new ArrayList<PCQPeptideNode>();
		pcq.getClusterSet().stream().map(c -> c.getPeptideNodes()).forEach(set -> peptideNodes.addAll(set));
		log.info("Now analyzing the " + peptideNodes.size() + " peptide nodes generated by PCQ...");
		PCQPeptideNodeHIVPTMAnalyzer pcqOutputAnalyzer = new PCQPeptideNodeHIVPTMAnalyzer(peptideNodes);
		List<HIVPosition> hivPositions = pcqOutputAnalyzer.getHIVPositions(proteinOfInterestACC);
		log.info(
				"Analysis resulted in " + hivPositions.size() + " positions in protein '" + proteinOfInterestACC + "'");
		resultGenerator = new HIVPTMResultGenerator(pcqInputFile.getParentFile(), prefix, suffix, hivPositions,
				intensityThreshold);
		resultGenerator.generateResults(true, true);
		this.averagePercentages = getAveragePercentages(hivPositions);

		log.info("Everything OK");
	}

	private TObjectDoubleMap<PTMCode> getAveragePercentages(List<HIVPosition> hivPositions) {
		TObjectDoubleMap<PTMCode> ret = new TObjectDoubleHashMap<PTMCode>();

		for (PTMCode ptmCode : PTMCode.values()) {
			TDoubleList list = new TDoubleArrayList();
			for (HIVPosition hivPosition : hivPositions) {
				list.add(hivPosition.getPercentageOfAveragesByPTMCode(ptmCode));
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

	public double getAveragePercentage(PTMCode ptmCode) {
		return averagePercentages.get(ptmCode);
	}

	public void deletePCQRunFolder() throws IOException {
		File parentFile = pcq.getFinalPeptideNodeTableFile().getParentFile();
		FileUtils.deleteDirectory(parentFile);
		File upperFolder = parentFile.getParentFile();
		while (upperFolder.listFiles().length == 0) {
			FileUtils.deleteDirectory(upperFolder);
			upperFolder = upperFolder.getParentFile();
		}
	}

	public void deleteGraphs() {
		this.resultGenerator.deleteGraphs();
	}
}
