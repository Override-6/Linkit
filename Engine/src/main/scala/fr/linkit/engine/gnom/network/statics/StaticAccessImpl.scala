package fr.linkit.engine.gnom.network.statics

import fr.linkit.api.gnom.cache.sync.SynchronizedObjectCache
import fr.linkit.api.gnom.cache.sync.invocation.MethodCaller
import fr.linkit.api.gnom.network.statics.{StaticAccess, StaticAccessor}
import fr.linkit.api.internal.system.AppLogger
import fr.linkit.engine.application.LinkitApplication
import fr.linkit.engine.application.resource.external.LocalResourceFolder._
import fr.linkit.engine.gnom.cache.sync.contract.description.SyncStaticsDescription
import fr.linkit.engine.gnom.cache.sync.instantiation.Constructor
import fr.linkit.engine.gnom.network.statics.StaticAccessImpl.CompilationRequestFactory
import fr.linkit.engine.internal.generation.compilation.factories.ClassCompilationRequestFactory
import fr.linkit.engine.internal.generation.compilation.resource.CachedClassFolderResource
import fr.linkit.engine.internal.generation.compilation.resource.CachedClassFolderResource.factory

import scala.reflect.{ClassTag, classTag}

class StaticAccessImpl(cache: SynchronizedObjectCache[MethodCaller]) extends StaticAccess {

    private val app      = cache.network.connection.getApp
    private val center   = app.compilerCenter
    private val resource = {
        val prop = LinkitApplication.getProperty("compilation.working_dir.classes")
        app.getAppResources.getOrOpenThenRepresent[CachedClassFolderResource[MethodCaller]](prop)
    }

    override def of[S: ClassTag]: StaticAccessor = {
        val clazz  = classTag[S].runtimeClass
        val caller = cache.getOrSynchronize(clazz.getName.hashCode)(getMethodCaller(clazz))
        new StaticAccessorImpl(caller, clazz)
    }

    private def getMethodCaller(clazz: Class[_]): Constructor[MethodCaller] = {
        val staticsDesc = SyncStaticsDescription(clazz)
        val cl = resource.findClass[MethodCaller](staticsDesc.classPackage + "." + staticsDesc.className, staticsDesc.parentLoader).getOrElse {
            genClass(staticsDesc)
        }
        Constructor()(ClassTag(cl))
    }

    private def genClass(context: SyncStaticsDescription[_]): Class[_ <: MethodCaller] = {
        val result = center.processRequest {
            AppLogger.info(s"Compiling Sync Statics Class for class '${context.clazz.getName}'...")
            CompilationRequestFactory.makeRequest(context)
        }
        AppLogger.info(s"Compilation done in ${result.getCompileTime} ms.")
        result.getValue.get
    }

}

object StaticAccessImpl {
    private final val Blueprint                 = new StaticsCallerClassBlueprint(getClass.getResourceAsStream("/generation/sync_statics.scbp"))
    private final val CompilationRequestFactory = new ClassCompilationRequestFactory[SyncStaticsDescription[_], MethodCaller](Blueprint)
}