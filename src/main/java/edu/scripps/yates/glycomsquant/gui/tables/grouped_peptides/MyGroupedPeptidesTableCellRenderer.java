package edu.scripps.yates.glycomsquant.gui.tables.grouped_peptides;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import org.apache.log4j.Logger;

import edu.scripps.yates.glycomsquant.gui.tables.runs.ColumnsRunTableUtil;
import edu.scripps.yates.glycomsquant.util.GuiUtils;

public class MyGroupedPeptidesTableCellRenderer extends DefaultTableCellRenderer {
	/**
	 * 
	 */
	private static final long serialVersionUID = 2779255416435904762L;
	private static Logger log = Logger.getLogger(MyGroupedPeptidesTableCellRenderer.class);

	public MyGroupedPeptidesTableCellRenderer() {
	}

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
			int row, int column) {
		int columnIndex = -1;
		ColumnsGroupedPeptidesTable selectedColumn = null;
		try {
			for (final ColumnsGroupedPeptidesTable tableColumn : ColumnsGroupedPeptidesTable.getColumns()) {
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
		final Color defaultColor = getColor(row, selectedColumn);
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

	private String getToolTip(String value, ColumnsGroupedPeptidesTable column) {
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
		final String explanation = "<br>Left click a row to select the peptide and show the associated information.<br>Right click to invert selection.<br>Multiple rows can be selected (use SHIFT or CONTROL).";
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

	private Color getColor(int row, ColumnsGroupedPeptidesTable selectedColumn) {
		Color color = null;
		if (selectedColumn != null) {
			color = selectedColumn.getColor();
		}
		if (row % 2 == 1) { // impar
			if (color != null) {
				return new Color(color.getRed() - 23, color.getGreen() - 8, color.getBlue() - 3);
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
