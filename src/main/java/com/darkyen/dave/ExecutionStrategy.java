package com.darkyen.dave;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Interface for (potentially) offloading the work to off-thread.
 */
public interface ExecutionStrategy {

    /** Execute given request on off-thread */
    <T> void execute(Request request, ResponseTranslator<T> translator, ResponseCallback<T> callback);

    ExecutionStrategy SYNCHRONOUS_EXECUTION_STRATEGY = new ExecutionStrategy() {

        public <T> void execute(Request request, ResponseTranslator<T> translator, ResponseCallback<T> callback) {
            final Response<T> response;
            try {
                response = request.execute(translator);
            } catch (WebbException e) {
                callback.failure(e);
                return;
            }
            callback.success(response);//Do not catch exceptions in callback
        }

    };

    /**
     * Primitive thread pool implementation of asynchronous execution strategy
     */
    @SuppressWarnings("WeakerAccess")
    class Async implements ExecutionStrategy {

        private final AsyncThread[] threads;
        private volatile boolean shutdown = false;

        private final BlockingQueue<AsyncTask> taskQueue = new LinkedBlockingQueue<AsyncTask>();

        public Async(int threadCount) {
            if (threadCount < 1) throw new IllegalArgumentException("threadCount must be >= 1");
            this.threads = new AsyncThread[threadCount];
            for (int i = 0; i < threadCount; i++) {
                this.threads[i] = new AsyncThread(i + 1);
            }
        }

        public <T> void execute(Request request, ResponseTranslator<T> translator, ResponseCallback<T> callback) {
            if (shutdown) {
                callFailure(callback, new WebbException("ExecutionStrategy.Async is in shutdown and does not accept more work"));
                return;
            }

            taskQueue.add(new AsyncTask<T>(request, translator, callback));
        }

        public void shutdown(boolean waitForFinish) {
            this.shutdown = true;
            if (waitForFinish) {
                for (AsyncThread thread : threads) {
                    thread.interrupt();
                }
            } else {
                for (AsyncThread thread : threads) {
                    thread.close();
                }
            }
        }

        /** For overriding */
        protected <T> void callSuccess(ResponseCallback<T> callback, Response<T> response) {
            try {
                callback.success(response);
            } catch (Throwable e) {
                callbackFailure(e);
            }
        }

        /** For overriding */
        protected <T> void callFailure(ResponseCallback<T> callback, WebbException response) {
            try {
                callback.failure(response);
            } catch (Throwable e) {
                callbackFailure(e);
            }
        }

        protected void callbackFailure(Throwable throwable) {
            System.err.println("ExecutionStrategy.Async - callbackFailure");
            throwable.printStackTrace(System.err);
        }

        private static final class AsyncTask <T> {
            public final Request request;
            public final ResponseTranslator<T> translator;
            public final ResponseCallback<T> callback;

            private AsyncTask(Request request, ResponseTranslator<T> translator, ResponseCallback<T> callback) {
                this.request = request;
                this.translator = translator;
                this.callback = callback;
            }
        }

        private final class AsyncThread extends Thread {

            private volatile boolean keepRunning = true;

            public AsyncThread(int order) {
                setName("AsyncThread - "+order);
                setDaemon(true);
                start();
            }

            @Override
            public void run() {
                while (keepRunning) {
                    try {
                        final AsyncTask task;

                        if (shutdown) {
                            task = taskQueue.poll();
                            if (task == null) {
                                return;
                            }
                        } else {
                            try {
                                task = taskQueue.take();
                            } catch (InterruptedException ignored) {
                                continue;
                            }
                        }

                        final Response response;
                        try {
                            response = task.request.execute(task.translator);
                        } catch (WebbException e) {
                            callFailure(task.callback, e);
                            continue;
                        }
                        callSuccess(task.callback, response);// Do not catch exceptions in callback
                    } catch (Throwable ex) {
                        System.err.println("ExecutionStrategy.Async - failure inside "+getName());
                        ex.printStackTrace(System.err);
                    }
                }
            }

            public void close() {
                keepRunning = false;
                interrupt();
            }
        }
    }
}
