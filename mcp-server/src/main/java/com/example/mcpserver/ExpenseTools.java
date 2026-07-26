package com.example.mcpserver;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class ExpenseTools {
    private static final Logger logger = LoggerFactory.getLogger(ExpenseTools.class);
    private final PersonRepository personRepository;

    public ExpenseTools(PersonRepository personRepository) {
        this.personRepository = personRepository;
    }

    @Tool(name = "search_expense_by_name_or_amount",
            description = "Find all expenses by providing either the name, the expense amount, or both variables. Leave fields blank if unknown.")
    public String searchExpenses(
            @ToolParam(description = "Name of the expense to search for. Pass null if unknown.", required = false) String name,
            @ToolParam(description = "Expense amount to search for. Pass null if unknown.", required = false) Double expense
    ) {
        // The authenticated MCP request has reached application code; record its token subject for auditing.
        logToolUse("search_expense_by_name_or_amount");
        // 1. Edge case: LLM provided no search criteria
        if (name == null && expense == null) {
            return "Please provide at least one search parameter (name or expense amount).";
        }

        // 2. Query the repository dynamically based on non-null parameters
        List<Person> persons;
        if (name != null && expense != null) {
            persons = personRepository.findByNameAndExpense(name, expense);
        } else if (name != null) {
            persons = personRepository.findByName(name);
        } else {
            persons = personRepository.findByExpense(expense);
        }

        // 3. Format and return results
        if (persons.isEmpty()) {
            return "No expenses found matching your criteria.";
        }

        StringBuilder result = new StringBuilder();
        for (Person person : persons) {
            result.append(person.toString()).append("\n");
        }
        return result.toString();
    }

    @Tool(name = "show_all_records", description = "Show all records currently in the database")
    public String showAllRecords(){
        // MCP invokes this method after the model selects the show_all_records tool.
        logToolUse("show_all_records");
        Iterable<Person> persons = personRepository.findAll();
        StringBuilder result = new StringBuilder();
        for (Person person : persons) {
            result.append(person.toString()).append("\n");
        }
        return result.toString();

    }

    @Tool(name = "modify_record_by_id", description = "Modify an existing record in the table by Id")
    public void modifyRecord(@ToolParam(description = "Id of the record to modify") Integer id, @ToolParam(description = "New name of the expense") String name, @ToolParam(description = "New expense amount") Double expense){
        logToolUse("modify_record_by_id");
        // The repository translates this into a database lookup, then save persists the changed entity.
        Person person = personRepository.findById(id).orElse(null);
        if (person != null) {
            person.setName(name);
            person.setExpense(expense);
            personRepository.save(person);
        }
    }

    @Tool(name = "add_record", description = "Add a new record to the table")
    public String addRecord(@ToolParam(description = "name of the expense") String name, @ToolParam(description = "Expense amount") Double expense){
        logToolUse("add_record");
        // Map the model-supplied tool parameters into the JPA entity stored in PostgreSQL.
        Person person = new Person();
        person.setName(name);
        person.setExpense(expense);
        personRepository.save(person);
        return "Record added successfully: " + person.toString();
    }

    private void logToolUse(String toolName) {
        // Resource-server authentication exposes the OAuth access token as a JwtAuthenticationToken.
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String subject = authentication instanceof JwtAuthenticationToken jwtAuthentication
                ? jwtAuthentication.getToken().getSubject()
                : authentication.getName();
        logger.info("MCP tool '{}' invoked by subject='{}'", toolName, subject);
    }
}