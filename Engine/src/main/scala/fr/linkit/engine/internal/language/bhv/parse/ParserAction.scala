package fr.linkit.engine.internal.language.bhv.parse

trait ParserAction {

    def parse(reader: BhvFileReader): Unit

}
