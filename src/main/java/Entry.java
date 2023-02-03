import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseProblemException;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.visitor.VoidVisitor;
import config.Config;
import visitor.MethodVisitor;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;

import static config.Config.targetDir;

/**
 * <p>You can check the problem detail on <a href="">Leetcode</a>.</p>
 *
 * @author Akasaka Isami
 * @since 2023/1/17 9:05 PM
 */
public class Entry {

    public static void main(String[] args) throws IOException {

        String dataSrc = Config.rootDir;
        File srcDir = new File(dataSrc);
        if (!srcDir.isDirectory() || !srcDir.exists()) {
            System.out.println("找不到源码！检查" + Config.rootDir + "是不是有源码？");
            return;
        }

        File tar1 = new File(targetDir);

        if (!new File(targetDir).exists())
            tar1.mkdirs();

        FileWriter fw = new FileWriter(targetDir + "data.txt");

        // 如果有 遍历所有java文件
        for (File file : Objects.requireNonNull(srcDir.listFiles())) {
            String fileName = file.getName();


            try {
                CompilationUnit cu = JavaParser.parse(file);

                VoidVisitor<String> methodVisitor = new MethodVisitor();
                methodVisitor.visit(cu, fileName);

            } catch (ParseProblemException e1) {
                System.out.println(fileName + "解析出错，直接跳过");
            } catch (FileNotFoundException e2) {
                System.out.println(fileName + "文件没找到，直接跳过");
            }
        }

        save_data(MethodVisitor.seqs, fw);

        fw.close();
    }

    private static void save_data(Map<String, String> seqs, FileWriter fw) throws IOException {
        for (Map.Entry<String, String> entry : seqs.entrySet()) {
            String key = entry.getKey();
            String seq = entry.getValue();
            fw.write(key + ' ' + seq + "\n");
        }
    }


}
