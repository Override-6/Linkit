/*
 *  Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *  This code is free software; you can only use it for personal uses, studies or documentation.
 *  You can download this source code, and modify it ONLY FOR PERSONAL USE and you
 *  ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 *
 *  Please contact maximebatista18@gmail.com if you need additional information or have any
 *  questions.
 */

package fr.linkit.engine.local.system.fsa.remote

import fr.linkit.api.connection.network.cache.repo.SharedObject
import fr.linkit.api.connection.network.cache.repo.annotations.Hidden
import fr.linkit.api.connection.{ConnectionContext, ExternalConnection}
import fr.linkit.api.local.concurrency.workerExecution
import fr.linkit.api.local.system.fsa.{FileAdapter, FileSystemAdapter}
import fr.linkit.engine.connection.network.cache.repo.CloudPuppetRepository
import fr.linkit.engine.local.system.fsa.AbstractFileSystemAdapter
import fr.linkit.engine.local.system.fsa.io.{IOFileAdapter, IOFileSystemAdapter}
import fr.linkit.engine.local.system.fsa.nio.{NIOFileAdapter, NIOFileSystemAdapter}

import java.io.{InputStream, OutputStream}
import java.net.URI

@SharedObject
class RemoteFileSystemAdapter private(delegateFSA: AbstractFileSystemAdapter,
                                      sharedAdapters: CloudPuppetRepository[FileAdapter]) extends AbstractFileSystemAdapter {

    override def createAdapter(path: String): FileAdapter = {
        val adapter = delegateFSA.createAdapter(path)
        sharedAdapters.postObject(adapter.hashCode(), adapter)
    }

    override def createAdapter(uri: URI): FileAdapter = {
        val adapter = delegateFSA.createAdapter(uri)
        sharedAdapters.postObject(adapter.hashCode(), adapter)
    }

    override def createDirectories(fa: FileAdapter): Unit = delegateFSA.createDirectories(fa)

    override def create(fa: FileAdapter): Unit = delegateFSA.create(fa)

    override def list(fa: FileAdapter): Array[FileAdapter] = delegateFSA.list(fa)

    //FIXME Make returned object synchronised.
    @Hidden
    override def newInputStream(fa: FileAdapter): InputStream = {
        throw new UnsupportedOperationException("Use readNBytes instead")
    }

    //FIXME Make returned object synchronised.
    @Hidden
    override def newOutputStream(fa: FileAdapter): OutputStream = {
        throw new UnsupportedOperationException("Use readNBytes instead")
    }

    override def delete(fa: FileAdapter): Unit = delegateFSA.delete(fa)

    override def move(from: FileAdapter, to: FileAdapter): Unit = delegateFSA.move(from, to)

    delegateFSA match {
        case adapter: IOFileSystemAdapter =>
            val field = classOf[IOFileAdapter].getDeclaredField("fsa")
            sharedAdapters.addFieldReplacement(field, adapter)

        case adapter: NIOFileSystemAdapter =>
            val field = classOf[NIOFileAdapter].getDeclaredField("fsa")
            sharedAdapters.addFieldReplacement(field, adapter)

        case _ => throw new IllegalArgumentException(s"RemoteFileSystemAdapter is only compatible with IO and NIO FSAdapters. Provided ${delegateFSA.getClass}")
    }

}

object RemoteFileSystemAdapter {

    @workerExecution
    def connect(delegateFSA: Class[_ <: FileSystemAdapter],
                connection: ExternalConnection): FileSystemAdapter = {

        if (classOf[RemoteFileSystemAdapter].isAssignableFrom(delegateFSA))
            throw new IllegalArgumentException("Delegate FSA can't be a RemoteFileSystemAdapter.")

        val cache = connection
                .network
                .getEntity(connection.boundIdentifier)
                .get
                .entityCache

        val sharedAdapters = cache.getCache(27, CloudPuppetRepository[FileAdapter])
        val sharedSystems  = cache.getCache(28, CloudPuppetRepository[AbstractFileSystemAdapter])
        //println(s"sharedAdapters = ${sharedAdapters}")
        val remoteFSA      = sharedSystems.findObject(delegateFSA.getName.hashCode)
                .getOrElse(throw new UnsupportedOperationException(s"${delegateFSA.getSimpleName} not found for connection ${connection.boundIdentifier}"))
        new RemoteFileSystemAdapter(remoteFSA, sharedAdapters)
    }

    @workerExecution
    def open(delegateFSA: AbstractFileSystemAdapter, selfConnection: ConnectionContext): FileSystemAdapter = {
        if (delegateFSA.isInstanceOf[RemoteFileSystemAdapter])
            throw new IllegalArgumentException("Delegate FSA can't be a RemoteFileSystemAdapter.")

        val cache = selfConnection
                .network
                .connectionEntity
                .entityCache

        val sharedAdapters = cache.getCache(27, CloudPuppetRepository[FileAdapter])
        val sharedSystems = cache.getCache(28, CloudPuppetRepository[AbstractFileSystemAdapter])

        val puppetFSA = sharedSystems.postObject(delegateFSA.getClass.getName.hashCode, delegateFSA)
        new RemoteFileSystemAdapter(puppetFSA, sharedAdapters)
    }
}
