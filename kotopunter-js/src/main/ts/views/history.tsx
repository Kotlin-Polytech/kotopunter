
import * as React from "react";
import {render} from "react-dom";
import * as Table from "react-bootstrap/lib/Table";
import * as Col from "react-bootstrap/lib/Col";
import {ListGroup, ListGroupItem} from "react-bootstrap";
import * as Pagination from "react-bootstrap/lib/Pagination";
import {run, sendAsync, setStateAsync} from "../util/common";
import * as QueryString from "querystring";
import {Generated} from "../util/kotopunter-generated";
import Address = Generated.Address;

import "less/kotopunter-bootstrap/bootstrap.less"
import {isNullOrUndefined} from "util";
import * as Button from "react-bootstrap/lib/Button";

type MapInfo = {
    map: string
}

type PunterInfo = {
    punter: number,
    team: string
}

type ScoreInfo = {
    scores: (PunterInfo & { score: number })[]
}

type Command = MapInfo | PunterInfo | ScoreInfo | {}

type GameInfo = {
    game: Command[]
}

type GameRecord = {
    id: number
    log: GameInfo
    time: number
}

function isMapInfo(c: Command | undefined): c is MapInfo { return !!c && c.hasOwnProperty('map') }
function isPunterInfo(c: Command | undefined): c is PunterInfo { return !!c && c.hasOwnProperty('team') }
function isScoreInfo(c: Command | undefined): c is ScoreInfo { return !!c && c.hasOwnProperty('scores') }

type HistoryProps = {}

type HistoryState = {
    page: number
    pageCount: number
    data: GameRecord[]
}

let PAGE_SIZE = 20;

class KotopunterPagination extends React.PureComponent<Pagination.PaginationProps> {
    constructor(props: Pagination.PaginationProps) {
        super(props)
    }

    render() {
        if(isNullOrUndefined(this.props.items) || this.props.items < 2) {
            return <div className={"vspace-10"} />
        }
        return <div className="text-center">
            <Pagination { ...this.props }/>
        </div>
    }
}

class History extends React.Component<HistoryProps, HistoryState> {

    constructor(props: HistoryProps) {
        super(props);

        let currentPage = parseInt(QueryString.parse(location.hash).page) || 0;

        this.state = {
            page: currentPage,
            pageCount: 0,
            data: []
        };

        run(async () => {
            await this.queryCount();
            await this.queryData();
        });
    }

    private hash = () => {
        let hash = QueryString.parse(location.hash);
        return "#" + QueryString.stringify({
            ...hash,
            currentPage: this.state.page
        });
    };

    queryCount = async () => {
        let msg = await sendAsync(Address.History.Count, {}) as {count: number};
        await setStateAsync(this,{pageCount: Math.ceil(msg.count / PAGE_SIZE)});
    };

    queryData = async () => {
        let msg = await sendAsync(Address.History.Page, {
            find: {},
            page: this.state.page,
            pageSize: PAGE_SIZE
        }) as GameRecord[];
        await setStateAsync(this,{data: msg});
    };

    setStateAndHash = async <K extends keyof HistoryState> (update: Pick<HistoryState, K>) => {
        history.replaceState(undefined, "History", this.hash());
        await setStateAsync(this, update);
    };

    mapOf = (status: GameRecord) => {
        let cmd = status.log.game.find(isMapInfo);
        if(isMapInfo(cmd)) {
            return cmd.map
        } else return "unknown"
    };

    scoreOf = (status: GameRecord) => {
        let cmd = status.log.game.find(isScoreInfo);
        if(isScoreInfo(cmd)) {
            return cmd
        } else return { scores: [] }
    };


    renderMapLink = (status: GameRecord) => {
        let map = this.mapOf(status);
        return <a target={"_blank"} href={`/legacy/map-viewer/index.html?map=../maps/${map}`}>{map}</a>
    };

    renderPunttvLink = (status: GameRecord) =>
        <a target={"_blank"} href={`/legacy/punttv/tv.html?game=/games/${status.id}.json`}>
            {"Watch this game with PuntTV!"}
        </a>;


    renderRow = (status: GameRecord) => <tr key={`game-${status.id}`}>
        <Col componentClass={'td'} md={3}>
            <strong>{`Id: ${status.id}`}</strong>
            <div>{this.renderMapLink(status)}</div>
            <div>{this.renderPunttvLink(status)}</div>
        </Col>
        <Col componentClass={'td'} md={9}>
            <ListGroup>
                {this.scoreOf(status).scores.map(scoring =>
                    <ListGroupItem key={`game-${status.id}-playa-${scoring.punter}`}>
                        {`${scoring.punter}: ${scoring.team}: `}
                        <strong>{scoring.score}</strong>
                    </ListGroupItem>
                )}
            </ListGroup>
        </Col>
    </tr>;

    onPageChanged = async (page: number) => {
        await this.setStateAndHash({ page: page });
        await this.queryData()
    };

    render() {
        return <div>
            <Button bsStyle={"link"} href={"/running"}>
                Currently running games
            </Button>
            <KotopunterPagination
                className="pagination"
                prev
                next
                first
                last
                ellipsis
                boundaryLinks
                items={this.state.pageCount}
                maxButtons={5}
                activePage={this.state.page + 1}
                onSelect={(e: any) => this.onPageChanged(e as number - 1)}/>
            <Table striped={true}>
                <tbody>
                {this.state.data.map(status => this.renderRow(status))}
                </tbody>
            </Table>
        </div>
    }

}

render(
    <History/>,
        document.getElementById('react-app')
);

