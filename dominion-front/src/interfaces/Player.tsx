import {CardShadowZone, CardSummary} from "./Card";

export interface BasePlayerState {
    id : number;
    name : string;
    money?: number;
    debt?: number;
    potion?: number;
    coffer?: number;
    action?: number;
    buy?: number;
    vt?: number;
    draw: number;
    discard: number;
    in_play: CardSummary[];
    color: string;
    tokens: Record<string, unknown>;
}

export interface OpponentPlayerState extends BasePlayerState {
    playerId: string;
    hand: number;
}

export interface UserPlayerState extends BasePlayerState {
    client: string;
    hand: CardSummary[];
    tavern?: CardSummary[];
    shadowZone?: Record<string, CardShadowZone[]>;
}