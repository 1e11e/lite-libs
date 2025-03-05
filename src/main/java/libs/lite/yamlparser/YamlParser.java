package libs.lite.yamlparser;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/*
 * Lite Libs tiny YAML parser
 */
class YamlParser {
    enum Yml {
        QUOTE("\"(?:\\\\\"|.)*?\"|'(?:\\\\'|.)*?'(?: |$)"), BLOCK("[>|][+-]?\\d? *(?:#.*)?\n ( +).+?\n(?: \\1.+?\n)*"),
        FLOW("[\\[{].*?\n ( +).+,\n(?: \\2.+,?\n)*(?: +[]}]\n)?"), DOC("^ (?:%.*|---|\\.\\.\\.)(?: |$)"), TAG("!\\S*"),
        ANCHOR("&\\S+"), COMMENT(" *#.*$"), SEQ("-(?: |$)"), BOOL("(?:true|false)(?: |$)"), NULL("(?:null|~)(?: |$)"),
        EMPTY("\\[]|\\{}"), INT("[-+]?\\d+(?: |$)"), TAB("^ +"), KEY("[^\\[{:\n]+:(?: |$)"), VAL("\\S[^\n]*?(?= #|$)");
        final String regex;
        Yml(String regex) { this.regex = regex; }
    }
    private final Object liteLibsYamlObject;
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
        liteLibsYamlObject = parseYaml();
        if (!tokens.isEmpty())
            throw new RuntimeException("Expected end of yaml, found: %.40s".formatted(tokens));
    }

    @SuppressWarnings("unchecked")
    public <T> T get(Object... keysAndIndexes) {
        return (T) Stream.of(keysAndIndexes).reduce(liteLibsYamlObject, (yamlObj, key) ->
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
            return str.matches("[-+.0-9eE]+") ? Double.valueOf(str) : str;
        } catch (NumberFormatException nfe) {
            return str;
        }
    }
}
