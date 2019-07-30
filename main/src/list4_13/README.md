## Listing4_13 Put If Absent 

先来看一下代码

```java
public class BetterVector<E> extends Vector<E> {
    public synchronized boolean putIfAbsent(E x) {
        boolean absent = !contains(x);
        if (absent)
            add(x);
        return absent;
    }
}
```

比较常见的PutIfAbsent问题，类似于Check And Act，通过继承一个线程安全的类，然后加入关键字来实现
Extension is more fragile than adding code directly to a class, because the implementation of the synchronization policy is now distributed over multiple, separately maintained source files. If the underlying class were to change its synchronization policy by choosing a different lock to guard its state variables, the subclass would subtly and silently break, because it no longer used the right lock to control concurrent access to the base class's state.

问题就是父类的安全性可能会发生改变，所以比较脆弱

那如果我们直接hold一个线程安全的类呢？

```java
public class ListHelper<E> {
    public List<E> list =
            Collections.synchronizedList(new ArrayList<E>());
    public synchronized boolean putIfAbsent(E x) {
        boolean absent = !list.contains(x);
        if (absent)
            list.add(x);
        return absent;
    }
}
```

这个其实是不work的，因为一旦同步，List用的是list的锁，ListHelper用的是Helper的锁，两个锁不一样

因此我们的锁对象应该是list：

```java
public boolean putIfAbsent(E x) {
    synchronized (list) {
        boolean absent = !list.contains(x);
        if (absent)
            list.add(x);
        return absent;
    }
}
```

另一种方法是把list转化为我们自己的list

```java
public class ImprovedList<T> implements List<T> {
    private final List<T> list;

    public ImprovedList(List<T> list) {
        this.list = list;
    }

    public synchronized boolean putIfAbsent(T x) {
        boolean contains = list.contains(x);
        if (contains)
            list.add(x);
        return !contains;
    }

    public synchronized void clear() {
        list.clear();
    }
    // ... similarly delegate other List methods
}
```