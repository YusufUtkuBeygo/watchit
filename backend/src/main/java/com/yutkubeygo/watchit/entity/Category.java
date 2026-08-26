package com.yutkubeygo.watchit.entity;


import aQute.bnd.annotation.licenses.LGPL_2_1_only;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter


//Sonradan kategorilere özellik eklemek istersek açıklama vb. diye oluşuturuldu
@Entity
@Table(name="categories")
public class Category {

    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private Long id;

    @Column(unique=true,nullable=false)
    private String name;

    @OneToMany(mappedBy="category")
    private List<Video> videos;

    public Category() {}

    public Category(String name)
    {
        this.name=name;
    }
}
