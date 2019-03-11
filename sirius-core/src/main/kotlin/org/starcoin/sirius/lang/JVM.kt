package org.starcoin.sirius.lang

import java.lang.management.ManagementFactory

fun getJVMArgs(): List<String> {
    val runtimeMxBean = ManagementFactory.getRuntimeMXBean()
    return runtimeMxBean.inputArguments
}