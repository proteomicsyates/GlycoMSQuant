package edu.scripps.yates.glycomsquant.gui.tables.scrollables;

import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.DefaultListSelectionModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.apache.log4j.Logger;

import edu.scripps.yates.glycomsquant.gui.MainFrame;
import edu.scripps.yates.glycomsquant.gui.files.FileManager;
import edu.scripps.yates.glycomsquant.gui.tables.runs.ColumnsRunTable;
import edu.scripps.yates.glycomsquant.gui.tables.runs.ColumnsRunTableUtil;
import edu.scripps.yates.glycomsquant.gui.tables.runs.MyRunsTable;

public class ScrollableRunsTable extends JPanel {
	/**
	 * 
	 */
	private static final long serialVersionUID = 725114162692158140L;

	private static Logger log = Logger.getLogger(ScrollableRunsTable.class);

	private final MyRunsTable table = new MyRunsTable();

	private JButton deleteRunButton;

	private JScrollPane scroll;

	private static boolean dontAskAndLoadResults = false;

	public ScrollableRunsTable(int wide) {

		initializeUI(wide);
	}

	private void initializeUI(int wide) {
		setLayout(new BorderLayout());

		setPreferredSize(new Dimension(wide, 400));

		//
		// Turn off JTable's auto resize so that JScrollpane
		// will show a horizontal scroll bar.
		//
		// table.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
		table.setSize(new Dimension(wide, 400));
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		addRunTableListeners(table);

		// in the north, add the delete button
		final JPanel deleterunPanel = new JPanel();
		deleteRunButton = new JButton("Delete selected run(s)");
		deleteRunButton.setToolTipText("Click to delete the selected run(s)");
		deleteRunButton.setEnabled(false);
		deleteRunButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {

				deleteSelectedRuns();
			}
		});
		deleterunPanel.add(deleteRunButton);
		add(deleterunPanel, BorderLayout.NORTH);

		scroll = new JScrollPane(table);

		add(scroll, BorderLayout.CENTER);

		super.repaint();
	}

	public JScrollPane getScrollPane() {
		return this.scroll;
	}

	protected void deleteSelectedRuns() {
		final int[] selectedRows = table.getSelectedRows();
		String article = "this";
		String plural = "";
		if (selectedRows.length > 1) {
			article = "these";
			plural = "s";
		}
		final int option = JOptionPane
				.showConfirmDialog(this,
						"Are you sure you want to delete " + article + " run" + plural
								+ " from your system? This cannot be undone.",
						"Delete runs", JOptionPane.YES_NO_CANCEL_OPTION);
		if (option == JOptionPane.YES_OPTION) {
			final int column = ColumnsRunTable.getColumns().indexOf(ColumnsRunTable.RUN_PATH);
			// collect the paths of the runs before and then delete, otherwise the indexes
			// may move
			final List<String> paths = new ArrayList<String>();
			for (final int selectedRow : selectedRows) {
				final int modelIndex = table.convertRowIndexToModel(selectedRow);
				final String runPath = table.getValueAt(modelIndex, column).toString();
				paths.add(runPath);

			}
			log.info("Deleting " + paths.size() + " runs");
			boolean someError = false;
			for (final String runPath : paths) {

				try {
					FileManager.removeRunFolder(runPath);
					loadRunsToTable();

				} catch (final IOException e) {
					someError = true;
					e.printStackTrace();
					JOptionPane.showMessageDialog(this, "Error deleting run '" + runPath + "'", "Error deleting run",
							JOptionPane.ERROR_MESSAGE);
				}
			}
			if (!someError) {
				JOptionPane.showMessageDialog(this, "Run" + plural + " deleted.", "Confirmation of deletion",
						JOptionPane.INFORMATION_MESSAGE);
			}
		}

	}

	private void addRunTableListeners(MyRunsTable table) {
		if (table != null) {
			table.setSelectionModel(new DefaultListSelectionModel());
			table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

				@Override
				public void valueChanged(ListSelectionEvent e) {
					if (table.getSelectedRowCount() > 1) {
						MainFrame.getInstance().enableRunComparison(true);
					} else {
						MainFrame.getInstance().enableRunComparison(false);
					}
					ScrollableRunsTable.this.deleteRunButton.setEnabled(table.getSelectedRowCount() > 0);

				}
			});
			table.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent e) {
					try {
						final JTable target = (JTable) e.getSource();
						int row = target.getSelectedRow();
						if (row == -1) {
							// no selection
							return;
						}
						if (target.getRowSorter() != null) {
							row = target.getRowSorter().convertRowIndexToModel(row);
						}
						final int column = target.getSelectedColumn();
						log.info("Row=" + row + " Column=" + column);
						// double click on left button
						if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {

							final int index = ColumnsRunTable.getColumns().indexOf(ColumnsRunTable.RUN_PATH);
							String value = target.getModel().getValueAt(row, index).toString();
							// remove the PREFIX
							value = value.replaceAll(ColumnsRunTableUtil.PREFIX, "");
							clickOnresult(value);
							return;

						} else if (e.getClickCount() == 1) {
//							final int index = ColumnsRunTable.getColumns().indexOf(ColumnsRunTable.RUN_PATH);
//							final String value = target.getModel().getValueAt(row, index).toString();
//							softClickOnResult(value);
							return;
						}

					} catch (final IllegalArgumentException ex) {
						ex.printStackTrace();
					}
				}
			});
		}
	}

	protected void softClickOnResult(String value) {
		MainFrame.getInstance().updateControlsWithParametersFromDisk(new File(value));

	}

	/**
	 * 
	 * @param individualResultsFolder folder to individual results
	 */
	protected void loadResultsFromDisk(File individualResultsFolder) {
		MainFrame.getInstance().loadResultsFromDisk(individualResultsFolder);

	}

	public MyRunsTable getTable() {
		return table;
	}

	protected void openExplorer(String runPath) {

		final File folder = new File(runPath);
		try {
			Desktop.getDesktop().open(folder);
		} catch (final IOException e) {
			e.printStackTrace();
		}

	}

	private final static String LOAD_RESULTS = "Load results";
	private final static String LOAD_RESULTS_AND_DONT_ASK = "Load results (and don't ask me if I click again)";
	private final static String OPEN_RESULTS_FOLDER = "Open results folder";

	protected void clickOnresult(String runPath) {

		final String[] selectionValues = { LOAD_RESULTS, LOAD_RESULTS_AND_DONT_ASK, OPEN_RESULTS_FOLDER };
		final JFrame frame = new JFrame();
		final JPanel panel = new JPanel();
		frame.getContentPane().add(panel);
		if (!dontAskAndLoadResults) {
			final int selectedOption = JOptionPane.showOptionDialog(panel,
					"Do you want to open the results folder in your system explorer or load the results into the tool?",
					"Results selected", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null,
					selectionValues, LOAD_RESULTS);
			if (OPEN_RESULTS_FOLDER.equals(selectionValues[selectedOption])) {
				openExplorer(runPath);
			} else if (LOAD_RESULTS.equals(selectionValues[selectedOption])) {
				loadResultsFromDisk(new File(runPath));
			} else if (LOAD_RESULTS_AND_DONT_ASK.equals(selectionValues[selectedOption])) {
				dontAskAndLoadResults = true;
				loadResultsFromDisk(new File(runPath));
			}
		} else {
			loadResultsFromDisk(new File(runPath));
		}
	}

	public void loadRunsToTable() {
		table.loadRunsToTable();
	}

}
