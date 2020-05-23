package edu.scripps.yates.glycomsquant.gui.tables.runs;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.UIResource;
import javax.swing.table.TableCellRenderer;

import org.apache.log4j.Logger;

class MyRunsTableBooleanCellRenderer extends JCheckBox implements TableCellRenderer, UIResource {
	/**
	 * 
	 */
	private static final long serialVersionUID = 2779255416435904762L;
	private static Logger log = Logger.getLogger(MyRunsTableBooleanCellRenderer.class);

	private static final Border noFocusBorder = new EmptyBorder(1, 1, 1, 1);

	protected MyRunsTableBooleanCellRenderer() {
		super();
		setHorizontalAlignment(JLabel.CENTER);
		setBorderPainted(true);
	}

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
			int row, int column) {
		if (isSelected) {
			setForeground(table.getSelectionForeground());
			super.setBackground(table.getSelectionBackground());
		} else {
			setForeground(table.getForeground());
			setBackground(table.getBackground());
		}
		setSelected((value != null && ((Boolean) value).booleanValue()));

		if (hasFocus) {
			setBorder(UIManager.getBorder("Table.focusCellHighlightBorder"));
		} else {
			setBorder(noFocusBorder);
		}
		try {
			for (final ColumnsRunTable tableColumn : ColumnsRunTable.values()) {
				final int columnIndex = table.getColumnModel().getColumnIndex(tableColumn.toString());
				if (columnIndex >= 0 && columnIndex == column && value != null) {
					final String defaultToolTip = getToolTip(value.toString(), tableColumn);
					setToolTipText(defaultToolTip);
				}
			}

		} catch (final IllegalArgumentException e) {
			e.printStackTrace();
		}

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
		return this;
	}

	private String getToolTip(String value, ColumnsRunTable column) {
		if (value == null || "".equals(value)) {
			return "";
		}
		value = column.getDescription() + ":<br><b>" + value + "</b>";
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
		return "<html>" + tmp + "</html>";

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

	private Color getColor(int row) {
		if (row % 2 == 1) { // impar
			return new Color(233, 248, 253);
		} else { // par
			return Color.white;
		}
	}
}
