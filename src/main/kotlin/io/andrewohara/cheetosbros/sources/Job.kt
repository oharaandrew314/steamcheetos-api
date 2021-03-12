package io.andrewohara.cheetosbros.sources

import io.andrewohara.cheetosbros.api.games.Uid
import java.util.*

data class Job(
    val userId: UUID,
    val platform: Platform,
    val gameId: Uid?
)