package com.simbiri.data.repository

import com.simbiri.data.database.entity.therapy.TherapyAssetTable
import com.simbiri.data.database.entity.therapy.TherapyModuleTable
import com.simbiri.data.database.entity.therapy.TherapySessionContraindicationTable
import com.simbiri.data.database.entity.therapy.TherapySessionCultureTagTable
import com.simbiri.data.database.entity.therapy.TherapySessionGoalTable
import com.simbiri.data.database.entity.therapy.TherapySessionTable
import com.simbiri.data.database.entity.user.UserTable
import com.simbiri.domain.model.common.TherapyModuleId
import com.simbiri.domain.model.common.TherapySessionId
import com.simbiri.domain.model.common.UserId
import com.simbiri.domain.model.therapy.TherapeuticPriority
import com.simbiri.domain.model.therapy.TherapyContentStatus
import com.simbiri.domain.model.therapy.TherapyContraindication
import com.simbiri.domain.model.therapy.TherapyGoal
import com.simbiri.domain.model.therapy.TherapyIntensity
import com.simbiri.domain.model.therapy.TherapyModality
import com.simbiri.domain.model.therapy.TherapyModule
import com.simbiri.domain.model.therapy.TherapySession
import com.simbiri.domain.model.user.UserType
import com.simbiri.domain.util.DataError
import com.simbiri.domain.util.ResultType
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.AfterClass
import org.junit.Before
import org.junit.BeforeClass
import org.testcontainers.postgresql.PostgreSQLContainer
import java.sql.Connection
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * PostgreSQL-backed integration coverage for the complete authored therapy-content aggregate.
 *
 * These tests verify real Exposed statements, foreign keys, uniqueness constraints, aggregate reconstruction,
 * module ordering, lifecycle compare-and-set behavior, and draft deletion.
 */
class TherapyContentPersistenceIntegrationTest {
    private lateinit var repository: TherapyContentRepoImpl

    @Before
    fun resetPersistenceState() {
        repository = TherapyContentRepoImpl(
            db = database,
            clock = CLOCK,
        )

        resetDatabase()
    }

    @Test
    fun `draft metadata and classifications persist through update`() = runBlocking {
        val therapySessionId = createDraft()
        val created = loadSession(therapySessionId)

        assertNotNull(created.seriesId)
        assertEquals(TherapyContentStatus.DRAFT, created.status)
        assertEquals(1, created.version)
        assertEquals("Grounding Through Color", created.title)
        assertEquals(setOf(TherapyGoal.GROUNDING, TherapyGoal.STRESS_RELIEF), created.goalTags)
        assertEquals(
            setOf(TherapyContraindication.HIGH_EMOTIONAL_INTENSITY),
            created.contraindications,
        )
        assertEquals(setOf("Kenyan", "Ubuntu"), created.cultureTags)

        val updateResult = repository.updateDraftDetails(
            created.copy(
                title = "Poetic Grounding",
                description = "A reflective drawing and poetry exercise.",
                tagline = "Breathe, write, and draw",
                therapeuticPriority = TherapeuticPriority.MENTAL_HEALTH,
                intensity = TherapyIntensity.MODERATE,
                locale = "sw-KE",
                goalTags = setOf(TherapyGoal.RELAXATION, TherapyGoal.SELF_EXPRESSION),
                contraindications = emptySet(),
                cultureTags = setOf("Kenyan", "Poetry"),
            )
        )

        assertIs<ResultType.Success<Unit>>(updateResult)

        val updated = loadSession(therapySessionId)

        assertEquals("Poetic Grounding", updated.title)
        assertEquals("A reflective drawing and poetry exercise.", updated.description)
        assertEquals("Breathe, write, and draw", updated.tagline)
        assertEquals(TherapyIntensity.MODERATE, updated.intensity)
        assertEquals("sw-KE", updated.locale)
        assertEquals(setOf(TherapyGoal.RELAXATION, TherapyGoal.SELF_EXPRESSION), updated.goalTags)
        assertEquals(emptySet(), updated.contraindications)
        assertEquals(setOf("Kenyan", "Poetry"), updated.cultureTags)

        val latestResult = repository.getLatestTherapySessionVersion(
            seriesId = requireNotNull(updated.seriesId),
        )
        val latest = assertIs<ResultType.Success<TherapySession>>(latestResult).data

        assertEquals(updated.id, latest.id)
        assertEquals(updated.seriesId, latest.seriesId)
        assertEquals(updated.version, latest.version)
    }

    @Test
    fun `module mutations preserve complete contiguous ordering`() = runBlocking {
        val therapySessionId = createDraft()
        val moduleIds = mutableListOf<TherapyModuleId>()

        repeat(4) { index ->
            val result = repository.addModule(
                therapySessionId = therapySessionId,
                module = moduleFixture(
                    orderIndex = index,
                    title = "Module ${index + 1}",
                ),
            )

            moduleIds += assertIs<ResultType.Success<TherapyModuleId>>(result).data
        }

        val initialSession = loadSession(therapySessionId)
        val openingModule = initialSession.modules.single { module ->
            module.id == moduleIds[0]
        }

        val updateResult = repository.updateModule(
            therapySessionId = therapySessionId,
            module = openingModule.copy(
                title = "Updated Opening Breath",
                instructions = "Draw one slow line while breathing out.",
            ),
        )

        assertIs<ResultType.Success<Unit>>(updateResult)

        val requestedOrder = listOf(
            moduleIds[3],
            moduleIds[1],
            moduleIds[0],
            moduleIds[2],
        )

        val reorderResult = repository.reorderModules(
            therapySessionId = therapySessionId,
            orderedModuleIds = requestedOrder,
        )

        assertIs<ResultType.Success<Unit>>(reorderResult)

        val reorderedSession = loadSession(therapySessionId)

        assertEquals(requestedOrder, reorderedSession.modules.mapNotNull(TherapyModule::id))
        assertEquals(listOf(0, 1, 2, 3), reorderedSession.modules.map(TherapyModule::orderIndex))

        val removeResult = repository.removeModule(
            therapySessionId = therapySessionId,
            therapyModuleId = moduleIds[1],
        )

        assertIs<ResultType.Success<Unit>>(removeResult)

        val remainingSession = loadSession(therapySessionId)
        val expectedRemainingOrder = listOf(
            moduleIds[3],
            moduleIds[0],
            moduleIds[2],
        )

        assertEquals(expectedRemainingOrder, remainingSession.modules.mapNotNull(TherapyModule::id))
        assertEquals(listOf(0, 1, 2), remainingSession.modules.map(TherapyModule::orderIndex))
        assertEquals(
            "Updated Opening Breath",
            remainingSession.modules.single { module -> module.id == moduleIds[0] }.title,
        )
    }

    @Test
    fun `lifecycle transitions persist status and publication timestamps`() = runBlocking {
        val therapySessionId = createDraft(
            draftFixture(
                modules = publishableModules(),
            )
        )

        assertTransition(
            therapySessionId = therapySessionId,
            expectedStatus = TherapyContentStatus.DRAFT,
            targetStatus = TherapyContentStatus.IN_REVIEW,
            transitionedAt = FIRST_REVIEWED_AT,
        )

        assertTransition(
            therapySessionId = therapySessionId,
            expectedStatus = TherapyContentStatus.IN_REVIEW,
            targetStatus = TherapyContentStatus.DRAFT,
            transitionedAt = RETURNED_TO_DRAFT_AT,
        )

        assertEquals(
            TherapyContentStatus.DRAFT,
            loadSession(therapySessionId).status,
        )

        assertTransition(
            therapySessionId = therapySessionId,
            expectedStatus = TherapyContentStatus.DRAFT,
            targetStatus = TherapyContentStatus.IN_REVIEW,
            transitionedAt = SECOND_REVIEWED_AT,
        )

        assertTransition(
            therapySessionId = therapySessionId,
            expectedStatus = TherapyContentStatus.IN_REVIEW,
            targetStatus = TherapyContentStatus.PUBLISHED,
            transitionedAt = PUBLISHED_AT,
        )

        val published = loadSession(therapySessionId)

        assertEquals(TherapyContentStatus.PUBLISHED, published.status)
        assertEquals(PUBLISHED_AT, published.publishedAt)
        assertNull(published.archivedAt)

        /*
         * A stale command that still expects DRAFT must not overwrite the published state.
         */
        val staleTransitionResult = repository.transitionStatus(
            therapySessionId = therapySessionId,
            expectedStatus = TherapyContentStatus.DRAFT,
            targetStatus = TherapyContentStatus.IN_REVIEW,
            transitionedAt = PUBLISHED_AT.plusSeconds(1),
        )

        val staleFailure = assertIs<ResultType.Failure<DataError>>(staleTransitionResult)

        assertIs<DataError.Conflict>(staleFailure.error)
        assertEquals(TherapyContentStatus.PUBLISHED, loadSession(therapySessionId).status)

        assertTransition(
            therapySessionId = therapySessionId,
            expectedStatus = TherapyContentStatus.PUBLISHED,
            targetStatus = TherapyContentStatus.ARCHIVED,
            transitionedAt = ARCHIVED_AT,
        )

        val archived = loadSession(therapySessionId)

        assertEquals(TherapyContentStatus.ARCHIVED, archived.status)
        assertEquals(PUBLISHED_AT, archived.publishedAt)
        assertEquals(ARCHIVED_AT, archived.archivedAt)
    }

    @Test
    fun `draft deletion removes aggregate while published deletion is rejected`() = runBlocking {
        val draftId = createDraft(
            draftFixture(
                modules = publishableModules(),
            )
        )

        val deleteDraftResult = repository.deleteDraft(draftId)

        assertIs<ResultType.Success<Unit>>(deleteDraftResult)

        val missingDraftResult = repository.getTherapySessionById(draftId)
        val missingDraftFailure = assertIs<ResultType.Failure<DataError>>(missingDraftResult)

        assertEquals(DataError.NotFound, missingDraftFailure.error)

        val publishedId = createDraft(
            draftFixture(
                modules = publishableModules(),
            )
        )

        assertTransition(
            therapySessionId = publishedId,
            expectedStatus = TherapyContentStatus.DRAFT,
            targetStatus = TherapyContentStatus.IN_REVIEW,
            transitionedAt = FIRST_REVIEWED_AT,
        )

        assertTransition(
            therapySessionId = publishedId,
            expectedStatus = TherapyContentStatus.IN_REVIEW,
            targetStatus = TherapyContentStatus.PUBLISHED,
            transitionedAt = PUBLISHED_AT,
        )

        val deletePublishedResult = repository.deleteDraft(publishedId)
        val deletePublishedFailure = assertIs<ResultType.Failure<DataError>>(deletePublishedResult)

        assertIs<DataError.Conflict>(deletePublishedFailure.error)
        assertEquals(TherapyContentStatus.PUBLISHED, loadSession(publishedId).status)
    }

    private suspend fun createDraft(
        session: TherapySession = draftFixture(),
    ): TherapySessionId {
        val result = repository.createDraft(session)

        return assertIs<ResultType.Success<TherapySessionId>>(result).data
    }

    private suspend fun loadSession(
        therapySessionId: TherapySessionId,
    ): TherapySession {
        val result = repository.getTherapySessionById(therapySessionId)

        return assertIs<ResultType.Success<TherapySession>>(result).data
    }

    private suspend fun assertTransition(
        therapySessionId: TherapySessionId,
        expectedStatus: TherapyContentStatus,
        targetStatus: TherapyContentStatus,
        transitionedAt: Instant,
    ) {
        val result = repository.transitionStatus(
            therapySessionId = therapySessionId,
            expectedStatus = expectedStatus,
            targetStatus = targetStatus,
            transitionedAt = transitionedAt,
        )

        assertIs<ResultType.Success<Unit>>(result)
    }

    private fun draftFixture(
        modules: List<TherapyModule> = emptyList(),
    ): TherapySession = TherapySession(
        authorId = AUTHOR_ID,
        title = "Grounding Through Color",
        description = "A guided breathing and drawing exercise for grounding.",
        intensity = TherapyIntensity.GENTLE,
        locale = "en-KE",
        tagline = "Let each stroke be a breath",
        status = TherapyContentStatus.DRAFT,
        version = 1,
        therapeuticPriority = TherapeuticPriority.MENTAL_HEALTH,
        goalTags = setOf(TherapyGoal.GROUNDING, TherapyGoal.STRESS_RELIEF),
        contraindications = setOf(TherapyContraindication.HIGH_EMOTIONAL_INTENSITY),
        cultureTags = setOf("Kenyan", "Ubuntu"),
        modules = modules,
    )

    private fun publishableModules(): List<TherapyModule> = listOf(
        moduleFixture(
            orderIndex = 0,
            title = "Opening Breath",
            modality = TherapyModality.BREATHING,
        ),
        moduleFixture(
            orderIndex = 1,
            title = "Grounding Lines",
            modality = TherapyModality.DRAWING,
        ),
        moduleFixture(
            orderIndex = 2,
            title = "Closing Reflection",
            modality = TherapyModality.REFLECTION,
        ),
    )

    private fun moduleFixture(
        orderIndex: Int,
        title: String,
        modality: TherapyModality = TherapyModality.DRAWING,
    ): TherapyModule = TherapyModule(
        orderIndex = orderIndex,
        title = title,
        goal = "Support calm attention and emotional awareness.",
        instructions = "Follow the prompt slowly and notice how your body feels.",
        whyThisHelps = "Focused creative movement can support grounding and reflection.",
        modality = modality,
        estimatedDurationSeconds = 60,
        isSkippable = false,
        isRepeatable = true,
    )

    private fun resetDatabase() {
        transaction(database) {
            SchemaUtils.drop(
                TherapyAssetTable,
                TherapySessionCultureTagTable,
                TherapySessionContraindicationTable,
                TherapySessionGoalTable,
                TherapyModuleTable,
                TherapySessionTable,
                UserTable,
            )

            SchemaUtils.create(
                UserTable,
                TherapySessionTable,
                TherapyModuleTable,
                TherapyAssetTable,
                TherapySessionGoalTable,
                TherapySessionContraindicationTable,
                TherapySessionCultureTagTable,
            )

            UserTable.insert { row ->
                row[UserTable.id] = AUTHOR_ID.value
                row[UserTable.accountName] = "therapy-author"
                row[UserTable.emailAddress] = "therapy-author@example.com"
                row[UserTable.firstName] = "Therapy"
                row[UserTable.lastName] = "Author"
                row[UserTable.about] = null
                row[UserTable.tagline] = null
                row[UserTable.profileUrl] = null
                row[UserTable.backgroundUrl] = null
                row[UserTable.birthDate] = LocalDate.parse("1990-01-01")
                row[UserTable.userType] = UserType.PSYCHOLOGIST.code
                row[UserTable.emailOptIn] = false
                row[UserTable.isPrivate] = true
                row[UserTable.isAnonymous] = false
                row[UserTable.isActive] = true
                row[UserTable.createdAt] = CREATED_AT
                row[UserTable.updatedAt] = CREATED_AT
            }
        }
    }

    companion object {
        private val postgres =
            PostgreSQLContainer("postgres:16-alpine").withDatabaseName("artnovax_therapy_test")
            .withUsername("artnovax_test").withPassword("artnovax_test")

        private lateinit var database: Database

        private val AUTHOR_ID = UserId(
            UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa")
        )

        private val CREATED_AT = Instant.parse("2026-08-06T17:30:00Z")
        private val FIRST_REVIEWED_AT = Instant.parse("2026-08-06T17:40:00Z")
        private val RETURNED_TO_DRAFT_AT = Instant.parse("2026-08-06T17:50:00Z")
        private val SECOND_REVIEWED_AT = Instant.parse("2026-08-06T18:00:00Z")
        private val PUBLISHED_AT = Instant.parse("2026-08-06T18:10:00Z")
        private val ARCHIVED_AT = Instant.parse("2026-08-06T18:20:00Z")
        private val CLOCK = Clock.fixed(CREATED_AT, ZoneOffset.UTC)

        @JvmStatic
        @BeforeClass
        fun startPostgres() {
            postgres.start()

            database = Database.connect(
                url = postgres.jdbcUrl,
                driver = "org.postgresql.Driver",
                user = postgres.username,
                password = postgres.password,
            )

            TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE
        }

        @JvmStatic
        @AfterClass
        fun stopPostgres() {
            postgres.stop()
        }
    }
}
