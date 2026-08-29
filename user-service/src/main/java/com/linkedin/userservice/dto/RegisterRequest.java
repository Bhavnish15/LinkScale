package com.linkedin.userservice.dto;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    @NotBlank(message = "Email is Required")
    @Email(message = "Invalid Email Format")
    private String email;

    @NotBlank(message = "Password is Required")
    @Size(min = 6, max = 100, message = "Password must be between 6 and 100 characters")
    private String password;

    @NotBlank(message = "First Name is Required")
    private String firstname;

    @NotBlank(message = "Last Name is Required")
    private String lastname;

    private String headline;
    private String location;
}
