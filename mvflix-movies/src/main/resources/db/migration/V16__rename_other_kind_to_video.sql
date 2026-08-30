-- Rename the generic non-movie catalog kind without changing existing records.
UPDATE movies
SET kind = 'VIDEO'
WHERE kind = 'OTHER';
