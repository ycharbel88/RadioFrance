package com.chy.radiofrance

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
