package libs.lite.jsonparser;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class JsonParserTest {
    @Test
    public void testFromComment() {
        var jp = new JsonParser("""
                {
                    "key": "value",
                    "ary": [
                        { "id": 1, "name": "one", "lock": true },
                        { "id": 2, "name": null }
                    ]
                }
                """);

        System.out.println("jp.get()  -->  " + jp.get());
        assertEquals("value", jp.get("key"));
        assertEquals(2, (int) jp.get("ary", 1, "id"));

        String key = jp.get("key");
        assertEquals("VALUE", key.toUpperCase());
        assertEquals("VALUE", jp.<String>get("key").toUpperCase());
        assertEquals("VALUE", ((String) jp.get("key")).toUpperCase());

        jp.<List<Map<String, ?>>>get("ary").forEach(x -> System.out.println(x.get("id") + " is " + x.get("name")));

        assertNull(jp.<String>get("ary", 1, "name"));
        assertNull(jp.<Boolean>get("ary", 1, "lock"));
    }

    @Test
    public void testJsonParser() {
        var jsonString = """
                {
                    "artists": [
                        {
                            "name": "Beatles",
                            "albums": [
                                { "name": "Help",       "year": 1965, "top1": true },
                                { "name": "Abbey Road", "year": 1969, "top1": true }
                            ]
                        },
                        {
                            "name": "Morcheeba",
                            "albums": [
                                { "name": "Charango",     "year": 2002, "top1": false },
                                { "name": "The Antidote", "year": 2005, "top1": false }
                            ]
                        },
                        {
                            "name": "Deep Purple",
                            "albums": [
                                { "name": "Fireball", "year": 1971, "top1": true },
                                { "name": "Burn",     "year": 1974, "top1": false }
                            ]
                        }
                    ]
                }
                """;
        var jp = new JsonParser(jsonString);

        int yearOfHelp = jp.get("artists", 0, "albums", 0, "year");
        assertEquals(1965, yearOfHelp);

        List<String> allArtists = jp.<List<Map<String, String>>>get("artists")
                .stream()
                .map(artist -> artist.get("name"))
                .toList();
        assertEquals(List.of("Beatles", "Morcheeba", "Deep Purple"), allArtists);

        List<String> albumsTop1 = jp.<List<Map<String, List<Map<String, ?>>>>>get("artists")
                .stream()
                .flatMap(artist -> artist.get("albums")
                        .stream()
                        .filter(album -> (Boolean) album.get("top1")))
                .map(album -> (String) album.get("name"))
                .toList();
        assertEquals(List.of("Help", "Abbey Road", "Fireball"), albumsTop1);
    }

    @Test
    public void testList() {
        assertEquals(
                List.of("one", "two", "three"), new JsonParser("""
                ["one", "two", "three"]""").get());
    }

    @Test
    public void testMap() {
        assertEquals(
                Map.of("k1", "v1", "k2", "v2", "k3", "v3"), new JsonParser("""
                { "k1": "v1", "k2": "v2", "k3": "v3" }""").get());
    }

    @Test
    public void testMapOfLists() {
        assertEquals(
                Map.of("k1", List.of("v1", "v2"), "k2", List.of("v3", "v4")), new JsonParser("""
                { "k1": ["v1", "v2"], "k2": ["v3", "v4"] }""").get());
    }

    @Test
    public void testListOfMaps() {
        assertEquals(
                List.of(Map.of("k1", "v1"), Map.of("k1", "v2")), new JsonParser("""
                [ { "k1": "v1" }, { "k1": "v2" } ]""").get());
    }

    @Test
    public void testListOfMapsAndList() {
        assertEquals(
                List.of(Map.of("k1", "v1"), List.of("a", "b"), Map.of("k2", "v2")), new JsonParser("""
                [ { "k1": "v1" }, ["a", "b"], { "k2": "v2" } ]""").get());
    }

    @Test
    public void testEmptyLists() {
        assertEquals(List.of(), new JsonParser("[]").get());
        assertEquals(List.of(List.of(), List.of(List.of())), new JsonParser("[ [], [ [] ] ]").get());
    }

    @Test
    public void testEmptyMaps() {
        assertEquals(Map.of(), new JsonParser("{}").get());
        assertEquals(Map.of("a", Map.of(), "b", Map.of()), new JsonParser("""
                { "a": {}, "b": {} }""").get());
    }

    @Test
    public void testBoolean() {
        assertEquals(Arrays.asList(true, false, null), new JsonParser("[true, false, null]").get());
    }

    @Test
    public void testInt() {
        assertEquals(List.of(-1, 0, 0, 0, 1, 23, 78),
                new JsonParser("[-1, -0, 0, +0, +1, 23, 0078]").get());
    }

    @Test
    public void testDouble() {
        assertEquals(List.of(-.1, -.1e2, 0.0, .1, 1.2E+1, 23.40, 0.0, 1e1, 1.0, 0.05),
                new JsonParser("[-.1, -.1e2, 0.0, .1, 1.2E+1, 23.40, 123.456e-500, 1e1, +1., 00.05]").get());
    }

    @Test
    public void testString() {
        assertEquals(List.of("test string ", "with \"quotes\"", "esc \n\t \\chars", ""), new JsonParser("""
                ["test string ", "with \\"quotes\\"", "esc \\n\\t \\\\chars", ""]""").get());
    }

    @Test
    public void testDeepNest() {
        var jp = new JsonParser("""
                { "a":
                    { "b": [
                        { "c":
                            { "d": ["e", "f"] }
                        },
                        { "g":
                            { "h": ["i", "j"] }
                        }
                    ]}
                }
                """);
        assertEquals("f", jp.get("a", "b", 0, "c", "d", 1));
        assertEquals(List.of("i", "j"), jp.get("a", "b", 1, "g", "h"));
    }

    @Test
    public void testMissingKey() {
        assertNull(new JsonParser("{}").get("n/a"));
    }

    @Test
    public void testWhitespace() {
        var jp = new JsonParser("""
                    [ 
                " string " , 
                \r  
                    {
                } 
                    ,  \t     false,null 
                ]  
                 
                """);
        assertEquals(Arrays.asList(" string ", Map.of(), false, null), jp.get());
    }

    @Test
    public void testShortJson() {
        assertNull(new JsonParser("null").get());
        assertEquals("", new JsonParser("\"\"").get());
        assertEquals(true, new JsonParser("true").get());
        assertEquals(false, new JsonParser("false").get());
        assertEquals(123, (int) new JsonParser("123").get());
        assertEquals(45.6, (double) new JsonParser("45.6").get());
    }

    @Test
    public void testInvalidShortJson() {
        var e1 = assertThrows(RuntimeException.class, () -> new JsonParser(null));
        var e2 = assertThrows(RuntimeException.class, () -> new JsonParser(""));
        var e3 = assertThrows(RuntimeException.class, () -> new JsonParser(":"));
        assertEquals("Json error null at end", e1.getMessage());
        assertEquals("Json error null at end", e2.getMessage());
        assertEquals("Json error For input string: \":\" at end", e3.getMessage());
    }

    @Test
    public void testInvalidValues() {
        var e1 = assertThrows(RuntimeException.class, () -> new JsonParser("[123, unknown, 456]"));
        var e2 = assertThrows(RuntimeException.class, () -> new JsonParser("[123, \"wrong', 456]]"));
        var e3 = assertThrows(RuntimeException.class, () -> new JsonParser("[123, 8.8.8.8, 456]"));
        var e4 = assertThrows(RuntimeException.class, () -> new JsonParser("NaN"));
        var e5 = assertThrows(RuntimeException.class, () -> new JsonParser("Infinity"));
        assertEquals("Json error For input string: \"unknown\",", e1.getMessage());
        assertEquals("Json error For input string: \"\"wrong'\",", e2.getMessage());
        assertEquals("Json error multiple points,", e3.getMessage());
        assertEquals("Json error For input string: \"nan\" at end", e4.getMessage());
        assertEquals("Json error For input string: \"infinity\" at end", e5.getMessage());
    }

    @Test
    public void testInvalidMissingOpeningBracket() {
        var e1 = assertThrows(RuntimeException.class, () -> new JsonParser("1, 2, 3]"));
        var e2 = assertThrows(RuntimeException.class, () -> new JsonParser("{ \"a\": 1, \"b\": 2 } ]"));
        assertEquals("Expected end of json, found: ,2,3]", e1.getMessage());
        assertEquals("Expected end of json, found: ]", e2.getMessage());
    }

    @Test
    public void testInvalidMissingClosingBracket() {
        var e1 = assertThrows(RuntimeException.class, () -> new JsonParser("["));
        var e2 = assertThrows(RuntimeException.class, () -> new JsonParser("[1, 2, 3"));
        var e3 = assertThrows(RuntimeException.class, () -> new JsonParser("[1, [2, 3, [4, 5]]"));
        var e4 = assertThrows(RuntimeException.class, () -> new JsonParser("{ \"a\": [1, 2, \"b\": [3, 4] }"));
        assertEquals("Json error in list near [ at end", e1.getMessage());
        assertEquals("Json error in list near 3 at end", e2.getMessage());
        assertEquals("Json error in list near [2, 3, [4, 5]] at end", e3.getMessage());
        assertEquals("Json error in list near b:", e4.getMessage());
    }

    @Test
    public void testInvalidMissingOpeningBrace() {
        var e1 = assertThrows(RuntimeException.class, () -> new JsonParser("\"a\": 1, \"b\": 2 }"));
        var e2 = assertThrows(RuntimeException.class, () -> new JsonParser("{ \"x\": \"a\": 1 } }"));
        var e3 = assertThrows(RuntimeException.class, () -> new JsonParser("[ { \"a\": 1 }, \"b\": 2 } ]"));
        assertEquals("Expected end of json, found: :1,\"b\":2}", e1.getMessage());
        assertEquals("Json error in map \"x\":\"a\":", e2.getMessage());
        assertEquals("Json error in list near b:", e3.getMessage()); // todo
    }

    @Test
    public void testInvalidMissingClosingBrace() {
        var e1 = assertThrows(RuntimeException.class, () -> new JsonParser("{"));
        var e2 = assertThrows(RuntimeException.class, () -> new JsonParser("{ \"a\": 1, \"b\": 2"));
        var e3 = assertThrows(RuntimeException.class, () -> new JsonParser("{ \"x\": { \"a\": 1, \"b\": 2 }"));
        var e4 = assertThrows(RuntimeException.class, () -> new JsonParser("{ \"x\": { \"a\": 1 }, \"b\": 2"));
        assertEquals("Json error null at end", e1.getMessage());
        assertEquals("Json error in map \"b\":2 at end", e2.getMessage());
        assertEquals("Json error null at end", e3.getMessage());
        assertEquals("Json error in map \"b\":2 at end", e4.getMessage());
    }

    @Test
    public void testInvalidListStructure() {
        var e1 = assertThrows(RuntimeException.class, () -> new JsonParser("[1, 2, 3,]"));
        var e2 = assertThrows(RuntimeException.class, () -> new JsonParser("[1,, 2, 3]"));
        var e3 = assertThrows(RuntimeException.class, () -> new JsonParser("[1 2, 3]"));
        var e4 = assertThrows(RuntimeException.class, () -> new JsonParser("[ [1, 2, 3} ]"));
        var e5 = assertThrows(RuntimeException.class, () -> new JsonParser("[ \"a\": 1, \"b\": 2 ]"));
        assertEquals("Json error For input string: \"]\" at end", e1.getMessage());
        assertEquals("Json error For input string: \",\"2", e2.getMessage());
        assertEquals("Json error in list near 12", e3.getMessage());
        assertEquals("Json error in list near 3}", e4.getMessage());
        assertEquals("Json error in list near a:", e5.getMessage()); // todo
    }

    @Test
    public void testInvalidMapStructure() {
        var e1 = assertThrows(RuntimeException.class, () -> new JsonParser("{ { \"a\": 1, \"b\": 2 } }"));
        var e2 = assertThrows(RuntimeException.class, () -> new JsonParser("{ \"x\": { \"a\":1, \"b\":2 ], \"y\":3}"));
        var e3 = assertThrows(RuntimeException.class, () -> new JsonParser("{ \"a\": 1, \"b\": 2, }"));
        var e4 = assertThrows(RuntimeException.class, () -> new JsonParser("{ a: 1, b: 2 }"));
        var e5 = assertThrows(RuntimeException.class, () -> new JsonParser("{ \"a\":: 1 }"));
        var e6 = assertThrows(RuntimeException.class, () -> new JsonParser("{ \"a\" 1, \"b\": 2 }"));
        var e7 = assertThrows(RuntimeException.class, () -> new JsonParser("{ 123: 5 }"));
        assertEquals("Json error in map {\"a\":1", e1.getMessage());
        assertEquals("Json error in map \"b\":2]", e2.getMessage());
        assertEquals("Json error null at end", e3.getMessage());
        assertEquals("Json error in map a:1,", e4.getMessage());
        assertEquals("Json error in map \"a\"::1", e5.getMessage());
        assertEquals("Json error in map \"a\"1,\"b\"", e6.getMessage());
        assertEquals("Json error in map 123:5}", e7.getMessage());
    }

    @Test
    public void testWrongTypes() {
        var jp = new JsonParser("{ \"num\": 1, \"str\": \"2\" }");
        assertEquals(1, jp.<String>get("num"));
        assertEquals("2", jp.<Integer>get("str"));
    }
}