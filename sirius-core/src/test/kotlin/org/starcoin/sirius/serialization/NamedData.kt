package org.starcoin.sirius.serialization

import kotlinx.serialization.Optional
import kotlinx.serialization.Serializable

@Serializable
data class NamedData(val name: String, @Optional val data: TestData? = null)
