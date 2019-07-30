## Listing 1.1. Non thread safe Sequence Generator

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
<img src="https://github.com/zzzyyyxxxmmm/JavaConcurrencyInPracticeCode/blob/master/main/img/list1_1.png" width="700" height="200">
</div>

当我们开启100个线程的时候去进行getNext操作：

```java
UnsafeSequence unsafeSequence = new UnsafeSequence();
for (int i = 0; i < 100; i++) {
    new Thread(new Runnable() {
        @Override
        public void run() {
            for (int i = 0; i < 100; i++) {
                System.out.println(Thread.currentThread().getName() + " " + unsafeSequence.getNext());
            }
        }
    }).start();
}
```

最终结果：
```java
Thread-79 9995
Thread-79 9996
Thread-79 9997
```

理论上，100个线程读取100次，由于计数是从0开始，那么最后输出的理论结果应该是9999，但显然我们的结果漏了几个。

这也就证明，上面的方法在多线程下执行并不安全

我们接下来创建一个通过synchronized关键字进行修饰的线程安全的类：
```java
public class Sequence {
    private int nextValue;
    public synchronized int getNext() {
        return nextValue++;
    }
}
```

再次进行测试：

```java
Thread-27 9997
Thread-27 9998
Thread-27 9999
```

最终我们得到了9999的结果:kissing_heart:
