package fr.overridescala.vps.ftp.api.utils;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URL;
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


    public static InetSocketAddress getPublicAddress() {
        try {
            URL url = new URL("http://ipv4bot.whatismyipaddress.com");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream())
            );
            return new InetSocketAddress(reader.readLine(), 4848);
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.exit(-1);
        return null;
    }

}
