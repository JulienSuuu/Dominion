export interface EnchantressRuleState {
    affectedPlayerId?: string;
    [key: string]: unknown;
}

export interface EnchantressEntry {
    cardId: string;
    clientId: string;
}

export interface EmpiresState {
    obeliskTarget: string;
    enchantress?: EnchantressEntry[];
}