package edu.scripps.yates.glycomsquant.gui.tables.comparison;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import org.apache.log4j.Logger;

import edu.scripps.yates.glycomsquant.gui.tables.runs.ColumnsRunTableUtil;
import edu.scripps.yates.glycomsquant.util.GuiUtils;

public class MyComparisonTableCellRenderer extends DefaultTableCellRenderer {
	/**
	 * 
	 */
	private static final long serialVersionUID = 2779255416435904762L;
	private static Logger log = Logger.getLogger(MyComparisonTableCellRenderer.class);
	private static Color[] significancyColors = { new Color(250, 150, 150), new Color(250, 95, 95),
			new Color(250, 0, 0) };

	public MyComparisonTableCellRenderer() {
	}

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
			int row, int column) {
		int columnIndex = -1;
		ColumnsComparisonTable selectedColumn = null;
		try {
			for (final ColumnsComparisonTable tableColumn : ColumnsComparisonTable.getColumns()) {
				final int columnIndexTMP = table.getColumnModel().getColumnIndex(tableColumn.toString());
				if (columnIndexTMP >= 0 && columnIndexTMP == column) {
					columnIndex = columnIndexTMP;
					selectedColumn = tableColumn;
					break;
				}
			}

		} catch (final IllegalArgumentException e) {
			e.printStackTrace();
		}

		String defaultToolTip = null;
		if (value == null) {
			value = "";
		}
		if (value.getClass() == Double.class && selectedColumn != null
				&& selectedColumn.getColumnClass().equals(Double.class)) {

			value = GuiUtils.formatDouble((double) value, false);
		}

		if (columnIndex > 0 && columnIndex == column) {
			defaultToolTip = getToolTip(value.toString(), selectedColumn);
		}

		try {
			if (defaultToolTip == null) {
				defaultToolTip = getToolTip(value.toString(), null);
			}
		} catch (final IllegalArgumentException e) {

		}

		if (defaultToolTip == null && value != null) {
			defaultToolTip = value.toString();
		}
		setToolTipText(defaultToolTip);
		final Color defaultColor = getColorBySignificancy(row, selectedColumn, value);
		this.setBackground(defaultColor);
		if (isSelected) {
			this.setForeground(Color.RED);
			this.setBackground(Color.YELLOW);
			if (hasFocus) {
				final java.awt.Font bold = new java.awt.Font(table.getFont().getName(), Font.BOLD,
						table.getFont().getSize());

				this.setFont(bold);
			}

		} else {
			this.setForeground(Color.BLACK);

		}
		final Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

		return c;
	}

	private String getToolTip(String value, ColumnsComparisonTable column) {
		if (value == null || "".equals(value)) {
			if (column != null) {
				return column.getDescription();
			}
			return "";
		}
		if (column != null) {
			value = column.getDescription() + ":" + ColumnsRunTableUtil.VALUE_SEPARATOR + "<b>" + value + "</b>";
		}
		String[] splited = null;
		if (value.contains(ColumnsRunTableUtil.VALUE_SEPARATOR)) {
			splited = value.split(ColumnsRunTableUtil.VALUE_SEPARATOR);
		}
		if (value.contains("\n")) {
			splited = value.split("\n");
		}

		String tmp = value;
		if (splited != null && splited.length > 0) {
			tmp = getSplitedInNewLines(splited);
		}
		final String explanation = "";
		return "<html>" + tmp + "<br>" + explanation + "</html>";

	}

	private String getSplitedInNewLines(String[] splited) {
		String ret = "";
		if (splited.length > 0) {
			for (final String string : splited) {
				if (!"".equals(ret))
					ret = ret + "<br>";
				ret = ret + string;
			}
		}
		return ret;
	}

	private Color getColorBySignificancy(int row, ColumnsComparisonTable column, Object value) {
		Color color = null;
		if (column != null && column == ColumnsComparisonTable.ADJUSTED_P_VALUE) {
			try {
				final double p = Double.valueOf(value.toString());
				if (p < 0.001) {
					color = significancyColors[2];
				} else if (p < 0.01) {
					color = significancyColors[1];
				} else if (p < 0.05) {
					color = significancyColors[0];
				}
			} catch (final NumberFormatException e) {

			}
		}

		if (row % 2 == 1) { // impar
			if (color != null) {
				return new Color(Math.max(0, color.getRed()) - 23, Math.max(0, color.getGreen() - 8),
						Math.max(0, color.getBlue() - 3));
			}
			return new Color(233, 248, 253);
		} else { // par
			if (color != null) {
				return color;
			}
			return Color.white;
		}
	}
}
