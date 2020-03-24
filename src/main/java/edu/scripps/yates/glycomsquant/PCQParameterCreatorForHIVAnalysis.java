package edu.scripps.yates.glycomsquant;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import org.apache.commons.io.FilenameUtils;

public class PCQParameterCreatorForHIVAnalysis {

	private static final String FASTA_FILE = "fastaFile";
	private static final String DEFAULT_PCQ_FILE = "pcq.properties";
	private static final String INPUT_FILE_PATH = "inputFilePath";
	private static final String INPUT_FILES = "inputFiles";
	private static final String UNIPROT_RELEASES_FOLDER = "uniprotReleasesFolder";
	private static final File DEFAULT_UNIPROT_RELEASES_FOLDER = new File("Z:\\\\share\\\\Salva\\\\data\\\\uniprotKB");
	private static final String OUTPUT_FILE_PATH = "outputFilePath";
	private static final String PREFIX = "outputPrefix";
	private static final String SUFFIX = "outputSuffix";
	private final File pcqInputFile;
	private final File fastaFile;
	private final String prefix;
	private final String suffix;

	public PCQParameterCreatorForHIVAnalysis(File pcqInputFile, File fastaFile, String prefix, String suffix) {
		this.pcqInputFile = pcqInputFile;
		this.fastaFile = fastaFile;
		this.prefix = prefix;
		this.suffix = suffix;
	}

	public File createPCQInputParameterFile() throws IOException {
		File ret = new File(pcqInputFile.getParentFile().getAbsolutePath() + File.separator + prefix + "_pcq_" + suffix
				+ ".properties");
		PrintWriter pw = new PrintWriter(ret, "ISO-8859-1");

		InputStream is = this.getClass().getClassLoader().getResourceAsStream(DEFAULT_PCQ_FILE);
		BufferedReader br = new BufferedReader(new InputStreamReader(is));
		String line = null;
		while ((line = br.readLine()) != null) {
			if (line.contains("=")) {
				String[] split = line.split("=");
				String pcqProperty = split[0].trim();
				String value = split[1].trim();
				if (pcqProperty.equals(FASTA_FILE)) {
					value = replaceBackSlashes(fastaFile.getAbsolutePath());
				} else if (pcqProperty.equals(INPUT_FILE_PATH)) {
					value = replaceBackSlashes(pcqInputFile.getParentFile().getAbsolutePath());
				} else if (pcqProperty.equals(INPUT_FILES)) {
					value = FilenameUtils.getBaseName(pcqInputFile.getAbsolutePath()) + "["
							+ FilenameUtils.getName(pcqInputFile.getAbsolutePath()) + "]";
				} else if (pcqProperty.equals(UNIPROT_RELEASES_FOLDER)) {
					if (DEFAULT_UNIPROT_RELEASES_FOLDER.exists()) {
						value = replaceBackSlashes(DEFAULT_UNIPROT_RELEASES_FOLDER.getAbsolutePath());
					} else {
						value = replaceBackSlashes(System.getProperty("user.dir"));
					}
				} else if (pcqProperty.equals(OUTPUT_FILE_PATH)) {
					value = replaceBackSlashes(pcqInputFile.getParentFile().getAbsolutePath());
				} else if (pcqProperty.equals(PREFIX)) {
					value = prefix != null ? prefix : "";
				} else if (pcqProperty.equals(SUFFIX)) {
					value = suffix != null ? suffix : "";
				}
				pw.write(pcqProperty + " = " + value + "\n");
			} else {
				pw.write(line + "\n");
			}
		}
		pw.close();
		return ret;
	}

	private String replaceBackSlashes(String absolutePath) {
		String replace = absolutePath.replace("\\", "\\\\");
		return replace;
	}

}
