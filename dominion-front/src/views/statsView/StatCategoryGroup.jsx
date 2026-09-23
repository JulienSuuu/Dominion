import React from 'react';
import StatItem from './StatItem';
import '../css/StatCategoryGroup.css';

export default function StatCategoryGroup({ categoryName, categoryStats, expandedKey, onToggle, getCardImageUrl }) {
    return (
        <div className="category-block">
            <h3 className="category-title">{categoryName}</h3>
            <div className="stats-list">
                {categoryStats.map((stat) => (
                    <StatItem
                        key={stat.key}
                        stat={stat}
                        isExpanded={expandedKey === stat.key}
                        onToggle={onToggle}
                        getCardImageUrl={getCardImageUrl}
                    />
                ))}
            </div>
        </div>
    );
}