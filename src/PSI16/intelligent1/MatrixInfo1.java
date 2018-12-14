package PSI16.intelligent1;

import java.util.LinkedHashMap;

public class MatrixInfo1 {

	private String gameMatrix[][];
	private int matrixSize;
	private boolean first;
	private LinkedHashMap<Integer, RowColumn1> myMatrix;
	boolean allKnown = false;
	int roundsBeforeChange;

	public MatrixInfo1(String[][] gameMatrix, int matrixsize, boolean first, int roundsBeforeChange) {
		this.gameMatrix = gameMatrix;
		this.matrixSize = matrixsize;
		this.first = first;
		this.roundsBeforeChange = roundsBeforeChange;
		allKnown = false;

		myMatrix = new LinkedHashMap<>();
		for (int i = 0; i < matrixSize; i++) {
			myMatrix.put(i, new RowColumn1(i));
		}

	}

	public void setGameMatrix(String[][] gameMatrix) {
		this.gameMatrix = gameMatrix;
	}

	private void updateMyMatrix(int lastPosition) {

		if (first) {
			/* Actualizas el contador de movimientos */
			myMatrix.get(lastPosition).setMovements(myMatrix.get(lastPosition).getMovements() + 1);

			/*
			 * Actualizas tu puntuación máxima, la puntuación máxima que consigue el
			 * oponente si tú consigues la tuya y su puntuación máxima, todo esto de cada
			 * fila
			 */
			for (int i = 0; i < gameMatrix.length; i++) {
				for (int j = 0; j < gameMatrix.length; j++) {
					if (gameMatrix[i][j] != null) {
						/* Cálculo de los valores mínimos */
						if (Integer.parseInt(gameMatrix[i][j].split(",")[0]) < myMatrix.get(i).getMinValue()) {
							myMatrix.get(i).setMinValue(Integer.parseInt(gameMatrix[i][j].split(",")[0]));
							myMatrix.get(i)
									.setOpponentValueWithMyMinValue(Integer.parseInt(gameMatrix[i][j].split(",")[1]));
						}
					}
				}
			}

			/* Actualizas el porcentaje de valores conocidos de cada fila */
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

			/* Actualizas el contador de movimientos */
			myMatrix.get(lastPosition).setMovements(myMatrix.get(lastPosition).getMovements() + 1);

			/*
			 * Actualizas tu puntuación máxima, la puntuación máxima que consigue el
			 * oponente si tú consigues la tuya y su puntuación máxima, todo esto de cada
			 * columna
			 */
			for (int i = 0; i < gameMatrix.length; i++) {
				for (int j = 0; j < gameMatrix.length; j++) {
					if (gameMatrix[i][j] != null) {
						/* Cálculo de los valores mínimos */
						if (Integer.parseInt(gameMatrix[i][j].split(",")[1]) < myMatrix.get(j).getMinValue()) {
							myMatrix.get(j).setMinValue(Integer.parseInt(gameMatrix[i][j].split(",")[1]));
							myMatrix.get(j)
									.setOpponentValueWithMyMinValue(Integer.parseInt(gameMatrix[i][j].split(",")[0]));
						}
					}
				}
			}

			/* Actualizas el porcentaje de valores conocidos de cada columna */
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

//		for (int i = 0; i < gameMatrix.length; i++) {
//			for (int j = 0; j < gameMatrix.length; j++) {
//				System.out.print(gameMatrix[i][j] + "\t");
//			}
//			System.out.println();
//		}

//		for (Integer key : myMatrix.keySet()) {
//			System.out.println(myMatrix.get(key));
//		}

	}

	public int getPosition(int lastPosition, int roundsCounter, boolean fixedOpponent, int opponentMove) {
		updateMyMatrix(lastPosition);
		int position = (int) Math.floor(Math.random() * matrixSize);
		RowColumn1 lastMinValue = new RowColumn1();

		/* Juego aleatoriamente las 5 primeras rondas */
		if (roundsCounter < 5) {
			return position;
		} else {
			/*
			 * Si mi oponente es fijo, escojo siempre el máximo valor de la fila/columna que
			 * él elija
			 */
			if (fixedOpponent) {
				/* Si el oponente es fijo, escojo los movimientos que no escogí antes */
				if (!allKnown) {
					for (Integer key : myMatrix.keySet()) {
						if (myMatrix.get(key).getMovements() == 0) {
							return key;
						}
					}
					allKnown = true;
				}

				int lastValue = 0;
				int lastPos = 0;
				if (first) {
					for (int i = 0; i < gameMatrix.length; i++) {
						if (gameMatrix[i][opponentMove] != null
								&& Integer.parseInt(gameMatrix[i][opponentMove].split(",")[0]) >= lastValue) {
							/*
							 * Si tengo 2 valores máximos iguales, escojo en el que menos puntos gane mi
							 * oponente
							 */
							if (Integer.parseInt(gameMatrix[opponentMove][i].split(",")[0]) == lastValue) {
								int lastDif = Integer.parseInt(gameMatrix[lastPos][opponentMove].split(",")[0])
										- Integer.parseInt(gameMatrix[lastPos][opponentMove].split(",")[1]);
								int currentDif = Integer.parseInt(gameMatrix[i][opponentMove].split(",")[0])
										- Integer.parseInt(gameMatrix[i][opponentMove].split(",")[1]);
								if (currentDif > lastDif) {
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
							if (Integer.parseInt(gameMatrix[opponentMove][i].split(",")[1]) == lastValue) {
								int lastDif = Integer.parseInt(gameMatrix[opponentMove][lastPos].split(",")[1])
										- Integer.parseInt(gameMatrix[opponentMove][lastPos].split(",")[0]);
								int currentDif = Integer.parseInt(gameMatrix[opponentMove][i].split(",")[1])
										- Integer.parseInt(gameMatrix[opponentMove][i].split(",")[0]);

								if (currentDif > lastDif) {
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

			} else {
				for (Integer key : myMatrix.keySet()) {
					/*
					 * Respondo con la columna/fila en la que se encuentra el máximo valor que puedo
					 * conseguir en el peor de los casos
					 */
					if (myMatrix.get(key).getMinValue() >= lastMinValue.getMinValue()) {
						if (myMatrix.get(key).getMinValue() == lastMinValue.getMinValue()) {
							/* Calculo la diferencia */
							int lastDif = myMatrix.get(lastMinValue.getColumnRow()).getMinValue()
									- myMatrix.get(lastMinValue.getColumnRow()).getOpponentValueWithMyMinValue();
							int currentDif = myMatrix.get(key).getMinValue()
									- myMatrix.get(key).getOpponentValueWithMyMinValue();
							/*
							 * Si hay más diferencia entre los valores que ya tenía que entre los nuevos,
							 * escojo los nuevos
							 */
							if (currentDif < lastDif) {
								position = key;
							}
						} else {
							position = key;
						}
						lastMinValue = myMatrix.get(key);
					}
				}
			}
		}

		return position;
	}
}
