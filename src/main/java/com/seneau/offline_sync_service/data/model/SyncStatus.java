package com.seneau.offline_sync_service.data.model;

public enum SyncStatus {
    PENDING,          // En attente dans la queue
    IN_PROGRESS,      // En cours de traitement
    COMPLETED,        // Terminé avec succès
    PARTIAL_FAILURE,  // Partiellement réussi
    FAILED,           // Échec total
    RETRY             // En cours de réessai
}