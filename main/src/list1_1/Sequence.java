package list1_1;

/**
 * @author jikangwang
 */
public class Sequence {
    private int nextValue;
    public synchronized int getNext() {
        return nextValue++;
    }
}
