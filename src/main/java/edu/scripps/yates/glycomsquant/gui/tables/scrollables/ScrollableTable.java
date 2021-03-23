package edu.scripps.yates.glycomsquant.gui.tables.scrollables;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;

import edu.scripps.yates.glycomsquant.gui.tables.MyAbstractTable;
import edu.scripps.yates.glycomsquant.util.GuiUtils;

public class ScrollableTable<T extends MyAbstractTable> extends JPanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7752606871766765157L;
	private final T table;

	private final JScrollPane scroll = new JScrollPane();

	public ScrollableTable(T table) {
		super(new BorderLayout());
		final JPanel northPanel = new JPanel();
		final JButton exportButton = new JButton("Save to file");
		exportButton.setToolTipText("Click to export table to a TAB-separated text file");
		exportButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				GuiUtils.saveTableToFile(ScrollableTable.this, getTable());
			}
		});
		northPanel.add(exportButton);
		add(northPanel, BorderLayout.NORTH);
		scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scroll.getVerticalScrollBar().setUnitIncrement(20);
		this.table = table;
		scroll.setViewportView(table);
		scroll.repaint();
		add(scroll, BorderLayout.CENTER);
	}

	public T getTable() {
		return table;
	}

	public JScrollPane getScroll() {
		return this.scroll;
	}
}
