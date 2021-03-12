package io.andrewohara.cheetosbros

import com.amazonaws.serverless.proxy.spark.SparkLambdaContainerHandler
import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestStreamHandler
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagementClientBuilder
import com.amazonaws.services.simplesystemsmanagement.model.GetParametersRequest
import io.andrewohara.cheetosbros.api.AuthBuilder
import io.andrewohara.cheetosbros.api.ServiceBuilder
import java.io.InputStream
import java.io.OutputStream

class ApiLambdaHandler: RequestStreamHandler {

    companion object {
        private val handler = SparkLambdaContainerHandler.getHttpApiV2ProxyHandler()
    }

    init {
        val steamParamName = System.getenv("STEAM_API_KEY_NAME")
        val publicPemName = System.getenv("PUBLIC_PEM_NAME")
        val privatePemName = System.getenv("PRIVATE_PEM_NAME")

        val ssm = AWSSimpleSystemsManagementClientBuilder.defaultClient()
        val request = GetParametersRequest()
            .withWithDecryption(true)
            .withNames(steamParamName, publicPemName, privatePemName)

        val params = ssm.getParameters(request).parameters

        val steamKey = params.first { it.name == steamParamName }.value
        val privateKey = params.first { it.name == privatePemName }.value
        val publicKey = params.first { it.name == publicPemName }.value
        val issuer = System.getenv("PEM_ISSUER")

        val services = ServiceBuilder.fromEnv(steamKey = steamKey)

        val auth = AuthBuilder.buildJwt(
            publicKeyIssuer = issuer,
            privateKey = privateKey,
            publicKey = publicKey,
            socialLinkDao = services.socialLinks,
            usersDao = services.usersDao
        )

        services.startSpark(auth, decodeQueryParams = true)
    }

    override fun handleRequest(input: InputStream, output: OutputStream, context: Context) {
        handler.proxyStream(input, output, context)
    }
}