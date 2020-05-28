package edu.scripps.yates.glycomsquant.gui.reference;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

@SuppressWarnings("serial")
public class ReferenceProteinSequenceEditor extends JDialog {

	private final JTextArea jTextAreaProteinSequence;

	public ReferenceProteinSequenceEditor(JFrame parent) {
		super(parent);
		setTitle("Reference protein sequence editor");

		final JScrollPane scrollPane = new JScrollPane();
		scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

		jTextAreaProteinSequence = new JTextArea(MappingToReferenceHXB2.HXB2);
		jTextAreaProteinSequence.setFont(new Font("Courier New", Font.PLAIN, 12));
		jTextAreaProteinSequence.setRows(25);
		jTextAreaProteinSequence.setColumns(80);
		jTextAreaProteinSequence.setLineWrap(true);
		jTextAreaProteinSequence.setToolTipText("Reference protein sequence");
		scrollPane.setViewportView(jTextAreaProteinSequence);

		final JPanel northPanel = new JPanel();
		getContentPane().add(northPanel, BorderLayout.NORTH);
		northPanel.setLayout(new BorderLayout(0, 0));

		final JLabel lblNewLabel = new JLabel(
				"<html>Here you can edit the protein sequence that you want to use as reference.<br>"
						+ "Some projects, such as HIV-related projects use a reference protein as a template to refer to any other variant of the protein.<br>"
						+ "That protein is aligned to the experimental protein and all sites with PTMs in that experimental protein will be mapped and renamed accordingly to that reference.<br>\r\nSee this this article to know more about it:");
		lblNewLabel.setBorder(new EmptyBorder(10, 10, 5, 10));
		northPanel.add(lblNewLabel, BorderLayout.CENTER);
		final JLabel hyperlink = new JLabel("Numbering Positions in HIV Relative to HXB2CG");

		hyperlink.setForeground(Color.BLUE.darker());
		hyperlink.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		hyperlink.addMouseListener(new MouseAdapter() {

			@Override
			public void mouseClicked(MouseEvent e) {
				try {

					Desktop.getDesktop()
							.browse(new URI("https://www.hiv.lanl.gov/content/sequence/HIV/REVIEWS/HXB2.html"));

				} catch (IOException | URISyntaxException e1) {
					e1.printStackTrace();
				}
			}

			@Override
			public void mouseEntered(MouseEvent e) {
				// the mouse has entered the label
			}

			@Override
			public void mouseExited(MouseEvent e) {
				// the mouse has exited the label
			}
		});
		hyperlink.setBorder(new EmptyBorder(0, 30, 10, 10));
		northPanel.add(hyperlink, BorderLayout.SOUTH);

		final JPanel centerPanel = new JPanel();
		centerPanel.setBorder(new EmptyBorder(0, 5, 5, 5));
		getContentPane().add(centerPanel, BorderLayout.CENTER);
		centerPanel.setLayout(new BorderLayout(0, 0));
		centerPanel.add(scrollPane, BorderLayout.CENTER);

		final JPanel panel2 = new JPanel();
		panel2.setBorder(new EmptyBorder(20, 10, 20, 10));
		centerPanel.add(panel2, BorderLayout.NORTH);
		panel2.setLayout(new BorderLayout(0, 0));

		final JPanel panel = new JPanel();
		panel2.add(panel, BorderLayout.NORTH);

		final JLabel lblNewLabel_1 = new JLabel("Click here to set HXB2 as your reference protein:");
		panel.add(lblNewLabel_1);

		final JButton btnNewButton = new JButton("HXB2");
		btnNewButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				setReferenceProteinSequence(MappingToReferenceHXB2.HXB2);
			}
		});
		btnNewButton.setToolTipText("click here to set HXB2 as your reference protein");
		panel.add(btnNewButton);

		final JLabel lblNewLabel_2 = new JLabel(
				"Or paste below the protein sequence that you want to use as reference:");
		lblNewLabel_2.setHorizontalAlignment(SwingConstants.CENTER);
		panel2.add(lblNewLabel_2, BorderLayout.SOUTH);

		//
		pack();
		final java.awt.Dimension screenSize = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
		final java.awt.Dimension dialogSize = getSize();
		setLocation((screenSize.width - dialogSize.width) / 2, (screenSize.height - dialogSize.height) / 2);
	}

	public String getReferenceProteinSequence() {
		final String proteinSequence = jTextAreaProteinSequence.getText();
		if (!"".equals(proteinSequence)) {
			return proteinSequence;
		}
		return null;
	}

	public void setReferenceProteinSequence(String referenceProteinSequence) {
		this.jTextAreaProteinSequence.setText(referenceProteinSequence);
	}

}
