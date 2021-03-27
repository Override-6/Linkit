package fr.`override`.linkit.extension.debug.commands

import fr.`override`.linkit.extension.controller.cli.CommandExecutor

import java.time.{Duration, LocalDateTime}

class NetworkCommand(network: Network) extends CommandExecutor {

    override def execute(implicit args: Array[String]): Unit = {
        val entities = network.listEntities
        val entitiesNames = entities.map(_.identifier).mkString(", ")
        val count = entities.size
        val upDate = network.startUpDate.toLocalDateTime
        val self = network.selfEntity
        val selfRemoteFragments = self.listRemoteFragmentControllers.map(_.nameIdentifier).mkString(", ")
        val duration = getDurationAsString(upDate)

        println()
        println(s"There are $count relays connected on the network.")
        println(s"Started at $upDate (Since: $duration)")
        println(s"Self entity : $self, enabled Remote Fragments : $selfRemoteFragments")
        println(s"    Status : ${self.getConnectionState}")
        println(s"All currently connected entities : $entitiesNames")
        println("--------------------------------------------------")
        println("For all entities : ")
        entities.foreach(genDescription)
    }

    private def genDescription(entity: NetworkEntity): Unit = {
        val name = entity.identifier
        val remoteFragments = entity.listRemoteFragmentControllers.map(_.nameIdentifier).mkString(", ")
        val apiVersion = entity.apiVersion
        val implVersion = entity.relayVersion
        val connectionDate = entity.connectionDate
        val connectionDuration = getDurationAsString(connectionDate.toLocalDateTime)

        println(s"-$name : ")
        println(s"    Enabled Remote Fragments : $remoteFragments")
        println(s"    Status : ${entity.getConnectionState}")
        println(s"    Connected at : $connectionDate (Since: $connectionDuration)")
        println(s"    $apiVersion")
        println(s"    $implVersion")
    }

    private def getDurationAsString(from: LocalDateTime): String = {
        var millis = Duration.between(from, LocalDateTime.now()).toMillis
        var seconds = (millis / 1000)
        var minutes = (seconds / 60)
        var hours = (minutes / 60)
        var days = (hours / 24)
        var months = (days / 31)
        var years = (months / 12)

        millis %= 1000
        seconds %= 60
        minutes %= 60
        hours %= 24
        days %= 31
        months %= 12

        s"$years y, $months m, $days d, $hours h, $minutes m, $seconds s and $millis ms"
    }


}
