package edu.scripps.yates.glycomsquant.comparison;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

import javax.swing.SwingWorker;

import edu.scripps.yates.glycomsquant.GlycoSite;
import edu.scripps.yates.glycomsquant.PTMCode;
import edu.scripps.yates.glycomsquant.gui.tasks.ResultLoaderFromDisk;
import edu.scripps.yates.glycomsquant.util.GlycoPTMAnalyzerUtil;
import edu.scripps.yates.glycomsquant.util.ResultsLoadedFromDisk;
import edu.scripps.yates.utilities.maths.Maths;
import gnu.trove.list.TDoubleList;
import gnu.trove.map.hash.THashMap;
import gnu.trove.set.hash.THashSet;

public class GlycoPTMRunComparator extends SwingWorker<Void, Void> implements PropertyChangeListener {

	public static final String COMPARATOR_ERROR = "Comparator_ERROR";
	public static final String COMPARATOR_FINISHED = "Comparator_FINISHED";

	private final List<String> selectedRuns;
	private final List<ResultsLoadedFromDisk> resultsFromDisk = new ArrayList<ResultsLoadedFromDisk>();
	private final ReentrantLock lock = new ReentrantLock(true);

	public GlycoPTMRunComparator(List<String> selectedRuns) {
		this.selectedRuns = selectedRuns;
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
			final ResultLoaderFromDisk loader = new ResultLoaderFromDisk(resultsFolder);
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
			firePropertyChange(COMPARATOR_ERROR, null, evt.getNewValue());

			this.cancel(true);
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
		final Map<String, GlycoSite> sites1 = GlycoPTMAnalyzerUtil.getSitesByReferencePosition(results1.getSites());
		final Map<String, GlycoSite> sites2 = GlycoPTMAnalyzerUtil.getSitesByReferencePosition(results2.getSites());
		Set<String> positions = new THashSet<String>();
		positions.addAll(sites1.keySet());
		positions.addAll(sites2.keySet());
		List<String> sortedPositions = new ArrayList<String>();
		sortedPositions.addAll(positions);
		Collections.sort(sortedPositions);
		for (final String position : sortedPositions) {

			final Map<PTMCode, MyMannWhitneyTestResult> compareSites = compareSites(sites1.get(position),
					results1.getResultProperties().isSumIntensitiesAcrossReplicates(), sites2.get(position),
					results2.getResultProperties().isSumIntensitiesAcrossReplicates());
			ret.addTTestsForPosition(position, compareSites);

		}
		return ret;
	}

	private Map<PTMCode, MyMannWhitneyTestResult> compareSites(GlycoSite glycoSite1,
			boolean sumIntensitiesAcrossReplicates1, GlycoSite glycoSite2, boolean sumIntensitiesAcrossReplicates2) {

		String referencePosition = glycoSite1 != null ? glycoSite1.getReferencePosition()
				: glycoSite2.getReferencePosition();
		Integer position1 = glycoSite1 != null ? glycoSite1.getPosition() : null;
		Integer position2 = glycoSite2 != null ? glycoSite2.getPosition() : null;

		final Map<PTMCode, MyMannWhitneyTestResult> ret = new THashMap<PTMCode, MyMannWhitneyTestResult>();
		for (final PTMCode ptmCode : PTMCode.values()) {
			// the value of sumIntensitiesAcrossReplicates here doesn't matter because each
			// glycoSite will have already the values and the boolean will not be used
			TDoubleList percentages1 = null;
			if (glycoSite1 != null) {
				percentages1 = glycoSite1.getIndividualPeptideProportionsByPTMCode(ptmCode,
						sumIntensitiesAcrossReplicates1);
			}
			TDoubleList percentages2 = null;
			if (glycoSite2 != null) {
				percentages2 = glycoSite2.getIndividualPeptideProportionsByPTMCode(ptmCode,
						sumIntensitiesAcrossReplicates2);
			}
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
			if (array1 != null && array2 != null) {
				final MyMannWhitneyTestResult result = new MyMannWhitneyTestResult(ptmCode, referencePosition,
						position1, position2, array1, array2);
				ret.put(ptmCode, result);
			}
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
