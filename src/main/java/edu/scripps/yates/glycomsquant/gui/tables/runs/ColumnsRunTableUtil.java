package edu.scripps.yates.glycomsquant.gui.tables.runs;

import java.io.File;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.JScrollBar;
import javax.swing.JScrollPane;

import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;

import edu.scripps.yates.glycomsquant.gui.files.ResultsProperties;

public class ColumnsRunTableUtil {
	private static Logger log = Logger.getLogger("log4j.logger.org.proteored");

	private static ColumnsRunTableUtil instance;

	private final static NumberFormat threeDigitsDecimal;

	private final static DecimalFormat df;

	private final static DecimalFormat scientificDecimalFormat;

	public static final String VALUE_SEPARATOR = ",";

	public static final String PREFIX = "/";// for making the table to sort, but we have to remove it so that the path
	static {
		threeDigitsDecimal = NumberFormat.getInstance();
		threeDigitsDecimal.setMaximumFractionDigits(3);
		threeDigitsDecimal.setGroupingUsed(false);
		df = new DecimalFormat("#.##");
		scientificDecimalFormat = new DecimalFormat("0.00E00");
	}

	private ColumnsRunTableUtil(File resultsFolder) {
	}

	public static ColumnsRunTableUtil getInstance(File resultsFolder) {
		if (instance == null) {
			instance = new ColumnsRunTableUtil(resultsFolder);
		}
		return instance;
	}

	public List<Object> getRunInfoList(File runPath, List<ColumnsRunTable> list, int index) {

		final List<Object> ret = new ArrayList<Object>();
		if (runPath == null)
			throw new IllegalArgumentException("runPath is null");
		final ResultsProperties resultsProperties = new ResultsProperties(runPath);

		for (final ColumnsRunTable column : list) {

			switch (column) {
			case NUMBER:
				ret.add(index);
				break;
			case INPUT_DATA_FILE:
				ret.add(cleanString(
						String.valueOf(FilenameUtils.getName(resultsProperties.getInputFile().getAbsolutePath()))));
				break;
			case LUCIPHOR_FILE:
				if (resultsProperties.getLuciphorFile() != null) {
					ret.add(cleanString(String
							.valueOf(FilenameUtils.getName(resultsProperties.getLuciphorFile().getAbsolutePath()))));
				} else {
					ret.add(cleanString(""));
				}
				break;
			case NORMALIZE_REPLICATES:
				ret.add(resultsProperties.isNormalizeReplicates());
				break;
			case NAME:
				ret.add(cleanString(resultsProperties.getName()));
				break;
			case RUN_PATH:
				ret.add(cleanString(PREFIX + FilenameUtils.getName(runPath.getAbsolutePath())));
				break;

			case THRESHOLD:
				ret.add(resultsProperties.getIntensityThreshold());
				break;
			case SUM_INTENSITIES_ACROSS_REPLICATES:
				ret.add(resultsProperties.isSumIntensitiesAcrossReplicates());
				break;
			case DISCARD_PEPTIDES_WITH_PTMS_IN_WRONG_MOTIFS:
				ret.add(resultsProperties.isDiscardWrongPositionedPTMs());
				break;
			case FIX_PTM_POSITIONS:
				ret.add(resultsProperties.isFixWrongPositionedPTMs());
				break;
			case DISCARD_NON_UNIQUE_PEPTIDES:
				ret.add(resultsProperties.isDiscardNonUniquePeptides());
				break;
			case NOT_ALLOW_CONSECUTIVE_SITES:
				ret.add(resultsProperties.isDontAllowConsecutiveMotifs());
				break;
			case USE_REFERENCE_PROTEIN:
				ret.add(resultsProperties.getReferenceProteinSequence() != null);
				break;
			case USE_CHARGE:
				ret.add(resultsProperties.isUseCharge());
				break;
			case DISCARD_REPEATED_PEPTIDES:
				ret.add(resultsProperties.isDiscardPeptidesRepeatedInProtein());
				break;
			default:
				throw new IllegalArgumentException("Column " + column + " is not supported by this exporter");

			}

		}

		return ret;

	}

	public String getStringFromList(List<String> lineStringList, char separator) {
		final StringBuilder sb = new StringBuilder();
		if (lineStringList != null)
			for (final String obj : lineStringList) {
				if (obj != null)
					sb.append(obj);
				else
					sb.append("-");
				sb.append(separator);
			}
		return sb.toString();
	}

	private String cleanString(String string) {
		if (string == null)
			return "-";
		if ("null".equals(string))
			return "-";
		if ("".equals(string))
			return "-";

		// if doesn't have a VALUE_SEPARATOR, try to convert to Integer or to a
		// Double:

		if (!string.contains(VALUE_SEPARATOR)) {

			try {
				final Integer i = Integer.valueOf(string);
				return String.valueOf(i);
			} catch (final NumberFormatException e1) {
				try {
					final Double d = Double.valueOf(string);
					return parseDouble(d);
				} catch (final NumberFormatException e) {
					return string.trim();
				}
			}
		}

		return string.trim();

	}

	private String cleanString(Collection<String> objs) {
		final StringBuilder sb = new StringBuilder("");
		if (objs == null || objs.isEmpty())
			return "-";
		for (final String obj : objs) {
			if (!"".equals(sb.toString()))
				sb.append(VALUE_SEPARATOR);
			sb.append(obj);
		}

		return sb.toString().trim();
	}

	private String parseDouble(Double score) {
		if (score == null)
			return "-";
		// String test = DecimalFormat.getNumberInstance().format(score);
		// return test;
		String format = "";
		if (score == 0.0) {
			return "0";
		}
		if (score > 0.01) {
			format = threeDigitsDecimal.format(score);
		} else {
			// NumberFormat formater = DecimalFormat.getInstance();
			// formater.setMaximumFractionDigits(30);
			// formater.setMinimumFractionDigits(3);
			// formater.setGroupingUsed(false);
			// format = formater.format(score);

			format = scientificDecimalFormat.format(score);
		}
		// final String format = df.format(score);
		// log.info("Parsed from " + score + " to " + format);
		if ("".equals(format)) {
			return "-";
		}
		try {
			NumberFormat.getInstance().parse(format);

		} catch (final NumberFormatException e) {
			log.info("CUIDADO");
		} catch (final ParseException e) {
			log.info("CUIDADO");
		}
		return format;
	}

	private String parseFloat(Float score) {
		if (score == null)
			return "-";
		// String test = DecimalFormat.getNumberInstance().format(score);
		// return test;
		String format = "";
		if (score == 0.0)
			return "0";
		if (score > 0.01) {
			final NumberFormat formater = NumberFormat.getInstance();
			formater.setMaximumFractionDigits(3);
			formater.setGroupingUsed(false);
			format = formater.format(score);
			// df = new DecimalFormat("0.###");
		} else {
			// NumberFormat formater = DecimalFormat.getInstance();
			// formater.setMaximumFractionDigits(30);
			// formater.setMinimumFractionDigits(3);
			// formater.setGroupingUsed(false);
			// format = formater.format(score);
			format = scientificDecimalFormat.format(score);
		}
		// final String format = df.format(score);
		// log.info("Parsed from " + score + " to " + format);
		if ("".equals(format)) {
			return "-";
		}
		try {
			NumberFormat.getInstance().parse(format);

		} catch (final NumberFormatException e) {
			log.info("CUIDADO");
		} catch (final ParseException e) {
			log.info("CUIDADO");
		}
		return format;
	}

	public static void scrollToBeginning(JScrollPane scrollPane) {

		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {

				final JScrollBar bar = scrollPane.getVerticalScrollBar();
				bar.setValue(bar.getMinimum());
			}
		});

	}
}
