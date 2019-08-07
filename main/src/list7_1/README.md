## Chapter7 Cancellation and Shutdown

这部分好难啊，看了好久

停止线程需要考虑的方面还是挺多的
1. 保证线程能够停止(safely, quickly, reliably)
2. 是否需要保证服务运行完再停止(logging service)

### Thread.stop()

这个方法是被弃用的, 主要是因为stop会立刻强制释放该线程所持有的锁

其实stop方法天生就不安全，因为它在终止一个线程时会强制中断线程的执行，不管run方法是否执行完了，
并且还会释放这个线程所持有的所有的锁对象。这一现象会被其它因为请求锁而阻塞的线程看到，使他们继续向下执行。
这就会造成数据的不一致，我们还是拿银行转账作为例子，我们还是从A账户向B账户转账500元，我们之前讨论过，
这一过程分为三步，第一步是从A账户中减去500元，假如到这时线程就被stop了，那么这个线程就会释放它所取得锁，
然后其他的线程继续执行，这样A账户就莫名其妙的少了500元而B账户也没有收到钱。这就是stop方法的不安全性。


### 利用volatile停止线程

```java
public class PrimeGenerator {
    private final List<BigInteger> primes
            = new ArrayList<BigInteger>();
    private volatile boolean cancelled;

    public void run() {
        BigInteger p = BigInteger.ONE;
        while (!cancelled) {
            p = p.nextProbablePrime();
            synchronized (this) {
                primes.add(p);
            }
        }
    }

    public void cancel() {
        cancelled = true;
    }

    public synchronized List<BigInteger> get() {
        return new ArrayList<BigInteger>(primes);
    }
}
```

一个比较常见的利用flag停止线程的方式,这样停止的方式有种明显的缺点就是里面可能永远读不到flag的值，比如阻塞
```java
class BrokenPrimeProducer extends Thread {
    private final BlockingQueue<BigInteger> queue;
    private volatile boolean cancelled = false;

    BrokenPrimeProducer(BlockingQueue<BigInteger> queue) {
        this.queue = queue;
    }

    public void run() {
        try {
            BigInteger p = BigInteger.ONE;
            while (!cancelled)
                queue.put(p = p.nextProbablePrime());
        } catch (InterruptedException consumed) {
        }
    }

    public void cancel() {
        cancelled = true;
    }
}

    void consumePrimes() throws InterruptedException {
        BlockingQueue<BigInteger> primes = ...;
        BrokenPrimeProducer producer = new BrokenPrimeProducer(primes);
        producer.start();
        try {
            while (needMorePrimes())
                consume(primes.take());
        } finally {
            producer.cancel();
        }
    }
```
这个方法有两点坏处
1. 必须等某些代码执行完才能读取flag
2. 一个线程不停产生prime，另一个线程无限循环阻塞式接收prime，但是一旦取消生产，另一个线程就会一直阻塞

### Interrupt
```java
public class Thread {
    public void interrupt() { ... }
    public boolean isInterrupted() { ... }
    public static boolean interrupted() { ... }
    ...
}
```

Thread.interrupt()方法不会中断一个正在运行的线程。它的作用是，在线程受到阻塞时抛出一个中断信号，这样线程就得以退出阻塞的状态。更确切的说，如果线程被Object.wait, Thread.join和Thread.sleep三种方法之一阻塞，
那么，它将接收到一个中断异常（InterruptedException），从而提早地终结被阻塞状态。

线程中一般使用一下方式：
``
while (!Thread.currentThread().isInterrupted() && more work to do)
{

}
``

```java
class PrimeProducer extends Thread {
    private final BlockingQueue<BigInteger> queue;

    PrimeProducer(BlockingQueue<BigInteger> queue) {
        this.queue = queue;
    }

    public void run() {
        try {
            BigInteger p = BigInteger.ONE;
            while (!Thread.currentThread().isInterrupted())
                queue.put(p = p.nextProbablePrime());
        } catch (InterruptedException consumed) {
            /*  Allow thread to exit  */
        }
    }

    public void cancel() {
        interrupt();
    }
}
```
中断政策指引我们，不要尝试中断不属于我们的线程，每个人只需要管理好自己创建的线程

### 一段神奇的代码

```java
public Task getNextTask(BlockingQueue<Taskgt; queue) {
        boolean interrupted = false;
        try {
            while (true) {
                try {
                    return queue.take();
                } catch (InterruptedException e) {
                    interrupted = true;
                    // fall through and retry
                }
            }
        } finally {
            if (interrupted)
                Thread.currentThread().interrupt();
        }
    }
```

Activities that do not support cancellation but still call interruptible blocking methods will have to call them in a loop, retrying when interruption is detected. In this case, they should save the interruption status locally and restore it just before returning, as shown in Listing 7.7, rather than immediately upon catching InterruptedException. Setting the interrupted status too early could result in an infinite loop, because most interruptible blocking methods check the interrupted status on entry and throw InterruptedException immediately if it is set. (Interruptible methods usually poll for interruption before blocking or doing any significant work, so as to be as responsive to interruption as possible.)

看到这段代码的时候哥震惊了(ΩДΩ)，完全不知道写的啥东西，就上面这段话我读了十几遍也没搞懂他到底在说啥

先搞懂这段代码执行顺序是什么样的，这是一个多层嵌套的Exception，首先queue.take()会抛出exception，然后被catch到。
这时interrupted会被设为true，然后跳出来执行finally块，然后再次调用当前线程的interrupt，然后退出。

如果没有设置interrupted变量，没有外层的try finally，那么里面的Exception就不会被外面捕获，而是由于循环而继续不停的抛出Exception.
那如果把``Thread.currentThread().interrupt();``写在里面呢，其实一样，也会一直循环抛错

啊，神清气爽:kissing_closed_eyes:


### Cancellation Via Future

```java
public static void main(String[] args) {
    ExecutorService threadPool = Executors.newSingleThreadExecutor();

    SimpleTask task = new SimpleTask(3_000); // task 需要运行 3 秒
    Future<Double> future = threadPool.submit(task);
    threadPool.shutdown(); // 发送关闭线程池的指令

    cancelTask(future, 2_000); // 在 2 秒之后取消该任务

    try {
        double time = future.get();
        System.out.format("任务运行时间: %.3f s\n", time);
    } catch (CancellationException ex) {
        System.err.println("任务被取消");
    } catch (InterruptedException ex) {
        System.err.println("当前线程被中断");
    } catch (ExecutionException ex) {
        System.err.println("任务执行出错");
    }

    
}
```

Future的cancel方法会抛出CancellationException，但实际并不会直接取消任务，而是通过调用interrupt

