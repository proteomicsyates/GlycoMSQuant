package edu.scripps.yates.glycomsquant.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.SystemColor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileFilter;

import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;
import org.jfree.chart.ChartPanel;
import org.proteored.pacom.utils.ComponentEnableStateKeeper;

import com.compomics.dbtoolkit.io.implementations.FASTADBLoader;
import com.compomics.util.protein.Protein;

import edu.scripps.yates.census.read.model.interfaces.QuantifiedPeptideInterface;
import edu.scripps.yates.glycomsquant.AppDefaults;
import edu.scripps.yates.glycomsquant.CurrentInputParameters;
import edu.scripps.yates.glycomsquant.GlycoPTMAnalyzer;
import edu.scripps.yates.glycomsquant.GlycoPTMPeptideAnalyzer;
import edu.scripps.yates.glycomsquant.GlycoPTMResultGenerator;
import edu.scripps.yates.glycomsquant.GlycoSite;
import edu.scripps.yates.glycomsquant.InputParameters;
import edu.scripps.yates.glycomsquant.QuantCompare2PCQInputTSV;
import edu.scripps.yates.glycomsquant.QuantCompareReader;
import edu.scripps.yates.glycomsquant.gui.files.FileManager;
import edu.scripps.yates.glycomsquant.gui.files.ResultsProperties;
import edu.scripps.yates.glycomsquant.gui.secondary_frame.AbstractJFrameWithAttachedHelpAndAttachedRunsDialog;
import edu.scripps.yates.glycomsquant.gui.tables.results_table.ResultsTableDialog;
import edu.scripps.yates.glycomsquant.gui.tasks.ResultLoaderFromDisk;
import edu.scripps.yates.utilities.fasta.FastaParser;
import edu.scripps.yates.utilities.maths.Maths;
import edu.scripps.yates.utilities.proteomicsmodel.enums.AmountType;
import uk.ac.ebi.pride.utilities.pridemod.ModReader;

public class MainFrame extends AbstractJFrameWithAttachedHelpAndAttachedRunsDialog
		implements PropertyChangeListener, InputParameters {
	private final static Logger log = Logger.getLogger(MainFrame.class);
	private JFileChooser fileChooserFASTA;
	private JTextArea statusTextArea;
	private JFileChooser fileChooserInputFile;
	private String proteinSequence;
	private JPanel chartPanel;
	private final static SimpleDateFormat format = new SimpleDateFormat("yyy-MM-dd HH:mm:ss:SSS");
	private static MainFrame instance;
	private final ComponentEnableStateKeeper componentStateKeeper = new ComponentEnableStateKeeper(true);
	private JButton separateChartsButton;
	private final List<ChartPanel> loadedCharts = new ArrayList<ChartPanel>();
	private final List<JFrame> chartDialogs = new ArrayList<JFrame>();
	private List<GlycoSite> currentGlycoSites;
	private JButton btnShowResultsTable;

	public static MainFrame getInstance() {
		if (instance == null) {
			instance = new MainFrame();
		}
		return instance;
	}

	private MainFrame() {
		super(400);
		setMaximumSize(GuiUtils.getScreenDimension());
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setTitle("GlycoMSQuant");
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (UnsupportedLookAndFeelException | ClassNotFoundException | InstantiationException
				| IllegalAccessException e) {
			e.printStackTrace();
		}
		initComponents();
		loadResources();
	}

	private void loadResources() {
		showMessage("Loading resources...");
		final Thread thread = new Thread(new Runnable() {

			@Override
			public void run() {
				ModReader.getInstance();
				showMessage("Resources loaded.");
			}

		});
		thread.start();

	}

	private void initComponents() {
		final JPanel menusPanel = new JPanel();
		getContentPane().add(menusPanel, BorderLayout.NORTH);
		final GridBagLayout gbl_menusPanel = new GridBagLayout();
		gbl_menusPanel.columnWidths = new int[] { 10, 0 };
		gbl_menusPanel.rowHeights = new int[] { 10, 0, 0 };
		gbl_menusPanel.columnWeights = new double[] { 1.0, Double.MIN_VALUE };
		gbl_menusPanel.rowWeights = new double[] { 0.0, 1.0, Double.MIN_VALUE };
		menusPanel.setLayout(gbl_menusPanel);

		final JPanel inputPanel = new JPanel();
		inputPanel.setBorder(new TitledBorder(null, "Input", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		final GridBagConstraints gbc_inputPanel = new GridBagConstraints();
		gbc_inputPanel.insets = new Insets(0, 0, 5, 0);
		gbc_inputPanel.fill = GridBagConstraints.HORIZONTAL;
		gbc_inputPanel.anchor = GridBagConstraints.NORTHWEST;
		gbc_inputPanel.gridx = 0;
		gbc_inputPanel.gridy = 0;
		menusPanel.add(inputPanel, gbc_inputPanel);
		inputPanel.setLayout(new GridLayout(3, 0, 0, 0));

		final JPanel dataFilePanel = new JPanel();
		inputPanel.add(dataFilePanel);
		dataFilePanel.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 5));

		final JLabel lblInputFile = new JLabel("Input file:");
		lblInputFile.setToolTipText("Input file as a text TAB-separated file with census quant compare format");
		dataFilePanel.add(lblInputFile);

		dataFileText = new JTextField();
		dataFileText.setToolTipText(
				"Full path to the input file as a text TAB-separated file with census quant compare format");
		dataFilePanel.add(dataFileText);
		dataFileText.setColumns(80);
		if (AppDefaults.getInstance().getInputFile() != null) {
			final File previousInputFile = new File(AppDefaults.getInstance().getInputFile());
			if (previousInputFile.exists()) {
				dataFileText.setText(previousInputFile.getAbsolutePath());
			} else if (previousInputFile.getParentFile().exists()) {
				dataFileText.setText(previousInputFile.getParentFile().getAbsolutePath());
			}
		}
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
		inputPanel.add(fastaFilePanel);
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

		final JPanel accessionPanel = new JPanel();
		final FlowLayout flowLayout = (FlowLayout) accessionPanel.getLayout();
		flowLayout.setAlignment(FlowLayout.LEFT);
		inputPanel.add(accessionPanel);

		final JLabel lblProteinOfInterest = new JLabel("Protein of interest:");
		lblProteinOfInterest.setToolTipText(
				"Accession of the protein of interest. This should be the accession that is present in input file and FASTA file.");
		accessionPanel.add(lblProteinOfInterest);

		String proteinOfInterest = GlycoPTMAnalyzer.DEFAULT_PROTEIN_OF_INTEREST;
		if (AppDefaults.getInstance().getProteinOfInterest() != null && !GlycoPTMAnalyzer.DEFAULT_PROTEIN_OF_INTEREST
				.equals(AppDefaults.getInstance().getProteinOfInterest())) {
			proteinOfInterest = AppDefaults.getInstance().getProteinOfInterest();

		}
		proteinOfInterestText = new JTextField(proteinOfInterest);
		if (AppDefaults.getInstance().getProteinOfInterest() != null && !GlycoPTMAnalyzer.DEFAULT_PROTEIN_OF_INTEREST
				.equals(AppDefaults.getInstance().getProteinOfInterest())) {
			if (AppDefaults.getInstance().getFasta() != null) {
				fastaFileText.setText(AppDefaults.getInstance().getFasta());
				updateProteinOfInterestAndRelatedControls();
			}
		}
		proteinOfInterestText.addKeyListener(new KeyListener() {

			@Override
			public void keyTyped(KeyEvent e) {

			}

			@Override
			public void keyReleased(KeyEvent e) {
				proteinSequence = null;
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

		showProteinSequenceButton = new JButton("Show protein sequence");
		showProteinSequenceButton.setToolTipText("Click to visualize the protein of interest sequence");
		showProteinSequenceButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				showSequenceDialog(proteinOfInterestText.getText());
			}
		});
		accessionPanel.add(showProteinSequenceButton);

		final JPanel analysisPanel = new JPanel();
		analysisPanel.setBorder(null);
		final GridBagConstraints gbc_analysisPanel = new GridBagConstraints();
		gbc_analysisPanel.anchor = GridBagConstraints.NORTH;
		gbc_analysisPanel.fill = GridBagConstraints.HORIZONTAL;
		gbc_analysisPanel.gridx = 0;
		gbc_analysisPanel.gridy = 1;
		menusPanel.add(analysisPanel, gbc_analysisPanel);
		analysisPanel.setLayout(new GridLayout(0, 3, 0, 0));

		final JPanel analysisParametersPanel = new JPanel();
		analysisParametersPanel.setBorder(
				new TitledBorder(null, "Analysis parameters", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		analysisPanel.add(analysisParametersPanel);
		analysisParametersPanel.setLayout(new GridLayout(0, 1, 0, 0));

		final JPanel intensityThresholdPanel = new JPanel();
		analysisParametersPanel.add(intensityThresholdPanel);
		final FlowLayout flowLayout_1 = (FlowLayout) intensityThresholdPanel.getLayout();
		flowLayout_1.setAlignment(FlowLayout.LEFT);
		final JCheckBox intensityThresholdCheckBox = new JCheckBox("Intensity threshold:");
		intensityThresholdCheckBox.setToolTipText(
				"Click to enable or disable the application of intensity threshold over the intensities in the input data file.");
		intensityThresholdCheckBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				intensityThresholdText.setEnabled(intensityThresholdCheckBox.isSelected());
			}
		});
		intensityThresholdPanel.add(intensityThresholdCheckBox);

		intensityThresholdText = new JTextField();
		intensityThresholdText.setEnabled(false);
		intensityThresholdText.setToolTipText(
				"If enabled and > 0.0, an intensity threshold will be applied over the intensities in the input data file. Any peptide with an intensity below this value, will be discarded.");
		intensityThresholdPanel.add(intensityThresholdText);
		intensityThresholdText.setColumns(10);

		final JPanel normalizeIntensityPanel = new JPanel();
		analysisParametersPanel.add(normalizeIntensityPanel);
		final FlowLayout flowLayout_2 = (FlowLayout) normalizeIntensityPanel.getLayout();
		flowLayout_2.setAlignment(FlowLayout.LEFT);
		normalizeIntensityCheckBox = new JCheckBox("Normalize replicates");
		normalizeIntensityCheckBox.setToolTipText(
				"Click to enable or disable the application of the normalization of the intensities in the input data file by replicates.");
		normalizeIntensityPanel.add(normalizeIntensityCheckBox);

		final JPanel analysisPerPeptidePanel = new JPanel();
		final FlowLayout flowLayout_3 = (FlowLayout) analysisPerPeptidePanel.getLayout();
		flowLayout_3.setAlignment(FlowLayout.LEFT);
		analysisParametersPanel.add(analysisPerPeptidePanel);

		calculateProportionsByPeptidesFirstCheckBox = new JCheckBox("Calculate proportions per peptide");
		calculateProportionsByPeptidesFirstCheckBox.setToolTipText(
				"If selected, the % of aboundances are calculated by peptide independently and then aggregated together.");
		calculateProportionsByPeptidesFirstCheckBox.setSelected(true);
		analysisPerPeptidePanel.add(calculateProportionsByPeptidesFirstCheckBox);

		final JPanel outputPanel = new JPanel();
		analysisPanel.add(outputPanel);
		outputPanel.setBorder(new TitledBorder(null, "Output", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		outputPanel.setLayout(new GridLayout(3, 1, 0, 0));
		final JPanel namePanel = new JPanel();
		final FlowLayout flowLayout_4 = (FlowLayout) namePanel.getLayout();
		flowLayout_4.setAlignment(FlowLayout.LEFT);
		outputPanel.add(namePanel);

		final JLabel lblNameForOutput = new JLabel("Name for output files:");
		lblNameForOutput.setToolTipText("Prefix that will be added to all the output files");
		namePanel.add(lblNameForOutput);

		nameTextField = new JTextField();
		if (AppDefaults.getInstance().getRunName() != null) {
			nameTextField.setText(AppDefaults.getInstance().getRunName());
		}
		nameTextField.setToolTipText("Prefix that will be added to all the output files");
		namePanel.add(nameTextField);
		nameTextField.setColumns(10);
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
		separateChartsButton = new JButton("Pop-up charts");
		separateChartsPanel.add(separateChartsButton);
		separateChartsButton.setEnabled(false);
		separateChartsButton.setToolTipText("Click to show graphs in separate resizable dialogs");
		outputPanel.add(separateChartsPanel);
		separateChartsButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if (separateChartsButton.getText().equals("Pop-up charts")) {
					separateCharts(true);
					separateChartsButton.setText("Close charts");
				} else {
					separateCharts(false);
					separateChartsButton.setText("Pop-up charts");
				}

			}
		});
		final JPanel showResultsPanel = new JPanel();
		final FlowLayout flowLayout_6 = (FlowLayout) showResultsPanel.getLayout();
		flowLayout_6.setAlignment(FlowLayout.LEFT);
		btnShowResultsTable = new JButton("Show results table");
		btnShowResultsTable.setToolTipText("Click to open a table with all the results of the analysis");
		btnShowResultsTable.setEnabled(false);
		btnShowResultsTable.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				showResultsTableDialog();
			}
		});
		showResultsPanel.add(btnShowResultsTable);
		outputPanel.add(showResultsPanel);

		final JPanel startButtonPanel = new JPanel();
		startButtonPanel
				.setBorder(new TitledBorder(null, "Control", TitledBorder.CENTER, TitledBorder.TOP, null, null));
		analysisPanel.add(startButtonPanel);

		final JButton startButton = new JButton("START");
		startButton.setFont(new Font("Tahoma", Font.BOLD, 14));
		startButton.setForeground(SystemColor.desktop);
		startButton.setAlignmentX(Component.CENTER_ALIGNMENT);
		startButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				startAnalysis();
			}
		});
		final FlowLayout fl_startButtonPanel = new FlowLayout(FlowLayout.CENTER, 5, 5);
		startButtonPanel.setLayout(fl_startButtonPanel);
		startButton.setToolTipText("Click to start the analysis");
		startButtonPanel.add(startButton);

		final JButton btnShowRuns = new JButton("Show Runs");
		btnShowRuns.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				showRunsDialog();
			}
		});
		startButtonPanel.add(btnShowRuns);

		final JButton btnRefreshRunsFolder = new JButton("Refresh runs");
		btnRefreshRunsFolder.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				refreshRuns();
			}
		});
		startButtonPanel.add(btnRefreshRunsFolder);

		final JPanel panelForCharts = new JPanel();
		panelForCharts.setPreferredSize(new Dimension(10, 400));
		panelForCharts.setBorder(new TitledBorder(
				new EtchedBorder(EtchedBorder.LOWERED, new Color(255, 255, 255), new Color(160, 160, 160)), "Charts",
				TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
		getContentPane().add(panelForCharts, BorderLayout.CENTER);
		panelForCharts.setLayout(new BorderLayout(0, 0));

		chartPanelScroll = new JScrollPane();
		chartPanelScroll.setBorder(null);
		chartPanelScroll.getVerticalScrollBar().setUnitIncrement(16);
		this.chartPanel = new JPanel();
		chartPanelScroll.setViewportView(chartPanel);
		chartPanel.setLayout(new BorderLayout(0, 0));
		panelForCharts.add(chartPanelScroll);

		final JPanel statusPanel = new JPanel();
		statusPanel.setBorder(new TitledBorder(null, "Status", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		getContentPane().add(statusPanel, BorderLayout.SOUTH);
		statusPanel.setLayout(new BorderLayout(0, 0));

		final JScrollPane scrollPane = new JScrollPane();
		scrollPane.setToolTipText("Status");
		statusPanel.add(scrollPane, BorderLayout.CENTER);

		statusTextArea = new JTextArea();
		statusTextArea.setToolTipText("Status");
		statusTextArea.setWrapStyleWord(true);
		statusTextArea.setFont(new Font("Tahoma", Font.PLAIN, 11));
		statusTextArea.setRows(5);
		scrollPane.setViewportView(statusTextArea);

		//
		pack();
		final java.awt.Dimension screenSize = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
		final java.awt.Dimension dialogSize = getSize();
		setLocation((screenSize.width - dialogSize.width) / 2, (screenSize.height - dialogSize.height) / 2);
	}

	protected void showResultsTableDialog() {
		if (currentGlycoSites == null || currentGlycoSites.isEmpty()) {
			showError("Start a new analysis or load a GlycoMSQuant run to load its resuls.");
			return;
		}
		final ResultsTableDialog tableDialog = new ResultsTableDialog();
		tableDialog.loadResultTable(currentGlycoSites, isCalculateProportionsByPeptidesFirst());
		tableDialog.setVisible(true);
	}

	protected void separateCharts(boolean separate) {
		if (separate) {
			for (final ChartPanel chartPanel : loadedCharts) {
				showChartDialog(chartPanel);
			}
		} else {
			for (final JFrame jframe : chartDialogs) {
				jframe.dispose();
			}

			showChartsInPanel(loadedCharts);
		}
	}

	private void showChartDialog(ChartPanel chartPanel) {
		final java.awt.Dimension screenSize = java.awt.Toolkit.getDefaultToolkit().getScreenSize();

		final JFrame frame = new JFrame(chartPanel.getChart().getTitle().getText());
		chartDialogs.add(frame);
		frame.setPreferredSize(new Dimension(screenSize.width / 2, screenSize.height / 2));
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame.getContentPane().setLayout(new BorderLayout(10, 10));

		final JScrollPane scroll = new JScrollPane(chartPanel);

		frame.getContentPane().add(scroll, BorderLayout.CENTER);
		frame.setVisible(true);
		frame.pack();

		final java.awt.Dimension dialogSize = frame.getSize();
		frame.setLocation((screenSize.width - dialogSize.width) / 2, (screenSize.height - dialogSize.height) / 2);
	}

	protected void refreshRuns() {

		getRunsAttachedDialog().loadResultFolders();
	}

	protected void showRunsDialog() {
//		super.showAttachedHelpDialog();
		super.showAttachedRunsDialog();

	}

	protected void startAnalysis() {
		// keep enable/disable states
		this.componentStateKeeper.keepEnableStates(this);
		this.componentStateKeeper.disable(this);
		clearGraphs();
		showMessage("Starting analysis...");
		// clear status
		this.statusTextArea.setText(null);
		readInputData();

	}

	protected void updateProteinOfInterestAndRelatedControls() {
		final boolean b = !proteinOfInterestText.getText().equals(GlycoPTMAnalyzer.DEFAULT_PROTEIN_OF_INTEREST);

		fastaFileText.setEnabled(b);
		lblFastaFile.setEnabled(b);
		selectFastaFileButton.setEnabled(b);
		if (!b) {// if it is not the default one
			// we check if the fasta is provided
			if ("".equals(fastaFileText.getText()) || !new File(fastaFileText.getText()).exists()) {
				showProteinSequenceButton.setEnabled(false);
			} else if (new File(fastaFileText.getText()).exists()) {
				showProteinSequenceButton.setEnabled(true);
			}
		}

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
			} else {
				showError("Error selecting file that doesn't exist: " + selectedFile.getAbsolutePath());
			}
		}
	}

	private void showError(String message) {
		showMessage(message, "ERROR");

	}

	private String getFormattedTime() {
		return format.format(new Date());

	}

	protected void showSequenceDialog(String proteinOfInterest) {
		getProteinSequence();
		if (proteinSequence == null) {
			showError("Error getting protein sequence from protein '" + proteinOfInterest + "'");
			return;
		}
		final java.awt.Dimension screenSize = java.awt.Toolkit.getDefaultToolkit().getScreenSize();

		final JFrame frame = new JFrame("Protein sequence of '" + proteinOfInterest + "'");

		frame.setPreferredSize(new Dimension(screenSize.width / 2, screenSize.height / 2));
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame.getContentPane().setLayout(new BorderLayout(10, 10));

		int rows = 0;
		int i = 0;
		while (i < this.proteinSequence.length()) {
			rows++;
			final int endLine = Float.valueOf(Maths.min(i + 80, proteinSequence.length() - 1)).intValue();
			i = endLine + 1;
		}
		final JTextArea textArea = new JTextArea(rows + 1, 80);
		final JScrollPane scroll = new JScrollPane(textArea);

		frame.getContentPane().add(scroll, BorderLayout.CENTER);
		i = 0;
		while (i < this.proteinSequence.length()) {
			final int endLine = Float.valueOf(Maths.min(i + 80, proteinSequence.length() - 1)).intValue();
			textArea.append("\n" + proteinSequence.substring(i, endLine));
			i = endLine + 1;
		}
		frame.setVisible(true);
		frame.pack();

		final java.awt.Dimension dialogSize = frame.getSize();
		frame.setLocation((screenSize.width - dialogSize.width) / 2, (screenSize.height - dialogSize.height) / 2);
	}

	private String getProteinSequence() {
		if (proteinSequence == null) {
			if (proteinOfInterestText.getText().equalsIgnoreCase(GlycoPTMAnalyzer.DEFAULT_PROTEIN_OF_INTEREST)) {
				proteinSequence = GlycoPTMAnalyzer.DEFAULT_PROTEIN_OF_INTEREST_SEQUENCE;
			} else {
				try {
					final FASTADBLoader loader = new FASTADBLoader();
					final String fastaPath = this.fastaFileText.getText();
					if (loader.canReadFile(new File(fastaPath))) {
						loader.load(fastaPath);
						Protein protein;
						protein = loader.nextProtein();
						while (protein != null) {
							final String acc = FastaParser.getACC(protein.getHeader().getRawHeader()).getAccession();
							if (acc.equalsIgnoreCase(proteinOfInterestText.getText())) {
								proteinSequence = protein.getSequence().getSequence();
								loader.close();
								break;
							}
							protein = loader.nextProtein();
						}
					}
				} catch (final IOException e) {
					e.printStackTrace();
				}
			}
		}
		return proteinSequence;
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
	private JButton startButton;
	private JTextField nameTextField;
	private JCheckBox normalizeIntensityCheckBox;
	private JScrollPane chartPanelScroll;
	private JCheckBox calculateProportionsByPeptidesFirstCheckBox;
	private QuantCompareReader inputDataReader;

	public static void main(String[] args) {
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
			if (fastaFileText.isEnabled()) {
				return new File(this.fastaFileText.getText());
			}
		}
		if (getProteinOfInterestACC().equals(GlycoPTMAnalyzer.DEFAULT_PROTEIN_OF_INTEREST)) {
			final String name = GlycoPTMAnalyzer.DEFAULT_PROTEIN_OF_INTEREST + ".fasta";

			final File targetFile = new File(System.getProperty("user.dir") + File.separator + name);

			return targetFile;

		}
		return null;
	}

	@Override
	public double getFakePTM() {
		return QuantCompare2PCQInputTSV.DEFAULT_FAKE_PTM;
	}

	@Override
	public String getName() {

		return this.nameTextField.getText();
	}

	@Override
	public double getIntensityThreshold() {
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
		return AmountType.INTENSITY;
	}

	@Override
	public boolean isNormalizeReplicates() {
		return normalizeIntensityCheckBox.isSelected();
	}

	private void readInputData() {
		final File inputFile = getInputFile();
		final String proteinOfInterestACC = getProteinOfInterestACC();
		final double fakePTM = getFakePTM();
		final double intensityThreshold = getIntensityThreshold();
		final AmountType amountType = getAmountType();
		final boolean normalizeExperimentsByProtein = isNormalizeReplicates();

		log.info("Reading input file '" + inputFile.getAbsolutePath() + "'...");
		inputDataReader = new QuantCompareReader(inputFile, proteinOfInterestACC, fakePTM, intensityThreshold,
				amountType, normalizeExperimentsByProtein);
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

		final ResultsProperties resultsProperties = ResultsProperties.getResultsProperties(resultsFolder);
		resultsProperties.setName(getName());
		resultsProperties.setInputDataFile(getInputFile());
		resultsProperties.setIntensityThreshold(getIntensityThreshold());
		resultsProperties.setNormalizeReplicates(isNormalizeReplicates());
		resultsProperties.setProteinOfInterest(getProteinOfInterestACC());
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		if (evt.getPropertyName().equals("progress")) {
			showMessage(evt.getNewValue());
		} else if (evt.getPropertyName().equals(QuantCompareReader.INPUT_DATA_READER_FINISHED)) {
			final List<QuantifiedPeptideInterface> peptides = (List<QuantifiedPeptideInterface>) evt.getNewValue();
			if (peptides != null && !peptides.isEmpty()) {
				showMessage(peptides.size() + " peptides read from input file ");
				log.info("Analyzing peptides...");
				final GlycoPTMPeptideAnalyzer peptideAnalyzer = new GlycoPTMPeptideAnalyzer(peptides,
						getProteinOfInterestACC(), getFastaFile(), getAmountType());
				peptideAnalyzer.addPropertyChangeListener(this);
				peptideAnalyzer.execute();
			} else {
				showError("Some error occurred because no peptides were read from input file.");
			}
		} else if (evt.getPropertyName().equals(QuantCompareReader.INPUT_DATA_READER_ERROR)) {
			showError(evt.getNewValue().toString());
			this.componentStateKeeper.setToPreviousState(this);
		} else if (evt.getPropertyName().equals(QuantCompareReader.INPUT_DATA_READER_START)) {
			showMessage("Reading input data...");
		} else if (evt.getPropertyName().equals(QuantCompareReader.NUM_VALID_PEPTIDES)) {
//			peptidesValid = (int) evt.getNewValue();
		} else if (evt.getPropertyName().equals(GlycoPTMPeptideAnalyzer.HIVPOSITIONS_CALCULATED)) {
			currentGlycoSites = (List<GlycoSite>) evt.getNewValue();
			showMessage("Analysis resulted in " + currentGlycoSites.size() + " positions in protein '"
					+ getProteinOfInterestACC() + "'");
			final File newIndividualResultFolder = FileManager.getNewIndividualResultFolder();
			// now that we have the new results folder, we can update the properties
			updateProperties(newIndividualResultFolder);
			final ResultsProperties resultsProperties = ResultsProperties
					.getResultsProperties(newIndividualResultFolder);
			resultsProperties.setFastaFile(getFastaFile());

			final GlycoPTMResultGenerator resultGenerator = new GlycoPTMResultGenerator(newIndividualResultFolder,
					getName(), currentGlycoSites, isCalculateProportionsByPeptidesFirst());
			resultGenerator.setGenerateGraph(true);
			resultGenerator.setGenerateTable(true);
			resultGenerator.setSaveGraphsToFiles(true);
			resultGenerator.addPropertyChangeListener(this);
			resultGenerator.execute();
		} else if (evt.getPropertyName().equals(GlycoPTMResultGenerator.RESULTS_GENERATOR_FINISHED)) {
			refreshRuns();
			this.componentStateKeeper.setToPreviousState(this);
		} else if (evt.getPropertyName().equals(GlycoPTMResultGenerator.RESULS_TABLE_GENERATED)) {
			final File tableFile = (File) evt.getNewValue();
			showMessage("Result table created at '" + tableFile.getAbsolutePath() + "'");
		} else if (evt.getPropertyName().equals(GlycoPTMResultGenerator.GLYCO_SITE_DATA_TABLE_GENERATED)) {
			final File file = (File) evt.getNewValue();
			showMessage("Glyco-sites data file create at: '" + file.getAbsolutePath() + "'");
		} else if (evt.getPropertyName().equals(GlycoPTMResultGenerator.CHART_GENERATED)) {
			final Object object = evt.getNewValue();
			showChartsInPanel(object);

		} else if (evt.getPropertyName().equals(GlycoPTMResultGenerator.RESULTS_GENERATOR_ERROR)) {
			showError(evt.getNewValue().toString());
			this.componentStateKeeper.setToPreviousState(this);
		} else if (evt.getPropertyName().equals(ResultLoaderFromDisk.RESULT_LOADER_FROM_DISK_STARTED)) {
			final File resultsFolder = (File) evt.getNewValue();
			updateControlsWithParametersFromDisk(resultsFolder);
			showMessage("Loading results from " + FilenameUtils.getName(resultsFolder.getAbsolutePath()) + "...");
		} else if (evt.getPropertyName().equals(ResultLoaderFromDisk.RESULT_LOADER_FROM_DISK_ERROR)) {
			showMessage("Error loading results: " + evt.getNewValue());
			this.componentStateKeeper.setToPreviousState(this);
		} else if (evt.getPropertyName().equals(ResultLoaderFromDisk.RESULT_LOADER_FROM_DISK_FINISHED)) {
			this.currentGlycoSites = (List<GlycoSite>) evt.getNewValue();
			final GlycoPTMResultGenerator resultGenerator = new GlycoPTMResultGenerator(currentGlycoSites);
			resultGenerator.addPropertyChangeListener(this);
			resultGenerator.setGenerateGraph(true);
			resultGenerator.setGenerateTable(false);
			resultGenerator.setSaveGraphsToFiles(false);
			resultGenerator.execute();
			showMessage("Generating graphs for the analysis of " + currentGlycoSites.size() + " glyco sites.");
			this.componentStateKeeper.setToPreviousState(this);
		}
	}

	public void updateControlsWithParametersFromDisk(File resultsFolder) {
		final ResultsProperties resultsProperties = ResultsProperties.getResultsProperties(resultsFolder);
		this.dataFileText.setText(resultsProperties.getInputDataFile().getAbsolutePath());
		this.intensityThresholdText.setText(String.valueOf(resultsProperties.getIntensityThreshold()));
		this.normalizeIntensityCheckBox.setSelected(resultsProperties.getNormalizeReplicates());
		this.nameTextField.setText(resultsProperties.getName());
		this.proteinOfInterestText.setText(resultsProperties.getProteinOfInterest());
		if (resultsProperties.getFastaFile() != null) {
			this.fastaFileText.setText(resultsProperties.getFastaFile().getAbsolutePath());
		}
	}

	private void showChartsInPanel(Object object) {

		showMessage("Showing charts...");
		final ScheduledExecutorService service = new ScheduledThreadPoolExecutor(1);
		final Runnable command = new Runnable() {

			@Override
			public void run() {
				try {
					separateChartsButton.setEnabled(true);
					btnShowResultsTable.setEnabled(true);
					if (object instanceof JComponent) {
						// this.jPanelChart.setGraphicPanel((JComponent) object);
						final JComponent jComponent = (JComponent) object;
						chartPanel.add(jComponent, BorderLayout.CENTER);
						if (jComponent instanceof ChartPanel) {
							final ChartPanel chartPanel = (ChartPanel) jComponent;
							if (!loadedCharts.contains(jComponent)) {
								loadedCharts.add(chartPanel);
							}
							chartPanel.updateUI();
						} else {
							if (jComponent.getComponent(0) instanceof JComponent) {
								((JComponent) jComponent.getComponent(0)).updateUI();
							}
						}

					} else if (object instanceof List) {
						final List lista = (List) object;
						if (lista.isEmpty()) {
							log.info(lista);
							return;
						}
						if (lista.get(0) instanceof JPanel) {
							final List<JPanel> chartList = (List<JPanel>) object;
							final JPanel panel = new JPanel();
							panel.setLayout(new GridLayout(chartList.size(), 1, 0, 20));
							chartPanel.add(panel, BorderLayout.CENTER);
							for (final JPanel jPanel : chartList) {

								panel.add(jPanel);
								if (jPanel instanceof ChartPanel) {
									final ChartPanel chartPanel = (ChartPanel) jPanel;
									if (!loadedCharts.contains(jPanel)) {
										loadedCharts.add(chartPanel);
									}
									chartPanel.updateUI();
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
	public boolean isCalculateProportionsByPeptidesFirst() {
		return this.calculateProportionsByPeptidesFirstCheckBox.isSelected();
	}

	@Override
	public List<String> getHelpMessages() {
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

	public boolean isChargeStateSensible() {
		return true;
	}

	public String getDecoyPattern() {
		return "Reverse";
	}

	public boolean isDistinguishModifiedSequences() {
		return true;
	}

	public boolean isIgnoreTaxonomies() {
		return true;
	}

	public void clearGraphs() {
		this.chartPanel.removeAll();
		loadedCharts.clear();

	}
}
