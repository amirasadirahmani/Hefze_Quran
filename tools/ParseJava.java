import javax.tools.*;
import com.sun.source.util.JavacTask;
import java.nio.file.*;
import java.util.*;
public class ParseJava {
    public static void main(String[] args) throws Exception {
        JavaCompiler compiler=ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> diagnostics=new DiagnosticCollector<>();
        try(StandardJavaFileManager files=compiler.getStandardFileManager(diagnostics,null,java.nio.charset.StandardCharsets.UTF_8)) {
            List<java.io.File> sources=new ArrayList<>();
            try(var paths=Files.walk(Path.of(args[0]))) {paths.filter(p->p.toString().endsWith(".java")).forEach(p->sources.add(p.toFile()));}
            JavacTask task=(JavacTask)compiler.getTask(null,files,diagnostics,List.of("-proc:none"),null,files.getJavaFileObjectsFromFiles(sources));
            task.parse();
            for(var diagnostic:diagnostics.getDiagnostics())if(diagnostic.getKind()==Diagnostic.Kind.ERROR)throw new AssertionError(diagnostic);
            System.out.println("PASS: parsed "+sources.size()+" Java files (syntax only; no Android SDK type-check)");
        }
    }
}
