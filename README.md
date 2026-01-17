# 🐾 PetBuddy Backend Services

A production-ready, cloud-native microservices platform for pet care management. Built with modern DevOps practices, event-driven architecture, and optimized for scalability.

## 🏗️ Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                         ☁️ CLOUD BACKBONE (Free Tiers)                          │
├──────────────────┬──────────────────┬──────────────────┬───────────────────────┤
│     Supabase     │     Upstash      │   Cloudflare R2  │      CloudAMQP        │
│    PostgreSQL    │      Redis       │   S3 Storage     │      RabbitMQ         │
└────────┬─────────┴────────┬─────────┴────────┬─────────┴───────────┬───────────┘
         │                  │                  │                     │
         └──────────────────┴──────────────────┴─────────────────────┘
                                       │
┌──────────────────────────────────────┼──────────────────────────────────────────┐
│                        💻 LOCAL KUBERNETES (Kind)                               │
├─────────────────────────────────────────────────────────────────────────────────┤
│  ┌───────────┐  ┌───────────┐  ┌───────────┐  ┌───────────┐  ┌───────────┐     │
│  │   Auth    │  │  Profile  │  │   Feed    │  │Interaction│  │Gamification│    │
│  │  :8081    │  │   :8082   │  │   :8085   │  │   :8084   │  │   :8086   │     │
│  └─────┬─────┘  └─────┬─────┘  └─────┬─────┘  └─────┬─────┘  └─────┬─────┘     │
│        └──────────────┴──────────────┴──────────────┴──────────────┘           │
│                                      │                                          │
│  ┌───────────────────────────────────┴───────────────────────────────────┐     │
│  │                      NGINX Ingress Controller                          │     │
│  │                   localhost:80 → /auth, /users, etc.                   │     │
│  └────────────────────────────────────────────────────────────────────────┘     │
└───────────────────────────────────────────┬─────────────────────────────────────┘
                                            │
                                   ┌────────▼────────┐
                                   │     ArgoCD      │ ◄── GitOps Deployment
                                   └────────┬────────┘
                                            │ watches
                                   ┌────────▼────────┐
                                   │ petbuddy-gitops │ ◄── Kubernetes Manifests
                                   └─────────────────┘
```

## 🛠️ Tech Stack

| Layer                 | Technologies                                               |
| --------------------- | ---------------------------------------------------------- |
| **Backend**           | Java 21, Spring Boot 3.5, Spring Security, Spring Data JPA |
| **API Communication** | REST, gRPC (protobuf), WebSocket                           |
| **Messaging**         | RabbitMQ/CloudAMQP (Event-Driven Architecture)             |
| **Databases**         | PostgreSQL/Supabase (per-service), Redis/Upstash (caching) |
| **Storage**           | Cloudflare R2 / MinIO / AWS S3 (presigned URLs)            |
| **CI/CD**             | GitHub Actions (build & push to GHCR)                      |
| **Orchestration**     | Kubernetes (Kind), Kustomize                               |
| **GitOps**            | ArgoCD (automated sync & self-healing)                     |
| **Observability**     | Prometheus, Grafana, Spring Actuator                       |
| **Frontend**          | React Native, TypeScript, React Query                      |

## ☁️ Cloud Services (Free Tiers)

| Service            | Provider      | Free Tier Limits         |
| ------------------ | ------------- | ------------------------ |
| PostgreSQL         | Supabase      | 500MB storage, 50K MAU   |
| Redis              | Upstash       | 10K commands/day, 256MB  |
| S3 Storage         | Cloudflare R2 | 10GB, **no egress fees** |
| RabbitMQ           | CloudAMQP     | 100 queues, 10K messages |
| Container Registry | GitHub (GHCR) | Unlimited public images  |

## 📦 Microservices

| Service                      | Port | gRPC | Description                                      |
| ---------------------------- | ---- | ---- | ------------------------------------------------ |
| **AuthMicroservice**         | 8081 | -    | JWT auth, Twilio OTP, OAuth2, session management |
| **UserProfileMicroservice**  | 8082 | 9091 | User profiles, pets, follows, medical documents  |
| **SocialFeedMicroservice**   | 8085 | -    | Posts, media uploads, feed algorithms            |
| **InteractionService**       | 8084 | -    | Likes, comments, real-time counters              |
| **FeedDistributionService**  | 8083 | -    | Fan-out, celebrity handling, caching             |
| **GamificationMicroservice** | 8086 | 9090 | Points, badges, leaderboards, streaks            |

## ⚡ Key Optimizations

- **Hybrid Cloud Architecture**: Cloud infrastructure + local orchestration
- **Cache-First Design**: Redis L2 cache with async DB writes (<10ms response)
- **Dual MinIO Clients**: Internal operations vs. public presigned URLs
- **Batch gRPC**: `BatchGetUserInfo` for efficient feed enrichment
- **Optimistic Updates**: Cache-then-persist pattern for interactions
- **Virtual Threads**: Java 21 virtual threads for high concurrency
- **TLS Support**: Redis SSL (Upstash) and RabbitMQ SSL (CloudAMQP)

## 🔄 Event-Driven Flow

```
User Likes Post
     │
     ▼
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│  LikeService    │────▶│   CloudAMQP     │────▶│  Gamification   │
│ (interaction)   │     │  like.created   │     │   +10 points    │
└─────────────────┘     └─────────────────┘     └─────────────────┘
```

## 🚀 Quick Start

### Option 1: Docker Compose (Local Infrastructure)

```bash
# Clone repository
git clone https://github.com/yourusername/PetBuddyBackendServices.git
cd PetBuddyBackendServices

# Start infrastructure
docker-compose up -d postgres redis rabbitmq minio

# Run services (from each service directory)
./gradlew bootRun
```

### Option 2: Hybrid Cloud (Recommended for Portfolio)

```bash
# 1. Set up cloud resources (Supabase, Upstash, CloudAMQP, R2)

# 2. Push to GitHub to trigger CI/CD
git push origin main  # GitHub Actions builds & pushes images

# 3. Create local Kind cluster with ArgoCD
cd k8s
bash setup-cluster.sh

# 4. Update secrets and deploy via GitOps
# Edit petbuddy-gitops/base/secrets.yaml with cloud credentials
kubectl apply -f ../petbuddy-gitops/argocd/application.yaml
```

## 🔧 Local Kubernetes (Kind + ArgoCD)

```bash
# Create 3-node cluster with ingress
kind create cluster --config k8s/kind-config.yaml

# Install NGINX Ingress Controller
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/main/deploy/static/provider/kind/deploy.yaml

# Install ArgoCD
kubectl create namespace argocd
kubectl apply -n argocd -f https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml

# Get ArgoCD password
kubectl -n argocd get secret argocd-initial-admin-secret -o jsonpath="{.data.password}" | base64 -d

# Access ArgoCD UI
kubectl port-forward svc/argocd-server -n argocd 8080:443
# Open https://localhost:8080

# Deploy via GitOps
kubectl apply -f petbuddy-gitops/argocd/application.yaml
```

## 🔁 CI/CD Pipeline

```
┌──────────────┐     ┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│  Developer   │────▶│   GitHub     │────▶│   GitHub     │────▶│    GHCR      │
│  git push    │     │   Actions    │     │   Build JAR  │     │ Docker Image │
└──────────────┘     └──────────────┘     └──────────────┘     └──────┬───────┘
                                                                       │
┌──────────────┐     ┌──────────────┐     ┌──────────────┐            │
│    Pods      │◀────│    ArgoCD    │◀────│ petbuddy-    │◀───────────┘
│   Running    │     │   Sync       │     │   gitops     │
└──────────────┘     └──────────────┘     └──────────────┘
```

## 📊 API Endpoints (via Ingress)

| Path              | Service           | Swagger                                         |
| ----------------- | ----------------- | ----------------------------------------------- |
| `/auth/*`         | Auth Service      | `http://localhost/auth/swagger-ui.html`         |
| `/users/*`        | User Profile      | `http://localhost/users/swagger-ui.html`        |
| `/posts/*`        | Social Feed       | `http://localhost/posts/swagger-ui.html`        |
| `/interactions/*` | Interaction       | `http://localhost/interactions/swagger-ui.html` |
| `/feed/*`         | Feed Distribution | `http://localhost/feed/swagger-ui.html`         |
| `/gamification/*` | Gamification      | `http://localhost/gamification/swagger-ui.html` |

## 📁 Project Structure

```
PetBuddyBackendServices/
├── AuthMicroservice/              # JWT, OTP, OAuth2
├── UserProfileMicroservice/       # Profiles, Pets, Follows
├── SocialFeedMicroservice/
│   ├── social-feed/              # Posts & Media
│   ├── interaction/              # Likes & Comments
│   └── feedDistributionService/  # Fan-out & Caching
├── GamificationMicroservice/      # Points & Badges
├── ReactNative-PetBuddy/          # Mobile Frontend
├── k8s/
│   ├── kind-config.yaml          # 3-node cluster config
│   └── setup-cluster.sh          # Automated setup script
├── .github/workflows/
│   └── build-all.yml             # CI/CD matrix build
├── docker-compose.yml             # Local development
└── monitoring/                    # Prometheus & Grafana

petbuddy-gitops/                   # Separate GitOps repo
├── argocd/application.yaml        # ArgoCD Application
└── base/
    ├── namespace.yaml
    ├── secrets.yaml               # Cloud credentials
    ├── configmaps.yaml
    ├── ingress.yaml
    ├── kustomization.yaml
    └── services/                  # K8s Deployments
```

## 🧪 Testing

```bash
# Unit tests
./gradlew test

# Build without tests (for CI)
./gradlew build -x test

# Integration tests
./gradlew integrationTest
```

## 🔐 Environment Variables

All services support cloud-native configuration via environment variables:

| Variable               | Description                   | Default   |
| ---------------------- | ----------------------------- | --------- |
| `REDIS_HOST`           | Redis/Upstash host            | localhost |
| `REDIS_SSL_ENABLED`    | Enable TLS for Upstash        | false     |
| `RABBITMQ_HOST`        | RabbitMQ/CloudAMQP host       | localhost |
| `RABBITMQ_SSL_ENABLED` | Enable TLS for CloudAMQP      | false     |
| `RABBITMQ_VHOST`       | CloudAMQP virtual host        | /         |
| `S3_PUBLIC_ENDPOINT`   | Public URL for presigned URLs | -         |

## 🤝 Contributing

1. Fork the repository
2. Create feature branch (`git checkout -b feature/amazing-feature`)
3. Commit changes (`git commit -m 'Add amazing feature'`)
4. Push to branch (`git push origin feature/amazing-feature`)
5. Open Pull Request

## 📄 License

MIT License - see [LICENSE](LICENSE) for details.

---

**Built with ❤️ for pet lovers everywhere** | [GitOps Repo](https://github.com/yourusername/petbuddy-gitops)
