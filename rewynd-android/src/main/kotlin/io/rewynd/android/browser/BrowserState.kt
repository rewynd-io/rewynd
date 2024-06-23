package io.rewynd.android.browser

import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import androidx.navigation.NavType
import io.ktor.util.decodeBase64String
import io.ktor.util.encodeBase64
import io.rewynd.model.EpisodeInfo
import io.rewynd.model.Library
import io.rewynd.model.SeasonInfo
import io.rewynd.model.ShowInfo
import kotlinx.parcelize.Parceler
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.TypeParceler
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.serializer

@Serializable
sealed interface BrowserState {
    @Serializable
    @Parcelize
    class EpisodeState(@TypeParceler<EpisodeInfo, EpisodeParceler> val episodeInfo: EpisodeInfo) :
        BrowserState,
        Parcelable

    @Serializable
    @Parcelize
    class SeasonState(@TypeParceler<SeasonInfo, SeasonParceler> val seasonInfo: SeasonInfo) : BrowserState, Parcelable

    @Serializable
    @Parcelize
    class ShowState(@TypeParceler<ShowInfo, ShowParceler> val showInfo: ShowInfo) : BrowserState, Parcelable

    @Serializable
    @Parcelize
    class LibraryState(@TypeParceler<Library, LibraryParceler> val library: Library) : BrowserState, Parcelable

    @Serializable
    @Parcelize
    data object HomeState : BrowserState, Parcelable
}

class SerializableParceler<T : Any>(private val serializer: KSerializer<T>) : Parceler<T> {
    override fun T.write(parcel: Parcel, flags: Int) {
        parcel.writeString(Json.encodeToString(serializer, this))
    }

    override fun create(parcel: Parcel): T = Json.decodeFromString(serializer, requireNotNull(parcel.readString()))

    companion object {
        inline fun <reified T : Any> serializableParceler() =
            SerializableParceler<T>(EmptySerializersModule().serializer<T>())
    }
}

class LibraryParceler : Parceler<Library> {
    override fun Library.write(parcel: Parcel, flags: Int) {
        parcel.writeString(Json.encodeToString(this).encodeBase64())
    }

    override fun create(parcel: Parcel): Library =
        Json.decodeFromString(requireNotNull(parcel.readString()).decodeBase64String())
}

class ShowParceler : Parceler<ShowInfo> {
    override fun ShowInfo.write(parcel: Parcel, flags: Int) {
        parcel.writeString(Json.encodeToString(this).encodeBase64())
    }

    override fun create(parcel: Parcel): ShowInfo =
        Json.decodeFromString(requireNotNull(parcel.readString()).decodeBase64String())
}

class SeasonParceler : Parceler<SeasonInfo> {
    override fun SeasonInfo.write(parcel: Parcel, flags: Int) {
        parcel.writeString(Json.encodeToString(this).encodeBase64())
    }

    override fun create(parcel: Parcel): SeasonInfo =
        Json.decodeFromString(requireNotNull(parcel.readString()).decodeBase64String())
}

class EpisodeParceler : Parceler<EpisodeInfo> {
    override fun EpisodeInfo.write(parcel: Parcel, flags: Int) {
        parcel.writeString(Json.encodeToString(this).encodeBase64())
    }

    override fun create(parcel: Parcel): EpisodeInfo =
        Json.decodeFromString(requireNotNull(parcel.readString()).decodeBase64String())
}

inline fun <reified T> parcelableType(
    isNullableAllowed: Boolean = false,
    json: Json = Json,
) = object : NavType<T>(isNullableAllowed = isNullableAllowed) {
    override fun get(bundle: Bundle, key: String) = bundle.getString(key)
        ?.let { parseValue(it) }

    override fun parseValue(value: String): T = json.decodeFromString(value.decodeBase64String())

    override fun serializeAsValue(value: T): String = json.encodeToString(value).encodeBase64()

    override fun put(bundle: Bundle, key: String, value: T) = bundle.putString(key, serializeAsValue(value))
}
