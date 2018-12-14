package PSI16.intelligent1;

public class RowColumn1 {
	private int columnRow;
	private int movements;
	private double knownPercentage;
	private int minValue;
	private int opponentValueWithMyMinValue;

	public RowColumn1(int lastPosition) {
		columnRow = lastPosition;
		movements = 0;
		knownPercentage = 0;
		minValue = 10;
		opponentValueWithMyMinValue = 10;
	}

	public RowColumn1() {
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

	public double getPercentageKnow() {
		return knownPercentage;
	}

	public void setPercentageKnow(int percentageKnow) {
		this.knownPercentage = percentageKnow;
	}

	public int getMinValue() {
		return minValue;
	}

	public void setMinValue(int minValue) {
		this.minValue = minValue;
	}

	public double getKnownPercentage() {
		return knownPercentage;
	}

	public void setKnownPercentage(double knownPercentage) {
		this.knownPercentage = knownPercentage;
	}

	public int getOpponentValueWithMyMinValue() {
		return opponentValueWithMyMinValue;
	}

	public void setOpponentValueWithMyMinValue(int opponentValueWithMyMinValue) {
		this.opponentValueWithMyMinValue = opponentValueWithMyMinValue;
	}

	@Override
	public String toString() {
		return "RowColumn1 [columnRow=" + columnRow + ", movements=" + movements + ", knownPercentage="
				+ knownPercentage + ", minValue=" + minValue + ", opponentValueWithMyMinValue="
				+ opponentValueWithMyMinValue + "]";
	}

}
