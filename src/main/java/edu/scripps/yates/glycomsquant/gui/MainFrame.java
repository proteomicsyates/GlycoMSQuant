package edu.scripps.yates.glycomsquant.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.SystemColor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileFilter;

import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;
import org.jfree.chart.ChartPanel;

import edu.scripps.yates.census.read.model.interfaces.QuantifiedPeptideInterface;
import edu.scripps.yates.glycomsquant.AppDefaults;
import edu.scripps.yates.glycomsquant.CurrentInputParameters;
import edu.scripps.yates.glycomsquant.GlycoPTMAnalyzer;
import edu.scripps.yates.glycomsquant.GlycoPTMPeptideAnalyzer;
import edu.scripps.yates.glycomsquant.GlycoPTMPeptideAnalyzer.PeptidesRemovedBecauseOfConsecutiveSitesWithPTM;
import edu.scripps.yates.glycomsquant.GlycoPTMResultGenerator;
import edu.scripps.yates.glycomsquant.GlycoSite;
import edu.scripps.yates.glycomsquant.InputDataReader;
import edu.scripps.yates.glycomsquant.InputParameters;
import edu.scripps.yates.glycomsquant.ProteinSequences;
import edu.scripps.yates.glycomsquant.comparison.GlycoPTMRunComparator;
import edu.scripps.yates.glycomsquant.comparison.RunComparisonResult;
import edu.scripps.yates.glycomsquant.comparison.RunComparisonTest;
import edu.scripps.yates.glycomsquant.gui.attached_frame.AbstractJFrameWithAttachedHelpAndAttachedRunsDialog;
import edu.scripps.yates.glycomsquant.gui.files.FileManager;
import edu.scripps.yates.glycomsquant.gui.files.ResultsProperties;
import edu.scripps.yates.glycomsquant.gui.tables.PeptidesTableDialog;
import edu.scripps.yates.glycomsquant.gui.tables.SitesTableDialog;
import edu.scripps.yates.glycomsquant.gui.tables.comparison.ComparisonTableDialog;
import edu.scripps.yates.glycomsquant.gui.tasks.IterationGraphGenerator;
import edu.scripps.yates.glycomsquant.gui.tasks.ResultLoaderFromDisk;
import edu.scripps.yates.glycomsquant.threshold_iteration.IterationGraphPanel;
import edu.scripps.yates.glycomsquant.threshold_iteration.IterativeThresholdAnalysis;
import edu.scripps.yates.glycomsquant.util.GuiUtils;
import edu.scripps.yates.glycomsquant.util.ResultsLoadedFromDisk;
import edu.scripps.yates.utilities.appversion.AppVersion;
import edu.scripps.yates.utilities.properties.PropertiesUtil;
import edu.scripps.yates.utilities.proteomicsmodel.enums.AmountType;
import edu.scripps.yates.utilities.swing.ComponentEnableStateKeeper;
import uk.ac.ebi.pride.utilities.pridemod.ModReader;

public class MainFrame extends AbstractJFrameWithAttachedHelpAndAttachedRunsDialog
		implements PropertyChangeListener, InputParameters {
	private final static Logger log = Logger.getLogger(MainFrame.class);
	private JFileChooser fileChooserFASTA;
	private JTextArea statusTextArea;
	private JFileChooser fileChooserInputFile;
	private JPanel chartPanel;
	private final static SimpleDateFormat format = new SimpleDateFormat("yyy-MM-dd HH:mm:ss:SSS");
	private static MainFrame instance;
	private static AppVersion version;
	private static final String APP_PROPERTIES = "app.properties";
	private final ComponentEnableStateKeeper componentStateKeeper = new ComponentEnableStateKeeper(true);
	private JButton separateChartsButton;
	private final List<JComponent> chartsInMainPanel = new ArrayList<JComponent>();
	private final List<JFrame> popupCharts = new ArrayList<JFrame>();
	private List<GlycoSite> currentGlycoSites;
	private JButton btnShowResultsTable;
	private JCheckBox iterativeThresholdAnalysisCheckBox;
	private JCheckBox intensityThresholdCheckBox;
	private JButton btnCloseCharts;
	private ProteinSequenceDialog proteinSequenceDialog;
	private JButton btnShowPeptidesTable;
	private boolean isSumIntensitiesAcrossReplicatesFromLoadedResults;
	private JCheckBox discardWrongPositionedPTMsCheckBox;
	private JCheckBoxMenuItem discardNonUniquePeptidesMenuItem;
	private JCheckBoxMenuItem dontAllowConsecutiveMotifsMenuItem;
	// text for separate charts button
	private final static String POPUP_CHARTS = "Pop-up charts";

	public static MainFrame getInstance() {
		if (instance == null) {
			instance = new MainFrame();
		}
		return instance;
	}

	private MainFrame() {
		super(500); // size of the attached dialog
		setMaximumSize(GuiUtils.getScreenDimension());
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setTitle("GlycoMSQuant");
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (UnsupportedLookAndFeelException | ClassNotFoundException | InstantiationException
				| IllegalAccessException e) {
			e.printStackTrace();
		}

		String proteinOfInterest = GlycoPTMAnalyzer.DEFAULT_PROTEIN_OF_INTEREST;
		if (AppDefaults.getInstance().getProteinOfInterest() != null && !GlycoPTMAnalyzer.DEFAULT_PROTEIN_OF_INTEREST
				.equals(AppDefaults.getInstance().getProteinOfInterest())) {
			proteinOfInterest = AppDefaults.getInstance().getProteinOfInterest();

		}

		initComponents(proteinOfInterest);
		loadResources();
	}

	private void loadResources() {
		showMessage("Loading resources...");
		final Thread thread = new Thread(new Runnable() {

			@Override
			public void run() {

				componentStateKeeper.keepEnableStates(MainFrame.this);
				try {
					componentStateKeeper.disable(MainFrame.this);
					// title with app version
					try {
						version = getVersion();
						if (version != null) {
							final String suffix = " (v" + version.toString() + ")";
							if (!getTitle().endsWith(suffix))
								setTitle(getTitle() + suffix);
						}
					} catch (final Exception e1) {
					}
					// mode reader
					ModReader.getInstance();
					final String motifRegexp = getMotifRegexp();
					final File fastaFile = getFastaFile();
					ProteinSequences.getInstance(fastaFile, motifRegexp).getProteinSequence(getProteinOfInterestACC());
					showMessage("Resources loaded.");
				} catch (final Exception e) {
					e.printStackTrace();
					showError(e.getMessage());
				} finally {
					componentStateKeeper.setToPreviousState(MainFrame.this);
				}
			}

		});
		thread.start();

	}

	private void initComponents(String proteinOfInterest) {
		final JPanel menusPanel = new JPanel();
		getContentPane().add(menusPanel, BorderLayout.NORTH);
		final GridBagLayout gbl_menusPanel = new GridBagLayout();
		gbl_menusPanel.columnWidths = new int[] { 0, 0, 0 };
		gbl_menusPanel.rowHeights = new int[] { 0, 0 };
		gbl_menusPanel.columnWeights = new double[] { 1.0, 0.0, 0.0 };
		gbl_menusPanel.rowWeights = new double[] { 0.0, 0.0 };
		menusPanel.setLayout(gbl_menusPanel);

		final GridBagLayout gbl_analysisParametersPanel = new GridBagLayout();
		gbl_analysisParametersPanel.columnWeights = new double[] { 1.0 };
		final JPanel analysisParametersPanel = new JPanel(gbl_analysisParametersPanel);
		final GridBagConstraints gbc_analysisParametersPanel = new GridBagConstraints();
		gbc_analysisParametersPanel.anchor = GridBagConstraints.NORTH;
		gbc_analysisParametersPanel.gridheight = 2;
		gbc_analysisParametersPanel.insets = new Insets(0, 0, 0, 5);
		gbc_analysisParametersPanel.gridx = 0;
		gbc_analysisParametersPanel.gridy = 0;
		menusPanel.add(analysisParametersPanel, gbc_analysisParametersPanel);
		analysisParametersPanel.setBorder(
				new TitledBorder(null, "Analysis parameters", TitledBorder.LEADING, TitledBorder.TOP, null, null));

		final JPanel normalizeIntensityPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		final GridBagConstraints c3 = new GridBagConstraints();
		c3.insets = new Insets(0, 0, 5, 0);
		c3.fill = GridBagConstraints.BOTH;
		c3.gridx = 0;
		c3.gridy = 2;
		analysisParametersPanel.add(normalizeIntensityPanel, c3);

		normalizeIntensityCheckBox = new JCheckBox("Normalize replicates");
		normalizeIntensityCheckBox.setToolTipText(
				"Click to enable or disable the application of the normalization of the intensities in the input data file by replicates.");
		normalizeIntensityPanel.add(normalizeIntensityCheckBox);

		final JPanel iterativeAnalysisPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		final GridBagConstraints c2 = new GridBagConstraints();
		c2.insets = new Insets(0, 0, 5, 0);
		c2.fill = GridBagConstraints.BOTH;
		c2.gridx = 0;
		c2.gridy = 3;
		analysisParametersPanel.add(iterativeAnalysisPanel, c2);

		iterativeThresholdAnalysisCheckBox = new JCheckBox("Iterative Threshold Analysis");
		iterativeThresholdAnalysisCheckBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				iterativeThresholdAnalysisClicked(iterativeThresholdAnalysisCheckBox.isSelected());
			}
		});
		iterativeThresholdAnalysisCheckBox.setToolTipText(
				"<html>If this option is activated, different intensity thresholds will be calculated <b>iterativelly</b>, and the averaged proportion of each PTM across all glyco-sites will be shown in a graph vs the number of peptides that pass the threshold.<br>\r\nThis may help to decide the optimal threshold.</html>");
		iterativeAnalysisPanel.add(iterativeThresholdAnalysisCheckBox);

		intensityThresholdIntervalLabel = new JLabel("Factor:");
		intensityThresholdIntervalLabel.setEnabled(false);
		iterativeAnalysisPanel.add(intensityThresholdIntervalLabel);
		intensityThresholdIntervalLabel.setToolTipText(
				"If the iterative threshold analysis is activated, this parameter will determine the factor by which the intensity threshold will be multiplied in every iteration.");

		intensityThresholdIntervalTextField = new JTextField("10");
		intensityThresholdIntervalTextField.setEnabled(false);
		intensityThresholdIntervalTextField.setToolTipText(
				"If the iterative threshold analysis is activated, this parameter will determine the factor by which the intensity threshold will be multiplied in every iteration.");
		iterativeAnalysisPanel.add(intensityThresholdIntervalTextField);
		intensityThresholdIntervalTextField.setColumns(5);

		final JPanel sumIntensitiesAcrossReplicatesPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		final GridBagConstraints c4 = new GridBagConstraints();
		c4.insets = new Insets(0, 0, 5, 0);
		c4.fill = GridBagConstraints.BOTH;
		c4.gridx = 0;
		c4.gridy = 0;

		analysisParametersPanel.add(sumIntensitiesAcrossReplicatesPanel, c4);

		sumIntensitiesAcrossReplicatesCheckBox = new JCheckBox("Sum intensities across replicates");
		sumIntensitiesAcrossReplicatesCheckBox.setToolTipText(
				"<html>If selected, for each peptide(+charge), the intensities are sum acrosss replicates before calculating the proportions.<br>If not selected, the proportions of each peptide(+charge) will be calculated in each replicate and then averaged among all the proportions covering a site.</html>");
		sumIntensitiesAcrossReplicatesCheckBox.setSelected(true);
		sumIntensitiesAcrossReplicatesPanel.add(sumIntensitiesAcrossReplicatesCheckBox);

		final JPanel discardWrongPositionedPTMsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		final GridBagConstraints c5 = new GridBagConstraints();
		c5.insets = new Insets(0, 0, 5, 0);
		c5.fill = GridBagConstraints.BOTH;
		c5.gridx = 0;
		c5.gridy = 1;
		analysisParametersPanel.add(discardWrongPositionedPTMsPanel, c5);

		discardWrongPositionedPTMsCheckBox = new JCheckBox("Discard peptides with PTMs in non-valid motifs");
		discardWrongPositionedPTMsCheckBox.setToolTipText(
				"If selected, peptides having PTMs of interest that are not in valid motifs are discarded regardless of having other positions with PTMs in valid motifs.");
		discardWrongPositionedPTMsCheckBox.setSelected(true);
		discardWrongPositionedPTMsPanel.add(discardWrongPositionedPTMsCheckBox);

		final JPanel intensityThresholdPanel = new JPanel();
		final GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(0, 0, 5, 0);
		c.fill = GridBagConstraints.BOTH;
		c.anchor = GridBagConstraints.NORTH;
		c.gridx = 0;
		c.gridy = 4;
		analysisParametersPanel.add(intensityThresholdPanel, c);
		final GridBagLayout gbl_intensityThresholdPanel = new GridBagLayout();
		gbl_intensityThresholdPanel.columnWidths = new int[] { 125, 86, 0 };
		gbl_intensityThresholdPanel.rowHeights = new int[] { 23, 0 };
		gbl_intensityThresholdPanel.columnWeights = new double[] { 0.0, 0.0, Double.MIN_VALUE };
		gbl_intensityThresholdPanel.rowWeights = new double[] { 0.0, Double.MIN_VALUE };
		intensityThresholdPanel.setLayout(gbl_intensityThresholdPanel);
		intensityThresholdCheckBox = new JCheckBox("Peak area threshold:");
		final GridBagConstraints gbc_intensityThresholdCheckBox = new GridBagConstraints();
		gbc_intensityThresholdCheckBox.anchor = GridBagConstraints.NORTHWEST;
		gbc_intensityThresholdCheckBox.insets = new Insets(5, 5, 5, 5);
		gbc_intensityThresholdCheckBox.gridx = 0;
		gbc_intensityThresholdCheckBox.gridy = 0;
		intensityThresholdPanel.add(intensityThresholdCheckBox, gbc_intensityThresholdCheckBox);
		intensityThresholdCheckBox.setToolTipText(
				"Click to enable or disable the application of the peak area threshold over the peak areas in the input data file.");
		intensityThresholdCheckBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				intensityThresholdText.setEnabled(intensityThresholdCheckBox.isSelected());
				if (!intensityThresholdCheckBox.isSelected()) {
					intensityThresholdText.setText(null);
				}
			}
		});

		intensityThresholdText = new JTextField();
		final GridBagConstraints gbc_intensityThresholdText = new GridBagConstraints();
		gbc_intensityThresholdText.fill = GridBagConstraints.HORIZONTAL;
		gbc_intensityThresholdText.anchor = GridBagConstraints.WEST;
		gbc_intensityThresholdText.gridx = 1;
		gbc_intensityThresholdText.gridy = 0;
		intensityThresholdPanel.add(intensityThresholdText, gbc_intensityThresholdText);
		intensityThresholdText.setEnabled(false);
		intensityThresholdText.setToolTipText(
				"If enabled and > 0.0, an intensity threshold will be applied over the intensities in the input data file. Any peptide with an intensity below this value, will be discarded.");
		intensityThresholdText.setColumns(10);

		final JPanel inputPanel = new JPanel();
		inputPanel.setBorder(new TitledBorder(null, "Input", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		final GridBagConstraints gbc_inputPanel = new GridBagConstraints();
		gbc_inputPanel.gridwidth = 2;
		gbc_inputPanel.anchor = GridBagConstraints.NORTHWEST;
		gbc_inputPanel.insets = new Insets(0, 0, 5, 0);
		gbc_inputPanel.gridx = 1;
		gbc_inputPanel.gridy = 0;
		menusPanel.add(inputPanel, gbc_inputPanel);
		final GridBagLayout gbl_inputPanel = new GridBagLayout();
		gbl_inputPanel.columnWidths = new int[] { 0 };
		gbl_inputPanel.rowHeights = new int[] { 0 };
		gbl_inputPanel.columnWeights = new double[] { 0.0 };
		gbl_inputPanel.rowWeights = new double[] { 0.0, 0.0, 0.0 };
		inputPanel.setLayout(gbl_inputPanel);

		final JPanel dataFilePanel = new JPanel();
		final GridBagConstraints gbc_dataFilePanel = new GridBagConstraints();
		gbc_dataFilePanel.fill = GridBagConstraints.HORIZONTAL;
		gbc_dataFilePanel.anchor = GridBagConstraints.WEST;
		gbc_dataFilePanel.gridx = 0;
		gbc_dataFilePanel.gridy = 0;
		inputPanel.add(dataFilePanel, gbc_dataFilePanel);
		dataFilePanel.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 5));

		final JLabel lblInputFile = new JLabel("Input file:");
		lblInputFile.setToolTipText("Input file as a text TAB-separated file with census quant compare format");
		dataFilePanel.add(lblInputFile);

		dataFileText = new JTextField();
		dataFileText.setToolTipText(
				"Full path to the input file as a text TAB-separated file with census quant compare format");
		dataFilePanel.add(dataFileText);
		dataFileText.setColumns(80);
		final JButton selectInputFileButton = new JButton("Select");
		selectInputFileButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				openSelectInputFileDialog();
			}
		});
		selectInputFileButton.setToolTipText("Click to select file on your file system");
		dataFilePanel.add(selectInputFileButton);

		final JPanel fastaFilePanel = new JPanel();
		final GridBagConstraints gbc_fastaFilePanel = new GridBagConstraints();
		gbc_fastaFilePanel.fill = GridBagConstraints.HORIZONTAL;
		gbc_fastaFilePanel.anchor = GridBagConstraints.WEST;
		gbc_fastaFilePanel.gridx = 0;
		gbc_fastaFilePanel.gridy = 1;
		inputPanel.add(fastaFilePanel, gbc_fastaFilePanel);
		fastaFilePanel.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 5));

		lblFastaFile = new JLabel("Fasta file:");
		lblFastaFile.setEnabled(false);
		lblFastaFile.setToolTipText("FASTA file containing the protein sequence of the protein of interest.");
		fastaFilePanel.add(lblFastaFile);

		fastaFileText = new JTextField();
		fastaFileText.setEnabled(false);
		fastaFileText.setToolTipText(
				"Full path to the FASTA file containing the protein sequence of the protein of interest.");
		fastaFilePanel.add(fastaFileText);
		fastaFileText.setColumns(80);

		selectFastaFileButton = new JButton("Select");
		selectFastaFileButton.setEnabled(false);
		selectFastaFileButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				openSelectFASTAFileDialog();

			}
		});
		selectFastaFileButton.setToolTipText("Click to select file on your file system");
		fastaFilePanel.add(selectFastaFileButton);

		final JPanel accessionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		final GridBagConstraints gbc_accessionPanel = new GridBagConstraints();
		gbc_accessionPanel.fill = GridBagConstraints.HORIZONTAL;
		gbc_accessionPanel.anchor = GridBagConstraints.WEST;
		gbc_accessionPanel.gridx = 0;
		gbc_accessionPanel.gridy = 2;
		inputPanel.add(accessionPanel, gbc_accessionPanel);

		final JLabel lblProteinOfInterest = new JLabel("Protein of interest:");
		lblProteinOfInterest.setToolTipText(
				"Accession of the protein of interest. This should be the accession that is present in input file and FASTA file.");
		accessionPanel.add(lblProteinOfInterest);
		proteinOfInterestText = new JTextField(proteinOfInterest);
		proteinOfInterestText.addKeyListener(new KeyListener() {

			@Override
			public void keyTyped(KeyEvent e) {

			}

			@Override
			public void keyReleased(KeyEvent e) {
				updateProteinOfInterestAndRelatedControls();
				AppDefaults.getInstance().setProteinOfInterest(proteinOfInterestText.getText());
			}

			@Override
			public void keyPressed(KeyEvent e) {

			}
		});
		proteinOfInterestText.setToolTipText(
				"Accession of the protein of interest. This should be the accession that is present in input file and FASTA file.");
		accessionPanel.add(proteinOfInterestText);
		proteinOfInterestText.setColumns(20);
		if (AppDefaults.getInstance().getInputFile() != null) {
			final File previousInputFile = new File(AppDefaults.getInstance().getInputFile());
			if (previousInputFile.exists()) {
				dataFileText.setText(previousInputFile.getAbsolutePath());
			} else if (previousInputFile.getParentFile().exists()) {
				dataFileText.setText(previousInputFile.getParentFile().getAbsolutePath());
			}
		}

		if (AppDefaults.getInstance().getProteinOfInterest() != null && !GlycoPTMAnalyzer.DEFAULT_PROTEIN_OF_INTEREST
				.equals(AppDefaults.getInstance().getProteinOfInterest())) {
			if (AppDefaults.getInstance().getFasta() != null) {
				fastaFileText.setText(AppDefaults.getInstance().getFasta());
				updateProteinOfInterestAndRelatedControls();
				// this needs to be after update controls because otherwise, fasta is disabled
				// and getFastaFile returns null
				ProteinSequences.getInstance(getFastaFile(), getMotifRegexp());
			}
		}

		final JPanel outputPanel = new JPanel();
		final GridBagConstraints gbc_outputPanel = new GridBagConstraints();
		gbc_outputPanel.insets = new Insets(0, 0, 0, 5);
		gbc_outputPanel.gridx = 1;
		gbc_outputPanel.gridy = 1;
		menusPanel.add(outputPanel, gbc_outputPanel);
		outputPanel.setBorder(new TitledBorder(null, "Output", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		final GridBagLayout gbl_outputPanel = new GridBagLayout();
		gbl_outputPanel.columnWidths = new int[] { 0 };
		gbl_outputPanel.rowHeights = new int[] { 0, 0, 0, 0 };
		gbl_outputPanel.columnWeights = new double[] { 1.0 };
		gbl_outputPanel.rowWeights = new double[] { 0.0, 0.0, 0.0, 1.0 };
		outputPanel.setLayout(gbl_outputPanel);
		final JPanel namePanel = new JPanel();
		final FlowLayout flowLayout_4 = (FlowLayout) namePanel.getLayout();
		flowLayout_4.setAlignment(FlowLayout.LEFT);
		final GridBagConstraints gbc_namePanel = new GridBagConstraints();
		gbc_namePanel.fill = GridBagConstraints.BOTH;
		gbc_namePanel.gridx = 0;
		gbc_namePanel.gridy = 0;
		outputPanel.add(namePanel, gbc_namePanel);

		final JLabel lblNameForOutput = new JLabel("Name for output files:");
		lblNameForOutput.setToolTipText("Prefix that will be added to all the output files");
		namePanel.add(lblNameForOutput);

		nameTextField = new JTextField();
		nameTextField.setToolTipText("Prefix that will be added to all the output files");
		namePanel.add(nameTextField);
		nameTextField.setColumns(30);
		nameTextField.addKeyListener(new KeyListener() {

			@Override
			public void keyTyped(KeyEvent e) {

			}

			@Override
			public void keyReleased(KeyEvent e) {
				AppDefaults.getInstance().setRunName(nameTextField.getText());
			}

			@Override
			public void keyPressed(KeyEvent e) {

			}
		});
		final JPanel separateChartsPanel = new JPanel();
		final FlowLayout flowLayout_5 = (FlowLayout) separateChartsPanel.getLayout();
		flowLayout_5.setAlignment(FlowLayout.LEFT);
		separateChartsButton = new JButton(POPUP_CHARTS);
		separateChartsPanel.add(separateChartsButton);
		separateChartsButton.setEnabled(false);
		separateChartsButton.setToolTipText("Click to show or close graphs in separate resizable dialogs");
		final GridBagConstraints gbc_separateChartsPanel = new GridBagConstraints();
		gbc_separateChartsPanel.fill = GridBagConstraints.BOTH;
		gbc_separateChartsPanel.gridx = 0;
		gbc_separateChartsPanel.gridy = 1;
		outputPanel.add(separateChartsPanel, gbc_separateChartsPanel);

		btnCloseCharts = new JButton("Close charts");
		btnCloseCharts.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				closeCharts();
			}
		});
		btnCloseCharts.setToolTipText("Click here to close all charts that have been openend on independent windows.");
		btnCloseCharts.setEnabled(false);
		separateChartsPanel.add(btnCloseCharts);
		separateChartsButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {

				separateCharts();

			}
		});

		showProteinSequenceButton = new JButton("Advanced results inspection");
		showProteinSequenceButton.setEnabled(false);
		separateChartsPanel.add(showProteinSequenceButton);
		showProteinSequenceButton.setToolTipText("Click to visualize the results overlayed on the protein sequence");
		showProteinSequenceButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {

				showSequenceDialog(getProteinOfInterestACC(), currentGlycoSites, currentPeptides);

			}
		});

		final JPanel panel = new JPanel();
		final GridBagConstraints gbc_panel = new GridBagConstraints();
		gbc_panel.anchor = GridBagConstraints.WEST;
		gbc_panel.fill = GridBagConstraints.VERTICAL;
		gbc_panel.gridx = 0;
		gbc_panel.gridy = 2;
		outputPanel.add(panel, gbc_panel);
		btnShowResultsTable = new JButton("Show sites table");
		panel.add(btnShowResultsTable);
		btnShowResultsTable.setToolTipText("Click to open a table with all the results of the analysis");
		btnShowResultsTable.setEnabled(false);
		btnShowPeptidesTable = new JButton("Show peptides table");
		panel.add(btnShowPeptidesTable);
		btnShowPeptidesTable.setToolTipText(
				"Click to open a table with all the measurements per peptide and how they map to the sites in the protein");
		btnShowPeptidesTable.setEnabled(false);
		btnShowPeptidesTable.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				showPeptidesResultsTableDialog();
			}
		});
		btnShowResultsTable.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				showSitesResultsTableDialog();
			}
		});

		final JPanel startButtonPanel = new JPanel();
		final GridBagConstraints gbc_startButtonPanel = new GridBagConstraints();
		gbc_startButtonPanel.anchor = GridBagConstraints.NORTHWEST;
		gbc_startButtonPanel.gridx = 2;
		gbc_startButtonPanel.gridy = 1;
		menusPanel.add(startButtonPanel, gbc_startButtonPanel);
		startButtonPanel
				.setBorder(new TitledBorder(null, "Control", TitledBorder.CENTER, TitledBorder.TOP, null, null));
		final GridBagLayout gbl_startButtonPanel = new GridBagLayout();
		gbl_startButtonPanel.columnWidths = new int[] { 0 };
		gbl_startButtonPanel.rowHeights = new int[] { 0, 0 };
		gbl_startButtonPanel.columnWeights = new double[] { 0.0 };
		gbl_startButtonPanel.rowWeights = new double[] { 0.0, 0.0 };
		startButtonPanel.setLayout(gbl_startButtonPanel);

		final JPanel panel_1 = new JPanel();
		final GridBagConstraints gbc_panel_1 = new GridBagConstraints();
		gbc_panel_1.anchor = GridBagConstraints.NORTHWEST;
		gbc_panel_1.gridx = 0;
		gbc_panel_1.gridy = 0;
		startButtonPanel.add(panel_1, gbc_panel_1);

		final JButton startButton_1 = new JButton("START");
		panel_1.add(startButton_1);
		startButton_1.setFont(new Font("Tahoma", Font.BOLD, 14));
		startButton_1.setForeground(SystemColor.desktop);
		startButton_1.setAlignmentX(Component.CENTER_ALIGNMENT);
		startButton_1.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				startAnalysis();
			}
		});
		startButton_1.setToolTipText("Click to start the analysis");

		compareRunsButton = new JButton("Compare runs");
		compareRunsButton.setToolTipText("Click to compare two (or more) runs");
		compareRunsButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				compareExperiments();
			}
		});
		panel_1.add(compareRunsButton);
		compareRunsButton.setEnabled(false);

		final JPanel panel_2 = new JPanel();
		final GridBagConstraints gbc_panel_2 = new GridBagConstraints();
		gbc_panel_2.anchor = GridBagConstraints.WEST;
		gbc_panel_2.gridx = 0;
		gbc_panel_2.gridy = 1;
		startButtonPanel.add(panel_2, gbc_panel_2);

		final JButton btnShowRuns = new JButton("Show Runs");
		panel_2.add(btnShowRuns);

		final JButton btnRefreshRunsFolder = new JButton("Refresh runs");
		panel_2.add(btnRefreshRunsFolder);
		btnRefreshRunsFolder.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				refreshRuns();
			}
		});
		btnShowRuns.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				showRunsDialog();
			}
		});
		if (AppDefaults.getInstance().getRunName() != null) {
			nameTextField.setText(AppDefaults.getInstance().getRunName());
		}

		final JPanel panelForCharts = new JPanel();
		panelForCharts.setPreferredSize(new Dimension(10, 400));
		panelForCharts.setBorder(new TitledBorder(
				new EtchedBorder(EtchedBorder.LOWERED, new Color(255, 255, 255), new Color(160, 160, 160)), "Charts",
				TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));

//		getContentPane().add(panelForCharts, BorderLayout.CENTER);
		panelForCharts.setLayout(new BorderLayout(0, 0));

		chartPanelScroll = new JScrollPane();
		componentStateKeeper.addInvariableComponent(chartPanelScroll);
		chartPanelScroll.setBorder(null);
		chartPanelScroll.getVerticalScrollBar().setUnitIncrement(16);
		this.chartPanel = new JPanel();
		chartPanelScroll.setViewportView(chartPanel);
		// TODO
//		chartPanel.setLayout(new BorderLayout(0, 0));
		chartPanel.setLayout(new GridBagLayout());
		panelForCharts.add(chartPanelScroll);

		final JPanel statusPanel = new JPanel();
		statusPanel.setBorder(new TitledBorder(null, "Status", TitledBorder.LEADING, TitledBorder.TOP, null, null));
//		getContentPane().add(statusPanel, BorderLayout.SOUTH);

		statusPanel.setLayout(new BorderLayout(0, 0));
		final JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		split.setTopComponent(panelForCharts);
		split.setBottomComponent(statusPanel);
		split.setContinuousLayout(true);
		split.setOneTouchExpandable(true);
// set minimum dimensions of components
		panelForCharts.setMinimumSize(new Dimension(500, 50));
		statusPanel.setMinimumSize(new Dimension(500, 50));
		getContentPane().add(split, BorderLayout.CENTER);

		final JScrollPane scrollPane = new JScrollPane();
		componentStateKeeper.addInvariableComponent(scrollPane);
		componentStateKeeper.addInvariableComponent(split);
		scrollPane.setToolTipText("Status");
		statusPanel.add(scrollPane, BorderLayout.CENTER);

		statusTextArea = new JTextArea();
		statusTextArea.setToolTipText("Status");
		statusTextArea.setWrapStyleWord(true);
		statusTextArea.setFont(new Font("Tahoma", Font.PLAIN, 11));
		statusTextArea.setRows(5);
		scrollPane.setViewportView(statusTextArea);
		// create menu
		createMenuBar();
		//
		pack();
		final java.awt.Dimension screenSize = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
		final java.awt.Dimension dialogSize = getSize();
		setLocation((screenSize.width - dialogSize.width) / 2, (screenSize.height - dialogSize.height) / 2);
	}

	private void createMenuBar() {
		final JMenuBar menuBar = new JMenuBar();

		// create menus
		final JMenu advancedParametersMenu = new JMenu("Advanced parameters");
		menuBar.add(advancedParametersMenu);
		discardNonUniquePeptidesMenuItem = new JCheckBoxMenuItem("Discard non-unique peptides", false);
		discardNonUniquePeptidesMenuItem
				.setToolTipText("<html>If enabled, peptides shared by multiple proteins will be discarded.</html>");
		advancedParametersMenu.add(discardNonUniquePeptidesMenuItem);
		dontAllowConsecutiveMotifsMenuItem = new JCheckBoxMenuItem("Don't allow consecutive motifs", true);
		dontAllowConsecutiveMotifsMenuItem
				.setToolTipText("<html>If enabled, motifs found in consecutive positions will be marked as ambiguous."
						+ "<br>Additionally, if a peptide is found modified in both positions, will be discarded."
						+ "<br>This is usually done when PTMs in consecutive positions are not physically possible.</html>");
		advancedParametersMenu.add(dontAllowConsecutiveMotifsMenuItem);

		setJMenuBar(menuBar);
	}

	protected void compareExperiments() {
		final List<String> selectedRuns = getRunsAttachedDialog().getScrollableTable().getTable().getSelectedRunPaths();
		final StringBuilder sb = new StringBuilder();
		for (final String string : selectedRuns) {
			sb.append("\t" + string);
		}
		showMessage("Comparing: " + sb.toString());

		componentStateKeeper.keepEnableStates(this);
		componentStateKeeper.disable(this);
		final GlycoPTMRunComparator comparator = new GlycoPTMRunComparator(selectedRuns);
		comparator.addPropertyChangeListener(this);
		comparator.execute();
	}

	protected void closeCharts() {
		for (final JFrame frame : popupCharts) {
			frame.dispose();
		}
	}

	protected void iterativeThresholdAnalysisClicked(boolean selected) {
		this.intensityThresholdIntervalLabel.setEnabled(selected);
		this.intensityThresholdIntervalTextField.setEnabled(selected);
		this.intensityThresholdText.setEnabled(!selected);
		this.intensityThresholdCheckBox.setEnabled(!selected);
	}

	protected void showSitesResultsTableDialog() {
		if (currentGlycoSites == null || currentGlycoSites.isEmpty()) {
			showError("Start a new analysis or load a GlycoMSQuant run to have sites to show.");
			return;
		}
		final SitesTableDialog tableDialog = new SitesTableDialog();
		tableDialog.loadResultTable(currentGlycoSites, isSumIntensitiesAcrossReplicates());
		tableDialog.setVisible(true);
	}

	protected void showPeptidesResultsTableDialog() {
		if (currentGlycoSites == null || currentGlycoSites.isEmpty()) {
			showError("Start a new analysis or load a GlycoMSQuant run to have peptides to show.");
			return;
		}
		if (currentPeptides == null || currentPeptides.isEmpty()) {
			showError("Start a new analysis or load a GlycoMSQuant run to have peptides to show.");
			return;
		}
		final PeptidesTableDialog tableDialog = new PeptidesTableDialog();
		tableDialog.getTable().loadResultTable(currentPeptides, currentGlycoSites, getProteinSequence());
		tableDialog.setVisible(true);
	}

	/**
	 * 
	 * @param separate if true, a pop-up window will be created for each chart. If
	 *                 false, it will close all pop-up windows that have not been
	 *                 yet closed and will set the graphs in the main graph panel of
	 *                 the app.
	 */
	protected void separateCharts() {
		final Iterator<JComponent> iterator = chartsInMainPanel.iterator();
		while (iterator.hasNext()) {
			final JComponent component = iterator.next();
			final int width = GuiUtils.getFractionOfScreenWidthSize(0.5);
			int height = GuiUtils.getFractionOfScreenHeightSize(3.0 / 4);
			if (component instanceof AbstractMultipleChartsBySitePanel) {
				height = GuiUtils.getFractionOfScreenHeightSize(6.0 / 7);
			}
			// remove from chartsInMainPanel
			iterator.remove();
			// show in dialog
			showChartDialog(component, width, height);
		}
		chartPanel.updateUI();
		this.separateChartsButton.setEnabled(false);
	}

	private static final Random random = new Random(new Date().getTime());

	private void showChartDialog(JComponent component, int width, int height) {
		final java.awt.Dimension screenSize = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
		String title = "";
		if (component instanceof ChartPanel) {
			final ChartPanel chartPanel = (ChartPanel) component;
			title = " - " + chartPanel.getChart().getTitle().getText();
		}
		final JFrame frame = new JFrame(getName() + title);
		popupCharts.add(frame);

		frame.setPreferredSize(new Dimension(width, height));
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame.getContentPane().setLayout(new BorderLayout(10, 10));

		final JScrollPane scroll = new JScrollPane(component);

		frame.getContentPane().add(scroll, BorderLayout.CENTER);
		frame.setVisible(true);
		frame.pack();

		final java.awt.Dimension dialogSize = frame.getSize();
		final int x = (screenSize.width - dialogSize.width) / 2;
		final int y = (screenSize.height - dialogSize.height) / 2;
		final double offsetScreenPercent = 0.10;
		// make each one randomly position with an offset across the x and y axis that
		// will be from -10% to 10% of the screen
		final int maxOffsetX = Double.valueOf(offsetScreenPercent * screenSize.width).intValue();
		final int maxOffsetY = Double.valueOf(offsetScreenPercent * screenSize.height).intValue();
		final int offsetX = x + random.nextInt(maxOffsetX) - maxOffsetX;
		final int offsetY = y + random.nextInt(maxOffsetY) - maxOffsetY;
		frame.setLocation(offsetX, offsetY);
		final WindowAdapter listener = new WindowAdapter() {

			@Override
			public void windowClosing(WindowEvent e) {

				// remove the chart dialog from the list
				popupCharts.remove(frame);
				if (popupCharts.isEmpty()) {
					btnCloseCharts.setEnabled(false);
				}
				// add to main panel
				showChartsInMainPanel(component);

			}
		};
		frame.addWindowListener(listener);
		btnCloseCharts.setEnabled(true);
	}

	protected void refreshRuns() {

		getRunsAttachedDialog().loadResultFolders();
	}

	protected void showRunsDialog() {
//		super.showAttachedHelpDialog();
		super.showAttachedRunsDialog();

	}

	protected void startAnalysis() {
		try {
			if (isIterativeAnalysis()) {
				checkValidityInputDataForIterativeAnalysis();
				final int selectedOption = JOptionPane.showConfirmDialog(this,
						"<html>Do you want to start an iterative analysis of the intensity threshold?<br>"
								+ "This will analyze the dataset with the choosen settings except for the intensity threshold,<br>"
								+ "which will be increased every iteration until there is no more peptides passing the filter<br>"
								+ "or until a max number of interations (" + IterativeThresholdAnalysis.MAX_ITERATIONS
								+ ").<br>"
								+ "Every iteration a chart will be updated with the average proportions (%) of each modification<br>"
								+ " type and with the number of peptides passing the intensity threshold filter.</html>",
						"Iterative analysis of intensity threshold", JOptionPane.YES_NO_CANCEL_OPTION,
						JOptionPane.QUESTION_MESSAGE);
				if (selectedOption == JOptionPane.YES_OPTION) {
					// disable sites and peptides tables
					this.btnShowPeptidesTable.setEnabled(false);
					this.btnShowResultsTable.setEnabled(false);
					showProteinSequenceButton.setEnabled(false);
					// keep enable/disable states
					this.componentStateKeeper.keepEnableStates(this);
					this.componentStateKeeper.disable(this);
					clearGraphs();
					// clear status
					this.statusTextArea.setText(null);
					showMessage("Starting iterative analysis of intensity threshold...");
					// clear iterativeThresholdAnalysis object
					iterativeThresholdAnalysis = null;
					// set intensity threshold to 0.0 to start
					this.intensityThresholdText.setText("0.0");
					readInputData();
				}
			} else {
				checkValidityInputDataForNormalAnalysis();
				// keep enable/disable states
				this.componentStateKeeper.keepEnableStates(this);
				this.componentStateKeeper.disable(this);
				clearGraphs();
				// clear status
				this.statusTextArea.setText(null);
				showMessage("Starting analysis...");
				try {
					readInputData();
				} catch (final Exception e) {
					e.printStackTrace();
					this.componentStateKeeper.setToPreviousState(this);
				}
			}
		} catch (final Exception e) {
			e.printStackTrace();
			showError(e.getMessage());
		}
	}

	private void checkValidityInputDataForNormalAnalysis() {
		checkValidityInputDataFile();
		if (!getProteinOfInterestACC().equals(GlycoPTMAnalyzer.DEFAULT_PROTEIN_OF_INTEREST)) {
			if ("".equals(getProteinOfInterestACC())) {
				throw new IllegalArgumentException("Protein of interest is empty. Try with the default one: '"
						+ GlycoPTMAnalyzer.DEFAULT_PROTEIN_OF_INTEREST + "'.");
			}
			if (getFastaFile() == null) {
				throw new IllegalArgumentException("If you are not using the default protein of interest '"
						+ GlycoPTMAnalyzer.DEFAULT_PROTEIN_OF_INTEREST
						+ "' you need to provide a fasta file in which the sequence of the protein of interest '"
						+ getProteinOfInterestACC()
						+ "' is present. It can be a fasta file with more proteins, the program will search for the one of interest.");
			}
			if (!getFastaFile().exists()) {
				throw new IllegalArgumentException("Fasta file '" + getFastaFile().getAbsolutePath() + "' not found.");
			}
		}
	}

	private void checkValidityInputDataForIterativeAnalysis() {
		checkValidityInputDataFile();
		if ("".equals(this.intensityThresholdIntervalTextField.getText())) {
			throw new IllegalArgumentException(
					"Iterative analysis of intensity threshold needs a intensity iteration factor.");
		}
		try {
			final double factor = Double.valueOf(this.intensityThresholdIntervalTextField.getText());
			if (factor <= 1.0) {
				throw new IllegalArgumentException("Intensity iteration factor must be greater than 1.0");
			}
		} catch (final NumberFormatException e) {
			throw new IllegalArgumentException("Intensity iteration factor is not a number: '"
					+ this.intensityThresholdIntervalTextField.getText() + "'.");
		}

	}

	private void checkValidityInputDataFile() {
		if ("".equals(this.dataFileText.getText())) {
			throw new IllegalArgumentException("Select an input file to work with.");
		}
		if (!new File(this.dataFileText.getText()).exists()) {
			throw new IllegalArgumentException("Input file: " + this.dataFileText.getText() + " not found.");
		}
	}

	private boolean isIterativeAnalysis() {
		return this.iterativeThresholdAnalysisCheckBox.isSelected();
	}

	protected void updateProteinOfInterestAndRelatedControls() {
		final boolean b = !proteinOfInterestText.getText().equals(GlycoPTMAnalyzer.DEFAULT_PROTEIN_OF_INTEREST);

		fastaFileText.setEnabled(b);
		lblFastaFile.setEnabled(b);
		selectFastaFileButton.setEnabled(b);

	}

	protected void openSelectFASTAFileDialog() {
		if (fileChooserFASTA == null) {
			fileChooserFASTA = new JFileChooser();
			final String previousFastaLocation = AppDefaults.getInstance().getFasta();
			if (previousFastaLocation != null) {
				fileChooserFASTA.setCurrentDirectory(new File(previousFastaLocation));
			}
			fileChooserFASTA.setFileFilter(new FileFilter() {

				@Override
				public String getDescription() {
					return "FASTA files";
				}

				@Override
				public boolean accept(File f) {
					if (f.isDirectory()) {
						return true;
					}
					if (FilenameUtils.getExtension(f.getAbsolutePath()).equals("fasta")) {
						return true;
					}

					return false;
				}
			});
		}
		if (fileChooserFASTA.showDialog(this, "Select FASTA file") == JFileChooser.APPROVE_OPTION) {
			final File selectedFile = fileChooserFASTA.getSelectedFile();
			if (selectedFile.exists()) {
				AppDefaults.getInstance().setFasta(selectedFile.getAbsolutePath());
				this.fastaFileText.setText(selectedFile.getAbsolutePath());
				// setup the proteinsequences
				ProteinSequences.getInstance(selectedFile, getMotifRegexp());
			} else {
				showError("Error selecting file that doesn't exist: " + selectedFile.getAbsolutePath());
			}
		}
	}

	private void showError(Object message) {
		if (message != null) {
			showMessage(message.toString(), "ERROR");
		} else {
			showMessage("Unknown error", "ERROR");
		}
	}

	private String getFormattedTime() {
		return format.format(new Date());

	}

	protected void showSequenceDialog(String proteinOfInterest, List<GlycoSite> glycoSites,
			List<QuantifiedPeptideInterface> peptides) {
		getProteinSequence();
		final String proteinSequence = getProteinSequence();
		if (proteinSequence == null) {
			showError("Error getting protein sequence from protein '" + proteinOfInterest + "'");
			return;
		}
		proteinSequenceDialog = new ProteinSequenceDialog(proteinOfInterest, proteinSequence, glycoSites, peptides,
				isSumIntensitiesAcrossReplicatesFromLoadedResults());
		proteinSequenceDialog.setVisible(true);
	}

	private boolean isSumIntensitiesAcrossReplicatesFromLoadedResults() {
		return isSumIntensitiesAcrossReplicatesFromLoadedResults;
	}

	private String getProteinSequence() {

		return ProteinSequences.getInstance().getProteinSequence(getProteinOfInterestACC());

	}

	protected void openSelectInputFileDialog() {
		if (fileChooserInputFile == null) {
			fileChooserInputFile = new JFileChooser();
			final String previousInputFile = AppDefaults.getInstance().getInputFile();
			if (previousInputFile != null) {
				fileChooserInputFile.setCurrentDirectory(new File(previousInputFile).getParentFile());
			}

			fileChooserInputFile.setFileFilter(new FileFilter() {

				@Override
				public String getDescription() {
					return "Excel Files (*.xlsx), Text Files(*.txt, *.tsv)";
				}

				@Override
				public boolean accept(File f) {
					if (f.isDirectory()) {
						return true;
					}
					if (FilenameUtils.getExtension(f.getAbsolutePath()).equals("tsv")) {
						return true;
					}
					if (FilenameUtils.getExtension(f.getAbsolutePath()).equals("txt")) {
						return true;
					}
					if (FilenameUtils.getExtension(f.getAbsolutePath()).equals("xlsx")) {
						return true;
					}
					return false;
				}
			});
		}
		if (fileChooserInputFile.showDialog(this, "Select input file") == JFileChooser.APPROVE_OPTION) {
			final File selectedFile = fileChooserInputFile.getSelectedFile();
			if (selectedFile.exists()) {
				AppDefaults.getInstance().setInputFile(selectedFile.getAbsolutePath());
				this.dataFileText.setText(selectedFile.getAbsolutePath());
				// set name as the file name
				final String experimentName = FilenameUtils.getBaseName(selectedFile.getAbsolutePath());
				this.nameTextField.setText(experimentName);
			} else {
				showError("Error selecting file that doesn't exist: " + selectedFile.getAbsolutePath());
			}
		}
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = -1997447648863546859L;
	private JTextField dataFileText;
	private JTextField fastaFileText;
	private JTextField proteinOfInterestText;
	private JTextField intensityThresholdText;
	private JButton selectFastaFileButton;
	private JLabel lblFastaFile;
	private JButton showProteinSequenceButton;
	private JTextField nameTextField;
	private JCheckBox normalizeIntensityCheckBox;
	private JScrollPane chartPanelScroll;
	private JCheckBox sumIntensitiesAcrossReplicatesCheckBox;
	private InputDataReader inputDataReader;
	private JLabel intensityThresholdIntervalLabel;
	private JTextField intensityThresholdIntervalTextField;
	private IterativeThresholdAnalysis iterativeThresholdAnalysis;
	private List<QuantifiedPeptideInterface> currentPeptides;
	private JButton compareRunsButton;
	private String motifRegexp = GlycoPTMAnalyzer.NEW_DEFAULT_MOTIF_REGEXP;
	private AmountType amountType = AmountType.INTENSITY;

	public static void main(String[] args) {
		// set to not disapear all tooltips
		ToolTipManager.sharedInstance().setDismissDelay(Integer.MAX_VALUE);

		final MainFrame frame = MainFrame.getInstance();
		frame.setVisible(true);
		CurrentInputParameters.getInstance().setInputParameters(frame);
	}

	@Override
	public File getInputFile() {
		if (!"".equals(this.dataFileText.getText())) {
			return new File(this.dataFileText.getText());
		}
		return null;
	}

	@Override
	public String getProteinOfInterestACC() {
		return proteinOfInterestText.getText();
	}

	@Override
	public File getFastaFile() {
		if (!"".equals(this.fastaFileText.getText())) {
			return new File(this.fastaFileText.getText());

		}
		if (getProteinOfInterestACC().equals(GlycoPTMAnalyzer.DEFAULT_PROTEIN_OF_INTEREST)) {
			return AppDefaults.getDefaultProteinOfInterestInternalFastaFile();
		}
		return null;
	}

	@Override
	public String getName() {

		return this.nameTextField.getText();
	}

	@Override
	public Double getIntensityThreshold() {
		try {
			if ("".equals(this.intensityThresholdText.getText())) {
				return 0.0;
			}
			final double t = Double.valueOf(this.intensityThresholdText.getText());
			return t;
		} catch (final NumberFormatException e) {
			showError(e.getMessage());
		}
		return 0.0;
	}

	@Override
	public AmountType getAmountType() {
		return amountType;
	}

	@Override
	public Boolean isNormalizeReplicates() {
		return normalizeIntensityCheckBox.isSelected();
	}

	/**
	 * Reads input data with the new threshold and starts the analysis after that
	 */
	private void readInputData() {
		final File inputFile = getInputFile();
		final String proteinOfInterestACC = getProteinOfInterestACC();
		final AmountType amountType = getAmountType();
		final boolean normalizeExperimentsByProtein = isNormalizeReplicates();
		final double intensityThreshold = getIntensityThreshold();
		final String motifRegexp = getMotifRegexp();
		final boolean discardWrongPositionedPTMs = isDiscardWrongPositionedPTMs();
		log.info("Reading input file '" + inputFile.getAbsolutePath() + "'...");
		inputDataReader = new InputDataReader(inputFile, proteinOfInterestACC, intensityThreshold, amountType,
				normalizeExperimentsByProtein, motifRegexp, discardWrongPositionedPTMs);
		inputDataReader.addPropertyChangeListener(this);
		inputDataReader.execute();
	}

	/**
	 * Update the values in properties file in the results folder
	 */
	private void updateProperties(File resultsFolder) {
		// copy input file to results folder
		if (getInputFile() != null) {// if it is null, it is because this object was created only with the
										// glycosites, to create the charts
			try {
				FileManager.copyInputDataFileToResultsFolder(getInputFile(), resultsFolder);
			} catch (final IOException e) {
				e.printStackTrace();
			}

		}

		final ResultsProperties resultsProperties = new ResultsProperties(resultsFolder);
		resultsProperties.setName(getName());
		resultsProperties.setProteinSequence(getProteinSequence());
		resultsProperties.setInputDataFile(getInputFile());
		resultsProperties.setIntensityThreshold(getIntensityThreshold());
		resultsProperties.setNormalizeReplicates(isNormalizeReplicates());
		resultsProperties.setSumIntensitiesAcrossReplicates(isSumIntensitiesAcrossReplicates());
		resultsProperties.setProteinOfInterest(getProteinOfInterestACC());
		resultsProperties.setMotifRegexp(getMotifRegexp());
		resultsProperties.setFastaFile(getFastaFile());
		resultsProperties.setDiscardWrongPositionedPTMs(isDiscardWrongPositionedPTMs());
		resultsProperties.setDiscardNonUniquePeptides(isDiscardNonUniquePeptides());
		resultsProperties.setDontAllowConsecutiveMotifs(isDontAllowConsecutiveMotifs());
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		if (evt.getPropertyName().equals("progress")) {
			showMessage(evt.getNewValue());
		} else if (evt.getPropertyName().equals(InputDataReader.INPUT_DATA_READER_FINISHED)) {
			currentPeptides = (List<QuantifiedPeptideInterface>) evt.getNewValue();
			if (currentPeptides != null && !currentPeptides.isEmpty()) {
				showMessage(currentPeptides.size() + " peptides read from input file ");
				log.info("Analyzing peptides...");
				final GlycoPTMPeptideAnalyzer peptideAnalyzer = new GlycoPTMPeptideAnalyzer(currentPeptides,
						getProteinOfInterestACC(), getFastaFile(), getAmountType(), getMotifRegexp(),
						isDontAllowConsecutiveMotifs());
				peptideAnalyzer.addPropertyChangeListener(this);
				peptideAnalyzer.execute();
			} else {
				showError("Some error occurred because no peptides were read from input file.");
			}
		} else if (evt.getPropertyName().equals(InputDataReader.INPUT_DATA_READER_ERROR)) {
			if (!isIterativeAnalysis()) {
				showError(evt.getNewValue());
			} else {
				// if there is an error, maybe is truly an error or is because there is no
				// peptides passing a so stringent threshold in an iterative analysis
				// we need to add the zero value at the last iteration in any case
				updateIterativeAnalysis(null);
			}
			this.componentStateKeeper.setToPreviousState(this);
		} else if (evt.getPropertyName().equals(InputDataReader.INPUT_DATA_READER_START)) {
			showMessage("Reading input data...");
		} else if (evt.getPropertyName().equals(InputDataReader.NUM_VALID_PEPTIDES)) {
//			peptidesValid = (int) evt.getNewValue();
		} else if (evt.getPropertyName().equals(GlycoPTMPeptideAnalyzer.HIVPOSITIONS_CALCULATED)) {
			currentGlycoSites = (List<GlycoSite>) evt.getNewValue();
			// only if it is not an iterative analysis, we generate the results.
			if (!isIterativeAnalysis()) {
				showMessage("Analysis resulted in " + currentGlycoSites.size() + " positions in protein '"
						+ getProteinOfInterestACC() + "'");
				final File newIndividualResultFolder = FileManager.getNewIndividualResultFolder();
				// now that we have the new results folder, we can update the properties
				updateProperties(newIndividualResultFolder);
				final ResultsProperties resultsProperties = new ResultsProperties(newIndividualResultFolder);
				resultsProperties.setProteinSequence(getProteinSequence());

				final GlycoPTMResultGenerator resultGenerator = new GlycoPTMResultGenerator(newIndividualResultFolder,
						currentGlycoSites, this);
				resultGenerator.setGenerateGraph(true);
				resultGenerator.setGenerateTable(true);
				resultGenerator.setSaveGraphsToFiles(false);
				resultGenerator.addPropertyChangeListener(this);
				resultGenerator.execute();
			} else {
				updateIterativeAnalysis(currentGlycoSites);

			}
		} else if (evt.getPropertyName().equals(IterationGraphGenerator.GRAPH_GENERATED)) {
			showChartsInMainPanel(evt.getNewValue());

		} else if (evt.getPropertyName().equals(IterationGraphGenerator.ITERATIVE_ANALYSIS_ERROR)) {
			showError(evt.getNewValue());
			this.componentStateKeeper.setToPreviousState(this);
		} else if (evt.getPropertyName().equals(GlycoPTMResultGenerator.RESULTS_GENERATOR_FINISHED)) {
			refreshRuns();
			isSumIntensitiesAcrossReplicatesFromLoadedResults = isSumIntensitiesAcrossReplicates();
			this.componentStateKeeper.setToPreviousState(this);
			this.showProteinSequenceButton.setEnabled(true);

		} else if (evt.getPropertyName().equals(GlycoPTMResultGenerator.RESULS_TABLE_GENERATED)) {
			final File tableFile = (File) evt.getNewValue();
			showMessage("Result table created at '" + FileManager.removeAppRootFolderString(tableFile.getAbsolutePath())
					+ "'");
		} else if (evt.getPropertyName().equals(GlycoPTMResultGenerator.GLYCO_SITE_DATA_TABLE_GENERATED)) {
			final File file = (File) evt.getNewValue();
			showMessage("Glyco-sites data file create at: '"
					+ FileManager.removeAppRootFolderString(file.getAbsolutePath()) + "'");
		} else if (evt.getPropertyName().equals(GlycoPTMResultGenerator.CHART_GENERATED)) {
			showChartsInMainPanel(evt.getNewValue());
		} else if (evt.getPropertyName().equals(GlycoPTMResultGenerator.RESULTS_GENERATOR_ERROR)) {
			showError(evt.getNewValue());
			this.componentStateKeeper.setToPreviousState(this);
		} else if (evt.getPropertyName().equals(ResultLoaderFromDisk.RESULT_LOADER_FROM_DISK_STARTED)) {
			final File resultsFolder = (File) evt.getNewValue();
			try {
				updateControlsWithParametersFromDisk(resultsFolder);
				showMessage("Loading results from " + FilenameUtils.getName(resultsFolder.getAbsolutePath()) + "...");
			} catch (final Exception e) {
				e.printStackTrace();
				showError("Error loading results: " + e.getMessage());
				componentStateKeeper.setToPreviousState(this);
			}
		} else if (evt.getPropertyName().equals(ResultLoaderFromDisk.RESULT_LOADER_FROM_DISK_ERROR)) {
			showMessage("Error loading results: " + evt.getNewValue());
			this.componentStateKeeper.setToPreviousState(this);
		} else if (evt.getPropertyName().equals(ResultLoaderFromDisk.RESULT_LOADER_FROM_DISK_FINISHED)) {
			final ResultsLoadedFromDisk results = (ResultsLoadedFromDisk) evt.getNewValue();
			isSumIntensitiesAcrossReplicatesFromLoadedResults = results.getResultProperties()
					.isSumIntensitiesAcrossReplicates();
			this.currentGlycoSites = results.getSites();
			this.sumIntensitiesAcrossReplicatesCheckBox
					.setSelected(results.getResultProperties().isSumIntensitiesAcrossReplicates());
			this.currentPeptides = results.getPeptides();
			final GlycoPTMResultGenerator resultGenerator = new GlycoPTMResultGenerator(currentGlycoSites, this);
			resultGenerator.addPropertyChangeListener(this);
			resultGenerator.setGenerateGraph(true);
			resultGenerator.setGenerateTable(false);
			resultGenerator.setSaveGraphsToFiles(false);
			resultGenerator.execute();
			showMessage("Generating graphs for the analysis of " + currentGlycoSites.size() + " glyco sites.");
			this.componentStateKeeper.setToPreviousState(this);
			showProteinSequenceButton.setEnabled(true);
		} else if (evt.getPropertyName().equals(GlycoPTMRunComparator.COMPARATOR_ERROR)) {
			componentStateKeeper.setToPreviousState(this);
			showMessage("Error comparing experiments: " + evt.getNewValue());
		} else if (evt.getPropertyName().equals(GlycoPTMRunComparator.COMPARATOR_FINISHED)) {
			componentStateKeeper.setToPreviousState(this);

			final RunComparisonResult comparison = (RunComparisonResult) evt.getNewValue();
			showMessage(comparison.toString());
			showComparisonTables(comparison);
		} else if (evt.getPropertyName().equals(GlycoPTMPeptideAnalyzer.HIVPOSITIONS_PEPTIDES_TO_REMOVE)) {
			final PeptidesRemovedBecauseOfConsecutiveSitesWithPTM peptidesToRemove = (PeptidesRemovedBecauseOfConsecutiveSitesWithPTM) evt
					.getNewValue();
			final int peptidesBefore = currentPeptides.size();
			for (final QuantifiedPeptideInterface peptide : peptidesToRemove.getPeptides()) {
				currentPeptides.remove(peptide);
			}
			final int peptidesAfter = currentPeptides.size();
			showMessage(peptidesToRemove.size() + " peptides were removed because they contain a PTM at positions "
					+ peptidesToRemove.getPosition1() + " and " + peptidesToRemove.getPosition2()
					+ " but it is not physically possible. Peptide list reduced from " + peptidesBefore + " to "
					+ peptidesAfter);
		}
	}

	private void showComparisonTables(RunComparisonResult comparison) {
		final List<RunComparisonTest> tests = comparison.getTests();
		for (final RunComparisonTest runComparisonTest : tests) {
			final ComparisonTableDialog table = new ComparisonTableDialog();
			table.loadTable(runComparisonTest);
			table.setVisible(true);
		}
	}

	private void updateIterativeAnalysis(List<GlycoSite> glycoSites) {
		try {
			// if it is an iterative analysis, we keep the data
			if (iterativeThresholdAnalysis == null) {
				// we clear the glycoSites that are static
				IterationGraphPanel.clearSites();
				iterativeThresholdAnalysis = new IterativeThresholdAnalysis(getIntensityThreshold(),
						Double.valueOf(this.intensityThresholdIntervalTextField.getText()),
						isSumIntensitiesAcrossReplicates());
			}
			if (iterativeThresholdAnalysis.hasErrors()) {
				// stop here
				return;
			}
			// we add new iteration data
			iterativeThresholdAnalysis.addIterationData(getIntensityThreshold(), glycoSites);
			// we launch background process to generate the graph
			final IterationGraphGenerator graphGenerator = new IterationGraphGenerator(
					iterativeThresholdAnalysis.getIterationsData(), glycoSites);
			graphGenerator.addPropertyChangeListener(this);
			graphGenerator.execute();

			// we dont make another iteration if the glycosites are empty or if it the last
			// iteration already
			if (!iterativeThresholdAnalysis.isLastIteration() && !glycoSites.isEmpty()) {
				// then we run it again after incrementing threshold
				this.intensityThresholdText.setText(String.valueOf(iterativeThresholdAnalysis.getNextThreshold()));
				readInputData();
			}
		} catch (final Exception e) {
			e.printStackTrace();
			componentStateKeeper.setToPreviousState(this);
		}
	}

	public void updateControlsWithParametersFromDisk(File resultsFolder) {
		final ResultsProperties resultsProperties = new ResultsProperties(resultsFolder);
		this.dataFileText.setText(resultsProperties.getInputFile().getAbsolutePath());
		if (resultsProperties.getIntensityThreshold() != null) {
			this.intensityThresholdText.setText(String.valueOf(resultsProperties.getIntensityThreshold()));
		} else {
			this.intensityThresholdText.setText("0.0");
		}
		if (resultsProperties.isNormalizeReplicates() != null) {
			this.normalizeIntensityCheckBox.setSelected(resultsProperties.isNormalizeReplicates());
		}
		this.nameTextField.setText(resultsProperties.getName());
		this.proteinOfInterestText.setText(resultsProperties.getProteinOfInterestACC());
		if (resultsProperties.getFastaFile() != null) {
			this.fastaFileText.setText(resultsProperties.getFastaFile().getAbsolutePath());
		} else {
			this.fastaFileText.setText("");
		}
		this.motifRegexp = resultsProperties.getMotifRegexp();
		if (resultsProperties.isDiscardWrongPositionedPTMs() != null) {
			this.discardWrongPositionedPTMsCheckBox.setSelected(resultsProperties.isDiscardWrongPositionedPTMs());
		}
		if (resultsProperties.isDiscardNonUniquePeptides() != null) {
			this.discardNonUniquePeptidesMenuItem.setSelected(resultsProperties.isDiscardNonUniquePeptides());
		}
		if (resultsProperties.isDontAllowConsecutiveMotifs() != null) {
			this.dontAllowConsecutiveMotifsMenuItem.setSelected(resultsProperties.isDontAllowConsecutiveMotifs());
		}
		this.amountType = resultsProperties.getAmountType();
		ProteinSequences.getInstance(getFastaFile(), getMotifRegexp());

	}

	private void showChartsInMainPanel(Object object) {

		showMessage("Showing charts...");
		final ScheduledExecutorService service = new ScheduledThreadPoolExecutor(1);
		final Runnable command = new Runnable() {

			@Override
			public void run() {
				try {
					final GridBagConstraints c = new GridBagConstraints();
					c.fill = GridBagConstraints.HORIZONTAL;
					c.gridx = 0;
					c.gridy = chartsInMainPanel.size() - 1;

					if (object instanceof IterationGraphPanel) {
						chartPanel.removeAll();
						final IterationGraphPanel iterationGraphPanel = (IterationGraphPanel) object;
						c.gridy++;
						c.weighty = 0;
						chartPanel.add(iterationGraphPanel, c);
						iterationGraphPanel.updateUI();
						// remove the charts that were stored because they are from previous iteration
						chartsInMainPanel.clear();
						chartsInMainPanel.add(iterationGraphPanel);

					} else if (object instanceof AbstractMultipleChartsBySitePanel) {
						final AbstractMultipleChartsBySitePanel proportionsChartPanel = (AbstractMultipleChartsBySitePanel) object;
						c.gridy++;
						c.weighty = 100;
						chartPanel.add(proportionsChartPanel, c);
						proportionsChartPanel.updateUI();
						chartsInMainPanel.add(proportionsChartPanel);
					} else if (object instanceof JComponent) {
						// this.jPanelChart.setGraphicPanel((JComponent) object);
						final JComponent jComponent = (JComponent) object;
						c.gridy++;
						c.weighty = 0;
						chartPanel.add(jComponent, c);
						if (jComponent instanceof ChartPanel) {
							final ChartPanel chartPanel = (ChartPanel) jComponent;
							if (!chartsInMainPanel.contains(chartPanel)) {
								chartsInMainPanel.add(chartPanel);
							}
							chartPanel.updateUI();
						} else {
							if (jComponent.getComponent(0) instanceof JComponent) {
								((JComponent) jComponent.getComponent(0)).updateUI();
							}
						}

					} else if (object instanceof List) {
						final List lista = (List) object;

						if (lista.get(0) instanceof JPanel) {
							final List<JPanel> chartList = (List<JPanel>) object;
//							final JPanel panel = new JPanel();
//							panel.setLayout(new GridLayout(chartList.size(), 1, 0, 20));

							for (int i = 0; i < chartList.size(); i++) {
								final JPanel jPanel = chartList.get(i);

								c.gridy++;
								c.weighty = 0;
								if (i == chartList.size() - 1) {
									c.weighty = 100;
								}

								chartPanel.add(jPanel, c);

								if (jPanel instanceof ChartPanel) {
									final ChartPanel chartPanel = (ChartPanel) jPanel;
									if (!chartsInMainPanel.contains(jPanel)) {
										chartsInMainPanel.add(chartPanel);
									}
									chartPanel.updateUI();
								} else if (jPanel instanceof AbstractMultipleChartsBySitePanel) {
									final AbstractMultipleChartsBySitePanel piePanel = (AbstractMultipleChartsBySitePanel) jPanel;
									piePanel.updateUI();
									chartsInMainPanel.add(piePanel);
								} else {
									if (jPanel.getComponent(0) instanceof JComponent) {
										((JComponent) jPanel.getComponent(0)).updateUI();
									}

								}
							}
						}
					}
					showMessage("Charts loaded.");
					chartPanel.validate();
					// enable popup charts
					separateChartsButton.setEnabled(true);
					// enable tables only if it is not iterative analysis
					if (!isIterativeAnalysis()) {
						btnShowResultsTable.setEnabled(true);
						btnShowPeptidesTable.setEnabled(true);
						showProteinSequenceButton.setEnabled(true);
					}
				} catch (final Exception e) {
					e.printStackTrace();
				}
			}
		};

		service.schedule(command, 100, TimeUnit.MILLISECONDS);
//		jScrollPaneChart.getViewport().addChangeListener(new ChangeListener() {
//
//			@Override
//			public void stateChanged(ChangeEvent e) {
//				jScrollPaneChart.revalidate();
//				ChartManagerFrame.this.repaint();
//			}
//		});
	}

	private void showMessage(Object message) {
		showMessage(message.toString(), "");
	}

	private void showMessage(String message, String header) {
		String tmp = "";
		if (!"".equals(header)) {
			tmp = ": ";
		}
		String text = "";
		if (!"".equals(this.statusTextArea.getText())) {
			text = "\n";
		}
		text += getFormattedTime() + ": " + header + tmp + message;
		log.info(text);
		this.statusTextArea.append(text);
		statusTextArea.setCaretPosition(statusTextArea.getText().length() - 1);
	}

	@Override
	public Boolean isSumIntensitiesAcrossReplicates() {
		return this.sumIntensitiesAcrossReplicatesCheckBox.isSelected();
	}

	@Override
	public List<String> getHelpMessages() {
		// TODO
		final String[] ret = { "GlycoMSQuant help", //
				"Here some help.", //
				"<b>Shere some help in bold</b>", //
				"list", //
				"- the <b>chart type</b> that is currently selected,", //
				"- the dataset <b>FDR values</b> at protein, peptide and PSM level, if a <i>FDR filter</i> has applied. Note that although the FDR filter is applied independently in each level 1 node, here you will find the global FDR calculated after applying the threshold defined in the filter and aggregating all the data.", //
				"- the <b>number of PSMs and number of peptides</b> (which depends on the <i>distinguish modified peptides</i> option,", //
				"- the <b>number of protein groups</b>, that will correspond to the number of protein groups at the level 0 of the Comparison Project Tree,", //
				"- the <b>number of Human genes</b>, in case of having recognizable Human proteins (from UniProt), and in brackets, the number of Human genes just counting one per protein group.", //
				"By selecting this option, the <i>General options panel</i> will appear, even if you checked the option for not showing this panel again. Any change in the options of this panel will reload the entire project again." };

		return Arrays.asList(ret);
	}

	/**
	 * 
	 * @param individualResultsFolder folder to one individual results
	 */
	public void loadResultsFromDisk(File individualResultsFolder) {

		this.componentStateKeeper.keepEnableStates(this);
		this.componentStateKeeper.disable(this);
		clearGraphs();
		final ResultLoaderFromDisk resultLoader = new ResultLoaderFromDisk(individualResultsFolder);
		resultLoader.addPropertyChangeListener(this);
		resultLoader.execute();
	}

	public static boolean isChargeStateSensible() {
		return true;
	}

	public static String getDecoyPattern() {
		return "Reverse";
	}

	public static boolean isDistinguishModifiedSequences() {
		return true;
	}

	public static boolean isIgnoreTaxonomies() {
		return true;
	}

	public void clearGraphs() {
		this.chartPanel.removeAll();
		chartsInMainPanel.clear();

	}

	@Override
	public String getMotifRegexp() {
		return this.motifRegexp;
	}

	public void enableRunComparison(boolean b) {
		this.compareRunsButton.setEnabled(b);
	}

	@Override
	public Boolean isDiscardWrongPositionedPTMs() {
		return this.discardWrongPositionedPTMsCheckBox.isSelected();
	}

	@Override
	public Boolean isDiscardNonUniquePeptides() {
		return this.discardNonUniquePeptidesMenuItem.isSelected();
	}

	@Override
	public Boolean isDontAllowConsecutiveMotifs() {
		return this.dontAllowConsecutiveMotifsMenuItem.isSelected();
	}

	public static AppVersion getVersion() {
		if (version == null) {
			try {
				final String tmp = PropertiesUtil.getProperties(new File(APP_PROPERTIES)).getProperty("assembly.dir");
				if (tmp.contains("v")) {
					version = new AppVersion(tmp.split("v")[1]);
				} else {
					version = new AppVersion(tmp);
				}
			} catch (final Exception e) {
				e.printStackTrace();
			}
		}
		return version;

	}
}
