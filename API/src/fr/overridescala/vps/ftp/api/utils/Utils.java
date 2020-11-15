package fr.overridescala.vps.ftp.api.utils;

import fr.overridescala.vps.ftp.api.exceptions.UnexpectedPacketException;
import fr.overridescala.vps.ftp.api.packet.fundamental.DataPacket;
import scala.collection.Seq;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

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

    public static Path formatPath(String path) {
        String formatted = path
                .replace('\\', File.separatorChar)
                .replace('/', File.separatorChar);
        return Paths.get(formatted);
    }

    public static Path subPathOfUnknownFile(Path unknownFile, int from) {
        return subPathOfUnknownFile(unknownFile.toString(), from);
    }

    public static Path subPathOfUnknownFile(String path, int from) {
        int currentNameCount = -1;
        StringBuilder subPathBuilder = new StringBuilder();
        for (char c : path.toCharArray()) {
            if (c == '/' || c == '\\')
                currentNameCount += 1;
            if (currentNameCount >= from) {
                subPathBuilder.append(c);
            }
        }
        return Paths.get(subPathBuilder.toString().replace('\\', '/'));
    }

    public static void checkPacketHeader(DataPacket packet, Seq<String> expectedHeaders) throws UnexpectedPacketException {
        if (expectedHeaders.contains(packet.header()))
            return;
        String msg = expectedHeaders.mkString("or") + " expected, received : " + packet.header();
        throw new UnexpectedPacketException(msg);
    }


    public static InetSocketAddress getPublicAddress() {
        try {
            URL url = new URL("http://ipv4bot.whatismyipaddress.com");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream())
            );
            return new InetSocketAddress(reader.readLine(), Constants.PORT());
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.exit(-1);
        return null;
    }

}
