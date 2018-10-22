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

	private int totalPlayers;
	private int matrixSize;
	private int rounds;
	private int roundsBeforeChange;
	private int percentageToBeChanged;

	private LinkedHashMap<Integer, String> playersPerMatch;
	private int currentMatch;

	public String gameMatrix[][];

	private LinkedHashMap<String, Integer> ranking;

	protected void setup() {

//		Object[] args = getArguments();
//		if (args != null && args.length == 5) {

//			totalPlayers = Integer.parseInt((String) args[0]);
//			matrixSize = Integer.parseInt((String) args[1]);
//			rounds = Integer.parseInt((String) args[2]);
//			roundsBeforeChange = Integer.parseInt((String) args[3]);
//			percentageToBeChanged = Integer.parseInt((String) args[4]);

		totalPlayers = 3;
		matrixSize = 4;
		rounds = 10;
		roundsBeforeChange = 0;
		percentageToBeChanged = 10;
		createMatrix();
		
		myGui = new GUI(this);
		System.setOut(new PrintStream(myGui.getLoggingOutputStream()));

		findPlayers();
		myGui.logLine("Agent " + getAID().getName() + " is ready.");
		ranking = new LinkedHashMap<>();

//		} else {
//			doDelete();
//		}
	}

	protected int findPlayers() {
//		boolean playersReady = false;
		/* Espera activa => botón de update */
//		while (!playersReady) {
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
//			System.out.println("Encontrados los siguientes jugadores: ");
			for (int i = 0; i < players.length; i++) {
				players[i] = result[i].getName();
//				System.out.println(players[i].getName());
			}
//				if (players.length == totalPlayers) {
//					playersReady = true;
//				}
		} catch (FIPAException e) {
			myGui.log(e.getMessage());
		}
//		}
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
		for (int i = 0; i < matrixSize; i++) {
			for (int j = 0; j < matrixSize; j++) {
//				System.out.print(gameMatrix[i][j] + "\t");
			}
//			System.out.println();
		}
	}

	protected void takeDown() {
//		System.out.println(getAID().getName() + " finalizado.");
	}

	private class Init extends OneShotBehaviour {

		public void action() {
			// Id#ID#N,S,R,I,P
			String infoContent = "#-#" + totalPlayers + "," + matrixSize + "," + rounds + "," + roundsBeforeChange + ","
					+ percentageToBeChanged;
			for (int i = 0; i < players.length; i++) {
				myGui.logLine("Sending information to player"+i);
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
//				System.out.println("currentMatch: " + currentMatch);
				myGui.logLine("Match "+currentMatch);
				if (currentMatch > playersPerMatch.size()) {
					step = END;
				} else {
					playerA = Integer.parseInt(playersPerMatch.get(currentMatch).split("-")[0]);
					playerB = Integer.parseInt(playersPerMatch.get(currentMatch).split("-")[1]);

					for (int i = 0; i < 2; i++) {
						myGui.logLine("Sending new game message to player "+i);
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
//				System.out.println(
//						"Playing: " + players[playerA].getLocalName() + "vs" + players[playerB].getLocalName());
				myGui.log("Player "+players[playerA].getLocalName()+" vs "+players[playerB].getLocalName());
				int row = 0;
				int column = 0;
				int playedRounds = 1;

//				createMatrix();

				while (playedRounds <= rounds) {
//					System.out.println("Ronda: " + playedRounds);
					if (playedRounds == roundsBeforeChange + 1) {
						updateMatrix();
					}
					/*Preguntar si esto está bien*/
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
						myGui.logLine("Main received "+response.getContent()+"from"+response.getSender().getLocalName());
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
//					System.out.println("Resultados: " + row + "," + column + "#" + gameMatrix[row][column]);
					myGui.logLine("Result: "+ row + "," + column + "#" + gameMatrix[row][column]);
					updateRanking(row, column);
					playedRounds++;
				}
				step = SENDENDGAMEMESSAGE;
				break;

			case SENDENDGAMEMESSAGE:
//				System.out.println("Fin de partido.");
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
//				System.out.println("Fin de liga.");
				myGui.logLine("End game");
				Set<String> keys = ranking.keySet();
				myGui.log("Ranking:");
				for (String key : keys) {
					myGui.logLine("Player: "+key+" Score: "+ranking.get(key));
//					System.out.println("Jugador: " + key + " Puntuación: " + ranking.get(key));
				}
				step = 5;
				break;
			}
		}

		private void updateMatrix() {
			int percentageChanged = 0;
			int percentageByPosition = 100 / (matrixSize * matrixSize);
			String matrix[][] = new String[matrixSize][matrixSize];

			for (int i = 0; i < matrixSize; i++) {
				for (int j = 0; j < matrixSize; j++) {
//					System.out.print(gameMatrix[i][j] + "\t");
				}
//				System.out.println();
			}

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

//			System.out.println("Porcentaje cambiado: " + percentageChanged);
			for (int i = 0; i < matrixSize; i++) {
				for (int j = 0; j < matrixSize; j++) {
//					System.out.print(gameMatrix[i][j] + "\t");
				}
//				System.out.println();
			}
			for (int i = 0; i < 2; i++) {
				ACLMessage end = new ACLMessage(ACLMessage.INFORM);
				end.addReceiver(players[Integer.parseInt(playersPerMatch.get(currentMatch).split("-")[i])]);
				end.setContent("Changed#" + percentageChanged);
				end.setConversationId("changed");
				myAgent.send(end);
			}
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
			
			myGui.logLine(playerAName+": "+ranking.get(playerAName));
			myGui.logLine(playerBName+": "+ranking.get(playerBName));
		}

		public boolean done() {
			return step == 5;
		}
	}
}
