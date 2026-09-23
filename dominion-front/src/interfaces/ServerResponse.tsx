// interfaces/ServerResponse.ts
import { GameState } from './GameState';
import { Choices } from './Choices';
import { LobbyState } from './LobbyState';
import {AppState, ItemChangeEvent, OnCardChangeEvent} from './AppState';
import { LogEvent } from './Log';
import { RankingEntry } from './Classement';
import {Theme} from "./Theme";

export interface ServerResponse {
    gameId?: string | null;
    view?: string;
    active_player?: boolean;
    game?: {
        turn_player?: string | number | null;
        players?: GameState['players']['opponents'];
        client?: GameState['players']['user'];
        supply?: GameState['shopSupply']['supply'];
        aside?: GameState['shopSupply']['aside'];
        events?: GameState['shopSupply']['events'];
        nocturne?: GameState['nocturne'];
        empires?: GameState['empires'];
        size?: number | null;
    } | null;
    instruction?: Choices['instruction'];
    choices?: Choices['choices'];
    buttons?: Choices['buttons'];
    selection_cards?: Choices['selection_cards'];
    mode?: Choices['mode'];
    players?: LobbyState['users'];
    availableCards?: LobbyState['availableCards'];
    availableEvents?: LobbyState['availableEvents'];
    selectedCards?: LobbyState['selectedCards'];
    selectedEvents?: LobbyState['selectedEvents'];
    presets?: LobbyState['presets'];
    options?: LobbyState['options'];
    itemChange?: ItemChangeEvent;
    onCardChange?: OnCardChangeEvent;
    logChange?: LogEvent | LogEvent[];
    classement?: RankingEntry[];
}

export interface AuthUserData {
    id?: string;
    pseudo?: string;
    isConnected?: boolean;
    isRegistering?: boolean;
    theme?: Theme | null;
}