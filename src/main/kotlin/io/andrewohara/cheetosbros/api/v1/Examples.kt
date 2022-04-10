package io.andrewohara.cheetosbros.api.v1

import org.http4k.core.Uri
import java.time.Instant

object Examples {

    val game = GameDtoV1(
        id = "123",
        name = "example game",
        displayImage = Uri.of("http://example.com/image.jpg"),
        achievementsCurrent = 1,
        achievementsTotal = 10,
        achievementsExpire = Instant.EPOCH
    )

    val achievement = AchievementDtoV1(
        id = "123",
        name = "Start the game",
        description = "Literally start the game",
        hidden = false,
        unlocked = true,
        unlockedOn = Instant.parse("2021-01-01T01:00:00Z"),
        iconLocked = Uri.of("http://cheetos.com/locked.png"),
        iconUnlocked = Uri.of("http://cheetos.com/unlocked.png")
    )

    val user = UserDtoV1(
        name = "xxCheetoHunter420xx",
        avatar = Uri.of("https://images.google.ca/slayer.jpg")
    )
}