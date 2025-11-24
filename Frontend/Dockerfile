# Multi-stage build für React Frontend
# Build Stage
FROM node:20-alpine AS build
WORKDIR /app

# Copy package files FROM ROOT
COPY Frontend/package.json Frontend/yarn.lock ./

# Install dependencies
RUN yarn install --frozen-lockfile

# Copy source code FROM ROOT
COPY Frontend/ .

# Build for production
RUN yarn build

# Runtime Stage
FROM nginx:alpine

# Copy custom nginx config FROM ROOT
COPY Frontend/nginx.conf /etc/nginx/conf.d/default.conf

# Copy built files from build stage
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=10s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:80/ || exit 1

CMD ["nginx", "-g", "daemon off;"]