package com.dede.sonimei.data.local

import android.media.MediaMetadataRetriever
import android.os.Parcel
import android.os.Parcelable
import com.dede.sonimei.data.BaseSong

data class LocalSong(
        val songId: Long,
        override val title: String?,
        val author: String?,
        val album: String?,
        val duration: Long,
        override var path: String?) : BaseSong(title, path), Parcelable {

    var pinyin: String = "#"

    private var picByteArray: ByteArrayWeakReference = ByteArrayWeakReference(null)

    constructor(parcel: Parcel) : this(
            parcel.readLong(),
            parcel.readString(),
            parcel.readString(),
            parcel.readString(),
            parcel.readLong(),
            parcel.readString()
    )

    override fun getName(): String {
        return "$title - $author"
    }

    fun picByteArray(): ByteArray? {
        var bytes = picByteArray.get()
        if (bytes != null) {
            return bytes
        }
        val metadataRetriever = MediaMetadataRetriever()
        metadataRetriever.setDataSource(path)
        bytes = metadataRetriever.embeddedPicture
        picByteArray = ByteArrayWeakReference(bytes)
        return bytes
    }

    companion object {
        @JvmField
        val CREATOR = object : Parcelable.Creator<LocalSong> {
            override fun createFromParcel(parcel: Parcel): LocalSong {
                return LocalSong(parcel)
            }

            override fun newArray(size: Int): Array<LocalSong?> {
                return arrayOfNulls(size)
            }
        }
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(songId)
        parcel.writeString(title)
        parcel.writeString(author)
        parcel.writeString(album)
        parcel.writeLong(duration)
        parcel.writeString(path)
    }

    override fun describeContents(): Int {
        return 0
    }
}