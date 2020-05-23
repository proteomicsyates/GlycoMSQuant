package edu.scripps.yates.glycomsquant.gui.tables;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.apache.log4j.Logger;

import edu.scripps.yates.glycomsquant.gui.tables.individual_peptides.ColumnsPeptidesTableUtil;
import edu.scripps.yates.glycomsquant.gui.tables.individual_peptides.MyPeptidesTable;
import edu.scripps.yates.glycomsquant.gui.tables.scrollables.ScrollableTable;
import edu.scripps.yates.glycomsquant.util.GuiUtils;

public class PeptidesTableDialog extends JFrame {
	/**
	 * 
	 */
	private static final long serialVersionUID = -1119185903108814297L;
	private static final Logger log = Logger.getLogger(PeptidesTableDialog.class);
	private final ScrollableTable<MyPeptidesTable> scrollableTable;

	public PeptidesTableDialog() {
		super();
		final Dimension preferredSize = new Dimension(GuiUtils.getFractionOfScreenWidthSize(3.0 / 5),
				GuiUtils.getFractionOfScreenHeightSize(3.0 / 4));
		setPreferredSize(preferredSize);
		setTitle("GlycoMSQuant peptides table");
		setFocusableWindowState(true);
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (UnsupportedLookAndFeelException | ClassNotFoundException | InstantiationException
				| IllegalAccessException e) {
			e.printStackTrace();
		}

		// contentPanel.setBackground(SystemColor.info);
		log.info("adding panels");

		scrollableTable = new ScrollableTable<MyPeptidesTable>(new MyPeptidesTable(true));
		getContentPane().add(scrollableTable, BorderLayout.CENTER);

		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				scrollableTable.getTable().repaint();
				scrollableTable.getTable().initializeSorter();
			}
		});
		pack();
		final java.awt.Dimension screenSize = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
		final java.awt.Dimension dialogSize = getSize();
		setLocation((screenSize.width - dialogSize.width) / 2, (screenSize.height - dialogSize.height) / 2);
	}

	public void forceVisible() {
		setVisible(true);
		ColumnsPeptidesTableUtil.scrollToBeginning(this.scrollableTable.getScroll());

	}

	@Override
	public void dispose() {
		log.info("Dialog dispose");
		super.dispose();

	}

	public MyPeptidesTable getTable() {
		return scrollableTable.getTable();
	}

}