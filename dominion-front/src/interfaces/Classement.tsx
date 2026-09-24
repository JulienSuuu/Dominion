import {CardSummary} from "./Card";

export interface GameOverPlayer {
    id: string | number;
    name: string;
    victoryPoints: number;
    cards: CardSummary[]
}

export interface RankingEntry {
    client: GameOverPlayer;
    points: number;
}