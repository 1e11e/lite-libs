package libs.lite.yamlparser;

import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class YamlParserTest {
    @Test
    public void testFromComment() {
        var yp = new YamlParser("""
                key: "value"        # quoted string
                seq:
                  - id: 1           # integer
                    name: one       # string
                    lock: true      # boolean
                  - id: 2
                    name:           # no value means null, can also use null or ~
                """);

        System.out.println("yp.get() --> " + yp.get());
        assertEquals("value", yp.get("key"));
        assertEquals(2, (int) yp.get("seq", 1, "id"));

        String key = yp.get("key");
        assertEquals("VALUE", key.toUpperCase());
        assertEquals("VALUE", yp.<String>get("key").toUpperCase());
        assertEquals("VALUE", ((String) yp.get("key")).toUpperCase());

        yp.<List<Map<String, ?>>>get("seq").forEach(x -> System.out.println(x.get("id") + " is " + x.get("name")));

        assertNull(yp.<String>get("seq", 1, "name"));
        assertNull(yp.<Boolean>get("seq", 1, "lock"));
    }

    @Test
    public void testYamlParser() {
        var yamlString = """
                %YAML 1.x
                ---
                artists:
                  - name: Beatles
                    albums:
                      - name: Help
                        year: 1965
                        top1: true
                      - name: Abbey Road
                        year: 1969
                        top1: true
                  - name: Morcheeba
                    albums:
                      - name: Charango
                        year: 2002
                        top1: false
                      - name: The Antidote
                        year: 2005
                        top1: false
                  - name: Deep Purple
                    albums:
                      - name: Fireball
                        year: 1971
                        top1: true
                      - name: Burn
                        year: 1974
                        top1: false
                ...
                """;
        var yp = new YamlParser(yamlString);
        int yearOfHelp = yp.get("artists", 0, "albums", 0, "year");
        assertEquals(1965, yearOfHelp);

        List<String> allArtists = yp.<List<Map<String, String>>>get("artists")
                .stream()
                .map(artist -> artist.get("name"))
                .toList();
        assertEquals(List.of("Beatles", "Morcheeba", "Deep Purple"), allArtists);

        List<String> top1Albums = yp.<List<Map<String, List<Map<String, ?>>>>>get("artists")
                .stream()
                .flatMap(artist -> artist.get("albums")
                        .stream()
                        .filter(album -> (Boolean) album.get("top1")))
                .map(album -> (String) album.get("name"))
                .toList();
        assertEquals(List.of("Help", "Abbey Road", "Fireball"), top1Albums);
    }

    @Test
    public void testList() {
        var yp = new YamlParser("""
                - one
                - two
                - three
                """);
        assertEquals(List.of("one", "two", "three"), yp.get());
    }

    @Test
    public void testMap() {
        var yp = new YamlParser("""
                k1: v1
                k2: v2
                k3: v3
                """);
        assertEquals(Map.of("k1", "v1", "k2", "v2", "k3", "v3"), yp.get());
    }

    @Test
    public void testListOfList() {
        assertEquals(List.of(List.of("a", "b")), new YamlParser("""
                -
                  - a
                  - b
                """).get());
        assertEquals(List.of(List.of("a", "b")), new YamlParser("""
                - - a
                  - b
                """).get());
    }

    @Test
    public void testMapOfLists() {
        var yp = new YamlParser("""
                k1:
                  - v1
                  - v2
                k2:
                  - v3
                  - v4
                """);
        assertEquals(Map.of(
                "k1", List.of("v1", "v2"),
                "k2", List.of("v3", "v4")), yp.get());
    }

    @Test
    public void testListOfMaps() {
        assertEquals(
                List.of(
                        Map.of("k1", "v1", "k2", "v2"),
                        Map.of("k1", "v3", "k2", "v4")),
                new YamlParser("""
                        - k1: v1
                          k2: v2
                        - k1: v3
                          k2: v4
                        """).get());
    }

    @Test
    public void testListOfListsOfMaps() {
        assertEquals(
                List.of(List.of(
                        Map.of("k1", "v1", "k2", "v2"),
                        Map.of("k1", "v3", "k2", "v4"))),
                new YamlParser("""
                        -
                          - k1: v1
                            k2: v2
                          - k1: v3
                            k2: v4
                        """).get());
        assertEquals(
                List.of(List.of(
                        Map.of("k1", "v1", "k2", "v2"),
                        Map.of("k1", "v3", "k2", "v4"))),
                new YamlParser("""
                        -
                          -
                            k1: v1
                            k2: v2
                          -
                            k1: v3
                            k2: v4
                        """).get());
        assertEquals(
                List.of(List.of(
                        Map.of("k1", "v1", "k2", "v2"),
                        Map.of("k1", "v3", "k2", "v4"))),
                new YamlParser("""
                        - - k1: v1
                            k2: v2
                          - k1: v3
                            k2: v4
                        """).get());
        assertEquals(
                List.of(List.of(List.of(
                        Map.of("k1", "v1", "k2", "v2"),
                        Map.of("k1", "v3", "k2", "v4")))),
                new YamlParser("""
                        - - - k1: v1
                              k2: v2
                            - k1: v3
                              k2: v4
                        """).get());
    }

    @Test
    public void testListOfMapsAndList() {
        var yp = new YamlParser("""
                - k1: v1
                - - a
                  - b
                - k2: v2
                """);
        assertEquals(3, yp.<List<?>>get().size());
        assertEquals(List.of(Map.of("k1", "v1"), List.of("a", "b"), Map.of("k2", "v2")), yp.get());
    }

    @Test
    public void testEmptyLists() {
        // [] is the only way to have an empty list in YAML, else it will be null
        var listOfNull = new ArrayList<>() {{ add(null); }};
        assertEquals(List.of(), new YamlParser("[]").get());
        assertEquals(listOfNull, new YamlParser("-").get());
        assertEquals(List.of(List.of(listOfNull)), new YamlParser("""
                -
                  -
                    -
                """).get());
        assertEquals(List.of(List.of(listOfNull)), new YamlParser("- - -").get());
        assertEquals(List.of(List.of(List.of(listOfNull))), new YamlParser("- - - -").get());
    }

    @Test
    public void testEmptyMaps() {
        // {} is the only way to have an empty map in YAML
        assertEquals(Map.of(), new YamlParser("{}").get());
        assertEquals(Map.of("a", Map.of(), "b", Map.of()), new YamlParser("""
                a: {}
                b: {}
                """).get());
    }

    @Test
    public void testBoolean() {
        var yp = new YamlParser("""
                - true
                - false
                """);
        assertEquals(List.of(true, false), yp.get());
    }

    @Test
    public void testNull() {
        var yp = new YamlParser("""
                empty:
                key:
                    empty:
                null: null
                tilde: ~
                last:
                """);
        assertNull(yp.get("empty"));
        assertNull(yp.get("key", "empty"));
        assertNull(yp.get("null"));
        assertNull(yp.get("tilde"));
        assertNull(yp.get("last"));
    }

    @Test
    public void testInt() {
        var yp = new YamlParser("""
                - -1
                - -0
                - 0
                - +0
                - +1
                - 23
                - 2000000000
                - 0078
                """);
        assertEquals(List.of(-1, 0, 0, 0, 1, 23, 2_000_000_000, 78), yp.get());
    }

    @Test
    public void testDouble() {
        var yp = new YamlParser("""
                - -.1
                - -.1e2
                - 0.0
                - .1
                - 1.
                - +1.2E+1
                - 23.40
                - 123.456e-500
                - 1e1
                - +1.
                """);
        assertEquals(List.of(-.1, -.1e2, 0.0, .1, 1., 1.2e1, 23.4, 0.0, 1E1, 1.0), yp.get());
    }

    @Test
    public void testShouldNotBeDouble() {
        var yp = new YamlParser("""
                - Infinity
                - -Infinity
                - NaN
                - -NaN
                - 1e11e
                """);
        assertEquals(List.of("Infinity", "-Infinity", "NaN", "-NaN", "1e11e"), yp.get());
    }

    @Test
    public void testString() {
        var yp = new YamlParser("""
                - test string
                - with "quotes"
                - with 'quotes'
                - with "odd'
                - esc \\n\\t \\chars
                - 8.8.8.8
                """);
        assertEquals(List.of("test string", "with \"quotes\"", "with 'quotes'", "with \"odd'", "esc \\n\\t \\chars", "8.8.8.8"), yp.get());
    }

    @Test
    public void testDoubleQuotedString() {
        var yp = new YamlParser("""
                - "test string "
                - "with \\"quotes\\""
                - "with 'quotes'"
                - "esc \\n\\t \\\\chars"
                - ""
                """);
        assertEquals(List.of("test string ", "with \"quotes\"", "with 'quotes'", "esc \n\t \\chars", ""), yp.get());
    }

    @Test
    public void testSingleQuotedString() {
        var yp = new YamlParser("""
                - 'test string '
                - 'with "quotes"'
                - 'with \\'quotes\\''
                - 'with ''quotes'''
                - 'esc \\n\\t \\\\chars'
                - ''
                """);
        assertEquals(List.of("test string ", "with \"quotes\"", "with 'quotes'", "with 'quotes'", "esc \n\t \\chars", ""), yp.get());
    }

    @Test
    public void testQuotedIsolation() {
        var yp = new YamlParser("""
                - "key: val"
                - "- list"
                - "a # comment ..."
                - "%YAML &anchor !tag ---"
                """);
        assertEquals(List.of("key: val", "- list", "a # comment ...", "%YAML &anchor !tag ---"), yp.get());
    }

    @Test
    public void testBlockString() {
        var yp = new YamlParser("""
                block: |
                  { hello: over,
                  multiple: lines }
                    - with an #indent
                  [in, the,
                  middle] ...
                block_ind: !!tag-ignore |2
                  hello over
                  multiple lines
                block_chomp1: &anchor-ignore |-
                  hello over
                  multiple lines
                block_chomp2: |+ #comment-ignore
                  hello over
                  multiple lines
                """);
        assertEquals("""
                { hello: over,
                multiple: lines }
                  - with an #indent
                [in, the,
                middle] ...
                """, yp.get("block"));
        assertEquals("hello over\nmultiple lines\n", yp.get("block_ind"));
        assertEquals("hello over\nmultiple lines", yp.get("block_chomp1"));
        assertEquals("hello over\nmultiple lines\n", yp.get("block_chomp2"));
    }

    @Test
    public void testFoldString() {
        var yp = new YamlParser("""
                fold: >
                  { hello: over,
                  multiple: lines }
                    - with an #indent
                  [in, the,
                  middle] ...
                fold_ind: !!tag-ignore >2
                  hello over
                  multiple lines
                fold_chomp1: &anchor-ignore >-
                  hello over
                  multiple lines
                fold_chomp2: >+ # comment-ignore
                  hello over
                  multiple lines
                """);
        assertEquals("""
                { hello: over, multiple: lines }
                  - with an #indent
                [in, the, middle] ...
                """, yp.get("fold"));
        assertEquals("hello over multiple lines\n", yp.get("fold_ind"));
        assertEquals("hello over multiple lines", yp.get("fold_chomp1"));
        assertEquals("hello over multiple lines\n", yp.get("fold_chomp2"));
    }

    @Test
    public void testInvalidFoldString() {
        var e1 = assertThrows(RuntimeException.class, () -> new YamlParser("""
                fold: >
                with no
                indent
                """));
        var e2 = assertThrows(RuntimeException.class, () -> new YamlParser("""
                fold: > invalid
                  with
                  indent
                """));
        assertEquals("Expected end of yaml, found: [VAL=with no, TAB= , VAL=indent]", e1.getMessage());
        assertEquals("Expected end of yaml, found: [VAL=with, TAB=   , VAL=indent]", e2.getMessage());
    }

    @Test
    public void testInvalidBlockString() {
        var e1 = assertThrows(RuntimeException.class, () -> new YamlParser("""
                block: |
                with no
                indent
                """));
        var e2 = assertThrows(RuntimeException.class, () -> new YamlParser("""
                block: | invalid
                  with
                  indent
                """));
        assertEquals("Expected end of yaml, found: [VAL=with no, TAB= , VAL=indent]", e1.getMessage());
        assertEquals("Expected end of yaml, found: [VAL=with, TAB=   , VAL=indent]", e2.getMessage());
    }

    @Test
    public void testDeepNest() {
        var yp = new YamlParser("""
                a:
                    b:
                      - c:
                            d:
                              - e
                              - f
                      - g:
                            h:
                              - i
                              - j
                """);
        assertEquals("f", yp.get("a", "b", 0, "c", "d", 1));
        assertEquals(List.of("i", "j"), yp.get("a", "b", 1, "g", "h"));

        var yp1 = new YamlParser("""
                root:
                    sub:
                        greet: hello
                        lists:
                            - - - deep
                                - str: >-
                                    no
                                    newlines
                key: val
                """);
        assertEquals("no newlines", yp1.get("root", "sub", "lists", 0, 0, 1, "str"));
        assertEquals("val", yp1.get("key"));

        var yp2 = new YamlParser("""
                root:
                    sub:
                        greet: hello
                        lists:
                            - - - deep
                                - str: >-
                                    no
                                    newlines
                    key: val
                """);
        assertEquals("val", yp2.get("root", "key"));

        var yp3 = new YamlParser("""
                root:
                    sub:
                        greet: hello
                        lists:
                            - - - deep
                                - str: >-
                                    no
                                    newlines
                        key: val
                """);
        assertEquals("val", yp3.get("root", "sub", "key"));

        var yp4 = new YamlParser("""
                root:
                    sub:
                        greet: hello
                        lists:
                            - - - deep
                                - str: >-
                                    no
                                    newlines
                            - key: val
                """);
        assertEquals("val", yp4.get("root", "sub", "lists", 1, "key"));

        var yp5 = new YamlParser("""
                root:
                    sub:
                        greet: hello
                        lists:
                            - - - deep
                                - str: >-
                                    no
                                    newlines
                                  key: val
                """);
        assertEquals("val", yp5.get("root", "sub", "lists", 0, 0, 1, "key"));

        var yp6 = new YamlParser("""
                root:
                    sub:
                        greet: hello
                        lists:
                            - - - deep
                                - str: >-
                                    no
                                    newlines
                                - key: val
                """);
        assertEquals("val", yp6.get("root", "sub", "lists", 0, 0, 2, "key"));
    }

    @Test
    public void testMissingKey() {
        assertNull(new YamlParser("""
                key1: val1
                """).get("key2"));
        assertNull(new YamlParser("{}").get("n/a"));
    }

    @Test
    public void testShortYaml() {
        assertNull(new YamlParser("").get());
        assertNull(new YamlParser("~").get());
        assertNull(new YamlParser("null").get());
        assertEquals("", new YamlParser("\"\"").get());
        assertEquals("", new YamlParser("''").get());
        assertEquals("x", new YamlParser("x").get());
        assertEquals("x x", new YamlParser("x x").get());
        assertEquals("x:x", new YamlParser("x:x").get());
        assertEquals(new HashMap<>() {{ put("x", null); }}, new YamlParser("x:").get());
        assertEquals(Map.of("x", "x"), new YamlParser("x: x").get());
        assertEquals(true, new YamlParser("true").get());
        assertEquals(false, new YamlParser("false").get());
        assertEquals(123, (int) new YamlParser("123").get());
        assertEquals(45.6, (double) new YamlParser("45.6").get());
        assertEquals(new ArrayList<>() {{ add(null); }}, new YamlParser("-").get());
        assertEquals(List.of(), new YamlParser("[]").get());
        assertEquals(Map.of(), new YamlParser("{}").get());
    }

    @Test
    public void testWhitespace() {
        var yp = new YamlParser("""
                key a :
                - " string "  
                  
                -          {} 
                key b : 
                     - key c' : \t     z e r o
                       
                       key"  d: one \t
                     -
                       key e : two 
                       
                       key f  : three
                key g:     # comment
                       true
                """);
        assertEquals(Map.of(
                "key a", List.of(
                        " string ",
                        Map.of()),
                "key b", List.of(
                        Map.of("key c'", "z e r o", "key\"  d", "one"),
                        Map.of("key e", "two", "key f", "three")),
                "key g", true), yp.get());
    }

    @Test
    public void testFlowCollection() {
        // flow collections are returned as strings, except empty map {} and empty list []
        var yp = new YamlParser("""
                subflows:
                  ary: [
                    [1, 3],
                    [2, 5],
                    [3, 7]
                  ]
                  map: { x: 1,
                         y: [2, 3],
                         z: 4 }
                mix: [true, 15, "mix"]
                empty list as str: [ ]
                empty map as str: { }
                real empty list: []
                real empty map: {}
                """);
        assertEquals("[\n     [1, 3],\n     [2, 5],\n     [3, 7]\n   ]", yp.get("subflows", "ary"));
        assertEquals("{ x: 1,\n          y: [2, 3],\n          z: 4 }", yp.get("subflows", "map"));
        assertEquals("[true, 15, \"mix\"]", yp.get("mix"));
        assertEquals("[ ]", yp.get("empty list as str"));
        assertEquals("{ }", yp.get("empty map as str"));
        assertEquals(List.of(), yp.get("real empty list"));
        assertEquals(Map.of(), yp.get("real empty map"));
    }

    @Test
    public void testDocument() {
        assertEquals("long string over 2 lines\n", new YamlParser("""
                # comment
                --- >
                  long string
                  over 2 lines
                """).get());
        assertEquals(List.of("hello"), new YamlParser("""
                --- - hello
                """).get());
        assertEquals(Map.of("key", "val1"), new YamlParser("""
                --- key: val1
                """).get());
        assertEquals(Map.of("key", "val2"), new YamlParser("""
                %YAML 1.2 # comment
                --- key: val2
                """).get());
        assertEquals(Map.of("key", "val4"), new YamlParser("""
                # one more
                # comment
                %TAG ! tag:stuff,2000:
                --- !shape
                key: val3
                ...
                ---
                key: val4
                ...
                """).get());
    }

    @Test
    public void testIgnoreTag() {
        assertEquals(Map.of("name", 5), new YamlParser("name: !!str 5").get());
    }

    @Test
    public void testIgnoreAnchor() {
        assertEquals(Map.of("name", "bilbo", "nick", "*handle"), new YamlParser("""
                name: &handle bilbo
                nick: *handle
                """).get());
    }

    @Test
    public void testMixingSpecialChars() {
        var yp = new YamlParser("""
                gt>: > not a fold               # not a fold since nothing indented follows
                pipe|: | not a block            # not a block since nothing indented follows
                excl!: !ignore-tag Wow! !Cool
                amp&: &ignore-anchor M&M &c:o
                hash#: Suit#13 #ignore-comment
                tilde~: ~/dir/not/null
                """);
        assertEquals(Map.of(
                "gt>", "> not a fold",
                "pipe|", "| not a block",
                "excl!", "Wow! !Cool",
                "amp&", "M&M &c:o",
                "hash#", "Suit#13",
                "tilde~", "~/dir/not/null"), yp.get());
    }

    @Test
    public void testOddities() {
        // Things that aren't exactly by the specs

        // keys in a row, like sequence - - -
        assertEquals(Map.of("root1", Map.of("key1", Map.of("key2", "value1")),
                "root2", "value2"),
                new YamlParser("""
                root1: key1: key2: value1
                root2: value2
                """).get());

        // Blank lines in a yaml-string are ignored, even in blocks and folds
        assertEquals(Map.of(
                "empty", "blank fold lines are ignored, <-so a newline is missing\n  (but indents are supported)\n"),
                new YamlParser("""
                empty: >
                    blank fold lines are ignored,
                    
                    <-so a newline is missing
                      (but indents are supported)
                """).get());

        // Chomp chars are slightly out of specs, but it is a confusing default by YAML imho.
        // By the specs, if you add a new key-value last here it will unexpectedly introduce a newline to the fold.
        // It makes more sense to always have the trailing newline, and use >- if you want to strip it.
        assertEquals(Map.of(
                "chomp", "should be no final newline, but it still appears here->\n"),
                new YamlParser("""
                chomp: >
                    should be no final newline,
                    but it still appears here->""").get());  // <-- no newline at end

        // Indents are not as important to the parser as undents, since they indicate the end of a block.
        // Indents are mostly valid even if they do not align and drift away.
        // If the undent is also misaligned, the structure is still considered sound.
        assertEquals(Map.of(
                "rootmap", Map.of(
                    "k1", "v1",
                    "k2", "v2",
                    "k3", "v3"),
                "rootkey", "val"
        ), new YamlParser("""
                rootmap:
                  k1: v1
                   k2: v2
                    k3: v3
                   rootkey: val
                """).get());

        // As soon as a properly aligned undent happens, the parser will detect it is off and throw an error.
        var e1 = assertThrows(RuntimeException.class, () -> new YamlParser("""
                rootmap:
                  k1: v1
                   k2: v2
                    k3: v3
                rootkey: val
                """));
        assertEquals("Expected end of yaml, found: [TAB= , KEY=rootkey, VAL=val]", e1.getMessage());
    }

    @Test
    public void testFailComplexKey() {
        var e1 = assertThrows(RuntimeException.class, () -> new YamlParser("""
                ? - a
                  - b
                :
                  - c
                """).get());
        assertEquals("Expected end of yaml, found: [TAB=    , SEQ=-, VAL=b, TAB= , VAL=:, T", e1.getMessage());
    }

    @Test
    public void testFailMultilineStringWithoutFoldOrBlock() {
        var e1 = assertThrows(RuntimeException.class, () -> new YamlParser("""
                a: hello over
                   multiple lines
                """).get());
        assertEquals("Expected end of yaml, found: [VAL=multiple lines]", e1.getMessage());
    }

    @Test
    public void testInvalidValues() {
        var e1 = assertThrows(RuntimeException.class, () -> new YamlParser("""
                - 123
                  unknown"""));
        var e2 = assertThrows(RuntimeException.class, () -> new YamlParser("""
                a: >*
                  two
                  lines"""));
        assertEquals("Expected end of yaml, found: [VAL=unknown]", e1.getMessage());
        assertEquals("Expected end of yaml, found: [VAL=two, TAB=   , VAL=lines]", e2.getMessage());
    }

    @Test
    public void testInvalidIndent() {
        // it is actually the undent that is invalid for the parser since it does not close a block
        var e1 = assertThrows(RuntimeException.class, () -> new YamlParser("""
                - 1
                    - 2
                - 3"""));
        var e2 = assertThrows(RuntimeException.class, () -> new YamlParser("""
                a: 1
                    b: 2
                c: 3"""));
        assertEquals("Expected end of yaml, found: [TAB=  , SEQ=-, INT=3]", e1.getMessage());
        assertEquals("Expected end of yaml, found: [TAB= , KEY=c, INT=3]", e2.getMessage());
    }

    @Test
    public void testInvalidMultipleRoot() {
        var e1 = assertThrows(RuntimeException.class, () -> new YamlParser("""
                - a
                b: 1"""));
        var e2 = assertThrows(RuntimeException.class, () -> new YamlParser("""
                a: 1
                - b"""));
        assertEquals("Expected end of yaml, found: [TAB= , KEY=b, INT=1]", e1.getMessage());
        assertEquals("Expected end of yaml, found: [SEQ=-, VAL=b]", e2.getMessage());
    }

    @Test
    public void testWrongTypes() {
        var yp = new YamlParser("""
                int: 1
                str: "2"
                """);
        assertEquals(1, yp.<String>get("int"));
        assertEquals("2", yp.<Integer>get("str"));
    }

    /*
     * Examples from YAML spec
     */
    @Test
    public void testExample1() {
        assertEquals(List.of("Mark McGwire", "Sammy Sosa", "Ken Griffey"), new YamlParser("""
                - Mark McGwire
                - Sammy Sosa
                - Ken Griffey
                """).get());
    }

    @Test
    public void testExample2() {
        assertEquals(Map.of("hr", 65, "avg", 0.278, "rbi", 147), new YamlParser("""
                hr:  65    # Home runs
                avg: 0.278 # Batting average
                rbi: 147   # Runs Batted In
                """).get());
    }

    @Test
    public void testExample3() {
        assertEquals(
                Map.of("american", List.of("Boston Red Sox", "Detroit Tigers", "New York Yankees"),
                        "national", List.of("New York Mets", "Chicago Cubs", "Atlanta Braves")),
                new YamlParser("""
                        american:
                          - Boston Red Sox
                          - Detroit Tigers
                          - New York Yankees
                        national:
                          - New York Mets
                          - Chicago Cubs
                          - Atlanta Braves
                        """).get());
    }

    @Test
    public void testExample4() {
        assertEquals(
                List.of(Map.of("name", "Mark McGwire", "hr", 65, "avg", 0.278),
                        Map.of("name", "Sammy Sosa", "hr", 63, "avg", 0.288)),
                new YamlParser("""
                        -
                          name: Mark McGwire
                          hr:   65
                          avg:  0.278
                        -
                          name: Sammy Sosa
                          hr:   63
                          avg:  0.288
                        """).get());
    }

    @Test
    public void testExample5() {
        assertEquals(
                List.of("[name        , hr, avg  ]", "[Mark McGwire, 65, 0.278]", "[Sammy Sosa  , 63, 0.288]"),
                new YamlParser("""
                        - [name        , hr, avg  ]
                        - [Mark McGwire, 65, 0.278]
                        - [Sammy Sosa  , 63, 0.288]
                        """).get());
    }

    @Test
    public void testExample6() {
        assertEquals(
                Map.of("Mark McGwire", "{hr: 65, avg: 0.278}",
                        "Sammy Sosa", "{\n     hr: 63,\n     avg: 0.288\n   }"),
                new YamlParser("""
                        Mark McGwire: {hr: 65, avg: 0.278}
                        Sammy Sosa: {
                            hr: 63,
                            avg: 0.288
                          }
                        """).get());
    }

    @Test
    public void testExample7() {
        assertEquals(
                List.of("Mark McGwire", "Sammy Sosa", "Ken Griffey", "Chicago Cubs", "St Louis Cardinals"),
                new YamlParser("""
                        # Ranking of 1998 home runs
                        ---
                        - Mark McGwire
                        - Sammy Sosa
                        - Ken Griffey
                                                
                        # Team ranking
                        ---
                        - Chicago Cubs
                        - St Louis Cardinals
                        """).get());
    }

    @Test
    public void testExample8() {
        assertEquals(
                Map.of("time", "20:03:47", "player", "Sammy Sosa", "action", "grand slam"),
                new YamlParser("""
                        ---
                        time: 20:03:20
                        player: Sammy Sosa
                        action: strike (miss)
                        ...
                        ---
                        time: 20:03:47
                        player: Sammy Sosa
                        action: grand slam
                        ...
                        """).get());
    }

    @Test
    public void testExample9() {
        assertEquals(
                Map.of("hr", List.of("Mark McGwire", "Sammy Sosa"), "rbi", List.of("Sammy Sosa", "Ken Griffey")),
                new YamlParser("""
                        ---
                        hr: # 1998 hr ranking
                          - Mark McGwire
                          - Sammy Sosa
                        rbi:
                          # 1998 rbi ranking
                          - Sammy Sosa
                          - Ken Griffey
                        """).get());
    }

    @Test
    public void testExample10() {
        assertEquals(
                Map.of("hr", List.of("Mark McGwire", "Sammy Sosa"), "rbi", List.of("*SS", "Ken Griffey")),
                new YamlParser("""
                        ---
                        hr:
                          - Mark McGwire
                          # Following node labeled SS
                          - &SS Sammy Sosa
                        rbi:
                          - *SS # Subsequent occurrence
                          - Ken Griffey
                        """).get());
    }

    @Test
    public void testExample11Fail() {
        // complex keys not supported
        assertThrows(RuntimeException.class,
                () -> new YamlParser("""
                        ? - Detroit Tigers
                          - Chicago cubs
                        :
                          - 2001-07-23
                                                
                        ? [ New York Yankees,
                            Atlanta Braves ]
                        : [ 2001-07-02, 2001-08-12,
                            2001-08-14 ]
                        """).get());
    }

    @Test
    public void testExample12() {
        assertEquals(
                List.of(Map.of("item", "Super Hoop", "quantity", 1),
                        Map.of("item", "Basketball", "quantity", 4),
                        Map.of("item", "Big Shoes", "quantity", 1)),
                new YamlParser("""
                        ---
                        # Products purchased
                        - item    : Super Hoop
                          quantity: 1
                        - item    : Basketball
                          quantity: 4
                        - item    : Big Shoes
                          quantity: 1
                        """).get());
    }

    @Test
    public void testExample13() {
        assertEquals("""
                        \\//||\\/||
                        // ||  ||__
                        """,
                new YamlParser("""
                        # ASCII Art
                        --- |
                          \\//||\\/||
                          // ||  ||__
                        """).get());
    }

    @Test
    public void testExample14() {
        assertEquals("Mark McGwire's year was crippled by a knee injury.\n",
                new YamlParser("""
                        --- >
                          Mark McGwire's
                          year was crippled
                          by a knee injury.
                        """).get());
    }

    @Test
    public void testExample15() {
        assertEquals("""
                        Sammy Sosa completed another fine season with great stats.
                          63 Home Runs
                          0.288 Batting Average
                        What a year!
                        """,
                new YamlParser("""
                        >
                         Sammy Sosa completed another
                         fine season with great stats.
                            
                           63 Home Runs
                           0.288 Batting Average
                            
                         What a year!
                        """).get());
    }

    @Test
    public void testExample16() {
        assertEquals(Map.of(
                        "name", "Mark McGwire",
                        "accomplishment", "Mark set a major league home run record in 1998.\n",
                        "stats", """
                                65 Home Runs
                                0.278 Batting Average
                                """),
                new YamlParser("""
                        name: Mark McGwire
                        accomplishment: >
                          Mark set a major league
                          home run record in 1998.
                        stats: |
                          65 Home Runs
                          0.278 Batting Average
                        """).get());
    }

    @Test
    public void testExample17() {
        assertEquals(Map.of(
                        "unicode", "Sosa did fine.☺",
                        "control", "\b1998\t1999\t2000\n",
                        "hex esc", "\\x0d\\x0a is \r\n",
                        "single", "\"Howdy!\" he cried.",
                        "quoted", " # Not a 'comment'.",
                        "tie-fighter", "|\\-*-/|"
                ),
                new YamlParser("""
                        unicode: "Sosa did fine.\u263A"
                        control: "\\b1998\\t1999\\t2000\\n"
                        hex esc: "\\\\x0d\\\\x0a is \\r\\n"

                        single: '"Howdy!" he cried.'
                        quoted: ' # Not a ''comment''.'
                        tie-fighter: '|\\\\-*-/|'
                        """).get());
    }

    @Test
    public void testExample18Fail() {
        // only multiline block or fold strings are supported
        assertThrows(RuntimeException.class,
                () -> new YamlParser("""
                        plain:
                          This unquoted scalar
                          spans many lines.

                        quoted: "So does this
                          quoted scalar.\\n"
                        """).get());
    }

    @Test
    public void testExample19() {
        assertEquals(Map.of(
                        "canonical", 12345,
                        "decimal", 12345,
                        "octal", "0o14",
                        "hexadecimal", "0xC"),
                new YamlParser("""
                        canonical: 12345
                        decimal: +12345
                        octal: 0o14
                        hexadecimal: 0xC
                        """).get());
    }

    @Test
    public void testExample20() {
        assertEquals(Map.of(
                        "canonical", 1.23015e+3,
                        "exponential", 12.3015e+02,
                        "fixed", 1230.15,
                        "negative infinity", "-.inf",
                        "not a number", ".NaN"),
                new YamlParser("""
                        canonical: 1.23015e+3
                        exponential: 12.3015e+02
                        fixed: 1230.15
                        negative infinity: -.inf
                        not a number: .NaN
                        """).get());
    }

    @Test
    public void testExample21() {
        assertEquals(new HashMap<String, Object>() {{
                         put("null", null);
                         put("booleans", "[ true, false ]");
                         put("string", "012345");
                     }},
                new YamlParser("""
                        null:
                        booleans: [ true, false ]
                        string: '012345'
                        """).get());
    }

    @Test
    public void testExample22() {
        assertEquals(Map.of(
                        "canonical", "2001-12-15T02:59:43.1Z",
                        "iso8601", "2001-12-14t21:59:43.10-05:00",
                        "spaced", "2001-12-14 21:59:43.10 -5",
                        "date", "2002-12-14"),
                new YamlParser("""
                        canonical: 2001-12-15T02:59:43.1Z
                        iso8601: 2001-12-14t21:59:43.10-05:00
                        spaced: 2001-12-14 21:59:43.10 -5
                        date: 2002-12-14
                        """).get());
    }

    @Test
    public void testExample23() {
        assertEquals(Map.of(
                        "not-date", "2002-04-28",
                        "picture", """
                                R0lGODlhDAAMAIQAAP//9/X
                                17unp5WZmZgAAAOfn515eXv
                                Pz7Y6OjuDg4J+fn5OTk6enp
                                56enmleECcgggoBADs=
                                """,
                        "application specific tag", """
                                The semantics of the tag
                                above may be different for
                                different documents.
                                """),
                new YamlParser("""
                        ---
                        not-date: !!str 2002-04-28
                                                
                        picture: !!binary |
                         R0lGODlhDAAMAIQAAP//9/X
                         17unp5WZmZgAAAOfn515eXv
                         Pz7Y6OjuDg4J+fn5OTk6enp
                         56enmleECcgggoBADs=
                                                
                        application specific tag: !something |
                         The semantics of the tag
                         above may be different for
                         different documents.
                        """).get());
    }

    @Test
    public void testExample24() {
        assertEquals(List.of(
                        Map.of("center", "{x: 73, y: 129}", "radius", 7),
                        Map.of("start", "*ORIGIN", "finish", "{ x: 89, y: 102 }"),
                        Map.of("start", "*ORIGIN", "color", "0xFFEEBB", "text", "Pretty vector drawing.")),
                new YamlParser("""
                        %TAG ! tag:clarkevans.com,2002:
                        --- !shape
                          # Use the ! handle for presenting
                          # tag:clarkevans.com,2002:circle
                        - !circle
                          center: &ORIGIN {x: 73, y: 129}
                          radius: 7
                        - !line
                          start: *ORIGIN
                          finish: { x: 89, y: 102 }
                        - !label
                          start: *ORIGIN
                          color: 0xFFEEBB
                          text: Pretty vector drawing.
                        """).get());
    }

    @Test
    public void testExample25Fail() {
        assertThrows(RuntimeException.class,
                () -> new YamlParser("""
                        # Sets are represented as a
                        # Mapping where each key is
                        # associated with a null value
                        --- !!set
                        ? Mark McGwire
                        ? Sammy Sosa
                        ? Ken Griff
                        """).get());
    }

    @Test
    public void testExample26() {
        // the parser always use a linked hashmap to preserve order (simplifies debugging a bit when they match 1:1),
        // but this omap yaml example becomes a list of maps since the tag is ignored
        assertEquals(List.of(
                        Map.of("Mark McGwire", 65),
                        Map.of("Sammy Sosa", 63),
                        Map.of("Ken Griffy", 58)),
                new YamlParser("""
                        # Ordered maps are represented as
                        # A sequence of mappings, with
                        # each mapping having one key
                        --- !!omap
                        - Mark McGwire: 65
                        - Sammy Sosa: 63
                        - Ken Griffy: 58
                        """).get());
    }

    @Test
    public void testExample27() {
        assertEquals(Map.of(
                        "invoice", 34843,
                        "date", "2001-01-23",
                        "bill-to", Map.of(
                                "given", "Chris",
                                "family", "Dumars",
                                "address", Map.of(
                                        "lines", """
                                                458 Walkman Dr.
                                                Suite #292
                                                """,
                                        "city", "Royal Oak",
                                        "state", "MI",
                                        "postal", 48046)),
                        "ship-to", "*id001",
                        "product", List.of(
                                Map.of("sku", "BL394D", "quantity", 4, "description", "Basketball", "price", 450.00),
                                Map.of("sku", "BL4438H", "quantity", 1, "description", "Super Hoop", "price", 2392.00)),
                        "tax", 251.42,
                        "total", 4443.52,
                        "comments", "Late afternoon is best. Backup contact is Nancy Billsmer @ 338-4338.\n"
                ),
                new YamlParser("""
                        --- !<tag:clarkevans.com,2002:invoice>
                        invoice: 34843
                        date   : 2001-01-23
                        bill-to: &id001
                            given  : Chris
                            family : Dumars
                            address:
                                lines: |
                                    458 Walkman Dr.
                                    Suite #292
                                city    : Royal Oak
                                state   : MI
                                postal  : 48046
                        ship-to: *id001
                        product:
                            - sku         : BL394D
                              quantity    : 4
                              description : Basketball
                              price       : 450.00
                            - sku         : BL4438H
                              quantity    : 1
                              description : Super Hoop
                              price       : 2392.00
                        tax  : 251.42
                        total: 4443.52
                        comments: > # NOTE: added > since only multiline block or fold strings are supported
                            Late afternoon is best.
                            Backup contact is Nancy
                            Billsmer @ 338-4338.
                        """).get());
    }


    @Test
    public void testExample28() {
        assertEquals(Map.of(
                "Time", "2001-11-23 15:02:31 -5",
                "User", "ed",
                "Warning", "A slightly different error message.\n",
                "Date", "2001-11-23 15:03:17 -5",
                "Fatal", "Unknown variable \"bar\"\n",
                "Stack", List.of(
                        Map.of("file", "TopClass.py", "line", 23, "code", "x = MoreObject(\"345\\n\")\n"),
                        Map.of("file", "MoreClass.py", "line", 58, "code", "foo = bar")
                    )
                ),
                new YamlParser("""
                        ---
                        Time: 2001-11-23 15:01:42 -5
                        User: ed
                        Warning: >
                          This is an error message
                          for the log file
                        ---
                        Time: 2001-11-23 15:02:31 -5
                        User: ed
                        Warning: >
                          A slightly different error
                          message.
                        ---
                        Date: 2001-11-23 15:03:17 -5
                        User: ed
                        Fatal: >
                          Unknown variable "bar"
                        Stack:
                          - file: TopClass.py
                            line: 23
                            code: |
                              x = MoreObject("345\\n")
                          - file: MoreClass.py
                            line: 58
                            code: |-
                              foo = bar
                        """).get());
    }
}