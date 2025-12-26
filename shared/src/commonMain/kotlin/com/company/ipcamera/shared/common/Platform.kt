package com.company.ipcamera.shared.common

expect class Platform() {
    val platform: String
    val version: String
    val architecture: String
}

