package com.cluster.Node.replication.worker;

import com.cluster.Node.replication.client.ReplicationClient;
import com.cluster.Node.replication.config.ReplicationProperties;
import com.cluster.Node.replication.model.ReplicationTask;
import com.cluster.Node.replication.queue.ReplicationQueue;
import lombok.extern.slf4j.Slf4j;

/**
 * Consumer loop that drains the {@link ReplicationQueue} and delivers each task via the
 * {@link ReplicationClient}. Delivery is retried up to {@code maxAttempts} with linear
 * backoff; a task that exhausts its attempts is logged and dropped (marked failed) so a
 * single unreachable replica cannot stall the pipeline.
 */
@Slf4j
public class ReplicationWorker implements Runnable {

    private final ReplicationQueue queue;
    private final ReplicationClient client;
    private final ReplicationProperties properties;

    public ReplicationWorker(ReplicationQueue queue,
                             ReplicationClient client,
                             ReplicationProperties properties) {
        this.queue = queue;
        this.client = client;
        this.properties = properties;
    }

    @Override
    public void run() {
        log.info("Replication worker {} started", Thread.currentThread().getName());
        while (!Thread.currentThread().isInterrupted()) {
            try {
                ReplicationTask task = queue.take();
                deliverWithRetry(task);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("Unexpected error in replication worker", e);
            }
        }
        log.info("Replication worker {} stopped", Thread.currentThread().getName());
    }

    private void deliverWithRetry(ReplicationTask task) throws InterruptedException {
        while (true) {
            int attempt = task.incrementAttempts();
            try {
                client.send(task);
                return;
            } catch (Exception e) {
                if (attempt >= properties.getMaxAttempts()) {
                    log.error("Replication FAILED after {} attempts: {} key={} v{} -> {}",
                            attempt, task.getRequest().getOperation(), task.getRequest().getKey(),
                            task.getRequest().getVersion(), task.getTarget().baseUrl(), e);
                    return;
                }
                long backoff = properties.getRetryBackoffMs() * attempt;
                log.warn("Replication attempt {} failed for key={} -> {} ({}); retrying in {}ms",
                        attempt, task.getRequest().getKey(), task.getTarget().baseUrl(),
                        e.getMessage(), backoff);
                Thread.sleep(backoff);
            }
        }
    }
}
