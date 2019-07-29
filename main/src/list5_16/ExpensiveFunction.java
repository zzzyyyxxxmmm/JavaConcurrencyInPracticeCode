package list5_16;

import java.math.BigInteger;

/**
 * @author jikangwang
 */
public class ExpensiveFunction implements Computable<String, BigInteger> {
    public BigInteger compute(String arg) {
        // after deep thought...
        return new BigInteger(arg);
    }
}
