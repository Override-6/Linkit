package fr.overridescala.vps.ftp.`extension`.fundamental

import fr.overridescala.vps.ftp.`extension`.fundamental._
import fr.overridescala.vps.ftp.api.Relay
import fr.overridescala.vps.ftp.api.task.ext.TaskExtension
import fr.overridescala.vps.ftp.api.utils.Utils

class FundamentalExtension(relay: Relay) extends TaskExtension(relay) {
    override def main(): Unit = {
        val completerHandler = relay.taskCompleterHandler
        completerHandler.putCompleter(UploadTask.TYPE, init => DownloadTask(Utils.deserialize(init.content)))
        completerHandler.putCompleter(DownloadTask.TYPE, init => UploadTask(Utils.deserialize(init.content)))
        completerHandler.putCompleter(DeleteFileTask.TYPE, init => new DeleteFileTask.Completer(new String(init.content)))
        completerHandler.putCompleter(PingTask.TYPE, _ => new PingTask.Completer())
        completerHandler.putCompleter(StressTestTask.TYPE, init => {
            val content = init.content
            new StressTestTask.StressTestCompleter(new String(content.slice(1, content.length)).toLong, content(0) != 1)
        })
        completerHandler.putCompleter(CreateFileTask.TYPE, init => {
            val content = init.content
            new CreateFileTask.Completer(new String(content.slice(1, content.length)), content(0) == 1)
        })
    }
}
