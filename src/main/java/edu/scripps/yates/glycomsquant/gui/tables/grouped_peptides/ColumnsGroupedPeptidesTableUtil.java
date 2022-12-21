package edu.scripps.yates.glycomsquant.gui.tables.grouped_peptides;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.swing.JScrollBar;
import javax.swing.JScrollPane;

import org.apache.log4j.Logger;

import edu.scripps.yates.census.read.model.interfaces.QuantifiedPSMInterface;
import edu.scripps.yates.glycomsquant.GlycoSite;
import edu.scripps.yates.glycomsquant.GroupedQuantifiedPeptide;
import edu.scripps.yates.glycomsquant.PTMCode;
import edu.scripps.yates.glycomsquant.gui.MainFrame;
import edu.scripps.yates.glycomsquant.util.GlycoPTMAnalyzerUtil;
import edu.scripps.yates.utilities.maths.Maths;
import edu.scripps.yates.utilities.strings.StringUtils;
import gnu.trove.list.TIntList;

public class ColumnsGroupedPeptidesTableUtil {
	private static Logger log = Logger.getLogger("log4j.logger.org.proteored");

	private static ColumnsGroupedPeptidesTableUtil instance;

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

	private ColumnsGroupedPeptidesTableUtil() {
	}

	public static ColumnsGroupedPeptidesTableUtil getInstance() {
		if (instance == null) {
			instance = new ColumnsGroupedPeptidesTableUtil();
		}
		return instance;
	}

	public List<Object> getPeptideInfoList(GroupedQuantifiedPeptide groupedPeptide, List<GlycoSite> glycoSites,
			List<ColumnsGroupedPeptidesTable> columns, String proteinSequence) {

		final List<Object> ret = new ArrayList<Object>();
		if (groupedPeptide == null)
			throw new IllegalArgumentException("peptide is null");
		final String groupedPeptideSequence = groupedPeptide.getKey(true);
		final List<GlycoSite> coveredGlycoSites = getCoveredGlycoSites(glycoSites, groupedPeptide);
		final TIntList starts = StringUtils.allPositionsOf(proteinSequence, groupedPeptide.getSequence());
		final List<String> replicates = GlycoPTMAnalyzerUtil.getReplicateNamesFromPeptide(groupedPeptide);
		final Set<QuantifiedPSMInterface> quantifiedPSMs = groupedPeptide.getQuantifiedPSMs();
		StringBuilder sb = new StringBuilder();
		for (final ColumnsGroupedPeptidesTable column : columns) {

			switch (column) {

			case STARTING_POSITION:
				sb = new StringBuilder();
				for (final int position : starts.toArray()) {
					if (!"".equals(sb.toString())) {
						sb.append(VALUE_SEPARATOR);
					}
					sb.append(position);
				}
				ret.add(sb.toString());
				break;
			case SEQUENCE:
				ret.add(groupedPeptideSequence);
				break;
			case CHARGE:
				ret.add(groupedPeptide.getChargeState());
				break;

			case REPLICATES:
				sb = new StringBuilder();
				for (final String replicate : replicates) {
					if (!"".equals(sb.toString())) {
						sb.append(VALUE_SEPARATOR);
					}
					sb.append(replicate);
				}
				ret.add(sb.toString());
				break;
			case SITES:
				sb = new StringBuilder();
				for (final GlycoSite site : coveredGlycoSites) {
					if (!"".equals(sb.toString())) {
						sb.append(VALUE_SEPARATOR);
					}
					sb.append(site.getPosition());
				}
				ret.add(sb.toString());

				break;
			case REF_SITES:
				sb = new StringBuilder();
				for (final GlycoSite site : coveredGlycoSites) {
					if (!"".equals(sb.toString())) {
						sb.append(VALUE_SEPARATOR);
					}
					sb.append(site.getReferencePosition());
				}
				ret.add(sb.toString());

				break;
			case SPC_PER_REPLICATE:
				sb = new StringBuilder();
				for (final String replicate : replicates) {
					int num = 0;
					for (final QuantifiedPSMInterface psm : quantifiedPSMs) {
						final boolean isInReplicate = psm.getConditions().stream()
								.filter(c -> c.getName().equalsIgnoreCase(replicate)).findAny().isPresent();
						if (isInReplicate) {
							num++;
						}
					}
					if (!"".equals(sb.toString())) {
						sb.append(VALUE_SEPARATOR);
					}
					sb.append(num);
				}
				ret.add(sb.toString());
				break;
			case TOTAL_SPC:
				// the total spc are the number of different conditions

				int num2 = 0;
				for (final QuantifiedPSMInterface psm : quantifiedPSMs) {
					for (final String replicate : replicates) {

						final boolean isInReplicate = psm.getConditions().stream()
								.filter(c -> c.getName().equalsIgnoreCase(replicate)).findAny().isPresent();
						if (isInReplicate) {
							num2++;
						}
					}

				}
				ret.add(num2);
				break;
			case PERCENT_2:
				try {
					final double percent = Maths.mean(groupedPeptide.getProportionsByPTMCode(PTMCode._2,
							MainFrame.getInstance(null).isSumIntensitiesAcrossReplicates())) * 100.0;
					ret.add(percent);
				} catch (final IllegalArgumentException e) {
					ret.add("-");
				}
				break;
			case PERCENT_203:
				try {
					final double percent2 = Maths.mean(groupedPeptide.getProportionsByPTMCode(PTMCode._203,
							MainFrame.getInstance(null).isSumIntensitiesAcrossReplicates())) * 100.0;
					ret.add(percent2);
				} catch (final IllegalArgumentException e) {
					ret.add("-");
				}
				break;
			case PERCENT_NoPTM:
				try {
					final double percent3 = Maths.mean(groupedPeptide.getProportionsByPTMCode(PTMCode._0,
							MainFrame.getInstance(null).isSumIntensitiesAcrossReplicates())) * 100.0;
					ret.add(percent3);
				} catch (final IllegalArgumentException e) {
					ret.add("-");
				}
				break;
			case NUMBER_INDIVIDUAL_PEPTIDES:
				ret.add(groupedPeptide.size());
				break;
			case NUMBER_INDIVIDUAL_MEASUREMENTS:

				ret.add(GlycoPTMAnalyzerUtil.getNumIndividualIntensities(groupedPeptide));
				break;
			default:
				throw new IllegalArgumentException("Column " + column + " is not supported by this exporter");

			}

		}

		return ret;

	}

	private List<GlycoSite> getCoveredGlycoSites(List<GlycoSite> glycoSites, GroupedQuantifiedPeptide peptides) {
		final List<GlycoSite> ret = new ArrayList<GlycoSite>();
		for (final GlycoSite glycoSite : glycoSites) {
			if (peptides.containsAny(glycoSite.getCoveredPeptides())) {
				ret.add(glycoSite);
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
