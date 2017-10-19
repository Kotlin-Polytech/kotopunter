package org.jetbrains.research.kotopunter.web.data

import org.jetbrains.research.kotopunter.util.Jsonable

object Auth {
    data class LoginResponse(val succeeded: Boolean = true, val error: String? = null): Jsonable
    data class SignUpResponse(val succeeded: Boolean = true, val error: String? = null): Jsonable
}