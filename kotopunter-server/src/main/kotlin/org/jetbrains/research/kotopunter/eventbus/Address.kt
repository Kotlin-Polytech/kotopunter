package org.jetbrains.research.kotopunter.eventbus

object Address {
    object Api {
        const val Dummy = "kotopunter.dummy"
    }

    object DB {
        fun create(entity: String) = "kotoed.db.$entity.create"
        fun find(entity: String) = "kotoed.db.$entity.find"
        fun batchCreate(entity: String) = "kotoed.db.$entity.create.batch"
        fun delete(entity: String) = "kotoed.db.$entity.delete"
        fun read(entity: String) = "kotoed.db.$entity.read"
        fun query(entity: String) = "kotoed.db.$entity.query"
        fun count(entity: String) = "kotoed.db.$entity.count"
    }

    object User {
        object Auth {
            const val SignUp = "kotopunter.user.auth.signup"
            const val Login = "kotopunter.user.auth.login"
            const val Info = "kotopunter.user.auth.info"
            const val HasPerm = "kotopunter.user.auth.hasPerm"

            const val SetPassword = "kotopunter.user.auth.setPassword"

            const val Restore = "kotopunter.user.auth.restore"
            const val RestoreSecret = "kotopunter.user.auth.restore.secret"
        }

        object OAuth {
            const val SignUp = "kotopunter.user.oauth.signup"
            const val Login = "kotopunter.user.oauth.login"
        }
    }

    object Dispatcher {
        const val Status = "kotopunter.dispatcher.status"
        const val Update = "kotopunter.dispatcher.update"
    }

    const val Schedule = "kotopunter.schedule"
}
