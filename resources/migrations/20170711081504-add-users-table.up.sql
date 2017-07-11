CREATE TABLE users
       (id UUID PRIMARY KEY,
        email VARCHAR(30),
        admin BOOLEAN,
        is_active BOOLEAN,
        token TEXT);
