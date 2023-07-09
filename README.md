# LITE LIBS: a simple suite of java lightweight libraries

__These classes are intended to make java shell scripting a bit easier and more
self-contained.__

"Wait a minute" you say, "what's a java shell script?" Well, it is a supported
way since Java 11 to make limited source code java programs like how bash-,
perl- and python-scripts work. So if you have a java project, you may not need
to add a huge dependency to python just to have some git-hook or init/start-scripts.

While the java standard library is quite rich, there are some glaring omissions.
Of course there are amazing 3rd party libs to fill the void, but then you must
distribute the dependencies along with your script. Lite Libs makes it easier
since its classes are so small they can just be pasted into a script.

The following missing stdlib functionality is offered by Lite Libs:
* GNU/POSIX style command line parser (79 lines)
* JSON parser (59 lines)
* YAML parser (99 lines)

## Caveats
Optimizing for brevity is not usually a desired metric, but is done here to not
become an overwhelming majority of a script – no one wants to paste a 1000 line
class into their 100 line script. And as a fun exercise in making java (known
for its verbosity) short and "script-like".
Packing as much functionality as possible into as few lines as possible effects
readability so just think of it as non-binary jar snippets and suppress your
clean code review instincts. At least there are plenty of unit tests.

Another issue is that IntelliJ java shell script support is not complete yet.

Also, the parsers may not be 100% compliant with the specs. Check the unit
tests to see if they are good enough for your task (or improve them if you are
so inclined). If you have a proper java project where space constraints is not
an issue, just add a dependency to something else since it is likely better.

## Java shell script mini howto
1. Add a shebang to the top of the file. No package. There must be a `main()`
   in the first class.
```
#!/usr/bin/env -S java --source 17
class Example {
    public static void main(String[] args) {
        System.out.println("Hello world");
    }
}
// more classes can follow here
```
2. Save the file. The filename can't end with __.java__ _(since the #! isn't
   valid java)_, use something else like __.jsh__ or no extension.
3. Make the file executable `chmod +x script.jsh`
4. Run it with `./script.jsh` or `java --source 17 script.jsh`, no build or
   package stage needed.
5. IntelliJ still have problems with java shell scripts, like underlining
   some expressions with red lines even if nothing is wrong.

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
taken while cursing java for not having it included.

That got me thinking of what more common functionality was missing from the
standard library, and for fun I started with a tiny JSON parser. Together with
the latest iteration of the commandline parser, Lite Libs had started.

Will java scripting take off? Hard to say, but if you know java and not bash
or python, it is certainly an option to experiment with.
