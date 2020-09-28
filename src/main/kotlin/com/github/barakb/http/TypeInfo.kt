package com.github.barakb.http

import java.lang.reflect.ParameterizedType
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.typeOf

typealias Type = java.lang.reflect.Type

@PublishedApi
internal open class TypeBase<T>

@OptIn(ExperimentalStdlibApi::class)
inline fun <reified T> typeInfo(): TypeInfo {
    val base = object : TypeBase<T>() {}
    val superType = base::class.java.genericSuperclass!!

    val reifiedType = (superType as ParameterizedType).actualTypeArguments.first()!!
    return TypeInfo(T::class, reifiedType, typeOf<T>())
}

@Suppress("unused")
internal fun Any.instanceOf(type: KClass<*>): Boolean = type.java.isInstance(this)


data class TypeInfo(
        val type: KClass<*>,
        val reifiedType: Type,
        val kotlinType: KType? = null
)

