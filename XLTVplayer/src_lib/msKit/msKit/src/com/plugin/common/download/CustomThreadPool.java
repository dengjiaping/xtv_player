package com.plugin.common.download;

import java.util.HashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import android.content.Context;
import android.os.Process;
import android.text.TextUtils;

import com.plugin.common.utils.Destroyable;
import com.plugin.common.utils.SingleInstanceBase;

public class CustomThreadPool extends SingleInstanceBase implements Destroyable{
    
	private static final String TAG = "RRThreadPool";

    private static final boolean USING_CUSTOM_THREADPOOL = true;

    public static class TaskWrapper implements Runnable {

        protected final Runnable runnable;
        protected boolean cancel;
        protected String mTaskName;

        public TaskWrapper(Runnable runnable) {
            this(runnable, null);
        }

        public TaskWrapper(Runnable runnable, String taskName) {
            mTaskName = taskName;
            this.runnable = runnable;
            cancel = false;
        }

        public void cancel() {
            cancel = true;
        }

        @Override
        public void run() {
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
            
            if (!cancel) {
                runnable.run();
            } else {
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            TaskWrapper that = (TaskWrapper) o;
            if (runnable != null ? !runnable.equals(that.runnable) : that.runnable != null)
                return false;
            return true;
        }

        @Override
        public int hashCode() {
            return runnable != null ? runnable.hashCode() : 0;
        }

        @Override
        public String toString() {
            return runnable.toString();
        }
    }
    
    
    public static final class ThreadPoolSnapShot {
        public int taskCount;

        public int coreTreadCount;

        public final int ALLOWED_MAX_TAKS;

        ThreadPoolSnapShot(int taskCount, int coreThreadCount, int max) {
            this.taskCount = taskCount;
            this.coreTreadCount = coreThreadCount;
            this.ALLOWED_MAX_TAKS = max;
        }
    }
    
    private static class IncrementInteger {
        private final int MAX_SIZE = Integer.MAX_VALUE / 2;

        private final AtomicInteger mInt = new AtomicInteger(1);

        public int getAndIncrement() {
            if (mInt.get() >= MAX_SIZE) {
                mInt.set(1);
            }

            return mInt.getAndIncrement();
        }
    }
    
    private final static IncrementInteger sIncrementInteger = new IncrementInteger();

    private static class PriorityThreadFactory implements ThreadFactory {
        private final int mPriority;
        private final String mName;

        public PriorityThreadFactory(String name, int priority) {
            mName = name;
            mPriority = priority;
        }

        public Thread newThread(Runnable r) {
            return new Thread(r, mName + '-' + sIncrementInteger.getAndIncrement()) {
                @Override
                public void run() {
                    Process.setThreadPriority(mPriority);
                    super.run();
                }
            };
        }
    }
    
    private static final int CORE_THREAD_COUNT = 5;
    private static final int MAX_THREAD_COUNT = 64;
    private static final long KEEP_ALIVE_DELAY = 5 * 1000;

    private static final int SPECIAL_CORE_THREAD_COUNT = 3;

    private ThreadPoolExecutor mExecutorService;
    private HashMap<String, ThreadPoolExecutor> mSpecialExectorMap;

    public static CustomThreadPool getInstance() {
        return SingleInstanceBase.getInstance(CustomThreadPool.class);
    }
    
    public static void asyncWork(Runnable run) {
        if (run != null) {
            CustomThreadPool.getInstance().excute(new TaskWrapper(run));
        }
    }
    
	@Override
	public void onDestroy() {
        if (USING_CUSTOM_THREADPOOL) {
            try {
                mExecutorService.shutdown();
                for (ThreadPoolExecutor e : mSpecialExectorMap.values()) {
                    e.shutdown();
                }
                mSpecialExectorMap.clear();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }		
	}

	@Override
	protected void init(Context context) {
		
	}

    protected CustomThreadPool() {
        super();

        if (USING_CUSTOM_THREADPOOL) {
            mExecutorService = new ThreadPoolExecutor(CORE_THREAD_COUNT, MAX_THREAD_COUNT, KEEP_ALIVE_DELAY,
                    TimeUnit.MILLISECONDS, new SynchronousQueue<Runnable>(true), new PriorityThreadFactory(
                            "custom-tpool", android.os.Process.THREAD_PRIORITY_BACKGROUND),
                    new ThreadPoolExecutor.DiscardPolicy() {
                        @Override
                        public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
                            super.rejectedExecution(r, e);

                        }
                    });

            mSpecialExectorMap = new HashMap<String, ThreadPoolExecutor>();
        }
    }
    
    private ThreadPoolExecutor createSpecialThreadPoolExecutor(String specialName) {
        return new ThreadPoolExecutor(SPECIAL_CORE_THREAD_COUNT, SPECIAL_CORE_THREAD_COUNT, KEEP_ALIVE_DELAY,
                TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(), new PriorityThreadFactory(specialName,
                        android.os.Process.THREAD_PRIORITY_BACKGROUND), new ThreadPoolExecutor.DiscardPolicy() {
                    @Override
                    public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
                        super.rejectedExecution(r, e);

                    }
                });
    }
    
    public boolean excute(TaskWrapper task) {
        if (USING_CUSTOM_THREADPOOL) {
            return internalCustomExcute(task, 0);
        } 

        return true;
    }
    
    private boolean internalCustomExcute(final TaskWrapper task, long delay) {
        if (task == null) {
            return false;
        }

        if (mExecutorService.isShutdown()) {
            return false;
        }

        delay = delay < 0 ? 0 : delay;
        mExecutorService.execute(new Runnable() {
            @Override
            public void run() {
                task.run();
            }
        });

        return true;
    }
    
    public boolean excuteWithSpecialThread(String specialWorkName, TaskWrapper task) {
        if (TextUtils.isEmpty(specialWorkName)) {
            return excute(task);
        } else {
            if (!mSpecialExectorMap.containsKey(specialWorkName)) {
                mSpecialExectorMap.put(specialWorkName, createSpecialThreadPoolExecutor(specialWorkName));
            }

            ThreadPoolExecutor e = mSpecialExectorMap.get(specialWorkName);
            if (e != null && !e.isShutdown()) {
                e.execute(task);
            } else {
                excute(task);
            }

            return false;
        }
    }

    public ThreadPoolSnapShot getSpecialThreadSnapShot(String name) {
        if (TextUtils.isEmpty(name)) {
            return getThreadSnapShot(mExecutorService);
        } else {
            if (!mSpecialExectorMap.containsKey(name)) {
                mSpecialExectorMap.put(name, createSpecialThreadPoolExecutor(name));
            }

            return getThreadSnapShot(mSpecialExectorMap.get(name));
        }
    }

    private ThreadPoolSnapShot getThreadSnapShot(ThreadPoolExecutor e) {
        if (e != null) {
            return new ThreadPoolSnapShot(e.getQueue().size(), e.getCorePoolSize(), e.getMaximumPoolSize());
        }

        return null;
    }
	
}
