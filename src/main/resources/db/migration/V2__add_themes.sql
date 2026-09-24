-- 1. Création de la table des thèmes
CREATE TABLE themes (
                        id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                        code VARCHAR(50) NOT NULL UNIQUE,
                        name VARCHAR(100) NOT NULL,
                        unlock_type VARCHAR(20) NOT NULL,
                        unlock_value INT NOT NULL DEFAULT 0
);

INSERT INTO themes (code, name, unlock_type, unlock_value) VALUES
                                                               ('dominion', 'Dominion Classic', 'FREE', 0),
                                                               ('seaside', 'Seaside', 'LEVEL', 5);

ALTER TABLE users ADD COLUMN current_theme_id UUID;
ALTER TABLE users ADD COLUMN level INT DEFAULT 0;

UPDATE users
SET current_theme_id = (SELECT id FROM themes WHERE code = 'dominion')
WHERE current_theme_id IS NULL;

ALTER TABLE users ADD CONSTRAINT fk_user_theme
    FOREIGN KEY (current_theme_id) REFERENCES themes(id);

CREATE TABLE user_themes (
                             user_id VARCHAR NOT NULL,
                             theme_id UUID NOT NULL,
                             PRIMARY KEY (user_id, theme_id),
                             FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                             FOREIGN KEY (theme_id) REFERENCES themes(id) ON DELETE CASCADE
);

INSERT INTO user_themes (user_id, theme_id)
SELECT u.id, t.id FROM users u, themes t WHERE t.code = 'dominion';