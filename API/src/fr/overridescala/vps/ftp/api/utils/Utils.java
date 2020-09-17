package fr.overridescala.vps.ftp.api.utils;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

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
        return Path.of(formatted);
    }

    public static Path subPathOfUnknownFile(Path unknownFile, int from) {
        String path = unknownFile.toString();
        var currentNameCount = 0;
        var subPathBuilder = new StringBuilder();
        for (char c : path.toCharArray()) {
            if (c == '/' || c == '\\')
                currentNameCount += 1;
            if (currentNameCount >= from) {
                subPathBuilder.append(c);
            }
        }
        return Path.of(subPathBuilder.toString().replace('\\', '/'));
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
