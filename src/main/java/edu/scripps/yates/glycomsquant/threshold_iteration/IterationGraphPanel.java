package edu.scripps.yates.glycomsquant.threshold_iteration;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
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
import org.proteored.pacom.analysis.charts.LineCategoryChart;
import org.proteored.pacom.utils.ComponentEnableStateKeeper;

import edu.scripps.yates.glycomsquant.PTMCode;
import edu.scripps.yates.glycomsquant.gui.GuiUtils;
import gnu.trove.map.hash.THashMap;

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

	public IterationGraphPanel(List<IterationData> iterations) {
		super(true);// doubled buffered (faster)
		this.iterations.addAll(iterations);
		initComponents();
		updateGraph();

	}

	private void initComponents() {
		setLayout(new BorderLayout(0, 0));

		final JPanel leftPanel = new JPanel();
		leftPanel.setPreferredSize(new Dimension(175, 10));
		add(leftPanel, BorderLayout.WEST);
		final GridBagLayout gbl_leftPanel = new GridBagLayout();
		gbl_leftPanel.columnWidths = new int[] { 175 };
		gbl_leftPanel.rowHeights = new int[] { 10, 0, 0 };
		gbl_leftPanel.columnWeights = new double[] { 1.0 };
		gbl_leftPanel.rowWeights = new double[] { 0.0, 1.0, Double.MIN_VALUE };
		leftPanel.setLayout(gbl_leftPanel);

		final JPanel ptmCodesPanel = new JPanel();
		ptmCodesPanel
				.setBorder(new TitledBorder(null, "PTM codes", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		final GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(0, 0, 5, 0);
		c.fill = GridBagConstraints.HORIZONTAL;
		c.anchor = GridBagConstraints.NORTH;
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
		for (final PTMCode ptmCode : PTMCode.values()) {
			final JCheckBox checkBox = new JCheckBox("% of " + ptmCode.getCode(), true);
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

		final JPanel peptidesPanel = new JPanel();
		peptidesPanel.setBorder(new TitledBorder(null, "Peptides", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		final GridBagConstraints gbc_peptidesPanel = new GridBagConstraints();
		gbc_peptidesPanel.anchor = GridBagConstraints.NORTH;
		gbc_peptidesPanel.fill = GridBagConstraints.HORIZONTAL;
		gbc_peptidesPanel.gridx = 0;
		gbc_peptidesPanel.gridy = 1;
		leftPanel.add(peptidesPanel, gbc_peptidesPanel);
		final GridBagLayout gbl_peptidesPanel = new GridBagLayout();
		gbl_peptidesPanel.columnWidths = new int[] { 0 };
		gbl_peptidesPanel.rowHeights = new int[] { 0 };
		gbl_peptidesPanel.columnWeights = new double[] { 0.0 };
		gbl_peptidesPanel.rowWeights = new double[] { 0.0 };
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
		c3.fill = GridBagConstraints.VERTICAL;
		c3.gridx = 0;
		c3.gridy = 0;
		peptidesPanel.add(totalPeptidesCheckBox, c3);
		c3.gridy++;
		// checkboxs for peptides and PTMs
		for (final PTMCode ptmCode : PTMCode.values()) {
			final JCheckBox checkBox = new JCheckBox("peptides with " + ptmCode.getCode());
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

		final JPanel centerPanel = new JPanel();

		add(centerPanel, BorderLayout.CENTER);
		centerPanel.setLayout(new BorderLayout(0, 0));

		scrollPane = new JScrollPane();
		componentStateKeeper.addInvariableComponent(scrollPane);
		centerPanel.add(scrollPane, BorderLayout.CENTER);
	}

	private void addProportionDataToDataset(DefaultCategoryDataset dataset, List<IterationData> iterations) {
		for (final IterationData iterationData : iterations) {
			for (final PTMCode ptmCode : PTMCode.values()) {
				if (checkBoxByPTMCode.get(ptmCode).isSelected()) {
					addPTMCodeProportionToDataset(ptmCode, dataset, iterationData);
				}
			}
		}
	}

	protected void updateGraph() {
		componentStateKeeper.keepEnableStates(this);
		try {
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
			final String xAxisLabel = "log10 intensity threshold";
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

			// second renderer if there is any
			if (plot.getRendererCount() > 1) {
				final CategoryItemRenderer renderer2 = plot.getRenderer(1);
				final StandardCategoryItemLabelGenerator labelGenerator2 = new StandardCategoryItemLabelGenerator("{2}",
						new DecimalFormat("#"));
				renderer2.setDefaultItemLabelGenerator(labelGenerator2);
				renderer2.setDefaultItemLabelFont(itemsFont);
				renderer2.setDefaultItemLabelsVisible(true);
			}
		} finally {
			componentStateKeeper.setToPreviousState(this);
		}
	}

	private void addPeptidesDataToDataset(DefaultCategoryDataset dataset, List<IterationData> iterations) {
		for (final IterationData iterationData : iterations) {
			if (this.totalPeptidesCheckBox.isSelected()) {
				addPeptidesDataToDataset(null, dataset, iterationData);
			}
			for (final PTMCode ptmCode : PTMCode.values()) {
				if (peptidesCheckBoxByPTMCode.get(ptmCode).isSelected()) {
					addPeptidesDataToDataset(ptmCode, dataset, iterationData);
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

	private void addPeptidesDataToDataset(PTMCode ptmCode, DefaultCategoryDataset dataset,
			IterationData iterationData) {
		if (ptmCode != null) {
			final double numPeptides = iterationData.getNumPeptidesByPTMCode(ptmCode);
			final double threshold = iterationData.getIntensityThreshold();
			final String thresholdColumnKey = GuiUtils.formatDouble(threshold);
			dataset.addValue(numPeptides, ptmCode.getCode(), thresholdColumnKey);
		} else {
			final double numPeptides = iterationData.getNumPeptides();
			final double threshold = iterationData.getIntensityThreshold();
			final String thresholdColumnKey = GuiUtils.formatDouble(threshold);
			dataset.addValue(numPeptides, "total peptides", thresholdColumnKey);
		}

	}

	private void addPTMCodeProportionToDataset(PTMCode ptmCode, DefaultCategoryDataset dataset,
			IterationData iterationData) {

		final double averagePercentage = iterationData.getAveragePercentageByPTMCode(ptmCode);
		final double threshold = iterationData.getIntensityThreshold();
		final String thresholdColumnKey = GuiUtils.formatDouble(threshold);
		dataset.addValue(averagePercentage, "peptides with " + ptmCode.getCode(), thresholdColumnKey);

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

}
