package com.disheveled.bills

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform