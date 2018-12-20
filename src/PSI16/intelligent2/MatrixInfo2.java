package PSI16.intelligent2;

import java.util.LinkedHashMap;

public class MatrixInfo2 {

	private String gameMatrix[][];
	private int matrixSize;
	private boolean first;
	private LinkedHashMap<Integer, RowColumn2> myMatrix;
	boolean allKnown = false;
	int roundsBeforeChange;

	public MatrixInfo2(String[][] gameMatrix, int matrixsize, boolean first, int roundsBeforeChange) {
		this.gameMatrix = gameMatrix;
		this.matrixSize = matrixsize;
		this.first = first;
		this.roundsBeforeChange = roundsBeforeChange;
		allKnown = false;

		myMatrix = new LinkedHashMap<>();
		for (int i = 0; i < matrixSize; i++) {
			myMatrix.put(i, new RowColumn2(i));
		}

	}

	public void setGameMatrix(String[][] gameMatrix) {
		this.gameMatrix = gameMatrix;
	}

	private void updateMyMatrix(int lastPosition) {

		/* Update movements counter */
		myMatrix.get(lastPosition).setMovements(myMatrix.get(lastPosition).getMovements() + 1);
		if (first) {

			/*
			 * Update the sum of payoffs for each row
			 */

			for (int i = 0; i < gameMatrix.length; i++) {
				int myTotalSum = 0;
				int opponentTotalSum = 0;
				for (int j = 0; j < gameMatrix.length; j++) {
					if (gameMatrix[i][j] != null) {
						myTotalSum += Integer.parseInt(gameMatrix[i][j].split(",")[0]);
						opponentTotalSum += Integer.parseInt(gameMatrix[i][j].split(",")[1]);
					}
				}
				myMatrix.get(i).setTotalSum(myTotalSum);
				myMatrix.get(i).setOpponentTotalSum(opponentTotalSum);
			}

			/* Update known percentage for each row */
			for (int i = 0; i < gameMatrix.length; i++) {
				int percentage = 0;
				for (int j = 0; j < gameMatrix.length; j++) {
					if (gameMatrix[i][j] != null) {
						percentage += 1;
					}
					myMatrix.get(i).setKnownPercentage((percentage / (double) matrixSize) * Double.valueOf(100));
				}

			}

		} else {

			/*
			 * Update the sum of payoffs for each row
			 */
			for (int i = 0; i < gameMatrix.length; i++) {
				int myTotalSum = 0;
				int opponentTotalSum = 0;
				for (int j = 0; j < gameMatrix.length; j++) {
					if (gameMatrix[j][i] != null) {
						myTotalSum += Integer.parseInt(gameMatrix[j][i].split(",")[1]);
						opponentTotalSum += Integer.parseInt(gameMatrix[j][i].split(",")[0]);
					}
				}
				myMatrix.get(i).setTotalSum(myTotalSum);
				myMatrix.get(i).setOpponentTotalSum(opponentTotalSum);
			}

			/* Update known percentage for every column */
			for (int i = 0; i < gameMatrix.length; i++) {
				double percentage = 0;
				for (int j = 0; j < gameMatrix.length; j++) {
					if (gameMatrix[j][i] != null) {
						percentage += 1;
					}
				}
				myMatrix.get(i).setKnownPercentage((percentage / (double) matrixSize) * Double.valueOf(100));
			}
		}

		for (int i = 0; i < gameMatrix.length; i++) {
			for (int j = 0; j < gameMatrix.length; j++) {
				// System.out.print(gameMatrix[i][j] + "\t");
			}
			// System.out.println();
		}

		for (Integer key : myMatrix.keySet()) {
			// System.out.println(myMatrix.get(key));
		}

	}

	public int getPosition(int lastPosition, int roundsCounter, boolean fixedOpponent, int opponentMove) {
		updateMyMatrix(lastPosition);
		int position = (int) Math.floor(Math.random() * matrixSize);

		/* Play randomly during 5 rounds, trying to know something of each row/column */
		if (roundsCounter < 5) {
			boolean init = false;
			for (Integer rc : myMatrix.keySet()) {
				if (myMatrix.get(rc).getKnownPercentage() == 0) {
					init = true;
					break;
				}
			}
			while (init) {
				position = (int) Math.floor(Math.random() * matrixSize);
				if (myMatrix.get(position).getKnownPercentage() == 0) {
					break;
				}
			}
			return position;
		} else {
			/*
			 * If fixed opponent, choose always the max value of the row/column
			 */
			if (fixedOpponent) {
				/* Choose at least once every possible movement */
				if (!allKnown) {
					for (Integer key : myMatrix.keySet()) {
						if (myMatrix.get(key).getMovements() == 0) {
							return key;
						}
					}
					allKnown = true;
				}

				return fixedOpponentStrategy(opponentMove, position);

			} else {
				/*
				 * To keep learning: probability of choosing randomly p = 0.2
				 */
				if (Math.random() < 0.2) {
					return (int) (Math.random() * matrixSize);
				} else {
					return maxSumStrategy(position);
				}
			}

		}
	}

	private int maxSumStrategy(int position) {
		RowColumn2 lastMaxSumValue = new RowColumn2();
		for (Integer key : myMatrix.keySet()) {
			/*
			 * Choose the row/column with the higher sum of payoffs
			 * */
			if (myMatrix.get(key).getTotalSum() >= lastMaxSumValue.getTotalSum()) {
				if (myMatrix.get(key).getTotalSum() == lastMaxSumValue.getTotalSum()) {
					if (myMatrix.get(key).getOpponentTotalSum() < lastMaxSumValue.getOpponentTotalSum()) {
						position = key;
						lastMaxSumValue = myMatrix.get(key);
					}
				} else {
					position = key;
					lastMaxSumValue = myMatrix.get(key);
				}
			}
		}
		return position;
	}

	private int fixedOpponentStrategy(int opponentMove, int position) {
		int lastValue = 0;
		int lastPos = -1;
		if (first) {
			for (int i = 0; i < gameMatrix.length; i++) {
				if (gameMatrix[i][opponentMove] != null
						&& Integer.parseInt(gameMatrix[i][opponentMove].split(",")[0]) >= lastValue) {
					/*
					 * If two equal max values, choose the worst for my opponent
					 */
					if (lastPos != -1 && Integer.parseInt(gameMatrix[opponentMove][i].split(",")[0]) == lastValue) {

						if (Integer.parseInt(gameMatrix[lastPos][opponentMove].split(",")[1]) > Integer
								.parseInt(gameMatrix[i][opponentMove].split(",")[1])) {
							position = i;
							lastValue = Integer.parseInt(gameMatrix[opponentMove][i].split(",")[0]);
							lastPos = i;
						}
					} else {
						position = i;
						lastValue = Integer.parseInt(gameMatrix[i][opponentMove].split(",")[0]);
						lastPos = i;
					}
				}
			}
		} else {
			for (int i = 0; i < gameMatrix.length; i++) {
				if (gameMatrix[opponentMove][i] != null
						&& Integer.parseInt(gameMatrix[opponentMove][i].split(",")[1]) >= lastValue) {
					if (lastPos != -1 && Integer.parseInt(gameMatrix[opponentMove][i].split(",")[1]) == lastValue) {
						if (Integer.parseInt(gameMatrix[opponentMove][lastPos].split(",")[0]) > Integer
								.parseInt(gameMatrix[opponentMove][i].split(",")[0])) {
							position = i;
							lastValue = Integer.parseInt(gameMatrix[opponentMove][i].split(",")[1]);
							lastPos = i;
						}
					} else {
						position = i;
						lastValue = Integer.parseInt(gameMatrix[opponentMove][i].split(",")[1]);
						lastPos = i;
					}
				}
			}
		}
		return position;
	}
}
