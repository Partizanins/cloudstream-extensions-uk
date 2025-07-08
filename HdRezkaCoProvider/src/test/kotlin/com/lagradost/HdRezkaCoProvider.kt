package com.lagradost

import com.lagradost.cloudstreamtest.ProviderTester
import kotlinx.coroutines.runBlocking
import org.junit.Test

class HdRezkaCoProvider {
    @Test
    fun testProvider() = runBlocking {
        val providerTester = ProviderTester(HdRezkaCoProvider())
        providerTester.testAll()
    }
}