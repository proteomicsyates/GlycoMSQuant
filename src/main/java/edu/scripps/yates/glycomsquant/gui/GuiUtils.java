package edu.scripps.yates.glycomsquant.gui;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.text.DecimalFormat;

import edu.scripps.yates.glycomsquant.PTMCode;
import edu.scripps.yates.utilities.maths.Maths;

public class GuiUtils {

	public static int getFractionOfScreenHeightSize(double fraction) {
		final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		final Double size = screenSize.getHeight() * fraction;
		return size.intValue();
	}

	public static int getFractionOfScreenWidthSize(double fraction) {
		final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		final Double size = screenSize.getWidth() * fraction;
		return size.intValue();
	}

	public static Dimension getScreenDimension() {
		return Toolkit.getDefaultToolkit().getScreenSize();
	}

	private final static DecimalFormat scientificFormatter = new DecimalFormat("#.##E0");
	private final static DecimalFormat threeDecimalsFormatter = new DecimalFormat("#.###");
	private final static DecimalFormat percentageFormatter = new DecimalFormat("0.0%");

	public static String formatDouble(double threshold) {
		return formatDouble(threshold, false);
	}

	public static String formatDouble(double threshold, boolean asPercentage) {
		if (asPercentage) {
			return percentageFormatter.format(threshold);
		}
		String formattedNum = scientificFormatter.format(Maths.max(1.0, threshold));
		if (threshold <= 100) {
			formattedNum = String.valueOf(threshold);
			if (threshold - Double.valueOf(threshold).intValue() == 0.0) {
				formattedNum = String.valueOf(Double.valueOf(threshold).intValue());
			} else {
				// 3 decimals
				formattedNum = threeDecimalsFormatter.format(threshold);
			}
		}
		return formattedNum;
	}

	public static String translateCode(String code) {
		if (PTMCode._0.getCode().equals(code)) {
			return "NonPTM";
		}
		return code;
	}
}
