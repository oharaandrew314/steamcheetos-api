package io.andrewohara.cheetosbros.sources.openxbl

data class ListFriendsResponse(
        val people: Collection<Friend>
) {

    data class Friend(
            val xuid: String,
            val isFavorite: Boolean,
            val isFollowingCaller: Boolean,
            val isFollowedByCaller: Boolean
    )
}

/*
{
	"people": [
		{
			"xuid": "2535413400000000",
			"isFavorite": true,
			"isFollowingCaller": true,
			"isFollowedByCaller": true,
			"isIdentityShared": true,
			"addedDateTimeUtc": "2016-01-03T20:29:06.850124Z",
			"displayName": "OpenXBL",
			"realName": "David Regimbal",
			"displayPicRaw": "http://images-eds.xboxlive.com/image?url=8Oaj9Ryq1G1_p3lLnXlsaZgGzAie6Mnu24_PawYuDYIoH77pJ.X5Z.MqQPibUVTcS9jr0n8i7LY1tL3U7Aiafawv0hvLVTadAYM74jo2GRWfV4isnhJzSvaPwT_5QUO5&format=png",
			"useAvatar": false,
			"gamertag": "OpenXBL",
			"gamerScore": "6855",
			"xboxOneRep": "GoodPlayer",
			"presenceState": "Offline",
			"presenceText": "Offline",
			"presenceDevices": null,
			"isBroadcasting": false,
			"isCloaked": null,
			"isQuarantined": false,
			"suggestion": null,
			"recommendation": null,
			"titleHistory": null,
			"multiplayerSummary": {
				"InMultiplayerSession": 0,
				"InParty": 0
			},
			"recentPlayer": null,
			"follower": null,
			"preferredColor": {
				"primaryColor": "193e91",
				"secondaryColor": "101836",
				"tertiaryColor": "102c69"
			},
			"presenceDetails": null,
			"titlePresence": null,
			"titleSummaries": null,
			"presenceTitleIds": null,
			"detail": null,
			"communityManagerTitles": null,
			"socialManager": {
				"titleIds": [],
				"pages": []
			},
			"broadcast": [],
			"tournamentSummary": null,
			"avatar": null
		},
	],
	"recommendationSummary": null,
	"friendFinderState": null
}
 */