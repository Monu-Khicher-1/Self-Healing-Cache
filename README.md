# Distributed Cache
## Project Overview

This is a **Distributed Self-Healing Cache System** built with Spring Boot that implements a Master-Slave architecture for managing a distributed in-memory cache. The system is designed to handle data distribution, rebalancing, replication, and automatic failure recovery across multiple nodes.
Caching is working in single thread, however, other background tasks like rebalancing, health checking, and data transfer are handled asynchronously.
---

## Implemented Features
1. Master-Slave architecture with centralized coordination
2. Consistent hashing with TreeMap-based hash ring
3. GET/PUT operations with TTL support
4. Automatic rebalancing when nodes join/leave
5. Health checking with 60-second heartbeat intervals
6. Dead node detection
7. Asynchronous data transfer during rebalancing
8. TTL-based automatic cache expiration (60-second cleanup)


## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    Client                                   │
└──────────────────────┬──────────────────────────────────────┘
                       │ HTTP Requests
                       ▼
┌────────────────────────────────────────────────────────────┐
│                      MASTER                                │
│  ┌─────────────────────────────────────────────────────┐   │
│  │         Consistent Hash Ring (TreeMap)              │   │
│  │    Maps keys to nodes for data distribution         │   │
│  └─────────────────────────────────────────────────────┘   │
│  ┌─────────────────────────────────────────────────────┐   │
│  │      Node Facade Service & Rebalance Service        │   │
│  │    Manages nodes, handles rebalancing logic         │   │
│  └─────────────────────────────────────────────────────┘   │
│  ┌─────────────────────────────────────────────────────┐   │
│  │   Discovery Service (Health Checking)               │   │
│  │   Detects dead nodes & removes them automatically   │   │
│  └─────────────────────────────────────────────────────┘   │
└────┬───────────────────────────────────────────────────────┘
     │
     │ HTTP Requests (GET/PUT operations & rebalancing)
     │
     ├──────────────────┬────────────────────┬────────────────┐
     ▼                  ▼                    ▼                ▼
┌─────────┐       ┌─────────┐          ┌─────────┐      ┌─────────┐
│ Node 1  │       │ Node 2  │          │ Node 3  │      │ Node N  │
│ ┌─────┐ │       │ ┌─────┐ │          │ ┌─────┐ │      │ ┌─────┐ │
│ │Cache│ │       │ │Cache│ │          │ │Cache│ │      │ │Cache│ │
│ │  +  │ │       │ │  +  │ │          │ │  +  │ │      │ │  +  │ │
│ │ TTL │ │       │ │ TTL │ │          │ │ TTL │ │      │ │ TTL │ │
│ └─────┘ │       │ └─────┘ │          │ └─────┘ │      │ └─────┘ │
└─────────┘       └─────────┘          └─────────┘      └─────────┘
```

---


## Working on
1. Replication logic and building self-healing system
2Implementing "PUT" and "DELETE" operations in the cache.

### Strategies
1. "DUAL WRITE" strategy: For handling data updates during rebalancing


## Future Scope (Not Included in current plan)

1. Functionality without Master (i.e. fully distributed)
2. Gossipping protocol for node discovery and communication
3. Quorum
4. Dockerization of the system
5. gRPC for internode communication 
6. Trying different hashing strategies.

---

**Document Version**: 1.0  
**Last Updated**: July 2026  
**Next Review**: End of Week 2




