package edu.scripps.yates.glycomsquant.gui.tables.sites;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.JScrollBar;
import javax.swing.JScrollPane;

import org.apache.log4j.Logger;

import edu.scripps.yates.glycomsquant.GlycoSite;
import edu.scripps.yates.glycomsquant.PTMCode;
import edu.scripps.yates.utilities.strings.StringUtils;
import gnu.trove.list.TIntList;

public class ColumnsSitesTableUtil {
	private static Logger log = Logger.getLogger("log4j.logger.org.proteored");

	private static ColumnsSitesTableUtil instance;

	private final static NumberFormat threeDigitsDecimal;

	private final static DecimalFormat df;

	private final static DecimalFormat scientificDecimalFormat;

	public static final String VALUE_SEPARATOR = ",";
	static {
		threeDigitsDecimal = NumberFormat.getInstance();
		threeDigitsDecimal.setMaximumFractionDigits(3);
		threeDigitsDecimal.setGroupingUsed(false);
		df = new DecimalFormat("#.##");
		scientificDecimalFormat = new DecimalFormat("0.00E00");
	}

	private ColumnsSitesTableUtil() {
	}

	public static ColumnsSitesTableUtil getInstance() {
		if (instance == null) {
			instance = new ColumnsSitesTableUtil();
		}
		return instance;
	}

	public List<Object> getGlycoSiteInfoList(GlycoSite glycoSite, boolean sumIntensitiesAcrossReplicates,
			List<ColumnsSitesTable> list) {

		final List<Object> ret = new ArrayList<Object>();
		if (glycoSite == null)
			throw new IllegalArgumentException("glycoSite is null");

		for (final ColumnsSitesTable column : list) {

			switch (column) {
			case SITE:
				ret.add(glycoSite.getPosition());
				break;
			case AVG_2:
				ret.add(glycoSite.getAverageIntensityByPTMCode(PTMCode._2));
				break;
			case AVG_203:
				ret.add(glycoSite.getAverageIntensityByPTMCode(PTMCode._203));
				break;
			case AVG_NoPTM:
				ret.add(glycoSite.getAverageIntensityByPTMCode(PTMCode._0));
				break;
			case PEPTIDES_2:
				ret.add(glycoSite.getPeptidesByPTMCode(PTMCode._2).size());
				break;

			case PEPTIDES_203:
				ret.add(glycoSite.getPeptidesByPTMCode(PTMCode._203).size());
				break;
			case PEPTIDES_NoPTM:
				ret.add(glycoSite.getPeptidesByPTMCode(PTMCode._0).size());
				break;
			case AVG_PROPORTIONS_2:
				ret.add(glycoSite.getAvgProportionByPTMCode(PTMCode._2, sumIntensitiesAcrossReplicates));
				break;

			case AVG_PROPORTIONS_203:
				ret.add(glycoSite.getAvgProportionByPTMCode(PTMCode._203, sumIntensitiesAcrossReplicates));
				break;
			case AVG_PROPORTIONS_NoPTM:
				ret.add(glycoSite.getAvgProportionByPTMCode(PTMCode._0, sumIntensitiesAcrossReplicates));
				break;
			case MEDIAN_PROPORTIONS_NoPTM:
				ret.add(glycoSite.getMedianProportionByPTMCode(PTMCode._0, sumIntensitiesAcrossReplicates));
				break;
			case MEDIAN_PROPORTIONS_2:
				ret.add(glycoSite.getMedianProportionByPTMCode(PTMCode._2, sumIntensitiesAcrossReplicates));
				break;
			case MEDIAN_PROPORTIONS_203:
				ret.add(glycoSite.getMedianProportionByPTMCode(PTMCode._203, sumIntensitiesAcrossReplicates));
				break;
			case SEM_2:
				ret.add(glycoSite.getSEMIntensityByPTMCode(PTMCode._2));
				break;
			case SEM_203:
				ret.add(glycoSite.getSEMIntensityByPTMCode(PTMCode._203));
				break;
			case SEM_NoPTM:
				ret.add(glycoSite.getSEMIntensityByPTMCode(PTMCode._0));
				break;
			case SEM_PERCENT_2:
				ret.add(glycoSite.getSEMOfProportionsByPTMCode(PTMCode._2, sumIntensitiesAcrossReplicates));
				break;
			case SEM_PERCENT_203:
				ret.add(glycoSite.getSEMOfProportionsByPTMCode(PTMCode._203, sumIntensitiesAcrossReplicates));
				break;
			case SEM_PERCENT_NoPTM:
				ret.add(glycoSite.getSEMOfProportionsByPTMCode(PTMCode._0, sumIntensitiesAcrossReplicates));
				break;
			case SPC_2:
				ret.add(glycoSite.getSPCByPTMCode(PTMCode._2));
				break;
			case SPC_203:
				ret.add(glycoSite.getSPCByPTMCode(PTMCode._203));
				break;
			case SPC_NoPTM:
				ret.add(glycoSite.getSPCByPTMCode(PTMCode._0));
				break;
//			case STDEV_2:
//				ret.add(glycoSite.getSTDEVIntensityByPTMCode(PTMCode._2));
//				break;
//			case STDEV_203:
//				ret.add(glycoSite.getSTDEVIntensityByPTMCode(PTMCode._203));
//				break;
//			case STDEV_NoPTM:
//				ret.add(glycoSite.getSTDEVIntensityByPTMCode(PTMCode._0));
//				break;
//			case STDEV_PERCENT_2:
//				ret.add(glycoSite.getSTDEVPercentageByPTMCode(PTMCode._2, sumIntensitiesAcrossReplicates));
//				break;
//			case STDEV_PERCENT_203:
//				ret.add(glycoSite.getSTDEVPercentageByPTMCode(PTMCode._203, sumIntensitiesAcrossReplicates));
//				break;
//			case STDEV_PERCENT_NoPTM:
//				ret.add(glycoSite.getSTDEVPercentageByPTMCode(PTMCode._0, sumIntensitiesAcrossReplicates));
//				break;
			case TOTAL_PEPTIDES:
				ret.add(glycoSite.getTotalNumPeptides());
				break;
			case TOTAL_SPC:
				ret.add(glycoSite.getTotalSPC());
				break;
			case ISSUE:
				if (glycoSite.isAmbiguous()) {
					final TIntList positions = glycoSite.getAmbiguousSites();
					final String plural = positions.size() > 1 ? "s" : "";

					final String positionsString = StringUtils.getSortedSeparatedValueString(positions, ",");
					ret.add("Ambiguous with site" + plural + ": " + positionsString);
				} else {
					ret.add("");
				}
				break;
			case REFERENCE_SITE:
				ret.add(glycoSite.getReferencePosition());
				break;
			default:
				throw new IllegalArgumentException("Column " + column + " is not supported by this exporter");

			}

		}

		return ret;

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
