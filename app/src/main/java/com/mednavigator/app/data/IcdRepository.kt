package com.mednavigator.app.data

import android.content.Context
import android.util.Log
import androidx.room.Room
import com.mednavigator.app.data.api.IcdApiClient
import com.mednavigator.app.data.api.Icd10CmCondition
import com.mednavigator.app.data.models.IcdCondition
import com.mednavigator.app.data.models.IcdConditionEntity
import com.mednavigator.app.data.models.IcdDatabase
import com.mednavigator.app.utils.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Repository for accessing and searching ICD-11 conditions from local database
 * Provides search, lookup, and context-building functions for medical guidance
 */
class IcdRepository(private val context: Context) {

    companion object {
        private const val TAG = "IcdRepository"
        @Volatile
        private var instance: IcdRepository? = null

        fun getInstance(context: Context): IcdRepository =
            instance ?: synchronized(this) {
                instance ?: IcdRepository(context).also { instance = it }
            }
    }

    private val icdDatabase: IcdDatabase by lazy {
        Room.databaseBuilder(
            context.applicationContext,
            IcdDatabase::class.java,
            Constants.ICD_DB_NAME
        ).fallbackToDestructiveMigration(dropAllTables = true).build()
    }

    private val dao by lazy { icdDatabase.icdConditionDao() }
    private val apiClient by lazy { IcdApiClient() }

    private val initMutex = Mutex()
    private var isInitialized = false
    private val searchCache = mutableMapOf<String, List<IcdCondition>>()

    /**
     * Initialize the ICD database with default conditions if empty
     * Should be called once on app startup
     */
    suspend fun initializeIfNeeded() {
        initMutex.withLock {
            if (isInitialized) return

            try {
                withContext(Dispatchers.IO) {
                    val count = dao.getConditionCount()
                    if (count == 0) {
                        Log.d(TAG, "ICD database is empty, loading default conditions...")
                        loadDefaultConditions()
                    } else {
                        Log.d(TAG, "ICD database already initialized with $count conditions")
                    }
                }
                isInitialized = true
                Log.d(TAG, "ICD database initialization complete")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize ICD database", e)
                isInitialized = true // Mark as initialized to prevent retries
                throw e
            }
        }
    }

    /**
     * Load comprehensive default ICD-11 conditions for common medical scenarios
     */
    private suspend fun loadDefaultConditions() {
        val defaultConditions = listOf(
            // Cardiovascular System
            IcdConditionEntity(
                code = "BA80",
                title = "Chest pain",
                definition = "A feeling of discomfort or pain in the chest area between the neck and abdomen. Can result from cardiac, pulmonary, or musculoskeletal causes.",
                bodySystem = "Circulatory system",
                chapter = "Cardiovascular system",
                synonyms = "heart pain, cardiac pain, precordial pain, thoracic pain, angina",
                searchKeywords = "chest heart cardiac ischemic angina myocardial infarction"
            ),
            IcdConditionEntity(
                code = "BA81",
                title = "Palpitations",
                definition = "Sensation of an abnormal or forceful heartbeat, which may be regular or irregular. The heart may feel like it is beating too fast, too slow, or irregularly.",
                bodySystem = "Circulatory system",
                chapter = "Cardiovascular system",
                synonyms = "heart palpitation, tachycardia sensation, arrhythmia feeling, heart fluttering",
                searchKeywords = "heartbeat fluttering irregular rhythm"
            ),
            IcdConditionEntity(
                code = "BA82",
                title = "Hypertension",
                definition = "Persistently elevated blood pressure, typically defined as systolic blood pressure ≥130 mmHg or diastolic ≥80 mmHg.",
                bodySystem = "Circulatory system",
                chapter = "Cardiovascular system",
                synonyms = "high blood pressure, elevated BP, HTN, hypertensive disease",
                searchKeywords = "blood pressure high elevated systolic diastolic"
            ),
            IcdConditionEntity(
                code = "BA83",
                title = "Myocardial infarction",
                definition = "Acute heart attack due to obstruction of blood flow to the heart muscle, typically caused by blood clot in coronary artery.",
                bodySystem = "Circulatory system",
                chapter = "Cardiovascular system",
                synonyms = "heart attack, MI, acute coronary syndrome, ACS, cardiac infarction",
                searchKeywords = "acute coronary heart attack MI cardiac emergency"
            ),

            // Respiratory System
            IcdConditionEntity(
                code = "CA90",
                title = "Cough",
                definition = "A sudden, usually involuntary, expulsion of air through the mouth and nose. Can be acute, chronic, productive (with sputum), or dry.",
                bodySystem = "Respiratory system",
                chapter = "Respiratory system",
                synonyms = "persistent cough, dry cough, wet cough, productive cough, hacking cough",
                searchKeywords = "respiratory throat phlegm sputum mucus bronchitis"
            ),
            IcdConditionEntity(
                code = "CA91",
                title = "Shortness of breath",
                definition = "Subjective sensation of difficulty breathing or inability to take a deep breath. Can be sudden or gradual onset.",
                bodySystem = "Respiratory system",
                chapter = "Respiratory system",
                synonyms = "dyspnea, breathlessness, SOB, difficult breathing, wheezing, labored breathing",
                searchKeywords = "breathing difficulty asthma bronchitis pneumonia respiratory"
            ),
            IcdConditionEntity(
                code = "CA92",
                title = "Pneumonia",
                definition = "Lung infection causing inflammation of the alveoli filled with fluid or pus. Often presents with fever, cough, and difficulty breathing.",
                bodySystem = "Respiratory system",
                chapter = "Respiratory system",
                synonyms = "lung infection, bacterial pneumonia, viral pneumonia, community-acquired pneumonia, CAP",
                searchKeywords = "infection fever cough respiratory pneumococcal"
            ),
            IcdConditionEntity(
                code = "CA93",
                title = "Asthma",
                definition = "Chronic inflammatory disease of the airways characterized by reversible airflow obstruction, bronchial hyperresponsiveness, and inflammation.",
                bodySystem = "Respiratory system",
                chapter = "Respiratory system",
                synonyms = "bronchial asthma, reactive airway disease, exercise-induced asthma",
                searchKeywords = "wheeze wheeze bronchial airway chronic inflammation reversible"
            ),

            // Neurological System
            IcdConditionEntity(
                code = "DA20",
                title = "Headache",
                definition = "Pain in the cranial region. Can be primary (migraine, tension) or secondary (due to underlying condition).",
                bodySystem = "Nervous system",
                chapter = "Neurological system",
                synonyms = "cephalgia, migraine, tension headache, throbbing pain, acute headache",
                searchKeywords = "head pain migraine tension cluster neurological"
            ),
            IcdConditionEntity(
                code = "DA21",
                title = "Migraine",
                definition = "Recurrent, typically unilateral, pulsating headache often accompanied by nausea, vomiting, and sensitivity to light or sound.",
                bodySystem = "Nervous system",
                chapter = "Neurological system",
                synonyms = "classic migraine, migraine with aura, migraine without aura, vascular headache",
                searchKeywords = "headache aura photophobia phonophobia vomiting neurological"
            ),
            IcdConditionEntity(
                code = "DA22",
                title = "Vertigo",
                definition = "Sensation of spinning or dizziness, often accompanied by nausea, vomiting, and loss of balance. Usually indicates inner ear or brain involvement.",
                bodySystem = "Nervous system",
                chapter = "Neurological system",
                synonyms = "dizziness, spinning sensation, benign paroxysmal positional vertigo, BPPV",
                searchKeywords = "dizzy spinning balance inner ear vestibular"
            ),
            IcdConditionEntity(
                code = "DA23",
                title = "Stroke",
                definition = "Acute neurological injury due to disrupted blood supply to brain (ischemic) or bleeding in brain (hemorrhagic). Medical emergency.",
                bodySystem = "Nervous system",
                chapter = "Neurological system",
                synonyms = "cerebrovascular accident, CVA, TIA, transient ischemic attack, hemorrhagic stroke",
                searchKeywords = "neurological paralysis weakness facial drooping emergency"
            ),

            // Gastrointestinal System
            IcdConditionEntity(
                code = "DA95",
                title = "Abdominal pain",
                definition = "Discomfort or pain in the abdominal region. Can be acute or chronic, localized or diffuse, with multiple possible causes.",
                bodySystem = "Digestive system",
                chapter = "Gastrointestinal system",
                synonyms = "belly pain, stomach pain, cramping, abdominal discomfort, periumbilical pain",
                searchKeywords = "stomach cramp gastric intestinal bowel acute chronic"
            ),
            IcdConditionEntity(
                code = "DA96",
                title = "Nausea and vomiting",
                definition = "Nausea is the sensation of unease and discomfort leading to vomiting, which is the forceful expulsion of stomach contents through the mouth.",
                bodySystem = "Digestive system",
                chapter = "Gastrointestinal system",
                synonyms = "emesis, retching, queasiness, gastric upset, throwing up",
                searchKeywords = "stomach upset gastric digestive food poisoning medication"
            ),
            IcdConditionEntity(
                code = "DA97",
                title = "Diarrhea",
                definition = "Abnormally frequent and unusually liquid bowel movements. Can be acute or chronic with various infectious and non-infectious causes.",
                bodySystem = "Digestive system",
                chapter = "Gastrointestinal system",
                synonyms = "loose stools, bowel dysfunction, gastroenteritis, loose motions, dysentery",
                searchKeywords = "bowel infection virus bacteria food poisoning gastroenteritis"
            ),
            IcdConditionEntity(
                code = "DA98",
                title = "Constipation",
                definition = "Reduced frequency of bowel movements or difficult passage of stool, often associated with hard, dry stools.",
                bodySystem = "Digestive system",
                chapter = "Gastrointestinal system",
                synonyms = "costiveness, irregular bowel movement, hard stools, fecal impaction",
                searchKeywords = "bowel movement difficult hard stool gastric dietary"
            ),

            // Infectious Diseases
            IcdConditionEntity(
                code = "EA90",
                title = "Fever",
                definition = "Elevation of body temperature above normal (>37.5°C or 99.5°F). Common symptom of infection or inflammation.",
                bodySystem = "General symptoms",
                chapter = "Infectious diseases",
                synonyms = "pyrexia, high temperature, elevated body temperature, febrile",
                searchKeywords = "temperature infection viral bacterial inflammatory"
            ),
            IcdConditionEntity(
                code = "EA91",
                title = "Influenza (flu)",
                definition = "Acute viral respiratory infection caused by influenza virus. Presents with fever, cough, muscle aches, and fatigue.",
                bodySystem = "Respiratory system",
                chapter = "Infectious diseases",
                synonyms = "viral flu, seasonal flu, flu virus, orthomyxovirus infection",
                searchKeywords = "viral infection respiratory fever cough muscle pain vaccine"
            ),
            IcdConditionEntity(
                code = "EA92",
                title = "Common cold",
                definition = "Acute viral infection of upper respiratory tract. Presents with nasal congestion, cough, sore throat, and sneezing.",
                bodySystem = "Respiratory system",
                chapter = "Infectious diseases",
                synonyms = "upper respiratory infection, URI, acute rhinitis, viral URI, rhinovirus",
                searchKeywords = "viral respiratory cough congestion sore throat sneezing"
            ),
            IcdConditionEntity(
                code = "EA93",
                title = "COVID-19",
                definition = "Disease caused by SARS-CoV-2 virus. Presents with fever, cough, fatigue, and can progress to severe pneumonia.",
                bodySystem = "Respiratory system",
                chapter = "Infectious diseases",
                synonyms = "coronavirus disease 2019, SARS-CoV-2, pangolin coronavirus",
                searchKeywords = "pandemic viral respiratory fever pneumonia vaccine"
            ),

            // Endocrine/Metabolic System
            IcdConditionEntity(
                code = "EB90",
                title = "Diabetes mellitus",
                definition = "Metabolic disorder characterized by elevated blood glucose due to insufficient insulin production (Type 1) or insulin resistance (Type 2).",
                bodySystem = "Endocrine system",
                chapter = "Endocrine/Metabolic",
                synonyms = "diabetes, Type 1 diabetes, Type 2 diabetes, insulin-dependent diabetes, NIDDM",
                searchKeywords = "blood glucose hyperglycemia hypoglycemia insulin glucose control"
            ),
            IcdConditionEntity(
                code = "EB91",
                title = "Hyperthyroidism",
                definition = "Excessive production of thyroid hormone, leading to elevated metabolic rate and symptoms like weight loss, palpitations, and anxiety.",
                bodySystem = "Endocrine system",
                chapter = "Endocrine/Metabolic",
                synonyms = "overactive thyroid, thyrotoxicosis, Graves disease, thyroid hyperfunction",
                searchKeywords = "thyroid hormone metabolism weight loss anxiety tremor"
            ),
            IcdConditionEntity(
                code = "EB92",
                title = "Hypothyroidism",
                definition = "Insufficient production of thyroid hormone, leading to decreased metabolic rate and symptoms like weight gain, fatigue, and depression.",
                bodySystem = "Endocrine system",
                chapter = "Endocrine/Metabolic",
                synonyms = "underactive thyroid, myxedema, Hashimoto thyroiditis, thyroid hypofunction",
                searchKeywords = "thyroid hormone metabolism weight gain fatigue depression"
            ),

            // Musculoskeletal System
            IcdConditionEntity(
                code = "FA01",
                title = "Back pain",
                definition = "Pain in the lumbar, thoracic, or cervical spine region. Can be acute or chronic, with causes including muscle strain, disc herniation, or arthritis.",
                bodySystem = "Musculoskeletal system",
                chapter = "Musculoskeletal diseases",
                synonyms = "lumbar pain, spinal pain, dorsal pain, lower back pain, lumbago",
                searchKeywords = "spine vertebra muscle disc strain herniation arthritis"
            ),
            IcdConditionEntity(
                code = "FA02",
                title = "Arthritis",
                definition = "Inflammation of one or more joints, causing pain, stiffness, swelling, and reduced mobility. Includes rheumatoid, osteo, and other forms.",
                bodySystem = "Musculoskeletal system",
                chapter = "Musculoskeletal diseases",
                synonyms = "joint inflammation, rheumatoid arthritis, RA, osteoarthritis, OA, gouty arthritis",
                searchKeywords = "joint pain swelling stiffness rheumatoid osteo inflammatory"
            ),
            IcdConditionEntity(
                code = "FA03",
                title = "Fracture",
                definition = "Break or crack in bone structure, can be simple (closed) or compound (open), affecting any bone in the body.",
                bodySystem = "Musculoskeletal system",
                chapter = "Musculoskeletal diseases",
                synonyms = "bone fracture, broken bone, comminuted fracture, stress fracture, pathological fracture",
                searchKeywords = "bone break trauma injury immobilization orthopedic surgery"
            ),

            // Dermatological System
            IcdConditionEntity(
                code = "FA95",
                title = "Urticaria (hives)",
                definition = "Allergic skin reaction characterized by sudden appearance of raised, itchy, red welts or bumps on the skin.",
                bodySystem = "Skin",
                chapter = "Skin diseases",
                synonyms = "hives, nettle rash, wheals, allergic rash, angioedema",
                searchKeywords = "allergic reaction itchy rash skin swelling histamine"
            ),
            IcdConditionEntity(
                code = "FA96",
                title = "Dermatitis",
                definition = "Inflammation of the skin causing redness, itching, and sometimes blistering. Includes contact, atopic, and seborrheic dermatitis.",
                bodySystem = "Skin",
                chapter = "Skin diseases",
                synonyms = "eczema, contact dermatitis, atopic dermatitis, skin inflammation, dermatosis",
                searchKeywords = "skin inflammation itchy rash allergic irritant contact"
            ),

            // Urinary/Renal System
            IcdConditionEntity(
                code = "GA01",
                title = "Urinary tract infection",
                definition = "Bacterial infection of the urinary system including urethra, bladder, ureters, or kidneys. Presents with dysuria, frequency, and urgency.",
                bodySystem = "Urinary system",
                chapter = "Genitourinary system",
                synonyms = "UTI, cystitis, pyelonephritis, urethritis, bladder infection",
                searchKeywords = "urinary burning dysuria kidney bladder infection bacterial"
            ),
            IcdConditionEntity(
                code = "GA02",
                title = "Kidney stones",
                definition = "Hard mineral deposits in the kidney that can cause severe pain when passing through the urinary tract.",
                bodySystem = "Urinary system",
                chapter = "Genitourinary system",
                synonyms = "nephrolithiasis, renal stones, calculi, ureterolithiasis, pain-stone",
                searchKeywords = "kidney pain renal colic stone obstruction mineral"
            ),

            // Psychological/Mental
            IcdConditionEntity(
                code = "QE80",
                title = "Anxiety disorder",
                definition = "Mental health disorder characterized by persistent worry, nervousness, fear, or apprehension affecting daily functioning.",
                bodySystem = "Mental/Behavioral",
                chapter = "Mental disorders",
                synonyms = "generalized anxiety, panic disorder, social anxiety, phobia, anxiety neurosis",
                searchKeywords = "mental health anxiety worry panic fear psychological stress"
            ),
            IcdConditionEntity(
                code = "QE81",
                title = "Depression",
                definition = "Mental disorder marked by persistent sadness, hopelessness, loss of interest, fatigue, and changes in sleep or appetite.",
                bodySystem = "Mental/Behavioral",
                chapter = "Mental disorders",
                synonyms = "major depressive disorder, clinical depression, dysphoria, depressive episode",
                searchKeywords = "mental health sadness hopelessness mood sleep appetite psychological"
            )
        )

        try {
            dao.insertConditions(defaultConditions)
            Log.d(TAG, "Successfully loaded ${defaultConditions.size} default ICD conditions")
        } catch (e: Exception) {
            Log.e(TAG, "Error loading default conditions", e)
            throw e
        }
    }

    /**
     * Search ICD conditions by symptom/keyword (local DB + API)
     */
    suspend fun searchConditions(query: String, limit: Int = Constants.ICD_SEARCH_LIMIT): List<IcdCondition> {
        if (query.isBlank()) return emptyList()

        val cacheKey = "search_$query"
        searchCache[cacheKey]?.let { return it }

        return try {
            withContext(Dispatchers.IO) {
                // Search local DB first
                val localResults = dao.searchConditionsByTitle(query.trim(), limit)
                    .map { it.toDomain() }

                // Try API search (with fallback to local only if offline)
                val apiResults = try {
                    apiClient.searchConditions(query, limit).map { it.toIcdCondition() }
                } catch (e: Exception) {
                    Log.w(TAG, "API search failed, using local results only", e)
                    emptyList()
                }

                // Combine and deduplicate results (prioritize local ICD-11, then add API ICD-10)
                val combinedResults = mutableListOf<IcdCondition>()
                combinedResults.addAll(localResults)

                // Add API results that don't conflict with local codes
                val localCodes = localResults.map { it.code }.toSet()
                apiResults.forEach { apiCondition ->
                    if (apiCondition.code !in localCodes) {
                        combinedResults.add(apiCondition)
                    }
                }

                // Limit total results
                val finalResults = combinedResults.take(limit)
                searchCache[cacheKey] = finalResults
                finalResults
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error searching conditions", e)
            emptyList()
        }
    }

    /**
     * Get ICD condition by exact code
     */
    suspend fun getConditionByCode(code: String): IcdCondition? {
        return try {
            withContext(Dispatchers.IO) {
                dao.getConditionByCode(code)?.toDomain()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching condition by code", e)
            null
        }
    }

    /**
     * Search conditions by body system
     */
    suspend fun searchByBodySystem(system: String, limit: Int = Constants.FACILITIES_LIMIT * 5): List<IcdCondition> {
        if (system.isBlank()) return emptyList()

        return try {
            withContext(Dispatchers.IO) {
                dao.searchConditionsByBodySystem(system, limit)
                    .map { it.toDomain() }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error searching by body system", e)
            emptyList()
        }
    }

    /**
     * Build a context string for the LLM prompt based on user symptoms
     * Searches for top matching conditions and formats them for injection into system prompt
     */
    suspend fun buildConditionContext(symptomQuery: String): String {
        if (symptomQuery.isBlank()) return ""

        return try {
            val matchedConditions = searchConditions(symptomQuery, Constants.ICD_SEARCH_LIMIT)
            if (matchedConditions.isEmpty()) return ""

            val contextBuilder = StringBuilder()
            contextBuilder.append("\n## Relevant Medical Conditions (ICD-11/ICD-10):\n")
            matchedConditions.forEachIndexed { index, condition ->
                val icdVersion = if (condition.chapter == "ICD-10-CM") "ICD-10" else "ICD-11"
                contextBuilder.append("${index + 1}. [${condition.code}] ${condition.title} ($icdVersion)\n")
                contextBuilder.append("   - Definition: ${condition.definition.take(150)}...\n")
                contextBuilder.append("   - Body System: ${condition.bodySystem}\n")
            }
            contextBuilder.append("\nUse these medical codes as reference for differential diagnosis.\n")

            contextBuilder.toString()
        } catch (e: Exception) {
            Log.e(TAG, "Error building condition context", e)
            ""
        }
    }

    /**
     * Get comprehensive medical context for multiple symptom keywords
     */
    suspend fun buildComprehensiveMedicalContext(symptoms: List<String>): String {
        if (symptoms.isEmpty()) return ""

        val allContexts = mutableListOf<String>()
        for (symptom in symptoms.take(3)) { // Limit to top 3 symptoms
            val context = buildConditionContext(symptom)
            if (context.isNotEmpty()) {
                allContexts.add(context)
            }
        }

        return if (allContexts.isEmpty()) {
            ""
        } else {
            "\n=== MEDICAL KNOWLEDGE BASE ===\n" +
            allContexts.joinToString("\n---\n") +
            "\n=== END KNOWLEDGE BASE ===\n"
        }
    }

    /**
     * Get database statistics
     */
    suspend fun getDatabaseStats(): Map<String, Int> {
        return try {
            withContext(Dispatchers.IO) {
                val totalConditions = dao.getConditionCount()
                val circulatoryCount = dao.getConditionCountByBodySystem("Circulatory system")
                val respiratoryCount = dao.getConditionCountByBodySystem("Respiratory system")

                mapOf(
                    "total" to totalConditions,
                    "circulatory" to circulatoryCount,
                    "respiratory" to respiratoryCount
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting database stats", e)
            emptyMap()
        }
    }

    private fun IcdConditionEntity.toDomain(): IcdCondition {
        return IcdCondition(
            code = this.code,
            title = this.title,
            definition = this.definition,
            bodySystem = this.bodySystem,
            chapter = this.chapter,
            synonyms = this.synonyms
        )
    }
}
