[View code on GitHub](https://github.com/oxyg3nium/oxyg3nium/.autodoc/docs/json/app/src/main/scala/org/oxyg3nium)

The `.autodoc/docs/json/app/src/main/scala/org/oxyg3nium/app` folder contains essential Scala files for the Oxyg3nium project, a blockchain platform. These files handle API configurations, block exporting and importing, application booting, CPU solo mining, API documentation generation, and REST and WebSocket server management.

For instance, `ApiConfig.scala` defines the `ApiConfig` class and its companion object, responsible for loading and validating configuration parameters for the Oxyg3nium API. This makes it easy to pass around and use these parameters in other parts of the codebase.

```scala
val apiConfig: ApiConfig = ...
val apiPort: Int = apiConfig.port
```

`BlocksExporter.scala` and `BlocksImporter.scala` provide functionality for exporting and importing blocks from the Oxyg3nium blockchain to a file, useful for analysis, backup, or migration purposes.

```scala
val blockFlow: BlockFlow = ...
val outputPath: Path = ...
val blocksExporter = new BlocksExporter(blockFlow, outputPath)
val filename = "exported_blocks.txt"
val exportResult = blocksExporter.export(filename)
```

`Boot.scala` serves as the Oxyg3nium application entry point, initializing the system by calling the `BootUp` class, which sets up the application environment, checks database compatibility, logs configurations, and starts the server.

`CpuSoloMiner.scala` defines a CPU solo miner for the Oxyg3nium cryptocurrency, allowing users to mine Oxyg3nium blocks using their CPU. This is achieved by creating a `CpuSoloMiner` instance that uses the `ExternalMinerMock` class to create a mock miner.

```scala
val minerConfig: MinerConfig = ...
val blockFlow: BlockFlow = ...
val miner = new CpuSoloMiner(minerConfig, blockFlow)
val miningResult = miner.start()
```

`Documentation.scala` generates documentation for the Oxyg3nium API using the OpenAPI specification. It defines the endpoints for the API, generates the OpenAPI specification, and includes information about the servers that can be used to access the API.

`RestServer.scala` and `WebSocketServer.scala` create and manage REST and WebSocket servers, respectively, which expose various endpoints for interacting with the Oxyg3nium blockchain. These servers are designed to be extensible and configurable, allowing developers to easily add new endpoints and customize the behavior of the servers.

```scala
val restServerConfig: RestServerConfig = ...
val blockFlow: BlockFlow = ...
val restServer = new RestServer(restServerConfig, blockFlow)
val serverResult = restServer.start()
```

Overall, the code in this folder plays a crucial role in the Oxyg3nium project, providing essential functionality for managing the Oxyg3nium blockchain, its API, and various server components.
