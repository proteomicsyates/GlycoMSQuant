package edu.scripps.yates.glycomsquant;

import java.util.ArrayList;
import java.util.List;

import edu.scripps.yates.glycomsquant.util.ResultsLoadedFromDisk;

public class RunComparisonResult {
	private final List<ResultsLoadedFromDisk> resultsFromDisk;
	private final List<RunComparisonTest> tests = new ArrayList<RunComparisonTest>();

	public RunComparisonResult(List<ResultsLoadedFromDisk> resultsFromDisk) {
		this.resultsFromDisk = resultsFromDisk;
	}

	public void addPairComparison(RunComparisonTest compareResultsTest) {
		tests.add(compareResultsTest);
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append(tests.size() + " comparison(s) between " + resultsFromDisk.size() + " experiments.\n");
		for (final RunComparisonTest test : tests) {
			sb.append(test.toString() + "\n");
		}
		return sb.toString();
	}
}
