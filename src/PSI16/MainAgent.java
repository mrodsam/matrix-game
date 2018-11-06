package PSI16;

import java.io.PrintStream;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Set;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.SimpleBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;

@SuppressWarnings("serial")
public class MainAgent extends Agent {

	private GUI myGui;

	private State state;

	private AID[] players;
	public GameParameters parameters = new GameParameters();

	private LinkedHashMap<Integer, String> playersPerMatch = new LinkedHashMap<>();
	private int currentMatch;

	private String gameMatrix[][];
	private LinkedHashMap<String, Integer> ranking = new LinkedHashMap<>();

	protected void setup() {

		state = State.s0CalculatePlayersPerMatch;

		myGui = new GUI(this);
		System.setOut(new PrintStream(myGui.getLoggingOutputStream()));
		myGui.leftPanelExtraInformation.setText(parameters.toString());

		findPlayers();
		myGui.logLine("Agent " + getAID().getName() + " is ready.");
	}

	/* Limitar el número de jugadores al parámetro introducido */
	protected int findPlayers() {
		myGui.logLine("Updating player list");

		DFAgentDescription template = new DFAgentDescription();
		ServiceDescription sd = new ServiceDescription();
		sd.setType("Player");
		template.addServices(sd);
		try {
			DFAgentDescription[] result = DFService.search(this, template);
			if (result.length > 0) {
				myGui.logLine("Found " + result.length + " players");
			}
			players = new AID[result.length];
			for (int i = 0; i < players.length; i++) {
				players[i] = result[i].getName();
			}

		} catch (FIPAException e) {
			myGui.log(e.getMessage());
		}

		String[] playerNames = new String[players.length];
		for (int i = 0; i < players.length; i++) {
			playerNames[i] = players[i].getName();
		}
		myGui.setPlayersUI(playerNames);
		return 0;
	}

	public int newGame() {
		addBehaviour(new Init());
		addBehaviour(new GameManager());
		return 0;
	}

	private enum State {
		s0CalculatePlayersPerMatch, s1SendNewGameMessages, s2Play, s3SendEndGameMessages, s4End;
	}

	private class Init extends OneShotBehaviour {

		public void action() {
			String infoContent = "Id#-#" + parameters.totalPlayers + "," + parameters.matrixSize + ","
					+ parameters.rounds + "," + parameters.roundsBeforeChange + "," + parameters.percentageToBeChanged;
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

	private class GameManager extends SimpleBehaviour {

		int playerA;
		int playerB;
		boolean end = false;

		public void action() {
			switch (state) {
			case s0CalculatePlayersPerMatch:
				int match = 1;
				LinkedList<String> combinations = new LinkedList<>();
				boolean stop = false;
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
				state = State.s1SendNewGameMessages;
				break;

			case s1SendNewGameMessages:
				myGui.logLine("Match " + currentMatch);
				if (currentMatch > playersPerMatch.size()) {
					state = State.s4End;
				} else {
					playerA = Integer.parseInt(playersPerMatch.get(currentMatch).split("-")[0]);
					playerB = Integer.parseInt(playersPerMatch.get(currentMatch).split("-")[1]);

					for (int i = 0; i < 2; i++) {
						myGui.logLine("Sending new game message to player " + i);
						ACLMessage newGame = new ACLMessage(ACLMessage.INFORM);
						newGame.addReceiver(players[Integer.parseInt(playersPerMatch.get(currentMatch).split("-")[i])]);
						newGame.setContent("NewGame#" + playerA + "," + playerB);
						myAgent.send(newGame);
					}

					state = State.s2Play;
				}
				break;
			case s2Play:
				myGui.logLine("Player " + players[playerA].getLocalName() + " vs " + players[playerB].getLocalName());
				int row = 0;
				int column = 0;
				int playedRounds = 1;

				createMatrix();

				while (playedRounds <= parameters.rounds) {
					myGui.leftPanelRoundsLabel
							.setText("Match " + currentMatch + " - Round " + playedRounds + " / " + parameters.rounds);
					myGui.leftPanelRoundsLabel.repaint();
					if (parameters.roundsBeforeChange != 0 && playedRounds == parameters.roundsBeforeChange + 1) {
						int percentageChanged = updateMatrix();
						for (int i = 0; i < 2; i++) {
							ACLMessage end = new ACLMessage(ACLMessage.INFORM);
							end.addReceiver(players[Integer.parseInt(playersPerMatch.get(currentMatch).split("-")[i])]);
							end.setContent("Changed#" + percentageChanged);
							myAgent.send(end);
						}
					}
					for (int i = 0; i < 2; i++) {
						ACLMessage position = new ACLMessage(ACLMessage.REQUEST);
						position.addReceiver(
								players[Integer.parseInt(playersPerMatch.get(currentMatch).split("-")[i])]);
						position.setContent("Position");
						myAgent.send(position);

						myGui.logLine("Main send " + position.getContent() + " to: "
								+ players[Integer.parseInt(playersPerMatch.get(currentMatch).split("-")[i])]);

						myGui.logLine("Main Waiting for movement");
						ACLMessage response = myAgent.blockingReceive();
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
						myAgent.send(result);
					}
					myGui.logLine("Result: " + row + "," + column + "#" + gameMatrix[row][column]);
					updateRanking(row, column, playerA, playerB);
					playedRounds++;
					doWait(1000);
				}
				state = State.s3SendEndGameMessages;
				break;

			case s3SendEndGameMessages:
				for (int i = 0; i < 2; i++) {
					ACLMessage end = new ACLMessage(ACLMessage.INFORM);
					end.addReceiver(players[Integer.parseInt(playersPerMatch.get(currentMatch).split("-")[i])]);
					end.setContent("EndGame");
					myAgent.send(end);
				}
				currentMatch++;
				state = State.s1SendNewGameMessages;
				break;
			case s4End:
				myGui.logLine("End game");
				Set<String> keys = ranking.keySet();
				myGui.logLine("Ranking:");
				for (String key : keys) {
					myGui.logLine("Player: " + key + " - " + ranking.get(key));
				}
				end = true;
				break;
			}
		}

		public boolean done() {
			return end;
		}
	}

	private void createMatrix() {
		gameMatrix = new String[parameters.matrixSize][parameters.matrixSize];
		for (int i = 0; i < parameters.matrixSize; i++) {
			for (int j = 0; j < parameters.matrixSize; j++) {
				int a = (int) (Math.random() * 9);
				int b = (int) (Math.random() * 9);
				gameMatrix[i][j] = a + "," + b;
				if (i != j) {
					gameMatrix[j][i] = b + "," + a;
				}

			}
		}
		updateTable();
	}

	private void updateTable() {
		for (int i = 0; i < parameters.matrixSize; i++) {
			for (int j = 0; j < parameters.matrixSize; j++) {
				myGui.data[i][j] = gameMatrix[i][j];
			}
		}

		myGui.payoffTable.repaint();
	}

	/*************************************
	 * ¿¿AQUÍ O EN LA UI??
	 ***********************************************/

	public void setParameters(String showInputDialog) {
		if (showInputDialog != null) {
			parameters = new GameParameters(Integer.parseInt(showInputDialog.split(",")[0]),
					Integer.parseInt(showInputDialog.split(",")[1]), Integer.parseInt(showInputDialog.split(",")[2]),
					Integer.parseInt(showInputDialog.split(",")[3]), Integer.parseInt(showInputDialog.split(",")[4]));

			myGui.leftPanelExtraInformation.setText(parameters.toString());
			myGui.logLine("Parameters: " + parameters.toString());
		}
	}

	public void setRounds(String numberOfRounds) {
		if (numberOfRounds != null) {
			parameters.rounds = Integer.parseInt(numberOfRounds);
			myGui.leftPanelRoundsLabel.setText("Match 0 - Round 0 / " + parameters.rounds);
			myGui.leftPanelExtraInformation.setText(parameters.toString());
			myGui.logLine("Rounds: " + numberOfRounds);
		}

	}

	/*********************************************************************************************************/
	private int updateMatrix() {
		myGui.logLine("Update matrix");
		int percentageChanged = 0;
		int percentageByPosition = 100 / (parameters.matrixSize * parameters.matrixSize);
		String matrix[][] = new String[parameters.matrixSize][parameters.matrixSize];

		while (percentageChanged < parameters.percentageToBeChanged) {
			int row = (int) (Math.random() * parameters.matrixSize);
			int column = (int) (Math.random() * parameters.matrixSize);

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
		updateTable();
		return percentageChanged;
	}

	private void updateRanking(int row, int column, int playerA, int playerB) {
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

	public class GameParameters {
		int totalPlayers;
		int matrixSize;
		int rounds;
		int roundsBeforeChange;
		int percentageToBeChanged;

		public GameParameters() {
			totalPlayers = 4;
			matrixSize = 4;
			rounds = 5;
			roundsBeforeChange = 0;
			percentageToBeChanged = 0;
		}

		public GameParameters(int totalPlayers, int matrixSize, int rounds, int roundsBeforeChange,
				int percentageToBeChanged) {
			super();
			this.totalPlayers = totalPlayers;
			this.matrixSize = matrixSize;
			this.rounds = rounds;
			this.roundsBeforeChange = roundsBeforeChange;
			this.percentageToBeChanged = percentageToBeChanged;
		}

		@Override
		public String toString() {
			return "Parameters [N=" + totalPlayers + ", S=" + matrixSize + ", R=" + rounds + ", I=" + roundsBeforeChange
					+ ", P=" + percentageToBeChanged + "]";
		}

	}

}
