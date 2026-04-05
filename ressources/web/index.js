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
        supply: null,
        players: null,
        turn_player: null,
        active_player: null,
        instruction: null,
        choices: null,
        buttons: null,
    });

    useEffect(() => {
        socket.onmessage = (event) => {
            const data = JSON.parse(event.data);
            setState({
                supply: data.game.supply,
                players: data.game.players,
                turn_player: data.game.turn_player,
                active_player: data.active_player,
                instruction: data.instruction,
                choices: data.choices,
                buttons: data.buttons,
                log: data.game.log,
            });
        };

        return () => { socket.onmessage = null; };
    }, []);

    if (state.supply === null) {
        return html`<div>Not connected to an active game.</div>`;
    }

    return html`
        <div id="main">
            <div id="game">
                <div id="supply">
                    <div id="kingdom_supply">
                        ${state.supply.slice(0, -7).map(pile =>
        html`<${Card} key=${pile.card} name=${pile.card} number=${pile.number} cost=${pile.cost} classes=${['half']} messageType="SUPPLY" overlay=${pile.number === 0} />`)}
                    </div>
                    <div id="common_supply">
                        ${state.supply.slice(-7).map(pile => html`<${Card} key=${pile.card} name=${pile.card} number=${pile.number} cost=${pile.cost} messageType="SUPPLY" overlay=${pile.number === 0} />`
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
                    game_over=${state.active_player === undefined}
                />`;
        })}
                </div>
            </div>
            <div id="side">
                <${MessagePrompt} />
                <${Log} log=${state.log} />
            </div>
        </div>`;

}

function Card({ name, number, cost, classes, messageType, overlay }) {
    const short_name = name ? name.replace(/[^A-Za-z]/g, '') : '';

    const handleMouseEnter = () => {
        if (!name) return;
        const zoomEl = document.getElementById('zoom');
        if (zoomEl) zoomEl.style.backgroundImage = `url(cards/${short_name}.jpg)`;
    };

    const handleMouseLeave = () => {
        const zoomEl = document.getElementById('zoom');
        if (zoomEl) zoomEl.style.backgroundImage = '';
    };

    return html`
        <div
            class="${['card', ...(classes || [])].join(' ')}"
            style=${name ? { backgroundImage: `url(cards/${short_name}.jpg)` } : null}
            onMouseEnter=${handleMouseEnter}
            onMouseLeave=${handleMouseLeave}
            onClick=${name && messageType ? () => sendMessage(`${messageType}:${name}`) : null}
        >
        ${cost !== undefined ? html`<div class="cost">${cost}</div>` : null}
        ${number ? html`<div class="number">${number}</div>` : null}
        ${overlay && html`<div class="overlay"></div>`}
        </div>`;
}

function Player({ data, is_active, is_turn_player, instruction, choices, buttons, game_over }) {
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
                    <span class=${data.draw.length > 0 ? "tooltip" : ""} data-tooltip=${data.draw.join(", ")}>
                    Draw: ${data.draw.length}
                    </span>
                        <span class=${data.discard.length > 0 ? "tooltip" : ""} data-tooltip=${data.discard.join(", ")}>
            Discard: ${data.discard.length}
        </span>
                    </div>
                </div>
            </div>
            <div class="instruction">
                <div>${instruction}</div>
                <div class="buttons">
                    ${buttons.map((button, index) => html`<button key=${index} onClick=${() => sendMessage("BUTTON:" + button.value)}>${button.label}</button>`)}
                    <button onClick=${() => sendMessage("")} disabled=${!choices.includes("")}>Pass</button>
                </div>
            </div>
            ${!game_over && html`<${ListOfCards} classes=${["in_play"]} cards=${data.in_play} />`}
            <${ListOfCards} classes=${["hand"]} cards=${sortedHand} messageType=${is_active ? "HAND" : null}/>
        </div>`;
}

function ListOfCards({ cards, classes, messageType }) {
    if (cards.length === 0) {
        return html`<div class=${classes.join(" ")}><${Card} classes=${["phantom"]} /></div>`;
    }
    return html`
        <div class=${classes.join(" ")}>
            ${cards.map((card, i) => {
        const isDuplicate = card === cards[i + 1];
        return html`<${Card} key=${i} name=${card} classes=${isDuplicate ? ['duplicate'] : []} messageType=${messageType} />`;
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
    useEffect(() => {
        if (ref.current && scrollLog) {
            ref.current.scrollTop = ref.current.scrollHeight;
        }
    }, [log]);
    const handleScroll = () => {
        if (ref.current) {
            const { scrollTop, clientHeight, scrollHeight } = ref.current;
            setScrollLog(scrollTop + clientHeight >= scrollHeight - 10);
        }
    };
    logText = log.map(line => line.replace(/^ +/g, match => '&nbsp;'.repeat(match.length))).join('<br>');
    return html`<div id="log" ref=${ref} onScroll=${handleScroll} dangerouslySetInnerHTML=${{ __html: logText || "" }} />`;
}

window.addEventListener("load", () => {
    document.addEventListener("keypress", event => {
        if (event.key === " ") {
            sendMessage("");
        }
    });
})



render(html`<${Main} />`, document.getElementById('root'));
