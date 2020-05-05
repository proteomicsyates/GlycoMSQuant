package edu.scripps.yates.glycomsquant.util;

import java.awt.Color;
import java.awt.Paint;

import edu.scripps.yates.glycomsquant.PTMCode;

public class ColorsUtil {
//	public static final Paint[] DEFAULT_COLORS = new Paint[] { getDefaultColorByPTMCode(PTMCode._0),
//			getDefaultColorByPTMCode(PTMCode._2), getDefaultColorByPTMCode(PTMCode._203) };

	public static Paint getBlack() {
		return Color.black;
	}

	public static Color getColorForTableByPTMCode(PTMCode ptmCode) {
		if (ptmCode == null) {
			return null;
		}
		switch (ptmCode) {
		case _0:
			return new Color(242, 242, 242);
		case _2:
			return new Color(250, 222, 250);
		case _203:
			return new Color(199, 255, 224);
		default:
			break;
		}
		throw new IllegalArgumentException("PTMCode not supported!");
	}

	public static Color getDefaultColorByPTMCode(PTMCode ptmCode) {
		if (ptmCode == null) {
			return null;
		}
		switch (ptmCode) {
		case _0:
			return new Color(166, 166, 166);
		case _2:
			return new Color(255, 0, 255);
		case _203:
			return new Color(0, 176, 80);
		default:
			break;
		}
		throw new IllegalArgumentException("PTMCode not supported!");
	}

	public static Paint[] getDefaultColorsSortedByPTMCode() {
		final Color[] ret = new Color[PTMCode.values().length];
		int i = 0;
		for (final PTMCode ptmCode : PTMCode.values()) {
			ret[i] = getDefaultColorByPTMCode(ptmCode);
			i++;
		}
		return ret;
	}
}
