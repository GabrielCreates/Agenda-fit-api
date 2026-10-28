
package br.com.fiap.agendafit.model;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GymClass {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Enumerated(EnumType.STRING)
    private Modality modality;

    @Enumerated(EnumType.STRING)
    @Column(name = "class_level")
    private Level level;

    private String instructor;
    private LocalDateTime startsAt;
    private Integer durationMinutes;
    private Integer capacity;
}
