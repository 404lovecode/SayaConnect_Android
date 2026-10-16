# SayaConnect v3 — အီးမေးလ်မလိုသော Login စနစ်

ဤဖိုင်သည် **Android Studio source code** ဖြစ်ပြီး စမ်းသပ်ပြီးသား APK မဟုတ်သေးပါ။

## Login နည်းလမ်း (၃) မျိုး

1. **ဖုန်းနံပါတ် + SMS ကုဒ်** — အီးမေးလ်မလိုပါ။ မြန်မာဖုန်းနံပါတ်ဆိုလျှင် နိုင်ငံကုဒ် `+95` ဖြင့်စရေးပါ။ `+95` နောက်တွင် ပြည်တွင်းဖုန်းနံပါတ်၏ အစ `0` ကို မထည့်ပါနှင့် (ဥပမာ `+959...`)။ SMS လက်ခံနိုင်ရပါမည်။
2. **Guest/ဧည့်သည်အကောင့်** — အီးမေးလ်၊ ဖုန်းနံပါတ် မလိုဘဲ ချက်ချင်းစသုံးနိုင်ပါသည်။ သို့သော် Guest အကောင့်သည် ယာယီဖြစ်သည်။ **Sign out လုပ်ခြင်း၊ App ဖျက်ခြင်း သို့မဟုတ် App data ရှင်းခြင်းဖြင့် အကောင့်ပြန်မရနိုင်တော့ခြင်း** ဖြစ်နိုင်ပါသည်။ ဖုန်းပြောင်းမီ Settings တွင် ဖုန်းနံပါတ် ချိတ်ဆက်ထားပါ။
3. **Email + Password** — ယခင်အသုံးပြုသူများအတွက် ဆက်လက်အသုံးပြုနိုင်ပါသည်။

**Username** သည် သူငယ်ချင်းရှာရန် အသုံးပြုသည့် နာမည်ဖြစ်ပြီး ယခု version တွင် password ဖြင့် login လုပ်ရန် မဟုတ်ပါ။

## Firebase Console တွင် ပြင်ဆင်ရန်

1. Firebase Console → Authentication → Sign-in method တွင် **Phone**, **Anonymous**, **Email/Password** ကို Enable လုပ်ပါ။
2. Authentication → Settings → SMS region policy တွင် လိုအပ်သော နိုင်ငံများ (Myanmar/MM, Thailand/TH စသည်) ကို Allow လုပ်ပါ။ အသစ်ဖွင့်သော Project များတွင် အစပိုင်းအားဖြင့် SMS နိုင်ငံအားလုံး ပိတ်ထားနိုင်ပါသည်။
3. **Blaze (Pay-as-you-go)** plan လိုအပ်ပါသည်။ SMS ကုဒ်ပို့ရန် ကုန်ကျစရိတ်ရှိနိုင်သဖြင့် Budget Alert ဖွင့်ပါ။
4. Firebase Project Settings → Android App တွင် အသုံးပြုမည့် APK signing key ၏ **SHA-1 / SHA-256** fingerprint များ ထည့်ပါ။ Play Integrity နှင့် reCAPTCHA fallback ကို စမ်းသပ်ပါ။
5. မူလ Firebase Project နှင့် `app/google-services.json` ကို အသုံးပြုပါ။ အရင်အကောင့်များ၏ data မပျောက်စေရန် Firebase Project အသစ်ကို မလိုအပ်ဘဲ မပြောင်းပါနှင့်။
6. Android Studio / GitHub Actions တွင် APK build လုပ်ပါ။ GitHub Actions အတွက် `GOOGLE_SERVICES_JSON_B64` secret လိုပါမည်။

## Guest ကို ဖုန်းနံပါတ် ချိတ်ဆက်ရန်

Settings → Account security → **Link phone number to this account** ကိုနှိပ်ပါ။ SMS ကုဒ်ဖြင့် အတည်ပြုပါ။ အောင်မြင်ပါက အရင် UID၊ Profile၊ Friends နှင့် Message များ ဆက်လက်ရှိနေပါမည်။ ဖုန်းနံပါတ်တစ်ခုကို အခြား account တွင် သုံးထားပြီးပါက ချိတ်ဆက်မရပါ။

**သတိပြုရန်** — SMS ပို့ဆောင်မှုသည် ဆက်သွယ်ရေးကွန်ရက်၊ စည်းမျဉ်းနှင့် အင်တာနက်အခြေအနေများပေါ် မူတည်ပါသည်။ VPN မလိုဘဲ နေရာတိုင်းတွင် ချိတ်ဆက်နိုင်မည်ဟု အာမမခံနိုင်ပါ။ Account linking ကို စက်အမှန်များတွင် မစမ်းသပ်ရသေးပါ။

လူအများသုံးမည့် App အဖြစ် မဖြန့်ချိမီ App Check၊ abuse rate limits၊ privacy policy၊ report/block နှင့် account deletion စနစ်များ ထည့်သွင်းရန် လိုအပ်ပါသည်။
