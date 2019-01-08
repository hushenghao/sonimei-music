package com.dede.sonimei.data.local

import android.media.MediaMetadataRetriever
import android.os.Build
import android.os.Parcel
import android.os.Parcelable
import com.dede.sonimei.data.BaseSong
import com.dede.sonimei.util.extends.applySchedulers
import com.mpatric.mp3agic.Mp3File
import io.reactivex.Observable

data class LocalSong(
        var songId: Long,
        override var title: String?,
        var author: String?,
        var album: String?,
        var duration: Long,
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

    constructor() : this(0, "", "", "", 0, "")

    override fun getName(): String {
        return "$title - $author"
    }

    fun picByteArray(): ByteArray? {
        var bytes = picByteArray.get()
        if (bytes != null) {
            return bytes
        }
        try {
            val metadataRetriever = MediaMetadataRetriever()
            metadataRetriever.setDataSource(path)
            bytes = metadataRetriever.embeddedPicture
            picByteArray = ByteArrayWeakReference(bytes)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return bytes
    }

    override fun loadLrc(): Observable<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Observable.create<String> {
                val mp3File = Mp3File(path)
                if (mp3File.hasId3v2Tag()) {
                    it.onNext(mp3File.id3v2Tag.lyrics ?: "")
                } else {
                    it.onNext("")
                }
            }.applySchedulers()
        } else {
            Observable.create<String> {
                it.onNext("")
            }
        }
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