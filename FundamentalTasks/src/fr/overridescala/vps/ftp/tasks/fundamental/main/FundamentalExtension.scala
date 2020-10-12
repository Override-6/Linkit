package fr.overridescala.vps.ftp.tasks.fundamental.main

import fr.overridescala.vps.ftp.api.Relay
import fr.overridescala.vps.ftp.api.task.ext.TaskExtension
import fr.overridescala.vps.ftp.api.utils.Utils
import fr.overridescala.vps.ftp.tasks.fundamental.{CreateFileTask, DeleteFileTask, DownloadTask, FileInfoTask, PingTask, StressTestTask, UploadTask}

class FundamentalExtension(relay: Relay) extends TaskExtension(relay) {
    override def main(): Unit = {
        val completerHandler = relay.getTaskCompleterHandler
        completerHandler.putCompleter(UploadTask.TYPE, init => DownloadTask(Utils.deserialize(init.content)))
        completerHandler.putCompleter(DownloadTask.TYPE, init => UploadTask(Utils.deserialize(init.content)))
        completerHandler.putCompleter(FileInfoTask.TYPE, init => new FileInfoTask.Completer(new String(init.content)))
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
