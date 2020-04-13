package edu.scripps.yates.glycomsquant.gui.files;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;

import com.google.common.io.Files;

public class FileManager {
	private final static Logger log = Logger.getLogger(FileManager.class);
	private static File resultRootFolder;
	public final static SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy_MM_dd HH_mm_ss");

	public static List<File> getResultFolders() {
		final List<File> ret = new ArrayList<File>();
		final File resultsFolder = getResultRootFolder();
		final File[] dateFolders = resultsFolder.listFiles();
		for (final File folder : dateFolders) {
			if (folder.isDirectory()) {
				if (containsResults(folder)) {
					ret.add(folder);
				}
			}
		}
		return ret;
	}

	private static boolean containsResults(File folder) {
		final Date date = getDateFromFolderName(folder);
		if (date == null) {
			return false;
		}
		if (!folder.isDirectory()) {
			return false;
		}

		final File[] listFiles = folder.listFiles();
		for (final File file : listFiles) {
			if (FilenameUtils.getBaseName(file.getName()).contains("_resultTable_")) {
				if (FilenameUtils.getExtension(file.getAbsolutePath()).equals("txt")) {
					return true;
				}
			}
		}
		return false;
	}

	public static Date getDateFromFolderName(File folder) {
		final String name = FilenameUtils.getName(folder.getAbsolutePath());
		Date date;
		try {
			date = dateFormatter.parse(name);
			return date;
		} catch (final ParseException e) {
		}
		return null;
	}

	public static File getResultRootFolder() {
		if (resultRootFolder == null) {
			resultRootFolder = new File(System.getProperty("user.dir"));
		}
		return resultRootFolder;
	}

	public static void setRootFolder(File resultFolder) {
		FileManager.resultRootFolder = resultFolder;
	}

	public static File getNewIndividualResultFolder() {
		final File resultRootFolder2 = getResultRootFolder();
		final String stringDateTime = getStringDateTime(new Date());
		File newFolder = new File(resultRootFolder2.getAbsolutePath() + File.separator + stringDateTime);
		if (!newFolder.exists()) {
			newFolder.mkdirs();
		} else {
			int i = 2;
			while (newFolder.exists()) {
				newFolder = new File(newFolder.getAbsolutePath() + "_" + i);
				i++;
			}
		}
		return newFolder;
	}

	private static String getStringDateTime(Date date) {
		return dateFormatter.format(date);
	}

	/**
	 * Copy input data file into the results folder and updates properties on it
	 * 
	 * @param inputDataFile
	 * @param resultsFolder
	 * @throws IOException
	 */
	public static void copyInputDataFileToResultsFolder(File inputDataFile, File resultsFolder) throws IOException {

		final File to = new File(resultsFolder.getAbsolutePath() + File.separator
				+ FilenameUtils.getName(inputDataFile.getAbsolutePath()));
		Files.copy(inputDataFile, to);
		log.info("Input data file copied to : " + to.getAbsolutePath());
		ResultsProperties.getResultsProperties(resultsFolder).setInputDataFile(inputDataFile);
	}

}
