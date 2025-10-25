package com.sun.apollo.model;

import com.sun.base.model.BaseEntity;
import jakarta.persistence.*;
import java.util.List;

@Entity
@Table(name = "songs")
public class SongEntity extends BaseEntity {

    @Column(name = "name")
    private String name;

    @OneToMany(mappedBy = "song", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<StemEntity> stems;

    // Getters and setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<StemEntity> getStems() {
        return stems;
    }

    public void setStems(List<StemEntity> stems) {
        this.stems = stems;
    }
}