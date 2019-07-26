### Listing 1.1. Non thread safe Sequence Generator

先来看一下代码

```java
public class UnsafeSequence {
    private int value;
    /** Returns a unique value. */
    public int getNext() {
        return value++;
    }
}
```

这段是一个很简单的自增代码，获取value的值，并且+1。
但是value++不具有原子性，整个++的过程分为三步：读取，+1，赋予value新值，因此在多线程的环境下就会导致问题：

<div align=center>
<img src="https://github.com/zzzyyyxxxmmm/JavaConcurrencyInPracticeCode/blob/master/main/img/list1_1.png" width="500" height="200">
</div>