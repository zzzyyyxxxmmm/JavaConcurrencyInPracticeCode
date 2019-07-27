### Listing 1.1.Race Condition in Lazy Initialization. Don't Do this. 

先来看一下代码（和原版代码有点区别）

```java
public class LazyInitRace {
    private static LazyInitRace instance = null;
    public static LazyInitRace getInstance() {
        if (instance == null){
            System.out.println("initialize");
            instance = new LazyInitRace();
        }
        return instance;
    }
}
```

这段表明如果instance是空，则初始化，并返回

这段代码也是很典型的单例模式，我们在多线程的情况下运行

```java
for (int i = 0; i < 100; i++) {
    new Thread(new Runnable() {
        @Override
        public void run() {
           LazyInitRace lazyInitRace=LazyInitRace.getInstance();
        }
    }).start();
}
```

最后输出的结果：
```
initialize
initialize
initialize
initialize
```

显然，instance被初始化了多次

如果A，B线程同时进入`if (instance == null)`这段代码，则两个线程会同时接收到instance为空的信息，然后初始化两次

那么怎么解决这个问题呢？

1. 加入synchronized关键字
```java
public static synchronized LazyInitRace getInstance() {
    if (instance == null){
        System.out.println("initialize");
        instance = new LazyInitRace();
    }
    return instance;
}
```

但这样显然这么多线程的情况下，其他线程很容易被阻塞，加上时间戳，看一下运行了多长时间
```java
long t1=System.currentTimeMillis();
    for (int i = 0; i < 100; i++) {
        new Thread(new Runnable() {
            @Override
            public void run() {
               LazyInitRace lazyInitRace=LazyInitRace.getInstance();
                System.out.println(System.currentTimeMillis()-t1);
            }
        }).start();
    }
```

2. 声明时进行初始化
```java
private static LazyInitRace instance = new LazyInitRace();
public static LazyInitRace getInstance() {
    System.out.println("initialize");
    return instance;
}
```
这种方法抛弃了Lazy loading效果，由于是static修饰的，因此在类装载时就被初始化，比较容易浪费内存



3. 双重锁
```java
private volatile static LazyInitRace instance = null;
public static LazyInitRace getInstance() {
    if (instance == null){
        synchronized (LazyInitRace.class){
            if(instance==null){
                System.out.println("initialize");
                instance = new LazyInitRace();
            }
        }
    }
    return instance;
}
```

这次在方法内部加锁，由于instance被创建之后，就不会获取锁了，因此速度提高了很多

但比较奇怪的是，一直到我开到了2000个线程，时间上都并没有太大的差别。目前猜测是synchronized关键字有一些优化。

4.登记式/静态内部类
```java
private static class LazyInitRaceHolder{
    private static final LazyInitRace instance=new LazyInitRace();
}
private static LazyInitRace instance = null;

public LazyInitRace() {
    System.out.println("initialize");
}

public static LazyInitRace getInstance() {
    return LazyInitRaceHolder.instance;
}
```

这种方法主要是利用了静态内部类的以一个特性，内部类只有当内部静态成员被调用时，才会被加载