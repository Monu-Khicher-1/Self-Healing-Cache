# 🔄 Distributed Self-Healing Cache

A distributed caching system in **Java 21 + Spring Boot** that mimics real production infrastructure (think Redis Cluster / DynamoDB-style sharding) — implementing **consistent hashing, replication, automatic failover, and zero-downtime rebalancing** from scratch.

> Built to explore the hard problems in distributed systems: data placement, fault tolerance, and safe topology changes under live traffic — no external cache/DB libraries used.

## ⚡ Highlights

- **Consistent hashing ring** (TreeMap) → O(log N) key lookup, minimal data movement on scale up/down
- **Primary-replica replication** (configurable factor) with async, version-gated writes — no lost updates under concurrent replication
- **Automatic failure detection & failover** — heartbeat-based (2s interval), dead-node eviction, and replica promotion in seconds, with **zero data loss**
- **Zero-downtime cluster resizing** via a dual-write / read-fallback migration protocol — nodes can join or leave without dropping traffic
- **TTL-based expiration** with background cleanup + per-request filtering
- **Fully containerized** with Docker for multi-node local testing

## 🧠 Why it's interesting

This project touches the interesting problem statements in distributed systems:
- How do you shard data across nodes and rebalance it live?
- How do you detect failures and fail over without losing data?
- How do you migrate data between nodes while still serving reads/writes?

Every one of these is implemented and testable end-to-end, not just diagrammed.

## 🏗️ Architecture

```
Client → Master (routing, cluster state, failover) → Cache Nodes (data + replication)
```

- **Master**: owns the hash ring, cluster membership, and topology version; routes every request to the correct primary/replica.
- **Cache Nodes**: store key-value data in memory, replicate to peers, and stream data during migrations or recovery.

## 🚀 Quick Start

```bash
# Build
cd Master && ./gradlew build && cd ../Node && ./gradlew build

# Run Master
cd Master && java -jar build/libs/Master-0.0.1-SNAPSHOT.jar

# Run a Cache Node
cd Node && SERVER_PORT=8082 MASTER_URL=http://localhost:8080 \
  java -jar build/libs/Node-0.0.1-SNAPSHOT.jar
```

```bash
# Write
curl -X POST http://localhost:8080/cache -H 'Content-Type: application/json' \
  -d '{"key":"user:123","value":"John","time":30}'

# Read
curl http://localhost:8080/cache?key=user:123
```

Docker Compose / multi-node setup and full configuration reference: see [`DETAILS.md`](./DETAILS.md).

## 🛠️ Tech Stack

`Java 21` `Spring Boot` `Gradle` `Docker` `REST` — distributed systems concepts: consistent hashing, quorum-free replication, gossip-free heartbeat failure detection, dual-write migrations.

## 🔮 Next Steps

- Gossip-based peer discovery (remove central master)
- Raft/Paxos-based leader election
- Persistent storage backend (RocksDB) + WAL
- Client-side topology-aware routing

---


**Document Version**: 2.0  
**Last Updated**: 26 July 2026
