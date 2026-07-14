package com.example

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.theme.MyApplicationTheme
import java.util.*

// --- Data Models ---
data class AppUser(
    val name: String,
    val email: String,
    val isTutor: Boolean,
    val level: String = "Beginner",
    val bio: String = "",
    val rate: String = ""
)

data class Review(
    val author: String,
    val rating: Int,
    val text: String,
    val date: String
)

data class Tutor(
    val id: String,
    val name: String,
    val imageRes: Int,
    val bio: String,
    val languages: List<String>,
    val hourlyRate: Double,
    val rating: Double,
    val availability: List<String>,
    val reviews: List<Review>
)

data class Booking(
    val tutorName: String,
    val date: String,
    val timeSlot: String,
    val amount: Double,
    val network: String,
    val phoneNumber: String
)

data class TranslatePhrase(
    val english: String,
    val swahili: String,
    val category: String,
    val pronunciationNote: String
)

data class ForeignStudent(
    val id: String,
    val name: String,
    val country: String,
    val imageRes: Int,
    val swahiliLevel: String,
    val payoutAmountTzs: Int,
    val bio: String,
    val interestTopic: String
)

class MainActivity : ComponentActivity() {
    private var textToSpeech: TextToSpeech? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize Text-To-Speech for Swahili learning helper
        textToSpeech = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech?.language = Locale("sw", "TZ")
            }
        }

        setContent {
            MyApplicationTheme {
                MainAppScreen(tts = textToSpeech)
            }
        }
    }

    override fun onDestroy() {
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        super.onDestroy()
    }
}

// --- Navigation Routes & State Hub ---
enum class Screen {
    WELCOME,
    DASHBOARD,
    TUTOR_DETAILS,
    BOOKING,
    CHAT_OPTIONS,
    WEBVIEW_CHAT,
    BOOKINGS_LIST
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun MainAppScreen(tts: TextToSpeech?) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current

    // App State
    var currentUser by remember { mutableStateOf<AppUser?>(null) }
    var currentScreen by remember { mutableStateOf(Screen.WELCOME) }
    var selectedTutor by remember { mutableStateOf<Tutor?>(null) }
    var bookings by remember { mutableStateOf<List<Booking>>(emptyList()) }
    var selectedSlotDate by remember { mutableStateOf("") }
    var selectedSlotTime by remember { mutableStateOf("") }

    // State for Wazungu Chat Funnel
    var isChatAccountCreated by remember { mutableStateOf(false) }
    var isChatAccountPaid by remember { mutableStateOf(false) }
    var chatMajina by remember { mutableStateOf("") }
    var chatEmail by remember { mutableStateOf("") }
    var chatUsername by remember { mutableStateOf("") }
    var chatNchi by remember { mutableStateOf("") }
    var chatPassword by remember { mutableStateOf("") }

    // Direct Web Chat Link from User instructions
    val mainChatUrl = "https://kozenachat.com/dist/#/v/eyJkIjoia296ZW5hc2l0ZS5jb20iLCJyIjoiSklTQUpJTEkiLCJuIjoiIn0"

    // Mock Tutors list using generated images
    val tutors = remember {
        listOf(
            Tutor(
                id = "tutor_1",
                name = "Baraka Kamau",
                imageRes = R.drawable.img_tutor_1_1784038485370,
                bio = "Habari! Mimi ni Baraka, mwalimu wako wa Kiswahili kutoka Dar es Salaam. I have over 5 years of experience teaching Swahili & Tanzanian culture to visitors and expats. Let's practice standard 'Sanifu' Swahili together!",
                languages = listOf("Kiswahili (Native)", "English (Fluent)", "Kizaramo"),
                hourlyRate = 12.00,
                rating = 4.9,
                availability = listOf("09:00 AM", "11:00 AM", "02:00 PM", "04:00 PM", "06:00 PM"),
                reviews = listOf(
                    Review("Sarah L.", 5, "Baraka is incredible! His lessons are fun and highly interactive.", "July 12, 2026"),
                    Review("Hans M.", 5, "Sana sana nzuri! I can now order food in local restaurants easily.", "July 08, 2026")
                )
            ),
            Tutor(
                id = "tutor_2",
                name = "Neema Mushi",
                imageRes = R.drawable.img_tutor_2_1784038498129,
                bio = "Karibu sana! I am Neema, raised in Arusha, right under Mount Meru. I specialize in conversational Swahili for tourists, Kilimanjaro climbers, and healthcare volunteers. Let's make learning memorable!",
                languages = listOf("Kiswahili (Native)", "English (Fluent)", "Kichagga"),
                hourlyRate = 15.00,
                rating = 5.0,
                availability = listOf("08:00 AM", "10:00 AM", "01:00 PM", "03:00 PM", "05:00 PM"),
                reviews = listOf(
                    Review("Michael K.", 5, "Neema's cultural tips saved my business trip. She is the best!", "July 10, 2026"),
                    Review("Emma W.", 5, "Perfect lessons before my Kilimanjaro climb. Asante sana!", "July 02, 2026")
                )
            ),
            Tutor(
                id = "tutor_3",
                name = "Hamisi Salim",
                imageRes = R.drawable.img_tutor_3_1784038509154,
                bio = "As-salamu alaykum! Learn Swahili with a coastal flavor from beautiful Zanzibar! I share Zanzibari history, Swahili idioms (Miseemo), and guide you to master the language smoothly.",
                languages = listOf("Kiswahili (Native)", "English (Fluent)", "Arabic (Conversational)"),
                hourlyRate = 10.00,
                rating = 4.8,
                availability = listOf("10:00 AM", "12:00 PM", "03:00 PM", "05:00 PM", "07:00 PM"),
                reviews = listOf(
                    Review("David P.", 4, "A great experience. Learned lots of Zanzibar cultural terms.", "July 11, 2026"),
                    Review("Fatima A.", 5, "Mwalimu mzuri sana. Explains difficult concepts patiently.", "June 28, 2026")
                )
            )
        )
    }

    val foreigners = remember {
        listOf(
            ForeignStudent(
                id = "foreigner_1",
                name = "Johnathan Smith",
                country = "Uingereza (UK) 🇬🇧",
                imageRes = R.drawable.img_foreigner_1_1784042022241,
                swahiliLevel = "Beginner (Kiwango cha Kwanza)",
                payoutAmountTzs = 45000,
                bio = "Habari! Mimi ni mwanafunzi kutoka London. Napenda utalii na wanyamapori wa Tanzania. Natafuta mwalimu wa kufanya mazungumzo rahisi ya kila siku.",
                interestTopic = "Serengeti & Wildlife Vocabulary"
            ),
            ForeignStudent(
                id = "foreigner_2",
                name = "Emily Rose Davis",
                country = "Marekani (USA) 🇺🇸",
                imageRes = R.drawable.img_foreigner_2_1784042038245,
                swahiliLevel = "Intermediate (Kiwango cha Kati)",
                payoutAmountTzs = 38000,
                bio = "Hi! Natafuta rafiki wa kuongea Kiswahili ili nikuze uwezo wangu wa kuongea. Nitajifunza maneno ya kitamaduni na chakula cha Kitanzania.",
                interestTopic = "Tanzanian Culture & Food"
            ),
            ForeignStudent(
                id = "foreigner_3",
                name = "Sarah Marie Jenkins",
                country = "Kanada (Canada) 🇨🇦",
                imageRes = R.drawable.img_foreigner_3_1784042052926,
                swahiliLevel = "Beginner (Kiwango cha Kwanza)",
                payoutAmountTzs = 30000,
                bio = "Nafanya biashara ya kahawa na wakulima wa Moshi na Arusha. Ninataka kujifunza Kiswahili cha biashara ili niongee nao vizuri.",
                interestTopic = "Business Swahili & Greetings"
            ),
            ForeignStudent(
                id = "foreigner_4",
                name = "Michael Vance Brown",
                country = "Australia 🇦🇺",
                imageRes = R.drawable.img_foreigner_4_1784042068969,
                swahiliLevel = "Advanced (Kiwango cha Juu)",
                payoutAmountTzs = 50000,
                bio = "Mambo! Mimi ni mtafiti wa lugha kutoka Sydney. Napenda sana fasihi, methali, na miseemo ya Kiswahili ya Zanzibar na Pwani.",
                interestTopic = "Zanzibari Idioms & Literature"
            ),
            ForeignStudent(
                id = "foreigner_5",
                name = "Thomas Müller",
                country = "Ujerumani (Germany) 🇩🇪",
                imageRes = R.drawable.img_foreigner_1_1784042022241,
                swahiliLevel = "Beginner (Kiwango cha Kwanza)",
                payoutAmountTzs = 32000,
                bio = "Karibu! Ninapanga kuja Tanzania kwa ajili ya kupanda mlima Kilimanjaro mwaka ujao. Nataka kujua mazungumzo ya msingi ya wasafiri.",
                interestTopic = "Kilimanjaro Hiking Terms"
            ),
            ForeignStudent(
                id = "foreigner_6",
                name = "Jessica Taylor",
                country = "Uingereza (UK) 🇬🇧",
                imageRes = R.drawable.img_foreigner_2_1784042038245,
                swahiliLevel = "Intermediate (Kiwango cha Kati)",
                payoutAmountTzs = 42000,
                bio = "Nitajitolea katika hospitali moja mkoani Arusha kuanzia mwezi ujao. Nahitaji kujifunza Kiswahili cha mawasiliano ya matibabu na wagonjwa.",
                interestTopic = "Medical & Patient Conversation"
            )
        )
    }

    // Swahili ↔ English quick-translate phrases database
    val translationHelperPhrases = remember {
        listOf(
            TranslatePhrase("How are you?", "Habari yako? / Mambo vipi?", "Greetings", "ha-BA-ri YA-ko / mambo vee-pee"),
            TranslatePhrase("I am fine, thanks.", "Salama, asante.", "Greetings", "sa-la-ma, a-san-te"),
            TranslatePhrase("Welcome!", "Karibu!", "Greetings", "ka-ree-boo"),
            TranslatePhrase("Welcome (plural)", "Karibuni!", "Greetings", "ka-ree-boo-nee"),
            TranslatePhrase("Thank you very much.", "Asante sana.", "Greetings", "a-san-te sa-na"),
            TranslatePhrase("How much is this?", "Hii ni bei gani?", "Shopping & Money", "hee nee bay gah-nee"),
            TranslatePhrase("Please reduce the price.", "Punguza bei kidogo tafadhali.", "Shopping & Money", "poon-goo-za bay kee-do-go ta-fa-dha-lee"),
            TranslatePhrase("Where is the market?", "Soko liko wapi?", "Travel & Places", "so-ko lee-ko wa-pee"),
            TranslatePhrase("I want to go to...", "Nataka kwenda...", "Travel & Places", "na-ta-ka kwen-da"),
            TranslatePhrase("Drive slowly please.", "Nenda polepole tafadhali.", "Travel & Places", "nen-da po-lay-po-lay ta-fa-dha-lee"),
            TranslatePhrase("Stop here.", "Shusha hapa.", "Travel & Places", "shoo-sha ha-pa"),
            TranslatePhrase("Yes / No", "Ndiyo / Hapana", "General Conversation", "ndee-yo / ha-pa-na"),
            TranslatePhrase("Excuse me", "Samahani", "General Conversation", "sa-ma-ha-nee")
        )
    }

    // Handle Back Press in App
    BackHandler(enabled = currentScreen != Screen.WELCOME) {
        when (currentScreen) {
            Screen.DASHBOARD -> {
                // If user backs out of dashboard, confirm or stay
            }
            Screen.TUTOR_DETAILS -> currentScreen = Screen.DASHBOARD
            Screen.BOOKING -> currentScreen = Screen.TUTOR_DETAILS
            Screen.CHAT_OPTIONS -> currentScreen = Screen.DASHBOARD
            Screen.WEBVIEW_CHAT -> currentScreen = Screen.CHAT_OPTIONS
            Screen.BOOKINGS_LIST -> currentScreen = Screen.DASHBOARD
            else -> currentScreen = Screen.DASHBOARD
        }
    }

    Scaffold(
        bottomBar = {
            if (currentUser != null && currentScreen != Screen.WEBVIEW_CHAT) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp,
                    modifier = Modifier.testTag("bottom_nav_bar")
                ) {
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                        label = { Text("Mwanzo", fontSize = 11.sp) },
                        selected = currentScreen == Screen.DASHBOARD || currentScreen == Screen.TUTOR_DETAILS || currentScreen == Screen.BOOKING,
                        onClick = { currentScreen = Screen.DASHBOARD },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Chat, contentDescription = "Chat") },
                        label = { Text("Chat & Tafsiri", fontSize = 11.sp) },
                        selected = currentScreen == Screen.CHAT_OPTIONS,
                        onClick = { currentScreen = Screen.CHAT_OPTIONS },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.DateRange, contentDescription = "Bookings") },
                        label = { Text("Ratiba Zangu", fontSize = 11.sp) },
                        selected = currentScreen == Screen.BOOKINGS_LIST,
                        onClick = { currentScreen = Screen.BOOKINGS_LIST },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = MaterialTheme.colorScheme.background
        ) {
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                },
                label = "ScreenTransition"
            ) { targetScreen ->
                when (targetScreen) {
                    Screen.WELCOME -> {
                        WelcomeScreen(
                            onSignUpCompleted = { user ->
                                currentUser = user
                                currentScreen = Screen.DASHBOARD
                            }
                        )
                    }

                    Screen.DASHBOARD -> {
                        DashboardScreen(
                            user = currentUser,
                            tutors = tutors,
                            onTutorSelected = { tutor ->
                                selectedTutor = tutor
                                currentScreen = Screen.TUTOR_DETAILS
                            },
                            onDirectChatClicked = {
                                currentScreen = Screen.CHAT_OPTIONS
                            }
                        )
                    }

                    Screen.TUTOR_DETAILS -> {
                        selectedTutor?.let { tutor ->
                            TutorDetailsScreen(
                                tutor = tutor,
                                onBackClick = { currentScreen = Screen.DASHBOARD },
                                onBookNowClick = { date, slot ->
                                    selectedSlotDate = date
                                    selectedSlotTime = slot
                                    currentScreen = Screen.BOOKING
                                }
                            )
                        } ?: run {
                            currentScreen = Screen.DASHBOARD
                        }
                    }

                    Screen.BOOKING -> {
                        val tutor = selectedTutor
                        if (tutor != null) {
                            BookingAndPaymentScreen(
                                tutor = tutor,
                                selectedDate = selectedSlotDate,
                                selectedTimeSlot = selectedSlotTime,
                                onBackClick = { currentScreen = Screen.TUTOR_DETAILS },
                                onPaymentSuccess = { booking ->
                                    bookings = bookings + booking
                                    Toast.makeText(context, "Somo limefanikiwa kulipiwa na kupangwa!", Toast.LENGTH_LONG).show()
                                    currentScreen = Screen.BOOKINGS_LIST
                                }
                            )
                        } else {
                            currentScreen = Screen.DASHBOARD
                        }
                    }

                    Screen.CHAT_OPTIONS -> {
                        ChatOptionsAndTranslateScreen(
                            tts = tts,
                            phrases = translationHelperPhrases,
                            foreigners = foreigners,
                            chatUrl = mainChatUrl,
                            onOpenWebView = { currentScreen = Screen.WEBVIEW_CHAT },
                            onOpenBrowser = {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(mainChatUrl))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Imeshindikana kufungua kivinjari", Toast.LENGTH_SHORT).show()
                                }
                            },
                            isChatAccountCreated = isChatAccountCreated,
                            onChatAccountCreatedChanged = { isChatAccountCreated = it },
                            isChatAccountPaid = isChatAccountPaid,
                            onChatAccountPaidChanged = { isChatAccountPaid = it },
                            chatMajina = chatMajina,
                            onChatMajinaChanged = { chatMajina = it },
                            chatEmail = chatEmail,
                            onChatEmailChanged = { chatEmail = it },
                            chatUsername = chatUsername,
                            onChatUsernameChanged = { chatUsername = it },
                            chatNchi = chatNchi,
                            onChatNchiChanged = { chatNchi = it },
                            chatPassword = chatPassword,
                            onChatPasswordChanged = { chatPassword = it }
                        )
                    }

                    Screen.WEBVIEW_CHAT -> {
                        WebViewChatScreen(
                            url = mainChatUrl,
                            onBackClick = { currentScreen = Screen.CHAT_OPTIONS }
                        )
                    }

                    Screen.BOOKINGS_LIST -> {
                        BookingsListScreen(
                            bookings = bookings,
                            onExploreTutors = { currentScreen = Screen.DASHBOARD }
                        )
                    }
                }
            }
        }
    }
}

// ==================== SCREEN COMPOSABLES ====================

@Composable
fun WelcomeScreen(onSignUpCompleted: (AppUser) -> Unit) {
    var isTutorMode by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var experienceOrLevel by remember { mutableStateOf("Kiwango cha Kwanza (Beginner)") }
    var hourlyRate by remember { mutableStateOf("12") }
    var bio by remember { mutableStateOf("") }

    val levels = listOf(
        "Kiwango cha Kwanza (Beginner)",
        "Kiwango cha Kati (Intermediate)",
        "Kiwango cha Juu (Advanced)"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                    )
                )
            )
            .padding(24.dp)
            .testTag("welcome_screen"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(24.dp))
            // Beautiful Tanzanian Cultural Header Icon/Emblem
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Chat,
                    contentDescription = "Chat Logo",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(54.dp)
                )
            }
        }

        item {
            Text(
                text = "CHATI KISWAHILI",
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 2.sp,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Ungana na walimu wazawa wa Tanzania kwa mafunzo na mazungumzo!",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))
        }

        item {
            // User type selector tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (!isTutorMode) MaterialTheme.colorScheme.primary else Color.Transparent)
                        .clickable { isTutorMode = false }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Mwanafunzi (Learner)",
                        color = if (!isTutorMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isTutorMode) MaterialTheme.colorScheme.primary else Color.Transparent)
                        .clickable { isTutorMode = true }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Mwalimu (Tutor)",
                        color = if (isTutorMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = if (!isTutorMode) "Sajili Mwanafunzi Mpya" else "Sajili Mwalimu Mpya",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.primary
                    )

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Jina kamili (Full Name)") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("signup_name_input"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Barua Pepe / WhatsApp") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("signup_email_input"),
                        singleLine = true
                    )

                    if (!isTutorMode) {
                        // Level Dropdown or Selection buttons for Learner
                        Text(
                            text = "Kiwango Chako Cha Kiswahili:",
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp
                        )
                        levels.forEach { level ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (experienceOrLevel == level) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent)
                                    .clickable { experienceOrLevel = level }
                                    .padding(vertical = 10.dp, horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = experienceOrLevel == level,
                                    onClick = { experienceOrLevel = level }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = level, fontSize = 14.sp)
                            }
                        }
                    } else {
                        // Hourly rate and bio fields for Tutor
                        OutlinedTextField(
                            value = hourlyRate,
                            onValueChange = { hourlyRate = it },
                            label = { Text("Gharama kwa saa ($ / USD)") },
                            leadingIcon = { Icon(Icons.Default.Star, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = bio,
                            onValueChange = { bio = it },
                            label = { Text("Maelezo yako (Bio & Experience)") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp),
                            maxLines = 4
                        )
                    }

                    Button(
                        onClick = {
                            if (name.isNotBlank() && email.isNotBlank()) {
                                onSignUpCompleted(
                                    AppUser(
                                        name = name,
                                        email = email,
                                        isTutor = isTutorMode,
                                        level = if (!isTutorMode) experienceOrLevel else "",
                                        bio = if (isTutorMode) bio else "",
                                        rate = if (isTutorMode) hourlyRate else ""
                                    )
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("signup_submit_button"),
                        enabled = name.isNotBlank() && email.isNotBlank()
                    ) {
                        Text(
                            text = "Jiunge Sasa (Join Now)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardScreen(
    user: AppUser?,
    tutors: List<Tutor>,
    onTutorSelected: (Tutor) -> Unit,
    onDirectChatClicked: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredTutors = tutors.filter {
        it.name.contains(searchQuery, ignoreCase = true) ||
                it.bio.contains(searchQuery, ignoreCase = true) ||
                it.languages.any { lang -> lang.contains(searchQuery, ignoreCase = true) }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .testTag("dashboard_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcoming & Tanzanian Cultural Hero Header
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(20.dp))
            ) {
                // Background image generated specifically to express Swahili and Tanzanian culture
                Image(
                    painter = painterResource(id = R.drawable.img_hero_banner_1784038469594),
                    contentDescription = "Tanzania Culture Banner",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                // Linear dark overlay for readability
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                            )
                        )
                )

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Habari, ${user?.name ?: "Mgeni"}!",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = if (user?.isTutor == true) "Karibu kusomesha na kubadilishana utamaduni" else "Tafuta walimu wazuri kutoka Tanzania",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 14.sp
                    )
                }
            }
        }

        // Direct Chat Feature call-to-action
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onDirectChatClicked() },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Chat,
                            contentDescription = "Direct Chat",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Mawasiliano na Chat Kuu",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Bofya hapa kuanza kuchati nao moja kwa moja kupitia Kozenachat link!",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Go",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Phrase of the day / Cultural Info Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "🇹🇿 Swahili Phrase of the Day",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = "\"Mtu ni watu.\"",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Translation: 'A person is people' (You are because of others - expressing the core concept of African community and hospitality/Undugu).",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }
        }

        // Search Section (for learners)
        if (user?.isTutor == false) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Waalimu Wetu Waliopo (Featured Tutors)",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("search_tutors_input"),
                        placeholder = { Text("Tafuta kwa jina, mji au lugha...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // Tutor List
            if (filteredTutors.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Hakuna mwalimu aliyepatikana kwa jina hilo.",
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                items(filteredTutors) { tutor ->
                    TutorItemCard(tutor = tutor, onClick = { onTutorSelected(tutor) })
                }
            }
        } else {
            // Tutor Specific Dashboard Info
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Dashibodi ya Mwalimu",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Divider(color = MaterialTheme.colorScheme.outlineVariant)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Gharama ya sasa", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                Text("$${user?.rate ?: "12.00"}/saa", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                            Column {
                                Text("Vipindi Vyako leo", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                Text("Hakuna bado", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                                .padding(12.dp)
                        ) {
                            Column {
                                Text(
                                    text = "Ushauri wa Kufundisha (Teaching Tips):",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Make sure to connect your learner with local daily cultural items. Start with standard greetings like 'Shikamoo' and 'Habari gani'.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TutorItemCard(tutor: Tutor, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("tutor_card_${tutor.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = tutor.imageRes),
                contentDescription = tutor.name,
                modifier = Modifier
                    .size(85.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = tutor.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Rating",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = tutor.rating.toString(),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = tutor.bio,
                    fontSize = 13.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Languages chip list
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        tutor.languages.take(2).forEach { lang ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = lang.split(" ").first(),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Text(
                        text = "$${tutor.hourlyRate}/hr",
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun TutorDetailsScreen(
    tutor: Tutor,
    onBackClick: () -> Unit,
    onBookNowClick: (String, String) -> Unit
) {
    var selectedDate by remember { mutableStateOf("July 15, 2026") }
    var selectedTimeSlot by remember { mutableStateOf("") }
    var ratingStars by remember { mutableStateOf(5) }
    var reviewTextInput by remember { mutableStateOf("") }
    var localReviews by remember { mutableStateOf(tutor.reviews) }

    val dateOptions = listOf("July 15, 2026", "July 16, 2026", "July 17, 2026", "July 18, 2026")

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .testTag("tutor_details_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier.background(MaterialTheme.colorScheme.surface, CircleShape)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
        }

        // Tutor Info Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = tutor.imageRes),
                    contentDescription = tutor.name,
                    modifier = Modifier
                        .size(110.dp)
                        .clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = tutor.name,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Rating",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${tutor.rating} (Kiwango cha Juu)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "$${tutor.hourlyRate} kwa saa",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Bio & Languages spoken
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Kuhusu Mimi (About Me)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = tutor.bio,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Lugha ninazozungumza:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        tutor.languages.forEach { lang ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = lang,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // Calendar Booking Slots
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "1. Chagua Tarehe (Select Date):",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        dateOptions.forEach { date ->
                            val isSelected = selectedDate == date
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .clickable { selectedDate = date }
                                    .padding(vertical = 12.dp, horizontal = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = date.replace(", 2026", ""),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "2. Chagua Muda (Select Time Slot):",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier.height(100.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(tutor.availability) { slot ->
                            val isSelected = selectedTimeSlot == slot
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { selectedTimeSlot = slot }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = slot,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { onBookNowClick(selectedDate, selectedTimeSlot) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("book_session_button"),
                        enabled = selectedTimeSlot.isNotEmpty()
                    ) {
                        Text(
                            text = "Ghairi na Weka Nafasi ($${tutor.hourlyRate})",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Reviews & Rating System
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Maoni ya Wanafunzi (${localReviews.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Write a Review input
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Andika Maoni yako (Leave a Review)", fontWeight = FontWeight.Bold, fontSize = 12.sp)

                        // Stars selector
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            (1..5).forEach { star ->
                                val isGold = star <= ratingStars
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = if (isGold) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outlineVariant,
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clickable { ratingStars = star }
                                )
                            }
                        }

                        OutlinedTextField(
                            value = reviewTextInput,
                            onValueChange = { reviewTextInput = it },
                            placeholder = { Text("Andika maoni hapa...", fontSize = 13.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 2,
                            singleLine = false
                        )

                        Button(
                            onClick = {
                                if (reviewTextInput.isNotBlank()) {
                                    val newReview = Review(
                                        author = "Wewe (You)",
                                        rating = ratingStars,
                                        text = reviewTextInput,
                                        date = "Today"
                                    )
                                    localReviews = listOf(newReview) + localReviews
                                    reviewTextInput = ""
                                    Toast.makeText(onBackClick().run { null }, "Asante kwa maoni!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.align(Alignment.End),
                            enabled = reviewTextInput.isNotBlank()
                        ) {
                            Text("Tuma", fontSize = 12.sp)
                        }
                    }

                    Divider(color = MaterialTheme.colorScheme.outlineVariant)

                    localReviews.forEach { review ->
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(review.author, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Row {
                                    (1..review.rating).forEach {
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                            }
                            Text(review.text, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
                            Text(review.date, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(4.dp))
                            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingAndPaymentScreen(
    tutor: Tutor,
    selectedDate: String,
    selectedTimeSlot: String,
    onBackClick: () -> Unit,
    onPaymentSuccess: (Booking) -> Unit
) {
    var selectedNetwork by remember { mutableStateOf("M-Pesa") }
    var phoneNumber by remember { mutableStateOf("") }
    var isPaying by remember { mutableStateOf(false) }

    val networks = listOf("M-Pesa", "Mixx by Yass", "Airtel Money", "Halopesa")

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .testTag("booking_payment_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier.background(MaterialTheme.colorScheme.surface, CircleShape)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
        }

        item {
            Text(
                text = "Malipo Placeholder & Thibitisha",
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Lipia kipindi chako kwa urahisi kupitia mitandao ya simu Tanzania.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
        }

        // Summary Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Muhtasari wa Somo (Lesson Summary)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Mwalimu:", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                        Text(tutor.name, fontWeight = FontWeight.Bold)
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Tarehe:", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                        Text(selectedDate, fontWeight = FontWeight.Bold)
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Muda:", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                        Text(selectedTimeSlot, fontWeight = FontWeight.Bold)
                    }

                    Divider(color = MaterialTheme.colorScheme.outlineVariant)

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Jumla ya Malipo (Total):", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("$${tutor.hourlyRate} (~${(tutor.hourlyRate * 2600).toInt()} TZS)", fontWeight = FontWeight.Black, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        // Network selection and phone number
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Lipia na Simu (Mobile Money Payment)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        networks.forEach { network ->
                            val isSelected = selectedNetwork == network
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { selectedNetwork = network }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = network,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = phoneNumber,
                        onValueChange = { phoneNumber = it },
                        label = { Text("Namba ya simu ya malipo (e.g., 07XXXXXXXX)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Button(
                        onClick = {
                            isPaying = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("submit_payment_button"),
                        enabled = phoneNumber.length >= 10 && !isPaying
                    ) {
                        if (isPaying) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
                        } else {
                            Text("Thibitisha Lipa Somo Sasa", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    if (isPaying) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Tuma Muamala wa Malipo") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    CircularProgressIndicator(modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Tafadhali kamilisha muamala wa TZS ${(tutor.hourlyRate * 2600).toInt()} kwenye simu yako ya $selectedNetwork. Ingiza PIN thibitisha...",
                        textAlign = TextAlign.Center,
                        fontSize = 14.sp
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        isPaying = false
                        onPaymentSuccess(
                            Booking(
                                tutorName = tutor.name,
                                date = selectedDate,
                                timeSlot = selectedTimeSlot,
                                amount = tutor.hourlyRate,
                                network = selectedNetwork,
                                phoneNumber = phoneNumber
                            )
                        )
                    }
                ) {
                    Text("Nimekamilisha Lipa / Umemaliza PIN")
                }
            }
        )
    }
}

@Composable
fun ChatOptionsAndTranslateScreen(
    tts: TextToSpeech?,
    phrases: List<TranslatePhrase>,
    foreigners: List<ForeignStudent>,
    chatUrl: String,
    onOpenWebView: () -> Unit,
    onOpenBrowser: () -> Unit,
    isChatAccountCreated: Boolean,
    onChatAccountCreatedChanged: (Boolean) -> Unit,
    isChatAccountPaid: Boolean,
    onChatAccountPaidChanged: (Boolean) -> Unit,
    chatMajina: String,
    onChatMajinaChanged: (String) -> Unit,
    chatEmail: String,
    onChatEmailChanged: (String) -> Unit,
    chatUsername: String,
    onChatUsernameChanged: (String) -> Unit,
    chatNchi: String,
    onChatNchiChanged: (String) -> Unit,
    chatPassword: String,
    onChatPasswordChanged: (String) -> Unit
) {
    var activeTab by remember { mutableStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    
    // Internal state to follow the funnel: "LIST", "REGISTER", "PAYMENT", "UNLOCKED"
    var subScreenState by remember {
        mutableStateOf(
            when {
                isChatAccountPaid -> "UNLOCKED"
                isChatAccountCreated -> "PAYMENT"
                else -> "LIST"
            }
        )
    }

    val categories = listOf("All", "Greetings", "Shopping & Money", "Travel & Places", "General Conversation")

    val filteredPhrases = phrases.filter {
        (selectedCategory == "All" || it.category == selectedCategory) &&
                (it.english.contains(searchQuery, ignoreCase = true) || it.swahili.contains(searchQuery, ignoreCase = true))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Tab Navigation Headers
        TabRow(
            selectedTabIndex = activeTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxWidth()
        ) {
            Tab(
                selected = activeTab == 0,
                onClick = { activeTab = 0 },
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text("Chat na Wazungu", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            )
            Tab(
                selected = activeTab == 1,
                onClick = { activeTab = 1 },
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text("Tafsiri & Msamiati", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            )
        }

        if (activeTab == 0) {
            // TAB 1: Chat na Wazungu Funnel
            when (subScreenState) {
                "LIST" -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                ),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text("FURSA MPYA", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black)
                                        }
                                        Text("Jipatie Kipato", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                                    }
                                    Text(
                                        text = "Chat na Wanafunzi wa Kigeni (Wazungu)",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = "Ongea na Wazungu wanaotafuta waalimu wa kufanya nao mazoezi ya Kiswahili ya kila siku. Dau zao ni kuanzia TZS 30,000 hadi TZS 50,000 kwa saa baada ya kumaliza kuchati nao!",
                                        fontSize = 12.sp,
                                        lineHeight = 16.sp,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }

                        item {
                            Text(
                                text = "Wanafunzi wa Kigeni Waliopo Mtandaoni",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        items(foreigners) { foreigner ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Image(
                                        painter = painterResource(id = foreigner.imageRes),
                                        contentDescription = foreigner.name,
                                        modifier = Modifier
                                            .size(72.dp)
                                            .clip(CircleShape)
                                            .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(foreigner.name, fontWeight = FontWeight.Black, fontSize = 16.sp)
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(foreigner.country, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            Box(
                                                modifier = Modifier
                                                    .background(Color(0xFF2E7D32).copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                                            ) {
                                                Text("Online", color = Color(0xFF2E7D32), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                        Text("Topic: ${foreigner.interestTopic}", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                                        Text(
                                            text = "Dau lake: TZS ${foreigner.payoutAmountTzs} / Saa",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF2E7D32),
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Button(
                                            onClick = {
                                                if (isChatAccountPaid) {
                                                    subScreenState = "UNLOCKED"
                                                } else if (isChatAccountCreated) {
                                                    subScreenState = "PAYMENT"
                                                } else {
                                                    subScreenState = "REGISTER"
                                                }
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                            modifier = Modifier.height(36.dp)
                                        ) {
                                            Text("Anza Kuchati Sasa", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                "REGISTER" -> {
                    var localMajina by remember { mutableStateOf(chatMajina) }
                    var localEmail by remember { mutableStateOf(chatEmail) }
                    var localUsername by remember { mutableStateOf(chatUsername) }
                    var localNchi by remember { mutableStateOf(chatNchi) }
                    var localPassword by remember { mutableStateOf(chatPassword) }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        item {
                            IconButton(
                                onClick = { subScreenState = "LIST" },
                                modifier = Modifier.background(MaterialTheme.colorScheme.surface, CircleShape)
                            ) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                            }
                        }

                        item {
                            Text(
                                text = "FUNGUA ACCOUNT",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Sajili akaunti yako ya mazungumzo ili uanze kuchati na wazungu na kulipwa.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                            )
                        }

                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    OutlinedTextField(
                                        value = localMajina,
                                        onValueChange = {
                                            localMajina = it
                                            onChatMajinaChanged(it)
                                        },
                                        label = { Text("Majina yako Kamili (Full Name)") },
                                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true
                                    )

                                    OutlinedTextField(
                                        value = localEmail,
                                        onValueChange = {
                                            localEmail = it
                                            onChatEmailChanged(it)
                                        },
                                        label = { Text("Barua Pepe (Email)") },
                                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true
                                    )

                                    OutlinedTextField(
                                        value = localUsername,
                                        onValueChange = {
                                            localUsername = it
                                            onChatUsernameChanged(it)
                                        },
                                        label = { Text("Jina la Mtumiaji (Username)") },
                                        leadingIcon = { Icon(Icons.Default.AccountCircle, contentDescription = null) },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true
                                    )

                                    OutlinedTextField(
                                        value = localNchi,
                                        onValueChange = {
                                            localNchi = it
                                            onChatNchiChanged(it)
                                        },
                                        label = { Text("Nchi (Country)") },
                                        leadingIcon = { Icon(Icons.Default.Place, contentDescription = null) },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true
                                    )

                                    OutlinedTextField(
                                        value = localPassword,
                                        onValueChange = {
                                            localPassword = it
                                            onChatPasswordChanged(it)
                                        },
                                        label = { Text("Nenosiri (Password)") },
                                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Button(
                                        onClick = {
                                            if (localMajina.isNotBlank() && localEmail.isNotBlank() && localUsername.isNotBlank() && localNchi.isNotBlank() && localPassword.isNotBlank()) {
                                                onChatAccountCreatedChanged(true)
                                                subScreenState = "PAYMENT"
                                            } else {
                                                // trigger alert/toast
                                            }
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text("SIGN UP", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                "PAYMENT" -> {
                    var selectedNetwork by remember { mutableStateOf("M-Pesa") }
                    var phoneNumber by remember { mutableStateOf("") }
                    var isProcessingPayment by remember { mutableStateOf(false) }
                    val context = LocalContext.current

                    val tanzanianNetworks = listOf("M-Pesa", "Mixx by Yass", "Airtel Money", "Halopesa")

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            IconButton(
                                onClick = { subScreenState = "REGISTER" },
                                modifier = Modifier.background(MaterialTheme.colorScheme.surface, CircleShape)
                            ) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                            }
                        }

                        item {
                            Text(
                                text = "SEHEM YA KULIPIA",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Kamilisha usajili kwa kulipia ada kidogo ya uanzishaji ili mfumo wetu uruhusu akaunti yako kufunguka na uanze kuchati.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                            )
                        }

                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text("MAELEKEZO YA LIPA NAMBA", fontWeight = FontWeight.Black, fontSize = 14.sp)
                                    Divider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f))
                                    Text(
                                        text = "Kiasi cha Malipo: TZS 14,500",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 18.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Lipa 14500 Kwa m pesa lipa Namba 354136248 Jina kozena site",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = "Unaweza pia kujaza namba zako hapa chini na mfumo utakata automatic kupitia mtandao wowote wa Tanzania uliouchagua.",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }

                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text("Chagua Mtandao wa Malipo", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        tanzanianNetworks.forEach { network ->
                                            val isSelected = selectedNetwork == network
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                                    .clickable { selectedNetwork = network }
                                                    .padding(vertical = 10.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = network,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }

                                    OutlinedTextField(
                                        value = phoneNumber,
                                        onValueChange = { phoneNumber = it },
                                        label = { Text("Jaza Namba Yako ya Kulipia (e.g., 07XXXXXXXX)") },
                                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true
                                    )

                                    Button(
                                        onClick = {
                                            isProcessingPayment = true
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(50.dp),
                                        enabled = phoneNumber.length >= 10 && !isProcessingPayment,
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        if (isProcessingPayment) {
                                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                                        } else {
                                            Text("LIPA TZS 14,500 SASA", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (isProcessingPayment) {
                        AlertDialog(
                            onDismissRequest = {},
                            title = { Text("Uhakiki wa Malipo") },
                            text = {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(48.dp))
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "Tafadhali kamilisha malipo kwenye simu yako kupitia $selectedNetwork.\nUmepelekewa ombi la malipo ya TZS 14,500.\nIngiza PIN yako kukamilisha...",
                                        textAlign = TextAlign.Center,
                                        fontSize = 14.sp
                                    )
                                }
                            },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        isProcessingPayment = false
                                        onChatAccountPaidChanged(true)
                                        subScreenState = "UNLOCKED"
                                        Toast.makeText(context, "Hongera! Akaunti yako imefunguliwa kikamilifu!", Toast.LENGTH_LONG).show()
                                    }
                                ) {
                                    Text("NIMEWEKA PIN / TAYARI")
                                }
                            }
                        )
                    }
                }

                "UNLOCKED" -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF2E7D32)),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Success",
                                        tint = Color.White,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Text(
                                        text = "Akaunti yako Imefunguka!",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Karibu, $chatMajina (@$chatUsername). Akaunti yako iko salama na imethibitishwa. Sasa unaweza kuanza kuchati na kufanya kazi na wanafunzi wote mtandaoni.",
                                        fontSize = 12.sp,
                                        textAlign = TextAlign.Center,
                                        color = Color.White.copy(alpha = 0.9f)
                                    )
                                }
                            }
                        }

                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(16.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        text = "FUNGUA CHATI KUU YAKO",
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "Ingia sasa kwenye mfumo wa live chati ili uwasiliane na kuanza kulipwa mshahara wako wa kuchati kuanzia TZS 30,000 hadi 50,000.",
                                        fontSize = 12.sp,
                                        color = Color.White.copy(alpha = 0.9f)
                                    )
                                    
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = onOpenWebView,
                                            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = MaterialTheme.colorScheme.primary),
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("Fungua Ndani ya App", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }

                                        OutlinedButton(
                                            onClick = onOpenBrowser,
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                            border = BorderStroke(1.dp, Color.White),
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("Fungua Browser", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            Text(
                                text = "Wanafunzi wa Chati Waliopo Online",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }

                        items(foreigners) { foreigner ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Image(
                                        painter = painterResource(id = foreigner.imageRes),
                                        contentDescription = foreigner.name,
                                        modifier = Modifier
                                            .size(60.dp)
                                            .clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(foreigner.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                        Text(foreigner.country, fontSize = 12.sp)
                                        Text("Topic: ${foreigner.interestTopic}", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                                        Text(
                                            text = "Malipo (Payout): TZS ${foreigner.payoutAmountTzs}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF2E7D32)
                                        )
                                    }
                                    IconButton(
                                        onClick = onOpenWebView,
                                        modifier = Modifier.background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                                    ) {
                                        Icon(Icons.Default.Send, contentDescription = "Chat", tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // TAB 2: Msaidizi wa Tafsiri (Swahili Translate Helper)
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text(
                        text = "Msaidizi wa Tafsiri (Swahili Translate Helper)",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Tafsiri maneno ya haraka ya Kiswahili na Kiingereza ili usaidike kwenye mazungumzo.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                    )
                }

                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Tafuta neno la kutafsiri...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            modifier = Modifier.height(100.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(categories) { category ->
                                val isSelected = selectedCategory == category
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                        .clickable { selectedCategory = category }
                                        .padding(vertical = 10.dp, horizontal = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = category,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }

                items(filteredPhrases) { phrase ->
                    val context = LocalContext.current
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(phrase.english, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                Text(phrase.swahili, fontSize = 17.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("Pronounced: [ ${phrase.pronunciationNote} ]", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                            }

                            IconButton(
                                onClick = {
                                    if (tts != null) {
                                        val params = Bundle()
                                        tts.speak(phrase.swahili, TextToSpeech.QUEUE_FLUSH, params, "SwahiliID")
                                    } else {
                                        Toast.makeText(context, "Text-to-Speech system isn't ready", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                                    .size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Speak",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebViewChatScreen(url: String, onBackClick: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chat ya Kozenachat", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary, titleContentColor = Color.White, navigationIconContentColor = Color.White)
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .testTag("webview_chat_screen")
        ) {
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                                return false
                            }
                        }
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            useWideViewPort = true
                            loadWithOverviewMode = true
                            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        }
                        loadUrl(url)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
fun BookingsListScreen(bookings: List<Booking>, onExploreTutors: () -> Unit) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .testTag("bookings_list_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Vipindi Vyako (My Schedule)",
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Ratiba ya masomo uliyolipia na kuweka nafasi na walimu wako.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
        }

        if (bookings.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outlineVariant
                        )
                        Text(
                            text = "Huna vipindi vyovyote vilivyopangwa bado.",
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Chagua mwalimu kwenye orodha yetu ili uanze kujifunza Kiswahili leo!",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center
                        )

                        Button(onClick = onExploreTutors) {
                            Text("Chagua Mwalimu Sasa")
                        }
                    }
                }
            }
        } else {
            items(bookings) { booking ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(booking.tutorName, fontWeight = FontWeight.Black, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF2E7D32))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("Imelipiwa", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Divider(color = MaterialTheme.colorScheme.outlineVariant)

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Tarehe:", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            }
                            Text(booking.date, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Muda wa somo:", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            }
                            Text(booking.timeSlot, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Muamala:", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            }
                            Text("${booking.network} (${booking.phoneNumber})", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Text(
                                text = "Unganisho ya somo na kiungo vitatumwa kwa namba yako ya WhatsApp kabla ya kipindi kuanza.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        }
    }
}
