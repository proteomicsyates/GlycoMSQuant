package edu.scripps.yates.glycomsquant.gui.tables;

import java.awt.BorderLayout;
import java.awt.Point;
import java.awt.SystemColor;
import java.awt.Window;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.JDialog;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.apache.log4j.Logger;

import edu.scripps.yates.glycomsquant.gui.tables.runs.ColumnsRunTableUtil;
import edu.scripps.yates.glycomsquant.gui.tables.scrollables.ScrollableRunsTable;

public class AttachedRunsDialog extends JDialog {
	/**
	 * 
	 */
	private static final long serialVersionUID = -1119185903108814297L;
	private static final Logger log = Logger.getLogger(AttachedRunsDialog.class);
	private final Window parentFrame;

	private boolean minimized = false;
	private final int maxWidth;

//	private final JScrollPane scrollPane;
	private final ScrollableRunsTable scrollableRunsTable;

	public AttachedRunsDialog(Window parentWindow, int maxWidth) {
		super(parentWindow, ModalityType.MODELESS);

		this.maxWidth = maxWidth;
		getContentPane().setBackground(SystemColor.info);
		setTitle("GlycoMSQuant runs");
//		setFocusableWindowState(false);
		setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (UnsupportedLookAndFeelException | ClassNotFoundException | InstantiationException
				| IllegalAccessException e) {
			e.printStackTrace();
		}
		this.parentFrame = parentWindow;

		scrollableRunsTable = new ScrollableRunsTable(200);

		getContentPane().add(scrollableRunsTable, BorderLayout.CENTER);

		addWindowListeners();
	}

	public ScrollableRunsTable getScrollableTable() {
		return this.scrollableRunsTable;
	}

	public void loadResultFolders() {

		log.info("Loading result folders");

		scrollableRunsTable.getTable().loadRunsToTable();

	}

	private void addWindowListeners() {
		this.parentFrame.addComponentListener(new ComponentListener() {

			@Override
			public void componentShown(ComponentEvent e) {
				log.debug(e.getID());
				AttachedRunsDialog.this.setVisible(true);
			}

			@Override
			public void componentResized(ComponentEvent e) {
				log.debug(e.getID());
				AttachedRunsDialog.this.setVisible(true);

			}

			@Override
			public void componentMoved(ComponentEvent e) {
				AttachedRunsDialog.this.setVisible(true);
			}

			@Override
			public void componentHidden(ComponentEvent e) {
				log.debug(e.getID());
				AttachedRunsDialog.this.setVisible(false);
			}
		});
		this.parentFrame.addWindowListener(new WindowListener() {

			@Override
			public void windowOpened(WindowEvent e) {
				log.debug(e.getID() + " from " + e.getOldState() + " to " + e.getNewState());
				// open by default
				AttachedRunsDialog.this.setVisible(true);
			}

			@Override
			public void windowClosing(WindowEvent e) {
				log.debug(e.getID() + " from " + e.getOldState() + " to " + e.getNewState());

			}

			@Override
			public void windowClosed(WindowEvent e) {
				log.debug(e.getID() + " from " + e.getOldState() + " to " + e.getNewState());
				AttachedRunsDialog.this.dispose();

			}

			@Override
			public void windowIconified(WindowEvent e) {
				log.debug(e.getID() + " from " + e.getOldState() + " to " + e.getNewState());
				minimized = true;
				AttachedRunsDialog.this.setVisible(false);

			}

			@Override
			public void windowDeiconified(WindowEvent e) {
				log.debug(e.getID() + " from " + e.getOldState() + " to " + e.getNewState());
				minimized = false;
				AttachedRunsDialog.this.setVisible(true);

			}

			@Override
			public void windowActivated(WindowEvent e) {
				log.debug(
						e.getID() + " from " + e.getOldState() + " to " + e.getNewState() + " minimized=" + minimized);
//				AttachedRunsDialog.this.setVisible(true);
			}

			@Override
			public void windowDeactivated(WindowEvent e) {
				log.debug(
						e.getID() + " from " + e.getOldState() + " to " + e.getNewState() + " minimized=" + minimized);
//				AttachedRunsDialog.this.setVisible(false);
			}

		});
		this.addWindowListener(new WindowListener() {

			@Override
			public void windowOpened(WindowEvent e) {
				log.debug(e.getID() + " " + e.getNewState() + " from " + e.getOldState());
			}

			@Override
			public void windowIconified(WindowEvent e) {
				log.debug(e.getID() + " " + e.getNewState() + " from " + e.getOldState());
			}

			@Override
			public void windowDeiconified(WindowEvent e) {
				log.debug(e.getID() + " " + e.getNewState() + " from " + e.getOldState());
			}

			@Override
			public void windowDeactivated(WindowEvent e) {
				log.debug(e.getID() + " " + e.getNewState() + " from " + e.getOldState());
			}

			@Override
			public void windowClosing(WindowEvent e) {
				log.debug(e.getID() + " " + e.getNewState() + " from " + e.getOldState());
				setVisible(false);
				minimized = true;

			}

			@Override
			public void windowClosed(WindowEvent e) {
				log.debug(e.getID() + " " + e.getNewState() + " from " + e.getOldState());
			}

			@Override
			public void windowActivated(WindowEvent e) {
				log.debug(e.getID() + " " + e.getNewState() + " from " + e.getOldState());
			}
		});
	}

	@Override
	public void setVisible(boolean b) {
		if (b) {
			if (!minimized) {
				log.debug("setting help dialog visible to " + b);
				super.setVisible(b);
				positionNextToParent();
				parentFrame.requestFocus();
				// if (!scrolledToBeggining) {
				// }
			}
		} else {
			log.debug("setting help dialog visible to " + b);
			super.setVisible(b);
		}
	}

	public void forceVisible() {
		minimized = false;
		setVisible(true);
		ColumnsRunTableUtil.scrollToBeginning(this.scrollableRunsTable.getScrollPane());

	}

	@Override
	public void dispose() {
		log.info("Dialog dispose");
		super.dispose();
		this.minimized = true;

	}

	private void positionNextToParent() {

		final Point parentLocationOnScreen = this.parentFrame.getLocation();
		final int x = parentLocationOnScreen.x + this.parentFrame.getWidth();
		final int y = parentLocationOnScreen.y;
		this.setLocation(x, y);
		this.setSize(maxWidth, this.parentFrame.getHeight());
		log.debug("Setting position next to the parent frame (" + x + "," + y + ")");
	}

	public void setMinimized(boolean b) {
		this.minimized = b;
	}

}
