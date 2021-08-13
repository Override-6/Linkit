The Linkit Framework.
![Cover](RCover.png)
 A Framework to easily synchronise and link (remote) JVM processes between them.  

# This readme is currently being rewritten
**WARNING :** for now, this framework should be used only for trusted networks, a security update that adds permissions will be sooner available, and JVM versions under 11 will not be supported.

 
## Features
### Network
* Multiple connection per application.
* Synchronized/Shared cache : 
   - Extendable and easy to use shared cache system.
   - Simple shared caches             : Shared Collection, Shared Map, Shared Instances
   - More complex and powerful caches : Synchronized objects, (like CORBA) but more powerful because any kind of object (see specification) can be synchronized without writing any piece of code.
     However, the synchronised objects behaviors can be controlled using Behavior descriptors and ".bhv" files. (see wiki for more).
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
Note: The kind of application and connection created depends on the used Engine Implementation (Client module creates ClientApplication and creates ClientConnection, and Server module creates ServerApplication, and opens ServerConnection)  
```scala
val config           = new ServerApplicationConfigBuilder {
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
## Acknowledgements
I owe a big part of my knowledge to a discord server named [ReadTheDocs](https://readthedocs-fr.github.io/), and some tutorials i found on internet.
Here is a non-ordered list of different people that helped me writing the project, or helped me get more trained with programmation :

- [TheElectronWill](https://github.com/TheElectronWill)
- [Akami](https://github.com/Tran-Antoine)
- [Hokkayado](https://github.com/Hokkaydo)
- [Mesabloo](https://github.com/Mesabloo)
- [MinusKube](https://github.com/MinusKube)

thanks for you <3
