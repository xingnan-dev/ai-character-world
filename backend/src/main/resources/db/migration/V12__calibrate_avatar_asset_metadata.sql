-- Calibrate metadata to the capabilities actually present in the bundled fixed VRM files.
-- These assets do not provide runtime-swappable hair, ears, wings, outfits, or accessories.

UPDATE t_avatar_asset
SET name = 'Nova - MetaPerson male casual base',
    file_size = 5475092,
    gender = 1,
    style_tags = 'basic,modern,casual',
    color_tags = 'brown,blue,teal,black,white',
    supported_attributes = '{"hair":{"color":["brown"],"style":["short"]},"eye":{"color":["blue"]},"body":{"type":["slim"]},"outfit":{"style":["casual"],"color":["teal","black","white"]},"ear":["human"],"wing":["none"],"accessories":[]}',
    description = 'Fixed MetaPerson male avatar with short brown hair and casual clothing; no interchangeable ears, wings, outfits, or accessories.'
WHERE file_url = '/models/avatars/nova.vrm';

UPDATE t_avatar_asset
SET name = 'Sky - MetaPerson fixed base',
    file_size = 5481132,
    gender = 0,
    style_tags = 'basic,modern',
    color_tags = '',
    supported_attributes = '{"hair":{"color":[],"style":[]},"eye":{"color":[]},"body":{"type":[]},"outfit":{"style":[],"color":[]},"ear":["human"],"wing":["none"],"accessories":[]}',
    description = 'Fixed MetaPerson avatar with unverified visual tags; no interchangeable ears, wings, outfits, or accessories.'
WHERE file_url = '/models/avatars/sky.vrm';
