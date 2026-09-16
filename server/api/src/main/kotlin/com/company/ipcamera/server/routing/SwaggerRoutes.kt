package com.company.ipcamera.server.routing

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import mu.KotlinLogging
import java.io.File

private val logger = KotlinLogging.logger {}

fun Route.swaggerRoutes() {
    route("/openapi") {
        get(".yaml") {
            call.respondText(loadOpenApiSpec(), ContentType.parse("text/vnd.yaml"), HttpStatusCode.OK)
        }
        get(".json") {
            call.respondText(loadOpenApiSpec(), ContentType.Application.Json, HttpStatusCode.OK)
        }
    }

    get("/docs") {
        val html = """
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<title>IP-CSS API</title>
<link rel="stylesheet" href="https://unpkg.com/swagger-ui-dist@5/swagger-ui.css">
</head>
<body>
<div id="swagger-ui"></div>
<script src="https://unpkg.com/swagger-ui-dist@5/swagger-ui-bundle.js"></script>
<script src="https://unpkg.com/swagger-ui-dist@5/swagger-ui-standalone-preset.js"></script>
<script>
SwaggerUIBundle({
url: '/api/v1/openapi.yaml',
dom_id: '#swagger-ui',
deepLinking: true,
presets: [SwaggerUIBundle.presets.apis, SwaggerUIStandalonePreset],
plugins: [SwaggerUIBundle.plugins.DownloadUrl],
layout: "StandaloneLayout"
});
</script>
</body>
</html>
""".trimIndent()
        call.respondText(html, ContentType.Text.Html, HttpStatusCode.OK)
    }
}

private fun loadOpenApiSpec(): String {
    val classpathResource = object {}.javaClass.getResourceAsStream("/docs/api/openapi.yaml")
    if (classpathResource != null) return classpathResource.reader().readText()
    val filePath = File("docs/api/openapi.yaml")
    if (filePath.exists()) return filePath.readText()
    logger.warn { "OpenAPI spec not found, using embedded minimal spec" }
    return """openapi: 3.0.3
info:
  title: IP-CSS API
  version: 0.3.0
paths:
  /health:
    get:
      summary: Health check
      responses:
        '200':
          description: OK
""".trimIndent()
}
