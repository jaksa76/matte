// This is a simplified example of an annotation processor
// In a real framework, this would run at compile time and generate mapper classes

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import java.util.Set;

// @SupportedAnnotationTypes("Entity")
// @SupportedSourceVersion(SourceVersion.RELEASE_17)
public class EntityProcessor { // extends AbstractProcessor {
    
    // This would be called by the Java compiler during compilation
    // public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    //     for (Element element : roundEnv.getElementsAnnotatedWith(Entity.class)) {
    //         // Generate mapper class for each @Entity
    //         generateMapper((TypeElement) element);
    //     }
    //     return true;
    // }
    
    public static void generateMapperExample() {
        System.out.println("""
            
            === How Annotation Processing Works ===
            
            1. Developer writes entity:
               @Entity(table = "users")
               class User { ... }
            
            2. Compiler runs annotation processor during compilation
            
            3. Processor generates UserMapper.java with:
               - toJson(User) method - direct field access
               - fromJson(String) method - direct field setters
               - toInsertSQL(User) method - direct field access
               - toSelectSQL(id) method - table name from annotation
            
            4. Generated code has ZERO reflection!
            
            Real frameworks using this approach:
            - Micronaut (compile-time DI & AOP)
            - Quarkus (compile-time reflection alternative)
            - MapStruct (compile-time mapping)
            - Lombok (compile-time code generation)
            - Dagger (compile-time DI)
            """);
    }
}
