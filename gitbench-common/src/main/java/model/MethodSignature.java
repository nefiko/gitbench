package model;

import lombok.*;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class MethodSignature implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String className;

    private String methodName;

    private List<String> parameterTypes;

    private String returnType;

    private String sourceFilePath;

    private int lineNumber;

    public String getUniqueId() {
        StringBuilder sb = new StringBuilder();
        sb.append(className).append("#").append(methodName).append("(");
        if (parameterTypes != null && !parameterTypes.isEmpty()) {
            sb.append(String.join(",", parameterTypes));
        }
        sb.append(")");
        return sb.toString();
    }

    public String getShortName() {
        String simpleClassName = className.substring(className.lastIndexOf('.') + 1);
        return simpleClassName + "." + methodName + "()";
    }

    @Override
    public String toString() {
        return getUniqueId();
    }
}