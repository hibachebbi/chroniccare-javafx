CREATE TABLE IF NOT EXISTS security_events (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NULL,
    email_attempted VARCHAR(255),
    event_type VARCHAR(100),
    severity VARCHAR(50),
    description TEXT,
    created_at DATETIME DEFAULT NOW(),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
);
