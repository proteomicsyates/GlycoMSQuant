package edu.scripps.yates.glycomsquant;

public enum PTMCode {
	_0("0.0"), _2("2.988"), _203("203.079");

	private final String code;

	private PTMCode(String code) {
		this.code = code;
	}

	public String getCode() {
		return this.code;
	}

	public static PTMCode getByValue(String ptmCode) {
		for (final PTMCode ptmCode2 : values()) {
			if (ptmCode2.getCode().equals(ptmCode)) {
				return ptmCode2;
			}
		}
		return null;
	}
}
