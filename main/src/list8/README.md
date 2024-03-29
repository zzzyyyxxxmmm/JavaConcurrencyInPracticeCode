## Chapter 8: Applying Thread Pools

往线程池中提交任务需要注意他们是否互相依赖，否则会容易造成死锁问题

长时间运行的任务最好加上超时机制


The **core pool size**, **maximum pool size,** and **keep alive time** govern thread creation 
and teardown. The core size is the target size; 
the implementation attempts to maintain the pool at 
this size even when there are no tasks to execute, 
and will not create more threads than this unless the 
work queue is full. The maximum pool size is the upper bound on how many pool threads can be active at once. 
A thread that has been idle for longer than the keepalive time becomes a candidate for reaping and can be terminated if the current pool 
size exceeds the core size.

### newFixedThreadPool
return new ThreadPoolExecutor(nThreads, nThreads,
                              0L, TimeUnit.MILLISECONDS,
                              new LinkedBlockingQueue<Runnable>());
                              
创建一个指定工作线程数量的线程池。每当提交一个任务就创建一个工作线程，如果工作线程数量达到线程池初始的最大数，则将提交的任务存入到池队列中。 
队列长度也是近乎无限的

### newCachedThreadPool
return new ThreadPoolExecutor(0, Integer.MAX_VALUE,
                              60L, TimeUnit.SECONDS,
                              new SynchronousQueue<Runnable>());

创建一个可缓存的线程池。这种类型的线程池特点是： 

* 工作线程的创建数量几乎没有限制(其实也有限制的,数目为Interger. MAX_VALUE), 这样可灵活的往线程池中添加线程。 
* 如果长时间没有往线程池中提交任务，即如果工作线程空闲了指定的时间(默认为1分钟)，则该工作线程将自动终止。终止后，如果你又提交了新的任务，则线程池重新创建一个工作线程。 
这个线程池使用了synchronousqueue，适用于庞 大或者无限的池，将任务直接从生产者交给工作线程。Synchronous 并不是一个真正的队列，而是一种管理直接在线程间移交信息的机制。为了把一个元素放入到synchronousqueue中，
必须有另一个线程正在等待接受移交的任务。如果没有这样一个线程，只要当前池的大小还小于最大值，ThreadPoolExcueter就会创建一个新的线程了；否则根据饱和策略，任务会被拒绝，这种方法更为高效，因为任务不必放置到队列中，就可以立即交由即将执行的线程处理


### newSingleThreadExecutor
return new FinalizableDelegatedExecutorService
    (new ThreadPoolExecutor(1, 1,
                            0L, TimeUnit.MILLISECONDS,
                            new LinkedBlockingQueue<Runnable>()));

创建一个单线程化的Executor，即只创建唯一的工作者线程来执行任务，如果这个线程异常结束，会有另一个取代它，保证顺序执行(我觉得这点是它的特色)。单工作线程最大的特点是可保证顺序地执行各个任务，并且在任意给定的时间不会有多个线程是活动的 。 

### newScheduleThreadPool
super(corePoolSize, Integer.MAX_VALUE,
      DEFAULT_KEEPALIVE_MILLIS, MILLISECONDS,
      new DelayedWorkQueue());

创建一个定长的线程池，而且支持定时的以及周期性的任务执行，类似于Timer。 
总结：
* FixedThreadPool是一个典型且优秀的线程池，它具有线程池提高程序效率和节省创建线程时所耗的开销的优点。但是，在线程池空闲时，即线程池中没有可运行任务时，它不会释放工作线程，还会占用一定的系统资源。 
* CachedThreadPool的特点就是在线程池空闲时，即线程池中没有可运行任务时，它会释放工作线程，从而释放工作线程所占用的资源。但是，但当出现新任务时，又要创建一新的工作线程，又要一定的系统开销。并且，在使用CachedThreadPool时，一定要注意控制任务的数量，否则，由于大量线程同时运行，很有会造成系统瘫痪。

The newCachedThreadPool factory is a good default choice for an Executor, providing better queuing performance than a fixed thread pool.[5] A fixed size thread pool is a good choice when you need to limit the number of concurrent tasks for resource 
management purposes, as in a server application that accepts requests
 from network clients and would otherwise be vulnerable to overload.

### 饱和策略

如果线程池满了， 那么默认会执行abort策略，抛出未检查的异常

discard会默认放弃这个任务

discard-oldest策略选择丢弃接下来会执行的任务
caller-runs不会抛异常，而是直接把任务推回高调用者那里，然后调用在调用excutor的线程中直接执行，通常就是主线程

The callerruns policy implements a form of throttling that neither discards tasks nor throws an exception, but instead tries to slow down the flow of new tasks by pushing some of the work back to the caller. It executes the newly submitted task not in a pool thread, but in the thread that calls execute.
 If we modified our WebServer example to use a bounded queue and the callerruns policy, after all the pool threads 
were occupied and the work queue filled up the next task would be executed in the main thread during the call to execute. Since this would probably take some time, the main 
thread cannot submit any more tasks for at least a little while, giving the worker threads some time to catch up on the backlog. The main thread would also not be calling accept during this time, so incoming requests will queue up in the TCP layer instead 
of in the application. If the overload persisted, eventually the TCP layer would decide it has queued enough connection requests and begin discarding connection requests as well. 
As the server becomes overloaded, the overload is gradually pushed outward  from the pool threads to the work queue to the application to the TCP layer, and eventually to the client  enabling more graceful degradation under load.

### Extending ThreadPoolExecutor

ThreadPoolExecutor was designed for extension, providing several "hooks" for subclasses to overridebeforeExecute, afterExecute, and terminatethat can be used to extend the behavior of ThreadPoolExecutor.

```java
public class TimingThreadPool extends ThreadPoolExecutor {
    private final ThreadLocal<Long> startTime
            = new ThreadLocal<Long>();
    private final Logger log = Logger.getLogger("TimingThreadPool");
    private final AtomicLong numTasks = new AtomicLong();
    private final AtomicLong totalTime = new AtomicLong();
    protected void beforeExecute(Thread t, Runnable r) {
        super.beforeExecute(t, r);
        log.fine(String.format("Thread %s: start %s", t, r));
        startTime.set(System.nanoTime());
}
    protected void afterExecute(Runnable r, Throwable t) {
        try {
            long endTime = System.nanoTime();
            long taskTime = endTime - startTime.get();
            numTasks.incrementAndGet();
            totalTime.addAndGet(taskTime);
            log.fine(String.format("Thread %s: end %s, time=%dns",
                    t, r, taskTime));
        } finally {
            super.afterExecute(r, t);
} }
    protected void terminated() {
        try {
            log.info(String.format("Terminated: avg time=%dns",
                    totalTime.get() / numTasks.get()));
        } finally {
            super.terminated();
} }
}
```

果然是利用Threadlocal存储的每个线程的开始执行时间

### 带返回结果的批量任务执行
如果需要写个高并发的程序，且每个任务需要返回执行结果

a、阻塞队列防止了内存中排队等待的任务过多，造成内存溢出（毕竟一般生产者速度比较快，比如爬虫准备好网址和规则，就去执行了，执行起来（消费者）还是比较慢的）

b、CompletionService可以实现，哪个任务先执行完成就返回，而不是按顺序返回，这样可以极大的提升效率；

CompletionService ： Executor + BlockingQueue 

```java

//将Executor和BlockingQueue功能融合在一起，可以将Callable的任务提交给它来执行， 然后使用take()方法获得已经完成的结果
public class CompletionServiceDemo
{
 
	public static void main(String[] args) throws InterruptedException,
			ExecutionException
	{
		/**
		 * 内部维护11个线程的线程池
		 */
		ExecutorService exec = Executors.newFixedThreadPool(11);
		/**
		 * 容量为10的阻塞队列
		 */
		final BlockingQueue<Future<Integer>> queue = new LinkedBlockingDeque<Future<Integer>>(
				10);
		//实例化CompletionService
		final CompletionService<Integer> completionService = new ExecutorCompletionService<Integer>(
				exec, queue);
 
		/**
		 * 模拟瞬间产生10个任务，且每个任务执行时间不一致
		 */
		for (int i = 0; i < 10; i++)
		{
			completionService.submit(new Callable<Integer>()
			{
				@Override
				public Integer call() throws Exception
				{
					int ran = new Random().nextInt(1000);
					Thread.sleep(ran);
					System.out.println(Thread.currentThread().getName()
							+ " 休息了 " + ran);
					return ran;
				}
			});
		}
		
		/**
		 * 立即输出结果
		 */
		for (int i = 0; i < 10; i++)
		{
			try
			{	
				//谁最先执行完成，直接返回
				Future<Integer> f = completionService.take();
				System.out.println(f.get());
			} catch (InterruptedException e)
			{
				e.printStackTrace();
			} catch (ExecutionException e)
			{
				e.printStackTrace();
			}
		}
 
		exec.shutdown();
 
	}
 
}
```

ExecutorService的invokeAll方法也能批量执行任务，并批量返回结果，但是必须等待所有的任务执行完成后统一返回，