/* to regenerate this file, run AddressExporter.kt */
export namespace Generated {

/*  see org/jetbrains/research/kotopunter/eventbus/Address.kt */
    export const Address = {
        Schedule: "kotopunter.schedule",
        Api: {
            Dummy: "kotopunter.dummy",
        },
        Dispatcher: {
            Status: "kotopunter.dispatcher.status",
            Update: "kotopunter.dispatcher.update",
        },
        User: {
            Auth: {
                HasPerm: "kotopunter.user.auth.hasPerm",
                Info: "kotopunter.user.auth.info",
                Login: "kotopunter.user.auth.login",
                Restore: "kotopunter.user.auth.restore",
                RestoreSecret: "kotopunter.user.auth.restore.secret",
                SetPassword: "kotopunter.user.auth.setPassword",
                SignUp: "kotopunter.user.auth.signup",
            },
            OAuth: {
                Login: "kotopunter.user.oauth.login",
                SignUp: "kotopunter.user.oauth.signup",
            },
        },
    };

/*  see org/jetbrains/research/kotopunter/web/UrlPattern.kt */
    export const UrlPattern = {
        CurrentGames: "/running",
        EventBus: "/eventbus/*",
        Index: "/",
        PuntTV: "/punttv/*",
        Star: "/*",
        Static: "/static/*",
        Auth: {
            Index: "/auth/login",
        },
    }

}
