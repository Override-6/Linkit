package fr.linkit.engine.connection.cache.`object`.description.language.parser

import java.io.InputStream
import java.nio.CharBuffer
import java.nio.file.{Files, Path}

import fr.linkit.api.local.resource.external.ResourceFile
import fr.linkit.engine.connection.cache.`object`.description.language.BehaviorFile

object BehaviorFileParser {

    def parse(resourceName: String): BehaviorFile = {
        val loader = Thread.currentThread().getContextClassLoader
        parse(loader.getResourceAsStream(resourceName))
    }

    def parse(filePath: Path): BehaviorFile = {
        parse(Files.newInputStream(filePath))
    }

    def parse(resource: ResourceFile): BehaviorFile = {
        parse(resource.getAdapter.newInputStream())
    }

    def parse(stream: InputStream): BehaviorFile = {
        parse(CharBuffer.wrap(new String(stream.readAllBytes())))
    }

    def parse(buff: CharBuffer): BehaviorFile = {

    }

}
