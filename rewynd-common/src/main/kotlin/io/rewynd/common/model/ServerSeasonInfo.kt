package io.rewynd.common.model

import io.rewynd.model.SeasonInfo
import kotlinx.serialization.Serializable

@Serializable
data class ServerSeasonInfo(val seasonInfo: SeasonInfo, val libraryData: LibraryData)
