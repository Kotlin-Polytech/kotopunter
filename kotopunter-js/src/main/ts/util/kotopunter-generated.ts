/* to regenerate this file, run AddressExporter.kt */
export namespace Generated {

/*  see org/jetbrains/research/kotopunter/eventbus/Address.kt */
    export const Address = {
        Schedule: "kotopunter.schedule",
        Api: {
            Dummy: "kotopunter.dummy",
        },
        DB: {
            count: (entity: string) => {
                return `kotoed.db.${entity}.count`;
            },
            create: (entity: string) => {
                return `kotoed.db.${entity}.create`;
            },
            delete: (entity: string) => {
                return `kotoed.db.${entity}.delete`;
            },
            find: (entity: string) => {
                return `kotoed.db.${entity}.find`;
            },
            read: (entity: string) => {
                return `kotoed.db.${entity}.read`;
            },
            readPage: (entity: string) => {
                return `kotoed.db.${entity}.readPage`;
            },
            update: (entity: string) => {
                return `kotoed.db.${entity}.update`;
            },
        },
        Dispatcher: {
            Status: "kotopunter.dispatcher.status",
            Update: "kotopunter.dispatcher.update",
        },
        History: {
            Count: "kotopunter.history.count",
            Page: "kotopunter.history.page",
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
        Games: "/games/:game.json",
        Index: "/",
        Legacy: "/legacy/*",
        MapView: "/viewMap/*",
        MapViewJSCore: "/viewMap/js-core/*",
        Maps: "/maps/*",
        PastGames: "/finished",
        PuntTV: "/punttv/*",
        Star: "/*",
        Static: "/static/*",
        Auth: {
            Index: "/auth/login",
        },
    }

}
