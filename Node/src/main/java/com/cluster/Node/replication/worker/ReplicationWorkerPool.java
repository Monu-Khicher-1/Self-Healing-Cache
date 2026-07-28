package com.cluster.Node.replication.worker;

import com.cluster.Node.replication.client.ReplicationClient;
import com.cluster.Node.replication.config.ReplicationProperties;
import com.cluster.Node.replication.queue.ReplicationQueue;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Owns the lifecycle of the replication worker threads. Kept separate from the
 * {@code ReplicationManager} (the producer) so that production and consumption of tasks
 * evolve independently (single-responsibility).
 */
@Slf4j
@Component
public class ReplicationWorkerPool {

    private final ReplicationQueue queue;
    private final ReplicationClient client;
    private final ReplicationProperties properties;
    private ExecutorService executor;

    public ReplicationWorkerPool(ReplicationQueue queue,
                                 ReplicationClient client,
                                 ReplicationProperties properties) {
        this.queue = queue;
        this.client = client;
        this.properties = properties;
    }

    @PostConstruct
    public void start() {
        int workers = Math.max(1, properties.getWorkers());
        AtomicInteger counter = new AtomicInteger();
        ThreadFactory threadFactory = r -> {
            Thread t = new Thread(r, "replication-worker-" + counter.incrementAndGet());
            t.setDaemon(true);
            return t;
        };
        executor = Executors.newFixedThreadPool(workers, threadFactory);
        for (int i = 0; i < workers; i++) {
            executor.submit(new ReplicationWorker(queue, client, properties));
        }
        log.info("Started {} replication worker(s)", workers);
    }

    @PreDestroy
    public void stop() {
        if (executor == null) {
            return;
        }
        executor.shutdownNow();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                log.warn("Replication workers did not terminate within timeout");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
