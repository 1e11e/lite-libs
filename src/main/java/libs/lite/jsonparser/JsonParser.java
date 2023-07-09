package libs.lite.jsonparser;

import java.util.*;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/*
 * Tiny JSON parser. The JSON string is parsed into an Object of Map/List/String/Integer/Double/Boolean and null.
 * A convenience getter will do the type casting. Sanity checks will detect if the JSON is invalid.
 *
 * Example:
 *     var jp = new JsonParser("""
 *         {
 *             "key": "value",
 *             "ary": [
 *                 { "id": 1, "name": "one", "lock": true },
 *                 { "id": 2, "name": null }
 *             ]
 *         }
 *         """);
 *
 * The getter takes any number of keys and indexes to navigate into the maps and lists of the structure:
 *     jp.get()                                -->  {key=value, ary=[{id=1, name=one, lock=true}, {id=2, name=null}]}
 *     jp.get("key")                           -->  "value"
 *     jp.get("ary", 0, "id")                  -->  1
 *
 * Type casting can be done in three ways:
 *     String key = jp.get("key")
 *     key.toUpperCase()                       -->  "VALUE"  (automatic)
 *     jp.<String>get("key").toUpperCase()     -->  "VALUE"  (type witness)
 *     ((String) jp.get("key")).toUpperCase()  -->  "VALUE"  (ugly manual)
 *
 * The entire tree of interest can be typed ahead to avoid further casting. Note the ? for Integer or String:
 *     jp.<List<Map<String, ?>>>get("ary").forEach(x -> System.out.println(x.get("id") + " is " + x.get("name")));
 *     -->  1 is one
 *     -->  2 is null                                        (see more examples in the unit tests)
 *
 * Null can have different meanings:
 *     jp.<String> get("ary", 1, "name")       -->  null     (defined as null)
 *     jp.<Boolean>get("ary", 1, "lock")       -->  null     (does not exist)
 *
 * Any JSON object can be top level:
 *     new JsonParser(".4e2").<Double>get()    --> 40.0
 *
 * The class is 59 lines and golfed to the edge of readability, ready to be pasted into a java shell script.
 * If you can trust the input to be error free you can remove all the if-throws and try-catch to shave 11 more lines.
 */
public class JsonParser {
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
