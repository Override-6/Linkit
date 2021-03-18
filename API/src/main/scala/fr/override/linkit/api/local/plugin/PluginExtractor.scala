package fr.`override`.linkit.api.local.plugin

trait PluginExtractor {

    def extract(file: String): PluginLoader

    def extract(clazz: Class[_ <: Plugin]): PluginLoader

    def extractAll(folder: String): PluginLoader

    def extractAll(classes: Class[_ <: Plugin]*): PluginLoader

}
