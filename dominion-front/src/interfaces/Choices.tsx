export interface Choices {
    instruction: string | null;
    choices: string[];
    buttons: Button[];
    selection_cards: string[];
    mode: boolean | null;
}

export interface Button{
    label: string;
    value: string;
}