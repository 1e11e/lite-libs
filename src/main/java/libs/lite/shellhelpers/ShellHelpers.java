package libs.lite.shellhelpers;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Misc helpers to use in java shell scripts.
 */
class ShellHelpers {
    static final Logger LOG = getLogger();

    public static void main(String... args) throws IOException, InterruptedException {
        System.out.println("hello " +  Ansi.GRN + "green " + Ansi.UNDERLINE + "world" + Ansi.RESET);

        LOG.setLevel(Level.ALL);
        LOG.info("some info here");
        LOG.config(() -> "configuration info");

        exec(false, ".", "ls");
        var output = exec(true, ".", "ls");
        System.out.println(">>> " + output.stdout.lines().count() + " lines");
    }

    /**
     * Get a logger with decent formatting: "[2023-08-11 10:59:01.799] WARNING message here (org.example.Main main)"
     */
    static Logger getLogger() {
        System.setProperty("java.util.logging.SimpleFormatter.format", "[%1$tF %1$tT.%1$tL] %4$-7s %5$s (%2$s)%6$s%n");
        var logger = Logger.getAnonymousLogger();
        logger.getParent().getHandlers()[0].setLevel(Level.ALL);
        return logger;
    }

    /**
     * Execute shell command. To disassociate the command from the java parent, use a sub shell.
     *     exec(false, ".", "sleep 99");                       // ps -ejH: gnome-terminal -> bash -> java -> sleep
     *     exec(false, ".", "bash", "-c", "(sleep 99)&");      // ps -ejH: gnome-terminal -> sleep
     *
     * @param capture If stdout and stderr should be captured in returning Output, or just printed to the console
     * @param cd Change directory to here when executing command
     * @param cmd Command to execute, as a single string ("ls file") or in parts ("ls", "file with space")
     * @return Output with exitcode, stdout and stderr. If not using capture, only exitcode is populated.
     */
    static Output exec(boolean capture, String cd, String... cmd) throws IOException, InterruptedException {
        var procBuilder = new ProcessBuilder(cmd.length > 1 ? cmd : cmd[0].split(" ")).directory(new File(cd));
        var proc = capture ? procBuilder.start() : procBuilder.inheritIO().start();
        return new Output(proc.waitFor(),
                new String(proc.getInputStream().readAllBytes()), new String(proc.getErrorStream().readAllBytes()));
    }
    record Output(int exitcode, String stdout, String stderr) {}
}

/**
 * Easy ANSI colors. There are four different blocks of eight values each. Not every terminal supports everything.
 * 1st block is normal colors.
 * 2nd block is control like RESET to restore everything, or DIM to get a darker version of the current color.
 * 3rd block "LT" is light/bright versions of the 1st block.
 * 4th block "BG" is background colors of the 1st block.
 * Example:
 *    System.out.println(Ansi.RED + "I'm red " + Ansi.DIM + "dark " + Ansi.BGCYN + "and cyan background" + Ansi.RESET);
 */
enum Ansi {
    BLK, RED, GRN, YEL, BLU, PUR, CYN, WHT, RESET, BOLD, DIM, ITALIC, UNDERLINE, BLINK, NOOP, REVERSE,
    LTBLK, LTRED, LTGRN, LTYEL, LTBLU, LTPUR, LTCYN, LTWHT, BGBLK, BGRED, BGGRN, BGYEL, BGBLU, BGPUR, BGCYN, BGWHT;
    @Override
    public String toString() { return "\u001B[" + (List.of(30, 0, 90, 40).get(ordinal() / 8) + ordinal() % 8) + "m"; }
}
