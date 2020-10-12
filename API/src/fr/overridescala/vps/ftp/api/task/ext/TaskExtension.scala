package fr.overridescala.vps.ftp.api.task.ext

import fr.overridescala.vps.ftp.api.Relay

abstract class TaskExtension(protected val relay: Relay) {
    def main(): Unit
}