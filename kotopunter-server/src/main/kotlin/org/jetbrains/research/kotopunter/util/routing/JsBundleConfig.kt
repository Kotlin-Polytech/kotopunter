package org.jetbrains.research.kotopunter.util.routing

data class JsBundleConfig(val jsBundleName: String?,
                          val cssBundleName: String? = jsBundleName,
                          val vendorJsBundleName: String? = "vendor",
                          val vendorCssBundleName: String? = vendorJsBundleName)