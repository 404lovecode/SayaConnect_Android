# SayaConnect v3 — Login နည်းလမ်းအသစ်များ

Phone/SMS၊ Guest (အီးမေးလ်မလို) နှင့် Email Login စနစ် (၃) မျိုး ပါရှိပါသည်။ Guest ကို Settings မှ ဖုန်းနံပါတ် ချိတ်ဆက်နိုင်ပါသည်။ **ပြင်ဆင်ရန် `LOGIN_OPTIONS_SETUP_MY.md` ကို ဖတ်ပါ။**

**v2:** အဖွဲ့လိုက် ၃–၄ ယောက် Video/Voice Call ထည့်သွင်းထားသည်။ `GROUP_CALL_SETUP_MY.md` ကို ဖတ်ပါ။ Firebase rules နှင့် functions များကို ပြန် deploy လုပ်ရပါမည်။

# SayaConnect — Android အက်ပ် တပ်ဆင်နည်း (မြန်မာ)

**ဤ ZIP သည် Android Studio အတွက် source code ဖြစ်ပါသည်။ အသင့် install လုပ်နိုင်သော APK မဟုတ်ပါ။** တကယ့်ဖုန်းအချင်းချင်း chat/video call သုံးနိုင်ရန် ကိုယ်ပိုင် Firebase project တစ်ခုကို ချိတ်ဆက်ရန် လိုအပ်ပါသည်။ Source ကို ဤနေရာ၌ Android SDK မရှိသဖြင့် compile/device-test မလုပ်နိုင်သေးပါ။

1. Windows ကွန်ပျူတာတွင် **Android Studio** တပ်ဆင်ပါ။ ZIP ကို ဖြည်၍ `settings.gradle.kts` ရှိသော `SayaConnect_Android_Studio` ဖိုလ်ဒါကို Android Studio တွင် **Open** လုပ်ပါ။
2. [Firebase Console](https://console.firebase.google.com/) တွင် project အသစ်လုပ်ပါ။ **Android App** ထည့်၍ Package name ကို `com.sayaconnect.app` ဟု ဖြည့်ပါ။ `google-services.json` ဖိုင်ကို download လုပ်ပြီး `app/google-services.json` အဖြစ် ထည့်ပါ။
3. Firebase မှ **Authentication → Email/Password**, **Cloud Firestore**, **Cloud Storage**, **Cloud Messaging** ကို ဖွင့်ပါ။
4. **Node.js 22** နှင့် **Firebase CLI** ကို install လုပ်ပါ။ `backend` ဖိုလ်ဒါတွင် `firebase login` → `firebase use --add` → `npm --prefix functions install` → `firebase deploy --only firestore:rules,storage,functions` အစဉ်လိုက် run ပါ။ Cloud Functions/Storage အသုံးပြုမှုအတွက် billing လိုနိုင်ပါသည်။
5. **မြန်မာအပါအဝင် စာသားအလိုအလျောက် ဘာသာပြန်** လိုပါက Google Cloud တွင် **Cloud Translation API** ဖွင့်၍ billing နှင့် permission များကို ပြင်ဆင်ပါ။ App Settings မှ **Auto translate** ကို ON လုပ်နိုင်ပါသည်။ ဝန်ဆောင်မှုမရပါက မူရင်းစာကို ပြသမည်။
6. မြန်မာနိုင်ငံနှင့် နိုင်ငံခြား Network များကြား ဖုန်းခေါ်ဆိုမှုကို ပိုမိုအဆင်ပြေစေရန် **TURN server** တစ်ခု စီစဉ်ပါ။ `gradle.properties` တွင် `TURN_URL`, `TURN_USER`, `TURN_PASSWORD` ကို ထည့်ပါ။ APK ထဲရှိ စကားဝှက်များကို ပြန်ဖော်နိုင်သဖြင့် public release မလုပ်မီ အချိန်တို TURN credential များသို့ ပြောင်းသင့်ပါသည်။
7. Android Studio တွင် **Sync Project** ပြီး **Run ▶** နှိပ်ပါ။ APK ဖိုင်ထုတ်ရန် **Build → Build APK(s)** ကို သုံးပါ။ Build အောင်မြင်မှ APK ဖိုင်ရပါမည်။
8. ဖုန်း ၂ လုံးတွင် install လုပ်ပြီး မတူညီသော email နှင့် account ဖွင့်ပါ။ Username တစ်ခုစီသတ်မှတ်ပါ။ **Explore** မှ `@username` ဖြင့် ရှာပြီး Friend Request ပို့ပါ။ **Friends** မှ လက်ခံကာ Chat နှင့် Video Call စမ်းပါ။ Camera/Microphone permission ခွင့်ပြုပါ။

## ပါဝင်သော လုပ်ဆောင်ချက်များ

Email ဖြင့် အကောင့်ဝင်ခြင်း၊ username၊ စာလုံးရေ ၁၀၀ အတွင်း bio (စကားလုံး ၁၀၀)၊ ပရိုဖိုင်ဓာတ်ပုံနှင့် သီချင်းထည့်ခြင်း၊ သူငယ်ချင်းရှာ/ထည့်ခြင်း၊ real-time စာတိုပို့ခြင်း၊ emoji၊ မြန်မာ/အင်္ဂလိပ် အခြေခံ UI၊ စာသားဘာသာပြန်ခြင်း (cloud service လိုအပ်)၊ WebRTC ဗီဒီယိုခေါ်ဆိုခြင်း၊ data သက်သာသော call mode၊ ဖုန်းခေါ်စဉ် emoji ပို့ခြင်း၊ နောက်ခံပုံနှင့် Light/Dark mode ပြောင်းခြင်းတို့ ပါဝင်ပါသည်။

**သတိပြုရန်** — Internet မရှိဘဲ မသုံးနိုင်ပါ။ VPN မလိုအပ်ဟု အာမခံ၍ မရပါ။ တချို့ Myanmar ISP များက Google/Firebase/TURN ဆက်သွယ်မှုကို ပိတ်ထားပါက ချိတ်ဆက်၍ မရနိုင်ပါ။ Internet နှေးလွန်းလျှင် video အရည်အသွေးကျနိုင်ပါသည်။ Firebase စာတိုများတွင် end-to-end encryption မပါသေးပါ။ Public app အဖြစ် မဖြန့်မီ စမ်းသပ်မှု၊ privacy စည်းမျဉ်း၊ moderation နှင့် security audit လိုအပ်ပါသည်။

`preview/index.html` ကို Browser ဖြင့် ဖွင့်၍ UI ဒီဇိုင်း mock preview ကို ကြည့်နိုင်ပါသည်။ ၎င်းသည် အမှန်တကယ် server ချိတ်ထားခြင်း မဟုတ်ပါ။
