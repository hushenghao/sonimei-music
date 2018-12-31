package com.dede.sonimei.data.local

import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable
import java.lang.ref.WeakReference

class ByteArrayWeakReference(referent: ByteArray?) : Serializable {

    private var wr: WeakReference<ByteArray?> = WeakReference(referent)

    fun get(): ByteArray? {
        return wr.get()
    }

    /**
     * Write only content of WeakReference. WeakReference itself is not seriazable.
     * @param out
     * @throws java.io.IOException
     */
    @Throws(IOException::class)
    private fun writeObject(out: ObjectOutputStream) {
        out.writeObject(wr.get())
    }

    /**
     * Read saved content of WeakReference and construct new WeakReference.
     * @param in
     * @throws IOException
     * @throws ClassNotFoundException
     */
    @Throws(IOException::class, ClassNotFoundException::class)
    private fun readObject(`in`: ObjectInputStream) {
        wr = WeakReference(`in`.readObject() as? ByteArray?)
    }
}