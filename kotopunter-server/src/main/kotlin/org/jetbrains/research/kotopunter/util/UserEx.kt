package org.jetbrains.research.kotopunter.util

import io.vertx.ext.auth.User

suspend fun User.isAuthorisedAsync(authority: String) = vxa<Boolean> { this.isAuthorised(authority, it) }