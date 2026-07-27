package com.aimanager.core.common

import java.util.UUID

object IdGenerator {
    fun newId(): String = UUID.randomUUID().toString()
}
