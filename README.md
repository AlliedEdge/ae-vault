# AE Vault

<p align="center">
  <strong>A Cloud-Native File Storage Platform</strong>
</p>

<p align="center">
  Secure • Scalable • Dockerized • Cloud Ready
</p>

---

## Overview

AE Vault is a cloud-native file storage platform built with modern backend technologies and cloud-native architecture. It provides secure file management, efficient metadata storage, intelligent caching, and scalable object storage while following production-oriented software engineering practices.

The project is designed to demonstrate backend engineering, cloud deployment, containerization, caching strategies, and scalable system design.

---

## Features

- Secure User Authentication
- Folder Management
- File Upload & Download
- Metadata Management
- Redis-Based Caching
- PostgreSQL Persistence
- AWS S3 Object Storage
- Dockerized Deployment
- Nginx Reverse Proxy
- Cloud Deployment on AWS EC2

---

## Architecture

<p align="center">
  <img src="architecture/hld-v1.svg" alt="AE Vault Architecture" width="1000"/>
</p>

---

## Technology Stack

### Frontend

- React
- React Query
- Axios

### Backend

- Spring Boot
- Spring Security
- Spring Data JPA
- REST APIs

### Database

- PostgreSQL
- Redis

### Cloud

- AWS EC2
- AWS S3

### Infrastructure

- Docker
- Docker Compose
- Nginx

---

## Project Structure

```text
ae-vault/
│
├── architecture/
│
├── backend/
│
├── frontend/
│
├── docker/
│
├── docker-compose.yml
│
└── README.md
```

---

## Deployment Overview

- React communicates with the backend through Nginx.
- Nginx acts as a reverse proxy.
- Spring Boot handles business logic and API requests.
- PostgreSQL stores application metadata.
- Redis caches frequently accessed data.
- AWS S3 stores uploaded files.
- Docker containers are deployed on a single AWS EC2 instance.

---

## Getting Started

### Clone the repository

```bash
git clone https://github.com/AlliedEdge/ae-vault.git
```

```bash
cd ae-vault
```

---

### Start the application

```bash
docker compose up --build
```

---

## Roadmap

### Version 1

- [ ] User Authentication
- [ ] Folder Management
- [ ] File Upload
- [ ] File Download
- [ ] AWS S3 Integration
- [ ] Redis Caching
- [ ] Docker Deployment
- [ ] Nginx Reverse Proxy

### Future Releases

- [ ] File Sharing
- [ ] Role-Based Access Control
- [ ] File Versioning
- [ ] Search
- [ ] Email Verification
- [ ] Audit Logs
- [ ] Monitoring & Metrics
- [ ] Kubernetes Deployment

---

## Contributing

Contributions are welcome.

If you'd like to contribute:

1. Fork the repository.
2. Create a feature branch.

```bash
git checkout -b feature/your-feature-name
```

3. Commit your changes.

```bash
git commit -m "Add your feature"
```

4. Push to your fork.

```bash
git push origin feature/your-feature-name
```

5. Open a Pull Request.

Please ensure that:

- Code follows the project's coding standards.
- Changes are documented where appropriate.
- Pull Requests are focused and descriptive.

---

## Issues

Found a bug?

Have a feature request?

Please open an Issue describing the problem or enhancement.

---

## License

This project is licensed under the MIT License.

---

<p align="center">
Built with Spring Boot, Docker, Redis, PostgreSQL, Nginx, and AWS.
</p>