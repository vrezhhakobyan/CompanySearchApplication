package com.example.intern_taks.domain;

import java.time.LocalDate;

public class Officer {

    private String name;
    private String role;
    private LocalDate appointmentDate;

    public Officer() {
    }

    public Officer(String name, String role, LocalDate appointmentDate) {
        this.name = name;
        this.role = role;
        this.appointmentDate = appointmentDate;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public LocalDate getAppointmentDate() {
        return appointmentDate;
    }

    public void setAppointmentDate(LocalDate appointmentDate) {
        this.appointmentDate = appointmentDate;
    }
}