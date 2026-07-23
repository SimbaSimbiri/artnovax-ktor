package com.simbiri.domain.repository

import com.simbiri.domain.model.common.TherapyModuleId
import com.simbiri.domain.model.common.TherapySessionId
import com.simbiri.domain.model.common.TherapySessionSeriesId
import com.simbiri.domain.model.common.Timestamp
import com.simbiri.domain.model.common.UserId
import com.simbiri.domain.model.therapy.TherapyContentStatus
import com.simbiri.domain.model.therapy.TherapyGoal
import com.simbiri.domain.model.therapy.TherapyIntensity
import com.simbiri.domain.model.therapy.TherapyModule
import com.simbiri.domain.model.therapy.TherapySession
import com.simbiri.domain.util.DataError
import com.simbiri.domain.util.ResultType

/**
 * Defines persistence operations for the Therapy Content aggregate.
 *
 * Authorization, domain-policy validation, and HTTP behavior belong to
 * the application, domain, and presentation layers respectively.
 */
interface TherapyContentRepository {

    /**
     * Retrieves therapy-session versions using optional content filters.
     */
    suspend fun getTherapySessions(
        status: TherapyContentStatus? = null,
        authorId: UserId? = null,
        goal: TherapyGoal? = null,
        intensity: TherapyIntensity? = null,
        locale: String? = null,
    ): ResultType<List<TherapySession>, DataError>

    /**
     * Retrieves one complete aggregate, including ordered modules,
     * assets, goals, contraindications, and cultural tags.
     */
    suspend fun getTherapySessionById(
        therapySessionId: TherapySessionId,
    ): ResultType<TherapySession, DataError>

    /**
     * Retrieves the highest persisted version within a session series.
     */
    suspend fun getLatestTherapySessionVersion(
        seriesId: TherapySessionSeriesId,
    ): ResultType<TherapySession, DataError>

    /**
     * Creates an initial draft or a later draft version.
     *
     * Initial draft:
     * - id must be null;
     * - seriesId may be null;
     * - version must equal 1.
     *
     * Later version:
     * - id must be null;
     * - seriesId must identify an existing series;
     * - version must be greater than 1.
     */
    suspend fun createDraft(
        session: TherapySession,
    ): ResultType<TherapySessionId, DataError>

    /**
     * Updates session-level draft details, classification sets, and
     * cover asset.
     *
     * Modules are managed through the dedicated module operations below.
     */
    suspend fun updateDraftDetails(
        session: TherapySession,
    ): ResultType<Unit, DataError>

    suspend fun addModule(
        therapySessionId: TherapySessionId,
        module: TherapyModule,
    ): ResultType<TherapyModuleId, DataError>

    suspend fun updateModule(
        therapySessionId: TherapySessionId,
        module: TherapyModule,
    ): ResultType<Unit, DataError>

    /**
     * Applies the supplied complete ordering.
     *
     * The list must contain every existing module exactly once.
     */
    suspend fun reorderModules(
        therapySessionId: TherapySessionId,
        orderedModuleIds: List<TherapyModuleId>,
    ): ResultType<Unit, DataError>

    suspend fun removeModule(
        therapySessionId: TherapySessionId,
        therapyModuleId: TherapyModuleId,
    ): ResultType<Unit, DataError>

    /**
     * Performs an atomic compare-and-set status transition.
     *
     * The update succeeds only when the persisted status equals
     * expectedStatus. This prevents concurrent lifecycle changes from
     * silently overwriting one another.
     */
    suspend fun transitionStatus(
        therapySessionId: TherapySessionId,
        expectedStatus: TherapyContentStatus,
        targetStatus: TherapyContentStatus,
        transitionedAt: Timestamp,
    ): ResultType<Unit, DataError>

    /**
     * Permanently removes a draft and its owned records.
     *
     * The repository must reject deletion when the persisted status is
     * not DRAFT.
     */
    suspend fun deleteDraft(
        therapySessionId: TherapySessionId,
    ): ResultType<Unit, DataError>
}
