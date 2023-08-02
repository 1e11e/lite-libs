package libs.lite.cli;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/*
 * Lite Libs tiny CLI parser
 */
class Cli {
    public enum Arity { ZERO, ONE, ZERO_ONE, MANY }
    public record Option(char shortName, String name, Arity arity, String help, String... regex) {
        public String getId() {
            return name.matches(LONG_OPT) ? name.split("[^\\w-]", 2)[0] : shortName != ' ' ? "-" + shortName : name;
        }
    }
    private static final String NO_KEY = "", ANY_OPT = "--?\\w.*", LONG_OPT = "--\\w.*";
    private final String nonOptKey, SPLIT = ",", END_OPTS = "--", SHORT_OPT = "-\\w", CLUSTER_SHORT_OPTS = "-\\w.+";
    private final Option OPT_ZERO = new Option(' ', null, Arity.ZERO, null),
            OPT_LITE_LIBS_HELP = new Option('h', "--help", Arity.ZERO, "Show this help");
    private final Map<String, List<String>> args = new LinkedHashMap<>();
    private final Map<String, Option> opts = new LinkedHashMap<>();
    private final Function<String, Stream<String>> splitClusterOpts = cluster -> Stream.of(cluster.substring(1).split(
            "", IntStream.range(1, cluster.length()).filter(i -> opts.getOrDefault("-" + cluster.charAt(i), OPT_ZERO)
                            .arity != Arity.ZERO || (cluster + " ").charAt(i + 1) == '=').findFirst().orElse(0)))
            .map(c -> c.length() == 1 ? "-" + c : "-" + c.charAt(0) + "=" + c.substring(c.charAt(1) == '=' ? 2 : 1));

    public Cli(List<String> argv, String help, Option... optv) { this(argv, help, false, NO_KEY, optv); }
    public Cli(List<String> argv, String help, String posKey, Option... optv) { this(argv, help, false, posKey, optv); }
    public Cli(List<String> argv, String help, Set<String> subcommandKeys, Option... optv) {
        this(argv, help, true, argv.stream().filter(subcommandKeys::contains).findFirst().orElse(NO_KEY), optv);
    }
    private Cli(List<String> argv, String help, boolean isSubcommand, String nonOptKey, Option... optv) {
        opts.put(this.nonOptKey = nonOptKey, new Option(' ', nonOptKey, Arity.MANY, help));
        Stream.concat(Stream.of(optv), Stream.of(OPT_LITE_LIBS_HELP)).forEach(opt -> opts.putIfAbsent(
                opt.shortName != ' ' ? "-" + opt.shortName : opt.getId(), opts.computeIfAbsent(opt.getId(), k -> opt)));
        argv.stream().takeWhile(arg -> isSubcommand ? !arg.equals(nonOptKey) : !arg.equals(END_OPTS))
                .flatMap(arg -> arg.matches(CLUSTER_SHORT_OPTS) ? splitClusterOpts.apply(arg) : Stream.of(arg))
                .reduce(nonOptKey, (prevOpt, arg) -> {
                    var pair = arg.matches(ANY_OPT) && arg.contains("=") ? arg.split("=", 2) : new String[] { arg, "" };
                    String key = opts.containsKey(pair[0]) ? opts.get(pair[0]).getId() : pair[0], val = pair[1];
                    if (key.matches(ANY_OPT))
                        args.computeIfAbsent(key, k -> new ArrayList<>()).addAll(val.isEmpty() ? List.of() : Arrays
                                .asList(val.split(opts.getOrDefault(key, OPT_ZERO).arity == Arity.MANY ? SPLIT : "$")));
                    else
                        args.computeIfAbsent(opts.getOrDefault(prevOpt, OPT_ZERO).arity == Arity.ZERO_ONE ? (nonOptKey
                                .equals(NO_KEY) ? prevOpt : nonOptKey) : prevOpt, k -> new ArrayList<>()).add(key);
                    return opts.getOrDefault(key, OPT_ZERO).arity != Arity.ZERO && val.isEmpty() ? key : nonOptKey;
                });
        argv.stream().dropWhile(arg -> isSubcommand ? !arg.equals(nonOptKey) : !arg.equals(END_OPTS))
                .skip(1).forEach(arg -> args.computeIfAbsent(nonOptKey, v -> new ArrayList<>()).add(arg));
        getError().ifPresent(error -> System.exit(error.isEmpty() ? printHelp() : printError(error)));
    }

    public Optional<String> getError() {
        return args.keySet().stream().map(key -> validate(key, opts.get(key))).filter(Objects::nonNull).findFirst();
    }

    private String validate(String key, Option opt) {
        String name = key.equals(nonOptKey) ? "" : key, params = String.join(name.isEmpty() ? " " : SPLIT, getAll(key));
        String equals = name.isEmpty() || getAll(key).isEmpty() ? "" : key.matches(SHORT_OPT) ? " " : "=";
        int size = getAll(key).size();
        if (opt == null || key.equals(NO_KEY) && !args.getOrDefault(NO_KEY, List.of()).isEmpty())
            return "Bad option: %s%s%s".formatted(name, equals, params);
        else if (List.of(size != 0, size != 1, size > 1, size == 0).get(opt.arity.ordinal()))
            return "Expected %s: %s%s%s".formatted(List.of("0 args", "1 arg", "0 or 1 args", "1 or more args")
                    .get(opt.arity.ordinal()), name, equals, params);
        else if (opt.regex.length > 0 && !getAll(key).stream().allMatch(arg -> arg.matches("(?i)" + opt.regex[0])))
            return "%s: %s%s%s".formatted(opt.regex[opt.regex.length - 1], name, equals, params);
        return key.equals(OPT_LITE_LIBS_HELP.getId()) ? "" : null;
    }

    public int printHelp() {
        int maxWidth = opts.values().stream().mapToInt(opt -> opt.name.length()).max().orElse(30) + 4;
        return System.out.printf("%s%nOptions:%n%s%n%n", opts.get(nonOptKey).help,
                opts.values().stream().distinct().skip(1).map(opt -> ("  %-" + maxWidth + "s  %s").formatted(
                        opt.name.matches(LONG_OPT) ? (opt.shortName != ' ' ? "-" + opt.shortName + ", " : "    ") +
                                opt.name : (opt.name.isBlank() ? "-" + opt.shortName + "  " : opt.name + "    "),
                        opt.help)).collect(Collectors.joining("\n"))).hashCode() & 0x100;
    }

    public int printError(String error) { return System.err.printf("%s%nTry --help for more%n", error).hashCode() | 1; }
    public boolean has(String key) { return args.containsKey(key); }
    public String get(String key, String... def) { return getAll(key, def).isEmpty() ? null : getAll(key, def).get(0); }
    public List<String> getAll(String key, String... defaults) {
        return args.getOrDefault(key, List.of()).isEmpty() ? Arrays.asList(defaults) : args.get(key);
    }
}
