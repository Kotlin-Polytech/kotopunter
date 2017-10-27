package org.jetbrains.research.kotopunter.eventbus

object Address {
    object Api {
        const val Dummy = "kotopunter.dummy"
    }

    object DB {
        fun create(entity: String) = "kotopunter.db.$entity.create"
        fun update(entity: String) = "kotopunter.db.$entity.update"
        fun find(entity: String) = "kotopunter.db.$entity.find"
        fun delete(entity: String) = "kotopunter.db.$entity.delete"
        fun read(entity: String) = "kotopunter.db.$entity.read"

        fun readPage(entity: String) = "kotopunter.db.$entity.readPage"
        fun count(entity: String) = "kotopunter.db.$entity.count"

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


    object History {
        const val Page = "kotopunter.history.page"
        const val Count = "kotopunter.history.count"
    }

    const val Schedule = "kotopunter.schedule"
}
