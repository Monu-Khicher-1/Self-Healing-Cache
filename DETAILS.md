

# Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                      Client                                 │
└────────────────────────────┬────────────────────────────────┘
                             │
                   POST/GET/DELETE /cache
                             │
                    ┌────────▼────────┐
                    │   Master Node   │
                    │  :8080 (HTTP)   │
                    └────────┬────────┘
                             │
        ┌────────────────────┼────────────────────┐
        │                    │                    │
      PUT/GET              PUT/GET              PUT/GET
        │                    │                    │
   ┌────▼────┐          ┌────▼────┐          ┌────▼────┐
   │ Cache 1 │          │ Cache 2 │          │ Cache 3 │
   │ :8082   │          │ :8083   │          │ :8084   │
   └─────────┘          └─────────┘          └─────────┘
```
# Full Reference

## API

**PUT (write)**
```bash
curl -X POST http://localhost:8080/cache \
  -H 'Content-Type: application/json' \
  -d '{"key":"user:123","value":"John","time":30}'
```

**GET (read)**
```bash
curl http://localhost:8080/cache?key=user:123
```

**DELETE**
```bash
curl -X DELETE http://localhost:8080/cache?key=user:123
```

**Topology info**
```bash
curl http://localhost:8080/topology/version
curl http://localhost:8080/topology
curl http://localhost:8080/topology/route?key=user:123
```

## Master Configuration
| Property | Default | Description |
|----------|---------|-------------|
| `cluster.replication.factor` | 2 | Number of replicas per key |
| `server.port` | 8080 | Master HTTP port |
| `failure.check-interval-ms` | 2000 | Heartbeat check frequency |
| `failure.timeout-ms` | 10000 | Time until node marked DEAD |
| `cluster.migration.drain-ms` | 1500 | Wait time during copy verification |
| `cluster.migration.verify-attempts` | 3 | Retry attempts for entry count check |

## Node Configuration
| Property | Default | Description |
|----------|---------|-------------|
| `server.port` | 8082+ | Node HTTP port |
| `master.url` | http://localhost:8080 | Master node URL |
| `heartbeat.interval-ms` | 2000 | Heartbeat send frequency |
| `cache.entry.ttl-ms` | 3600000 | Default TTL (1 hour) |

## State Machines

**Node Lifecycle**
```
REGISTERING → ACTIVE ⇄ SUSPECT ⟷ DEAD ⇄ RECOVERING
```

**Transition (Migration)**
```
PLANNED → DUAL_WRITE → COPYING → VERIFYING → COMPLETED
                                            ↘ FAILED
```

## Node Join Flow
1. Register new node → add to ring
2. Plan transitions (old replica sets → new replica sets)
3. Activate transitions → enable dual-write routing
4. Copy data from old primary to new owners (streaming)
5. Verify copy (entry count with retry logic)
6. Complete transition → remove fallback paths

## Docker Setup

Run all commands from the repository root.

```bash
docker network create self-healing-cache-net

docker build -t self-healing-master:latest ./Master
docker build -t self-healing-node:latest ./Node

docker run -d --name master --network self-healing-cache-net -p 8080:8080 \
  self-healing-master:latest

docker run -d --name node1 --network self-healing-cache-net -p 8082:8082 \
  -e CLUSTER_MASTER_HOST=master -e CLUSTER_MASTER_PORT=8080 \
  -e CLUSTER_NODE_HOST=node1 -e SERVER_PORT=8082 \
  self-healing-node:latest

docker run -d --name node2 --network self-healing-cache-net -p 8083:8083 \
  -e CLUSTER_MASTER_HOST=master -e CLUSTER_MASTER_PORT=8080 \
  -e CLUSTER_NODE_HOST=node2 -e SERVER_PORT=8083 \
  self-healing-node:latest
```

## Testing

```bash
cd Master && ./gradlew build
cd ../Node && ./gradlew build
```