package edu.scripps.yates.glycomsquant.gui;

import java.awt.Color;
import java.awt.Paint;

import edu.scripps.yates.glycomsquant.PTMCode;

public class ColorsUtil {
	public static final Paint[] DEFAULT_COLORS = new Paint[] { new Color(166, 166, 166), new Color(255, 0, 255),
			new Color(0, 176, 80) };

	public static Paint getColorByPTMCode(PTMCode ptmCode) {
		return DEFAULT_COLORS[ptmCode.ordinal()];
	}
}
