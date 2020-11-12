package fr.overridescala.vps.ftp.client

import java.net.InetSocketAddress
import java.util.Scanner

import fr.overridescala.vps.ftp.`extension`.controller.ControllerExtension
import fr.overridescala.vps.ftp.`extension`.fundamental.main.FundamentalExtension
import fr.overridescala.vps.ftp.`extension`.ppc.PPCExtension
import fr.overridescala.vps.ftp.api.Relay
import fr.overridescala.vps.ftp.api.utils.Constants

object Main {

    private val SERVER_ADDRESS = new InetSocketAddress("161.97.104.230", Constants.PORT)
    private val LOCALHOST = new InetSocketAddress("localhost", Constants.PORT)

    print("say 'y' to connect to localhost : ")
    private val scanner = new Scanner(System.in)
    private val isLocalhost = scanner.nextLine().startsWith("y")
    print("choose a identifier : ")
    private val identifier = scanner.nextLine()
    private val address = if (isLocalhost) LOCALHOST else SERVER_ADDRESS

    /**
     * @param args "--local-run", used to determine if the application is run into IntelliJ or from an external context
     * */
    def main(args: Array[String]): Unit = {
        val localRun = args.contains("--local-run")
        val relayPoint = new RelayPoint(address, identifier, localRun)
        if (localRun) {
            new ControllerExtension(relayPoint).main()
            new FundamentalExtension(relayPoint).main()
            new PPCExtension(relayPoint).main()
        }
        relayPoint.start()
    }

}