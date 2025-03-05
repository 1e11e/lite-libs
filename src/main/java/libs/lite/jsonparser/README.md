Lite Libs JSON Parser
=====================
Tiny JSON parser. The JSON string is parsed into an Object of Map/List/String/Integer/Double/Boolean and null.
A convenience getter will do the type casting. Sanity checks will detect if the JSON is invalid.

Example
   ```java
   var jp = new JsonParser("""
       {
           "key": "value",
           "ary": [
               { "id": 1, "name": "one", "lock": true },
               { "id": 2, "name": null }
           ]
       }
       """);
   ```
The getter takes any number of keys and indexes to navigate into the maps and lists of the structure:
   ```java
   jp.get();                 // {key=value, ary=[{id=1, name=one, lock=true}, {id=2, name=null}]}
   jp.get("key");            // "value"
   jp.get("ary", 0, "id");   // 1
   ```
Type casting can be done in three ways:
   ```java
   String key = jp.get("key");
   key.toUpperCase();                            // "VALUE"  (automatic)
   jp.<String>get("key").toUpperCase();          // "VALUE"  (type witness)
   ((String) jp.get("key")).toUpperCase();       // "VALUE"  (manual)
   ```
The entire tree of interest can be typed ahead to avoid further casting. Note the ? for Integer or String:
   ```java
   jp.<List<Map<String, ?>>>get("ary")
       .forEach(x -> System.out.println(x.get("id") + " is " + x.get("name")));
   -->  1 is one
   -->  2 is null                         (see more examples in the unit tests)
   ```
Null can have different meanings:
   ```java
   jp.<String> get("ary", 1, "name");            // null  (defined as null)
   jp.<Boolean>get("ary", 1, "lock");            // null  (does not exist)
   ```
Any JSON object can be top level:
   ```java
   new JsonParser(".4e2").<Double>get();         // 40.0
   ```
Notes
-----
The class is 49 lines and golfed to the edge of readability, ready to be pasted into a java shell script.
If you can trust the input to be error free you can remove all the if-throws and try-catch to shave 10 more lines.
