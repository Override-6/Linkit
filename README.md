[![wakatime](https://wakatime.com/badge/github/Override-6/Linkit.svg)](https://wakatime.com/badge/github/Override-6/Linkit)
<center> <h1>The Linkit Framework</h1> </center>  

![Cover](RCover.png)
 A Framework to easily synchronise and link (remote) JVM processes between them.  

## This readme is currently being rewritten
**The README is more of a messy tutorial than an actual introduction to the project.**  
**Take a look at the [wiki](https://github.com/Override-6/Linkit/wiki/Introducing-Linkit) for an introduction to the Linkit Framework : https://github.com/Override-6/Linkit/wiki/Introducing-Linkit**

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
