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
* Customizable / Configurable packet persistence.
### Local
Most of local features are a non negligeable help for writing and maintaining your network developement.  
* Resource handling (Attach a representation for a Folder or a File, Access to resources of a distant machine, checksums).  
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
##### Introduction to Synchronized Objects.  
If you want to synchronize your objects, you'll have to open a `SynchronizedObjectCenter[T]` Cache, in which you can post objects, identified by an int, and retrieve them using theirs identifier.  
When you post an object on the cache (using `postObject(int, T)`), you'll create a SynchronizedObjectTree, in which the first object in the node tree is called a **root object**.   
All root objects of a `SynchronizedObjectCenter[T]` must have the type of `T`. but inner synchronized objects (such as fields, methods parameters or methods return values) can be any Object.  
A `SynchronizedObjectTree` contains a `SynchronizedObjectBehaviorCenter`, which is where all `SynchronizedObjectBehavior` are stored.  

##### Introduction to SynchronizedObjectBehavior
SynchronizedObjectBehavior contains all informations about the class's fields (FieldBehavior) and methods (MethodBehavior), that depicts what will they do as an object will get synchronized.  
Example :  
- For fields, they simply say if a field value must also be synchronized.  
- For Methods, They depicts what parameters will be synchronized between the caller engine and the processors engines.  
If the return value may also be synchronized, or if the remote invocation must follow some rules. See wiki for more.  
A SynchronizedObjectBehavior and a SynchronizedObjectBehaviorTree can be created using their builders.


##### Simple use case.
Let's say you have an `ArrayList` of `Player`, and you want the list to be the "same" for all connected engines.   
You also want you Players to be « The same for everyone », like, if you make any action on a player object on your engine, the action will be spread out to the other engines, as if they all get the « same instance ».
Here is what you would do : 
First: define a behavior tree (using ".bhv" files)
```bhv
describe class java.util.ArrayList {
   forall fields -> disable //no inner fields will become a synchronized object. (this is the default behavior)
   forall methods -> disable //no method invocation will be spread to the other engines. if a method call is performed on your client, you'll be the only one that will keep this action.
    
   //"broadcast" means that the RMI request will be send to every engines.  
   //"invokeonly" means that the RMI will be requested, but no result is awaited (return value or even method throwing).  
   enable method add as broadcast and invokeonly {
      args {
         0 -> enable // the first parameter object will be synchronized. this way, if you add a Player in your list, the Player object will become a synchronized object.
      }
      returnvalue -> false //do not synchronize the return value of the method.
   }
   //the behavior of the method add is pasted on the method remove. Opening brackets only performs modifications.
   enable method remove as add {
      args {
         0 -> disable //the first parameter is not synchronized (we only want to say that an object is removed, no need to sync it)
      }
   }
   
   //useless because we've already disabled all methods by default.
   disable toStream
   disable addAll
   //...
}
describe class fr.linkit.example.cache.obj.Player {
    forall methods -> enable as broadcast_if_owner //the RMI request will be requested only if the engine's owns the object.
    disable toString
    disable equals
    disable hashCode
}
```
Second: program
```scala
// using the « Global Cache »
val manager = helloConnection.network.globalCache
// we want our object cache be opened with the id '10'
val objectCenter = manager.retrieveCache(10, DefaultSynchronizedObjectCenter[ArrayList[Player]])
//our ListBuffer takes the identifier "0"
//The third parameter is optional. It defines the behavior of the objects contained in the tree of the synchronized ListBuffer object.
val synchronizedList = objectCenter.postObject(0, new ArrayList[Player](), BehaviorTreeResource("examples/PlayerTree.bhv")) 
// NOTE : Only the returned object is synchronized, the given one is only a base which will be cloned for the synchronization.
// now do what you want. remove, add player, move or kill a player... everything is gonna be the same for all engines.
```
##### More Complex use case : basic 2d game.
See the repository https://github.com/Override-6/2DShooter, and more specially the class https://github.com/Override-6/2DShooter/blob/master/core/src/main/scala/fr/overrride/game/shooter/session/PlayState.scala.
##### Even more Complex : Minecraft Server.
Just jocking. but maybe in few years this would be possible to create a minecraft server from the Client's code and then using Linkit and some ".bhv" files to handle this ? :0

## Acknowledgements
I owe a big part of my knowledge to a discord server named [ReadTheDocs](https://readthedocs-fr.github.io/), and some tutorials i found on internet.
Here is a non-ordered list of different people that helped me writing the project, or helped me get more trained with programmation :

- [TheElectronWill](https://github.com/TheElectronWill)
- [Akami](https://github.com/Tran-Antoine)
- [Hokkayado](https://github.com/Hokkaydo)
- [Mesabloo](https://github.com/Mesabloo)
- [MinusKube](https://github.com/MinusKube)

thanks for you <3
