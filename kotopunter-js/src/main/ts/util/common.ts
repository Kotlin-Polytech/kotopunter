import * as _ from "lodash";
import {defaultErrorHandler, eventBus} from "../eventBus";
import {EventbusMessage} from "./vertx";

export function sleep(ms: number): Promise<void> {
    return new Promise(resolve => setTimeout(resolve, ms));
}

export function identity<T>(arg: T): T {
    return arg;
}

export function doNothing() : void {}

export function run<T>(body: () => T): T { return body() }

export function typedKeys<T>(obj: T) {
    return Object.keys(obj) as Array<keyof T>
}

export function pick<T extends {}, K extends keyof T>(obj: T, keys: K[]): Pick<T, K> {
    return _.pick(obj, keys)
}

export function intersperse<T>(arr: Array<T>, inter: T) {
    return _.flatten(arr.map((a, i) => i != 0 ? [inter, a] : [a]))
}

export function sendAsync<Request, Response>(address: string, request: Request,
                                             onError?: typeof defaultErrorHandler): Promise<Response> {
    return eventBus.awaitOpen().then(_ =>
        eventBus.send<Request, Response>(address, request, undefined, onError)
    );
}

export function registerHandlerAsync(address: string, handler: (error: Error, message: EventbusMessage) => void) {
    return eventBus.awaitOpen().then(_ =>
        eventBus.registerHandler(address, {}, handler)
    )
}

export function setStateAsync<S, P, U extends keyof S>(self: React.Component<P, S>, state: Pick<S, U>): Promise<void> {
    return new Promise(((resolve, _) => {
        self.setState(state, () => resolve())
    }));
}

