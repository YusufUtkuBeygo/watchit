package com.yutkubeygo.watchit.entity;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter

@Entity
@Table(
        name = "subscriptions",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"subscriber_id", "channel_id"})
        }
)
public class Subscription {

    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private Long id;

    //Many subscription rows can point same user
    @ManyToOne
    @JoinColumn(name="subscriber_id")
    private User subscriber;

    //Many subscription rows can point same channel
    @ManyToOne
    @JoinColumn(name="channel_id")
    private Channel channel;

    //Parameter and without parameter constructors
    public Subscription(){}

    public Subscription(User user, Channel channel)
    {
        this.subscriber=user;
        this.channel=channel;
    }
    
}
