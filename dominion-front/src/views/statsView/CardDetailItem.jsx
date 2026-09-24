import React from 'react';
import '../css/CardDetailItem.css';

export default function CardDetailItem({ itemName, count, getCardImageUrl }) {
    return (
        <li className="stat-detail-item card-item">
            <div className="card-detail">
                <img
                    src={getCardImageUrl(itemName)}
                    alt={itemName}
                    className="card-thumb"
                />
                <span className="item-name">{itemName}</span>
            </div>
            <span className="item-count">×{count}</span>
        </li>
    );
}