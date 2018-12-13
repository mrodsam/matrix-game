package PSI16;

import java.util.Vector;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;

public class Intelligent_1 extends Agent {

	/**
	 * 
	 */
	private static final long serialVersionUID = -705940755342909716L;

	private int myId, opponentId;
	private boolean first = false;
	private int totalPlayers, matrixSize, rounds, roundsBeforeChange, percentageToBeChanged, percentageChanged;
	private State state;
	private ACLMessage msg;
	private AID mainAgent;
	private String gameMatrix[][];
	private int payoffs;

	final double dDecFactorLR = 0.99;
	final double dMINLearnRate = 0.05;
	double dLearnRate = 0.1;
	int iNewAction;
	int iNumActions = 2;
	int iLastAction;
	double dLastFunEval;
	StateAction oPresentStateAction;
	StateAction oLastStateAction;
	Vector<StateAction> oVStateActions = new Vector<>(10, 5);

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
		/**
		 * 
		 */
		private static final long serialVersionUID = 4022117288173081562L;

		public void action() {
			msg = blockingReceive();
			if (msg != null) {
				switch (state) {
				case s0ReceiveLeagueInfo:
					if (msg.getContent().startsWith("Id#") && msg.getPerformative() == ACLMessage.INFORM) {
						if (validateInfoMessage(msg)) {
							state = State.s1ReceiveGameInfo;
							payoffs = 0;
						} else {
							// ERROR
						}
					}
					break;

				case s1ReceiveGameInfo:
					if (msg.getPerformative() == ACLMessage.INFORM) {
						if (msg.getContent().startsWith("Id#")) {
							if (validateInfoMessage(msg)) {
								state = State.s1ReceiveGameInfo;
								payoffs = 0;
							} else {
								// ERROR
							}
						} else if (msg.getContent().startsWith("NewGame#")) {
							if (validateNewGameMessage(msg)) {
								if (myId < opponentId)
									first = true;
								state = State.s2SelectPosition;
								iNewAction = (int) (Math.random() * (matrixSize - 1));
							} else {
								// ERROR
							}
						}

					}
					break;
				case s2SelectPosition:
					if (msg.getContent().startsWith("Position") && msg.getPerformative() == ACLMessage.REQUEST) {
						ACLMessage positionReply = msg.createReply();
						positionReply.setPerformative(ACLMessage.INFORM);
						/*****************************************/
						positionReply.setContent("Position#" + iNewAction);
						/*****************************************/
						myAgent.send(positionReply);
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

			/******************* DEFINIR ESTADOS Y REWARD *********************/
			System.out.println(getLocalName() + " payoffs = " + payoffs);
			if (first) {
				payoffs += Integer.parseInt(payoffsSplit[0]);
				vGetNewActionAutomata(getState(), matrixSize, Integer.parseInt(payoffsSplit[0]));
			} else {
				payoffs += Integer.parseInt(payoffsSplit[1]);
				vGetNewActionAutomata(getState(), matrixSize, Integer.parseInt(payoffsSplit[1]));
			}

			if (first) {
				iLastAction = row;
			} else {
				iLastAction = column;
			}
			return true;
		}

		public String getState() {
			String state = "";
			for (int i = 0; i < gameMatrix.length; i++) {
				for (int j = 0; j < gameMatrix.length; j++) {
					if (gameMatrix[i][j] != null) {
						state += i;
						state += j;
					}
				}
			}
			System.out.println("Estado: " + state);
			return state;

		}

	}

	public void vGetNewActionAutomata(String sState, int iNActions, double dFunEval) {
		boolean bFound;
		StateAction oStateProbs;

		bFound = false;
		for (int i = 0; i < oVStateActions.size(); i++) {
			oStateProbs = (StateAction) oVStateActions.elementAt(i);
			if (oStateProbs.sState.equals(sState)) {
				oPresentStateAction = oStateProbs;
				bFound = true;
				break;
			}
		}
		if (!bFound) {
			oPresentStateAction = new StateAction(sState, iNActions, true);
			oVStateActions.add(oPresentStateAction);
		}

		if (oLastStateAction != null) {
			if (dFunEval - dLastFunEval > 0)
				for (int i = 0; i < iNActions; i++)
					if (i == iLastAction)
						oLastStateAction.dValAction[i] += dLearnRate * (1.0 - oLastStateAction.dValAction[i]);
					else
						oLastStateAction.dValAction[i] *= (1.0 - dLearnRate);
		}

		double dValAcc = 0;
		double dValRandom = Math.random();
		for (int i = 0; i < iNActions; i++) {
			dValAcc += oPresentStateAction.dValAction[i];
			if (dValRandom < dValAcc) {
				iNewAction = i;
				break;
			}
		}

		oLastStateAction = oPresentStateAction;
		dLastFunEval = dFunEval;
		dLearnRate *= dDecFactorLR;
		if (dLearnRate < dMINLearnRate)
			dLearnRate = dMINLearnRate;
	}

	public class StateAction {
		String sState;
		double[] dValAction;

		StateAction(String sAuxState, int iNActions) {
			sState = sAuxState;
			dValAction = new double[iNActions];
		}

		StateAction(String sAuxState, int iNActions, boolean bLA) {
			this(sAuxState, iNActions);
			if (bLA)
				for (int i = 0; i < iNActions; i++) // This constructor is used for LA and sets up initial probabilities
					dValAction[i] = 1.0 / iNActions;
		}

		public String sGetState() {
			return sState;
		}

		public double dGetQAction(int i) {
			return dValAction[i];
		}
	}
}
