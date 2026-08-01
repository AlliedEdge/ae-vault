<div align="center">

# 🚀 Ziboto

### Cloud-Native File Storage Platform

[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Docker](https://img.shields.io/badge/Docker-Ready-2496ED.svg)](https://www.docker.com/)
[![AWS](https://img.shields.io/badge/AWS-Deployed-FF9900.svg)](https://aws.amazon.com/)

**Secure** • **Scalable** • **Production-Ready**

[Features](#-features) • [Architecture](#-architecture) • [Quick Start](#-quick-start) • [Roadmap](#-roadmap)

</div>

---

## 📖 Overview

**Ziboto** is a modern, cloud-native file storage platform engineered with enterprise-grade backend technologies and cloud-first architecture. Built to showcase professional software engineering practices, Ziboto delivers secure file management, intelligent caching, scalable object storage, and robust metadata persistence.

### Why Ziboto?

- **Production-Ready Architecture** – Designed with scalability and reliability in mind
- **Cloud-Native Design** – Built for AWS with containerized deployment
- **Modern Tech Stack** – Leveraging Spring Boot, React, PostgreSQL, Redis, and AWS S3
- **Developer-Friendly** – Fully dockerized for seamless local development and deployment

---

## ✨ Features

<table>
<tr>
<td width="50%">

### 🔐 Security & Authentication
- JWT-based user authentication
- Role-based access control (planned)
- Secure file access management

### 📁 File Management
- Hierarchical folder structure
- Multi-file upload support
- Fast download with streaming
- Metadata tracking and search

</td>
<td width="50%">

### ⚡ Performance & Scale
- Redis-based intelligent caching
- PostgreSQL for reliable persistence
- AWS S3 for scalable object storage
- Optimized query performance

### 🐳 DevOps Ready
- Fully containerized with Docker
- Docker Compose orchestration
- Nginx reverse proxy
- AWS EC2 cloud deployment

</td>
</tr>
</table>

---

## 🏗️ Architecture

<div align="center">
  <img src="architecture/hld-v1.svg" alt="Ziboto System Architecture" width="100%"/>
  
  *High-Level Architecture: Cloud-native design with microservices-ready structure*
</div>

### Architecture Highlights

- **Frontend Layer**: React SPA with optimistic UI updates via React Query
- **API Gateway**: Nginx reverse proxy for load balancing and SSL termination
- **Application Layer**: Spring Boot REST APIs with JWT authentication
- **Cache Layer**: Redis for session management and frequently accessed data
- **Persistence Layer**: PostgreSQL for metadata and relational data
- **Storage Layer**: AWS S3 for scalable object storage

---

## 🛠️ Technology Stack

<div align="center">

| Layer | Technologies |
|-------|-------------|
| **Frontend** | React • React Query • Axios • Modern UI |
| **Backend** | Spring Boot 3.x • Spring Security • Spring Data JPA • RESTful APIs |
| **Database** | PostgreSQL 15+ • Redis 7+ |
| **Cloud** | AWS EC2 • AWS S3 • AWS VPC |
| **DevOps** | Docker • Docker Compose • Nginx • Git |

</div>

---

## 📂 Project Structure

```text
ziboto/
│
├── architecture/          # System architecture diagrams and docs
│   └── hld-v1.svg
│
├── backend/              # Spring Boot application
│   ├── src/
│   └── pom.xml
│
├── frontend/             # React application
│   ├── src/
│   └── package.json
│
├── docker/               # Docker configurations
│   ├── backend.Dockerfile
│   ├── frontend.Dockerfile
│   └── nginx.conf
│
├── docker-compose.yml    # Multi-container orchestration
└── README.md            # This file
```

---

## 🚀 Quick Start

### Prerequisites

- Docker Engine 20.10+
- Docker Compose 2.0+
- Git

### Installation

1️⃣ **Clone the repository**

```bash
git clone https://github.com/yourusername/ziboto.git
cd ziboto
```

2️⃣ **Configure environment variables**

```bash
# Create .env file with your AWS credentials
cp .env.example .env
# Edit .env with your configuration
```

3️⃣ **Launch the application**

```bash
docker compose up --build
```

4️⃣ **Access Ziboto**

- **Frontend**: http://localhost
- **Backend API**: http://localhost/api
- **API Docs**: http://localhost/api/swagger-ui.html

### Development Mode

```bash
# Start backend only
docker compose up backend postgres redis

# Start frontend in dev mode (in another terminal)
cd frontend && npm run dev
```

---

## 🗺️ Roadmap

### 🎯 Version 1.0 (Current Sprint)

- [x] Project architecture and setup
- [ ] User authentication & JWT integration
- [ ] Folder hierarchy management
- [ ] File upload with chunking
- [ ] File download with streaming
- [ ] AWS S3 integration
- [ ] Redis caching layer
- [ ] Docker multi-container deployment
- [ ] Nginx reverse proxy configuration

### 🔮 Version 2.0 (Future Enhancements)

- [ ] File sharing with expirable links
- [ ] Role-based access control (RBAC)
- [ ] File versioning and history
- [ ] Full-text search with Elasticsearch
- [ ] Email verification workflow
- [ ] Comprehensive audit logging
- [ ] Prometheus metrics & Grafana dashboards
- [ ] Kubernetes deployment with Helm charts
- [ ] CI/CD pipeline with GitHub Actions

### 💡 Ideas for Contribution

- Real-time collaboration features
- Mobile app (React Native)
- File preview for common formats
- Automatic virus scanning
- Multi-region replication

---

## 🤝 Contributing

We welcome contributions from the community! Whether you're fixing bugs, improving documentation, or proposing new features, your input is valuable.

### How to Contribute

1. **Fork the repository** and create your branch from `main`

```bash
git checkout -b feature/amazing-feature
```

2. **Make your changes** and commit with descriptive messages

```bash
git commit -m "feat: add amazing new feature"
```

3. **Push to your fork**

```bash
git push origin feature/amazing-feature
```

4. **Open a Pull Request** with a clear description of changes

### Contribution Guidelines

- ✅ Follow existing code style and conventions
- ✅ Write clear commit messages (use [Conventional Commits](https://www.conventionalcommits.org/))
- ✅ Add tests for new features
- ✅ Update documentation as needed
- ✅ Keep PRs focused and atomic

---

## 🐛 Issues & Support

Encountered a bug? Have a feature request? We'd love to hear from you!

- **Bug Reports**: [Open an issue](https://github.com/yourusername/ziboto/issues/new?template=bug_report.md)
- **Feature Requests**: [Request a feature](https://github.com/yourusername/ziboto/issues/new?template=feature_request.md)
- **Questions**: Check existing [discussions](https://github.com/yourusername/ziboto/discussions) or start a new one

---

## 📄 License

This project is licensed under the **MIT License** - see the [LICENSE](LICENSE) file for details.

---

## 🙏 Acknowledgments

Built with powerful open-source technologies:
- [Spring Boot](https://spring.io/projects/spring-boot) - Application framework
- [React](https://react.dev/) - Frontend library
- [PostgreSQL](https://www.postgresql.org/) - Database
- [Redis](https://redis.io/) - Caching layer
- [Docker](https://www.docker.com/) - Containerization
- [AWS](https://aws.amazon.com/) - Cloud infrastructure

---

<div align="center">

**⭐ Star this repo if you find it helpful!**

Made with ❤️ by the Ziboto team

[Report Bug](https://github.com/yourusername/ziboto/issues) • [Request Feature](https://github.com/yourusername/ziboto/issues) • [Documentation](https://github.com/yourusername/ziboto/wiki)

</div>