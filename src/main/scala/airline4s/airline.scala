package airline4s

import io.airlift.airline._

/**
 * see what the airline sample looks like in scala
 * @author wassj
 */
object airline {
    def main(args: Array[String]) {

        val builder = Cli.builder[Runnable]("git")
                      .withDescription("the stupid content tracker")
                      .withDefaultCommand(classOf[Help])
                      .withCommands(classOf[Help], classOf[Add])

        builder.withGroup("remote")
        .withDescription("Manage set of tracked repositories")
        .withDefaultCommand(classOf[RemoteShow])
        .withCommands(classOf[RemoteShow], classOf[RemoteAdd])

        val gitParser: Cli[Runnable] = builder.build()

        gitParser.parse(args: _*).run();
    }

    trait Verbose extends Runnable {
        @Option(`type` = OptionType.GLOBAL, name = Array("-v"), description = "Verbose mode")
        var verbose: Boolean = false

        def run(): Unit = println(getClass.getSimpleName)
    }

    @Command(name = "add", description = "Add file contents to the index")
    class Add extends Verbose {
        @Arguments(description = "Patterns of files to be added")
        var patterns: Seq[String] = _

        @Option(name = Array("-i"), description = "Add modified contents interactively.")
        var interactive: Boolean = _
    }

    @Command(name = "show", description = "Gives some information about the remote <name>")
    class RemoteShow extends Verbose {
        @Option(name = Array("-n"), description = "Do not query remote heads")
        var noQuery: Boolean = false

        @Arguments(description = "Remote to show")
        var remote: String = _
    }

    @Command(name = "add", description = "Adds a remote")
    class RemoteAdd extends Verbose {
        @Option(name = Array("-t"), description = "Track only a specific branch")
        var branch: String = _

        @Arguments(description = "Remote repository to add")
        var remote: Seq[String] = _
    }
}


