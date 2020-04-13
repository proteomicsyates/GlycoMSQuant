package edu.scripps.yates.glycomsquant.gui.tables.run_table;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Point;
import java.awt.SystemColor;
import java.awt.Window;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;

import org.apache.log4j.Logger;

import edu.scripps.yates.glycomsquant.gui.files.FileManager;

public class AttachedRunsAttachedDialog extends JDialog {
	/**
	 * 
	 */
	private static final long serialVersionUID = -1119185903108814297L;
	private static final Logger log = Logger.getLogger(AttachedRunsAttachedDialog.class);
	private final Window parentFrame;
	private static final String HTML_START = "<html>";
	private static final String HTML_END = "</html>";
	private static final String HTML_NEW_LINE = "<br>";
	private static final String HTML_BOLD_START = "<b>";
	private static final String HTML_BOLD_END = "</b>";
	private static final String HTML_ITALIC_START = "<i>";
	private static final String HTML_ITALIC_END = "</i>";
	private static final String HTML_BLANK_SPACE = "&nbsp;";
	private boolean minimized = false;
	private final int maxWidth;

	private Style regularText;
	private Style indentedText;
	private Style boldText;
	private Style italicText;
	private Style headingText;
	private Style subheadingText;
	private Style biggerRegularText;
	private Style biggerHeadingText;
	private Style defaultRegularText;
	private Style defaultSubheadingText;
	private final JScrollPane scrollPane;

	public AttachedRunsAttachedDialog(Window parentWindow, int maxWidth) {
		super(parentWindow, ModalityType.MODELESS);

		this.maxWidth = maxWidth;
		getContentPane().setBackground(SystemColor.info);
		setTitle("GlycoMSQuant runs");
		setFocusableWindowState(false);
		setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (UnsupportedLookAndFeelException | ClassNotFoundException | InstantiationException
				| IllegalAccessException e) {
			e.printStackTrace();
		}
		this.parentFrame = parentWindow;

		scrollPane = new JScrollPane();
		scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scrollPane.getVerticalScrollBar().setUnitIncrement(20);
		getContentPane().add(scrollPane, BorderLayout.CENTER);
		// contentPanel.setBackground(SystemColor.info);

		addWindowListeners();
	}

	public void loadResultFolders() {

		log.info("Loading result folders");

		final JPanel contentPanel = new JPanel();
		scrollPane.setViewportView(contentPanel);

		contentPanel.setLayout(new BorderLayout());

		log.info("adding panels");
		final JPanel northPanel = new JPanel();
		// panel.setMaximumSize(new Dimension(100, Integer.MAX_VALUE));
		northPanel.setLayout(new BorderLayout());

		contentPanel.add(northPanel, BorderLayout.NORTH);
		final ScrollableRunsTable table = new ScrollableRunsTable(200);
		contentPanel.add(table, BorderLayout.CENTER);
		table.getTable().clearData();

		addColumnsInTable(table.getTable(), ColumnsRunTable.getColumnsStringForTable());
		final List<File> resultFolders = FileManager.getResultFolders();
		if (resultFolders != null) {
			for (int i = 0; i < resultFolders.size(); i++) {
				final File folder = resultFolders.get(i);
				final MyRunsTableModel model = (MyRunsTableModel) table.getTable().getModel();

				final List<Object> runInfoList = ColumnsRunTableUtil.getInstance(folder).getRunInfoList(folder,
						ColumnsRunTable.getColumns(), i + 1);
				model.addRow(runInfoList.toArray());
				log.info("Table now with " + model.getRowCount() + " rows");
			}

			log.info(resultFolders.size() + " folders added to attached window");
		}
		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				table.getTable().repaint();
				table.initializeSorter();
			}
		});
	}

	private void addColumnsInTable(MyRunsTable table, List<String> columnsStringList) {
		final DefaultTableModel defaultModel = (DefaultTableModel) table.getModel();
		log.info("Adding colums " + columnsStringList.size() + " columns");
		if (columnsStringList != null) {

			for (final String columnName : columnsStringList) {
				defaultModel.addColumn(columnName);
			}
			log.info("Added " + table.getColumnCount() + " colums");
			for (int i = 0; i < table.getColumnCount(); i++) {
				final TableColumn column = table.getColumnModel().getColumn(i);
				final ColumnsRunTable[] columHeaders = ColumnsRunTable.values();
				for (final ColumnsRunTable header : columHeaders) {
					if (column.getHeaderValue().equals(header.getName()))
						column.setPreferredWidth(header.getDefaultWidth());
//					column.setMaxWidth(header.getDefaultWidth());
					column.setMinWidth(header.getDefaultWidth());
				}
				column.setResizable(true);
			}
		}
	}

	private void insertTextToDocument(String message, DefaultStyledDocument document) {

		final List<String> messages = new ArrayList<String>();
		if (message.contains(HTML_NEW_LINE)) {
			for (final String messageTMP : message.split(HTML_NEW_LINE)) {
				messages.add(messageTMP);
			}
		} else {
			messages.add(message);
		}
		try {
			boolean lookingForBoldStart = true;
			boolean lookingForItalicStart = true;
			boolean insideBold = false;
			boolean insideItalic = false;
			boolean firstTime = true;
			for (int i = 0; i < messages.size(); i++) {
				String helpMessage = messages.get(i);
				if (firstTime) {
					helpMessage = helpMessage.trim();
					firstTime = false;
				}

				if (helpMessage.startsWith(HTML_BOLD_START) && helpMessage.endsWith(HTML_BOLD_END)) {
					document.insertString(document.getLength(), removeHTMLTags(helpMessage) + "\n", subheadingText);
					firstTime = true;
					continue;
				}

				Style style = null;

				if (i == 0) {
					style = headingText;
				}
				int startBold = helpMessage.indexOf(HTML_BOLD_START);
				if (startBold == -1) {
					startBold = Integer.MAX_VALUE;
				}
				final int endBold = helpMessage.indexOf(HTML_BOLD_END);
				int startItalic = helpMessage.indexOf(HTML_ITALIC_START);
				if (startItalic == -1) {
					startItalic = Integer.MAX_VALUE;
				}
				final int endItalic = helpMessage.indexOf(HTML_ITALIC_END);
				String tmpMessage;
				if (!insideBold && !insideItalic && lookingForBoldStart && startBold < startItalic
						&& helpMessage.contains(HTML_BOLD_END)) {

					tmpMessage = helpMessage.substring(0, startBold);
					document.insertString(document.getLength(), tmpMessage, style);
					helpMessage = helpMessage.substring(tmpMessage.length() + HTML_BOLD_START.length());
					lookingForBoldStart = false;
					insideBold = true;
					messages.set(i, helpMessage);
					i--;
					continue;
				} else if (insideBold) {
					tmpMessage = helpMessage.substring(0, endBold);
					document.insertString(document.getLength(), tmpMessage, boldText);
					helpMessage = helpMessage.substring(tmpMessage.length() + HTML_BOLD_END.length());
					insideBold = false;
					lookingForBoldStart = true;
					messages.set(i, helpMessage);
					i--;
					continue;
				} else if (!insideBold && !insideItalic && lookingForItalicStart && startItalic < startBold
						&& helpMessage.contains(HTML_ITALIC_END)) {

					tmpMessage = helpMessage.substring(0, startItalic);
					document.insertString(document.getLength(), tmpMessage, style);
					helpMessage = helpMessage.substring(tmpMessage.length() + HTML_ITALIC_START.length());
					lookingForItalicStart = false;
					insideItalic = true;
					messages.set(i, helpMessage);
					i--;
					continue;
				} else if (insideItalic) {
					tmpMessage = helpMessage.substring(0, endItalic);
					document.insertString(document.getLength(), tmpMessage, italicText);
					helpMessage = helpMessage.substring(tmpMessage.length() + HTML_ITALIC_END.length());
					lookingForItalicStart = true;
					insideItalic = false;
					messages.set(i, helpMessage);
					i--;
					continue;
				} else {
					document.insertString(document.getLength(), helpMessage, style);

				}

				document.insertString(document.getLength(), "\n", style);
				firstTime = true;
			}

		} catch (final BadLocationException e) {
			e.printStackTrace();
		}

	}

	private void addWindowListeners() {
		this.parentFrame.addComponentListener(new ComponentListener() {

			@Override
			public void componentShown(ComponentEvent e) {
				log.debug(e.getID());
				AttachedRunsAttachedDialog.this.setVisible(true);
			}

			@Override
			public void componentResized(ComponentEvent e) {
				log.debug(e.getID());
				AttachedRunsAttachedDialog.this.setVisible(true);

			}

			@Override
			public void componentMoved(ComponentEvent e) {
				AttachedRunsAttachedDialog.this.setVisible(true);
			}

			@Override
			public void componentHidden(ComponentEvent e) {
				log.debug(e.getID());
				AttachedRunsAttachedDialog.this.setVisible(false);
			}
		});
		this.parentFrame.addWindowListener(new WindowListener() {

			@Override
			public void windowOpened(WindowEvent e) {
				log.debug(e.getID() + " from " + e.getOldState() + " to " + e.getNewState());
				// open by default
				AttachedRunsAttachedDialog.this.setVisible(true);
			}

			@Override
			public void windowClosing(WindowEvent e) {
				log.debug(e.getID() + " from " + e.getOldState() + " to " + e.getNewState());

			}

			@Override
			public void windowClosed(WindowEvent e) {
				log.debug(e.getID() + " from " + e.getOldState() + " to " + e.getNewState());
				AttachedRunsAttachedDialog.this.dispose();

			}

			@Override
			public void windowIconified(WindowEvent e) {
				log.debug(e.getID() + " from " + e.getOldState() + " to " + e.getNewState());
				minimized = true;
				AttachedRunsAttachedDialog.this.setVisible(false);

			}

			@Override
			public void windowDeiconified(WindowEvent e) {
				log.debug(e.getID() + " from " + e.getOldState() + " to " + e.getNewState());
				minimized = false;
				AttachedRunsAttachedDialog.this.setVisible(true);

			}

			@Override
			public void windowActivated(WindowEvent e) {
				log.debug(
						e.getID() + " from " + e.getOldState() + " to " + e.getNewState() + " minimized=" + minimized);
				AttachedRunsAttachedDialog.this.setVisible(true);
			}

			@Override
			public void windowDeactivated(WindowEvent e) {
				log.debug(
						e.getID() + " from " + e.getOldState() + " to " + e.getNewState() + " minimized=" + minimized);
				// AttachedHelpDialog.this.setVisible(false);
			}

		});
		this.addWindowListener(new WindowListener() {

			@Override
			public void windowOpened(WindowEvent e) {
				log.debug(e.getID() + " " + e.getNewState() + " from " + e.getOldState());
			}

			@Override
			public void windowIconified(WindowEvent e) {
				log.debug(e.getID() + " " + e.getNewState() + " from " + e.getOldState());
			}

			@Override
			public void windowDeiconified(WindowEvent e) {
				log.debug(e.getID() + " " + e.getNewState() + " from " + e.getOldState());
			}

			@Override
			public void windowDeactivated(WindowEvent e) {
				log.debug(e.getID() + " " + e.getNewState() + " from " + e.getOldState());
			}

			@Override
			public void windowClosing(WindowEvent e) {
				log.debug(e.getID() + " " + e.getNewState() + " from " + e.getOldState());
				setVisible(false);
				minimized = true;

			}

			@Override
			public void windowClosed(WindowEvent e) {
				log.debug(e.getID() + " " + e.getNewState() + " from " + e.getOldState());
			}

			@Override
			public void windowActivated(WindowEvent e) {
				log.debug(e.getID() + " " + e.getNewState() + " from " + e.getOldState());
			}
		});
	}

	@Override
	public void setVisible(boolean b) {
		if (b) {
			if (!minimized) {
				log.debug("setting help dialog visible to " + b);
				super.setVisible(b);
				positionNextToParent();
				parentFrame.requestFocus();
				// if (!scrolledToBeggining) {
				// }
			}
		} else {
			log.debug("setting help dialog visible to " + b);
			super.setVisible(b);
		}
	}

	public void forceVisible() {
		minimized = false;
		setVisible(true);
		ColumnsRunTableUtil.scrollToBeginning(this.scrollPane);

	}

	@Override
	public void dispose() {
		log.info("Dialog dispose");
		super.dispose();
		this.minimized = true;

	}

	private void positionNextToParent() {

		final Point parentLocationOnScreen = this.parentFrame.getLocation();
		final int x = parentLocationOnScreen.x + this.parentFrame.getWidth();
		final int y = parentLocationOnScreen.y;
		this.setLocation(x, y);
		this.setSize(maxWidth, this.parentFrame.getHeight());
		log.debug("Setting position next to the parent frame (" + x + "," + y + ")");
	}

	public void setMinimized(boolean b) {
		this.minimized = b;
	}

	private void createStyles(StyleContext sc) {
		// default text
		final Style defaultStyle = sc.getStyle(StyleContext.DEFAULT_STYLE);
		regularText = sc.addStyle("MainStyle", defaultStyle);
		StyleConstants.setAlignment(regularText, StyleConstants.ALIGN_JUSTIFIED);
		StyleConstants.setFontSize(regularText, 11);
		StyleConstants.setFontFamily(regularText, "optima");
		StyleConstants.setSpaceAbove(regularText, 1);
		StyleConstants.setSpaceBelow(regularText, 3);
		StyleConstants.setLeftIndent(regularText, 5);
		StyleConstants.setRightIndent(regularText, 5);

		defaultRegularText = regularText;
		// StyleConstants.setFirstLineIndent(regularText, 20);

		// biggerRegularText
		this.biggerRegularText = sc.addStyle("biggerRegularText", regularText);
		StyleConstants.setFontSize(biggerRegularText, 13);

		// bold text
		this.boldText = sc.addStyle("boldText", regularText);
		StyleConstants.setBold(boldText, true);

		// heading text
		this.headingText = sc.addStyle("headingText", regularText);
		StyleConstants.setBold(headingText, true);
		StyleConstants.setForeground(headingText, Color.decode("0x000099"));
		StyleConstants.setFontSize(headingText, 12);
		StyleConstants.setSpaceBelow(headingText, 8);

		// subheading
		this.subheadingText = sc.addStyle("subheadingText", regularText);
		StyleConstants.setBold(subheadingText, true);
		StyleConstants.setForeground(subheadingText, Color.decode("0x000099"));
		StyleConstants.setFontSize(subheadingText, 11);
		StyleConstants.setItalic(subheadingText, true);
		StyleConstants.setSpaceBelow(subheadingText, 8);
		StyleConstants.setSpaceAbove(subheadingText, 30);
		defaultSubheadingText = subheadingText;
		// biggerSubHeadingText
		this.biggerHeadingText = sc.addStyle("biggerHeadingText", subheadingText);
		StyleConstants.setFontSize(biggerHeadingText, 14);

		// italic text
		this.italicText = sc.addStyle("italicText", regularText);
		StyleConstants.setItalic(italicText, true);

		// indented text
		indentedText = sc.addStyle("indent text", regularText);
		StyleConstants.setLeftIndent(indentedText, 30);
	}

	private static String removeHTMLTags(String string) {
		return string.replace(HTML_START, "").replace(HTML_END, "").replace(HTML_BOLD_START, "")
				.replace(HTML_BOLD_END, "").replace(HTML_ITALIC_START, "").replace(HTML_ITALIC_END, "")
				.replace(HTML_NEW_LINE, "").replace(HTML_BLANK_SPACE, " ");
	}

}
