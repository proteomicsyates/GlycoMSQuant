package edu.scripps.yates.glycomsquant.gui.tasks;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import javax.swing.SwingWorker;

import org.apache.log4j.Logger;

import edu.scripps.yates.census.read.model.interfaces.QuantifiedPeptideInterface;
import edu.scripps.yates.glycomsquant.GlycoSite;
import edu.scripps.yates.glycomsquant.InputDataReader;
import edu.scripps.yates.glycomsquant.gui.files.ResultsProperties;
import edu.scripps.yates.glycomsquant.util.GlycoPTMAnalyzerUtil;
import edu.scripps.yates.glycomsquant.util.ResultsLoadedFromDisk;

public class ResultLoaderFromDisk extends SwingWorker<Void, Void> {
	public static final String RESULT_LOADER_FROM_DISK_STARTED = "resultLoaderStarted";
	public static final String RESULT_LOADER_FROM_DISK_FINISHED = "resultLoaderFinished";
	public static final String RESULT_LOADER_FROM_DISK_ERROR = "resultLoaderError";
	private final File individualResultsFolder;
	private final String referenceProteinSequence;
	private final static Logger log = Logger.getLogger(ResultLoaderFromDisk.class);

	/**
	 * 
	 * @param individualResultsFolder individual results folder
	 */
	public ResultLoaderFromDisk(File individualResultsFolder, String referenceProteinSequence) {
		this.individualResultsFolder = individualResultsFolder;
		this.referenceProteinSequence = referenceProteinSequence;
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
			final File inputDataFile = resultsProperties.getInputFile();
			if (inputDataFile == null || !inputDataFile.exists()) {
				throw new IllegalArgumentException("Input data file in result folder '"
						+ individualResultsFolder.getAbsolutePath() + "' is not found");
			}
			File luciphorFile = resultsProperties.getLuciphorFile();
			if (luciphorFile == null || !luciphorFile.exists()) {
				luciphorFile = null;
			}
			final InputDataReader reader = new InputDataReader(inputDataFile, luciphorFile,
					resultsProperties.getProteinOfInterestACC(), resultsProperties.getIntensityThreshold(),
					resultsProperties.getAmountType(), resultsProperties.isNormalizeReplicates(),
					resultsProperties.getMotifRegexp(), resultsProperties.isDiscardWrongPositionedPTMs(),
					resultsProperties.isFixWrongPositionedPTMs(), resultsProperties.isDiscardPeptidesWithNoMotifs());

			final List<GlycoSite> glycoSites = readDataFile(dataFile, reader);
			final List<QuantifiedPeptideInterface> peptides = GlycoPTMAnalyzerUtil.getPeptidesFromSites(glycoSites);
			final ResultsLoadedFromDisk results = new ResultsLoadedFromDisk(resultsProperties, glycoSites, peptides);
			firePropertyChange(RESULT_LOADER_FROM_DISK_FINISHED, null, results);
		} catch (final Exception e) {
			e.printStackTrace();
			firePropertyChange(RESULT_LOADER_FROM_DISK_ERROR, null, e.getMessage());
		}
		return null;
	}

	private List<GlycoSite> readDataFile(File dataFile, InputDataReader reader) throws IOException {
		log.info("Reading data file: " + dataFile.getAbsolutePath());
		final List<GlycoSite> ret = new ArrayList<GlycoSite>();
		final List<String> lines = Files.readAllLines(dataFile.toPath());
		StringBuilder sb = new StringBuilder();
		for (final String line : lines) {
			if (line.startsWith(GlycoSite.GLYCOSITE)) {
				if (!"".equals(sb.toString())) {
					final GlycoSite glycoSite = GlycoSite.readGlycoSiteFromString(sb.toString(), reader,
							this.referenceProteinSequence);
					ret.add(glycoSite);
					sb = new StringBuilder();
				}
			}
			sb.append(line + "\n");
		}
		if (!"".equals(sb.toString())) {
			final GlycoSite glycoSite = GlycoSite.readGlycoSiteFromString(sb.toString(), reader,
					this.referenceProteinSequence);
			ret.add(glycoSite);
			sb = new StringBuilder();
		}
		log.info(ret.size() + " glycosites read from data file");
		return ret;
	}

}
