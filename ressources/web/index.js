const { h, render } = preact;
const { useState, useEffect, useRef } = preactHooks;
const html = htm.bind(h);

const socket = new WebSocket("ws://localhost:3232");
function sendMessage(message) {
    socket.send(message);
    console.log(`Message envoyé: "${message}"`);
}

function Main() {
    const [state, setState] = useState({
        view: "LOBBY",
        supply: null,
        players: null,
        turn_player: null,
        active_player: null,
        instruction: null,
        choices: null,
        buttons: null,
        selection_cards: null,
        message_type: null,
        mode:false,
        availableCards: {},
        presets: {},
        selectedCards: null,
        aside: {},
        options: {}

    });

    useEffect(() => {
        socket.onmessage = (event) => {
            const data = JSON.parse(event.data);
            console.log("--- MESSAGE REÇU ---", data);
            console.log("OPTIONS REÇUES:", data.options); // Vérifie si c'est bien l'objet avec les extensions

            setState(prevState => {
                const hasGame = data.game !== null && data.game !== undefined;
                return {
                    ...prevState,
                    view: data.view || prevState.view,
                    availableCards: data.availableCards || prevState.availableCards,
                    selectedCards: data.selectedCards || prevState.selectedCards,
                    presets: data.presets || prevState.presets,
                    options: data.options || prevState.options,

                    supply: hasGame ? data.game.supply : prevState.supply,
                    aside: hasGame? data.game.aside : prevState.aside,
                    players: hasGame ? data.game.players : prevState.players,
                    turn_player: hasGame ? data.game.turn_player : prevState.turn_player,
                    size: hasGame ? data.game.size : prevState.size,
                    log: hasGame ? data.game.log : prevState.log,

                    active_player: data.active_player !== undefined ? data.active_player : prevState.active_player,
                    instruction: data.instruction || prevState.instruction,
                    choices: data.choices || prevState.choices,
                    buttons: data.buttons || prevState.buttons,
                    selection_cards: data.selection_cards || prevState.selection_cards,
                    mode: data.mode !== undefined ? data.mode : prevState.mode,
                };
            });
        };
        return () => { socket.onmessage = null; };
    }, []);

    if (state.view === "FERRYMAN_CHOICE" || state.view === "YOUNG_WITCH_CHOICE") {
        const isWitch = state.view === "YOUNG_WITCH_CHOICE";
        const title = isWitch ? "La Jeune Sorcière" : "Le Passeur";
        const description = isWitch
            ? "Choisissez une carte (2$ ou 3$) comme Fléau (Bane) :"
            : "Choisissez une carte (3$ ou 4$) à mettre de côté :";
        const prefix = isWitch ? "CONFIRM_YOUNG_WITCH:" : "CONFIRM_FERRYMAN:";
        const themeClass = isWitch ? "witch-theme" : "ferryman-theme";
        const categories = Object.entries(state.options || {});

        return html`
            <div class="hub-layout">
                <div class="hub-parchment selection-modal ${themeClass}">
                    <div class="modal-header">
                        <h2>${title}</h2>
                        <p class="subtitle">${description}</p>
                    </div>

                    <div class="selection-container scrollable">
                        ${categories.map(([expansion, cards]) => html`
                            <div class="extension-group">
                                <h3 class="extension-title">${expansion}</h3>
                                <div class="selection-grid">
                                    ${cards.map(cardName => html`
                                        <${CardHub}
                                                key=${cardName}
                                                name=${cardName}
                                                onClick=${() => sendMessage(prefix + cardName)}
                                        />
                                    `)}
                                </div>
                            </div>
                        `)}
                    </div>

                    <div class="modal-footer">
                        <small>Cette carte sera une pile supplémentaire (11ème pile).</small>
                    </div>
                </div>
            </div>`;
    }

    if (!state.supply) {
        return html`
            <div id="main" class="hub-layout">
                <div class="hub-parchment">
                    <h1>Configuration du Royaume</h1>

                    <section class="presets-section">
                        <h4 class="section-subtitle">Sets Recommandés</h4>
                        <div class="presets-scroll-area">
                        ${Object.entries(state.presets || {}).map(([expansion, sets]) => {
                            const isCombo = expansion.includes("&");

                            return html`
                                <div class="set-extension-group ${isCombo ? 'combo-mode' : ''}">
                                    <div class="group-header">
                                        <h4 class="set-extension-title">${expansion}</h4>
                                        <div class="title-line"></div>
                                    </div>

                                    <div class="presets-container">
                                        ${Object.entries(sets).map(([setName, cards]) => html`
                                            <div class="preset-item">
                                                <button class="preset-tag" onClick=${() => sendMessage("SET_CARDS:" + cards.join(","))}>
                                                    ${setName}
                                                    <span class="tooltip-text">
                                                        <strong class="tooltip-title">${setName}</strong><br/>
                                                        <small class="tooltip-expansion">${expansion}</small>
                                                        <div class="tooltip-divider"></div>
                                                        <div class="tooltip-content">
                                                            ${cards.slice(0, 10).join(', ')}
                                                            ${cards.length > 10 ? html`
                                                                <div class="tooltip-extra">
                                                                    <span class="extra-label">+ EXTRA :</span> ${cards[10]}
                                                                </div>
                                                            ` : ''}
                                                        </div>
                                                    </span>
                                                </button>
                                            </div>
                                        `)}
                                    </div>
                                </div>
                            `;
                        })} 
                        </div>

                        <div class="presets-actions">
                            <button class="preset-tag clear-btn" onClick=${() => sendMessage("CLEAR_CARDS")}>
                                🗑️ Vider tout
                            </button>
                        </div>
                    </section>
                    
                    
                    
                    <section class="hub-section">
                        <div class="hub-header">
                            <h3>Sélectionnez 10 cartes de Royaume</h3>
                            <div class="counter ${state.selectedCards?.length >= 10 ? 'ready' : ''}">
                                ${state.selectedCards?.length || 0} / 10
                            </div>
                        </div>

                        <div class="hub-scroll-container">
                            ${state.availableCards && Object.entries(state.availableCards).map(([extension, cards]) => html`
                            <div class="extension-group">
                                <h2 class="extension-title">${extension}</h2>
                                <div class="selection-grid">
                                    ${cards.map(cardName => {
                                        const isSelected = state.selectedCards?.includes(cardName);
                                        const selectionIndex = state.selectedCards?.indexOf(cardName);
                                        const isExtra = isSelected && selectionIndex >= 10;
                                        
                                        return html`
                                            <div class="${isExtra ? 'card-extra-glow' : ''}">
                                                <${CardHub}
                                                        key=${cardName}
                                                        name=${cardName}
                                                        isSelected=${isSelected}
                                                        onClick=${() => {
                                                            if (!isSelected && state.selectedCards?.length >= 10) return;
                                                            sendMessage("TOGGLE:" + cardName);
                                                        }}
                                                />
                                            </div>`;
                                    })}
                                </div>
                            </div>
                        `)}
                        </div>
                    </section>

                    <div class="hub-footer">
                        <button class="start-button"
                                disabled=${state.selectedCards?.length < 10}
                                onClick=${() => sendMessage("START_GAME")}>
                            COMMENCER LA PARTIE
                        </button>
                    </div>
                </div>
            </div>`;
    }
        return html`
        <div id="main">
            ${state.selection_cards && html`
                <${SelectionOverlay}
                        instruction=${state.instruction}
                        cards=${state.selection_cards}
                        choices=${state.choices}
                        buttons=${state.buttons}
                        mode=${state.mode}
                        onSelect=${(choice) => sendMessage(choice)}
                />
            `}

            <div id="game">
                <div id="aside-supply-zone">
                    ${state.aside && Object.keys(state.aside).length > 0 && Object.entries(state.aside).map(([category, piles]) => html`
                        <div class="aside-group">
                            <div class="aside-header">${category.toUpperCase()}</div>
                            <div class="aside-row">
                                ${piles.map(pile => html`
                                    <${Card} 
                                            key=${pile.card} 
                                            name=${pile.card} 
                                            number=${pile.number} 
                                            cost=${pile.cost} 
                                            potion=${pile.potion} 
                                            debt=${pile.debt}
                                            messageType=${category.toUpperCase() === 'EVENTS' ? 'BUY_EVENT' : 'NONE'}
                                            classes=${['half', category.toUpperCase() === 'EVENTS' ? 'event-style' : '']}
                                            overlay=${pile.number === 0}
                                    />
                                `)}
                            </div>
                        </div>
                    `)}
                </div>
                
                <div id="supply">
                    <div id="kingdom_supply">
                        ${state.supply.slice(0, -state.size).map(pile =>
            html`<${Card}
                                        key=${pile.card}
                                        name=${pile.card}
                                        number=${pile.number}
                                        cost=${pile.cost}
                                        potion=${pile.potion}
                                        debt=${pile.debt}
                                        classes=${['half']}
                                        messageType="SUPPLY"
                                        overlay=${pile.number === 0}
                                />`
        )}
                    </div>
                    <div id="common_supply">
                        ${state.supply.slice(-state.size).map(pile =>
            html`<${Card}
                                        key=${pile.card}
                                        name=${pile.card}
                                        number=${pile.number}
                                        cost=${pile.cost}
                                        potion=${pile.potion}
                                        debt=${pile.debt}
                                        messageType="SUPPLY"
                                        overlay=${pile.number === 0}
                                />`
        )}
                    </div>
                </div>
                
                <div id="players">
                    ${state.players.map((player_data, index) => {
            const is_active = index === state.active_player;
            const is_turn_player = index === state.turn_player;
            return html`<${Player}
                                key=${player_data.name}
                                data=${player_data}
                                is_active=${is_active}
                                is_turn_player=${is_turn_player}
                                instruction=${is_active ? state.instruction : ""}
                                choices=${is_active ? state.choices : []}
                                buttons=${is_active ? state.buttons : []}
                                game_over=${state.instruction === "Game over"}
                                mode=${state.mode}
                        />`;
        })}
                </div>
            </div>

            <div id="side">
                <${Log} log=${state.log} />
            </div>
        </div>`;
}

function CardHub({ name, isSelected, onClick }) {
    const short_name = name ? name.replace(/[^A-Za-z]/g, '') : '';

    const handleMouseEnter = () => {
        if (!name) return;
        const zoomEl = document.getElementById('zoom');
        if (zoomEl){
            zoomEl.style.backgroundImage = `url(cards/${short_name}.jpg)`;
            zoomEl.classList.add('active');
        }
    };

    const handleMouseLeave = () => {
        const zoomEl = document.getElementById('zoom');
        if (zoomEl) zoomEl.classList.remove('active');
    };

    return html`
        <div
            class="card ${isSelected ? 'selected' : ''}"
            style=${name ? { backgroundImage: `url(cards/${short_name}.jpg)` } : null}
            onClick=${onClick}
            onMouseEnter=${handleMouseEnter}
            onMouseLeave=${handleMouseLeave}
        >
            ${isSelected && html`
                <div class="selected-check">
                    <span>✓</span>
                </div>
            `}
        </div>`;
}

function Card({ name, number, cost, potion, debt, classes, messageType, overlay, style }) {
    const short_name = name ? name.replace(/[^A-Za-z]/g, '') : '';

    const handleMouseEnter = () => {
        if (!name) return;
        const zoomEl = document.getElementById('zoom');
        if (zoomEl){
            zoomEl.style.backgroundImage = `url(cards/${short_name}.jpg)`;
            zoomEl.classList.add('active');
        }
    };

    const handleMouseLeave = () => {
        const zoomEl = document.getElementById('zoom');
        if (zoomEl) zoomEl.classList.remove('active');
    };

    const combinedStyle = {
        ...(style || {}),
        backgroundImage: name ? `url(cards/${short_name}.jpg)` : null
    };

    return html`
        <div
                class="${['card', ...(classes || [])].join(' ')}"
                style=${combinedStyle}
                onMouseEnter=${handleMouseEnter}
                onMouseLeave=${handleMouseLeave}
                onClick=${name && messageType ? () => sendMessage(`${messageType}:${name}`) : null}
        >
            <div class="card-footer">
                ${cost > 0 ? html`<div class="cost">${cost}</div>` : null}
                ${potion > 0 ? html`<div class="potion"></div>` : null}
                ${debt > 0 ? html`<div class="debt">${debt}</div>` : null}
            </div>

            ${number ? html`<div class="number">${number}</div>` : null}
            ${overlay && html`<div class="overlay"></div>`}
        </div>`;
}

function Player({ data, is_active, is_turn_player, instruction, choices, buttons, game_over, mode }) {
    const classes = ["player", is_active ? "active" : "", is_turn_player ? "turn" : ""].join(" ");
    const sortedHand = [...data.hand].sort();
    return html`
        <div class=${classes}>
            <div class="player_info">
                <div class="name">${data.name}</div>
                <div class="data stats-container">
                    <div class="stat-group resources">
                        <span class="coins">🟡 ${data.money}</span>
                        <span class="${data.debt > 0 ? 'debt-active' : ''}"> ⬡ ${data.debt}</span>
                        <span class="potions">⚗️ ${data.potion}</span>
                        <span class="coffers">💰 ${data.coffre}</span>
                    </div>

                    <div class="stat-group mechanics">
                        <span>Actions: ${data.actions}</span>
                        <span>Buys: ${data.buys}</span>
                    </div>

                        <div class="stat-group deck">
                    <span>
                    Draw: ${data.draw.length}
                    </span>
                        <span>
            Discard: ${data.discard.length}
        </span>
                    </div>
                </div>
            </div>

            <div id="interaction-container">
                ${instruction ? html`
        <div class="instruction">
            <div>${instruction}</div>
            <div class="buttons">
                ${buttons.map((button, index) => html`
                    <button key=${index} onClick=${() => sendMessage("BUTTON:" + button.value)}>
                        ${button.label}
                    </button>
                `)}
                <button 
                    onClick=${() => sendMessage("")} 
                    disabled=${!choices.includes("")}
                >
                    Pass
                </button>
            </div>
        </div>
    ` : null}
            </div>
            ${!game_over && html`<${ListOfCards} classes=${["in_play"]} cards=${data.in_play} />`}
            <${ListOfCards} classes=${!game_over && is_active ? "hand" : "hand-basic"} cards=${sortedHand} messageType=${is_active ? "HAND" : null}/>
        </div>`;
}

function SelectionOverlay({ instruction, cards, choices, buttons, onSelect, mode}) {
    if (mode === false) return null;

    return html`
        <div class="selection-overlay">
            <div class="selection-window">
                <div class="selection-instruction">${instruction}</div>

                <div class="selection-grid">
                    ${(cards || []).map((fullChoice) => {
                        const cardName = fullChoice.split(":")[2];
                        const isSelectable = choices.includes(fullChoice);

                        return html`
                            <div
                                    class="selection-item ${isSelectable ? 'selectable' : 'not-selectable'}"
                                    onClick=${() => isSelectable ? onSelect(fullChoice) : null}
                            >
                                <${Card} name=${cardName} />
                            </div>
                        `;
                    })}
                </div>
                
                <div class="buttons">
                    ${buttons.map((button, index) => html`<button key=${index} onClick=${() => sendMessage("BUTTON:" + button.value)}>${button.label}</button>`)}
                    <button onClick=${() => sendMessage("")} disabled=${!choices.includes("")}>Pass</button>
                </div>
            </div>
        </div>`;
}


function ListOfCards({ cards, classes, messageType }) {
    if (!cards || cards.length === 0) {
        return html`<div class=${classes.join(" ")}></div>`;
    }
    const classList = Array.isArray(classes) ? classes : [classes];
    const total = cards.length;
    const isHand = classList.includes("hand") || classList.includes("hand-basic");
    return html`
        <div class=${classList.join(" ")} style="--total: ${total}">
            ${cards.map((card, i) => {
                const isDuplicate = card === cards[i + 1];

                return html`
                    <${Card}
                            key=${i}
                            name=${card}
                            classes=${isDuplicate ? ['duplicate'] : []}
                            messageType=${messageType}
                            style=${isHand ? { "--index": i } : null}
                    />`;
            })}
        </div>`;
}

function MessagePrompt() {
    return html`<div id="message">
        <input id="message-input" type="text" placeholder="message" onKeyDown=${event => {
            if (event.key === "Enter") {
                socket.send(event.target.value);
                console.log(`Message envoyé: "${event.target.value}"`);
                event.target.value = "";
            }
        }}/>
    </div>`;
}

function Log({ log }) {
    const ref = useRef(null);
    const [scrollLog, setScrollLog] = useState(true);
    const [isOpen, setIsOpen] = useState(true);

    useEffect(() => {
        if (ref.current && scrollLog && isOpen) {
            ref.current.scrollTop = ref.current.scrollHeight;
        }
    }, [log, isOpen]);

    const handleScroll = () => {
        if (ref.current) {
            const { scrollTop, clientHeight, scrollHeight } = ref.current;
            setScrollLog(scrollTop + clientHeight >= scrollHeight - 10);
        }
    };

    const logText = log.map(line =>
        line.replace(/^ +/g, match => '&nbsp;'.repeat(match.length))
    ).join('<br>');

    return html`
        <div class="log-floating-container ${isOpen ? 'open' : 'closed'}">
            <div class="log-header" onClick=${() => setIsOpen(!isOpen)}>
                <span>Journal de bord</span>
                
                <button class="toggle-btn">${isOpen ? '−' : '+'}</button>
            </div>
            
            <div
                    id="log"
                    ref=${ref}
                    onScroll=${handleScroll}
                    dangerouslySetInnerHTML=${{ __html: logText || "" }}
            /><${MessagePrompt} />
        </div>
    `;
}
window.addEventListener("load", () => {
    document.addEventListener("keypress", event => {
        if (event.key === " ") {
            sendMessage("");
        }
    });
})



render(html`<${Main} />`, document.getElementById('root'));
