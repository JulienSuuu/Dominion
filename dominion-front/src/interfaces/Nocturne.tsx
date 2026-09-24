import { SupplyPile } from './Supply';

export interface StateOwnerInfo {
    owner: number;
    card: string;
}

export interface NocturneState {
    discardMat: Record<string, number>;

    nocturnePile: Record<string, SupplyPile[]>;

    druidBoons: string[];

    states: Record<string, StateOwnerInfo>;
}