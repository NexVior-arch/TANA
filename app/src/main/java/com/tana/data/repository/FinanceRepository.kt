package com.tana.data.repository

import com.tana.data.api.OpenRouterChatRequest
import com.tana.data.api.OpenRouterClient
import com.tana.data.api.OpenRouterMessage
import com.tana.data.api.OpenRouterStreamChunk
import com.tana.data.database.AppDatabase
import com.tana.data.model.AgentRoutineEntity
import com.tana.data.model.AgentRoutineType
import com.tana.data.model.AiMessageEntity
import com.tana.data.model.SavingsGoalEntity
import com.tana.data.model.TransactionEntity
import com.tana.data.model.TransactionType
import com.tana.data.model.UserPreferenceEntity
import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.util.Locale

/**
 * Result of a deposit/withdraw operation so the caller (ViewModel/UI) always
 * knows exactly what happened - never silently clamped or ignored.
 */
sealed class SavingsOpResult {
    data class Success(val actualAmount: Double, val newBalance: Double) : SavingsOpResult()
    data class InsufficientBalance(val available: Double, val requested: Double) : SavingsOpResult()
    object GoalNotFound : SavingsOpResult()
    data class InvalidAmount(val reason: String) : SavingsOpResult()
}

class FinanceRepository(private val database: AppDatabase) {

    private val transactionDao = database.transactionDao()
    private val savingsGoalDao = database.savingsGoalDao()
    private val userPrefDao = database.userPreferenceDao()
    private val aiMessageDao = database.aiMessageDao()
    private val agentRoutineDao = database.agentRoutineDao()

    // --- Agent Routines ---
    val allAgentRoutines: Flow<List<AgentRoutineEntity>> = agentRoutineDao.getAllRoutines()
    val activeAgentRoutines: Flow<List<AgentRoutineEntity>> = agentRoutineDao.getActiveRoutines()

    suspend fun insertAgentRoutine(routine: AgentRoutineEntity): Long =
        agentRoutineDao.insertRoutine(routine)

    suspend fun updateAgentRoutine(routine: AgentRoutineEntity) =
        agentRoutineDao.updateRoutine(routine)

    suspend fun deleteAgentRoutine(id: Long) =
        agentRoutineDao.deleteById(id)

    suspend fun setAgentRoutineEnabled(id: Long, enabled: Boolean) =
        agentRoutineDao.setEnabled(id, enabled)

    suspend fun setAgentRoutineApproved(id: Long, approved: Boolean) =
        agentRoutineDao.setApproved(id, approved)

    suspend fun disableRoutinesByQuery(query: String) =
        agentRoutineDao.disableRoutinesByQuery(query)

    // --- Transactions ---
    val allTransactions: Flow<List<TransactionEntity>> = transactionDao.getAllTransactions()

    fun getRecentTransactions(limit: Int = 10): Flow<List<TransactionEntity>> =
        transactionDao.getRecentTransactions(limit)

    fun getTransactionsInRange(startMillis: Long, endMillis: Long): Flow<List<TransactionEntity>> =
        transactionDao.getTransactionsInRange(startMillis, endMillis)

    suspend fun insertTransaction(transaction: TransactionEntity): Long =
        transactionDao.insertTransaction(transaction)

    suspend fun insertAllTransactions(transactions: List<TransactionEntity>) =
        transactionDao.insertAll(transactions)

    suspend fun deleteTransaction(id: Long) {
        database.withTransaction {
            val tx = transactionDao.getByIdSync(id) ?: return@withTransaction
            if (tx.goalId != null) {
                val goal = savingsGoalDao.getGoalById(tx.goalId)
                if (goal != null) {
                    val updatedAmount = (goal.currentAmount - tx.amount).coerceAtLeast(0.0)
                    val isCompleted = updatedAmount >= goal.targetAmount && goal.targetAmount > 0
                    savingsGoalDao.updateGoal(goal.copy(currentAmount = updatedAmount, isCompleted = isCompleted))
                }
            }
            transactionDao.deleteById(id)
        }
    }

    suspend fun clearTransactions() =
        transactionDao.clearAll()

    suspend fun clearAllData() {
        database.withTransaction {
            transactionDao.clearAll()
            savingsGoalDao.clearAll()
            aiMessageDao.clearHistory()
        }
    }

    // --- Savings Goals ---
    val allSavingsGoals: Flow<List<SavingsGoalEntity>> = savingsGoalDao.getAllGoals()
    val activeSavingsGoals: Flow<List<SavingsGoalEntity>> = savingsGoalDao.getActiveGoals()

    suspend fun getGoalById(id: Long): SavingsGoalEntity? = savingsGoalDao.getGoalById(id)

    suspend fun findGoalByName(nameQuery: String): SavingsGoalEntity? {
        val goals = allSavingsGoals.firstOrNull() ?: emptyList()
        val trimmed = nameQuery.trim().lowercase()
        return goals.firstOrNull { it.title.trim().lowercase() == trimmed }
            ?: goals.firstOrNull { it.title.lowercase().contains(trimmed) || trimmed.contains(it.title.lowercase()) }
            ?: goals.firstOrNull { !it.isCompleted }
    }

    suspend fun insertSavingsGoal(goal: SavingsGoalEntity): Long =
        savingsGoalDao.insertGoal(goal)

    suspend fun insertAllGoals(goals: List<SavingsGoalEntity>) =
        savingsGoalDao.insertAll(goals)

    suspend fun updateSavingsGoal(goal: SavingsGoalEntity) =
        savingsGoalDao.updateGoal(goal)

    /**
     * Updates goal metadata (title, target, plan, note, etc.) WITHOUT ever touching
     * currentAmount. currentAmount must only ever change via depositToGoal /
     * withdrawFromGoal / updateSavingsTransaction / deleteSavingsTransaction, so that
     * a concurrent auto-deposit (AI agent) can never be silently overwritten by
     * someone editing the goal's title/target at the same time.
     */
    suspend fun updateSavingsGoalMetadata(
        goalId: Long,
        title: String,
        targetAmount: Double,
        fillPlanAmount: Double,
        fillPlanFrequency: String,
        note: String
    ): SavingsOpResult {
        return database.withTransaction {
            val current = savingsGoalDao.getGoalById(goalId) ?: return@withTransaction SavingsOpResult.GoalNotFound
            val isCompleted = current.currentAmount >= targetAmount && targetAmount > 0
            savingsGoalDao.updateGoal(
                current.copy(
                    title = title,
                    targetAmount = targetAmount,
                    fillPlanAmount = fillPlanAmount,
                    fillPlanFrequency = fillPlanFrequency,
                    note = note,
                    isCompleted = isCompleted
                )
            )
            SavingsOpResult.Success(0.0, current.currentAmount)
        }
    }

    suspend fun deleteSavingsGoal(id: Long) {
        database.withTransaction {
            savingsGoalDao.deleteById(id)
            transactionDao.deleteByGoalId(id)
        }
    }

    /**
     * Deposits [depositAmount] into [goalId] and records the matching transaction
     * atomically. Runs inside a single DB transaction so a crash or a concurrent
     * write (e.g. the autonomous AI agent depositing at the same time) can never
     * leave the goal balance and transaction history out of sync.
     */
    suspend fun depositToGoal(goalId: Long, depositAmount: Double, note: String = ""): SavingsOpResult {
        if (depositAmount <= 0.0) return SavingsOpResult.InvalidAmount("Nominal setoran harus lebih besar dari 0")

        return database.withTransaction {
            val goal = savingsGoalDao.getGoalById(goalId) ?: return@withTransaction SavingsOpResult.GoalNotFound
            val updatedAmount = goal.currentAmount + depositAmount
            val isCompleted = goal.targetAmount > 0 && updatedAmount >= goal.targetAmount
            savingsGoalDao.updateGoal(goal.copy(currentAmount = updatedAmount, isCompleted = isCompleted))

            transactionDao.insertTransaction(
                TransactionEntity(
                    type = TransactionType.SAVINGS,
                    amount = depositAmount,
                    category = if (note.isNotBlank()) note.trim() else "Tabungan: ${goal.title}",
                    note = note.trim(),
                    timestamp = System.currentTimeMillis(),
                    goalId = goalId
                )
            )
            SavingsOpResult.Success(actualAmount = depositAmount, newBalance = updatedAmount)
        }
    }

    /**
     * Withdraws [withdrawAmount] from [goalId]. This is a savings TARGET tracker,
     * not a bank ATM - the user is free to record a withdrawal larger than the
     * currently tracked balance (e.g. correcting a past entry, or the real-world
     * account already had other funds). The resulting balance can go negative;
     * it is still recorded atomically with its transaction so history stays in sync.
     */
    suspend fun withdrawFromGoal(goalId: Long, withdrawAmount: Double, note: String = ""): SavingsOpResult {
        if (withdrawAmount <= 0.0) return SavingsOpResult.InvalidAmount("Nominal penarikan harus lebih besar dari 0")

        return database.withTransaction {
            val goal = savingsGoalDao.getGoalById(goalId) ?: return@withTransaction SavingsOpResult.GoalNotFound
            val updatedAmount = goal.currentAmount - withdrawAmount
            val isCompleted = updatedAmount >= goal.targetAmount && goal.targetAmount > 0
            savingsGoalDao.updateGoal(goal.copy(currentAmount = updatedAmount, isCompleted = isCompleted))

            transactionDao.insertTransaction(
                TransactionEntity(
                    type = TransactionType.SAVINGS,
                    amount = -withdrawAmount,
                    category = if (note.isNotBlank()) note.trim() else "Tarik: ${goal.title}",
                    note = note.trim(),
                    timestamp = System.currentTimeMillis(),
                    goalId = goalId
                )
            )
            SavingsOpResult.Success(actualAmount = withdrawAmount, newBalance = updatedAmount)
        }
    }

    /**
     * Edits an existing savings transaction's amount/note/timestamp and re-derives
     * the goal's currentAmount from the delta, atomically. The resulting balance
     * can go negative (this is a savings target tracker, not a bank ATM) - no
     * popup or rejection, consistent with withdrawFromGoal.
     */
    suspend fun updateSavingsTransaction(
        transaction: TransactionEntity,
        newAmount: Double,
        newNote: String,
        newTimestamp: Long
    ): SavingsOpResult {
        return database.withTransaction {
            val goalId = transaction.goalId
            if (goalId == null) {
                transactionDao.updateTransaction(transaction.copy(amount = newAmount, note = newNote, timestamp = newTimestamp))
                return@withTransaction SavingsOpResult.Success(newAmount, 0.0)
            }

            val goal = savingsGoalDao.getGoalById(goalId) ?: return@withTransaction SavingsOpResult.GoalNotFound
            val oldAmount = transaction.amount
            val diff = newAmount - oldAmount
            val newCurrent = goal.currentAmount + diff

            transactionDao.updateTransaction(transaction.copy(amount = newAmount, note = newNote, timestamp = newTimestamp))
            val isCompleted = newCurrent >= goal.targetAmount && goal.targetAmount > 0
            savingsGoalDao.updateGoal(goal.copy(currentAmount = newCurrent, isCompleted = isCompleted))
            SavingsOpResult.Success(actualAmount = newAmount, newBalance = newCurrent)
        }
    }

    suspend fun deleteSavingsTransaction(transactionId: Long, goalId: Long?): SavingsOpResult {
        return database.withTransaction {
            val tx = transactionDao.getByIdSync(transactionId) ?: return@withTransaction SavingsOpResult.GoalNotFound
            if (goalId != null) {
                val goal = savingsGoalDao.getGoalById(goalId)
                if (goal != null) {
                    val newCurrent = (goal.currentAmount - tx.amount).coerceAtLeast(0.0)
                    val isCompleted = newCurrent >= goal.targetAmount && goal.targetAmount > 0
                    savingsGoalDao.updateGoal(goal.copy(currentAmount = newCurrent, isCompleted = isCompleted))
                }
            }
            transactionDao.deleteById(transactionId)
            SavingsOpResult.Success(0.0, 0.0)
        }
    }

    // --- Preferences ---
    fun getPreference(key: String): Flow<String?> = userPrefDao.getPreference(key)
    suspend fun getPreferenceDirect(key: String): String? = userPrefDao.getPreferenceDirect(key)
    suspend fun setPreference(key: String, value: String) {
        userPrefDao.setPreference(UserPreferenceEntity(key = key, value = value))
    }

    // --- AI Messages ---
    val aiMessages: Flow<List<AiMessageEntity>> = aiMessageDao.getAllMessages()

    suspend fun insertAiMessage(role: String, content: String): Long {
        return aiMessageDao.insertMessage(
            AiMessageEntity(
                role = role,
                content = content,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    suspend fun updateAiMessageContent(id: Long, content: String) {
        aiMessageDao.updateMessageContent(id, content)
    }

    suspend fun clearAiHistory() = aiMessageDao.clearHistory()

    // --- Financial Summary Data Context for OpenRouter AI ---
    suspend fun generateFinancialContextSummary(): String {
        val transactions = allTransactions.firstOrNull() ?: emptyList()
        val goals = allSavingsGoals.firstOrNull() ?: emptyList()

        val totalIncome = transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val totalExpense = transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val totalSavings = if (goals.isNotEmpty()) {
            val goalSavings = goals.sumOf { it.currentAmount }
            val orphanSavings = transactions.filter { it.type == TransactionType.SAVINGS && it.goalId == null }.sumOf { it.amount }
            (goalSavings + orphanSavings).coerceAtLeast(0.0)
        } else {
            transactions.filter { it.type == TransactionType.SAVINGS }.sumOf { it.amount }.coerceAtLeast(0.0)
        }
        val netBalance = totalIncome - totalExpense - totalSavings
        val netWorth = totalIncome - totalExpense

        val expenseByCategory = transactions
            .filter { it.type == TransactionType.EXPENSE }
            .groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
            .toList()
            .sortedByDescending { it.second }
            .take(5)

        val formatter = NumberFormat.getCurrencyInstance(Locale("id", "ID"))

        val goalsSummary = if (goals.isEmpty()) {
            "Belum ada target tabungan yang dibuat."
        } else {
            goals.joinToString("\n") { g ->
                val status = if (g.isCompleted) "[TERCAPAI]" else "[BERJALAN]"
                val percent = if (g.targetAmount > 0) (g.currentAmount / g.targetAmount * 100).toInt() else 0
                "- ${g.title}: ${formatter.format(g.currentAmount)} dari target ${formatter.format(g.targetAmount)} ($percent%) $status"
            }
        }

        val topExpenseStr = if (expenseByCategory.isEmpty()) {
            "Belum ada catatan pengeluaran."
        } else {
            expenseByCategory.joinToString("\n") { (cat, amt) ->
                "- $cat: ${formatter.format(amt)}"
            }
        }

        return """
            Ringkasan Keuangan Pengguna Saat Ini:
            - Total Pemasukan: ${formatter.format(totalIncome)}
            - Total Pengeluaran: ${formatter.format(totalExpense)}
            - Total Tabungan/Investasi Disimpan: ${formatter.format(totalSavings)}
            - Saldo Bersih Tersisa: ${formatter.format(netBalance)}
            
            Top 5 Kategori Pengeluaran Terbesar:
            $topExpenseStr
            
            Target Tabungan:
            $goalsSummary
        """.trimIndent()
    }

    suspend fun requestAiFinancialAdvice(
        context: android.content.Context,
        apiKey: String,
        modelName: String,
        userQuery: String,
        mediaAttachmentName: String? = null,
        mediaType: String? = null, // "image" or "video"
        attachedImageUri: String? = null,
        chatHistory: List<AiMessageEntity>
    ): Result<String> {
        return try {
            val financialSummary = generateFinancialContextSummary()
            val mediaContext = if (mediaAttachmentName != null) {
                "\n[Lampiran Media Pengguna: $mediaAttachmentName (Tipe: ${mediaType ?: "Dokumen/Gambar"})]\nPengguna melampirkan file ini untuk dianalisa struktur rincian pengeluaran/rencana target tabungan."
            } else ""

            val systemPrompt = """
                Anda adalah TANA Financial Advisor AI, asisten keuangan dan perencana tabungan pribadi berbasis data riil, presisi, dan objektif.
                Gaya komunikasi: Profesional, ringkas, matematis, berbasis fakta, tanpa spekulasi atau opini kosong, dan TANPA EMOJI (sesuai tema monokrom bersih).

                Berikut adalah data keuangan riil dan target tabungan pengguna saat ini dari database aplikasi:
                $financialSummary
                $mediaContext

                ATURAN UTAMA & INSTRUKSI KERJA AI:
                1. STRICT FACTUALITY (FAKTA RIIL): Gunakan data riil di atas dalam setiap kalkulasi (saldo bersih, perputaran kas, target tabungan yang sedang aktif). Dilarang mengarang angka fiktif jika data tersedia.
                2. BACA STRUK & DOKUMEN: Jika pengguna mengirimkan atau menyebut struk belanja/invoice/dokumen/foto belanja, identifikasi daftar item, subtotal, pajak, diskon, dan total akhir pengeluaran secara terperinci langsung dari gambar.
                3. MANAJEMEN ANGGARAN CERDAS: Hitung alokasi tabungan harian/bulanan yang realistis berdasarkan sisa saldo dan arus kas riil. Berikan perhitungan pasti (misal: dengan menyisihkan Rp 20.000/hari, target tercapai dalam X hari).
                4. AUTONOMOUS AI AGENT & RUTINITAS OTOMATIS:
                   Pengguna dapat meminta Anda membuat tugas/rutinitas otonom (bekerja sendiri tanpa user mengetik lagi setiap hari).
                   
                   a) USULAN RUTINITAS SETORAN OTOMATIS (misal: setor 10rb tiap hari kecuali Minggu):
                      Jika pengguna meminta setor berkala otomatis, buat usulan perizinan dengan tag:
                      [PROPOSE_AGENT_ROUTINE: AUTO_DEPOSIT | <Nama Target Tabungan> | <Nominal Angka> | 2,3,4,5,6,7 | Setoran otomatis harian kecuali Minggu]
                      (Keterangan active days: 1=Minggu, 2=Senin, 3=Selasa, 4=Rabu, 5=Kamis, 6=Jumat, 7=Sabtu. Contoh kecuali Minggu adalah: 2,3,4,5,6,7)
                      
                   b) RUTINITAS MOTIVASI / PENGINGAT HARIAN MANDIRI (misal: kasih motivasi tiap hari):
                      [PROPOSE_AGENT_ROUTINE: DAILY_MOTIVATION | Motivasi Finansial Harian | 0 | 1,2,3,4,5,6,7 | Kirimkan motivasi finansial mandiri setiap hari]
                      
                   c) MENGHENTIKAN / MEMBATALKAN RUTINITAS OTOMATIS (misal: stop setor otomatis / matikan agen):
                      Format: [STOP_AGENT_ROUTINE: <Nama Target / ALL>]
                      Contoh: [STOP_AGENT_ROUTINE: Dana Darurat] atau [STOP_AGENT_ROUTINE: ALL]

                5. INTERAKSI AKSI FINANSIAL LANGSUNG (INSTANT):
                   a) UNTUK MENYETOR UANG KE TABUNGAN (SEKETIKA):
                      Format: [SETOR_NABUNG: <Nama Target Tabungan> | <Nominal Angka Murni> | <Catatan Ringkas>]
                      Contoh: [SETOR_NABUNG: Dana Darurat | 50000 | Setoran instan via AI]

                   b) UNTUK MENARIK UANG DARI TABUNGAN:
                      Format: [TARIK_NABUNG: <Nama Target Tabungan> | <Nominal Angka Murni> | <Catatan>]
                      Contoh: [TARIK_NABUNG: Dana Darurat | 25000 | Penarikan dana via AI]
                      
                   c) UNTUK MEMBUAT TARGET TABUNGAN BARU:
                      Format: [TARGET_NABUNG: <Nama Target> | <Target Angka> | <Setoran Rutin> | <DAILY/WEEKLY/MONTHLY>]
                      Contoh: [TARGET_NABUNG: Beli Laptop Asus | 8000000 | 40000 | DAILY]
                      
                   d) UNTUK MENCATAT TRANSAKSI DARI STRUK ATAU PERMINTAAN PENGGUNA:
                      Format: [CATAT_TRANSAKSI: <INCOME/EXPENSE> | <Nominal Angka Murni> | <Kategori> | <Catatan>]
                      Contoh: [CATAT_TRANSAKSI: EXPENSE | 45000 | Makanan & Minuman | Makan Siang dari Struk]

                    e) UNTUK MENGHAPUS TARGET TABUNGAN:
                       Format: [HAPUS_NABUNG: <Nama Target Tabungan>]
                       Contoh: [HAPUS_NABUNG: Beli Laptop]

                    f) UNTUK MENGUBAH PARAMETER TARGET TABUNGAN:
                       Format: [UPDATE_GOAL: <Nama Target> | <Target Angka Baru> | <Setoran Rutin Baru>]
                       Contoh: [UPDATE_GOAL: Dana Darurat | 10000000 | 50000]

                    g) UNTUK MEMPERBAIKI KESALAHAN TRANSAKSI TABUNGAN (SALAH KETIK):
                       Format: [UBAH_TRANSAKSI_NABUNG: <Nama Target> | <Nominal Baru> | <Catatan Perbaikan>]
                       Contoh: [UBAH_TRANSAKSI_NABUNG: Dana Darurat | 50000 | Koreksi nominal salah ketik]

                6. ATURAN RE-PROMPT & RESET STATUS AKSI:
                   - Selalu keluarkan tag aksi murni TANPA akhiran _DONE atau _CANCEL (contoh: selalu [SETOR_NABUNG: ...], BUKAN [SETOR_NABUNG_CANCEL:]).
                   - Jika permintaan sebelumnya dibatalkan pengguna lalu pengguna meminta lagi (misal: "setor lagi", "coba setor ke dana darurat"), buat usulan aksi baru yang segar dan siap dieksekusi.
                   - Selalu sertakan tag aksi yang relevan di akhir jawaban agar kartu interaktif muncul dan dieksekusi atau disetujui langsung oleh pengguna.
            """.trimIndent()

            val messages = mutableListOf<com.tana.data.api.OpenRouterRequestMessage>()
            messages.add(com.tana.data.api.OpenRouterRequestMessage(role = "system", content = systemPrompt))

            // Build clean alternating chat context (excluding the new user query if already present at tail)
            val historyWithoutCurrentTail = if (chatHistory.isNotEmpty() && chatHistory.last().role == "user" && chatHistory.last().content.contains(userQuery.take(20))) {
                chatHistory.dropLast(1)
            } else {
                chatHistory
            }

            val recentHistory = historyWithoutCurrentTail.takeLast(10)
            var lastRole = "system"
            for (msg in recentHistory) {
                if (msg.role != "user" && msg.role != "assistant") continue
                // Ensure no duplicate consecutive roles in payload
                if (msg.role == lastRole) continue

                val sanitizedContent = msg.content
                    .replace(Regex("""\[(PROPOSE_AGENT_ROUTINE|STOP_AGENT_ROUTINE|TARGET_NABUNG|SETOR_TABUNGAN|SETOR_NABUNG|TARIK_NABUNG|CATAT_TRANSAKSI|HAPUS_NABUNG|UPDATE_GOAL|UBAH_TRANSAKSI_NABUNG)(?:_DONE|_CANCEL)?:\s*([^\]]+)\]"""), "[$1: $2]")
                    .replace(Regex("""\[ATTACHMENT_URI:\s*[^\]]+\]"""), "")
                    .trim()

                if (sanitizedContent.isNotBlank()) {
                    messages.add(com.tana.data.api.OpenRouterRequestMessage(role = msg.role, content = sanitizedContent))
                    lastRole = msg.role
                }
            }

            val fullUserQuery = if (mediaAttachmentName != null) {
                "[Melampirkan $mediaAttachmentName]\n$userQuery"
            } else {
                userQuery
            }

            // If the last added message was user, replace or ensure we append properly
            if (lastRole == "user") {
                messages.removeAt(messages.size - 1)
            }

            // Convert image to base64 Data URI for Vision AI models if attached
            val base64DataUri = if (!attachedImageUri.isNullOrBlank()) {
                convertUriToBase64DataUri(context, attachedImageUri)
            } else null

            if (!base64DataUri.isNullOrBlank()) {
                val contentParts = listOf(
                    com.tana.data.api.OpenRouterContentPart(
                        type = "text",
                        text = fullUserQuery.ifBlank { "Tolong analisa rincian struk/gambar ini dan berikan ringkasan total biaya, item belanjaan, dan saran alokasi tabungan." }
                    ),
                    com.tana.data.api.OpenRouterContentPart(
                        type = "image_url",
                        imageUrl = com.tana.data.api.OpenRouterImageUrl(url = base64DataUri)
                    )
                )
                messages.add(com.tana.data.api.OpenRouterRequestMessage(role = "user", content = contentParts))
            } else {
                messages.add(com.tana.data.api.OpenRouterRequestMessage(role = "user", content = fullUserQuery))
            }

            val request = OpenRouterChatRequest(
                model = modelName,
                messages = messages,
                temperature = 0.6
            )

            val response = OpenRouterClient.service.createChatCompletion(
                authorization = "Bearer $apiKey",
                request = request
            )

            if (response.isSuccessful && response.body() != null) {
                val reply = response.body()?.choices?.firstOrNull()?.message?.content
                if (!reply.isNullOrBlank()) {
                    Result.success(reply.trim())
                } else {
                    val err = response.body()?.error?.message ?: "Respons AI kosong dari OpenRouter."
                    Result.failure(Exception(err))
                }
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Gagal menghubungi OpenRouter (${response.code()})"
                Result.failure(Exception("Error OpenRouter [${response.code()}]: $errorMsg"))
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) {
                throw e
            }
            Result.failure(e)
        }
    }

    suspend fun requestAiFinancialAdviceStream(
        context: android.content.Context,
        apiKey: String,
        modelName: String,
        userQuery: String,
        mediaAttachmentName: String? = null,
        mediaType: String? = null,
        attachedImageUri: String? = null,
        chatHistory: List<AiMessageEntity> = emptyList(),
        onChunk: suspend (String) -> Unit
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val financialSummary = generateFinancialContextSummary()
            val mediaContext = if (mediaAttachmentName != null) {
                "\n[Lampiran Media Pengguna: $mediaAttachmentName (Tipe: ${mediaType ?: "Dokumen/Gambar"})]\nPengguna melampirkan file ini untuk dianalisa struktur rincian pengeluaran/rencana target tabungan."
            } else ""

            val systemPrompt = """
                Anda adalah TANA Financial Advisor AI, asisten keuangan dan perencana tabungan pribadi berbasis data riil, presisi, dan objektif.
                Gaya komunikasi: Profesional, ringkas, matematis, berbasis fakta, tanpa spekulasi atau opini kosong, dan TANPA EMOJI (sesuai tema monokrom bersih).

                Berikut adalah data keuangan riil dan target tabungan pengguna saat ini dari database aplikasi:
                $financialSummary
                $mediaContext

                ATURAN UTAMA & INSTRUKSI KERJA AI:
                1. STRICT FACTUALITY (FAKTA RIIL): Gunakan data riil di atas dalam setiap kalkulasi (saldo bersih, perputaran kas, target tabungan yang sedang aktif). Dilarang mengarang angka fiktif jika data tersedia.
                2. BACA STRUK & DOKUMEN: Jika pengguna mengirimkan atau menyebut struk belanja/invoice/dokumen/foto belanja, identifikasi daftar item, subtotal, pajak, diskon, dan total akhir pengeluaran secara terperinci langsung dari gambar.
                3. MANAJEMEN ANGGARAN CERDAS: Hitung alokasi tabungan harian/bulanan yang realistis berdasarkan sisa saldo dan arus kas riil. Berikan perhitungan pasti (misal: dengan menyisihkan Rp 20.000/hari, target tercapai dalam X hari).
                4. AUTONOMOUS AI AGENT & RUTINITAS OTOMATIS:
                   Pengguna dapat meminta Anda membuat tugas/rutinitas otonom (bekerja sendiri tanpa user mengetik lagi setiap hari).
                   
                   a) USULAN RUTINITAS SETORAN OTOMATIS (misal: setor 10rb tiap hari kecuali Minggu):
                      Jika pengguna meminta setor berkala otomatis, buat usulan perizinan dengan tag:
                      [PROPOSE_AGENT_ROUTINE: AUTO_DEPOSIT | <Nama Target Tabungan> | <Nominal Angka> | 2,3,4,5,6,7 | Setoran otomatis harian kecuali Minggu]
                      (Keterangan active days: 1=Minggu, 2=Senin, 3=Selasa, 4=Rabu, 5=Kamis, 6=Jumat, 7=Sabtu. Contoh kecuali Minggu adalah: 2,3,4,5,6,7)
                      
                   b) RUTINITAS MOTIVASI / PENGINGAT HARIAN MANDIRI (misal: kasih motivasi tiap hari):
                      [PROPOSE_AGENT_ROUTINE: DAILY_MOTIVATION | Motivasi Finansial Harian | 0 | 1,2,3,4,5,6,7 | Kirimkan motivasi finansial mandiri setiap hari]
                      
                   c) MENGHENTIKAN / MEMBATALKAN RUTINITAS OTOMATIS (misal: stop setor otomatis / matikan agen):
                      Format: [STOP_AGENT_ROUTINE: <Nama Target / ALL>]
                      Contoh: [STOP_AGENT_ROUTINE: Dana Darurat] atau [STOP_AGENT_ROUTINE: ALL]

                5. INTERAKSI AKSI FINANSIAL LANGSUNG (INSTANT):
                   a) UNTUK MENYETOR UANG KE TABUNGAN (SEKETIKA):
                      Format: [SETOR_NABUNG: <Nama Target Tabungan> | <Nominal Angka Murni> | <Catatan Ringkas>]
                      Contoh: [SETOR_NABUNG: Dana Darurat | 50000 | Setoran instan via AI]

                   b) UNTUK MENARIK UANG DARI TABUNGAN:
                      Format: [TARIK_NABUNG: <Nama Target Tabungan> | <Nominal Angka Murni> | <Catatan>]
                      Contoh: [TARIK_NABUNG: Dana Darurat | 25000 | Penarikan dana via AI]
                      
                   c) UNTUK MEMBUAT TARGET TABUNGAN BARU:
                      Format: [TARGET_NABUNG: <Nama Target> | <Target Angka> | <Setoran Rutin> | <DAILY/WEEKLY/MONTHLY>]
                      Contoh: [TARGET_NABUNG: Beli Laptop Asus | 8000000 | 40000 | DAILY]
                      
                   d) UNTUK MENCATAT TRANSAKSI DARI STRUK ATAU PERMINTAAN PENGGUNA:
                      Format: [CATAT_TRANSAKSI: <INCOME/EXPENSE> | <Nominal Angka Murni> | <Kategori> | <Catatan>]
                      Contoh: [CATAT_TRANSAKSI: EXPENSE | 45000 | Makanan & Minuman | Makan Siang dari Struk]

                    e) UNTUK MENGHAPUS TARGET TABUNGAN:
                       Format: [HAPUS_NABUNG: <Nama Target Tabungan>]
                       Contoh: [HAPUS_NABUNG: Beli Laptop]

                    f) UNTUK MENGUBAH PARAMETER TARGET TABUNGAN:
                       Format: [UPDATE_GOAL: <Nama Target> | <Target Angka Baru> | <Setoran Rutin Baru>]
                       Contoh: [UPDATE_GOAL: Dana Darurat | 10000000 | 50000]

                    g) UNTUK MEMPERBAIKI KESALAHAN TRANSAKSI TABUNGAN (SALAH KETIK):
                       Format: [UBAH_TRANSAKSI_NABUNG: <Nama Target> | <Nominal Baru> | <Catatan Perbaikan>]
                       Contoh: [UBAH_TRANSAKSI_NABUNG: Dana Darurat | 50000 | Koreksi nominal salah ketik]

                6. ATURAN RE-PROMPT & RESET STATUS AKSI:
                   - Selalu keluarkan tag aksi murni TANPA akhiran _DONE atau _CANCEL (contoh: selalu [SETOR_NABUNG: ...], BUKAN [SETOR_NABUNG_CANCEL:]).
                   - Jika permintaan sebelumnya dibatalkan pengguna lalu pengguna meminta lagi (misal: "setor lagi", "coba setor ke dana darurat"), buat usulan aksi baru yang segar dan siap dieksekusi.
                   - Selalu sertakan tag aksi yang relevan di akhir jawaban agar kartu interaktif muncul dan dieksekusi atau disetujui langsung oleh pengguna.
            """.trimIndent()

            val messages = mutableListOf<com.tana.data.api.OpenRouterRequestMessage>()
            messages.add(com.tana.data.api.OpenRouterRequestMessage(role = "system", content = systemPrompt))

            val historyWithoutCurrentTail = if (chatHistory.isNotEmpty() && chatHistory.last().role == "user" && chatHistory.last().content.contains(userQuery.take(20))) {
                chatHistory.dropLast(1)
            } else {
                chatHistory
            }

            val recentHistory = historyWithoutCurrentTail.takeLast(10)
            var lastRole = "system"
            for (msg in recentHistory) {
                if (msg.role != "user" && msg.role != "assistant") continue
                if (msg.role == lastRole) continue

                val sanitizedContent = msg.content
                    .replace(Regex("""\[(PROPOSE_AGENT_ROUTINE|STOP_AGENT_ROUTINE|TARGET_NABUNG|SETOR_TABUNGAN|SETOR_NABUNG|TARIK_NABUNG|CATAT_TRANSAKSI|HAPUS_NABUNG|UPDATE_GOAL|UBAH_TRANSAKSI_NABUNG)(?:_DONE|_CANCEL)?:\s*([^\]]+)\]"""), "[$1: $2]")
                    .replace(Regex("""\[ATTACHMENT_URI:\s*[^\]]+\]"""), "")
                    .trim()

                if (sanitizedContent.isNotBlank()) {
                    messages.add(com.tana.data.api.OpenRouterRequestMessage(role = msg.role, content = sanitizedContent))
                    lastRole = msg.role
                }
            }

            val fullUserQuery = if (mediaAttachmentName != null) {
                "[Melampirkan $mediaAttachmentName]\n$userQuery"
            } else {
                userQuery
            }

            if (lastRole == "user" && messages.isNotEmpty()) {
                messages.removeAt(messages.size - 1)
            }

            val base64DataUri = if (!attachedImageUri.isNullOrBlank()) {
                convertUriToBase64DataUri(context, attachedImageUri)
            } else null

            if (!base64DataUri.isNullOrBlank()) {
                val contentParts = listOf(
                    com.tana.data.api.OpenRouterContentPart(
                        type = "text",
                        text = fullUserQuery.ifBlank { "Tolong analisa rincian struk/gambar ini dan berikan ringkasan total biaya, item belanjaan, dan saran alokasi tabungan." }
                    ),
                    com.tana.data.api.OpenRouterContentPart(
                        type = "image_url",
                        imageUrl = com.tana.data.api.OpenRouterImageUrl(url = base64DataUri)
                    )
                )
                messages.add(com.tana.data.api.OpenRouterRequestMessage(role = "user", content = contentParts))
            } else {
                messages.add(com.tana.data.api.OpenRouterRequestMessage(role = "user", content = fullUserQuery))
            }

            val request = OpenRouterChatRequest(
                model = modelName,
                messages = messages,
                temperature = 0.6,
                stream = true
            )

            val response = OpenRouterClient.service.createChatCompletionStream(
                authorization = "Bearer $apiKey",
                request = request
            )

            if (!response.isSuccessful) {
                val errorMsg = response.errorBody()?.string() ?: "Gagal menghubungi OpenRouter (${response.code()})"
                return@withContext Result.failure(Exception("Error OpenRouter [${response.code()}]: $errorMsg"))
            }

            val body = response.body() ?: return@withContext Result.failure(Exception("Respons stream kosong"))
            val source = body.source()
            val fullContent = StringBuilder()

            while (!source.exhausted()) {
                kotlinx.coroutines.currentCoroutineContext().ensureActive()
                val line = source.readUtf8Line() ?: break
                val trimmed = line.trim()
                if (trimmed.isEmpty() || trimmed.startsWith(":")) continue

                if (trimmed.startsWith("data:")) {
                    val dataPayload = trimmed.removePrefix("data:").trim()
                    if (dataPayload == "[DONE]") {
                        break
                    }
                    val token = extractDeltaContent(dataPayload)
                    if (!token.isNullOrEmpty()) {
                        fullContent.append(token)
                        onChunk(token)
                    }
                }
            }

            if (fullContent.isNotBlank()) {
                Result.success(fullContent.toString().trim())
            } else {
                Result.failure(Exception("Respons stream tidak menghasilkan teks"))
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) {
                throw e
            }
            Result.failure(e)
        }
    }

    private fun extractDeltaContent(jsonStr: String): String? {
        try {
            val chunk = OpenRouterClient.moshi.adapter(OpenRouterStreamChunk::class.java).fromJson(jsonStr)
            val content = chunk?.choices?.firstOrNull()?.delta?.content
            if (!content.isNullOrEmpty()) return content
        } catch (_: Exception) {}

        try {
            val regex = Regex(""""delta"\s*:\s*\{[^}]*"content"\s*:\s*"((?:[^"\\]|\\.)*)"""")
            val match = regex.find(jsonStr)
            if (match != null) {
                return unescapeJson(match.groupValues[1])
            }
        } catch (_: Exception) {}

        return null
    }

    private fun unescapeJson(s: String): String {
        val sb = StringBuilder()
        var i = 0
        val len = s.length
        while (i < len) {
            val c = s[i]
            if (c == '\\' && i + 1 < len) {
                when (val next = s[i + 1]) {
                    '"' -> { sb.append('"'); i += 2 }
                    '\\' -> { sb.append('\\'); i += 2 }
                    '/' -> { sb.append('/'); i += 2 }
                    'b' -> { sb.append('\b'); i += 2 }
                    'f' -> { sb.append('\u000c'); i += 2 }
                    'n' -> { sb.append('\n'); i += 2 }
                    'r' -> { sb.append('\r'); i += 2 }
                    't' -> { sb.append('\t'); i += 2 }
                    'u' -> {
                        if (i + 5 < len) {
                            val hex = s.substring(i + 2, i + 6)
                            try {
                                sb.append(hex.toInt(16).toChar())
                                i += 6
                            } catch (_: Exception) {
                                sb.append(c)
                                i++
                            }
                        } else {
                            sb.append(c)
                            i++
                        }
                    }
                    else -> {
                        sb.append(next)
                        i += 2
                    }
                }
            } else {
                sb.append(c)
                i++
            }
        }
        return sb.toString()
    }

    private fun convertUriToBase64DataUri(context: android.content.Context, uriString: String): String? {
        return try {
            if (uriString.startsWith("data:image")) return uriString

            val inputStream: java.io.InputStream? = when {
                uriString.startsWith("content://") -> {
                    try {
                        context.contentResolver.openInputStream(android.net.Uri.parse(uriString))
                    } catch (e: Exception) {
                        null
                    }
                }
                uriString.startsWith("file://") -> {
                    val path = android.net.Uri.parse(uriString).path
                    if (path != null && java.io.File(path).exists()) {
                        java.io.File(path).inputStream()
                    } else {
                        try {
                            context.contentResolver.openInputStream(android.net.Uri.parse(uriString))
                        } catch (e: Exception) {
                            null
                        }
                    }
                }
                uriString.startsWith("/") -> {
                    val file = java.io.File(uriString)
                    if (file.exists()) file.inputStream() else null
                }
                else -> {
                    val file = java.io.File(uriString)
                    if (file.exists()) {
                        file.inputStream()
                    } else {
                        try {
                            context.contentResolver.openInputStream(android.net.Uri.parse(uriString))
                        } catch (e: Exception) {
                            null
                        }
                    }
                }
            }
            if (inputStream == null) return null

            val bytes = inputStream.readBytes()
            inputStream.close()

            val bitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
            val maxDim = 1024
            val scaledBitmap = if (bitmap.width > maxDim || bitmap.height > maxDim) {
                val scale = maxDim.toFloat() / Math.max(bitmap.width, bitmap.height)
                android.graphics.Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).toInt(), (bitmap.height * scale).toInt(), true)
            } else {
                bitmap
            }

            val outputStream = java.io.ByteArrayOutputStream()
            scaledBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 85, outputStream)
            val compressedBytes = outputStream.toByteArray()
            val base64Str = android.util.Base64.encodeToString(compressedBytes, android.util.Base64.NO_WRAP)
            "data:image/jpeg;base64,$base64Str"
        } catch (e: Exception) {
            null
        }
    }
}
