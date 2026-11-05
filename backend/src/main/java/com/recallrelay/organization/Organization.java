package com.recallrelay.organization;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "organizations")
public class Organization {

    @Id
    private Long id;

    @Column(name = "organization_code", nullable = false, unique = true)
    private String organizationCode;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "email_domain")
    private String emailDomain;

    protected Organization() {
    }

    public Long getId() { return id; }
    public String getOrganizationCode() { return organizationCode; }
    public String getName() { return name; }
    public String getEmailDomain() { return emailDomain; }
}
