package com.everlog.android.template.testfixtures

import arrow.core.Either
import com.everlog.android.common.data.model.ELRoutine
import com.everlog.android.common.kotlin.Load
import com.everlog.android.template.data.RoutineRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf

class FakeRoutineRepository(
    var saveRoutines: Either<Throwable, Unit> = Either.Right(Unit),
    var routine: Flow<Load<ELRoutine?>> = flowOf(Load.Ready(null)),
) : RoutineRepository {
    // What routines() gives, changed by the test as it goes
    val routines = MutableStateFlow<Load<List<ELRoutine>>>(Load.Loading)

    // The routines of each save, in order
    val saved = mutableListOf<List<ELRoutine>>()

    override fun routine(uuid: String): Flow<Load<ELRoutine?>> = routine

    override fun routines(): Flow<Load<List<ELRoutine>>> = routines

    override suspend fun saveRoutines(routines: List<ELRoutine>): Either<Throwable, Unit> {
        saved += routines
        return saveRoutines
    }

    override suspend fun exerciseTemplateCounts(): Either<Throwable, Map<String, Int>> = Either.Right(emptyMap())
}
