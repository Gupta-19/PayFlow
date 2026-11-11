CREATE TABLE users (
    id            UUID PRIMARY KEY,
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE wallets (
    id         UUID PRIMARY KEY,
    user_id    UUID UNIQUE REFERENCES users(id),        -- NULL only for the SYSTEM wallet
    kind       VARCHAR(10) NOT NULL DEFAULT 'USER',     -- USER | SYSTEM
    balance    BIGINT      NOT NULL DEFAULT 0,          -- paise
    currency   VARCHAR(3)  NOT NULL DEFAULT 'INR',
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT user_balance_not_negative CHECK (kind = 'SYSTEM' OR balance >= 0)
);

CREATE TABLE payments (
    id              UUID PRIMARY KEY,
    type            VARCHAR(10)  NOT NULL,              -- TOPUP | TRANSFER
    from_wallet_id  UUID         NOT NULL REFERENCES wallets(id),
    to_wallet_id    UUID         NOT NULL REFERENCES wallets(id),
    amount          BIGINT       NOT NULL CHECK (amount > 0),
    status          VARCHAR(10)  NOT NULL,              -- COMPLETED
    note            VARCHAR(140),
    created_by      UUID         NOT NULL REFERENCES users(id),
    idempotency_key VARCHAR(100) NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT one_payment_per_key UNIQUE (created_by, idempotency_key),
    CONSTRAINT no_self_payment CHECK (from_wallet_id <> to_wallet_id)
);

CREATE TABLE ledger_entries (
    id         UUID PRIMARY KEY,
    payment_id UUID        NOT NULL REFERENCES payments(id),
    wallet_id  UUID        NOT NULL REFERENCES wallets(id),
    direction  VARCHAR(6)  NOT NULL CHECK (direction IN ('DEBIT', 'CREDIT')),
    amount     BIGINT      NOT NULL CHECK (amount > 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_ledger_wallet_time  ON ledger_entries (wallet_id, created_at DESC);
CREATE INDEX idx_payments_from_time  ON payments (from_wallet_id, created_at DESC);

-- The SYSTEM wallet funds every top-up; its balance goes negative by design
INSERT INTO wallets (id, kind) VALUES ('00000000-0000-0000-0000-000000000001', 'SYSTEM');
