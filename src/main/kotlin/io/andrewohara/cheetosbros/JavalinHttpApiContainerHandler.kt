package io.andrewohara.cheetosbros

import com.amazonaws.serverless.proxy.AwsHttpApiV2SecurityContextWriter
import com.amazonaws.serverless.proxy.AwsProxyExceptionHandler
import com.amazonaws.serverless.proxy.internal.servlet.AwsHttpApiV2HttpServletRequestReader
import com.amazonaws.serverless.proxy.internal.servlet.AwsHttpServletResponse
import com.amazonaws.serverless.proxy.internal.servlet.AwsLambdaServletContainerHandler
import com.amazonaws.serverless.proxy.internal.servlet.AwsProxyHttpServletResponseWriter
import com.amazonaws.serverless.proxy.model.AwsProxyResponse
import com.amazonaws.serverless.proxy.model.HttpApiV2ProxyRequest
import com.amazonaws.services.lambda.runtime.Context
import io.javalin.Javalin
import java.util.concurrent.CountDownLatch
import javax.servlet.http.HttpServletRequest

class JavalinHttpApiContainerHandler(
    private val app: Javalin
): AwsLambdaServletContainerHandler<HttpApiV2ProxyRequest, AwsProxyResponse, HttpServletRequest, AwsHttpServletResponse>(
    HttpApiV2ProxyRequest::class.java,
    AwsProxyResponse::class.java,
    AwsHttpApiV2HttpServletRequestReader(),
    AwsProxyHttpServletResponseWriter(true),
    AwsHttpApiV2SecurityContextWriter(),
    AwsProxyExceptionHandler()
) {

    private var initialized = false

    override fun getContainerResponse(request: HttpServletRequest, latch: CountDownLatch): AwsHttpServletResponse {
        return AwsHttpServletResponse(request, latch)
    }

    override fun handleRequest(containerRequest: HttpServletRequest, containerResponse: AwsHttpServletResponse, lambdaContext: Context?) {
        if (!initialized) initialize()
        doFilter(containerRequest, containerResponse, app.servlet())
    }

    override fun initialize() {
        super.initialize()
        initialized = true
    }
}