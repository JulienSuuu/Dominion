import {Card} from "./Card";
import {LogEvent} from "./Log";
import {RankingEntry} from "./Classement";

export interface AppState {
    gameId: string | null;
    view: string | null;
    itemChange: ItemChangeEvent[];
    onCardChange: OnCardChangeEvent[];
    logChange: LogEvent[];
    ranking: RankingEntry[];
}

export interface ItemChangeEvent {
    id: string;
    item: string;
    old: number;
    new: number;
    delta: number;
}

export interface OnCardChangeEvent {
    type: string;
    location: string;
    playerId: string;
    supplyName: string;
    count: number;
    cards: string[];
    topCard: Card;
}
