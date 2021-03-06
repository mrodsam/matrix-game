package PSI16;

import java.io.PrintStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
	public String gameMatrix[][];
	private LinkedHashMap<String, Integer> ranking = new LinkedHashMap<>();

	protected void setup() {

		gameMatrix = new String[parameters.matrixSize][parameters.matrixSize];
		myGui = new GUI(this);
		System.setOut(new PrintStream(myGui.getLoggingOutputStream()));
		myGui.leftPanelExtraInformation.setText(parameters.toString());

		findPlayers();
		System.out.println("[" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")) + "] - " + "Agent "
				+ getAID().getName() + " is ready.");
	}

	protected void findPlayers() {
		System.out.println("[" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")) + "] - "
				+ "Updating player list");

		DFAgentDescription template = new DFAgentDescription();
		ServiceDescription sd = new ServiceDescription();
		sd.setType("Player");
		template.addServices(sd);
		try {
			DFAgentDescription[] result = DFService.search(this, template);
			if (result.length > 0) {
				System.out.println("[" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")) + "] - "
						+ "Found " + result.length + " players");
			}
			if (result.length < parameters.totalPlayers)
				players = new AID[result.length];
			else
				players = new AID[parameters.totalPlayers];

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
	}

	public void newGame() {
		state = State.s0CalculatePlayersPerMatch;
		gameMatrix = new String[parameters.matrixSize][parameters.matrixSize];
		createMatrix();
		if (players.length == parameters.totalPlayers) {
			addBehaviour(new Init());
			addBehaviour(new GameManager());
		} else {
			System.out.println("[" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")) + "] - "
					+ "Insufficient number of players");
		}
	}

	private enum State {
		s0CalculatePlayersPerMatch, s1SendNewGameMessages, s2Play, s3SendEndGameMessages, s4End;
	}

	private class Init extends OneShotBehaviour {

		public void action() {
			for (int i = 0; i < players.length; i++) {
				String infoContent = "Id#" + i + "#" + parameters.totalPlayers + "," + parameters.matrixSize + ","
						+ parameters.rounds + "," + parameters.roundsBeforeChange + ","
						+ parameters.percentageToBeChanged;
				ACLMessage info = new ACLMessage(ACLMessage.INFORM);
				info.addReceiver(players[i]);
				info.setContent(infoContent);
				System.out.println("[" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")) + "] - "
						+ "Information about the game: " + infoContent);
				myAgent.send(info);
			}
		}
	}

	private class GameManager extends SimpleBehaviour {

		int playerAId;
		int playerBId;
		int currentMatch;
		LinkedHashMap<Integer, String> playersPerMatch = new LinkedHashMap<>();
		boolean done = false;

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
				if (currentMatch > playersPerMatch.size()) {
					state = State.s4End;
				} else {
					System.out.println("[" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
							+ "] - " + "Match " + currentMatch);
					playerAId = Integer.parseInt(playersPerMatch.get(currentMatch).split("-")[0]);
					playerBId = Integer.parseInt(playersPerMatch.get(currentMatch).split("-")[1]);

					System.out.println("[" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
							+ "] - " + "Sending new game messages");
					ACLMessage newGame = new ACLMessage(ACLMessage.INFORM);
					newGame.addReceiver(players[playerAId]);
					newGame.addReceiver(players[playerBId]);
					newGame.setContent("NewGame#" + playerAId + "," + playerBId);
					myAgent.send(newGame);

					state = State.s2Play;
				}
				break;

			case s2Play:
				System.out.println("[" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")) + "] - "
						+ players[playerAId].getLocalName() + " vs " + players[playerBId].getLocalName());
				int row = 0;
				int column = 0;
				int playedRounds = 1;

				while (playedRounds <= parameters.rounds) {
					myGui.leftPanelRoundsLabel
							.setText("Match " + currentMatch + " - Round " + playedRounds + " / " + parameters.rounds);
					myGui.leftPanelRoundsLabel.repaint();
					if (parameters.roundsBeforeChange != 0 && playedRounds % (parameters.roundsBeforeChange) == 0) {
						int percentageChanged = updateMatrix();
						for (int i = 0; i < 2; i++) {
							ACLMessage change = new ACLMessage(ACLMessage.INFORM);
							change.addReceiver(
									players[Integer.parseInt(playersPerMatch.get(currentMatch).split("-")[i])]);
							change.setContent("Changed#" + percentageChanged);
							myAgent.send(change);
							System.out.println("[" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
									+ "] - " + change.getContent());
						}
					}
					for (int i = 0; i < 2; i++) {
						ACLMessage position = new ACLMessage(ACLMessage.REQUEST);
						position.addReceiver(
								players[Integer.parseInt(playersPerMatch.get(currentMatch).split("-")[i])]);
						position.setContent("Position");
						myAgent.send(position);

						System.out.println("[" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
								+ "] - " + "Main Waiting for movement");
						ACLMessage response = myAgent.blockingReceive();
						System.out.println("[" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
								+ "] - " + "Main received " + response.getContent() + " from "
								+ response.getSender().getLocalName());
						if (i == 0) {
							row = Integer.parseInt(response.getContent().split("#")[1]);
						} else if (i == 1) {
							column = Integer.parseInt(response.getContent().split("#")[1]);
						}
					}

					ACLMessage result = new ACLMessage(ACLMessage.INFORM);
					result.addReceiver(players[playerAId]);
					result.addReceiver(players[playerBId]);
					result.setContent("Results#" + row + "," + column + "#" + gameMatrix[row][column]);
					myAgent.send(result);

					System.out.println("[" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
							+ "] - " + "Result: " + row + "," + column + "#" + gameMatrix[row][column]);
					updateRanking(row, column, playerAId, playerBId);
					playedRounds++;
				}
				state = State.s3SendEndGameMessages;
				break;

			case s3SendEndGameMessages:
				ACLMessage end = new ACLMessage(ACLMessage.INFORM);
				end.addReceiver(players[playerAId]);
				end.addReceiver(players[playerBId]);
				end.setContent("EndGame");
				myAgent.send(end);

				currentMatch++;
				state = State.s1SendNewGameMessages;
				break;

			case s4End:
				System.out.println("[" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")) + "] - "
						+ "End game");
				Set<String> keys = ranking.keySet();
				System.out.println("[" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")) + "] - "
						+ "Ranking:");
				for (String key : keys) {
					System.out.println("[" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
							+ "] - " + key + " - " + ranking.get(key));
				}
				ranking.clear();
				done = true;
				break;
			}
		}

		public boolean done() {
			return done;
		}
	}

	private void createMatrix() {

		gameMatrix = new String[parameters.matrixSize][parameters.matrixSize];
		for (int i = 0; i < parameters.matrixSize; i++) {
			for (int j = 0; j < parameters.matrixSize; j++) {
				int a = (int) (Math.random() * 10);
				int b = (int) (Math.random() * 10);
				gameMatrix[i][j] = a + "," + b;
				if (i != j) {
					gameMatrix[j][i] = b + "," + a;
				}
			}
		}
		myGui.setMatrixUI(gameMatrix);
	}

	public void setParameters(String showInputDialog) {
		if (showInputDialog != null) {
			parameters = new GameParameters(Integer.parseInt(showInputDialog.split(",")[0]),
					Integer.parseInt(showInputDialog.split(",")[1]), Integer.parseInt(showInputDialog.split(",")[2]),
					Integer.parseInt(showInputDialog.split(",")[3]), Integer.parseInt(showInputDialog.split(",")[4]));

			myGui.leftPanelExtraInformation.setText(parameters.toString());
			System.out.println("[" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")) + "] - "
					+ "Parameters: " + parameters.toString());
		}
	}

	public void setRounds(String numberOfRounds) {
		if (numberOfRounds != null) {
			parameters.rounds = Integer.parseInt(numberOfRounds);
			myGui.leftPanelRoundsLabel.setText("Match 0 - Round 0 / " + parameters.rounds);
			myGui.leftPanelExtraInformation.setText(parameters.toString());
			System.out.println("[" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")) + "] - "
					+ "Rounds: " + numberOfRounds);
		}

	}

	private int updateMatrix() {
		System.out.println(
				"[" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")) + "] - " + "Update matrix");
		int percentageChanged = 0;
		int percentageByPosition = 100 / (parameters.matrixSize * parameters.matrixSize);
		String matrix[][] = new String[parameters.matrixSize][parameters.matrixSize];

		while (percentageChanged < parameters.percentageToBeChanged) {
			int row = (int) (Math.random() * parameters.matrixSize);
			int column = (int) (Math.random() * parameters.matrixSize);

			int a = (int) (Math.random() * 10);
			int b = (int) (Math.random() * 10);

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
		myGui.setMatrixUI(gameMatrix);
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
		System.out.println("[" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")) + "] - "
				+ playerAName + ": " + ranking.get(playerAName));
		System.out.println("[" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")) + "] - "
				+ playerBName + ": " + ranking.get(playerBName));

		myGui.setRankingUI(ranking);
	}

	public class GameParameters {
		int totalPlayers;
		int matrixSize;
		int rounds;
		int roundsBeforeChange;
		int percentageToBeChanged;

		public GameParameters() {
			totalPlayers = 5;
			matrixSize = 4;
			rounds = 1000;
			roundsBeforeChange = 0;
			percentageToBeChanged = 0;
		}

		public GameParameters(int totalPlayers, int matrixSize, int rounds, int roundsBeforeChange,
				int percentageToBeChanged) {
			this.totalPlayers = totalPlayers;
			this.matrixSize = matrixSize;
			this.rounds = rounds;
			this.roundsBeforeChange = roundsBeforeChange;
			this.percentageToBeChanged = percentageToBeChanged;
		}

		@Override
		public String toString() {
			return "N=" + totalPlayers + ", S=" + matrixSize + ", R=" + rounds + ", I=" + roundsBeforeChange + ", P="
					+ percentageToBeChanged;
		}

	}

}
