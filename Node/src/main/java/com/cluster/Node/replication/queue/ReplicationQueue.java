package com.cluster.Node.replication.queue;

import com.cluster.Node.replication.config.ReplicationProperties;
import com.cluster.Node.replication.model.ReplicationTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Thin wrapper around a bounded {@link BlockingQueue} that decouples the request-handling
 * threads (producers) from the {@code ReplicationWorker} threads (consumers). Keeping this
 * behind a component makes the queueing strategy swappable (e.g. a durable queue later).
 */
@Slf4j
@Component
public class ReplicationQueue {
    private final BlockingQueue<ReplicationTask> queue;

    public ReplicationQueue(ReplicationProperties properties) {
        this.queue = new LinkedBlockingQueue<>(properties.getQueueCapacity());
    }

    /**
     * Enqueues a task without blocking the caller. Returns {@code false} if the queue is full,
     * so the client response is never delayed by replication back-pressure.
     */
    public boolean submit(ReplicationTask task) {
        boolean accepted = queue.offer(task);
        if (!accepted) {
            log.warn("Replication queue full ({} items); dropping task for key {} -> {}",
                    queue.size(), task.getRequest().getKey(), task.getTarget().baseUrl());
        }
        return accepted;
    }

    public ReplicationTask take() throws InterruptedException {
        return queue.take();
    }

    public int size() {
        return queue.size();
    }
}
