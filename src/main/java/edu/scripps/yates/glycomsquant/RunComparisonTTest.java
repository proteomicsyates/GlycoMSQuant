package edu.scripps.yates.glycomsquant;

import java.util.Map;

import edu.scripps.yates.glycomsquant.util.GuiUtils;
import edu.scripps.yates.glycomsquant.util.ResultsLoadedFromDisk;
import edu.scripps.yates.utilities.maths.TTest;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TIntObjectHashMap;

public class RunComparisonTTest extends TIntObjectHashMap<Map<PTMCode, TTest>> {
	private final ResultsLoadedFromDisk results1;
	private final ResultsLoadedFromDisk results2;

	public RunComparisonTTest(ResultsLoadedFromDisk results1, ResultsLoadedFromDisk results2) {
		this.results1 = results1;
		this.results2 = results2;
	}

	public Map<PTMCode, TTest> getResultsForPosition(int position) {
		return this.get(position);
	}

	public Map<PTMCode, TTest> getResultsForPosition(GlycoSite site) {
		return getResultsForPosition(site.getPosition());
	}

	public ResultsLoadedFromDisk getResults1() {
		return results1;
	}

	public ResultsLoadedFromDisk getResults2() {
		return results2;
	}

	public void addTTestsForPosition(int position, Map<PTMCode, TTest> ttests) {
		this.put(position, ttests);

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
			final Map<PTMCode, TTest> resultsForPosition = getResultsForPosition(position);
			for (final PTMCode ptmCode : PTMCode.values()) {
				sb.append("Position " + position + "\t" + GuiUtils.translateCode(ptmCode.getCode()) + "\t");
				final TTest tTest = resultsForPosition.get(ptmCode);
				if (tTest != null) {
					sb.append("p-value:" + tTest.pvalue + tTest.printSignificanceAsterisks());
				} else {
					sb.append("NaN");
				}
				sb.append("\n");
			}
		}

		return sb.toString();
	}
}
