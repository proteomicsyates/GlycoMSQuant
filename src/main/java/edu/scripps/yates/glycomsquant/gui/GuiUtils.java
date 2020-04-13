package edu.scripps.yates.glycomsquant.gui;

import java.awt.Dimension;
import java.awt.Toolkit;

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
}
