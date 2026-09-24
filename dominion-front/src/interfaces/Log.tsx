export interface BaseLogEvent {
    id: string;
    type: string;
    text: string;
    timestamp: string;
}

export interface PlayerLogEvent extends BaseLogEvent {
    playerId: string;
    playerName: string;
}

export type LogEvent = BaseLogEvent | PlayerLogEvent;