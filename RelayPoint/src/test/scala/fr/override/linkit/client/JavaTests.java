package fr.override.linkit.client;

import java.util.Arrays;
import java.util.List;

public class JavaTests {

    public static void main(String[] args) {
        List<String> names = Arrays.asList("Natan", "Joel", "Daniel", "Tristan");
        names.stream()
                .findAny();
    }

}
