package com.lagradost

import com.lagradost.cloudstreamtest.ProviderTester

suspend fun main() {
    ProviderTester(UATuTFunProvider()).testAll()
}