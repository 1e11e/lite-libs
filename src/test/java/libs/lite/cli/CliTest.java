package libs.lite.cli;

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

public class CliTest {

    static final String POS_KEY = "POS_KEY";
    static final Set<String> SUB_KEYS = Set.of("SUB_KEY1", "SUB_KEY2");

    Cli.Option optZero = new Cli.Option('z', "--zero", Cli.Arity.ZERO, "");
    Cli.Option optOne = new Cli.Option('o', "--one", Cli.Arity.ONE, "");
    Cli.Option optZeroOne = new Cli.Option('x', "--zero-one", Cli.Arity.ZERO_ONE, "");
    Cli.Option optMany = new Cli.Option('m', "--many", Cli.Arity.MANY, "");

    Cli.Option optZero2 = new Cli.Option('Z', "--zero2", Cli.Arity.ZERO, "");

    private Cli getCli(String args, Cli.Option... optv) {
        final var isFirstConstructorCall = new AtomicBoolean(true);
        return new Cli(List.of(args.split(" ")), "Helptext", optv) {
            @Override
            public Optional<String> getError() {
                return isFirstConstructorCall.getAndSet(false) ? Optional.empty() : super.getError();
            }
        };
    }

    private Cli getCli(String args, String posParamName, Cli.Option... optv) {
        final var isFirstConstructorCall = new AtomicBoolean(true);
        return new Cli(List.of(args.split(" ")), "Helptext", posParamName, optv) {
            @Override
            public Optional<String> getError() {
                return isFirstConstructorCall.getAndSet(false) ? Optional.empty() : super.getError();
            }
        };
    }

    private Cli getCli(String args, Set<String> subcommands, Cli.Option... optv) {
        final var isFirstConstructorCall = new AtomicBoolean(true);
        return new Cli(List.of(args.split(" ")), "Helptext", subcommands, optv) {
            @Override
            public Optional<String> getError() {
                return isFirstConstructorCall.getAndSet(false) ? Optional.empty() : super.getError();
            }
        };
    }


    @Test
    public void testInvalidOption() {
        Cli.Option[] options = { optZero, optOne };
        assertEquals("Bad option: --nok", getCli("--nok --zero --one=val", options).getError().get());
        assertEquals("Bad option: --nok", getCli("--zero --nok --one=val", options).getError().get());
        assertEquals("Bad option: --nok", getCli("--zero --one=val --nok", options).getError().get());
        assertEquals("Bad option: --nok=opt", getCli("--nok=opt --zero --one=val", options).getError().get());
        assertEquals("Bad option: --nok=opt", getCli("--zero --nok=opt --one=val", options).getError().get());
        assertEquals("Bad option: --nok=opt", getCli("--zero --one=val --nok=opt", options).getError().get());
        assertEquals("Bad option: --nok", getCli("--zero --one val --nok opt", options).getError().get());
    }

    @Test
    public void testInvalidArg() {
        Cli.Option[] options = { optZero, optOne };
        assertEquals("Bad option: nok", getCli("nok --zero --one val", options).getError().get());
        assertEquals("Bad option: nok", getCli("--zero nok --one val", options).getError().get());
        assertEquals("Bad option: nok", getCli("--zero --one val nok", options).getError().get());
    }

    @Test
    public void testInvalidOptionPositionalParameter() {
        Cli.Option[] options = { optOne };
        assertEquals("Bad option: --nok", getCli("--nok file.txt --one=val", POS_KEY, options).getError().get());
        assertEquals("Bad option: --nok", getCli("file.txt --nok --one=val", POS_KEY, options).getError().get());
        assertEquals("Bad option: --nok", getCli("file.txt --one=val --nok", POS_KEY, options).getError().get());
        assertEquals("Bad option: --nok=opt", getCli("--nok=opt file.txt --one=val", POS_KEY, options).getError().get());
        assertEquals("Bad option: --nok=opt", getCli("file.txt --nok=opt --one=val", POS_KEY, options).getError().get());
        assertEquals("Bad option: --nok=opt", getCli("file.txt --one=val --nok=opt", POS_KEY, options).getError().get());

        assertEquals("Bad option: --nok", getCli("--nok --one=val file.txt", POS_KEY, options).getError().get());
        assertEquals("Bad option: --nok", getCli("--one=val --nok file.txt", POS_KEY, options).getError().get());
        assertEquals("Bad option: --nok", getCli("--one=val file.txt --nok", POS_KEY, options).getError().get());
        assertEquals("Bad option: --nok=opt", getCli("--nok=opt --one=val file.txt", POS_KEY, options).getError().get());
        assertEquals("Bad option: --nok=opt", getCli("--one=val --nok=opt file.txt", POS_KEY, options).getError().get());
        assertEquals("Bad option: --nok=opt", getCli("--one=val file.txt --nok=opt", POS_KEY, options).getError().get());

        Cli.Option[] options2 = { optZero };
        assertTrue(getCli("--zero file.txt", POS_KEY, options2).getError().isEmpty());
    }

    @Test
    public void testPositionalParametersArity() {
        assertTrue(getCli("", POS_KEY).getError().isEmpty());
        assertTrue(getCli("nf1", POS_KEY).getError().isEmpty());
        assertTrue(getCli("nf1 nf2 nf3 nf4 nf5 nf6 nf7 nf8 nf9", POS_KEY).getError().isEmpty());
    }

    @Test
    public void testPositionalParametersKeyAsOptionParameter() {
        var cli = getCli("--one " + POS_KEY + " nf1", POS_KEY, optOne);
        assertEquals(POS_KEY, cli.get("--one"));
        assertEquals("nf1", cli.get(POS_KEY));
    }

    @Test
    public void testArityZeroValidation() {
        assertTrue(getCli("--zero", optZero).getError().isEmpty());
        assertEquals("Expected 0 args: --zero=b1", getCli("--zero=b1", optZero).getError().get());
        assertEquals("Expected 0 args: --zero=b1,b2", getCli("--zero=b1,b2", optZero).getError().get());
        assertEquals("Bad option: b1", getCli("--zero b1", optZero).getError().get());
        assertEquals("Bad option: b1 b2", getCli("--zero b1 --zero b2", optZero).getError().get());

        var cli = getCli("--zero b1 --zero b2", POS_KEY, optZero);
        assertTrue(cli.getError().isEmpty());
        assertEquals(List.of("b1", "b2"), cli.getAll(POS_KEY));
        assertTrue(cli.has("--zero"));
    }

    @Test
    public void testArityOneValidation() {
        assertEquals("Expected 1 arg: --one", getCli("--one", optOne).getError().get());
        assertTrue(getCli("--one=v1", optOne).getError().isEmpty());
        // only split comma to list for Arity.MANY
        assertTrue(getCli("--one=v1,v2", optOne).getError().isEmpty());
        assertEquals("Expected 1 arg: --one=v1,v2", getCli("--one v1 --one v2", optOne).getError().get());
    }

    @Test
    public void testArityZeroOneValidation() {
        assertTrue(getCli("--zero-one", optZeroOne).getError().isEmpty());
        assertTrue(getCli("--zero-one=v1", optZeroOne).getError().isEmpty());
        // only split comma to list for Arity.MANY
        assertTrue(getCli("--zero-one=v1,v2", optZeroOne).getError().isEmpty());
        assertEquals("Expected 0 or 1 args: --zero-one=v1,v2", getCli("--zero-one v1 --zero-one v2", optZeroOne).getError().get());
    }

    @Test
    public void testZero() {
        assertFalse(getCli("", optZero).has("--zero"));
        assertTrue(getCli("--zero", optZero).has("--zero"));
        assertTrue(getCli("-z", optZero).has("--zero"));

        assertNull(getCli("--zero", optZero).get("--zero"));
        assertEquals("default", getCli("--zero", optZero).get("--zero", "default"));
        assertEquals(List.of(), getCli("--zero", optZero).getAll("--zero"));
        assertEquals(List.of("default"), getCli("--zero", optZero).getAll("--zero", "default"));
    }

    @Test
    public void testZeroOne() {
        assertFalse(getCli("", optZeroOne).has("--zero-one"));
        assertTrue(getCli("--zero-one", optZeroOne).has("--zero-one"));
        assertTrue(getCli("-x", optZeroOne).has("--zero-one"));
        assertEquals("val", getCli("--zero-one=val", optZeroOne).get("--zero-one"));
        assertEquals("val", getCli("--zero-one val", optZeroOne).get("--zero-one"));
        assertEquals("val", getCli("-x=val", optZeroOne).get("--zero-one"));
        assertEquals("val", getCli("-x val", optZeroOne).get("--zero-one"));
        assertEquals("val", getCli("-xval", optZeroOne).get("--zero-one"));

        assertNull(getCli("", optZeroOne).get("--zero-one"));
        assertEquals("default", getCli("", optZeroOne).get("--zero-one", "default"));
        assertTrue(getCli("--zero-one=val").has("--zero-one"));
        assertEquals(List.of("val"), getCli("--zero-one=val", optZeroOne).getAll("--zero-one"));
        assertEquals(List.of("default"), getCli("--zero-one", optZeroOne).getAll("--zero-one", "default"));

        // optional option will not consume positional parameter
        var cliPos = getCli("--zero-one val", POS_KEY, optZeroOne);
        assertTrue(cliPos.has("--zero-one"));
        assertEquals(List.of("val"), cliPos.getAll(POS_KEY));

        // optional option will not consume subcommands
        var cliSub = getCli("--zero-one SUB_KEY1 sub", SUB_KEYS, optZeroOne);
        assertTrue(cliSub.has("--zero-one"));
        assertEquals(List.of("sub"), cliSub.getAll("SUB_KEY1"));
    }

    @Test
    public void testOne() {
        assertFalse(getCli("", optOne).has("--one"));
        assertEquals("val", getCli("--one=val", optOne).get("--one"));
        assertEquals("val", getCli("--one val", optOne).get("--one"));
        assertEquals("val", getCli("-o=val", optOne).get("--one"));
        assertEquals("val", getCli("-o val", optOne).get("--one"));
        assertEquals("val", getCli("-oval", optOne).get("--one"));

        assertEquals("--key=val,val", getCli("--one=--key=val,val", optOne).get("--one"));
        assertEquals("key=val,val", getCli("--one key=val,val", optOne).get("--one"));
        assertEquals("--key=val,val", getCli("-o=--key=val,val", optOne).get("--one"));
        assertEquals("key=val,val", getCli("-o key=val,val", optOne).get("--one"));
        assertEquals("--key=val,val", getCli("-o--key=val,val", optOne).get("--one"));

        assertNull(getCli("", optOne).get("--one"));
        assertEquals("default", getCli("", optOne).get("--one", "default"));
        assertTrue(getCli("--one=val").has("--one"));
        assertEquals(List.of("val"), getCli("--one=val", optOne).getAll("--one"));
        assertEquals(List.of("val"), getCli("--one val", optOne).getAll("--one", "default"));
    }

    @Test
    public void testMany() {
        assertFalse(getCli("", optOne).has("--many"));
        assertEquals(List.of("m1", "m2", "m3"), getCli("--many=m1,m2,m3", optMany).getAll("--many"));
        assertEquals(List.of("m1", "m2", "m3"), getCli("--many=m1 --many=m2 --many=m3", optMany).getAll("--many"));
        assertEquals(List.of("m1", "m2", "m3"), getCli("--many m1 --many m2 --many m3", optMany).getAll("--many"));
        assertEquals(List.of("m1", "m2", "m3"), getCli("--many m1 --many=m2,m3", optMany).getAll("--many"));

        assertEquals(List.of("m1", "m2", "m3"), getCli("-m=m1,m2,m3", optMany).getAll("--many"));
        assertEquals(List.of("m1", "m2", "m3"), getCli("-m m1 -m m2 -m m3", optMany).getAll("--many"));
        assertEquals(List.of("m1", "m2", "m3"), getCli("-mm1 -mm2 -mm3", optMany).getAll("--many"));
        assertEquals(List.of("m1", "m2", "m3"), getCli("-m=m1 -m m2 -mm3", optMany).getAll("--many"));
        assertEquals(List.of("m1", "m2", "m3"), getCli("-m m1 --many=m2,m3", optMany).getAll("--many"));

        // preserve comma if no equals sign
        assertEquals(List.of("1,024", "2,048"), getCli("--many 1,024 --many 2,048", optMany).getAll("--many"));

        assertEquals("m1", getCli("--many=m1,m2,m3", optMany).get("--many"));
        assertEquals(List.of(), getCli("", optMany).getAll("--many"));
        assertEquals(List.of("m4", "m5"), getCli("", optMany).getAll("--many", "m4", "m5"));
    }

    @Test
    public void testShortOpts() {
        Cli.Option[] options = { optZero, optZero2, optOne };
        var cli = getCli("-z -Z -o val", options);
        assertTrue(cli.has("--zero") && cli.has("--zero2") && cli.get("--one").equals("val"));
        cli = getCli("-zZo val", options);
        assertTrue(cli.has("--zero") && cli.has("--zero2") && cli.get("--one").equals("val"));
        cli = getCli("-Zzo=val", options);
        assertTrue(cli.has("--zero") && cli.has("--zero2") && cli.get("--one").equals("val"));
        cli = getCli("-zoZ", options);
        assertTrue(cli.has("--zero") && !cli.has("--zero2") && cli.get("--one").equals("Z"));

        cli = getCli("-z -R -o val", options);
        assertEquals("Bad option: -R", cli.getError().get());
        cli = getCli("-zZRoval", options);
        assertEquals("Bad option: -R", cli.getError().get());
        cli = getCli("-z?o val", options);
        assertEquals("Bad option: -?", cli.getError().get());
        cli = getCli("-zZ=val", options);
        assertEquals("Expected 0 args: --zero2=val", cli.getError().get());
        cli = getCli("-zZ val", options);
        assertEquals("Bad option: val", cli.getError().get());
        cli = getCli("-zZval", options);
        assertEquals("Bad option: -v", cli.getError().get());

        Cli.Option[] optionsWithPositionals = { optZero, optZero2, optOne };
        assertTrue(getCli("-zZ val", POS_KEY, optionsWithPositionals).getError().isEmpty());
    }

    @Test
    public void testRegex() {
        Cli.Option[] options = {
                new Cli.Option(' ', "--timeout", Cli.Arity.ONE, "", "\\d+", "Not a number"),
                new Cli.Option(' ', "--id", Cli.Arity.MANY, "", "[A-Z]\\d{4}", "Id must be letter + 4 digits"),
                new Cli.Option(' ', "--log-level", Cli.Arity.ONE, "", "help|warn|error"),  // no error message
        };
        assertEquals("123", getCli("--timeout=123", options).get("--timeout"));
        assertEquals("Not a number: --timeout=five", getCli("--timeout=five", options).getError().get());
        assertEquals(List.of("R0047", "X2913"), getCli("--id=R0047,X2913", options).getAll("--id"));
        assertEquals("Id must be letter + 4 digits: --id=A0001,39K", getCli("--id=A0001,39K", options).getError().get());
        assertEquals("warn", getCli("--log-level=warn", options).get("--log-level"));
        assertEquals("help|warn|error: --log-level=fail", getCli("--log-level fail", options).getError().get());
    }

    @Test
    public void testEndOfOptionsDelimiter() {
        Cli.Option[] options = { optOne, optZero };
        assertEquals(List.of("--zero", "--one", "val"),
                getCli("--zero --one val -- --zero --one val", POS_KEY, options).getAll(POS_KEY));
        assertEquals(List.of("aa", "bb", "cc"),
                getCli("--zero aa --one val bb -- cc", POS_KEY, options).getAll(POS_KEY));
        assertEquals(List.of("aa", "bb", "cc"),
                getCli("aa bb cc --", POS_KEY, options).getAll(POS_KEY));
        assertEquals(List.of("aa", "bb", "cc", "--"),
                getCli("-- aa bb cc -- ", POS_KEY, options).getAll(POS_KEY));
    }

    @Test
    public void testOptionLikeParameters() {
        Cli.Option[] options = { optOne, optZero };
        assertEquals("-", getCli("--one -", POS_KEY, options).get("--one"));
        assertEquals("-", getCli("--one=-", POS_KEY, options).get("--one"));
        assertEquals("-", getCli("-o -", POS_KEY, options).get("--one"));
        assertEquals("-", getCli("-o=-", POS_KEY, options).get("--one"));
        assertEquals("-", getCli("-o-", POS_KEY, options).get("--one"));
        assertEquals("--zero", getCli("--one=--zero", POS_KEY, options).get("--one"));
        assertEquals("--zero", getCli("-o=--zero", POS_KEY, options).get("--one"));
        assertEquals("--zero", getCli("-o--zero", POS_KEY, options).get("--one"));
        assertEquals("-5", getCli("--one=-5", POS_KEY, options).get("--one"));
        assertEquals("-5", getCli("-o=-5", POS_KEY, options).get("--one"));
        assertEquals("-5", getCli("-o-5", POS_KEY, options).get("--one"));
        assertEquals("--", getCli("--one=--", POS_KEY, options).get("--one"));
        assertEquals("--", getCli("-o=--", POS_KEY, options).get("--one"));
        assertEquals("--", getCli("-o--", POS_KEY, options).get("--one"));
    }

    @Test
    public void testIgnoreLongOptionNameHelpParams() {
        Cli.Option[] options = {
                new Cli.Option('o', "--opacity <percent>", Cli.Arity.ONE, "Opacity in percent"),
                new Cli.Option(' ', "--skip=EXT,...", Cli.Arity.MANY, "Skip these file extensions"),
                new Cli.Option('b', "--backup=[<ext>]", Cli.Arity.ZERO_ONE, "Make backup files"),
        };
        var cli = getCli("--opacity 50 --skip jpg --backup=.old", POS_KEY, options);
        assertEquals("50", cli.get("--opacity"));
        assertEquals("jpg", cli.get("--skip"));
        assertEquals(".old", cli.get("--backup"));
    }

    @Test
    public void testHelp() {
        Cli.Option[] options = {
                new Cli.Option('o', "--opacity=<percent>", Cli.Arity.ONE, "Opacity in percent", "\\d\\d?|100", "Must be 0-100"),
                new Cli.Option(' ', "--skip=<ext>,...", Cli.Arity.MANY, "Skip these file extensions"),
                new Cli.Option('b', "--backup[=<ext>]", Cli.Arity.ZERO_ONE, "Make backup files, default extension .bak"),
                new Cli.Option('p', "", Cli.Arity.ZERO, "Use parallel execution"),
                new Cli.Option('v', "-v, -vv, -vvv", Cli.Arity.ZERO_ONE, "Verbosity"),
                new Cli.Option('h', "--help", Cli.Arity.ZERO, "Show this help")
        };
        var cli = getCli("--help", POS_KEY, options);
        var testOut = new ByteArrayOutputStream();
        System.setOut(new PrintStream(testOut));
        cli.printHelp();
        var stdOut = testOut.toString();
        assertEquals("""
                Helptext
                Options:
                  -o, --opacity=<percent>  Opacity in percent
                      --skip=<ext>,...     Skip these file extensions
                  -b, --backup[=<ext>]     Make backup files, default extension .bak
                  -p                       Use parallel execution
                  -v, -vv, -vvv            Verbosity
                  -h, --help               Show this help
                                
                """, stdOut);

        cli = getCli("--opacity 128", POS_KEY, options);
        testOut = new ByteArrayOutputStream();
        System.setErr(new PrintStream(testOut));
        cli.printError(cli.getError().get());
        var stdErr = testOut.toString();
        assertEquals("""
                Must be 0-100: --opacity=128
                Try --help for more
                """, stdErr);
    }

    @Test
    public void testSubcommand() {
        var cliStart = getCli("--zero start file.txt --one startsub", Set.of("start", "stop"), optZero);
        assertTrue(cliStart.has("--zero"));
        assertFalse(cliStart.has("--one"));
        assertEquals(List.of("file.txt", "--one", "startsub"), cliStart.getAll("start"));

        var subCli = getCli(String.join(" ", cliStart.getAll("start")), "subFiles", optOne);
        assertFalse(subCli.has("--zero"));
        assertTrue(subCli.has("--one"));
        assertEquals("startsub", subCli.get("--one"));
        assertEquals(List.of("file.txt"), subCli.getAll("subFiles"));

        var cliStop = getCli("--zero stop --one stopsub", Set.of("start", "stop"), optZero);
        assertEquals(List.of("--one", "stopsub"), cliStop.getAll("stop"));

        var cliZeroOne = getCli("--zero-one start file.txt --one startsub", Set.of("start", "stop"), optZeroOne);
        assertTrue(cliZeroOne.has("--zero-one"));
        assertEquals(List.of("file.txt", "--one", "startsub"), cliZeroOne.getAll("start"));

        var cliOne = getCli("--one start file.txt --one startsub", Set.of("start", "stop"), optOne);
        assertEquals("Expected 1 arg: --one", cliOne.getError().get());

        var cliRestart = getCli("--zero restart --one restartsub", Set.of("start", "stop"), optZero);
        assertEquals("Bad option: restart restartsub", cliRestart.getError().get());
    }
}