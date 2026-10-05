package com.emm.hello.analytics

interface ProductAnalytics {
    fun track(event: ProductEvent)
}
