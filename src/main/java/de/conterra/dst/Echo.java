package de.conterra.dst;

/**
 * Print out all commandline arguments separately to debug argument splitting.
 */
public class Echo {

    public static void main(String ... args) {
        for (String arg : args) {
            System.out.append('[').append(arg).append("]\n");
        }
    }
}
