package de.conterra.dst;

import java.io.*;
import java.nio.charset.Charset;
import java.util.function.Consumer;

/**
 * Class IOPipe starts a separate thread to
 * transfer incoming strings from a Reader to an consumer.
 *
 * An input of bytes (InputStream) can be used,
 * if the encoding of the incoming bytes is known.
 *
 */
class IOPipe extends Thread {

    /**
     * Open an IOPipe reading characters.
     *
     * @param name Name of the generated thread.
     * @param target Receiver of the lines read.
     * @param source Source of characters to read.
     * @return The new IOPipe thread created.
     */
    public static IOPipe open(String name, Consumer<String> target, Reader source) {
        return new IOPipe(name, target, source);
    }


    /**
     * Open an IOPipe reading characters.
     *
     * @param name Name of the generated thread.
     * @param target Receiver of the lines read.
     * @param source Source of bytes to read.
     * @param encoding Encoding of the input bytes read.
     * @return The new IOPipe thread created.
     */
    public static IOPipe open(String name, Consumer<String> target, InputStream source, Charset encoding) {
        return open(name, target, new InputStreamReader(source, encoding));
    }

    /**
     * Open an IOPipe reading characters.
     *
     * @param name Name of the generated thread.
     * @param target Receiver of the lines read.
     * @param source Source of bytes to read.
     */
    IOPipe(String name, Consumer<String> target, Reader source) {
        super(name);
        this.reader = source;
        this.target = target;
        this.start();
    }

    final Reader reader;
    final Consumer<String> target;

    /**
     * Reader loop to read and send lines until the reader is closed.
     * Any error terminates the thread without further notifications.
     * todo: add some error handler.
     */
    @Override
    public void run() {
        try (var is = new BufferedReader(reader)) {
            is.lines().forEach(target);
        } catch (IOException e) {
            // turn it into an runtime exception,
            // no further exception handling implemented.
            throw new UncheckedIOException(e);
        }
    }
}
