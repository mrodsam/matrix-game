package PSI16;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

@SuppressWarnings("serial")
public final class GUI extends JFrame implements ActionListener {
	
	JLabel leftPanelRoundsLabel;
	JLabel leftPanelExtraInformation;
	JLabel leftPanelRankingLabel1;
	JLabel leftPanelRankingLabel2;
	JList<String> playersList;
	JList<String> rankingList;
	private JTable payoffTable;
	private MainAgent mainAgent;
	private JPanel rightPanel;
	private JTextArea rightPanelLoggingTextArea;
	private LoggingOutputStream loggingOutputStream;

	public GUI() {
		initUI();
	}

	public GUI(MainAgent agent) {
		mainAgent = agent;
		initUI();
		loggingOutputStream = new LoggingOutputStream(rightPanelLoggingTextArea);
	}

	public void log(String s) {
		Runnable appendLine = () -> {
			rightPanelLoggingTextArea
					.append('[' + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")) + "] - " + s);
			rightPanelLoggingTextArea.setCaretPosition(rightPanelLoggingTextArea.getDocument().getLength());
		};
		SwingUtilities.invokeLater(appendLine);
	}

	public OutputStream getLoggingOutputStream() {
		return loggingOutputStream;
	}

	public void logLine(String s) {
		log(s + "\n");
	}

	public void setPlayersUI(String[] players) {
		DefaultListModel<String> listModel = new DefaultListModel<>();
		for (String s : players) {
			listModel.addElement(s);
		}
		playersList.setModel(listModel);
	}

	public void setRankingUI(LinkedHashMap<String, Integer> ranking) {
		DefaultListModel<String> listModel = new DefaultListModel<>();
		for (Map.Entry<String, Integer> entry : ranking.entrySet()) {
			String key = entry.getKey();
			Integer value = entry.getValue();
			listModel.addElement(key + " - " + value);
		}
		rankingList.setModel(listModel);
	}

	public void setMatrixUI(String[][] gameMatrix) {
		Object[] nullPointerWorkAround = new Object[mainAgent.parameters.matrixSize];
		for (int i = 0; i < mainAgent.parameters.matrixSize; i++) {
			nullPointerWorkAround[i] = "*";
		}
		DefaultTableModel tableModel = new DefaultTableModel(nullPointerWorkAround, 0);
		for (int i = 0; i < gameMatrix.length; i++) {
			tableModel.addRow(gameMatrix[i]);
		}

		payoffTable.setModel(tableModel);
	}

	public void initUI() {
		setTitle("GUI");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setMinimumSize(new Dimension(600, 400));
		setPreferredSize(new Dimension(1000, 600));
		setJMenuBar(createMainMenuBar());
		setContentPane(createMainContentPane());
		pack();
		setVisible(true);
	}

	private Container createMainContentPane() {
		JPanel pane = new JPanel(new GridBagLayout());
		GridBagConstraints gc = new GridBagConstraints();
		gc.fill = GridBagConstraints.BOTH;
		gc.anchor = GridBagConstraints.FIRST_LINE_START;
		gc.gridy = 0;
		gc.weightx = 0.5;
		gc.weighty = 0.5;

		// LEFT PANEL
		gc.gridx = 0;
		gc.weightx = 1;
		pane.add(createLeftPanel(), gc);

		// CENTRAL PANEL
		gc.gridx = 1;
		gc.weightx = 8;
		pane.add(createCentralPanel(), gc);

		// RIGHT PANEL
		gc.gridx = 2;
		gc.weightx = 8;
		pane.add(createRightPanel(), gc);
		return pane;
	}

	private JPanel createLeftPanel() {
		JPanel leftPanel = new JPanel();
		leftPanel.setLayout(new GridBagLayout());
		GridBagConstraints gc = new GridBagConstraints();

		leftPanelRoundsLabel = new JLabel("Match 0 - Round 0 / " + mainAgent.parameters.rounds);
		JButton leftPanelNewButton = new JButton("New");
		leftPanelNewButton.addActionListener(actionEvent -> mainAgent.newGame());

		leftPanelExtraInformation = new JLabel("Parameters - ");
		leftPanelRankingLabel1 = new JLabel("PlayerA - Payoffs");
		leftPanelRankingLabel2 = new JLabel("PlayerB - Payoffs");

		DefaultListModel<String> listModel = new DefaultListModel<>();
		listModel.addElement("Ranking");
		rankingList = new JList<>(listModel);
		rankingList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		rankingList.setSelectedIndex(0);
		rankingList.setVisibleRowCount(5);
		JScrollPane listScrollPane = new JScrollPane(rankingList);

		gc.fill = GridBagConstraints.HORIZONTAL;
		gc.anchor = GridBagConstraints.FIRST_LINE_START;
		gc.gridx = 0;
		gc.weightx = 0.5;
		gc.weighty = 0.5;

		gc.gridy = 1;
		leftPanel.add(leftPanelNewButton, gc);
		gc.gridy = 4;
		leftPanel.add(leftPanelExtraInformation, gc);
		gc.gridy = 5;
		leftPanel.add(leftPanelRoundsLabel, gc);
		gc.gridy = 6;
		leftPanel.add(leftPanelRankingLabel1, gc);
		gc.gridy = 7;
		leftPanel.add(leftPanelRankingLabel2, gc);
		gc.gridy = 8;
		gc.weighty = 10;
		leftPanel.add(listScrollPane, gc);

		return leftPanel;
	}

	private JPanel createCentralPanel() {
		JPanel centralPanel = new JPanel(new GridBagLayout());

		GridBagConstraints gc = new GridBagConstraints();
		gc.weightx = 0.5;

		gc.fill = GridBagConstraints.BOTH;
		gc.anchor = GridBagConstraints.FIRST_LINE_START;
		gc.gridx = 0;

		gc.gridy = 0;
		gc.weighty = 1;
		centralPanel.add(createCentralTopSubpanel(), gc);
		gc.gridy = 1;
		gc.weighty = 4;
		centralPanel.add(createCentralBottomSubpanel(), gc);
		return centralPanel;
	}

	private JPanel createCentralTopSubpanel() {
		JPanel centralTopSubpanel = new JPanel(new GridBagLayout());

		DefaultListModel<String> listModel = new DefaultListModel<>();
		listModel.addElement("Empty");
		playersList = new JList<>(listModel);
		playersList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		playersList.setSelectedIndex(0);
		playersList.setVisibleRowCount(5);
		JScrollPane listScrollPane = new JScrollPane(playersList);

		JLabel info1 = new JLabel("Selected player info");
		JButton updatePlayersButton = new JButton("Update players");
		updatePlayersButton.addActionListener(actionEvent -> mainAgent.findPlayers());

		GridBagConstraints gc = new GridBagConstraints();
		gc.weightx = 0.5;
		gc.weighty = 0.5;
		gc.anchor = GridBagConstraints.CENTER;

		gc.gridx = 0;
		gc.gridy = 0;
		gc.gridheight = 666;
		gc.fill = GridBagConstraints.BOTH;
		centralTopSubpanel.add(listScrollPane, gc);
		gc.gridx = 1;
		gc.gridheight = 1;
		gc.fill = GridBagConstraints.NONE;
		centralTopSubpanel.add(info1, gc);
		gc.gridy = 1;
		centralTopSubpanel.add(updatePlayersButton, gc);

		return centralTopSubpanel;
	}

	public JPanel createCentralBottomSubpanel() {
		JPanel centralBottomSubpanel = new JPanel(new GridBagLayout());

		Object[] nullPointerWorkAround = new Object[mainAgent.parameters.matrixSize];
		for (int i = 0; i < mainAgent.parameters.matrixSize; i++) {
			nullPointerWorkAround[i] = "*";
		}

		JLabel payoffLabel = new JLabel("Payoff matrix");

		DefaultTableModel tableModel = new DefaultTableModel(nullPointerWorkAround, 0);
		for (int i = 0; i < mainAgent.gameMatrix.length; i++) {
			tableModel.addRow(mainAgent.gameMatrix[i]);
		}

		payoffTable = new JTable(tableModel);
		payoffTable.setTableHeader(null);
		payoffTable.setEnabled(false);

		JScrollPane player1ScrollPane = new JScrollPane(payoffTable);

		GridBagConstraints gc = new GridBagConstraints();
		gc.weightx = 0.5;
		gc.fill = GridBagConstraints.BOTH;
		gc.anchor = GridBagConstraints.FIRST_LINE_START;

		gc.gridx = 0;
		gc.gridy = 0;
		gc.weighty = 0.5;
		centralBottomSubpanel.add(payoffLabel, gc);
		gc.gridy = 1;
		gc.gridx = 0;
		gc.weighty = 2;
		centralBottomSubpanel.add(player1ScrollPane, gc);

		return centralBottomSubpanel;
	}

	private JPanel createRightPanel() {
		rightPanel = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		c.anchor = GridBagConstraints.FIRST_LINE_START;
		c.weighty = 1d;
		c.weightx = 1d;

		rightPanelLoggingTextArea = new JTextArea("");
		rightPanelLoggingTextArea.setEditable(false);
		JScrollPane jScrollPane = new JScrollPane(rightPanelLoggingTextArea);
		rightPanel.add(jScrollPane, c);
		return rightPanel;
	}

	private JMenuBar createMainMenuBar() {
		JMenuBar menuBar = new JMenuBar();

		JMenu menuFile = new JMenu("File");
		JMenuItem exitFileMenu = new JMenuItem("Exit");
		exitFileMenu.setToolTipText("Exit application");
		exitFileMenu.addActionListener(actionEvent -> System.exit(0));

		JMenuItem newGameFileMenu = new JMenuItem("New Game");
		newGameFileMenu.setToolTipText("Start a new game");
		newGameFileMenu.addActionListener(actionEvent -> mainAgent.newGame());

		menuFile.add(newGameFileMenu);
		menuFile.add(exitFileMenu);
		menuBar.add(menuFile);

		JMenu menuEdit = new JMenu("Edit");
		JMenuItem resetPlayerEditMenu = new JMenuItem("Reset Players");
		resetPlayerEditMenu.setToolTipText("Reset all player");
		resetPlayerEditMenu.setActionCommand("reset_players");
		resetPlayerEditMenu.addActionListener(actionEvent -> mainAgent.findPlayers());

		JMenuItem parametersEditMenu = new JMenuItem("Parameters");
		parametersEditMenu.setToolTipText("Modify the parameters of the game");
		parametersEditMenu.addActionListener(actionEvent -> mainAgent.setParameters(
				JOptionPane.showInputDialog(new Frame("Configure parameters"), "Enter parameters N,S,R,I,P")));

		menuEdit.add(resetPlayerEditMenu);
		menuEdit.add(parametersEditMenu);
		menuBar.add(menuEdit);

		JMenu menuRun = new JMenu("Run");

		JMenuItem newRunMenu = new JMenuItem("New");
		newRunMenu.setToolTipText("Starts a new series of games");
		newRunMenu.addActionListener(actionEvent -> mainAgent.newGame());

		JMenuItem roundNumberRunMenu = new JMenuItem("Number Of rounds");
		roundNumberRunMenu.setToolTipText("Change the number of rounds");
		roundNumberRunMenu.addActionListener(actionEvent -> mainAgent
				.setRounds(JOptionPane.showInputDialog(new Frame("Configure rounds"), "How many rounds?")));

		menuRun.add(newRunMenu);
		menuRun.add(roundNumberRunMenu);
		menuBar.add(menuRun);

		JMenu menuWindow = new JMenu("Window");

		JCheckBoxMenuItem toggleVerboseWindowMenu = new JCheckBoxMenuItem("Verbose", true);
		toggleVerboseWindowMenu
				.addActionListener(actionEvent -> rightPanel.setVisible(toggleVerboseWindowMenu.getState()));

		menuWindow.add(toggleVerboseWindowMenu);
		menuBar.add(menuWindow);

		return menuBar;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() instanceof JButton) {
			JButton button = (JButton) e.getSource();
			logLine("Button " + button.getText());
		} else if (e.getSource() instanceof JMenuItem) {
			JMenuItem menuItem = (JMenuItem) e.getSource();
			logLine("Menu " + menuItem.getText());
		}
	}

	public class LoggingOutputStream extends OutputStream {
		private JTextArea textArea;

		public LoggingOutputStream(JTextArea jTextArea) {
			textArea = jTextArea;
		}

		@Override
		public void write(int i) throws IOException {
			textArea.append(String.valueOf((char) i));
			textArea.setCaretPosition(textArea.getDocument().getLength());
		}
	}
}
