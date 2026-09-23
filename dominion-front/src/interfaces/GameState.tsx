import {OpponentPlayerState, UserPlayerState} from "./Player";
import {SupplyPile} from "./Supply";
import {NocturneState} from "./Nocturne";
import {EmpiresState} from "./Empires";

export interface GameState {
    gameId: string | null;
    players: {
        opponents: OpponentPlayerState[];
        turn_player: string | number | null;
        active_player: boolean;
        user: UserPlayerState | null;
    };
    shopSupply: {
        supply: SupplyPile[] | null;
        aside: Record<string, SupplyPile[]>;
        events: SupplyPile[] | null;
    };
    infos: {
        size: number | null;
    };
    nocturne?: NocturneState;
    empires?: EmpiresState;
}


