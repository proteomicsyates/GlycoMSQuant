package edu.scripps.yates.glycomsquant;

import java.util.ArrayList;
import java.util.List;

import edu.scripps.yates.glycomsquant.util.ResultsLoadedFromDisk;

public class RunComparisonResult {
	private final List<ResultsLoadedFromDisk> resultsFromDisk;
	private final List<RunComparisonTTest> tests = new ArrayList<RunComparisonTTest>();

	public RunComparisonResult(List<ResultsLoadedFromDisk> resultsFromDisk) {
		this.resultsFromDisk = resultsFromDisk;
	}

	public void addPairComparison(RunComparisonTTest compareResultsTest) {
		tests.add(compareResultsTest);
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append(tests.size() + " comparison(s) between " + resultsFromDisk.size() + " experiments.\n");
		for (final RunComparisonTTest test : tests) {
			sb.append(test.toString() + "\n");
		}
		return sb.toString();
	}
}
