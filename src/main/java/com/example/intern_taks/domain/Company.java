package com.example.intern_taks.domain;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class Company {

    private String companyNumber;
    private String name;
    private String status;
    private String companyType;
    private LocalDate incorporationDate;
    private LocalDate dissolutionDate;
    private String registeredOfficeAddress;

    private List<Officer> officers = new ArrayList<>();
    private List<PersonWithSignificantControl> personsWithSignificantControl = new ArrayList<>();

    public Company() {
    }

    public Company(
            String companyNumber,
            String name,
            String status,
            String companyType,
            LocalDate incorporationDate,
            LocalDate dissolutionDate,
            String registeredOfficeAddress
    ) {
        this.companyNumber = companyNumber;
        this.name = name;
        this.status = status;
        this.companyType = companyType;
        this.incorporationDate = incorporationDate;
        this.dissolutionDate = dissolutionDate;
        this.registeredOfficeAddress = registeredOfficeAddress;
    }

    public String getCompanyNumber() {
        return companyNumber;
    }

    public void setCompanyNumber(String companyNumber) {
        this.companyNumber = companyNumber;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCompanyType() {
        return companyType;
    }

    public void setCompanyType(String companyType) {
        this.companyType = companyType;
    }

    public LocalDate getIncorporationDate() {
        return incorporationDate;
    }

    public void setIncorporationDate(LocalDate incorporationDate) {
        this.incorporationDate = incorporationDate;
    }

    public LocalDate getDissolutionDate() {
        return dissolutionDate;
    }

    public void setDissolutionDate(LocalDate dissolutionDate) {
        this.dissolutionDate = dissolutionDate;
    }

    public String getRegisteredOfficeAddress() {
        return registeredOfficeAddress;
    }

    public void setRegisteredOfficeAddress(String registeredOfficeAddress) {
        this.registeredOfficeAddress = registeredOfficeAddress;
    }

    public List<Officer> getOfficers() {
        return officers;
    }

    public void setOfficers(List<Officer> officers) {
        this.officers = officers;
    }

    public List<PersonWithSignificantControl> getPersonsWithSignificantControl() {
        return personsWithSignificantControl;
    }

    public void setPersonsWithSignificantControl(
            List<PersonWithSignificantControl> personsWithSignificantControl
    ) {
        this.personsWithSignificantControl = personsWithSignificantControl;
    }
}