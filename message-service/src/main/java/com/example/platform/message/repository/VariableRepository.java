package com.example.platform.message.repository;

import com.example.platform.message.domain.MessageVariable;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class VariableRepository {

    private final Map<String, MessageVariable> variables = new LinkedHashMap<>();

    public VariableRepository() {
        save(new MessageVariable("userName", "User Name", "Display name of the receiver.", "TEXT", "Platform User", true, true, false));
        save(new MessageVariable("alertCode", "Alert Code", "Alert identifier.", "TEXT", null, true, true, false));
        save(new MessageVariable("currentTime", "Current Time", "Auto filled current time.", "DATETIME", null, true, true, true));
        save(new MessageVariable("invoiceNo", "Invoice Number", "Invoice identifier.", "TEXT", null, true, true, false));
        save(new MessageVariable("dueDate", "Due Date", "Invoice due date.", "DATE", null, true, true, false));
        save(new MessageVariable("serviceName", "Service Name", "Service identifier.", "TEXT", null, true, true, false));
        save(new MessageVariable("errorCount", "Error Count", "Number of errors in the window.", "NUMBER", "0", true, true, false));
        save(new MessageVariable("simulateFailure", "Simulate Failure", "Forces a transient delivery failure on first attempt.", "BOOLEAN", "false", false, true, false));
    }

    public void save(MessageVariable variable) {
        variables.put(variable.variableCode(), variable);
    }

    public Optional<MessageVariable> findByCode(String variableCode) {
        return Optional.ofNullable(variables.get(variableCode));
    }

    public List<MessageVariable> findAllByCodes(List<String> variableCodes) {
        return variableCodes.stream()
                .map(variables::get)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());
    }

    public List<MessageVariable> findAll() {
        return List.copyOf(variables.values());
    }
}
