import {ItemChangeEvent} from "../../../interfaces/AppState";
import {UserPlayerState} from "../../../interfaces/Player";

export interface DeltaInfo{
    delta: number;
    id: number;
    rawInfo: ItemChangeEvent;
}

export interface StatValue{
    value: number;
    delta: DeltaInfo | null;
}

export interface PlayerStats {
    money: StatValue;
    action: StatValue;
    buy: StatValue;
    debt: StatValue;
    vp: StatValue;
    potion: StatValue;
    coffer: StatValue;
}

export interface PlayerProps {
    data: UserPlayerState;
    game_over: boolean;
    sendMessage: (message: string) => void;
}