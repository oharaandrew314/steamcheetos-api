package io.andrewohara.cheetosbros.sources.openxbl


data class SearchFriendResponse(
        val profileUsers: Collection<SearchFriendResult>
)

data class SearchFriendResult(
        val id: String,
        val hostId: String,
        val settings: Collection<SearchFriendSetting>,
        val isSponsoredUser: Boolean

)

data class SearchFriendSetting(val id: String, val value: String)