package PSI16;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;

public class Intelligent_1 extends Agent {

	private int myId, opponentId;
	private int totalPlayers, matrixSize, rounds, roundsBeforeChange, percentageToBeChanged, percentageChanged;
	private State state;
	private ACLMessage msg;
	private AID mainAgent;

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
		public void action() {
			msg = blockingReceive();
			if (msg != null) {
				switch (state) {
				case s0ReceiveLeagueInfo:
					if (msg.getContent().startsWith("Id#") && msg.getPerformative() == ACLMessage.INFORM) {
						if (validateInfoMessage(msg)) {
							state = State.s1ReceiveGameInfo;
							System.out.println("Estado 0 a 1");
						} else {
							// ERROR
						}
					}
					break;

				case s1ReceiveGameInfo:
					if (msg.getContent().startsWith("NewGame#") && msg.getPerformative() == ACLMessage.INFORM) {
						if (validateNewGameMessage(msg)) {
							state = State.s2SelectPosition;
							System.out.println("Estado 1 a 2");
						} else {
							// ERROR
						}
					}
					break;
				case s2SelectPosition:
					if (msg.getContent().startsWith("Position") && msg.getPerformative() == ACLMessage.REQUEST) {
						ACLMessage positionReply = msg.createReply();
						positionReply.setPerformative(ACLMessage.INFORM);
						/*****************************************/
						positionReply.setContent("Position#" + calculatePosition());
						/*****************************************/
						myAgent.send(positionReply);
						System.out.println("Estado 2 a 3");
						state = State.s3ReceiveRoundResult;
					} else if (msg.getContent().startsWith("Changed") && msg.getPerformative() == ACLMessage.INFORM) {
						if (validateChangedMessage(msg)) {
							state = State.s2SelectPosition;
						} else {
							// ERROR
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

		// Id#ID#N,S,R,I,P.
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
			if (msg.getSender() != mainAgent)
				return false;
			String msgContent = msg.getContent();
			String[] contentSplit = msgContent.split("#");
			if (contentSplit.length != 2)
				return false;

			percentageChanged = Integer.parseInt(contentSplit[1]);
			return true;
		}

		private int calculatePosition() {
			// TODO Auto-generated method stub
			return 0;
		}

		private void processResults(ACLMessage msg) {
			// TODO Auto-generated method stub

		}

	}

}
