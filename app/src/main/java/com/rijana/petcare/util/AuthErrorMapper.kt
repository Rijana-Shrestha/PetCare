package com.rijana.petcare.util

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.rijana.petcare.viewmodel.AuthErrorField

data class AuthError(val message: String, val field: AuthErrorField)


fun mapSignInError(e: Exception): AuthError = when (e) {
    is FirebaseAuthInvalidUserException -> when (e.errorCode) {
        "ERROR_USER_NOT_FOUND" -> AuthError("No account found with this email", AuthErrorField.EMAIL)
        "ERROR_USER_DISABLED" -> AuthError("This account has been disabled", AuthErrorField.EMAIL)
        else -> AuthError("We couldn't find that account", AuthErrorField.EMAIL)
    }
    is FirebaseAuthInvalidCredentialsException -> when (e.errorCode) {
        "ERROR_WRONG_PASSWORD" -> AuthError("Incorrect password", AuthErrorField.PASSWORD)
        "ERROR_INVALID_EMAIL" -> AuthError("Enter a valid email address", AuthErrorField.EMAIL)
        // Merged code newer Firebase projects return for "wrong password" or "no such user" - see note above.
        else -> AuthError("Incorrect email or password", AuthErrorField.PASSWORD)
    }
    is FirebaseNetworkException -> AuthError("Network error - check your connection and try again", AuthErrorField.GENERAL)
    else -> AuthError(e.message ?: "Sign in failed. Please try again.", AuthErrorField.GENERAL)
}

fun mapSignUpError(e: Exception): AuthError = when (e) {
    is FirebaseAuthUserCollisionException -> AuthError("An account with this email already exists", AuthErrorField.EMAIL)
    is FirebaseAuthWeakPasswordException -> AuthError("Password is too weak - use at least 6 characters", AuthErrorField.PASSWORD)
    is FirebaseAuthInvalidCredentialsException -> AuthError("Enter a valid email address", AuthErrorField.EMAIL)
    is FirebaseNetworkException -> AuthError("Network error - check your connection and try again", AuthErrorField.GENERAL)
    else -> AuthError(e.message ?: "Sign up failed. Please try again.", AuthErrorField.GENERAL)
}