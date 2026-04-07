package pl.telco.incident.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = DateRangeValidator.class)
@Repeatable(DateRangeValid.List.class)
public @interface DateRangeValid {

    String from();

    String to();

    /**
     * When true, 'from' must be strictly before 'to' (equal is invalid).
     * When false (default), 'from' must be before or equal to 'to'.
     */
    boolean strict() default false;

    String message() default "Invalid date range";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    @Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @Constraint(validatedBy = {})
    @interface List {
        DateRangeValid[] value();

        Class<?>[] groups() default {};

        Class<? extends Payload>[] payload() default {};
    }
}
