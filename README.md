# TupleSpaces

Distributed Systems Project 2024

**Group A18**

*(choose one of the following levels and erase the other one)*\
**Difficulty level: I am Death incarnate!**

### Code Identification

In all source files (namely in the *groupId*s of the POMs), replace __GXX__ with your group identifier. The group
identifier consists of either A or T followed by the group number - always two digits. This change is important for
code dependency management, to ensure your code runs using the correct components and not someone else's.

### Team Members

| Number | Name              | User                             | Email                               |
|--------|-------------------|----------------------------------|-------------------------------------|
| 99309  | Rafael Girão      | <https://github.com/rafaelsgirao>   | <mailto:rafael.s.girao@tecnico.ulisboa.pt>   |
| 102082  | Simão Sanguinho       | <https://github.com/simaosanguinho>     | <mailto:simao.sanguinho@tecnico.ulisboa.pt>     |
| 103252  | José Pereira | <https://github.com/pereira0x> | <mailto:jose.a.pereira@tecnico.ulisboa.pt> |

## Getting Started

The overall system is made up of several modules. The different types of servers are located in _ServerX_ (where X denotes stage 1, 2 or 3).
The clients is in _Client_.
The definition of messages and services is in _Contract_. The future naming server
is in _NamingServer_.

See the [Project Statement](https://github.com/tecnico-distsys/TupleSpaces) for a complete domain and system description.

### Prerequisites

The Project is configured with Java 17 (which is only compatible with Maven >= 3.8), but if you want to use Java 11 you
can too -- just downgrade the version in the POMs.

To confirm that you have them installed and which versions they are, run in the terminal:

```s
javac -version
mvn -version
```

### Installation

To compile and install all modules:

```s
mvn clean install
```

## Built With

- [Maven](https://maven.apache.org/) - Build and dependency management tool;
- [gRPC](https://grpc.io/) - RPC framework.
