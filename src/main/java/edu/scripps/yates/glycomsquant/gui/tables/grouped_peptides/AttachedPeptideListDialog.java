package edu.scripps.yates.glycomsquant.gui.tables.grouped_peptides;

import java.awt.BorderLayout;
import java.awt.Point;
import java.awt.SystemColor;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.Collection;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.apache.log4j.Logger;

import edu.scripps.yates.glycomsquant.GroupedQuantifiedPeptide;
import edu.scripps.yates.glycomsquant.gui.ProteinSequenceDialog;
import edu.scripps.yates.glycomsquant.gui.tables.individual_peptides.ScrollablePeptidesTable;
import edu.scripps.yates.glycomsquant.gui.tables.runs.ColumnsRunTableUtil;

public class AttachedPeptideListDialog extends JDialog {
	/**
	 * 
	 */
	private static final long serialVersionUID = -1119185903108814297L;
	private static final Logger log = Logger.getLogger(AttachedPeptideListDialog.class);

	private boolean minimized = false;
	private final int maxWidth;

	private final JScrollPane scrollPane1;
	private ScrollableGroupedPeptidesTable table;
	private final ProteinSequenceDialog proteinSequenceDialog;
	private final ScrollableGroupedPeptidesTable scrollableGroupedPeptidesTable;
	private final JScrollPane scrollPane2;
	private final ScrollablePeptidesTable individualPeptidesTable;

	public AttachedPeptideListDialog(JFrame parentFrame, int maxWidth) {

		super(parentFrame, ModalityType.MODELESS);
		this.proteinSequenceDialog = (ProteinSequenceDialog) parentFrame;
		this.maxWidth = maxWidth;
		getContentPane().setBackground(SystemColor.info);
		setTitle("GlycoMSQuant runs");
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
		scrollPane1 = new JScrollPane();
		scrollPane1.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scrollPane1.getVerticalScrollBar().setUnitIncrement(20);
		scrollableGroupedPeptidesTable = new ScrollableGroupedPeptidesTable(proteinSequenceDialog);
		scrollPane1.setViewportView(scrollableGroupedPeptidesTable);
		split.setTopComponent(scrollPane1);
		// individual peptides
		scrollPane2 = new JScrollPane();
		scrollPane2.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scrollPane2.getVerticalScrollBar().setUnitIncrement(20);
		individualPeptidesTable = new ScrollablePeptidesTable(false);
		// if we select a grouped peptide, we show individual peptides
		scrollableGroupedPeptidesTable.getTable().setSelectionListenerIndividualPeptidesTable(individualPeptidesTable);
		scrollPane2.setViewportView(individualPeptidesTable);
		split.setBottomComponent(scrollPane2);
		addWindowListeners();
	}

	public MyGroupedPeptidesTable getTable() {
		return this.scrollableGroupedPeptidesTable.getTable();
	}

	public void loadTable(Collection<GroupedQuantifiedPeptide> peptidesToLoad, Integer positionInProtein) {
		scrollableGroupedPeptidesTable.getTable().loadTable(peptidesToLoad, positionInProtein);
	}

	private void addWindowListeners() {
		this.proteinSequenceDialog.addComponentListener(new ComponentListener() {

			@Override
			public void componentShown(ComponentEvent e) {
				AttachedPeptideListDialog.this.setVisible(true);
			}

			@Override
			public void componentResized(ComponentEvent e) {
				AttachedPeptideListDialog.this.setVisible(true);

			}

			@Override
			public void componentMoved(ComponentEvent e) {
				AttachedPeptideListDialog.this.setVisible(true);
			}

			@Override
			public void componentHidden(ComponentEvent e) {
				AttachedPeptideListDialog.this.setVisible(false);
			}
		});
		this.proteinSequenceDialog.addWindowListener(new WindowListener() {

			@Override
			public void windowOpened(WindowEvent e) {
				AttachedPeptideListDialog.this.setVisible(true);
			}

			@Override
			public void windowClosing(WindowEvent e) {

			}

			@Override
			public void windowClosed(WindowEvent e) {
				AttachedPeptideListDialog.this.dispose();

			}

			@Override
			public void windowIconified(WindowEvent e) {
				minimized = true;
				AttachedPeptideListDialog.this.setVisible(false);

			}

			@Override
			public void windowDeiconified(WindowEvent e) {
				minimized = false;
				AttachedPeptideListDialog.this.setVisible(true);

			}

			@Override
			public void windowActivated(WindowEvent e) {

				AttachedPeptideListDialog.this.setVisible(true);
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
		ColumnsRunTableUtil.scrollToBeginning(this.scrollPane1);
		ColumnsRunTableUtil.scrollToBeginning(this.scrollPane2);
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
