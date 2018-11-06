package PSI16;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;

@SuppressWarnings("serial")
public class FixedPlayer extends Agent {

	private int matrixSize;
	private State state;
	private int fixedPosition;
	private ACLMessage msg;

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
						String output[] = msg.getContent().split("#")[2].split(",");
						matrixSize = Integer.parseInt(output[1]);
						fixedPosition = (int) (Math.random() * matrixSize);
						state = State.s1ReceiveGameInfo;
					}
					break;

				case s1ReceiveGameInfo:
					if (msg.getContent().startsWith("NewGame#") && msg.getPerformative() == ACLMessage.INFORM) {
						state = State.s2SelectPosition;
					}
					break;
				case s2SelectPosition:
					if (msg.getContent().startsWith("Position") && msg.getPerformative() == ACLMessage.REQUEST) {
						ACLMessage positionReply = msg.createReply();
						positionReply.setPerformative(ACLMessage.INFORM);
						positionReply.setContent("Position#" + fixedPosition);
						myAgent.send(positionReply);
						state = State.s3ReceiveRoundResult;
					}
					break;
				case s3ReceiveRoundResult:
					if (msg.getContent().startsWith("EndGame") && msg.getPerformative() == ACLMessage.INFORM) {
						state = State.s1ReceiveGameInfo;
						break;
					}

					if (msg.getContent().startsWith("Results#") && msg.getPerformative() == ACLMessage.INFORM) {
						state = State.s2SelectPosition;
					}
					break;
				}
			}

		}

	}

}
