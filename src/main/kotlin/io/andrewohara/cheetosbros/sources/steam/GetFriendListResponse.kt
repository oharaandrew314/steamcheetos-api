package io.andrewohara.cheetosbros.sources.steam

data class GetFriendListResponse(
        val friendslist: FriendsList
){
    data class FriendsList(
            val friends: Collection<Friend>
    )

    data class Friend(
            val steamid: String,
            val relationship: String, // known values are "friend"
            val friend_since: Long
    )
}