package refdiff.core.diff;

public class ThresholdsProvider {
	private double t1 = 0.5;
	private double t2 = 0.5;
	
//	public double getValue() {
//		return t1;
//	}
	
	public double getMinimum() {
		return t1;
	}
	
	public double getIdeal() {
		return t2;
	}
	
	public double getExtractMinimum() {
		return 0.3;
	}
}
