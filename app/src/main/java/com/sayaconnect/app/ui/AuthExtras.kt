package com.sayaconnect.app.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.PhoneAuthCredential
import com.sayaconnect.app.data.ConnectRepository
import kotlinx.coroutines.launch

private fun Context.hostActivity(): Activity? {
    var current: Context = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return current as? Activity
}

/** Shared by first-time phone login and by existing users linking a recovery phone. */
@Composable
fun PhoneAuthForm(
    repo: ConnectRepository,
    ui: String,
    linkToCurrent: Boolean,
    onFinished: () -> Unit,
    onError: (Throwable) -> Unit
) {
    val activity = LocalContext.current.hostActivity()
    val scope = rememberCoroutineScope()
    var number by remember { mutableStateOf("+95") }
    var smsCode by remember { mutableStateOf("") }
    var verificationId by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var waitingForSms by remember { mutableStateOf(false) }

    fun submitCredential(credential: PhoneAuthCredential) {
        if (busy) return
        busy = true
        scope.launch {
            try {
                repo.usePhoneCredential(credential, linkToCurrent)
                onFinished()
            } catch (e: Exception) {
                onError(e)
            } finally {
                busy = false
            }
        }
    }

    Column(horizontalAlignment = Alignment.Start) {
        Text(if (linkToCurrent) {
            if (ui == "my") "ဖုန်းနံပါတ် ချိတ်ဆက်ရန်" else "Protect your account"
        } else {
            if (ui == "my") "ဖုန်းနံပါတ်ဖြင့် ဝင်ရောက်ရန်" else "Sign in with your phone"
        }, fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(10.dp))
        Text(if (ui == "my") "နိုင်ငံကုဒ်ပါသော ဖုန်းနံပါတ်ကို ရိုက်ထည့်ပါ (ဥပမာ +959...)"
            else "Enter your phone number with country code (e.g. +959...). Omit the local leading zero.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(value = number,
            onValueChange = { number = it.filter { c -> c.isDigit() || c == '+' }.take(16) },
            modifier = Modifier.fillMaxWidth(), singleLine = true,
            label = { Text(if (ui == "my") "ဖုန်းနံပါတ်" else "Phone number") },
            leadingIcon = { Icon(Icons.Default.PhoneAndroid, null) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
        Spacer(Modifier.height(12.dp))
        Button(onClick = {
            if (activity == null) {
                onError(IllegalStateException("Phone verification requires an Android Activity"))
            } else {
                busy = true
                try {
                    repo.sendPhoneCode(activity, number,
                        onCodeSent = { id ->
                            verificationId = id
                            waitingForSms = true
                            busy = false
                        },
                        onAutoVerified = { credential ->
                            waitingForSms = false
                            busy = false
                            submitCredential(credential)
                        },
                        onFailure = { error ->
                            busy = false
                            onError(error)
                        })
                } catch (e: Exception) {
                    busy = false
                    onError(e)
                }
            }
        }, enabled = !busy && Regex("^\\+[1-9][0-9]{6,14}$").matches(number),
            modifier = Modifier.fillMaxWidth().height(50.dp)) {
            if (busy) CircularProgressIndicator(Modifier.size(19.dp))
            else Text(if (waitingForSms) {
                if (ui == "my") "ကုဒ်ပြန်ပို့ရန်" else "Send another code"
            } else if (ui == "my") "SMS ကုဒ်ပို့ရန်" else "Send SMS code")
        }

        if (waitingForSms && verificationId != null) {
            Spacer(Modifier.height(14.dp))
            OutlinedTextField(value = smsCode,
                onValueChange = { smsCode = it.filter(Char::isDigit).take(6) },
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                label = { Text(if (ui == "my") "ဂဏန်း ၆ လုံး ကုဒ်" else "6-digit verification code") },
                leadingIcon = { Icon(Icons.Default.VerifiedUser, null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            Spacer(Modifier.height(12.dp))
            Button(onClick = {
                busy = true
                val id = verificationId ?: return@Button
                scope.launch {
                    try {
                        repo.verifyPhoneCode(id, smsCode, linkToCurrent)
                        onFinished()
                    } catch (e: Exception) {
                        onError(e)
                    } finally {
                        busy = false
                    }
                }
            }, enabled = !busy && smsCode.length == 6,
                modifier = Modifier.fillMaxWidth().height(50.dp)) {
                if (busy) CircularProgressIndicator(Modifier.size(19.dp))
                else Text(if (ui == "my") "အတည်ပြုမည်" else "Verify and continue")
            }
        }
        Spacer(Modifier.height(14.dp))
        Text(if (ui == "my")
            "SMS ရရှိရန် ဖုန်းလိုင်းလိုအပ်ပြီး သင့်နံပါတ်ကို Firebase မှ စစ်ဆေးအတည်ပြုပါမည်။ SMS ဝန်ဆောင်မှုသည် ကွန်ရက်အလိုက် ကွာခြားနိုင်ပါသည်။"
            else "Verification requires SMS reception and internet. Your number is processed by Firebase to verify your identity. SMS availability varies by network.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun PhoneLinkScreen(
    repo: ConnectRepository,
    ui: String,
    onBack: () -> Unit,
    onFinished: () -> Unit,
    onError: (Throwable) -> Unit
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp)) {
        TextButton(onClick = onBack) {
            Icon(Icons.Default.ArrowBack, null)
            Spacer(Modifier.width(8.dp))
            Text(if (ui == "my") "နောက်သို့" else "Back to settings")
        }
        Spacer(Modifier.height(18.dp))
        Text(if (ui == "my") "အကောင့်ကို လုံခြုံစေပါ" else "Secure your SayaConnect account",
            fontSize = 25.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(11.dp))
        Text(if (ui == "my")
            "မူလအကောင့်၊ သူငယ်ချင်းများနှင့် စာတိုများမပျောက်စေရန် သင့်ဖုန်းနံပါတ်ကို လက်ရှိအကောင့်နှင့် ချိတ်ဆက်ပါ။"
            else "Attach a phone number to your existing account, so you can sign in again on another phone without changing your user ID.",
            style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(20.dp))
        PhoneAuthForm(repo, ui, linkToCurrent = true, onFinished = onFinished, onError = onError)
        Spacer(Modifier.height(16.dp))
        Text(if (ui == "my")
            "ဖုန်းနံပါတ်ကို အခြားအကောင့်တစ်ခုတွင် အသုံးပြုပြီးပါက လက်ရှိအကောင့်နှင့် ချိတ်ဆက်၍မရပါ။"
            else "A phone number already associated with another account cannot be linked here. SayaConnect will not overwrite your existing account.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
