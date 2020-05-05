package edu.scripps.yates.glycomsquant.gui.tables.individual_peptides;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.SystemColor;
import java.util.List;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;

import org.apache.log4j.Logger;

import edu.scripps.yates.census.read.model.interfaces.QuantifiedPeptideInterface;
import edu.scripps.yates.glycomsquant.GlycoSite;
import edu.scripps.yates.glycomsquant.util.GuiUtils;

public class PeptidesTableDialog extends JFrame {
	/**
	 * 
	 */
	private static final long serialVersionUID = -1119185903108814297L;
	private static final Logger log = Logger.getLogger(PeptidesTableDialog.class);
	private ScrollablePeptidesTable scrollableTable;
	private final Dimension preferredSize;

	public PeptidesTableDialog() {
		super();
		preferredSize = new Dimension(GuiUtils.getFractionOfScreenWidthSize(3.0 / 5),
				GuiUtils.getFractionOfScreenHeightSize(3.0 / 4));
		setPreferredSize(preferredSize);
		getContentPane().setBackground(SystemColor.info);
		setTitle("GlycoMSQuant peptides results table");
//		setFocusableWindowState(false);
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (UnsupportedLookAndFeelException | ClassNotFoundException | InstantiationException
				| IllegalAccessException e) {
			e.printStackTrace();
		}

		// contentPanel.setBackground(SystemColor.info);

		pack();
		final java.awt.Dimension screenSize = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
		final java.awt.Dimension dialogSize = getSize();
		setLocation((screenSize.width - dialogSize.width) / 2, (screenSize.height - dialogSize.height) / 2);
	}

	public void loadResultTable(List<QuantifiedPeptideInterface> peptides, List<GlycoSite> glycoSites,
			String proteinSequence) {

		log.info("Loading result peptide table with " + glycoSites.size() + " glyco sites");

//		final JPanel contentPanel = new JPanel();
//		scrollPane.setViewportView(contentPanel);

//		contentPanel.setLayout(new BorderLayout());

		log.info("adding panels");
		final JPanel northPanel = new JPanel();
		// panel.setMaximumSize(new Dimension(100, Integer.MAX_VALUE));
		northPanel.setLayout(new BorderLayout());

//		contentPanel.add(northPanel, BorderLayout.NORTH);
		scrollableTable = new ScrollablePeptidesTable(true);
		getContentPane().add(scrollableTable, BorderLayout.CENTER);
//		contentPanel.add(table, BorderLayout.CENTER);
		scrollableTable.getTable().clearData();

		addColumnsInTable(scrollableTable.getTable(), ColumnsPeptidesTable.getColumnsStringForTable(true));

		if (glycoSites != null) {
			for (int i = 0; i < peptides.size(); i++) {
				final QuantifiedPeptideInterface peptide = peptides.get(i);
				final MyPeptidesTableModel model = (MyPeptidesTableModel) scrollableTable.getTable().getModel();

				final List<Object> glycoSiteInfoList = ColumnsPeptidesTableUtil.getInstance().getPeptideInfoList(
						peptide, glycoSites, ColumnsPeptidesTable.getColumns(true), proteinSequence);
				model.addRow(glycoSiteInfoList.toArray());
				log.info("Table now with " + model.getRowCount() + " rows");

			}

			log.info(glycoSites.size() + " peptides added to attached window");
		}
		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				scrollableTable.getTable().repaint();
				scrollableTable.initializeSorter();
			}
		});
	}

	private void addColumnsInTable(MyPeptidesTable table, List<String> columnsStringList) {
		final DefaultTableModel defaultModel = (DefaultTableModel) table.getModel();
		log.info("Adding colums " + columnsStringList.size() + " columns");
		if (columnsStringList != null) {

			for (final String columnName : columnsStringList) {
				defaultModel.addColumn(columnName);
			}
			log.info("Added " + table.getColumnCount() + " colums");
			for (int i = 0; i < table.getColumnCount(); i++) {
				final TableColumn column = table.getColumnModel().getColumn(i);
				final ColumnsPeptidesTable[] columHeaders = ColumnsPeptidesTable.values();
				boolean set = false;
				for (final ColumnsPeptidesTable header : columHeaders) {
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

	public void forceVisible() {
		setVisible(true);
		ColumnsPeptidesTableUtil.scrollToBeginning(this.scrollableTable);

	}

	@Override
	public void dispose() {
		log.info("Dialog dispose");
		super.dispose();

	}

	public void setMinimized(boolean b) {
	}

}
