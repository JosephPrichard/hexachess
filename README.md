# Hexagonal Chess
A website to play hexagonal chess online.

Created using Java, Javascript, Jooby, Handlebars, Postgres, and Redis.

## Build and Deployment

### Run Keydb

`docker pull eqalpha/keydb`

`docker run -d --name keydb -p 6379:6379 eqalpha/keydb`

### Run Postgres

`docker pull postgres`

`docker run -d --name postgres -p 5432:5432 postgres`

### Build Server

``

### Run Server