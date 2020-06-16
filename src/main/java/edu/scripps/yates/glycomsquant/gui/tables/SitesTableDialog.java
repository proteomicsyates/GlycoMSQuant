package edu.scripps.yates.glycomsquant.gui.tables;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.SystemColor;
import java.util.List;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.apache.log4j.Logger;

import edu.scripps.yates.glycomsquant.GlycoSite;
import edu.scripps.yates.glycomsquant.gui.tables.scrollables.ScrollableTable;
import edu.scripps.yates.glycomsquant.gui.tables.sites.ColumnsSitesTableUtil;
import edu.scripps.yates.glycomsquant.gui.tables.sites.MySitesTable;
import edu.scripps.yates.glycomsquant.util.GuiUtils;

public class SitesTableDialog extends JFrame {
	/**
	 * 
	 */
	private static final long serialVersionUID = -1119185903108814297L;
	private static final Logger log = Logger.getLogger(SitesTableDialog.class);

	private final JScrollPane scrollPane;
	private ScrollableTable<MySitesTable> scrollableTable;

	public SitesTableDialog(String nameForTitle) {
		super();
		setPreferredSize(new Dimension(GuiUtils.getFractionOfScreenWidthSize(4.0 / 5),
				GuiUtils.getFractionOfScreenHeightSize(3.0 / 4)));
		getContentPane().setBackground(SystemColor.info);
		setTitle("GlycoMSQuant sites results table - " + nameForTitle);
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

	public void loadResultTable(List<GlycoSite> glycoSites, boolean sumIntensitiesAcrossReplicates) {

		log.info("Loading result table with " + glycoSites.size() + " glyco sites");

		final JPanel contentPanel = new JPanel();
		scrollPane.setViewportView(contentPanel);

		contentPanel.setLayout(new BorderLayout());

		log.info("adding panels");
		final JPanel northPanel = new JPanel();
		// panel.setMaximumSize(new Dimension(100, Integer.MAX_VALUE));
		northPanel.setLayout(new BorderLayout());

		contentPanel.add(northPanel, BorderLayout.NORTH);
		scrollableTable = new ScrollableTable<MySitesTable>(new MySitesTable());
		contentPanel.add(scrollableTable, BorderLayout.CENTER);

		scrollableTable.getTable().loadResultTable(glycoSites, sumIntensitiesAcrossReplicates);
	}

	public void forceVisible() {
		setVisible(true);
		ColumnsSitesTableUtil.scrollToBeginning(this.scrollPane);

	}

	@Override
	public void dispose() {
		log.info("Dialog dispose");
		super.dispose();

	}

	public void setMinimized(boolean b) {
	}

}
