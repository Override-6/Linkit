package fr.override.linkit.client;

public class JavaTests {

    public static void main(String[] args) {

    }

    @Override
    public boolean equals(Object obj) {
        return JavaTests.super.equals(obj);
    }
}
