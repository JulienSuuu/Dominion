export interface LobbyState {
    gameId: string | null;
    availableCards?: Record<string, string[]>;
    availableEvents?: Record<string, string[]>;
    selectedCards?: string[];
    selectedEvents?: string[];
    users: LobbyUser[];
    presets?: Record<string, Preset[]>;
    options?: Record<string, string[]>;
}

export interface Preset {
    name: string;
    cards: string[];
}

export interface LobbyUser {
    id: string;
    pseudo: string;
    isReady: boolean;
}