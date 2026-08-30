-- Normalize VIDEO rows that were stored with the former MovieMetadata shape.
UPDATE catalog_items
SET metadata = jsonb_build_object(
        'title', metadata -> 'title',
        'description', metadata -> 'overview',
        'recordedAt', NULL)
WHERE kind = 'VIDEO'
  AND metadata ? 'overview'
  AND NOT metadata ? 'description';
