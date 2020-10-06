# FileTransferer
FileTransferer is a member of a bigger personnal projet called VPS this project is a groupement of other sub projects that's need to be hosted and would work all the time. This FileTransferer projet links my home computer and my works' computer through the network

## Functionalities
This program is divided in three parts, API, RelayServer and RelayPoint.
RelayServer and RelayPoint does not have any dependance between them, but they implements the API.
RelayServer is the server, that will handle connection between him and the RelayPoints, which are basically the clients that can only connect to RelayServers.
### API
#### Task
the API offers you to make any thing you want threw the network with a task feature. Tasks communicates with their Tasks completer with Packets.
##### Tasks and completers
    Any task comming from a Relay
##### The Task class
