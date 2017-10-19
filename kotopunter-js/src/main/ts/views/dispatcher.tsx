
import * as React from "react";
import {render} from "react-dom";

import "less/kotopunter-bootstrap/bootstrap.less"
import * as Table from "react-bootstrap/lib/Table";

import {registerHandlerAsync, run, sendAsync, setStateAsync} from "../util/common";
import {Kotopunter} from "../util/kotoed-api";
import * as Row from "react-bootstrap/lib/Row";
import {ListGroup, ListGroupItem} from "react-bootstrap";
import {eventBus} from "../eventBus";
import {Link} from "react-router-dom";
import * as Col from "react-bootstrap/lib/Col";
import {ColProps} from "react-bootstrap/lib/Col";

type Int = number

type Player = { name?: string }
type Status = { port: Int, players: Array<Player>, map: string, phase: 'CREATED' | 'RUNNING' | 'ENDED' }

type NewPlayer = { game: Int,   name: string }
type GameCreated = { game: Int }
type GameStarted = { game: Int }
type GameFinished = { game: Int }
type UpdatePayload = NewPlayer | GameCreated | GameStarted | GameFinished
type Update = { type: string, payload: UpdatePayload }

type DispatcherState = { games: Array<Status> }
function DispatcherState() { return { games: [] } }

type DispatcherProps = {}

export class Dispatcher extends React.Component<DispatcherProps, DispatcherState> {
    constructor(props: DispatcherProps) {
        super(props);
        this.state = DispatcherState();

        run(async () => await registerHandlerAsync(Kotopunter.Address.Dispatcher.Update, this.updateAll));

        this.updateAll()
    }

    desc = (phase: Status['phase']) => {
        switch (phase) {
            case 'CREATED': return "Waiting for players...";
            case 'RUNNING': return "Game started";
            case 'ENDED': return "Game ended";
        }
    };

    updateAll = async () => {
        let stata = await sendAsync(Kotopunter.Address.Dispatcher.Status, {});
        await setStateAsync(this, { games: stata as Status[] })
    };

    renderRow = (status: Status) => <tr key={`game-${status.port}`}>
        <Col componentClass={'td'} md={3}>
            <strong>{`Port: ${status.port}`}</strong>
            <div><a target={"_blank"} href={`/legacy/map-viewer/index.html?map=../maps/${status.map}`}>{status.map}</a></div>
            <div>{this.desc(status.phase)}</div>
        </Col>
        <Col componentClass={'td'} md={9}>
            <ListGroup>
                {status.players.map((player, ix) =>
                    <ListGroupItem key={`playa-${status.port}-${ix}`}>{`${ix}: ${player.name || "free"}`}</ListGroupItem>)}
            </ListGroup>
        </Col>
    </tr>;

    render() {
        return <Table striped={true}>
            <tbody>
                {this.state.games.map(status => this.renderRow(status))}
            </tbody>
        </Table>
    }
}

render(
    <Dispatcher/>,
    document.getElementById('dispatcher-app')
);

