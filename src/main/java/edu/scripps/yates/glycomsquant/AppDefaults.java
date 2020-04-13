package edu.scripps.yates.glycomsquant;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;

import edu.scripps.yates.utilities.properties.PropertiesUtil;

public class AppDefaults {
	private static AppDefaults instance;
	private static final String FASTA_PROPERTY = "fasta";
	private static final String INPUT_FILE_PROPERTY = "input_file";
	private static final String PROTEIN_PROPERTY = "protein";
	private static final String PROPERTIES_FILE = "HIVPTMAnalyzer_defaults.properties";
	private static final String RUN_NAME = "run_name";
	private String fasta;
	private String inputFile;
	private Properties properties;
	private File propertiesFile;
	private String protein;
	private String runName;
	private final static String comments = "# properties file from HIVPTMAnalyzer";

	private AppDefaults() {
		try {
			propertiesFile = new File(System.getProperty("user.dir") + File.separator + PROPERTIES_FILE);
			if (propertiesFile.exists()) {
				properties = PropertiesUtil.getProperties(propertiesFile);
				if (properties.containsKey(FASTA_PROPERTY)) {
					this.fasta = properties.getProperty(FASTA_PROPERTY);
				}
				if (properties.containsKey(INPUT_FILE_PROPERTY)) {
					this.inputFile = properties.getProperty(INPUT_FILE_PROPERTY);
				}
				if (properties.containsKey(PROTEIN_PROPERTY)) {
					this.protein = properties.getProperty(PROTEIN_PROPERTY);
				}
				if (properties.containsKey(RUN_NAME)) {
					this.runName = properties.getProperty(RUN_NAME);
				}
			} else {
				// create file
				final FileWriter fw = new FileWriter(propertiesFile);
				fw.write(comments);
				fw.close();
				properties = PropertiesUtil.getProperties(propertiesFile);
			}
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}

	public void setFasta(String fasta) {
		this.fasta = fasta;
		properties.put(FASTA_PROPERTY, fasta);
		try {
			savePropetiesFile();
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}

	public void setInputFile(String inputFile) {
		this.inputFile = inputFile;
		properties.put(INPUT_FILE_PROPERTY, inputFile);
		try {
			savePropetiesFile();
		} catch (final IOException e) {
			e.printStackTrace();
		}

	}

	public void setProteinOfInterest(String protein) {
		this.protein = protein;
		properties.put(PROTEIN_PROPERTY, protein);
		try {
			savePropetiesFile();
		} catch (final IOException e) {
			e.printStackTrace();
		}

	}

	public void setRunName(String runName) {
		this.runName = runName;
		properties.put(RUN_NAME, runName);
		try {
			savePropetiesFile();
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}

	private synchronized void savePropetiesFile() throws IOException {
		properties.store(new FileWriter(this.propertiesFile), comments);
	}

	public static AppDefaults getInstance() {
		if (instance == null) {
			instance = new AppDefaults();
		}
		return instance;
	}

	public String getFasta() {
		return fasta;
	}

	public String getInputFile() {
		return inputFile;
	}

	public String getProteinOfInterest() {
		return protein;
	}

	public String getRunName() {
		return runName;
	}
}
