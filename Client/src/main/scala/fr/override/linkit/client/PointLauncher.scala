/*
 * Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can only use it for personal uses, studies or documentation.
 * You can download this source code, and modify it ONLY FOR PERSONAL USE and you
 * ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 *
 * Please contact maximebatista18@gmail.com if you need additional information or have any
 * questions.
 */

package fr.`override`.linkit.client

import fr.`override`.linkit.api.local.concurrency.workerExecution
import fr.`override`.linkit.api.local.system.Reason
import fr.`override`.linkit.client.config.RelayPointBuilder

import java.net.InetSocketAddress
import java.nio.file.Paths
import java.util.Scanner

object PointLauncher {

    val PORT = 48484
    val SERVER_ADDRESS = new InetSocketAddress("192.168.1.19", PORT)
    val LOCALHOST = new InetSocketAddress("localhost", PORT)


    /**
     * @param args "--ide-run", used to determine if the application is running into IntelliJ or from an external context
     *             "--no-tasks", used to decide if the relay should not load any tasks from his RelayExtensions.
     * */
    def main(args: Array[String]): Unit = {
        val ideRun = args.contains("--ide-run")
        val loadExtensions = !(args.contains("--no-ext") || ideRun)

        print("say 'y' to connect to localhost : ")
        val scanner = new Scanner(System.in)
        val isLocalhost = scanner.nextLine().startsWith("y")
        val address = if (isLocalhost) LOCALHOST else SERVER_ADDRESS

        print("choose an identifier : ")
        val identifier = scanner.nextLine()

        launch(ideRun, loadExtensions, address, identifier)
    }

    def launch(ideRun: Boolean,
               loadExtensions: Boolean,
               address: InetSocketAddress,
               identifier0: String): Unit = {

        val relayPoint: RelayPoint = new RelayPointBuilder {
            enableExtensionsFolderLoad = loadExtensions
            extensionsFolder = getExtensionFolderPath

            override var serverAddress: InetSocketAddress = address
            override var identifier: String = identifier0
        }

        relayPoint runLater {
            val hooks = relayPoint.relayHooks
            hooks.connected.await()
            hooks.ready.await()
        } runLater {
            startRelay(relayPoint, ideRun)
        }
    }

    @workerExecution
    private def startRelay(relayPoint: RelayPoint, ideRun: Boolean): Unit = {
        relayPoint.start()

        Runtime.getRuntime.addShutdownHook(new Thread(() => relayPoint.runLater(relayPoint.close(Reason.INTERNAL))))

        if (ideRun && relayPoint.isOpen) {

            import fr.`override`.linkit.extension.controller.ControllerExtension
            import fr.`override`.linkit.extension.debug.DebugExtension
            import fr.`override`.linkit.extension.easysharing.EasySharing

            val loader = relayPoint.extensionLoader
            loader.loadExtensions(
                classOf[ControllerExtension],
                classOf[EasySharing],
                classOf[DebugExtension]
            )
        }
    }


    private def getExtensionFolderPath: String = {
        lazy val sourcePath = Paths.get(getClass.getProtectionDomain.getCodeSource.getLocation.toURI).getParent.toString
        System.getenv().get("COMPUTERNAME") match {
            case "PC_MATERIEL_NET" => "C:\\Users\\maxim\\Desktop\\Dev\\Linkit\\ClientSide\\RelayExtensions"
            //case "LORDI-N4SO7IERS" => "D:\\Users\\Maxime\\Desktop\\Dev\\Perso\\FileTransferer\\ClientSide\\RelayExtensions"
            case _ => sourcePath + "/RelayExtensions/"
        }
    }
}
