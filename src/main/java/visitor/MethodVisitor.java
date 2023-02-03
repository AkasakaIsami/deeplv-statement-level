package visitor;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import util.LogUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * <p>You can check the problem detail on <a href="">Leetcode</a>.</p>
 *
 * @author Akasaka Isami
 * @since 2023/1/17 10:40 PM
 */
public class MethodVisitor extends VoidVisitorAdapter<String> {
    // 级别@类名@函数名@行号：该语句的seq
    public static Map<String, String> seqs = new HashMap<>();

    String key;
    // 对于每一个函数 都要维护一个记录遍历到当前哪个节点的sb
    StringBuilder trace = new StringBuilder();


    @Override
    public void visit(MethodDeclaration node, String fileName) {
        trace = new StringBuilder();
        if (node != null && node.isMethodDeclaration()) {
            System.out.println("遍历函数" + node.getNameAsString() + "中");
            MethodDeclaration methodDeclaration = node.asMethodDeclaration();
            this.key = fileName + "@" + methodDeclaration.getNameAsString();

            int line = methodDeclaration.getBegin().isPresent() ? methodDeclaration.getBegin().get().line : -1;
            String tempKey = this.key + "@" + line;
            String type = methodDeclaration.getMetaModel().getTypeName();
            trace.append(type).append(' ');

            Optional<BlockStmt> body = methodDeclaration.getBody();
            if (body.isPresent()) {
                NodeList<Statement> statements = body.get().getStatements();
                for (Statement statement : statements) {
                    dfs(statement);
                }

            }
        }

    }

    /**
     * 传入当前节点和父亲节点的key
     *
     * @param node 当前节点
     */
    private void dfs(Statement node) {
        if (node.isExpressionStmt()) {
            // TODO: 对于Expression的不同类型，也要进行考虑
            // 比如int a = getA() 这需要构建两个节点
            ExpressionStmt exStmt = ((ExpressionStmt) node).asExpressionStmt();
            Expression expression = exStmt.getExpression();
            String code = expression.toString();

            String type = expression.getMetaModel().getTypeName();
            trace.append(type).append(' ');

            if (LogUtil.isLogStatement(code)) {
                /** 如果是日志语句 那就
                 *  1。 获取自己的日志级别
                 *  2。 获取自己的messgae信息
                 *  3。 把自己加入map
                 *  4。 继续遍历
                 */
                int level = LogUtil.getLogLevel(expression);
                String msgTokens = LogUtil.getLogMessageToken(expression);

                int line = expression.getBegin().isPresent() ? expression.getBegin().get().line : -1;
                String tempKey = level + "@" + this.key + "@" + line;
                seqs.put(tempKey, trace.toString() + msgTokens);
            }

            // 接下来我们处理可能带block的节点
            // 首先我们处理循环语句 因为循环比较简单
            // ————————————————————————————————————————————————————————————————————————————————————————————
        } else if (node.isWhileStmt()) {
            WhileStmt whileStmt = ((WhileStmt) node).asWhileStmt();
            int line = whileStmt.getBegin().isPresent() ? whileStmt.getBegin().get().line : -1;
            String tempKey = this.key + "@" + line;
            String type = whileStmt.getMetaModel().getTypeName();
            trace.append(type).append(' ');

            // 这里需要考虑 因为有body了 所以要对body进行dfs
            if (!whileStmt.getBody().isBlockStmt()) {
                dfs(whileStmt.getBody());
            } else {
                NodeList<Statement> statements = whileStmt.getBody().asBlockStmt().getStatements();
                for (Statement statement : statements) {
                    dfs(statement);
                }
            }

            type = "endWhile";
            trace.append(type).append(' ');

        } else if (node.isForStmt()) {
            ForStmt forStmt = ((ForStmt) node).asForStmt();
            int line = forStmt.getBegin().isPresent() ? forStmt.getBegin().get().line : -1;
            String tempKey = this.key + "@" + line;
            String type = forStmt.getMetaModel().getTypeName();
            trace.append(type).append(' ');

            // 这里需要考虑 因为有body了 所以要对body进行dfs
            if (!forStmt.getBody().isBlockStmt()) {
                dfs(forStmt.getBody());
            } else {
                NodeList<Statement> statements = forStmt.getBody().asBlockStmt().getStatements();
                for (Statement statement : statements) {
                    dfs(statement);
                }
            }
            // 还没完！因为每个block后都需要加一个标注块结束的控制节点！

            line = forStmt.getEnd().isPresent() ? forStmt.getEnd().get().line : line;
            tempKey = this.key + "@" + line + "@endFor";
            type = "endFor";
            trace.append(type).append(' ');

        } else if (node.isForeachStmt()) {
            ForeachStmt foreachStmt = ((ForeachStmt) node).asForeachStmt();
            int line = foreachStmt.getBegin().isPresent() ? foreachStmt.getBegin().get().line : -1;
            String tempKey = this.key + "@" + line;
            String type = foreachStmt.getMetaModel().getTypeName();
            trace.append(type).append(' ');

            // 这里需要考虑 因为有body了 所以要对body进行dfs
            if (!foreachStmt.getBody().isBlockStmt()) {
                dfs(foreachStmt.getBody());
            } else {
                NodeList<Statement> statements = foreachStmt.getBody().asBlockStmt().getStatements();
                for (Statement statement : statements) {
                    dfs(statement);
                }
            }
            // 还没完！因为每个block后都需要加一个标注块结束的控制节点！
            line = foreachStmt.getEnd().isPresent() ? foreachStmt.getEnd().get().line : line;
            tempKey = this.key + "@" + line + "@endForeach";
            type = "endForeach";
            trace.append(type).append(' ');

        } else if (node.isDoStmt()) {
            DoStmt doStmt = ((DoStmt) node).asDoStmt();
            int doLine = doStmt.getBegin().isPresent() ? doStmt.getBegin().get().line : -1;
            int whileLine = doStmt.getCondition().getBegin().isPresent() ? doStmt.getCondition().getBegin().get().line : -1;

            String tempKey = this.key + "@" + doLine;
            String type = "do";
            trace.append(type).append(' ');

            // 这里需要考虑 因为有body了 所以要对body进行dfs
            if (!doStmt.getBody().isBlockStmt()) {
                dfs(doStmt.getBody());
            } else {
                NodeList<Statement> statements = doStmt.getBody().asBlockStmt().getStatements();
                for (Statement statement : statements) {
                    dfs(statement);
                }
            }
            // 还没完！do最后要wile语句
            tempKey = this.key + "@" + whileLine;
            // TODO: 这里需要看一下是不是DoStmt
            type = doStmt.getMetaModel().getTypeName();
            trace.append(type).append(' ');

            // 接下来依旧是可能带block的节点
            // 不过这次是条件分支 比较难写
            // ————————————————————————————————————————————————————————————————————————————————————————————
        } else if (node.isIfStmt()) {
            IfStmt ifStmt = ((IfStmt) node).asIfStmt();
            // 对于if语句 我们只需要顺序遍历then块和else块就好
            int line = ifStmt.getBegin().isPresent() ? ifStmt.getBegin().get().line : -1;
            String tempKey = this.key + "@" + line;
            String type = ifStmt.getMetaModel().getTypeName();
            trace.append(type).append(' ');

            if (!ifStmt.getThenStmt().isBlockStmt()) {
                dfs(ifStmt.getThenStmt());
            } else {
                BlockStmt thenBlockStmt = ifStmt.getThenStmt().asBlockStmt();
                NodeList<Statement> statements = thenBlockStmt.getStatements();
                for (Statement statement : statements)
                    dfs(statement);
            }

            if (ifStmt.getElseStmt().isPresent()) {
                // 如果存在else块 要先判断是elif 还是else
                if (ifStmt.getElseStmt().get().isIfStmt()) {
                    dfs(ifStmt.getElseStmt().get().asIfStmt());
                } else {
                    // 不然就是else
                    // else节点好像没有 得自己建一个
                    line = ifStmt.getElseStmt().get().getBegin().isPresent() ? ifStmt.getElseStmt().get().getBegin().get().line : -1;
                    tempKey = this.key + "@" + line;
                    String elseType = "else";
                    trace.append(elseType).append(' ');

                    if (!ifStmt.getElseStmt().get().isBlockStmt()) {
                        dfs(ifStmt.getElseStmt().get());
                    } else {
                        BlockStmt elseBlockStmt = ifStmt.getElseStmt().get().asBlockStmt();
                        NodeList<Statement> statements = elseBlockStmt.getStatements();
                        for (Statement statement : statements) {
                            dfs(statement);
                        }
                    }
                    // 还没完 对于ifStmt 最后要加一个endif块
                    line = ifStmt.getElseStmt().get().getEnd().isPresent() ? ifStmt.getElseStmt().get().getEnd().get().line : line;
                    tempKey = this.key + "@" + line + "@endIf";
                    type = "endIf";
                    trace.append(type).append(' ');

                }
            } else {
                // 当前节点没有else了 我们再考虑endif
                // 有else就接着遍历else 没else我们就用endif封上
                // else结束也要封
                // 还没完 对于ifStmt 最后要加一个endif块
                line = ifStmt.getEnd().isPresent() ? ifStmt.getEnd().get().line : line;
                tempKey = this.key + "@" + line + "@endIf";
                type = "endIf";
                trace.append(type).append(' ');
            }


        } else if (node.isSwitchStmt()) {
            SwitchStmt switchStmt = ((SwitchStmt) node).asSwitchStmt();
            int line = switchStmt.getBegin().isPresent() ? switchStmt.getBegin().get().line : -1;
            String tempKey = this.key + "@" + line;
            String type = switchStmt.getMetaModel().getTypeName();
            trace.append(type).append(' ');

            NodeList<SwitchEntryStmt> caseEntries = switchStmt.getEntries();
            if (!caseEntries.isEmpty()) {
                for (SwitchEntryStmt caseEntry : caseEntries) {
                    dfs(caseEntry);
                    NodeList<Statement> statements = caseEntry.getStatements();
                    for (Statement statement : statements)
                        dfs(statement);
                }
            }

            // 没完！ 要接一个endSwtich
            line = switchStmt.getEnd().isPresent() ? switchStmt.getEnd().get().line : line;
            tempKey = this.key + "@" + line;
            type = "endSwtich";
            trace.append(type).append(' ');


            // 接下来处理一些块结构
            // ————————————————————————————————————————————————————————————————————————————————————————————
        } else if (node.isSynchronizedStmt()) {
            SynchronizedStmt synchronizedStmt = ((SynchronizedStmt) node).asSynchronizedStmt();
            int line = synchronizedStmt.getBegin().isPresent() ? synchronizedStmt.getBegin().get().line : -1;
            String tempKey = this.key + "@" + line;
            String type = synchronizedStmt.getMetaModel().getTypeName();
            trace.append(type).append(' ');

            // 这里需要考虑 因为有body了 所以要对body进行dfs
            if (!synchronizedStmt.getBody().isBlockStmt()) {
                dfs(synchronizedStmt.getBody());
            } else {
                NodeList<Statement> statements = synchronizedStmt.getBody().asBlockStmt().getStatements();
                for (Statement statement : statements) {
                    dfs(statement);
                }
            }
            // 还没完！因为每个block后都需要加一个标注块结束的控制节点！
            line = synchronizedStmt.getEnd().isPresent() ? synchronizedStmt.getEnd().get().line + 1 : line;
            tempKey = this.key + "@" + line + "@endSynchronized";
            type = "endSynchronized";
            trace.append(type).append(' ');

        } else if (node.isBlockStmt()) {
            BlockStmt blockStmt = ((BlockStmt) node).asBlockStmt();
            NodeList<Statement> statements = blockStmt.getStatements();
            for (Statement statement : statements) {
                dfs(statement);
            }
            // 接下来处理一些简单语句
            // ————————————————————————————————————————————————————————————————————————————————————————————
        } else if (node.isSwitchEntryStmt()) {
            SwitchEntryStmt switchEntryStmt = ((SwitchEntryStmt) node).asSwitchEntryStmt();
            int line = switchEntryStmt.getBegin().isPresent() ? switchEntryStmt.getBegin().get().line : -1;
            String tempKey = this.key + "@" + line;
            String type = switchEntryStmt.getMetaModel().getTypeName();
            trace.append(type).append(' ');

        } else if (node.isBreakStmt()) {
            BreakStmt breakStmt = ((BreakStmt) node).asBreakStmt();
            int line = breakStmt.getBegin().isPresent() ? breakStmt.getBegin().get().line : -1;
            String tempKey = this.key + "@" + line;
            String type = breakStmt.getMetaModel().getTypeName();
            trace.append(type).append(' ');

        } else if (node.isContinueStmt()) {
            ContinueStmt continueStmt = ((ContinueStmt) node).asContinueStmt();
            int line = continueStmt.getBegin().isPresent() ? continueStmt.getBegin().get().line : -1;
            String tempKey = this.key + "@" + line;
            String type = continueStmt.getMetaModel().getTypeName();
            trace.append(type).append(' ');

        } else if (node.isLabeledStmt()) {
            LabeledStmt labeledStmt = ((LabeledStmt) node).asLabeledStmt();
            int line = labeledStmt.getBegin().isPresent() ? labeledStmt.getBegin().get().line : -1;
            String tempKey = this.key + "@" + line;
            String type = labeledStmt.getMetaModel().getTypeName();
            trace.append(type).append(' ');

        } else if (node.isReturnStmt()) {
            ReturnStmt returnStmt = ((ReturnStmt) node).asReturnStmt();
            int line = returnStmt.getBegin().isPresent() ? returnStmt.getBegin().get().line : -1;
            String tempKey = this.key + "@" + line;
            String type = returnStmt.getMetaModel().getTypeName();
            trace.append(type).append(' ');

        } else if (node.isEmptyStmt()) {
            EmptyStmt emptyStmt = ((EmptyStmt) node).asEmptyStmt();
            int line = emptyStmt.getBegin().isPresent() ? emptyStmt.getBegin().get().line : -1;
            String tempKey = this.key + "@" + line;
            String type = emptyStmt.getMetaModel().getTypeName();
            trace.append(type).append(' ');

        } else if (node.isAssertStmt()) {
            AssertStmt assertStmt = ((AssertStmt) node).asAssertStmt();
            int line = assertStmt.getBegin().isPresent() ? assertStmt.getBegin().get().line : -1;
            String tempKey = this.key + "@" + line;
            String type = assertStmt.getMetaModel().getTypeName();
            trace.append(type).append(' ');

        } else if (node.isExplicitConstructorInvocationStmt()) {
            ExplicitConstructorInvocationStmt explicitConstructorInvocationStmt = ((ExplicitConstructorInvocationStmt) node).asExplicitConstructorInvocationStmt();
            int line = explicitConstructorInvocationStmt.getBegin().isPresent() ? explicitConstructorInvocationStmt.getBegin().get().line : -1;
            String tempKey = this.key + "@" + line;
            String type = explicitConstructorInvocationStmt.getMetaModel().getTypeName();
            trace.append(type).append(' ');

        } else if (node.isLocalClassDeclarationStmt()) {
            LocalClassDeclarationStmt localClassDeclarationStmt = ((LocalClassDeclarationStmt) node).asLocalClassDeclarationStmt();
            int line = localClassDeclarationStmt.getBegin().isPresent() ? localClassDeclarationStmt.getBegin().get().line : -1;
            String tempKey = this.key + "@" + line;
            String type = localClassDeclarationStmt.getMetaModel().getTypeName();
            trace.append(type).append(' ');


            // 最后我们来处理trycatch
        } else if (node.isThrowStmt()) {
            ThrowStmt throwStmtStmt = ((ThrowStmt) node).asThrowStmt();
            int line = throwStmtStmt.getBegin().isPresent() ? throwStmtStmt.getBegin().get().line : -1;
            String tempKey = this.key + "@" + line;
            String type = throwStmtStmt.getMetaModel().getTypeName();
            trace.append(type).append(' ');


        } else if (node.isTryStmt()) {
            TryStmt tryStmt = ((TryStmt) node).asTryStmt();
            int line = tryStmt.getBegin().isPresent() ? tryStmt.getBegin().get().line : -1;
            String tempKey = this.key + "@" + line;
            String type = tryStmt.getMetaModel().getTypeName();
            trace.append(type).append(' ');

            BlockStmt tryBlock = tryStmt.getTryBlock();
            if (!tryBlock.getStatements().isEmpty())
                dfs(tryBlock);

            int finalLine = line;
            NodeList<CatchClause> catchClauses = tryStmt.getCatchClauses();
            if (!catchClauses.isEmpty()) {
                for (CatchClause catchClause : catchClauses) {
                    line = catchClause.getBegin().isPresent() ? catchClause.getBegin().get().line : -1;
                    tempKey = this.key + "@" + line;
                    type = catchClause.getMetaModel().getTypeName();
                    trace.append(type).append(' ');


                    BlockStmt catchBody = catchClause.getBody();
                    if (!catchBody.getStatements().isEmpty())
                        dfs(catchBody);
                    finalLine = catchClause.getEnd().get().line;

                }
            }

            Optional<BlockStmt> finallyBlock = tryStmt.getFinallyBlock();
            if (finallyBlock.isPresent()) {
                line = tryStmt.getFinallyBlock().get().getBegin().isPresent() ? tryStmt.getFinallyBlock().get().getBegin().get().line : -1;
                tempKey = this.key + "@" + line;
                type = "finally";
                trace.append(type).append(' ');

                if (!finallyBlock.get().getStatements().isEmpty())
                    dfs(finallyBlock.get());


                finalLine = finallyBlock.get().getEnd().get().line;
            }

            // 最后接一个endtry就好
            tempKey = this.key + "@" + finalLine + "@endTry";
            type = "endTry";
            trace.append(type).append(' ');


        }

    }


}
