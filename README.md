# The Linkit Framework.
![Cover](RCover.png)
 A Framework to easily synchronise and link (remote) JVM processes between them.  

## This readme is currently being rewritten
**WARNING :** for now, this framework should be used only for trusted networks, a security update that adds permissions will be sooner available, and JVM versions under 11 will not be supported.

 
## Features
### Network
* Multiple connection per application.
* Synchronized/Shared cache : 
   - Extendable and easy to use shared cache system.
   - Simple shared caches             : Shared Collection, Shared Map, Shared Instances
   - More complex and powerful caches : Synchronized objects, (like CORBA) but more powerful because any kind of object (see specification) can be synchronized without writing any piece of code.
     However, the synchronised objects' behaviors can be controlled using Behavior descriptors and ".bhv" files. (see wiki for more).
* Static RMI. basic feature but can be very powerful if combined with synchronized objects.
* Packet management, (registrable channels, Multithreaded packet injection, extendable system)
* Customizable packet persistence.
### Local
Most of local features are a non negligeable help for writing and maintaining your network developement.  
* Resource handling (Attach a representation for a Folder or a File, Access to resources of a distant machine).  
* ForkJoinPool-like thread system + Using Scala Futures for thread tasks.  
* Simple Class Source generation (using ClassBlueprints) and language Compilator management in order to quickly create classes at runtime.  
* ClassMapping (Mainly used for Packet Persistence) Simply assignates a class name to its name hashcode code.  
* Script creation. Write your code in a file and then use it for configuration, simple execution, performing remote code execution... 

## Some examples
### Creating the Application and opening a Connection.
Note: The kind of application and connection created depends on the used Engine Implementation (Client module creates ClientApplication and creates ClientConnection, and Server module creates ServerApplication, and opens ServerConnection).  
Here is an example for the Server module : (Client Modules nearly changes nothing)
```scala
val config = new ServerApplicationConfigBuilder {
            //Note : You can specify settings by oberriding values in the ApplicationConfigBuilder. See wiki for further details.
            loadSchematic = new ScalaServerAppSchematic {
                servers += new ServerConnectionConfigBuilder {
                    override val identifier: String = "HelloServer"
                    override val port      : Int    = 48484
                    nWorkerThreadFunction = c => c + 1 //allow one thread per connection.

                    configName = "HelloConfig"
                }
            }
        }
        val serverApp = ServerApplication.launch(config, getClass)
        AppLogger.trace(s"Build complete: $serverApp")
        val helloConnection = serverApp.findConnection("HelloServer").get //also works using the server port

```

### Synchronized and Shared caches
#### Introduction to SharedCacheManager
Any network is represented by its Network object. In which you can access to all « Engine » objects where an Engine object represent an Engine connection which is connected to the server (the Server Engine is included in the Network).  
The Network object can create a SharedCacheManager. A SharedCacheManager is identified to a « Family » String, and the cache manager can open or retrieve caches from a cache integer identifier.  
Each Engine Object contains a default cache manager, where the family string is the engine's identifier. There is also a « global » cache manager of family « Global Cache » which is directly stored in the Network object.  
Note : The fact that a cache manager is « owned » by a' engine could be a bit misleading. Dont forget this : they are only here for categorisation. The fact that a cache manager is « Global » or not does not affect the caches behavior.
For further details about the Shared Cache Management, take a look at the wiki.

#### Simple Shared Cache 
There is currently two type of simple caches : SharedMap, SharedCollection and SharedInstance (the SharedInstance is a simple ValueWrapper).  
You can create any cache like this :  

```scala
//let's say we work on the global cache manager, and we open a SharedCollection of Strings
val manager = helloConnection.network.globalCache
val cacheId: Int = 45 //cacheID of our cache is 45
val collection = manager.retrieveCache(45, SharedCollection[String])
collection.addListener(str => println(s"new String added $str"))
collection.add("Hello")
collection.add("World") //all remote engines that will get the cache from the Global Shared Cache Manager will get it updated.
```

#### Complex Shared Cache (Synchronized Objects)


## Acknowledgements
I owe a big part of my knowledge to a discord server named [ReadTheDocs](https://readthedocs-fr.github.io/), and some tutorials i found on internet.
Here is a non-ordered list of different people that helped me writing the project, or helped me get more trained with programmation :

- [TheElectronWill](https://github.com/TheElectronWill)
- [Akami](https://github.com/Tran-Antoine)
- [Hokkayado](https://github.com/Hokkaydo)
- [Mesabloo](https://github.com/Mesabloo)
- [MinusKube](https://github.com/MinusKube)

thanks for you <3
