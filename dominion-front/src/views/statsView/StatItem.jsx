import React from 'react';
import CardDetailItem from './CardDetailItem';
import '../css/StatItem.css';

export default function StatItem({ stat, isExpanded, onToggle, getCardImageUrl }) {
    const details = stat.details || {};
    const hasDetails = Object.keys(details).length > 0;
    const isCardType = stat.type === 'Card';

    return (
        <div className={`stat-item ${hasDetails ? 'clickable' : ''}`}>
            {/* Ligne principale */}
            <div
                className="stat-main-info"
                onClick={() => hasDetails && onToggle(stat.key)}
            >
                <div className="stat-label-group">
                    {hasDetails && (
                        <span className={`chevron ${isExpanded ? 'open' : ''}`}>
                            ▲
                        </span>
                    )}
                    <span className="stat-name">{stat.displayName}</span>
                </div>
                <span className="stat-count">{stat.value}</span>
            </div>

            {/* Sous-liste déroulante */}
            {hasDetails && isExpanded && (
                <ul className={`stat-details-list ${isCardType ? 'cards-grid' : ''}`}>
                    {Object.entries(details).map(([itemName, count]) => (
                        isCardType ? (
                            <CardDetailItem
                                key={itemName}
                                itemName={itemName}
                                count={count}
                                getCardImageUrl={getCardImageUrl}
                            />
                        ) : (
                            <li key={itemName} className="stat-detail-item">
                                <span className="item-name">{itemName}</span>
                                <span className="item-count">×{count}</span>
                            </li>
                        )
                    ))}
                </ul>
            )}
        </div>
    );
}