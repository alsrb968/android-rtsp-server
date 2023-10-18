package kr.co.makeitall.arduino

import java.io.*

/**
 * Represents the lines found in an [InputStream]. The lines are read
 * one at a time using [BufferedReader.readLine] and may be streamed
 * through an iterator or returned all at once.
 *
 *
 * This class does not handle any concurrency issues.
 *
 *
 * The stream is closed automatically when the for loop is done :)
 *
 * <pre>`for(String line : new LineReader(stream))
 * // ...
`</pre> *
 *
 *
 * An [IllegalStateException] will be thrown if any [IOException]s
 * occur when reading or closing the stream.
 *
 * @author    Torleif Berger
 * @license   http://creativecommons.org/licenses/by/3.0/
 * @see http://www.geekality.net/?p=1614
 */
class LineReader(reader: Reader = StringReader("")) : Iterable<String>, Closeable {
    private val reader: BufferedReader

    /**
     * Creates a new [LineReader].
     *
     *
     * Uses a [FileReader] to read the file.
     *
     * @param file Path to file with lines to read.
     * @throws FileNotFoundException
     */
    constructor(file: String) : this(FileReader(file)) {}

    /**
     * Creates an empty [LineReader] with no content.
     */
    init {
        this.reader = BufferedReader(reader)
    }

    /**
     * Closes the underlying stream.
     */
    @Throws(IOException::class)
    override fun close() {
        reader.close()
    }

    /**
     * Makes sure the underlying stream is closed.
     */
    @Throws(Throwable::class)
    protected fun finalize() {
        close()
    }

    /**
     * Returns an iterator over the lines remaining to be read.
     *
     *
     * The underlying stream is closed automatically once [Iterator.hasNext]
     * returns false, so closing it manually after using a for loop shouldn't be necessary.
     *
     * @return This iterator.
     */
    override fun iterator(): Iterator<String> {
        return LineIterator()
    }

    /**
     * Returns all lines remaining to be read and closes the stream.
     *
     * @return The lines read from the stream.
     */
    fun readLines(): Collection<String> {
        val lines: MutableCollection<String> = ArrayList()
        for (line in this) {
            lines.add(line)
        }
        return lines
    }

    private inner class LineIterator : MutableIterator<String> {
        private var nextLine: String? = null
        fun bufferNext(): String? {
            return try {
                reader.readLine().also { nextLine = it }
            } catch (e: IOException) {
                throw IllegalStateException("I/O error while reading stream.", e)
            }
        }

        override fun hasNext(): Boolean {
            val hasNext = nextLine != null || bufferNext() != null
            if (!hasNext) try {
                reader.close()
            } catch (e: IOException) {
                throw IllegalStateException("I/O error when closing stream.", e)
            }
            return hasNext
        }

        override fun next(): String {
            if (!hasNext()) throw NoSuchElementException()
            val result = nextLine
            nextLine = null
            return result!!
        }

        override fun remove() {
            throw UnsupportedOperationException()
        }
    }
}
