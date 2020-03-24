package edu.scripps.yates.glycomsquant;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;

import edu.scripps.yates.utilities.properties.PropertiesUtil;

public class AppDefaults {
	private static AppDefaults instance;
	private static final String FASTA_PROPERTY = "fasta";
	private static final String INPUT_FOLDER_PROPERTY = "input_folder";
	private static final String PROPERTIES_FILE = "HIVPTMAnalyzer_defaults.properties";
	private String fasta;
	private String inputFolder;
	private Properties properties;
	private File propertiesFile;
	private final static String comments = "# properties file from HIVPTMAnalyzer";

	private AppDefaults() {
		try {
			propertiesFile = new File(System.getProperty("user.dir") + File.separator + PROPERTIES_FILE);
			if (propertiesFile.exists()) {
				properties = PropertiesUtil.getProperties(propertiesFile);
				if (properties.containsKey(FASTA_PROPERTY)) {
					this.fasta = properties.getProperty(FASTA_PROPERTY);
				}
				if (properties.containsKey(INPUT_FOLDER_PROPERTY)) {
					this.inputFolder = properties.getProperty(INPUT_FOLDER_PROPERTY);
				}
			} else {
				// create file
				FileWriter fw = new FileWriter(propertiesFile);
				fw.write(comments);
				fw.close();
				properties = PropertiesUtil.getProperties(propertiesFile);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void setFasta(String fasta) {
		this.fasta = fasta;
		properties.put(FASTA_PROPERTY, fasta);
		try {
			savePropetiesFile();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void setInputFolder(String inputFolder) {
		this.inputFolder = inputFolder;
		properties.put(INPUT_FOLDER_PROPERTY, inputFolder);
		try {
			savePropetiesFile();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private void savePropetiesFile() throws IOException {
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

	public String getInputFolder() {
		return inputFolder;
	}
}
