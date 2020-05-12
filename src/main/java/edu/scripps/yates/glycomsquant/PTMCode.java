package edu.scripps.yates.glycomsquant;

public enum PTMCode {
	_0(0.0), _2(2.988), _203(203.079);

	private final String code;
	private double deltaMass;

	private PTMCode(double deltaMass) {
		this.code = String.valueOf(deltaMass);
		this.deltaMass = deltaMass;
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
}
