package com.campusconnect.profile.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "skills")
public class Skill {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String name;

    protected Skill() {}

    public Long getId() { return id; }
    public String getName() { return name; }
}