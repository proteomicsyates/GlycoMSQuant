package edu.scripps.yates.glycomsquant.gui.tables.runs;

import java.awt.BorderLayout;
import java.awt.Point;
import java.awt.SystemColor;
import java.awt.Window;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.apache.log4j.Logger;

public class AttachedRunsDialog extends JDialog {
	/**
	 * 
	 */
	private static final long serialVersionUID = -1119185903108814297L;
	private static final Logger log = Logger.getLogger(AttachedRunsDialog.class);
	private final Window parentFrame;

	private boolean minimized = false;
	private final int maxWidth;

	private final JScrollPane scrollPane;
	private ScrollableRunsTable table;

	public AttachedRunsDialog(Window parentWindow, int maxWidth) {
		super(parentWindow, ModalityType.MODELESS);

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
		this.parentFrame = parentWindow;

		scrollPane = new JScrollPane();
		scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scrollPane.getVerticalScrollBar().setUnitIncrement(20);
		getContentPane().add(scrollPane, BorderLayout.CENTER);
		// contentPanel.setBackground(SystemColor.info);

		addWindowListeners();
	}

	public void loadResultFolders() {

		log.info("Loading result folders");

		final JPanel contentPanel = new JPanel();
		scrollPane.setViewportView(contentPanel);

		contentPanel.setLayout(new BorderLayout());

		log.info("adding panels");
		final JPanel northPanel = new JPanel();
		// panel.setMaximumSize(new Dimension(100, Integer.MAX_VALUE));
		northPanel.setLayout(new BorderLayout());

		contentPanel.add(northPanel, BorderLayout.NORTH);
		table = new ScrollableRunsTable(200);
		contentPanel.add(table, BorderLayout.CENTER);
		table.loadRunsToTable();

	}

	private void addWindowListeners() {
		this.parentFrame.addComponentListener(new ComponentListener() {

			@Override
			public void componentShown(ComponentEvent e) {
//				log.debug(e.getID());
				AttachedRunsDialog.this.setVisible(true);
			}

			@Override
			public void componentResized(ComponentEvent e) {
//				log.debug(e.getID());
				AttachedRunsDialog.this.setVisible(true);

			}

			@Override
			public void componentMoved(ComponentEvent e) {
				AttachedRunsDialog.this.setVisible(true);
			}

			@Override
			public void componentHidden(ComponentEvent e) {
//				log.debug(e.getID());
				AttachedRunsDialog.this.setVisible(false);
			}
		});
		this.parentFrame.addWindowListener(new WindowListener() {

			@Override
			public void windowOpened(WindowEvent e) {
//				log.debug(e.getID() + " from " + e.getOldState() + " to " + e.getNewState());
				// open by default
				AttachedRunsDialog.this.setVisible(true);
			}

			@Override
			public void windowClosing(WindowEvent e) {
//				log.debug(e.getID() + " from " + e.getOldState() + " to " + e.getNewState());

			}

			@Override
			public void windowClosed(WindowEvent e) {
//				log.debug(e.getID() + " from " + e.getOldState() + " to " + e.getNewState());
				AttachedRunsDialog.this.dispose();

			}

			@Override
			public void windowIconified(WindowEvent e) {
//				log.debug(e.getID() + " from " + e.getOldState() + " to " + e.getNewState());
				minimized = true;
				AttachedRunsDialog.this.setVisible(false);

			}

			@Override
			public void windowDeiconified(WindowEvent e) {
//				log.debug(e.getID() + " from " + e.getOldState() + " to " + e.getNewState());
				minimized = false;
				AttachedRunsDialog.this.setVisible(true);

			}

			@Override
			public void windowActivated(WindowEvent e) {
				log.debug(
						e.getID() + " from " + e.getOldState() + " to " + e.getNewState() + " minimized=" + minimized);
				AttachedRunsDialog.this.setVisible(true);
			}

			@Override
			public void windowDeactivated(WindowEvent e) {
				log.debug(
						e.getID() + " from " + e.getOldState() + " to " + e.getNewState() + " minimized=" + minimized);
				// AttachedHelpDialog.this.setVisible(false);
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
		ColumnsRunTableUtil.scrollToBeginning(this.scrollPane);

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

	public List<String> getSelectedRunPathss() {
		final List<ColumnsRunTable> columns = ColumnsRunTable.getColumns();
		final int runPathIndex = columns.indexOf(ColumnsRunTable.RUN_PATH);
		final int[] selectedRows = this.table.getTable().getSelectedRows();
		final List<String> ret = new ArrayList<String>();
		for (final int row : selectedRows) {
			final int rowInModel = this.table.getTable().getRowSorter().convertRowIndexToModel(row);
			String runPath = table.getTable().getModel().getValueAt(rowInModel, runPathIndex).toString();
			runPath = runPath.replaceAll(ColumnsRunTableUtil.PREFIX, "");
			ret.add(runPath);
		}
		return ret;
	}

}
