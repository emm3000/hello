package com.emm.hello.analytics

class FakeProductAnalytics : ProductAnalytics {

    var events: List<ProductEvent> = emptyList()
        private set

    override fun track(event: ProductEvent) {
        events = events + event
    }
}
