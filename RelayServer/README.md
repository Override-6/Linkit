# linKit - Relay Server
The Relay Server is an implementation of the [API](https://github.com/Override-6/LinKit/blob/master/API/)  
This implementation can handle multiples Relay connection (such as [RelayPoint](https://github.com/Override-6/LinKit/blob/master/RelayPoint/) for example)
as long as the connected Relay respects the same [Packet formation](ref API/README.md#packet_formation)

1- In order to accept a client connection, the __first__ packet thats need to be send __directly__ once the connection socket is accepted is called a **welcome packet**
wich defines connection identifier.
