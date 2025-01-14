interface Shape : DimentionAware, SizeAware {
    fun getDimensionsProductStartsWithIndex(startsWith: Int): Int
}

/**
 * Реализация Point по умолчаению
 *
 * Должны работать вызовы DefaultShape(10), DefaultShape(12, 3), DefaultShape(12, 3, 12, 4, 56)
 * с любым количество параметров
 *
 * При попытке создать пустой Shape бросается EmptyShapeException
 *
 * При попытке указать неположительное число по любой размерности бросается NonPositiveDimensionException
 * Свойство index - минимальный индекс с некорректным значением, value - само значение
 *
 * Сама коллекция параметров недоступна, доступ - через методы интерфейса
 */
class DefaultShape(private vararg val dimensions: Int) : Shape {
    override val ndim: Int
        get() = dimensions.size

    override fun dim(i: Int): Int = dimensions[i]

    init {
        if (dimensions.isEmpty()) throw ShapeArgumentException.EmptyShapeException()
        dimensions.withIndex().forEach { (index, dim) ->
            if (dim <= 0) throw ShapeArgumentException.NonPositiveDimensionException(index, dim)
        }
    }

    override val size: Int = dimensions.reduce { acc, i -> acc * i }

    private fun getDimensionsProductCache(): IntArray {
        val arr = IntArray(ndim) { 1 }
        for (i in ndim - 2 downTo 0) {
            arr[i] = arr[i + 1] * dim(i + 1)
        }
        return arr
    }

    private val dimensionsProductCache: IntArray = getDimensionsProductCache()
    override fun getDimensionsProductStartsWithIndex(startsWith: Int): Int = dimensionsProductCache[startsWith]
}

sealed class ShapeArgumentException(reason: String = "") : IllegalArgumentException(reason) {
    class EmptyShapeException : ShapeArgumentException("Empty shape given")
    class NonPositiveDimensionException(val index: Int, val value: Int) :
        ShapeArgumentException("Non positive dimension: $value found in position: $index")
}
