-- V1__init_docmind.sql

CREATE EXTENSION IF NOT EXISTS pgcrypto;
CREATE EXTENSION IF NOT EXISTS vector;

-- =========================
-- USERS
-- =========================

CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(50) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'USER'
        CHECK (role IN ('USER', 'ADMIN')),
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- =========================
-- DOCUMENTS
-- =========================

CREATE TABLE IF NOT EXISTS documents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    filename VARCHAR(255) NOT NULL,
    file_type VARCHAR(20) NOT NULL,
    file_size_bytes BIGINT,
    total_chunks INTEGER NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'PROCESSING'
        CHECK (status IN ('PROCESSING', 'READY', 'FAILED')),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_documents_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- =========================
-- CHAT SESSIONS
-- =========================

CREATE TABLE IF NOT EXISTS chat_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    document_id UUID,
    user_query TEXT NOT NULL,
    llm_response TEXT NOT NULL,
    retrieved_chunks JSONB NOT NULL DEFAULT '[]',
    response_time_ms INTEGER,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_chat_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_chat_document
        FOREIGN KEY (document_id) REFERENCES documents(id) ON DELETE SET NULL
);

-- =========================
-- INDEXES
-- =========================

CREATE INDEX IF NOT EXISTS idx_documents_user_id ON documents(user_id);
CREATE INDEX IF NOT EXISTS idx_documents_filename ON documents(filename);
CREATE INDEX IF NOT EXISTS idx_sessions_user_id ON chat_sessions(user_id);
CREATE INDEX IF NOT EXISTS idx_sessions_document_id ON chat_sessions(document_id);

-- =========================
-- AUTO UPDATE updated_at
-- =========================

CREATE OR REPLACE FUNCTION update_timestamp()
RETURNS TRIGGER AS
$$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_documents_updated_at
BEFORE UPDATE ON documents
FOR EACH ROW
EXECUTE FUNCTION update_timestamp();