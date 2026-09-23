
interface ImageModule {
    default: string;
}

const cardImages: Record<string, ImageModule> = import.meta.glob<ImageModule>(
    '/src/assets/cards/*.{jpg,png,webp}',
    { eager: true }
);

export const preloadAllCards = (): void => {
    Object.values(cardImages).forEach((module) => {
        const img = new Image();
        img.src = module.default;
    });
};

export const getCardImageUrl = (cardId: string): string => {
    const match = cardImages[`/src/assets/cards/${cardId}.png`]
        || cardImages[`/src/assets/cards/${cardId}.jpg`];

    return match ? match.default : '/src/assets/cards/placeholder.png';
};