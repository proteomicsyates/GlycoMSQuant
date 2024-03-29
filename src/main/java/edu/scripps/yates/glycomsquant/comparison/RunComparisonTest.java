package edu.scripps.yates.glycomsquant.comparison;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.scripps.yates.glycomsquant.GlycoSite;
import edu.scripps.yates.glycomsquant.PTMCode;
import edu.scripps.yates.glycomsquant.util.GuiUtils;
import edu.scripps.yates.glycomsquant.util.ResultsLoadedFromDisk;

/**
 * A single comparison between two experiments
 * 
 * @author salvador
 *
 */
public class RunComparisonTest extends HashMap<String, Map<PTMCode, MyMannWhitneyTestResult>> {
	private final ResultsLoadedFromDisk results1;
	private final ResultsLoadedFromDisk results2;
	private final List<MyMannWhitneyTestResult> comparisons = new ArrayList<MyMannWhitneyTestResult>();
	private final static DecimalFormat format = new DecimalFormat("#.##");
	private final static DecimalFormat formatPercentage = new DecimalFormat("#.#%");

	public RunComparisonTest(ResultsLoadedFromDisk results1, ResultsLoadedFromDisk results2) {
		this.results1 = results1;
		this.results2 = results2;
	}

	public Map<PTMCode, MyMannWhitneyTestResult> getResultsForPosition(String position) {
		return this.get(position);
	}

	public Map<PTMCode, MyMannWhitneyTestResult> getResultsForPosition(GlycoSite site) {
		return getResultsForPosition(site.getReferencePosition());
	}

	public ResultsLoadedFromDisk getResults1() {
		return results1;
	}

	public ResultsLoadedFromDisk getResults2() {
		return results2;
	}

	public void addTTestsForPosition(String position, Map<PTMCode, MyMannWhitneyTestResult> tests) {
		this.put(position, tests);
		this.comparisons.addAll(tests.values());
	}

	private String formatPValue(double pvalue) {
		if (pvalue >= 0.001) {
			return new DecimalFormat("#.###").format(pvalue);
		} else {
			return new DecimalFormat("0.0E0").format(pvalue);
		}
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();

		final String name1 = results1.getResultProperties().getName();
		final String name2 = results2.getResultProperties().getName();
		sb.append("Comparison between\n1:\t" + name1 + "\n2:\t" + name2 + "\n");
		sb.append("Position\tPTM\t%1\t%2\tsem(%1)\tsem(%2)\tp-value\tBH-corrected p-value\tSignificancy\n");
		final List<String> positions = new ArrayList<String>();
		positions.addAll(this.keySet());
		Collections.sort(positions);
		for (final String position : positions) {
			final Map<PTMCode, MyMannWhitneyTestResult> resultsForPosition = getResultsForPosition(position);
			for (final PTMCode ptmCode : PTMCode.values()) {
				sb.append(position + "\t" + GuiUtils.translateCode(ptmCode.getCode()) + "\t");
				final MyMannWhitneyTestResult test = resultsForPosition.get(ptmCode);
				if (test != null) {
					sb.append(formatPercentage.format(test.getXMean()) + "\t" + formatPercentage.format(test.getYMean())
							+ "\t");
					sb.append(formatPercentage.format(test.getXSem()) + "\t" + formatPercentage.format(test.getYSem())
							+ "\t");
					sb.append(formatPValue(test.getPValue()) + "\t");
					sb.append(formatPValue(test.getCorrectedPValue()) + "\t"
							+ MyMannWhitneyTestResult.printSignificanceAsterisks(test.getCorrectedPValue()));
				} else {
					sb.append("NaN");
				}
				sb.append("\n");
			}
		}

		return sb.toString();
	}

	public List<MyMannWhitneyTestResult> getComparisons() {
		return comparisons;

	}
}
