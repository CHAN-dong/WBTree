package learned_index;

public class Segment {
    public Model model;
    public double[] segData;

    public Segment() {}

    public Segment(Segment segment) {
        this.model = segment.model;
        this.segData = segment.segData;
    }
    public Segment(Model model, double[] segData) {
        this.model = model;
        this.segData = segData;
    }

    public double getKey() {
        return segData[segData.length - 1];
    }

    public int size() {
        return segData.length;
    }
    //Find the left boundary
//    public boolean lookup(long tar, int err) {
//        int pos = findLeftBound(tar, err);
//        return pos >= 0 && pos < segData.length && segData[pos] == tar;
//    }
//
//    public int findLeftBound(long tar, int err) {
//        int pos = model.find(tar);
//        int l = Math.max(0, pos - err);
//        int r = Math.min(segData.length - 1, pos + err);
//        return Utils.findLeftBound(segData, tar, l, r);
//    }

}
