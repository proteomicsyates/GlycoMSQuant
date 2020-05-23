package edu.scripps.yates.glycomsquant.gui.tables;

import java.awt.BorderLayout;
import java.awt.Point;
import java.awt.SystemColor;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JSplitPane;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.apache.log4j.Logger;

import edu.scripps.yates.glycomsquant.gui.ProteinSequenceDialog;
import edu.scripps.yates.glycomsquant.gui.tables.grouped_peptides.MyGroupedPeptidesTable;
import edu.scripps.yates.glycomsquant.gui.tables.individual_peptides.MyPeptidesTable;
import edu.scripps.yates.glycomsquant.gui.tables.runs.ColumnsRunTableUtil;
import edu.scripps.yates.glycomsquant.gui.tables.scrollables.ScrollableTable;

public class GroupedPeptideListDialog extends JDialog {
	/**
	 * 
	 */
	private static final long serialVersionUID = -1119185903108814297L;
	private static final Logger log = Logger.getLogger(GroupedPeptideListDialog.class);

	private boolean minimized = false;
	private final int maxWidth;

	private final ProteinSequenceDialog proteinSequenceDialog;
	private final ScrollableTable<MyGroupedPeptidesTable> scrollableGroupedPeptidesTable;
	private final ScrollableTable<MyPeptidesTable> scrollableIndividualPeptidesTable;

	public GroupedPeptideListDialog(JFrame parentFrame, int maxWidth) {

		super(parentFrame, ModalityType.MODELESS);
		this.proteinSequenceDialog = (ProteinSequenceDialog) parentFrame;
		this.maxWidth = maxWidth;
		getContentPane().setBackground(SystemColor.info);
		setTitle("GlycoMSQuant peptides table");
		setFocusableWindowState(false);
		setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (UnsupportedLookAndFeelException | ClassNotFoundException | InstantiationException
				| IllegalAccessException e) {
			e.printStackTrace();
		}

		final JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		getContentPane().add(split, BorderLayout.CENTER);
		// grouped peptides

		scrollableGroupedPeptidesTable = new ScrollableTable<MyGroupedPeptidesTable>(
				new MyGroupedPeptidesTable(proteinSequenceDialog));
		split.setTopComponent(scrollableGroupedPeptidesTable);
		//
		//
		// individual peptides

		scrollableIndividualPeptidesTable = new ScrollableTable<MyPeptidesTable>(new MyPeptidesTable(false));
		// if we select a grouped peptide, we show individual peptides
		scrollableGroupedPeptidesTable.getTable()
				.setSelectionListenerIndividualPeptidesTable(scrollableIndividualPeptidesTable);
		split.setBottomComponent(scrollableIndividualPeptidesTable);
		addWindowListeners();
	}

	public MyGroupedPeptidesTable getGroupedPeptidesTable() {
		return this.scrollableGroupedPeptidesTable.getTable();
	}

	public MyPeptidesTable getIndividualPeptidesTable() {
		return this.scrollableIndividualPeptidesTable.getTable();
	}

	private void addWindowListeners() {
		this.proteinSequenceDialog.addComponentListener(new ComponentListener() {

			@Override
			public void componentShown(ComponentEvent e) {
				GroupedPeptideListDialog.this.setVisible(true);
			}

			@Override
			public void componentResized(ComponentEvent e) {
				GroupedPeptideListDialog.this.setVisible(true);

			}

			@Override
			public void componentMoved(ComponentEvent e) {
				GroupedPeptideListDialog.this.setVisible(true);
			}

			@Override
			public void componentHidden(ComponentEvent e) {
				GroupedPeptideListDialog.this.setVisible(false);
			}
		});
		this.proteinSequenceDialog.addWindowListener(new WindowListener() {

			@Override
			public void windowOpened(WindowEvent e) {
				GroupedPeptideListDialog.this.setVisible(true);
			}

			@Override
			public void windowClosing(WindowEvent e) {

			}

			@Override
			public void windowClosed(WindowEvent e) {
				GroupedPeptideListDialog.this.dispose();

			}

			@Override
			public void windowIconified(WindowEvent e) {
				minimized = true;
				GroupedPeptideListDialog.this.setVisible(false);

			}

			@Override
			public void windowDeiconified(WindowEvent e) {
				minimized = false;
				GroupedPeptideListDialog.this.setVisible(true);

			}

			@Override
			public void windowActivated(WindowEvent e) {

				GroupedPeptideListDialog.this.setVisible(true);
			}

			@Override
			public void windowDeactivated(WindowEvent e) {

				// AttachedHelpDialog.this.setVisible(false);
			}

		});
		this.addWindowListener(new WindowListener() {

			@Override
			public void windowOpened(WindowEvent e) {
			}

			@Override
			public void windowIconified(WindowEvent e) {
			}

			@Override
			public void windowDeiconified(WindowEvent e) {
			}

			@Override
			public void windowDeactivated(WindowEvent e) {
			}

			@Override
			public void windowClosing(WindowEvent e) {
				setVisible(false);
				minimized = true;

			}

			@Override
			public void windowClosed(WindowEvent e) {
			}

			@Override
			public void windowActivated(WindowEvent e) {
			}
		});
	}

	@Override
	public void setVisible(boolean b) {
		if (b) {
			if (!minimized) {
				super.setVisible(b);
				positionNextToParent();
				proteinSequenceDialog.requestFocus();
				// if (!scrolledToBeggining) {
				// }
			}
		} else {
			super.setVisible(b);
		}
	}

	public void forceVisible() {
		minimized = false;
		setVisible(true);
		ColumnsRunTableUtil.scrollToBeginning(this.scrollableGroupedPeptidesTable.getScroll());
		ColumnsRunTableUtil.scrollToBeginning(this.scrollableIndividualPeptidesTable.getScroll());
	}

	@Override
	public void dispose() {
		log.info("Dialog dispose");
		super.dispose();
		this.minimized = true;

	}

	private void positionNextToParent() {

		final Point parentLocationOnScreen = this.proteinSequenceDialog.getLocation();
		final int x = parentLocationOnScreen.x + this.proteinSequenceDialog.getWidth();
		final int y = parentLocationOnScreen.y;
		this.setLocation(x, y);
		this.setSize(maxWidth, this.proteinSequenceDialog.getHeight());
//		log.debug("Setting position next to the parent frame (" + x + "," + y + ")");
	}

	public void setMinimized(boolean b) {
		this.minimized = b;
	}

}
