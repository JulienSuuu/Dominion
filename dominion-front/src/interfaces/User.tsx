import {Theme} from "./Theme";

export interface User {
    id : string | number | null;
    pseudo : string | null;
    isConnected : boolean;
    isRegistering : boolean;
    currentTheme : Theme | null;
}