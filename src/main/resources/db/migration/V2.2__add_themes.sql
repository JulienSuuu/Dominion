ALTER TABLE themes ALTER COLUMN unlock_type TYPE TEXT;

UPDATE themes
SET unlock_value = 25, unlock_type = '{"type":"STAT","key":"GAMES_PLAYED"}'
WHERE code = 'nocturne';

INSERT INTO themes (code, name, unlock_type, unlock_value) VALUES
                                                                   ( 'prosperity', 'Prosperity', '{"type":"STAT","key":"RESOURCES_OBTAINED", "detailName":"MONEY"}', 125),
                                                                   ( 'intrigue', 'Intrigue', '{"type":"STAT","key":"TOTAL_CARDS_PLAYED"}', 1000);