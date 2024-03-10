package com.example.springbootlab1.repository;

import jakarta.persistence.*;

import java.util.Objects;

@Entity
public class Coordinates {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String lat;
    private String lng;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getLat() {
        return lat;
    }

    public void setLat(String lat) {
        this.lat = lat;
    }

    public String getLng() {
        return lng;
    }

    public void setLng(String lng) {
        this.lng = lng;
    }

    @ManyToOne
    @JoinColumn(name = "country_id")
    private Country country;

    public void setCountry(Country country){
        this.country = country;
    }

    public Country getCountry(Country country){
        return country;
    }

    public boolean checkId(Long id){
        return Objects.equals(this.id, id);
    }
}