package com.example.mcpserver;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
@Entity
@Table(name = "persons")
public class Person {

    // PostgreSQL assigns the primary key when the expense record is inserted.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // These fields form the data returned by MCP expense tools and persisted through JPA.
    @Column(name = "name")
    private String name;

    @Column(name = "expense")
    private Double expense;

    // JPA and the tool layer use these accessors to hydrate and update entity state.
    public Integer getId() {
        return this.id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Double getExpense() {
        return this.expense;
    }

    public void setExpense(Double expense) {
        this.expense = expense;
    }

    @Override
    public String toString() {
        return "Person{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", expense=" + expense +
                '}';
    }
}
