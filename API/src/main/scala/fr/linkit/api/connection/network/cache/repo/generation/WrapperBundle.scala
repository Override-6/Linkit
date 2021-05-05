package fr.linkit.api.connection.network.cache.repo.generation

import fr.linkit.api.connection.network.cache.repo.{PuppetDescription, Puppeteer}

case class WrapperBundle[S <: Serializable](puppeteer: Puppeteer[S], description: PuppetDescription[S])
