Lite Libs CLI
=============
Tiny GNU/POSIX-style commandline option parser that has validation of options, parameters & arity, and built-in help.
No special string syntax or annotations to learn.

### Supported features
- Long options like `--check`, `--check file.txt` or `--check=file.txt`
- Short options like `-c`, `-cfile.txt`, `-c file.txt` or `-c=file.txt`
- Sub commands like `check`, `check file.txt` or `check --force file.txt`
- Positional parameters like `pos1 --check file.txt pos2 pos3`, i.e. any argument that is not an option/parameter
- Clustered short options like `-abcfile.txt`, `-abc file.txt` or `-abc=file.txt` are the same as `-a -b -c=file.txt`
- Multi-value options via multiple flags `--list=val1 --list=val2`, or comma separated `--list=val1,val2`
- To preserve commas in multi-values, skip equals sign: `--list 1,024 --list 2,048`
- Optional option parameters like `--check` or `--check=file.txt` (valid arity both with and without parameter)
- Parameters can be `-` to indicate stdin
- To use option-like parameters, avoid space separator: `--flag=--check`, `-f=--check`, `--offset=-5`, `-o-5` are OK
- End of options delimiter `--`, to handle positional parameters that starts with `-`
- Regex check for option parameters
- Default value if option is missing
- Help text with options and descriptions

### No built-in support for the following, which must be handled manually
- Required parameters and dependencies between different options
- Types other than (List of) String and Boolean, but with a regex in place i.e. `parseInt()` can safely be done
- Arity other than 0 (bool), 1 (key-value), 0..1 (optional option parameter) and 1..n (list)
- The usage string

### Optional features
- GNU style unique abbreviations, i.e. `--check` can be shortened to `-chek/--che/--ch/--c` as long as it does not
  clash with other options or abbreviations. So if `--change` is added, `--ch/--c` would no longer be allowed.
  Support can be added by inserting the following code block before the `argv.stream().takeWhile(...)` line in the
  private constructor:
  ```java
  opts.keySet().stream().filter(k -> k.startsWith("--")).flatMap(key -> IntStream.range(1, key.length() - 2)
                 .mapToObj(i -> Map.entry(key.substring(0, key.length() - i), opts.get(key))))
         .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (m1, m2) -> null)).entrySet().stream()
         .filter(kv -> kv.getValue() != null).forEach(kv -> opts.putIfAbsent(kv.getKey(), kv.getValue()));
  ```

EXAMPLE
-------
A simple example of a CLI with positional parameters and options. Options can't be named `'h'` or `"--help"` since that
is a built in option. An option without a short name is just a blank char, and an option without a long name is
just an empty string:
```java
public static void main(String[] args) {
    var cli = new Cli(List.of(args), """
            Watermark 1.0 (dummy example)
            Apply a watermark image file to other image files.
            Usage: watermark [options] <watermark> <image> [<image>...]
            """, "FILES",
            new Cli.Option('o', "--opacity=<percent>", Cli.Arity.ONE, "Opacity in %", "\\d\\d?|100", "Expected 0-100"),
            new Cli.Option(' ', "--skip=<ext>,...",    Cli.Arity.MANY, "Skip these file extensions"),
            new Cli.Option('b', "--backup[=<ext>]",    Cli.Arity.ZERO_ONE, "Make backup files, default extension .bak"),
            new Cli.Option('p', "",                    Cli.Arity.ZERO, "Use parallel execution")
    );
    if (cli.getAll("FILES").size() < 2)                                    // manual check of required arguments
        System.exit(cli.printError("Two or more files required"));

    String watermark = cli.getAll("FILES").remove(0);                      // first positional parameter
    int opacity = Integer.parseInt(cli.get("--opacity", "50"));            // default opacity is 50 if missing
    String bak = cli.has("--backup") ? cli.get("--backup", ".bak") : "";   // optional option param: <ext>, ".bak" or ""

    (cli.has("-p") ? cli.getAll("FILES").parallelStream() : cli.getAll("FILES").stream())
            .filter(img -> cli.getAll("--skip").stream().noneMatch(img::endsWith))
            .forEach(img -> applyWatermark(watermark, img, opacity, bak));
}
```
Running it with the following arguments will print an error to stderr since opacity is out of range, and then exit:

    $ ./watermark -po 128 --skip=.gif,.jpg --backup=.orig logo.png overview.png product.png

    Expected 0-100: --opacity=128
    Try --help for more

Running it with `--help` prints the help to stdout:

    $ ./watermark --help

    Watermark 1.0 (dummy example)
    Apply a watermark image file to other image files.
    Usage: watermark [options] <watermark> <image> [<image>...]

    Options:
      -o, --opacity=<percent>  Opacity in %
          --skip=<ext>,...     Skip these file extensions
      -b, --backup[=<ext>]     Make backup files, default extension .bak
      -p                       Use parallel execution
      -h, --help               Show this help

Note the built in `-h`/`--help` option last.

CONSTRUCTOR
-----------
There are three signatures for the constructor

1)  **Options only**, which like the other two signatures take the command line arguments, a help text and options. 
    
    Example of options only:  `./showcalendar --type julian`
    ```java
    var cli = new Cli(List.of(args), helpText, optionVerbose, optionType);                              // opts only
    var type = cli.get("--type");                 // --> "julian"
    ```
2)  **Positional parameters**, which also takes a string parameter ("FILES"), used as a key to access positional params.
    
    Example of positional parameters with options:  `./dos2unix --format unix file1.txt file2.txt`
    ```java
    var cli = new Cli(List.of(args), helpText, "FILES", optionVerbose, optionFormat);                   // positional
    var format = cli.get("--format");             // --> "unix"
    var files = cli.getAll("FILES");              // --> ["file1.txt", "file2.txt"]
    ```
3)  **Subcommands**, which instead takes a set of subcommand strings ("start", "stop"), also used as keys to access
    subcommand parameters.

    Example of subcommand and options:  `./server start --daemon /tmp/lockfiles`
    ```java
    var cli = new Cli(List.of(args), helpText, Set.of("start", "stop"), optionVerbose);                 // subcommand
    if (cli.has("start") {
        var subCli = new Cli(cli.getAll("start"), subHelpText, "DIR", optionForce, optionDaemon);       // positional
        var isDaemon = subCli.has("--daemon");    // --> true
        var dir = subCli.get("DIR", "/tmp");      // --> "/tmp/lockfiles" or default "/tmp"
    } else if (cli.has("stop")) {
        ...
    }
    ```

OPTIONS
-------
All options are optional, so the arity is only checked if an option is used. Examples of arity:
   ```java
   new Cli.Option('b', "--bool", Cli.Arity.ZERO, "Bool desc"),                          // 0 parameters
   new Cli.Option('k', "--key",  Cli.Arity.ONE,  "Key desc"),                           // 1 parameter
   new Cli.Option('l', "--list", Cli.Arity.MANY, "List desc"),                          // 1..n parameters
   new Cli.Option('z', "--zero-or-one", Cli.Arity.ZERO_ONE, "Optional option param"),   // 0..1 parameters
   ```
Example parameters in the option name are only used for display in the help text, and are not any kind of directive.
Anything following a non-option character (i.e. not alphanumeric or dash) is ignored when using the name for access.
   ```java
   new Cli.Option('k', "--key=<value>",        Cli.Arity.ONE,  "Key desc"),             // only "--key" is used
   new Cli.Option('l', "--list=<element>,...", Cli.Arity.MANY, "List desc"),            // only "--list" is used
   ```
The format of the help parameter is up to you, and completely optional to use as it is just for show:

    GNU style:  "--key=VALUE"
    GIT style:  "--key=<value>"
    CURL style: "--key <value>",  or any other way you want

Short options are a single char so the dash is automatically prepended, i.e. `'b'` becomes `"-b"`. It means you can't
have a short option like `-cp` for classpath since it would be treated as clustered short options. If no short option is
wanted, set it to a blank char `' '` (since it is a char it can't be empty or null).

If no long option name is wanted, the field can either be an empty string or used for short option help. Anything not
starting with `--` can be written and will be displayed instead of the short option name, so make sure to include it.

Example, only a short option and no long option. The following will display `-d<1..3>` in the help:
   ```java
   new Cli.Option('d', "-d<1..3>", Cli.Arity.ONE, "Debug level", "[1-3]", "Expected level to be 1-3")
   ```
Example, only a long option and no short option. Note how the regex validates what the example parameters state:
   ```java
   new Cli.Option(' ', "--debug[=FINE|INFO|WARN]", Cli.Arity.ZERO_ONE, "Debug level", "FINE|INFO|WARN", "Bad level")
   ```

GETTING PARAMETERS
------------------
Assume the following commandline arguments:

    ./script --bool  --key=val  --list=e1  --list=e2

The following are equivalent:

    ./script --bool  --key val  --list e1  --list e2      # optional '=' long separator removed
    ./script -b  -k val  -l e1  -l e2                     # using short options
    ./script -bk val  -l e1  -l e2                        # using clustered short options
    ./script -bkval  -le1  -le2                           # optional ' ' short separator removed
    ./script -bkval  -le1,e2                              # using comma separated list

Methods to get the values. Note, always use the long name ("--bool") if available, else the short name ("-b"):
   ```java
   cli.has("--bool")                   // will return true, or false if none given
   cli.get("--key")                    // will return "val", or null if none given
   cli.getAll("--list")                // will return ["e1", "e2"], or empty list if none given
   ```
An optional default can be used:
   ```java
   cli.get("--key", "default")         // will return "default" if none given
   cli.getAll("--list", "e3", "e4")    // will return ["e3", "e4"] if none given
   ```
Arity is not important for these calls:
   ```java
   cli.has("--list")                   // check if list is used, true or false
   cli.get("--list")                   // get first of list, "e1", or null if none given
   cli.getAll("--key")                 // will return ["val"], or empty list if none given
   ```
If the option parameter looks like an option, use '=' to be explicit. It is more likely that something option-like is
an option, valid or not, so better to give an error than silently consuming it.

    ./script --key --bool               # will give an error, "Expected 1 arg: --key"
    ./script --key=--bool               # okay

OPTIONAL OPTION PARAMETER
-------------------------
Optional option parameters (arity ZERO_ONE) have three states, unlike other arities that only have two states:

    ./script --zero-or-one param                          # 1) Option provided with parameter, like ONE and MANY
    ./script --zero-or-one                                # 2) Option provided without parameter, like ZERO
    ./script                                              # 3) No option provided

An example of how to get the three:
   ```java
   String zeroOrOne = cli.has("--zero-or-one") ? cli.get("--zero-or-one", "2 opt w/o param") : "3 no opt";
   ```
Hack: ZERO_ONE can be used for verbosity levels like -v, -vv, -vvv. Define this option:
   ```java
   Cli.Option('v', "-v, -vv, -vvv", Cli.Arity.ZERO_ONE, "Verbosity", "v{1,2}", "Must be -v, -vv or -vvv"),
   ```
Note how no long option name is used, so it is short option help instead. Get the verbosity as follows:
   ```java
   int verbosityLevel = cli.has("-v") ? cli.get("-v", "").length() + 1 : 0;      // 0-3 for none, -v, -vv, -vvv
   ```

POSITIONAL PARAMETERS
---------------------
Any arguments that are not options or option parameters will be positional parameters. Options and positional params
can be mixed. Positional params always have the arity MANY so you have to manually check the number of parameters:
   ```java
   var cli = new Cli(List.of(args), "usage: concat <src> [<src>...] <dest>", "FILES", option1, option2);
   if (cli.getAll("FILES").size() < 2)
       System.exit(cli.printError("One or more source files and one destination file expected"));
   String dest = cli.getAll("FILES").remove(cli.getAll("FILES").size() - 1);
   List<String> src = cli.getAll("FILES");
   ```
Positional parameters have priority over arity ZERO_ONE option parameters, unless using "=" (or if positionals are
not allowed):

    ./pos --zero-one param1 param2          # both param1 and param2 are positional parameters
    ./pos --zero-one=param1 param2          # only param2 is a positional parameter
    ./pos --zero-one=param1                 # no positional parameters, which is okay

SUBCOMMANDS
-----------
Anything following a subcommand is a subcommand parameter. If none of the subcommands appear, but parameters that do
not belong to any option exist, an error is printed. Subcommands always have the arity MANY and the normal use case
is to pass the parameters into a new cli parser instance to get the sub options (or sub sub commands):
   ```java
   var cli = new Cli(List.of(args), helpText, Set.of("add", "rm", "mv"), globalOption1, globalOption2);
   var opt1 = cli.get("--global-opt1");
   if (cli.has("add")) {
       var addCli = new Cli(cli.getAll("add"), addHelpText, "ADD_FILES", addOption3, addOption4);
       var filesToAdd = addCli.getAll("ADD_FILES");
       var opt3 = cli.get("--add-opt3");
   } else if (cli.has("rm")) {
       ...
   } else if (cli.has("mv")) {
       ...
   } else {
      // no sub command provided, may be okay, may be an error, it is up to you
   }
   ```
Note how there is a separate help for the subcommand, so `./script --help` and `./script add --help` are different.

Subcommands have priority over arity ZERO-ONE and ONE/MANY option parameters, unless using "=" (or if subcommands are
not allowed):

    ./sub --zero-one subcmd param2          # param2 is a subcmd parameter
    ./sub --zero-one=param1 subcmd param2   # param2 is a subcmd parameter
    ./sub --zero-one=subcmd param2          # param2 is an illegal subcommand, so an error is printed
    ./sub --zero-one param2                 # param2 is an illegal subcommand, so an error is printed
    ./sub --zero-one=param2                 # no subcommand provided, which is okay

    ./sub --one param1 subcmd param2        # param1 is --one parameter, param2 is subcmd parameter
    ./sub --one subcmd param2               # param2 is subcmd parameter but --one has nothing so an error is printed
    ./sub --one=subcmd param2               # param2 is an illegal subcommand, so an error is printed
    ./sub --one param1 param2               # param2 is an illegal subcommand, so an error is printed
    ./sub --one param1                      # param1 is --one parameter and no subcommand provided, which is okay

VALIDATIONS
-----------
Passing an option that is not declared will print an error. Passing an option like `--bool=y` or `--key=v1 --key=v2`
will also print an error since arity is wrong. If a regex is used and doesn't match, the optional error message or
the regex is printed as an error. 

You can also do manual checks and call `cli.printError("Your own error message")`, which
will print to stderr with an exit code. Or use `cli.printHelp()` which will only print help to stdout.

Notes
-----
The class is 79 lines and golfed to the edge of readability, ready to be pasted into a java shell script.
