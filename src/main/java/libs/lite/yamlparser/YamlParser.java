package libs.lite.yamlparser;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/*
 * Tiny YAML parser. The YAML string is parsed into an Object of Map/List/String/Integer/Double/Boolean and null.
 * A convenience getter will do the type casting. Basic sanity checks will detect if the YAML is very invalid.
 *
 * Example:
 *    var yp = new YamlParser("""
 *        key: "value"        # quoted string
 *        seq:
 *          - id: 1           # integer
 *            name: one       # string
 *            lock: true      # boolean
 *          - id: 2
 *            name:           # no value means null, can also use null or ~
 *        """);
 *
 * The getter takes any number of keys and indexes to navigate into the maps and lists of the structure:
 *     yp.get()                                -->  {key=value, ary=[{id=1, name=one, lock=true}, {id=2, name=null}]}
 *     yp.get("key")                           -->  "value"
 *     yp.get("seq", 0, "id")                  -->  1
 *
 * Type casting can be done in three ways:
 *     String key = yp.get("key")
 *     key.toUpperCase()                       -->  "VALUE"  (automatic)
 *     yp.<String>get("key").toUpperCase()     -->  "VALUE"  (type witness)
 *     ((String) yp.get("key")).toUpperCase()  -->  "VALUE"  (ugly manual)
 *
 * The entire tree of interest can be typed ahead to avoid further casting. Note the ? for Integer or String:
 *     yp.<List<Map<String, ?>>>get("seq").forEach(x -> System.out.println(x.get("id") + " is " + x.get("name")));
 *     -->  1 is one
 *     -->  2 is null                                        (see more examples in the unit tests)
 *
 * Null can have different meanings:
 *     yp.<String> get("seq", 1, "name")       -->  null     (defined as null)
 *     yp.<Boolean>get("seq", 1, "lock")       -->  null     (does not exist)
 *
 * Any YAML object can be top level:
 *     new YamlParser(".4e2").<Double>get()    --> 40.0
 *
 * YAML is a format with (too?) many degrees of freedom, so to keep the parser small there are some compromises and
 * bending of the specs. It can only handle a subset of YAML features similar to the level of complexity found in JSON.
 * The following features have limited or no support:
 * - !!tags and !tags are ignored.
 * - &anchors are ignored and *aliases are just a value.
 * - << override is not supported and will just be a key "<<" with the value *alias.
 * - Indentation indicators in block |n and fold >n are ignored (but block/fold strings are supported, with chomp char).
 * - Different TRUE/Yes/Y/ON for boolean are just strings. Only true/false/null are supported.
 * - Hexadecimal, octal, .inf and .NaN etc are just strings.
 * - Flow collections will return as a string, except empty list [] and empty map {} which are supported.
 * - Document separators (--- and ...) are ignored. All keys will be merged (and potentially overwritten).
 * - ? complex mapping keys will not work.
 * - Multiline strings without block | or fold > will not work (but multiline block and fold strings are supported).
 * With the above caveats, it parses ~25 of the 28 examples in the YAML 1.2 specification.
 *
 * The class is 99 lines and golfed to the edge of readability, ready to be pasted into a java shell script.
 * The brittle regexps makes it exciting to change anything. Run the unit tests often.
 */
public class YamlParser {
    enum Yml {
        QUOTE("\"(?:\\\\\"|.)*?\"|'(?:\\\\'|.)*?'(?: |$)"), BLOCK("[>|][+-]?\\d? *(?:#.*)?\n ( +).+?\n(?: \\1.+?\n)*"),
        FLOW("[\\[{].*?\n ( +).+,\n(?: \\2.+,?\n)*(?: +[]}]\n)?"), DOC("^ (?:%.*|---|\\.\\.\\.)(?: |$)"), TAG("!\\S*"),
        ANCHOR("&\\S+"), COMMENT(" *#.*$"), SEQ("-(?: |$)"), BOOL("(?:true|false)(?: |$)"), NULL("(?:null|~)(?: |$)"),
        EMPTY("\\[]|\\{}"), INT("[-+]?\\d+(?: |$)"), TAB("^ +"), KEY("[^\\[{:\n]+:(?: |$)"), VAL("\\S[^\n]*?(?= #|$)");
        final String regex;
        Yml(String regex) { this.regex = regex; }
    }
    private final Object yamlObject;
    private final LinkedList<Token> tokens;
    private final Map<String, Object> mapObj = Map.of("true", true, "false", false, "{}", Map.of(), "[]", List.of());
    private final LinkedList<Integer> indentStack = new LinkedList<>(List.of(0));
    private record Token(Yml yml, String val) { public String toString() { return yml.name() + "=" + val; } }

    public YamlParser(String yamlString) {
        var pattern = Pattern.compile("(?m)" + String.join("|", Stream.of(Yml.values()).map(y -> y.regex).toList()));
        tokens = pattern.matcher(" " + yamlString.replaceAll("\n\\s*\n|\n", "\n ").trim() + "\n").results()
                .map(mr -> new Token(Stream.of(Yml.values()).filter(y -> mr.group().matches(y.regex)).findFirst()
                        .orElse(Yml.VAL), mr.group().isBlank() ? mr.group() : mr.group().trim()))
                .filter(token -> !Set.of(Yml.DOC, Yml.TAG, Yml.ANCHOR, Yml.COMMENT).contains(token.yml))
                .map(token -> token.yml == Yml.KEY ? new Token(Yml.KEY, token.val.split(":")[0].trim()) : token)
                .collect(Collectors.toCollection(LinkedList::new));
        for (int i = 1; i < tokens.size(); i++)
            switch (tokens.get(i - 1).yml + "," + tokens.get(i).yml) {
                case "SEQ,KEY", "SEQ,SEQ", "KEY,KEY" -> tokens.add(i, new Token(Yml.TAB, tokens.get(i - 2).val + " "));
                case "TAB,SEQ" -> tokens.set(i - 1, new Token(Yml.TAB, tokens.get(i - 1).val + " "));
                case "TAB,TAB" -> tokens.remove(i--);
            }
        yamlObject = parseYaml();
        if (!tokens.isEmpty())
            throw new RuntimeException("Expected end of yaml, found: %.40s".formatted(tokens));
    }

    @SuppressWarnings("unchecked")
    public <T> T get(Object... keysAndIndexes) {
        return (T) Stream.of(keysAndIndexes).reduce(yamlObject, (yamlObj, key) ->
                key instanceof Integer index ? ((List<?>) yamlObj).get(index) : ((Map<?, ?>) yamlObj).get(key));
    }

    private Object parseYaml() {
        isUndent();
        return tokens.isEmpty() ? null : switch (tokens.peek().yml) {
            case KEY -> getYamlMap(new LinkedHashMap<>());
            case SEQ -> getYamlList(new ArrayList<>());
            case BOOL, NULL, EMPTY -> mapObj.getOrDefault(tokens.pop().val, null);
            case QUOTE -> tokens.pop().val.replaceAll("^.(.*).$", "$1").replace("''", "'").translateEscapes();
            case BLOCK -> getYamlFoldOrBlock(tokens.pop().val);
            case INT -> Integer.valueOf(tokens.pop().val);
            default -> getStringOrDouble(tokens.pop().val);
        };
    }

    private boolean isUndent() {
        if (!tokens.isEmpty() && tokens.peek().yml == Yml.TAB) {
            int currIndent = tokens.peek().val.length(), prevIndent = indentStack.getFirst();
            if (currIndent <= prevIndent)
                (currIndent < prevIndent ? indentStack : tokens).pop();
            else
                indentStack.push(tokens.pop().val.length());
            return currIndent - prevIndent < 0;
        }
        return false;
    }

    private Map<String, Object> getYamlMap(Map<String, Object> map) {
        while (!tokens.isEmpty() && tokens.peek().yml == Yml.KEY) {
            String key = tokens.pop().val, next = tokens.isEmpty() ? "" : tokens.peek().val;
            map.put(key, next.isBlank() && next.length() <= indentStack.getFirst() ? null : parseYaml());
            if (isUndent())
                return map;
        }
        return map;
    }

    private List<Object> getYamlList(List<Object> list) {
        while (tokens.poll() != null)  {
            list.add(parseYaml());
            if (isUndent() || tokens.isEmpty() || tokens.peek().yml != Yml.SEQ)
                return list;
        }
        return list;
    }

    private String getYamlFoldOrBlock(String foldBlock) {
        String token = foldBlock.split("\n", 2)[0], block = foldBlock.split("\n", 2)[1].stripTrailing().stripIndent();
        return (token.startsWith("|") ? block : block.lines().map(l -> l.startsWith(" ") ? "\n" + l + "\n" : l + " ")
                .collect(Collectors.joining()).replaceAll("\n\n| \n", "\n"))
                .stripTrailing() + (token.substring(1).startsWith("-") ? "" : "\n");
    }

    private Object getStringOrDouble(String str) {
        try {
            return str.matches(".*\\d.*") ? Double.valueOf(str) : str;
        } catch (NumberFormatException nfe) {
            return str;
        }
    }
}
