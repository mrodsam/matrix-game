package PSI16.intelligent2;

public class RowColumn2 {
	private int columnRow;
	private int movements;
	private double knownPercentage;
	private int totalSum;
	private int opponentTotalSum;

	public RowColumn2() {

	}

	public RowColumn2(int lastPosition) {
		columnRow = lastPosition;
		movements = 0;
		knownPercentage = 0;

	}

	public int getColumnRow() {
		return columnRow;
	}

	public void setColumnRow(int columnRow) {
		this.columnRow = columnRow;
	}

	public int getMovements() {
		return movements;
	}

	public void setMovements(int movements) {
		this.movements = movements;
	}

	public double getKnownPercentage() {
		return knownPercentage;
	}

	public void setKnownPercentage(double knownPercentage) {
		this.knownPercentage = knownPercentage;
	}

	public int getTotalSum() {
		return totalSum;
	}

	public void setTotalSum(int totalSum) {
		this.totalSum = totalSum;
	}

	public int getOpponentTotalSum() {
		return opponentTotalSum;
	}

	public void setOpponentTotalSum(int opponentTotalSum) {
		this.opponentTotalSum = opponentTotalSum;
	}

	@Override
	public String toString() {
		return "RowColumn2 [columnRow=" + columnRow + ", movements=" + movements + ", knownPercentage="
				+ knownPercentage + ", totalSum=" + totalSum + ", opponentTotalSum=" + opponentTotalSum + "]";
	}

}
