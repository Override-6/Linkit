# LinKit - A personal project to easily synchronise and link programs
LinKit offers an entire networking interface, that can be extended and controlled by any 'Relay'  
The main key points of this project are the extension, stability, pleasant and the practicality  
The project is mainly write in scala, and some classes are write in Java.  

## Table of contents
* [Notes and General information](https://github.com/Override-6/LinKit/#notes-and-general-information)
* [The API](https://github.com/Override-6/LinKit/#the-api)
* [The Client](https://github.com/Override-6/LinKit/#the-client)
* The Server
* Features
* How to extend
* How to configure
* Setup
* Inspiration
* Acknowledgements

## Notes and General information
This Readme contains the general information to create your own project with this program  
If you want further details about the [API](https://github.com/Override-6/LinKit/blob/master/API), the [RelayServer](https://github.com/Override-6/LinKit/blob/master/RelayServer) or the [RelayPoint](https://github.com/Override-6/LinKit/blob/master/RelayPoint), you can open theirs respective folders.  
This project quickly progresses, and, until the project stands in beta, irreversible changes would often be made,  
such as refactoring, movement or deletion of different classes, packages, folders or methods, Illogical feature operations, bugs or even redesigning some aspect.  
Some Scala practices would be a real nightmare to use in Java, such as Builders, so they have a Java version in order to be more user-friendly with java developers.

### Versioning
The current versioning system is the [SemVer](https://semver.org/), but, during the beta phase of this project, it will not being respected.  
This way, the first number is always 0, any feature or irreversible change would increment the middle number, and small reversible patches increments the last number.  

#### During beta phase : 
0.x.y
* x feature, irreversible internal changes or remote changes such as packet protocol or communication 
* y patches, internal bugfixes, that are fully reversible  

No objectives are scheduled in order to exit the beta phase.

## The API
The API is a bag of features and definitions of behavior that a Relay implementation must support.  
Some features are pre-implemented and handled by the api, such as the [extension](TODO), the [event handling system](TODO) or the [Packet handling](TODO) and [Task scheduling](TODO) which are partially implemented.  
All of those features are accessible from the Relay interface.  

(see [API README](https://github.com/Override-6/LinKit/tree/master/API) to get further details)  

### Packet handling

Protocol Used : TCP

[Packet](https://github.com/Override-6/LinKit/blob/master/API/src/fr/override/linkit/api/packet/Packet.scala) implementations are simply serialised / deserialised using ObjectOuput/InputStreams. (the serialisation method may change to JSON). Any packet is serialised/deserialised with the [PacketTranslator](https://github.com/Override-6/LinKit/blob/master/API/src/fr/override/linkit/api/packet/PacketTranslator.scala), that handles the byte seqence of packet content __and__ their coordinates.

Here you can have a quick look to what the api is capable
### Network package

The network is represented into the package api.network, and ensures t

## The Client  
The Client, or RelayPoint, is one implementation of the Relay interface, which can connect to any server implementation.  
In order to instantiate a RelayPoint, you need a RelayPointConfiguration.  
The different option explications of a RelayPointConfiguration can be find in the [RelayPoint README](https://github.com/Override-6/LinKit/tree/master/RelayPoint), but here, we will focus on how to create a default RelayPoint by using its builder :  

```scala
val relayPoint: RelayPoint = new RelayPointBuilder {
    override var serverAddress: InetSocketAddress = serverAddress
    override var identifier: String = relayPointIdentifier
} //No need to write .build() thanks to the scala implicits !
relayPoint.start() 
```

Here you have the JavaBuilder version for Java users :
```java
RelayPoint relayPoint = RelayPointBuilder
    .javaBuilder(serverAddress, relayPointIdentifier)
    .build();
relayPoint.start();
```

## The Server
The Server, or RelayServer, is an implementation of the relay that handles multiple connections, it is meant to faciliate packet exchange between clients, and must make sure that it can behaves like a client.  
A server implementation is the center of the [network](TODO). Even if its network entity does not provide a lot more features, the server must ensure that every thing works correctly, and handles the connection informations and packet sharing.  

