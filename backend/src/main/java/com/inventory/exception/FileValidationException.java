package com.inventory.exception;

import com.inventory.dto.ValidationErrorDto;
import lombok.Getter;

import java.util.List;

@Getter
public class FileValidationException extends RuntimeException {

    private final List<ValidationErrorDto> errors;

    public FileValidationException(String message, List<ValidationErrorDto> errors) {
        super(message);
        this.errors = errors;
    }
}
