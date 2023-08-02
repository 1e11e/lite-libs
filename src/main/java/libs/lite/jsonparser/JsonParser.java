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
    private final Object jsonObject;
    private final LinkedList<String> tokens;
    private final String QUOTE = "\"(?:\\\\\"|.)*?\"", COMPLEX = "[\\[\\]{},:]", PRIMITIVE = "[^\\[\\]{},:\\s]+";
    private final Map<String, ?> mapObj = Map.of("}", Map.of(), "]", List.of(), "true", true, "false", false);

    public JsonParser(String jsonString) {
        tokens = Pattern.compile(QUOTE + "|" + COMPLEX + "|" + PRIMITIVE).matcher(jsonString != null ? jsonString : "")
                .results().map(MatchResult::group).collect(Collectors.toCollection(LinkedList::new));
        try {
            jsonObject = parseJson(tokens.isEmpty() && jsonString != null ? "" : tokens.pop());
        } catch (RuntimeException e) {
            throw new RuntimeException(e.getMessage() + " near " + (tokens.isEmpty() ? "end" : tokens.pop()), e);
        }
        if (!tokens.isEmpty())
            throw new RuntimeException("Expected end of json, found: %.40s".formatted(String.join(" ", tokens)));
    }

    @SuppressWarnings("unchecked")
    public <T> T get(Object... keysAndIndexes) {
        return (T) Stream.of(keysAndIndexes).reduce(jsonObject, (jsonObj, key) ->
                key instanceof Integer index ? ((List<?>) jsonObj).get(index) : ((Map<?, ?>) jsonObj).get(key));
    }

    private Object parseJson(String token) {
        return switch (token) {
            case "{" -> "}".equals(tokens.peek()) ? mapObj.get(tokens.pop()) : getJsonMap(new LinkedHashMap<>());
            case "[" -> "]".equals(tokens.peek()) ? mapObj.get(tokens.pop()) : getJsonList(new ArrayList<>());
            case "true", "false", "null" -> mapObj.getOrDefault(token, null);
            default -> {
                if (token.matches(QUOTE))
                    yield token.substring(1, token.length() - 1).translateEscapes();
                else if (token.matches("[-+]?[0-9]+"))
                    yield Integer.parseInt(token);
                yield Double.parseDouble(token.matches("[+-]?(Infinity|NaN)") ? token.toLowerCase() : token);
            }
        };
    }

    private Map<String, Object> getJsonMap(Map<String, Object> map) {
        do {
            String key = tokens.poll(), colon = tokens.poll(), value = tokens.poll();
            if (tokens.isEmpty() || !key.matches(QUOTE) || !colon.equals(":") ||
                    !tokens.peek().matches("[},]") && !value.matches("[\\[{]"))
                throw new IllegalStateException("Map error after " + (key != null ? key : "{"));
            map.put(key.substring(1, key.length() - 1), parseJson(value));
        } while (!tokens.pop().equals("}"));
        return map;
    }

    private List<Object> getJsonList(List<Object> list) {
        do {
            list.add(tokens.isEmpty() ? "[" : parseJson(tokens.pop()));
            if (tokens.isEmpty() || !tokens.peek().matches("[],]"))
                throw new IllegalStateException("List error after %.40s".formatted(list.get(list.size() - 1)));
        } while (!tokens.pop().equals("]"));
        return list;
    }
}
