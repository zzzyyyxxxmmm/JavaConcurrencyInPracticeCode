package list4_4;

/**
 * @author jikangwang
 */
public class MutablePoint {
    public int x, y;

    public MutablePoint() {
        x = 0;
        y = 0;
    }

    public MutablePoint(MutablePoint p) {
        this.x = p.x;
        this.y = p.y;
    }

    public MutablePoint(int x, int y) {
        this.x = x;
        this.y = y;
    }
}
