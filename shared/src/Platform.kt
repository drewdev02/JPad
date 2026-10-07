package org.andrew.jpad

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
