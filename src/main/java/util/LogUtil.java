package util;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.metamodel.PropertyMetaModel;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.stream.Collectors.toList;

/**
 * @author Akasaka Isami
 * @description 日志语句相关的工具类
 * <a href="https://regexr-cn.com/">参考这个网址</>
 */
public class LogUtil {

    private static final String regexGLOBAL = ".*?(log|trace|(system\\.out)|(system\\.err)).*?(.*?)";

    private static final Set<String> level_prix = new HashSet<String>() {{
        add("log");
        add("logger");
        add("logging");
        add("getlogger");
        add("getlog");
    }};

    private static final Set<String> levels = new HashSet<String>() {{
        add("trace");
        add("debug");
        add("info");
        add("warn");
        add("error");
        add("fatal");
    }};

    public static boolean isLogStatement(String statement) {
        String myRegex = "(log|logger|logging|getlogger|getlog)\\.(trace|debug|info|warn|error|fatal)\\(.*?\\)";
        Pattern p = Pattern.compile(myRegex, Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(statement);
        return m.find();
    }

    // 获取输入日志语句的级别
    public static int getLogLevel(Expression expression) {
        String level = ((MethodCallExpr) expression).getName().toString();
        int result = -1;
        switch (level) {
            case "error":
                result = 4;
                break;
            case "warn":
                result = 3;
                break;
            case "info":
                result = 2;
                break;
            case "debug":
                result = 1;
                break;
            case "trace":
                result = 0;
                break;
        }

        return result;
    }


    public static String getLogMessageToken(Expression expression) {
        StringBuilder result = new StringBuilder();

        for (Node node : expression.getChildNodes()) {
            if (node instanceof StringLiteralExpr) {
                String msg = ((StringLiteralExpr) node).getValue();
                String tokens = DotPrintFilter.cutHump(msg);
                result.append(tokens).append(" ");

            }
        }

        if (result.length() != 0)
            result.deleteCharAt(result.length() - 1);

        return result.toString();
    }
}
