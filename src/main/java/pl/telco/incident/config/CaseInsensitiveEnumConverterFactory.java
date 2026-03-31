package pl.telco.incident.config;

import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterFactory;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class CaseInsensitiveEnumConverterFactory implements ConverterFactory<String, Enum> {

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <T extends Enum> Converter<String, T> getConverter(Class<T> targetType) {
        return source -> {
            if (source == null || source.isBlank()) {
                return null;
            }

            return (T) Enum.valueOf(targetType, source.trim().toUpperCase(Locale.ROOT));
        };
    }
}
