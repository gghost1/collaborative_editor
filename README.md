# Collaborative editor

## Collaborative drawing canvas with update merging

It's real-time collaborative editor for drawings with multiple clients. Synchronization realized via message broker and pub/sub pattern system used for safe storing in database.

## How to use

- In order to start this application you must have docker compose version > v2.6.0
- Write this command in termianal:

          docker-compose up --build


Also, you can configurate time how often data will be merge with canvas from database.

For this you need modify update.interval={TIME_IN_SECONDS} in application.properties in backend directory.

### Use it with pleasure!
