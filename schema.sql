-- Starto V2 Database Schema
-- Extensions
CREATE EXTENSION IF NOT EXISTS postgis;
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- ─────────────────────────────────────────────────────────────────────────────
-- Users
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS users (
  id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  firebase_uid      VARCHAR(128) UNIQUE NOT NULL,
  username          VARCHAR(50)  UNIQUE,
  name              VARCHAR(255) NOT NULL,
  email             VARCHAR(255) UNIQUE NOT NULL,
  phone             VARCHAR(20),
  role              VARCHAR(50)  NOT NULL,
  industry          VARCHAR(100),
  sub_industry      VARCHAR(100),
  city              VARCHAR(100),
  state             VARCHAR(100),
  country           VARCHAR(100) DEFAULT 'India',
  lat               DECIMAL(10,8),
  lng               DECIMAL(11,8),
  location_point    GEOGRAPHY(POINT, 4326),
  bio               TEXT,
  avatar_url        TEXT,
  website_url       TEXT,
  linkedin_url      TEXT,
  plan              VARCHAR(20)  DEFAULT 'EXPLORER',
  plan_expires_at   TIMESTAMPTZ,
  signal_count      INTEGER      DEFAULT 0,
  network_size      INTEGER      DEFAULT 0,
  network_tier      VARCHAR(20)  DEFAULT 'local',
  is_online         BOOLEAN      DEFAULT false,
  last_seen         TIMESTAMPTZ,
  is_verified       BOOLEAN      DEFAULT false,
  is_active         BOOLEAN      DEFAULT true,
  fcm_token         TEXT,
  created_at        TIMESTAMPTZ  DEFAULT now(),
  updated_at        TIMESTAMPTZ  DEFAULT now()
);

-- ─────────────────────────────────────────────────────────────────────────────
-- Signals
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS signals (
  id               UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id          UUID        REFERENCES users(id) ON DELETE CASCADE,
  username         VARCHAR(50),
  type             VARCHAR(50) NOT NULL,
  category         VARCHAR(100),
  title            VARCHAR(255) NOT NULL,
  description      TEXT        NOT NULL,
  seeking          VARCHAR(255),
  stage            VARCHAR(50),
  city             VARCHAR(100),
  state            VARCHAR(100),
  lat              DECIMAL(10,8),
  lng              DECIMAL(11,8),
  location_point   GEOGRAPHY(POINT, 4326),
  timeline_days    INTEGER,
  compensation     VARCHAR(100),
  visibility       VARCHAR(20) DEFAULT 'global',
  status           VARCHAR(20) DEFAULT 'open',
  signal_strength  VARCHAR(20) DEFAULT 'normal',
  response_count   INTEGER     DEFAULT 0,
  offer_count      INTEGER     DEFAULT 0,
  view_count       INTEGER     DEFAULT 0,
  is_boosted       BOOLEAN     DEFAULT false,
  boost_expires_at TIMESTAMPTZ,
  expires_at       TIMESTAMPTZ DEFAULT now() + INTERVAL '7 days',
  created_at       TIMESTAMPTZ DEFAULT now(),
  updated_at       TIMESTAMPTZ DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_signals_location ON signals USING GIST(location_point);
CREATE INDEX IF NOT EXISTS idx_signals_city     ON signals(city);
CREATE INDEX IF NOT EXISTS idx_signals_type     ON signals(type);
CREATE INDEX IF NOT EXISTS idx_signals_status   ON signals(status);
CREATE INDEX IF NOT EXISTS idx_signals_user_id  ON signals(user_id);
CREATE INDEX IF NOT EXISTS idx_signals_created  ON signals(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_signals_search   ON signals USING GIN(to_tsvector('english', title || ' ' || description));

-- ─────────────────────────────────────────────────────────────────────────────
-- Signal Views  (Fix #5 — was missing)
-- Matches SignalView.java entity
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS signal_views (
  id             UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
  signal_id      UUID        NOT NULL REFERENCES signals(id) ON DELETE CASCADE,
  viewer_user_id UUID        REFERENCES users(id) ON DELETE SET NULL,  -- null = anonymous
  viewed_at      TIMESTAMPTZ DEFAULT now(),
  is_follower    BOOLEAN     DEFAULT false
);

CREATE INDEX IF NOT EXISTS idx_signal_views_signal_id ON signal_views(signal_id);
CREATE INDEX IF NOT EXISTS idx_signal_views_viewer    ON signal_views(viewer_user_id);

-- ─────────────────────────────────────────────────────────────────────────────
-- Responses
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS responses (
  id         UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
  signal_id  UUID        NOT NULL REFERENCES signals(id)  ON DELETE CASCADE,
  user_id    UUID        NOT NULL REFERENCES users(id)    ON DELETE CASCADE,
  message    TEXT        NOT NULL,
  type       VARCHAR(30) DEFAULT 'respond',
  status     VARCHAR(20) DEFAULT 'pending',
  created_at TIMESTAMPTZ DEFAULT now()
);

-- ─────────────────────────────────────────────────────────────────────────────
-- Comments  (Fix #5 — was missing)
-- Matches Comment.java entity (supports threaded replies via parent_id)
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS comments (
  id         UUID  PRIMARY KEY DEFAULT gen_random_uuid(),
  signal_id  UUID  REFERENCES signals(id) ON DELETE CASCADE,
  user_id    UUID  NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  username   VARCHAR(50),
  content    TEXT  NOT NULL,
  parent_id  UUID  REFERENCES comments(id) ON DELETE CASCADE,  -- null = top-level
  created_at TIMESTAMPTZ DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_comments_signal_id ON comments(signal_id);
CREATE INDEX IF NOT EXISTS idx_comments_parent_id ON comments(parent_id);
CREATE INDEX IF NOT EXISTS idx_comments_user_id   ON comments(user_id);

-- ─────────────────────────────────────────────────────────────────────────────
-- Offers  (Fix #5 — was missing)
-- Matches Offer.java entity
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS offers (
  id                UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
  requester_id      UUID        NOT NULL REFERENCES users(id)   ON DELETE CASCADE,
  receiver_id       UUID        NOT NULL REFERENCES users(id)   ON DELETE CASCADE,
  signal_id         UUID        NOT NULL REFERENCES signals(id) ON DELETE CASCADE,
  organization_name VARCHAR(255),
  portfolio_link    VARCHAR(500),
  message           TEXT,
  status            VARCHAR(20) DEFAULT 'pending',
  created_at        TIMESTAMPTZ DEFAULT now(),
  updated_at        TIMESTAMPTZ DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_offers_receiver_id  ON offers(receiver_id);
CREATE INDEX IF NOT EXISTS idx_offers_requester_id ON offers(requester_id);
CREATE INDEX IF NOT EXISTS idx_offers_signal_id    ON offers(signal_id);

-- ─────────────────────────────────────────────────────────────────────────────
-- Reviews  (Fix #5 — was missing)
-- Matches Review.java entity
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS reviews (
  id          UUID    PRIMARY KEY DEFAULT gen_random_uuid(),
  reviewer_id UUID    NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  reviewed_id UUID    NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  rating      INTEGER NOT NULL CHECK (rating BETWEEN 1 AND 5),
  comment     TEXT,
  created_at  TIMESTAMPTZ DEFAULT now(),
  UNIQUE (reviewer_id, reviewed_id)
);

CREATE INDEX IF NOT EXISTS idx_reviews_reviewed_id ON reviews(reviewed_id);
CREATE INDEX IF NOT EXISTS idx_reviews_reviewer_id ON reviews(reviewer_id);

-- ─────────────────────────────────────────────────────────────────────────────
-- Connections
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS connections (
  id           UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
  requester_id UUID        REFERENCES users(id) ON DELETE CASCADE,
  receiver_id  UUID        REFERENCES users(id) ON DELETE CASCADE,
  status       VARCHAR(20) DEFAULT 'pending',
  created_at   TIMESTAMPTZ DEFAULT now(),
  UNIQUE(requester_id, receiver_id)
);

-- ─────────────────────────────────────────────────────────────────────────────
-- Messages
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS messages (
  id          UUID    PRIMARY KEY DEFAULT gen_random_uuid(),
  sender_id   UUID    REFERENCES users(id) ON DELETE CASCADE,
  receiver_id UUID    REFERENCES users(id) ON DELETE CASCADE,
  content     TEXT    NOT NULL,
  is_read     BOOLEAN DEFAULT false,
  created_at  TIMESTAMPTZ DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_messages_sender_id   ON messages(sender_id);
CREATE INDEX IF NOT EXISTS idx_messages_receiver_id ON messages(receiver_id);

-- ─────────────────────────────────────────────────────────────────────────────
-- Notifications
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS notifications (
  id         UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id    UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  type       VARCHAR(50) NOT NULL,
  title      VARCHAR(255),
  body       TEXT,
  data       JSONB,
  is_read    BOOLEAN     DEFAULT false,
  created_at TIMESTAMPTZ DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_notifications_user_id ON notifications(user_id);
CREATE INDEX IF NOT EXISTS idx_notifications_is_read ON notifications(is_read);

-- ─────────────────────────────────────────────────────────────────────────────
-- AI Usage
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS ai_usage (
  id         UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id    UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  used_date  DATE        NOT NULL DEFAULT CURRENT_DATE,
  count      INTEGER     DEFAULT 0,
  UNIQUE(user_id, used_date)
);

-- ─────────────────────────────────────────────────────────────────────────────
-- Explore Reports
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS explore_reports (
  id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id         UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  location        VARCHAR(255),
  industry        VARCHAR(100),
  budget          BIGINT,
  stage           VARCHAR(50),
  target_customer VARCHAR(50),
  report_data     JSONB       NOT NULL,
  created_at      TIMESTAMPTZ DEFAULT now()
);

-- ─────────────────────────────────────────────────────────────────────────────
-- Subscriptions
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS subscriptions (
  id                UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id           UUID        REFERENCES users(id) ON DELETE CASCADE,
  plan              VARCHAR(20) NOT NULL,
  razorpay_order_id VARCHAR(255),
  razorpay_sub_id   VARCHAR(255),
  payment_id        VARCHAR(255),
  status            VARCHAR(20) DEFAULT 'active',
  amount            INTEGER,
  currency          VARCHAR(10) DEFAULT 'INR',
  started_at        TIMESTAMPTZ DEFAULT now(),
  expires_at        TIMESTAMPTZ,
  cancelled_at      TIMESTAMPTZ
);

-- ─────────────────────────────────────────────────────────────────────────────
-- Nearby Spaces
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS nearby_spaces (
  id             UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
  name           VARCHAR(255) NOT NULL,
  type           VARCHAR(50),
  address        TEXT,
  city           VARCHAR(100),
  state          VARCHAR(100),
  lat            DECIMAL(10,8),
  lng            DECIMAL(11,8),
  location_point GEOGRAPHY(POINT, 4326),
  description    TEXT,
  contact        TEXT,
  website        TEXT,
  verified       BOOLEAN      DEFAULT false,
  created_at     TIMESTAMPTZ  DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_nearby_location ON nearby_spaces USING GIST(location_point);

-- ─────────────────────────────────────────────────────────────────────────────
-- Comments
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS comments (
  id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  signal_id  UUID REFERENCES signals(id) ON DELETE CASCADE,
  user_id    UUID REFERENCES users(id)   ON DELETE CASCADE,
  parent_id  UUID REFERENCES comments(id) ON DELETE CASCADE,
  content    TEXT        NOT NULL,
  created_at TIMESTAMPTZ DEFAULT now(),
  updated_at TIMESTAMPTZ DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_comments_signal_id ON comments(signal_id);
CREATE INDEX IF NOT EXISTS idx_comments_user_id   ON comments(user_id);
CREATE INDEX IF NOT EXISTS idx_comments_parent_id ON comments(parent_id);

-- ─────────────────────────────────────────────────────────────────────────────
-- Offers
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS offers (
  id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  signal_id  UUID REFERENCES signals(id) ON DELETE CASCADE,
  user_id    UUID REFERENCES users(id)   ON DELETE CASCADE,
  amount     BIGINT,
  currency   VARCHAR(10)  DEFAULT 'INR',
  message    TEXT,
  status     VARCHAR(20)  DEFAULT 'pending',
  created_at TIMESTAMPTZ  DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_offers_signal_id ON offers(signal_id);
CREATE INDEX IF NOT EXISTS idx_offers_user_id   ON offers(user_id);
CREATE INDEX IF NOT EXISTS idx_offers_status    ON offers(status);

-- ─────────────────────────────────────────────────────────────────────────────
-- Reviews
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS reviews (
  id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  reviewer_id UUID REFERENCES users(id)   ON DELETE CASCADE,
  reviewee_id UUID REFERENCES users(id)   ON DELETE CASCADE,
  signal_id   UUID REFERENCES signals(id) ON DELETE SET NULL,
  rating      INTEGER CHECK (rating >= 1 AND rating <= 5),
  comment     TEXT,
  created_at  TIMESTAMPTZ DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_reviews_reviewer_id ON reviews(reviewer_id);
CREATE INDEX IF NOT EXISTS idx_reviews_reviewee_id ON reviews(reviewee_id);
CREATE INDEX IF NOT EXISTS idx_reviews_signal_id   ON reviews(signal_id);

-- ─────────────────────────────────────────────────────────────────────────────
-- Signal Views
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS signal_views (
  id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  signal_id  UUID REFERENCES signals(id) ON DELETE CASCADE,
  viewer_id  UUID REFERENCES users(id)   ON DELETE SET NULL,
  viewed_at  TIMESTAMPTZ DEFAULT now(),
  ip_address VARCHAR(45)
);

CREATE INDEX IF NOT EXISTS idx_signal_views_signal_id ON signal_views(signal_id);
CREATE INDEX IF NOT EXISTS idx_signal_views_viewer_id ON signal_views(viewer_id);
