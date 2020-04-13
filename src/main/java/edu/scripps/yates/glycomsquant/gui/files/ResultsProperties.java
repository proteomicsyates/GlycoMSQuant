package edu.scripps.yates.glycomsquant.gui.files;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Properties;

import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;

import edu.scripps.yates.utilities.properties.PropertiesUtil;

public class ResultsProperties {
	private static final Logger log = Logger.getLogger(ResultsProperties.class);
	private static final String DEFAULT_PROPERTIES_FILE_NAME = "results.properties";
	private static final String INPUT_DATA_FILE = "input_data_file";
	private static final String RESULTS_TABLE_FILE = "results_table_file";
	private static final String NAME = "run_name";
	private static final String INTENSITY_THRESHOLD = "intensity_threshold";
	private static final String NORMALIZE_REPLICATES = "normalize_replicates";
	private static final String GLYCO_SITE_TABLE_FILE = "glyco_sites_table_file";
	private static final String FASTA_FILE = "fasta_file";
	private static final String PROTEIN_OF_INTEREST = "protein_of_interest";
	private static File currentIndividualFolder;
	private static ResultsProperties instance;
	public final static SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	private final File individualResultsFolder;
	private File inputDataFile;
	private File resultsTableFile;
	private boolean loaded = false;
	private String name;
	private Double intensityThreshold;
	private Boolean normalizeReplicates;
	private File glycoSitesTableFile;
	private File fastaFile;
	private String proteinOfInterest;

	public static ResultsProperties getResultsProperties(File individualResultsFolder) {
		if (!individualResultsFolder.equals(currentIndividualFolder)) {
			instance = new ResultsProperties(individualResultsFolder);
			currentIndividualFolder = individualResultsFolder;
		}
		return instance;
	}

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
				if (properties.containsKey(FASTA_FILE)) {
					this.fastaFile = new File(individualResultsFolder.getAbsolutePath() + File.separator
							+ properties.getProperty(FASTA_FILE));
				}
				if (properties.containsKey(INTENSITY_THRESHOLD)) {
					this.intensityThreshold = Double.valueOf(properties.getProperty(INTENSITY_THRESHOLD));
				}
				if (properties.containsKey(NORMALIZE_REPLICATES)) {
					this.normalizeReplicates = Boolean.valueOf(properties.getProperty(NORMALIZE_REPLICATES));
				}
				if (properties.containsKey(GLYCO_SITE_TABLE_FILE)) {
					this.glycoSitesTableFile = new File(individualResultsFolder.getAbsolutePath() + File.separator
							+ properties.getProperty(GLYCO_SITE_TABLE_FILE));
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

	public void setResultsTableFile(File resultsTableFile) {
		this.resultsTableFile = resultsTableFile;
		updateProperties();
	}

	public void setName(String name) {
		this.name = name;
		updateProperties();
	}

	public File getInputDataFile() {
		loadProperties();
		return this.inputDataFile;
	}

	public File getResultsTableFile() {
		loadProperties();
		return this.resultsTableFile;
	}

	public File getFastaFile() {
		loadProperties();
		return this.fastaFile;
	}

	public void setFastaFile(File fastaFile) {
		this.fastaFile = fastaFile;
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
			if (fastaFile != null) {
				properties.put(FASTA_FILE, FilenameUtils.getName(this.fastaFile.getAbsolutePath()));
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
			final FileWriter writer = new FileWriter(propertiesFile);
			properties.store(writer, "Properties of the GlycoMSAnalyzer run performed on "
					+ dateFormatter.format(FileManager.getDateFromFolderName(individualResultsFolder)));
			writer.close();
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}

	public String getName() {
		loadProperties();
		return this.name;
	}

	public void setIntensityThreshold(double intensityThreshold) {
		this.intensityThreshold = intensityThreshold;
		updateProperties();

	}

	public Boolean getNormalizeReplicates() {
		loadProperties();
		return normalizeReplicates;
	}

	public void setNormalizeReplicates(Boolean normalizeReplicates) {
		this.normalizeReplicates = normalizeReplicates;
		updateProperties();
	}

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

	public String getProteinOfInterest() {
		return proteinOfInterest;
	}
}
