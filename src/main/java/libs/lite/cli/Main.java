package libs.lite.cli;

import java.util.List;

/*
 * This is a small example of how to use the command line option parser of Lite Libs.
 * When using it in a script you can't use import libs.lite.cli.Cli, instead you paste the class into your script.
 * See main.jsh for such an example.
 */
public class Main {
    public static void main(String[] args) {
        args = "-po 128  --skip .jpg --backup=.orig logo.png overview.png product.png".split(" ");
        var cli = new Cli(List.of(args), """
                Watermark 1.0 (dummy example)
                Apply a watermark image file to other image files.
                Usage: watermark [options] <watermark> <image> [<image>...]
                """, "FILES",
                new Cli.Option('o', "--opacity=<percent>", Cli.Arity.ONE,  "Opacity in %", "\\d\\d?|100", "Not 0-100"),
                new Cli.Option(' ', "--skip=<ext>,...",    Cli.Arity.MANY, "Skip these file extensions"),
                new Cli.Option('b', "--backup[=<ext>]",    Cli.Arity.ZERO_ONE, "Make backup files, default extension .bak"),
                new Cli.Option('p', "",                    Cli.Arity.ZERO, "Use parallel execution")
        );
        if (cli.getAll("FILES").size() < 2)
            System.exit(cli.printError("Two or more files required"));

        String watermark = cli.getAll("FILES").removeFirst();
        int opacity = Integer.parseInt(cli.get("--opacity", "50"));
        String bak = cli.has("--backup") ? cli.get("--backup", ".bak") : "";

        (cli.has("-p") ? cli.getAll("FILES").parallelStream() : cli.getAll("FILES").stream())
                .filter(img -> cli.getAll("--skip").stream().noneMatch(img::endsWith))
                .forEach(img -> applyWatermark(watermark, img, opacity, bak));
    }

    static void applyWatermark(String watermark, String img, int opacity, String bak) {
        System.out.println("watermark: %s, img: %s, opacity: %d, bak: %s".formatted(watermark, img, opacity, bak));
    }
}

