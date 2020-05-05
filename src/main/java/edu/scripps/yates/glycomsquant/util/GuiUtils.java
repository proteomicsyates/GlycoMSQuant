package edu.scripps.yates.glycomsquant.util;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Toolkit;
import java.text.DecimalFormat;

import javax.swing.BorderFactory;
import javax.swing.JLabel;

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
	private static final int BORDER_THICKNESS = 1;
	public static final Font aminoacidLabelFont = new Font("Consolas", Font.PLAIN, 14);

	public static String formatDouble(double value) {
		return formatDouble(value, false);
	}

	public static String formatDouble(double value, boolean asPercentage) {
		if (Double.isNaN(value)) {
			return "-";
		}
		if (asPercentage) {
			return percentageFormatter.format(value);
		}
		String formattedNum = scientificFormatter.format(Maths.max(1.0, value));
		if (value <= 100) {
			formattedNum = String.valueOf(value);
			if (value - Double.valueOf(value).intValue() == 0.0) {
				formattedNum = String.valueOf(Double.valueOf(value).intValue());
			} else {
				// 3 decimals
				formattedNum = threeDecimalsFormatter.format(value);
			}
		}
		return formattedNum;
	}

	public static String translateCode(PTMCode code) {
		return translateCode(code.getCode());
	}

	public static String translateCode(String code) {
		if (PTMCode._0.getCode().equals(code)) {
			return "No-PTM";
		}
		return code;
	}

	public static void setAminoacidBorder(JLabel label, Color color) {
		label.setBorder(BorderFactory.createLineBorder(color, BORDER_THICKNESS));
	}

	public static void setSelectedAminoacidBorder(JLabel label) {
		setAminoacidBorder(label, Color.red);
	}

	public static void setPlainFont(JLabel label) {
		setFont(label, Font.PLAIN);
	}

	public static void setBoldFont(JLabel label) {
		setFont(label, Font.BOLD);
	}

	private static void setFont(JLabel label, int style) {
		final Font oldFont = label.getFont();
		final Font newFont = new Font(oldFont.getName(), style, oldFont.getSize());
		label.setFont(newFont);
	}

	public static Font getFontForSmallChartTitle() {
		return new Font("SansSerif", Font.BOLD, 10);
	}

	public static Font headerFont() {
		final Font font = new JLabel().getFont();
		return new Font(font.getName(), font.getStyle(), font.getSize() + 5);
	}
}