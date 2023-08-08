LITE LIBS: a simple suite of java lightweight libraries
========================================================
__These classes are intended to make java scripting a bit easier and more
self-contained.__

"Wait a minute" you say, "what's java scripting?" Well, it is a supported
way since Java 11 to make limited source code java programs like how bash-,
perl- and python-scripts work. So if you have a java project, you may not need
to add a huge dependency to python just to have some git-hooks or init-scripts.

Unfortunately it is a cumbersome exercise to pull in external jars into a
java script, so for simplicity you are stuck with the java standard library.
While it is quite rich, there are some basics missing. Lite Libs makes it easier
since its classes are so small they can just be pasted into a script.

The following missing stdlib functionality is currently offered by Lite Libs:
* [GNU/POSIX style command line parser](src/main/java/libs/lite/cli) (79 lines)
* [JSON parser](src/main/java/libs/lite/jsonparser) (59 lines)
* [YAML parser](src/main/java/libs/lite/yamlparser) (99 lines)

Two ways of java scripting
--------------------------
Example that reads a json file and prints a value. It is basically a normal java
program that is dependency free and needs no build step or packaging.

### 1. Plain java scripting
The first way is exactly like a normal java program.
```java
package org.example;

// <stdlib imports hidden>

// First class name must match filename: Script.java
class Script {
    public static void main(String[] args) throws IOException {
        var configJson = new String(Files.readAllBytes(Path.of(args[0])));
        var jsonParser = new JsonParser(configJson);
        var testPort = jsonParser.get("deployment", "test", "port");
        System.out.println("test env at port: " + port);
    }
}

// Paste the classes you need from Lite Libs at the end, no access modifier
class JsonParser {
    ...
}
```
Run it like this
```
$ java Script.java config.json
```
**Pros**
- Full support in IntelliJ, including breakpoints

**Cons**
- Clunky way of running it

### 2. Java shell scripting
The second way is almost the same as the first but two things have changed:
- The package is replaced with a shebang at the first line
- The filename is changed to anything but `*.java` since the `#!` is not valid
  java (it can be named without an extension or something like `.jsh`)

```java
#!/usr/bin/env -S java --source 17

// <stdlib imports hidden>

// First class name does not have to match filename
class Script {
    public static void main(String[] args) throws IOException {
        var configJson = new String(Files.readAllBytes(Path.of(args[0])));
        var jsonParser = new JsonParser(configJson);
        var testPort = jsonParser.get("deployment", "test", "port");
        System.out.println("test env at port: " + port);
    }
}

// Paste the classes you need from Lite Libs at the end, no access modifier
class JsonParser {
    ...
}
```
Make sure it is executable first:

    $ chmod +x script.jsh

Run it like this:

    $ ./script.jsh config.json

**Pros**
- Much more elegant way of running it
- You have control over the java flags, like `--class-path` and `--enable-preview`

**Cons**
- Only partial support in IntelliJ for java shell scripts, no debugging
- Must be run with `java --source 17 script.jsh config.json` on Windows systems
  without a *nix environment that recognises the shebang

__A good idea is to develop your script as plain java scripting, and then add the
shebang and rename it when it is time to distribute it as a java shell script.__

## Caveats
Optimizing for brevity is not usually a desired metric, but is done here to not
become an overwhelming majority of a script as no one wants to paste a 1000 line
class into their 100 line script — and as a fun exercise in making java (known
for its verbosity) short and "script-like".

Packing so much functionality into as few lines as possible effects readability
so just think of it as ascii jar snippets and suppress your clean code review
instincts, or simply reformat the code to your taste. At least there are plenty
of unit tests.

Also, the parsers may not be 100% compliant with the specs. Check the unit
tests to see if they are good enough for your task (or improve them if you are
so inclined). If you have a proper java project where space constraints is not
an issue, just add a dependency to something else since it is likely better.

## Background

> But isn't java an extremely poor choice as a scripting language?

The single-file source-code launcher capability was added to lower the threshold
for beginners trying to get their first java programs running,
but at the same time a fast and statically typed scripting language was born.
Java has also improved significantly over the last decade in case you didn't
notice and will probably improve more in the scripting area as well.

The concept is rather new and I have only made a few such scripts. But on every
occasion I needed to get commandline arguments in a more sophisticated way than
just `args[0]`, so each time a slightly different approach to parsing them was
taken while cursing java for not having such a basic feature included.

That got me thinking of what more common functionality was missing from the
standard library, and for fun I started with a tiny JSON parser. Together with
the latest iteration of the commandline parser, Lite Libs had started.

__Will java scripting take off? Hard to say, but if you know java but not bash
or python very well, it is certainly an option to experiment with.__
