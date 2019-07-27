## Listing 1.1. Non thread safe Sequence Generator

先来看一下代码
```java
@ThreadSafe
public class StatelessFactorizer implements Servlet {
    public void service(ServletRequest req, ServletResponse resp) {
        BigInteger i = extractFromRequest(req);
        BigInteger[] factors = factor(i);
        encodeIntoResponse(resp, factors);
} }
```

这就是一个很简单的servlet，接到一个数字，因式分解，然后返回结果
这上面的是stateless的，即该段代码不依赖于任何外部条件，无论执行多少次都是相同的结果，因此stateless总是线程安全的

我们这里就直接模拟一个
```java
public class Factorizer {
    public int run(int num){
       return num*2; 
    }
}
```
输入一个数，返回他的2倍

### 加入计数器

```java
public class Factorizer {
    private long count = 0;

    public long getCount() {
        return count;
    }

    public int run(int num) {
        count++;
        return num * 2;
    }
}
```
在[list1_1 Sequence Generator](https://github.com/zzzyyyxxxmmm/JavaConcurrencyInPracticeCode/tree/master/main/src/list1_1)中，我们已经知道
这段代码在多线程情况下不安全，除了加syncronized关键字以外，这里提供了一个AtomicLong的线程安全类，在这个类上进行操作是可以保证线程安全的
```java
public class Factorizer {
    private final AtomicLong count = new AtomicLong(0);

    public long getCount() {
        return count.get();
    }

    public int run(int num) {
        count.incrementAndGet();
        return num * 2;
    }
}
```

### 复合原子类操作

通常，由多个原子操作组成的复合操作不一定是线程安全的
```java
public class Factorizer {
    private final AtomicReference<Integer> lastNumber
            = new AtomicReference<Integer>();
    private final AtomicReference<Integer> lastResult
            = new AtomicReference<Integer>();

    public int run(int num) {
        if (num == lastNumber.get()) {
            return lastResult.get();
        } else {
            int result = 2 * num;
            lastNumber.set(num);
            lastResult.set(result);
            return result;
        }
    }
}
```
由于这段代码中，lastNumber和LastResult无法同时更新，可能会导致意外的结果, 如果直接在方法上加关键字又会导致效率低下

```java
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
```

读和写都需要加锁