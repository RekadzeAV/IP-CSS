package com.company.ipcamera.server.routing

import com.company.ipcamera.server.cluster.ClusterService
import com.company.ipcamera.server.dto.ApiResponse
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.koin.ktor.ext.inject

@Serializable
data class ClusterNodeDto(val nodeId: String, val url: String?, val lastSeen: Long)

@Serializable
data class ClusterMeResponse(val nodeId: String, val clusterEnabled: Boolean)

/**
 * Маршруты кластера (4.2.1): идентификация узла для LB, список узлов для админа.
 */
fun Route.clusterRoutes() {
    val clusterService: ClusterService by inject()

    // Публичный: идентификатор текущего узла (для load balancer / sticky session)
    get("/cluster/me") {
        call.respond(
            HttpStatusCode.OK,
            ApiResponse(
                success = true,
                data = ClusterMeResponse(
                    nodeId = clusterService.getNodeId(),
                    clusterEnabled = clusterService.isEnabled()
                ),
                message = "Cluster node info"
            )
        )
    }

    // Админ: список узлов кластера (требует JWT и роль admin)
    authenticate("jwt-auth") {
        get("/cluster/nodes") {
            val principal = call.principal<io.ktor.server.auth.jwt.JWTPrincipal>()
            val isAdmin = principal?.payload?.getClaim("role")?.asString() == "admin"
            if (!isAdmin) {
                call.respond(HttpStatusCode.Forbidden, ApiResponse<Nothing>(success = false, data = null, message = "Admin only"))
                return@get
            }
            if (!clusterService.isEnabled()) {
                call.respond(
                    HttpStatusCode.OK,
                    ApiResponse(
                        success = true,
                        data = listOf(ClusterNodeDto(
                            nodeId = clusterService.getNodeId(),
                            url = clusterService.getCurrentNode().url,
                            lastSeen = clusterService.getCurrentNode().lastSeen
                        )),
                        message = "Cluster disabled; single node"
                    )
                )
                return@get
            }
            val peers = clusterService.getPeers()
            val all = (listOf(clusterService.getCurrentNode()) + peers)
                .distinctBy { it.nodeId }
                .map { ClusterNodeDto(it.nodeId, it.url, it.lastSeen) }
            call.respond(
                HttpStatusCode.OK,
                ApiResponse(success = true, data = all, message = "Cluster nodes retrieved")
            )
        }
    }
}
