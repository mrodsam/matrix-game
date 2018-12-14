package PSI16.intelligent1;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;

@SuppressWarnings("serial")
public class Intelligent1 extends Agent {

	private int myId, opponentId;
	private boolean first = false;
	private int totalPlayers, matrixSize, rounds, roundsBeforeChange, percentageToBeChanged, percentageChanged;
	private State state;
	private ACLMessage msg;
	private AID mainAgent;
	private String gameMatrix[][];
	private MatrixInfo1 matrixInfo;
	private int position;
	private int roundsCounter;
	private int opponentMove = -1;
	private boolean fixedOpponent;

	protected void setup() {
		state = State.s0ReceiveLeagueInfo;

		DFAgentDescription dfad = new DFAgentDescription();
		dfad.setName(getAID());

		ServiceDescription sd = new ServiceDescription();
		sd.setType("Player");
		sd.setName("Game");
		dfad.addServices(sd);

		try {
			DFService.register(this, dfad);
		} catch (FIPAException e) {
			e.printStackTrace();
		}

		addBehaviour(new Game());
	}

	protected void takeDown() {
		try {
			DFService.deregister(this);
		} catch (FIPAException e) {
			e.printStackTrace();
		}
	}

	private enum State {
		s0ReceiveLeagueInfo, s1ReceiveGameInfo, s2SelectPosition, s3ReceiveRoundResult
	}

	private class Game extends CyclicBehaviour {

		@Override
		public void action() {
			msg = blockingReceive();
			if (msg != null) {
				switch (state) {
				case s0ReceiveLeagueInfo:
					if (msg.getContent().startsWith("Id#") && msg.getPerformative() == ACLMessage.INFORM) {
						if (validateInfoMessage(msg)) {
							state = State.s1ReceiveGameInfo;
							position = 0;
						} else {
							System.out.println("[" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
									+ "] - Info message not valid");
						}
					}
					break;
				case s1ReceiveGameInfo:
					if (msg.getPerformative() == ACLMessage.INFORM) {
						if (msg.getContent().startsWith("Id#")) {
							if (validateInfoMessage(msg)) {
								state = State.s1ReceiveGameInfo;
							} else {
								System.out.println(
										"[" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
												+ "] - Info message not valid");
							}
						} else if (msg.getContent().startsWith("NewGame#")) {
							if (validateNewGameMessage(msg)) {
								fixedOpponent = true;
								if (myId < opponentId)
									first = true;
								roundsCounter = 0;
								position = 0;
								state = State.s2SelectPosition;
							} else {
								System.out.println(
										"[" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
												+ "] - New game message not valid");
							}
						}
					}
					break;
				case s2SelectPosition:
					if (msg.getContent().startsWith("Position") && msg.getPerformative() == ACLMessage.REQUEST) {
						ACLMessage positionReply = msg.createReply();
						positionReply.setPerformative(ACLMessage.INFORM);
						/*****************************************/
						positionReply.setContent("Position#" + position);
						/*****************************************/
						myAgent.send(positionReply);
						roundsCounter++;
						state = State.s3ReceiveRoundResult;
					} else if (msg.getContent().startsWith("Changed") && msg.getPerformative() == ACLMessage.INFORM) {
						if (validateChangedMessage(msg)) {
							state = State.s2SelectPosition;
						} else {
							System.out.println("[" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
									+ "] - Changed message not valid");
						}

					} else if (msg.getContent().startsWith("EndGame") && msg.getPerformative() == ACLMessage.INFORM) {
						state = State.s1ReceiveGameInfo;
						break;
					}
					break;

				case s3ReceiveRoundResult:
					if (msg.getContent().startsWith("Results#") && msg.getPerformative() == ACLMessage.INFORM) {
						processResults(msg);
						state = State.s2SelectPosition;
					}
					break;

				}
			}
		}

		private boolean validateInfoMessage(ACLMessage msg) {
			String msgContent = msg.getContent();

			String[] contentSplit = msgContent.split("#");
			if (contentSplit.length != 3)
				return false;

			myId = Integer.parseInt(contentSplit[1]);

			String[] parametersSplit = contentSplit[2].split(",");
			if (parametersSplit.length != 5)
				return false;
			totalPlayers = Integer.parseInt(parametersSplit[0]);
			matrixSize = Integer.parseInt(parametersSplit[1]);
			rounds = Integer.parseInt(parametersSplit[2]);
			roundsBeforeChange = Integer.parseInt(parametersSplit[3]);
			percentageToBeChanged = Integer.parseInt(parametersSplit[4]);

			/* Iniciar matrices */
			gameMatrix = new String[matrixSize][matrixSize];

			mainAgent = msg.getSender();
			return true;
		}

		private boolean validateNewGameMessage(ACLMessage msg) {
			if (!msg.getSender().equals(mainAgent))
				return false;

			String msgContent = msg.getContent();

			String[] contentSplit = msgContent.split("#");
			if (contentSplit.length != 2)
				return false;

			String[] idSplit = contentSplit[1].split(",");
			if (idSplit.length != 2)
				return false;

			/* Iniciar matrices */
			gameMatrix = new String[matrixSize][matrixSize];
			matrixInfo = new MatrixInfo1(gameMatrix, matrixSize, first, roundsBeforeChange);

			if (myId == Integer.parseInt(idSplit[0])) {
				opponentId = Integer.parseInt(idSplit[1]);
				return true;
			} else if (myId == Integer.parseInt(idSplit[1])) {
				opponentId = Integer.parseInt(idSplit[0]);
				return true;
			}
			return false;
		}

		private boolean validateChangedMessage(ACLMessage msg) {
			if (!msg.getSender().equals(mainAgent))
				return false;
			String msgContent = msg.getContent();
			String[] contentSplit = msgContent.split("#");
			if (contentSplit.length != 2)
				return false;

			percentageChanged = Integer.parseInt(contentSplit[1]);

			/* Reiniciar matrices */
			if (percentageChanged > 30) {
				gameMatrix = new String[matrixSize][matrixSize];
				matrixInfo = new MatrixInfo1(gameMatrix, matrixSize, first, roundsBeforeChange);
			}

			return true;
		}

		private boolean processResults(ACLMessage msg) {
			if (!msg.getSender().equals(mainAgent))
				return false;

			String msgContent = msg.getContent();
			String[] contentSplit = msgContent.split("#");

			if (contentSplit.length != 3)
				return false;

			String[] positionSplit = contentSplit[1].split(",");
			String[] payoffsSplit = contentSplit[2].split(",");

			int row = Integer.parseInt(positionSplit[0]);
			int column = Integer.parseInt(positionSplit[1]);

			gameMatrix[row][column] = contentSplit[2];
			if (row != column) {
				gameMatrix[column][row] = payoffsSplit[1] + "," + payoffsSplit[0];
			}
			if (first) {
				if (opponentMove != -1 && opponentMove != column) {
					fixedOpponent = false;
				}
				opponentMove = column;
				position = matrixInfo.getPosition(row, roundsCounter, fixedOpponent, opponentMove);
			} else {
				if (opponentMove != -1 && opponentMove != row) {
					fixedOpponent = false;
				}
				opponentMove = row;
				position = matrixInfo.getPosition(column, roundsCounter, fixedOpponent, opponentMove);
			}
			return true;
		}
	}
}