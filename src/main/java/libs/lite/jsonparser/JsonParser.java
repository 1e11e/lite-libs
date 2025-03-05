package libs.lite.jsonparser;

import java.util.*;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/*
 * Lite Libs tiny JSON parser
 */
class JsonParser {
    private final Object liteLibsJsonObject;
    private final LinkedList<String> tokens;
    private final String QUOTE = "\"(?:\\\\\"|.)*?\"", COMPLEX = "[\\[\\]{},:]", PRIMITIVE = "[^\\[\\]{},:\\s]+";
    private final Map<String, ?> mapObj = Map.of("}", Map.of(), "]", List.of(), "true", true, "false", false);
    public JsonParser(String jsonString) {
        tokens = Pattern.compile(QUOTE + "|" + COMPLEX + "|" + PRIMITIVE).matcher(jsonString != null ? jsonString : "")
                .results().map(MatchResult::group).collect(Collectors.toCollection(LinkedList::new));
        try {
            liteLibsJsonObject = parseJson(tokens.pop());
        } catch (RuntimeException e) {
            throw new MatchException("Json error " + e.getMessage() + (tokens.isEmpty() ? " at end" : tokens.pop()), e);
        }
        if (!tokens.isEmpty())
            throw new RuntimeException("Expected end of json, found: %.40s".formatted(String.join("", tokens)));
    }
    @SuppressWarnings("unchecked")
    public <T> T get(Object... keysAndIndexes) {
        return (T) Stream.of(keysAndIndexes).reduce(liteLibsJsonObject, (jsonObj, key) ->
                key instanceof Integer index ? ((List<?>) jsonObj).get(index) : ((Map<?, ?>) jsonObj).get(key));
    }
    private Object parseJson(String token) {
        return switch (token) {
            case "{" -> "}".equals(tokens.peek()) ? mapObj.get(tokens.pop()) : getJsonMap(new LinkedHashMap<>());
            case "[" -> "]".equals(tokens.peek()) ? mapObj.get(tokens.pop()) : getJsonList(new ArrayList<>());
            case "true", "false", "null" -> mapObj.getOrDefault(token, null);
            case String s when s.matches(QUOTE) -> token.substring(1, token.length() - 1).translateEscapes();
            case String s when s.matches("[-+]?[0-9]+") -> Integer.parseInt(token);
            default -> Double.parseDouble(token.matches("[+-]?(Infinity|NaN)") ? token.toLowerCase() : token);
        };
    }
    private Map<String, Object> getJsonMap(Map<String, Object> map) {
        do {
            String key = tokens.pop(), colon = tokens.pop(), val = tokens.pop(), next = String.valueOf(tokens.peek());
            if (!key.matches(QUOTE) || !colon.matches(":") || !val.matches("[\\[{]") && !next.matches("[},]"))
                throw new RuntimeException("in map " + key + colon + val);
            map.put(key.substring(1, key.length() - 1), parseJson(val));
        } while (!tokens.pop().equals("}"));
        return map;
    }
    private List<Object> getJsonList(List<Object> list) {
        do {
            list.add(tokens.isEmpty() ? "[" : parseJson(tokens.pop()));
            if (tokens.isEmpty() || !tokens.peek().matches("[],]"))
                throw new RuntimeException("in list near " + list.getLast());
        } while (!tokens.pop().equals("]"));
        return list;
    }
}
