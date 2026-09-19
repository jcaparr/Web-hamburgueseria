-- The (user_id, burger_joint_id) unique constraints already index lookups by user.
-- These cover the other direction: the joint detail page and the ranking queries,
-- which group ratings and wishlist items by burger joint.
-- Kept out of V1 so databases baselined at version 1 also pick them up.
create index if not exists ix_ratings_burger_joint on ratings (burger_joint_id);
create index if not exists ix_wishlist_burger_joint on wishlist_items (burger_joint_id);
