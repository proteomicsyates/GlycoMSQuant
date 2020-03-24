package edu.scripps.yates.glycomsquant.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileFilter;

import org.apache.commons.io.FilenameUtils;

import com.compomics.dbtoolkit.io.implementations.FASTADBLoader;
import com.compomics.util.protein.Protein;

import edu.scripps.yates.glycomsquant.AppDefaults;
import edu.scripps.yates.glycomsquant.HIVPTMAnalyzer;
import edu.scripps.yates.utilities.fasta.FastaParser;
import edu.scripps.yates.utilities.maths.Maths;

public class MainFrame extends JFrame implements PropertyChangeListener {

	private JFileChooser fileChooserFASTA;
	private JTextArea statusTextArea;
	private JFileChooser fileChooserInputFile;
	private String proteinSequence;
	private final static SimpleDateFormat format = new SimpleDateFormat("MM-dd HH:mm:ss.SSS");

	public MainFrame() {
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setTitle("GlycoQuant");
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (UnsupportedLookAndFeelException | ClassNotFoundException | InstantiationException
				| IllegalAccessException e) {
			e.printStackTrace();
		}
		initComponents();
	}

	private void initComponents() {
		JPanel menusPanel = new JPanel();
		getContentPane().add(menusPanel, BorderLayout.NORTH);
		GridBagLayout gbl_menusPanel = new GridBagLayout();
		gbl_menusPanel.columnWidths = new int[] { 10, 0 };
		gbl_menusPanel.rowHeights = new int[] { 10, 0, 0 };
		gbl_menusPanel.columnWeights = new double[] { 1.0, Double.MIN_VALUE };
		gbl_menusPanel.rowWeights = new double[] { 0.0, 1.0, Double.MIN_VALUE };
		menusPanel.setLayout(gbl_menusPanel);

		JPanel inputPanel = new JPanel();
		inputPanel.setBorder(new TitledBorder(
				new EtchedBorder(EtchedBorder.LOWERED, new Color(255, 255, 255), new Color(160, 160, 160)), "Input",
				TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
		GridBagConstraints gbc_inputPanel = new GridBagConstraints();
		gbc_inputPanel.insets = new Insets(0, 0, 5, 0);
		gbc_inputPanel.fill = GridBagConstraints.HORIZONTAL;
		gbc_inputPanel.anchor = GridBagConstraints.NORTHWEST;
		gbc_inputPanel.gridx = 0;
		gbc_inputPanel.gridy = 0;
		menusPanel.add(inputPanel, gbc_inputPanel);
		inputPanel.setLayout(new GridLayout(3, 0, 0, 10));

		JPanel dataFilePanel = new JPanel();
		inputPanel.add(dataFilePanel);
		dataFilePanel.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));

		JLabel lblInputFile = new JLabel("Input file:");
		lblInputFile.setToolTipText("Input file as a text TAB-separated file with census quant compare format");
		dataFilePanel.add(lblInputFile);

		dataFileText = new JTextField();
		dataFileText.setToolTipText(
				"Full path to the input file as a text TAB-separated file with census quant compare format");
		dataFilePanel.add(dataFileText);
		dataFileText.setColumns(80);

		JButton selectInputFileButton = new JButton("Select");
		selectInputFileButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				openSelectInputFileDialog();
			}
		});
		selectInputFileButton.setToolTipText("Click to select file on your file system");
		dataFilePanel.add(selectInputFileButton);

		JPanel fastaFilePanel = new JPanel();
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

		JPanel accessionPanel = new JPanel();
		FlowLayout flowLayout = (FlowLayout) accessionPanel.getLayout();
		flowLayout.setAlignment(FlowLayout.LEFT);
		inputPanel.add(accessionPanel);

		JLabel lblProteinOfInterest = new JLabel("Protein of interest:");
		lblProteinOfInterest.setToolTipText(
				"Accession of the protein of interest. This should be the accession that is present in input file and FASTA file.");
		accessionPanel.add(lblProteinOfInterest);

		proteinOfInterestText = new JTextField(HIVPTMAnalyzer.DEFAULT_PROTEIN_OF_INTEREST);
		proteinOfInterestText.addKeyListener(new KeyListener() {

			@Override
			public void keyTyped(KeyEvent e) {
				// TODO Auto-generated method stub

			}

			@Override
			public void keyReleased(KeyEvent e) {
				updateProteinOfInterestAndRelatedControls();

			}

			@Override
			public void keyPressed(KeyEvent e) {
				// TODO Auto-generated method stub

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

		JPanel analysisPanel = new JPanel();
		analysisPanel.setBorder(
				new TitledBorder(null, "Analysis parameters", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		GridBagConstraints gbc_analysisPanel = new GridBagConstraints();
		gbc_analysisPanel.fill = GridBagConstraints.BOTH;
		gbc_analysisPanel.gridx = 0;
		gbc_analysisPanel.gridy = 1;
		menusPanel.add(analysisPanel, gbc_analysisPanel);
		analysisPanel.setLayout(new GridLayout(2, 0, 0, 0));

		JPanel intensityThresholdPanel = new JPanel();
		FlowLayout flowLayout_1 = (FlowLayout) intensityThresholdPanel.getLayout();
		flowLayout_1.setAlignment(FlowLayout.LEFT);
		analysisPanel.add(intensityThresholdPanel);
		JCheckBox intensityThresholdCheckBox = new JCheckBox("Intensity threshold:");
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

		JPanel normalizeIntensityPanel = new JPanel();
		FlowLayout flowLayout_2 = (FlowLayout) normalizeIntensityPanel.getLayout();
		flowLayout_2.setAlignment(FlowLayout.LEFT);
		analysisPanel.add(normalizeIntensityPanel);
		JCheckBox normalizeIntensityCheckBox = new JCheckBox("Normalize intensities");
		normalizeIntensityCheckBox.setToolTipText(
				"Click to enable or disable the application of the normalization of the intensities in the input data file.");
		normalizeIntensityPanel.add(normalizeIntensityCheckBox);

		JPanel chartPanel = new JPanel();
		chartPanel.setBorder(new TitledBorder(null, "Chart", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		getContentPane().add(chartPanel, BorderLayout.CENTER);

		JPanel statusPanel = new JPanel();
		statusPanel.setBorder(new TitledBorder(null, "Status", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		getContentPane().add(statusPanel, BorderLayout.SOUTH);
		statusPanel.setLayout(new BorderLayout(0, 0));

		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		statusPanel.add(scrollPane, BorderLayout.CENTER);

		statusTextArea = new JTextArea();
		scrollPane.setViewportView(statusTextArea);

		//
		pack();
		final java.awt.Dimension screenSize = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
		final java.awt.Dimension dialogSize = getSize();
		setLocation((screenSize.width - dialogSize.width) / 2, (screenSize.height - dialogSize.height) / 2);
	}

	protected void updateProteinOfInterestAndRelatedControls() {
		boolean b = !proteinOfInterestText.getText().equals(HIVPTMAnalyzer.DEFAULT_PROTEIN_OF_INTEREST);

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
			String previousFastaLocation = AppDefaults.getInstance().getFasta();
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
			File selectedFile = fileChooserFASTA.getSelectedFile();
			if (selectedFile.exists()) {
				AppDefaults.getInstance().setFasta(selectedFile.getAbsolutePath());
				this.fastaFileText.setText(selectedFile.getAbsolutePath());
			} else {
				showError("Error selecting file that doesn't exist: " + selectedFile.getAbsolutePath());
			}
		}
	}

	private void showError(String message) {
		this.statusTextArea.setText(getFormattedTime() + "\t" + this.statusTextArea.getText() + "\n" + message);

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

		JFrame frame = new JFrame("Protein sequence of '" + proteinOfInterest + "'");
		frame.setSize(new Dimension(600, 300));
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
//		frame.setLayout(new BorderLayout(10, 10));

		int rows = 0;
		int i = 0;
		while (i < this.proteinSequence.length()) {
			rows++;
			int endLine = Float.valueOf(Maths.min(i + 80, proteinSequence.length() - 1)).intValue();
			i = endLine + 1;
		}
		JTextArea textArea = new JTextArea(rows + 1, 80);
		JScrollPane scroll = new JScrollPane(textArea);

		frame.add(scroll);// , BorderLayout.CENTER);
		i = 0;
		while (i < this.proteinSequence.length()) {
			int endLine = Float.valueOf(Maths.min(i + 80, proteinSequence.length() - 1)).intValue();
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
			if (proteinOfInterestText.getText().equalsIgnoreCase(HIVPTMAnalyzer.DEFAULT_PROTEIN_OF_INTEREST)) {
				proteinSequence = HIVPTMAnalyzer.DEFAULT_PROTEIN_OF_INTEREST_SEQUENCE;
			} else {
				try {
					FASTADBLoader loader = new FASTADBLoader();
					String fastaPath = this.fastaFileText.getText();
					if (loader.canReadFile(new File(fastaPath))) {
						loader.load(fastaPath);
						Protein protein;
						protein = loader.nextProtein();
						while (protein != null) {
							String acc = FastaParser.getACC(protein.getHeader().getRawHeader()).getAccession();
							if (acc.equalsIgnoreCase(proteinOfInterestText.getText())) {
								proteinSequence = protein.getSequence().getSequence();
								loader.close();
								break;
							}
							protein = loader.nextProtein();
						}
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return proteinSequence;
	}

	protected void openSelectInputFileDialog() {
		if (fileChooserInputFile == null) {
			fileChooserInputFile = new JFileChooser();
			String previousInputFolder = AppDefaults.getInstance().getInputFolder();
			if (previousInputFolder != null) {
				fileChooserInputFile.setCurrentDirectory(new File(previousInputFolder));
			}

			fileChooserInputFile.setFileFilter(new FileFilter() {

				@Override
				public String getDescription() {
					return "txt, tsv files";
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
					return false;
				}
			});
		}
		if (fileChooserInputFile.showDialog(this, "Select FASTA file") == JFileChooser.APPROVE_OPTION) {
			File selectedFile = fileChooserInputFile.getSelectedFile();
			if (selectedFile.exists()) {
				AppDefaults.getInstance().setInputFolder(selectedFile.getAbsolutePath());
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

	public static void main(String[] args) {
		MainFrame frame = new MainFrame();
		frame.setVisible(true);

	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		// TODO Auto-generated method stub

	}
}
