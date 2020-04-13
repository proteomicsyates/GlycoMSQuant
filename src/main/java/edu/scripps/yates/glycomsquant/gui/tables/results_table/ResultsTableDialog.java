package edu.scripps.yates.glycomsquant.gui.tables.results_table;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.SystemColor;
import java.util.List;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;

import org.apache.log4j.Logger;

import edu.scripps.yates.glycomsquant.GlycoSite;
import edu.scripps.yates.glycomsquant.gui.GuiUtils;

public class ResultsTableDialog extends JFrame {
	/**
	 * 
	 */
	private static final long serialVersionUID = -1119185903108814297L;
	private static final Logger log = Logger.getLogger(ResultsTableDialog.class);

	private final JScrollPane scrollPane;

	public ResultsTableDialog() {
		super();
		setPreferredSize(new Dimension(GuiUtils.getFractionOfScreenWidthSize(4.0 / 5),
				GuiUtils.getFractionOfScreenHeightSize(3.0 / 4)));
		getContentPane().setBackground(SystemColor.info);
		setTitle("GlycoMSQuant results table");
//		setFocusableWindowState(false);
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
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

		pack();
		final java.awt.Dimension screenSize = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
		final java.awt.Dimension dialogSize = getSize();
		setLocation((screenSize.width - dialogSize.width) / 2, (screenSize.height - dialogSize.height) / 2);
	}

	public void loadResultTable(List<GlycoSite> glycoSites, boolean calculateProportionsByPeptidesFirst) {

		log.info("Loading result table with " + glycoSites.size() + " glyco sites");

		final JPanel contentPanel = new JPanel();
		scrollPane.setViewportView(contentPanel);

		contentPanel.setLayout(new BorderLayout());

		log.info("adding panels");
		final JPanel northPanel = new JPanel();
		// panel.setMaximumSize(new Dimension(100, Integer.MAX_VALUE));
		northPanel.setLayout(new BorderLayout());

		contentPanel.add(northPanel, BorderLayout.NORTH);
		final ScrollableResultsTable table = new ScrollableResultsTable(200, calculateProportionsByPeptidesFirst);
		contentPanel.add(table, BorderLayout.CENTER);
		table.getTable().clearData();

		addColumnsInTable(table.getTable(),
				ColumnsResultsTable.getColumnsStringForTable(calculateProportionsByPeptidesFirst));

		if (glycoSites != null) {
			for (int i = 0; i < glycoSites.size(); i++) {
				final GlycoSite glycoSite = glycoSites.get(i);
				final MyResultsTableModel model = (MyResultsTableModel) table.getTable().getModel();

				final List<Object> glycoSiteInfoList = ColumnsResultsTableUtil.getInstance().getGlycoSiteInfoList(
						glycoSite, calculateProportionsByPeptidesFirst,
						ColumnsResultsTable.getColumns(calculateProportionsByPeptidesFirst), i + 1);
				model.addRow(glycoSiteInfoList.toArray());
				log.info("Table now with " + model.getRowCount() + " rows");
			}

			log.info(glycoSites.size() + " glycoSites added to attached window");
		}
		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				table.getTable().repaint();
				table.initializeSorter();
			}
		});
	}

	private void addColumnsInTable(MyResultsTable table, List<String> columnsStringList) {
		final DefaultTableModel defaultModel = (DefaultTableModel) table.getModel();
		log.info("Adding colums " + columnsStringList.size() + " columns");
		if (columnsStringList != null) {

			for (final String columnName : columnsStringList) {
				defaultModel.addColumn(columnName);
			}
			log.info("Added " + table.getColumnCount() + " colums");
			for (int i = 0; i < table.getColumnCount(); i++) {
				final TableColumn column = table.getColumnModel().getColumn(i);
				final ColumnsResultsTable[] columHeaders = ColumnsResultsTable.values();
				for (final ColumnsResultsTable header : columHeaders) {
					if (column.getHeaderValue().equals(header.getName()))
						column.setPreferredWidth(header.getDefaultWidth());
//					column.setMaxWidth(header.getDefaultWidth());
					column.setMinWidth(header.getDefaultWidth());
				}
				column.setResizable(true);
			}
		}
	}

	public void forceVisible() {
		setVisible(true);
		ColumnsResultsTableUtil.scrollToBeginning(this.scrollPane);

	}

	@Override
	public void dispose() {
		log.info("Dialog dispose");
		super.dispose();

	}

	public void setMinimized(boolean b) {
	}

}
