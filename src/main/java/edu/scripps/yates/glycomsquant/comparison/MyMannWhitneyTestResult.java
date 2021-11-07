package edu.scripps.yates.glycomsquant.comparison;

import org.apache.commons.math3.stat.inference.MannWhitneyUTest;
import org.apache.commons.math3.stat.ranking.NaNStrategy;
import org.apache.commons.math3.stat.ranking.TiesStrategy;

import edu.scripps.yates.glycomsquant.PTMCode;
import edu.scripps.yates.utilities.maths.Maths;
import edu.scripps.yates.utilities.maths.PValueCorrection;
import edu.scripps.yates.utilities.maths.PValueCorrectionResult;
import edu.scripps.yates.utilities.maths.PValueCorrectionType;
import edu.scripps.yates.utilities.maths.PValuesCollection;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.TObjectDoubleMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectDoubleHashMap;

/**
 * A test results for comparison data between two groups. In GlycoMSQuant, that
 * is a comparison between the individual proportions of a PTM in a site between
 * 2 experiments.
 * 
 * @author salvador
 *
 */
public class MyMannWhitneyTestResult {

	private final double[] x;
	private final double[] y;
	private final double pvalue;
	private Double correctedPValue;
	private final PValueCorrectionType method = PValueCorrectionType.BH;
	private final PTMCode ptm;
	private final String referencePosition;
	private final Integer[] positions = new Integer[2];
	private static TObjectDoubleMap<Integer> pvaluesByID = new TObjectDoubleHashMap<Integer>();
	private static int id = 0;
	private static TIntObjectMap<MyMannWhitneyTestResult> instancesByID = new TIntObjectHashMap<MyMannWhitneyTestResult>();

	/**
	 * Needed to call before starting a new comparison to clear the pvalues
	 */
	public static void startComparison() {
		pvaluesByID.clear();
		id = 0;
		instancesByID.clear();
	}

	public MyMannWhitneyTestResult(PTMCode ptm, String referencePosition, Integer position1, Integer position2,
			double[] x, double y[]) {
		this.ptm = ptm;
		this.referencePosition = referencePosition;
		this.positions[0] = position1;
		this.positions[1] = position2;
		final MannWhitneyUTest test = new MannWhitneyUTest(NaNStrategy.REMOVED, TiesStrategy.AVERAGE);
		this.x = x;
		this.y = y;
		if ((x != null && y == null) || (x == null && x != null)) {
			pvalue = 0.0;
		} else {
			if (x == null && y == null) {
				pvalue = 1.0;
			} else {
				pvalue = test.mannWhitneyUTest(x, y);
			}
		}
		pvaluesByID.put(id, pvalue);
		instancesByID.put(id, this);
		id++;
	}

	public double getXMean() {
		if (x != null) {
			return Maths.mean(x);
		}
		return 0.0;
	}

	public double getXSem() {
		if (x != null) {
			return Maths.sem(x);
		}
		return 0.0;
	}

	public double getYMean() {
		if (y != null) {
			return Maths.mean(y);
		}
		return 0.0;
	}

	public double getYSem() {
		if (y != null) {
			return Maths.sem(y);
		}
		return 0.0;
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

	public String getIndividualPositionsString() {
		StringBuilder sb = new StringBuilder();
		if (positions[0] != null) {
			sb.append(positions[0]);
		} else {
			sb.append("-");
		}
		sb.append("-");
		if (positions[1] != null) {
			sb.append(positions[1]);
		} else {
			sb.append("-");
		}
		return sb.toString();
	}

	public String getReferencePosition() {
		return this.referencePosition;
	}

	public PTMCode getPtm() {
		return ptm;
	}
}
