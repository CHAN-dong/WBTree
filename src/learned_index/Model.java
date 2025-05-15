package learned_index;

public class Model {
    double slop;
    double inter;

    public Model() {
    }
    public Model(double slop, double inter) {
        this.slop = slop;
        this.inter = inter;
    }

    public int find(double tar) {
        return  (int) (slop * (tar - inter));
    }
}
