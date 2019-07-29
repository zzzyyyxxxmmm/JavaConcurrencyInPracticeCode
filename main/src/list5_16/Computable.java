package list5_16;

/**
 * @author jikangwang
 */
public interface Computable<A, V> {
    V compute(A arg) throws InterruptedException;
}
