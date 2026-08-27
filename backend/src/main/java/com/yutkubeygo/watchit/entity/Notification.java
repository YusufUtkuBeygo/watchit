package com.yutkubeygo.watchit.entity;


import com.yutkubeygo.watchit.enums.NotificationType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter

@Entity
@Table(name="notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name="receiver_id")
    private User receiver;

    @ManyToOne
    @JoinColumn(name="sender_id")
    private User sender;

    private String title;

    private String message;

    @Enumerated(EnumType.STRING)//Enumların tipinin değiştirilmesini engellemek için kullanılır
    private NotificationType type;

    private Boolean isRead = false;

    @CreationTimestamp
    private LocalDateTime createdAt;

    public Notification() {}

    public Notification(String title,User sender,User receiver, String message, NotificationType type)
    {
        this.title = title;
        this.sender = sender;
        this.receiver = receiver;
        this.message = message;
        this.type = type;
    }
}
