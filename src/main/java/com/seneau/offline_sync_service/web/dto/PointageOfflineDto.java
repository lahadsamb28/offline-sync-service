package com.seneau.offline_sync_service.web.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PointageOfflineDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull(message = "Le matricule est obligatoire")
    @Positive(message = "Le matricule doit être positif")
    private Long matricule;

    @NotNull(message = "L'heure de pointage est obligatoire")
    @PastOrPresent(message = "L'heure de pointage ne peut pas être dans le futur")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime heurePointage;

}
