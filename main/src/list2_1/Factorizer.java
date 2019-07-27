package list2_1;

import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author jikangwang
 */
public class Factorizer {
    private Integer lastNumber;
    private Integer lastResult;

    public int run(int num) {
        Integer result = null;
        synchronized (this) {
            if (num == lastNumber) {
                result = lastResult;
            }
        }

        if (result == null) {
            result = num * 2;
            synchronized (this) {
                lastNumber = num;
                lastResult = result;
            }
        }
        return result;

    }
}
