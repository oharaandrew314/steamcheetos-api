package io.andrewohara.cheetosbros.sources

import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

interface SyncExecutor {

    fun run(task: () -> Unit)
    fun <T> call(task: () -> T): Future<T>
}

class ThreadPoolSyncExecutor(threads: Int = 20): SyncExecutor {

    private val executor = Executors.newFixedThreadPool(threads)

    override fun run(task: () -> Unit) {
        executor.submit(task)
    }

    override fun <T> call(task: () -> T): Future<T> = executor.submit(task)
}

class InlineSyncExecutor: SyncExecutor {

    override fun run(task: () -> Unit) = task()

    override fun <T> call(task: () -> T): Future<T> = FakeFuture(task())

    private class FakeFuture<T>(private val result: T): Future<T> {
        override fun cancel(mayInterruptIfRunning: Boolean) = false
        override fun isCancelled() = false
        override fun isDone() = true

        override fun get() = result
        override fun get(timeout: Long, unit: TimeUnit) = result
    }
}