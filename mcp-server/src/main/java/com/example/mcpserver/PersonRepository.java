package com.example.mcpserver;

import org.springframework.data.repository.CrudRepository;
import java.util.List;


public interface PersonRepository extends CrudRepository<Person, Integer> {
    // Spring Data derives SQL from these method names; ExpenseTools chooses one from optional search inputs.
    List<Person> findByName(String name);
    List<Person> findByExpense(Double expense);
    List<Person> findByNameAndExpense(String name, Double expense);
}
