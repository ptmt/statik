import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.int
import com.potomushto.statik.BlogEngine

class Hello : CliktCommand() {
    val rootPath by option(help = "Path to the root of the site").file(
        mustExist = true,
        canBeFile = false,
        canBeDir = true,
        mustBeReadable = true
    ).required()
    
    val watch by option(
        "--watch", "-w",
        help = "Watch mode: automatically rebuild the site when files change"
    ).flag()
    
    val port by option(
        "--port", "-p",
        help = "Port for the HTTP server (only used with --watch). Overrides config.json when provided."
    ).int()

    override fun run() {
        BlogEngine.run(rootPath.path, watch, port)
    }
}

fun main(args: Array<String>) = Hello().main(args)
