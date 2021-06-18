package de.conterra.dst;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * Class ProcessRunner manages a started Process and its input/output streams.
 * Process output/error is consumed asynchronously by separate threads.
 * Each line printed by the process is forwarded to the given output/error handlers.
 *
 * The process input stream may be written to directly.
 * Since each stream of a process send/receives a stream of bytes,
 * each of the needs an encoding information to convert the bytes into characters.
 * Common encodings are:
 *
 * Charset.forName("IBM850") for native DOS console commands.
 * Charset.forName("CP1252") for many WindowsXP applications.
 * StandardCharsets.UTF_16LE for some native windows applications.
 * StandardCharsets.UTF_8    for modern windows applications.
 *
 * The ProcessRunner implements AutoCloseable without exceptions.
 * Using close closes the process input stream, waits for the process to exit (if not already done)
 * and waits for the output streams to drain their remaining content to the handles.
 *
 * Calling close wll wait until 5 seconds for the process to terminate voluntary
 * else it is terminated forcibly.
 */
public class ProcessRunner implements AutoCloseable {

    protected final Process process;

    private final IOPipe outputHandler;
    private final IOPipe errorHandler;

    private final BufferedWriter inputWriter;

    /**
     * Create a new ProcessRunner.
     * @param process to monitor.
     * @param encoding to be used for the process input/ouput.
     * @param output target of output lines read.
     * @param error target of error lines read.
     */
    protected ProcessRunner(Process process, Charset encoding, Consumer<String> output, Consumer<String> error) {
        this.process = process;
        this.outputHandler = IOPipe.open("outputHandler", output, process.getInputStream(), encoding);
        this.errorHandler = IOPipe.open("errorHandler", error, process.getErrorStream(), encoding);
        this.inputWriter = new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), encoding));
    }

    /**
     * Get the character stream to send input to the process.
     * @return a BufferedWriter for the input.
     */
    public BufferedWriter getInputWriter() {
        return inputWriter;
    }

    /**
     * Propagate the exit handler of te process.
     * @return a Future to hook into.
     */
    public CompletableFuture<Process> onExit() {
        return process.onExit();
    }

    /**
     * Install a simple exit handler to be called on exit.
     * The action is called as soon as the process stops.
     * You must call close() to wait for the output streams to process any pending data.
     *
     * @param action to be called.
     * @return a CompletionStage wich can be monitored.
     * todo: wait for the output streams to finish processing pending lines.
     */
    public CompletionStage<Void> onExit(Runnable action) {
        return process.onExit().thenAccept(p -> action.run());
    }

    /**
     * May be called, stop the process any time.
     * This waits for the output streams to finish any remaining processing.
     */
    public void stop() {
        try {
            process.destroyForcibly();
        } finally {
            close();
        }
    }

    /**
     * Wait for the process to stop at least 5 seconds.
     * After that the process is stopped forcibly.
     * @throws InterruptedException if the waiting thread is interrupted.
     */
    public void shutdown() throws InterruptedException {
        try {
            process.getOutputStream().close();
            process.waitFor(5, TimeUnit.SECONDS);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            process.destroyForcibly();
            errorHandler.join();
            outputHandler.join();
        }
    }

    /**
     * Shutdown the process and do some more error handling.
     */
    @Override
    public void close() {
        try {
            shutdown();
        } catch (InterruptedException e) {
            // stop waiting, restore interrupt state and raise exception.
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }
}
