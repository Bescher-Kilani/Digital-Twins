# DigiTwin Studio - Docker Setup

This project has been containerized using Docker Compose for easy development and deployment.

## 🚀 Quick Start

1. **Prerequisites**: Make sure you have Docker and Docker Compose installed
2. **Start all services**: Run the following command in the project root:
   ```bash
   docker-compose up -d
   ```
3. **Access the application**:
   - **Frontend (React)**: http://localhost:3000
   - **Backend (Spring Boot)**: http://localhost:9090
   - **Keycloak (Auth)**: http://localhost:8080
   - **MongoDB**: localhost:27017

## 📦 Services

- **Frontend**: React application with Vite, served by Nginx
- **Backend**: Spring Boot application with Java 21
- **Keycloak**: Identity provider (v24.0.2) with pre-configured DigiTwinStudio realm
- **MongoDB**: Database service

## 🔐 Keycloak Configuration

The Keycloak service comes pre-configured with:
- **DigiTwinStudio realm** - Automatically imported on startup
- **Admin credentials**: admin/admin123 (can be changed in docker-compose.yml)
- **Pre-configured users and clients** from the previous setup
- **Multi-environment support** - Works with both localhost:3000 (Docker) and localhost:5173 (dev)

## 🛠️ Development

- All services will start automatically with `docker-compose up -d`
- The backend will wait for MongoDB and Keycloak to be ready
- JWT tokens issued by Keycloak work seamlessly between frontend and backend
- Hot reloading is available for development (modify docker-compose.yml if needed)

## 🗂️ Important Files

- `docker-compose.yml` - Main orchestration file
- `keycloak-import/` - Contains the DigiTwinStudio realm configuration
- `Frontend/DigiTwinStudio/Dockerfile` - Frontend container definition
- `Backend/Dockerfile` - Backend container definition

## 🔧 Troubleshooting

### Common Issues

1. **JWT validation errors**: The backend has been configured to accept JWT tokens from both external (localhost:8080) and internal (keycloak:8080) Keycloak URLs
2. **Content Security Policy errors**: The Keycloak client configuration supports both development and production URLs

### Debug Commands

- Check container logs: `docker-compose logs [service-name]`
- Restart services: `docker-compose restart`
- Full reset: `docker-compose down && docker-compose up -d`
- Check specific service: `docker-compose ps [service-name]`

## 🌐 Environment Variables

You can customize the setup by creating a `.env` file with:
```
MONGODB_ROOT_USERNAME=admin
MONGODB_ROOT_PASSWORD=password123
KEYCLOAK_ADMIN=admin
KEYCLOAK_ADMIN_PASSWORD=admin123
FRONTEND_PORT=3000
BACKEND_PORT=9090
KEYCLOAK_PORT=8080
MONGODB_PORT=27017
```

## ⚡ Performance Notes

- Backend health checks are configured for proper startup sequencing
- Keycloak import happens automatically on first startup
- MongoDB uses persistent volumes for data retention

Update:
docker-compose down
docker-compose build <part> e.g frontend, backend
