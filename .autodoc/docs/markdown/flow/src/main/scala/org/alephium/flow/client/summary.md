[View code on GitHub](https://github.com/oxyg3nium/oxyg3nium/.autodoc/docs/json/flow/src/main/scala/org/oxyg3nium/flow/client)

The `Node.scala` file in the Oxyg3nium project defines the structure and behavior of a node in the Oxyg3nium network. It consists of a `Node` trait and a `Node` object, which provide the necessary components and methods for managing a node in the network.

The `Node` trait extends the `Service` trait, which provides methods for starting and stopping a service. It defines several components that are essential for a node's operation, such as `BlockFlow`, `MisbehaviorManager`, `DiscoveryServer`, `TcpController`, `Bootstrapper`, `CliqueManager`, `EventBus`, and `AllHandlers`. These components handle various aspects of the node's functionality, such as managing the blockchain, discovering other nodes, managing TCP connections, bootstrapping new nodes, managing consensus algorithms, and broadcasting events.

The `Node` object provides a default implementation of the `Node` trait and defines methods for building and initializing a node. The `build` method creates a new `Node` object, taking a `Storages` object for managing data storage and an `ActorSystem` object for managing actors in the node. The `buildBlockFlowUnsafe` method creates a new `BlockFlow` object, representing the blockchain in the Oxyg3nium network. It checks if the node has been initialized and creates a `BlockFlow` object from either the storage or the genesis block accordingly. The `checkGenesisBlocks` method ensures that the genesis blocks in the `BlockFlow` object match the genesis blocks in the configuration file, throwing an exception if they do not match.

Here's an example of how the `Node` object might be used to create a new node:

```scala
import org.oxyg3nium.flow.client.Node
import org.oxyg3nium.flow.storage.Storages
import akka.actor.ActorSystem

val storages = Storages.default()
val actorSystem = ActorSystem("Oxyg3niumNodeSystem")
val node = Node.build(storages, actorSystem)
```

In this example, a new `Node` object is created using the `build` method, with a default `Storages` object and an `ActorSystem` object named "Oxyg3niumNodeSystem". This new node can then be used to interact with the Oxyg3nium network, manage the blockchain, and perform other node-related tasks.

In summary, the `Node.scala` file defines the structure and behavior of a node in the Oxyg3nium network, providing the necessary components and methods for managing a node's operation. The `Node` trait and `Node` object work together to handle various aspects of a node's functionality, such as managing the blockchain, discovering other nodes, managing TCP connections, bootstrapping new nodes, managing consensus algorithms, and broadcasting events. This file is essential for understanding how nodes work within the Oxyg3nium project and how they interact with other parts of the system.
