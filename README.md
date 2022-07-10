[<img alt="wakatime" src="https://wakatime.com/badge/github/Override-6/Linkit.svg"/>](https://wakatime.com/badge/github/Override-6/Linkit) (
counter since 3 march 2021)
<div style="text-align: center;"> <h1>The Linkit Framework</h1> </div>  

![Cover](RCover.png)

Linkit is a framework that handles the network side of an application, allowing it to share objects between the server
and its clients. Shared objects can be converted into "Connected Objects"
in which method calls can be triggered according to a defined contract amongst actual and distant versions of the
connected object. This special handling is available for instances of any public / non-final classes.

this readme will introduce the functionality of "connected objects", but the other features available are treated in the
WIKI.

### simple example of a chatting application

Let's say we want to create an online chat application in the Scala language.  
We will assume that it is a discord-like application, with chat servers, containing members and channels, in which
members can send text messages.

Here is our project architecture:  
<img height="350" src="https://github.com/Override-6/Linkit/blob/redesign/syncobjects_contract/Diagrams-Readme/ChattingUML.png?raw=true" width="350"/>

Our app has the following features:

* chat servers can accept, ban and kick members
* chat servers can create new channels
* members can send messages into channels of their servers
* members can modify their messages.

### What's next ?

Now that we have our architecture, and our features defined, we'll take a look at how to make all of those channels,
messages and chat servers synchronized for every client using our app.

First, we define our contract to specify what we want to synchronize, and how:

```bhv
name "DiscordLikeAppContract"

import org.discordlikeapp.**

describe ChatServer, Channel, Message {
    foreach method enable following broadcast
    
    ChatServer.addChannel(sync Channel)
    Channel.addMessage(sync Message)
}
```

For each method contained in ChatServer, Channel and Message, we specify that the method call must be triggered on all
clients (`enable following broadcast`)  
for ChatServer.addChannel(Channel) and Channel.addMessage(Message), we specify that the argument value will also get
converted to a connected object  
The contract must be contained on the server, and on the client.

We start our java FX application, and we will assume that all of our logic is contained in the `DiscordLikeWindow` class
in order to simplify the next steps, as we don't really want to explain how the app works, but only how Linkit can be
used to connect our channels/chat servers/messages etc.

```scala
class DiscordLikeWindow(user: User) {
    
    private val userServers = initChatServers()
    
    private def initChatServers(): Map[Int, ChatServer] = {
        val userPseudonym              = user.getPseudonym
        val network: Network           = connectToNetwork(pseudonym) //we init our network connection
        val contract                   = Contract("DiscordLikeAppContract") //we use the defined contract
        val chatServerCache            = network.globalCaches.attachToCache(userPseudonym.##, DefaultConnectedObjectCache[ChatServer](contract))
        val servers: Array[ChatServer] = chatServerCache.listAllRoots.toArray
        servers.map(s => (s.getId, s)).toMap
    }
    
    //...Other Methods
}
```

The `Network` object represents the Linkit Framework's network.  
the Network object allows you to attach to a `SharedCache`, in this case, we attach to the
associated `ConnectedObjectCache[ChatServer]` of the user's pseudonym   
(Note: as SharedCaches are identified using integers, we use `userPseudonym.##` to use the user's pseudonym hashcode as
an identifier for the cache.)

Now that we have initiated our ChatServer cache, we now just have to use the ChatServers contained in the map, and use
the objects as normal objects

As we defined in our contract that each method call on a connected object must be triggered on the server and on all connected clients, 
the synchronisation is now ensured by the contract. So once an object is modified (ex: a message is edited), it's gonna be modified all over
the network:

to post a message in a channel:

```scala
val chatServer: ChatServer = userServers(serverId)
val channel                = chatServer.getChannel(channelId)
val connectedMessage = channel.addMessage(new Message(user, "The message here"))
```
to edit a message: 
```scala
connectedMessage.modify("The modified Message")
```

to ban/kick/accept a member in a server:
```scala
val chatServer: ChatServer = userServers(serverId)
chatServer.kick(user)
chatServer.ban(user)
chatServer.accept(user)
```
etc
