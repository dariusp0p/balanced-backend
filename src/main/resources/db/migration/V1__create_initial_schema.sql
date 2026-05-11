CREATE TABLE users (
                       id BIGSERIAL PRIMARY KEY,

                       email VARCHAR(255) NOT NULL UNIQUE,
                       password_hash VARCHAR(255) NOT NULL,

                       first_name VARCHAR(255) NOT NULL,
                       last_name VARCHAR(255),

                       is_admin BOOLEAN NOT NULL DEFAULT FALSE,

                       created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE foods (
                       id BIGSERIAL PRIMARY KEY,

                       name VARCHAR(255) NOT NULL,
                       brand VARCHAR(255),

                       source VARCHAR(50) NOT NULL,

                       external_id VARCHAR(255),

                       serving_size DOUBLE PRECISION,
                       serving_unit VARCHAR(50),

                       calories_per_100g DOUBLE PRECISION NOT NULL,
                       protein_per_100g DOUBLE PRECISION NOT NULL,
                       carbs_per_100g DOUBLE PRECISION NOT NULL,
                       fats_per_100g DOUBLE PRECISION NOT NULL,

                       raw_source_json TEXT,

                       created_by_user_id BIGINT,

                       created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                       updated_at TIMESTAMP WITH TIME ZONE NOT NULL,

                       CONSTRAINT fk_foods_created_by_user
                           FOREIGN KEY (created_by_user_id)
                               REFERENCES users(id)
                               ON DELETE SET NULL
);

CREATE TABLE food_log_groups (
                                 id BIGSERIAL PRIMARY KEY,

                                 user_id BIGINT NOT NULL,

                                 name VARCHAR(255) NOT NULL,
                                 meal_type VARCHAR(50) NOT NULL,

                                 date DATE NOT NULL,

                                 compute_from_food_logs BOOLEAN NOT NULL,

                                 total_calories DOUBLE PRECISION NOT NULL,
                                 total_protein DOUBLE PRECISION NOT NULL,
                                 total_carbs DOUBLE PRECISION NOT NULL,
                                 total_fats DOUBLE PRECISION NOT NULL,

                                 created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                                 updated_at TIMESTAMP WITH TIME ZONE NOT NULL,

                                 CONSTRAINT fk_food_log_groups_user
                                     FOREIGN KEY (user_id)
                                         REFERENCES users(id)
                                         ON DELETE CASCADE
);

CREATE TABLE food_logs (
                           id BIGSERIAL PRIMARY KEY,

                           user_id BIGINT NOT NULL,
                           group_id BIGINT,
                           food_id BIGINT,

                           name VARCHAR(255) NOT NULL,

                           quantity DOUBLE PRECISION NOT NULL,
                           unit VARCHAR(50) NOT NULL,

                           calories DOUBLE PRECISION NOT NULL,
                           protein DOUBLE PRECISION NOT NULL,
                           carbs DOUBLE PRECISION NOT NULL,
                           fats DOUBLE PRECISION NOT NULL,

                           notes TEXT,

                           date DATE NOT NULL,
                           time TIME NOT NULL,

                           created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                           updated_at TIMESTAMP WITH TIME ZONE NOT NULL,

                           CONSTRAINT fk_food_logs_user
                               FOREIGN KEY (user_id)
                                   REFERENCES users(id)
                                   ON DELETE CASCADE,

                           CONSTRAINT fk_food_logs_group
                               FOREIGN KEY (group_id)
                                   REFERENCES food_log_groups(id)
                                   ON DELETE SET NULL,

                           CONSTRAINT fk_food_logs_food
                               FOREIGN KEY (food_id)
                                   REFERENCES foods(id)
                                   ON DELETE SET NULL
);

CREATE INDEX idx_users_email
    ON users(email);

CREATE INDEX idx_foods_created_by_user_id
    ON foods(created_by_user_id);

CREATE INDEX idx_foods_name
    ON foods(name);

CREATE INDEX idx_foods_source_external_id
    ON foods(source, external_id);

CREATE INDEX idx_food_log_groups_user_id
    ON food_log_groups(user_id);

CREATE INDEX idx_food_log_groups_user_id_date
    ON food_log_groups(user_id, date);

CREATE INDEX idx_food_log_groups_user_id_meal_type
    ON food_log_groups(user_id, meal_type);

CREATE INDEX idx_food_logs_user_id
    ON food_logs(user_id);

CREATE INDEX idx_food_logs_user_id_date
    ON food_logs(user_id, date);

CREATE INDEX idx_food_logs_user_id_group_id
    ON food_logs(user_id, group_id);

CREATE INDEX idx_food_logs_food_id
    ON food_logs(food_id);