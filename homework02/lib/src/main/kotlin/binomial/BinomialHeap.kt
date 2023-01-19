package binomial

/*
 * BinomialHeap - реализация биномиальной кучи
 *
 * https://en.wikipedia.org/wiki/Binomial_heap
 *
 * Запрещено использовать
 *
 *  - var
 *  - циклы
 *  - стандартные коллекции
 *
 * Детали внутренней реализации должны быть спрятаны
 * Создание - только через single() и plus()
 *
 * Куча совсем без элементов не предусмотрена
 *
 * Операции
 *
 * plus с кучей
 * plus с элементом
 * top - взятие минимального элемента
 * drop - удаление минимального элемента
 */
class BinomialHeap<T : Comparable<T>> private constructor(private val trees: FList<BinomialTree<T>?>) :
    SelfMergeable<BinomialHeap<T>> {
    companion object {
        fun <T : Comparable<T>> single(value: T): BinomialHeap<T> = BinomialHeap(flistOf(BinomialTree.single(value)))
    }

    /*
     * слияние куч
     *
     * Требуемая сложность - O(log(n))
     */
    override fun plus(other: BinomialHeap<T>): BinomialHeap<T> {
        return BinomialHeap(merge(this.trees as FList.Cons, other.trees as FList.Cons, null))
    }

    private fun merge(
        element1: FList<BinomialTree<T>?>,
        element2: FList<BinomialTree<T>?>,
        element3: BinomialTree<T>? = null,
    ): FList<BinomialTree<T>?> {

        return when {
            element1 is FList.Cons && element2 is FList.Cons -> {
                val treeSum = nullableTreeSum(element1.head, element2.head)
                if (treeSum.second != null) {
                    FList.Cons(
                        element3,
                        merge(element1.tail, element2.tail, treeSum.second)
                    )
                } else {
                    val treeSum2 = nullableTreeSum(treeSum.first, element3)
                    FList.Cons(
                        treeSum2.first,
                        merge(element1.tail, element2.tail, treeSum2.second)
                    )
                }
            }
            element1 is FList.Cons && element2 is FList.Nil -> {
                val treeSum = nullableTreeSum(element1.head, element3)
                FList.Cons(
                    treeSum.first,
                    mergeSingle(treeSum.second, element1.tail)
                )
            }
            element1 is FList.Nil && element2 is FList.Cons -> {
                val treeSum = nullableTreeSum(element2.head, element3)
                FList.Cons(
                    treeSum.first,
                    mergeSingle(treeSum.second, element2.tail)
                )
            }
            else -> FList.Cons(element3, FList.nil())
        }
    }

    private fun mergeSingle(
        element1: BinomialTree<T>?,
        element2: FList<BinomialTree<T>?>,
    ): FList<BinomialTree<T>?> {
        return if (element2 is FList.Cons && (element2.head != null || element2.tail !is FList.Nil)) {
            val treeSum = nullableTreeSum(element1, element2.head)
            FList.Cons(treeSum.first, mergeSingle(treeSum.second, element2.tail))
        } else {
            val treeSum = nullableTreeSum(element1, null)
            if (treeSum.second == null) flistOf(treeSum.first)
            else flistOf(treeSum.first, treeSum.second)
        }
    }

    private fun nullableTreeSum(
        tree1: BinomialTree<T>?,
        tree2: BinomialTree<T>?,
    ): Pair<BinomialTree<T>?, BinomialTree<T>?> =
        when {
            tree1 != null && tree2 != null -> null to tree1 + tree2
            tree1 != null && tree2 == null -> tree1 to null
            tree1 == null && tree2 != null -> tree2 to null
            else -> null to null
        }

    /*
     * добавление элемента
     *
     * Требуемая сложность - O(log(n))
     */
    operator fun plus(elem: T): BinomialHeap<T> = plus(single(elem))

    /*
     * минимальный элемент
     *
     * Требуемая сложность - O(log(n))
     */
    fun top(): T {
        return trees.filter { it != null }.map { it!!.value }.minOrNull() ?: throw EmptyHeapException()
    }

    /*
     * удаление элемента
     *
     * Требуемая сложность - O(log(n))
     */
    fun drop(): BinomialHeap<T> {
        val treeWithMinElement =
            trees.filter { it != null }.minByOrNull { it!!.value } ?: throw EmptyHeapException()
        val minElement = treeWithMinElement.value
        val heap1 = BinomialHeap(trees.map { if (it?.value == minElement) null else it })
        return if (treeWithMinElement.order == 0) heap1
        else heap1.plus(BinomialHeap(treeWithMinElement.children.map { if (it.order < 0) null else it }.reverse()))
    }
}

class EmptyHeapException : RuntimeException("Empty heap is not supported")
