package fr.linkit.api.gnom.persistence.incident

trait IncidentHandler {
    def handleUnknownClass(code: Int): Class[_]
}