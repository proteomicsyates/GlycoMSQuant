package edu.scripps.yates.glycomsquant.gui.tables.grouped_peptides;

import java.awt.BorderLayout;
import java.awt.Point;
import java.awt.SystemColor;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import javax.swing.DefaultListSelectionModel;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;

import org.apache.log4j.Logger;

import edu.scripps.yates.glycomsquant.GlycoSite;
import edu.scripps.yates.glycomsquant.GroupedQuantifiedPeptide;
import edu.scripps.yates.glycomsquant.gui.ProteinSequenceDialog;
import edu.scripps.yates.glycomsquant.gui.tables.runs.ColumnsRunTableUtil;
import gnu.trove.map.hash.THashMap;

public class AttachedPeptideListDialog extends JDialog {
	/**
	 * 
	 */
	private static final long serialVersionUID = -1119185903108814297L;
	private static final Logger log = Logger.getLogger(AttachedPeptideListDialog.class);

	private boolean minimized = false;
	private final int maxWidth;

	private final JScrollPane scrollPane;
	private ScrollableGroupedPeptidesTable table;
	private final ProteinSequenceDialog proteinSequenceDialog;
	private final ScrollableGroupedPeptidesTable peptideTable;
	private Map<String, GroupedQuantifiedPeptide> peptidesByPeptideKey;

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

		scrollPane = new JScrollPane();
		scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scrollPane.getVerticalScrollBar().setUnitIncrement(20);
		getContentPane().add(scrollPane, BorderLayout.CENTER);
		// contentPanel.setBackground(SystemColor.info);
		peptideTable = new ScrollableGroupedPeptidesTable();
		scrollPane.setViewportView(peptideTable);
		addWindowListeners();
	}

	protected List<GroupedQuantifiedPeptide> getSelectedPeptides(int[] selectedRows) {
		final List<GroupedQuantifiedPeptide> ret = new ArrayList<GroupedQuantifiedPeptide>();
		final List<ColumnsGroupedPeptidesTable> columns = ColumnsGroupedPeptidesTable.getColumns();
		final int peptideKeyIndex = columns.indexOf(ColumnsGroupedPeptidesTable.SEQUENCE);
		final int chargeIndex = columns.indexOf(ColumnsGroupedPeptidesTable.CHARGE);
		for (final int selectedRow : selectedRows) {
			final int row = peptideTable.getTable().getRowSorter().convertRowIndexToModel(selectedRow);
			final String fullSequence = peptideTable.getTable().getModel().getValueAt(row, peptideKeyIndex).toString();
			final String charge = peptideTable.getTable().getModel().getValueAt(row, chargeIndex).toString();
			final String peptideKey = fullSequence + "-" + charge;
			log.info(peptideKey + " selected");
			ret.add(getPeptidesBySequenceAndCharge().get(peptideKey));
		}
		return ret;
	}

	private Map<String, GroupedQuantifiedPeptide> getPeptidesBySequenceAndCharge() {
		if (peptidesByPeptideKey == null) {
			peptidesByPeptideKey = new THashMap<String, GroupedQuantifiedPeptide>();
			final List<ColumnsGroupedPeptidesTable> columns = ColumnsGroupedPeptidesTable.getColumns();
			final int fullSequenceIndex = columns.indexOf(ColumnsGroupedPeptidesTable.SEQUENCE);
			final int chargeIndex = columns.indexOf(ColumnsGroupedPeptidesTable.CHARGE);
			for (final GroupedQuantifiedPeptide peptide : getPeptides()) {
				final String fullSequence = ColumnsGroupedPeptidesTableUtil.getInstance()
						.getPeptideInfoList(peptide, getGlycoSites(), columns, getProteinSequence())
						.get(fullSequenceIndex).toString();
				final String charge = ColumnsGroupedPeptidesTableUtil.getInstance()
						.getPeptideInfoList(peptide, getGlycoSites(), columns, getProteinSequence()).get(chargeIndex)
						.toString();
				final String peptideKey = fullSequence + "-" + charge;
				if (peptidesByPeptideKey.containsKey(peptideKey)) {
					throw new IllegalArgumentException(peptideKey + " should be unique");
				}
				peptidesByPeptideKey.put(peptideKey, peptide);
			}
		}
		return peptidesByPeptideKey;
	}

	private List<GlycoSite> getGlycoSites() {
		return this.proteinSequenceDialog.getGlycoSites();
	}

	private List<GroupedQuantifiedPeptide> getPeptides() {
		return this.proteinSequenceDialog.getGroupedPeptides();
	}

	private String getProteinSequence() {
		return this.proteinSequenceDialog.getProteinSequence();
	}

	public MyGroupedPeptidesTable getTable() {
		return this.peptideTable.getTable();
	}

	public void loadTable(Collection<GroupedQuantifiedPeptide> peptidesToLoad) {
		peptideTable.getTable().clearData();

		addColumnsInTable(peptideTable.getTable(), ColumnsGroupedPeptidesTable.getColumnsStringForTable());

		if (getGlycoSites() != null && peptidesToLoad != null) {
			// sort peptides by key with sequence and charge
			final List<GroupedQuantifiedPeptide> list = new ArrayList<GroupedQuantifiedPeptide>();
			list.addAll(peptidesToLoad);
			Collections.sort(list, new Comparator<GroupedQuantifiedPeptide>() {

				@Override
				public int compare(GroupedQuantifiedPeptide o1, GroupedQuantifiedPeptide o2) {
					return o1.getKey(false).compareTo(o2.getKey(false));
				}
			});
			for (final GroupedQuantifiedPeptide peptide : list) {
				final MyGroupedPeptidesTableModel model = (MyGroupedPeptidesTableModel) peptideTable.getTable()
						.getModel();
				final List<Object> glycoSiteInfoList = ColumnsGroupedPeptidesTableUtil.getInstance().getPeptideInfoList(
						peptide, getGlycoSites(), ColumnsGroupedPeptidesTable.getColumns(), getProteinSequence());
				model.addRow(glycoSiteInfoList.toArray());
//				log.info("Table now with " + model.getRowCount() + " rows");

			}
			final DefaultListSelectionModel selectionModel = new DefaultListSelectionModel();
			this.peptideTable.getTable().setSelectionModel(selectionModel);
			// add listener to selection
			selectionModel.addListSelectionListener(new ListSelectionListener() {

				@Override
				public void valueChanged(ListSelectionEvent e) {
					final List<GroupedQuantifiedPeptide> selectedPeptides = new ArrayList<GroupedQuantifiedPeptide>();
					final int[] selectedRows = peptideTable.getTable().getSelectedRows();
					for (final int selectedRow : selectedRows) {
						final int rowInModel = peptideTable.getTable().convertRowIndexToModel(selectedRow);
						final GroupedQuantifiedPeptide peptide = list.get(rowInModel);
						selectedPeptides.add(peptide);
					}
					proteinSequenceDialog.highlightPeptidesOnSequence(selectedPeptides);
					proteinSequenceDialog.showChartsFromPeptides(selectedPeptides, -1);
				}
			});
			log.info(getPeptides().size() + " peptides added to attached window");
		}
		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				peptideTable.getTable().repaint();
				peptideTable.initializeSorter();
			}
		});
		this.proteinSequenceDialog.highlightPeptidesOnSequence(peptidesToLoad);

	}

	private void addColumnsInTable(MyGroupedPeptidesTable table, List<String> columnsStringList) {
		final DefaultTableModel defaultModel = (DefaultTableModel) table.getModel();
		log.info("Adding colums " + columnsStringList.size() + " columns");
		if (columnsStringList != null) {

			for (final String columnName : columnsStringList) {
				defaultModel.addColumn(columnName);
			}
			log.info("Added " + table.getColumnCount() + " colums");
			for (int i = 0; i < table.getColumnCount(); i++) {
				final TableColumn column = table.getColumnModel().getColumn(i);
				final ColumnsGroupedPeptidesTable[] columHeaders = ColumnsGroupedPeptidesTable.values();
				boolean set = false;
				for (final ColumnsGroupedPeptidesTable header : columHeaders) {
					if (column.getHeaderValue().equals(header.getName())) {
						column.setPreferredWidth(header.getDefaultWidth());
						set = true;
					}
//					column.setMaxWidth(header.getDefaultWidth());
//					column.setMinWidth(header.getDefaultWidth());
				}
				if (!set) {
					log.info("äsdf ");
				}
				column.setResizable(true);
			}
		}
	}

	private void addWindowListeners() {
		this.proteinSequenceDialog.addComponentListener(new ComponentListener() {

			@Override
			public void componentShown(ComponentEvent e) {
//				log.debug(e.getID());
				AttachedPeptideListDialog.this.setVisible(true);
			}

			@Override
			public void componentResized(ComponentEvent e) {
//				log.debug(e.getID());
				AttachedPeptideListDialog.this.setVisible(true);

			}

			@Override
			public void componentMoved(ComponentEvent e) {
				AttachedPeptideListDialog.this.setVisible(true);
			}

			@Override
			public void componentHidden(ComponentEvent e) {
//				log.debug(e.getID());
				AttachedPeptideListDialog.this.setVisible(false);
			}
		});
		this.proteinSequenceDialog.addWindowListener(new WindowListener() {

			@Override
			public void windowOpened(WindowEvent e) {
//				log.debug(e.getID() + " from " + e.getOldState() + " to " + e.getNewState());
				// open by default
				AttachedPeptideListDialog.this.setVisible(true);
			}

			@Override
			public void windowClosing(WindowEvent e) {
//				log.debug(e.getID() + " from " + e.getOldState() + " to " + e.getNewState());

			}

			@Override
			public void windowClosed(WindowEvent e) {
//				log.debug(e.getID() + " from " + e.getOldState() + " to " + e.getNewState());
				AttachedPeptideListDialog.this.dispose();

			}

			@Override
			public void windowIconified(WindowEvent e) {
//				log.debug(e.getID() + " from " + e.getOldState() + " to " + e.getNewState());
				minimized = true;
				AttachedPeptideListDialog.this.setVisible(false);

			}

			@Override
			public void windowDeiconified(WindowEvent e) {
//				log.debug(e.getID() + " from " + e.getOldState() + " to " + e.getNewState());
				minimized = false;
				AttachedPeptideListDialog.this.setVisible(true);

			}

			@Override
			public void windowActivated(WindowEvent e) {
				log.debug(
						e.getID() + " from " + e.getOldState() + " to " + e.getNewState() + " minimized=" + minimized);
				AttachedPeptideListDialog.this.setVisible(true);
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
				proteinSequenceDialog.requestFocus();
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

		final Point parentLocationOnScreen = this.proteinSequenceDialog.getLocation();
		final int x = parentLocationOnScreen.x + this.proteinSequenceDialog.getWidth();
		final int y = parentLocationOnScreen.y;
		this.setLocation(x, y);
		this.setSize(maxWidth, this.proteinSequenceDialog.getHeight());
		log.debug("Setting position next to the parent frame (" + x + "," + y + ")");
	}

	public void setMinimized(boolean b) {
		this.minimized = b;
	}

}
