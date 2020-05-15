package edu.scripps.yates.glycomsquant;

public enum PTMCode {
	_0(0.0, true), _2(2.988), _203(203.079);

	private final String code;
	private double deltaMass;
	private final boolean isEmptyPTM;

	private PTMCode(double deltaMass) {
		this(deltaMass, false);
	}

	private PTMCode(double deltaMass, boolean isEmptyPTM) {
		this.code = String.valueOf(deltaMass);
		this.deltaMass = deltaMass;
		this.isEmptyPTM = isEmptyPTM;
	}

	public String getCode() {
		return this.code;
	}

	public static PTMCode getByValue(double massDiff) {
		return getByValue(massDiff, 0.001);
	}

	public static PTMCode getByValue(double massDiff, double tolerance) {
		for (final PTMCode ptmCode2 : values()) {
			if (Math.abs(massDiff - ptmCode2.deltaMass) < tolerance) {
				return ptmCode2;
			}
		}
		return null;
	}

	public boolean isEmptyPTM() {
		return isEmptyPTM;
	}
}
