package com.example.petpals.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.petpals.R
import com.google.firebase.auth.FirebaseAuth

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onNavigateToSignUp: () -> Unit
) {
    val auth = FirebaseAuth.getInstance()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var isPasswordVisible by remember { mutableStateOf(false) }

    val logoBrown = Color(0xFF4E342E)
    val logoBackgroundColor = Color(0xFFFFF8F0)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(logoBackgroundColor)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(), // גובה מותאם אוטומטית
            elevation = CardDefaults.cardElevation(12.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = logoBackgroundColor)
        ) {
            Column(
                modifier = Modifier
                    .padding(32.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                PetPalsLogo()

                Text(
                    "ברוכים הבאים ל-PetPals",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = logoBrown,
                    textAlign = TextAlign.Center
                )

                Text(
                    "שתפו את הרגעים המיוחדים עם חיות המחמד שלכם",
                    style = MaterialTheme.typography.bodyMedium,
                    color = logoBrown.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )

                // אימייל
                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        errorMessage = null
                    },
                    label = { Text("אימייל") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = logoBrown,
                        unfocusedBorderColor = logoBrown,
                        focusedLabelColor = logoBrown,
                        unfocusedLabelColor = logoBrown,
                        cursorColor = logoBrown,
                        focusedTextColor = logoBrown,
                        unfocusedTextColor = logoBrown
                    )
                )

                // סיסמה עם אייקון עין
                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        errorMessage = null
                    },
                    label = { Text("סיסמה") },
                    singleLine = true,
                    visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    trailingIcon = {
                        val icon = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                        IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                            Icon(
                                icon,
                                contentDescription = if (isPasswordVisible) "הסתר סיסמה" else "הצג סיסמה",
                                tint = logoBrown
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = logoBrown,
                        unfocusedBorderColor = logoBrown,
                        focusedLabelColor = logoBrown,
                        unfocusedLabelColor = logoBrown,
                        cursorColor = logoBrown,
                        focusedTextColor = logoBrown,
                        unfocusedTextColor = logoBrown
                    )
                )

                // שכחתי סיסמה
                TextButton(
                    onClick = {
                        if (email.isBlank()) {
                            errorMessage = "נא להזין כתובת אימייל כדי לאפס סיסמה"
                        } else {
                            auth.sendPasswordResetEmail(email)
                                .addOnCompleteListener { task ->
                                    errorMessage = if (task.isSuccessful) {
                                        "אימייל לאיפוס סיסמה נשלח בהצלחה"
                                    } else {
                                        "שליחת האימייל נכשלה. בדקו את הכתובת ונסו שוב"
                                    }
                                }
                        }
                    },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("שכחתי סיסמה", color = logoBrown, fontSize = 14.sp)
                }

                // כפתור התחברות
                Button(
                    onClick = {
                        if (email.isBlank() || password.isBlank()) {
                            errorMessage = "נא למלא את כל השדות"
                            return@Button
                        }

                        isLoading = true
                        auth.signInWithEmailAndPassword(email, password)
                            .addOnCompleteListener { task ->
                                isLoading = false
                                if (task.isSuccessful) {
                                    onLoginSuccess()
                                } else {
                                    errorMessage = when {
                                        task.exception?.message?.contains("password") == true -> "סיסמה שגויה"
                                        task.exception?.message?.contains("email") == true -> "כתובת אימייל לא קיימת"
                                        else -> "שגיאה בהתחברות. נסו שנית"
                                    }
                                }
                            }
                    },
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = logoBrown,
                        contentColor = Color.White
                    )
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White
                        )
                    } else {
                        Text("התחבר", fontWeight = FontWeight.Medium, fontSize = 18.sp)
                    }
                }

                // מעבר להרשמה
                TextButton(
                    onClick = onNavigateToSignUp,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.textButtonColors(contentColor = logoBrown)
                ) {
                    Text("אין לך חשבון? הירשם כאן", color = logoBrown)
                }

                // הודעת שגיאה / הצלחה
                errorMessage?.let { message ->
                    val isSuccess = message.contains("נשלח בהצלחה")
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSuccess) Color(0xFFB9F6CA) else MaterialTheme.colorScheme.errorContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                            .padding(top = 8.dp) // מוסיף רווח קטן מלמעלה
                    ) {
                        Text(
                            text = message,
                            modifier = Modifier.padding(12.dp),
                            color = if (isSuccess) Color(0xFF1B5E20) else MaterialTheme.colorScheme.onErrorContainer,
                            textAlign = TextAlign.Center,
                            softWrap = true
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PetPalsLogo() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        androidx.compose.foundation.Image(
            painter = androidx.compose.ui.res.painterResource(
                id = R.drawable.logo_petpals
            ),
            contentDescription = "PetPals Logo",
            modifier = Modifier.size(160.dp)
        )
    }
}


