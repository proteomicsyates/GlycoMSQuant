package edu.scripps.yates.glycomsquant.gui.tables.comparison;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.JScrollBar;
import javax.swing.JScrollPane;

import org.apache.log4j.Logger;

import edu.scripps.yates.glycomsquant.comparison.MyMannWhitneyTestResult;

public class ColumnsComparisonTableUtil {
	private static Logger log = Logger.getLogger("log4j.logger.org.proteored");

	private static ColumnsComparisonTableUtil instance;

	private final static NumberFormat threeDigitsDecimal;

	private final static DecimalFormat df;

	private final static DecimalFormat scientificDecimalFormat;

	public static final String VALUE_SEPARATOR = ", ";
	static {
		threeDigitsDecimal = NumberFormat.getInstance();
		threeDigitsDecimal.setMaximumFractionDigits(3);
		threeDigitsDecimal.setGroupingUsed(false);
		df = new DecimalFormat("#.##");
		scientificDecimalFormat = new DecimalFormat("0.00E00");
	}

	private ColumnsComparisonTableUtil() {
	}

	public static ColumnsComparisonTableUtil getInstance() {
		if (instance == null) {
			instance = new ColumnsComparisonTableUtil();
		}
		return instance;
	}

	public List<Object> getMyMannWhitneyTestResultInfoList(MyMannWhitneyTestResult comparison,
			List<ColumnsComparisonTable> columns) {

		final List<Object> ret = new ArrayList<Object>();
		if (comparison == null)
			throw new IllegalArgumentException("comparison is null");

		for (final ColumnsComparisonTable column : columns) {

			switch (column) {

			case ADJUSTED_P_VALUE:

				ret.add(comparison.getCorrectedPValue());
				break;
			case POSITION:
				ret.add(comparison.getPosition());
				break;
			case P_VALUE:
				ret.add(comparison.getPValue());
				break;

			case PROPORTION_1:
				ret.add(comparison.getXMean());
				break;
			case PROPORTION_2:
				ret.add(comparison.getYMean());
				break;

			case SEM_1:
				ret.add(comparison.getXSem());
				break;
			case SEM_2:
				ret.add(comparison.getYMean());
				break;
			case PTM:
				ret.add(comparison.getPtm().getCode());

				break;
			case SIGNIFICANCY:
				ret.add(MyMannWhitneyTestResult.printSignificanceAsterisks(comparison.getCorrectedPValue()));

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
