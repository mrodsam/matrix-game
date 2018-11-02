package PSI16;

import java.io.PrintStream;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Set;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

@SuppressWarnings("serial")
public class MainAgent extends Agent {

	private GUI myGui;

	private final int CALCULATEPLAYERSPERMATCH = 0;
	private final int SENDNEWGAMEMESSAGES = 1;
	private final int PLAY = 2;
	private final int SENDENDGAMEMESSAGE = 3;
	private final int END = 4;

	private AID[] players;

	public int totalPlayers;
	public int matrixSize;
	public int rounds;
	public int roundsBeforeChange;
	public int percentageToBeChanged;
	public int numberOfMatches;

	private LinkedHashMap<Integer, String> playersPerMatch;
	private int currentMatch;

	public String gameMatrix[][];

	public LinkedHashMap<String, Integer> ranking;

	protected void setup() {

		totalPlayers = 4;
		matrixSize = 4;
		rounds = 5;
		roundsBeforeChange = 2;
		percentageToBeChanged = 50;
		numberOfMatches = totalPlayers * (totalPlayers - 1) / 2;

		myGui = new GUI(this);
		System.setOut(new PrintStream(myGui.getLoggingOutputStream()));
		myGui.leftPanelExtraInformation.setText("Parameters - " + totalPlayers + ", " + matrixSize + ", " + rounds
				+ ", " + roundsBeforeChange + ", " + percentageToBeChanged);

		findPlayers();
		myGui.logLine("Agent " + getAID().getName() + " is ready.");
		ranking = new LinkedHashMap<>();
	}

	protected int findPlayers() {
		myGui.logLine("Updating player list");
		DFAgentDescription template = new DFAgentDescription();
		ServiceDescription sd = new ServiceDescription();
		sd.setType("Player");
		template.addServices(sd);
		try {
			DFAgentDescription[] result = DFService.search(this, template);
			players = new AID[result.length];
			if (result.length > 0) {
				myGui.logLine("Found " + result.length + " players");
			}
			for (int i = 0; i < players.length; i++) {
				players[i] = result[i].getName();
			}

		} catch (FIPAException e) {
			myGui.log(e.getMessage());
		}

		// Provisional
		String[] playerNames = new String[players.length];
		for (int i = 0; i < players.length; i++) {
			playerNames[i] = players[i].getName();
		}
		myGui.setPlayersUI(playerNames);
		return 0;
	}

	public int newGame() {
		addBehaviour(new Init());
		addBehaviour(new Play());
		return 0;
	}

	private void createMatrix() {
		gameMatrix = new String[matrixSize][matrixSize];
		for (int i = 0; i < matrixSize; i++) {
			for (int j = 0; j < matrixSize; j++) {
				int a = (int) (Math.random() * 9);
				int b = (int) (Math.random() * 9);
				gameMatrix[i][j] = a + "," + b;
				if (i != j) {
					gameMatrix[j][i] = b + "," + a;
				}

			}
		}
		/*************** MOSTRAR LA MATRIZ ***********/
//		myGui.logLine("New matrix");
//		for (int i = 0; i < matrixSize; i++) {
//			for (int j = 0; j < matrixSize; j++) {
//				System.out.print(gameMatrix[i][j] + "\t");
//			}
//			System.out.println();
//		}
		/*******************************************/
		updateTable();
	}

	private void updateTable() {
		int offset = matrixSize * (currentMatch - 1) + (currentMatch - 1);
		if (currentMatch == 1) {
			offset = 0;
		}
		for (int i = 0, x = offset + 1; i < matrixSize + 1; i++, x++) {
			for (int j = 0; j < matrixSize; j++) {
				if (i == matrixSize) {
					if (j == 0) {
						myGui.data[x][j] = "Match " + currentMatch;
					}
				} else {
					myGui.data[x][j] = gameMatrix[i][j];
				}
			}
		}
		myGui.payoffTable.repaint();
	}

	private class Init extends OneShotBehaviour {

		public void action() {
			// Id#ID#N,S,R,I,P
			String infoContent = "#-#" + totalPlayers + "," + matrixSize + "," + rounds + "," + roundsBeforeChange + ","
					+ percentageToBeChanged;
			myGui.logLine("Information about the game: " + infoContent);
			for (int i = 0; i < players.length; i++) {
				infoContent = infoContent.replace("-", String.valueOf(i));
				ACLMessage info = new ACLMessage(ACLMessage.INFORM);
				info.addReceiver(players[i]);
				info.setContent(infoContent);
				info.setConversationId("game-info");
				myAgent.send(info);
				infoContent = infoContent.replace(String.valueOf(i), "-");
			}
		}
	}

	private class Play extends Behaviour {

		int step = CALCULATEPLAYERSPERMATCH;
		int playerA;
		int playerB;

		public void action() {
			switch (step) {
			case CALCULATEPLAYERSPERMATCH:
				int match = 1;
				LinkedList<String> combinations = new LinkedList<>();
				boolean stop = false;
				playersPerMatch = new LinkedHashMap<>();
				currentMatch = 1;

				for (int i = 0; i < players.length; i++) {
					for (int j = 0; j < players.length; j++) {
						stop = false;
						for (String string : combinations) {
							if (string.equals(i + "-" + j)) {
								stop = true;
							}
						}
						if (i != j && !stop) {
							playersPerMatch.put(match, i + "-" + j);
							combinations.add(j + "-" + i);
							match++;
						}
					}
				}
				step = SENDNEWGAMEMESSAGES;
				break;

			case SENDNEWGAMEMESSAGES:
				myGui.logLine("Match " + currentMatch);
				if (currentMatch > playersPerMatch.size()) {
					step = END;
				} else {
					playerA = Integer.parseInt(playersPerMatch.get(currentMatch).split("-")[0]);
					playerB = Integer.parseInt(playersPerMatch.get(currentMatch).split("-")[1]);

					for (int i = 0; i < 2; i++) {
						myGui.logLine("Sending new game message to player " + i);
						ACLMessage newGame = new ACLMessage(ACLMessage.INFORM);
						newGame.addReceiver(players[Integer.parseInt(playersPerMatch.get(currentMatch).split("-")[i])]);
						newGame.setContent("#" + playerA + "," + playerB);
						newGame.setConversationId("new-game");
						myAgent.send(newGame);
					}

					step = PLAY;
				}
				break;
			case PLAY:
				myGui.logLine("Player " + players[playerA].getLocalName() + " vs " + players[playerB].getLocalName());
				int row = 0;
				int column = 0;
				int playedRounds = 1;

				createMatrix();

				while (playedRounds <= rounds) {
					myGui.leftPanelRoundsLabel
							.setText("Match " + currentMatch + " - Round " + playedRounds + " / " + rounds);
					myGui.leftPanelRoundsLabel.repaint();
					if (playedRounds == roundsBeforeChange + 1) {
						updateMatrix();
						/**** Actualizar tabla UI ***/
					}
					/* Preguntar si esto está bien */
					for (int i = 0; i < 2; i++) {
						ACLMessage position = new ACLMessage(ACLMessage.REQUEST);
						position.addReceiver(
								players[Integer.parseInt(playersPerMatch.get(currentMatch).split("-")[i])]);
						position.setContent("Position");
						position.setConversationId("position");
						myAgent.send(position);

						MessageTemplate mt = MessageTemplate.MatchConversationId("position");
						myGui.logLine("Main Waiting for movement");
						ACLMessage response = myAgent.blockingReceive(mt);
						myGui.logLine("Main received " + response.getContent() + " from "
								+ response.getSender().getLocalName());
						if (i == 0) {
							row = Integer.parseInt(response.getContent().split("#")[1]);
						} else if (i == 1) {
							column = Integer.parseInt(response.getContent().split("#")[1]);
						}

					}
					for (int i = 0; i < 2; i++) {
						ACLMessage result = new ACLMessage(ACLMessage.INFORM);
						result.addReceiver(players[Integer.parseInt(playersPerMatch.get(currentMatch).split("-")[i])]);
						result.setContent("Results#" + row + "," + column + "#" + gameMatrix[row][column]);
						result.setConversationId("results");
						myAgent.send(result);
					}
					myGui.logLine("Result: " + row + "," + column + "#" + gameMatrix[row][column]);
					updateRanking(row, column);
					playedRounds++;
					doWait(1000);
				}
				step = SENDENDGAMEMESSAGE;
				break;

			case SENDENDGAMEMESSAGE:
				for (int i = 0; i < 2; i++) {
					ACLMessage end = new ACLMessage(ACLMessage.INFORM);
					end.addReceiver(players[Integer.parseInt(playersPerMatch.get(currentMatch).split("-")[i])]);
					end.setContent("EndGame");
					end.setConversationId("end-game");
					myAgent.send(end);
				}
				currentMatch++;
				step = SENDNEWGAMEMESSAGES;
				break;
			case END:
				myGui.logLine("End game");
				Set<String> keys = ranking.keySet();
				myGui.logLine("Ranking:");
				for (String key : keys) {
					myGui.logLine("Player: " + key + " - " + ranking.get(key));
				}
				step = 5;
				break;
			}
		}

		private void updateMatrix() {
			myGui.logLine("Update matrix");
			int percentageChanged = 0;
			int percentageByPosition = 100 / (matrixSize * matrixSize);
			String matrix[][] = new String[matrixSize][matrixSize];

			while (percentageChanged < percentageToBeChanged) {
				int row = (int) (Math.random() * matrixSize);
				int column = (int) (Math.random() * matrixSize);

				int a = (int) (Math.random() * 9);
				int b = (int) (Math.random() * 9);

				if (matrix[column][row] != null || matrix[row][column] != null) {
					continue;
				}

				gameMatrix[row][column] = a + "," + b;
				matrix[row][column] = a + "," + b;

				if (row == column) {
					percentageChanged += percentageByPosition;
				}

				if (row != column) {
					gameMatrix[column][row] = b + "," + a;
					percentageChanged += 2 * percentageByPosition;
				}
			}

			/*************************************************/

			for (int i = 0; i < matrixSize; i++) {
				for (int j = 0; j < matrixSize; j++) {
//					System.out.print(gameMatrix[i][j] + "\t");
				}
//				System.out.println();
			}
			/*************************************************/
			for (int i = 0; i < 2; i++) {
				ACLMessage end = new ACLMessage(ACLMessage.INFORM);
				end.addReceiver(players[Integer.parseInt(playersPerMatch.get(currentMatch).split("-")[i])]);
				end.setContent("Changed#" + percentageChanged);
				end.setConversationId("changed");
				myAgent.send(end);
			}
			updateTable();
		}

		private void updateRanking(int row, int column) {
			String playerAName = players[playerA].getLocalName();
			String playerBName = players[playerB].getLocalName();

			int score = 0;

			if (ranking.get(playerAName) != null) {
				score = ranking.get(playerAName) + Integer.parseInt(gameMatrix[row][column].split(",")[0]);
			} else {
				score = Integer.parseInt(gameMatrix[row][column].split(",")[0]);
			}
			ranking.put(playerAName, score);
			score = 0;
			if (ranking.get(playerBName) != null) {
				score = ranking.get(playerBName) + Integer.parseInt(gameMatrix[row][column].split(",")[1]);
			} else {
				score = Integer.parseInt(gameMatrix[row][column].split(",")[1]);
			}
			ranking.put(playerBName, score);

			myGui.leftPanelRankingLabel1.setText(playerAName + ": " + ranking.get(playerAName));
			myGui.leftPanelRankingLabel2.setText(playerBName + ": " + ranking.get(playerBName));
			myGui.logLine(playerAName + ": " + ranking.get(playerAName));
			myGui.logLine(playerBName + ": " + ranking.get(playerBName));
			
			myGui.setRankingUI(ranking);
		}

		public boolean done() {
			return step == 5;
		}
	}

	public void setParameters(String showInputDialog) {
		if (showInputDialog != null) {
			totalPlayers = Integer.parseInt(showInputDialog.split(",")[0]);
			matrixSize = Integer.parseInt(showInputDialog.split(",")[1]);
			rounds = Integer.parseInt(showInputDialog.split(",")[2]);
			roundsBeforeChange = Integer.parseInt(showInputDialog.split(",")[3]);
			percentageToBeChanged = Integer.parseInt(showInputDialog.split(",")[4]);
			myGui.leftPanelExtraInformation.setText("Parameters - " + totalPlayers + ", " + matrixSize + ", " + rounds
					+ ", " + roundsBeforeChange + ", " + percentageToBeChanged);
			myGui.logLine("Parameters: " + showInputDialog);
		}
	}

	public void setRounds(String numberOfRounds) {
		if (numberOfRounds != null) {
			rounds = Integer.parseInt(numberOfRounds);
			myGui.leftPanelRoundsLabel.setText("Match 0 - Round 0 / " + rounds);
			myGui.leftPanelExtraInformation.setText("Parameters - " + totalPlayers + ", " + matrixSize + ", " + rounds
					+ ", " + roundsBeforeChange + ", " + percentageToBeChanged);
			myGui.logLine("Rounds: " + numberOfRounds);
		}

	}
}
