package scanner.impl;

import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

public class ComplexityAnalyzer extends VoidVisitorAdapter<Void> {
    int branches = 0;
    int loops = 0;
    int maxDepth = 0;
    int currentDepth = 0;

    private void goDeeper() {
        currentDepth++;
        if (currentDepth > maxDepth) {
            maxDepth = currentDepth;
        }
    }

    private void goBack() {
        currentDepth--;
    }

    @Override
    public void visit(IfStmt n, Void arg) {
        branches++;
        goDeeper();
        super.visit(n, arg);
        goBack();
    }

    @Override
    public void visit(ForStmt n, Void arg) {
        branches++;
        loops++;
        goDeeper();
        super.visit(n, arg);
        goBack();
    }

    @Override
    public void visit(ForEachStmt n, Void arg) {
        branches++;
        loops++;
        goDeeper();
        super.visit(n, arg);
        goBack();
    }

    @Override
    public void visit(WhileStmt n, Void arg) {
        branches++;
        loops++;
        goDeeper();
        super.visit(n, arg);
        goBack();
    }

    @Override
    public void visit(DoStmt n, Void arg) {
        branches++;
        loops++;
        goDeeper();
        super.visit(n, arg);
        goBack();
    }

    @Override
    public void visit(SwitchStmt n, Void arg) {
        branches += n.getEntries().size();
        super.visit(n, arg);
    }

    @Override
    public void visit(TryStmt n, Void arg) {
        branches += n.getCatchClauses().size();
        super.visit(n, arg);
    }
}