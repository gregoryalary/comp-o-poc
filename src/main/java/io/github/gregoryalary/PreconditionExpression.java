package io.github.gregoryalary;

import com.google.common.collect.Range;
import org.apache.jena.query.*;

import java.util.HashMap;

public class PreconditionExpression {

    private static String FIND_ALL_VARIABLE_BINDINGS_QUERY =
            "PREFIX profile:          <http://www.daml.org/services/owl-s/1.2/Profile.owl#>\n" +
                    "PREFIX service:          <http://www.daml.org/services/owl-s/1.2/Service.owl#>\n" +
                    "PREFIX expr: <http://www.daml.org/services/owl-s/1.2/generic/Expression.owl#>\n" +
                    "\n" +
                    "SELECT ?variable ?object\n" +
                    "WHERE {\n" +
                    "   <%s> expr:variableBinding/expr:theObject ?object .\n" +
                    "   <%s> expr:variableBinding/expr:theVariable ?variable .\n" +
                    "}";

    private static String FIND_EXPRESSION_DATA =
            "PREFIX profile:          <http://www.daml.org/services/owl-s/1.2/Profile.owl#>\n" +
                    "PREFIX service:          <http://www.daml.org/services/owl-s/1.2/Service.owl#>\n" +
                    "PREFIX expr: <http://www.daml.org/services/owl-s/1.2/generic/Expression.owl#>\n" +
                    "\n" +
                    "SELECT ?data\n" +
                    "WHERE {\n" +
                    "   <%s> expr:expressionData ?data .\n" +
                    "}";

    private final ServicePrecondition ressource;

    private HashMap<String, String> variableBinding;

    private String operator;

    private String firstOperand;

    private int secondOperand;

    public PreconditionExpression(ServicePrecondition ressource) {
        this.ressource = ressource;
        this.initVariableBindings();
        this.parseExpression();
    }

    private void parseExpression() {
        String expression = "";
        Query query = QueryFactory.create(String.format(FIND_EXPRESSION_DATA, ressource.getURI()));
        for (ResultSet results = QueryExecutionFactory.create(query, ServiceEnvironment.getModel()).execSelect(); results.hasNext(); ) {
            QuerySolution solution = results.nextSolution();
            expression = solution.getLiteral("data").getString();
        }
        String[] tokens = expression.split(" ");
        if (tokens.length != 3)
            throw new RuntimeException(String.format("Malformed precondition data for %s : %s", ressource.getURI(), expression));
        operator = tokens[0];
        if (variableBinding.containsKey(tokens[1].replace("?", ""))) {
            firstOperand = variableBinding.get(tokens[1].replace("?", ""));
        } else {
            System.out.println(variableBinding);
            throw new RuntimeException(String.format("No binding in %s for the variable : %s", ressource, tokens[1]));
        }
        if (tokens[2].matches("^\\d+$")) {
            secondOperand = Integer.parseInt(tokens[2]);
        } else {
            throw new RuntimeException("Invalid value operand in [" + ressource.getURI() + "] : " + tokens[2]);
        }
    }

    private void initVariableBindings() {
        variableBinding = new HashMap();
        Query query = QueryFactory.create(String.format(FIND_ALL_VARIABLE_BINDINGS_QUERY, ressource.getURI(), ressource.getURI()));
        for (ResultSet results = QueryExecutionFactory.create(query, ServiceEnvironment.getModel()).execSelect(); results.hasNext(); ) {
            QuerySolution solution = results.nextSolution();
            if (solution.contains("?object") && solution.contains("?variable")) {
                variableBinding.put(
                        solution.getLiteral("?variable").getString(),
                        solution.getResource("?object").getURI()
                );
            }
        }
    }

    @Override
    public String toString() {
        return String.format("%s %s %s", firstOperand, operator, secondOperand);
    }

    public String getOperator() {
        return operator;
    }

    public String getFirstOperand() {
        return firstOperand;
    }

    public int getSecondOperand() {
        return secondOperand;
    }

    public Range getAcceptedValues() {
        switch (getOperator()) {
            case "=":  return Range.singleton(getSecondOperand());
            case ">=": return Range.atLeast(getSecondOperand());
            case "<=": return Range.atMost(getSecondOperand());
            case ">":  return Range.greaterThan(getSecondOperand());
            case "<":  return Range.lessThan(getSecondOperand());
            default:   throw new RuntimeException(String.format("Unknow operator in [%s] : %s", ressource.getURI(), getOperator()));
        }
    }

    public boolean isCompatibleWith(PreconditionExpression expression, ComposablePerformBindings binding) {
        Parameter thisExpressionParameter = new Input(ServiceEnvironment.getModel().getResource(getFirstOperand()));
        Parameter otherExpressionParameter = new Input(ServiceEnvironment.getModel().getResource(expression.getFirstOperand()));
        boolean sameRessource = thisExpressionParameter.getURI().equals(otherExpressionParameter.getURI());
        for (Parameter interfaceInput : binding.getInputBinding().keySet()) {
            if (interfaceInput.getURI().equals(otherExpressionParameter.getURI())) sameRessource = true;
        }
        for (Parameter serviceInput : binding.getInputBinding().keySet()) {
            if (serviceInput.getURI().equals(thisExpressionParameter.getURI())) sameRessource = true;
        }
        if (sameRessource) {
            return !getAcceptedValues().intersection(expression.getAcceptedValues()).isEmpty();
        } else {
            return true;
        }
    }

}
