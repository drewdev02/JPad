package org.andrew.jpad.core.dump

import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.util.Collections
import java.util.IdentityHashMap

class ObjectDumper(
    private val maxDepth: Int = 6,
    private val maxCollectionItems: Int = 1000
) {

    fun dump(target: Any?, title: String? = null): DumpValue {
        val visited = Collections.newSetFromMap(IdentityHashMap<Any, Boolean>())
        return dumpInternal(target, depth = 0, title = title, visited = visited)
    }

    private fun dumpInternal(
        obj: Any?,
        depth: Int,
        title: String?,
        visited: MutableSet<Any>
    ): DumpValue {
        if (obj == null) {
            return DumpValue.Primitive(null, null)
        }

        // Handle primitive / scalar types
        if (isScalar(obj)) {
            val typeName = obj.javaClass.simpleName
            return DumpValue.Primitive(obj, typeName)
        }

        // Cycle check
        if (!visited.add(obj)) {
            return DumpValue.Primitive("[Circular reference to ${obj.javaClass.simpleName}]", obj.javaClass.simpleName)
        }

        if (depth >= maxDepth) {
            return DumpValue.Primitive(obj.toString(), obj.javaClass.simpleName)
        }

        return when (obj) {
            is Map<*, *> -> dumpMap(obj, depth, title, visited)
            is Collection<*> -> dumpCollection(obj, depth, title, visited)
            is Array<*> -> dumpCollection(obj.toList(), depth, title, visited)
            is Iterable<*> -> dumpCollection(obj.toList(), depth, title, visited)
            is Sequence<*> -> dumpCollection(obj.take(maxCollectionItems).toList(), depth, title, visited)
            else -> dumpObject(obj, depth, title, visited)
        }
    }

    private fun dumpMap(
        map: Map<*, *>,
        depth: Int,
        title: String?,
        visited: MutableSet<Any>
    ): DumpValue {
        val typeName = title ?: "${map.javaClass.simpleName} (${map.size} items)"
        val columns = listOf("Key", "Value")
        val rows = map.entries.take(maxCollectionItems).map { entry ->
            mapOf(
                "Key" to dumpInternal(entry.key, depth + 1, null, visited),
                "Value" to dumpInternal(entry.value, depth + 1, null, visited)
            )
        }
        return DumpValue.Table(
            typeName = typeName,
            columns = columns,
            rows = rows,
            totalCount = map.size
        )
    }

    private fun dumpCollection(
        list: Collection<*>,
        depth: Int,
        title: String?,
        visited: MutableSet<Any>
    ): DumpValue {
        val typeName = title ?: "List (${list.size} items)"
        val sampleItems = list.take(maxCollectionItems)

        if (sampleItems.isEmpty()) {
            return DumpValue.Table(typeName = typeName, columns = listOf("Value"), rows = emptyList(), totalCount = 0)
        }

        // Check if all items are scalars
        val allScalars = sampleItems.all { it == null || isScalar(it) }
        if (allScalars) {
            val rows = sampleItems.map { item ->
                mapOf("Value" to dumpInternal(item, depth + 1, null, visited))
            }
            return DumpValue.Table(
                typeName = typeName,
                columns = listOf("Value"),
                rows = rows,
                totalCount = list.size
            )
        }

        // Elements are complex objects: collect properties / columns across items
        val properties = linkedSetOf<String>()
        val extractedRows = mutableListOf<Map<String, Any?>>()

        sampleItems.forEach { item ->
            if (item != null) {
                val props = extractProperties(item)
                properties.addAll(props.keys)
                extractedRows.add(props)
            } else {
                extractedRows.add(emptyMap())
            }
        }

        val columns = properties.toList().ifEmpty { listOf("Value") }
        val rows = extractedRows.map { propMap ->
            val rowValues = mutableMapOf<String, DumpValue>()
            columns.forEach { col ->
                val rawVal = propMap[col]
                rowValues[col] = dumpInternal(rawVal, depth + 1, null, visited)
            }
            rowValues
        }

        return DumpValue.Table(
            typeName = typeName,
            columns = columns,
            rows = rows,
            totalCount = list.size
        )
    }

    private fun dumpObject(
        obj: Any,
        depth: Int,
        title: String?,
        visited: MutableSet<Any>
    ): DumpValue {
        val typeName = title ?: obj.javaClass.simpleName
        val props = extractProperties(obj)

        if (props.isEmpty()) {
            return DumpValue.Primitive(obj.toString(), obj.javaClass.simpleName)
        }

        val entries = props.map { (key, value) ->
            key to dumpInternal(value, depth + 1, null, visited)
        }

        return DumpValue.KeyValueList(
            title = typeName,
            entries = entries
        )
    }

    private fun extractProperties(obj: Any): Map<String, Any?> {
        val clazz = obj.javaClass
        val result = linkedMapOf<String, Any?>()

        // 1. Check if Record (Java 16+)
        if (clazz.isRecord) {
            clazz.recordComponents.forEach { comp ->
                try {
                    val value = comp.accessor.invoke(obj)
                    result[comp.name] = value
                } catch (_: Throwable) {}
            }
            if (result.isNotEmpty()) return result
        }

        val declaredFieldNames = buildSet {
            var curr: Class<*>? = clazz
            while (curr != null && curr != Any::class.java) {
                curr.declaredFields.forEach { add(it.name) }
                curr = curr.superclass
            }
        }

        // 2. POJO / Kotlin / Java Bean Getters
        val getterMethods = clazz.methods.filter { method ->
            isGetter(method)
        }.sortedBy { it.name }

        getterMethods.forEach { method ->
            try {
                val propName = resolvePropertyName(method.name, declaredFieldNames)
                val value = method.invoke(obj)
                result[propName] = value
            } catch (_: Throwable) {}
        }

        // 3. Public fields if no getters found
        if (result.isEmpty()) {
            clazz.fields.filter { field ->
                !Modifier.isStatic(field.modifiers) && !field.name.startsWith("$")
            }.forEach { field ->
                try {
                    result[field.name] = field.get(obj)
                } catch (_: Throwable) {}
            }
        }

        // 4. Declared fields as fallback
        if (result.isEmpty()) {
            var currentClass: Class<*>? = clazz
            while (currentClass != null && currentClass != Any::class.java) {
                currentClass.declaredFields.filter { field ->
                    !Modifier.isStatic(field.modifiers) && !field.name.startsWith("$")
                }.forEach { field ->
                    try {
                        field.isAccessible = true
                        result.putIfAbsent(field.name, field.get(obj))
                    } catch (_: Throwable) {}
                }
                currentClass = currentClass.superclass
            }
        }

        return result
    }

    private fun isGetter(method: Method): Boolean {
        if (Modifier.isStatic(method.modifiers)) return false
        if (method.parameterCount != 0) return false
        if (method.returnType == Void.TYPE) return false
        if (method.name == "getClass" || method.name == "hashCode" || method.name == "toString") return false

        val isStandardGetter = method.name.startsWith("get") && method.name.length > 3
        val isBooleanGetter = method.name.startsWith("is") && method.name.length > 2 &&
                (method.returnType == java.lang.Boolean.TYPE || method.returnType == java.lang.Boolean::class.java)

        return isStandardGetter || isBooleanGetter
    }

    private fun resolvePropertyName(methodName: String, declaredFieldNames: Set<String>): String {
        if (methodName.startsWith("is") && declaredFieldNames.contains(methodName)) {
            return methodName
        }
        val raw = if (methodName.startsWith("get")) methodName.substring(3) else methodName.substring(2)
        val candidate = raw.replaceFirstChar { it.lowercase() }
        if (declaredFieldNames.contains(candidate)) {
            return candidate
        }
        if (methodName.startsWith("is") && declaredFieldNames.contains(methodName)) {
            return methodName
        }
        return candidate
    }

    private fun isScalar(obj: Any): Boolean {
        return obj is Number ||
                obj is String ||
                obj is Boolean ||
                obj is Char ||
                obj is Enum<*> ||
                obj is java.time.temporal.Temporal ||
                obj is java.util.Date ||
                obj is java.util.UUID ||
                obj is Class<*> ||
                obj is Throwable
    }
}
