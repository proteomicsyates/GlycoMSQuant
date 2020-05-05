package edu.scripps.yates.glycomsquant.gui.tasks;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import javax.swing.SwingWorker;

import org.apache.log4j.Logger;

import edu.scripps.yates.census.read.QuantCompareParser;
import edu.scripps.yates.census.read.model.interfaces.QuantifiedPeptideInterface;
import edu.scripps.yates.glycomsquant.GlycoSite;
import edu.scripps.yates.glycomsquant.gui.MainFrame;
import edu.scripps.yates.glycomsquant.gui.files.ResultsProperties;
import edu.scripps.yates.glycomsquant.util.GlycoPTMAnalyzerUtil;
import edu.scripps.yates.glycomsquant.util.ResultsLoadedFromDisk;

public class ResultLoaderFromDisk extends SwingWorker<Void, Void> {
	public static final String RESULT_LOADER_FROM_DISK_STARTED = "resultLoaderStarted";
	public static final String RESULT_LOADER_FROM_DISK_FINISHED = "resultLoaderFinished";
	public static final String RESULT_LOADER_FROM_DISK_ERROR = "resultLoaderError";
	private final File individualResultsFolder;
	private final static Logger log = Logger.getLogger(ResultLoaderFromDisk.class);

	/**
	 * 
	 * @param individualResultsFolder individual results folder
	 */
	public ResultLoaderFromDisk(File individualResultsFolder) {
		this.individualResultsFolder = individualResultsFolder;
	}

	@Override
	protected Void doInBackground() throws Exception {
		try {
			firePropertyChange(RESULT_LOADER_FROM_DISK_STARTED, null, individualResultsFolder);

			final ResultsProperties resultsProperties = new ResultsProperties(individualResultsFolder);
			final File dataFile = resultsProperties.getGlycoSitesTableFile();

			if (dataFile == null || !dataFile.exists()) {
				throw new IllegalArgumentException("GlycoSite file in result folder '"
						+ individualResultsFolder.getAbsolutePath() + "' is not found");
			}
			final File inputDataFile = resultsProperties.getInputDataFile();
			if (inputDataFile == null || !inputDataFile.exists()) {
				throw new IllegalArgumentException("Input data file in result folder '"
						+ individualResultsFolder.getAbsolutePath() + "' is not found");
			}
			final QuantCompareParser parser = new QuantCompareParser(inputDataFile);
			MainFrame.getInstance();
			parser.setChargeSensible(MainFrame.isChargeStateSensible());
			MainFrame.getInstance();
			parser.setDecoyPattern(MainFrame.getDecoyPattern());
			MainFrame.getInstance();
			parser.setDistinguishModifiedSequences(MainFrame.isDistinguishModifiedSequences());
			MainFrame.getInstance();
			parser.setIgnoreTaxonomies(MainFrame.isIgnoreTaxonomies());
			final List<GlycoSite> glycoSites = readDataFile(dataFile, parser);
			final Boolean calculatePeptideProportionsFirst = resultsProperties.getCalculatePeptideProportionsFirst();
			final List<QuantifiedPeptideInterface> peptides = GlycoPTMAnalyzerUtil.getPeptidesFromSites(glycoSites);
			final ResultsLoadedFromDisk results = new ResultsLoadedFromDisk(resultsProperties, glycoSites, peptides,
					calculatePeptideProportionsFirst);
			firePropertyChange(RESULT_LOADER_FROM_DISK_FINISHED, null, results);
		} catch (final Exception e) {
			e.printStackTrace();
			firePropertyChange(RESULT_LOADER_FROM_DISK_ERROR, null, e.getMessage());
		}
		return null;
	}

	private List<GlycoSite> readDataFile(File dataFile, QuantCompareParser parser) throws IOException {
		log.info("Reading data file: " + dataFile.getAbsolutePath());
		final List<GlycoSite> ret = new ArrayList<GlycoSite>();
		final List<String> lines = Files.readAllLines(dataFile.toPath());
		StringBuilder sb = new StringBuilder();
		for (final String line : lines) {
			if (line.startsWith(GlycoSite.GLYCOSITE)) {
				if (!"".equals(sb.toString())) {
					final GlycoSite glycoSite = GlycoSite.readGlycoSiteFromString(sb.toString(), parser);
					ret.add(glycoSite);
					sb = new StringBuilder();
				}
			}
			sb.append(line + "\n");
		}
		if (!"".equals(sb.toString())) {
			final GlycoSite glycoSite = GlycoSite.readGlycoSiteFromString(sb.toString(), parser);
			ret.add(glycoSite);
			sb = new StringBuilder();
		}
		log.info(ret.size() + " glycosites read from data file");
		return ret;
	}

}
