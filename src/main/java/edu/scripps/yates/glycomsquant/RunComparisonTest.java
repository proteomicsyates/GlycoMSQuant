package edu.scripps.yates.glycomsquant;

import java.util.Map;

import edu.scripps.yates.glycomsquant.util.GuiUtils;
import edu.scripps.yates.glycomsquant.util.ResultsLoadedFromDisk;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TIntObjectHashMap;

public class RunComparisonTest extends TIntObjectHashMap<Map<PTMCode, MyMannWhitneyTestResult>> {
	private final ResultsLoadedFromDisk results1;
	private final ResultsLoadedFromDisk results2;

	public RunComparisonTest(ResultsLoadedFromDisk results1, ResultsLoadedFromDisk results2) {
		this.results1 = results1;
		this.results2 = results2;
	}

	public Map<PTMCode, MyMannWhitneyTestResult> getResultsForPosition(int position) {
		return this.get(position);
	}

	public Map<PTMCode, MyMannWhitneyTestResult> getResultsForPosition(GlycoSite site) {
		return getResultsForPosition(site.getPosition());
	}

	public ResultsLoadedFromDisk getResults1() {
		return results1;
	}

	public ResultsLoadedFromDisk getResults2() {
		return results2;
	}

	public void addTTestsForPosition(int position, Map<PTMCode, MyMannWhitneyTestResult> tests) {
		this.put(position, tests);

	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();

		sb.append("Comparison between " + results1.getResultProperties().getName() + " and "
				+ results2.getResultProperties().getName() + "\n");
		final TIntList positions = new TIntArrayList();
		positions.addAll(this.keySet());
		positions.sort();
		for (final int position : positions.toArray()) {
			final Map<PTMCode, MyMannWhitneyTestResult> resultsForPosition = getResultsForPosition(position);
			for (final PTMCode ptmCode : PTMCode.values()) {
				sb.append("Position " + position + "\t" + GuiUtils.translateCode(ptmCode.getCode()) + "\t");
				final MyMannWhitneyTestResult test = resultsForPosition.get(ptmCode);
				if (test != null) {
					sb.append("p-value:" + test.getPValue() + "\t");
					sb.append("BH-corrected p-value:" + test.getCorrectedPValue()
							+ MyMannWhitneyTestResult.printSignificanceAsterisks(test.getCorrectedPValue()));
				} else {
					sb.append("NaN");
				}
				sb.append("\n");
			}
		}

		return sb.toString();
	}
}
