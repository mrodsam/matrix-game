package PSI16;

import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

@SuppressWarnings("serial")
public class FixedPlayer extends Agent {

	private final int RECEIVELEAGUEINFO = 0;
	private final int RECEIVEGAMEINFO = 1;
	private final int SELECTPOSITION = 2;
	private final int RECEIVEROUNDRESULTS = 3;

	private int matrixSize;
	private int fixedPosition;

	protected void setup() {

		DFAgentDescription dfad = new DFAgentDescription();
		dfad.setName(getAID());

		ServiceDescription sd = new ServiceDescription();
		sd.setType("Player");
		sd.setName("Matrix game");
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

	private class Game extends Behaviour {
		ACLMessage info = null;
		ACLMessage newgame = null;
		ACLMessage positionMsg = null;
		ACLMessage results = null;
		ACLMessage end = null;
		ACLMessage changed = null;
		int step = RECEIVELEAGUEINFO;

		public void action() {
			switch (step) {
			case RECEIVELEAGUEINFO:
				MessageTemplate mt = MessageTemplate.MatchConversationId("game-info");
				info = myAgent.blockingReceive(mt);

				if (info != null) {
					String output[] = info.getContent().split("#")[2].split(",");
					matrixSize = Integer.parseInt(output[1]);
					fixedPosition = (int) (Math.random() * matrixSize);
					step = RECEIVEGAMEINFO;

				}
				break;

			case RECEIVEGAMEINFO:
				MessageTemplate mt1 = MessageTemplate.MatchConversationId("new-game");
				newgame = myAgent.blockingReceive(mt1);
				if (newgame != null) {
					step = SELECTPOSITION;
				}
				break;
			case SELECTPOSITION:
				/******** A los jugadores tontos esto les da igual ********************/
				MessageTemplate mt5 = MessageTemplate.MatchConversationId("changed");
				changed = myAgent.receive(mt5);
				if (changed != null) {
					// TODO
				}
				/********************************************************************/
				MessageTemplate mt2 = MessageTemplate.MatchConversationId("position");
				positionMsg = myAgent.blockingReceive(mt2);
				if (positionMsg != null) {
					ACLMessage positionReply = positionMsg.createReply();
					positionReply.setPerformative(ACLMessage.INFORM);
					positionReply.setContent("Position#" + String.valueOf(fixedPosition));
					myAgent.send(positionReply);
					step = RECEIVEROUNDRESULTS;
				}
				break;
			case RECEIVEROUNDRESULTS:
				MessageTemplate mt4 = MessageTemplate.MatchConversationId("end-game");
				end = myAgent.receive(mt4);
				if (end != null) {
					step = RECEIVEGAMEINFO;
					break;
				}
				MessageTemplate mt3 = MessageTemplate.MatchConversationId("results");
				results = myAgent.blockingReceive(mt3);
				if (results != null) {
					step = SELECTPOSITION;
				}
				break;
			}

		}

		public boolean done() {
			return step == 4;
		}
	}
}
