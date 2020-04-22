package edu.scripps.yates.glycomsquant.threshold_iteration;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Paint;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.TitledBorder;

import org.apache.log4j.Logger;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.axis.AxisLocation;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.labels.StandardCategoryItemLabelGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.CategoryItemRenderer;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.data.category.DefaultCategoryDataset;

import edu.scripps.yates.glycomsquant.GlycoSite;
import edu.scripps.yates.glycomsquant.PTMCode;
import edu.scripps.yates.glycomsquant.gui.ColorsUtil;
import edu.scripps.yates.glycomsquant.gui.GuiUtils;
import edu.scripps.yates.glycomsquant.gui.charts.LineCategoryChart;
import edu.scripps.yates.utilities.swing.ComponentEnableStateKeeper;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.THashMap;
import gnu.trove.map.hash.TIntObjectHashMap;

public class IterationGraphPanel extends JPanel {
	/**
	* 
	*/
	private static final long serialVersionUID = 8304330918891833070L;
	private final Map<PTMCode, JCheckBox> checkBoxByPTMCode = new THashMap<PTMCode, JCheckBox>();
	private final Map<PTMCode, JCheckBox> peptidesCheckBoxByPTMCode = new THashMap<PTMCode, JCheckBox>();
	private JCheckBox totalPeptidesCheckBox;
	private final List<IterationData> iterations = new ArrayList<IterationData>();
	private JScrollPane scrollPane;
	private ChartPanel chartPanel;
	private LineCategoryChart chart;
	private static final Logger log = Logger.getLogger(IterationGraphPanel.class);
	private final ComponentEnableStateKeeper componentStateKeeper = new ComponentEnableStateKeeper(true);
	private final Map<String, PTMCode> ptmCodeByRowKey = new THashMap<String, PTMCode>();
	// static because we want to have as much as possible
	private static final TIntObjectHashMap<GlycoSite> glycoSites = new TIntObjectHashMap<GlycoSite>();
	private final TIntObjectMap<JCheckBox> checkBoxesBySites = new TIntObjectHashMap<JCheckBox>();
	private JCheckBox hideAveragedProportionsCheckBox;
	private JCheckBox hideTotalPeptidesCheckBox;

	public static void clearSites() {
		glycoSites.clear();
	}

	public IterationGraphPanel(List<IterationData> iterations, List<GlycoSite> glycoSites) {
		super(true);// doubled buffered (faster)
		this.iterations.addAll(iterations);
		if (glycoSites != null) {
			for (final GlycoSite glycoSite : glycoSites) {
				if (!IterationGraphPanel.glycoSites.containsKey(glycoSite.getPosition())) {
					IterationGraphPanel.glycoSites.put(glycoSite.getPosition(), glycoSite);
				}
			}
		}
		initComponents();
		updateGraph();

	}

	private void initComponents() {
		setLayout(new BorderLayout(0, 0));

		final JPanel leftPanel = new JPanel();
		leftPanel.setPreferredSize(new Dimension(250, 10));
		add(leftPanel, BorderLayout.WEST);
		final GridBagLayout gbl_leftPanel = new GridBagLayout();
		gbl_leftPanel.columnWidths = new int[] { 250 };
		gbl_leftPanel.rowHeights = new int[] { 10, 0, 0, 0 };
		gbl_leftPanel.columnWeights = new double[] { 1.0 };
		gbl_leftPanel.rowWeights = new double[] { 0.0, 1.0, 1.0, Double.MIN_VALUE };
		leftPanel.setLayout(gbl_leftPanel);

		final JPanel ptmCodesPanel = new JPanel();
		ptmCodesPanel.setBorder(
				new TitledBorder(null, "Average proportions", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		final GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(0, 5, 0, 5);
		c.fill = GridBagConstraints.HORIZONTAL;
		c.anchor = GridBagConstraints.WEST;
		c.gridx = 0;
		c.gridy = 0;
		leftPanel.add(ptmCodesPanel, c);
		final GridBagLayout gbl_ptmCodesPanel = new GridBagLayout();
		gbl_ptmCodesPanel.columnWidths = new int[] { 0 };
		gbl_ptmCodesPanel.rowHeights = new int[] { 0 };
		gbl_ptmCodesPanel.columnWeights = new double[] { Double.MIN_VALUE };
		gbl_ptmCodesPanel.rowWeights = new double[] { Double.MIN_VALUE };
		ptmCodesPanel.setLayout(gbl_ptmCodesPanel);
		// checkboxs for PTMs
		final GridBagConstraints c2 = new GridBagConstraints();
		c2.gridx = 0;
		c2.gridy = 0;
		c2.insets = new Insets(0, 10, 0, 0);
		c2.anchor = GridBagConstraints.WEST;
		for (final PTMCode ptmCode : PTMCode.values()) {
			final JCheckBox checkBox = new JCheckBox("% of " + GuiUtils.translateCode(ptmCode.getCode()), true);
			this.checkBoxByPTMCode.put(ptmCode, checkBox);
			ptmCodesPanel.add(checkBox, c2);
			checkBox.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					updateGraph();
				}
			});
			c2.gridy++;
		}
		hideAveragedProportionsCheckBox = new JCheckBox("Hide averaged proportions", false);
		hideAveragedProportionsCheckBox
				.setToolTipText("<html>If selected, the averaged proportions will be hidden from the chart<br>"
						+ " and only the site-specific proportions could be seen if selected below</html>");
		ptmCodesPanel.add(hideAveragedProportionsCheckBox, c2);
		hideAveragedProportionsCheckBox.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				updateGraph();
			}
		});
		//
		final JPanel peptidesPanel = new JPanel();
		peptidesPanel.setBorder(new TitledBorder(null, "Peptides", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		final GridBagConstraints gbc_peptidesPanel = new GridBagConstraints();
		gbc_peptidesPanel.fill = GridBagConstraints.HORIZONTAL;
		gbc_peptidesPanel.insets = new Insets(0, 5, 5, 5);
		gbc_peptidesPanel.gridx = 0;
		gbc_peptidesPanel.gridy = 1;
		leftPanel.add(peptidesPanel, gbc_peptidesPanel);
		final GridBagLayout gbl_peptidesPanel = new GridBagLayout();
		gbl_peptidesPanel.columnWidths = new int[] { 0 };
		gbl_peptidesPanel.rowHeights = new int[] { 0 };
		gbl_peptidesPanel.columnWeights = new double[] { Double.MIN_VALUE };
		gbl_peptidesPanel.rowWeights = new double[] { Double.MIN_VALUE };
		peptidesPanel.setLayout(gbl_peptidesPanel);

		totalPeptidesCheckBox = new JCheckBox("Total peptides");
		totalPeptidesCheckBox.setSelected(true);
		totalPeptidesCheckBox.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				updateGraph();
			}
		});
		totalPeptidesCheckBox.setToolTipText("Total number of peptides in each iteration");

		final GridBagConstraints c3 = new GridBagConstraints();
		c3.anchor = GridBagConstraints.WEST;
		c3.insets = new Insets(0, 10, 0, 0);
		c3.gridx = 0;
		c3.gridy = 0;
		peptidesPanel.add(totalPeptidesCheckBox, c3);

		final JPanel sitesPanel = new JPanel();
		final JScrollPane scroll = new JScrollPane(sitesPanel);

		sitesPanel.setBorder(new TitledBorder(null, "Sites", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		final GridBagConstraints gbc_sitesPanel = new GridBagConstraints();
		gbc_sitesPanel.insets = new Insets(0, 5, 0, 5);
		gbc_sitesPanel.weighty = 100.0;
		gbc_sitesPanel.anchor = GridBagConstraints.WEST;
		gbc_sitesPanel.fill = GridBagConstraints.BOTH;
		gbc_sitesPanel.gridx = 0;
		gbc_sitesPanel.gridy = 2;
		leftPanel.add(scroll, gbc_sitesPanel);
		final GridBagLayout gbl_sitesPanel = new GridBagLayout();
		gbl_sitesPanel.columnWidths = new int[] { 0 };
		gbl_sitesPanel.rowHeights = new int[] { 0 };
		gbl_sitesPanel.columnWeights = new double[] { Double.MIN_VALUE };
		gbl_sitesPanel.rowWeights = new double[] { Double.MIN_VALUE };
		sitesPanel.setLayout(gbl_sitesPanel);
		c3.gridy++;
		// checkboxs for peptides and PTMs
		for (final PTMCode ptmCode : PTMCode.values()) {
			final JCheckBox checkBox = new JCheckBox("peptides with " + GuiUtils.translateCode(ptmCode.getCode()));
			checkBox.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					updateGraph();
				}
			});
			this.peptidesCheckBoxByPTMCode.put(ptmCode, checkBox);
			peptidesPanel.add(checkBox, c3);
			c3.gridy++;
		}
		hideTotalPeptidesCheckBox = new JCheckBox("Hide total peptides", false);
		hideTotalPeptidesCheckBox
				.setToolTipText("<html>If selected, the total number of peptides will be hidden from the chart<br>"
						+ "and only the site-specific numbers could be seen if selected below</html>");
		hideTotalPeptidesCheckBox.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				updateGraph();
			}
		});
		peptidesPanel.add(hideTotalPeptidesCheckBox, c3);

		// now we add the checkboxes of the sites
		final GridBagConstraints c4 = new GridBagConstraints();
		c4.anchor = GridBagConstraints.WEST;
		c4.fill = GridBagConstraints.VERTICAL;
		c4.gridx = 0;
		c4.gridy = 0;
		c4.insets = new Insets(0, 10, 0, 10);
		if (glycoSites != null) {
			final TIntList positions = getSortedGlycoSitesPositions();
			for (final int position : positions.toArray()) {
				final JCheckBox checkBox = new JCheckBox("Site " + position, false);
				checkBox.addActionListener(new ActionListener() {

					@Override
					public void actionPerformed(ActionEvent e) {
						updateGraph();

					}
				});
				checkBoxesBySites.put(position, checkBox);
				sitesPanel.add(checkBox, c4);

				if (c4.gridx == 0) {
					c4.gridx = 1;
				} else {
					c4.gridx = 0;
					c4.gridy++;
				}
			}
		}
		final JPanel centerPanel = new JPanel();

		add(centerPanel, BorderLayout.CENTER);
		centerPanel.setLayout(new BorderLayout(0, 0));

		scrollPane = new JScrollPane();
		componentStateKeeper.addInvariableComponent(scrollPane);
		centerPanel.add(scrollPane, BorderLayout.CENTER);
	}

	private TIntList getSortedGlycoSitesPositions() {
		final TIntList positions = new TIntArrayList(glycoSites.keys());
		positions.sort();
		return positions;
	}

	private void addProportionDataToDataset(DefaultCategoryDataset dataset, List<IterationData> iterations) {
		for (final IterationData iterationData : iterations) {
			if (!hideAveragedProportionsCheckBox.isSelected()) {
				for (final PTMCode ptmCode : PTMCode.values()) {
					if (checkBoxByPTMCode.get(ptmCode).isSelected()) {
						addPTMCodeProportionToDataset(ptmCode, dataset, iterationData);
					}
				}
			}
			// now the proportions by sites

			for (final int position : this.getSortedGlycoSitesPositions().toArray()) {
				final GlycoSite site = glycoSites.get(position);
				if (isSiteSelected(site)) {
					for (final PTMCode ptmCode : PTMCode.values()) {
						if (checkBoxByPTMCode.get(ptmCode).isSelected()) {
							addSitePTMCodeProportionToDataset(site, ptmCode, dataset, iterationData);
						}
					}
				}
			}

		}
	}

	protected void updateGraph() {
		componentStateKeeper.keepEnableStates(this);
		try {
			ptmCodeByRowKey.clear();
			// ptmcodes
			final DefaultCategoryDataset dataset = new DefaultCategoryDataset();
			addProportionDataToDataset(dataset, iterations);

			final boolean totalPeptides = this.totalPeptidesCheckBox.isSelected();
			boolean somePTMCodePeptides = false;
			for (final PTMCode ptmCode : PTMCode.values()) {
				if (peptidesCheckBoxByPTMCode.get(ptmCode).isSelected()) {
					somePTMCodePeptides = true;
				}
			}
			final String xAxisLabel = "Peak area threshold";
			final String yAxisLabel = "PTM %";
			final String title = "PTM %";
			DefaultCategoryDataset dataset2 = null;
			if (totalPeptides || somePTMCodePeptides) {
				dataset2 = new DefaultCategoryDataset();
				addPeptidesDataToDataset(dataset2, iterations);
			}
			final String subtitle = "";
			chart = new LineCategoryChart(title, subtitle, xAxisLabel, yAxisLabel, dataset, PlotOrientation.VERTICAL);
			chartPanel = chart.getChartPanel();
			scrollPane.setViewportView(chartPanel);
			if (dataset2 != null) {
				addSecondAxis(dataset2, "# peptides");
			}

			// x labels with 1 decimal
			final CategoryPlot plot = (CategoryPlot) chart.getChart().getPlot();
			final CategoryItemRenderer renderer = plot.getRenderer();
			final StandardCategoryItemLabelGenerator labelGenerator = new StandardCategoryItemLabelGenerator("{2}",
					new DecimalFormat("#.##"));
			renderer.setDefaultItemLabelGenerator(labelGenerator);
			final Font itemsFont = new java.awt.Font("arial", Font.PLAIN, 8);
			renderer.setDefaultItemLabelFont(itemsFont);
			renderer.setDefaultItemLabelsVisible(true);
			final Font tickLabelsFont = new java.awt.Font("arial", Font.PLAIN, 10);
			plot.getDomainAxis().setTickLabelFont(tickLabelsFont);

			plot.getRangeAxis(0).setMinorTickCount(10);
			final NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
			rangeAxis.setRange(0.0, 1.0);
			rangeAxis.setTickUnit(new NumberTickUnit(0.1, new DecimalFormat("#.#"), 0));
			rangeAxis.setMinorTickMarksVisible(true);
			rangeAxis.setMinorTickCount(2);

			for (int row = 0; row < dataset.getRowCount(); row++) {
				final String rowKey = (String) dataset.getRowKey(row);
				final PTMCode ptmCode = ptmCodeByRowKey.get(rowKey);
				final Paint paint = ColorsUtil.getDefaultColorByPTMCode(ptmCode);
				renderer.setSeriesPaint(row, paint);
			}

			// second renderer if there is any
			if (plot.getRendererCount() > 1) {

				final CategoryItemRenderer renderer2 = plot.getRenderer(1);
				final StandardCategoryItemLabelGenerator labelGenerator2 = new StandardCategoryItemLabelGenerator("{2}",
						new DecimalFormat("#"));
				renderer2.setDefaultItemLabelGenerator(labelGenerator2);
				renderer2.setDefaultItemLabelFont(itemsFont);
				renderer2.setDefaultItemLabelsVisible(true);

				for (int row = 0; row < dataset2.getRowCount(); row++) {
					final String rowKey = (String) dataset2.getRowKey(row);
					final PTMCode ptmCode = ptmCodeByRowKey.get(rowKey);
					final Paint paint = ColorsUtil.getDefaultColorByPTMCode(ptmCode);
					renderer2.setSeriesPaint(row, paint);
				}
			}

		} finally {
			componentStateKeeper.setToPreviousState(this);
		}
	}

	private void addPeptidesDataToDataset(DefaultCategoryDataset dataset, List<IterationData> iterations) {
		for (final IterationData iterationData : iterations) {
			if (!hideTotalPeptidesCheckBox.isSelected()) {
				if (this.totalPeptidesCheckBox.isSelected()) {
					addPeptidesDataToDataset(null, dataset, iterationData);
				}
				for (final PTMCode ptmCode : PTMCode.values()) {
					if (peptidesCheckBoxByPTMCode.get(ptmCode).isSelected()) {
						addPeptidesDataToDataset(ptmCode, dataset, iterationData);
					}
				}
			}
			// now the peptide numbers by sites
			for (final int position : this.getSortedGlycoSitesPositions().toArray()) {
				final GlycoSite site = glycoSites.get(position);
				if (isSiteSelected(site)) {
					if (this.totalPeptidesCheckBox.isSelected()) {
						addSitePeptidesDataToDataset(site, null, dataset, iterationData);
					}
					for (final PTMCode ptmCode : PTMCode.values()) {
						if (peptidesCheckBoxByPTMCode.get(ptmCode).isSelected()) {
							addSitePeptidesDataToDataset(site, ptmCode, dataset, iterationData);
						}
					}

				}
			}
		}
	}

	public void addSecondAxis(DefaultCategoryDataset dataset, String axisName) {
		if (chart != null) {
			final NumberAxis newAxis = new NumberAxis(axisName);
			newAxis.setAutoRange(true);

			final CategoryPlot plot = (CategoryPlot) this.chart.getChart().getPlot();

			plot.setRangeAxis(1, newAxis);
			plot.setRangeAxisLocation(1, AxisLocation.BOTTOM_OR_RIGHT);
			plot.setDataset(1, dataset);
			plot.mapDatasetToRangeAxis(1, 1);

			final LineAndShapeRenderer newRenderer = new LineAndShapeRenderer(true, true);
			plot.setRenderer(1, newRenderer);

		}
	}

	private void addSitePeptidesDataToDataset(GlycoSite site, PTMCode ptmCode, DefaultCategoryDataset dataset,
			IterationData iterationData) {
		if (ptmCode != null) {
			final double numPeptides = iterationData.getNumPeptidesBySiteAndPTMCode(site.getPosition(), ptmCode);
			final double threshold = iterationData.getIntensityThreshold();
			final String thresholdColumnKey = GuiUtils.formatDouble(threshold);
			final String rowKey = "# peptides with " + GuiUtils.translateCode(ptmCode.getCode()) + " in site "
					+ site.getPosition();
			dataset.addValue(numPeptides, rowKey, thresholdColumnKey);
			ptmCodeByRowKey.put(rowKey, ptmCode);
		} else {
			final double numPeptides = iterationData.getNumPeptidesBySite(site.getPosition());
			final double threshold = iterationData.getIntensityThreshold();
			final String thresholdColumnKey = GuiUtils.formatDouble(threshold);
			final String rowKey = "total peptides in site " + site.getPosition();
			dataset.addValue(numPeptides, rowKey, thresholdColumnKey);
			ptmCodeByRowKey.put(rowKey, ptmCode);
		}

	}

	private void addPeptidesDataToDataset(PTMCode ptmCode, DefaultCategoryDataset dataset,
			IterationData iterationData) {
		if (ptmCode != null) {
			final double numPeptides = iterationData.getNumPeptidesByPTMCode(ptmCode);
			final double threshold = iterationData.getIntensityThreshold();
			final String thresholdColumnKey = GuiUtils.formatDouble(threshold);
			final String rowKey = "# peptides with " + GuiUtils.translateCode(ptmCode.getCode());
			dataset.addValue(numPeptides, rowKey, thresholdColumnKey);
			ptmCodeByRowKey.put(rowKey, ptmCode);
		} else {
			final double numPeptides = iterationData.getNumPeptides();
			final double threshold = iterationData.getIntensityThreshold();
			final String thresholdColumnKey = GuiUtils.formatDouble(threshold);
			final String rowKey = "total peptides";
			dataset.addValue(numPeptides, rowKey, thresholdColumnKey);
			ptmCodeByRowKey.put(rowKey, ptmCode);
		}

	}

	private void addSitePTMCodeProportionToDataset(GlycoSite site, PTMCode ptmCode, DefaultCategoryDataset dataset,
			IterationData iterationData) {

		final double averagePercentage = iterationData.getPercentageBySiteAndPTMCode(site.getPosition(), ptmCode);
		final double threshold = iterationData.getIntensityThreshold();
		final String thresholdColumnKey = GuiUtils.formatDouble(threshold);
		final String rowKey = "% of " + GuiUtils.translateCode(ptmCode.getCode()) + " in site " + site.getPosition();
		dataset.addValue(averagePercentage, rowKey, thresholdColumnKey);
		ptmCodeByRowKey.put(rowKey, ptmCode);

	}

	private void addPTMCodeProportionToDataset(PTMCode ptmCode, DefaultCategoryDataset dataset,
			IterationData iterationData) {

		final double averagePercentage = iterationData.getAveragePercentageByPTMCode(ptmCode);
		final double threshold = iterationData.getIntensityThreshold();
		final String thresholdColumnKey = GuiUtils.formatDouble(threshold);
		final String rowKey = "% of " + GuiUtils.translateCode(ptmCode.getCode());
		dataset.addValue(averagePercentage, rowKey, thresholdColumnKey);
		ptmCodeByRowKey.put(rowKey, ptmCode);

	}

	@Override
	public void updateUI() {
		if (chartPanel != null) {
			try {
				this.chartPanel.updateUI();
				log.info("all is ok");
			} catch (final Exception e) {
				e.printStackTrace();
			}
		}
		super.updateUI();
	}

	private boolean isSiteSelected(GlycoSite site) {
		if (this.checkBoxesBySites.containsKey(site.getPosition())) {
			final JCheckBox checkBox = this.checkBoxesBySites.get(site.getPosition());
			return checkBox.isSelected();
		}
		return false;
	}
}
