package libs.lite.shellhelpers;

import javax.net.ssl.*;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Misc helpers to paste into java shell scripts.
 */
class ShellHelpers {
    static final Logger LOG = getLogger();

    public static void main(String... args) throws IOException, InterruptedException {
        var gitIgnore = httpGet("https://raw.githubusercontent.com/1e11e/lite-libs/refs/heads/main/.gitignore");
        System.out.println("httpGet():\n" + gitIgnore);

        System.out.println("Ansi: hello " +  Ansi.GRN + "green " + Ansi.UNDERLINE + "world" + Ansi.RESET);

        LOG.setLevel(Level.ALL);
        LOG.info("Logger example, some info here");
        LOG.config(() -> "Logger lambda example, configuration info");

        var output = exec(true, ".", "ls");
        System.out.println("exec(): " + output.stdout.lines().count() + " lines");
        // echo to stdout, no capture
        exec(false, ".", "ls");
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

    /**
     * Simple http GET.
     * @param url
     * @return Response as string
     */
    static String httpGet(String url) {
        try (var inputStream = new URI(url).toURL().openStream()) {
            return new String(inputStream.readAllBytes());
        } catch (URISyntaxException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Same as --insecure for curl
     */
    static void noCheckCertificate() {
        try {
            var sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, new TrustManager[] { new X509TrustManager() {
                        public java.security.cert.X509Certificate[] getAcceptedIssuers() { return null; }
                        public void checkClientTrusted(X509Certificate[] certs, String authType) { }
                        public void checkServerTrusted(X509Certificate[] certs, String authType) { }
                    }}, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            throw new RuntimeException(e);
        }
    }
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
