package com.micakee.app

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalLayoutDirection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

private val Teal = Color(0xFF0B7F82)
private val DeepTeal = Color(0xFF075E64)
private val Gold = Color(0xFFF0C94B)
private val GoldSoft = Color(0xFFFFE9A6)
private val Cream = Color(0xFFF9F5EA)
private val White = Color(0xFFFFFFFF)
private val Ink = Color(0xFF173E40)
private val Muted = Color(0xFF6F8584)
private const val BASE = "https://micakee.ir"
private val http = OkHttpClient()

private suspend fun api(path: String, method: String = "GET", body: String? = null, token: String = ""): Result<String> = withContext(Dispatchers.IO) {
    runCatching {
        val rb = body?.toRequestBody("application/json".toMediaType())
        val req = Request.Builder().url(BASE + path)
            .apply { if (token.isNotBlank()) addHeader("Authorization", "Bearer $token") }
            .method(method, rb).build()
        http.newCall(req).execute().use { res ->
            val text = res.body?.string().orEmpty()
            if (!res.isSuccessful) error(JSONObject(text.ifBlank { "{}" }).optString("error", "خطای سرور"))
            text
        }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) { MiCakeeApp(this) } }
    }
}

@Composable private fun Logo(modifier: Modifier = Modifier) {
    Image(painterResource(R.drawable.micakee_logo), null, modifier.clip(RoundedCornerShape(24.dp)), contentScale = ContentScale.Crop)
}

@Composable private fun MiCakeeApp(ctx: Context) {
    val pref = ctx.getSharedPreferences("micakee", Context.MODE_PRIVATE)
    var token by remember { mutableStateOf(pref.getString("token", "").orEmpty()) }
    var screen by remember { mutableStateOf(if (token.isBlank()) "login" else "home") }
    var mobile by remember { mutableStateOf(pref.getString("mobile", "").orEmpty()) }
    var otp by remember { mutableStateOf("") }
    var msg by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var requestNonce by remember { mutableIntStateOf(0) }
    var verifyNonce by remember { mutableIntStateOf(0) }

    MaterialTheme(colorScheme = lightColorScheme(primary = Teal, secondary = Gold, background = Cream)) {
        when (screen) {
            "login" -> LoginScreen(mobile, { mobile = it }, busy, msg) { if (!Regex("09\\d{9}").matches(mobile)) { msg = "شماره موبایل ۱۱ رقمی معتبر وارد کن." } else { busy=true; msg="در حال ارسال کد..."; requestNonce++ } }
            "otp" -> OtpScreen(otp, { otp=it }, busy, msg, { screen="login"; otp="" }) { if (!Regex("\\d{6}").matches(otp)) msg="کد ۶ رقمی را وارد کن." else { busy=true; msg="در حال بررسی کد..."; verifyNonce++ } }
            "home" -> HomeScreen({screen="profile"},{screen="recipes"},{screen="calc"},{screen="price"},{screen="design"},{screen="assistant"})
            "profile" -> ProfileScreen(token,{screen="home"}) { pref.edit().clear().apply(); token=""; mobile=""; screen="login" }
            "recipes" -> RecipesScreen(token){screen="home"}
            "calc" -> CalculatorScreen{screen="home"}
            "price" -> PriceScreen{screen="home"}
            "design" -> DesignScreen(token){screen="home"}
            "assistant" -> AssistantScreen(token){screen="home"}
        }
        LaunchedEffect(requestNonce) { if(requestNonce==0) return@LaunchedEffect; val r=api("/api/auth/request-otp","POST",JSONObject().put("mobile",mobile).toString()); busy=false; r.onSuccess { msg="کد ارسال شد ✨"; screen="otp" }.onFailure { msg="⚠️ ${it.message}" } }
        LaunchedEffect(verifyNonce) { if(verifyNonce==0) return@LaunchedEffect; val r=api("/api/auth/verify-otp","POST",JSONObject().put("mobile",mobile).put("code",otp).toString()); busy=false; r.onSuccess { val t=JSONObject(it).optString("token"); if(t.isBlank()) msg="پاسخ ورود نامعتبر است." else { pref.edit().putString("token",t).putString("mobile",mobile).apply(); token=t; msg=""; screen="home" } }.onFailure { msg="⚠️ ${it.message}" } }
    }
}

@Composable private fun LoginScreen(m:String,onM:(String)->Unit,busy:Boolean,msg:String,onNext:()->Unit){
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(DeepTeal,Teal,Cream)))) {
        Column(Modifier.fillMaxSize().padding(22.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center){
            Logo(Modifier.size(150.dp)); Spacer(Modifier.height(20.dp)); Text("می‌کیک",fontSize=34.sp,fontWeight=FontWeight.ExtraBold,color=White); Text("دستیار هوشمند قنادها",fontSize=18.sp,color=White); Spacer(Modifier.height(28.dp))
            Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(30.dp),colors=CardDefaults.cardColors(containerColor=White)){ Column(Modifier.padding(22.dp)){ Text("خوش اومدی قناد جان 👩🏻‍🍳",fontSize=21.sp,fontWeight=FontWeight.Bold,color=Ink); Text("برای ورود فقط شماره موبایلت رو وارد کن.",color=Muted); Spacer(Modifier.height(18.dp)); OutlinedTextField(m,onM,label={Text("شماره موبایل")},singleLine=true,modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(18.dp)); Spacer(Modifier.height(12.dp)); Button(onClick=onNext,enabled=!busy,modifier=Modifier.fillMaxWidth().height(56.dp),shape=RoundedCornerShape(18.dp),colors=ButtonDefaults.buttonColors(containerColor=Teal)){Text(if(busy)"در حال ارسال..." else "دریافت کد ورود  ←",fontSize=17.sp,fontWeight=FontWeight.Bold)}; if(msg.isNotBlank()){Spacer(Modifier.height(10.dp));Text(msg,color=Teal)} } }
        }
    }
}

@Composable private fun OtpScreen(o:String,onO:(String)->Unit,busy:Boolean,msg:String,onBack:()->Unit,onVerify:()->Unit){
    Surface(Modifier.fillMaxSize(),color=Cream){Column(Modifier.fillMaxSize().padding(22.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center){Logo(Modifier.size(100.dp));Spacer(Modifier.height(18.dp));Text("تأیید ورود",fontSize=28.sp,fontWeight=FontWeight.ExtraBold,color=Ink);Text("کد ۶ رقمی پیامک را وارد کن",color=Muted);Spacer(Modifier.height(22.dp));OutlinedTextField(o,{if(it.length<=6)onO(it)},label={Text("کد تأیید")},singleLine=true,modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(18.dp));Spacer(Modifier.height(12.dp));Button(onClick=onVerify,enabled=!busy,modifier=Modifier.fillMaxWidth().height(56.dp),shape=RoundedCornerShape(18.dp),colors=ButtonDefaults.buttonColors(containerColor=Teal)){Text(if(busy)"در حال بررسی..." else "تأیید و ورود",fontSize=17.sp,fontWeight=FontWeight.Bold)};TextButton(onClick=onBack){Text("ویرایش شماره",color=Teal)};if(msg.isNotBlank())Text(msg,color=Teal)}}
}

@Composable private fun HomeScreen(account:()->Unit,recipes:()->Unit,calc:()->Unit,price:()->Unit,design:()->Unit,assistant:()->Unit){
    Scaffold(containerColor=Cream,bottomBar={Row(Modifier.fillMaxWidth().background(White).padding(vertical=8.dp),horizontalArrangement=Arrangement.SpaceEvenly){BottomButton("⌂","خانه"){};BottomButton("📖","دستورها"){recipes()};BottomButton("🧮","ابزار"){calc()};BottomButton("👤","حساب"){account()}}}){p->
        Column(Modifier.fillMaxSize().padding(p).padding(horizontal=18.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(14.dp)){
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Logo(Modifier.size(64.dp));Spacer(Modifier.width(10.dp));Column(Modifier.weight(1f)){Text("mi.cakee",fontSize=26.sp,fontWeight=FontWeight.ExtraBold,color=Teal);Text("دستیار هوشمند قنادها",fontSize=12.sp,color=Muted)};Box(Modifier.size(46.dp).clip(CircleShape).background(White).clickable{account()},contentAlignment=Alignment.Center){Text("👩🏻‍🍳",fontSize=23.sp)}}
            Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(28.dp),colors=CardDefaults.cardColors(containerColor=White)){Column(Modifier.padding(20.dp)){Text("سلام قناد جان 👋",fontSize=24.sp,fontWeight=FontWeight.ExtraBold,color=Ink);Spacer(Modifier.height(5.dp));Text("دنیای شیرینی‌های خاص، با هوش مصنوعی ✨",fontSize=14.sp,color=Muted);Spacer(Modifier.height(12.dp));Row(verticalAlignment=Alignment.CenterVertically){Text("اشتراک رایگان",fontSize=12.sp,fontWeight=FontWeight.Bold,color=Teal,modifier=Modifier.background(Cream,RoundedCornerShape(50.dp)).padding(horizontal=12.dp,vertical=7.dp));Spacer(Modifier.weight(1f));Text("پروفایل من  ›",fontSize=13.sp,color=Teal,modifier=Modifier.clickable{account()})}}}
            OutlinedTextField(value="",onValueChange={},readOnly=true,label={Text("جستجوی دستور، مواد اولیه یا سؤال از هوش مصنوعی...")},leadingIcon={Text("🔎",fontSize=19.sp)},modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(18.dp),colors=OutlinedTextFieldDefaults.colors(unfocusedContainerColor=White,focusedContainerColor=White))
            Card(Modifier.fillMaxWidth().height(205.dp).clickable{design()},shape=RoundedCornerShape(30.dp),colors=CardDefaults.cardColors(containerColor=Teal)){Box(Modifier.fillMaxSize().padding(22.dp)){Column(Modifier.fillMaxHeight().fillMaxWidth(.80f),verticalArrangement=Arrangement.SpaceBetween){Column{Text("کیک رؤیایی‌ات را بساز ✨",fontSize=24.sp,fontWeight=FontWeight.ExtraBold,color=White);Spacer(Modifier.height(7.dp));Text("ایده‌ات را بگو؛ می‌کیک برایت طرح پیشنهادی می‌سازد.",fontSize=14.sp,color=White.copy(alpha=.88f))};Button(onClick=design,shape=RoundedCornerShape(16.dp),colors=ButtonDefaults.buttonColors(containerColor=Gold)){Text("شروع طراحی",color=Ink,fontWeight=FontWeight.Bold)}};Text("👑",fontSize=70.sp,modifier=Modifier.align(Alignment.BottomEnd))}}
            Text("ابزارهای کاربردی",fontSize=20.sp,fontWeight=FontWeight.Bold,color=Ink)
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)){ToolCard("🧁","تغییر مقدار مواد",calc);ToolCard("💰","قیمت‌گذاری",price)}
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)){ToolCard("📖","دستورهای محبوب",recipes);ToolCard("🤖","دستیار AI",assistant)}
            Card(Modifier.fillMaxWidth().clickable{assistant()},shape=RoundedCornerShape(24.dp),colors=CardDefaults.cardColors(containerColor=GoldSoft)){Row(Modifier.padding(18.dp),verticalAlignment=Alignment.CenterVertically){Box(Modifier.size(54.dp).clip(CircleShape).background(White),contentAlignment=Alignment.Center){Text("🎙",fontSize=28.sp)};Spacer(Modifier.width(12.dp));Column(Modifier.weight(1f)){Text("سؤال صوتی از AI",fontSize=17.sp,fontWeight=FontWeight.Bold,color=Ink);Text("با صدای خودت سؤال بپرس و جواب بگیر",fontSize=13.sp,color=Muted)};Text("›",fontSize=29.sp,color=Teal)}}
            Card(Modifier.fillMaxWidth().clickable{recipes()},shape=RoundedCornerShape(24.dp),colors=CardDefaults.cardColors(containerColor=White)){Column(Modifier.padding(18.dp)){Row(verticalAlignment=Alignment.CenterVertically){Text("دستورهای محبوب",fontSize=18.sp,fontWeight=FontWeight.Bold,color=Ink);Spacer(Modifier.weight(1f));Text("مشاهده همه  ›",fontSize=13.sp,color=Teal)};Spacer(Modifier.height(8.dp));Text("برای سفارش بعدی ایده بگیر و دستورهای قنادی را سریع پیدا کن.",fontSize=13.sp,color=Muted)}}
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable private fun BottomButton(icon:String,label:String,onClick:()->Unit){
    Column(horizontalAlignment=Alignment.CenterHorizontally,modifier=Modifier.clickable{onClick()}.padding(horizontal=14.dp)){Text(icon,fontSize=21.sp);Text(label,fontSize=11.sp,color=Teal)}
}

@Composable private fun RowScope.ToolCard(icon:String,title:String,onClick:()->Unit){Card(Modifier.weight(1f).height(128.dp).clickable{onClick()},shape=RoundedCornerShape(24.dp),colors=CardDefaults.cardColors(containerColor=White)){Column(Modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.SpaceBetween){Box(Modifier.size(48.dp).clip(CircleShape).background(Cream),contentAlignment=Alignment.Center){Text(icon,fontSize=25.sp)};Text(title,fontSize=15.sp,fontWeight=FontWeight.Bold,color=Ink)}}}


@Composable private fun CalcRow(label:String,value:Int,unit:String){
    Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(18.dp),colors=CardDefaults.cardColors(containerColor=White)){
        Row(Modifier.fillMaxWidth().padding(16.dp),verticalAlignment=Alignment.CenterVertically){
            Text(label,fontWeight=FontWeight.Bold,color=Ink,modifier=Modifier.weight(1f))
            Text("$value $unit",fontWeight=FontWeight.Bold,color=Teal)
        }
    }
    Spacer(Modifier.height(8.dp))
}

@Composable private fun ProfileScreen(token:String,back:()->Unit,logout:()->Unit){var text by remember{mutableStateOf("در حال دریافت اطلاعات حساب...")};LaunchedEffect(token){api("/api/me",token=token).onSuccess{val u=JSONObject(it).optJSONObject("user");text="شماره موبایل: ${u?.optString("mobile","")}\nنوع حساب: ${u?.optString("plan","رایگان")}"}.onFailure{text="⚠️ ${it.message}"}};SimplePage("حساب کاربری",back){Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(26.dp),colors=CardDefaults.cardColors(containerColor=White)){Column(Modifier.padding(20.dp)){Text("حساب من",fontSize=20.sp,fontWeight=FontWeight.Bold,color=Ink);Spacer(Modifier.height(10.dp));Text(text,color=Muted)}};Spacer(Modifier.height(16.dp));Button(logout,Modifier.fillMaxWidth().height(52.dp),shape=RoundedCornerShape(16.dp),colors=ButtonDefaults.buttonColors(containerColor=Teal)){Text("خروج از حساب")}}
}

@Composable private fun RecipesScreen(token:String,back:()->Unit){
    var list by remember{mutableStateOf(listOf<Pair<String,String>>())}
    var msg by remember{mutableStateOf("در حال دریافت دستورها...")}
    LaunchedEffect(Unit){
        api("/api/recipes",token=token).onSuccess{a->
            val j=JSONArray(a)
            list=(0 until j.length()).map{val x=j.getJSONObject(it);x.optString("name") to x.optString("category")}
            msg=if(list.isEmpty())"بانک دستورها فعلاً خالی است." else ""
        }.onFailure{msg="⚠️ ${it.message}"}
    }
    SimplePage("بانک دستورهای می‌کیک",back){
        if(msg.isNotBlank())Text(msg,color=Muted)
        list.forEach{x->
            Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(20.dp),colors=CardDefaults.cardColors(containerColor=White)){
                Column(Modifier.padding(17.dp)){
                    Text(x.first,fontWeight=FontWeight.Bold,fontSize=17.sp,color=Ink)
                    Text(x.second,color=Teal,fontSize=13.sp)
                }
            }
            Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
private fun CalculatorScreen(back: () -> Unit) {
    var eggs by remember { mutableStateOf("6") }
    val n = eggs.toIntOrNull()?.coerceAtLeast(1) ?: 1
    SimplePage("محاسبه مواد اولیه", back) {
        OutlinedTextField(
            value = eggs,
            onValueChange = { eggs = it.filter(Char::isDigit) },
            label = { Text("تعداد تخم‌مرغ") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp)
        )
        Spacer(Modifier.height(12.dp))
        CalcRow("آرد", n * 30, "گرم")
        CalcRow("شکر", n * 30, "گرم")
        CalcRow("آب", n * 10, "گرم")
        CalcRow("روغن مایع", n * 10, "گرم")
        Text("بیکینگ‌پودر: برای ۶ تخم‌مرغ حدود ۱ قاشق غذاخوری", color = Muted, fontSize = 13.sp)
    }
}

@Composable
private fun PriceScreen(back: () -> Unit) {
    var cost by remember { mutableStateOf("") }
    var margin by remember { mutableStateOf("30") }
    val c = cost.toDoubleOrNull() ?: 0.0
    val m = margin.toDoubleOrNull() ?: 0.0
    val sale = c * (1 + m / 100)
    SimplePage("قیمت‌گذاری و سود", back) {
        OutlinedTextField(
            value = cost,
            onValueChange = { cost = it.filter(Char::isDigit) },
            label = { Text("هزینه مواد (تومان)") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp)
        )
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = margin,
            onValueChange = { margin = it.filter(Char::isDigit) },
            label = { Text("درصد سود") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp)
        )
        Spacer(Modifier.height(18.dp))
        Card(
            Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Teal)
        ) {
            Column(Modifier.padding(20.dp)) {
                Text("قیمت پیشنهادی فروش", color = White)
                Text("${sale.toLong()} تومان", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = Gold)
            }
        }
    }
}

@Composable
private fun DesignScreen(token: String, back: () -> Unit) {
    var prompt by remember { mutableStateOf("") }
    var out by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var nonce by remember { mutableIntStateOf(0) }
    LaunchedEffect(nonce) {
        if (nonce == 0) return@LaunchedEffect
        api("/api/design", "POST", JSONObject().put("prompt", prompt).toString(), token)
            .onSuccess {
                out = JSONObject(it).optString("answer", "طرحی دریافت نشد.")
                busy = false
            }
            .onFailure {
                out = "⚠️ ${it.message}"
                busy = false
            }
    }
    SimplePage("طراحی کیک با هوش مصنوعی", back) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Teal)
        ) {
            Column(Modifier.padding(20.dp)) {
                Text("کیک رویایی‌ات را توصیف کن ✨", fontSize = 21.sp, fontWeight = FontWeight.Bold, color = White)
                Spacer(Modifier.height(6.dp))
                Text("رنگ، مناسبت، تعداد طبقات و سبک تزیین را بنویس.", color = White.copy(alpha = .85f))
                Spacer(Modifier.height(14.dp))
                OutlinedTextField(
                    value = prompt,
                    onValueChange = { prompt = it },
                    label = { Text("مثلاً کیک تولد دخترانه فیروزه‌ای و طلایی") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 4,
                    shape = RoundedCornerShape(18.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = White,
                        focusedContainerColor = White
                    )
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { busy = true; nonce++ },
                    enabled = !busy && prompt.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Gold)
                ) {
                    Text(if (busy) "در حال ساخت..." else "ساخت طرح پیشنهادی", color = Ink, fontWeight = FontWeight.Bold)
                }
            }
        }
        if (out.isNotBlank()) {
            Spacer(Modifier.height(14.dp))
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = White)
            ) {
                Text(out, Modifier.padding(18.dp), color = Ink, fontSize = 15.sp)
            }
        }
    }
}

@Composable
private fun AssistantScreen(token: String, back: () -> Unit) {
    var q by remember { mutableStateOf("") }
    var answer by remember { mutableStateOf("سلام قناد جان 👑\nسؤالت را درباره شیرینی و قنادی بپرس.") }
    var busy by remember { mutableStateOf(false) }
    var nonce by remember { mutableIntStateOf(0) }
    LaunchedEffect(nonce) {
        if (nonce == 0) return@LaunchedEffect
        api("/api/ai/ask", "POST", JSONObject().put("text", q).toString(), token)
            .onSuccess {
                answer = JSONObject(it).optString("answer", "پاسخی دریافت نشد.")
                busy = false
            }
            .onFailure {
                answer = "⚠️ ${it.message}"
                busy = false
            }
    }
    SimplePage("دستیار هوشمند قنادها", back) {
        Card(
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = White)
        ) {
            Text(answer, Modifier.padding(20.dp), color = Ink, fontSize = 17.sp)
        }
        Spacer(Modifier.weight(1f))
        OutlinedTextField(
            value = q,
            onValueChange = { q = it },
            label = { Text("سؤالت را بنویس...") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            shape = RoundedCornerShape(18.dp)
        )
        Spacer(Modifier.height(10.dp))
        Button(
            onClick = { busy = true; nonce++ },
            enabled = !busy && q.isNotBlank(),
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Teal)
        ) {
            Text(if (busy) "در حال پاسخ..." else "ارسال سؤال")
        }
    }
}

@Composable private fun SimplePage(title:String,back:()->Unit,content:@Composable ColumnScope.()->Unit){Surface(Modifier.fillMaxSize(),color=Cream){Column(Modifier.fillMaxSize().padding(horizontal=18.dp,vertical=16.dp)){Row(verticalAlignment=Alignment.CenterVertically){Text("‹",fontSize=34.sp,color=Teal,modifier=Modifier.clickable{back()});Spacer(Modifier.width(5.dp));Text(title,fontSize=23.sp,fontWeight=FontWeight.ExtraBold,color=Ink)};Spacer(Modifier.height(18.dp));Column(Modifier.fillMaxSize(),content=content)}}}
