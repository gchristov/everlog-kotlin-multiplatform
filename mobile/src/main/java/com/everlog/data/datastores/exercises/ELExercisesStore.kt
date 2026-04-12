package com.everlog.data.datastores.exercises

import com.everlog.constants.ELConstants
import com.everlog.data.datastores.base.ELCollectionStore
import com.everlog.data.datastores.events.collection.ELColStoreItemAddedEvent
import com.everlog.data.datastores.events.collection.ELColStoreItemModifiedEvent
import com.everlog.data.datastores.events.collection.ELColStoreItemRemovedEvent
import com.everlog.data.datastores.events.collection.ELColStoreItemsLoadedEvent
import com.everlog.data.model.exercise.ELExercise
import com.everlog.managers.firebase.FirestorePathManager
import com.google.firebase.firestore.Query
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import rx.subjects.PublishSubject
import timber.log.Timber
import java.util.*
import java.util.concurrent.TimeUnit

class ELExercisesStore : ELCollectionStore<ELExercise>() {

    private val mAllItemsReady = PublishSubject.create<List<ELExercise>>()
    private val mUserExercisesStore = ELUserExercisesStore()

    init {
        observeItemsLoaded()
    }

    fun observeAllItemsReady(): Observable<List<ELExercise>> {
        return mAllItemsReady
    }

    override fun destroy() {
        mUserExercisesStore.destroy()
        super.destroy()
    }

    override fun getType(): Class<ELExercise> {
        return ELExercise::class.java
    }

    override fun getQuery(): Query {
        return FirestorePathManager.globalExercisesCollection
                .orderBy(ELConstants.FIELD_NAME)
    }

    override fun getTag(): String {
        return "ELExercisesStore"
    }

    override fun getItems() {
        mUserExercisesStore.getItems()
        super.getItems()
    }

    // Observers
    private fun observeItemsLoaded() {
        Observable.combineLatest(onItemsLoadedSubscription(),
                mUserExercisesStore.onItemsLoadedSubscription()
        ) { global: List<ELExercise>, user: List<ELExercise> ->
            Timber.tag(tag).d("Merging results")
            val merged = ArrayList(global)
            merged.addAll(user)
            merged.sortWith(Comparator { e1: ELExercise, e2: ELExercise -> compareTwoExercises(e1, e2) })
            merged
        }
                .onBackpressureBuffer()
                .debounce(300, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ result: List<ELExercise> ->
                    Timber.tag(tag).d("Notifying merged items ready: items=%s", result.size)
                    mAllItemsReady.onNext(result)
                }) { throwable: Throwable? -> mAllItemsReady.onError(throwable) }
    }

    private fun compareTwoExercises(e1: ELExercise, e2: ELExercise): Int {
        return e1.name!!.compareTo(e2.name!!)
    }

    // Events

    override fun getCollectionStoreItemAddedEvent(position: Int,
                                                  item: ELExercise,
                                                  hasPendingWrites: Boolean,
                                                  fromCache: Boolean): ELColStoreItemAddedEvent<ELExercise> {
        return ELColStoreExerciseAddedEvent(position, item, hasPendingWrites, fromCache)
    }

    override fun getCollectionStoreItemModifiedEvent(oldPosition: Int,
                                                     newPosition: Int,
                                                     item: ELExercise,
                                                     hasPendingWrites: Boolean,
                                                     fromCache: Boolean): ELColStoreItemModifiedEvent<ELExercise> {
        return ELColStoreExerciseModifiedEvent(oldPosition, newPosition, item, hasPendingWrites, fromCache)
    }

    override fun getCollectionStoreItemRemovedEvent(position: Int,
                                                    item: ELExercise,
                                                    hasPendingWrites: Boolean,
                                                    fromCache: Boolean): ELColStoreItemRemovedEvent<ELExercise> {
        return ELColStoreExerciseRemovedEvent(position, item, hasPendingWrites, fromCache)
    }

    override fun getCollectionStoreItemLoadedEvent(items: List<ELExercise>?,
                                                   error: Throwable?,
                                                   fromCache: Boolean): ELColStoreItemsLoadedEvent<ELExercise> {
        return ELColStoreExercisesLoadedEvent(items, error, fromCache)
    }

    class ELColStoreExerciseAddedEvent internal constructor(position: Int,
                                                            item: ELExercise,
                                                            hasPendingWrites: Boolean,
                                                            fromCache: Boolean) : ELColStoreItemAddedEvent<ELExercise>(position, item, hasPendingWrites, fromCache)

    class ELColStoreExerciseModifiedEvent internal constructor(oldPosition: Int,
                                                               newPosition: Int,
                                                               item: ELExercise,
                                                               hasPendingWrites: Boolean,
                                                               fromCache: Boolean) : ELColStoreItemModifiedEvent<ELExercise>(oldPosition, newPosition, item, hasPendingWrites, fromCache)

    class ELColStoreExerciseRemovedEvent internal constructor(position: Int,
                                                              item: ELExercise,
                                                              hasPendingWrites: Boolean,
                                                              fromCache: Boolean) : ELColStoreItemRemovedEvent<ELExercise>(position, item, hasPendingWrites, fromCache)

    class ELColStoreExercisesLoadedEvent internal constructor(items: List<ELExercise>?,
                                                              error: Throwable?,
                                                              fromCache: Boolean) : ELColStoreItemsLoadedEvent<ELExercise>(items, error, fromCache)
}