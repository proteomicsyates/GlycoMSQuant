package edu.scripps.yates.glycomsquant.comparison;

import java.util.ArrayList;
import java.util.List;

import edu.scripps.yates.glycomsquant.util.ResultsLoadedFromDisk;

/**
 * A set of comparisons between experiments
 * 
 * @author salvador
 *
 */
public class RunComparisonResult {
	private final List<ResultsLoadedFromDisk> resultsFromDisk;
	private final List<RunComparisonTest> tests = new ArrayList<RunComparisonTest>();

	public RunComparisonResult(List<ResultsLoadedFromDisk> resultsFromDisk) {
		this.resultsFromDisk = resultsFromDisk;
	}

	protected void addPairComparison(RunComparisonTest compareResultsTest) {
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

	public List<RunComparisonTest> getTests() {
		return tests;
	}
}
