import re

with open('app/src/main/java/com/example/ui/screens/AiScreen.kt', 'r') as f:
    text = f.read()

# 1. Add missing import
if "import androidx.compose.ui.graphics.asImageBitmap" not in text:
    text = text.replace("import androidx.compose.ui.Alignment", "import androidx.compose.ui.Alignment\nimport androidx.compose.ui.graphics.asImageBitmap\nimport android.speech.SpeechRecognizer")

# 2. Add onUpdateAiMessageContent to AiScreen signature
sig_find = "onAddTransactionFromAi: ((type: TransactionType, amount: Double, category: String, note: String) -> Unit)? = null,"
sig_replace = "onAddTransactionFromAi: ((type: TransactionType, amount: Double, category: String, note: String) -> Unit)? = null,\n    val onUpdateAiMessageContent: ((Long, String) -> Unit)? = null,"
text = text.replace(sig_find, sig_replace)

# 3. Add speechRecognizer state
state_find = "val listState = rememberLazyListState()"
state_replace = "val listState = rememberLazyListState()\n    var speechRecognizer by remember { mutableStateOf<SpeechRecognizer?>(null) }"
text = text.replace(state_find, state_replace)

# 4. Remove local speechRecognizer
text = text.replace("var speechRecognizer: SpeechRecognizer? = null", "")

with open('app/src/main/java/com/example/ui/screens/AiScreen.kt', 'w') as f:
    f.write(text)
