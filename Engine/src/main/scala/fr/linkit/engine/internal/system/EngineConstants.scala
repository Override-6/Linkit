/*
 * Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR FILE HEADERS.
 *
 * This code is free software; you can only use it for personal uses, studies or documentation.
 * You can download this source code, and modify it ONLY FOR PERSONAL USE and you
 * ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 * For any professional use, please contact me at overridelinkit@gmail.com.
 *
 * Please contact overridelinkit@gmail.com if you need additional information or have any
 * questions.
 */

package fr.linkit.engine.internal.system

import com.google.gson._
import fr.linkit.api.internal.system
import fr.linkit.api.internal.system.{Version, Versions}
import java.lang.reflect.{ParameterizedType, Type}

import scala.collection.mutable.ArrayBuffer

object EngineConstants {

    val Version: Version    = system.Version(name = "Engine", code = "1.0.0", stable = false)
    val ImplVersionProperty = "LinkitImplementationVersion"

    val Gson: Gson = new GsonBuilder()
            .registerTypeAdapter(classOf[Versions], VersionsAdapter)
            .registerTypeAdapter(classOf[Version], VersionAdapter)
            .registerTypeAdapter(classOf[ArrayBuffer[Any]], ArrayBufferAdapter)
            .create()

    val UserGson: Gson = Gson.newBuilder()
            .setPrettyPrinting()
            .setLenient()
            .create()

    object VersionsAdapter extends InstanceCreator[Versions] {

        override def createInstance(`type`: Type): Versions = new DynamicVersions()
    }

    object VersionAdapter extends JsonDeserializer[Version] with JsonSerializer[Version] {

        override def deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): Version = {
            system.Version(json.getAsString)
        }

        override def serialize(src: Version, typeOfSrc: Type, context: JsonSerializationContext): JsonElement = {
            context.serialize(src.toString)
        }
    }

    object ArrayBufferAdapter extends JsonDeserializer[ArrayBuffer[Any]] with JsonSerializer[ArrayBuffer[Any]] {

        override def deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): ArrayBuffer[Any] = {
            val buffer    = new ArrayBuffer[Any]()
            val itemClass: Type = typeOfT match {
                case clazz: Class[_]             => clazz.getTypeParameters()(0).getBounds()(0)
                case typeImpl: ParameterizedType => typeImpl.getActualTypeArguments()(0)
            }
            json.getAsJsonArray
                    .forEach(element => {
                        buffer += context.deserialize(element, itemClass)
                    })
            buffer
        }

        override def serialize(src: ArrayBuffer[Any], typeOfSrc: Type, context: JsonSerializationContext): JsonElement = {
            val array = new JsonArray(src.size)
            src.foreach(any => {
                array.add(context.serialize(any))
            })
            array
        }
    }

}
