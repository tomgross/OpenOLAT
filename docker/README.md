
# Development with docker support

# Database setup with Docker

```
$ docker-compose up -d
$ docker exec  8b70ded71361 bash -c "psql -U openolat  openolat < setupDatabase.sql"
```
