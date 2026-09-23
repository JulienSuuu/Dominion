import React from 'react';
import Card from '../components/card/Card.tsx';
import {useGame} from "../context/GameContext.tsx";

function SelectionOverlay({onSelect, sendMessage}) {
    const {choices} = useGame();

    if (choices.mode === false) return null;


    return (
        <div className="selection-overlay">
            <div className="selection-window">
                <div className="selection-instruction">{choices.instruction}</div>

                {/* Grille de sélection des cartes */}
                <div className="selection-grid">
                    {(choices.selection_cards || []).map((fullChoice, index) => {
                        const cardName = fullChoice.split(":")[2];
                        const isSelectable = choices.choices.includes(fullChoice);

                        return (
                            <div
                                key={`${fullChoice}-${index}`}
                                className={`selection-item ${isSelectable ? 'selectable' : 'not-selectable'}`}
                                onClick={() => isSelectable && typeof onSelect === 'function' ? onSelect(fullChoice) : null}
                            >
                                <Card name={cardName} />
                            </div>
                        );
                    })}
                </div>

                <div className="buttons">
                    {choices.buttons.map((button, index) => (
                        <button
                            key={`btn-${index}`}
                            onClick={() => typeof sendMessage === 'function' && sendMessage(JSON.stringify({gameAction: "BUTTON", message : `BUTTON:${button.value}`,}))}
                        >
                            {button.label}
                        </button>
                    ))}

                    <button
                        onClick={() => typeof sendMessage === 'function' && sendMessage(JSON.stringify(
                            {
                                gameAction: "PASS",
                                message : "",
                            }
                        ))}
                        disabled={!choices.choices.includes("")}
                    >
                        Pass
                    </button>
                </div>
            </div>
        </div>
    );
}

export default SelectionOverlay;