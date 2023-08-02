Lite Libs YAML parser
=====================
Tiny YAML parser. The YAML string is parsed into an Object of Map/List/String/Integer/Double/Boolean and null.
A convenience getter will do the type casting. Basic sanity checks will detect if the YAML is very invalid.

Example:
   ```java
   var yp = new YamlParser("""
       key: "value"        # quoted string
       seq:
         - id: 1           # integer
           name: one       # string
           lock: true      # boolean
         - id: 2
           name:           # no value means null, can also use null or ~
       """);
   ```
The getter takes any number of keys and indexes to navigate into the maps and lists of the structure:
   ```java
   yp.get();                 // {key=value, seq=[{id=1, name=one, lock=true}, {id=2, name=null}]}
   yp.get("key");            // "value"
   yp.get("seq", 0, "id");   // 1
   ```
Type casting can be done in three ways:
   ```java
   String key = yp.get("key");
   key.toUpperCase();                            // "VALUE"  (automatic)
   yp.<String>get("key").toUpperCase();          // "VALUE"  (type witness)
   ((String) yp.get("key")).toUpperCase();       // "VALUE"  (manual)
   ```
The entire tree of interest can be typed ahead to avoid further casting. Note the ? for Integer or String:
   ```java
   yp.<List<Map<String, ?>>>get("seq")
       .forEach(x -> System.out.println(x.get("id") + " is " + x.get("name")));
   -->  1 is one
   -->  2 is null                         (see more examples in the unit tests)
   ```
Null can have different meanings:
   ```java
   yp.<String> get("seq", 1, "name");            // null  (defined as null)
   yp.<Boolean>get("seq", 1, "lock");            // null  (does not exist)
   ```
Any YAML object can be top level:
   ```java
   new YamlParser(".4e2").<Double>get();         // 40.0
   ```
Caveats
-------
YAML is a format with (too?) many degrees of freedom, so to keep the parser small there are some compromises and
bending of the specs. It can only handle a subset of YAML features similar to the level of complexity found in JSON.
The following features have limited or no support:
- `!!tags` and `!tags` are ignored.
- `&anchors` are ignored and `*aliases` are just a value.
- `<<` override is not supported and will just be a key "<<" with the value `*alias`.
- Indentation indicators in block `|n` and fold `>n` are ignored (but block/fold strings are supported with chomp char).
- Different TRUE/Yes/Y/ON for boolean are just strings. Only `true`/`false`/`null` are supported.
- Hexadecimal, octal, .inf and .NaN etc are just strings.
- Flow collections will return as a string, except empty list `[]` and empty map `{}` which are supported.
- Document separators `---` and `...` are ignored. All keys will be merged and potentially overwritten.
- `?` complex mapping keys will not work.
- Multiline strings without block `|` or fold `>` will not work (but multiline block and fold strings are supported).

With the above caveats, it parses ~25 of the 28 examples in the YAML 1.2 specification.

Notes
-----
The class is 99 lines and golfed to the edge of readability, ready to be pasted into a java shell script.
The brittle regexps makes it exciting to change anything. Run the unit tests often.
