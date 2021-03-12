package io.andrewohara.cheetosbros

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagementClientBuilder
import com.amazonaws.services.simplesystemsmanagement.model.GetParameterRequest
import io.andrewohara.cheetosbros.api.ServiceBuilder
import io.andrewohara.cheetosbros.lib.PlatformConverter
import io.andrewohara.cheetosbros.lib.UidConverter
import io.andrewohara.cheetosbros.sources.*
import java.time.Instant
import java.util.*

class SyncLambdaHandler: RequestHandler<DynamodbEvent, Unit> {

    companion object {
        private val uidConverter = UidConverter()
        private val platformConverter = PlatformConverter()
    }

    private val service = let {
        val steamParamName = System.getenv("STEAM_API_KEY_NAME")

        val ssm = AWSSimpleSystemsManagementClientBuilder.defaultClient()
        val request = GetParameterRequest()
            .withName(steamParamName)
            .withWithDecryption(true)

        val steamApiKey = ssm.getParameter(request).parameter.value

        ServiceBuilder.fromEnv(steamApiKey).jobService
    }

    override fun handleRequest(input: DynamodbEvent, context: Context) {
        for (record in input.records) {
            val model = record.dynamodb.newImage ?: continue

            val jobId = model["jobId"]?.let { UUID.fromString(it.s) } ?: continue
            val job = Job(
                userId = model["userId"]?.let { UUID.fromString(it.s) } ?: continue,
                platform = model["platform"]?.let { platformConverter.unconvert(it.s) } ?: continue,
                gameId = model["gameId"]?.s?.let(uidConverter::unconvert)
            )

            service.execute(jobId, job, Instant.now())
        }
    }
}