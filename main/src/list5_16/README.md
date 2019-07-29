### Listing5_16 Initial Cache Attempt Using HashMap and Synchronization.

先来看一下代码

```java
public class Memorizer1<A, V> implements Computable<A, V> {
    private final Map<A, V> cache = new HashMap<A, V>();
    private final Computable<A, V> c;

    public Memorizer1(Computable<A, V> c) {
        this.c = c;
    }

    public synchronized V compute(A arg) throws InterruptedException {
        V result = cache.get(arg);
        if (result == null) {
            result = c.compute(arg);
            cache.put(arg, result);
        }
        return result;
    }
}
```

一个做计算缓存的代码，由于hashmap不安全，于是加上关键字，但加了还不如不加，因为多线程情况下，大部分线程会被卡主

改用ConcurrentHash会解决这些问题，但是有个细节值得注意：
如果线程A正在计算F(27),另一个线程B也要计算F(27), 由于线程A还没计算完，没有缓存，线程B只能重新计算，那么能不能让线程B等待A计算完之后直接拿A的结果呢？

我们可以使用Future
```java
public class Memorizer3<A, V> implements Computable<A, V> {
    private final Map<A, Future<V>> cache
            = new ConcurrentHashMap<A, Future<V>>();
    private final Computable<A, V> c;
    public Memorizer3(Computable<A, V> c) { this.c = c; }
    public V compute(final A arg) throws InterruptedException {
        Future<V> f = cache.get(arg);
        if (f == null) {
            Callable<V> eval = new Callable<V>() {
                public V call() throws InterruptedException {
                    return c.compute(arg);
                }
            };
            FutureTask<V> ft = new FutureTask<V>(eval);
            f = ft;
            cache.put(arg, ft);
            ft.run(); // call to c.compute happens here
}
try {
            return f.get();
        } catch (ExecutionException e) {
            throw launderThrowable(e.getCause());
        }
} }
```

map存的是Future