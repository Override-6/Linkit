package fr.override.linkit.api.utils;

import fr.override.linkit.api.packet.fundamental.DataPacket;
import fr.override.linkit.api.exception.UnexpectedPacketException;
import scala.collection.Seq;

import java.io.*;


public class Utils {

    public static synchronized byte[] serialize(Object object) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(object);
            return bos.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new byte[0];
    }

    public static synchronized <T> T deserialize(byte[] bytes) {
        try {
            return unsafeDeserialize(bytes);
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public static synchronized <T> T unsafeDeserialize(byte[] bytes) throws IOException, ClassNotFoundException {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
             ObjectInputStream ois = new ObjectInputStream(bis)) {
            return (T) ois.readObject();
        }
    }

    public static void checkPacketHeader(DataPacket packet, Seq<String> expectedHeaders) throws UnexpectedPacketException {
        if (expectedHeaders.contains(packet.header()))
            return;
        String msg = expectedHeaders.mkString("or") + " expected, received : " + packet.header();
        throw new UnexpectedPacketException(msg);
    }

}
