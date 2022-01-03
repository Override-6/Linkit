package fr.linkit.engine.internal.language.bhv.parse

trait ParserAction[X] {

    def parse(reader: BhvFileReader): X

}
