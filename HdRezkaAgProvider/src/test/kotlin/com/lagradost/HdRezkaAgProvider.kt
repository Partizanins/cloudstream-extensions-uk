package com.lagradost

import com.lagradost.cloudstreamtest.ProviderTester
import kotlinx.coroutines.runBlocking
import org.junit.Test

class HdRezkaAgProviderTest {
    @Test
    fun testProvider() = runBlocking {
        val providerTester = ProviderTester(HdRezkaAgProvider())
        providerTester.testAll()
    }
}