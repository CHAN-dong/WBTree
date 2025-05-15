package index;

import jdk.nashorn.internal.ir.debug.ObjectSizeCalculator;

import java.util.List;

public class Token {
    public List<String>[] left;
    public List<String>[] right;

    public List<String>[] leftKey;
    public List<String>[] rightKey;

    public Token(List<String>[] left, List<String>[] right, List<String>[] leftKey, List<String>[] rightKey) {
        this.left = left;
        this.right = right;
        this.leftKey = leftKey;
        this.rightKey = rightKey;
    }

    public double getTokenSize() {
        System.setProperty("java.vm.name", "Java HotSpot(TM) ");
        long sz = ObjectSizeCalculator.getObjectSize(this);
        return sz / 1024.0;
    }


}