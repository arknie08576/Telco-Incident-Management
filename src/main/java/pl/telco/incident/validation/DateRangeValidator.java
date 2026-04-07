package pl.telco.incident.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.lang.reflect.Field;
import java.time.LocalDateTime;

public class DateRangeValidator implements ConstraintValidator<DateRangeValid, Object> {

    private String fromFieldName;
    private String toFieldName;
    private boolean strict;

    @Override
    public void initialize(DateRangeValid annotation) {
        this.fromFieldName = annotation.from();
        this.toFieldName = annotation.to();
        this.strict = annotation.strict();
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        LocalDateTime from = getFieldValue(value, fromFieldName);
        LocalDateTime to = getFieldValue(value, toFieldName);

        if (from == null || to == null) {
            return true;
        }

        boolean valid = strict ? from.isBefore(to) : !from.isAfter(to);

        if (!valid) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                    .addPropertyNode(fromFieldName)
                    .addConstraintViolation();
        }

        return valid;
    }

    private LocalDateTime getFieldValue(Object target, String fieldName) {
        try {
            Field field = findField(target.getClass(), fieldName);
            field.setAccessible(true);
            return (LocalDateTime) field.get(target);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new IllegalStateException("Cannot access field '" + fieldName + "' on " + target.getClass().getSimpleName(), e);
        }
    }

    private Field findField(Class<?> clazz, String fieldName) throws NoSuchFieldException {
        Class<?> current = clazz;
        while (current != null) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchFieldException(fieldName);
    }
}
