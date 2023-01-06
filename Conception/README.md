# A little bit confused...

Hey, I'm maxime, the only developper of this ambitious project.  
Ambitious ? Why is Linkit ambitious ? Well, honestly, i can't even figure it out myself.  
Linkit was originally meant to be a fast project for synchronizing my computer's file system to a vps I bought
impulsively for
the occasion, (a FS synchronized with a cloud server such as OneDrive already does).

But at that time, the project was codded so hatively that I was kinda ashamed by its code quality, so I tried to enhance
its structure, and then I developped packet channels in which you can send and receive a restricted set of packets.  
I was not particularly proud of that, but I felt that I liked to send data between clients and servers.  
I remember that I was frustrated not to be able to send any kind of objects I wanted threw my packet channels, because
the serializer used was hand made and string-based, it was only able to send few type of packet, it was simple but
really boring to extend and absolutely
[horrible](https://github.com/Override-6/Linkit/blob/e06894fb0ae330e4da9c6a5f87b2f649978a49bf/API/src/fr/overridescala/vps/ftp/api/packet/Protocol.scala)

So I made a second version, the serializer outputed binary and accepted any object that was not required to implement
the Serializable interface,
but of course, due to this permisive trait, it was stuttering in front of any architectural complexity
of the objects it handled, and was hard to maintain.  
Then I made few more versions that were more and more conceptualized and that was able to support objects of a bigger complexity.  
I don't know why, but I like to make serializers, I made few more until the actual one (of which I'm quite proud but
still have a lot of issues and is really slow).
What I like in making serializers, is to be able to support any situation and object structural complexity.

... TODO continuer de raconter sa vie