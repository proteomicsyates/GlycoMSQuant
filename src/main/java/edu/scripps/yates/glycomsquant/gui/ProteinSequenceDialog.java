package edu.scripps.yates.glycomsquant.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

import edu.scripps.yates.glycomsquant.GlycoSite;
import edu.scripps.yates.utilities.maths.Maths;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;

public class ProteinSequenceDialog extends JFrame {
	/**
	 * 
	 */
	private static final long serialVersionUID = -7470139520024826767L;
	private final String proteinSequence;
	private final List<GlycoSite> glycoSites;
	private final TIntObjectMap<GlycoSite> glycoSitesByPosition = new TIntObjectHashMap<GlycoSite>();

	public ProteinSequenceDialog(String proteinOfInterest, String proteinSequence, List<GlycoSite> glycoSites) {
		super("Protein sequence of '" + proteinOfInterest + "'");
		this.proteinSequence = proteinSequence;
		this.glycoSites = glycoSites;
		glycoSites.stream().forEach(g -> glycoSitesByPosition.put(g.getPosition(), g));
		final java.awt.Dimension screenSize = java.awt.Toolkit.getDefaultToolkit().getScreenSize();

		setPreferredSize(new Dimension(screenSize.width / 2, screenSize.height / 2));
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		getContentPane().setLayout(new BorderLayout(10, 10));

		// count the number of rows
		int numRows = 0;
		int proteinSequenceIndex = 0;
		while (proteinSequenceIndex < this.proteinSequence.length()) {
			numRows++;
			final int endLine = Float.valueOf(Maths.min(proteinSequenceIndex + 80, proteinSequence.length() - 1))
					.intValue();
			proteinSequenceIndex = endLine + 1;
		}

		final JTextPane pane = new JTextPane();
		final JScrollPane scroll = new JScrollPane(pane);
		getContentPane().add(scroll, BorderLayout.CENTER);

		// Set the attributes before adding text

		final SimpleAttributeSet attributeSet = new SimpleAttributeSet();
		StyleConstants.setForeground(attributeSet, Color.black);

		final Font font = new Font("Courier", Font.PLAIN, 15);
		StyleConstants.setFontFamily(attributeSet, font.getFamily());
		StyleConstants.setFontSize(attributeSet, font.getSize());
		StyleConstants.setItalic(attributeSet, (font.getStyle() & Font.ITALIC) != 0);
		StyleConstants.setBold(attributeSet, (font.getStyle() & Font.BOLD) != 0);
		pane.setCharacterAttributes(attributeSet, true);

		final SimpleAttributeSet coveredSequenceAttSet = new SimpleAttributeSet();
		StyleConstants.setLeftIndent(coveredSequenceAttSet, 20);
		StyleConstants.setRightIndent(coveredSequenceAttSet, 20);
		StyleConstants.setSpaceAbove(coveredSequenceAttSet, 20);
		StyleConstants.setSpaceBelow(coveredSequenceAttSet, 20);
//		StyleConstants.setItalic(attributeSet, true);
		StyleConstants.setForeground(coveredSequenceAttSet, Color.black);
		StyleConstants.setBackground(coveredSequenceAttSet, Color.cyan);

		StyleConstants.setFontFamily(coveredSequenceAttSet, font.getFamily());
		StyleConstants.setFontSize(coveredSequenceAttSet, font.getSize());
		StyleConstants.setItalic(coveredSequenceAttSet, (font.getStyle() & Font.ITALIC) != 0);
		StyleConstants.setBold(coveredSequenceAttSet, (font.getStyle() & Font.BOLD) != 0);
		final Document doc = pane.getStyledDocument();
		try {

			proteinSequenceIndex = 0;
			while (proteinSequenceIndex < this.proteinSequence.length()) {
				final int endLine = Float.valueOf(Maths.min(proteinSequenceIndex + 80, proteinSequence.length() - 1))
						.intValue();
				final String proteinSequenceLine = proteinSequence.substring(proteinSequenceIndex, endLine);
				for (int index = 0; index < proteinSequenceLine.length(); index++) {
					final int indexInProtein = index + proteinSequenceIndex;
					if (isGlycoSite(indexInProtein + 1)) {
						doc.insertString(doc.getLength(), "" + proteinSequence.charAt(indexInProtein),
								coveredSequenceAttSet);
					} else {
						doc.insertString(doc.getLength(), "" + proteinSequence.charAt(indexInProtein), attributeSet);
					}
				}
				doc.insertString(doc.getLength(), "\n", attributeSet);
				proteinSequenceIndex = endLine + 1;
			}
		} catch (final BadLocationException e) {
			e.printStackTrace();
		}
		pack();

		final java.awt.Dimension dialogSize = getSize();
		setLocation((screenSize.width - dialogSize.width) / 2, (screenSize.height - dialogSize.height) / 2);
	}

	private boolean isGlycoSite(int positionInProtein) {
		return glycoSitesByPosition.containsKey(positionInProtein);
	}

}
