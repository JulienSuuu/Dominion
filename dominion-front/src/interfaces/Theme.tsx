export type UnlockType =
    | { type: 'FREE' }
    | { type: 'LEVEL' }
    | { type: 'PURCHASE' }
    | { type: 'ACHIEVEMENT'; subject: string }
    | { type: 'STAT'; key: string; detailName?: string | null };

export interface Theme {
    id: string;
    code: string;
    name: string;
    unlockType: UnlockType;
    unlockValue: number;
}