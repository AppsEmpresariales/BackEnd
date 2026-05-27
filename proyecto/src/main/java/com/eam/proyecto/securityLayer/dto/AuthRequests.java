package com.eam.proyecto.securityLayer.dto;

import lombok.Data;

public class AuthRequests {

    @Data
    public static class LoginRequest {
        private String email;
        private String password;
    }

    @Data
    public static class RegisterRequest {
        private Long cedula;
        private String nombre;
        private String email;
        private String password;
        private Long organizacionNit;
        private String organizacionNombre;
        private String organizacionEmail;
        private String organizacionTelefono;
        private String organizacionDirCalle;
        private String organizacionDirNumero;
        private String organizacionDirComuna;
    }
}
