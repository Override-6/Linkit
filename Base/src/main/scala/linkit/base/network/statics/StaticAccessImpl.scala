/*
 * Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR FILE HEADERS.
 *
 * This code is free software; you can USE it as you want.
 * You can download this source code, and modify it ONLY FOR PRIVATE USE but you
 * ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 * For any professional use, please contact me at overridelinkit@gmail.com.
 *
 * Please contact overridelinkit@gmail.com if you need additional information or have any
 * questions.
 */

package linkit.base.network.statics

import fr.linkit.api.application.resource.local.LocalFolder
import fr.linkit.api.gnom.cache.SharedCacheManager
import fr.linkit.api.gnom.cache.sync.contract.descriptor.ContractDescriptorData
import fr.linkit.api.gnom.network.statics.{StaticAccess, StaticAccessor, StaticsCaller}
import fr.linkit.api.gnom.persistence.context.Deconstructible
import fr.linkit.api.gnom.persistence.context.Deconstructible.Persist
import fr.linkit.api.internal.compilation.RuntimeCompilationException
import fr.linkit.api.internal.system.log.AppLoggers
import fr.linkit.engine.gnom.cache.sync.contract.description.SyncStaticsDescription
import StaticAccessImpl.CompilationRequestFactory
import fr.linkit.engine.internal.compilation.ClassCompilationRequestFactory
import fr.linkit.engine.internal.compilation.resource.CachedClassFolderResource

import scala.reflect.{ClassTag, classTag}

class StaticAccessImpl @Persist()(cacheId: Int, manager: SharedCacheManager, contract: ContractDescriptorData) extends StaticAccess with Deconstructible {

    private val cache    = manager.attachToCache(cacheId, DefaultConnectedStaticsCache.apply(contract))
    private val app      = manager.network.connection.getApp
    private val center   = app.compilerCenter
    private val resource = {
        val prop     = LinkitApplication.getProperty("compilation.working_dir") + "/Classes"
        val resource = app.getAppResources.getOrOpen[LocalFolder](prop)
        type R = CachedClassFolderResource[StaticsCaller]
        val entry = resource.getEntry
        import CachedClassFolderResource._
        entry
                .findRepresentation[R]()
                .getOrElse {
                    entry.attachRepresentation[R]()
                    entry.getRepresentation[R]()
                }
    }

    override def apply[S: ClassTag]: StaticAccessor = {
        val clazz  = classTag[S].runtimeClass
        val caller = cache.getOrSynchronize(clazz.getName.hashCode)(getMethodCaller(clazz))
        new StaticAccessorImpl(caller, clazz)
    }

    override def deconstruct(): Array[Any] = Array(cacheId, manager, contract)

    private def getMethodCaller(clazz: Class[_]): SyncStaticAccessInstanceCreator = {
        val staticsDesc = SyncStaticsDescription(clazz)
        val className   = staticsDesc.classPackage + "." + clazz.getSimpleName + "StaticsCaller"
        val mcClass     = resource.findClass[StaticsCaller](className, staticsDesc.parentLoader).getOrElse {
            genClass(staticsDesc)
        }.asInstanceOf[Class[StaticsCaller]]
        new SyncStaticAccessInstanceCreator(mcClass, Array(), clazz)
    }

    private def genClass(context: SyncStaticsDescription[_]): Class[_ <: StaticsCaller] = {
        val result = center.processRequest {
            AppLoggers.Compilation.info(s"Compiling Statics method caller for '${context.specs.mainClass}'...")
            CompilationRequestFactory.makeRequest(context)
        }
        AppLoggers.Compilation.info(s"Compilation done in ${result.getCompileTime} ms.")
        result.getClasses match {
            case clazz :: Nil => clazz
            case _ :: _ :: _  => throw new RuntimeCompilationException("Compiler center returned multiple classes.")
            case Nil          => throw new RuntimeCompilationException("Compiler center returned no result.")
        }
    }

}

object StaticAccessImpl {

    private final val Blueprint                 = new StaticsCallerClassBlueprint(getClass.getResourceAsStream("/generation/statics_caller.scbp"))
    private final val CompilationRequestFactory = new ClassCompilationRequestFactory[SyncStaticsDescription[_], StaticsCaller](Blueprint)
}