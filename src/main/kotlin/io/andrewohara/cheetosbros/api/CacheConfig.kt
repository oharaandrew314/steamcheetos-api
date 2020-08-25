package io.andrewohara.cheetosbros.api

import java.time.Duration

data class CacheConfig(
        val library: Duration,
        val achievements: Duration,
        val achievementStatuses: Duration,
        val friends: Duration
)