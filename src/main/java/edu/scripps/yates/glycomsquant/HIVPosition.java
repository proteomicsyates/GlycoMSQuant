package edu.scripps.yates.glycomsquant;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.scripps.yates.census.read.model.interfaces.QuantifiedPSMInterface;
import edu.scripps.yates.utilities.maths.Maths;
import gnu.trove.list.TDoubleList;
import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.map.hash.THashMap;
import gnu.trove.set.hash.THashSet;

public class HIVPosition {
	private final int position;
	private final THashMap<PTMCode, TDoubleList> valuesByPTMCode = new THashMap<PTMCode, TDoubleList>();
	private final THashMap<PTMCode, List<QuantifiedPSMInterface>> psmsByPTMCode = new THashMap<PTMCode, List<QuantifiedPSMInterface>>();
	private static final PTMCode[] ptmCodes = { PTMCode._0, PTMCode._2, PTMCode._203 };

	public HIVPosition(int position) {
		super();
		this.position = position;

	}

	/**
	 * 
	 * @param ptmCode
	 * @param value
	 * @param psm
	 */
	public void addValue(PTMCode ptmCode, double value, QuantifiedPSMInterface psm) {
		if (!valuesByPTMCode.containsKey(ptmCode)) {
			valuesByPTMCode.put(ptmCode, new TDoubleArrayList());
			psmsByPTMCode.put(ptmCode, new ArrayList<QuantifiedPSMInterface>());
		}
		valuesByPTMCode.get(ptmCode).add(value);
		psmsByPTMCode.get(ptmCode).add(psm);
	}

	public int getPosition() {
		return position;
	}

	public TDoubleList getValuesByPTMCode(PTMCode ptmCode) {
		return valuesByPTMCode.get(ptmCode);
	}

	public Map<PTMCode, TDoubleList> getValuesByPTMCode() {
		return this.valuesByPTMCode;
	}

	@Override
	public String toString() {
		return "HIVPosition [position=" + position + ", value=" + getValuesString() + ", error=" + getError() + "]";
	}

	public double getAverageByPTMCode(PTMCode ptmCode) {
		if (valuesByPTMCode.containsKey(ptmCode)) {
			return Maths.mean(valuesByPTMCode.get(ptmCode));
		}
		return 0.0;
	}

	public double getSumByPTMCode(PTMCode ptmCode) {
		if (valuesByPTMCode.containsKey(ptmCode)) {
			return Maths.sum(valuesByPTMCode.get(ptmCode));
		}
		return 0.0;
	}

	public double getSTDEVByPTMCode(PTMCode ptmCode) {
		if (valuesByPTMCode.containsKey(ptmCode)) {
			return Maths.stddev(valuesByPTMCode.get(ptmCode));
		}
		return 0.0;
	}

	/**
	 * Calculates the Standard Error of Mean
	 * 
	 * @param ptmCode
	 * @return
	 */
	public double getSEMByPTMCode(PTMCode ptmCode) {
		if (valuesByPTMCode.containsKey(ptmCode)) {
			return Maths.sem(valuesByPTMCode.get(ptmCode));
		}
		return 0.0;
	}

	public double getPercentageOfAveragesByPTMCode(PTMCode ptmCode) {
		double averagePTMCodeOfInterest = 0.0;
		TDoubleList averages = new TDoubleArrayList();
		for (PTMCode ptmCode2 : ptmCodes) {
			double averageByPTMCode = getAverageByPTMCode(ptmCode2);
			averages.add(averageByPTMCode);
			if (ptmCode2 == ptmCode) {
				averagePTMCodeOfInterest = averageByPTMCode;
			}
		}
		double percentage = averagePTMCodeOfInterest / averages.sum();
		return percentage;
	}

	public double getPercentageOfSumsByPTMCode(PTMCode ptmCode) {
		double sumPTMCodeOfInterest = 0.0;
		TDoubleList sums = new TDoubleArrayList();
		for (PTMCode ptmCode2 : ptmCodes) {
			double sumByPTMCode = getSumByPTMCode(ptmCode2);
			sums.add(sumByPTMCode);
			if (ptmCode2 == ptmCode) {
				sumPTMCodeOfInterest = sumByPTMCode;
			}
		}
		double percentage = sumPTMCodeOfInterest / sums.sum();
		return percentage;
	}

	private String getValuesString() {
		StringBuilder sb = new StringBuilder();
		for (PTMCode ptmCode : ptmCodes) {
			if (valuesByPTMCode.containsKey(ptmCode)) {
				TDoubleList tDoubleList = valuesByPTMCode.get(ptmCode);
				double mean = Maths.mean(tDoubleList);
				if (!"".equals(sb.toString())) {
					sb.append(", ");
				}
				sb.append("(" + ptmCode + ") " + mean);
			}
		}
		return sb.toString();
	}

	private String getError() {
		StringBuilder sb = new StringBuilder();
		for (PTMCode ptmCode : ptmCodes) {
			if (valuesByPTMCode.containsKey(ptmCode)) {
				TDoubleList tDoubleList = valuesByPTMCode.get(ptmCode);
				double stdev = Maths.stddev(tDoubleList);
				if (!"".equals(sb.toString())) {
					sb.append(", ");
				}
				sb.append("(" + ptmCode + ") " + stdev);
			}
		}
		return sb.toString();
	}

	public int getSPCByPTMCode(PTMCode ptmCode) {
		if (!psmsByPTMCode.containsKey(ptmCode)) {
			return 0;
		}
		return psmsByPTMCode.get(ptmCode).size();
	}

	public int getNumPeptidesByPTMCode(PTMCode ptmCode) {
		if (!psmsByPTMCode.containsKey(ptmCode)) {
			return 0;
		}
		List<QuantifiedPSMInterface> psms = psmsByPTMCode.get(ptmCode);
		Set<String> peptideSeqs = new THashSet<String>();
		psms.stream().forEach(psm -> peptideSeqs.add(psm.getSequence()));
		return peptideSeqs.size();

	}
}
