package edu.scripps.yates.glycomsquant.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.LineBorder;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.CategoryPlot;

import edu.scripps.yates.census.read.model.interfaces.QuantifiedPeptideInterface;
import edu.scripps.yates.glycomsquant.GlycoSite;
import edu.scripps.yates.glycomsquant.GroupedQuantifiedPeptide;
import edu.scripps.yates.glycomsquant.ProteinSequences;
import edu.scripps.yates.glycomsquant.gui.attached_frame.AbstractJFrameWithAttachedHelpAndAttachedPeptideListDialog;
import edu.scripps.yates.glycomsquant.gui.charts.ChartUtils;
import edu.scripps.yates.glycomsquant.gui.charts.ErrorType;
import edu.scripps.yates.glycomsquant.gui.tables.grouped_peptides.MyGroupedPeptidesTable;
import edu.scripps.yates.glycomsquant.util.GlycoPTMAnalyzerUtil;
import edu.scripps.yates.glycomsquant.util.GuiUtils;
import edu.scripps.yates.utilities.maths.Maths;
import edu.scripps.yates.utilities.strings.StringUtils;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.THashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.set.hash.THashSet;

public class ProteinSequenceDialog extends AbstractJFrameWithAttachedHelpAndAttachedPeptideListDialog {
	/**
	 * 
	 */
	private static final long serialVersionUID = -7470139520024826767L;
	private static final Color GLYCO_SITE_BACKGROUND_COLOR = Color.yellow;
	private static final Color NON_COVERED_BACKGROUD = Color.white;
	private static final Color COVERED_BACKGROUND = new Color(219, 219, 189);
	private final String proteinSequence;
	private final TIntObjectMap<GlycoSite> glycoSitesByPosition = new TIntObjectHashMap<GlycoSite>();
	private static final int proteinSequenceLineLength = 80;
	private final List<GroupedQuantifiedPeptide> groupedPeptides = new ArrayList<GroupedQuantifiedPeptide>();
	private TIntList peptideOccupancyArray;
	private final List<GlycoSite> glycoSites;
	private TIntObjectHashMap<JLabel> labelsByPositions;
	private TObjectIntHashMap<JLabel> positionsByLabels;

	private int selectedAminoacidPosition = -1;
	private final TIntObjectHashMap<Set<GroupedQuantifiedPeptide>> peptidesByPositionsInProtein = new TIntObjectHashMap<Set<GroupedQuantifiedPeptide>>();
	private final JPanel proteinSequencePanel;
	private final JLabel proteinNameLabel;
	private final JLabel numberOfPeptidesLabel;
	private final JLabel proteinCoverageLabel;
	private final JLabel selectedPositionLabel;
	private final JPanel graphsPanel;
	private final boolean sumIntensitiesAcrossReplicates;
	public final THashMap<JLabel, Color> defaultColorsByAminoacidLabels = new THashMap<JLabel, Color>();
	private final String currentProteinAcc;

	public ProteinSequenceDialog(String proteinOfInterest, String proteinSequence, List<GlycoSite> glycoSites,
			List<QuantifiedPeptideInterface> peptides, boolean sumIntensitiesAcrossReplicates) {
		super(GuiUtils.getFractionOfScreenWidthSize(0.4));
		this.currentProteinAcc = proteinOfInterest;
		setTitle("Protein sequence of '" + proteinOfInterest + "'");
		this.proteinSequence = proteinSequence;

		this.glycoSites = glycoSites;

		// we create groupedPeptides not linked to any position
		final Map<String, GroupedQuantifiedPeptide> groupedPeptideByKey = GlycoPTMAnalyzerUtil
				.getGroupedPeptidesFromPeptides(peptides, proteinOfInterest);

		// add to this.groupedPeptides
		this.groupedPeptides.addAll(groupedPeptideByKey.values());

		//
		this.sumIntensitiesAcrossReplicates = sumIntensitiesAcrossReplicates;
		glycoSites.stream().forEach(g -> glycoSitesByPosition.put(g.getPosition(), g));
		// the width will be the size of a label * proteinSequenceLineLength + 50
		final JLabel tempLabel = new JLabel("A");
		tempLabel.setFont(GuiUtils.aminoacidLabelFont);
		final double labelWidth = tempLabel.getPreferredSize().getWidth();
		LineBorder border = (LineBorder) tempLabel.getBorder();
		GuiUtils.setAminoacidBorder(tempLabel, Color.white);
		border = (LineBorder) tempLabel.getBorder();
		final int thickness = border.getThickness();
		final int width = Double.valueOf((labelWidth + (thickness * 2)) * proteinSequenceLineLength + 50).intValue();
		setPreferredSize(new Dimension(width, GuiUtils.getFractionOfScreenHeightSize(0.9)));
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		getContentPane().setLayout(new BorderLayout(10, 10));

		final JPanel mainPanel = new JPanel();
		final JScrollPane scroll = new JScrollPane(mainPanel);
		scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		mainPanel.setLayout(new BorderLayout(0, 0));

		final JPanel panelSequence = new JPanel();
		panelSequence.setBackground(Color.WHITE);
		mainPanel.add(panelSequence, BorderLayout.NORTH);
		final GridBagLayout gbl_panelSequence = new GridBagLayout();
		gbl_panelSequence.columnWidths = new int[] { 0 };
		gbl_panelSequence.rowHeights = new int[] { 0 };
		gbl_panelSequence.columnWeights = new double[] { 0.0 };
		gbl_panelSequence.rowWeights = new double[] { 0.0 };
		panelSequence.setLayout(gbl_panelSequence);

		proteinSequencePanel = new JPanel();
		proteinSequencePanel.setBackground(Color.WHITE);
		final GridBagConstraints gbc_proteinSequencePanel = new GridBagConstraints();
		gbc_proteinSequencePanel.insets = new Insets(20, 5, 20, 5);
		gbc_proteinSequencePanel.gridx = 0;
		gbc_proteinSequencePanel.gridy = 0;
		panelSequence.add(proteinSequencePanel, gbc_proteinSequencePanel);
		final GridBagLayout gbl_proteinSequencePanel = new GridBagLayout();
		gbl_proteinSequencePanel.columnWidths = new int[] { 0 };
		gbl_proteinSequencePanel.rowHeights = new int[] { 0 };
		gbl_proteinSequencePanel.columnWeights = new double[] { Double.MIN_VALUE };
		gbl_proteinSequencePanel.rowWeights = new double[] { Double.MIN_VALUE };
		proteinSequencePanel.setLayout(gbl_proteinSequencePanel);

		final JPanel southPanel = new JPanel();
		mainPanel.add(southPanel, BorderLayout.CENTER);
		southPanel.setLayout(new BorderLayout(0, 0));

		final JPanel selectionInformationPanel = new JPanel();
		southPanel.add(selectionInformationPanel, BorderLayout.NORTH);
		final GridBagLayout gbl_selectionInformationPanel = new GridBagLayout();
		gbl_selectionInformationPanel.columnWidths = new int[] { 0, 0, 0 };
		gbl_selectionInformationPanel.rowHeights = new int[] { 0, 0, 0, 0, 0 };
		gbl_selectionInformationPanel.columnWeights = new double[] { 0.0, 0.0, Double.MIN_VALUE };
		gbl_selectionInformationPanel.rowWeights = new double[] { 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE };
		selectionInformationPanel.setLayout(gbl_selectionInformationPanel);

		final JLabel lblNewLabel_1 = new JLabel("Number of peptides:");
		final GridBagConstraints gbc_lblNewLabel_1 = new GridBagConstraints();
		gbc_lblNewLabel_1.anchor = GridBagConstraints.EAST;
		gbc_lblNewLabel_1.insets = new Insets(0, 10, 5, 5);
		gbc_lblNewLabel_1.gridx = 0;
		gbc_lblNewLabel_1.gridy = 1;
		selectionInformationPanel.add(lblNewLabel_1, gbc_lblNewLabel_1);

		numberOfPeptidesLabel = new JLabel("0");
		final GridBagConstraints gbc_numberOfPeptidesLabel = new GridBagConstraints();
		gbc_numberOfPeptidesLabel.anchor = GridBagConstraints.WEST;
		gbc_numberOfPeptidesLabel.insets = new Insets(0, 0, 5, 0);
		gbc_numberOfPeptidesLabel.gridx = 1;
		gbc_numberOfPeptidesLabel.gridy = 1;
		selectionInformationPanel.add(numberOfPeptidesLabel, gbc_numberOfPeptidesLabel);

		final JLabel lblNewLabel_2 = new JLabel("Protein coverage:");
		final GridBagConstraints gbc_lblNewLabel_2 = new GridBagConstraints();
		gbc_lblNewLabel_2.anchor = GridBagConstraints.EAST;
		gbc_lblNewLabel_2.insets = new Insets(0, 10, 5, 5);
		gbc_lblNewLabel_2.gridx = 0;
		gbc_lblNewLabel_2.gridy = 2;
		selectionInformationPanel.add(lblNewLabel_2, gbc_lblNewLabel_2);

		final JLabel lblNewLabel_3 = new JLabel("Protein:");
		final GridBagConstraints gbc_lblNewLabel_3 = new GridBagConstraints();
		gbc_lblNewLabel_3.anchor = GridBagConstraints.SOUTHEAST;
		gbc_lblNewLabel_3.insets = new Insets(10, 10, 5, 5);
		gbc_lblNewLabel_3.gridx = 0;
		gbc_lblNewLabel_3.gridy = 0;
		selectionInformationPanel.add(lblNewLabel_3, gbc_lblNewLabel_3);

		proteinNameLabel = new JLabel(proteinOfInterest);
		final GridBagConstraints gbc_proteinNameLabel = new GridBagConstraints();
		gbc_proteinNameLabel.anchor = GridBagConstraints.SOUTHWEST;
		gbc_proteinNameLabel.insets = new Insets(0, 0, 5, 0);
		gbc_proteinNameLabel.gridx = 1;
		gbc_proteinNameLabel.gridy = 0;
		selectionInformationPanel.add(proteinNameLabel, gbc_proteinNameLabel);

		proteinCoverageLabel = new JLabel("0 %");
		final GridBagConstraints gbc_proteinCoverageLabel = new GridBagConstraints();
		gbc_proteinCoverageLabel.anchor = GridBagConstraints.WEST;
		gbc_proteinCoverageLabel.insets = new Insets(0, 0, 5, 0);
		gbc_proteinCoverageLabel.gridx = 1;
		gbc_proteinCoverageLabel.gridy = 2;
		selectionInformationPanel.add(proteinCoverageLabel, gbc_proteinCoverageLabel);

		final JLabel lblNewLabel_4 = new JLabel("Position:");
		final GridBagConstraints gbc_lblNewLabel_4 = new GridBagConstraints();
		gbc_lblNewLabel_4.anchor = GridBagConstraints.EAST;
		gbc_lblNewLabel_4.insets = new Insets(0, 10, 10, 5);
		gbc_lblNewLabel_4.gridx = 0;
		gbc_lblNewLabel_4.gridy = 3;
		selectionInformationPanel.add(lblNewLabel_4, gbc_lblNewLabel_4);

		selectedPositionLabel = new JLabel("0");
		final GridBagConstraints gbc_selectedPositionLabel = new GridBagConstraints();
		gbc_selectedPositionLabel.insets = new Insets(0, 0, 10, 0);
		gbc_selectedPositionLabel.anchor = GridBagConstraints.WEST;
		gbc_selectedPositionLabel.gridx = 1;
		gbc_selectedPositionLabel.gridy = 3;
		selectionInformationPanel.add(selectedPositionLabel, gbc_selectedPositionLabel);

		graphsPanel = new JPanel();
		graphsPanel.setBackground(Color.white);
		southPanel.add(graphsPanel, BorderLayout.CENTER);
		final GridBagLayout gbl_graphsPanel = new GridBagLayout();
		graphsPanel.setLayout(gbl_graphsPanel);
		getContentPane().add(scroll, BorderLayout.CENTER);

		// load sequence
		loadSequence(getProteinSequence(), getPeptideOccupancyArray());
		// load peptides
		loadPeptidesTable(getGroupedPeptides(null), null);
		// add keyboard listener to function with arrows
		addKeyListener(getArrowKeyListener());
		getPeptideListAttachedDialog().addKeyListener(getArrowKeyListener());
		pack();

		final java.awt.Dimension dialogSize = getSize();
		final Dimension screenSize = GuiUtils.getScreenDimension();
		int x = (screenSize.width - dialogSize.width) / 2;
		// now set it to the left as much as the peptide list width
		x = Double.valueOf(Math.max(0, getPeptideListAttachedDialog().getPreferredSize().getWidth() * 1.0 / 2))
				.intValue();
		setLocation(x, (screenSize.height - dialogSize.height) / 2);
	}

	private KeyListener getArrowKeyListener() {
		final KeyListener listener = new KeyListener() {

			@Override
			public void keyTyped(KeyEvent e) {

			}

			@Override
			public void keyReleased(KeyEvent e) {
				processArrowKey(e);

			}

			@Override
			public void keyPressed(KeyEvent e) {

			}
		};
		return listener;
	}

	protected void processArrowKey(KeyEvent e) {
		// if there is no selection, just do nothing

		final MyGroupedPeptidesTable table = this.getPeptideListAttachedDialog().getTable();
		if (table.getSelectedRowCount() > 0) {
			int newSelectedRow = -1;
			int newSelectedRow2 = -1;
			final int[] selectedRows = table.getSelectedRows();
			boolean ctrl = false;
			if ((e.getModifiers() & KeyEvent.CTRL_MASK) != 0) {
				ctrl = true;
			}
			switch (e.getKeyCode()) {
			case KeyEvent.VK_UP:
				// move selection in the table up
				newSelectedRow = selectedRows[0] - 1;
				if (ctrl) {
					newSelectedRow2 = Maths.max(selectedRows);
				} else {
					newSelectedRow2 = newSelectedRow;
				}
				break;
			case KeyEvent.VK_DOWN:
				// move selection in the table down
				newSelectedRow2 = Float.valueOf(Maths.min(Maths.max(selectedRows) + 1, table.getRowCount() - 1))
						.intValue();
				if (ctrl) {
					newSelectedRow = Maths.min(selectedRows);
				} else {
					newSelectedRow = newSelectedRow2;
				}
				break;
			}
			if (newSelectedRow >= 0) {
				table.getSelectionModel().setSelectionInterval(newSelectedRow, newSelectedRow2);
			}
			return;
		}
		// now it is for the protein sequence
		if (selectedAminoacidPosition <= 0) {
			return;
		}
		int newPosition = -1;
		// is it CTRL pressed?
		int step = 1;
		boolean gotoNextGlycoSite = false;
		if ((e.getModifiers() & KeyEvent.CTRL_MASK) != 0 && (e.getModifiers() & KeyEvent.SHIFT_MASK) != 0) {
			gotoNextGlycoSite = true;
		} else if ((e.getModifiers() & KeyEvent.CTRL_MASK) != 0) {
			step = 5;
		}
		switch (e.getKeyCode()) {
		case KeyEvent.VK_LEFT:
			// move selection to the left, so 1 position less
			if (!gotoNextGlycoSite) {
				newPosition = selectedAminoacidPosition - step;
			} else {
				newPosition = getPreviousGlycoSite(selectedAminoacidPosition);
			}
			break;
		case KeyEvent.VK_RIGHT:
			// move selection to the right, so 1 position more
			if (!gotoNextGlycoSite) {
				newPosition = selectedAminoacidPosition + step;
			} else {
				newPosition = getNextGlycoSite(selectedAminoacidPosition);
			}
			break;
		case KeyEvent.VK_UP:
			// move selection up, so substract one line of positions
			newPosition = selectedAminoacidPosition - proteinSequenceLineLength - 1;
			break;
		case KeyEvent.VK_DOWN:
			// move selection down, so add one line of positions
			newPosition = selectedAminoacidPosition + proteinSequenceLineLength + 1;
			break;
		case KeyEvent.VK_ESCAPE:
			// clear selection
			proteinPositionClicked(null, false, -1);
			return;
		default:
			break;
		}
		if (newPosition <= 0 || newPosition > getProteinSequence().length()) {
			return;
		}
		final JLabel selectedAALabel = labelsByPositions.get(newPosition);
		final boolean isGlycoSitePosition = isGlycoSite(newPosition);
		proteinPositionClicked(selectedAALabel, isGlycoSitePosition, newPosition);
	}

	private int getPreviousGlycoSite(int position) {
		position--;
		while (!isGlycoSite(position) && position > 0) {
			position--;
		}
		return position;
	}

	private int getNextGlycoSite(int position) {
		position++;
		while (!isGlycoSite(position) && position > 0) {
			position++;
		}
		return position;
	}

	private void loadSequence(String proteinSequence, TIntList peptideOccupancyArray) {
		proteinSequencePanel.removeAll();
		final GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.NONE;
		c.gridy = 0;

		labelsByPositions = new TIntObjectHashMap<JLabel>();
		positionsByLabels = new TObjectIntHashMap<JLabel>();
		defaultColorsByAminoacidLabels.clear();
		int proteinSequenceIndex = 0;
		while (proteinSequenceIndex < proteinSequence.length()) {
			c.gridx = 0;
			final int endLine = Float
					.valueOf(Maths.min(proteinSequenceIndex + proteinSequenceLineLength, proteinSequence.length() - 1))
					.intValue();
			final String proteinSequenceLine = proteinSequence.substring(proteinSequenceIndex, endLine);

			for (int index = 0; index < proteinSequenceLine.length(); index++) {
				final int indexInProtein = index + proteinSequenceIndex;
				final int positionInProtein = indexInProtein + 1;
				final String positionInReference = ProteinSequences.getInstance()
						.mapPositionToReferenceProtein(currentProteinAcc, positionInProtein);
				final JLabel label = new JLabel("" + proteinSequence.charAt(indexInProtein));
				label.setOpaque(true);
				label.setFont(GuiUtils.aminoacidLabelFont);
				String tooltip = "<html>Aminoacid '<b>" + proteinSequence.charAt(indexInProtein) + "</b>' at position "
						+ positionInProtein;
				if (positionInReference != null) {
					tooltip += " - Position in reference " + ProteinSequences.REFERENCE + ": " + positionInReference;
				}
				tooltip += "<br>Click on it to filter the peptide list with only the peptides covering this position.<html>";
				label.setToolTipText(tooltip);
				if (isGlycoSite(positionInProtein)) {
					label.setBackground(GLYCO_SITE_BACKGROUND_COLOR);
					label.addMouseListener(getMouseListenerForGlycoSitePosition(positionInProtein));
				} else {
					if (peptideOccupancyArray.get(indexInProtein) > 0) {
						final Color colorByOccupancy = getColorByOccupancy(positionInProtein, peptideOccupancyArray);
						label.setBackground(colorByOccupancy);
						label.addMouseListener(getMouseListenerForNonGlycoSitePosition(positionInProtein));
					} else {
						label.setBackground(NON_COVERED_BACKGROUD);
						label.addMouseListener(getMouseListenerForNonGlycoSitePosition(positionInProtein));
					}
				}
				defaultColorsByAminoacidLabels.put(label, label.getBackground());

				if (selectedAminoacidPosition != positionInProtein) {
					setDefaultAminoacidBorder(label);
				} else {
					GuiUtils.setSelectedAminoacidBorder(label);
				}
				labelsByPositions.put(positionInProtein, label);
				positionsByLabels.put(label, positionInProtein);
				proteinSequencePanel.add(label, c);
				c.gridx++;
			}
			c.gridy++;
			proteinSequenceIndex = endLine + 1;
		}
		updateProteinCoverage(peptideOccupancyArray);

		validate(); // this refresh GUI but not resizes the window to the preferred size
	}

	private void updateNumberOfPeptides(int numPeptides) {
		this.numberOfPeptidesLabel.setText(numPeptides + "");

	}

	private void updateProteinCoverage(TIntList peptideOccupancyArray2) {
		int numOccupied = 0;
		for (final int numPeptidesInPosition : peptideOccupancyArray2.toArray()) {
			if (numPeptidesInPosition > 0) {
				numOccupied++;
			}
		}
		final double coverage = 1.0 * numOccupied / peptideOccupancyArray2.size();
		final DecimalFormat formatter = new DecimalFormat("#.#%");
		this.proteinCoverageLabel.setText(formatter.format(coverage));
	}

	private JLabel getSelectedAminoacidLabel() {
		if (labelsByPositions.containsKey(this.selectedAminoacidPosition)) {
			return this.labelsByPositions.get(this.selectedAminoacidPosition);
		}
		return null;
	}

	private void mouseExited(MouseEvent e) {
		// unless is the selected one
		final JLabel label = (JLabel) e.getComponent();
		if (label.equals(getSelectedAminoacidLabel())) {
			return;
		}
		// set NOT bold font
		GuiUtils.setPlainFont(label);

		// set default border
		setDefaultAminoacidBorder(label);

		// update the selected position label accordingly. even if it is -1 it will be
		// handled
		updateSelectedPosition(selectedAminoacidPosition);
	}

	private void mouseEntered(MouseEvent e) {
		final JLabel label = (JLabel) e.getComponent();
		// set bold font
		GuiUtils.setBoldFont(label);
		// set red border
		GuiUtils.setSelectedAminoacidBorder(label);
		// update selected position
		updateSelectedPosition(positionsByLabels.get(label));
	}

	private void mouseClicked(MouseEvent e, boolean isGlycoSitePosition, int position) {

		final JLabel label = (JLabel) e.getComponent();
		proteinPositionClicked(label, isGlycoSitePosition, position);

	}

	private void proteinPositionClicked(JLabel label, boolean isGlycoSitePosition, int position) {
		// if it is already the selected one, deselect it
		if (label == null || label.equals(getSelectedAminoacidLabel())) {
			selectedAminoacidPosition = -1;
			if (label != null) {
				// set default border
				setDefaultAminoacidBorder(label);
				// set non bold
				GuiUtils.setPlainFont(label);
			}
			// load all peptides in table
			final List<GroupedQuantifiedPeptide> peptides = getGroupedPeptides(position);
			loadPeptidesTable(peptides, position);
			updateNumberOfPeptides(peptides.size());
			showChartsFromPeptides(peptides, -1);
			return;
		}
		// set red border
		GuiUtils.setSelectedAminoacidBorder(label);
		// make this the selected one
		selectedAminoacidPosition = positionsByLabels.get(label);

		// set default border from the rest
		for (final JLabel label2 : labelsByPositions.valueCollection()) {
			if (label2.equals(label)) {
				continue;
			}
			setDefaultAminoacidBorder(label2);
		}

		// in case of being a glycoSite
		if (isGlycoSitePosition) {
			final int glycoSiteposition = positionsByLabels.get(label);
			final GlycoSite glycoSite = this.glycoSitesByPosition.get(glycoSiteposition);
			showChartsFromGlycoSite(glycoSite);
		} else {
			showChartsFromPeptides(getPeptidesCoveringPosition(position), position);

		}

		// show peptides in the table covering this position
		showPeptidesCoveringPosition(positionsByLabels.get(label));

	}

	private MouseListener getMouseListenerForNonGlycoSitePosition(int position) {
		final MouseListener ret = new MouseListener() {

			@Override
			public void mouseReleased(MouseEvent e) {

			}

			@Override
			public void mousePressed(MouseEvent e) {

			}

			@Override
			public void mouseExited(MouseEvent e) {
				ProteinSequenceDialog.this.mouseExited(e);
			}

			@Override
			public void mouseEntered(MouseEvent e) {
				ProteinSequenceDialog.this.mouseEntered(e);
			}

			@Override
			public void mouseClicked(MouseEvent e) {
				ProteinSequenceDialog.this.mouseClicked(e, false, position);
			}
		};
		return ret;
	}

	private MouseListener getMouseListenerForGlycoSitePosition(int position) {
		final MouseListener ret = new MouseListener() {

			@Override
			public void mouseReleased(MouseEvent e) {

			}

			@Override
			public void mousePressed(MouseEvent e) {

			}

			@Override
			public void mouseExited(MouseEvent e) {
				ProteinSequenceDialog.this.mouseExited(e);
			}

			@Override
			public void mouseEntered(MouseEvent e) {
				ProteinSequenceDialog.this.mouseEntered(e);
			}

			@Override
			public void mouseClicked(MouseEvent e) {
				ProteinSequenceDialog.this.mouseClicked(e, true, position);
			}
		};
		return ret;
	}

	protected void loadPeptidesTable(Collection<GroupedQuantifiedPeptide> groupedPeptides, Integer positionInProtein) {
		getPeptideListAttachedDialog().loadTable(groupedPeptides, positionInProtein);
	}

	protected void updateSelectedPosition(int position) {
		if (position > 0) {
			final String positionInReference = ProteinSequences.getInstance()
					.mapPositionToReferenceProtein(currentProteinAcc, position);
			this.selectedPositionLabel.setText(position + "");
			if (positionInReference != null) {
				this.selectedPositionLabel.setText(this.selectedPositionLabel.getText() + " - (Position in reference "
						+ ProteinSequences.REFERENCE + ": " + positionInReference + ")");
			}
		} else {
			this.selectedPositionLabel.setText("");
		}
	}

	@Override
	public void setVisible(boolean b) {
		showAttachedPeptideListDialog();
		super.setVisible(b);
	}

	protected void showPeptidesCoveringPosition(int position) {
		// get peptides covering that position
		final Set<GroupedQuantifiedPeptide> peptidesToLoad = getPeptidesCoveringPosition(position);
		// IMPORTANT: set the position for which we are getting the data
		peptidesToLoad.stream().forEach(pep -> pep.setPositionInPeptideWithPositionInProtein(position));
		// load table with these peptides
		loadPeptidesTable(peptidesToLoad, position);
		// update the number of peptides
		updateNumberOfPeptides(peptidesToLoad.size());
	}

	private Set<GroupedQuantifiedPeptide> getPeptidesCoveringPosition(int positionInProtein) {
		if (!peptidesByPositionsInProtein.containsKey(positionInProtein)) {
			for (final GroupedQuantifiedPeptide peptide : getGroupedPeptides(positionInProtein)) {
				final int firstPositionOfPeptideInProtein = GlycoPTMAnalyzerUtil
						.getPositionsInProtein(peptide.getSequence(), this.currentProteinAcc);
				final int lastPositionOfPeptideInProtein = firstPositionOfPeptideInProtein
						+ peptide.getSequence().length() - 1;
				if (positionInProtein >= firstPositionOfPeptideInProtein
						&& positionInProtein <= lastPositionOfPeptideInProtein) {
					if (!peptidesByPositionsInProtein.containsKey(positionInProtein)) {
						peptidesByPositionsInProtein.put(positionInProtein, new THashSet<GroupedQuantifiedPeptide>());
					}
					peptidesByPositionsInProtein.get(positionInProtein).add(peptide);
				}
			}
		}
		if (peptidesByPositionsInProtein.containsKey(positionInProtein)) {
			final Set<GroupedQuantifiedPeptide> ret = peptidesByPositionsInProtein.get(positionInProtein);
			ret.stream().forEach(pep -> pep.setPositionInPeptideWithPositionInProtein(positionInProtein));
			return ret;
		}
		return Collections.emptySet();
	}

	/**
	 * Gets the peptide occupancy array using all the peptides
	 * 
	 * @return
	 */
	private TIntList getPeptideOccupancyArray() {
		if (peptideOccupancyArray == null) {
			peptideOccupancyArray = getPeptideOccupancyArray(this.getGroupedPeptides(null), this.getProteinSequence());
		}
		return peptideOccupancyArray;
	}

	private TIntList getPeptideOccupancyArray(Collection<GroupedQuantifiedPeptide> peptides, String proteinSequence) {

		final TIntList ret = new TIntArrayList(proteinSequence.length());
		// fill it with 0s
		for (int i = 0; i < proteinSequence.length(); i++) {
			ret.add(0);
		}
		for (final GroupedQuantifiedPeptide peptide : peptides) {
			final String sequence = peptide.getSequence();
			final TIntArrayList positions = StringUtils.allPositionsOf(proteinSequence, sequence);
			for (final int position : positions.toArray()) {
				for (int i = 0; i < sequence.length(); i++) {
					ret.set(position - 1 + i, ret.get(position - 1 + i) + 1);
				}
			}
		}
		return ret;

	}

	private boolean isGlycoSite(int positionInProtein) {
		return glycoSitesByPosition.containsKey(positionInProtein);
	}

	private Color getColorByOccupancy(int position, TIntList peptideOccupancyArray) {
		final float normalizedOccupancy = getNormalizedOccupancy(position, peptideOccupancyArray);
		final Color ret = getColor(normalizedOccupancy);
		return ret;
	}

	private float getNormalizedOccupancy(int position, TIntList peptideOccupancyArray) {
		final int occupancyInPosition = peptideOccupancyArray.get(position - 1);
		final int max = peptideOccupancyArray.max();
		final float ret = occupancyInPosition * 1.0f / max;
		return ret;
	}

	/**
	 * Gets a color from a color range, that corresponds to a value between 0 and 1.
	 * 
	 * @param value
	 * @return
	 */
	private Color getColor(float value) {
		final float minHue = 120f / 255; // corresponds to green
		final float maxHue = 0; // corresponds to red
		final float hue = value * maxHue + (1 - value) * minHue;
		final Color c = new Color(Color.HSBtoRGB(hue, 1, 0.5f));
		return c;
	}

	public List<GlycoSite> getGlycoSites() {
		return this.glycoSites;
	}

	/**
	 * Gets the complete list of peptides after setting the position in the protein
	 * for which we want the data.<br>
	 * Note that it doesn't mean that we get the list of GroupedQuantifiedPeptide
	 * covering that position. For that purpose, call to
	 * getPeptidesCoveringPosition(position) method.
	 * 
	 * @param position
	 * @return
	 */
	public List<GroupedQuantifiedPeptide> getGroupedPeptides(Integer positionInProtein) {
		this.groupedPeptides.stream().forEach(pep -> pep.setPositionInPeptideWithPositionInProtein(positionInProtein));
		return this.groupedPeptides;
	}

	public String getProteinSequence() {
		return this.proteinSequence;
	}

	public void highlightPeptidesOnSequence(Collection<GroupedQuantifiedPeptide> selectedGroupedPeptides,
			Integer positionInProtein) {
		TIntList peptideOccupancyArray = null;
		if (selectedGroupedPeptides == null || selectedGroupedPeptides.isEmpty()) {
			peptideOccupancyArray = getPeptideOccupancyArray(getGroupedPeptides(positionInProtein),
					getProteinSequence());
		} else {
			peptideOccupancyArray = getPeptideOccupancyArray(selectedGroupedPeptides, getProteinSequence());
		}
		loadSequence(getProteinSequence(), peptideOccupancyArray);
	}

	public void showChartsFromPeptides(Collection<GroupedQuantifiedPeptide> selectedPeptides, int positionInProtein) {
		// set position as -1
		selectedPeptides.stream().forEach(pep -> pep.setPositionInPeptide(-1));
		final int width = 300;
		final int height = 200;
		graphsPanel.removeAll();
		try {
			final JPanel headerPanel = new JPanel();
			headerPanel.setBackground(Color.white);
			if (selectedPeptides.isEmpty()) {
				String text = "No peptides covering position " + positionInProtein;

				if (positionInProtein != -1) {
					final String positionInReference = ProteinSequences.getInstance()
							.mapPositionToReferenceProtein(currentProteinAcc, positionInProtein);
					if (positionInReference != null) {
						text += " <i>(" + positionInReference + " in reference " + ProteinSequences.REFERENCE + ")</i>";
					}
				}
				final JLabel headerLabel = new JLabel("<html>" + text + "</html>");
				headerLabel.setFont(GuiUtils.headerFont());
				headerPanel.add(headerLabel);
				final GridBagConstraints c = new GridBagConstraints();
				c.gridx = 0;
				c.gridy = 0;
				c.fill = GridBagConstraints.HORIZONTAL;
				c.ipady = 20;
				c.anchor = GridBagConstraints.NORTH;
				this.graphsPanel.add(headerPanel, c);
				return;
			}
			// sort peptides in a list
			final List<GroupedQuantifiedPeptide> peptides = new ArrayList<GroupedQuantifiedPeptide>();
			peptides.addAll(selectedPeptides);
			Collections.sort(peptides, new Comparator<GroupedQuantifiedPeptide>() {

				@Override
				public int compare(GroupedQuantifiedPeptide o1, GroupedQuantifiedPeptide o2) {

					return o1.getKey(false).compareTo(o2.getKey(false));
				}
			});

			String text = null;

			final int numMeasurements = GlycoPTMAnalyzerUtil.getNumIndividualProportions(peptides,
					sumIntensitiesAcrossReplicates);

			final String numMeasurementsText = " (" + numMeasurements + " measurements)";
			if (positionInProtein != -1) {
				final String positionInReference = ProteinSequences.getInstance()
						.mapPositionToReferenceProtein(currentProteinAcc, positionInProtein);
				if (peptides.size() > 1) {
					text = "Charts summarizing <br>" + peptides.size() + " peptides " + numMeasurementsText
							+ "<br> covering position " + positionInProtein;
				} else {
					text = "Charts summarizing <br>peptide " + peptides.iterator().next().getKey(false) + " "
							+ numMeasurementsText + "<br> covering position " + positionInProtein;
				}
				if (positionInReference != null) {
					text += " <i>(" + positionInReference + " in reference " + ProteinSequences.REFERENCE + ")</i>";
				}
				text += ":";
			} else {
				if (peptides.size() > 1) {
					text = "Charts summarizing <br>the " + peptides.size() + " selected peptides <br>"
							+ numMeasurementsText + ":";
				} else {
					text = "Charts summarizing <br>selected peptide " + peptides.iterator().next().getKey(false)
							+ " <br>" + numMeasurementsText + ":";
				}
			}
			final JLabel headerLabel = new JLabel("<html><div style='text-align: center;'>" + text + "</div></html>");
			headerLabel.setFont(GuiUtils.headerFont());
			headerPanel.add(headerLabel);
			final GridBagConstraints c = new GridBagConstraints();
			c.gridx = 0;
			c.gridy = 0;
			c.gridwidth = 2; // number of charts horizontally
			c.fill = GridBagConstraints.HORIZONTAL;
			c.ipady = 20;
			c.anchor = GridBagConstraints.NORTH;
			this.graphsPanel.add(headerPanel, c);

			// starting in row 1
			// proportions pie chart

			// intensities whiskeyChart
			final ChartPanel chart2 = createIntensitiesWhiskeyChartForPeptides(peptides, width, height);
			final GridBagConstraints c3 = new GridBagConstraints();
			c3.gridx = 0;
			c3.gridy = 1;
			c3.fill = GridBagConstraints.NONE;
			c3.anchor = GridBagConstraints.FIRST_LINE_START;
			this.graphsPanel.add(chart2, c3);
			chart2.updateUI();

			// intensities error bar
			final ChartPanel chart1 = createIntensitiesErrorBarChartForPeptides(peptides, width, height);
			final GridBagConstraints c2 = new GridBagConstraints();
			c2.gridx = 1;
			c2.gridy = 1;
			c2.fill = GridBagConstraints.NONE;
			c2.anchor = GridBagConstraints.FIRST_LINE_START;
			this.graphsPanel.add(chart1, c2);
			chart1.updateUI();
		} finally {
			this.graphsPanel.repaint();
			this.repaint();
		}
	}

	public void showChartsFromGlycoSite(GlycoSite glycoSite) {

		final Collection<GroupedQuantifiedPeptide> selectedPeptides = glycoSite.getCoveredGroupedPeptides();
		final int positionInProtein = glycoSite.getPosition();
		final int width = 300;
		final int height = 200;
		graphsPanel.removeAll();
		try {
			final JPanel headerPanel = new JPanel();
			headerPanel.setBackground(Color.white);
			if (selectedPeptides.isEmpty()) {
				final JLabel headerLabel = new JLabel("No peptides covering position " + positionInProtein);
				headerLabel.setFont(GuiUtils.headerFont());
				headerPanel.add(headerLabel);
				final GridBagConstraints c = new GridBagConstraints();
				c.gridx = 0;
				c.gridy = 0;
				c.fill = GridBagConstraints.HORIZONTAL;
				c.ipady = 20;
				c.anchor = GridBagConstraints.NORTH;
				this.graphsPanel.add(headerPanel, c);
				return;
			}
			// sort peptides in a list
			final List<GroupedQuantifiedPeptide> peptides = new ArrayList<GroupedQuantifiedPeptide>();
			peptides.addAll(selectedPeptides);
			Collections.sort(peptides, new Comparator<GroupedQuantifiedPeptide>() {

				@Override
				public int compare(GroupedQuantifiedPeptide o1, GroupedQuantifiedPeptide o2) {

					return o1.getKey(false).compareTo(o2.getKey(false));
				}
			});

			String text = null;

			final int numMeasurements = GlycoPTMAnalyzerUtil.getNumIndividualIntensities(peptides,
					sumIntensitiesAcrossReplicates);

			final String numMeasurementsText = " (" + numMeasurements + " measurements)";
			if (positionInProtein != -1) {
				final String positionInReference = ProteinSequences.getInstance()
						.mapPositionToReferenceProtein(currentProteinAcc, positionInProtein);
				if (peptides.size() > 1) {
					text = "Charts summarizing <br>" + peptides.size() + " peptides " + numMeasurementsText
							+ "<br> covering position " + positionInProtein;
				} else {
					text = "Charts summarizing <br>peptide " + peptides.iterator().next().getKey(false) + " "
							+ numMeasurementsText + "<br> covering position " + positionInProtein;
				}
				if (positionInReference != null) {
					text += " <i>(" + positionInReference + " in reference " + ProteinSequences.REFERENCE + ")</i>";
				}
				text += ":";
				if (glycoSite.isAmbiguous()) {
					text = "This site is ambiguous between position " + glycoSite.getPosition() + " <i>("
							+ glycoSite.getReferencePosition() + " in " + ProteinSequences.REFERENCE
							+ ")</i> and position "
							+ StringUtils.getSortedSeparatedValueString(glycoSite.getAmbiguousSites(), ",") + ".<br>"
							+ "It is not possible to have PTMs in consecutive sites in the protein.<br>(<i>'Don't allow consecutive motifs'</i> was enabled).<br><br>"
							+ text;
				}
			} else {
				if (peptides.size() > 1) {
					text = "Charts summarizing <br>the " + peptides.size() + " selected peptides <br>"
							+ numMeasurementsText + ":";
				} else {
					text = "Charts summarizing <br>selected peptide " + peptides.iterator().next().getKey(false)
							+ " <br>" + numMeasurementsText + ":";
				}
			}
			final JLabel headerLabel = new JLabel("<html><div style='text-align: center;'>" + text + "</div></html>");
			headerLabel.setFont(GuiUtils.headerFont());
			headerPanel.add(headerLabel);
			final GridBagConstraints c = new GridBagConstraints();
			c.gridx = 0;
			c.gridy = 0;
			c.gridwidth = 2; // number of charts horizontally
			c.fill = GridBagConstraints.HORIZONTAL;
			c.ipady = 20;
			c.anchor = GridBagConstraints.NORTH;
			this.graphsPanel.add(headerPanel, c);

			// starting in row 1
			// proportions pie chart
			final ChartPanel chart3 = ChartUtils.createProportionsPieChartForGroupedPeptides(peptides, "", "",
					sumIntensitiesAcrossReplicates, width, height);
			final GridBagConstraints c4 = new GridBagConstraints();
			c4.gridx = 0;
			c4.gridy = 1;
			c4.fill = GridBagConstraints.NONE;
			c4.anchor = GridBagConstraints.FIRST_LINE_START;
			this.graphsPanel.add(chart3, c4);
			chart3.updateUI();
			// proportions whiskeyChart
			final ChartPanel chart2 = createProportionsWhiskeyChartForPeptides(peptides, width, height);
			final GridBagConstraints c3 = new GridBagConstraints();
			c3.gridx = 1;
			c3.gridy = 1;
			c3.fill = GridBagConstraints.NONE;
			c3.anchor = GridBagConstraints.FIRST_LINE_START;
			this.graphsPanel.add(chart2, c3);
			chart2.updateUI();

			// proportions scatter plot
			final ChartPanel chart4 = createProportionsScatterPlotChartForPeptides(peptides, width, height);
			final GridBagConstraints c5 = new GridBagConstraints();
			c5.gridx = 0;
			c5.gridy = 2;
			c5.fill = GridBagConstraints.NONE;
			c5.anchor = GridBagConstraints.FIRST_LINE_START;
			this.graphsPanel.add(chart4, c5);
			chart4.updateUI();
			// intensities whiskeyChart
			final ChartPanel chart1 = createIntensitiesErrorBarChartForPeptides(peptides, width, height);
			final GridBagConstraints c2 = new GridBagConstraints();
			c2.gridx = 1;
			c2.gridy = 2;
			c2.fill = GridBagConstraints.NONE;
			c2.anchor = GridBagConstraints.FIRST_LINE_START;
			this.graphsPanel.add(chart1, c2);
			chart1.updateUI();
		} finally {
			this.graphsPanel.repaint();
			this.repaint();
		}
	}

	private ChartPanel createProportionsScatterPlotChartForPeptides(Collection<GroupedQuantifiedPeptide> peptides,
			int width, int height) {
		final ChartPanel chartPanel = ChartUtils.createProportionsScatterPlotChartForPeptides(peptides,
				sumIntensitiesAcrossReplicates, "", "", width, height);

		return chartPanel;
	}

	private ChartPanel createIntensitiesErrorBarChartForPeptides(Collection<GroupedQuantifiedPeptide> selectedPeptides,
			int width, int height) {

		final ChartPanel chartPanel = ChartUtils.createIntensitiesErrorBarChartForPeptides(selectedPeptides, "", "",
				false, ErrorType.SEM, width, height);
		final CategoryPlot plot = (CategoryPlot) chartPanel.getChart().getPlot();
		final ValueAxis rangeAxis = plot.getRangeAxis();
		// font for the axis
		rangeAxis.setLabelFont(AbstractMultipleChartsBySitePanel.axisFont);
		rangeAxis.setTickLabelFont(AbstractMultipleChartsBySitePanel.axisFont);
		final CategoryAxis domainAxis = plot.getDomainAxis();
		domainAxis.setLabelFont(AbstractMultipleChartsBySitePanel.axisFont);
		domainAxis.setTickLabelFont(AbstractMultipleChartsBySitePanel.axisFont);
		return chartPanel;
	}

	private ChartPanel createIntensitiesWhiskeyChartForPeptides(Collection<GroupedQuantifiedPeptide> selectedPeptides,
			int width, int height) {

		final ChartPanel chartPanel = ChartUtils.createIntensitiesBoxAndWhiskerChartForGroupedPeptides(selectedPeptides,
				"", "", sumIntensitiesAcrossReplicates, width, height);
		final CategoryPlot plot = (CategoryPlot) chartPanel.getChart().getPlot();
		final ValueAxis rangeAxis = plot.getRangeAxis();
		// font for the axis
		rangeAxis.setLabelFont(AbstractMultipleChartsBySitePanel.axisFont);
		rangeAxis.setTickLabelFont(AbstractMultipleChartsBySitePanel.axisFont);
		final CategoryAxis domainAxis = plot.getDomainAxis();
		domainAxis.setLabelFont(AbstractMultipleChartsBySitePanel.axisFont);
		domainAxis.setTickLabelFont(AbstractMultipleChartsBySitePanel.axisFont);
		return chartPanel;
	}

	private ChartPanel createProportionsWhiskeyChartForPeptides(Collection<GroupedQuantifiedPeptide> selectedPeptides,
			int width, int height) {

		final ChartPanel chartPanel = ChartUtils.createProportionsBoxAndWhiskerChartForGroupedPeptides(selectedPeptides,
				"", "", sumIntensitiesAcrossReplicates, width, height);
		final CategoryPlot plot = (CategoryPlot) chartPanel.getChart().getPlot();
		final ValueAxis rangeAxis = plot.getRangeAxis();
		// font for the axis
		rangeAxis.setLabelFont(AbstractMultipleChartsBySitePanel.axisFont);
		rangeAxis.setTickLabelFont(AbstractMultipleChartsBySitePanel.axisFont);
		final CategoryAxis domainAxis = plot.getDomainAxis();
		domainAxis.setLabelFont(AbstractMultipleChartsBySitePanel.axisFont);
		domainAxis.setTickLabelFont(AbstractMultipleChartsBySitePanel.axisFont);
		return chartPanel;
	}

	private void setDefaultAminoacidBorder(JLabel label) {
		GuiUtils.setAminoacidBorder(label, defaultColorsByAminoacidLabels.get(label));
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
}
