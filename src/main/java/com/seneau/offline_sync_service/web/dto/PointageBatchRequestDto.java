package com.seneau.offline_sync_service.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PointageBatchRequestDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotEmpty(message = "La liste des pointages ne peut pas être vide")
    @Valid
    private List<PointageOfflineDto> pointages;
}
