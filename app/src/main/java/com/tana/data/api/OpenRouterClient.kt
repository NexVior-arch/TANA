package com.tana.data.api

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object OpenRouterClient {
    private const val BASE_URL = "https://openrouter.ai/api/v1/"

    val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.HEADERS
        })
        .build()

    val service: OpenRouterService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(OpenRouterService::class.java)
    }

    var cachedLiveModels: List<OpenRouterModel>? = null

    val defaultRichModels = listOf(
        OpenRouterModel(
            id = "google/gemini-2.5-flash",
            name = "Google Gemini 2.5 Flash",
            description = "Model generasi terbaru Google dengan analisis keuangan mendalam & super cepat.",
            pricing = OpenRouterPricing(prompt = "0.00000015", completion = "0.0000006"),
            context_length = 1048576
        ),
        OpenRouterModel(
            id = "google/gemini-2.0-flash-001",
            name = "Google Gemini 2.0 Flash",
            description = "Super cepat, multimodal, hemat biaya & penalaran tajam.",
            pricing = OpenRouterPricing(prompt = "0.0000001", completion = "0.0000004"),
            context_length = 1048576
        ),
        OpenRouterModel(
            id = "google/gemini-2.0-flash-lite-001",
            name = "Google Gemini 2.0 Flash Lite",
            description = "Model ultra ringan & paling responsif untuk efisiensi tinggi.",
            pricing = OpenRouterPricing(prompt = "0.000000075", completion = "0.0000003"),
            context_length = 1048576
        ),
        OpenRouterModel(
            id = "google/gemini-2.5-pro",
            name = "Google Gemini 2.5 Pro",
            description = "Flagship reasoning & analisis finansial paling akurat.",
            pricing = OpenRouterPricing(prompt = "0.00000125", completion = "0.000005"),
            context_length = 2097152
        ),
        OpenRouterModel(
            id = "google/gemini-1.5-flash",
            name = "Google Gemini 1.5 Flash",
            description = "Model stabil dengan jutaan konteks token.",
            pricing = OpenRouterPricing(prompt = "0.000000075", completion = "0.0000003"),
            context_length = 1048576
        ),
        OpenRouterModel(
            id = "google/gemini-1.5-pro",
            name = "Google Gemini 1.5 Pro",
            description = "Model Pro stabil untuk perencanaan anggaran kompleks.",
            pricing = OpenRouterPricing(prompt = "0.00000125", completion = "0.000005"),
            context_length = 2097152
        ),
        OpenRouterModel(
            id = "deepseek/deepseek-r1:free",
            name = "DeepSeek R1 Reasoning (Gratis)",
            description = "Model reasoning open-weights terdepan, gratis dan sangat cerdas.",
            pricing = OpenRouterPricing(prompt = "0", completion = "0"),
            context_length = 65536
        ),
        OpenRouterModel(
            id = "deepseek/deepseek-chat:free",
            name = "DeepSeek V3 Chat (Gratis)",
            description = "Chatbot cepat dan serbaguna gratis untuk konsultasi harian.",
            pricing = OpenRouterPricing(prompt = "0", completion = "0"),
            context_length = 65536
        ),
        OpenRouterModel(
            id = "meta-llama/llama-3.3-70b-instruct:free",
            name = "Meta Llama 3.3 70B Instruct (Gratis)",
            description = "Generasi terbaru Llama 3.3 dengan pemahaman konteks luar biasa.",
            pricing = OpenRouterPricing(prompt = "0", completion = "0"),
            context_length = 131072
        ),
        OpenRouterModel(
            id = "meta-llama/llama-3.1-8b-instruct:free",
            name = "Meta Llama 3.1 8B Instruct (Gratis)",
            description = "Model cepat dan ringan untuk respon instan tanpa biaya.",
            pricing = OpenRouterPricing(prompt = "0", completion = "0"),
            context_length = 131072
        ),
        OpenRouterModel(
            id = "qwen/qwen-2.5-72b-instruct:free",
            name = "Qwen 2.5 72B Instruct (Gratis)",
            description = "Model unggulan Alibaba dengan kemampuan matematika & data hebat.",
            pricing = OpenRouterPricing(prompt = "0", completion = "0"),
            context_length = 131072
        ),
        OpenRouterModel(
            id = "nvidia/nemotron-3-nano-omni-30b-a3b-reasoning:free",
            name = "Nvidia Nemotron 3 Nano (Gratis)",
            description = "Model penalaran gratis dari Nvidia dengan kemampuan reasoning tinggi.",
            pricing = OpenRouterPricing(prompt = "0", completion = "0"),
            context_length = 128000
        ),
        OpenRouterModel(
            id = "nvidia/llama-3.1-nemotron-70b-instruct:free",
            name = "Nvidia Llama 3.1 Nemotron 70B (Gratis)",
            description = "Model 70B canggih gratis dari Nvidia untuk instruksi finansial.",
            pricing = OpenRouterPricing(prompt = "0", completion = "0"),
            context_length = 131072
        ),
        OpenRouterModel(
            id = "mistralai/mistral-7b-instruct:free",
            name = "Mistral 7B Instruct (Gratis)",
            description = "Model eropa efisien dan to-the-point gratis.",
            pricing = OpenRouterPricing(prompt = "0", completion = "0"),
            context_length = 32768
        ),
        OpenRouterModel(
            id = "openai/gpt-4o-mini",
            name = "OpenAI GPT-4o Mini",
            description = "Model ringkas dan pintar dari OpenAI dengan biaya sangat terjangkau.",
            pricing = OpenRouterPricing(prompt = "0.00000015", completion = "0.0000006"),
            context_length = 128000
        ),
        OpenRouterModel(
            id = "openai/gpt-4o",
            name = "OpenAI GPT-4o",
            description = "Model multimodal flagship dari OpenAI dengan kecerdasan tingkat tinggi.",
            pricing = OpenRouterPricing(prompt = "0.0000025", completion = "0.00001"),
            context_length = 128000
        ),
        OpenRouterModel(
            id = "openai/o3-mini",
            name = "OpenAI o3-mini",
            description = "Model reasoning penalaran matematika dan sains terbaru OpenAI.",
            pricing = OpenRouterPricing(prompt = "0.0000011", completion = "0.0000044"),
            context_length = 200000
        ),
        OpenRouterModel(
            id = "anthropic/claude-3.5-sonnet",
            name = "Anthropic Claude 3.5 Sonnet",
            description = "Standar emas untuk penulisan saran finansial, etika, dan analisis.",
            pricing = OpenRouterPricing(prompt = "0.000003", completion = "0.000015"),
            context_length = 200000
        ),
        OpenRouterModel(
            id = "anthropic/claude-3.5-haiku",
            name = "Anthropic Claude 3.5 Haiku",
            description = "Versi kilat Claude dengan kecepatan luar biasa dan akurasi tinggi.",
            pricing = OpenRouterPricing(prompt = "0.0000008", completion = "0.000004"),
            context_length = 200000
        ),
        OpenRouterModel(
            id = "google/gemma-2-9b-it:free",
            name = "Google Gemma 2 9B (Gratis)",
            description = "Model open weight Google dengan performa efisien.",
            pricing = OpenRouterPricing(prompt = "0", completion = "0"),
            context_length = 8192
        ),
        OpenRouterModel(
            id = "google/gemma-2-27b-it:free",
            name = "Google Gemma 2 27B (Gratis)",
            description = "Model open weight 27B Google dengan akurasi reasoning superior.",
            pricing = OpenRouterPricing(prompt = "0", completion = "0"),
            context_length = 8192
        ),
        OpenRouterModel(
            id = "microsoft/phi-3-mini-128k-instruct:free",
            name = "Microsoft Phi-3 Mini 128K (Gratis)",
            description = "Model mini Microsoft dengan konteks 128k.",
            pricing = OpenRouterPricing(prompt = "0", completion = "0"),
            context_length = 128000
        ),
        OpenRouterModel(
            id = "microsoft/phi-3-medium-128k-instruct:free",
            name = "Microsoft Phi-3 Medium (Gratis)",
            description = "Model medium Microsoft untuk tugas penalaran.",
            pricing = OpenRouterPricing(prompt = "0", completion = "0"),
            context_length = 128000
        ),
        OpenRouterModel(
            id = "cohere/command-r-plus-08-2024",
            name = "Cohere Command R+",
            description = "Model enterprise Cohere untuk RAG dan analisis teks mendalam.",
            pricing = OpenRouterPricing(prompt = "0.0000025", completion = "0.00001"),
            context_length = 128000
        ),
        OpenRouterModel(
            id = "perplexity/sonar-reasoning",
            name = "Perplexity Sonar Reasoning",
            description = "Model pencarian dan reasoning online canggih.",
            pricing = OpenRouterPricing(prompt = "0.000001", completion = "0.000005"),
            context_length = 127072
        )
    )

    val defaultModels = defaultRichModels.map { it.id }
}
