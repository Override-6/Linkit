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

package fr.`override`.linkit.server

import fr.`override`.linkit.api.system.CloseReason
import fr.`override`.linkit.server.config.{AmbiguityStrategy, RelayServerBuilder}

import java.nio.file.Paths


object Main {
    def main(args: Array[String]): Unit = {
        println(s"running server with arguments ${args.mkString("'", ", ", "'")}")
        val ideRun = args.contains("--ide-run")
        val relayServer: RelayServer = new RelayServerBuilder {
            relayIDAmbiguityStrategy = AmbiguityStrategy.REJECT_NEW
            enableExtensionsFolderLoad = !ideRun
            extensionsFolder = getExtensionFolderPath
        }
        relayServer.runLater {
            relayServer.start()

            if (ideRun) {

                val loader = relayServer.extensionLoader
                loader.loadExtensions(
                    classOf[ControllerExtension],
                    classOf[EasySharing],
                    classOf[DebugExtension]
                )
            }
        }
        Runtime.getRuntime.addShutdownHook(new Thread(() => relayServer.runLater(relayServer.close(CloseReason.INTERNAL))))
    }

    private def getExtensionFolderPath: String = {
        val sourcePath = Paths.get(getClass.getProtectionDomain.getCodeSource.getLocation.toURI).getParent.toString
        System.getenv().get("COMPUTERNAME") match {
            case "PC_MATERIEL_NET" => "C:\\Users\\maxim\\Desktop\\Dev\\Linkit\\ClientSide\\RelayExtensions"
            case "LORDI-N4SO7IERS" => "D:\\Users\\Maxime\\Desktop\\Dev\\Perso\\FileTransferer\\ClientSide\\RelayExtensions"
            case _ => sourcePath + "/RelayExtensions/"
        }
    }

}
