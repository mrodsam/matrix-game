package PSI16.intelligent1;

import java.util.LinkedHashMap;

public class MatrixInfo1 {

	private String gameMatrix[][];
	private int matrixSize;
	private boolean first;
	private LinkedHashMap<Integer, RowColumn1> myMatrix;
	boolean allKnown = false;
	int roundsBeforeChange;
	double p;

	public MatrixInfo1(String[][] gameMatrix, int matrixsize, boolean first, int roundsBeforeChange) {
		this.gameMatrix = gameMatrix;
		this.matrixSize = matrixsize;
		this.first = first;
		this.roundsBeforeChange = roundsBeforeChange;
		allKnown = false;
		p = 0.2;

		myMatrix = new LinkedHashMap<>();
		for (int i = 0; i < matrixSize; i++) {
			myMatrix.put(i, new RowColumn1(i, first));
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
			 * Update min score and opponent score with your min score for each row
			 */
			for (int i = 0; i < gameMatrix.length; i++) {
				for (int j = 0; j < gameMatrix.length; j++) {
					if (gameMatrix[i][j] != null) {
						if (Integer.parseInt(gameMatrix[i][j].split(",")[0]) <= myMatrix.get(i).getMinValue()) {
							if (Integer.parseInt(gameMatrix[i][j].split(",")[0]) == myMatrix.get(i).getMinValue()) {
								if (Integer.parseInt(gameMatrix[i][j].split(",")[1]) < myMatrix.get(i)
										.getOpponentValueWithMyMinValue()) {
									myMatrix.get(i).setMinValue(Integer.parseInt(gameMatrix[i][j].split(",")[0]));
									myMatrix.get(i).setOpponentValueWithMyMinValue(
											Integer.parseInt(gameMatrix[i][j].split(",")[1]));
								}

							} else {
								myMatrix.get(i).setMinValue(Integer.parseInt(gameMatrix[i][j].split(",")[0]));
								myMatrix.get(i).setOpponentValueWithMyMinValue(
										Integer.parseInt(gameMatrix[i][j].split(",")[1]));
							}
						}
					}
				}
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
			 * Update min score and opponent score with your min score for every column
			 */
			for (int i = 0; i < gameMatrix.length; i++) {
				for (int j = 0; j < gameMatrix.length; j++) {
					if (gameMatrix[i][j] != null) {
						if (Integer.parseInt(gameMatrix[i][j].split(",")[1]) <= myMatrix.get(j).getMinValue()) {
							if (Integer.parseInt(gameMatrix[i][j].split(",")[1]) == myMatrix.get(j).getMinValue()) {
								if (Integer.parseInt(gameMatrix[i][j].split(",")[0]) < myMatrix.get(j)
										.getOpponentValueWithMyMinValue()) {
									myMatrix.get(j).setMinValue(Integer.parseInt(gameMatrix[i][j].split(",")[1]));
									myMatrix.get(j).setOpponentValueWithMyMinValue(
											Integer.parseInt(gameMatrix[i][j].split(",")[0]));
								}
							} else {
								myMatrix.get(j).setMinValue(Integer.parseInt(gameMatrix[i][j].split(",")[1]));
								myMatrix.get(j).setOpponentValueWithMyMinValue(
										Integer.parseInt(gameMatrix[i][j].split(",")[0]));
							}
						}
					}
				}
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
				//System.out.print(gameMatrix[i][j] + "\t");
			}
			//System.out.println();
		}

		for (Integer key : myMatrix.keySet()) {
			//System.out.println(myMatrix.get(key));
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
					//System.out.println("Escojo " + position + " porque <5 rounds y no conozco nada");
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
							//System.out.println("Escojo " + key + " porque no la escog� nunca");
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
				if (Math.random() < p) {
					//System.out.println("Escojo RANDOM");
					return (int) (Math.random() * matrixSize);
				} else {
					return minMaxStrategy(position);
				}
			}
		}
	}

	private int minMaxStrategy(int position) {
		//System.out.println("Estrategia min-max");
		RowColumn1 lastMinValue = new RowColumn1();
		for (Integer key : myMatrix.keySet()) {
			/*
			 * Choose the row/column with max value in the worst case (min-max strategy)
			 */
			if (myMatrix.get(key).getMinValue() >= lastMinValue.getMinValue()) {
				if (myMatrix.get(key).getMinValue() == lastMinValue.getMinValue()) {
					if (lastMinValue.getOpponentValueWithMyMinValue() > myMatrix.get(key)
							.getOpponentValueWithMyMinValue()) {
						position = key;
						lastMinValue = myMatrix.get(key);
					}
				} else {
					position = key;
					lastMinValue = myMatrix.get(key);
				}
			}
		}
		//System.out.println("Escojo " + position);
		return position;
	}

	private int fixedOpponentStrategy(int opponentMove, int position) {
		//System.out.println("Estrategia oponente fijo, su movimiento " + opponentMove);
		int lastValue = -1;
		int lastPos = -1;
		if (first) {
			for (int i = 0; i < gameMatrix.length; i++) {
				if (gameMatrix[i][opponentMove] == null) {
					position = i;
					break;
				}
				if (gameMatrix[i][opponentMove] != null
						&& Integer.parseInt(gameMatrix[i][opponentMove].split(",")[0]) >= lastValue) {
					/*
					 * If two equal max values, choose the worst for my opponent
					 */

					if (lastPos != -1 && Integer.parseInt(gameMatrix[i][opponentMove].split(",")[0]) == lastValue) {
						//System.out.println("SON IGUALES");

						if (Integer.parseInt(gameMatrix[lastPos][opponentMove].split(",")[1]) > Integer
								.parseInt(gameMatrix[i][opponentMove].split(",")[1])) {
							//System.out.println("Escojo " + i + " porque "
//									+ Integer.parseInt(gameMatrix[i][opponentMove].split(",")[0]) + "es igual que "
//									+ lastValue + " pero " + Integer.parseInt(gameMatrix[i][opponentMove].split(",")[1])
//									+ "es menor que "
//									+ Integer.parseInt(gameMatrix[lastPos][opponentMove].split(",")[1]));
							position = i;
							lastValue = Integer.parseInt(gameMatrix[i][opponentMove].split(",")[0]);
							lastPos = i;
						}
					} else {
						//System.out.println(
//								"Escojo " + i + " porque " + Integer.parseInt(gameMatrix[i][opponentMove].split(",")[0])
//										+ "es mayor que " + lastValue);
						position = i;
						lastValue = Integer.parseInt(gameMatrix[i][opponentMove].split(",")[0]);
						lastPos = i;
					}
				}
			}
		} else {
			for (int i = 0; i < gameMatrix.length; i++) {
				if (gameMatrix[opponentMove][i] == null) {
					position = i;
					break;
				}
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
		//System.out.println("Escojo " + position);
		return position;
	}
}
