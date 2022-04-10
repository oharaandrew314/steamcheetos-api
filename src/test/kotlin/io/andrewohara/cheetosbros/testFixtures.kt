package io.andrewohara.cheetosbros

import io.andrewohara.cheetosbros.sources.AchievementData
import io.andrewohara.cheetosbros.sources.GameData
import org.http4k.core.Uri


val satisfactoryData = GameData(
    id = "1337",
    name = "Satisfactory",
    displayImage = Uri.of("satisfactory")
)

val satisfactoryAchievementData = listOf(
    AchievementData(
        gameId = satisfactoryData.id,
        id = "ACH01",
        name = "Choo!",
        description = "Build a train locomotive",
        hidden = false,
        iconLocked = Uri.of("locked"),
        iconUnlocked = Uri.of("unlocked")
    ),
    AchievementData(
        gameId = satisfactoryData.id,
        id = "ACH02",
        name = "3.6 Roentgen",
        description = "Detonate a nobelisk on a nuclear power plant",
        hidden = true,
        iconLocked = Uri.of("locked"),
        iconUnlocked = Uri.of("unlocked")
    ),
    AchievementData(
        gameId = satisfactoryData.id,
        id = "ACH03",
        name = "You are feeling very sleepy",
        description = "Collect a Mercer Sphere",
        hidden = false,
        iconLocked = Uri.of("locked"),
        iconUnlocked = Uri.of("unlocked")
    )
)

val me3Data = GameData(
    id = "9001",
    name = "Mass Effect 3",
    displayImage = Uri.of("me3")
)

val me3AchievementData = listOf(
    AchievementData(
        gameId = me3Data.id,
        id = "liara_01",
        name = "It's a blue alien babe!",
        description = "Recruit Liara",
        hidden = false,
        iconLocked = Uri.of("locked"),
        iconUnlocked = Uri.of("unlocked")
    ),
    AchievementData(
        gameId = me3Data.id,
        id = "wrex_02",
        name = "The Dinasaurs are extinct",
        description = "Headbutt a Krogan",
        hidden = false,
        iconLocked = Uri.of("locked"),
        iconUnlocked = Uri.of("unlocked")
    ),
    AchievementData(
        gameId = me3Data.id,
        id = "end_01",
        name = "Did we win?",
        description = "Choose the colour of your ending",
        hidden = true,
        iconLocked = Uri.of("locked"),
        iconUnlocked = Uri.of("unlocked")
    ),
)

val godOfWarData = GameData(
    "88888",
    name = "God of War: Atreus",
    displayImage = Uri.of("gow4")
)

val godOfWarAchievement1Data = AchievementData(
    gameId = godOfWarData.id,
    id = "boy",
    name = "Boy",
    description = "Meet Atreus",
    hidden = false,
    iconLocked = Uri.of("locked"),
    iconUnlocked = Uri.of("unlocked")
)

val godOfWarAchievement2Data = AchievementData(
    gameId = godOfWarData.id,
    id = "sorry",
    name = "Don't be Sorry!",
    description = "Be Better",
    hidden = false,
    iconLocked = Uri.of("locked"),
    iconUnlocked = Uri.of("unlocked")
)

val factorioData = GameData(
    id = "99999",
    name = "Factorio",
    displayImage = Uri.of("factorio")
)

val factorioTrainCheeto = AchievementData(
    gameId = factorioData.id,
    id = "n00b",
    name = "Trains have right of way",
    description = "Get killed by your own locomotive",
    hidden = true,
    iconLocked = Uri.of("locked"),
    iconUnlocked = Uri.of("unlocked")
)