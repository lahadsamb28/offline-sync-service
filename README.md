# 🐰 Offline Sync Service avec RabbitMQ - Guide Complet

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.0-brightgreen)](https://spring.io/projects/spring-boot)
[![RabbitMQ](https://img.shields.io/badge/RabbitMQ-3.12-orange)](https://www.rabbitmq.com/)
[![Angular](https://img.shields.io/badge/Angular-17%2B-red)](https://angular.io/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-blue)](https://www.postgresql.org/)

Microservice de synchronisation asynchrone des pointages offline avec support de **2 types de pointages** :
- 🏢 **STANDARD** : Pointage avec matricule (badge/terminal)
- 📍 **TERRAIN** : Pointage GPS sans matricule (mobile)

## 🎯 Architecture Complète

```
┌─────────────────────────────────────────────────────────────┐
│                   FRONTEND ANGULAR                          │
│                                                             │
│  ┌──────────────────┐         ┌──────────────────┐        │
│  │ Pointage Standard│         │ Pointage Terrain │        │
│  │  (matricule)     │         │     (GPS)        │        │
│  └────────┬─────────┘         └────────┬─────────┘        │
│           │                             │                   │
│           └─────────────┬───────────────┘                   │
│                         ▼                                   │
│              OfflineSyncService.ts                          │
│        (2 queues locales séparées)                         │
└────────────────────┬────────────────────────────────────────┘
                     │ HTTP REST API
                     ▼
┌─────────────────────────────────────────────────────────────┐
│           OFFLINE SYNC SERVICE (Port 8082)                  │
│                                                             │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  Controller                                          │  │
│  │  /pointages/sync         → Standard                  │  │
│  │  /pointages/terrain/sync → Terrain                   │  │
│  └────────────────┬─────────────────────────────────────┘  │
│                   │                                         │
│  ┌────────────────▼─────────────────────────────────────┐  │
│  │  OfflineSyncService                                  │  │
│  │  - Validation                                        │  │
│  │  - Création SyncBatch (BDD)                         │  │
│  └────────────────┬─────────────────────────────────────┘  │
│                   │                                         │
│  ┌────────────────▼─────────────────────────────────────┐  │
│  │  RabbitMQProducer                                    │  │
│  │  sendSyncMessage() → Queue                          │  │
│  └──────────────────────────────────────────────────────┘  │
└────────────────────┬────────────────────────────────────────┘
                     │ AMQP
                     ▼
┌─────────────────────────────────────────────────────────────┐
│                    RABBITMQ BROKER                          │
│                                                             │
│  Exchange: pointage.sync.exchange (Direct)                  │
│  Queue: pointage.offline.queue                              │
│  DLQ: pointage.offline.dlq.queue                           │
│                                                             │
│  Configuration:                                             │
│  - Prefetch: 5 messages                                     │
│  - TTL: 1 heure                                            │
│  - Retry: 3 fois avec backoff exponentiel                  │
│  - Concurrence: 3-10 consumers                             │
└────────────────────┬────────────────────────────────────────┘
                     │ AMQP Consumer
                     ▼
┌─────────────────────────────────────────────────────────────┐
│         OFFLINE SYNC SERVICE - Consumer                     │
│                                                             │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  @RabbitListener                                     │  │
│  │  consumeSyncMessage(SyncMessageDto)                  │  │
│  └────────────────┬─────────────────────────────────────┘  │
│                   │                                         │
│  ┌────────────────▼─────────────────────────────────────┐  │
│  │  Type = STANDARD ?                                   │  │
│  │    → processBatchStandard()                         │  │
│  │    → PointageServiceClient                          │  │
│  │      .enregistrerPointagesOffline()                 │  │
│  │                                                      │  │
│  │  Type = TERRAIN ?                                    │  │
│  │    → processBatchTerrain()                          │  │
│  │    → PointageServiceClient                          │  │
│  │      .enregistrerPointagesOfflineTerrain()          │  │
│  └──────────────────────────────────────────────────────┘  │
└────────────────────┬────────────────────────────────────────┘
                     │ HTTP Feign Client
                     ▼
┌─────────────────────────────────────────────────────────────┐
│          POINTAGE SERVICE (Port 8081)                       │
│                                                             │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  PointageController                                  │  │
│  │  /api/v1/pointages/offline/batch                     │  │
│  │  /api/v1/pointages/offline/terrain/batch            │  │
│  └────────────────┬─────────────────────────────────────┘  │
│                   │                                         │
│  ┌────────────────▼─────────────────────────────────────┐  │
│  │  PointageService                                     │  │
│  │  - enregistrerPointagesOffline()                     │  │
│  │  - enregistrerPointagesOfflineTerrain()             │  │
│  │  - Business Logic                                    │  │
│  │  - Calculs temps/ponctualité                        │  │
│  └────────────────┬─────────────────────────────────────┘  │
│                   │                                         │
│                   ▼                                         │
│              PostgreSQL DB                                  │
│          (pointage_journalier,                             │
│           pointage_detail)                                  │
└─────────────────────────────────────────────────────────────┘
```

## 📋 DTOs et Structures de Données

### Pointage STANDARD
```json
{
  "pointages": [
    {
      "matricule": 12345,
      "heurePointage": "2024-12-13T08:30:00"
    }
  ]
}
```

### Pointage TERRAIN
```json
{
  "pointages": [
    {
      "heurePointage": "2024-12-13T08:30:00",
      "latitude": 14.6937,
      "longitude": -17.4441
    }
  ]
}
```

## 🚀 Installation et Configuration

### 1. Clone le Projet
```bash
git clone https://github.com/lahadsamb28/offline-sync-service
cd offline-sync-service
```

### 2. Lancer avec Docker Compose
```bash
# Démarrer PostgreSQL + RabbitMQ + Service
docker-compose up -d

# Vérifier les logs
docker-compose logs -f offline-sync-service

# Accéder à RabbitMQ Management
http://localhost:15672 (guest/guest)
```

### 3. Configuration du Pointage Service

Dans votre microservice de pointage, ajoutez ces endpoints :

#### Controller
```java
@PostMapping("/api/v1/pointages/offline/batch")
public ResponseEntity<List<PointageBatchDto>> enregistrerOffline(
        @RequestParam String email,
        @RequestBody PointageBatchRequestDto request) {
    return ResponseEntity.ok(pointageService.enregistrerPointagesOffline(email, request));
}

@PostMapping("/api/v1/pointages/offline/terrain/batch")
public ResponseEntity<List<PointageBatchDto>> enregistrerOfflineTerrain(
        @RequestParam String email,
        @RequestBody PointageTerrainBatchRequestDto request,
        @RequestHeader("X-User-Privileges") List<String> privileges) {
    return ResponseEntity.ok(
        pointageService.enregistrerPointagesOfflineTerrain(email, request, privileges)
    );
}
```

## 📱 Utilisation Frontend Angular

### Pointage STANDARD (avec badge)
```typescript
import { OfflineSyncService } from './services/offline-sync.service';

// Dans votre component
constructor(private offlineSyncService: OfflineSyncService) {}

// Ajouter un pointage standard
enregistrerPointageStandard(matricule: number) {
  const pointage = {
    matricule: matricule,
    heurePointage: new Date().toISOString()
  };

  if (this.offlineSyncService.isOnline()) {
    // Ajouter à la queue
    this.offlineSyncService.addPointageStandardToQueue(pointage);
    
    // Synchroniser immédiatement
    this.offlineSyncService.syncPointagesStandard().subscribe({
      next: (response) => {
        console.log('Batch envoyé:', response.batchId);
        this.toastr.success('Pointage en cours de traitement');
      },
      error: (error) => {
        this.toastr.error('Erreur lors de l\'envoi');
      }
    });
  } else {
    // Mode offline
    this.offlineSyncService.addPointageStandardToQueue(pointage);
    this.toastr.warning('Pointage enregistré hors ligne');
  }
}
```

### Pointage TERRAIN (mobile avec GPS)
```typescript
// Obtenir la position GPS
async enregistrerPointageTerrain() {
  try {
    const position = await this.getCurrentPosition();
    
    const pointage = {
      heurePointage: new Date().toISOString(),
      latitude: position.coords.latitude,
      longitude: position.coords.longitude
    };

    if (this.offlineSyncService.isOnline()) {
      this.offlineSyncService.addPointageTerrainToQueue(pointage);
      
      this.offlineSyncService.syncPointagesTerrain(['AUTH_COLLABORATEUR']).subscribe({
        next: (response) => {
          console.log('Pointage terrain envoyé:', response.batchId);
          this.toastr.success('Pointage terrain en cours de traitement');
        }
      });
    } else {
      this.offlineSyncService.addPointageTerrainToQueue(pointage);
      this.toastr.warning('Pointage terrain enregistré hors ligne');
    }
  } catch (error) {
    this.toastr.error('Impossible d\'obtenir la position GPS');
  }
}

private getCurrentPosition(): Promise<GeolocationPosition> {
  return new Promise((resolve, reject) => {
    navigator.geolocation.getCurrentPosition(resolve, reject, {
      enableHighAccuracy: true,
      timeout: 5000
    });
  });
}
```

### Afficher les compteurs
```typescript
// Dans votre template
<div class="pending-indicator">
  <span>Standard: {{ (offlineSyncService.pendingCountStandard | async) || 0 }}</span>
  <span>Terrain: {{ (offlineSyncService.pendingCountTerrain | async) || 0 }}</span>
  <span>Total: {{ (offlineSyncService.pendingCountTotal | async) || 0 }}</span>
</div>
```

## 🔧 Endpoints API

### Standard
```http
POST /api/v1/offline-sync/pointages/sync
Authorization: Bearer {token}
Content-Type: application/json

{
  "pointages": [
    { "matricule": 12345, "heurePointage": "2024-12-13T08:30:00" }
  ]
}
```

### Terrain
```http
POST /api/v1/offline-sync/pointages/terrain/sync
Authorization: Bearer {token}
X-User-Privileges: AUTH_COLLABORATEUR
Content-Type: application/json

{
  "pointages": [
    { 
      "heurePointage": "2024-12-13T08:30:00",
      "latitude": 14.6937,
      "longitude": -17.4441
    }
  ]
}
```

### Statut
```http
GET /api/v1/offline-sync/status/{batchId}
```

## 🗄️ Base de Données

### Table sync_batches
```sql
CREATE TABLE sync_batches (
    id BIGSERIAL PRIMARY KEY,
    batch_id VARCHAR(36) UNIQUE NOT NULL,
    email VARCHAR(255) NOT NULL,
    type_pointage VARCHAR(20) NOT NULL, -- 'STANDARD' ou 'TERRAIN'
    total_pointages INTEGER NOT NULL,
    success_count INTEGER DEFAULT 0,
    failure_count INTEGER DEFAULT 0,
    status VARCHAR(20) NOT NULL,
    error_message TEXT,
    created_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP,
    results TEXT -- JSON
);

CREATE INDEX idx_batch_id ON sync_batches(batch_id);
CREATE INDEX idx_email_status ON sync_batches(email, status);
CREATE INDEX idx_created_at ON sync_batches(created_at);
```

## 📊 Monitoring

### RabbitMQ Management
```
http://localhost:15672
Username: guest
Password: guest

- Voir les queues en temps réel
- Monitorer le throughput
- Visualiser les messages en DLQ
```

### Actuator Endpoints
```
http://localhost:8082/actuator/health
http://localhost:8082/actuator/metrics/rabbitmq.connections
http://localhost:8082/actuator/prometheus
```

## 🧪 Tests

### Backend
```bash
mvn clean test
mvn verify
```

### Test manuel avec curl

#### Standard
```bash
curl -X POST http://localhost:8082/api/v1/offline-sync/pointages/sync \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "pointages": [{
      "matricule": 12345,
      "heurePointage": "2024-12-13T08:30:00"
    }]
  }'
```

#### Terrain
```bash
curl -X POST http://localhost:8082/api/v1/offline-sync/pointages/terrain/sync \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "X-User-Privileges: AUTH_COLLABORATEUR" \
  -d '{
    "pointages": [{
      "heurePointage": "2024-12-13T08:30:00",
      "latitude": 14.6937,
      "longitude": -17.4441
    }]
  }'
```
## 🚨 Troubleshooting

### RabbitMQ ne démarre pas
```bash
docker-compose restart rabbitmq
docker logs offline-sync-rabbitmq
```

### Messages bloqués dans la queue
- Vérifier les consumers dans RabbitMQ Management
- Vérifier les logs : `docker logs offline-sync-service`

### Erreur de validation
- Latitude/Longitude hors limites
- Pointage dans le futur
- Pointage trop ancien (> 30 jours)

## 📞 Support

- **GitHub Issues**: [https://github.com/lahadsamb28/offline-sync-service/issues](https://github.com/lahadsamb28/offline-sync-service/issues)
- **Email**: scheikhabdou98@outlook.com

---

Made by ALS DEVELOPER