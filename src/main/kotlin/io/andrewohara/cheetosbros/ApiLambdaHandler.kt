package io.andrewohara.cheetosbros

import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagementClientBuilder
import com.amazonaws.services.simplesystemsmanagement.model.GetParametersRequest
import io.andrewohara.cheetosbros.api.ServiceBuilder
import org.http4k.core.HttpHandler
import org.http4k.serverless.ApiGatewayV2LambdaFunction
import org.http4k.serverless.AppLoader

object ApiLambdaLoader: AppLoader {

    override fun invoke(env: Map<String, String>): HttpHandler {
        val steamParamName = env.getValue("STEAM_API_KEY_NAME")
        val publicPemName = env.getValue("PUBLIC_PEM_NAME")
        val privatePemName = env.getValue("PRIVATE_PEM_NAME")

        val ssm = AWSSimpleSystemsManagementClientBuilder.defaultClient()
        val request = GetParametersRequest()
            .withWithDecryption(true)
            .withNames(steamParamName, publicPemName, privatePemName)

        val params = ssm.getParameters(request).parameters

        val steamKey = params.first { it.name == steamParamName }.value
        val privateKey = params.first { it.name == privatePemName }.value
        val publicKey = params.first { it.name == publicPemName }.value
        val issuer = System.getenv("PEM_ISSUER")

        val services = ServiceBuilder.fromProps(env + mapOf("STEAM_API_KEY" to steamKey))

        val authDao = services.createJwtAuth(
            issuer = issuer,
            privateKey = privateKey,
            publicKey = publicKey
        )

        return services.createHttp(authDao)
    }
}

class ApiLambdaHandler: ApiGatewayV2LambdaFunction(ApiLambdaLoader)