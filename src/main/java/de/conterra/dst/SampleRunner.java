package de.conterra.dst;

import java.io.*;
import java.nio.charset.Charset;

/**
 * An sample to run an external process.
 * This example runs a DIR command.
 */
public class SampleRunner {

    public static void main(String ... args) throws IOException {
        run("cmd.exe", "/c", "dir", "C:\\Windows");
    }

    public static void run(String ... args) throws IOException {

        Process process = new ProcessBuilder(args).start();

        // DOS commands still use Cp850 since 1981.
        Charset CP850 = Charset.forName("IBM850");

        // start the process and propagate handler functions for output and error.
        // try with resource automatically closes the reader and flushes its remaining output.
        try(ProcessRunner runner = new ProcessRunner(process, CP850, SampleRunner::print, SampleRunner::error)) {
            // example to read input from the keyboard and send it to the command.
            // since "DIR" does nor read any input, this will stall.
            //Reader keyboard = new InputStreamReader(System.in);
            //keyboard.transferTo(runner.getInputWriter());
        } finally {
            print("process finished");
        }
    }

    // outputs are made synchronized o prevent mixing incomplete lines on the console.

    /**
     * @param message to print.
     */
    static synchronized void print(String message) {
        System.out.print("out: ");
        System.out.print(message);
        System.out.println();
    }

    /**
     * @param message to print.
     */
    static synchronized void error(String message) {
        System.out.print("err: ");
        System.out.print(message);
        System.out.println();
    }

}
