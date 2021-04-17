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

package fr.linkit.core.local.system

import com.google.gson._
import fr.linkit.api.local.system
import fr.linkit.api.local.system.{Version, Versions}

import java.lang.reflect.Type

object AbstractCoreConstants {

    val Version: Version    = system.Version(name = "AC", code = "1.0.0", stable = false)
    val ImplVersionProperty = "LinkitImplementationVersion"

    val Gson: Gson = new GsonBuilder()
            .registerTypeAdapter(classOf[Versions], VersionsAdapter)
            .registerTypeAdapter(classOf[Version], VersionAdapter)
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
            val pattern = json.getAsJsonObject.get("version").getAsString
            system.Version(pattern)
        }

        override def serialize(src: Version, typeOfSrc: Type, context: JsonSerializationContext): JsonElement = {
            val element = new JsonObject()
            element.addProperty("version", src.toString)
            element
        }
    }

    object BufferAdapter extends JsonDeserializer[Version] with JsonSerializer[Version] {

        override def deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): Version = {

        }

        override def serialize(src: Version, typeOfSrc: Type, context: JsonSerializationContext): JsonElement = ???
    }

}
