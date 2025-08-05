package com.example.petpals.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
fun SignUpScreen(
    onSignUpSuccess: () -> Unit
) {
    val auth = FirebaseAuth.getInstance()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    // מצבים להצגת/הסתרת סיסמאות
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isConfirmPasswordVisible by remember { mutableStateOf(false) }

    val logoBackgroundColor = Color(0xFFFFF8F0)
    val logoBrown = Color(0xFF4E342E)

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
                .wrapContentHeight(),
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
                    "הצטרפו לקהילת PetPals",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = logoBrown,
                    textAlign = TextAlign.Center
                )

                Text(
                    "צרו חשבון חדש ותתחילו לשתף את החוויות שלכם",
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

                // סיסמה עם עין
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
                            Icon(icon, contentDescription = if (isPasswordVisible) "הסתר סיסמה" else "הצג סיסמה", tint = logoBrown)
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

                // אימות סיסמה עם עין
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = {
                        confirmPassword = it
                        errorMessage = null
                    },
                    label = { Text("אימות סיסמה") },
                    singleLine = true,
                    visualTransformation = if (isConfirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    trailingIcon = {
                        val icon = if (isConfirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                        IconButton(onClick = { isConfirmPasswordVisible = !isConfirmPasswordVisible }) {
                            Icon(icon, contentDescription = if (isConfirmPasswordVisible) "הסתר סיסמה" else "הצג סיסמה", tint = logoBrown)
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
                Button(
                    onClick = {
                        // בדיקות ולידציה בסיסיות
                        when {
                            email.isBlank() -> {
                                errorMessage = "נא למלא כתובת אימייל"; return@Button
                            }
                            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                                errorMessage = "כתובת האימייל אינה תקינה"; return@Button
                            }
                            password.isBlank() -> {
                                errorMessage = "נא למלא סיסמה"; return@Button
                            }
                            password.length < 6 -> {
                                errorMessage = "הסיסמה חייבת להכיל לפחות 6 תווים"; return@Button
                            }
                            password != confirmPassword -> {
                                errorMessage = "הסיסמאות אינן תואמות"; return@Button
                            }
                        }

                        // התחלת הרשמה
                        isLoading = true
                        auth.createUserWithEmailAndPassword(email, password)
                            .addOnCompleteListener { task ->
                                isLoading = false
                                if (task.isSuccessful) {
                                    val user = auth.currentUser
                                    if (user != null) {
                                        user.sendEmailVerification()
                                            .addOnCompleteListener { verifyTask ->
                                                if (verifyTask.isSuccessful) {
                                                    errorMessage = "נשלח מייל אימות ל-$email, בדקו את תיבת הדואר שלכם"
                                                    android.util.Log.d("SignUp", "✅ אימייל אימות נשלח בהצלחה ל-$email")
                                                } else {
                                                    errorMessage = "ההרשמה הצליחה אך לא ניתן לשלוח מייל אימות כרגע"
                                                    android.util.Log.e("SignUp", "❌ כשל בשליחת אימייל אימות: ${verifyTask.exception?.message}")
                                                }
                                                onSignUpSuccess()
                                            }
                                    } else {
                                        errorMessage = "שגיאה: המשתמש לא אותחל"
                                        android.util.Log.e("SignUp", "❌ auth.currentUser חזר null לאחר הרשמה")
                                    }
                                } else {
                                    android.util.Log.e("SignUp", "❌ שגיאת הרשמה: ${task.exception?.message}")
                                    errorMessage = when {
                                        task.exception?.message?.contains("already in use") == true -> "כתובת האימייל כבר קיימת במערכת"
                                        task.exception?.message?.contains("weak") == true -> "הסיסמה חלשה מדי"
                                        task.exception?.message?.contains("invalid") == true -> "כתובת אימייל לא תקינה"
                                        else -> "שגיאה ביצירת החשבון. נסו שנית"
                                    }
                                }
                            }
                    },
                    enabled = !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
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
                        Text("הירשם עכשיו", fontWeight = FontWeight.Medium, fontSize = 18.sp)
                    }
                }

             // כרטיס תנאים
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = logoBrown.copy(alpha = 0.05f)
                    )
                ) {
                    Text(
                        text = "על ידי הרשמה אתם מסכימים לתנאי השימוש ומדיניות הפרטיות של PetPals",
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = logoBrown,
                        textAlign = TextAlign.Center
                    )
                }

                // הודעת שגיאה / הצלחה
                errorMessage?.let { message ->
                    val isSuccess = message.contains("נשלח מייל אימות")
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSuccess) Color(0xFFB9F6CA) else MaterialTheme.colorScheme.errorContainer
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    ) {
                        Text(
                            text = message,
                            modifier = Modifier
                                .padding(12.dp)
                                .fillMaxWidth(),
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

