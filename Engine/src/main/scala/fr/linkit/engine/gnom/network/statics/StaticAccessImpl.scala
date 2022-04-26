package fr.linkit.engine.gnom.network.statics

import fr.linkit.api.gnom.cache.SharedCacheManager
import fr.linkit.api.gnom.cache.sync.contract.descriptor.ContractDescriptorData
import fr.linkit.api.gnom.cache.sync.invocation.MethodCaller
import fr.linkit.api.gnom.network.statics.{StaticAccess, StaticAccessor, StaticsCaller}
import fr.linkit.api.gnom.persistence.context.{Deconstructible, Persist}
import fr.linkit.api.internal.system.AppLogger
import fr.linkit.engine.application.LinkitApplication
import fr.linkit.engine.application.resource.external.LocalResourceFolder._
import fr.linkit.engine.gnom.cache.sync.contract.description.SyncStaticsDescription
import fr.linkit.engine.gnom.network.statics.StaticAccessImpl.CompilationRequestFactory
import fr.linkit.engine.internal.generation.compilation.factories.ClassCompilationRequestFactory
import fr.linkit.engine.internal.generation.compilation.resource.CachedClassFolderResource
import fr.linkit.engine.internal.generation.compilation.resource.CachedClassFolderResource.factory

import scala.reflect.{ClassTag, classTag}

class StaticAccessImpl @Persist()(cacheId: Int, manager: SharedCacheManager, contract: ContractDescriptorData) extends StaticAccess with Deconstructible {


    private val cache = manager.attachToCache(cacheId, DefaultSynchronizedStaticsCache.apply(contract))
    private val app      = cache.network.connection.getApp
    private val center   = app.compilerCenter
    private val resource = {
        val prop = LinkitApplication.getProperty("compilation.working_dir.classes")
        app.getAppResources.getOrOpenThenRepresent[CachedClassFolderResource[StaticsCaller]](prop)
    }

    override def apply[S: ClassTag]: StaticAccessor = {
        val clazz  = classTag[S].runtimeClass
        val caller = cache.getOrSynchronize(clazz.getName.hashCode)(getMethodCaller(clazz))
        new StaticAccessorImpl(caller, clazz)
    }

    override def deconstruct(): Array[Any] = Array(cacheId, manager, contract)

    private def getMethodCaller(clazz: Class[_]): SyncStaticAccessInstanceCreator = {
        val staticsDesc = SyncStaticsDescription(clazz)
        val mcClass     = resource.findClass[StaticsCaller](staticsDesc.classPackage + "." + clazz.getSimpleName + "StaticsCaller", staticsDesc.parentLoader).getOrElse {
            genClass(staticsDesc)
        }.asInstanceOf[Class[StaticsCaller]]
        new SyncStaticAccessInstanceCreator(mcClass, Array(), clazz)
    }

    private def genClass(context: SyncStaticsDescription[_]): Class[_ <: StaticsCaller] = {
        val result = center.processRequest {
            AppLogger.info(s"Compiling Statics method caller for class '${context.clazz.getName}'...")
            CompilationRequestFactory.makeRequest(context)
        }
        AppLogger.info(s"Compilation done in ${result.getCompileTime} ms.")
        result.getValue.get
    }

}

object StaticAccessImpl {

    private final val Blueprint                 = new StaticsCallerClassBlueprint(getClass.getResourceAsStream("/generation/statics_caller.scbp"))
    private final val CompilationRequestFactory = new ClassCompilationRequestFactory[SyncStaticsDescription[_], StaticsCaller](Blueprint)
}