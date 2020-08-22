package io.andrewohara.cheetosbros.sources.openxbl

class GetAccountResponse(
        val profileUsers: Collection<ProfileUser>
) {

    data class ProfileUser(
            val id: String,
            val hostId: String,
            val settings: Collection<Setting>,
            val isSponsoredUser: Boolean
    ) {
        fun getAvatar() = settings.firstOrNull { it.id == "GameDisplayPicRaw" }?.value
        fun getGamertag() = settings.firstOrNull { it.id == "Gamertag" }?.value
    }

    data class Setting(
            val id: String,
            val value: String
    )
}

/*
{
	"profileUsers": [
		{
			"id": "2535413400000000",
			"hostId": "2535413400000000",
			"settings": [
				{
					"id": "GameDisplayPicRaw",
					"value": "http://images-eds.xboxlive.com/image?url=wHwbXKif8cus8csoZ03RW_ES.ojiJijNBGRVUbTnZKsoCCCkjlsEJrrMqDkYqs3MBhMLdvWFHLCswKMlApTSbzvES1cjEAVPrczatfOc0jR0Ss4zHEy6ErElLAY8rAVFRNqPmGHxiumHSE9tZRnlghsACzaoisWEww1VSUd9Sx0-&format=png"
				},
				{
					"id": "Gamerscore",
					"value": "6855"
				},
				{
					"id": "Gamertag",
					"value": "OpenXBL"
				},
				{
					"id": "AccountTier",
					"value": "Gold"
				},
				{
					"id": "XboxOneRep",
					"value": "GoodPlayer"
				},
				{
					"id": "PreferredColor",
					"value": "http://dlassets.xboxlive.com/public/content/ppl/colors/00003.json"
				},
				{
					"id": "RealName",
					"value": "David Regimbal"
				},
				{
					"id": "Bio",
					"value": "Xbox Live API"
				},
				{
					"id": "Location",
					"value": "United States"
				}
			],
			"isSponsoredUser": false
		}
	]
}
 */