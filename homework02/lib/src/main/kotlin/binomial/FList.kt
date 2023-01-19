package binomial

/*
 * FList - реализация функционального списка
 *
 * Пустому списку соответствует тип Nil, непустому - Cons
 *
 * Запрещено использовать
 *
 *  - var
 *  - циклы
 *  - стандартные коллекции
 *
 *  Исключение Array-параметр в функции flistOf. Но даже в ней нельзя использовать цикл и forEach.
 *  Только обращение по индексу
 */
sealed class FList<T> : Iterable<T> {
    // размер списка, 0 для Nil, количество элементов в цепочке для Cons
    abstract val size: Int

    // пустой ли списк, true для Nil, false для Cons
    abstract val isEmpty: Boolean

    // получить список, применив преобразование
    // требуемая сложность - O(n)
    abstract fun <U> map(f: (T) -> U): FList<U>

    // получить список из элементов, для которых f возвращает true
    // требуемая сложность - O(n)
    abstract fun filter(f: (T) -> Boolean): FList<T>

    // свертка
    // требуемая сложность - O(n)
    // Для каждого элемента списка (curr) вызываем f(acc, curr),
    // где acc - это base для начального элемента, или результат вызова
    // f(acc, curr) для предыдущего
    // Результатом fold является результат последнего вызова f(acc, curr)
    // или base, если список пуст
    abstract fun <U> fold(base: U, f: (U, T) -> U): U

    // разворот списка
    // требуемая сложность - O(n)
    fun reverse(): FList<T> {
        tailrec fun rec(curState: FList<T>, last: FList<T>): FList<T> {
            if (last is Nil) return curState
            return rec(Cons((last as Cons).head, curState), last.tail)
        }
        return rec(nil(), this)
    }


    /*
     * Это не очень красиво, что мы заводим отдельный Nil на каждый тип
     * И вообще лучше, чтобы Nil был объектом
     *
     * Но для этого нужны приседания с ковариантностью
     *
     * dummy - костыль для того, что бы все Nil-значения были равны
     *         и чтобы Kotlin-компилятор был счастлив (он требует, чтобы у Data-классов
     *         были свойство)
     *
     * Также для борьбы с бойлерплейтом были введены функция и свойство nil в компаньоне
     */
    data class Nil<T>(private val dummy: Int = 0) : FList<T>() {

        override fun iterator(): Iterator<T> {
            class FListNilIterator : Iterator<T> {
                override fun hasNext(): Boolean = false

                override fun next(): T {
                    throw NoSuchElementException()
                }
            }
            return FListNilIterator()
        }

        override val size: Int = 0
        override val isEmpty: Boolean = true
        override fun <U> map(f: (T) -> U): FList<U> = nil()
        override fun filter(f: (T) -> Boolean): FList<T> = nil()
        override fun <U> fold(base: U, f: (U, T) -> U): U = base
    }

    data class Cons<T>(val head: T, val tail: FList<T>) : FList<T>() {

        override val size: Int = 1 + tail.size
        override val isEmpty: Boolean = false
        override fun iterator(): Iterator<T> {
            class FListConsIterator : Iterator<T> {
                var firstAppeal = true
                val tailIterator = tail.iterator()
                override fun hasNext(): Boolean = if (firstAppeal) true else tailIterator.hasNext()

                override fun next(): T {
                    return if (firstAppeal) {
                        firstAppeal = false
                        head
                    } else {
                        tailIterator.next()
                    }
                }
            }
            return FListConsIterator()
        }

        override fun <U> map(f: (T) -> U): FList<U> {
            tailrec fun rec(curState: FList<U>, last: FList<T>): FList<U> {
                if (last is Nil) return curState
                return rec(Cons(f((last as Cons).head), curState), last.tail)
            }
            return rec(nil(), this).reverse()
        }

        override fun filter(f: (T) -> Boolean): FList<T> {
            tailrec fun rec(curState: FList<T>, last: FList<T>): FList<T> {
                if (last is Nil) return curState
                val state = if (f((last as Cons).head)) Cons(last.head, curState) else curState
                return rec(state, last.tail)
            }
            return rec(nil(), this).reverse()
        }

        override fun <U> fold(base: U, f: (U, T) -> U): U {
            tailrec fun rec(acc: U, last: FList<T>): U {
                if (last is Nil) return acc
                return rec(f(acc, (last as Cons).head), last.tail)
            }
            return rec(base, this)
        }
    }

    companion object {
        fun <T> nil() = Nil<T>()
        val nil = Nil<Any>()
    }
}

// конструирование функционального списка в порядке следования элементов
// требуемая сложность - O(n)
fun <T> flistOf(vararg values: T): FList<T> {
    tailrec fun flistOfFromInd(index: Int, state: FList<T>, vararg values: T): FList<T> {
        if (index == -1) return state
        return flistOfFromInd(index - 1, FList.Cons(values[index], state), *values)
    }
    return flistOfFromInd(values.size - 1, FList.nil(), *values)
}
