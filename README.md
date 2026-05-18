# URBANOVA - Shared Micromobility Platform

A shared scooter/bike management system built with React Native + Spring Boot.

## Introduction

URBANOVA is a shared micromobility platform including:
- **Mobile App** (React Native): Find nearby vehicles, scan QR code to ride, online payment
- **Admin Dashboard** (Vue 3): Vehicle management, order management, user management, analytics
- **Backend Service** (Spring Boot): REST API, JWT authentication, MySQL storage

## Tech Stack

| Module | Technology |
|--------|------------|
| Mobile | React Native, Expo, TanStack Query, Zustand |
| Admin Dashboard | Vue 3, Vite, Element Plus, Pinia |
| Backend | Spring Boot 3, MyBatis-Plus, JWT |
| Database | MySQL 8.0 |
| Map | react-native-maps + Google Maps API |
| Deployment | Nginx, EAS Build |

## Prerequisites

| Tool | Version |
|------|---------|
| Java | 17+ |
| Node.js | 20+ |
| MySQL | 8.0+ |
| Maven | 3.8+ |
| npm | 10+ |

## Features

| Module | Features |
|--------|----------|
| Mobile | Map view, Vehicle reservation, Order management, Wallet top-up |
| Admin Dashboard | Vehicle CRUD, Order management, User management, Analytics dashboard |
| Backend | JWT auth, GPS-based vehicle query, Payment simulation, Email notification |

## Quick Start
### 0. Preparation
Make sure you have replaced all `$` variables with actual values before running the project.
${BackendIP} to Server Address(If everything is running locally, change it to localhost.)
all `$` variables in application.properties

run the .sql in mysql to build database

in app.json need replaced androidApiKey,iosApiKey,projectId with actual values

### 1. Backend
cd urbanova
mvn clean package
java -jar target/urbanova-0.0.1-SNAPSHOT.jar

### 2. Mobile App
cd urbanova-mobile-frontend
npm install
npx expo start

### Admin Dashboard
cd urbanova-frontend
npm install
npm run dev

## Project Structure
urbanova-e-scooter-system/
├── urbanova/                    # Backend
├── urbanova-mobile-frontend/    # Mobile App
├── urbanova-frontend/           # Admin Dashboard
└── README.md

## Deployment
# Backend
nohup java -jar urbanova-0.0.1-SNAPSHOT.jar > app.log 2>&1 &

# Frontend (Nginx)
npm run build
--Upload dist to server and configure Nginx