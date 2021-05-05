package fr.linkit.api.local.generation

class ValueInserter(builder: StringBuilder) {

    private var posShift = 0

    def insert(value: String, valueName: String, pos: Int): Unit = {
        val valueClauseLength = valueName.length + 2
        builder.replace(pos + posShift, valueClauseLength, value)
        posShift += value.length - valueClauseLength
    }

}
