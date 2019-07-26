package list1_1;

/**
 * @author jikangwang
 */
//NotThreadSafe
public class UnsafeSequence {
    private int value;
    /** Returns a unique value. */
    public int getNext() {
        return value++;
    }
}
