# Collaborative Editor

![React](https://img.shields.io/badge/Frontend-React-blue?logo=react)
![Redux](https://img.shields.io/badge/State-Redux-purple?logo=redux)
![Spring Boot](https://img.shields.io/badge/Backend-SpringBoot-brightgreen?logo=spring)
![Kafka](https://img.shields.io/badge/Broker-Kafka-black?logo=apachekafka)
![PostgreSQL](https://img.shields.io/badge/DB-PostgreSQL-blue?logo=postgresql)
![Python](https://img.shields.io/badge/Merge%20Service-Python-yellow?logo=python)
![NGINX](https://img.shields.io/badge/Proxy-NGINX-darkgreen?logo=nginx)
![Docker](https://img.shields.io/badge/Deploy-Docker-blue?logo=docker)

---

## Live Demo

🌐 [collaborative-editor.duckdns.org](http://collaborative-editor.duckdns.org)

---

## Overview

Collaborative Editor is a real-time drawing canvas that allows multiple users to draw together, with instant synchronization, persistent storage, and robust backend architecture.

---

## Technologies

- **Frontend:** React + Redux, STOMP over WebSockets
- **Backend:** Spring Boot (Java), WebSocket/STOMP, Kafka integration
- **Merge Service:** Python, merges and saves canvas to PostgreSQL
- **Database:** PostgreSQL (stores canvas as JSON)
- **NGINX:** Reverse proxy, SPA fallback, gzip, load balancing ready
- **Docker Compose:** For easy deployment and orchestration

---

## Quick Start

### 1. Prerequisites
- Docker & Docker Compose v2.6.0+
- Open ports 80 (and 443 for HTTPS, optional)

### 2. Clone & Run
```bash
git clone https://github.com/yourusername/collaborative_editor.git
cd collaborative_editor
docker compose up --build
```

### 3. Open in Browser
- Go to [http://localhost](http://localhost) (local)
- Or [http://collaborative-editor.duckdns.org](http://collaborative-editor.duckdns.org) (production)

---

## Configuration

- **Backend config:** `backend/src/main/resources/application.properties`
- **Frontend config:** `frontend/` (see `nginx.conf` for proxy rules)
- **Domain:** Make sure your DNS A-record points to your server's IP.

---

## Features
- Real-time collaborative drawing
- Room-based canvas (unique URL for each room)
- Persistent storage (PostgreSQL)
- WebSocket synchronization (STOMP)
- Kafka for event streaming
- Python merge service for efficient storage
- SPA fallback for React Router
- Gzip compression for fast loading
- Dockerized deployment

---

## Production Deployment

- The app is deployed at: [collaborative-editor.duckdns.org](http://collaborative-editor.duckdns.org)
- Uses Docker Compose for all services
- NGINX handles HTTPS, proxying, and static assets
- For HTTPS, use [Let's Encrypt](https://letsencrypt.org/) and update nginx config

---
