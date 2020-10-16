package edu.scripps.yates.glycomsquant.gui.files;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Properties;

import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;

import edu.scripps.yates.glycomsquant.InputParameters;
import edu.scripps.yates.glycomsquant.gui.MainFrame;
import edu.scripps.yates.utilities.properties.PropertiesUtil;
import edu.scripps.yates.utilities.proteomicsmodel.enums.AmountType;

public class ResultsProperties implements InputParameters {
	private static final Logger log = Logger.getLogger(ResultsProperties.class);
	public final static SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	private static final String DEFAULT_PROPERTIES_FILE_NAME = "results.properties";
	private static final String INPUT_DATA_FILE = "input_data_file";
	private static final String FASTA_FILE = "fasta_file";
	private static final String LUCIPHOR_FILE = "luciphor_file";
	private static final String RESULTS_TABLE_FILE = "results_table_file";
	private static final String NAME = "run_name";
	private static final String INTENSITY_THRESHOLD = "intensity_threshold";
	private static final String NORMALIZE_REPLICATES = "normalize_replicates";
	private static final String SUM_INTENSITIES_ACROSS_REPLICATES = "sum_intensities_across_replicates";
	private static final String GLYCO_SITE_TABLE_FILE = "glyco_sites_table_file";
	private static final String PROTEIN_SEQUENCE = "protein_sequence";
	private static final String PROTEIN_OF_INTEREST = "protein_of_interest";
	private static final String MOTIF_REGEXP = "motif_regexp";
	private static final String DISCARD_WRONG_POSITIONED_PTMS = "discard_peptides_with_ptms_in_wrong_motifs";
	private static final String DISCARD_NON_UNIQUE_PEPTIDES = "discard_non_unique_peptides";
	private static final String DONT_ALLOW_CONSECUTIVE_MOTIFS = "dont_allow_consecutive_motifs";
	private static final String REFERENCE_PROTEIN_SEQUENCE = "reference_protein_sequence";
	private static final String FIX_WRONG_POSITIONED_PTMS = "fix_wrong_positioned_PTMs";
	private final File individualResultsFolder;
	private File inputDataFile;
	private File resultsTableFile;
	private File fastaFile;
	private boolean loaded = false;
	private String name;
	private Double intensityThreshold;
	private Boolean normalizeReplicates;
	private File glycoSitesTableFile;
	private String proteinSequence;
	private String proteinOfInterest;
	private Boolean sumIntensitiesAcrossReplicates;
	private String motifRegexp;
	private Boolean discardWrongPositionedPTMs;
	private Boolean fixWrongPositionedPTMs;
	private Boolean discardNonUniquePeptides;
	private Boolean dontAllowConsecutiveMotifs;
	private String referenceProteinSequence;
	private File luciphorFile;

	/**
	 * 
	 * @param individualResultsFolder the folder of the individual results
	 */
	public ResultsProperties(File individualResultsFolder) {
		this.individualResultsFolder = individualResultsFolder;

		loadProperties();
	}

	private File getPropertiesFile() {

		final File propertiesFile = new File(
				individualResultsFolder.getAbsolutePath() + File.separator + DEFAULT_PROPERTIES_FILE_NAME);
		if (!propertiesFile.exists()) {
			if (!propertiesFile.getParentFile().exists()) {
				propertiesFile.getParentFile().mkdirs();
				log.info("Created folder '" + propertiesFile.getParent() + "'");
			}
		}
		return propertiesFile;
	}

	private void loadProperties() {
		if (loaded) {
			return;
		}
		try {
			final File propertiesFile = getPropertiesFile();
			if (propertiesFile.exists()) {
				final Properties properties = PropertiesUtil.getProperties(propertiesFile);
				if (properties.containsKey(INPUT_DATA_FILE)) {
					this.inputDataFile = new File(individualResultsFolder.getAbsolutePath() + File.separator
							+ properties.getProperty(INPUT_DATA_FILE));
				}
				if (properties.containsKey(RESULTS_TABLE_FILE)) {
					this.resultsTableFile = new File(individualResultsFolder.getAbsolutePath() + File.separator
							+ properties.getProperty(RESULTS_TABLE_FILE));
				}
				this.name = properties.getProperty(NAME);
				this.proteinOfInterest = properties.getProperty(PROTEIN_OF_INTEREST);
				if (properties.containsKey(PROTEIN_SEQUENCE)) {
					this.proteinSequence = properties.getProperty(PROTEIN_SEQUENCE);
				}
				if (properties.containsKey(INTENSITY_THRESHOLD)) {
					this.intensityThreshold = Double.valueOf(properties.getProperty(INTENSITY_THRESHOLD));
				}
				if (properties.containsKey(NORMALIZE_REPLICATES)) {
					this.normalizeReplicates = Boolean.valueOf(properties.getProperty(NORMALIZE_REPLICATES));
				}
				if (properties.containsKey(SUM_INTENSITIES_ACROSS_REPLICATES)) {
					this.sumIntensitiesAcrossReplicates = Boolean
							.valueOf(properties.getProperty(SUM_INTENSITIES_ACROSS_REPLICATES));
				}
				if (properties.containsKey(GLYCO_SITE_TABLE_FILE)) {
					this.glycoSitesTableFile = new File(individualResultsFolder.getAbsolutePath() + File.separator
							+ properties.getProperty(GLYCO_SITE_TABLE_FILE));
				}
				if (properties.containsKey(FASTA_FILE)) {
					// fasta file is not copied so it has to have a full path
					this.fastaFile = new File(properties.getProperty(FASTA_FILE));
				}
				if (properties.containsKey(LUCIPHOR_FILE)) {
					// LUCIPHOR_FILE file is copied
					this.luciphorFile = new File(individualResultsFolder.getAbsolutePath() + File.separator
							+ properties.getProperty(LUCIPHOR_FILE));
				}
				if (properties.containsKey(MOTIF_REGEXP)) {
					// fasta file is not copied so it has to have a full path
					this.motifRegexp = properties.getProperty(MOTIF_REGEXP);
				}
				if (properties.containsKey(DISCARD_WRONG_POSITIONED_PTMS)) {
					this.discardWrongPositionedPTMs = Boolean
							.valueOf(properties.getProperty(DISCARD_WRONG_POSITIONED_PTMS));
				}
				if (properties.containsKey(FIX_WRONG_POSITIONED_PTMS)) {
					this.fixWrongPositionedPTMs = Boolean.valueOf(properties.getProperty(FIX_WRONG_POSITIONED_PTMS));
				}
				if (properties.containsKey(DISCARD_NON_UNIQUE_PEPTIDES)) {
					this.discardNonUniquePeptides = Boolean
							.valueOf(properties.getProperty(DISCARD_NON_UNIQUE_PEPTIDES));
				}
				if (properties.containsKey(DONT_ALLOW_CONSECUTIVE_MOTIFS)) {
					this.dontAllowConsecutiveMotifs = Boolean
							.valueOf(properties.getProperty(DONT_ALLOW_CONSECUTIVE_MOTIFS));
				}
				if (properties.containsKey(REFERENCE_PROTEIN_SEQUENCE)) {
					this.referenceProteinSequence = properties.getProperty(REFERENCE_PROTEIN_SEQUENCE);
				}
				loaded = true;
			}
		} catch (final IOException e) {
			e.printStackTrace();
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}

	public void setInputDataFile(File inputDataFile) {
		this.inputDataFile = inputDataFile;
		updateProperties();
	}

	public void setLuciphorFile(File luciphorFile) {
		this.luciphorFile = luciphorFile;
		updateProperties();
	}

	public void setResultsTableFile(File resultsTableFile) {
		this.resultsTableFile = resultsTableFile;
		updateProperties();
	}

	public void setFastaFile(File fastaFile) {
		this.fastaFile = fastaFile;
		updateProperties();
	}

	public void setMotifRegexp(String motifRegexp) {
		this.motifRegexp = motifRegexp;
		updateProperties();
	}

	public void setName(String name) {
		this.name = name;
		updateProperties();
	}

	@Override
	public File getInputFile() {
		loadProperties();
		return this.inputDataFile;
	}

	@Override
	public File getLuciphorFile() {
		loadProperties();
		return this.luciphorFile;
	}

	public File getResultsTableFile() {
		loadProperties();
		return this.resultsTableFile;
	}

	public String getProteinSequence() {
		loadProperties();
		return this.proteinSequence;
	}

	public void setProteinSequence(String sequence) {
		this.proteinSequence = sequence;
		updateProperties();
	}

	private void updateProperties() {
		final File propertiesFile = getPropertiesFile();

		try {
			if (!propertiesFile.exists()) {
				propertiesFile.createNewFile();
			}
			final Properties properties = PropertiesUtil.getProperties(propertiesFile);
			if (inputDataFile != null) {
				properties.put(INPUT_DATA_FILE, FilenameUtils.getName(this.inputDataFile.getAbsolutePath()));
			}
			if (resultsTableFile != null) {
				properties.put(RESULTS_TABLE_FILE, FilenameUtils.getName(this.resultsTableFile.getAbsolutePath()));
			}
			if (glycoSitesTableFile != null) {
				properties.put(GLYCO_SITE_TABLE_FILE,
						FilenameUtils.getName(this.glycoSitesTableFile.getAbsolutePath()));
			}
			if (name != null) {
				properties.put(NAME, this.name);
			}
			if (proteinSequence != null) {
				properties.put(PROTEIN_SEQUENCE, this.proteinSequence);
			}
			if (proteinOfInterest != null) {
				properties.put(PROTEIN_OF_INTEREST, this.proteinOfInterest);
			}

			if (resultsTableFile != null) {
				properties.put(RESULTS_TABLE_FILE, FilenameUtils.getName(this.resultsTableFile.getAbsolutePath()));
			}
			if (intensityThreshold != null) {
				properties.put(INTENSITY_THRESHOLD, String.valueOf(this.intensityThreshold));
			}
			if (normalizeReplicates != null) {
				properties.put(NORMALIZE_REPLICATES, String.valueOf(this.normalizeReplicates));
			}
			if (sumIntensitiesAcrossReplicates != null) {
				properties.put(SUM_INTENSITIES_ACROSS_REPLICATES, String.valueOf(this.sumIntensitiesAcrossReplicates));
			}
			if (fastaFile != null) {
				properties.put(FASTA_FILE, this.fastaFile.getAbsolutePath());
			}
			if (luciphorFile != null) {
				properties.put(LUCIPHOR_FILE, FilenameUtils.getName(this.luciphorFile.getAbsolutePath()));
			}
			if (motifRegexp != null) {
				properties.put(MOTIF_REGEXP, this.motifRegexp);
			}
			if (discardWrongPositionedPTMs != null) {
				properties.put(DISCARD_WRONG_POSITIONED_PTMS, String.valueOf(discardWrongPositionedPTMs));
			}
			if (fixWrongPositionedPTMs != null) {
				properties.put(FIX_WRONG_POSITIONED_PTMS, String.valueOf(fixWrongPositionedPTMs));
			}
			if (discardNonUniquePeptides != null) {
				properties.put(DISCARD_NON_UNIQUE_PEPTIDES, String.valueOf(discardNonUniquePeptides));
			}
			if (dontAllowConsecutiveMotifs != null) {
				properties.put(DONT_ALLOW_CONSECUTIVE_MOTIFS, String.valueOf(dontAllowConsecutiveMotifs));
			}
			if (referenceProteinSequence != null) {
				properties.put(REFERENCE_PROTEIN_SEQUENCE, referenceProteinSequence);
			}
			final FileWriter writer = new FileWriter(propertiesFile);
			properties.store(writer, "Properties of the GlycoMSAnalyzer run performed on "
					+ dateFormatter.format(FileManager.getDateFromFolderName(individualResultsFolder)));
			writer.close();
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public String getName() {
		loadProperties();
		return this.name;
	}

	public void setIntensityThreshold(double intensityThreshold) {
		this.intensityThreshold = intensityThreshold;
		updateProperties();

	}

	public void setNormalizeReplicates(Boolean normalizeReplicates) {
		this.normalizeReplicates = normalizeReplicates;
		updateProperties();
	}

	public void setSumIntensitiesAcrossReplicates(Boolean sumIntensitiesAcrossReplicates) {
		this.sumIntensitiesAcrossReplicates = sumIntensitiesAcrossReplicates;
		updateProperties();
	}

	@Override
	public Double getIntensityThreshold() {
		loadProperties();
		return intensityThreshold;
	}

	public void setGlycoSitesTableFile(File dataTableFile) {
		this.glycoSitesTableFile = dataTableFile;
		updateProperties();
	}

	public File getGlycoSitesTableFile() {
		loadProperties();
		return glycoSitesTableFile;
	}

	public void setProteinOfInterest(String proteinOfInterestACC) {
		this.proteinOfInterest = proteinOfInterestACC;
		updateProperties();
	}

	@Override
	public String getProteinOfInterestACC() {
		loadProperties();
		return proteinOfInterest;
	}

	@Override
	public Boolean isSumIntensitiesAcrossReplicates() {
		loadProperties();
		return this.sumIntensitiesAcrossReplicates;
	}

	@Override
	public AmountType getAmountType() {
		loadProperties();
		return MainFrame.getInstance().getAmountType();
	}

	@Override
	public File getFastaFile() {
		loadProperties();
		return this.fastaFile;
	}

	@Override
	public Boolean isNormalizeReplicates() {
		loadProperties();
		return normalizeReplicates;
	}

	@Override
	public String getMotifRegexp() {
		loadProperties();
		return this.motifRegexp;
	}

	@Override
	public Boolean isDiscardWrongPositionedPTMs() {
		loadProperties();
		return discardWrongPositionedPTMs;
	}

	public void setDiscardWrongPositionedPTMs(Boolean discardWrongPositionedPTMs2) {
		this.discardWrongPositionedPTMs = discardWrongPositionedPTMs2;
		updateProperties();
	}

	public void setFixWrongPositionedPTMs(Boolean fixWrongPositionedPTMs2) {
		this.fixWrongPositionedPTMs = fixWrongPositionedPTMs2;
		updateProperties();
	}

	public void setDiscardNonUniquePeptides(Boolean discardNonUniquePeptides) {
		this.discardNonUniquePeptides = discardNonUniquePeptides;
		updateProperties();
	}

	public void setReferenceProteinSequence(String proteinSequence) {
		this.referenceProteinSequence = proteinSequence;
		updateProperties();
	}

	@Override
	public Boolean isDiscardNonUniquePeptides() {
		loadProperties();
		return this.discardNonUniquePeptides;
	}

	public void setDontAllowConsecutiveMotifs(Boolean dontAllowConsecutiveMotifs) {
		this.dontAllowConsecutiveMotifs = dontAllowConsecutiveMotifs;
		updateProperties();
	}

	@Override
	public Boolean isDontAllowConsecutiveMotifs() {
		loadProperties();
		return this.dontAllowConsecutiveMotifs;
	}

	@Override
	public String getReferenceProteinSequence() {
		loadProperties();
		return this.referenceProteinSequence;
	}

	@Override
	public Boolean isFixWrongPositionedPTMs() {
		loadProperties();
		return this.fixWrongPositionedPTMs;
	}
}
