package index;


public class GraModel {
//

    public double evaluate(double x) {
        return (slope * x - intercept);
    }

    double slope;
    double intercept;
    int start;
    int endX;

    public GraModel(double slope, double intercept, int startX, int endX) {
        this.slope = slope;
        this.intercept = intercept;
        this.start = startX;
        this.endX = endX;
    }


    public double derivative() {
        return slope;
    }
}