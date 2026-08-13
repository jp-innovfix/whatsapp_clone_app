package com.jp.whatsappclone.data.repository

import androidx.room.withTransaction
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.DocumentChange
import com.jp.whatsappclone.data.domain.Member
import com.jp.whatsappclone.data.domain.SessionState
import com.jp.whatsappclone.data.local.MemberDao
import com.jp.whatsappclone.data.local.InnovfixDatabase
import com.jp.whatsappclone.data.local.SearchDao
import com.jp.whatsappclone.data.local.SearchIndex
import com.jp.whatsappclone.data.mapper.toDomain
import com.jp.whatsappclone.data.mapper.toMemberEntity
import com.jp.whatsappclone.data.remote.BackendConfiguration
import com.jp.whatsappclone.data.remote.FirebaseServices
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseDirectoryRepository @Inject constructor(
    private val authRepository: AuthRepository,
    private val services: FirebaseServices,
    private val configuration: BackendConfiguration,
    private val memberDao: MemberDao,
    private val searchDao: SearchDao,
    private val database: InnovfixDatabase,
) : DirectoryRepository {
    override fun observeActiveMembers(): Flow<List<Member>> = authRepository.observeSession().flatMapLatest { session ->
        val userId = (session as? SessionState.SignedIn)?.user?.id
        if (userId == null) flowOf(emptyList()) else observeForAuthenticatedUser(userId)
    }

    private fun observeForAuthenticatedUser(userId: String): Flow<List<Member>> = channelFlow {
        launch { memberDao.observeActive().collect { send(it.map { entity -> entity.toDomain() }) } }
        val listener: ListenerRegistration? = services.firestore
            ?.collection("organizations")
            ?.document(configuration.organizationId)
            ?.collection("members")
            ?.whereEqualTo("active", true)
            ?.orderBy("displayName", Query.Direction.ASCENDING)
            ?.addSnapshotListener { snapshot, _ ->
                val currentSnapshot = snapshot
                    ?.takeUnless { it.metadata.isFromCache }
                    ?: return@addSnapshotListener
                launch {
                    database.withTransaction {
                        val owner = database.localAccountDao().get()
                        if (
                            services.auth?.currentUser?.uid != userId ||
                            owner?.ownerUid != userId ||
                            owner.orgId != configuration.organizationId
                        ) return@withTransaction
                        currentSnapshot.documentChanges.forEach { change ->
                            val entity = change.document.toMemberEntity(configuration.organizationId)
                            if (change.type == DocumentChange.Type.REMOVED || entity == null) {
                                memberDao.delete(change.document.id)
                                searchDao.remove(SearchIndex.MEMBER, change.document.id)
                            } else {
                                memberDao.upsert(entity)
                                searchDao.index(SearchIndex.member(entity))
                            }
                        }
                        val remoteIds = currentSnapshot.documents.mapTo(mutableSetOf()) { it.id }
                        memberDao.ids().filterNot(remoteIds::contains).forEach { memberId ->
                            memberDao.delete(memberId)
                            searchDao.remove(SearchIndex.MEMBER, memberId)
                        }
                    }
                }
            }
        awaitClose { listener?.remove() }
    }
}
