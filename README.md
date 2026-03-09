# DeployFast — Task Manager 


---

## Architecture

```
src/main/java/com/deployfast/taskmanager/
├── config/          ← SecurityConfig (Spring Security + JWT)
├── controller/      ← AuthController, TaskController, CategoryController
├── dto/
│   ├── request/     ← RegisterRequest, LoginRequest, StoreTaskRequest, UpdateTaskRequest
│   └── response/    ← ApiResponse<T>, AuthResponse, TaskResponse, UserResponse
├── entity/          ← User, Task, Category (JPA Entities)
├── exception/       ← GlobalExceptionHandler + exceptions métier
├── repository/      ← JPA Repositories avec requêtes JPQL
├── security/        ← JwtTokenProvider, JwtAuthenticationFilter, UserDetailsServiceImpl
└── service/         ← AuthService, TaskService, CategoryService
```

---

## Démarrage Rapide

### Prérequis
- Java 21+
- Docker & Docker Compose

### Lancer l'application
```bash
# 1. Cloner le projet
git clone https://github.com/deployfast/taskmanager.git
cd taskmanager

# 2. Configurer l'environnement
cp .env.example .env

# 3. Démarrer tous les services
docker compose up -d

# 4. Vérifier que l'app est démarrée
curl http://localhost:8080/actuator/health
```

---

## API Endpoints

### Authentification
```
POST   /api/v1/auth/register       Inscription
POST   /api/v1/auth/login          Connexion → JWT token
POST   /api/v1/auth/refresh        Renouveler le token
GET    /api/v1/auth/me             Profil utilisateur connecté
```

### Tâches (JWT requis)
```
GET    /api/v1/tasks               Liste paginée + filtres
POST   /api/v1/tasks               Créer une tâche
GET    /api/v1/tasks/{id}          Détail d'une tâche
PUT    /api/v1/tasks/{id}          Mise à jour complète
PATCH  /api/v1/tasks/{id}          Mise à jour partielle
DELETE /api/v1/tasks/{id}          Supprimer
PATCH  /api/v1/tasks/{id}/complete Marquer comme terminée
```

### Filtres disponibles sur GET /api/v1/tasks
```
?status=PENDING|IN_PROGRESS|COMPLETED|CANCELLED
?priority=LOW|MEDIUM|HIGH|URGENT
?search=mot-clé
?dueBefore=2026-12-31
?page=0&size=15&sortBy=createdAt&direction=desc
```

---

## Tests

```bash
# Lancer tous les tests
mvn test

# Avec couverture (rapport dans target/site/jacoco/)
mvn verify

# Couverture minimale requise : 60%
```

### Structure des tests
```
tests/
├── controller/
│   ├── AuthControllerTest.java   ← 7 tests d'intégration
│   └── TaskControllerTest.java   ← 10 tests d'intégration
├── service/
│   └── TaskServiceTest.java      ← 9 tests unitaires (Mockito)
└── security/
    └── JwtTokenProviderTest.java ← 7 tests unitaires
```

---

## Sécurité

| Protection | Implémentation |
|------------|----------------|
| Authentification | JWT (JJWT 0.12), tokens signés HS256 |
| Autorisation | Spring Security `@PreAuthorize`, contrôles manuels par ownership |
| Mots de passe | BCrypt cost 12 |
| Injections SQL | Hibernate/JPA requêtes préparées |
| Headers HTTP | X-Content-Type-Options, X-Frame-Options, HSTS, CSP |
| Rate Limiting | Via Spring Security (configurable) |
| CORS | Configuration restrictive par domaine |

---

## Pipeline CI/CD (GitHub Actions)

```
push/PR → main
    │
    ├── [1] Build & Tests 
    │
    ├── [2] SonarQube Analysis (SAST)
    │
    ├── [3] Security Scan (Trivy + OWASP Dependency-Check)
    │
    ├── [4] Docker Build & Push (multi-arch: amd64/arm64)
    │
    └── [5] Deploy to Production (SSH + zero-downtime)
```

### Secrets GitHub à configurer
```
SONAR_TOKEN          Token SonarQube
SONAR_HOST_URL       URL SonarQube 
DOCKERHUB_USERNAME   Login Docker Hub
DOCKERHUB_TOKEN      Token Docker Hub
DEPLOY_HOST          IP/domaine serveur de production
DEPLOY_USER          Utilisateur SSH
DEPLOY_SSH_KEY       Clé SSH privée
SLACK_WEBHOOK        URL webhook Slack (notifications)
```

---

## Variables d'Environnement

```env
# Base de données
DB_HOST=localhost
DB_PORT=3306
DB_NAME=taskmanager
DB_USER=taskuser
DB_PASSWORD=SecurePass@2024

# JWT
JWT_SECRET=votre-secret-256-bits-minimum
JWT_EXPIRATION=86400000        # 24h en ms
JWT_REFRESH=604800000          # 7j en ms
```


## Question 3: conception du pipeline ##
## Expliquer ##
la strategie de branches adoptee ( gitflow)
- gitflow a ete utliser enfin de faciliter le developpemnent et l'intgegration  continue de  l'application
- ## branch principale ##
-  main
-  develop
-  la branch secondaire on peut avoir le feature
-  ## les declancheur du pipeline (push, merge request,tag)
-  push: losque le developeur pousse sont  code ppour verifier si le code fonctionne
-  Merge Request:lorsque un merge est cree vers un develop
-  tag: est fait pour cree  une version stable prete pour le deploiemen
  ## Different etape du pipeline ##
  elle est composer de plusieur etape   qui es le :
  - le build
  - Test
  - Build
  - deploiement

    
           Developer
               │
               │
            Git Push
               │
             
        ┌───────────────┐
        │  CI Pipeline  │
        └───────────────┘
               │
            
        ┌───────────────┐
        │    BUILD      │
        │ mvn package   │
        └───────────────┘
               │
             
        ┌───────────────┐
        │     TESTS     │
        │   mvn test    │
        └───────────────┘
               │
              
        ┌───────────────┐
        │ CODE ANALYSIS │
        │   SonarQube   │
        └───────────────┘
               │
          
        ┌───────────────┐
        │ BUILD DOCKER  │
        │ docker build  │
        └───────────────┘
               │
             
        ┌───────────────┐
        │ PUSH IMAGE    │
        │ DockerHub     │
        └───────────────┘
               │
           
        ┌───────────────┐
        │  DEPLOYMENT   │
        │   Server│
        └───────────────┘


-  
