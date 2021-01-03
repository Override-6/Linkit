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
The API is a bag of features and definitions of performance an implementation must support.  
Some features are pre-implemented and handled by the api, such as the [extension](TODO), the [event handling system](TODO) or the [Packet handling](TODO) and [Task scheduling](TODO) which are partially implemented.  
All of those features are accessible from the Relay interface. 

TODO continue this part

##The Client  
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

Here you have the JavaBuilder version if you are using Java :
```java
RelayPoint relayPoint = RelayPointBuilder
    .javaBuilder(serverAddress, relayPointIdentifier)
    .build();
relayPoint.start();
```
