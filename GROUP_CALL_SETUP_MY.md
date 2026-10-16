# SayaConnect v2 — အဖွဲ့လိုက် Video / Voice Call

SayaConnect v2 မှာ **စုစုပေါင်း ၃ ယောက်မှ ၄ ယောက်အထိ** အဖွဲ့လိုက် Video Call နှင့် Voice Call ပြုလုပ်ရန် source code ထည့်သွင်းထားပါသည်။ Android APK အသင့်မဟုတ်သေးပါ။ Android Studio, Firebase နှင့် လိုအပ်သော server configuration များ ဖြည့်သွင်းပြီး build လုပ်ရပါမည်။

## အသုံးပြုနည်း

1. App ထဲမှာ သူငယ်ချင်း **အနည်းဆုံး ၂ ယောက်** add လုပ်ပြီး friend request များ accept လုပ်ထားပါ။
2. **Friends → Group call** ကို နှိပ်ပါ။
3. **Video** သို့မဟုတ် **Voice** ရွေးပြီး သူငယ်ချင်း ၂–၃ ယောက်ကို ရွေးပါ။ ထို့နောက် **Start call** နှိပ်ပါ။
4. ဖိတ်ခေါ်ခံရသူများက **Answer** သို့မဟုတ် **Decline** ရွေးနိုင်ပါသည်။
5. **Mic / Camera / Flip / React** ကို သုံးနိုင်ပါသည်။ စတင်ခေါ်သူက အဖွဲ့လိုက်ခေါ်ဆိုမှုကို ပိတ်နိုင်ပြီး ဖိတ်ခေါ်ခံရသူက Leave လုပ်နိုင်ပါသည်။

## တည်ဆောက်ရန်

* `README_MY.md` ထဲမှ Android Studio နှင့် Firebase setup အဆင့်များကို လိုက်နာပါ။
* အရင် SayaConnect v1 မှ update လုပ်ပါက **`backend/firestore.rules` နှင့် Cloud Functions များ ပြန် deploy လုပ်ရန် လိုအပ်ပါသည်။** `backend` folder ထဲမှာ `firebase deploy --only firestore:rules,functions` ရိုက်ထည့်ပါ။
* GitHub Actions ဖြင့် APK build လုပ်နိုင်ရန် `.github/workflows/android-debug.yml` ထည့်ထားပါသည်။ Firebase config ကို `GOOGLE_SERVICES_JSON_B64` GitHub Actions secret ထဲမှာ ထည့်ရပါမည်။ အသေးစိတ်ကို `GROUP_CALL_SETUP.md` တွင် ကြည့်ပါ။
* Myanmar နှင့် အခြားနိုင်ငံများသို့ ချိတ်ဆက်ရာတွင် TURN server လိုအပ်နိုင်ပါသည်။ အင်တာနက် မရှိသည့်အခြေအနေ၊ ကန့်သတ်ထားသော network များတွင် VPN မလိုဘဲ အမြဲချိတ်ဆက်နိုင်မည်ဟု အာမမခံနိုင်ပါ။
* အင်တာနက်နှေးပါက Settings ထဲမှ **low-data** ကို ဖွင့်ထားပြီး video အစား voice call အသုံးပြုရန် အကြံပြုပါသည်။
* Group call ကို ဖုန်းအစစ် ၃–၄ လုံးနှင့် စမ်းသပ်ခြင်း၊ Firestore security rules စမ်းသပ်ခြင်းနှင့် Android build စမ်းသပ်ခြင်းများ မပြုလုပ်ရသေးပါ။
