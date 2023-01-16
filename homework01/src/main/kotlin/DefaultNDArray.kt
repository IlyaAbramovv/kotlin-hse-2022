interface NDArray : SizeAware, DimentionAware {
    /*
     * Получаем значение по индексу point
     *
     * Если размерность point не равна размерности NDArray
     * бросаем IllegalPointDimensionException
     *
     * Если позиция по любой из размерностей некорректна с точки зрения
     * размерности NDArray, бросаем IllegalPointCoordinateException
     */
    fun at(point: Point): Int

    /*
     * Устанавливаем значение по индексу point
     *
     * Если размерность point не равна размерности NDArray
     * бросаем IllegalPointDimensionException
     *
     * Если позиция по любой из размерностей некорректна с точки зрения
     * размерности NDArray, бросаем IllegalPointCoordinateException
     */
    fun set(point: Point, value: Int)

    /*
     * Копируем текущий NDArray
     *
     */
    fun copy(): NDArray

    /*
     * Создаем view для текущего NDArray
     *
     * Ожидается, что будет создан новая реализация  интерфейса.
     * Но она не должна быть видна в коде, использующем эту библиотеку как внешний артефакт
     *
     * Должна быть возможность делать view над view.
     *
     * In-place-изменения над view любого порядка видна в оригнале и во всех view
     *
     * Проблемы thread-safety игнорируем
     */
    fun view(): NDArray

    /*
     * In-place сложение
     *
     * Размерность other либо идентична текущей, либо на 1 меньше
     * Если она на 1 меньше, то по всем позициям, кроме "лишней", она должна совпадать
     *
     * Если размерности совпадают, то делаем поэлементное сложение
     *
     * Если размерность other на 1 меньше, то для каждой позиции последней размерности мы
     * делаем поэлементное сложение
     *
     * Например, если размерность this - (10, 3), а размерность other - (10), то мы для три раза прибавим
     * other к каждому срезу последней размерности
     *
     * Аналогично, если размерность this - (10, 3, 5), а размерность other - (10, 5), то мы для пять раз прибавим
     * other к каждому срезу последней размерности
     */
    fun add(other: NDArray)

    /*
     * Умножение матриц. Immutable-операция. Возвращаем NDArray
     *
     * Требования к размерности - как для умножения матриц.
     *
     * this - обязательно двумерна
     *
     * other - может быть двумерной, с подходящей размерностью, равной 1 или просто вектором
     *
     * Возвращаем новую матрицу (NDArray размерности 2)
     *
     */
    fun dot(other: NDArray): NDArray
}

/*
 * Базовая реализация NDArray
 *
 * Конструкторы должны быть недоступны клиенту
 *
 * Инициализация - через factory-методы ones(shape: Shape), zeros(shape: Shape) и метод copy
 */
class DefaultNDArray private constructor(
    number: Int = 0,
    private val shape: Shape,
    private var array: IntArray = IntArray(shape.size) { number }
) : NDArray {

    override val ndim: Int = shape.ndim
    override val size: Int = shape.size

    override fun dim(i: Int): Int = shape.dim(i)

    override fun at(point: Point): Int {
        if (point.ndim != this.ndim)
            throw NDArrayException.IllegalPointDimensionException(point.ndim, this.ndim)
        assertPointHasCorrectCoordinates(point)
        return array[convertPointToArrayIndex(point)]
    }

    override fun set(point: Point, value: Int) {
        if (point.ndim != this.ndim)
            throw NDArrayException.IllegalPointDimensionException(point.ndim, this.ndim)
        assertPointHasCorrectCoordinates(point)
        array[convertPointToArrayIndex(point)] = value
    }

    private fun assertPointHasCorrectCoordinates(point: Point) {
        for (i in 0 until point.ndim) {
            if (point.dim(i) > this.dim(i) || point.dim(i) < 0)
                throw NDArrayException.IllegalPointCoordinateException(i)
        }
    }

    private fun convertPointToArrayIndex(point: Point): Int =
        (0 until point.ndim)
            .fold(0) { acc, i -> acc + point.dim(i) * shape.getDimensionsProductStartsWithIndex(i) }

    override fun copy(): NDArray = DefaultNDArray(shape = this.shape, array = this.array.copyOf())

    override fun add(other: NDArray) {
        when {
            elementHasSameDimensions(other) ->
                for (i in array.indices) {
                    array[i] += other.at(convertIndexToPoint(i, other.ndim))
                }
            (this.ndim - 1 == other.ndim) && (0 until other.ndim).all { this.dim(it) == other.dim(it) } ->
                for (i in array.indices) {
                    array[i] += other.at(convertIndexToPoint(i / (size / other.size), other.ndim))
                }
            else -> throw NDArrayException.IllegalNDArrayDimensionException(
                "It's only possible to make \"add\" between NDArrays with dimensions differ by no more than one"
            )
        }
    }

    private fun elementHasSameDimensions(element: DimentionAware): Boolean {
        if (element.ndim != this.ndim) return false
        for (i in 0 until ndim) {
            if (this.dim(i) != element.dim(i)) return false
        }
        return true
    }

    private fun convertIndexToPoint(index: Int, ndim: Int): Point {
        var k = index
        val pointCoordinates = IntArray(ndim)
        for (i in 0 until ndim) {
            var dimProduct = this.shape.getDimensionsProductStartsWithIndex(i)
            if (ndim != this.ndim) dimProduct /= this.shape.dim(this.ndim - 1)
            pointCoordinates[i] = k / dimProduct
            k %= dimProduct
        }
        return DefaultPoint(*pointCoordinates)
    }

    override fun dot(other: NDArray): NDArray {

        if (this.ndim != 2)
            throw NDArrayException.IllegalNDArrayDimensionException("It's only possible to multiply 2d-arrays")
        if (other.ndim > 2 || other.dim(0) != this.dim(1))
            throw NDArrayException.IllegalNDArrayDimensionException(
                "It's only possible to multiply matrices if their dimensions are compatible"
            )
        val otherIsMatrix = other.ndim > 1
        val otherSecondDim = if (other.ndim > 1) other.dim(1) else 1
        val matrixProduct =
            if (otherIsMatrix) zeros(DefaultShape(dim(0), otherSecondDim)) else zeros(DefaultShape(dim(0)))
        for (i in 0 until dim(0)) {
            for (j in 0 until otherSecondDim) {
                var matrixProductValue = 0
                for (k in 0 until this.dim(1)) {
                    matrixProductValue += at(DefaultPoint(i, k)) *
                            if (other.ndim > 1) other.at(DefaultPoint(k, j)) else other.at(DefaultPoint(k))
                }
                val point = if (otherIsMatrix) DefaultPoint(i, j) else DefaultPoint(i)
                matrixProduct.set(point = point, value = matrixProductValue)
            }
        }
        return matrixProduct
    }

    override fun view(): NDArray = DefaultNDArrayView()

    internal inner class DefaultNDArrayView : NDArray by this@DefaultNDArray

    companion object {
        fun ones(shape: Shape): NDArray = DefaultNDArray(number = 1, shape = shape)
        fun zeros(shape: Shape): NDArray = DefaultNDArray(number = 0, shape = shape)
    }
}

sealed class NDArrayException(reason: String = "Unknown") : Exception(reason) {
    class IllegalPointCoordinateException(index: Int) :
        NDArrayException(
            "Point coordinate number $index is greater than $index'ths dimension of the NDArray or less than zero"
        )

    class IllegalPointDimensionException(pointNDim: Int, ndArrayNDim: Int) : NDArrayException(
        "The number of point coordinates should be equal to the dimension of the NDArray (now: $pointNDim != $ndArrayNDim)"
    )

    class IllegalNDArrayDimensionException(reason: String) : NDArrayException(reason)
}