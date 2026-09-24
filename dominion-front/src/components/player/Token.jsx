import React from 'react';

function Token({
                   name,
                   id,
                   classes = [],
                   playerColor,
                   style = {},
                   data = {},
                   tokenKey,
                   sendMessage
               }) {

    const short_name = name ? name.replace(/[^A-Za-z]/g, '') : '';
    const send = (message) => {
        sendMessage(JSON.stringify({
            gameAction: "TOKEN",
            message: message,
        }));
    }
    const targetKey = tokenKey || name;
    const currentTarget = data.tokens?.supplyTokens?.[targetKey];
    if(currentTarget) return null;

    const baseStyle = currentTarget
        ? { display: 'none', pointerEvents: 'none' }
        : {};


    const combinedStyle = {
        ...baseStyle,
        ...(style || {}),
        backgroundImage: name ? `url(../src/assets/images/${short_name}.png)` : null,
        borderColor: playerColor
    };

    const handleClick = (e) => {
        if (e && e.stopPropagation) e.stopPropagation();

        if (name && typeof send === 'function') {
            send(`TOKEN:${name}`);
        }
    };

    return (
        <div
            id={id}
            className={['token', ...(classes || [])].join(' ')}
            style={combinedStyle}
            onClick={handleClick}
        />
    );
}

export default Token;