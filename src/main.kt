import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.file
import com.potomushto.statik.BlogEngine

class Hello : CliktCommand() {
    val rootPath by option(help = "Path to the root of the site").file(
        mustExist = true,
        canBeFile = false,
        canBeDir = true,
        mustBeReadable = true
    ).required()

    override fun run() {
        BlogEngine.run(rootPath.path)
    }
}

fun main(args: Array<String>) = Hello().main(args)
