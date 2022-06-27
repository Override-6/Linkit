[![wakatime](https://wakatime.com/badge/github/Override-6/Linkit.svg)](https://wakatime.com/badge/github/Override-6/Linkit) (counter since 3 march 2021)
<center> <h1>The Linkit Framework</h1> </center>  

![Cover](RCover.png)
A framework to quickly create the network side of an application.
Linkit is a framework that handles the network side of an application,  
allowing it to share objects between the server and its clients. 
Shared objects can be converted into "Connected Objects" 
in which method calls can be triggered according to a defined contract amongst actual and distant versions of the connected object.
This special handling is available for instances of any public / non-final classes, .

## Some Examples
this readme will introduce the functionality of "connected objects",
but the other features available are treated in the WIKI. 

### simple example of a chatting application
Let's say we want to create an online chat application in the Scala language.  
We will assume that it is a discord-like application, with chat servers, containing members and channels, in which members can send text messages.

Here is our project architecture:
<img src="https://github.com/Override-6/Linkit/blob/redesign/syncobjects_contract/Diagrams-Readme/ChattingUML.png?raw=true" width="350" height="350" />

Our app has the following features:
* chat servers can accept, ban and kick members
* chat servers can create new channels  
* members can send messages into channels of their servers
* members can modify their messages.

### What's next ?
Now that we have our architecture, and our features defined, we'll take a look 
at how to make all of those channels, messages and chat servers synchronized for every client
using our app.

We start our java FX application, and we will suppose that all of our logic is contained in 
the `DiscordLikeWindow` class in order to simplify the next steps, as we don't really want to explain
how the app works, but only how Linkit can be used to connect our channels/chat servers/messages etc.
```scala
import javafx.application.Application
class DiscordLikeApp extends Application {
    
    override def start(primaryStage: Stage) {
        primaryStage.setTitle("Discord Like Application")
        val window = new DiscordLikeWindow()
        Scene scene = new Scene(window, 1920, 1080)
        primaryStage.setScene(scene)
        primaryStage.show()
    }

}
```

in `DiscordLikeWindow`, whe have the following methods: 

```scala
def initChatServers(userPseudonym: String): ConnectedObjectCache[ChatServer] = {
    val network : Network = connectToNetwork(pseudonym) //we init our network connection
    val servers = network.globalCaches.attachToCache(userPseudonym.##, DefaultConnectedObjectCache[ChatServer])
    servers
}
```
The `Network` object represents the Linkit Framework's network.  
You can collect information about the server and the client (the `Engine` class is used to represent a server or a client connection in the network.).  
You can also attach to a `SharedCache`, in this case, we want to attach to the associated `ConnectedObjectCache[ChatServer]` of the user's pseudonym   
(Note: as SharedCaches are identified using integers, we can use `userPseudonym.##` to use the user's pseudonym hashcode as an identifier for the cache.)

Now that we have initiated our ChatServer list, we 

```bhv
name "DiscordLikeAppContract"

import org.discordlikeapp.**

describe ChatServer, Channel, Message {
    broadcast *
}

```