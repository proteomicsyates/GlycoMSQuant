package edu.scripps.yates.glycomsquant.comparison;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import javax.swing.SwingWorker;

import edu.scripps.yates.glycomsquant.GlycoSite;
import edu.scripps.yates.glycomsquant.PTMCode;
import edu.scripps.yates.glycomsquant.gui.tasks.ResultLoaderFromDisk;
import edu.scripps.yates.glycomsquant.util.GlycoPTMAnalyzerUtil;
import edu.scripps.yates.glycomsquant.util.ResultsLoadedFromDisk;
import edu.scripps.yates.utilities.maths.Maths;
import gnu.trove.list.TDoubleList;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.THashMap;

public class GlycoPTMRunComparator extends SwingWorker<Void, Void> implements PropertyChangeListener {

	public static final String COMPARATOR_ERROR = "Comparator_ERROR";
	public static final String COMPARATOR_FINISHED = "Comparator_FINISHED";

	private final List<String> selectedRuns;
	private final List<ResultsLoadedFromDisk> resultsFromDisk = new ArrayList<ResultsLoadedFromDisk>();
	private final ReentrantLock lock = new ReentrantLock(true);
	private final String referenceProteinSequence;

	public GlycoPTMRunComparator(List<String> selectedRuns, String referenceProteinSequence) {
		this.selectedRuns = selectedRuns;
		this.referenceProteinSequence = referenceProteinSequence;
	}

	@Override
	protected Void doInBackground() throws Exception {
		try {
			// first load results from disk. once it is done, do the comparison
			loadRunsFromDisk();
		} catch (final Exception e) {
			firePropertyChange(COMPARATOR_ERROR, null, e.getMessage());
		}
		return null;
	}

	private void loadRunsFromDisk() {

		for (final String selectedRun : selectedRuns) {
			final File resultsFolder = new File(selectedRun);
			final ResultLoaderFromDisk loader = new ResultLoaderFromDisk(resultsFolder, referenceProteinSequence);
			loader.addPropertyChangeListener(this);
			loader.run();
		}

		return;
	}

	private void addResults(ResultsLoadedFromDisk result) {
		lock.lock();
		this.resultsFromDisk.add(result);
		lock.unlock();
	}

	private boolean hasEnoughResults() {
		lock.lock();
		try {
			return resultsFromDisk.size() == selectedRuns.size();
		} finally {
			lock.unlock();
		}
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		if (evt.getPropertyName().equals(ResultLoaderFromDisk.RESULT_LOADER_FROM_DISK_FINISHED)) {
			final ResultsLoadedFromDisk result = (ResultsLoadedFromDisk) evt.getNewValue();
			addResults(result);
			if (hasEnoughResults()) {
				compareResults();
			}
		} else if (evt.getPropertyName().equals(ResultLoaderFromDisk.RESULT_LOADER_FROM_DISK_STARTED)) {
			firePropertyChange(ResultLoaderFromDisk.RESULT_LOADER_FROM_DISK_STARTED, null, evt.getNewValue());
		} else if (evt.getPropertyName().equals(ResultLoaderFromDisk.RESULT_LOADER_FROM_DISK_ERROR)) {
			firePropertyChange(ResultLoaderFromDisk.RESULT_LOADER_FROM_DISK_ERROR, null, evt.getNewValue());
		}
	}

	public void compareResults() {
		try {
			MyMannWhitneyTestResult.startComparison();
			final RunComparisonResult comparison = new RunComparisonResult(this.resultsFromDisk);
			for (int i = 0; i < this.resultsFromDisk.size(); i++) {
				for (int j = i + 1; j < this.resultsFromDisk.size(); j++) {
					final ResultsLoadedFromDisk results1 = resultsFromDisk.get(i);
					final ResultsLoadedFromDisk results2 = resultsFromDisk.get(j);
					try {
						final RunComparisonTest compareResults = compareResults(results1, results2);
						comparison.addPairComparison(compareResults);
					} catch (final Exception e) {
						e.printStackTrace();
						firePropertyChange(COMPARATOR_ERROR, null,
								"Error comparing " + results1.getResultProperties().getName() + " vs "
										+ results2.getResultProperties().getName());
					}

				}
			}
			firePropertyChange(COMPARATOR_FINISHED, null, comparison);
		} catch (final Exception e) {
			e.printStackTrace();
			firePropertyChange(COMPARATOR_ERROR, null, e.getMessage());
		}
	}

	private RunComparisonTest compareResults(ResultsLoadedFromDisk results1, ResultsLoadedFromDisk results2) {
		final RunComparisonTest ret = new RunComparisonTest(results1, results2);
		final TIntObjectMap<GlycoSite> sites1 = GlycoPTMAnalyzerUtil.getSitesByPosition(results1.getSites());
		final TIntObjectMap<GlycoSite> sites2 = GlycoPTMAnalyzerUtil.getSitesByPosition(results2.getSites());
		for (final int position : sites1.keys()) {
			if (sites2.containsKey(position)) {
				final Map<PTMCode, MyMannWhitneyTestResult> compareSites = compareSites(sites1.get(position),
						results1.getResultProperties().isSumIntensitiesAcrossReplicates(), sites2.get(position),
						results2.getResultProperties().isSumIntensitiesAcrossReplicates());
				ret.addTTestsForPosition(position, compareSites);
			}
		}
		return ret;
	}

	private Map<PTMCode, MyMannWhitneyTestResult> compareSites(GlycoSite glycoSite1,
			boolean sumIntensitiesAcrossReplicates1, GlycoSite glycoSite2, boolean sumIntensitiesAcrossReplicates2) {
		final Map<PTMCode, MyMannWhitneyTestResult> ret = new THashMap<PTMCode, MyMannWhitneyTestResult>();
		for (final PTMCode ptmCode : PTMCode.values()) {
			// the value of sumIntensitiesAcrossReplicates here doesn't matter because each
			// glycoSite will have already the values and the boolean will not be used
			final TDoubleList percentages1 = glycoSite1.getIndividualPeptideProportionsByPTMCode(ptmCode,
					sumIntensitiesAcrossReplicates1);
			final TDoubleList percentages2 = glycoSite2.getIndividualPeptideProportionsByPTMCode(ptmCode,
					sumIntensitiesAcrossReplicates2);
// 			System.out.println("Test: " + ptmCode.getCode() + " in " + glycoSite1.getPosition());
//			if (ptmCode == PTMCode._2 && glycoSite1.getPosition() == 363) {
//				System.out.println("asfd");
//			}
			double[] array1 = null;
			if (percentages1 != null) {
				array1 = percentages1.toArray();
			}
			double[] array2 = null;
			if (percentages2 != null) {
				array2 = percentages2.toArray();
			}
			final MyMannWhitneyTestResult result = new MyMannWhitneyTestResult(ptmCode, glycoSite1.getPosition(),
					array1, array2);
			ret.put(ptmCode, result);

//			if (isValidForTTest(percentages1) && isValidForTTest(percentages2)) {
//				
//				test = TTest.test(percentages1.toArray(), percentages2.toArray(), true);
//			} else if (isValidForTTest(percentages1)) {
//				test = TTest.test(percentages1.toArray(), 0.0);
//			} else if (isValidForTTest(percentages2)) {
//				test = TTest.test(percentages2.toArray(), 0.0);
//			} else {
//				// both 0, so is null
//			}
//			ret.put(ptmCode, test);

		}
		return ret;
	}

	public static boolean isValidForTTest(TDoubleList values) {
		if (true)
			return true;
		if (values == null) {
			return false;
		}
		if (values.size() < 2) {
			return false;
		}

		final double stddev = Maths.stddev(values);
		if (Double.isNaN(stddev)) {
			return false;
		}
//		for (final double num : values.toArray()) {
//			if (Double.compare(num, 0.0) != 0) {
//				return true;
//			}
//		}
		return true;
	}
}
