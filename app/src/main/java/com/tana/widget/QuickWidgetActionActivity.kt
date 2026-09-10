package com.tana.widget

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.tana.data.database.AppDatabase
import com.tana.data.model.SavingsGoalEntity
import com.tana.data.repository.FinanceRepository
import com.tana.ui.components.ThousandsSeparatorVisualTransformation
import com.tana.ui.theme.TanaTheme
import com.tana.ui.viewmodel.FinanceViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class QuickWidgetActionActivity : ComponentActivity() {

    private lateinit var repository: FinanceRepository

    private var isDepositState by mutableStateOf(true)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val db = AppDatabase.getInstance(applicationContext)
        repository = FinanceRepository(db)

        val initialAction = intent.action ?: "ACTION_DEPOSIT"
        isDepositState = initialAction != "ACTION_WITHDRAW"

        setContent {
            TanaTheme {
                QuickWidgetFullScreen(
                    initialIsDeposit = isDepositState,
                    onBack = { finishAndRemoveTask() },
                    onExecute = { goalId, amount, isDeposit, note ->
                        executeAction(goalId, amount, isDeposit, note)
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val action = intent.action ?: "ACTION_DEPOSIT"
        isDepositState = action != "ACTION_WITHDRAW"
    }

    private fun executeAction(goalId: Long, amount: Double, isDeposit: Boolean, note: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val result = if (isDeposit) {
                    repository.depositToGoal(goalId, amount, note.ifBlank { "Setor via Widget" })
                } else {
                    repository.withdrawFromGoal(goalId, amount, note.ifBlank { "Tarik via Widget" })
                }

                when (result) {
                    is com.tana.data.repository.SavingsOpResult.Success -> {
                        WidgetUpdateHelper.updateAllWidgets(applicationContext)
                        withContext(Dispatchers.Main) {
                            val actionName = if (isDeposit) "Setoran" else "Penarikan"
                            val formatted = FinanceViewModel.formatRupiah(result.actualAmount)
                            Toast.makeText(applicationContext, "✓ $actionName $formatted berhasil dicatat!", Toast.LENGTH_SHORT).show()
                            finishAndRemoveTask()
                        }
                    }
                    is com.tana.data.repository.SavingsOpResult.InsufficientBalance -> {
                        withContext(Dispatchers.Main) {
                            val avail = FinanceViewModel.formatRupiah(result.available)
                            Toast.makeText(applicationContext, "Gagal: Saldo tidak cukup (tersedia $avail)", Toast.LENGTH_LONG).show()
                        }
                    }
                    is com.tana.data.repository.SavingsOpResult.GoalNotFound -> {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(applicationContext, "Gagal: Target tabungan tidak ditemukan", Toast.LENGTH_LONG).show()
                        }
                    }
                    is com.tana.data.repository.SavingsOpResult.InvalidAmount -> {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(applicationContext, "Gagal: ${result.reason}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(applicationContext, "Gagal: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickWidgetFullScreen(
    initialIsDeposit: Boolean,
    onBack: () -> Unit,
    onExecute: (goalId: Long, amount: Double, isDeposit: Boolean, note: String) -> Unit
) {
    BackHandler(enabled = true) {
        onBack()
    }
    val context = LocalContext.current
    var isDeposit by remember { mutableStateOf(initialIsDeposit) }
    var savingsGoals by remember { mutableStateOf<List<SavingsGoalEntity>>(emptyList()) }
    var selectedGoal by remember { mutableStateOf<SavingsGoalEntity?>(null) }
    var isDropdownOpen by remember { mutableStateOf(false) }

    var amountText by remember { mutableStateOf("50000") }
    var noteText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }

    val scrollState = rememberScrollState()

    // Fetch goals directly from Room DB
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val db = AppDatabase.getInstance(context)
            val goals = db.savingsGoalDao().getAllGoalsDirect()
            withContext(Dispatchers.Main) {
                savingsGoals = goals
                selectedGoal = goals.firstOrNull { !it.isCompleted } ?: goals.firstOrNull()
                isLoading = false
            }
        }
    }

    Scaffold(
        containerColor = Color(0xFF0A0918),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (isDeposit) "Setor Tabungan Cepat" else "Tarik Tabungan Cepat",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            ),
                            color = Color.White
                        )
                        Text(
                            text = "Aksi Pintar Langsung dari Widget",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = Color(0xFF9C9587)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF0F241F))
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                actions = {
                    // Mode Pill indicator
                    Surface(
                        modifier = Modifier.padding(end = 16.dp),
                        shape = RoundedCornerShape(20.dp),
                        color = if (isDeposit) Color(0x2594722F) else Color(0x25FB7185),
                        border = BorderStroke(1.dp, if (isDeposit) Color(0xFF94722F) else Color(0xFFFB7185))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(if (isDeposit) Color(0xFF94722F) else Color(0xFFFB7185))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isDeposit) "SETOR" else "TARIK",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    fontSize = 10.sp,
                                    letterSpacing = 1.sp
                                ),
                                color = if (isDeposit) Color(0xFFC6A15B) else Color(0xFFFDA4AF)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0A0918)
                )
            )
        },
        bottomBar = {
            val parsedAmount = amountText.toDoubleOrNull() ?: 0.0
            val curGoal = selectedGoal

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding(),
                color = Color(0xFF0D0B1F),
                border = BorderStroke(1.dp, Color(0xFF262445))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                ) {
                    Button(
                        onClick = {
                            when {
                                curGoal == null -> {
                                    Toast.makeText(context, "Pilih target tabungan terlebih dahulu!", Toast.LENGTH_SHORT).show()
                                }
                                parsedAmount <= 0 -> {
                                    Toast.makeText(context, "Nominal harus lebih besar dari 0!", Toast.LENGTH_SHORT).show()
                                }
                                else -> {
                                    onExecute(curGoal.id, parsedAmount, isDeposit, noteText)
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .shadow(
                                elevation = 12.dp,
                                shape = RoundedCornerShape(16.dp),
                                spotColor = if (isDeposit) Color(0xFF94722F) else Color(0xFFFB7185)
                            ),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDeposit) Color(0xFF94722F) else Color(0xFFFB7185),
                            disabledContainerColor = Color(0xFF29264A)
                        ),
                        enabled = curGoal != null && parsedAmount > 0
                    ) {
                        Icon(
                            imageVector = if (isDeposit) Icons.Filled.Add else Icons.Filled.Remove,
                            contentDescription = null,
                            tint = if (isDeposit) Color(0xFF022312) else Color(0xFF3B0311),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isDeposit) {
                                "KONFIRMASI SETOR (${FinanceViewModel.formatCompact(parsedAmount)})"
                            } else {
                                "KONFIRMASI TARIK (${FinanceViewModel.formatCompact(parsedAmount)})"
                            },
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp,
                                letterSpacing = 0.5.sp
                            ),
                            color = if (isDeposit) Color(0xFF022312) else Color(0xFF3B0311)
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // Tactile Segmented Switch
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF0D1C18))
                    .border(1.dp, Color(0xFF1D3B34), RoundedCornerShape(14.dp))
                    .padding(4.dp)
            ) {
                // Setor Option
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (isDeposit) Brush.horizontalGradient(
                                listOf(Color(0xFF94722F), Color(0xFF8A6C38))
                            ) else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                        )
                        .clickable { isDeposit = true },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.TrendingUp,
                            contentDescription = null,
                            tint = if (isDeposit) Color(0xFF022312) else Color(0xFFA7C8BF),
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "SETOR (+)",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp
                            ),
                            color = if (isDeposit) Color(0xFF022312) else Color(0xFFA7C8BF)
                        )
                    }
                }

                // Tarik Option
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (!isDeposit) Brush.horizontalGradient(
                                listOf(Color(0xFFFB7185), Color(0xFFE11D48))
                            ) else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                        )
                        .clickable { isDeposit = false },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.TrendingDown,
                            contentDescription = null,
                            tint = if (!isDeposit) Color(0xFF380312) else Color(0xFFA7C8BF),
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "TARIK (-)",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp
                            ),
                            color = if (!isDeposit) Color(0xFF380312) else Color(0xFFA7C8BF)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Section 1: Target Tabungan Tujuan
            Text(
                text = "PILIH TARGET TABUNGAN",
                style = MaterialTheme.typography.labelSmall.copy(
                    letterSpacing = 1.4.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 11.sp
                ),
                color = Color(0xFFA7C8BF)
            )
            Spacer(modifier = Modifier.height(8.dp))

            val curGoal = selectedGoal
            Box(modifier = Modifier.fillMaxWidth()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { isDropdownOpen = true },
                    color = Color(0xFF0D1D19),
                    border = BorderStroke(1.2.dp, Color(0xFF224A40))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF14382F)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Flag,
                                    contentDescription = null,
                                    tint = Color(0xFFC6A15B),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = curGoal?.title ?: if (isLoading) "Memuat target..." else "Belum ada target tabungan",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White,
                                    maxLines = 1
                                )
                                if (curGoal != null) {
                                    Text(
                                        text = "Saldo: ${FinanceViewModel.formatRupiah(curGoal.currentAmount)} / ${FinanceViewModel.formatCompact(curGoal.targetAmount)}",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                        color = Color(0xFFA7C8BF)
                                    )
                                }
                            }
                        }

                        Icon(
                            imageVector = Icons.Filled.ArrowDropDown,
                            contentDescription = "Pilih Target",
                            tint = Color(0xFFA7C8BF)
                        )
                    }
                }

                // Dropdown Options
                DropdownMenu(
                    expanded = isDropdownOpen,
                    onDismissRequest = { isDropdownOpen = false },
                    modifier = Modifier
                        .background(Color(0xFF0B1714))
                        .border(1.dp, Color(0xFF1E3D35), RoundedCornerShape(16.dp))
                ) {
                    if (savingsGoals.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text("Tidak ada target tabungan aktif", color = Color.Gray, fontSize = 13.sp) },
                            onClick = { isDropdownOpen = false }
                        )
                    } else {
                        savingsGoals.forEach { goal ->
                            val isChosen = curGoal?.id == goal.id
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                            Text(
                                                text = goal.title,
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = if (isChosen) FontWeight.Bold else FontWeight.Normal
                                                ),
                                                color = if (isChosen) Color(0xFFC6A15B) else Color.White
                                            )
                                            Text(
                                                text = "Saldo: ${FinanceViewModel.formatRupiah(goal.currentAmount)}",
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                                color = Color(0xFFA7C8BF)
                                            )
                                        }
                                        if (isChosen) {
                                            Icon(
                                                imageVector = Icons.Filled.Check,
                                                contentDescription = null,
                                                tint = Color(0xFFC6A15B),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    selectedGoal = goal
                                    isDropdownOpen = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // Section 2: Big Amount Input Box with Auto Thousand Separator (.)
            Text(
                text = "NOMINAL TRANSAKSI (RP)",
                style = MaterialTheme.typography.labelSmall.copy(
                    letterSpacing = 1.4.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 11.sp
                ),
                color = Color(0xFFA7C8BF)
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = amountText,
                onValueChange = { input ->
                    val filtered = input.filter { it.isDigit() }
                    amountText = filtered
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                visualTransformation = ThousandsSeparatorVisualTransformation(),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF0D1D19),
                    unfocusedContainerColor = Color(0xFF0D1D19),
                    focusedBorderColor = if (isDeposit) Color(0xFF94722F) else Color(0xFFFB7185),
                    unfocusedBorderColor = Color(0xFF224A40),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                textStyle = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 22.sp
                ),
                prefix = {
                    Text(
                        text = "Rp ",
                        color = if (isDeposit) Color(0xFFC6A15B) else Color(0xFFFDA4AF),
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp
                    )
                },
                trailingIcon = {
                    if (amountText.isNotEmpty()) {
                        IconButton(onClick = { amountText = "" }) {
                            Icon(
                                imageVector = Icons.Filled.Clear,
                                contentDescription = "Hapus",
                                tint = Color(0xFF71695A),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Quick Nominal Pills Grid
            Text(
                text = "PILIHAN CEPAT NOMINAL",
                style = MaterialTheme.typography.labelSmall.copy(
                    letterSpacing = 1.2.sp,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                ),
                color = Color(0xFF71695A)
            )
            Spacer(modifier = Modifier.height(6.dp))

            val presetsRow1 = listOf(
                10_000 to "+10.000",
                20_000 to "+20.000",
                50_000 to "+50.000"
            )
            val presetsRow2 = listOf(
                100_000 to "+100.000",
                500_000 to "+500.000",
                1_000_000 to "+1.000.000"
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                presetsRow1.forEach { (value, label) ->
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                val current = amountText.toDoubleOrNull() ?: 0.0
                                amountText = (current + value).toLong().toString()
                            },
                        color = Color(0xFF102620),
                        border = BorderStroke(1.dp, Color(0xFF204D40))
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            ),
                            color = if (isDeposit) Color(0xFFC6A15B) else Color(0xFFFDA4AF),
                            modifier = Modifier
                                .padding(vertical = 10.dp)
                                .wrapContentWidth(Alignment.CenterHorizontally)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                presetsRow2.forEach { (value, label) ->
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                val current = amountText.toDoubleOrNull() ?: 0.0
                                amountText = (current + value).toLong().toString()
                            },
                        color = Color(0xFF102620),
                        border = BorderStroke(1.dp, Color(0xFF204D40))
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            ),
                            color = if (isDeposit) Color(0xFFC6A15B) else Color(0xFFFDA4AF),
                            modifier = Modifier
                                .padding(vertical = 10.dp)
                                .wrapContentWidth(Alignment.CenterHorizontally)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Section 3: Catatan Transaksi (Opsional)
            Text(
                text = "CATATAN TRANSAKSI (OPSIONAL)",
                style = MaterialTheme.typography.labelSmall.copy(
                    letterSpacing = 1.4.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 11.sp
                ),
                color = Color(0xFFA7C8BF)
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = noteText,
                onValueChange = { noteText = it },
                placeholder = { Text("Contoh: Uang kembalian, bonus proyek, dll...", color = Color(0xFF71695A), fontSize = 13.sp) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF0D1D19),
                    unfocusedContainerColor = Color(0xFF0D1D19),
                    focusedBorderColor = Color(0xFFC6A15B),
                    unfocusedBorderColor = Color(0xFF224A40),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}
