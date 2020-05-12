package edu.scripps.yates.glycomsquant;

import org.apache.commons.math3.stat.inference.MannWhitneyUTest;
import org.apache.commons.math3.stat.ranking.NaNStrategy;
import org.apache.commons.math3.stat.ranking.TiesStrategy;

import edu.scripps.yates.utilities.maths.PValueCorrection;
import edu.scripps.yates.utilities.maths.PValueCorrectionResult;
import edu.scripps.yates.utilities.maths.PValueCorrectionType;
import edu.scripps.yates.utilities.maths.PValuesCollection;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.TObjectDoubleMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectDoubleHashMap;

public class MyMannWhitneyTestResult {

	private final double u;
	private final double pvalue;
	private Double correctedPValue;
	private final PValueCorrectionType method = PValueCorrectionType.BH;
	private static TObjectDoubleMap<Integer> pvaluesByID = new TObjectDoubleHashMap<Integer>();
	private static int id = 0;
	private static TIntObjectMap<MyMannWhitneyTestResult> instancesByID = new TIntObjectHashMap<MyMannWhitneyTestResult>();

	public static void startComparison() {
		pvaluesByID.clear();
		id = 0;
		instancesByID.clear();
	}

	public MyMannWhitneyTestResult(double[] x, double y[]) {
		final MannWhitneyUTest test = new MannWhitneyUTest(NaNStrategy.REMOVED, TiesStrategy.AVERAGE);
		u = test.mannWhitneyU(x, y);
		pvalue = test.mannWhitneyUTest(x, y);
		pvaluesByID.put(id, pvalue);
		instancesByID.put(id, this);
		id++;
	}

	public double getPValue() {
		return pvalue;
	}

	public double getCorrectedPValue() {
		if (correctedPValue == null) {
			correctPValues();
		}
		return correctedPValue;
	}

	private synchronized void correctPValues() {
		final PValuesCollection<Integer> collection = new PValuesCollection<Integer>(pvaluesByID);
		final PValueCorrectionResult<Integer> pAdjust = PValueCorrection.pAdjust(collection, method);
		final PValuesCollection<Integer> correctedPValuesByID = pAdjust.getCorrectedPValues();
		for (final Integer id : correctedPValuesByID.getSortedKeysByPValue()) {
			final MyMannWhitneyTestResult test = instancesByID.get(id);
			final Double pValue2 = correctedPValuesByID.getPValue(id);
			test.setCorrectedPValue(pValue2);
		}

	}

	private void setCorrectedPValue(Double value) {
		this.correctedPValue = value;
	}

	/**
	 * depending on pvalue, prints '*' if <0.05, '**' if <0.01 and '***' if <0.001
	 * 
	 * @return
	 */
	public static String printSignificanceAsterisks(double pvalue) {
		final StringBuilder sb = new StringBuilder();
		if (pvalue < 0.05) {
			sb.append("*");
		}
		if (pvalue < 0.01) {
			sb.append("*");
		}
		if (pvalue < 0.001) {
			sb.append("*");
		}
		return sb.toString();
	}
}
