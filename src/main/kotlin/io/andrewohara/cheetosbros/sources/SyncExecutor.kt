package io.andrewohara.cheetosbros.sources

import java.util.concurrent.Executors

interface SyncExecutor {

    fun run(task: () -> Unit)
}

class ThreadPoolSyncExecutor(threads: Int = 20): SyncExecutor {

    private val executor = Executors.newFixedThreadPool(threads)

    override fun run(task: () -> Unit) {
        executor.submit(task)
    }
}

class InlineSyncExecutor: SyncExecutor {

    override fun run(task: () -> Unit) = task()
}