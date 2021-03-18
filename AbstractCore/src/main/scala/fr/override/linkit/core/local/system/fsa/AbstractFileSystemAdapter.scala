package fr.`override`.linkit.core.local.system.fsa

import java.io.File

import fr.`override`.linkit.api.local.system.fsa.FileSystemAdapter

abstract class AbstractFileSystemAdapter extends FileSystemAdapter {

    private val adapters = mutable.Map.empty[String, FileAdapter]

    override def getAdapter(path: String): FileAdapter = {
        val formatted = path
                .replace('\\', File.separatorChar)
                .replace('/', File.separatorChar)
        if (adapters.contains(formatted))
            return adapters(formatted)

        val adapter = createAdapter(formatted)
        adapters.put(formatted, adapter)
        adapter
    }

    protected def createAdapter(path: String): FileAdapter

}
