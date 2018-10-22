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
public class Intelligent_1 extends Agent {

	private final int RECEIVELEAGUEINFO = 0;

	private int id;
	private int totalPlayers;
	private int matrixSize;
	private int rounds;
	private int roundsBeforeChange;
	private int percentageChanged;

	protected void setup() {

		/* Registro en las páginas amarillas */
		DFAgentDescription dfad = new DFAgentDescription();
		dfad.setName(getAID());

		ServiceDescription sd = new ServiceDescription();
		sd.setType("player");
		sd.setName("Matrix game");
		dfad.addServices(sd);

		try {
			DFService.register(this, dfad);
		} catch (FIPAException e) {
			e.printStackTrace();
		}

		addBehaviour(new ReceiveInfo());

	}

	private class ReceiveInfo extends Behaviour {
		ACLMessage info = null;
		int step = RECEIVELEAGUEINFO;

		public void action() {
			switch (step) {
			case RECEIVELEAGUEINFO:
				MessageTemplate mt = MessageTemplate.MatchConversationId("game-info");
				info = myAgent.blockingReceive(mt);

				if (info != null) {

					id = Integer.parseInt(info.getContent().split("#")[1]);

					String output[] = info.getContent().split("#")[2].split(",");
					totalPlayers = Integer.parseInt(output[0]);
					matrixSize = Integer.parseInt(output[1]);
					rounds = Integer.parseInt(output[2]);
					roundsBeforeChange = Integer.parseInt(output[3]);
					percentageChanged = Integer.parseInt(output[4]);

					step = 2;

				}
				break;
			}
		}

		public boolean done() {
			// TODO Auto-generated method stub
			return step == 2;
		}
	}
}
