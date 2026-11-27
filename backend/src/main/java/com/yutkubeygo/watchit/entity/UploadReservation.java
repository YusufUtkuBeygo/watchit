package com.yutkubeygo.watchit.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

//Yükleme defterinin bir satırı (V27): kim, ne zaman, kaç baytlık yükleme adresi aldı. Günlük kota buna bakar
@Getter
@NoArgsConstructor
@Entity
@Table(name = "upload_reservations")
public class UploadReservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long bytes;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public UploadReservation(Long userId, long bytes)
    {
        this.userId = userId;
        this.bytes = bytes;
    }
}
